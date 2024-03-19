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
package org.hisp.dhis.analytics.orgunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgUnitHelperTest {
  @Test
  void getActiveOrganisationUnits_returns_only_active_units() {
    // Given
    List<OrganisationUnit> rqOrgUnit = new ArrayList<>();

    rqOrgUnit.add(new OrganisationUnit("name1"));
    rqOrgUnit.add(new OrganisationUnit("name2"));
    rqOrgUnit.add(new OrganisationUnit("name3"));

    rqOrgUnit.get(0).setUid(CodeGenerator.generateUid());
    rqOrgUnit.get(1).setUid(CodeGenerator.generateUid());
    String target = CodeGenerator.generateUid();
    rqOrgUnit.get(2).setUid(target);

    GridHeader gridHeader = new GridHeader("ou");

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);
    grid.addRow().addColumn(List.of(target));

    // When
    List<OrganisationUnit> rsOrgUnits = OrgUnitHelper.getActiveOrganisationUnits(grid, rqOrgUnit);

    // Then
    assertEquals(1, rsOrgUnits.size());
  }

  @Test
  void getActiveOrganisationUnits_returns_same_units() {
    // Given
    List<OrganisationUnit> rqOrgUnit = new ArrayList<>();

    rqOrgUnit.add(new OrganisationUnit("name1"));
    rqOrgUnit.add(new OrganisationUnit("name2"));
    rqOrgUnit.add(new OrganisationUnit("name3"));

    rqOrgUnit.get(0).setUid(CodeGenerator.generateUid());
    rqOrgUnit.get(1).setUid(CodeGenerator.generateUid());
    String target = CodeGenerator.generateUid();
    rqOrgUnit.get(2).setUid(target);

    GridHeader gridHeader = new GridHeader("ou");

    Grid grid = new ListGrid();
    grid.addHeader(gridHeader);

    // When
    List<OrganisationUnit> rsOrgUnits = OrgUnitHelper.getActiveOrganisationUnits(grid, rqOrgUnit);

    // Then
    assertEquals(3, rsOrgUnits.size());
  }

  @Test
  void getActiveOrganisationUnits_returns_null_when_called_with_null_param() {
    // Given, When, Then
    assertNull(OrgUnitHelper.getActiveOrganisationUnits(null, null));
    assertNull(OrgUnitHelper.getActiveOrganisationUnits(new ListGrid(), null));
  }
}
