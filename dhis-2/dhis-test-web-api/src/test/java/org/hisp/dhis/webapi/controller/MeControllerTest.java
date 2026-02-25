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

import static java.util.Collections.singletonList;
import static org.hisp.dhis.http.HttpAssertions.assertSeries;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.http.HttpStatus.Series;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;
import org.hisp.dhis.security.apikey.ApiTokenStore;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonMeDto;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.user.MeController} API.
 *
 * @author Jan Bernitt
 */
@Transactional
class MeControllerTest extends H2ControllerIntegrationTestBase {
  private User userA;

  @Autowired private ApiTokenStore apiTokenStore;

  @Autowired private FileResourceService fileResourceService;

  @BeforeEach
  void setUp() {
    userA = createUserWithAuth("userA", "ALL");

    switchContextToUser(userA);
  }

  @Test
  void testGetCurrentUser() {
    switchToAdminUser();
    assertEquals(getCurrentUser().getUid(), GET("/me").content().as(JsonMeDto.class).getId());
  }

  @Test
  void testGetCurrentUser_Fields() {
    JsonMeDto me =
        GET("/me?fields=name,settings[keyUiCustomColorMobile]").content().as(JsonMeDto.class);
    assertEquals(2, me.size());
    assertEquals(Set.of("name", "settings"), Set.copyOf(me.names()));
    JsonMap<JsonMixed> settings = me.getSettings();
    assertEquals(1, settings.size());
    assertEquals(List.of("keyUiCustomColorMobile"), settings.names());
  }

  @Test
  void testGetCurrentUserDataApprovalWorkflows() {
    JsonArray workflows =
        GET("/me/dataApprovalWorkflows").content().getArray("dataApprovalWorkflows");
    assertTrue(workflows.isArray());
    assertTrue(workflows.isEmpty());
  }

  @Test
  void testGetAuthorities() {
    assertEquals(
        singletonList("ALL"), GET("/me/authorities").content(HttpStatus.OK).stringValues());
  }

  @Test
  void testUpdateCurrentUser() {
    assertSeries(Series.SUCCESSFUL, PUT("/me", "{'surname':'Lars'}"));
    assertEquals("Lars", GET("/me").content().as(JsonMeDto.class).getSurname());
  }

  @Test
  void testHasAuthority() {
    assertTrue(GET("/me/authorities/ALL").content(HttpStatus.OK).booleanValue());
    // with no authorities
    switchToNewUser("Kalle");
    assertFalse(GET("/me/authorities/missing").content(HttpStatus.OK).booleanValue());
  }

  @Test
  void testGetEmailVerifiedProperty() {
    assertFalse(GET("/me").content().as(JsonMeDto.class).getEmailVerified());
  }

  @Test
  void testGetSettings() {
    JsonObject settings = GET("/me/settings").content(HttpStatus.OK);
    assertTrue(settings.isObject());
    assertFalse(settings.isEmpty());
    assertTrue(settings.get("keyMessageSmsNotification").exists());
    assertEquals("en", settings.getString("keyUiLocale").string());
  }

  @Test
  void testGetSettings_ByKey() {
    JsonObject settings =
        GET("/me/settings?key=keyUiCustomColorMobile&key=keyAnalysisDisplayProperty")
            .content(HttpStatus.OK);
    assertTrue(settings.isObject());
    assertEquals(2, settings.size());
    assertEquals(
        Set.of("keyUiCustomColorMobile", "keyAnalysisDisplayProperty"),
        Set.copyOf(settings.names()));
  }

  @Test
  void testGetSetting() {
    assertEquals("en", GET("/me/settings/{key}", "keyUiLocale").content(HttpStatus.OK).string());
  }

  @Test
  void testChangePassword() {
    assertStatus(
        HttpStatus.ACCEPTED,
        PUT("/me/changePassword", "{'oldPassword':'district','newPassword':'$ecrEt42'}"));
  }

  @Test
  void testChangePassword_WrongNew() {
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
    assertEquals(
        "Password must have at least 8, and at most 72 characters",
        PUT("/me/changePassword", "{'oldPassword':'district','newPassword':'secret'}")
            .error(Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testChangePassword_WrongOld() {
    assertEquals(
        "OldPassword is incorrect",
        PUT("/me/changePassword", "{'oldPassword':'wrong','newPassword':'secret'}")
            .error(Series.CLIENT_ERROR)
            .getMessage());
  }

  @Test
  void testVerifyPasswordText() {
    assertTrue(
        POST("/me/verifyPassword", ContentType("text/plain"), Body("district"))
            .content()
            .getBoolean("isCorrectPassword")
            .booleanValue());
    assertFalse(
        POST("/me/verifyPassword", ContentType("text/plain"), Body("wrong"))
            .content()
            .getBoolean("isCorrectPassword")
            .booleanValue());
  }

  @Test
  void testVerifyPasswordJson() {
    assertTrue(
        POST("/me/verifyPassword", "{'password':'district'}")
            .content()
            .getBoolean("isCorrectPassword")
            .booleanValue());
    assertFalse(
        POST("/me/verifyPassword", "{'password':'wrong'}")
            .content()
            .getBoolean("isCorrectPassword")
            .booleanValue());
  }

  public interface JsonPasswordValidation extends JsonObject {

    default boolean isValidPassword() {
      return getBoolean("isValidPassword").booleanValue();
    }

    default String getErrorMessage() {
      return getString("errorMessage").string();
    }
  }

  @Test
  void testValidatePasswordText() {
    JsonPasswordValidation result =
        POST("/me/validatePassword", ContentType("text/plain"), Body("$ecrEt42"))
            .content()
            .as(JsonPasswordValidation.class);
    assertTrue(result.isValidPassword());
    assertNull(result.getErrorMessage());
  }

  @Test
  void testValidatePasswordText_TooShort() {
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
    JsonPasswordValidation result =
        POST("/me/validatePassword", ContentType("text/plain"), Body("secret"))
            .content()
            .as(JsonPasswordValidation.class);
    assertFalse(result.isValidPassword());
    assertEquals(
        "Password must have at least 8, and at most 72 characters", result.getErrorMessage());
  }

  @Test
  void testValidatePasswordText_TooLong() {
    POST("/systemSettings/maxPasswordLength", "72").content(HttpStatus.OK);
    JsonPasswordValidation result =
        POST(
                "/me/validatePassword",
                ContentType("text/plain"),
                Body("supersecretsupersecretsupersecretsupersecretsupersecretsupersecretsuperse"))
            .content()
            .as(JsonPasswordValidation.class);
    assertFalse(result.isValidPassword());
    assertEquals(
        "Password must have at least 8, and at most 72 characters", result.getErrorMessage());
  }

  @Test
  void testValidatePasswordText_NoDigits() {
    JsonPasswordValidation result =
        POST(
                "/me/validatePassword",
                ContentType("text/plain"),
                Body("supersecretsupersecretsupersecretsupersecretsupersecretsupersecretsupers"))
            .content()
            .as(JsonPasswordValidation.class);
    assertFalse(result.isValidPassword());
    assertEquals("Password must have at least one digit", result.getErrorMessage());
  }

  @Test
  void testGetDashboard() {
    JsonObject dashboard = GET("/me/dashboard").content();
    assertEquals(0, dashboard.getNumber("unreadInterpretations").intValue());
    assertEquals(0, dashboard.getNumber("unreadMessageConversations").intValue());
  }

  @Test
  void testUpdateInterpretationsLastRead() {
    assertStatus(HttpStatus.NO_CONTENT, POST("/me/dashboard/interpretations/read"));
  }

  @Test
  void testGetApprovalLevels() {
    assertTrue(GET("/me/dataApprovalLevels").content(HttpStatus.OK).isArray());
  }

  @Test
  void testPersonalAccessTokensIsPresent() {
    long thirtyDaysInTheFuture = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);
    ApiKeyTokenGenerator.TokenWrapper wrapper =
        generatePersonalAccessToken(null, thirtyDaysInTheFuture, null);
    apiTokenStore.save(wrapper.getApiToken());

    JsonObject response = GET("/me?fields=patTokens").content();
    JsonArray patTokens = response.getArray("patTokens");
    JsonValue id = patTokens.getObject(0).get("id");

    assertTrue(id.exists());
  }

  @Test
  void testGetCurrentUserAttributeValues() {
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    User userByUsername = userService.getUserByUsername(currentUsername);
    userByUsername.setAttributeValues(
        AttributeValues.of("{\"myattribute\": {\"value\": \"myvalue\"}}"));
    userService.updateUser(userByUsername);

    assertEquals(
        "myvalue", GET("/me").content().as(JsonMeDto.class).getAttributeValues().get(0).getValue());
  }

  @Test
  void testGetTwoFactorType() {
    JsonMeDto jsonMeDto = GET("/me").content().as(JsonMeDto.class);
    assertEquals(TwoFactorType.NOT_ENABLED.toString(), jsonMeDto.getTwoFactorType());
  }

  // NOTE: ACL filtering tests for userGroups/userRoles in /api/me are in
  // MeControllerAclTest.java which extends PostgresControllerIntegrationTestBase.
  // These tests require PostgreSQL-specific JSON functions (jsonb_has_user_group_ids, etc.)
  // See DHIS2-20458 for details.

  @Test
  void testRemoveAvatar() throws IOException {
    // First, upload an avatar image
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "dhis2.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse uploadResponse = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonObject savedObject =
        uploadResponse.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    String fileResourceId = savedObject.getString("id").string();

    // Set the avatar on the current user
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    User user = userService.getUserByUsername(currentUsername);
    FileResource fileResource = fileResourceService.getFileResource(fileResourceId);
    user.setAvatar(fileResource);
    userService.updateUser(user);

    // Verify avatar is set
    User userWithAvatar = userService.getUserByUsername(currentUsername);
    assertNotNull(userWithAvatar.getAvatar());
    assertEquals(fileResourceId, userWithAvatar.getAvatar().getUid());

    // Now remove the avatar
    assertStatus(HttpStatus.NO_CONTENT, DELETE("/me/avatar"));

    // Verify avatar is removed
    User userAfterRemoval = userService.getUserByUsername(currentUsername);
    assertNull(userAfterRemoval.getAvatar());

    // Verify file resource is marked as unassigned
    FileResource fileResourceAfterRemoval = fileResourceService.getFileResource(fileResourceId);
    assertFalse(fileResourceAfterRemoval.isAssigned());
  }

  @Test
  void testRemoveAvatar_NoExistingAvatar() {
    // Verify user has no avatar
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    User user = userService.getUserByUsername(currentUsername);
    assertNull(user.getAvatar());

    // Removing avatar when there is none should succeed (idempotent operation)
    assertStatus(HttpStatus.NO_CONTENT, DELETE("/me/avatar"));

    // Verify still no avatar
    User userAfter = userService.getUserByUsername(currentUsername);
    assertNull(userAfter.getAvatar());
  }

  @Test
  void testRemoveAvatar_WithoutSpecialAuthorities() throws IOException {
    // Switch to a user without special authorities (simulating a guest user)
    switchToNewUser("guestUser");

    // First, upload an avatar image
    File file = new ClassPathResource("file/dhis2.png").getFile();
    MockMultipartFile image =
        new MockMultipartFile("file", "dhis2.png", "image/png", Files.readAllBytes(file.toPath()));
    HttpResponse uploadResponse = POST_MULTIPART("/fileResources?domain=USER_AVATAR", image);
    JsonObject savedObject =
        uploadResponse.content(HttpStatus.ACCEPTED).getObject("response").getObject("fileResource");
    String fileResourceId = savedObject.getString("id").string();

    // Set the avatar on the current user
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    User user = userService.getUserByUsername(currentUsername);
    FileResource fileResource = fileResourceService.getFileResource(fileResourceId);
    user.setAvatar(fileResource);
    userService.updateUser(user);

    // Verify avatar is set
    User userWithAvatar = userService.getUserByUsername(currentUsername);
    assertNotNull(userWithAvatar.getAvatar());

    // Remove the avatar - this should work even without special authorities
    assertStatus(HttpStatus.NO_CONTENT, DELETE("/me/avatar"));

    // Verify avatar is removed
    User userAfterRemoval = userService.getUserByUsername(currentUsername);
    assertNull(userAfterRemoval.getAvatar());
  }
}
