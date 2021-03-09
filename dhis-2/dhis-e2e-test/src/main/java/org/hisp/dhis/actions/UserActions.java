package org.hisp.dhis.actions;



import com.google.gson.JsonObject;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.dto.ApiResponse;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class UserActions
    extends RestApiActions
{
    private IdGenerator idGenerator = new IdGenerator();

    public UserActions()
    {
        super( "/users" );
    }

    public String addUser( final String firstName, final String surname, final String username, final String password )
    {
        String id = idGenerator.generateUniqueId();

        JsonObject user = new JsonObject();

        user.addProperty( "id", id );
        user.addProperty( "firstName", firstName );
        user.addProperty( "surname", surname );

        JsonObject credentials = new JsonObject();
        credentials.addProperty( "username", username );
        credentials.addProperty( "password", password );

        JsonObject userInfo = new JsonObject();
        userInfo.addProperty( "id", id );

        credentials.add( "userInfo", userInfo );
        user.add( "userCredentials", credentials );

        ApiResponse response = this.post( user );

        response.validate().statusCode( 200 );

        TestRunStorage.addCreatedEntity( endpoint, id );

        return id;
    }

    public void addRoleToUser( String userId, String userRoleId )
    {
        ApiResponse response = this.get( userId );
        if ( response.extractList( "userCredentials.userRoles.id" ).contains( userRoleId ) )
        {
            return;
        }

        JsonObject object = response.getBody();

        JsonObject userRole = new JsonObject();
        userRole.addProperty( "id", userRoleId );

        object.get( "userCredentials" ).getAsJsonObject().get( "userRoles" ).getAsJsonArray().add( userRole );

        this.update( userId, object ).validate().statusCode( 200 );
    }

    public void addUserToUserGroup( String userId, String userGroupId )
    {
        ApiResponse response = this.get( userId );
        if ( response.extractList( "userGroups.id" ).contains( userGroupId ) )
        {
            return;
        }

        JsonObject object = response.getBody();

        JsonObject userGroupAccess = new JsonObject();
        userGroupAccess.addProperty( "id", userGroupId );

        object.get( "userGroups" ).getAsJsonArray().add( userGroupAccess );

        this.update( userId, object ).validate().statusCode( 200 );
    }

    public void grantUserAccessToOrgUnit( String userId, String orgUnitId )
    {
        JsonObject object = this.get( userId ).getBody();
        JsonObject orgUnit = new JsonObject();
        orgUnit.addProperty( "id", orgUnitId );

        object.get( "organisationUnits" ).getAsJsonArray().add( orgUnit );
        object.get( "dataViewOrganisationUnits" ).getAsJsonArray().add( orgUnit );
        object.get( "teiSearchOrganisationUnits" ).getAsJsonArray().add( orgUnit );

        ApiResponse response = this.update( userId, object );
        response.validate().statusCode( 200 );
    }

    public void grantCurrentUserAccessToOrgUnit( String orgUnitId )
    {
        String userId = new LoginActions().getLoggedInUserId();

        grantUserAccessToOrgUnit( userId, orgUnitId );
    }

    public String addUser( final String userName, final String password )
    {
        return addUser( "johnny", "bravo", userName, password );
    }

    public void updateUserPassword( String userId, String password )
    {
        new LoginActions().loginAsSuperUser();
        JsonObject user = this.get( userId ).getBody();
        user.getAsJsonObject( "userCredentials" )
            .addProperty( "password", password );

        this.update( userId, user );
    }
}
