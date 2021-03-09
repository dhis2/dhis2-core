package org.hisp.dhis.actions;



import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.schemas.Schema;
import org.hisp.dhis.dto.schemas.SchemaProperty;
import org.hisp.dhis.helpers.QueryParamsBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SchemasActions
    extends RestApiActions
{
    public SchemasActions()
    {
        super( "/schemas" );
    }

    public List<SchemaProperty> getRequiredProperties( String resource )
    {
        List<SchemaProperty> list = get( resource ).extractList( "properties", SchemaProperty.class );

        return list.stream()
            .filter( (schemaProperty -> schemaProperty.isRequired()) )
            .collect( Collectors.toList() );
    }

    public Schema getSchema( String resource )
    {
        return get( resource ).extractObject( "", Schema.class );
    }

    public ApiResponse validateObjectAgainstSchema( String resource, Object obj )
    {
        return post( resource, obj );
    }

    public String findSchemaPropertyByKlassName( String klass, String property )
    {
        return findSchemaPropertyByKnownProperty( property, "klass", klass );
    }

    public String findSchemaPropertyByKnownProperty( String propertyToFind, String knownPropertyName, String knownPropertyValue )
    {
        return get( "", new QueryParamsBuilder().add( String.format( "fields=%s,%s", propertyToFind, knownPropertyName ) ) )
            .extractString(
                String.format( "schemas.find{it.%s == '%s'}.%s", knownPropertyName, knownPropertyValue, propertyToFind ) );
    }
}
