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
package org.hisp.dhis.association;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.association.jdbc.JdbcOrgUnitAssociationsStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the user hierarchy filter of the org unit associations query against Postgres, for a
 * non-superuser. Org unit tree used by all tests:
 *
 * <pre>
 * A
 * ├── B
 * │   └── C
 * └── D
 * </pre>
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Transactional
class OrgUnitAssociationsHierarchyFilterTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private DataSetService dataSetService;

  @Autowired
  @Qualifier("jdbcDataSetOrgUnitAssociationsStore")
  private JdbcOrgUnitAssociationsStore store;

  private OrganisationUnit ouA, ouB, ouC, ouD;

  @BeforeEach
  void setUp() {
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B', ouA);
    ouC = createOrganisationUnit('C', ouB);
    ouD = createOrganisationUnit('D', ouA);
    manager.save(ouA);
    manager.save(ouB);
    manager.save(ouC);
    manager.save(ouD);
  }

  @Test
  void returnsOnlyOrgUnitsWithinUserHierarchy() {
    DataSet dataSet = addDataSet('X', ouC, ouD);
    loginAs(ouB);

    assertAssociations(Set.of(ouC.getUid()), dataSet);
  }

  @Test
  void includesUserOrgUnitItself() {
    DataSet dataSet = addDataSet('X', ouB);
    loginAs(ouB);

    assertAssociations(Set.of(ouB.getUid()), dataSet);
  }

  @Test
  void omitsDataSetWithAllOrgUnitsOutsideUserHierarchy() {
    DataSet dataSet = addDataSet('X', ouD);
    loginAs(ouB);

    SetValuedMap<String, String> associations = getAssociations(dataSet);

    assertFalse(associations.containsKey(dataSet.getUid()), associations::toString);
  }

  @Test
  void returnsNullOrgUnitForDataSetWithoutOrgUnits() {
    DataSet dataSet = addDataSet('X');
    loginAs(ouB);

    assertAssociations(new HashSet<>(Arrays.asList((String) null)), dataSet);
  }

  @Test
  void combinesMultipleUserOrgUnits() {
    DataSet dataSet = addDataSet('X', ouA, ouC, ouD);
    loginAs(ouB, ouD);

    assertAssociations(Set.of(ouC.getUid(), ouD.getUid()), dataSet);
  }

  @Test
  void returnsNoOrgUnitsForUserWithoutOrgUnits() {
    DataSet assigned = addDataSet('X', ouC);
    DataSet unassigned = addDataSet('Y');
    loginAs();

    SetValuedMap<String, String> associations = getAssociations(assigned, unassigned);

    assertFalse(associations.containsKey(assigned.getUid()), associations::toString);
    assertEquals(
        new HashSet<>(Arrays.asList((String) null)), associations.get(unassigned.getUid()));
  }

  private DataSet addDataSet(char uniqueCharacter, OrganisationUnit... orgUnits) {
    DataSet dataSet = createDataSet(uniqueCharacter);
    dataSet.setSharing(Sharing.builder().publicAccess("rw------").build());
    for (OrganisationUnit orgUnit : orgUnits) {
      dataSet.addOrganisationUnit(orgUnit);
    }
    dataSetService.addDataSet(dataSet);
    return dataSet;
  }

  private void loginAs(OrganisationUnit... orgUnits) {
    injectSecurityContextUser(createAndAddUser(false, "user", Set.of(orgUnits), Set.of(orgUnits)));
    manager.flush();
  }

  private void assertAssociations(Set<String> expected, DataSet dataSet) {
    SetValuedMap<String, String> associations = getAssociations(dataSet);
    assertEquals(expected, associations.get(dataSet.getUid()), associations::toString);
  }

  private SetValuedMap<String, String> getAssociations(DataSet... dataSets) {
    Set<String> uids = new HashSet<>();
    for (DataSet dataSet : dataSets) {
      uids.add(dataSet.getUid());
    }
    return store.getOrganisationUnitsAssociationsForCurrentUser(uids, false);
  }
}
