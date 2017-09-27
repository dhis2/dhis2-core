package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Parameters.*;

import java.util.HashMap;
import java.util.Optional;

/**
 * Enum describing the different jobs in the system.
 * Each job has a name, class and an identifier describing the minimum interval time between executions.
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", DataStatisticsJobParameters.class, 1000, null ),
    DATA_INTEGRITY( "dataIntegrity", DataIntegrityJobParameters.class, 1000, null ),
    RESOURCE_TABLE( "resourceTableJob", ResourceTableJobParameters.class, 1000, null ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class, 1800, new HashMap<String, String>()
    {{
        put("skipTableTypes", "/api/analytics/tableTypes");
    }}),
    DATA_SYNC( "dataSyncJob", DataSyncJobParameters.class, 1000, null ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", FileResourceCleanUpJobParameters.class, 1000, null ),
    META_DATA_SYNC( "metaDataSyncJob", MetadataSyncJobParameters.class, 1000, null ),
    SMS_SEND( "smsSendJob", SmsJobParameters.class, 1000, null ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", SendScheduledMessageJobParameters.class, 1000, null ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", ProgramNotificationJobParameters.class, 1000, null ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", ValidationResultNotificationJobParameters.class, 1000,
        null ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null, 1000, null ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class, 1000, null ),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class, 1000, null ),
    TEST( "test", TestJobParameters.class, 1800, null );

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
