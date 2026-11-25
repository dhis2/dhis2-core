/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Header;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.Objects;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpClientAdapter.Header;
import org.hisp.dhis.http.HttpClientAdapter.HttpResponse;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.security.twofa.TwoFactorType;
import org.hisp.dhis.system.DefaultSystemService;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemUpdateNotificationService;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.TestUtils;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternParser;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.security.CrossOriginGetGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that GET endpoints which change state reject cross-origin navigations and resource loads,
 * and keep working for same-origin requests and for clients that send no Fetch Metadata headers.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class CrossOriginGetControllerTest extends H2ControllerIntegrationTestBase {

  /** A release feed with one newer patch version for 2.38.0. */
  private static final String RELEASE_FEED =
      """
      {"versions": [{
        "name": "2.38", "version": 38, "latestPatchVersion": 1,
        "patchVersions": [
          {"name": "2.38.0", "version": 0, "releaseDate": "2022-05-05",
           "url": "https://releases.dhis2.org/2.38/dhis2-stable-2.38.0.war"},
          {"name": "2.38.1", "version": 1, "releaseDate": "2022-07-04",
           "url": "https://releases.dhis2.org/2.38/dhis2-stable-2.38.1.war"}]}]}
      """;

  @Autowired private MessageService messageService;

  /** Spied to serve {@link #RELEASE_FEED} instead of the feed on releases.dhis2.org. */
  @MockitoSpyBean private SystemUpdateNotificationService systemUpdateNotificationService;

  /** Browser headers of cross-origin requests that never carry an Origin header. */
  static Stream<Arguments> rejectedHeaders() {
    return Stream.of(
        Arguments.of(
            "cross-site navigation",
            new Header[] {
              Header("Sec-Fetch-Site", "cross-site"),
              Header("Sec-Fetch-Mode", "navigate"),
              Header("Sec-Fetch-Dest", "document")
            }),
        Arguments.of(
            "same-site image load",
            new Header[] {
              Header("Sec-Fetch-Site", "same-site"),
              Header("Sec-Fetch-Mode", "no-cors"),
              Header("Sec-Fetch-Dest", "image")
            }),
        Arguments.of(
            "cross-site navigation with an empty Origin",
            new Header[] {
              Header("Sec-Fetch-Site", "cross-site"),
              Header("Sec-Fetch-Mode", "navigate"),
              Header("Origin", "")
            }));
  }

  /** Requests that pass the guard. */
  static Stream<Arguments> acceptedHeaders() {
    return Stream.of(
        Arguments.of("no Fetch Metadata (Android, scripts)", new Header[0]),
        Arguments.of(
            "same-origin fetch",
            new Header[] {
              Header("Sec-Fetch-Site", "same-origin"),
              Header("Sec-Fetch-Mode", "cors"),
              Header("Sec-Fetch-Dest", "empty")
            }),
        Arguments.of(
            "typed URL or bookmark",
            new Header[] {
              Header("Sec-Fetch-Site", "none"),
              Header("Sec-Fetch-Mode", "navigate"),
              Header("Sec-Fetch-Dest", "document")
            }),
        Arguments.of(
            "cross-origin CORS fetch from an allowed origin",
            new Header[] {
              Header("Sec-Fetch-Site", "cross-site"),
              Header("Sec-Fetch-Mode", "cors"),
              Header("Sec-Fetch-Dest", "empty"),
              Header("Origin", "https://allowed.example.org")
            }));
  }

  @Test
  void testAppReloadWithPost() {
    assertWebMessage(
        "OK", 200, "OK", "Apps reloaded", POST("/maintenance/appReload").content(HttpStatus.OK));
  }

  @Test
  void testAppReloadWithPostWithoutAuthority() {
    switchToNewUser("guest");
    assertMissingAuthority("F_PERFORM_MAINTENANCE", POST("/maintenance/appReload"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testAppReloadWithGetRejected(String name, Header[] headers) {
    assertRejected(GET("/maintenance/appReload", (Object[]) headers));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedHeaders")
  void testAppReloadWithGetAccepted(String name, Header[] headers) {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Apps reloaded",
        GET("/maintenance/appReload", (Object[]) headers).content(HttpStatus.OK));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testMarkReadWithGetRejected(String name, Header[] headers) {
    User recipient = createRecipient();
    String uid = postMessageTo(recipient);
    switchToNewUser(recipient);

    assertRejected(GET("/messageConversations/{uid}?markRead=true", args(uid, headers)));

    assertFalse(isRead(uid, recipient));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testGetMessageConversationWithoutMarkRead(String name, Header[] headers) {
    User recipient = createRecipient();
    String uid = postMessageTo(recipient);
    switchToNewUser(recipient);

    assertStatus(HttpStatus.OK, GET("/messageConversations/{uid}", args(uid, headers)));

    assertFalse(isRead(uid, recipient));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedHeaders")
  void testMarkReadWithGetAccepted(String name, Header[] headers) {
    User recipient = createRecipient();
    String uid = postMessageTo(recipient);
    switchToNewUser(recipient);

    assertStatus(
        HttpStatus.OK, GET("/messageConversations/{uid}?markRead=true", args(uid, headers)));

    assertTrue(isRead(uid, recipient));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testGenerateAndReserveRejected(String name, Header[] headers) throws Exception {
    String uid = postGeneratedAttribute();

    assertRejected(
        GET(
            "/trackedEntityAttributes/{uid}/generateAndReserve?ORG_UNIT_CODE=A030101",
            args(uid, headers)));
    assertRejected(
        GET("/trackedEntityAttributes/{uid}/generate?ORG_UNIT_CODE=A030101", args(uid, headers)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedHeaders")
  void testGenerateAndReserveAccepted(String name, Header[] headers) throws Exception {
    String uid = postGeneratedAttribute();

    assertStatus(
        HttpStatus.OK,
        GET(
            "/trackedEntityAttributes/{uid}/generateAndReserve?ORG_UNIT_CODE=A030101",
            args(uid, headers)));
    assertStatus(
        HttpStatus.OK,
        GET("/trackedEntityAttributes/{uid}/generate?ORG_UNIT_CODE=A030101", args(uid, headers)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testRouteRunRejectedBeforeRouteLookup(String name, Header[] headers) {
    assertRejected(GET("/routes/nonExisting/run", (Object[]) headers));
    assertRejected(GET("/routes/nonExisting/run/sub/path", (Object[]) headers));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedHeaders")
  void testRouteRunAccepted(String name, Header[] headers) {
    // passes the guard and reaches the route lookup
    assertEquals(HttpStatus.NOT_FOUND, GET("/routes/nonExisting/run", (Object[]) headers).status());
    assertEquals(
        HttpStatus.NOT_FOUND, GET("/routes/nonExisting/run/sub/path", (Object[]) headers).status());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testTwoFactorQrCodeRejected(String name, Header[] headers) {
    User user = switchToNewUser("twofactor");

    // browsers send a wildcard Accept header for navigations and image loads
    Header[] browserHeaders = Arrays.copyOf(headers, headers.length + 1);
    browserHeaders[headers.length] = Header("Accept", "image/avif,image/webp,*/*;q=0.8");
    assertRejected(GET("/2fa/qrCode", (Object[]) browserHeaders));

    assertEquals(TwoFactorType.NOT_ENABLED, userService.getUser(user.getUid()).getTwoFactorType());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testSystemUpdatesRejected(String name, Header[] headers) {
    assertRejected(GET("/systemUpdates", (Object[]) headers));
  }

  @Test
  void testSystemUpdatesWithoutAuthority() {
    switchToNewUser("guest");
    assertMissingAuthority("F_PERFORM_MAINTENANCE", GET("/systemUpdates"));
  }

  /**
   * Serves a local release feed: the test must not depend on releases.dhis2.org. Supplies the build
   * version too: {@code build.properties} is only generated at package time, so a {@code mvn test}
   * run has none.
   */
  @Test
  void testSystemUpdatesWithAuthority() {
    switchToNewUser("maintainer", "F_PERFORM_MAINTENANCE");
    doReturn(JsonParser.parseString(RELEASE_FEED).getAsJsonObject())
        .when(systemUpdateNotificationService)
        .fetchReleaseFeed();

    JsonObject versions;
    try (MockedStatic<DefaultSystemService> systemService =
        mockStatic(DefaultSystemService.class, CALLS_REAL_METHODS)) {
      systemService
          .when(DefaultSystemService::loadBuildProperties)
          .thenReturn(SystemInfo.builder().version("2.38.0").build());

      versions =
          GET("/systemUpdates?forceVersion=2.38.0")
              .content(HttpStatus.OK)
              .getObject("response.versionMetadata");
    }

    assertEquals(List.of("2.38.1"), versions.names());
    assertFalse(messageService.getMatchingExtId("2.38.1").isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedHeaders")
  void testMetadataSyncWithGetRejected(String name, Header[] headers) {
    assertRejected(GET("/metadata/sync?versionName=Version_1", (Object[]) headers));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedHeaders")
  void testMetadataSyncWithGetAccepted(String name, Header[] headers) {
    // passes the guard and reaches the parameter validation
    assertMissingVersionName(GET("/metadata/sync", (Object[]) headers));
  }

  @Test
  void testMetadataSyncWithPost() {
    assertMissingVersionName(POST("/metadata/sync"));
  }

  @Test
  void testMetadataSyncWithPostWithoutAuthority() {
    switchToNewUser("guest");
    assertMissingAuthority("F_METADATA_MANAGE", POST("/metadata/sync"));
  }

  private static void assertRejected(HttpResponse response) {
    JsonWebMessage message = response.content(HttpStatus.FORBIDDEN).as(JsonWebMessage.class);
    assertEquals(CrossOriginGetGuard.MESSAGE, message.getMessage());
    assertEquals(ErrorCode.E1008, message.getErrorCode());
  }

  private static void assertMissingVersionName(HttpResponse response) {
    JsonWebMessage error = response.content(HttpStatus.BAD_REQUEST).as(JsonWebMessage.class);
    assertTrue(error.getMessage().contains("versionName"), error.getMessage());
  }

  private static void assertMissingAuthority(String authority, HttpResponse response) {
    assertEquals(
        "Access is denied, requires one Authority from [" + authority + "]",
        response.error(HttpStatus.FORBIDDEN).getMessage());
  }

  /** Combines a path placeholder value with request headers into one argument list. */
  private static Object[] args(String uid, Header[] headers) {
    Object[] args = new Object[headers.length + 1];
    args[0] = uid;
    System.arraycopy(headers, 0, args, 1, headers.length);
    return args;
  }

  /**
   * Creates the message recipient. With authority ALL the controller returns all messages and does
   * not replace the message list of the entity, which this test's shared Hibernate session could
   * not flush afterwards.
   */
  private User createRecipient() {
    return createUserWithAuth("recipient", "ALL");
  }

  private String postMessageTo(User recipient) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/messageConversations/",
            "{'subject':'Subject','text':'Text','users':[{'id':'" + recipient.getUid() + "'}]}"));
  }

  private boolean isRead(String conversationUid, User user) {
    return manager.get(MessageConversation.class, conversationUid).isRead(user);
  }

  private String postGeneratedAttribute() throws Exception {
    TrackedEntityAttribute attribute = createTrackedEntityAttribute('A');
    attribute.setGenerated(true);
    String pattern = "ORG_UNIT_CODE() + RANDOM(#######)";
    TextPattern textPattern = TextPatternParser.parse(pattern);
    textPattern.setOwnerObject(Objects.TRACKEDENTITYATTRIBUTE);
    textPattern.setOwnerUid(attribute.getUid());
    attribute.setTextPattern(textPattern);
    attribute.setPattern(pattern);
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            TrackedEntityAttributeSchemaDescriptor.API_ENDPOINT,
            new String(TestUtils.convertObjectToJsonBytes(attribute))));
  }
}
