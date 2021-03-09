package org.hisp.dhis.dto.schemas;



import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;
import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public enum PropertyType
{
    NUMBER( "INTEGER", "NUMBER" ),
    STRING( "TEXT" ),
    BOOLEAN( "BOOLEAN" ),
    CONSTANT( "CONSTANT" ),
    REFERENCE( "REFERENCE" ),
    COMPLEX( "COMPLEX" ),
    COLLECTION( "COLLECTION" ),
    IDENTIFIER( "IDENTIFIER" ),
    DATE( "DATE" ),
    UNKNOWN( "" );

    private final List<String> values;

    private PropertyType( String... values )
    {
        this.values = Arrays.asList( values );
    }

    @JsonCreator
    public static PropertyType getPropertyTypeFromValue( String value )
    {
        for ( PropertyType type : PropertyType.values() )
        {
            if ( type.values.contains( value ) )
            {
                return type;
            }
        }
        return UNKNOWN;
    }
}
