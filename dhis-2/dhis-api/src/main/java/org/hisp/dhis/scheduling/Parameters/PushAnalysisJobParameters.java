package org.hisp.dhis.scheduling.Parameters;

import com.cronutils.model.field.CronField;
import com.cronutils.model.field.CronFieldName;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

import java.util.Map;

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

    @Override
    public ErrorReport validate( Map<CronFieldName, CronField> cronFieldNameCronFieldMap )
    {
        return null;
    }
}
