package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Henning Håkonsen
 */
public class TestJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 3600315605964091689L;

    @Property
    private String message;

    public TestJobParameters()
    {
    }

    public TestJobParameters( String message )
    {
        this.message = message;
    }

    @JacksonXmlProperty
    @JsonProperty
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    @Override
    public JobParameters mapParameters( JsonNode parameters )
        throws IOException
    {
        List<Field> fieldList = Arrays.stream( TestJobParameters.class.getDeclaredFields() )
            .filter( field -> field.getType().getSimpleName().equals( "Property" ) ).collect( Collectors.toList() );

        for ( Field field : fieldList )
        {
            String fieldName = field.getName();
            System.out.println( "fieldName: " + fieldName + ", field: " + field.getName() );
            if ( parameters.get( fieldName ) != null )
            {
                switch ( fieldName )
                {
                case "message":
                    this.message = parameters.get( "message" ).textValue();
                    break;
                default:
                    throw new IOException( "Unknown parameter '" + field.getName() + "'." );
                }
            }
            else
            {
                throw new IOException( "Property '" + fieldName + "' not present" );
            }
        }

        return this;
    }
}
