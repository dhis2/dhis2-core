package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.scheduling.TaskId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class AnalyticsJobConfiguration extends JobConfiguration
{
    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    private Integer lastYears;

    private TaskId taskId;

    public void setLastYears( Integer lastYears )
    {
        this.lastYears = lastYears;
    }

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void run()
    {
        SecurityContextHolder.clearContext(); // No security context
        analyticsTableGenerator.generateTables( lastYears, taskId, new HashSet<>(), false );
    }
}
