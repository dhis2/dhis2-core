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
package org.hisp.dhis.webapi.controller.deprecated.tracker;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Ameen
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventRequestToSearchParamsMapperTest {

  @Mock private UserService userService;
  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityService entityInstanceService;

  @Mock private DataElementService dataElementService;

  @Mock private InputUtils inputUtils;

  @Mock private SchemaService schemaService;

  @Mock private TrackerAccessManager trackerAccessManager;

  private EventRequestToSearchParamsMapper requestToSearchParamsMapper;

  private OrganisationUnit orgUnit;

  private final Map<String, User> userMap = new HashMap<>();

  private final String orgUnitId = "orgUnitId";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  @BeforeEach
  public void setUp() {
    requestToSearchParamsMapper =
        new EventRequestToSearchParamsMapper(
            userService,
            programService,
            organisationUnitService,
            programStageService,
            aclService,
            entityInstanceService,
            dataElementService,
            inputUtils,
            schemaService);

    Program program = new Program();
    User user = new User();
    OrganisationUnit ou = new OrganisationUnit();
    user.setOrganisationUnits(Set.of(ou));
    TrackedEntity tei = new TrackedEntity();
    DataElement de = new DataElement();

    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);
    //    when(getCurrentUser()).thenReturn(user);
    when(programService.getProgram(any())).thenReturn(program);
    // when(organisationUnitService.getOrganisationUnit(any())).thenReturn(ou);

    when(organisationUnitService.isInUserHierarchy(user, ou)).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(entityInstanceService.getTrackedEntity(any())).thenReturn(tei);
    when(dataElementService.getDataElement(any())).thenReturn(de);

    orgUnit = createOrgUnit("orgUnit", orgUnitId);
    orgUnit.setChildren(
        Set.of(
            createOrgUnit("captureScopeChild", "captureScopeChildUid"),
            createOrgUnit("searchScopeChild", "searchScopeChildUid")));

    userMap.put("admin", createUserWithAuthority(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS));
    userMap.put("superuser", createUserWithAuthority(Authorities.ALL));
  }

  private EventSearchParams map(OrganisationUnitSelectionMode orgUnitMode) {
    return map(null, orgUnitMode);
  }

  private EventSearchParams map(String orgUnitId, OrganisationUnitSelectionMode orgUnitMode) {
    return requestToSearchParamsMapper.map(
        "programuid",
        null,
        null,
        null,
        orgUnitId,
        orgUnitMode,
        "teiUid",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        Collections.emptyList(),
        Collections.emptyList(),
        false,
        new HashSet<>(),
        new HashSet<>(),
        null,
        null,
        new HashSet<>(),
        Collections.singleton("UXz7xuGCEhU:GT:100"),
        new HashSet<>(),
        false,
        false);
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "ACCESSIBLE", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeRequiresUserScopeOrgUnitAndUserHasNoOrgUnitsAssigned(
      OrganisationUnitSelectionMode orgUnitMode) {
    User user = new User();
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOuMode(orgUnitMode);

    Exception exception =
        Assertions.assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));

    assertEquals(
        "User needs to be assigned either search or data capture org units",
        exception.getMessage());
  }

  @Test
  void shouldMapRequestedOrgUnitAsSelectedWhenOrgUnitProvidedAndNoOrgUnitModeProvided()
      throws ForbiddenException {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);
    //    when(getCurrentUser()).thenReturn(user.);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(trackerAccessManager.canAccess(user, program, orgUnit)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    EventSearchParams searchParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(SELECTED, searchParams.getOrgUnitSelectionMode());
    assertEquals(orgUnit, searchParams.getOrgUnit());
  }

  @Test
  void shouldFailWhenNoOuModeSpecifiedAndUserHasNoAccessToOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(CLOSED);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of your search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeRequiresOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    User user = new User();
    user.setOrganisationUnits(Set.of(orgUnit));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    Exception exception = assertThrows(IllegalQueryException.class, () -> map(orgUnitMode));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: " + orgUnitMode,
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeDoesNotUseIt(
      OrganisationUnitSelectionMode orgUnitMode) {
    User user = new User();
    user.setOrganisationUnits(Set.of(orgUnit));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    String orgUnitId = orgUnit.getUid();
    Exception exception =
        assertThrows(IllegalQueryException.class, () -> map(orgUnitId, orgUnitMode));

    assertStartsWith(
        "ouMode " + orgUnitMode + " cannot be used with orgUnits.", exception.getMessage());
  }

  private static Stream<Arguments>
      shouldMapOrgUnitWhenProgramProvidedAndRequestedOrgUnitInSearchScope() {
    return Stream.of(
        arguments(SELECTED, OPEN),
        arguments(SELECTED, CLOSED),
        arguments(CHILDREN, OPEN),
        arguments(CHILDREN, CLOSED),
        arguments(DESCENDANTS, OPEN),
        arguments(DESCENDANTS, CLOSED));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMapOrgUnitWhenProgramProvidedAndRequestedOrgUnitInSearchScope(
      OrganisationUnitSelectionMode orgUnitMode, AccessLevel accessLevel) {
    Program program = new Program();
    program.setAccessLevel(accessLevel);

    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    OrganisationUnit searchScopeChildOrgUnit = createOrgUnit("searchScopeChildOrgUnit", "uid5");
    searchScopeOrgUnit.setChildren(Set.of(searchScopeChildOrgUnit));
    searchScopeChildOrgUnit.setParent(searchScopeOrgUnit);

    User user = new User();
    user.setOrganisationUnits(Set.of(createOrgUnit("captureScopeOrgUnit", "uid")));
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(searchScopeChildOrgUnit.getUid()))
        .thenReturn(searchScopeChildOrgUnit);
    when(organisationUnitService.isInUserHierarchy(
            searchScopeChildOrgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventSearchParams eventSearchParams = map(searchScopeChildOrgUnit.getUid(), orgUnitMode);
    assertEquals(searchScopeChildOrgUnit, eventSearchParams.getOrgUnit());
  }

  private static Stream<Arguments> shouldMapOrgUnitWhenProgramProvidedAndNoOrgUnitProvided() {
    return Stream.of(
        arguments(ACCESSIBLE, OPEN),
        arguments(ACCESSIBLE, CLOSED),
        arguments(CAPTURE, OPEN),
        arguments(CAPTURE, CLOSED));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMapOrgUnitWhenProgramProvidedAndNoOrgUnitProvided(
      OrganisationUnitSelectionMode orgUnitMode, AccessLevel accessLevel) {
    Program program = new Program();
    program.setAccessLevel(accessLevel);

    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    OrganisationUnit searchScopeChildOrgUnit = createOrgUnit("searchScopeChildOrgUnit", "uid5");
    searchScopeOrgUnit.setChildren(Set.of(searchScopeChildOrgUnit));
    searchScopeChildOrgUnit.setParent(searchScopeOrgUnit);

    User user = new User();
    user.setOrganisationUnits(Set.of(createOrgUnit("captureScopeOrgUnit", "uid")));
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(searchScopeChildOrgUnit.getUid()))
        .thenReturn(searchScopeChildOrgUnit);
    when(organisationUnitService.isInUserHierarchy(
            searchScopeChildOrgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventSearchParams eventSearchParams = map(orgUnitMode);
    assertNull(eventSearchParams.getOrgUnit());
    assertEquals(orgUnitMode, eventSearchParams.getOrgUnitSelectionMode());
  }

  @Test
  void shouldMapOrgUnitWhenModeAllProgramProvidedAndRequestedOrgUnitInSearchScope() {
    Program program = new Program();
    program.setAccessLevel(OPEN);

    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    OrganisationUnit searchScopeChildOrgUnit = createOrgUnit("searchScopeChildOrgUnit", "uid5");
    searchScopeOrgUnit.setChildren(Set.of(searchScopeChildOrgUnit));
    searchScopeChildOrgUnit.setParent(searchScopeOrgUnit);

    User user = new User();
    user.setOrganisationUnits(Set.of(createOrgUnit("captureScopeOrgUnit", "uid")));
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(searchScopeChildOrgUnit.getUid()))
        .thenReturn(searchScopeChildOrgUnit);
    when(organisationUnitService.isInUserHierarchy(
            searchScopeChildOrgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventSearchParams eventSearchParams = map(searchScopeChildOrgUnit.getUid(), ALL);
    assertEquals(searchScopeChildOrgUnit, eventSearchParams.getOrgUnit());
  }

  @Test
  void shouldNotMapOrgUnitWhenModeAllProgramProvidedAndNoOrgUnitProvided() {
    Program program = new Program();
    program.setAccessLevel(OPEN);

    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));

    //    when(getCurrentUser()).thenReturn(user);
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    EventSearchParams eventSearchParams = map(ALL);
    assertNull(eventSearchParams.getOrgUnit());
  }

  @Test
  void shouldFailWhenUserHasSearchInAllAuthorityButNoAccessToProgram() {
    Program program = new Program();
    program.setAccessLevel(OPEN);

    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));
    user.setUsername("anyUser");
    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername())).thenReturn(user);

    when(aclService.canDataRead(user, program)).thenReturn(false);

    Exception illegalQueryException =
        assertThrows(org.hisp.dhis.common.IllegalQueryException.class, () -> map(ALL));
    assertEquals(
        String.format("User has no access to program: %s", program.getUid()),
        illegalQueryException.getMessage());
  }

  @ParameterizedTest
  @EnumSource(value = OrganisationUnitSelectionMode.class)
  void shouldFailWhenRequestedOrgUnitOutsideOfSearchScope(
      OrganisationUnitSelectionMode orgUnitMode) {
    when(organisationUnitService.getOrganisationUnit(orgUnitId)).thenReturn(orgUnit);

    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, () -> map(orgUnitId, orgUnitMode));
    assertEquals(
        "Organisation unit is not part of your search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"admin", "superuser"})
  void shouldMapOrgUnitAndModeWhenModeAllAndUserIsAuthorized(String userName) {
    //    when(getCurrentUser()).thenReturn(userMap.get(userName));

    when(userService.getUserByUsername(CurrentUserUtil.getCurrentUsername()))
        .thenReturn(userMap.get(userName));

    EventSearchParams eventSearchParams = map(ALL);

    assertNull(eventSearchParams.getOrgUnit());
    assertEquals(ALL, eventSearchParams.getOrgUnitSelectionMode());
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }

  private User createUserWithAuthority(Authorities authority) {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(authority.name()));
    user.setUserRoles(Set.of(userRole));

    return user;
  }
}
