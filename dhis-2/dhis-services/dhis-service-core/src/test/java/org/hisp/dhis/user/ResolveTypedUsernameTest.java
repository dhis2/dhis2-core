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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hisp.dhis.attribute.AttributeService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Tests {@link DefaultUserService#resolveTypedUsername(String)}: an exact username match wins over
 * the username without edge whitespace, and only a username with edge whitespace costs a lookup.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class ResolveTypedUsernameTest {

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
  @Mock private AttributeService attributeService;

  @BeforeEach
  void setUp() {
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
            new SessionRegistryImpl(),
            attributeService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "ad min", ""})
  void testKeepsUsernameWithoutEdgeWhitespaceWithoutLookup(String username) {
    assertEquals(username, userService.resolveTypedUsername(username));
    verifyNoInteractions(userStore);
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin ", " admin", "\u00a0admin"})
  void testPrefersExactUsername(String username) {
    when(userStore.getUserByUsername(username)).thenReturn(new User());

    assertEquals(username, userService.resolveTypedUsername(username));
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin ", " admin", "\u00a0admin"})
  void testStripsWhenNoExactUsername(String username) {
    assertEquals("admin", userService.resolveTypedUsername(username));
  }

  @Test
  void testNullStaysNullWithoutLookup() {
    assertNull(userService.resolveTypedUsername(null));
    verifyNoInteractions(userStore);
  }
}
