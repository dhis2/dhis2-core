package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Custom deserializer for {@link JobConfiguration} objects. This is due to different objects
 * being stored in the parameter variable.
 *
 * @author Henning Håkonsen
 */
public class JobConfigurationDeserializer
    extends JsonDeserializer<JobConfiguration>
{
    private final String NAME = "name";
    private final String JOB_TYPE = "jobType";
    private final String ENABLED = "enabled";
    private final String CONTINUOUS_EXECUTION = "continuousExecution";
    private final String JOB_PARAMETERS = "jobParameters";
    private final String CRON_EXPRESSION = "cronExpression";

    @Override
    public JobConfiguration deserialize( JsonParser jsonParser,
        DeserializationContext deserializationContext )
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree( jsonParser );

        if ( !root.has( NAME ) )
        {
            throw new IOException( "name must not be null" );
        }

        if ( !root.has( JOB_TYPE ) )
        {
            throw new IOException( "jobType must not be null" );
        }

        JobType jobType = JobType.valueOf( root.get( JOB_TYPE ).textValue() );

        JobParameters jobParameters = null;
        if ( root.get( JOB_PARAMETERS ) != null && jobType.getJobParameters() != null )
        {
            jobParameters = mapper.convertValue( root.get( JOB_PARAMETERS ), jobType.getJobParameters() );
        }

        boolean enabled = !root.has( ENABLED ) || root.get( ENABLED ).asBoolean();

        boolean continuousExecution = root.has( CONTINUOUS_EXECUTION ) && root.get( CONTINUOUS_EXECUTION ).asBoolean();

        String cronExpression = root.has( CRON_EXPRESSION ) ? root.get( CRON_EXPRESSION ).textValue() : "";

        return new JobConfiguration( root.get( NAME ).textValue(), jobType,
            cronExpression, jobParameters,
            continuousExecution, enabled );
    }
}
