package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.parameters.*;

import java.util.HashMap;
import java.util.Optional;

/**
 * Enum describing the different jobs in the system.
 * Each job has a name, class and possibly a map containing relative endpoints for possible parameters.
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", true, null, null ),
    DATA_INTEGRITY( "dataIntegrity", true, null, null ),
    RESOURCE_TABLE( "resourceTableJob", true, null, null ),
    ANALYTICS_TABLE( "analyticsTableJob", true, AnalyticsJobParameters.class, new HashMap<String, String>()
    {{
        put( "skipTableTypes", "/api/analytics/tableTypes" );
    }} ),
    DATA_SYNC( "dataSyncJob", true, null, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", true, null, null ),
    META_DATA_SYNC( "metaDataSyncJob", true, null, null ),
    SMS_SEND( "smsSendJob", true, SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", true, null, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob",true,  null, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", true, null, null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", true, null, null ),
    MONITORING( "monitoringJob", true, MonitoringJobParameters.class, new HashMap<String, String>()
    {{
        put( "organisationUnits", "/api/organisationUnits" );
        put( "validationRuleGroups", "/api/validationRuleGroups" );
        put( "validationRuleGroupUids", "/api/validationRuleGroups" );
        put( "parentOrgUnitUid", "/api/organisationUnits" );
    }} ),
    PUSH_ANALYSIS( "pushAnalysis", true, PushAnalysisJobParameters.class, new HashMap<String, String>()
    {{
        put( "pushAnalysisId", "/api/pushAnalysis" );
    }} ),
    PREDICTOR( "predictor", true, PredictorJobParameters.class, new HashMap<String, String>()
    {{
        put( "predictors", "/api/predictors" );
    }} ),
    DATASET_NOTIFICATION( "dataSetNotification", true, null, null ),

    // For tests
    TEST( "test", false, TestJobParameters.class, null ),

    // To satifisfy code that used the old enum TaskCategory
    DATAVALUE_IMPORT( null, false, null, null ),
    ANALYTICSTABLE_UPDATE( null, false, null, null ),
    METADATA_IMPORT( null, false, null, null ),
    DATAVALUE_IMPORT_INTERNAL( null, false, null, null ),
    EVENT_IMPORT( null, false, null, null ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, false, null, null );

    private final String key;

    private final Class<?> clazz;

    private final boolean configurable;

    HashMap<String, String> relativeApiElements;

    JobType( String key, boolean configurable, Class<?> clazz, HashMap<String, String> relativeApiElements )
    {
        this.key = key;
        this.clazz = clazz;
        this.configurable = configurable;
        this.relativeApiElements = relativeApiElements;
    }

    public String getKey()
    {
        return key;
    }

    public Class<JobParameters> getClazz()
    {
        return (Class<JobParameters>) clazz;
    }

    public static Optional<JobType> getByJobType( String jobType )
    {
        for ( JobType jobType1 : JobType.values() )
        {
            if ( jobType1.getKey().equals( jobType ) )
            {
                return Optional.of( jobType1 );
            }
        }

        return Optional.empty();
    }

    public boolean isConfigurable()
    {
        return configurable;
    }

    public static Class<JobParameters> getClazz( String jobType )
    {
        Optional<JobType> getJobType = getByJobType( jobType );

        return getJobType.get().getClazz();
    }

    public HashMap<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
