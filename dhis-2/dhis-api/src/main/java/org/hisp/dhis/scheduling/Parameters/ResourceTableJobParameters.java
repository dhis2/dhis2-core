package org.hisp.dhis.scheduling.Parameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class ResourceTableJobParameters
    implements JobParameters
{
    private TaskId taskId;

    public ResourceTableJobParameters()
    {}

    public ResourceTableJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}

