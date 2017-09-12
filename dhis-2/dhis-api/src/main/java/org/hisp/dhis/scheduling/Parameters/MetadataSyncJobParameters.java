package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class MetadataSyncJobParameters
    implements JobParameters
{
    private TaskId taskId;

    public MetadataSyncJobParameters()
    {}

    public MetadataSyncJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public TaskId getTaskId()
    {
        return taskId;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }
}
