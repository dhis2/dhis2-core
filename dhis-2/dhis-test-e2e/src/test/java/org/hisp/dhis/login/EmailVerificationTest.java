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
package org.hisp.dhis.login;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.hisp.dhis.BaseE2ETest;
import org.hisp.dhis.test.e2e.helpers.config.TestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.subethamail.wiser.WiserMessage;

public class EmailVerificationTest extends BaseE2ETest {
  @BeforeAll
  static void setup() throws JsonProcessingException {
    startSMTPServer();
    serverApiUrl = TestConfiguration.get().baseUrl();
    serverHostUrl = TestConfiguration.get().baseUrl().replace("/api", "/");
    orgUnitUID = createOrgUnit();
  }

  @AfterEach
  void tearDown() {
    wiser.getMessages().clear();
    invalidateAllSession();
  }

  @Test
  void testAutoVerifyEmailWhenAcceptingInvite() throws MessagingException, IOException {
    String adminUsername = "admin";
    String adminPassword = "district";
    String invitedUsername = CodeGenerator.generateCode(8).toLowerCase();
    String invitedEmail = invitedUsername + "@dhis2.org";
    String newPassword = "Test123###...";

    // Clear any existing messages
    wiser.getMessages().clear();

    // Configure email settings
    configureEmail2FASettings(performInitialLogin(adminUsername, adminPassword));

    // Create an invited user as admin
    String adminCookie = performInitialLogin(adminUsername, adminPassword);
    configureEmail2FASettings(adminCookie);

    String invitedUserUid = createInvitedUser(adminCookie, invitedUsername, invitedEmail);
    assertNotNull(invitedUserUid, "Failed to create invited user");

    // Wait for invitation email to be received and extract token
    waitForEmailCount(1);
    String restoreToken = extractInvitationToken();
    assertNotNull(restoreToken, "Failed to get restore token from invitation email");

    // Verify user was created but email is not verified yet
    ResponseEntity<String> userResponse = getWithCookie("/users/" + invitedUserUid, adminCookie);
    assertEquals(HttpStatus.OK, userResponse.getStatusCode());
    String userBody = userResponse.getBody();
    String verifiedEmail = objectMapper.readTree(userBody).get("emailVerified").asText();
    assertEquals(
        "false", verifiedEmail, "Email should not be verified before accepting invitation");

    // Accept the invitation with token from email
    ResponseEntity<String> acceptResponse =
        acceptInvitation(invitedUsername, newPassword, restoreToken);
    assertEquals(HttpStatus.OK, acceptResponse.getStatusCode());

    // Verify that the email was automatically verified
    ResponseEntity<String> updatedUserResponse =
        getWithCookie("/users/" + invitedUserUid, adminCookie);
    assertEquals(HttpStatus.OK, updatedUserResponse.getStatusCode());
    String body = updatedUserResponse.getBody();
    JsonNode updatedUserJson = objectMapper.readTree(body);
    String verifiedEmailAfter = updatedUserJson.get("emailVerified").asText();
    assertEquals("true", verifiedEmailAfter, "Email should be verified after accepting invitation");

    // Verify the user can login successfully
    ResponseEntity<LoginResponse> loginResponse =
        loginWithUsernameAndPassword(invitedUsername, newPassword, null);
    assertLoginSuccess(loginResponse, DEFAULT_LOGIN_REDIRECT);
  }

  /**
   * Waits for the SMTP server to receive the expected number of emails. Throws an AssertionError if
   * timeout is reached.
   */
  private void waitForEmailCount(int expectedCount) {
    long timeout = System.currentTimeMillis() + 10000; // 10 second timeout
    while (wiser.getMessages().size() < expectedCount && System.currentTimeMillis() < timeout) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    assertTrue(
        wiser.getMessages().size() >= expectedCount,
        "Did not receive expected number of emails: " + expectedCount);
  }

  /** Extracts the invitation token from the invitation email */
  private String extractInvitationToken() throws MessagingException, IOException {
    List<WiserMessage> messages = wiser.getMessages();
    assertFalse(messages.isEmpty(), "No emails received");

    MimeMessage invitationMessage = messages.get(0).getMimeMessage();
    String invitationEmail = getTextFromMessage(invitationMessage);

    // Look for the token in the invitation URL
    // Format is typically
    // http://localhost:8080/login/#/complete-registration?token=aXM0SmVUaEo3bVhrNDZlN0cwVkVadUlNY0JCR1BsYWJnVEdkT3hIT3Fublk6SUN0OW9JQU5FWm1HcUJpaVMxV0xwd3AzR3dvN29LaTRkOHFFMzh3ZEZaNHNVaw&email=jpjdk0ie@dhis2.org&username=invitevlk45rojitc
    int tokenStartIndex = invitationEmail.indexOf("token=");
    assertTrue(tokenStartIndex != -1);
    tokenStartIndex += 6;
    int tokenEndIndex = invitationEmail.indexOf("&", tokenStartIndex);
    return invitationEmail.substring(tokenStartIndex, tokenEndIndex).trim();
  }

  private String createInvitedUser(String adminCookie, String username, String email)
      throws JsonProcessingException {
    Map<String, Object> userObject = new HashMap<>();
    userObject.put("firstName", "Invited");
    userObject.put("surname", "User");
    userObject.put("email", email);
    userObject.put("userRoles", List.of(Map.of("id", SUPER_USER_ROLE_UID)));
    userObject.put("organisationUnits", List.of(Map.of("id", orgUnitUID)));

    // Use the dedicated invitation endpoint
    ResponseEntity<String> response =
        postWithCookie("/users/invite", objectMapper.writeValueAsString(userObject), adminCookie);
    HttpStatusCode statusCode = response.getStatusCode();
    String body = response.getBody();
    assertSame(HttpStatus.CREATED, statusCode);

    JsonNode responseJson = objectMapper.readTree(body);
    JsonNode jsonNode = responseJson.get("response").get("uid");
    return jsonNode.asText();
  }

  private ResponseEntity<String> acceptInvitation(
      String username, String newPassword, String restoreToken) throws JsonProcessingException {
    Map<String, Object> inviteData = new HashMap<>();

    inviteData.put("username", username);
    inviteData.put("password", newPassword);
    inviteData.put("token", restoreToken);

    inviteData.put("firstName", "First name");
    inviteData.put("surname", "Surname");

    HttpEntity<String> requestEntity =
        new HttpEntity<>(objectMapper.writeValueAsString(inviteData), jsonHeaders());
    return restTemplate.postForEntity(serverApiUrl + "/auth/invite", requestEntity, String.class);
  }
}
