/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import org.hisp.dhis.scheduling.parameters.*;

import java.util.Map;

import org.hisp.dhis.scheduling.parameters.*;

import com.google.common.collect.ImmutableMap;

/**
<<<<<<< HEAD
 * Enum describing the different jobs in the system. Each job has a key, class, configurable
 * status and possibly a map containing relative endpoints for possible parameters.
=======
 * Enum describing the different jobs in the system. Each job has a key, class,
 * configurable status and possibly a map containing relative endpoints for
 * possible parameters.
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
 * <p>
<<<<<<< HEAD
 * The key must match the jobs bean name so that the {@link SchedulingManager} can fetch
 * the correct job
=======
 * The key must match the jobs bean name so that the {@link SchedulingManager}
 * can fetch the correct job
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", false ),
    DATA_INTEGRITY( "dataIntegrityJob", true ),
    RESOURCE_TABLE( "resourceTableJob", true ),
    ANALYTICS_TABLE( "analyticsTableJob", true, SchedulingType.CRON, AnalyticsJobParameters.class, ImmutableMap.of(
        "skipTableTypes", "/api/analytics/tableTypes" ) ),
<<<<<<< HEAD
    CONTINUOUS_ANALYTICS_TABLE( "continuousAnalyticsTableJob", true, SchedulingType.FIXED_DELAY, ContinuousAnalyticsJobParameters.class, ImmutableMap.of(
        "skipTableTypes", "/api/analytics/tableTypes" ) ),
    DATA_SYNC( "dataSyncJob", true, SchedulingType.CRON, DataSynchronizationJobParameters.class, null ),
    TRACKER_PROGRAMS_DATA_SYNC( "trackerProgramsDataSyncJob", true, SchedulingType.CRON, TrackerProgramsDataSynchronizationJobParameters.class, null ),
    EVENT_PROGRAMS_DATA_SYNC( "eventProgramsDataSyncJob", true, SchedulingType.CRON, EventProgramsDataSynchronizationJobParameters.class, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUpJob", false ),
    IMAGE_PROCESSING( "imageProcessingJob", false ),
    META_DATA_SYNC( "metadataSyncJob", true, SchedulingType.CRON, MetadataSyncJobParameters.class, null ),
    SMS_SEND( "sendSmsJob", false, SchedulingType.CRON, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", true ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", false ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", false ),
    MONITORING( "monitoringJob", true, SchedulingType.CRON, MonitoringJobParameters.class, ImmutableMap.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes", "validationRuleGroups", "/api/validationRuleGroups" ) ),
    PUSH_ANALYSIS( "pushAnalysisJob", true, SchedulingType.CRON, PushAnalysisJobParameters.class, ImmutableMap.of(
        "pushAnalysis", "/api/pushAnalysis" ) ),
    PREDICTOR( "predictorJob", true, SchedulingType.CRON, PredictorJobParameters.class, ImmutableMap.of(
        "predictors", "/api/predictors", "predictorGroups", "/api/predictorGroups" ) ),
    DATA_SET_NOTIFICATION( "dataSetNotificationJob", false ),
    REMOVE_EXPIRED_RESERVED_VALUES( "removeExpiredReservedValuesJob", false ),
=======
    CONTINUOUS_ANALYTICS_TABLE( "continuousAnalyticsTableJob", true, SchedulingType.FIXED_DELAY,
        ContinuousAnalyticsJobParameters.class, ImmutableMap.of(
            "skipTableTypes", "/api/analytics/tableTypes" ) ),
    DATA_SYNC( "dataSyncJob", true, SchedulingType.CRON, DataSynchronizationJobParameters.class, null ),
    TRACKER_PROGRAMS_DATA_SYNC( "trackerProgramsDataSyncJob", true, SchedulingType.CRON,
        TrackerProgramsDataSynchronizationJobParameters.class, null ),
    EVENT_PROGRAMS_DATA_SYNC( "eventProgramsDataSyncJob", true, SchedulingType.CRON,
        EventProgramsDataSynchronizationJobParameters.class, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUpJob", false ),
    IMAGE_PROCESSING( "imageProcessingJob", false ),
    META_DATA_SYNC( "metadataSyncJob", true, SchedulingType.CRON, MetadataSyncJobParameters.class, null ),
    SMS_SEND( "sendSmsJob", false, SchedulingType.CRON, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", true ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", false ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", false ),
    MONITORING( "monitoringJob", true, SchedulingType.CRON, MonitoringJobParameters.class, ImmutableMap.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes", "validationRuleGroups",
        "/api/validationRuleGroups" ) ),
    PUSH_ANALYSIS( "pushAnalysisJob", true, SchedulingType.CRON, PushAnalysisJobParameters.class, ImmutableMap.of(
        "pushAnalysis", "/api/pushAnalysis" ) ),
    PREDICTOR( "predictorJob", true, SchedulingType.CRON, PredictorJobParameters.class, ImmutableMap.of(
        "predictors", "/api/predictors", "predictorGroups", "/api/predictorGroups" ) ),
    DATA_SET_NOTIFICATION( "dataSetNotificationJob", false ),
    REMOVE_USED_OR_EXPIRED_RESERVED_VALUES( "removeUsedOrExpiredReservedValuesJob", false ),
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    TRACKER_IMPORT_JOB( "trackerImportJob", false ),
    TRACKER_IMPORT_NOTIFICATION_JOB( "trackerImportNotificationJob", false ),
    TRACKER_IMPORT_RULE_ENGINE_JOB( "trackerImportRuleEngineJob", false ),

    // Internal jobs
    LEADER_ELECTION( "leaderElectionJob", false ),
    LEADER_RENEWAL( "leaderRenewalJob", false ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, false ),
    DATAVALUE_IMPORT_INTERNAL( null, false ),
    METADATA_IMPORT( null, false ),
    DATAVALUE_IMPORT( null, false ),
    EVENT_IMPORT( null, false ),
    ENROLLMENT_IMPORT( null, false ),
    TEI_IMPORT( null, false ),

    // Testing purposes
    MOCK( "mockJob", false, SchedulingType.CRON, MockJobParameters.class, null ),

<<<<<<< HEAD
    // Deprecated, present to satisfy code using the old enumeration TaskCategory
    @Deprecated GML_IMPORT( null, false ),
    @Deprecated ANALYTICSTABLE_UPDATE( null, false ),
    @Deprecated PROGRAM_DATA_SYNC( null, false );
=======
    // Deprecated, present to satisfy code using the old enumeration
    // TaskCategory
    @Deprecated
    GML_IMPORT( null, false ),
    @Deprecated
    ANALYTICSTABLE_UPDATE( null, false ),
    @Deprecated
    PROGRAM_DATA_SYNC( null, false );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

    private final String key;

    private final boolean configurable;

    private final SchedulingType schedulingType;

    private final Class<? extends JobParameters> jobParameters;

    private final Map<String, String> relativeApiElements;

    JobType( String key, boolean configurable )
    {
        this( key, configurable, SchedulingType.CRON, null, null );
    }

<<<<<<< HEAD
    JobType( String key, boolean configurable, SchedulingType schedulingType, Class<? extends JobParameters> jobParameters,
=======
    JobType( String key, boolean configurable, SchedulingType schedulingType,
        Class<? extends JobParameters> jobParameters,
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        Map<String, String> relativeApiElements )
    {
        this.key = key;
        this.configurable = configurable;
        this.schedulingType = schedulingType;
        this.jobParameters = jobParameters;
        this.relativeApiElements = relativeApiElements;
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

    public String getKey()
    {
        return key;
    }

    public Class<? extends JobParameters> getJobParameters()
    {
        return jobParameters;
    }

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
