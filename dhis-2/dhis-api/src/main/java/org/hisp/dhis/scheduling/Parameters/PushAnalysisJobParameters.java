package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Henning Håkonsen
 */
public class PushAnalysisJobParameters
    implements JobParameters
{
    private int pushAnalysisId;
    private TaskId taskId;

    public PushAnalysisJobParameters()
    {}

    public PushAnalysisJobParameters( TaskId taskId, int pushAnalysisId )
    {
        this.pushAnalysisId = pushAnalysisId;
        this.taskId = taskId;
    }

    public int getPushAnalysisId()
    {
        return pushAnalysisId;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }
}
