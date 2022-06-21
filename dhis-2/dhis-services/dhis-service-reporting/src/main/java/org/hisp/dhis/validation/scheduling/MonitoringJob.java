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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.validation.ValidationAnalysisParams;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleGroup;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.validation.ValidationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Component( "monitoringJob" )
public class MonitoringJob implements Job
{
    private final ValidationService validationService;

    private final ValidationRuleService validationRuleService;

    private final PeriodService periodService;

    private final Notifier notifier;

    private final MessageService messageService;

    public MonitoringJob( ValidationService validationService, ValidationRuleService validationRuleService,
        PeriodService periodService, Notifier notifier, MessageService messageService )
    {
        checkNotNull( validationRuleService );
        checkNotNull( validationService );
        checkNotNull( periodService );
        checkNotNull( notifier );
        checkNotNull( messageService );

        this.validationService = validationService;
        this.validationRuleService = validationRuleService;
        this.periodService = periodService;
        this.notifier = notifier;
        this.messageService = messageService;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.MONITORING;
    }

    @Override
    @Transactional
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        notifier.clear( jobConfiguration ).notify( jobConfiguration, "Monitoring data" );

        MonitoringJobParameters monitoringJobParameters = (MonitoringJobParameters) jobConfiguration.getJobParameters();

        // TODO improve collection usage

        try
        {
            List<Period> periods;
            Collection<ValidationRule> validationRules;
            List<String> groupUIDs = monitoringJobParameters.getValidationRuleGroups();

            if ( groupUIDs.isEmpty() )
            {
                validationRules = validationRuleService
                    .getValidationRulesWithNotificationTemplates();
            }
            else
            {
                validationRules = groupUIDs.stream()
                    .map( validationRuleService::getValidationRuleGroup )
                    .filter( Objects::nonNull )
                    .map( ValidationRuleGroup::getMembers )
                    .filter( Objects::nonNull )
                    .reduce( Sets.newHashSet(), SetUtils::union );
            }

            if ( monitoringJobParameters.getRelativeStart() != 0 && monitoringJobParameters.getRelativeEnd() != 0 )
            {
                Date startDate = DateUtils.addDays( new Date(),
                    monitoringJobParameters.getRelativeStart() );
                Date endDate = DateUtils.addDays( new Date(), monitoringJobParameters.getRelativeEnd() );

                periods = periodService.getPeriodsBetweenDates( startDate, endDate );

                periods = ListUtils.union( periods, periodService.getIntersectionPeriods( periods ) );
            }
            else
            {
                periods = validationRules.stream()
                    .map( ValidationRule::getPeriodType )
                    .distinct()
                    .map( ( vr ) -> Arrays.asList( vr.createPeriod(), vr.getPreviousPeriod( vr.createPeriod() ) ) )
                    .reduce( Lists.newArrayList(), ListUtils::union );
            }

            ValidationAnalysisParams parameters = validationService
                .newParamsBuilder( validationRules, null, periods )
                .withIncludeOrgUnitDescendants( true )
                .withMaxResults( ValidationService.MAX_SCHEDULED_ALERTS )
                .withSendNotifications( monitoringJobParameters.isSendNotifications() )
                .withPersistResults( monitoringJobParameters.isPersistResults() )
                .build();

            validationService.validationAnalysis( parameters );

            notifier.notify( jobConfiguration, INFO, "Monitoring process done", true );
        }
        catch ( RuntimeException ex )
        {
            notifier.notify( jobConfiguration, ERROR, "Process failed: " + ex.getMessage(), true );

            messageService.sendSystemErrorNotification( "Monitoring process failed", ex );

            throw ex;
        }
    }

}
