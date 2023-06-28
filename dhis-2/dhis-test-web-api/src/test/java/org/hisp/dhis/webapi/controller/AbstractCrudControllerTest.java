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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.web.HttpStatus.Series.SUCCESSFUL;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClient.ContentType;
import static org.hisp.dhis.web.WebClientUtils.assertSeries;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.snippets.SomeUserId;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonError;
import org.hisp.dhis.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.webapi.json.domain.JsonGeoMap;
import org.hisp.dhis.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.webapi.json.domain.JsonStats;
import org.hisp.dhis.webapi.json.domain.JsonTranslation;
import org.hisp.dhis.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Tests the generic operations offered by the {@link AbstractCrudController}
 * using specific endpoints.
 *
 * @author Jan Bernitt
 */
class AbstractCrudControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testGetObjectList()
    {
        JsonList<JsonUser> users = GET( "/users/" ).content( HttpStatus.OK ).getList( "users", JsonUser.class );
        assertEquals( 1, users.size() );
        JsonUser user = users.get( 0 );
        assertEquals( "FirstNameadmin Surnameadmin", user.getDisplayName() );
    }

    @Test
    void testGetObject()
    {
        String id = run( SomeUserId::new );
        JsonUser userById = GET( "/users/{id}", id )
            .content( HttpStatus.OK ).as( JsonUser.class );

        assertTrue( userById.exists() );
        assertEquals( id, userById.getId() );
    }

    @Test
    void testGetObjectProperty()
    {
        // response will look like: { "surname": <name> }
        JsonUser userProperty = GET( "/users/{id}/surname", run( SomeUserId::new ) ).content( HttpStatus.OK )
            .as( JsonUser.class );
        assertEquals( "Surnameadmin", userProperty.getSurname() );
        assertEquals( 1, userProperty.size() );
    }

    @Test
    void testPartialUpdateObject()
    {
        assertStatus( HttpStatus.OK, PATCH( "/users/" + "M5zQapPyTZI" + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/surname', 'value': 'Peter'}]" ) );
        assertEquals( "Peter", GET( "/users/{id}", "M5zQapPyTZI" ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    void testPartialUpdateObjectWithOldPatch()
    {
        assertStatus( HttpStatus.NO_CONTENT, PATCH_OLD( "/users/" + "M5zQapPyTZI",
            "{'surname': 'Peter'}" ) );
        assertEquals( "Peter", GET( "/users/{id}", "M5zQapPyTZI" ).content().as( JsonUser.class ).getSurname() );
    }

    @Test
    void testPartialUpdateObjectNestedObjects()
    {
        assertStatus( HttpStatus.BAD_REQUEST, PATCH_OLD( "/users/" + "M5zQapPyTZI",
            "{'user': {'surname' : 'Peter'}}" ) );
    }

    @Test
    void testPatchRemoveById()
    {
        String ou1 = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit 1', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        String ou2 = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit 2', 'shortName':'OU2', 'openingDate': '2020-01-01'}" ) );

        String dsId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName':'MDS', 'periodType':'Monthly',"
                + "'organisationUnits':[{'id':'" + ou1 + "'},{'id':'" + ou2 + "'}]}" ) );

        JsonObject dataSet = GET( "/dataSets/{id}", dsId ).content();
        assertEquals( 2, dataSet.getArray( "organisationUnits" ).size() );

        assertStatus( HttpStatus.OK, PATCH( "/dataSets/" + dsId,
            "[{'op': 'remove-by-id', 'path': '/organisationUnits', 'id': '" + ou1 + "'}]" ) );
        dataSet = GET( "/dataSets/{id}", dsId ).content();
        assertEquals( 1, dataSet.getArray( "organisationUnits" ).size() );
        assertEquals( ou2,
            dataSet.getArray( "organisationUnits" ).get( 0, JsonObject.class ).getString( "id" ).string() );
    }

    @Test
    void testPartialUpdateObject_Validation()
    {
        String id = run( SomeUserId::new );
        JsonError error = PATCH( "/users/" + id + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/email', 'value': 'Not-valid'}]" ).error();
        assertEquals( "Property `email` requires a valid email address, was given `Not-valid`",
            error.getTypeReport().getErrorReports().get( 0 ).getMessage() );
    }

    @Test
    void replaceTranslationsForNotTranslatableObject()
    {
        String id = getCurrentUser().getUid();
        JsonArray translations = GET( "/users/{id}/translations", id ).content().getArray( "translations" );
        assertTrue( translations.isEmpty() );
        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/users/" + id + "/translations",
                "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn'}]}" )
                .content( HttpStatus.CONFLICT ) );
        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E1107 );
        assertEquals( "Object type `User` is not translatable", error.getMessage() );
    }

    @Test
    void replaceTranslationsOk()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );
        JsonArray translations = GET( "/dataSets/{id}/translations", id )
            .content().getArray( "translations" );

        assertTrue( translations.isEmpty() );

        PUT( "/dataSets/" + id + "/translations",
            "{'translations': [{'locale':'sv', 'property':'name', 'value':'name sv'}]}" )
            .content( HttpStatus.NO_CONTENT );

        GET( "/dataSets/{id}", id ).content();

        translations = GET( "/dataSets/{id}/translations", id ).content().getArray( "translations" );
        assertEquals( 1, translations.size() );
        JsonTranslation translation = translations.get( 0, JsonTranslation.class );
        assertEquals( "sv", translation.getLocale() );
        assertEquals( "name", translation.getProperty() );
        assertEquals( "name sv", translation.getValue() );
    }

    @Test
    void replaceTranslationsWithDuplicateLocales()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );
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
    void replaceTranslations_NoSuchEntity()
    {
        String translations = "{'translations': [{'locale':'sv', 'property':'name'}]}";
        assertWebMessage( "Not Found", 404, "ERROR", "User with id notanid could not be found.",
            PUT( "/users/notanid/translations", translations ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void replaceTranslations_MissingValue()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'locale':'en', 'property':'name'}]}" )
                .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `value`",
            error.getMessage() );
    }

    @Test
    void replaceTranslations_MissingProperty()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'locale':'en', 'value':'namn 1'}]}" )
                .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `property`",
            error.getMessage() );
    }

    @Test
    void replaceTranslations_MissingLocale()
    {
        String id = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT( "/dataSets/" + id + "/translations",
                "{'translations': [{'property':'name', 'value':'namn 1'}]}" )
                .content( HttpStatus.CONFLICT ) );

        JsonErrorReport error = message.find( JsonErrorReport.class,
            report -> report.getErrorCode() == ErrorCode.E4000 );

        assertEquals( "Missing required property `locale`",
            error.getMessage() );
    }

    @Test
    void testPatchObject()
    {
        String id = getCurrentUser().getUid();
        assertStatus( HttpStatus.OK, PATCH( "/users/" + id + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/firstName', 'value': 'Fancy Mike'}]" ) );
        assertEquals( "Fancy Mike", GET( "/users/{id}", id ).content().as( JsonUser.class ).getFirstName() );
    }

    @Test
    void testPatchSharingUserGroups()
    {
        UserGroup userGroupA = createUserGroup( 'A', Set.of() );
        userGroupA.setUid( "th4S6ovwcr8" );
        UserGroup userGroupB = createUserGroup( 'B', Set.of() );
        userGroupB.setUid( "ZoHNWQajIoe" );
        manager.save( userGroupA );
        manager.save( userGroupB );

        String dsId = assertStatus( HttpStatus.CREATED,
            POST( "/dataSets/", "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" ) );

        assertStatus( HttpStatus.OK, PATCH( "/dataSets/" + dsId,
            "[{'op': 'add', 'path': '/sharing/userGroups/th4S6ovwcr8', 'value': { 'access': 'rw------', 'id': 'th4S6ovwcr8' } }]" ) );

        assertStatus( HttpStatus.OK, PATCH( "/dataSets/" + dsId,
            "[{'op': 'add', 'path': '/sharing/userGroups/ZoHNWQajIoe', 'value': { 'access': 'rw------', 'id': 'ZoHNWQajIoe' } }]" ) );

        JsonObject dataSet = GET( "/dataSets/{id}", dsId ).content();
        assertNotNull( dataSet.getObject( "sharing" ).getObject( "userGroups" ).getObject( "th4S6ovwcr8" ) );
        assertNotNull( dataSet.getObject( "sharing" ).getObject( "userGroups" ).getObject( "ZoHNWQajIoe" ) );
    }

    @Test
    void testUpdateObjectProperty()
    {
        String peter = "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'username':'peter47', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
        String peterUserId = assertStatus( HttpStatus.CREATED, POST( "/users", peter ) );
        JsonObject roles = GET( "/userRoles?fields=id" ).content();
        String roleId = roles.getArray( "userRoles" ).getObject( 0 ).getString( "id" ).string();
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + peterUserId ) );
        JsonUser oldPeter = GET( "/users/{id}", peterUserId ).content().as( JsonUser.class );
        assertEquals( "Peter", oldPeter.getFirstName() );
        assertEquals( 1, oldPeter.getArray( "userRoles" ).size() );

        assertStatus( HttpStatus.NO_CONTENT,
            PATCH( "/users/" + peterUserId + "/firstName",
                Body( "{'firstName': 'Fry'}" ), ContentType( MediaType.APPLICATION_JSON ) ) );

        JsonUser newPeter = GET( "/users/{id}", peterUserId ).content().as( JsonUser.class );
        assertEquals( "Fry", newPeter.getFirstName() );
        // are user roles still there?
        assertEquals( 1, newPeter.getArray( "userRoles" ).size() );
    }

    @Test
    void testUpdateObject()
    {
        String peter = "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'username':'peter47', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
        String peterUserId = assertStatus( HttpStatus.CREATED, POST( "/users", peter ) );
        JsonObject roles = GET( "/userRoles?fields=id" ).content();
        String roleId = roles.getArray( "userRoles" ).getObject( 0 ).getString( "id" ).string();
        assertStatus( HttpStatus.NO_CONTENT, POST( "/userRoles/" + roleId + "/users/" + peterUserId ) );
        JsonUser oldPeter = GET( "/users/{id}", peterUserId ).content().as( JsonUser.class );
        assertEquals( "Peter", oldPeter.getFirstName() );
        assertEquals( 1, oldPeter.getArray( "userRoles" ).size() );
        assertStatus( HttpStatus.OK,
            PUT( "/users/" + peterUserId,
                Body( oldPeter.getString( "firstName" ).node().replaceWith( "\"Fry\"" ).getDeclaration() ),
                ContentType( MediaType.APPLICATION_JSON ) ) );
        JsonUser newPeter = GET( "/users/{id}", peterUserId ).content().as( JsonUser.class );
        assertEquals( "Fry", newPeter.getFirstName() );
        // are user roles still there?
        assertEquals( 1, newPeter.getArray( "userRoles" ).size() );
    }

    @Test
    void testPostJsonObject()
    {
        HttpResponse response = POST( "/constants/", "{'name':'answer', 'shortName': 'answer', 'value': 42}" );
        assertWebMessage( "Created", 201, "OK", null, response.content( HttpStatus.CREATED ) );
        assertEquals( "http://localhost/constants/" + assertStatus( HttpStatus.CREATED, response ),
            response.header( "Location" ) );
    }

    @Test
    void testSetAsFavorite()
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
    void testSetAsFavorite_NotFavoritable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be set as favorite",
            POST( "/users/" + getSuperuserUid() + "/favorite" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testSetAsFavorite_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            POST( "/maps/xyz/favorite" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testRemoveAsFavorite()
    {
        // first we need to create an entity that can be marked as favorite
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        // make it a favorite
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/favorite" ) );
        assertWebMessage( "OK", 200, "OK", "Object '" + mapId + "' removed as favorite for user 'admin'",
            DELETE( "/maps/" + mapId + "/favorite" ).content( HttpStatus.OK ) );
        assertEquals( emptyList(), GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class ).getFavorites() );
    }

    @Test
    void testRemoveAsFavorite_NotFavoritable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be set as favorite",
            DELETE( "/users/xyz/favorite" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testRemoveAsFavorite_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            DELETE( "/maps/xyz/favorite" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testSubscribe()
    {
        // first we need to create an entity that can be subscribed to
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        assertWebMessage( "OK", 200, "OK", "User 'admin' subscribed to object '" + mapId + "'",
            POST( "/maps/" + mapId + "/subscriber" ).content( HttpStatus.OK ) );
        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( singletonList( getCurrentUser().getUid() ), map.getSubscribers() );
    }

    @Test
    void testSubscribe_NotSubscribable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be subscribed to",
            POST( "/users/" + getSuperuserUid() + "/subscriber" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testSubscribe_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            POST( "/maps/xyz/subscriber" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testUnsubscribe()
    {
        String mapId = assertStatus( HttpStatus.CREATED, POST( "/maps/", "{'name':'My map'}" ) );
        assertStatus( HttpStatus.OK, POST( "/maps/" + mapId + "/subscriber" ) );
        assertWebMessage( "OK", 200, "OK", "User 'admin' removed as subscriber of object '" + mapId + "'",
            DELETE( "/maps/" + mapId + "/subscriber" ).content( HttpStatus.OK ) );
        JsonGeoMap map = GET( "/maps/{uid}", mapId ).content().as( JsonGeoMap.class );
        assertEquals( emptyList(), map.getSubscribers() );
    }

    @Test
    void testUnsubscribe_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Map with id xyz could not be found.",
            DELETE( "/maps/xyz/subscriber" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testUnsubscribe_NotSubscribable()
    {
        assertWebMessage( "Conflict", 409, "ERROR", "Objects of this class cannot be subscribed to",
            DELETE( "/users/xyz/subscriber" ).content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testPutJsonObject_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "OrganisationUnit with id xyz could not be found.",
            PUT( "/organisationUnits/xyz", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testPutJsonObject_skipTranslations()
    {
        // first the updated entity needs to be created
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'My Group'}" ) );
        assertStatus( HttpStatus.NO_CONTENT,
            PUT( "/userGroups/" + groupId + "/translations",
                "{'translations':[{'property':'NAME','locale':'no','value':'norsk test'},"
                    + "{'property':'DESCRIPTION','locale':'no','value':'norsk test beskrivelse'}]}" ) );
        // verify we have translations
        assertEquals( 2, GET( "/userGroups/{uid}/translations", groupId ).content().getArray( "translations" ).size() );
        // now put object with skipping translations
        assertSeries( SUCCESSFUL, PUT( "/userGroups/" + groupId + "?skipTranslation=true", "{'name':'Europa'}" ) );
        assertEquals( 2, GET( "/userGroups/{uid}/translations", groupId ).content().getArray( "translations" ).size() );
    }

    @Test
    void testPutJsonObject_skipSharing()
    {
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'My Group'}" ) );
        JsonObject group = GET( "/userGroups/{id}", groupId ).content();
        String groupWithoutSharing = group.getObject( "sharing" ).node().replaceWith( "null" ).toString();
        assertStatus( HttpStatus.OK, PUT( "/userGroups/" + groupId + "?skipSharing=true", groupWithoutSharing ) );
        assertEquals( "rw------",
            GET( "/userGroups/{id}", groupId ).content().as( JsonGeoMap.class ).getSharing().getPublic().string() );
    }

    @Test
    void testPutJsonObject_accountExpiry()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();
        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );
        assertStatus( HttpStatus.OK, PUT( "/users/{id}", userId,
            Body( user.node().addMember( "accountExpiry", "null" ).toString() ) ) );
        assertNull(
            GET( "/users/{id}", userId ).content().as( JsonUser.class ).getAccountExpiry() );
    }

    @Test
    void testPutJsonObject_accountExpiry_PutNoChange()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();
        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );
        assertStatus( HttpStatus.OK, PUT( "/users/{id}", userId, Body( user.toString() ) ) );
        assertNull(
            GET( "/users/{id}", userId ).content().as( JsonUser.class ).getAccountExpiry() );
    }

    @Test
    void testPutJsonObject()
    {
        // first the updated entity needs to be created
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        assertWebMessage( "OK", 200, "OK", null,
            PUT( "/organisationUnits/" + ouId, "{'name':'New name', 'shortName':'OU1', 'openingDate': '2020-01-01'}" )
                .content( HttpStatus.OK ) );
        assertEquals( "New name",
            GET( "/organisationUnits/{id}", ouId ).content().as( JsonIdentifiableObject.class ).getName() );
    }

    @Test
    void testPutJsonObject_accountExpiry_NaN()
    {
        String userId = switchToNewUser( "someUser" ).getUid();
        switchToSuperuser();
        JsonUser user = GET( "/users/{id}", userId ).content().as( JsonUser.class );
        String body = user.node().addMember( "accountExpiry", "\"NaN\"" ).toString();
        assertEquals( "Invalid date format 'NaN', only ISO format or UNIX Epoch timestamp is supported.",
            PUT( "/users/{id}", userId, Body( body ) ).error().getMessage() );
    }

    @Test
    void testDeleteObject()
    {
        // first the deleted entity needs to be created
        String ouId = assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );
        assertWebMessage( "OK", 200, "OK", null, DELETE( "/organisationUnits/" + ouId ).content( HttpStatus.OK ) );
        assertEquals( 0, GET( "/organisationUnits" ).content().getArray( "organisationUnits" ).size() );
    }

    @Test
    void testDeleteObject_NoSuchObject()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "OrganisationUnit with id xyz could not be found.",
            DELETE( "/organisationUnits/xyz" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testGetCollectionItem()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        // add an item to the collection
        assertSeries( SUCCESSFUL, POST( "/userGroups/" + groupId + "/users/" + userId ) );
        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    void testAddCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.OK,
            POST( "/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}" ) );
        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    void testMergeCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );

        assertStatus( HttpStatus.OK,
            POST( "/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}" ) );

        assertUserGroupHasOnlyUser( groupId, userId );

        User testUser1 = createAndAddUser( "test1" );
        User testUser2 = createAndAddUser( "test2" );

        // Add 2 new users and remove existing user from the created group
        assertStatus( HttpStatus.OK,
            POST( "/userGroups/" + groupId + "/users",
                "{'additions': [{'id':'" + testUser1.getUid() + "'},{'id':'" + testUser2.getUid() + "'}]" +
                    ",'deletions':[{'id':'" + userId + "'}]}" ) );

        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/", groupId ).content()
            .getList( "users", JsonUser.class );

        assertEquals( 2, usersInGroup.size() );
    }

    @Test
    void testReplaceCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}" ) );
        String peter = "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'username':'peter47', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
        String peterUserId = assertStatus( HttpStatus.CREATED, POST( "/users", peter ) );

        JsonWebMessage message = PUT( "/userGroups/" + groupId + "/users",
            "{'identifiableObjects':[{'id':'" + peterUserId + "'}]}" )
            .content( HttpStatus.OK ).as( JsonWebMessage.class );
        JsonStats stats = message.getResponse().as( JsonTypeReport.class ).getStats();
        assertEquals( 1, stats.getUpdated() );
        assertEquals( 1, stats.getDeleted() );
        assertUserGroupHasOnlyUser( groupId, peterUserId );
    }

    @Test
    void testAddCollectionItem_Owned()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.OK, POST( "/userGroups/{uid}/users/{itemId}", groupId, userId ) );
        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    void testAddCollectionItem_NonOwned()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.OK, POST( "/users/{uid}/userGroups/{itemId}", userId, groupId ) );
        assertUserGroupHasOnlyUser( groupId, userId );
    }

    @Test
    void testDeleteCollectionItem_Owned()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.OK, POST( "/userGroups/{uid}/users/{itemId}", groupId, userId ) );
        assertUserGroupHasOnlyUser( groupId, userId );

        assertStatus( HttpStatus.OK, DELETE( "/userGroups/{uid}/users/{itemId}", groupId, userId ) );
        assertUserGroupHasNoUser( groupId );
    }

    @Test
    void testDeleteCollectionItem_NonOwned()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED, POST( "/userGroups/", "{'name':'testers'}" ) );
        assertStatus( HttpStatus.OK, POST( "/users/{uid}/userGroups/{itemId}", userId, groupId ) );
        assertUserGroupHasOnlyUser( groupId, userId );

        assertStatus( HttpStatus.OK, DELETE( "/users/{uid}/userGroups/{itemId}", userId, groupId ) );
        assertUserGroupHasNoUser( groupId );
    }

    @Test
    void testDeleteCollectionItemsJson()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which has a collection
        String groupId = assertStatus( HttpStatus.CREATED,
            POST( "/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}" ) );
        assertStatus( HttpStatus.OK,
            DELETE( "/userGroups/" + groupId + "/users", "{'identifiableObjects':[{'id':'" + userId + "'}]}" ) );
        assertEquals( 0, GET( "/userGroups/{uid}/users/", groupId ).content().getArray( "users" ).size() );
    }

    @Test
    void testSetSharing()
    {
        String userId = getCurrentUser().getUid();
        // first create an object which can be shared
        String programId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}" ) );
        String sharing = "{'owner':'" + userId + "', 'public':'rwrw----', 'external': true }";
        assertStatus( HttpStatus.NO_CONTENT, PUT( "/programs/" + programId + "/sharing", sharing ) );
        JsonIdentifiableObject program = GET( "/programs/{id}", programId ).content()
            .as( JsonIdentifiableObject.class );
        assertTrue( program.exists() );
        assertEquals( "rwrw----", program.getSharing().getPublic().string() );
        assertFalse( program.getSharing().isExternal(), "programs cannot be external" );
    }

    @Test
    void testSetSharing_InvalidPublicAccess()
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
    void testSharingDisplayName()
    {
        UserGroup userGroup = createUserGroup( 'A', Set.of() );
        manager.save( userGroup );
        String userId = getCurrentUser().getUid();

        String sharing = "{'owner':'" + userId + "', 'public':'rwrw----', 'external': true,'userGroups':{\""
            + userGroup.getUid() + "\":{\"id\":\"" + userGroup.getUid() + "\",\"access\":\"rwrw----\"} } }";

        String programId = assertStatus( HttpStatus.CREATED,
            POST( "/programs/", "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION', 'sharing': "
                + sharing + "}" ) );

        assertStatus( HttpStatus.CREATED,
            POST( "/programStages/", "{\"id\": \"VlhIwWqEHsI\",\n" + "\"sortOrder\": 1," +
                "\"name\": \"test\", \"minDaysFromStart\": \"0\", \"displayGenerateEventBox\": true, \"autoGenerateEvent\": true,"
                +
                "\"program\":" + "{\"id\": \"" + programId + "\"}, \"sharing\": " + sharing + "}" ) );

        JsonIdentifiableObject program = GET( "/programs/{id}?fields=sharing", programId ).content()
            .as( JsonIdentifiableObject.class );
        assertEquals( "UserGroupA",
            program.getSharing().getUserGroups().get( userGroup.getUid() ).getString( "displayName" ).string() );

        JsonIdentifiableObject programStage = GET( "/programs/{id}?fields=programStages[sharing]", programId ).content()
            .as( JsonIdentifiableObject.class );
        assertEquals( "UserGroupA",
            programStage.getList( "programStages", JsonIdentifiableObject.class ).get( 0 ).getSharing().getUserGroups()
                .get( userGroup.getUid() ).getString( "displayName" ).string() );

    }

    @Test
    void testSetSharing_EntityNoFound()
    {
        assertWebMessage( "Not Found", 404, "ERROR", "Program with id doesNotExist could not be found.",
            PUT( "/programs/doesNotExist/sharing", "{}" ).content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testFieldsFilterWithAttributeValues()
    {
        Attribute attribute = createAttribute( 'A' );
        attribute.setDataElementAttribute( true );
        manager.save( attribute );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.getAttributeValues().add( new AttributeValue( "value", attribute ) );
        manager.save( dataElement );

        JsonList<JsonIdentifiableObject> response = GET( "/dataElements?fields=id,name,attributeValues",
            dataElement.getUid() ).content().getList( "dataElements", JsonIdentifiableObject.class );
        assertEquals( attribute.getUid(), response.get( 0 ).getAttributeValues().get( 0 ).getAttribute().getId() );

        response = GET( "/dataElements?fields=id,name,attributeValues[id,attribute[id,name]]", dataElement.getUid() )
            .content().getList( "dataElements", JsonIdentifiableObject.class );
        assertEquals( attribute.getUid(), response.get( 0 ).getAttributeValues().get( 0 ).getAttribute().getId() );
        assertEquals( attribute.getName(), response.get( 0 ).getAttributeValues().get( 0 ).getAttribute().getName() );

    }

    @Test
    void testFieldFilterWithAttribute()
    {
        Attribute attribute = createAttribute( 'A' );
        attribute.setDataElementAttribute( true );
        manager.save( attribute );

        JsonList<JsonIdentifiableObject> response = GET(
            "/attributes?fields=id,name&filter=dataElementAttribute:eq:true" )
            .content().getList( "attributes", JsonIdentifiableObject.class );
        assertEquals( attribute.getUid(), response.get( 0 ).getId() );

        response = GET(
            "/attributes?fields=id,name&filter=userAttribute:eq:true" )
            .content().getList( "attributes", JsonIdentifiableObject.class );
        assertEquals( 0, response.size() );
    }

    @Test
    void testCreateObjectWithInvalidUid()
    {
        JsonImportSummary response = POST( "/dataSets/",
            "{'id':'11111111111','name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" )
            .content( HttpStatus.CONFLICT ).get( "response" ).as( JsonImportSummary.class );
        assertEquals( "Invalid UID `11111111111` for property `DataSet`",
            response.find( JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4014 ).getMessage() );
    }

    @Test
    void testUpdateObjectWithInvalidUid()
    {
        DataSet dataSet = createDataSet( 'A' );
        dataSet.setPeriodType( PeriodType.getPeriodTypeByName( "Monthly" ) );
        dataSet.setUid( "11111111111" );
        manager.save( dataSet );

        PUT( "/dataSets/11111111111",
            "{'id':'11111111111','name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}" )
            .content( HttpStatus.OK );

        JsonIdentifiableObject response = GET( "/dataSets/11111111111" ).content().as( JsonIdentifiableObject.class );
        assertEquals( "My data set", response.getName() );
    }

    @Test
    void testGetOrgUnitCsvWithOpeningDate()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/organisationUnits/", "{'name':'My Unit 1', 'shortName':'OU1', 'openingDate': '2020-01-01'}" ) );

        HttpResponse response = GET(
            "/organisationUnits.csv?fields=id,name,openingDate" );
        assertTrue( response.content( "text" ).contains( "2020-01-01T00:00:00.000" ) );
    }

    private void assertUserGroupHasOnlyUser( String groupId, String userId )
    {
        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/users/", groupId, userId ).content()
            .getList( "users", JsonUser.class );
        assertEquals( 1, usersInGroup.size() );
        assertEquals( userId, usersInGroup.get( 0 ).getId() );
    }

    private void assertUserGroupHasNoUser( String groupId )
    {
        JsonList<JsonUser> usersInGroup = GET( "/userGroups/{uid}/users/", groupId ).content()
            .getList( "users", JsonUser.class );
        assertEquals( 0, usersInGroup.size() );
    }
}
