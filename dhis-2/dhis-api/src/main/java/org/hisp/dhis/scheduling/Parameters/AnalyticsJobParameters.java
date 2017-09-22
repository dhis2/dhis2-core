package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

import java.util.Set;

/**
 * @author Henning Håkonsen
 */
public class AnalyticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 1L;

    private JobId jobId;
    private Integer lastYears;
    private Set<String> skipTableTypes;
    private boolean skipResourceTables;

    public AnalyticsJobParameters()
    {}

    public AnalyticsJobParameters( Integer lastYears, JobId jobId, Set<String> skipTableTypes, boolean skipResourceTables )
    {
        this.lastYears = lastYears;
        this.jobId = jobId;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
    }

    public JobId getJobId()
    {
        return jobId;
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
