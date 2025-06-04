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
package org.hisp.dhis.merge.orgunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.merge.DataMergeStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class OrgUnitMergeServiceTest extends IntegrationTestBase {

  @Autowired private OrgUnitMergeService service;
  @Autowired private IdentifiableObjectManager idObjectManager;
  @Autowired private PeriodService periodService;
  @Autowired private UserService _userService;

  private PeriodType ptA;
  private OrganisationUnit ouA;
  private OrganisationUnit ouB;
  private OrganisationUnit ouC;
  private OrganisationUnit ouD;
  private OrganisationUnit ouE;
  private OrganisationUnit ouF;

  @Override
  public void setUpTest() {
    userService = _userService;
    ptA = periodService.getPeriodTypeByClass(MonthlyPeriodType.class);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    ouD = createOrganisationUnit('D');
    ouE = createOrganisationUnit('E');
    ouF = createOrganisationUnit('F');
    idObjectManager.save(List.of(ouA, ouB, ouC, ouD, ouE, ouF));
  }

  @Test
  void testGetFromQuery() {
    OrgUnitMergeQuery query = new OrgUnitMergeQuery();
    query.setSources(Lists.newArrayList(BASE_OU_UID + 'A', BASE_OU_UID + 'B'));
    query.setTarget(BASE_OU_UID + 'C');
    OrgUnitMergeRequest request = service.getFromQuery(query);
    assertEquals(2, request.getSources().size());
    assertTrue(request.getSources().contains(ouA));
    assertTrue(request.getSources().contains(ouB));
    assertEquals(ouC, request.getTarget());
    assertEquals(DataMergeStrategy.LAST_UPDATED, request.getDataValueMergeStrategy());
    assertEquals(DataMergeStrategy.LAST_UPDATED, request.getDataApprovalMergeStrategy());
    assertTrue(request.isDeleteSources());
  }

  @Test
  void testSourceOrgUnitNotFound() {
    OrgUnitMergeQuery query = new OrgUnitMergeQuery();
    query.setSources(Lists.newArrayList(BASE_OU_UID + 'A', BASE_OU_UID + 'X'));
    query.setTarget(BASE_OU_UID + 'C');
    IllegalQueryException ex =
        assertThrows(IllegalQueryException.class, () -> service.getFromQuery(query));
    assertEquals(ErrorCode.E1503, ex.getErrorCode());
  }

  @Test
  void testMerge() {
    DataSet dsA = createDataSet('A', ptA);
    dsA.addOrganisationUnit(ouA);
    dsA.addOrganisationUnit(ouB);
    DataSet dsB = createDataSet('B', ptA);
    dsB.addOrganisationUnit(ouA);
    idObjectManager.save(dsA);
    idObjectManager.save(dsB);
    OrganisationUnitGroup ougA = createOrganisationUnitGroup('A');
    ougA.addOrganisationUnit(ouA);
    ougA.addOrganisationUnit(ouB);
    OrganisationUnitGroup ougB = createOrganisationUnitGroup('B');
    ougB.addOrganisationUnit(ouA);
    idObjectManager.save(ougA);
    idObjectManager.save(ougB);
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouA.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouB.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouC.getUid()));
    assertEquals(2, ouA.getDataSets().size());
    assertEquals(1, ouB.getDataSets().size());
    assertEquals(0, ouC.getDataSets().size());
    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder()
            .addSource(ouA)
            .addSource(ouB)
            .withTarget(ouC)
            .withDeleteSources(true)
            .build();
    service.merge(request);
    assertEquals(2, ouC.getGroups().size());
    assertNull(idObjectManager.get(OrganisationUnit.class, ouA.getUid()));
    assertNull(idObjectManager.get(OrganisationUnit.class, ouB.getUid()));
    assertNotNull(idObjectManager.get(OrganisationUnit.class, ouC.getUid()));
  }

  @Test
  @DisplayName("OrgUnit merge has correct users for new merged org unit")
  void orgUnitMergeCorrectUsersTest() {
    // given multiple users
    // each of which have different kinds of access to the source org units
    User mergeUser = createAndAddUser("mergeUser", ouA, "ALL");
    injectSecurityContext(UserDetails.fromUser(mergeUser));
    Set<OrganisationUnit> sources = new HashSet<>(Arrays.asList(ouD, ouE));

    User userWithNoOrgUnits = createAndAddUser("user1");

    User userDataCaptureOrgUnits = createAndAddUser("user2");
    userDataCaptureOrgUnits.setOrganisationUnits(sources);

    User userDataViewOrgUnits = createAndAddUser("user3");
    userDataViewOrgUnits.setDataViewOrganisationUnits(sources);

    User userTeiSearchOrgUnits = createAndAddUser("user4");
    userTeiSearchOrgUnits.setTeiSearchOrganisationUnits(sources);

    User userAllOrgUnits = createAndAddUser("user5");
    userAllOrgUnits.setOrganisationUnits(sources);
    userAllOrgUnits.setDataViewOrganisationUnits(sources);
    userAllOrgUnits.setTeiSearchOrganisationUnits(sources);

    idObjectManager.save(
        List.of(
            userWithNoOrgUnits,
            userAllOrgUnits,
            userDataCaptureOrgUnits,
            userDataViewOrgUnits,
            userTeiSearchOrgUnits));

    assertUserHasExpectedOrgUnits(userAllOrgUnits, 2, 2, 2);
    assertUserHasExpectedOrgUnits(userWithNoOrgUnits, 0, 0, 0);
    assertUserHasExpectedOrgUnits(userDataCaptureOrgUnits, 2, 0, 0);
    assertUserHasExpectedOrgUnits(userDataViewOrgUnits, 0, 2, 0);
    assertUserHasExpectedOrgUnits(userTeiSearchOrgUnits, 0, 0, 2);

    OrgUnitMergeRequest request =
        new OrgUnitMergeRequest.Builder()
            .addSources(sources)
            .withTarget(ouF)
            .withDeleteSources(true)
            .build();

    // when
    service.merge(request);

    // then all users should have the appropriate access for the merged org units
    assertUserHasExpectedOrgUnits(userAllOrgUnits, 1, 1, 1);
    assertUserHasExpectedOrgUnits(userWithNoOrgUnits, 0, 0, 0);
    assertUserHasExpectedOrgUnits(userDataCaptureOrgUnits, 1, 0, 0);
    assertUserHasExpectedOrgUnits(userDataViewOrgUnits, 0, 1, 0);
    assertUserHasExpectedOrgUnits(userTeiSearchOrgUnits, 0, 0, 1);

    assertUserOnlyHasMergedOrgUnit(userAllOrgUnits.getOrganisationUnits(), ouF);
    assertUserOnlyHasMergedOrgUnit(userAllOrgUnits.getDataViewOrganisationUnits(), ouF);
    assertUserOnlyHasMergedOrgUnit(userAllOrgUnits.getTeiSearchOrganisationUnits(), ouF);
    assertUserOnlyHasMergedOrgUnit(userDataCaptureOrgUnits.getTeiSearchOrganisationUnits(), ouF);
    assertUserOnlyHasMergedOrgUnit(userDataViewOrgUnits.getTeiSearchOrganisationUnits(), ouF);
    assertUserOnlyHasMergedOrgUnit(userTeiSearchOrgUnits.getTeiSearchOrganisationUnits(), ouF);
  }

  private void assertUserOnlyHasMergedOrgUnit(
      Set<OrganisationUnit> orgUnits, OrganisationUnit target) {
    assertTrue(orgUnits.stream().allMatch(ou -> ou.equals(target)));
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
