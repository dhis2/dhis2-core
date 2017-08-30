package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob" ),
    RESOURCE_TABLE( "resourceTableJob" ),
    ANALYTICS( "analyticsJob" ),
    DATA_SYNC( "dataSyncJob" ),
    META_DATA_SYNC( "metaDataSyncJob" ),
    MESSAGE_SEND( "messageSendJob" ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob" ),
    VALIDATION_RESULTS_NOTIFICATION( "validationResultNotificationJob" ),
    CREDENTIALS_EXPIRY_ALERT( "credentialsExpiryAlertJob" ),
    MONITORING( "monitoringJob" ),
    PUSH_ANALYSIS( "pushAnalysis" );

    private final String key;

    JobType( String key )
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }
}
