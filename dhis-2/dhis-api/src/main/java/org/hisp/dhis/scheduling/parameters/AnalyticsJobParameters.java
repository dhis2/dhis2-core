package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.databind.JsonNode;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Henning Håkonsen
 */
public class AnalyticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 4613054056442242637L;

    @Property
    private Integer lastYears;

    @Property
    private Set<String> skipTableTypes = new HashSet<>( );

    @Property
    private boolean skipResourceTables;

    public AnalyticsJobParameters()
    {
    }

    public AnalyticsJobParameters( Integer lastYears, Set<String> skipTableTypes, boolean skipResourceTables )
    {
        this.lastYears = lastYears;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
    }

    public Integer getLastYears()
    {
        return lastYears;
    }

    public Set<String> getSkipTableTypes()
    {
        return skipTableTypes;
    }

    public boolean isSkipResourceTables()
    {
        return skipResourceTables;
    }

    public JobParameters mapParameters( JsonNode parameters )
        throws IOException
    {
        List<Field> fieldList = Arrays.stream( AnalyticsJobParameters.class.getDeclaredFields() )
            .filter( field -> field.getType().getSimpleName().equals( "Property" ) ).collect( Collectors.toList() );

        for ( Field field : fieldList )
        {
            String fieldName = field.getName();
            if ( parameters.get( fieldName ) != null )
            {
                switch ( fieldName )
                {
                case "lastYears":
                    this.lastYears = parameters.get( "lastYears" ).asInt();
                    break;
                case "skipTableTypes":
                    for ( final JsonNode tableType : parameters.get( "skipTableTypes" ) )
                    {
                        this.skipTableTypes.add( tableType.textValue() );
                    }
                    break;
                case "skipResourceTables":
                    this.skipResourceTables = parameters.get( "skipResourceTables" ).asBoolean();
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
