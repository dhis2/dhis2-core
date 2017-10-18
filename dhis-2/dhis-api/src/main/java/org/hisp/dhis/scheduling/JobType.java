package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.parameters.*;

import java.util.HashMap;
import java.util.Optional;

/**
 * Enum describing the different jobs in the system.
 * Each job has a name, class and an identifier describing the minimum interval time between executions.
 *
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", null, 1, null ),
    DATA_INTEGRITY( "dataIntegrity", null, 1, null ),
    RESOURCE_TABLE( "resourceTableJob", null, 1, null ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class, 1, new HashMap<String, String>()
    {{
        put("skipTableTypes", "/api/analytics/tableTypes");
    }}),
    DATA_SYNC( "dataSyncJob", null, 1, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", null, 1, null ),
    META_DATA_SYNC( "metaDataSyncJob", null, 1, null ),
    SMS_SEND( "smsSendJob", SmsJobParameters.class, 1, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", null, 1, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", null, 1, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", null, 1, null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null, 1, null ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class, 1, new HashMap<String, String>()
    {{
        put("organisationUnits", "/api/organisationUnits");
        put("validationRuleGroups", "/api/validationRuleGroups");
    }}),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class, 1, new HashMap<String, String>()
    {{
        put("pushAnalysisId", "/api/pushAnalysis");
    }}),
    TEST( "test", TestJobParameters.class, 1, null );

    private final String key;

    private final Class<?> clazz;

    private final long minimumFrequencyInSeconds;

    HashMap<String, String> relativeApiElements;

    JobType( String key, Class<?> clazz, long minimumFrequencyInSeconds,
        HashMap<String, String> relativeApiElements )
    {
        this.key = key;
        this.clazz = clazz;
        this.minimumFrequencyInSeconds = minimumFrequencyInSeconds;
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

    public long getMinimumFrequencyInSeconds()
    {
        return minimumFrequencyInSeconds;
    }

    public HashMap<String, String> getRelativeApiElements()
    {
        return relativeApiElements;
    }
}
