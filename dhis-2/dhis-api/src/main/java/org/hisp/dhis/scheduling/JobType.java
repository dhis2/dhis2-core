package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Parameters.*;

import java.util.Optional;

/**
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob", DataStatisticsJobParameters.class ),
    RESOURCE_TABLE( "resourceTableJob", ResourceTableJobParameters.class ),
    ANALYTICS_TABLE( "analyticsTableJob", AnalyticsJobParameters.class ),
    DATA_SYNC( "dataSyncJob", DataSyncJobParameters.class ),
    META_DATA_SYNC( "metaDataSyncJob", MetadataSyncJobParameters.class ),
    MESSAGE_SEND( "messageSendJob", MessageSendJobParameters.class ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob", ProgramNotificationJobParameters.class ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob", ValidationResultNotificationJobParameters.class ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob", null ),
    MONITORING( "monitoringJob", MonitoringJobParameters.class ),
    PUSH_ANALYSIS( "pushAnalysis", PushAnalysisJobParameters.class ),
    TEST( "test", TestJobParameters.class );

    private final String key;

    private final Class<?> clazz;

    JobType( String key, Class<?> clazz )
    {
        this.key = key;
        this.clazz = clazz;
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
}
