/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionDestroyedEvent;

/**
 * Tests how the {@link UserSettingsService} holds the settings of a user for the user's sessions:
 * the settings stored for the user with the system settings of the last login as fallback.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class UserSettingsServiceTest extends PostgresIntegrationTestBase {

  @Autowired private UserSettingsService userSettingsService;
  @Autowired private UserSessionAware userSessions;
  @Autowired private SystemSettingsService systemSettingsService;

  private String username;

  @BeforeEach
  void setUp() {
    username = getAdminUser().getUsername();
  }

  @AfterEach
  void tearDown() {
    // the test is not transactional and settings are held outside the DB
    systemSettingsService.deleteAll(Set.of("keyUiLocale"));
    startNewRequest();
    userSettingsService.deleteAll(username);
    login();
  }

  @Test
  void testOnSessionInit_SystemSettingChangeAppliesAtNextLogin() {
    putSystemUiLocale("fr");
    login();
    assertEquals("fr", getEffectiveUiLocale());

    putSystemUiLocale("nb");
    assertEquals("fr", getEffectiveUiLocale(), "settings are stable until the next login");

    // settings of the first login are still held, as they are when the user has another session
    login();
    assertEquals("nb", getEffectiveUiLocale());
  }

  @Test
  void testOnSessionInit_StoredValueWinsOverSystemSetting() throws Exception {
    userSettingsService.put("keyUiLocale", "de");
    putSystemUiLocale("fr");
    login();

    assertEquals("de", getEffectiveUiLocale());
  }

  @Test
  void testOnSessionRequest_RestoresSettingsWhenSessionOfUserEnded() throws Exception {
    userSettingsService.put("keyUiLocale", "de");
    login();

    userSessions.onSessionExpired(sessionDestroyed());
    userSessions.onSessionRequest(username);

    assertEquals("de", getEffectiveUiLocale());
  }

  /** The short form of a key (without the {@code key} prefix) refers to the same setting. */
  @ParameterizedTest
  @ValueSource(strings = {"keyUiLocale", "uiLocale"})
  void testGetUserSettings_NoFallbackHasOnlyStoredValues(String key) throws Exception {
    putSystemUiLocale("fr");
    login();

    UserSettings settings = userSettingsService.getUserSettings(username, false);
    assertEquals(Set.of(), settings.keys());
    assertTrue(settings.toJson(false, Set.of(key), false).get(key).isNull());

    userSettingsService.put("keyUiLocale", "de");
    settings = userSettingsService.getUserSettings(username, false);
    assertEquals(Set.of("keyUiLocale"), settings.keys());
    assertEquals("de", settings.toJson(false, Set.of(key), false).get(key).string());
  }

  private void putSystemUiLocale(String locale) {
    systemSettingsService.put("keyUiLocale", locale);
    startNewRequest();
  }

  /** A request does not see the settings that the thread held for the request before. */
  private void startNewRequest() {
    systemSettingsService.clearCurrentSettings();
    ThreadUserSettings.clear();
  }

  private void login() {
    userSessions.onSessionInit(
        new AuthenticationSuccessEvent(SecurityContextHolder.getContext().getAuthentication()));
  }

  /** The end of a session of the current user, for example by a logout. */
  private static SessionDestroyedEvent sessionDestroyed() {
    SecurityContext context = SecurityContextHolder.getContext();
    return new SessionDestroyedEvent(context) {
      @Override
      public List<SecurityContext> getSecurityContexts() {
        return List.of(context);
      }

      @Override
      public String getId() {
        return "session-of-user";
      }
    };
  }

  /** Asserts the service and the current user's settings agree, then returns the UI locale. */
  private String getEffectiveUiLocale() {
    startNewRequest();
    String fromService =
        userSettingsService.getUserSettings(username, true).getUserUiLocale().toString();
    String fromCurrent = UserSettings.getCurrentSettings().getUserUiLocale().toString();
    assertEquals(fromService, fromCurrent, "service and current settings disagree");
    return fromCurrent;
  }
}
