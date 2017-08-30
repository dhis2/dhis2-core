package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.TaskId;

import java.util.Set;

/**
 * Created by henninghakonsen on 25/08/2017.
 * Project: dhis-2.
 */
public class AnalyticsJobConfiguration
    extends JobConfiguration
{
    private TaskId taskId;
    private Integer lastYears;
    private Set<String> skipTableTypes;
    private boolean skipResourceTables;

    public AnalyticsJobConfiguration( String name, JobType jobType, String cronExpression, Integer lastYears, TaskId taskId, Set<String> skipTableTypes, boolean skipResourceTables )
    {
        super(name, jobType, cronExpression);
        this.lastYears = lastYears;
        this.taskId = taskId;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public Integer getLastYears()
    {
        return lastYears;
    }

    public Set<String> getSkipTableTypes()
    {
        return skipTableTypes;
    }

    public boolean isSkipResourceTables()
    {
        return skipResourceTables;
    }
}
