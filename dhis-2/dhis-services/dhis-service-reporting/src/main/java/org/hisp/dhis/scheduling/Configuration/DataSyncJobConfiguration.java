package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.dxf2.synch.DataSynchronizationJob;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class DataSyncJobConfiguration extends JobConfiguration
{
    public DataSyncJobConfiguration( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public Runnable getRunnable( )
    {
        return new DataSynchronizationJob( taskId );
    }
}
