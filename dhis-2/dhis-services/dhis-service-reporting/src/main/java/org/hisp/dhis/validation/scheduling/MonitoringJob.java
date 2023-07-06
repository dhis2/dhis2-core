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
package org.hisp.dhis.validation.scheduling;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.util.DateUtils.addDays;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.ValidationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Component
@RequiredArgsConstructor
public class MonitoringJob implements Job {
  private final ValidationService validationService;

  private final ValidationRuleService validationRuleService;

  private final PeriodService periodService;

  private final MessageService messageService;

  @Override
  public JobType getJobType() {
    return JobType.MONITORING;
  }

  @Override
  @Transactional
  public void execute(JobConfiguration config, JobProgress progress) {
    progress.startingProcess("Data validation");
    try {
      MonitoringJobParameters params = (MonitoringJobParameters) config.getJobParameters();
      List<String> groupUIDs = params.getValidationRuleGroups();
      Collection<ValidationRule> rules = getValidationRules(groupUIDs);
      progress.startingStage("Preparing analysis parameters");
      List<Period> periods =
          progress.runStage(
              List.of(),
              ps -> rules.stream().map(BaseIdentifiableObject::getName).collect(joining(", ")),
              () -> getPeriods(params, rules));

      ValidationAnalysisParams parameters =
          validationService
              .newParamsBuilder(rules, null, periods)
              .withIncludeOrgUnitDescendants(true)
              .withMaxResults(ValidationService.MAX_SCHEDULED_ALERTS)
              .withSendNotifications(params.isSendNotifications())
              .withPersistResults(params.isPersistResults())
              .build();

      validationService.validationAnalysis(parameters, progress);

      progress.completedProcess("Data validation done");
    } catch (RuntimeException ex) {
      progress.failedProcess(ex);
      messageService.sendSystemErrorNotification("Data validation failed", ex);
      throw ex;
    }
  }

  private Collection<ValidationRule> getValidationRules(List<String> groupUIDs) {
    if (groupUIDs.isEmpty()) {
      return validationRuleService.getValidationRulesWithNotificationTemplates();
    }
    return groupUIDs.stream()
        .map(validationRuleService::getValidationRuleGroup)
        .filter(Objects::nonNull)
        .map(ValidationRuleGroup::getMembers)
        .filter(Objects::nonNull)
        .reduce(Sets.newHashSet(), SetUtils::union);
  }

  private List<Period> getPeriods(
      MonitoringJobParameters params, Collection<ValidationRule> rules) {
    if (params.getRelativeStart() != 0 && params.getRelativeEnd() != 0) {
      Date startDate = addDays(new Date(), params.getRelativeStart());
      Date endDate = addDays(new Date(), params.getRelativeEnd());

      List<Period> periods = periodService.getPeriodsBetweenDates(startDate, endDate);
      return ListUtils.union(periods, periodService.getIntersectionPeriods(periods));
    }
    return rules.stream()
        .map(ValidationRule::getPeriodType)
        .distinct()
        .map(vr -> asList(vr.createPeriod(), vr.getPreviousPeriod(vr.createPeriod())))
        .reduce(Lists.newArrayList(), ListUtils::union);
  }
}
