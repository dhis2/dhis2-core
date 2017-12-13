package org.hisp.dhis.predictor;

import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PredictorJob
    extends AbstractJob
{
    @Autowired
    private PredictionService predictionService;

    @Override
    public JobType getJobType()
    {
        return JobType.PREDICTOR;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
        throws Exception
    {
        PredictorJobParameters predictorJobParameters = ( PredictorJobParameters ) jobConfiguration.getJobParameters();

        if ( predictorJobParameters == null )
        {
            throw new Exception( "No job parameters present in predictor job" );
        }

        List<String> predictors = predictorJobParameters.getPredictors();

        // TODO get start/end date
        //Date startDate = predictorJobParameters.getRelativeStart();
        //Date endDate = predictorJobParameters.getRelativeEnd();

        predictionService.predictPredictors( predictors, null, null );
    }
}
