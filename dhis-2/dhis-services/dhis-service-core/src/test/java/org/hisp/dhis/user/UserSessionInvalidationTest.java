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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Asserts that session invalidation expires exactly the sessions of the given usernames, and that
 * it does so without loading a single user.
 *
 * <p>The session registry is the real {@link SessionRegistryImpl}, not a mock, because the whole
 * point of the fix is that the registry keys principals on the username alone. A mock would assert
 * nothing about that.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSessionInvalidationTest {

  @Mock private UserSettingsService userSettingsService;
  @Mock private RestTemplate restTemplate;
  @Mock private MessageSender emailMessageSender;
  @Mock private I18nManager i18nManager;
  @Mock private ObjectMapper jsonMapper;
  @Mock private UserStore userStore;
  @Mock private UserGroupService userGroupService;
  @Mock private UserRoleStore userRoleStore;
  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private CacheProvider cacheProvider;
  @Mock private PasswordManager passwordManager;
  @Mock private AclService aclService;
  @Mock private OrganisationUnitService organisationUnitService;

  private SessionRegistryImpl sessionRegistry;
  private UserService userService;

  @BeforeEach
  void setUp() {
    sessionRegistry = new SessionRegistryImpl();
    userService =
        new DefaultUserService(
            userSettingsService,
            restTemplate,
            emailMessageSender,
            i18nManager,
            jsonMapper,
            userStore,
            userGroupService,
            userRoleStore,
            settingsProvider,
            cacheProvider,
            passwordManager,
            aclService,
            organisationUnitService,
            sessionRegistry);
  }

  @Test
  @DisplayName("A session registered under a fully hydrated principal is expired by username alone")
  void expiresSessionRegisteredUnderFullyHydratedPrincipal() {
    sessionRegistry.registerNewSession("session-1", hydratedPrincipal("alice"));

    userService.invalidateUserSessions("alice");

    assertTrue(sessionOf("session-1").isExpired());
  }

  @Test
  @DisplayName("Session invalidation loads no user and issues no org unit lookup")
  void invalidationLoadsNoUser() {
    sessionRegistry.registerNewSession("session-1", hydratedPrincipal("alice"));

    userService.invalidateUserSessions(List.of("alice"));

    verifyNoInteractions(userStore, organisationUnitService, userGroupService);
  }

  @Test
  @DisplayName("Only the sessions of the given usernames are expired")
  void expiresOnlyTheGivenUsernames() {
    sessionRegistry.registerNewSession("alice-1", hydratedPrincipal("alice"));
    sessionRegistry.registerNewSession("alice-2", hydratedPrincipal("alice"));
    sessionRegistry.registerNewSession("bob-1", hydratedPrincipal("bob"));
    sessionRegistry.registerNewSession("carol-1", hydratedPrincipal("carol"));

    userService.invalidateUserSessions(List.of("alice", "bob"));

    assertTrue(sessionOf("alice-1").isExpired());
    assertTrue(sessionOf("alice-2").isExpired());
    assertTrue(sessionOf("bob-1").isExpired());
    assertFalse(sessionOf("carol-1").isExpired());
  }

  @Test
  @DisplayName("A username with no session, a null username and an empty list are all no-ops")
  void handlesAbsentAndEmptyInput() {
    userService.invalidateUserSessions("nobody");
    userService.invalidateUserSessions((String) null);
    userService.invalidateUserSessions(List.of());
    userService.invalidateUserSessions((List<String>) null);

    verifyNoInteractions(userStore, organisationUnitService);
  }

  private SessionInformation sessionOf(String sessionId) {
    return sessionRegistry.getSessionInformation(sessionId);
  }

  /**
   * The principal a real login registers: a {@link UserDetails} built from a {@link User} by the
   * same {@code createUserDetails} path the authentication filter uses.
   */
  private static UserDetails hydratedPrincipal(String username) {
    User user = new User();
    user.setUsername(username);
    user.setUid("uid" + username);
    user.setAutoFields();
    return UserDetails.createUserDetails(
        user, true, true, Set.of("orgUnit1"), Set.of("orgUnit2"), Set.of("orgUnit3"));
  }
}
