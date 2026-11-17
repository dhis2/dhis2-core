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

import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.FAIL;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;
import static org.hisp.dhis.test.TestBase.createCategoryOptionCombo;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;
import org.hisp.dhis.scheduling.RecordingJobProgress;
import org.junit.jupiter.api.Test;

/** Tests what a failing org unit chunk does to a validation run. */
class ValidatorTest {

  @Test
  void failingChunkAbortsRunWithFailPolicy() {
    ValidationRunContext context = createTwoChunkContext();

    assertThrows(RuntimeException.class, () -> validate(context, FAIL));
  }

  @Test
  void failingChunkIsSkippedWithSkipPolicy() {
    ValidationRunContext context = createTwoChunkContext();

    assertDoesNotThrow(() -> validate(context, SKIP_ITEM));
  }

  /** Runs with a runner that fails for the second chunk only. */
  private static void validate(ValidationRunContext context, FailurePolicy onChunkFailure) {
    OrganisationUnit last = context.getOrgUnits().get(context.getOrgUnits().size() - 1);
    DataValidationRunner runner = mock(DataValidationRunner.class);
    doThrow(new IllegalStateException("chunk failed"))
        .when(runner)
        .run(argThat(orgUnits -> orgUnits.contains(last)), any());

    Validator.validate(context, runner, RecordingJobProgress.transitory(), onChunkFailure);
  }

  private static ValidationRunContext createTwoChunkContext() {
    List<OrganisationUnit> orgUnits = new ArrayList<>();
    for (int i = 0; i <= ValidationRunContext.ORG_UNITS_PER_TASK; i++) {
      orgUnits.add(createOrganisationUnit("Unit" + i));
    }
    return ValidationRunContext.newBuilder()
        .withOrgUnits(orgUnits)
        .withPeriodTypeXs(List.of(new PeriodTypeExtended(new MonthlyPeriodType())))
        .withDefaultAttributeCombo(createCategoryOptionCombo('A', 'B'))
        .withMaxResults(500)
        .build();
  }
}
