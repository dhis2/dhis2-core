package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Henning Håkonsen
 */
public class AnalyticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4613054056442242637L;

    @Property
    private Integer lastYears = null;

    @Property
    private Set<String> skipTableTypes = new HashSet<>( );

    @Property
    private boolean skipResourceTables = false;

    public AnalyticsJobParameters()
    {
    }

    public AnalyticsJobParameters( Integer lastYears, Set<String> skipTableTypes, boolean skipResourceTables )
    {
        this.lastYears = lastYears;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
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

    public ErrorReport validate()
    {
        return null;
    }
}
