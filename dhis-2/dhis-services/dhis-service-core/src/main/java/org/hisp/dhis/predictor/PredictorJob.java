package org.hisp.dhis.predictor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PredictorJob
    implements Job
{
    @Autowired
    private PredictorService predictorService;

    private static final Log log = LogFactory.getLog( PredictorJob.class );

    @Override
    public JobType getJobType()
    {
        return JobType.PREDICTOR;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        PredictorJobParameters predictorJobParameters = ( PredictorJobParameters ) jobConfiguration.getJobParameters();

        try
        {
            if ( predictorJobParameters == null )
            {
                throw new Exception( "No job parameters present in predictor job" );
            }

            List<String> predictorUids = predictorJobParameters.getPredictorUids();
            Date startDate = predictorJobParameters.getStartDate();
            Date endDate = predictorJobParameters.getEndDate();

            predictorUids.forEach( uid -> {
                Predictor predictor = predictorService.getPredictor( uid );

                int count = predictorService.predict( predictor, startDate, endDate );

                log.info( "Generated " + count + " predictions" );
            } );
        }
        catch ( Exception ex )
        {
            log.error( "Unable to predict.", ex);
        }
    }
}
