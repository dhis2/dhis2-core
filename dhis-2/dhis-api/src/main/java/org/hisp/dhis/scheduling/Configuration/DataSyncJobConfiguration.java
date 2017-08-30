package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class DataSyncJobConfiguration extends JobConfiguration
{
    private TaskId taskId;

    public DataSyncJobConfiguration( String name, JobType jobType, String cronExpression, TaskId taskId )
    {
        super( name, jobType, cronExpression );
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}
