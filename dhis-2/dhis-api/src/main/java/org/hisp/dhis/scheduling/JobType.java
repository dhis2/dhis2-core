package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Parameters.*;

import java.util.Optional;

/**
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", DataStatisticsJobParameters.class, 1000 ),
    RESOURCE_TABLE( "resourceTableJob", ResourceTableJobParameters.class, 1000 ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class, 1800 ),
    DATA_SYNC( "dataSyncJob", DataSyncJobParameters.class, 1000 ),
    META_DATA_SYNC( "metaDataSyncJob", MetadataSyncJobParameters.class, 1000 ),
    MESSAGE_SEND( "messageSendJob", MessageSendJobParameters.class, 1000 ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", ProgramNotificationJobParameters.class, 1000 ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", ValidationResultNotificationJobParameters.class, 1000 ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null, 1000 ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class, 1000 ),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class, 1000 ),
    TEST( "test", TestJobParameters.class, 1 );

    private final String key;

    private final Class<?> clazz;

    private final long allowedFrequencyInSeconds;

    JobType( String key, Class<?> clazz, long allowedFrequencyInSeconds)
    {
        this.key = key;
        this.clazz = clazz;
        this.allowedFrequencyInSeconds = allowedFrequencyInSeconds;
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
        for ( JobType sJobType : JobType.values() )
        {
            if ( sJobType.getKey().equals( jobType ) )
            {
                return Optional.of( sJobType );
            }
        }

        return Optional.empty();
    }

    public static Class<JobParameters> getClazz( String jobType )
    {
        Optional<JobType> getJobType = getByJobType( jobType );

        return getJobType.get().getClazz();
    }

    public long getAllowedFrequencyInSeconds()
    {
        return allowedFrequencyInSeconds;
    }
}
