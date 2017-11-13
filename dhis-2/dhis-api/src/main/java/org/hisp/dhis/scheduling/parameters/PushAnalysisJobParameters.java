package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

/**
 * @author Henning Håkonsen
 */
public class PushAnalysisJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -1848833906375595488L;

    @Property
    private String pushAnalysis;

    public PushAnalysisJobParameters()
    {
    }

    public PushAnalysisJobParameters( String pushAnalysis )
    {
        this.pushAnalysis = pushAnalysis;
    }

    public String getPushAnalysis()
    {
        return pushAnalysis;
    }
}
