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
    DATA_STATISTICS( "dataStatisticsJob", false, null, null ),
    DATA_INTEGRITY( "dataIntegrity", true, null, null ),
    RESOURCE_TABLE( "resourceTableJob", true, null, null ),
    ANALYTICS_TABLE( "analyticsTableJob", true, AnalyticsJobParameters.class, ImmutableMap.of(
        "skipTableTypes", "/api/analytics/tableTypes"
    ) ),
    DATA_SYNC( "dataSyncJob", true, null, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", false, null, null ),
    META_DATA_SYNC( "metaDataSyncJob", true, null, null ),
    SMS_SEND( "smsSendJob", false, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true, null, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", true, null, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", false, null, null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", true, null, null ),
    MONITORING( "monitoringJob", true, MonitoringJobParameters.class, ImmutableMap.of(
        "relativePeriods", "/api/periodTypes/relativePeriodTypes",
        "validationRuleGroups", "/api/validationRuleGroups"
    ) ),
    PUSH_ANALYSIS( "pushAnalysis", true, PushAnalysisJobParameters.class, ImmutableMap.of(
        "pushAnalysis", "/api/pushAnalysis"
    ) ),
    PREDICTOR( "predictor", true, PredictorJobParameters.class, ImmutableMap.of(
        "predictors", "/api/predictors"
    ) ),
    DATA_SET_NOTIFICATION( "dataSetNotification", false, null, null ),
    STARTUP( "startup", false, null, null ),

    // For tests
    MOCK( "mockJob", false, MockJobParameters.class, null ),

    // To satifisfy code that used the old enum TaskCategory
    DATAVALUE_IMPORT( null, false, null, null ),
    ANALYTICSTABLE_UPDATE( null, false, null, null ),
    METADATA_IMPORT( null, false, null, null ),
    DATAVALUE_IMPORT_INTERNAL( null, false, null, null ),
    EVENT_IMPORT( null, false, null, null ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, false, null, null );

    private final String key;

    private final Class<? extends JobParameters> jobParameters;

    private final boolean configurable;

    ImmutableMap<String, String> relativeApiElements;

    JobType( String key, boolean configurable, Class<? extends JobParameters> jobParameters, ImmutableMap<String, String> relativeApiElements )
    {
        this.key = key;
        this.jobParameters = jobParameters;
        this.configurable = configurable;
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

    public ImmutableMap<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
