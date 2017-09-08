package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Henning Håkonsen
 */
public class JobConfigurationSerializer
    extends JsonSerializer<JobConfiguration>
{
    @Override
    public void serialize( JobConfiguration jobConfiguration, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider )
        throws IOException, JsonProcessingException
    {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField( "name", jobConfiguration.getName() );
        jsonGenerator.writeStringField( "cronExpression", jobConfiguration.getCronExpression() );
        jsonGenerator.writeStringField( "jobType", jobConfiguration.getJobType().name() );
        jsonGenerator.writeStringField( "jobStatus", jobConfiguration.getJobStatus().name() );
        jsonGenerator.writeObjectField( "jobParameters", jobConfiguration.getJobParameters() );
        jsonGenerator.writeEndObject();

        System.out.println("Json: " + jsonGenerator);
    }
}
