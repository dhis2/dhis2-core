package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class DataSyncJobParameters
    implements JobParameters
{
    private TaskId taskId;

    public DataSyncJobParameters()
    {}

    public DataSyncJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}
