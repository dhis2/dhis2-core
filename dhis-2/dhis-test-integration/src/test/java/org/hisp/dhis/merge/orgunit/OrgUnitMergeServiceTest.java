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
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class OrgUnitMergeServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private OrgUnitMergeService service;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private PeriodService periodService;

  private PeriodType ptA;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  @Override
  public void setUpTest() {
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
}
