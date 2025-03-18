/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.split.orgunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
class OrgUnitSplitServiceTest extends PostgresIntegrationTestBase {

  @Autowired private OrgUnitSplitService service;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private PeriodService periodService;

  private PeriodType ptA;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @BeforeEach
  void setUp() {
    ptA = periodService.getPeriodTypeByClass(MonthlyPeriodType.class);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    idObjectManager.save(ouC);
  }

  @Test
  void testGetFromQuery() {
    OrgUnitSplitQuery query = new OrgUnitSplitQuery();
    query.setSource(BASE_OU_UID + 'A');
    query.setTargets(Lists.newArrayList(BASE_OU_UID + 'B', BASE_OU_UID + 'C'));
    query.setPrimaryTarget(BASE_OU_UID + 'B');
    OrgUnitSplitRequest request = service.getFromQuery(query);
    assertEquals(ouA, request.getSource());
    assertEquals(2, request.getTargets().size());
    assertTrue(request.getTargets().contains(ouB));
    assertTrue(request.getTargets().contains(ouC));
    assertEquals(ouB, request.getPrimaryTarget());
    assertTrue(request.isDeleteSource());
  }

  @Test
  void testTargetOrgUnitNotFound() {
    OrgUnitSplitQuery query = new OrgUnitSplitQuery();
    query.setSource(BASE_OU_UID + 'A');
    query.setTargets(Lists.newArrayList(BASE_OU_UID + 'B', BASE_OU_UID + 'X'));
    query.setPrimaryTarget(BASE_OU_UID + 'B');
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getFromQuery(query));
    assertEquals(ErrorCode.E1515, ex.getErrorCode());
  }

  @Test
  void testGetFromQueryWithoutPrimaryTarget() {
    OrgUnitSplitQuery query = new OrgUnitSplitQuery();
    query.setSource(BASE_OU_UID + 'A');
    query.setTargets(Lists.newArrayList(BASE_OU_UID + 'B', BASE_OU_UID + 'C'));
    OrgUnitSplitRequest request = service.getFromQuery(query);
    assertEquals(ouA, request.getSource());
    assertEquals(2, request.getTargets().size());
    assertTrue(request.getTargets().contains(ouB));
    assertTrue(request.getTargets().contains(ouC));
    assertEquals(ouB, request.getPrimaryTarget());
    assertTrue(request.isDeleteSource());
  }

  @Test
  void testSplit() {
    DataSet dsA = createDataSet('A', ptA);
    dsA.addOrganisationUnit(ouA);
    DataSet dsB = createDataSet('B', ptA);
    dsB.addOrganisationUnit(ouA);
    idObjectManager.save(dsA);
    idObjectManager.save(dsB);
    Program prA = createProgram('A');
    prA.addOrganisationUnit(ouA);
    Program prB = createProgram('B');
    prB.addOrganisationUnit(ouA);
    idObjectManager.save(ouA);
    idObjectManager.save(ouB);
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouA.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouB.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouC.getUid()));
    OrgUnitSplitRequest request =
        new OrgUnitSplitRequest.Builder()
            .withSource(ouA)
            .addTarget(ouB)
            .addTarget(ouC)
            .withPrimaryTarget(ouB)
            .build();
    assertEquals(2, ouA.getDataSets().size());
    assertEquals(0, ouB.getDataSets().size());
    assertEquals(0, ouC.getDataSets().size());
    assertEquals(2, ouA.getPrograms().size());
    assertEquals(0, ouB.getPrograms().size());
    assertEquals(0, ouC.getPrograms().size());
    service.split(request);
    assertEquals(2, ouB.getDataSets().size());
    assertEquals(2, ouC.getDataSets().size());
    assertEquals(2, ouB.getPrograms().size());
    assertEquals(2, ouC.getPrograms().size());
    assertNull(idObjectManager.get(OrganisationUnit.class, ouA.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouB.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouC.getUid()));
  }

  @Test
  @DisplayName("OrgUnit split has correct users for new split org units")
  void orgUnitSplitCorrectUsersTest() {
    // given multiple users
    // each of which have different kinds of access to the same org unit
    Set<OrganisationUnit> source = new HashSet<>(Collections.singletonList(ouA));

    User userWithNoOrgUnits = createAndAddUser("user1");

    User userDataCaptureOrgUnits = createAndAddUser("user2");
    userDataCaptureOrgUnits.setOrganisationUnits(source);

    User userDataViewOrgUnits = createAndAddUser("user3");
    userDataViewOrgUnits.setDataViewOrganisationUnits(source);

    User userTeiSearchOrgUnits = createAndAddUser("user4");
    userTeiSearchOrgUnits.setTeiSearchOrganisationUnits(source);

    User userAllOrgUnits = createAndAddUser("user5");
    userAllOrgUnits.setOrganisationUnits(source);
    userAllOrgUnits.setDataViewOrganisationUnits(source);
    userAllOrgUnits.setTeiSearchOrganisationUnits(source);

    idObjectManager.save(
        List.of(
            userWithNoOrgUnits,
            userAllOrgUnits,
            userDataCaptureOrgUnits,
            userDataViewOrgUnits,
            userTeiSearchOrgUnits));

    assertUserHasExpectedOrgUnits(userAllOrgUnits, 1, 1, 1);
    assertUserHasExpectedOrgUnits(userWithNoOrgUnits, 0, 0, 0);
    assertUserHasExpectedOrgUnits(userDataCaptureOrgUnits, 1, 0, 0);
    assertUserHasExpectedOrgUnits(userDataViewOrgUnits, 0, 1, 0);
    assertUserHasExpectedOrgUnits(userTeiSearchOrgUnits, 0, 0, 1);

    OrgUnitSplitRequest request =
        new OrgUnitSplitRequest.Builder()
            .withSource(ouA)
            .addTargets(Set.of(ouB, ouC))
            .withPrimaryTarget(ouB)
            .withDeleteSource(true)
            .build();

    // when
    service.split(request);

    // then all users should have the appropriate access for the split org units
    assertUserHasExpectedOrgUnits(userAllOrgUnits, 2, 2, 2);
    assertUserHasExpectedOrgUnits(userWithNoOrgUnits, 0, 0, 0);
    assertUserHasExpectedOrgUnits(userDataCaptureOrgUnits, 2, 0, 0);
    assertUserHasExpectedOrgUnits(userDataViewOrgUnits, 0, 2, 0);
    assertUserHasExpectedOrgUnits(userTeiSearchOrgUnits, 0, 0, 2);
  }

  private void assertUserHasExpectedOrgUnits(
      User user, int orgUnits, int dataViewOrgUnits, int teiSearchOrgUnits) {
    assertEquals(
        orgUnits,
        user.getOrganisationUnits().size(),
        "user should have %s org units".formatted(orgUnits));
    assertEquals(
        dataViewOrgUnits,
        user.getDataViewOrganisationUnits().size(),
        "user should have %s data view org units".formatted(dataViewOrgUnits));
    assertEquals(
        teiSearchOrgUnits,
        user.getTeiSearchOrganisationUnits().size(),
        "user should have %s tei search org units".formatted(teiSearchOrgUnits));
  }
}
