package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.scheduling.TaskId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class ResourceTableJobConfiguration extends JobConfiguration
{
    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    private TaskId taskId;

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    @Override
    public void run()
    {
        analyticsTableGenerator.generateResourceTables( taskId );
    }
}
