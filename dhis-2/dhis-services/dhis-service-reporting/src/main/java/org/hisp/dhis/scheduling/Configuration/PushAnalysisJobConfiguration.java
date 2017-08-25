package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.pushanalysis.scheduling.PushAnalysisJob;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class PushAnalysisJobConfiguration extends JobConfiguration
{
    private int pushAnalysisId;

    public PushAnalysisJobConfiguration( TaskId taskId, int pushAnalysisId )
    {
        this.taskId = taskId;
        this.pushAnalysisId = pushAnalysisId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new PushAnalysisJob( taskId, pushAnalysisId );
    }
}
