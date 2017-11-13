package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.Date;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PredictorJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 5526554074518768146L;

    @Property
    private Date startDate;

    @Property
    private Date endDate;

    @Property
    private List<String> predictorUids;

    PredictorJobParameters()
    {
    }

    PredictorJobParameters( Date startDate, Date endDate, List<String> predictorUids )
    {
        this.startDate = startDate;
        this.endDate = endDate;
        this.predictorUids = predictorUids;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }

    public List<String> getPredictorUids()
    {
        return predictorUids;
    }

    public void setPredictorUids( List<String> predictorUids )
    {
        this.predictorUids = predictorUids;
    }
}
