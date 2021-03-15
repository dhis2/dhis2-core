



package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.OrgUnit;
import org.hisp.dhis.helpers.JsonParserUtils;
import org.hisp.dhis.utils.DataGenerator;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OrgUnitActions
    extends RestApiActions
{
    public OrgUnitActions()
    {
        super( "/organisationUnits" );
    }

    /**
     * Sends POST request to create provided org unit
     *
     * @param orgUnit
     * @return
     */
    public ApiResponse post( final OrgUnit orgUnit )
    {
        JsonObject object = JsonParserUtils.toJsonObject( orgUnit );
        if ( orgUnit.getParent() != null )
        {
            JsonObject parent = new JsonObject();
            parent.addProperty( "id", orgUnit.getParent() );
            object.add( "parent", parent );
        }

        return super.post( object );
    }

    /***
     * Generates dummy org unit and sends POST request to create it.
     * @return
     */
    public ApiResponse postDummyOrgUnit()
    {
        return post( generateDummy() );
    }

    public String create( final OrgUnit orgUnit )
    {
        ApiResponse response = post( orgUnit );

        response.validate().statusCode( 201 );

        return response.extractString( "response.uid" );
    }

    public OrgUnit generateDummy()
    {
        String randomString = DataGenerator.randomString();

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setName( "AutoTest OrgUnit" + randomString );
        orgUnit.setShortName( "AutoTest orgUnit short name " + randomString );
        orgUnit.setOpeningDate( "2017-09-11T00:00:00.000" );

        return orgUnit;
    }

    public String createOrgUnit()
    {
        return create( generateDummy() );
    }

    public String createOrgUnit( int level )
    {
        OrgUnit orgUnit = generateDummy();
        orgUnit.setLevel( level );

        return create( orgUnit );
    }

    public String createOrgUnitWithParent( String parentId )
    {
        OrgUnit orgUnit = generateDummy();

        orgUnit.setParent( parentId );

        return create( orgUnit );
    }

    public String createOrgUnitWithParent( String parentId, int level )
    {
        OrgUnit orgUnit = generateDummy();
        orgUnit.setLevel( level );
        orgUnit.setParent( parentId );

        return create( orgUnit );
    }
}
