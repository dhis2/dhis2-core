package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.analytics.table.scheduling.ResourceTableJob;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class ResourceTableJobConfiguration extends JobConfiguration
{
    public ResourceTableJobConfiguration( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new ResourceTableJob( taskId );
    }
}

