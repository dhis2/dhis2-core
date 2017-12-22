package org.hisp.dhis.scheduling;

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.scheduling.parameters.*;

/**
 * Enum describing the different jobs in the system.
 * Each job has a name, class and possibly a map containing relative endpoints for possible parameters.
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", false, true, null, null ),
    DATA_INTEGRITY( "dataIntegrity", true, true, null, null ),
    RESOURCE_TABLE( "resourceTableJob", true, true, null, null ),
    ANALYTICS_TABLE( "analyticsTableJob", true, true, AnalyticsJobParameters.class, ImmutableMap.of(
        "skipTableTypes", "/api/analytics/tableTypes"
    ) ),
    DATA_SYNC( "dataSyncJob", true, true, null, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", false, true, null, null ),
    META_DATA_SYNC( "metaDataSyncJob", true, true, null, null ),
    SMS_SEND( "smsSendJob", false, false, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true, false, null, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", true, true, null, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", false, true, null, null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", true, false, null, null ),
    MONITORING( "monitoringJob", true, true, MonitoringJobParameters.class, ImmutableMap.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes",
        "validationRuleGroups", "/api/validationRuleGroups"
    ) ),
    PUSH_ANALYSIS( "pushAnalysis", true, true, PushAnalysisJobParameters.class, ImmutableMap.of(
        "pushAnalysis", "/api/pushAnalysis"
    ) ),
    PREDICTOR( "predictor", true, true, PredictorJobParameters.class, ImmutableMap.of(
        "predictors", "/api/predictors"
    ) ),
    DATA_SET_NOTIFICATION( "dataSetNotification", false, true, null, null ),
    STARTUP( "startup", false, false, null, null ),

    // For tests
    MOCK( "mockJob", false, false, MockJobParameters.class, null ),

    // To satifisfy code that used the old enum TaskCategory
    DATAVALUE_IMPORT( null, false, true, null, null ),
    ANALYTICSTABLE_UPDATE( null, true, false, null, null ),
    METADATA_IMPORT( null, false, true, null, null ),
    DATAVALUE_IMPORT_INTERNAL( null, false, true, null, null ),
    EVENT_IMPORT( null, false, true, null, null ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, false, true, null, null );

    private final String key;

    private final Class<? extends JobParameters> jobParameters;

    private final boolean configurable;

    private final boolean executable;

    ImmutableMap<String, String> relativeApiElements;

    JobType( String key, boolean configurable, boolean executable, Class<? extends JobParameters> jobParameters, ImmutableMap<String, String> relativeApiElements )
    {
        this.key = key;
        this.jobParameters = jobParameters;
        this.configurable = configurable;
        this.executable = executable;
        this.relativeApiElements = relativeApiElements;
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

    public boolean isExecutable()
    {
        return executable;
    }

    public ImmutableMap<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
