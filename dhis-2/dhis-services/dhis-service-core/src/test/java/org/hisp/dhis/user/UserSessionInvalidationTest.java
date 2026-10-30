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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Unit test of {@link DefaultUserService#invalidateUserSessions(String)}. Verifies that sessions
 * registered with a fully populated principal are expired when looked up with a thin username-only
 * principal, and that no user is loaded from the store in the process.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class UserSessionInvalidationTest {

  private DefaultUserService userService;

  private final SessionRegistryImpl sessionRegistry = new SessionRegistryImpl();

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
  @Mock private AttributeService attributeService;
  @Mock private Cache<String> userDisplayNameCache;
  @Mock private Cache<Integer> userFailedLoginAttemptCache;
  @Mock private Cache<Integer> userAccountRecoverAttemptCache;
  @Mock private Cache<Integer> twoFaDisableFailedAttemptCache;

  @BeforeEach
  void setUp() {
    when(cacheProvider.<String>createUserDisplayNameCache()).thenReturn(userDisplayNameCache);
    when(cacheProvider.<Integer>createUserFailedLoginAttemptCache(0))
        .thenReturn(userFailedLoginAttemptCache);
    when(cacheProvider.<Integer>createUserAccountRecoverAttemptCache(0))
        .thenReturn(userAccountRecoverAttemptCache);
    when(cacheProvider.<Integer>createDisable2FAFailedAttemptCache(0))
        .thenReturn(twoFaDisableFailedAttemptCache);

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
            sessionRegistry,
            attributeService);
  }

  @Test
  void invalidateUserSessionsExpiresSessionsWithoutLoadingUser() {
    sessionRegistry.registerNewSession("session1", fullPrincipal("alice"));
    sessionRegistry.registerNewSession("session2", fullPrincipal("alice"));

    userService.invalidateUserSessions("alice");

    assertTrue(sessionRegistry.getSessionInformation("session1").isExpired());
    assertTrue(sessionRegistry.getSessionInformation("session2").isExpired());
    verifyNoInteractions(userStore);
  }

  @Test
  void invalidateUserSessionsLeavesSessionsOfOtherUsersAlone() {
    sessionRegistry.registerNewSession("session1", fullPrincipal("alice"));
    sessionRegistry.registerNewSession("session2", fullPrincipal("bob"));

    userService.invalidateUserSessions("alice");

    assertTrue(sessionRegistry.getSessionInformation("session1").isExpired());
    assertFalse(sessionRegistry.getSessionInformation("session2").isExpired());
  }

  @Test
  void invalidateUserSessionsToleratesUnknownAndNullUsernames() {
    assertDoesNotThrow(() -> userService.invalidateUserSessions("unknown"));
    assertDoesNotThrow(() -> userService.invalidateUserSessions(null));
    verifyNoInteractions(userStore);
  }

  /** A principal as it is registered at login, carrying more than just the username. */
  private static UserDetails fullPrincipal(String username) {
    return UserDetails.empty()
        .id(42L)
        .uid("a1234567890")
        .username(username)
        .password("secret")
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();
  }
}
