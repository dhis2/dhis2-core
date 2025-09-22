/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import static org.hisp.dhis.http.HttpAssertions.assertSeries;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.http.HttpStatus.Series.SUCCESSFUL;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonAttributeValue;
import org.hisp.dhis.test.webapi.json.domain.JsonError;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonGeoMap;
import org.hisp.dhis.test.webapi.json.domain.JsonIdentifiableObject;
import org.hisp.dhis.test.webapi.json.domain.JsonImportSummary;
import org.hisp.dhis.test.webapi.json.domain.JsonStats;
import org.hisp.dhis.test.webapi.json.domain.JsonTranslation;
import org.hisp.dhis.test.webapi.json.domain.JsonTypeReport;
import org.hisp.dhis.test.webapi.json.domain.JsonUser;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the generic operations offered by the {@link AbstractCrudController} using specific
 * endpoints.
 *
 * @author Jan Bernitt
 */
@Transactional
class AbstractCrudControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetObjectList() {
    JsonList<JsonUser> users =
        GET("/users/").content(HttpStatus.OK).getList("users", JsonUser.class);
    assertEquals(1, users.size());
    JsonUser user = users.get(0);
    assertStartsWith("First", user.getDisplayName());
  }

  @Test
  void testGetObject() {
    String id = GET("/users/").content().getList("users", JsonUser.class).get(0).getId();
    JsonUser userById = GET("/users/{id}", id).content(HttpStatus.OK).as(JsonUser.class);

    assertTrue(userById.exists());
    assertEquals(id, userById.getId());
  }

  @Test
  void testGetObjectProperty() {
    // response will look like: { "surname": <name> }
    String userId = GET("/users/").content().getList("users", JsonUser.class).get(0).getId();
    JsonUser userProperty =
        GET("/users/{id}/surname", userId).content(HttpStatus.OK).as(JsonUser.class);
    assertStartsWith("Surname", userProperty.getSurname());
    assertEquals(1, userProperty.size());
  }

  @Test
  void testPartialUpdateObject() {
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/" + "M5zQapPyTZI" + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/surname', 'value': 'Peter'}]"));
    assertEquals(
        "Peter", GET("/users/{id}", "M5zQapPyTZI").content().as(JsonUser.class).getSurname());
  }

  @Test
  @DisplayName("Should not return error when adding an item to collection using PATCH api")
  void testPatchCollectionItem() {
    String catOption1 =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions/", "{'name':'CategoryOption1', 'shortName':'CATOPT1'}"));

    String cat =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories/",
                "{'name':'Category', 'shortName':'CAT','dataDimensionType':'DISAGGREGATION','categoryOptions':[{'id':'"
                    + catOption1
                    + "'}]}"));

    String catOption2 =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions/", "{'name':'CategoryOption2', 'shortName':'CATOPT2'}"));

    PATCH(
            "/categories/" + cat,
            "[{'op': 'add', 'path': '/categoryOptions/-', 'value': { 'id': '"
                + catOption2
                + "' } }]")
        .content(HttpStatus.OK);

    JsonObject category = GET("/categories/{id}", cat).content();
    assertEquals(2, category.getArray("categoryOptions").size());
  }

  @Test
  void testPatchRemoveById() {
    String ou1 =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit 1', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    String ou2 =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit 2', 'shortName':'OU2', 'openingDate': '2020-01-01'}"));

    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName':'MDS', 'periodType':'Monthly',"
                    + "'organisationUnits':[{'id':'"
                    + ou1
                    + "'},{'id':'"
                    + ou2
                    + "'}]}"));

    JsonObject dataSet = GET("/dataSets/{id}", dsId).content();
    assertEquals(2, dataSet.getArray("organisationUnits").size());

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/dataSets/" + dsId,
            "[{'op': 'remove-by-id', 'path': '/organisationUnits', 'id': '" + ou1 + "'}]"));
    dataSet = GET("/dataSets/{id}", dsId).content();
    assertEquals(1, dataSet.getArray("organisationUnits").size());
    assertEquals(
        ou2,
        dataSet.getArray("organisationUnits").get(0, JsonObject.class).getString("id").string());
  }

  @Test
  void testPartialUpdateObject_Validation() {
    String id = GET("/users/").content().getList("users", JsonUser.class).get(0).getId();
    JsonError error =
        PATCH(
                "/users/" + id + "?importReportMode=ERRORS",
                "[{'op': 'add', 'path': '/email', 'value': 'Not-valid'}]")
            .error();
    assertEquals(
        "Property `email` requires a valid email address, was given `Not-valid`",
        error.getTypeReport().getErrorReports().get(0).getMessage());
  }

  @Test
  void replaceTranslationsForNotTranslatableObject() {
    String id = getCurrentUser().getUid();
    JsonArray translations = GET("/users/{id}/translations", id).content().getArray("translations");
    assertTrue(translations.isEmpty());
    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/users/" + id + "/translations",
                    "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn'}]}")
                .content(HttpStatus.CONFLICT));
    JsonErrorReport error =
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E1107);
    assertEquals("Object type `User` is not translatable", error.getMessage());
  }

  @Test
  void replaceTranslationsOk() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));
    JsonArray translations =
        GET("/dataSets/{id}/translations", id).content().getArray("translations");

    assertTrue(translations.isEmpty());

    PUT(
            "/dataSets/" + id + "/translations",
            "{'translations': [{'locale':'sv', 'property':'name', 'value':'name sv'}]}")
        .content(HttpStatus.NO_CONTENT);

    GET("/dataSets/{id}", id).content();

    translations = GET("/dataSets/{id}/translations", id).content().getArray("translations");
    assertEquals(1, translations.size());
    JsonTranslation translation = translations.get(0, JsonTranslation.class);
    assertEquals("sv", translation.getLocale());
    assertEquals("name", translation.getProperty());
    assertEquals("name sv", translation.getValue());
  }

  @Test
  void replaceTranslationsWithDuplicateLocales() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));
    JsonArray translations =
        GET("/dataSets/{id}/translations", id).content().getArray("translations");

    assertTrue(translations.isEmpty());

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/dataSets/" + id + "/translations",
                    "{'translations': [{'locale':'sv', 'property':'name', 'value':'namn 1'},{'locale':'sv', 'property':'name', 'value':'namn2'}]}")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport error =
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E1106);
    assertEquals(
        String.format(
            "There are duplicate translation records for property `name` and locale `sv` on DataSet `%s`",
            id),
        error.getMessage());
    assertEquals("name", error.getErrorProperties().get(0));
  }

  @Test
  void replaceTranslations_NoSuchEntity() {
    String translations = "{'translations': [{'locale':'sv', 'property':'name'}]}";
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "User with id notanid could not be found.",
        PUT("/users/notanid/translations", translations).content(HttpStatus.NOT_FOUND));
  }

  @Test
  void replaceTranslations_MissingValue() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/dataSets/" + id + "/translations",
                    "{'translations': [{'locale':'en', 'property':'name'}]}")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport error =
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4000);

    assertEquals("Missing required property `value`", error.getMessage());
  }

  @Test
  void replaceTranslations_MissingProperty() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/dataSets/" + id + "/translations",
                    "{'translations': [{'locale':'en', 'value':'namn 1'}]}")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport error =
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4000);

    assertEquals("Missing required property `property`", error.getMessage());
  }

  @Test
  void replaceTranslations_MissingLocale() {
    String id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    JsonWebMessage message =
        assertWebMessage(
            "Conflict",
            409,
            "WARNING",
            "One or more errors occurred, please see full details in import report.",
            PUT(
                    "/dataSets/" + id + "/translations",
                    "{'translations': [{'property':'name', 'value':'namn 1'}]}")
                .content(HttpStatus.CONFLICT));

    JsonErrorReport error =
        message.find(JsonErrorReport.class, report -> report.getErrorCode() == ErrorCode.E4000);

    assertEquals("Missing required property `locale`", error.getMessage());
  }

  @Test
  void testPatchObject() {
    String id = getCurrentUser().getUid();
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/users/" + id + "?importReportMode=ERRORS",
            "[{'op': 'add', 'path': '/firstName', 'value': 'Fancy Mike'}]"));
    assertEquals("Fancy Mike", GET("/users/{id}", id).content().as(JsonUser.class).getFirstName());
  }

  @Test
  void testPatchSharingUserGroups() {
    UserGroup userGroupA = createUserGroup('A', Set.of());
    userGroupA.setUid("th4S6ovwcr8");
    UserGroup userGroupB = createUserGroup('B', Set.of());
    userGroupB.setUid("ZoHNWQajIoe");
    manager.save(userGroupA);
    manager.save(userGroupB);

    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}"));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/dataSets/" + dsId,
            "[{'op': 'add', 'path': '/sharing/userGroups/th4S6ovwcr8', 'value': { 'access': 'rw------', 'id': 'th4S6ovwcr8' } }]"));

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/dataSets/" + dsId,
            "[{'op': 'add', 'path': '/sharing/userGroups/ZoHNWQajIoe', 'value': { 'access': 'rw------', 'id': 'ZoHNWQajIoe' } }]"));

    JsonObject dataSet = GET("/dataSets/{id}", dsId).content();
    assertNotNull(dataSet.getObject("sharing").getObject("userGroups").getObject("th4S6ovwcr8"));
    assertNotNull(dataSet.getObject("sharing").getObject("userGroups").getObject("ZoHNWQajIoe"));
  }

  @Test
  void testUpdateObject() {
    String peter =
        "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'username':'peter47', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
    String peterUserId = assertStatus(HttpStatus.CREATED, POST("/users", peter));
    JsonObject roles = GET("/userRoles?fields=id").content();
    String roleId = roles.getArray("userRoles").getObject(0).getString("id").string();
    assertStatus(HttpStatus.NO_CONTENT, POST("/userRoles/" + roleId + "/users/" + peterUserId));
    JsonUser oldPeter = GET("/users/{id}", peterUserId).content().as(JsonUser.class);
    assertEquals("Peter", oldPeter.getFirstName());
    assertEquals(1, oldPeter.getArray("userRoles").size());
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/users/" + peterUserId,
            Body(oldPeter.getString("firstName").node().replaceWith("\"Fry\"").getDeclaration()),
            ContentType(MediaType.APPLICATION_JSON)));
    JsonUser newPeter = GET("/users/{id}", peterUserId).content().as(JsonUser.class);
    assertEquals("Fry", newPeter.getFirstName());
    // are user roles still there?
    assertEquals(1, newPeter.getArray("userRoles").size());
  }

  @Test
  void testPostJsonObject() {
    HttpResponse response =
        POST("/constants/", "{'name':'answer', 'shortName': 'answer', 'value': 42}");
    assertWebMessage("Created", 201, "OK", null, response.content(HttpStatus.CREATED));
    assertEquals(
        "http://localhost/api/constants/" + assertStatus(HttpStatus.CREATED, response),
        response.header("Location"));
  }

  @Test
  void testSetAsFavorite() {
    // first we need to create an entity that can be marked as favorite
    String mapId = assertStatus(HttpStatus.CREATED, POST("/maps/", "{'name':'My map'}"));
    String userId = getCurrentUser().getUid();
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Object '" + mapId + "' set as favorite for user 'admin'",
        POST("/maps/" + mapId + "/favorite").content(HttpStatus.OK));
    JsonGeoMap map = GET("/maps/{uid}", mapId).content().as(JsonGeoMap.class);
    assertEquals(singletonList(userId), map.getFavorites());
  }

  @Test
  void testSetAsFavorite_NotFavoritable() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Objects of this class cannot be set as favorite",
        POST("/users/" + getAdminUid() + "/favorite").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSetAsFavorite_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Map with id xyz could not be found.",
        POST("/maps/xyz/favorite").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testRemoveAsFavorite() {
    // first we need to create an entity that can be marked as favorite
    String mapId = assertStatus(HttpStatus.CREATED, POST("/maps/", "{'name':'My map'}"));
    // make it a favorite
    assertStatus(HttpStatus.OK, POST("/maps/" + mapId + "/favorite"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Object '" + mapId + "' removed as favorite for user 'admin'",
        DELETE("/maps/" + mapId + "/favorite").content(HttpStatus.OK));
    assertEquals(
        emptyList(), GET("/maps/{uid}", mapId).content().as(JsonGeoMap.class).getFavorites());
  }

  @Test
  void testRemoveAsFavorite_NotFavoritable() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Objects of this class cannot be set as favorite",
        DELETE("/users/xyz/favorite").content(HttpStatus.CONFLICT));
  }

  @Test
  void testRemoveAsFavorite_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Map with id xyz could not be found.",
        DELETE("/maps/xyz/favorite").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testSubscribe() {
    // first we need to create an entity that can be subscribed to
    String mapId = assertStatus(HttpStatus.CREATED, POST("/maps/", "{'name':'My map'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "User 'admin' subscribed to object '" + mapId + "'",
        POST("/maps/" + mapId + "/subscriber").content(HttpStatus.OK));
    JsonGeoMap map = GET("/maps/{uid}", mapId).content().as(JsonGeoMap.class);
    assertEquals(singletonList(getCurrentUser().getUid()), map.getSubscribers());
  }

  @Test
  void testSubscribe_NotSubscribable() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Objects of this class cannot be subscribed to",
        POST("/users/" + getAdminUid() + "/subscriber").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSubscribe_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Map with id xyz could not be found.",
        POST("/maps/xyz/subscriber").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testUnsubscribe() {
    String mapId = assertStatus(HttpStatus.CREATED, POST("/maps/", "{'name':'My map'}"));
    assertStatus(HttpStatus.OK, POST("/maps/" + mapId + "/subscriber"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "User 'admin' removed as subscriber of object '" + mapId + "'",
        DELETE("/maps/" + mapId + "/subscriber").content(HttpStatus.OK));
    JsonGeoMap map = GET("/maps/{uid}", mapId).content().as(JsonGeoMap.class);
    assertEquals(emptyList(), map.getSubscribers());
  }

  @Test
  void testUnsubscribe_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Map with id xyz could not be found.",
        DELETE("/maps/xyz/subscriber").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testUnsubscribe_NotSubscribable() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Objects of this class cannot be subscribed to",
        DELETE("/users/xyz/subscriber").content(HttpStatus.CONFLICT));
  }

  @Test
  void testPutJsonObject_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "OrganisationUnit with id xyz could not be found.",
        PUT(
                "/organisationUnits/xyz",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}")
            .content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testPutJsonObject_skipTranslations() {
    // first the updated entity needs to be created
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'My Group'}"));
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/userGroups/" + groupId + "/translations",
            "{'translations':[{'property':'NAME','locale':'no','value':'norsk test'},"
                + "{'property':'DESCRIPTION','locale':'no','value':'norsk test beskrivelse'}]}"));
    // verify we have translations
    assertEquals(
        2,
        GET("/userGroups/{uid}/translations", groupId).content().getArray("translations").size());
    // now put object with skipping translations
    assertSeries(
        SUCCESSFUL, PUT("/userGroups/" + groupId + "?skipTranslation=true", "{'name':'Europa'}"));
    assertEquals(
        2,
        GET("/userGroups/{uid}/translations", groupId).content().getArray("translations").size());
  }

  @Test
  void testPutJsonObject_skipSharing() {
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'My Group'}"));
    JsonObject group = GET("/userGroups/{id}", groupId).content();
    String groupWithoutSharing = group.getObject("sharing").node().replaceWith("null").toString();
    assertStatus(
        HttpStatus.OK, PUT("/userGroups/" + groupId + "?skipSharing=true", groupWithoutSharing));
    assertEquals(
        "rw------",
        GET("/userGroups/{id}", groupId)
            .content()
            .as(JsonGeoMap.class)
            .getSharing()
            .getPublic()
            .string());
  }

  @Test
  void testPutJsonObject_accountExpiry() {
    String userId = switchToNewUser("someUser").getUid();
    switchToAdminUser();
    JsonUser user = GET("/users/{id}", userId).content().as(JsonUser.class);
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/users/{id}",
            userId,
            Body(user.node().addMember("accountExpiry", "null").toString())));
    assertNull(GET("/users/{id}", userId).content().as(JsonUser.class).getAccountExpiry());
  }

  @Test
  void testPutJsonObject_accountExpiry_PutNoChange() {
    String userId = switchToNewUser("someUser").getUid();
    switchToAdminUser();
    JsonUser user = GET("/users/{id}", userId).content().as(JsonUser.class);
    assertStatus(HttpStatus.OK, PUT("/users/{id}", userId, Body(user.toString())));
    assertNull(GET("/users/{id}", userId).content().as(JsonUser.class).getAccountExpiry());
  }

  @Test
  void testPutJsonObject() {
    // first the updated entity needs to be created
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        PUT(
                "/organisationUnits/" + ouId,
                "{'name':'New name', 'shortName':'OU1', 'openingDate': '2020-01-01'}")
            .content(HttpStatus.OK));
    assertEquals(
        "New name",
        GET("/organisationUnits/{id}", ouId).content().as(JsonIdentifiableObject.class).getName());
  }

  @Test
  void testPutJsonObject_accountExpiry_NaN() {
    String userId = switchToNewUser("someUser").getUid();
    switchToAdminUser();
    JsonUser user = GET("/users/{id}", userId).content().as(JsonUser.class);
    String body = user.node().addMember("accountExpiry", "\"NaN\"").toString();
    assertEquals(
        "Invalid date format 'NaN', only ISO format or UNIX Epoch timestamp is supported.",
        PUT("/users/{id}", userId, Body(body)).error().getMessage());
  }

  @Test
  void testDeleteObject() {
    // first the deleted entity needs to be created
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    assertWebMessage(
        "OK", 200, "OK", null, DELETE("/organisationUnits/" + ouId).content(HttpStatus.OK));
    assertEquals(0, GET("/organisationUnits").content().getArray("organisationUnits").size());
  }

  @Test
  void testDeleteObject_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "OrganisationUnit with id xyz could not be found.",
        DELETE("/organisationUnits/xyz").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testGetCollectionItem() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));
    // add an item to the collection
    assertSeries(SUCCESSFUL, POST("/userGroups/" + groupId + "/users/" + userId));
    assertUserGroupHasOnlyUser(groupId, userId);
  }

  @Test
  void testAddCollectionItemsJson() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));
    assertStatus(
        HttpStatus.OK,
        POST("/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}"));
    assertUserGroupHasOnlyUser(groupId, userId);
  }

  @Test
  void testMergeCollectionItemsJson() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));

    assertStatus(
        HttpStatus.OK,
        POST("/userGroups/" + groupId + "/users", "{'additions': [{'id':'" + userId + "'}]}"));

    assertUserGroupHasOnlyUser(groupId, userId);

    User testUser1 = createAndAddUser("test1");
    User testUser2 = createAndAddUser("test2");

    manager.flush();
    manager.clear();
    switchToAdminUser();

    // TODO: MAS: This tests fails because it will update the acting user's usergroups and then fail
    // in the test:
    // This should be the only case for using getCurrentUserGroupInfo() in the code.
    // Hence we have to keep it until this is fixed in a separate PR.
    // InternalHibernateGenericStoreImpl.getSharingPredicates()
    // if (userDetails.getUserGroupIds().size() != currentUserGroupInfo.getUserGroupUIDs().size())
    // we need to make sure that user in session is updated when user groups change.

    // Add 2 new users and remove existing user from the created group
    assertStatus(
        HttpStatus.OK,
        POST(
            "/userGroups/" + groupId + "/users",
            "{'additions': [{'id':'"
                + testUser1.getUid()
                + "'},{'id':'"
                + testUser2.getUid()
                + "'}]"
                + ",'deletions':[{'id':'"
                + userId
                + "'}]}"));

    JsonList<JsonUser> usersInGroup =
        GET("/userGroups/{uid}/", groupId).content().getList("users", JsonUser.class);

    assertEquals(2, usersInGroup.size());
  }

  @Test
  void testReplaceCollectionItemsJson() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    manager.flush();
    manager.clear();
    switchToAdminUser();

    // TODO: MAS: This tests fails because it will update the acting user's usergroups and then fail
    // in the test:
    // This should be the only case for using getCurrentUserGroupInfo() in the code.
    // Hence we have to keep it until this is fixed in a separate PR.
    // InternalHibernateGenericStoreImpl.getSharingPredicates()
    // if (userDetails.getUserGroupIds().size() != currentUserGroupInfo.getUserGroupUIDs().size())
    // we need to make sure that user in session is updated when user groups change.

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}"));
    String peter =
        "{'name': 'Peter', 'firstName':'Peter', 'surname':'Pan', 'username':'peter47', 'userRoles': [{'id': 'yrB6vc5Ip3r'}]}";
    String peterUserId = assertStatus(HttpStatus.CREATED, POST("/users", peter));

    JsonWebMessage message =
        PUT(
                "/userGroups/" + groupId + "/users",
                "{'identifiableObjects':[{'id':'" + peterUserId + "'}]}")
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    JsonStats stats = message.getResponse().as(JsonTypeReport.class).getStats();
    assertEquals(1, stats.getUpdated());
    assertEquals(1, stats.getDeleted());
    assertUserGroupHasOnlyUser(groupId, peterUserId);
  }

  @Test
  void testAddCollectionItem_Owned() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));
    assertStatus(HttpStatus.OK, POST("/userGroups/{uid}/users/{itemId}", groupId, userId));
    assertUserGroupHasOnlyUser(groupId, userId);
  }

  @Test
  void testAddCollectionItem_NonOwned() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));
    assertStatus(HttpStatus.OK, POST("/users/{uid}/userGroups/{itemId}", userId, groupId));
    assertUserGroupHasOnlyUser(groupId, userId);
  }

  @Test
  void testDeleteCollectionItem_Owned() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));

    manager.flush();
    manager.clear();
    switchToAdminUser();

    assertStatus(HttpStatus.OK, POST("/userGroups/{uid}/users/{itemId}", groupId, userId));
    assertUserGroupHasOnlyUser(groupId, userId);

    assertStatus(HttpStatus.OK, DELETE("/userGroups/{uid}/users/{itemId}", groupId, userId));
    assertUserGroupHasNoUser(groupId);
  }

  @Test
  void testDeleteCollectionItem_NonOwned() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    String groupId = assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'testers'}"));
    assertStatus(HttpStatus.OK, POST("/users/{uid}/userGroups/{itemId}", userId, groupId));
    assertUserGroupHasOnlyUser(groupId, userId);

    assertStatus(HttpStatus.OK, DELETE("/users/{uid}/userGroups/{itemId}", userId, groupId));

    assertUserGroupHasNoUser(groupId);
  }

  @Test
  void testDeleteCollectionItemsJson() {
    String userId = getCurrentUser().getUid();
    // first create an object which has a collection
    manager.flush();
    manager.clear();
    switchToAdminUser();

    // TODO: MAS: This tests fails because it will update the acting user's usergroups and then fail
    // in the test:
    // This should be the only case for using getCurrentUserGroupInfo() in the code.
    // Hence we have to keep it until this is fixed in a separate PR.
    // InternalHibernateGenericStoreImpl.getSharingPredicates()
    // if (userDetails.getUserGroupIds().size() != currentUserGroupInfo.getUserGroupUIDs().size())
    // we need to make sure that user in session is updated when user groups change.

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/userGroups/", "{'name':'testers', 'users':[{'id':'" + userId + "'}]}"));
    assertStatus(
        HttpStatus.OK,
        DELETE(
            "/userGroups/" + groupId + "/users",
            "{'identifiableObjects':[{'id':'" + userId + "'}]}"));
    assertEquals(0, GET("/userGroups/{uid}/users/", groupId).content().getArray("users").size());
  }

  @Test
  void testSetSharing() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing = "{'owner':'" + userId + "', 'public':'rwrw----', 'external': true }";
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonIdentifiableObject program =
        GET("/programs/{id}", programId).content().as(JsonIdentifiableObject.class);
    assertTrue(program.exists());
    assertEquals("rwrw----", program.getSharing().getPublic().string());
    assertFalse(program.getSharing().isExternal(), "programs cannot be external");
  }

  @Test
  void testSetSharing_InvalidPublicAccess() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing = "{'owner':'" + userId + "', 'public':'illegal', 'external': true }";
    JsonWebMessage message =
        PUT("/programs/" + programId + "/sharing", sharing)
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "One or more errors occurred, please see full details in import report.",
        message);
    JsonTypeReport response = message.get("response", JsonTypeReport.class);
    assertEquals(1, response.getObjectReports().size());
    assertEquals(
        ErrorCode.E3015,
        response.getObjectReports().get(0).getErrorReports().get(0).getErrorCode());
  }

  @Test
  void testSharingDisplayName() {
    UserGroup userGroup = createUserGroup('A', Set.of());
    manager.save(userGroup);
    String userId = getCurrentUser().getUid();

    String sharing =
        "{'owner':'"
            + userId
            + "', 'public':'rwrw----', 'external': true,'userGroups':{\""
            + userGroup.getUid()
            + "\":{\"id\":\""
            + userGroup.getUid()
            + "\",\"access\":\"rwrw----\"} } }";

    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION', 'sharing': "
                    + sharing
                    + "}"));

    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/programStages/",
            "{\"id\": \"VlhIwWqEHsI\",\n"
                + "\"sortOrder\": 1,"
                + "\"name\": \"test\", \"minDaysFromStart\": \"0\", \"displayGenerateEventBox\": true, \"autoGenerateEvent\": true,"
                + "\"program\":"
                + "{\"id\": \""
                + programId
                + "\"}, \"sharing\": "
                + sharing
                + "}"));

    JsonIdentifiableObject program =
        GET("/programs/{id}?fields=sharing", programId).content().as(JsonIdentifiableObject.class);
    assertEquals(
        "UserGroupA",
        program
            .getSharing()
            .getUserGroups()
            .get(userGroup.getUid())
            .getString("displayName")
            .string());

    JsonIdentifiableObject programStage =
        GET("/programs/{id}?fields=programStages[sharing]", programId)
            .content()
            .as(JsonIdentifiableObject.class);
    assertEquals(
        "UserGroupA",
        programStage
            .getList("programStages", JsonIdentifiableObject.class)
            .get(0)
            .getSharing()
            .getUserGroups()
            .get(userGroup.getUid())
            .getString("displayName")
            .string());
  }

  @Test
  void testSetSharing_EntityNoFound() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Program with id doesNotExist could not be found.",
        PUT("/programs/doesNotExist/sharing", "{}").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testFieldsFilterWithAttributeValues() {
    Attribute attribute = createAttribute('A');
    attribute.setDataElementAttribute(true);
    manager.save(attribute);
    DataElement dataElement = createDataElement('A');
    dataElement.addAttributeValue(attribute.getUid(), "value");
    manager.save(dataElement);

    JsonList<JsonIdentifiableObject> response =
        GET("/dataElements?fields=id,name,attributeValues", dataElement.getUid())
            .content()
            .getList("dataElements", JsonIdentifiableObject.class);
    JsonAttributeValue attributeValue0 = response.get(0).getAttributeValues().get(0);
    assertEquals(attribute.getUid(), attributeValue0.getAttribute().getId());

    response =
        GET(
                "/dataElements?fields=id,name,attributeValues[id,attribute[id,name]]",
                dataElement.getUid())
            .content()
            .getList("dataElements", JsonIdentifiableObject.class);
    attributeValue0 = response.get(0).getAttributeValues().get(0);
    assertEquals(attribute.getUid(), attributeValue0.getAttribute().getId());
    assertEquals("AttributeA", attributeValue0.getAttribute().getName());
  }

  @Test
  void testFieldFilterWithAttribute() {
    Attribute attribute = createAttribute('A');
    attribute.setDataElementAttribute(true);
    manager.save(attribute);

    JsonList<JsonIdentifiableObject> response =
        GET("/attributes?fields=id,name&filter=dataElementAttribute:eq:true")
            .content()
            .getList("attributes", JsonIdentifiableObject.class);
    assertEquals(attribute.getUid(), response.get(0).getId());

    response =
        GET("/attributes?fields=id,name&filter=userAttribute:eq:true")
            .content()
            .getList("attributes", JsonIdentifiableObject.class);
    assertEquals(0, response.size());
  }

  @Test
  void testCreateObjectWithInvalidUid() {
    JsonImportSummary response =
        POST(
                "/dataSets/",
                "{'id':'11111111111','name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}")
            .content(HttpStatus.CONFLICT)
            .get("response")
            .as(JsonImportSummary.class);
    assertEquals(
        "Invalid UID `11111111111` for property `DataSet`",
        response
            .find(JsonErrorReport.class, error -> error.getErrorCode() == ErrorCode.E4014)
            .getMessage());
  }

  @Test
  void testUpdateObjectWithInvalidUid() {
    DataSet dataSet = createDataSet('A');
    dataSet.setPeriodType(PeriodType.getPeriodTypeByName("Monthly"));
    dataSet.setUid("11111111111");
    manager.save(dataSet);

    PUT(
            "/dataSets/11111111111",
            "{'id':'11111111111','name':'My data set', 'shortName': 'MDS', 'periodType':'Monthly'}")
        .content(HttpStatus.OK);

    JsonIdentifiableObject response =
        GET("/dataSets/11111111111").content().as(JsonIdentifiableObject.class);
    assertEquals("My data set", response.getName());
  }

  @Test
  void testGetOrgUnitCsvWithOpeningDate() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits/",
            "{'name':'My Unit 1', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));

    HttpResponse response = GET("/organisationUnits.csv?fields=id,name,openingDate");
    assertTrue(response.content("text").contains("2020-01-01T00:00:00.000"));
  }

  @Test
  @DisplayName("Should return dataElements that are part of a dataElementGroup")
  void testRootJunctionOR() {
    DataElement dataElement = createDataElement('A');
    manager.save(dataElement);
    DataElementGroup dataElementGroup = createDataElementGroup('A');
    dataElementGroup.addDataElement(dataElement);
    manager.save(dataElementGroup);

    JsonMixed response =
        GET("/dataElements?filter=dataElementGroups.id:in:[%s]&rootJunction=OR"
                .formatted(dataElementGroup.getUid()))
            .content();
    assertFalse(response.getArray("dataElements").isEmpty());
  }

  @Test
  void testFilterSharingEmptyTrue() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing =
        TextUtils.replace(
            """
      {'owner':'${userId}', 'public':'rwrw----', 'external': true,'users':{},'userGroups':{}}}""",
            Map.of("userId", userId));
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonObject programs =
        GET("/programs?filter=sharing.users:empty", programId).content().as(JsonObject.class);

    assertFalse(programs.get("programs").as(JsonArray.class).isEmpty());
  }

  @Test
  void testFilterSharingEmptyFalse() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing =
        TextUtils.replace(
            """
      {'owner':'${userId}', 'public':'rwrw----', 'external': true,'users':{'${userId}':{'id':'${userId}','access':'rw------'}},'userGroups':{}}}""",
            Map.of("userId", userId));
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonObject programs =
        GET("/programs?filter=sharing.users:empty", programId).content().as(JsonObject.class);
    assertTrue(programs.get("programs").as(JsonArray.class).isEmpty());
  }

  @Test
  void testFilterSharingEqTrue() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing =
        TextUtils.replace(
            """
      {'owner':'${userId}', 'public':'rwrw----', 'external': true,'users':{'${userId}':{'id':'${userId}','access':'rw------'}},'userGroups':{}}}""",
            Map.of("userId", userId));
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonObject programs =
        GET("/programs?filter=sharing.users:eq:2", programId).content().as(JsonObject.class);
    assertTrue(programs.get("programs").as(JsonArray.class).isEmpty());
  }

  @Test
  void testFilterSharingGt() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing =
        TextUtils.replace(
            """
      {'owner':'${userId}', 'public':'rwrw----', 'external': true,'users':{'${userId}':{'id':'${userId}','access':'rw------'}},'userGroups':{}}}""",
            Map.of("userId", userId));
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonObject programs =
        GET("/programs?filter=sharing.users:gt:0", programId).content().as(JsonObject.class);
    assertEquals(1, programs.get("programs").as(JsonArray.class).size());
  }

  @Test
  void testFilterSharingLt() {
    String userId = getCurrentUser().getUid();
    // first create an object which can be shared
    String programId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/programs/",
                "{'name':'test', 'shortName':'test', 'programType':'WITHOUT_REGISTRATION'}"));
    String sharing =
        TextUtils.replace(
            """
      {'owner':'${userId}', 'public':'rwrw----', 'external': true,'users':{'${userId}':{'id':'${userId}','access':'rw------'}},'userGroups':{}}}""",
            Map.of("userId", userId));
    assertStatus(HttpStatus.NO_CONTENT, PUT("/programs/" + programId + "/sharing", sharing));
    JsonObject programs =
        GET("/programs?filter=sharing.users:lt:2", programId).content().as(JsonObject.class);
    assertEquals(1, programs.get("programs").as(JsonArray.class).size());
  }

  @Test
  void testPostObject_MandatoryAttributeNoValue() {
    String attr =
        "{'name':'USER', 'valueType':'TRUE_ONLY', 'userAttribute':true, 'mandatory':true}";
    String attrId = assertStatus(HttpStatus.CREATED, POST("/attributes", attr));
    // language=JSON5
    String user =
        """
        {
          "username": "testMandatoryAttribute",
          "password": "-hu@_ka9$P",
          "firstName": "testMandatoryAttribute",
          "surname": "tester",
          "userRoles":[{ "id": "yrB6vc5Ip3r" }],
          "attributeValues": [{ "attribute": { "id": "%s" }, "value": "" } ]
        }
        """;
    assertErrorMandatoryAttributeRequired(attrId, POST("/users", user.formatted(attrId)));
  }

  @Test
  void testPostObject_MandatoryAttributeNoAttribute() {
    String attr =
        "{'name':'USER', 'valueType':'TRUE_ONLY', 'userAttribute':true, 'mandatory':true}";
    String attrId = assertStatus(HttpStatus.CREATED, POST("/attributes", attr));
    String user =
        """
        {
          "username": "testMandatoryAttribute",
          "password": "-hu@_ka9$P",
          "firstName": "testMandatoryAttribute",
          "surname": "tester",
          "userRoles":[{ "id": "yrB6vc5Ip3r" }]
        }
        """;
    assertErrorMandatoryAttributeRequired(attrId, POST("/users", user));
  }

  @Test
  void testSortIAscending() {
    POST(
            "/categories/",
            "{'name':'Child Health', 'shortName':'CAT1','dataDimensionType':'DISAGGREGATION'}")
        .content(HttpStatus.CREATED);
    POST(
            "/categories/",
            "{'name':'births attended by', 'shortName':'CAT2','dataDimensionType':'DISAGGREGATION'}")
        .content(HttpStatus.CREATED);

    JsonList<JsonIdentifiableObject> response =
        GET("/categories?order=name:iasc")
            .content()
            .getList("categories", JsonIdentifiableObject.class);
    assertEquals("births attended by", response.get(0).getDisplayName());
    assertEquals("Child Health", response.get(1).getDisplayName());
  }

  @Test
  void testSortNoneTextCaseInsensitive() {
    POST(
            "/categories/",
            "{'name':'Child Health', 'shortName':'CAT1','dataDimensionType':'DISAGGREGATION','lastUpdated':'2017-05-19T15:13:52.488'}")
        .content(HttpStatus.CREATED);
    POST(
            "/categories/",
            "{'name':'births attended by', 'shortName':'CAT2','dataDimensionType':'DISAGGREGATION', 'lastUpdated':'2017-05-19T15:14:52.488'}")
        .content(HttpStatus.CREATED);

    JsonList<JsonIdentifiableObject> response =
        GET("/categories?order=created:idesc")
            .content()
            .getList("categories", JsonIdentifiableObject.class);
    assertEquals("births attended by", response.get(0).getDisplayName());
    assertEquals("Child Health", response.get(1).getDisplayName());
  }

  @Test
  void testCreateCategoryOption() {
    // First create an organisation unit to reference from the category option
    String ouId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                "{"
                    + "'name':'OU A',"
                    + "'shortName':'OUA',"
                    + "'openingDate':'2020-01-01'"
                    + "}"));

    // Create category option with various scalar properties and the organisationUnits collection
    String coId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptions",
                "{"
                    + "'name':'CO A',"
                    + "'shortName':'COA',"
                    + "'code':'C-A',"
                    + "'description':'A category option',"
                    + "'formName':'Form A',"
                    + "'organisationUnits':[{'id':'"
                    + ouId
                    + "'}]"
                    + "}"));

    // Fetch and verify all properties including collections
    JsonObject co = GET("/categoryOptions/" + coId).content(HttpStatus.OK).as(JsonObject.class);

    assertEquals("CO A", co.getString("displayName").string());
    assertEquals("COA", co.getString("shortName").string());
    assertEquals("C-A", co.getString("code").string());
    assertEquals("A category option", co.getString("description").string());
    assertEquals("Form A", co.getString("formName").string());

    // organisationUnits collection should contain the created OU
    JsonArray ouArr = co.getArray("organisationUnits");
    assertNotNull(ouArr);
    assertEquals(1, ouArr.size());
    assertEquals(ouId, ouArr.getObject(0).getString("id").string());

    // Other collections should be present and empty upon creation
    assertEquals(0, co.getArray("categories").size());
    assertEquals(0, co.getArray("categoryOptionCombos").size());
    // property name for groups is categoryOptionGroups in JSON
    assertEquals(0, co.getArray("categoryOptionGroups").size());
  }

  @Test
  void testCategoryOptionCategoriesPopulatedAfterLinkingCategory() {
    // Create a category option
    String coId =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name':'CO B', 'shortName':'COB' }"));

    // Create a category that includes the created category option
    String catId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name':'Cat A', 'shortName':'CatA', 'dataDimensionType':'DISAGGREGATION',"
                    + " 'categoryOptions': [ { 'id': '"
                    + coId
                    + "' } ] }"));

    // Verify the category option now shows the category in its categories collection
    JsonObject co = GET("/categoryOptions/" + coId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray cats = co.getArray("categories");
    assertNotNull(cats);
    assertEquals(1, cats.size());
    assertEquals(catId, cats.getObject(0).getString("id").string());
  }

  @Test
  void testCategoryOptionCombosPopulatedAfterCreatingCategoryCombo() {
    // Create a category option
    String coId =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name':'CO C', 'shortName':'COC' }"));

    // Create a category that includes the created category option
    String catId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name':'Cat B', 'shortName':'CatB', 'dataDimensionType':'DISAGGREGATION',"
                    + " 'categoryOptions': [ { 'id': '"
                    + coId
                    + "' } ] }"));

    // Create a category combo that includes the category
    String ccId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                "{ 'name':'CC B', 'dataDimensionType':'DISAGGREGATION', 'categories': [ { 'id': '"
                    + catId
                    + "' } ] }"));

    // Read the category combo to get the generated category option combo id
    JsonObject cc = GET("/categoryCombos/" + ccId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray comboCocs = cc.getArray("categoryOptionCombos");
    assertNotNull(comboCocs);
    assertTrue(comboCocs.size() >= 1);
    String expectedCocId = comboCocs.getObject(0).getString("id").string();

    // Verify the category option now shows the related COC in its categoryOptionCombos collection
    JsonObject co = GET("/categoryOptions/" + coId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray coCocs = co.getArray("categoryOptionCombos");
    assertNotNull(coCocs);
    assertTrue(coCocs.size() >= 1);
    boolean contains = false;
    for (int i = 0; i < coCocs.size(); i++) {
      if (expectedCocId.equals(coCocs.getObject(i).getString("id").string())) {
        contains = true;
        break;
      }
    }
    assertTrue(contains);
  }

  @Test
  void testCategoryOptionGroupsPopulatedAfterLinkingGroup() {
    // Create a category option
    String coId =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name':'CO D', 'shortName':'COD' }"));

    // Create a category option group that includes the category option as a member
    String cogId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryOptionGroups",
                "{ 'name':'Group A', 'shortName':'GroupA', 'dataDimensionType':'DISAGGREGATION',"
                    + " 'categoryOptions': [ { 'id': '"
                    + coId
                    + "' } ] }"));

    // Verify the category option now shows the group in its categoryOptionGroups collection
    JsonObject co = GET("/categoryOptions/" + coId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray groups = co.getArray("categoryOptionGroups");
    assertNotNull(groups);
    assertEquals(1, groups.size());
    assertEquals(cogId, groups.getObject(0).getString("id").string());
  }

  private void assertErrorMandatoryAttributeRequired(String attrId, HttpResponse response) {
    JsonError msg = response.content(HttpStatus.CONFLICT).as(JsonError.class);
    JsonList<JsonErrorReport> errorReports = msg.getTypeReport().getErrorReports();
    assertEquals(1, errorReports.size());
    JsonErrorReport error = errorReports.get(0);
    assertEquals(ErrorCode.E4011, error.getErrorCode());
    assertEquals(List.of(attrId), error.getErrorProperties());
  }

  private void assertUserGroupHasOnlyUser(String groupId, String userId) {
    manager.flush();
    manager.clear();
    switchToAdminUser();

    JsonList<JsonUser> usersInGroup =
        GET("/userGroups/{uid}/users/", groupId, userId).content().getList("users", JsonUser.class);
    assertEquals(1, usersInGroup.size());
    assertEquals(userId, usersInGroup.get(0).getId());
  }

  private void assertUserGroupHasNoUser(String groupId) {
    manager.flush();
    manager.clear();
    switchToAdminUser();

    JsonList<JsonUser> usersInGroup =
        GET("/userGroups/{uid}/users/", groupId).content().getList("users", JsonUser.class);
    assertEquals(0, usersInGroup.size());
  }

  // -------------------------------------------------------------------------
  // Section tests
  // -------------------------------------------------------------------------

  @Test
  void testSectionCanBeCreatedSuccessfully() {
    // First create a DataSet (required for Section)
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test DataSet',
                'shortName': 'TDS',
                'periodType': 'Monthly'
            }
            """));

    // Create a Section
    String sectionId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sections/",
                """
            {
                'name': 'Test Section',
                'description': 'A test section',
                'dataSet': {
                    'id': '%s'
                },
                'sortOrder': 1
            }
            """
                    .formatted(dataSetId)));

    // Verify the section was created correctly
    JsonObject section = GET("/sections/" + sectionId).content(HttpStatus.OK).as(JsonObject.class);
    assertNotNull(section);
    assertEquals("Test Section", section.getString("name").string());
    assertEquals("A test section", section.getString("description").string());
    assertEquals(1, section.getNumber("sortOrder").intValue());
    assertEquals(dataSetId, section.getObject("dataSet").getString("id").string());
  }

  @Test
  void testSectionCanBeCreatedWithDataElements() {
    // Create DataSet
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test DataSet',
                'shortName': 'TDS',
                'periodType': 'Monthly'
            }
            """));

    // Create DataElements
    String de1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements/",
                """
            {
                'name': 'Data Element 1',
                'shortName': 'DE1',
                'valueType': 'TEXT',
                'domainType': 'AGGREGATE',
                'aggregationType': 'SUM'
            }
            """));

    String de2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements/",
                """
            {
                'name': 'Data Element 2',
                'shortName': 'DE2',
                'valueType': 'TEXT',
                'domainType': 'AGGREGATE',
                'aggregationType': 'SUM'
            }
            """));

    // Create Section with DataElements
    String sectionId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sections/",
                """
            {
                'name': 'Test Section with DEs',
                'dataSet': {
                    'id': '%s'
                },
                'dataElements': [
                    {'id': '%s'},
                    {'id': '%s'}
                ],
                'sortOrder': 1
            }
            """
                    .formatted(dataSetId, de1Id, de2Id)));

    // Verify section has the data elements
    JsonObject section = GET("/sections/" + sectionId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray dataElements = section.getArray("dataElements");
    assertNotNull(dataElements);
    assertEquals(2, dataElements.size());
  }

  @Test
  void testSectionCanBeCreatedWithDisplayOptions() {
    // Create DataSet
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test DataSet',
                'shortName': 'TDS',
                'periodType': 'Monthly'
            }
            """));

    // Create Section with display options
    String sectionId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sections/",
                """
            {
                'name': 'Section with Display Options',
                'dataSet': {
                    'id': '%s'
                },
                'showRowTotals': true,
                'showColumnTotals': false,
                'disableDataElementAutoGroup': true,
                'sortOrder': 1
            }
            """
                    .formatted(dataSetId)));

    // Verify display options
    JsonObject section = GET("/sections/" + sectionId).content(HttpStatus.OK).as(JsonObject.class);
    assertTrue(section.getBoolean("showRowTotals").booleanValue());
    assertFalse(section.getBoolean("showColumnTotals").booleanValue());
    assertTrue(section.getBoolean("disableDataElementAutoGroup").booleanValue());
  }

  @Test
  void testSectionSupportsAttributeValues() {
    // Create attribute first
    String attributeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/attributes/",
                """
            {
                'name': 'Test Attribute',
                'shortName': 'TA',
                'valueType': 'TEXT',
                'sectionAttribute': true
            }
            """));

    // Create DataSet
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test DataSet',
                'shortName': 'TDS',
                'periodType': 'Monthly'
            }
            """));

    // Create Section with attribute value
    String sectionId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sections/",
                """
            {
                'name': 'Section with Attributes',
                'code': 'SEC001',
                'dataSet': {
                    'id': '%s'
                },
                'attributeValues': [
                    {
                        'attribute': {'id': '%s'},
                        'value': 'test attribute value'
                    }
                ],
                'sortOrder': 1
            }
            """
                    .formatted(dataSetId, attributeId)));

    // Verify attribute values and code
    JsonObject section = GET("/sections/" + sectionId).content(HttpStatus.OK).as(JsonObject.class);
    assertEquals("SEC001", section.getString("code").string());

    JsonArray attributeValues = section.getArray("attributeValues");
    assertNotNull(attributeValues);
    assertEquals(1, attributeValues.size());
    assertEquals("test attribute value", attributeValues.getObject(0).getString("value").string());
  }

  @Test
  void testSectionCanBeCreatedWithIndicators() {
    // Create DataSet
    String dataSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                """
            {
                'name': 'Test DataSet',
                'shortName': 'TDS',
                'periodType': 'Monthly'
            }
            """));

    // Create IndicatorType first
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes/",
                """
            {
                'name': 'Test Indicator Type',
                'factor': 100
            }
            """));

    // Create Indicator
    String indicatorId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Test Indicator',
                'shortName': 'TI',
                'indicatorType': {
                    'id': '%s'
                },
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    // Create Section with indicators
    String sectionId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/sections/",
                """
            {
                'name': 'Section with Indicators',
                'description': 'Section with indicators',
                'dataSet': {
                    'id': '%s'
                },
                'indicators': [
                    {'id': '%s'}
                ],
                'sortOrder': 2
            }
            """
                    .formatted(dataSetId, indicatorId)));

    // Verify the section with indicators
    JsonObject section = GET("/sections/" + sectionId).content(HttpStatus.OK).as(JsonObject.class);
    assertEquals("Section with Indicators", section.getString("name").string());

    JsonArray indicators = section.getArray("indicators");
    assertNotNull(indicators);
    assertEquals(1, indicators.size());
    assertEquals(indicatorId, indicators.getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("Test creating IndicatorGroup with Indicators and bi-directional relationship")
  void testCreateIndicatorGroupWithIndicators() {
    // Create IndicatorType first
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes/",
                """
            {
                'name': 'Test Indicator Type',
                'factor': 100
            }
            """));

    // Create Indicators
    String indicator1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Test Indicator 1',
                'shortName': 'TI1',
                'indicatorType': {
                    'id': '%s'
                },
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    String indicator2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Test Indicator 2',
                'shortName': 'TI2',
                'indicatorType': {
                    'id': '%s'
                },
                'numerator': '2',
                'denominator': '2'
            }
            """
                    .formatted(indicatorTypeId)));

    // Create IndicatorGroup with indicators (members collection)
    String indicatorGroupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Test Indicator Group',
                'shortName': 'TIG',
                'indicators': [
                    {'id': '%s'},
                    {'id': '%s'}
                ]
            }
            """
                    .formatted(indicator1Id, indicator2Id)));

    // Verify the indicator group was created with the indicators
    JsonObject indicatorGroup =
        GET("/indicatorGroups/" + indicatorGroupId).content(HttpStatus.OK).as(JsonObject.class);
    assertNotNull(indicatorGroup);
    assertEquals("Test Indicator Group", indicatorGroup.getString("name").string());

    // Verify indicators collection (members mapping)
    JsonArray indicators = indicatorGroup.getArray("indicators");
    assertNotNull(indicators);
    assertEquals(2, indicators.size());

    // Verify bi-directional relationship
    JsonObject indicator1 = GET("/indicators/" + indicator1Id).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray indicator1Groups = indicator1.getArray("indicatorGroups");
    assertNotNull(indicator1Groups);
    assertEquals(1, indicator1Groups.size());
    assertEquals(indicatorGroupId, indicator1Groups.getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("Test adding/removing IndicatorGroup members using PATCH")
  void testIndicatorGroupMembersWithPATCH() {
    // Create IndicatorType
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorTypes/",
                """
            {
                'name': 'Test Type',
                'factor': 1
            }
            """));

    // Create three indicators
    String indicator1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Indicator 1',
                'shortName': 'I1',
                'indicatorType': {'id': '%s'},
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    String indicator2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Indicator 2',
                'shortName': 'I2',
                'indicatorType': {'id': '%s'},
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    String indicator3Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Indicator 3',
                'shortName': 'I3',
                'indicatorType': {'id': '%s'},
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    // Create IndicatorGroup with initial indicators
    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Dynamic Group',
                'indicators': [
                    {'id': '%s'},
                    {'id': '%s'}
                ]
            }
            """
                    .formatted(indicator1Id, indicator2Id)));

    // Test adding an indicator via PATCH
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/indicatorGroups/" + groupId,
            "[{'op': 'add', 'path': '/indicators/-', 'value': { 'id': '" + indicator3Id + "' } }]"));

    // Verify all three indicators are now in the group
    JsonObject group = GET("/indicatorGroups/" + groupId).content().as(JsonObject.class);
    assertEquals(3, group.getArray("indicators").size());

    // Test removing an indicator via PATCH remove-by-id
    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/indicatorGroups/" + groupId,
            "[{'op': 'remove-by-id', 'path': '/indicators', 'id': '" + indicator1Id + "'}]"));

    // Verify only two indicators remain
    group = GET("/indicatorGroups/" + groupId).content().as(JsonObject.class);
    assertEquals(2, group.getArray("indicators").size());

    // Verify the correct indicators are present
    JsonArray remainingIndicators = group.getArray("indicators");
    List<String> remainingIds = List.of(
        remainingIndicators.getObject(0).getString("id").string(),
        remainingIndicators.getObject(1).getString("id").string());
    assertTrue(remainingIds.contains(indicator2Id));
    assertTrue(remainingIds.contains(indicator3Id));
    assertFalse(remainingIds.contains(indicator1Id));
  }

  @Test
  @DisplayName("Test IndicatorGroupSets with IndicatorGroups and bi-directional relationship")
  void testCreateIndicatorGroupSetsWithIndicators() {
    // Create two IndicatorGroups
    String group1Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Group 1',
                'shortName': 'G1'
            }
            """));

    String group2Id =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Group 2',
                'shortName': 'G2'
            }
            """));

    // Create IndicatorGroupSet that includes both groups
    String groupSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroupSets/",
                """
            {
                'name': 'Test Group Set',
                'shortName': 'TGS',
                'indicatorGroups': [
                    {'id': '%s'},
                    {'id': '%s'}
                ]
            }
            """
                    .formatted(group1Id, group2Id)));

    // Verify the groups are in the group set
    JsonObject groupSet = GET("/indicatorGroupSets/" + groupSetId).content(HttpStatus.OK).as(JsonObject.class);
    JsonArray groupsInSet = groupSet.getArray("indicatorGroups");
    assertNotNull(groupsInSet);
    assertEquals(2, groupsInSet.size());

    // Verify inverse relationship - groups should reference the group set
    JsonObject group1 = GET("/indicatorGroups/" + group1Id).content().as(JsonObject.class);
    JsonArray group1Sets = group1.getArray("groupSets");
    assertNotNull(group1Sets);
    assertEquals(1, group1Sets.size());
    assertEquals(groupSetId, group1Sets.getObject(0).getString("id").string());

    JsonObject group2 = GET("/indicatorGroups/" + group2Id).content().as(JsonObject.class);
    JsonArray group2Sets = group2.getArray("groupSets");
    assertNotNull(group2Sets);
    assertEquals(1, group2Sets.size());
    assertEquals(groupSetId, group2Sets.getObject(0).getString("id").string());
  }

  @Test
  @DisplayName("Should create IndicatorGroup with Indicators and verify lazy loading of collections")
  void testIndicatorGroupWithIndicators() {
    String indicatorTypeId =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes/", "{'name': 'Basic Type', 'factor': 1}"));

    String indicatorId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicators/",
                """
            {
                'name': 'Basic Indicator',
                'shortName': 'BI',
                'indicatorType': {'id': '%s'},
                'numerator': '1',
                'denominator': '1'
            }
            """
                    .formatted(indicatorTypeId)));

    String groupId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroups/",
                """
            {
                'name': 'Lazy Test Group',
                'indicators': [{'id': '%s'}]
            }
            """
                    .formatted(indicatorId)));

    String groupSetId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/indicatorGroupSets/",
                """
            {
                'name': 'Lazy Test Group Set',
                'indicatorGroups': [{'id': '%s'}]
            }
            """
                    .formatted(groupId)));

    JsonObject group = GET("/indicatorGroups/" + groupId).content().as(JsonObject.class);

    // Test members collection accessibility
    JsonArray indicators = group.getArray("indicators");
    assertNotNull(indicators);
    assertEquals(1, indicators.size());

    // Test groupSets collection accessibility
    JsonArray groupSets = group.getArray("groupSets");
    assertNotNull(groupSets);
    assertEquals(1, groupSets.size());

    // Verify the relationships persist correctly
    assertEquals(indicatorId, indicators.getObject(0).getString("id").string());
    assertEquals(groupSetId, groupSets.getObject(0).getString("id").string());
  }
}
