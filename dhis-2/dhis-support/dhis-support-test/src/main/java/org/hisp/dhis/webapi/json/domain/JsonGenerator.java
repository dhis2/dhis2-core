package org.hisp.dhis.webapi.json.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.period.PeriodType;

public class JsonGenerator
{
    public static String generateObject( JsonSchema schema )
    {
        StringBuilder json = new StringBuilder();
        appendObject( json, schema.getRequiredProperties() );
        return json.toString();
    }

    private static void appendObject( StringBuilder json, Iterable<JsonProperty> properties )
    {
        json.append( '{' );
        int i = 0;
        for ( JsonProperty property : properties )
        {
            if ( i > 0 )
            {
                json.append( ',' );
            }
            json.append( '"' ).append( property.getName() ).append( '"' ).append( ':' );
            appendValue( json, property );
            i++;
        }
        json.append( '}' );
    }

    private static void appendValue( StringBuilder json, JsonProperty property )
    {
        switch ( property.getPropertyType() )
        {
        default:
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
            // TODO create an object: {"id": <uid>} with uid of created object
            throw new UnsupportedOperationException( "reference not supported: " + property.getName() );
        }
    }

    private static String generateId( JsonProperty property )
    {
        switch ( property.getName() )
        {
        case "id":
        case "uid":
        case "code":
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
        default:
            int length = getSmallestPositiveValue( property );
            StringBuilder str = new StringBuilder( length );
            str.append( property.getName() );
            int i = 1;
            while ( str.length() < length )
            {
                str.append( i++ );
            }
            return str.toString();
        }
    }

    private static int getSmallestPositiveValue( JsonProperty property )
    {
        return Math.min( Math.max( 1, property.getMin().intValue() ), property.getMax().intValue() );
    }
}
