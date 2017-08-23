package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.pushanalysis.PushAnalysisService;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class PushAnalysisJobConfiguration extends JobConfiguration
{
    private int pushAnalysisId;

    private TaskId taskId;

    private PushAnalysisService pushAnalysisService;

    public PushAnalysisJobConfiguration( int pushAnalysisId, TaskId taskId, PushAnalysisService pushAnalysisService )
    {
        this.pushAnalysisId = pushAnalysisId;
        this.taskId = taskId;
        this.pushAnalysisService = pushAnalysisService;
    }

    @Override
    public void run()
    {
        pushAnalysisService.runPushAnalysis( pushAnalysisId, taskId );
    }
}
