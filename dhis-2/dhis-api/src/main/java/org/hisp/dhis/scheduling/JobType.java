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
    DATA_STATISTICS( "dataStatisticsJob", null, null ),
    DATA_INTEGRITY( "dataIntegrity", null, null ),
    RESOURCE_TABLE( "resourceTableJob", null, null ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class, new HashMap<String, String>()
    {{
        put( "skipTableTypes", "/api/analytics/tableTypes" );
    }} ),
    DATA_SYNC( "dataSyncJob", null, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", null, null ),
    META_DATA_SYNC( "metaDataSyncJob", null, null ),
    SMS_SEND( "smsSendJob", SmsJobParameters.class, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", null, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", null, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", null, null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null, null ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class, new HashMap<String, String>()
    {{
        put( "organisationUnits", "/api/organisationUnits" );
        put( "validationRuleGroups", "/api/validationRuleGroups" );
    }} ),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class, new HashMap<String, String>()
    {{
        put( "pushAnalysisId", "/api/pushAnalysis" );
    }} ),
    DATA_VALIDATION( "dataValidation", DataValidationJobParameters.class, new HashMap<String, String>()
    {{
        put( "validationRuleGroupUids", "/api/validationRuleGroups" );
        put( "parentOrgUnitUid", "/api/organisationUnits" );
    }} ),
    PREDICTOR( "predictor", PredictorJobParameters.class, new HashMap<String, String>()
    {{
        put( "predictor", "/api/predictors" );
    }} ),
    TEST( "test", TestJobParameters.class, null ),

    // To satifisfy code that used the old enum TaskCategory
    DATAVALUE_IMPORT( null, null, null ),
    ANALYTICSTABLE_UPDATE( null, null, null ),
    METADATA_IMPORT( null, null, null ),
    DATAVALUE_IMPORT_INTERNAL( null, null, null ),
    EVENT_IMPORT( null, null, null ),
    COMPLETE_DATA_SET_REGISTRATION_IMPORT( null, null, null );

    private final String key;

    private final Class<?> clazz;

    HashMap<String, String> relativeApiElements;

    JobType( String key, Class<?> clazz, HashMap<String, String> relativeApiElements )
    {
        this.key = key;
        this.clazz = clazz;
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
