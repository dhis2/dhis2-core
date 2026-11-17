/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.validation;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataDumpService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Validation analysis with more than one org unit chunk, so the chunks are evaluated on worker
 * threads of a fork join pool.
 *
 * <p>Not transactional on purpose: the worker threads use their own transactions and can only see
 * committed data. The test data is removed by the test framework after each test.
 */
class ValidationServiceParallelChunksTest extends PostgresIntegrationTestBase {

  @Autowired private ValidationService validationService;
  @Autowired private ValidationRuleService validationRuleService;
  @Autowired private DataElementService dataElementService;
  @Autowired private CategoryService categoryService;
  @Autowired private DataDumpService dataDumpService;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private PeriodService periodService;
  @Autowired private TransactionTemplate transactionTemplate;

  /**
   * A data value with a non-default attribute option combo makes the chunk look the combo up by id
   * on its worker thread, which reads it as the current user. Without the caller's security context
   * on the worker thread that lookup failed with "No authentication found" and the chunk was
   * skipped, silently dropping its violations.
   */
  @Test
  void chunkWithNonDefaultAttributeOptionComboRunsAsCaller() {
    int orgUnitCount = ValidationRunContext.ORG_UNITS_PER_TASK + 1;
    record Setup(ValidationRule rule, Period period, CategoryOptionCombo aoc) {}
    Setup setup =
        transactionTemplate.execute(
            status -> {
              CategoryOption option = createCategoryOption('X');
              categoryService.addCategoryOption(option);
              Category category = createCategory('X', option);
              categoryService.addCategory(category);
              CategoryCombo combo = createCategoryCombo('X', category);
              categoryService.addCategoryCombo(combo);
              CategoryOptionCombo aoc = createCategoryOptionCombo(combo, option);
              categoryService.addCategoryOptionCombo(aoc);
              CategoryOptionCombo defaultCoc = categoryService.getDefaultCategoryOptionCombo();

              DataElement de = createDataElement('A');
              dataElementService.addDataElement(de);
              Period period =
                  createPeriod(new MonthlyPeriodType(), getDate(2000, 3, 1), getDate(2000, 3, 31));
              periodService.addPeriod(period);
              OrganisationUnit withData = null;
              for (int i = 0; i < orgUnitCount; i++) {
                OrganisationUnit unit = createOrganisationUnit("Unit" + i);
                organisationUnitService.addOrganisationUnit(unit);
                withData = unit;
              }
              dataDumpService.upsertValues(
                  createDataValue(de, period, withData, defaultCoc, aoc, "5"));

              ValidationRule rule =
                  createValidationRule(
                      'A',
                      equal_to,
                      new Expression("#{" + de.getUid() + "}", "left"),
                      new Expression("0", "right"),
                      new MonthlyPeriodType());
              validationRuleService.saveValidationRule(rule);
              return new Setup(rule, period, aoc);
            });
    JobProgress progress = mock(JobProgress.class, CALLS_REAL_METHODS);

    List<ValidationResult> results =
        validationService.validationAnalysis(
            validationService
                .newParamsBuilder(List.of(setup.rule()), null, List.of(setup.period()))
                .withIncludeOrgUnitDescendants(true)
                .build(),
            progress);

    verify(progress, never()).failedWorkItem(any(Exception.class));
    assertEquals(1, results.size());
    assertEquals(setup.aoc().getUid(), results.get(0).getAttributeOptionCombo().getUid());
  }
}
