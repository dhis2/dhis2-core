package org.hisp.dhis.actions;



import io.restassured.RestAssured;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UaaActions
    extends RestApiActions
{
    public UaaActions()
    {
        super( "/uaa" );
        setBaseUri( RestAssured.baseURI.replace( "/api", "/" ) );
    }
}
