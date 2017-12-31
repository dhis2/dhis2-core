package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PredictorJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 5526554074518768146L;

    @Property
    private int relativeStart;

    @Property
    private int relativeEnd;

    @Property
    private List<String> predictors;

    public PredictorJobParameters()
    {
    }

    public PredictorJobParameters( int relativeStart, int relativeEnd, List<String> predictors )
    {
        this.relativeStart = relativeStart;
        this.relativeEnd = relativeEnd;
        this.predictors = predictors;
    }

    public int getRelativeStart()
    {
        return relativeStart;
    }

    public void setRelativeStart( int relativeStart )
    {
        this.relativeStart = relativeStart;
    }

    public int getRelativeEnd()
    {
        return relativeEnd;
    }

    public void setRelativeEnd( int relativeEnd )
    {
        this.relativeEnd = relativeEnd;
    }

    public List<String> getPredictors()
    {
        return predictors;
    }

    public void setPredictors( List<String> predictors )
    {
        this.predictors = predictors;
    }

    @Override
    public ErrorReport validate()
    {
        return null;
    }
}
