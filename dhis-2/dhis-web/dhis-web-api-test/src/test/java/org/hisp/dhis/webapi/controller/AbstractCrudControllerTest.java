/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.webapi.WebClient.Body;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertError;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertSeries;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.domain.JsonGeoMap;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonTranslation;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.snippets.SomeUserId;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the generic operations offered by the {@link AbstractCrudController}
 * using specific endpoints.
 *
 * @author Jan Bernitt
 */
public class AbstractCrudControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetObjectList()
    {
        JsonList<JsonUser> users = GET( "/users/" )
            .content( HttpStatus.OK ).getList( "users", JsonUser.class );

        assertEquals( 1, users.size() );
        JsonUser user = users.get( 0 );
        assertEquals( "admin admin", user.getDisplayName() );
    }

    @Test
    public void testGetObject()
    {
        String id = run( SomeUserId::new );
        JsonUser userById = GET( "/users/{id}", id )
            .content( HttpStatus.OK ).as( JsonUser.class );

        assertEquals( id, userById.getId() );
        assertTrue( userById.getUserCredentials().exists() );
    }

    @Test
    public void testGetObjectProperty()
    {
        // response will look like: { "surname": <name> }
        JsonUser userProperty = GET( "/users/{id}/surname", run( SomeUserId::new ) )
            .content( HttpStatus.OK ).as( JsonUser.class );

        assertEquals( "admin", userProperty.getSurname() );
        assertEquals( 1, userProperty.size() );
    }

    @Test
    public void testPartialUpdateObject()
    {
        String id = run( SomeUserId::new );
        assertStatus( HttpStatus.NO_CONTENT, PATCH( "/users/" + id, "{'surname':'Peter'}" ) );

        assertEquals( "Peter", GET( "/users/{id}", id ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    public void replaceTranslations()
    {
        String id = getCurrentUser().getUid();
        JsonArray translations = GET( "/users/{id}/translations", id )
            .content().getArray( "translations" );

        assertTrue( translations.isEmpty() );

        assertStatus( HttpStatus.NO_CONTENT, PUT( "/users/" + id + "/translations",
            "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn'}]}" ) );

        translations = GET( "/users/{id}/translations", id )
            .content().getArray( "translations" );
        assertEquals( 1, translations.size() );
        JsonTranslation translation = translations.get( 0, JsonTranslation.class );
        assertEquals( "sv", translation.getLocale() );
        assertEquals( "name", translation.getProperty() );
        assertEquals( "namn", translation.getValue() );
    }

    @Test
    public void replaceTranslations_MissingValue()
    {
        String id = getCurrentUser().getUid();
        String translations = "{'translations': [{'locale':'sv', 'property':'name'}]}";
        assertError( ErrorCode.E4000, "Missing required property `value`.",
            PUT( "/users/" + id + "/translations", translations ).error() );
    }

    @Test
    public void replaceTranslations_MissingProperty()
    {
        String id = getCurrentUser().getUid();
        assertError( ErrorCode.E4000, "Missing required property `property`.",
            PUT( "/users/" + id + "/translations",
                "{'translations': [{'locale':'sv', 'value':'namn'}]}" ).error() );
    }

    @Test
    public void replaceTranslations_MissingLocale()
    {
        String id = getCurrentUser().getUid();
        String translations = "{'translations': [{'property':'name', 'value':'namn'}]}";
        assertError( ErrorCode.E4000, "Missing required property `locale`.",
            PUT( "/users/" + id + "/translations", translations ).error() );
    }

    @Test
    public void testUpdateObjectProperty()
    {
        String id = getCurrentUser().getUid();
        assertStatus( HttpStatus.NO_CONTENT,
            PATCH( "/users/" + id + "/firstName", "{'firstName':'Fancy Mike'}" ) );
        assertEquals( "Fancy Mike", GET( "/users/{id}", id )
            .content().as( JsonUser.class ).getFirstName() );
    }

    @Test
    public void testUpdateObjectProperty_ReadOnlyProperty()
    {
        String id = getCurrentUser().getUid();
        assertStatus( HttpStatus.FORBIDDEN,
            PATCH( "/users/" + id + "/displayName", "{'displayName':'Fancy Mike'}" ) );
    }

    @Test
    public void testPostJsonObject()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/constants/", "{'name':'answer', 'value': 42}" ) );
    }

    @Test
    public void testSetAsFavorite()
    {
        // first we need to create an entity that can be marked as favorite
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );

        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/favorite" ) );
        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( singletonList( getCurrentUser().getUid() ), map.getFavorites() );
    }

    @Test
    public void testRemoveAsFavorite()
    {
        // first we need to create an entity that can be marked as favorite
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        // make it a favorite
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/favorite" ) );

        assertStatus( HttpStatus.OK, DELETE( "/maps/" + mapId + "/favorite" ) );
        assertEquals( emptyList(), GET( "/maps/{uid}", mapId )
            .content().as( JsonGeoMap.class ).getFavorites() );
    }

    @Test
    public void testSubscribe()
    {
        // first we need to create an entity that can be subscribed to
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );

        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/subscriber" ) );
        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( singletonList( getCurrentUser().getUid() ), map.getSubscribers() );
    }

    @Test
    public void testUnsubscribe()
    {
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/subscriber" ) );

        assertStatus( HttpStatus.OK, DELETE( "/maps/" + mapId + "/subscriber" ) );
        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( emptyList(), map.getSubscribers() );
    }

    @Test
    public void testPutJsonObject()
    {
        // first the updated entity needs to be created
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );

        assertStatus( HttpStatus.NO_CONTENT, PUT( "/maps/" + mapId, "{'name':'Europa'}" ) );
        assertEquals( "Europa", GET( "/maps/{id}", mapId )
            .content().as( JsonGeoMap.class ).getName() );
    }

    @Test
    public void testPutJsonObject_skipTranslations()
    {
        // first the updated entity needs to be created
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'My Group'}" ) );

        assertStatus( HttpStatus.NO_CONTENT, PUT( "/userGroups/" + groupId + "/translations",
            "{'translations':[{'property':'NAME','locale':'no','value':'norsk test'}," +
                "{'property':'DESCRIPTION','locale':'no','value':'norsk test beskrivelse'}]}" ) );
        // verify we have translations
        assertEquals( 2, GET( "/userGroups/{uid}/translations", groupId )
            .content().getArray( "translations" ).size() );

        // now put object with skipping translations
        assertSeries( SUCCESSFUL,
            PUT( "/userGroups/" + groupId + "?skipTranslation=true", "{'name':'Europa'}" ) );
        assertEquals( 2, GET( "/userGroups/{uid}/translations", groupId )
            .content().getArray( "translations" ).size() );
    }

    @Test
    public void testPutJsonObject_skipSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'My Group'}" ) );
        JsonObject group = GET( "/userGroups/{id}", groupId ).content();

        String groupWithoutSharing = group.getObject( "sharing" ).node().replaceWith( "null" ).toString();
        assertStatus( HttpStatus.OK, PUT( "/userGroups/" + groupId + "?skipSharing=true", groupWithoutSharing ) );
        assertEquals( "rw------", GET( "/userGroups/{id}", groupId )
            .content().as( JsonGeoMap.class ).getSharing().getPublic().string() );
    }

    @Test
    public void testPutJsonObject_accountExpiry()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();

        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );

        assertStatus( HttpStatus.OK,
            PUT( "/users/{id}", userId,
                Body( user.getUserCredentials().node()
                    .addMember( "accountExpiry", "null" ).toString() ) ) );

        assertNull( GET( "/users/{id}", userId )
            .content().as( JsonUser.class ).getUserCredentials().getAccountExpiry() );
    }

    @Test
    public void testPutJsonObject_accountExpiry_PutNoChange()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();

        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );

        assertStatus( HttpStatus.OK,
            PUT( "/users/{id}", userId, Body( user.toString() ) ) );

        assertNull( GET( "/users/{id}", userId )
            .content().as( JsonUser.class ).getUserCredentials().getAccountExpiry() );
    }

    @Test
    public void testPutJsonObject_accountExpiry_NaN()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();

        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );

        String body = user.getUserCredentials().node().addMember( "accountExpiry", "\"NaN\"" ).toString();
        assertEquals( "Invalid date format 'NaN', only ISO format or UNIX Epoch timestamp is supported.",
            PUT( "/users/{id}", userId, Body( body ) ).error().getMessage() );
    }

    @Test
    public void testDeleteObject()
    {
        // first the deleted entity needs to be created
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );

        assertStatus( HttpStatus.OK, DELETE( "/maps/" + mapId ) );
        assertEquals( 0, GET( "/maps" ).content().getArray( "maps" ).size() );
    }

    @Test
    public void testGetCollectionItem()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        // add an item to the collection
        assertSeries( SUCCESSFUL, POST( "/userGroups/" + groupId + "/users/" + userId ) );

        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    public void testAddCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            POST( "/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}" ) );

        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    public void testReplaceCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}" ) );
        String peter = "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'userCredentials':{'username':'peter47'}}";
        String peterUserId = assertStatus( HttpStatus.CREATED, POST( "/users", peter ) );

        assertStatus( HttpStatus.NO_CONTENT,
            PUT( "/userGroups/" + groupId + "/users", "{'identifiableObjects':[{'id':'" + peterUserId + "'}]}" ) );

        assertUserGroupHasOnlyUser( groupId, peterUserId );
    }

    @Test
    public void testAddCollectionItem()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );

        assertStatus( HttpStatus.NO_CONTENT, POST( "/userGroups/{uid}/users/{itemId}", groupId, userId ) );
        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    public void testDeleteCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}" ) );

        assertStatus( HttpStatus.NO_CONTENT,
            DELETE( "/userGroups/" + groupId + "/users", "{'identifiableObjects':[{'id':'" + userId + "'}]}" ) );
        assertEquals( 0, GET( "/userGroups/{uid}/users/", groupId ).content().getArray( "users" ).size() );
    }

    @Test
    public void testSetSharing()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which can be shared
        String programId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}" ) );

        String sharing = "{'owner':'" + userId + "', 'public':'rwrw----', 'external': true }";
        assertStatus( HttpStatus.NO_CONTENT, PUT( "/programs/" + programId + "/sharing", sharing ) );

        JsonIdentifiableObject program = GET( "/programs/{id}", programId )
            .content().as( JsonIdentifiableObject.class );
        assertTrue( program.exists() );
        assertEquals( "rwrw----", program.getSharing().getPublic().string() );
        assertFalse( "programs cannot be external", program.getSharing().isExternal() );
    }

    private void assertUserGroupHasOnlyUser( String groupId, String userId )
    {
        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/users/{itemId}", groupId, userId ).content()
            .getList( "users", JsonUser.class );
        assertEquals( 1, usersInGroup.size() );
        assertEquals( userId, usersInGroup.get( 0 ).getId() );
    }
}
