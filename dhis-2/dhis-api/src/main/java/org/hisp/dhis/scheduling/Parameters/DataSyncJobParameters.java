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

    @Override
    public ErrorReport validate( Map<CronFieldName, CronField> cronFieldNameCronFieldMap )
    {
        return null;
    }
}
