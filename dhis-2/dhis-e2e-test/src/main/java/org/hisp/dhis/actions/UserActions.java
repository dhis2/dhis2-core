package org.hisp.dhis.actions;

/*
 * Copyright (c) 2004-2020, University of Oslo
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
