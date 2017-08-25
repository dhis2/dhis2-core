package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.validation.scheduling.MonitoringJob;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MonitoringJobConfiguration extends JobConfiguration
{
    public MonitoringJobConfiguration( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new MonitoringJob( taskId );
    }
}
