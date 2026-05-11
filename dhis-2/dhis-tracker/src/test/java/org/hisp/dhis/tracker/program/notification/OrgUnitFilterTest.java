/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.program.notification;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.tracker.imports.notification.GroupMemberInfo;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultProgramNotificationService#filterByOrgUnit} and {@link
 * DefaultProgramNotificationService#extractParentUidFromPath}.
 *
 * <p>Hierarchy used in tests:
 *
 * <pre>
 *   Country (countryUid)
 *     └── Region (regionUid)
 *           └── Facility (facilityUid)
 * </pre>
 *
 * Facility path: {@code /countryUid/regionUid/facilityUid}
 */
class OrgUnitFilterTest {

  @Test
  void shouldReturnAllMembersWhenNoFilterIsSet() {
    ProgramNotificationTemplate template = template(false, false);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userAtCountry = new GroupMemberInfo(1, "countryUid");
    GroupMemberInfo userAtRegion = new GroupMemberInfo(2, "regionUid");
    GroupMemberInfo userAtFacility = new GroupMemberInfo(3, "facilityUid");
    GroupMemberInfo userElsewhere = new GroupMemberInfo(4, "otherUid");
    GroupMemberInfo userNoOrgUnit = new GroupMemberInfo(5, null);

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template,
            facility,
            Set.of(userAtCountry, userAtRegion, userAtFacility, userElsewhere, userNoOrgUnit));

    assertContainsOnly(
        Set.of(userAtCountry, userAtRegion, userAtFacility, userElsewhere, userNoOrgUnit), result);
  }

  @Test
  void shouldKeepMembersInHierarchy() {
    ProgramNotificationTemplate template = template(true, false);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userAtCountry = new GroupMemberInfo(1, "countryUid");
    GroupMemberInfo userAtRegion = new GroupMemberInfo(2, "regionUid");
    GroupMemberInfo userAtFacility = new GroupMemberInfo(3, "facilityUid");

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, facility, Set.of(userAtCountry, userAtRegion, userAtFacility));

    assertContainsOnly(Set.of(userAtCountry, userAtRegion, userAtFacility), result);
  }

  @Test
  void shouldExcludeMembersOutsideHierarchy() {
    ProgramNotificationTemplate template = template(true, false);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userInHierarchy = new GroupMemberInfo(1, "regionUid");
    GroupMemberInfo userOutside = new GroupMemberInfo(2, "otherUid");

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, facility, Set.of(userInHierarchy, userOutside));

    assertContainsOnly(Set.of(userInHierarchy), result);
  }

  @Test
  void shouldExcludeMembersWithNullOrgUnitInHierarchyFilter() {
    ProgramNotificationTemplate template = template(true, false);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userNoOrgUnit = new GroupMemberInfo(1, null);

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, facility, Set.of(userNoOrgUnit));

    assertEquals(Set.of(), result);
  }

  @Test
  void shouldHandleRootOrgUnitInHierarchyFilter() {
    ProgramNotificationTemplate template = template(true, false);
    OrganisationUnit root = orgUnit("/rootUid");

    GroupMemberInfo userAtRoot = new GroupMemberInfo(1, "rootUid");
    GroupMemberInfo userElsewhere = new GroupMemberInfo(2, "otherUid");

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, root, Set.of(userAtRoot, userElsewhere));

    assertContainsOnly(Set.of(userAtRoot), result);
  }

  @Test
  void shouldKeepOnlyMembersAtParentOrgUnit() {
    ProgramNotificationTemplate template = template(false, true);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userAtRegion = new GroupMemberInfo(1, "regionUid");
    GroupMemberInfo userAtCountry = new GroupMemberInfo(2, "countryUid");
    GroupMemberInfo userAtFacility = new GroupMemberInfo(3, "facilityUid");

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, facility, Set.of(userAtRegion, userAtCountry, userAtFacility));

    assertContainsOnly(Set.of(userAtRegion), result);
  }

  @Test
  void shouldReturnEmptyWhenRootOrgUnitHasNoParent() {
    ProgramNotificationTemplate template = template(false, true);
    OrganisationUnit root = orgUnit("/rootUid");

    GroupMemberInfo userAtRoot = new GroupMemberInfo(1, "rootUid");

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(template, root, Set.of(userAtRoot));

    assertEquals(Set.of(), result);
  }

  @Test
  void shouldExcludeMembersWithNullOrgUnitInParentFilter() {
    ProgramNotificationTemplate template = template(false, true);
    OrganisationUnit facility = orgUnit("/countryUid/regionUid/facilityUid");

    GroupMemberInfo userNoOrgUnit = new GroupMemberInfo(1, null);

    Set<GroupMemberInfo> result =
        DefaultProgramNotificationService.filterByOrgUnit(
            template, facility, Set.of(userNoOrgUnit));

    assertEquals(Set.of(), result);
  }

  @Test
  void shouldExtractParentFromThreeLevelPath() {
    assertEquals(
        "regionUid",
        DefaultProgramNotificationService.extractParentUidFromPath(
            "/countryUid/regionUid/facilityUid"));
  }

  @Test
  void shouldExtractParentFromTwoLevelPath() {
    assertEquals(
        "countryUid",
        DefaultProgramNotificationService.extractParentUidFromPath("/countryUid/regionUid"));
  }

  @Test
  void shouldReturnNullForRootPath() {
    assertNull(DefaultProgramNotificationService.extractParentUidFromPath("/rootUid"));
  }

  @Test
  void shouldReturnNullForNullPath() {
    assertNull(DefaultProgramNotificationService.extractParentUidFromPath(null));
  }

  private static ProgramNotificationTemplate template(boolean hierarchyOnly, boolean parentOnly) {
    ProgramNotificationTemplate t = new ProgramNotificationTemplate();
    t.setNotifyUsersInHierarchyOnly(hierarchyOnly);
    t.setNotifyParentOrganisationUnitOnly(parentOnly);
    return t;
  }

  private static OrganisationUnit orgUnit(String path) {
    OrganisationUnit ou = new OrganisationUnit();
    ou.setPath(path);
    return ou;
  }
}
