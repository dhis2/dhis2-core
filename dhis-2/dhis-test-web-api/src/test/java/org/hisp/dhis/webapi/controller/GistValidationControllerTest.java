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

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the validation aspect of Gist API.
 *
 * @author Jan Bernitt
 */
class GistValidationControllerTest extends AbstractGistControllerTest {

  @Test
  void testValidation_Filter_MisplacedArgument() {
    assertEquals(
        "Filter `surname:null:[value]` uses an unary operator and does not need an argument.",
        GET("/users/gist?filter=surname:null:value").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testValidation_Filter_MissingArgument() {
    assertEquals(
        "Filter `surname:eq:[]` uses a binary operator that does need an argument.",
        GET("/users/gist?filter=surname:eq").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testValidation_Filter_TooManyArguments() {
    assertEquals(
        "Filter `surname:gt:[a, b]` can only be used with a single argument.",
        GET("/users/gist?filter=surname:gt:[a,b]").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testValidation_Filter_CanRead_UserDoesNotExist() {
    assertEquals(
        "Filtering by user access in filter `surname:canread:[not-a-UID]` requires permissions to manage the user not-a-UID.",
        GET("/users/gist?filter=surname:canRead:not-a-UID")
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }

  @Test
  void testValidation_Filter_CanRead_NotAuthorized() {
    String uid = getSuperuserUid();
    switchToGuestUser();
    assertEquals(
        "Filtering by user access in filter `surname:canread:[M5zQapPyTZI]` requires permissions to manage the user M5zQapPyTZI.",
        GET("/users/gist?filter=surname:canRead:{id}", uid)
            .error(HttpStatus.FORBIDDEN)
            .getMessage());
  }

  @Test
  void testValidation_Filter_CanAccessMissingPattern() {
    assertEquals(
        "Filter `surname:canaccess:["
            + getSuperuserUid()
            + "]` requires a user ID and an access pattern argument.",
        GET("/users/gist?filter=surname:canAccess").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testValidation_Filter_CanAccessMaliciousPattern() {
    assertEquals(
        "Filter `surname:canaccess:[fake-UID, drop tables]` pattern argument must be 2 to 8 letters allowing letters 'r', 'w', '_' and '%'.",
        GET("/users/gist?filter=surname:canAccess:[fake-UID,drop tables]")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void testValidation_Order_CollectionProperty() {
    assertEquals(
        "Property `userGroup` cannot be used as order property.",
        GET("/users/gist?order=userGroups").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testValidation_Field_UnknownPreset() {
    assertEquals(
        "Field not supported: `:unknown`",
        GET("/organisationUnits/gist?fields=:unknown").error(HttpStatus.CONFLICT).getMessage());
  }

  @Test
  void testValidation_Field_NonPersistentPluck() {
    assertEquals(
        "Property `displayName` cannot be plucked as it is not a persistent field.",
        GET("/users/gist?fields=id,userGroups~pluck(displayName)")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void testValidation_Field_MultiPluck() {
    assertEquals(
        "Property `foo` does not exist in userGroup",
        GET("/users/gist?fields=id,userGroups~pluck(name,foo)")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void testValidation_Access_UserPublicFields() {
    switchToGuestUser();
    JsonObject userLookup =
        GET("/users/{id}/gist?fields=id,code,surname,firstName", getSuperuserUid()).content();
    assertTrue(userLookup.has("id", "code", "surname", "firstName"));
    assertEquals(getSuperuserUid(), userLookup.getString("id").string());
    assertEquals("admin", userLookup.getString("code").string());
    assertEquals("admin", userLookup.getString("surname").string());
    assertEquals("admin", userLookup.getString("firstName").string());
  }

  @Test
  void testValidation_Access_UserPrivateFields() {
    switchToGuestUser();
    String url = "/users/{id}/gist?fields=id,email";
    assertEquals(
        "Field `email` is not readable as user is not allowed to view objects of type User.",
        GET(url, getSuperuserUid()).error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET(url, getSuperuserUid()));
  }

  @Test
  void testValidation_Access_UserPublicFields2() {
    switchToGuestUser();
    assertEquals(
        "admin", GET("/users/{id}/gist?fields=username", getSuperuserUid()).content().string());
  }

  @Test
  void testValidation_Access_UserPrivateFields2() {
    switchToGuestUser();
    String url = "/users/{id}/gist?fields=twoFA,disabled";
    assertEquals(
        "Field `twoFA` is not readable as user is not allowed to view objects of type User.",
        GET(url, getSuperuserUid()).error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET(url, getSuperuserUid()));
  }

  @Test
  void testValidation_Access_CollectionOwnerSharing() {
    JsonObject group = GET("/userGroups/{id}", userGroupId).content();
    String sharing =
        group
            .getObject("sharing")
            .node()
            .extract()
            .member("public")
            .replaceWith("\"--------\"")
            .toString();
    assertStatus(HttpStatus.NO_CONTENT, PUT("/userGroups/" + userGroupId + "/sharing", sharing));
    switchToGuestUser();
    assertEquals(
        "User not allowed to view UserGroup " + userGroupId,
        GET("/userGroups/{id}/users/gist", userGroupId).error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET("/userGroups/{id}/users/gist", userGroupId));
  }
}
