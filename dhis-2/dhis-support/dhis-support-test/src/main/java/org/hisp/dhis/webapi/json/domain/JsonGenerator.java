package org.hisp.dhis.webapi.json.domain;

import static java.util.Collections.emptyMap;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.webapi.json.JsonList;

/**
 * Generates JSON object declarations as {@link String} based on
 * {@link JsonSchema}.
 *
 * @author Jan Bernitt
 */
public class JsonGenerator
{
    private final Map<String, JsonSchema> schemasByEndpoint = new HashMap<>();

    private final Map<String, JsonSchema> schemasByKlass = new HashMap<>();

    public JsonGenerator( JsonList<JsonSchema> schemas )
    {
        for ( JsonSchema s : schemas )
        {
            String endpoint = s.getRelativeApiEndpoint();
            if ( endpoint != null )
            {
                schemasByEndpoint.put( endpoint, s );
            }
            // avoid triggering class loading by using getString
            schemasByKlass.put( s.getString( "klass" ).string(), s );
        }
    }

    public Map<String, String> generateObjects( JsonSchema schema )
    {
        Map<String, String> objects = new LinkedHashMap<>();
        addObject( schema, false, objects );
        return objects;
    }

    private String addObject( JsonSchema schema, boolean addId, Map<String, String> objects )
    {
        String object = createObject( schema, addId, objects );
        objects.put( schema.getRelativeApiEndpoint(), object );
        return object;
    }

    private String createObject( JsonSchema schema, boolean addId, Map<String, String> objects )
    {
        StringBuilder json = new StringBuilder();
        json.append( '{' );
        int i = 0;
        if ( addId )
        {
            json.append( '"' ).append( "id" ).append( '"' ).append( ':' ).append( '"' )
                .append( CodeGenerator.generateUid() ).append( '"' );
            i++;
        }
        for ( JsonProperty property : schema.getRequiredProperties() )
        {
            if ( property.getPropertyType() == PropertyType.REFERENCE && property.getRelativeApiEndpoint() == null )
            {
                continue;
            }
            if ( i > 0 )
            {
                json.append( ',' );
            }
            json.append( '"' ).append( property.getName() ).append( '"' ).append( ':' );
            appendValue( json, property, objects );
            i++;
        }
        json.append( '}' );
        return json.toString();
    }

    private void appendValue( StringBuilder json, JsonProperty property, Map<String, String> objects )
    {
        switch ( property.getPropertyType() )
        {
        case COMPLEX:
            JsonSchema schema = schemasByKlass.get( property.getString( "klass" ).string() );
            if ( schema == null )
            {
                json.append( "null" );
            }
            else
            {
                json.append( createObject( schema, false, emptyMap() ) );
            }
            break;
        case TEXT:
            json.append( '"' ).append( generateString( property ) ).append( '"' );
            break;
        case DATE:
            json.append( '"' ).append( generateDateString() ).append( '"' );
            break;
        case CONSTANT:
            json.append( '"' ).append( property.getConstants().get( 0 ) ).append( '"' );
            break;
        case BOOLEAN:
            json.append( "true" );
            break;
        case INTEGER:
        case NUMBER:
            json.append( getSmallestPositiveValue( property ) );
            break;
        case IDENTIFIER:
            json.append( '"' ).append( generateId( property ) ).append( '"' );
            break;
        case REFERENCE:
            String object = objects.get( property.getRelativeApiEndpoint() );
            if ( object == null )
            {
                schema = schemasByEndpoint.get( property.getRelativeApiEndpoint() );
                object = addObject( schema, true, objects );
            }
            if ( object.isEmpty() )
            {
                // we are already trying to compute an object of this type
                json.append( "null" );
                return;
            }
            int idStart = object.indexOf( "\"id\":" ) + 6;
            int idEnd = object.indexOf( '"', idStart );
            json.append( "{\"id\":\"" ).append( object, idStart, idEnd ).append( "\"}" );
            break;
        default:
            throw new IllegalArgumentException( property.getName() + " " + property.getPropertyType() );
        }
    }

    private static String generateId( JsonProperty property )
    {
        switch ( property.getName() )
        {
        case "id":
        case "uid":
        case "code":
        case "cid":
            return CodeGenerator.generateUid();
        default: // assume reference
                 // TODO return id of object of correct type
            throw new UnsupportedOperationException( "id type not supported: " + property.getName() );
        }
    }

    private static String generateDateString()
    {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format( LocalDateTime.now() );
    }

    private static String generateString( JsonProperty property )
    {
        switch ( property.getName() )
        {
        case "url":
            return "http://example.com";
        case "cronExpression":
            return "* * * * * *";
        case "periodType":
            return PeriodType.PERIOD_TYPES.get( 0 ).getName();

        case "name":
        case "shortName":
            // there are often unique constrains on TEXT attributes called name,
            // so...
            return getUniqueString( property );
        default:
            return getRandomString( property );
        }
    }

    private static String getRandomString( JsonProperty property )
    {
        int length = getSmallestPositiveValue( property );
        StringBuilder str = new StringBuilder( length );
        str.append( property.getName() );
        char c = 'a';
        while ( str.length() < length )
        {
            str.append( c++ );
        }
        return str.toString();
    }

    private static int getSmallestPositiveValue( JsonProperty property )
    {
        int min = property.getMin().intValue();
        int max = property.getMax().intValue();
        return Math.min( Math.max( 1, min ), Math.max( 1, max ) );
    }

    private static String getUniqueString( JsonProperty property )
    {
        int min = property.getMin().intValue();
        int max = property.getMax().intValue();
        return min < 11 && (max < 0 || max >= 11)
            ? CodeGenerator.generateUid()
            : CodeGenerator.generateCode( max );
    }
}
