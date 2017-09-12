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

        boolean enabled = root.get( "enabled" ) == null || root.get( "enabled" ).booleanValue();

        return new JobConfiguration( root.get( "name" ).toString(), jobType, cronExpression, jobParameters, enabled );
    }
}
