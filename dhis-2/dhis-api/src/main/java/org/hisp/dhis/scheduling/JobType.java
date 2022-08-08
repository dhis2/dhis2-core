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
package org.hisp.dhis.scheduling;

import java.util.Map;

import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.CredentialsExpiryAlertJobParameters;
import org.hisp.dhis.scheduling.parameters.DataIntegrityJobParameters;
import org.hisp.dhis.scheduling.parameters.DataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.scheduling.parameters.EventProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.scheduling.parameters.MockJobParameters;
import org.hisp.dhis.scheduling.parameters.MonitoringJobParameters;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.hisp.dhis.scheduling.parameters.PushAnalysisJobParameters;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerProgramsDataSynchronizationJobParameters;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;

/**
 * Enum describing the different jobs in the system. Each job has a key, class,
 * configurable status and possibly a map containing relative endpoints for
 * possible parameters.
 * <p>
 * The key must match the jobs bean name so that the {@link SchedulingManager}
 * can fetch the correct job
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( false ),
    DATA_INTEGRITY( true, SchedulingType.CRON, DataIntegrityJobParameters.class,
        Map.of( "checks", "/api/dataIntegrity" ) ),
    RESOURCE_TABLE( true ),
    ANALYTICS_TABLE( true, SchedulingType.CRON, AnalyticsJobParameters.class, Map.of(
        "skipTableTypes", "/api/analytics/tableTypes",
        "skipPrograms", "/api/programs" ) ),
    CONTINUOUS_ANALYTICS_TABLE( true, SchedulingType.FIXED_DELAY,
        ContinuousAnalyticsJobParameters.class, Map.of(
            "skipTableTypes", "/api/analytics/tableTypes" ) ),
    DATA_SYNC( true, SchedulingType.CRON, DataSynchronizationJobParameters.class, null ),
    TRACKER_PROGRAMS_DATA_SYNC( true, SchedulingType.CRON,
        TrackerProgramsDataSynchronizationJobParameters.class, null ),
    EVENT_PROGRAMS_DATA_SYNC( true, SchedulingType.CRON,
        EventProgramsDataSynchronizationJobParameters.class, null ),
    FILE_RESOURCE_CLEANUP( false ),
    IMAGE_PROCESSING( false ),
    META_DATA_SYNC( true, SchedulingType.CRON, MetadataSyncJobParameters.class, null ),
    AGGREGATE_DATA_EXCHANGE( true, SchedulingType.CRON, AggregateDataExchangeJobParameters.class,
        Map.of( "dataExchangeId", "/api/aggregateDataExchanges" ) ),
    SMS_SEND( false, SchedulingType.CRON, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( true ),
    PROGRAM_NOTIFICATIONS( true ),
    VALIDATION_RESULTS_NOTIFICATION( false ),
    CREDENTIALS_EXPIRY_ALERT( false, SchedulingType.CRON, CredentialsExpiryAlertJobParameters.class, null ),
    MONITORING( true, SchedulingType.CRON, MonitoringJobParameters.class, Map.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes",
        "validationRuleGroups", "/api/validationRuleGroups" ) ),
    PUSH_ANALYSIS( true, SchedulingType.CRON, PushAnalysisJobParameters.class, Map.of(
        "pushAnalysis", "/api/pushAnalysis" ) ),
    // TODO: Update API in tracker search optimization job map to reflect actual
    // api url after implementation
    TRACKER_SEARCH_OPTIMIZATION( true, SchedulingType.CRON, TrackerTrigramIndexJobParameters.class, Map.of(
        "attributes", "/api/trackedEntityAttributes/indexable" ) ),
    PREDICTOR( true, SchedulingType.CRON, PredictorJobParameters.class, Map.of(
        "predictors", "/api/predictors",
        "predictorGroups", "/api/predictorGroups" ) ),
    DATA_SET_NOTIFICATION( false ),
    REMOVE_USED_OR_EXPIRED_RESERVED_VALUES( false ),
    TRACKER_IMPORT_JOB( false ),
    TRACKER_IMPORT_NOTIFICATION_JOB( false ),
    TRACKER_IMPORT_RULE_ENGINE_JOB( false ),

    // Internal jobs
    LEADER_ELECTION( false ),
    LEADER_RENEWAL( false ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( false ),
    DATAVALUE_IMPORT_INTERNAL( false ),
    METADATA_IMPORT( false ),
    DATAVALUE_IMPORT( false ),
    GEOJSON_IMPORT( false ),
    EVENT_IMPORT( false ),
    ENROLLMENT_IMPORT( false ),
    TEI_IMPORT( false ),
    DISABLE_INACTIVE_USERS( true, SchedulingType.CRON,
        DisableInactiveUsersJobParameters.class, null ),
    ACCOUNT_EXPIRY_ALERT( false ),
    SYSTEM_VERSION_UPDATE_CHECK( false ),

    // Testing purposes
    MOCK( false, SchedulingType.CRON, MockJobParameters.class, null ),

    // Deprecated, present to satisfy code using the old enumeration
    // TaskCategory
    @Deprecated
    GML_IMPORT( false ),
    @Deprecated
    ANALYTICSTABLE_UPDATE( false ),
    @Deprecated
    PROGRAM_DATA_SYNC( false );

    private final boolean configurable;

    private final SchedulingType schedulingType;

    private final Class<? extends JobParameters> jobParameters;

    private final Map<String, String> relativeApiElements;

    JobType( boolean configurable )
    {
        this( configurable, SchedulingType.CRON, null, null );
    }

    JobType( boolean configurable, SchedulingType schedulingType,
        Class<? extends JobParameters> jobParameters,
        Map<String, String> relativeApiElements )
    {
        this.configurable = configurable;
        this.schedulingType = schedulingType;
        this.jobParameters = jobParameters;
        this.relativeApiElements = relativeApiElements;
    }

    public boolean isUsingNotifications()
    {
        return this == RESOURCE_TABLE
            || this == SEND_SCHEDULED_MESSAGE
            || this == ANALYTICS_TABLE
            || this == CONTINUOUS_ANALYTICS_TABLE
            || this == DATA_SET_NOTIFICATION
            || this == MONITORING
            || this == VALIDATION_RESULTS_NOTIFICATION
            || this == SYSTEM_VERSION_UPDATE_CHECK
            || this == EVENT_PROGRAMS_DATA_SYNC
            || this == TRACKER_PROGRAMS_DATA_SYNC
            || this == DATA_SYNC
            || this == SMS_SEND
            || this == PUSH_ANALYSIS
            || this == PREDICTOR;
    }

    public boolean isUsingErrorNotification()
    {
        return this == ANALYTICS_TABLE
            || this == VALIDATION_RESULTS_NOTIFICATION
            || this == DATA_SET_NOTIFICATION
            || this == SYSTEM_VERSION_UPDATE_CHECK
            || this == EVENT_PROGRAMS_DATA_SYNC
            || this == TRACKER_PROGRAMS_DATA_SYNC
            || this == PROGRAM_NOTIFICATIONS;
    }

    public boolean isCronSchedulingType()
    {
        return getSchedulingType() == SchedulingType.CRON;
    }

    public boolean isFixedDelaySchedulingType()
    {
        return getSchedulingType() == SchedulingType.FIXED_DELAY;
    }

    public boolean hasJobParameters()
    {
        return jobParameters != null;
    }

    public Class<? extends JobParameters> getJobParameters()
    {
        return jobParameters;
    }

    /**
     * @return Can a user create jobs of this type?
     */
    public boolean isConfigurable()
    {
        return configurable;
    }

    public SchedulingType getSchedulingType()
    {
        return schedulingType;
    }

    public Map<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
