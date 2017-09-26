package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Parameters.*;

import java.util.Optional;

/**
 * Enum describing the different jobs in the system.
 * Each job has a name, class and an identifier describing the minimum interval time between executions.
 *
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", DataStatisticsJobParameters.class, 1000 ),
    DATA_INTEGRITY( "dataIntegrity", DataIntegrityJobParameters.class, 1000 ),
    RESOURCE_TABLE( "resourceTableJob", ResourceTableJobParameters.class, 1000 ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class, 1800 ),
    DATA_SYNC( "dataSyncJob", DataSyncJobParameters.class, 1000 ),
    FILE_RESOURCE_CLEANUP( "fileResourceCleanUp", FileResourceCleanUpJobParameters.class, 1000 ),
    META_DATA_SYNC( "metaDataSyncJob", MetadataSyncJobParameters.class, 1000 ),
    SMS_SEND( "smsSendJob", SmsJobParameters.class, 1000 ),
    SEND_SCHEDULED_MESSAGE( "sendScheduledMessageJob", SendScheduledMessageJobParameters.class, 1000 ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", ProgramNotificationJobParameters.class, 1000 ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", ValidationResultNotificationJobParameters.class, 1000 ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null, 1000 ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class, 1000 ),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class, 1000 ),
    TEST( "test", TestJobParameters.class, 1800 );

    private final String key;

    private final Class<?> clazz;

    private final long minimumFrequencyInSeconds;

    JobType( String key, Class<?> clazz, long minimumFrequencyInSeconds )
    {
        this.key = key;
        this.clazz = clazz;
        this.minimumFrequencyInSeconds = minimumFrequencyInSeconds;
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
}
