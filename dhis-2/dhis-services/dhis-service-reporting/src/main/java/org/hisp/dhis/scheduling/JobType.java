package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public enum JobType
{
    RESOURCE_TABLE( "resourceTableJob" ),
    ANALYTICS( "analyticsJob" ),
    MONITORING( "monitoringJob" ),
    DATA_SYNC( "dataSyncJob" ),
    META_DATA_SYNC( "metadataSyncJob" ),
    MESSAGE_SEND( "messageSendJob" ),
    PROGRAM_NOTIFICATIONS( "programNotificationsJob" ),
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
