/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.resourcetable.table;

import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.jupiter.api.Test;

class OrganisationUnitStructureResourceTableTest {
  @Test
  void testCreateBatchObjectsWhenLevelsAreSame() {
    int maxOrgUnitLevels = 3;
    int currentLevel = 3;

    OrganisationUnit root = createOrganisationUnit('A');
    root.setPath("/p1");

    OrganisationUnit ou1 = createOrganisationUnit('B', root);
    ou1.setPath("/p1/p2");

    OrganisationUnit ou2 = createOrganisationUnit('C', ou1);
    ou2.setHierarchyLevel(currentLevel);
    ou2.setPath("/p1/p2/ou2");

    OrganisationUnit ou3 = createOrganisationUnit('D', ou1);
    ou3.setHierarchyLevel(currentLevel);
    ou3.setPath("/p1/p2/ou3");

    List<OrganisationUnit> organisationUnits = new ArrayList<>();
    organisationUnits.add(ou2);
    organisationUnits.add(ou3);

    OrganisationUnitStructureResourceTable resourceTable =
        new OrganisationUnitStructureResourceTable(null, maxOrgUnitLevels, null);

    assertDoesNotThrow(() -> resourceTable.createBatchObjects(organisationUnits, currentLevel));
  }

  @Test
  void testCreateBatchObjectsWhenHierarchyLevelIsLowerThanMaxLevel() {
    int maxOrgUnitLevels = 3;
    int currentLevel = 2;

    OrganisationUnit root = createOrganisationUnit('A');
    root.setPath("/p1");

    OrganisationUnit ou1 = createOrganisationUnit('B', root);
    ou1.setPath("/p1/p2");

    List<OrganisationUnit> organisationUnits = new ArrayList<>();
    organisationUnits.add(ou1);

    OrganisationUnitStructureResourceTable resourceTable =
        new OrganisationUnitStructureResourceTable(null, maxOrgUnitLevels, null);

    assertDoesNotThrow(() -> resourceTable.createBatchObjects(organisationUnits, currentLevel));
  }

  @Test
  void testCreateBatchObjectsWhenCurrentLevelIsLargerThanMaxLevel() {
    int maxOrgUnitLevels = 2;
    int currentLevel = 3;

    OrganisationUnit root = createOrganisationUnit('A');
    root.setPath("/p1");

    OrganisationUnit ou1 = createOrganisationUnit('B', root);
    ou1.setPath("/p1/p2");
    ou1.setUid("uid-123");

    List<OrganisationUnit> organisationUnits = new ArrayList<>();
    organisationUnits.add(ou1);

    OrganisationUnitStructureResourceTable resourceTable =
        new OrganisationUnitStructureResourceTable(null, maxOrgUnitLevels, null);

    Exception ex =
        assertThrows(
            IllegalStateException.class,
            () -> resourceTable.createBatchObjects(organisationUnits, currentLevel));

    assertEquals(
        "Invalid hierarchy level or missing parent for organisation unit uid-123.",
        ex.getMessage());
  }

  @Test
  void testCreateBatchObjectsWhenCurrentLevelHasNoParent() {
    int maxOrgUnitLevels = 2;
    int currentLevel = 3;

    OrganisationUnit root = createOrganisationUnit('A');
    root.setPath("/p1");

    OrganisationUnit ou1 = createOrganisationUnit('B');
    ou1.setPath("/p1/p2");
    ou1.setUid("uid-123");

    List<OrganisationUnit> organisationUnits = new ArrayList<>();
    organisationUnits.add(ou1);

    OrganisationUnitStructureResourceTable resourceTable =
        new OrganisationUnitStructureResourceTable(null, maxOrgUnitLevels, null);

    Exception ex =
        assertThrows(
            IllegalStateException.class,
            () -> resourceTable.createBatchObjects(organisationUnits, currentLevel));

    assertEquals(
        "Invalid hierarchy level or missing parent for organisation unit uid-123.",
        ex.getMessage());
  }
}
