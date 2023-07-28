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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.EventCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  @Mock private CurrentUserService currentUserService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityInstanceService entityInstanceService;

  @Mock private DataElementService dataElementService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private InputUtils inputUtils;

  @Mock private SchemaService schemaService;

  @InjectMocks private EventRequestToSearchParamsMapper mapper;

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
    mapper =
        new EventRequestToSearchParamsMapper(
            currentUserService,
            programService,
            organisationUnitService,
            programStageService,
            aclService,
            entityInstanceService,
            dataElementService,
            inputUtils,
            schemaService,
            trackerAccessManager);

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

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeDescendants()
      throws ForbiddenException {
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

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnitId);
    eventCriteria.setOuMode(DESCENDANTS);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeDescendants() throws ForbiddenException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(DESCENDANTS);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeDescendantsAndUserHasNoAccessToCaptureScopeOrgUnit() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(DESCENDANTS);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeDescendantsAndUserHasNoAccessToSearchScopeOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(DESCENDANTS);

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeChildren()
      throws ForbiddenException {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit =
        createOrgUnit("captureScopeChild", "captureScopeChildUid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(CHILDREN);

    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeChildren() throws ForbiddenException {
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

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeChildrenAndUserHasNoAccessToCaptureScopeOrgUnit() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(CHILDREN);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeChildrenAndUserHasNoAccessToSearchScopeOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(CHILDREN);

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenOuModeCapture() throws ForbiddenException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOuMode(CAPTURE);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenOuModeAccessible() throws ForbiddenException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOuMode(ACCESSIBLE);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapRequestedOrgUnitWhenOuModeSelected() throws ForbiddenException {
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

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(SELECTED);

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertContainsOnly(List.of(orgUnit), searchParams.getAccessibleOrgUnits());
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
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(trackerAccessManager.canAccess(user, program, orgUnit)).thenReturn(true);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());
    eventCriteria.setOrgUnit(orgUnit.getUid());

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertEquals(SELECTED, searchParams.getOrgUnitSelectionMode());
    assertContainsOnly(List.of(orgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapRequestedOrgUnitAsAccessibleWhenNoOrgUnitProvidedAndNoOrgUnitModeProvided()
      throws ForbiddenException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setProgram(program.getUid());

    EventSearchParams searchParams = mapper.map(eventCriteria);

    assertEquals(ACCESSIBLE, searchParams.getOrgUnitSelectionMode());
    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
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

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventCriteria));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeAccessible() {
    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(ACCESSIBLE);

    Exception exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(eventCriteria));

    assertEquals(
        "Org unit mode ACCESSIBLE cannot be used with an org unit specified. Please remove the org unit and try again.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCapture() {
    EventCriteria eventCriteria = new EventCriteria();
    eventCriteria.setOrgUnit(orgUnit.getUid());
    eventCriteria.setOuMode(CAPTURE);

    Exception exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(eventCriteria));

    assertEquals(
        "Org unit mode CAPTURE cannot be used with an org unit specified. Please remove the org unit and try again.",
        exception.getMessage());
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }
}
