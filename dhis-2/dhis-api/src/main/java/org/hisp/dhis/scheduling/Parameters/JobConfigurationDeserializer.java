package org.hisp.dhis.scheduling.Parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobType;

import java.io.IOException;

/**
 * @author Henning Håkonsen
 */
public class JobConfigurationDeserializer
    extends JsonDeserializer<JobConfiguration>
{
    @Override
    public JobConfiguration deserialize( JsonParser jsonParser,
        DeserializationContext deserializationContext )
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = mapper.readTree(jsonParser);

        String jobTypeString = root.get( "jobType" ).toString();
        JobType jobType = JobType.valueOf( jobTypeString.substring( 1, jobTypeString.length() - 1 ) );

        String cronExpression = mapper.convertValue( root.get( "cronExpression" ), String.class );

        JobParameters jobParameters = mapper.convertValue( root.get( "jobParameters" ), jobType.getClazz() );

        return new JobConfiguration( root.get( "name" ).toString(), jobType, cronExpression, jobParameters );
    }
}
