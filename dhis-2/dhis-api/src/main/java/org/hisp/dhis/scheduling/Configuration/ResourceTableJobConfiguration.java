package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class ResourceTableJobConfiguration
    extends JobConfiguration
{
    private TaskId taskId;

    public ResourceTableJobConfiguration( String name, JobType jobType, String cronExpression, TaskId taskId )
    {
        super( name, jobType, cronExpression );
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}

