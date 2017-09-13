package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class MetadataSyncJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4L;

    private TaskId taskId;

    public MetadataSyncJobParameters()
    {}

    public MetadataSyncJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }
}
