package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class MonitoringJobParameters
    implements JobParameters
{
    private TaskId taskId;

    public MonitoringJobParameters()
    {}

    public MonitoringJobParameters( TaskId taskId )
    {
        this.taskId = taskId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}
