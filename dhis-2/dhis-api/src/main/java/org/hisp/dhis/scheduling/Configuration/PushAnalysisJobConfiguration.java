package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class PushAnalysisJobConfiguration extends JobConfiguration
{
    private int pushAnalysisId;
    private TaskId taskId;

    public PushAnalysisJobConfiguration( String name, JobType jobType, String cronExpression, TaskId taskId, int pushAnalysisId )
    {
        super( name, jobType, cronExpression );
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
