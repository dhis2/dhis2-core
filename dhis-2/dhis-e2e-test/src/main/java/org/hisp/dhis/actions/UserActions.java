/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.actions;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.hisp.dhis.Constants;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;

import com.google.gson.JsonObject;

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

    public String addUser( final String userName, final String password )
    {
        return addUserFull( userName, "bravo", userName, password, "" );
    }

    public String addUserFull( final String firstName, final String surname, final String username,
        final String password,
        String... auth )
    {
        String roleUid = new UserRoleActions().createWithAuthorities( auth );

        String id = idGenerator.generateUniqueId();

        JsonObject user = new JsonObjectBuilder()
            .addProperty( "id", id )
            .addProperty( "firstName", firstName )
            .addProperty( "surname", surname )
            .addProperty( "username", username )
            .addProperty( "password", password )
            .addArray( "userRoles", new JsonObjectBuilder().addProperty( "id", roleUid ).build() )
            .build();

        ApiResponse response = this.post( user );

        response.validate().statusCode( 201 );

        TestRunStorage.addCreatedEntity( endpoint, id );

        return id;
    }

    public void addRoleToUser( String userId, String userRoleId )
    {
        ApiResponse response = this.get( userId );
        if ( response.extractList( "userRoles.id" ).contains( userRoleId ) )
        {
            return;
        }

        JsonObject object = response.getBody();

        JsonObject userRole = new JsonObject();
        userRole.addProperty( "id", userRoleId );

        object.get( "userRoles" ).getAsJsonArray().add( userRole );

        this.update( userId, object ).validate().statusCode( 200 );
    }

    public void addUserToUserGroup( String userId, String userGroupId )
    {
        ApiResponse response = this.get( userId );
        List<String> userGroups = response.extractList( "userGroups.id" );
        if ( userGroups != null && userGroups.contains( userGroupId ) )
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
        this.grantUserAccessToOrgUnits( userId, orgUnitId, orgUnitId, orgUnitId );
    }

    public void grantUserAccessToOrgUnits( String userId, String captureOu, String searchOu, String dataReadOu )
    {
        JsonObject object = this.get( userId ).getBodyAsJsonBuilder()
            .addOrAppendToArray( "organisationUnits", new JsonObjectBuilder().addProperty( "id", captureOu ).build() )
            .addOrAppendToArray( "dataViewOrganisationUnits",
                new JsonObjectBuilder().addProperty( "id", dataReadOu ).build() )
            .addOrAppendToArray( "teiSearchOrganisationUnits",
                new JsonObjectBuilder().addProperty( "id", searchOu ).build() )
            .build();

        ApiResponse response = this.update( userId, object );
        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) );
    }

    public void grantUserSearchAccessToOrgUnit( String userId, String orgUnitId )
    {
        JsonObject object = this.get( userId ).getBodyAsJsonBuilder()
            .addOrAppendToArray( "teiSearchOrganisationUnits",
                new JsonObjectBuilder().addProperty( "id", orgUnitId ).build() )
            .build();

        ApiResponse response = this.update( userId, object );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) );
    }

    public void grantUserDataViewAccessToOrgUnit( String userId, String orgUnitId )
    {
        JsonObject object = this.get( userId ).getBodyAsJsonBuilder()
            .addOrAppendToArray( "dataViewOrganisationUnits",
                new JsonObjectBuilder().addProperty( "id", orgUnitId ).build() )
            .build();

        ApiResponse response = this.update( userId, object );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) );
    }

    public void grantUserCaptureAccessToOrgUnit( String userId, String orgUnitId )
    {
        JsonObject object = this.get( userId ).getBodyAsJsonBuilder()
            .addOrAppendToArray( "organisationUnits", new JsonObjectBuilder().addProperty( "id", orgUnitId ).build() )
            .build();

        ApiResponse response = this.update( userId, object );

        response.validate().statusCode( 200 )
            .body( "status", equalTo( "OK" ) );
    }

    /**
     * Grants user access to all org units imported before the tests.
     * /test/resources/setup/metadata.json
     *
     * @param userId
     */
    public void grantUserAccessToTAOrgUnits( String userId )
    {
        for ( String orgUnitId : Constants.ORG_UNIT_IDS )
        {
            grantUserAccessToOrgUnit( userId, orgUnitId );
        }
    }

    public void grantCurrentUserAccessToOrgUnit( String orgUnitId )
    {
        String userId = new LoginActions().getLoggedInUserId();

        grantUserAccessToOrgUnit( userId, orgUnitId );
    }

    public void updateUserPassword( String userId, String password )
    {
        new LoginActions().loginAsSuperUser();
        JsonObject user = this.get( userId ).getBody();
        user.addProperty( "password", password );

        this.update( userId, user );
    }
}
