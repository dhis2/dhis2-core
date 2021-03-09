package org.hisp.dhis.actions;



import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class IdGenerator
    extends RestApiActions
{
    public IdGenerator()
    {
        super( "/system" );
    }

    public String generateUniqueId()
    {
        String id = get( "id.json", new QueryParamsBuilder().add( "limit=1" ) )
            .validate()
            .statusCode( 200 )
            .extract().path( "codes[0]" );

        return id;
    }
}
