package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.analytics.table.scheduling.AnalyticsTableJob;
import org.hisp.dhis.scheduling.TaskId;

/**
 * Created by henninghakonsen on 25/08/2017.
 * Project: dhis-2.
 */
public class AnalyticsJobConfiguration
    extends JobConfiguration
{
    private Integer lastYears;

    public AnalyticsJobConfiguration( Integer lastYears, TaskId taskId )
    {
        this.lastYears = lastYears;
        this.taskId = taskId;
    }

    @Override
    public Runnable getRunnable()
    {
        return new AnalyticsTableJob( lastYears, taskId );
    }
}
