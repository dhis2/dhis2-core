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
package org.hisp.dhis.webapi.controller.event;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.webapi.controller.event.mapper.TrackerEventCriteriaMapperUtils.validateOrgUnitMode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.EventQueryParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.webapi.controller.event.mapper.RequestToSearchParamsMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.EventCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Ameen
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EventRequestToParamsMapperTest {

  @Mock private CurrentUserService currentUserService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityInstanceService entityInstanceService;

  @Mock private DataElementService dataElementService;

  @Mock private InputUtils inputUtils;

  @Mock private SchemaService schemaService;

  @Mock private TrackerAccessManager trackerAccessManager;

  private RequestToSearchParamsMapper requestToSearchParamsMapper;

  private OrganisationUnit orgUnit;

  private final String orgUnitId = "orgUnitId";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private final List<OrganisationUnit> orgUnitDescendants =
      List.of(
          createOrgUnit("orgUnit1", "uid1"),
          createOrgUnit("orgUnit2", "uid2"),
          createOrgUnit("captureScopeOrgUnit", "uid3"),
          createOrgUnit("searchScopeOrgUnit", "uid4"));

  @BeforeEach
  public void setUp() {
    requestToSearchParamsMapper =
        new RequestToSearchParamsMapper(
            currentUserService,
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
    TrackedEntityInstance tei = new TrackedEntityInstance();
    DataElement de = new DataElement();

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(programService.getProgram(any())).thenReturn(program);
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(ou);

    when(organisationUnitService.isInUserHierarchy(ou)).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(entityInstanceService.getTrackedEntityInstance(any())).thenReturn(tei);
    when(dataElementService.getDataElement(any())).thenReturn(de);

    orgUnit = createOrgUnit("orgUnit", orgUnitId);
    orgUnit.setChildren(
        Set.of(
            createOrgUnit("captureScopeChild", "captureScopeChildUid"),
            createOrgUnit("searchScopeChild", "searchScopeChildUid")));
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldMapRequestedOrgUnitWhenProgramProtected(OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setAccessLevel(PROTECTED);
    program.setUid(PROGRAM_UID);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid3");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    when(organisationUnitService.isInUserHierarchy(
            orgUnitId, user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnitId);
    eventCriteria.setOuMode(orgUnitMode);

    EventQueryParams queryParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(orgUnitId, queryParams.getOrgUnit().getUid());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldMapRequestedOrgUnitWhenProgramOpen(OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);
    when(organisationUnitService.isInUserHierarchy(
            orgUnitId, user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    EventQueryParams queryParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(orgUnitId, queryParams.getOrgUnit().getUid());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenProgramProtectedAndUserHasNoAccessToCaptureScopeOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN", "ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenProgramOpenAndUserHasNoAccessToSearchScopeOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(orgUnitMode);

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeChildren() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeChild", "searchScopeChildUid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(CHILDREN);

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventQueryParams searchParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(searchParams.getOrgUnit(), orgUnit);
  }

  @Test
  void shouldMapOrgUnitModeAccessibleWhenNoOrgUnitProvidedAndNoOrgUnitModeProvided() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());

    EventQueryParams queryParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(ACCESSIBLE, queryParams.getOrgUnitSelectionMode());
    assertNull(queryParams.getOrgUnit());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeNeedsOrgUnitAndNoOrgUnitProvided(OrganisationUnitSelectionMode mode) {
    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOuMode(mode);

    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is required for org unit mode: " + mode, exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"CAPTURE", "ACCESSIBLE", "ALL"})
  void shouldPassWhenOuModeDoesNotNeedOrgUnitAndOrgUnitProvided(
      OrganisationUnitSelectionMode mode) {
    when(currentUserService.getCurrentUser()).thenReturn(createSearchInAllOrgUnitsUser());
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOuMode(mode);

    assertDoesNotThrow(() -> requestToSearchParamsMapper.map(eventCriteria));
  }

  @Test
  void shouldFailWhenModeAccessibleAndUserScopeNotSet() {
    when(currentUserService.getCurrentUser()).thenReturn(new User());

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOuMode(ACCESSIBLE);

    Exception exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "User needs to be assigned either search or data capture org units",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenModeCaptureAndUserCaptureScopeNotSet() {
    when(currentUserService.getCurrentUser()).thenReturn(new User());

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOuMode(CAPTURE);

    Exception exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals("User needs to be assigned data capture org units", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "ACCESSIBLE", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeRequiresUserScopeOrgUnitAndUserHasNoOrgUnitsAssigned(
      OrganisationUnitSelectionMode orgUnitMode) {
    User user = new User();
    Program program = new Program();
    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () -> validateOrgUnitMode(orgUnitMode, user, program, null));

    assertEquals(
        "User needs to be assigned either search or data capture org units",
        exception.getMessage());
  }

  private User createSearchInAllOrgUnitsUser() {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(
        Set.of(Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));
    user.setOrganisationUnits(Set.of(orgUnit));

    return user;
  }

  @Test
  void shouldMapRequestedOrgUnitAsSelectedWhenOrgUnitProvidedAndNoOrgUnitModeProvided() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(trackerAccessManager.canAccess(user, program, orgUnit)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    EventQueryParams searchParams = requestToSearchParamsMapper.map(eventCriteria);

    assertEquals(SELECTED, searchParams.getOrgUnitSelectionMode());
    assertEquals(searchParams.getOrgUnit(), orgUnit);
  }

  @Test
  void shouldFailWhenNoOuModeSpecifiedAndUserHasNoAccessToOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(CLOSED);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class, () -> requestToSearchParamsMapper.map(eventCriteria));
    assertEquals(
        "Organisation unit is not part of the search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }
}
