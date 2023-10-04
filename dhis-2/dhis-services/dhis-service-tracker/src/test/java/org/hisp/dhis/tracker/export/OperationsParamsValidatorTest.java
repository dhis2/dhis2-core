/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static java.util.Collections.emptySet;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateAccessibleOrgUnits;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationsParamsValidatorTest {

  @Mock OrganisationUnitService organisationUnitService;

  @Mock TrackerAccessManager trackerAccessManager;

  private OrganisationUnit organisationUnit;
  private static final String PARENT_ORG_UNIT_UID = "parent-org-unit";

  private final OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid3");

  private final OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");

  private final Program program = new Program("program");

  private final User user = new User();

  private final List<OrganisationUnit> orgUnitDescendants =
      List.of(
          createOrgUnit("orgUnit1", "uid1"),
          createOrgUnit("orgUnit2", "uid2"),
          captureScopeOrgUnit,
          searchScopeOrgUnit);

  @BeforeEach
  public void setUp() {
    organisationUnit = createOrgUnit("orgUnit", PARENT_ORG_UNIT_UID);
    organisationUnit.setChildren(Set.of(captureScopeOrgUnit, searchScopeOrgUnit));
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeDescendants()
      throws ForbiddenException {
    when(organisationUnitService.getOrganisationUnitWithChildren(PARENT_ORG_UNIT_UID))
        .thenReturn(orgUnitDescendants);

    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            Set.of(organisationUnit),
            DESCENDANTS,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(captureScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeDescendants() throws ForbiddenException {
    when(organisationUnitService.getOrganisationUnitWithChildren(PARENT_ORG_UNIT_UID))
        .thenReturn(orgUnitDescendants);

    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            Set.of(organisationUnit),
            DESCENDANTS,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(searchScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeDescendantsAndUserHasNoAccessToCaptureScopeOrgUnit() {
    when(organisationUnitService.getOrganisationUnitWithChildren(PARENT_ORG_UNIT_UID))
        .thenReturn(orgUnitDescendants);

    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(createOrgUnit("made-up-org-unit", "made-up-org-unit-uid")));

    Exception exception =
        Assertions.assertThrows(
            ForbiddenException.class,
            () ->
                validateAccessibleOrgUnits(
                    user,
                    Set.of(organisationUnit),
                    DESCENDANTS,
                    program,
                    organisationUnitService::getOrganisationUnitWithChildren,
                    trackerAccessManager));

    assertEquals(
        String.format("User does not have access to orgUnit: %s", PARENT_ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeDescendantsAndUserHasNoAccessToSearchScopeOrgUnit() {
    when(organisationUnitService.getOrganisationUnitWithChildren(PARENT_ORG_UNIT_UID))
        .thenReturn(orgUnitDescendants);

    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(
        Set.of(createOrgUnit("made-up-org-unit", "made-up-org-unit-uid")));

    Exception exception =
        Assertions.assertThrows(
            ForbiddenException.class,
            () ->
                validateAccessibleOrgUnits(
                    user,
                    Set.of(organisationUnit),
                    DESCENDANTS,
                    program,
                    organisationUnitService::getOrganisationUnitWithChildren,
                    trackerAccessManager));

    assertEquals(
        String.format("User does not have access to orgUnit: %s", PARENT_ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeChildren()
      throws ForbiddenException {
    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            Set.of(organisationUnit),
            CHILDREN,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(captureScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeChildren() throws ForbiddenException {
    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            Set.of(organisationUnit),
            CHILDREN,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(searchScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeChildrenAndUserHasNoAccessToCaptureScopeOrgUnit() {
    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(createOrgUnit("made-up-org-unit", "made-up-org-unit-uid")));

    Exception exception =
        Assertions.assertThrows(
            ForbiddenException.class,
            () ->
                validateAccessibleOrgUnits(
                    user,
                    Set.of(organisationUnit),
                    CHILDREN,
                    program,
                    organisationUnitService::getOrganisationUnitWithChildren,
                    trackerAccessManager));

    assertEquals(
        String.format("User does not have access to orgUnit: %s", PARENT_ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeChildrenAndUserHasNoAccessToSearchScopeOrgUnit() {
    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(
        Set.of(createOrgUnit("made-up-org-unit", "made-up-org-unit-uid")));

    Exception exception =
        Assertions.assertThrows(
            ForbiddenException.class,
            () ->
                validateAccessibleOrgUnits(
                    user,
                    Set.of(organisationUnit),
                    CHILDREN,
                    program,
                    organisationUnitService::getOrganisationUnitWithChildren,
                    trackerAccessManager));

    assertEquals(
        String.format("User does not have access to orgUnit: %s", PARENT_ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenOuModeCapture() throws ForbiddenException {
    program.setAccessLevel(OPEN);
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            emptySet(),
            CAPTURE,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(captureScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenOuModeAccessibleAndProgramOpen() throws ForbiddenException {
    program.setAccessLevel(OPEN);
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            emptySet(),
            ACCESSIBLE,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(searchScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenOuModeAccessibleAndProgramProtected()
      throws ForbiddenException {
    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            emptySet(),
            ACCESSIBLE,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(captureScopeOrgUnit), accessibleOrgUnits);
  }

  @Test
  void shouldMapRequestedOrgUnitWhenOuModeSelected() throws ForbiddenException {
    program.setAccessLevel(PROTECTED);
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(trackerAccessManager.canAccess(user, program, organisationUnit)).thenReturn(true);

    Set<OrganisationUnit> accessibleOrgUnits =
        validateAccessibleOrgUnits(
            user,
            Set.of(organisationUnit),
            SELECTED,
            program,
            organisationUnitService::getOrganisationUnitWithChildren,
            trackerAccessManager);

    assertContainsOnly(Set.of(organisationUnit), accessibleOrgUnits);
  }

  @Test
  void shouldFailWhenOuModeCaptureAndUserHasNoOrgUnitsAssigned() {
    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> validateOrgUnitMode(CAPTURE, new User(), program));

    assertEquals("User needs to be assigned data capture orgunits", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "ACCESSIBLE", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeRequiresUserScopeOrgUnitAndUserHasNoOrgUnitsAssigned(
      OrganisationUnitSelectionMode orgUnitMode) {
    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> validateOrgUnitMode(orgUnitMode, new User(), program));

    assertEquals(
        "User needs to be assigned either search or data capture org units",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenOuModeAllAndNotSuperuser() {
    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> validateOrgUnitMode(ALL, new User(), program));

    assertEquals(
        "Current user is not authorized to query across all organisation units",
        exception.getMessage());
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }
}
