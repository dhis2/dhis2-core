package org.hisp.dhis.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Unit tests of {@link JobConfigurationDeserializer}.
 *
 * @author Volker Schmidt
 */
public class JobConfigurationDeserializerTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserializeWithoutId() throws IOException
    {
        final JobConfiguration jobConfiguration = objectMapper.readValue( "{\"name\":\"Test\",\"jobType\":\"RESOURCE_TABLE\"}", JobConfiguration.class );
        Assert.assertNull( jobConfiguration.getUid() );
    }

    @Test
    public void deserializeWithId() throws IOException
    {
        final JobConfiguration jobConfiguration = objectMapper.readValue( "{\"id\":\"kjsdfDSU23\",\"name\":\"Test\",\"jobType\":\"RESOURCE_TABLE\"}", JobConfiguration.class );
        Assert.assertEquals( "kjsdfDSU23", jobConfiguration.getUid() );
    }
}