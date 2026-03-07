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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.UserSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class UserReplicationServiceTest {

  private DefaultUserService userService;

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
  @Mock private SessionRegistry sessionRegistry;
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

  @AfterEach
  void tearDown() {
    CurrentUserUtil.clearSecurityContext();
  }

  @Test
  void replicateUserThrowsNotFoundWhenInsertUserCopyAffectsNoRows() {
    CurrentUserUtil.injectUserInSecurityContext(
        UserDetails.empty()
            .id(42L)
            .uid("a1234567890")
            .username("admin")
            .password("secret")
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build());

    User existingUser = new User();
    existingUser.setUid("b1234567890");

    User sourceUser = new User();
    sourceUser.setUid("b1234567890");
    sourceUser.setUsername("source");
    sourceUser.setExternalAuth(false);

    when(userStore.getUserByUsername("replica")).thenReturn(null);
    when(userStore.getByUidNoAcl("b1234567890")).thenReturn(sourceUser);
    when(passwordManager.encode("Str0ngPass!")).thenReturn("encodedPassword");
    when(userStore.insertUserCopy(
            eq("b1234567890"),
            anyString(),
            any(UUID.class),
            eq("replica"),
            eq("encodedPassword"),
            eq(42L)))
        .thenReturn(0);

    NotFoundException ex =
        assertThrows(
            NotFoundException.class,
            () -> userService.replicateUser(existingUser, "replica", "Str0ngPass!"));

    assertEquals("User not found: b1234567890", ex.getMessage());
    verify(userRoleStore, never()).copyRoleMemberships(any(), any());
    verify(userStore, never()).copyOrgUnitMemberships(any(), any());
    verify(userStore, never()).copyDimensionConstraints(any(), any());
    verify(userStore, never()).clearUserQueryCache();
  }

  @Test
  void replicateUserClearsQueryCacheAfterSuccessfulJdbcReplication() throws Exception {
    CurrentUserUtil.injectUserInSecurityContext(
        UserDetails.empty()
            .id(42L)
            .uid("a1234567890")
            .username("admin")
            .password("secret")
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build());

    User existingUser = new User();
    existingUser.setUid("b1234567890");

    User sourceUser = new User();
    sourceUser.setUid("b1234567890");
    sourceUser.setUsername("source");
    sourceUser.setExternalAuth(false);

    User replicaUser = new User();
    replicaUser.setUid("c1234567890");

    when(userStore.getUserByUsername("replica")).thenReturn(null);
    when(userStore.getByUidNoAcl("b1234567890")).thenReturn(sourceUser);
    when(passwordManager.encode("Str0ngPass!")).thenReturn("encodedPassword");
    when(userStore.insertUserCopy(
            eq("b1234567890"),
            anyString(),
            any(UUID.class),
            eq("replica"),
            eq("encodedPassword"),
            eq(42L)))
        .thenReturn(1);
    UserSettings settings = org.mockito.Mockito.mock(UserSettings.class);
    when(settings.toMap()).thenReturn(java.util.Map.of());
    when(userSettingsService.getUserSettings("source", false)).thenReturn(settings);
    when(userStore.getByUidNoAcl(anyString())).thenReturn(sourceUser, replicaUser);

    userService.replicateUser(existingUser, "replica", "Str0ngPass!");

    verify(userStore, times(1)).clearUserQueryCache();
  }
}
