package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class MessageSendJobParameters
    implements JobParameters
{
    private TaskId taskId;

    public MessageSendJobParameters( )
    {}

    public MessageSendJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}
