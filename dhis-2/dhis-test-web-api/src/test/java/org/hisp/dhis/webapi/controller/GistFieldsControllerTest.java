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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.attribute.Attribute.ObjectType;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.webapi.json.domain.JsonUserGroup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the {@link org.hisp.dhis.gist.GistQuery.Field} related features of the Gist API.
 *
 * @author Jan Bernitt
 */
class GistFieldsControllerTest extends AbstractGistControllerTest {

  @Test
  void testField_Sharing_EmbedsObject() {
    JsonObject groups =
        GET("/users/{uid}/userGroups/gist?fields=id,sharing,users&headless=true", getSuperuserUid())
            .content()
            .getObject(0);
    assertTrue(groups.has("id", "sharing"));
    assertTrue(
        groups.getObject("sharing").has("owner", "external", "users", "userGroups", "public"));
  }

  @Test
  void testField_Single_List() {
    assertEquals(
        singletonList("groupX"),
        GET("/users/{uid}/userGroups/gist?fields=name&headless=true", getSuperuserUid())
            .content()
            .stringValues());
  }

  @Test
  void testField_Collection_DefaultIsSize() {
    JsonArray matches = GET("/userGroups/gist?fields=name,users&headless=true").content();
    assertEquals(1, matches.size());
    assertEquals(1, matches.getObject(0).getNumber("users").intValue());
    assertTrue(matches.getObject(0).getObject("apiEndpoints").getString("users").exists());
  }

  @Test
  void testField_Single_OwnerObject() {
    assertEquals("admin", GET("/users/{uid}/surname/gist", getSuperuserUid()).content().string());
  }

  @Test
  void testField_Remove_BangSyntax() {
    JsonObject user = GET("/users/{uid}/gist?fields=*,!surname", getSuperuserUid()).content();
    assertFalse(user.has("surname"));
  }

  @Test
  void testField_Remove_MinusSyntax() {
    JsonObject user = GET("/users/{uid}/gist?fields=*,-surname", getSuperuserUid()).content();
    assertFalse(user.has("surname"));
  }

  @Test
  void testField_PresetExpandsToReadableFields() {
    switchToGuestUser();
    JsonArray users = GET("/users/gist?headless=true").content();
    JsonObject user0 = users.getObject(0);
    assertContainsOnly(
        user0.node().members().keySet(), "id", "code", "surname", "firstName", "username");
    switchToSuperuser();
    users = GET("/users/gist?headless=true").content();
    user0 = users.getObject(0);
    assertTrue(user0.size() > 4);
  }

  /*
   * Synthetic Fields
   */
  @Test
  void testField_Href() {
    JsonArray users = GET("/users/gist?fields=id,href&headless=true", getSuperuserUid()).content();
    JsonObject user0 = users.getObject(0);
    assertTrue(user0.has("id", "href"));
    assertEquals(
        "/users/" + user0.getString("id").string() + "/gist", user0.getString("href").string());
  }

  @Test
  void testField_ApiEndpoints_AbsoluteURLs() {
    JsonObject groups =
        GET("/users/{uid}/userGroups/gist?fields=name,users&absoluteUrls=true", getSuperuserUid())
            .content();
    assertTrue(
        groups
            .getArray("userGroups")
            .getObject(0)
            .getObject("apiEndpoints")
            .getString("users")
            .string()
            .startsWith("http://"));
  }

  @Test
  void testField_ApiEndpoints_ContainsOnlyNonEmpty() {
    String noUsersGroupId =
        assertStatus(HttpStatus.CREATED, POST("/userGroups/", "{'name':'groupX', 'users':[]}"));
    JsonObject group = GET("/userGroups/{uid}/gist?fields=name,users", noUsersGroupId).content();
    assertFalse(group.getObject("apiEndpoints").getString("users").exists());
    group = GET("/userGroups/{uid}/gist?fields=name,users::size", noUsersGroupId).content();
    assertFalse(group.getObject("apiEndpoints").getString("users").exists());
    group = GET("/userGroups/{uid}/gist?fields=name,users", userGroupId).content();
    assertTrue(group.getObject("apiEndpoints").getString("users").exists());
    group = GET("/userGroups/{uid}/gist?fields=name,users::size", userGroupId).content();
    assertTrue(group.getObject("apiEndpoints").getString("users").exists());
  }

  @Test
  void testField_DisplayName() {
    JsonObject gist =
        GET("/users/{uid}/userGroups/gist?fields=displayName,id", getSuperuserUid()).content();
    JsonArray groups = gist.getArray("userGroups");
    assertEquals(1, groups.size());
    JsonObject group = groups.getObject(0);
    assertEquals(asList("displayName", "id"), group.names());
    assertEquals("groupX", group.getString("displayName").string());
  }

  @Test
  void testField_DisplayName_WithLocale() {
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/organisationUnits/" + orgUnitId + "/translations",
            "{'translations': ["
                + "{'locale':'sv', 'property':'name', 'value':'enhet A'}, "
                + "{'locale':'de', 'property':'name', 'value':'Einheit A'}]}"));
    JsonString displayName =
        GET("/organisationUnits/{id}/gist?fields=displayName&locale=de&headless=true", orgUnitId)
            .content();
    assertEquals("Einheit A", displayName.string());
    displayName =
        GET("/organisationUnits/{id}/gist?fields=displayName&locale=sv&headless=true", orgUnitId)
            .content();
    assertEquals("enhet A", displayName.string());
  }

  @Test
  void testField_Access() {
    JsonArray groups =
        GET("/users/{uid}/userGroups/gist?fields=id,access&headless=true", getSuperuserUid())
            .content();
    assertEquals(1, groups.size());
    JsonObject group = groups.getObject(0);
    JsonObject access = group.getObject("access");
    assertTrue(access.has("manage", "externalize", "write", "read", "update", "delete"));
    assertTrue(access.getBoolean("manage").booleanValue());
    assertTrue(access.getBoolean("externalize").booleanValue());
    assertTrue(access.getBoolean("write").booleanValue());
    assertTrue(access.getBoolean("read").booleanValue());
    assertTrue(access.getBoolean("update").booleanValue());
    assertTrue(access.getBoolean("delete").booleanValue());
  }

  @Test
  @Disabled("unstable for unknown reason - needs investigation")
  void testField_Attribute() {
    // setup a DE with custom attribute value
    String attrId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/attributes",
                "{"
                    + "'name':'extra', "
                    + "'valueType':'TEXT', "
                    + "'"
                    + ObjectType.DATA_ELEMENT.getPropertyName()
                    + "':true}"));
    String ccId =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0)
            .getString("id")
            .string();
    String deId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements/",
                "{'name':'My data element', 'shortName':'DE1', 'code':'DE1', 'valueType':'INTEGER', "
                    + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                    + "'categoryCombo': {'id': '"
                    + ccId
                    + "'},"
                    + "'attributeValues':[{'attribute': {'id':'"
                    + attrId
                    + "'}, 'value':'extra-value'}]"
                    + "}"));
    // test single field
    assertEquals(
        "extra-value",
        GET("/dataElements/{de}/gist?fields={attr}", deId, attrId).content().string());
    // test multiple fields also with an alias 'extra'
    JsonObject dataElement =
        GET("/dataElements/{de}/gist?fields=id,name,{attr}::rename(extra)", deId, attrId).content();
    assertEquals(deId, dataElement.getString("id").string());
    assertEquals("My data element", dataElement.getString("name").string());
    assertEquals("extra-value", dataElement.getString("extra").string());
    // test in listing
    JsonArray dataElements =
        GET("/dataElements/gist?fields=id,name,{attr}::rename(extra)&headless=true", attrId)
            .content();
    assertEquals(1, dataElements.size());
    assertEquals("extra-value", dataElements.getObject(0).getString("extra").string());
  }

  @Test
  void testField_UserNameAutomaticFromTransformation() {
    JsonArray users = GET("/users/gist?fields=id,name&headless=true").content();
    assertEquals("admin admin", users.getObject(0).getString("name").string());
  }

  @Test
  void testNestedFieldsOfListProperty() {
    JsonArray groups =
        GET("/userGroups/gist?fields=id,name,users[id,username]&headless=true").content();
    assertEquals(1, groups.size());
    JsonUserGroup group = groups.get(0, JsonUserGroup.class);
    JsonArray members = group.getArray("users");
    assertTrue(members.isArray());
    assertEquals(1, members.size());
    JsonObject member = members.getObject(0);
    assertTrue(member.has("id", "username"));
    assertEquals(getSuperuserUid(), member.getString("id").string());
    assertEquals("admin", member.getString("username").string());
  }
}
