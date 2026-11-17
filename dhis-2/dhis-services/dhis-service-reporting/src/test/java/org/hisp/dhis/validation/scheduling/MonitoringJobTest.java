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
package org.hisp.dhis.validation.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.JobEntry;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonitoringJobTest {
  private static final String GROUP_UID = "groupAAAAAA";

  @Mock private ValidationService validationService;

  @Mock private ValidationRuleService validationRuleService;

  @Mock private PeriodService periodService;

  @Mock private MessageService messageService;

  private MonitoringJob job;

  @BeforeEach
  void setUp() {
    job =
        new MonitoringJob(validationService, validationRuleService, periodService, messageService);

    ValidationRule rule = new ValidationRule();
    rule.setPeriodType(new MonthlyPeriodType());
    ValidationRuleGroup group = new ValidationRuleGroup();
    group.setMembers(Set.of(rule));
    when(validationRuleService.getValidationRuleGroup(GROUP_UID)).thenReturn(group);
    when(validationService.newParamsBuilder(anyCollection(), isNull(), anyCollection()))
        .thenAnswer(
            inv ->
                new ValidationAnalysisParams.Builder(inv.getArgument(0), null, inv.getArgument(2)));
  }

  private ValidationAnalysisParams run(boolean persist, boolean replace) {
    MonitoringJobParameters params =
        new MonitoringJobParameters(0, 0, List.of(GROUP_UID), false, persist);
    params.setReplaceResults(replace);
    job.execute(
        new JobEntry(UID.of("jobAAAAAAAA"), JobType.MONITORING, params), JobProgress.noop());

    ArgumentCaptor<ValidationAnalysisParams> captor =
        ArgumentCaptor.forClass(ValidationAnalysisParams.class);
    verify(validationService).validationAnalysis(captor.capture(), any());
    return captor.getValue();
  }

  @Test
  void testReplaceRunHasNoResultLimit() {
    ValidationAnalysisParams params = run(false, true);
    assertTrue(params.isReplacePersistedResults());
    assertEquals(Integer.MAX_VALUE, params.getMaxResults());
  }

  @Test
  void testPersistRunKeepsScheduledResultLimit() {
    ValidationAnalysisParams params = run(true, false);
    assertFalse(params.isReplacePersistedResults());
    assertEquals(ValidationService.MAX_SCHEDULED_ALERTS, params.getMaxResults());
  }

  @Test
  void testReplaceImpliesPersist() {
    ValidationAnalysisParams params = run(false, true);
    assertTrue(params.isPersistResults());
    assertTrue(params.isReplacePersistedResults());
  }
}
