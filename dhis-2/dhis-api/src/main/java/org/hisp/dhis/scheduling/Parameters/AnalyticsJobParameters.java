package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.Set;

/**
 * @author Henning Håkonsen
 */
public class AnalyticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 1L;

    private JobId jobId;

    @Property
    private Integer lastYears;

    @Property
    private Set<String> skipTableTypes;

    @Property
    private boolean skipResourceTables;

    public AnalyticsJobParameters()
    {
    }

    public AnalyticsJobParameters( Integer lastYears, JobId jobId, Set<String> skipTableTypes, boolean skipResourceTables, boolean continuousGeneration )
    {
        this.lastYears = lastYears;
        this.jobId = jobId;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
    }

    @Override
    public JobId getJobId()
    {
        return jobId;
    }

    @Override
    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
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
