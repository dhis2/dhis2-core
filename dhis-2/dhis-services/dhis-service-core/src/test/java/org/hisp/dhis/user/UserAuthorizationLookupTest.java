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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.web.client.RestTemplate;

/**
 * Tests that the user and user role authorization checks of {@link DefaultUserService} load what
 * they need once [DHIS2-21665]: groups only when the group authority is held, and in one call; a
 * stored role only once; the organisation units of a user in one call.
 *
 * @author Morten Svanaes
 */
@ExtendWith(MockitoExtension.class)
class UserAuthorizationLookupTest {

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
  @Mock private UserDetails currentUser;
  @Mock private SystemSettings settings;

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
            new SessionRegistryImpl());
  }

  @Test
  void callerWithoutGroupAuthorityIsRejectedWithoutLoadingGroups() {
    assertFalse(userService.canAddOrUpdateUser(List.of("groupA", "groupB"), currentUser));

    verifyNoInteractions(userGroupService);
  }

  @Test
  void groupManagerHasTheGroupsLoadedInOneCall() {
    UserGroup other = new UserGroup("other");
    UserGroup managed = new UserGroup("managed");
    when(currentUser.isAuthorized(anyString()))
        .thenAnswer(call -> UserGroup.AUTH_USER_ADD_IN_GROUP.equals(call.getArgument(0)));
    when(userGroupService.getUserGroupsByUid(List.of("other", "managed")))
        .thenReturn(List.of(other, managed));
    when(currentUser.canManage(any(UserGroup.class)))
        .thenAnswer(call -> call.getArgument(0) == managed);

    assertTrue(userService.canAddOrUpdateUser(List.of("other", "managed"), currentUser));

    verify(userGroupService, never()).getUserGroup(anyString());
  }

  /**
   * The organisation units of the three user collections are loaded in one call; an unknown one and
   * one outside the hierarchy of the current user are reported.
   */
  @Test
  void orgUnitsOfTheUserAreLoadedInOneCall() {
    OrganisationUnit inside = orgUnit("OrgUnitIns1", "/OrgUnitRoot/OrgUnitIns1");
    OrganisationUnit outside = orgUnit("OrgUnitOut1", "/OrgUnitOthr/OrgUnitOut1");
    User user = new User();
    user.setOrganisationUnits(Set.of(orgUnit("OrgUnitIns1", null)));
    user.setDataViewOrganisationUnits(Set.of(orgUnit("OrgUnitOut1", null)));
    user.setTeiSearchOrganisationUnits(
        Set.of(orgUnit("OrgUnitIns1", null), orgUnit("OrgUnitUnk1", null)));
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(currentUser.getUserOrgUnitIds()).thenReturn(Set.of("OrgUnitRoot"));
    when(organisationUnitService.getOrganisationUnitsByUid(
            Set.of("OrgUnitIns1", "OrgUnitOut1", "OrgUnitUnk1")))
        .thenReturn(List.of(inside, outside));

    List<ErrorReport> errors = userService.validateUserCreateOrUpdateAccess(user, currentUser);

    List<String> orgUnitErrors =
        errors.stream()
            .filter(error -> error.getErrorCode() == ErrorCode.E7617)
            .map(ErrorReport::getMessage)
            .toList();
    assertEquals(2, orgUnitErrors.size(), orgUnitErrors::toString);
    assertTrue(orgUnitErrors.get(0).contains("OrgUnitOut1"), orgUnitErrors::toString);
    assertTrue(orgUnitErrors.get(1).contains("OrgUnitUnk1"), orgUnitErrors::toString);
    verify(organisationUnitService).getOrganisationUnitsByUid(any());
    verifyNoMoreInteractions(organisationUnitService);
  }

  /** An update without authorities keeps the stored ones, which are checked; loaded once. */
  @Test
  void roleUpdateWithoutAuthoritiesChecksTheStoredRoleLoadedOnce() {
    UserRole stored = role("Stored", Set.of("F_PERFORM_MAINTENANCE"));
    UserRole update = role("Update", null);
    when(userRoleStore.get(7L)).thenReturn(stored);

    List<ErrorReport> errors = userService.validateUserRoleCreateOrUpdate(update, currentUser);

    assertEquals(List.of(ErrorCode.E3003), errors.stream().map(ErrorReport::getErrorCode).toList());
    verify(currentUser).canIssueUserRole(stored, true);
    verify(userRoleStore).get(7L);
    verifyNoMoreInteractions(userRoleStore);
  }

  private static UserRole role(String name, Set<String> authorities) {
    UserRole role = new UserRole();
    role.setId(7L);
    role.setUid("roleUid0001");
    role.setName(name);
    role.setAuthorities(authorities);
    return role;
  }

  private static OrganisationUnit orgUnit(String uid, String path) {
    OrganisationUnit unit = new OrganisationUnit();
    unit.setUid(uid);
    unit.setPath(path);
    return unit;
  }
}
