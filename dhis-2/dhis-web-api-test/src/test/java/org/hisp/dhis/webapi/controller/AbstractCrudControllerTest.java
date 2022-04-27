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
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertSeries;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonArray;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.JsonObject;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonGeoMap;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonTranslation;
import org.hisp.dhis.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
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
        assertStatus( HttpStatus.OK, PATCH( "/users/" + id + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/surname', 'value': 'Peter'}]" ) );

        assertEquals( "Peter", GET( "/users/{id}", id ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    public void testPartialUpdateObject_Validation()
    {
        String id = run( SomeUserId::new );
        JsonError error = PATCH( "/users/" + id + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/email', 'value': 'Not-valid'}]" ).error();

        assertEquals( "Property `email` requires a valid email address, was given `Not-valid`.",
            error.getTypeReport().getErrorReports().get( 0 ).getMessage() );
    }

    @Test
    public void replaceTranslationsForNotTranslatableObject()
    {
        String id = getCurrentUser().getUid();
        JsonArray translations = GET( "/users/{id}/translations", id )
            .content().getArray( "translations" );

        assertTrue( translations.isEmpty() );
        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/users/" + id + "/translations",
                "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn'}]}" )
                    .content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E1107 );
        assertEquals( "Object type `User` is not translatable.", error.getMessage() );
    }

    @Test
    public void replaceTranslationsOk()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );
        JsonArray translations = GET( "/dataSets/{id}/translations", id )
            .content().getArray( "translations" );

        assertTrue( translations.isEmpty() );

        PUT( "/dataSets/" + id + "/translations",
            "{'translations': [{'locale':'sv', 'property':'name', 'value':'name sv'}]}" )
                .content( HttpStatus.NO_CONTENT );

        JsonResponse content = GET( "/dataSets/{id}", id ).content();

        translations = GET( "/dataSets/{id}/translations", id ).content().getArray( "translations" );
        assertEquals( 1, translations.size() );
        JsonTranslation translation = translations.get( 0, JsonTranslation.class );
        assertEquals( "sv", translation.getLocale() );
        assertEquals( "name", translation.getProperty() );
        assertEquals( "name sv", translation.getValue() );
    }

    @Test
    public void replaceTranslationsWithDuplicateLocales()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );
        JsonArray translations = GET( "/dataSets/{id}/translations", id )
            .content().getArray( "translations" );

        assertTrue( translations.isEmpty() );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn 1'},{'locale':'sv', 'property':'name', 'value':'namn2'}]}" )
                    .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E1106 );
        assertEquals( "There are duplicate translation record for property `name` and locale `sv`",
            error.getMessage() );
        assertEquals( "name", error.getErrorProperties().get( 0 ) );
    }

    @Test
    public void replaceTranslations_NoSuchEntity()
    {
        String translations = "{'translations': [{'locale':'sv', 'property':'name'}]}";
        assertWebMessage( "Not Found", 404, "ERROR", "User with id notanid could not be found.",
            PUT( "/users/notanid/translations", translations ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void replaceTranslations_MissingValue()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'locale':'en', 'property':'name'}]}" )
                    .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `value`.",
            error.getMessage() );
    }

    @Test
    public void replaceTranslations_MissingProperty()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'locale':'en', 'value':'namn 1'}]}" )
                    .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `property`.",
            error.getMessage() );
    }

    @Test
    public void replaceTranslations_MissingLocale()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'property':'name', 'value':'namn 1'}]}" )
                    .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `locale`.",
            error.getMessage() );
    }

    @Test
    public void testUpdateObjectProperty()
    {
        String id = getCurrentUser().getUid();
        assertStatus( HttpStatus.OK,
            PATCH( "/users/" + id + "?importReportMode=ERRORS",
                "[{'op': 'add', 'path': '/firstName', 'value': 'Fancy Mike'}]" ) );

        assertEquals( "Fancy Mike", GET( "/users/{id}", id )
            .content().as( JsonUser.class ).getFirstName() );
    }

    @Test
    public void testPostJsonObject()
    {
        HttpResponse response = POST( "/constants/", "{'name':'answer', 'value': 42}" );
        assertWebMessage( "Created", 201, "OK", null,
            response.content( HttpStatus.CREATED ) );
        assertEquals( "http://localhost/constants/" + assertStatus( HttpStatus.CREATED, response ),
            response.header( "Location" ) );
    }

    @Test
    public void testSetAsFavorite()
    {
        // first we need to create an entity that can be marked as favorite
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        String userId = getCurrentUser().getUid();

        assertWebMessage( "OK", 200, "OK", "Object '" + mapId + "' set as favorite for user 'admin'",
            POST( "/maps/" + mapId + "/favorite" ).content( HttpStatus.OK ) );

        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( singletonList( userId ), map.getFavorites() );
    }

    @Test
    public void testSetAsFavorite_NotFavoritable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be set as favorite",
            POST( "/users/" + getSuperuserUid() + "/favorite" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testSetAsFavorite_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            POST( "/maps/xyz/favorite" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testRemoveAsFavorite()
    {
        // first we need to create an entity that can be marked as favorite
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        // make it a favorite
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/favorite" ) );

        assertWebMessage( "OK", 200, "OK", "Object '" + mapId + "' removed as favorite for user 'admin'",
            DELETE( "/maps/" + mapId + "/favorite" ).content( HttpStatus.OK ) );
        assertEquals( emptyList(), GET( "/maps/{uid}", mapId )
            .content().as( JsonGeoMap.class ).getFavorites() );
    }

    @Test
    public void testRemoveAsFavorite_NotFavoritable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be set as favorite",
            DELETE( "/users/xyz/favorite" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testRemoveAsFavorite_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            DELETE( "/maps/xyz/favorite" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testSubscribe()
    {
        // first we need to create an entity that can be subscribed to
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );

        assertWebMessage( "OK", 200, "OK", "User 'admin' subscribed to object '" + mapId + "'",
            POST( "/maps/" + mapId + "/subscriber" ).content( HttpStatus.OK ) );

        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( singletonList( getCurrentUser().getUid() ), map.getSubscribers() );
    }

    @Test
    public void testSubscribe_NotSubscribable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be subscribed to",
            POST( "/users/" + getSuperuserUid() + "/subscriber" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testSubscribe_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            POST( "/maps/xyz/subscriber" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testUnsubscribe()
    {
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/subscriber" ) );

        assertWebMessage( "OK", 200, "OK", "User 'admin' removed as subscriber of object '" + mapId + "'",
            DELETE( "/maps/" + mapId + "/subscriber" ).content( HttpStatus.OK ) );

        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( emptyList(), map.getSubscribers() );
    }

    @Test
    public void testUnsubscribe_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            DELETE( "/maps/xyz/subscriber" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    public void testUnsubscribe_NotSubscribable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be subscribed to",
            DELETE( "/users/xyz/subscriber" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    public void testPutJsonObject()
    {
        // first the updated entity needs to be created
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        assertWebMessage( "OK", 200, "OK", null,
            PUT( "/organisationUnits/" + ouId, "{'name':'New name', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.OK ) );

        assertEquals( "New name", GET( "/organisationUnits/{id}", ouId )
            .content().as( JsonIdentifiableObject.class ).getName() );
    }

    @Test
    public void testPutJsonObject_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "OrganisationUnit with id xyz could not be found.",
            PUT( "/organisationUnits/xyz", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.NOT_FOUND ) );
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
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        assertWebMessage( "OK", 200, "OK", null, DELETE( "/organisationUnits/" + ouId ).content( HttpStatus.OK ) );
        assertEquals( 0, GET( "/organisationUnits" ).content().getArray( "organisationUnits" ).size() );
    }

    @Test
    public void testDeleteObject_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "OrganisationUnit with id xyz could not be found.",
            DELETE( "/organisationUnits/xyz" ).content( HttpStatus.NOT_FOUND ) );
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
    public void testMergeCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.NO_CONTENT,
            POST( "/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}" ) );
        assertUserGroupHasOnlyUser( groupId, userId );

        User testUser1 = createUser( "test1" );
        User testUser2 = createUser( "test2" );

        // Add 2 new users and remove existing user from the created group
        assertStatus( HttpStatus.NO_CONTENT,
            POST( "/userGroups/" + groupId + "/users",
                "{'additions': [{'id':'" + testUser1.getUid() + "'},{'id':'" + testUser2.getUid() + "'}]" +
                    ",'deletions':[{'id':'" + userId + "'}]}" ) );

        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/", groupId ).content()
            .getList( "users", JsonUser.class );
        assertEquals( 2, usersInGroup.size() );
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

    @Test
    public void testSetSharing_InvalidPublicAccess()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which can be shared
        String programId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}" ) );

        String sharing = "{'owner':'" + userId + "', 'public':'illegal', 'external': true }";
        JsonWebMessage message = PUT( "/programs/" + programId + "/sharing", sharing ).content( HttpStatus.CONFLICT )
            .as( JsonWebMessage.class );
        assertWebMessage( "Conflict", 409, "ERROR",
            "One or more errors occurred, please see full details in import report.", message );
        JsonTypeReport response = message.get( "response", JsonTypeReport.class );
        assertEquals( 1, response.getObjectReports().size() );
        assertEquals( ErrorCode.E3015, response.getObjectReports().get( 0 ).getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    public void testSetSharing_EntityNoFound()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Program with id doesNotExist could not be found.",
            PUT( "/programs/doesNotExist/sharing", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    private void assertUserGroupHasOnlyUser( String groupId, String userId )
    {
        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/users/{itemId}", groupId, userId ).content()
            .getList( "users", JsonUser.class );
        assertEquals( 1, usersInGroup.size() );
        assertEquals( userId, usersInGroup.get( 0 ).getId() );
    }
}
