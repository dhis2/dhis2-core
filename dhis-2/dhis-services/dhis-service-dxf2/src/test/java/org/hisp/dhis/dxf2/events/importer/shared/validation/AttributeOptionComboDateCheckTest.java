/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class AttributeOptionComboDateCheckTest extends BaseValidationTest {

  private AttributeOptionComboDateCheck rule;

  private Program program;

  private final String PROGRAM_UID = "ProgramUidA";

  @BeforeEach
  void setUp() {
    rule = new AttributeOptionComboDateCheck();
    program = new Program();
    program.setUid(PROGRAM_UID);
    program.setName("TestProgram");
    event.setProgram(PROGRAM_UID);
  }

  @Test
  void failOnCategoryOptionStartDateBeforeEventDate() {
    event.setEventDate("2019-05-01");
    event.setDueDate("2019-05-10");
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo("2020-01-01", true);
    mockContext(categoryOptionCombo);
    ImportSummary importSummary = rule.check(new ImmutableEvent(event), this.workContext);
    assertHasError(
        importSummary,
        event,
        "Event date 2019-05-01 is before start date 2020-01-01 for attributeOption 'test'");
  }

  @Test
  void failOnCategoryOptionEndDateBeforeEventDate() {
    event.setEventDate("2019-05-01");
    event.setDueDate("2019-05-10");
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo("2019-04-01", false);
    mockContext(categoryOptionCombo);
    ImportSummary importSummary = rule.check(new ImmutableEvent(event), this.workContext);
    assertHasError(
        importSummary,
        event,
        "Event date 2019-05-01 is after end date 2019-04-01 for attributeOption 'test' in program 'TestProgram'");
  }

  @Test
  void succeedBeforeOpenDaysAfterCoEndDate() {
    event.setEventDate("2019-05-01");
    event.setDueDate("2019-05-10");
    program.setOpenDaysAfterCoEndDate(31);
    CategoryOptionCombo categoryOptionCombo = createCategoryOptionCombo("2019-04-01", false);
    mockContext(categoryOptionCombo);
    ImportSummary importSummary = rule.check(new ImmutableEvent(event), this.workContext);
    assertNoError(importSummary);
  }

  private void mockContext(CategoryOptionCombo categoryOptionCombo) {
    Map<String, CategoryOptionCombo> cocMap = new HashMap<>();
    cocMap.put(event.getUid(), categoryOptionCombo);
    when(workContext.getCategoryOptionComboMap()).thenReturn(cocMap);
    Map<String, Program> programsMap = new HashMap<>();
    programsMap.put(PROGRAM_UID, program);
    when(workContext.getProgramsMap()).thenReturn(programsMap);
  }

  private CategoryOptionCombo createCategoryOptionCombo(String date, boolean startDate) {
    CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
    Set<CategoryOption> catOptions = new HashSet<>();
    CategoryOption categoryOptionA = new CategoryOption();
    categoryOptionA.setName("test");
    if (startDate) {
      categoryOptionA.setStartDate(DateUtils.parseDate(date));
    } else {
      categoryOptionA.setEndDate(DateUtils.parseDate(date));
    }
    catOptions.add(categoryOptionA);
    categoryOptionCombo.setCategoryOptions(catOptions);
    return categoryOptionCombo;
  }
}
