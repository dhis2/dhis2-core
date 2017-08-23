package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public enum JobType
{
    TASK_RESOURCE_TABLE( "resourceTableTask" ),
    TASK_DATAMART_LAST_YEAR( "dataMartLastYearTask" ),
    TASK_ANALYTICS( "analyticsTask" ),
    TASK_MONITORING( "monitoringTask" ),
    TASK_DATA_SYNC( "dataSyncTask" ),
    TASK_META_DATA_SYNC( "metadataSyncTask" ),
    TASK_SEND_SCHEDULED_SMS_NOW( "sendScheduledMessageTaskNow" ),
    TASK_SCHEDULED_PROGRAM_NOTIFICATIONS( "scheduledProgramNotificationsTask" );

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
