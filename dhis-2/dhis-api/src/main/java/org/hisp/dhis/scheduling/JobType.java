package org.hisp.dhis.scheduling;

/**
 * @author Henning Håkonsen
 */
public enum JobType
{
    DATA_STATISTICS( "dataStatisticsJob" ),
    RESOURCE_TABLE( "resourceTableJob" ),
    ANALYTICS_TABLE( "analyticsTableJob" ),
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
