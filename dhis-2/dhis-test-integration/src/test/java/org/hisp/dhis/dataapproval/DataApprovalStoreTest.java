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
package org.hisp.dhis.dataapproval;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataApprovalStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataApprovalStore dataApprovalStore;

  @Autowired private DataApprovalLevelService dataApprovalLevelService;

  @Autowired private DataApprovalService dataApprovalService;

  @Autowired private PeriodService periodService;

  @Autowired private CategoryService categoryService;

  @Autowired private OrganisationUnitService organisationUnitService;

  // -------------------------------------------------------------------------
  // Supporting data
  // -------------------------------------------------------------------------
  private DataApprovalLevel level1;

  private DataApprovalLevel level2;

  private DataApprovalWorkflow workflowA1;

  private DataApprovalWorkflow workflowA12;

  private DataApprovalWorkflow workflowB12;

  private Period periodA;

  private Period periodB;

  private OrganisationUnit sourceA;

  private OrganisationUnit sourceB;

  private OrganisationUnit sourceC;

  private OrganisationUnit sourceD;

  private User userA;

  private User userB;

  private CategoryOptionCombo categoryOptionCombo;

  @BeforeAll
  void setUp() {
    // ---------------------------------------------------------------------
    // Add supporting data
    // ---------------------------------------------------------------------
    CategoryCombo categoryCombo = categoryService.getDefaultCategoryCombo();
    categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
    level1 = new DataApprovalLevel("01", 1, null);
    level2 = new DataApprovalLevel("02", 2, null);
    dataApprovalLevelService.addDataApprovalLevel(level1);
    dataApprovalLevelService.addDataApprovalLevel(level2);
    PeriodType periodType = PeriodType.getPeriodTypeByName("Monthly");
    workflowA1 =
        new DataApprovalWorkflow("workflowA1", periodType, categoryCombo, newHashSet(level1));
    workflowA12 =
        new DataApprovalWorkflow(
            "workflowA12", periodType, categoryCombo, newHashSet(level1, level2));
    workflowB12 =
        new DataApprovalWorkflow(
            "workflowB12", periodType, categoryCombo, newHashSet(level1, level2));
    dataApprovalService.addWorkflow(workflowA1);
    dataApprovalService.addWorkflow(workflowA12);
    dataApprovalService.addWorkflow(workflowB12);
    periodA = createPeriod(getDay(5), getDay(6));
    periodB = createPeriod(getDay(6), getDay(7));
    periodService.addPeriod(periodA);
    periodService.addPeriod(periodB);
    sourceA = createOrganisationUnit('A');
    sourceB = createOrganisationUnit('B', sourceA);
    sourceC = createOrganisationUnit('C', sourceB);
    sourceD = createOrganisationUnit('D', sourceC);
    organisationUnitService.addOrganisationUnit(sourceA);
    organisationUnitService.addOrganisationUnit(sourceB);
    organisationUnitService.addOrganisationUnit(sourceC);
    organisationUnitService.addOrganisationUnit(sourceD);
    userA = makeUser("A");
    userB = makeUser("B");
    userService.addUser(userA);
    userService.addUser(userB);
  }

  // -------------------------------------------------------------------------
  // Basic DataApproval
  // -------------------------------------------------------------------------
  @Test
  @Disabled("DHIS2-19679 loading DAs causes conflict because of Period same ID different instances")
  void testAddAndGetDataApproval() {
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2, workflowA12, periodA, sourceB, categoryOptionCombo, false, date, userA);
    DataApproval dataApprovalC =
        new DataApproval(
            level1, workflowA12, periodB, sourceA, categoryOptionCombo, false, date, userA);
    DataApproval dataApprovalD =
        new DataApproval(
            level1, workflowB12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    DataApproval dataApprovalE = null;
    dataApprovalStore.addDataApproval(dataApprovalA);
    dataApprovalStore.addDataApproval(dataApprovalB);
    dataApprovalStore.addDataApproval(dataApprovalC);
    dataApprovalStore.addDataApproval(dataApprovalD);
    dataApprovalA =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    assertNotNull(dataApprovalA);
    assertEquals(level1.getId(), dataApprovalA.getDataApprovalLevel().getId());
    assertEquals(workflowA12.getId(), dataApprovalA.getWorkflow().getId());
    assertEquals(periodA, dataApprovalA.getPeriod());
    assertEquals(sourceA.getId(), dataApprovalA.getOrganisationUnit().getId());
    assertEquals(date, dataApprovalA.getCreated());
    assertEquals(userA.getId(), dataApprovalA.getCreator().getId());
    dataApprovalB =
        dataApprovalStore.getDataApproval(
            level2, workflowA12, periodA, sourceB, categoryOptionCombo);
    assertNotNull(dataApprovalB);
    assertEquals(level2.getId(), dataApprovalB.getDataApprovalLevel().getId());
    assertEquals(workflowA12.getId(), dataApprovalB.getWorkflow().getId());
    assertEquals(periodA, dataApprovalB.getPeriod());
    assertEquals(sourceB.getId(), dataApprovalB.getOrganisationUnit().getId());
    assertEquals(date, dataApprovalB.getCreated());
    assertEquals(userA.getId(), dataApprovalB.getCreator().getId());
    dataApprovalC =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodB, sourceA, categoryOptionCombo);
    assertNotNull(dataApprovalC);
    assertEquals(level1.getId(), dataApprovalC.getDataApprovalLevel().getId());
    assertEquals(workflowA12.getId(), dataApprovalC.getWorkflow().getId());
    assertEquals(periodB, dataApprovalC.getPeriod());
    assertEquals(sourceA.getId(), dataApprovalC.getOrganisationUnit().getId());
    assertEquals(date, dataApprovalC.getCreated());
    assertEquals(userA.getId(), dataApprovalC.getCreator().getId());
    dataApprovalD =
        dataApprovalStore.getDataApproval(
            level1, workflowB12, periodA, sourceA, categoryOptionCombo);
    assertNotNull(dataApprovalD);
    assertEquals(level1.getId(), dataApprovalD.getDataApprovalLevel().getId());
    assertEquals(workflowB12.getId(), dataApprovalD.getWorkflow().getId());
    assertEquals(periodA, dataApprovalD.getPeriod());
    assertEquals(sourceA.getId(), dataApprovalD.getOrganisationUnit().getId());
    assertEquals(date, dataApprovalD.getCreated());
    assertEquals(userA.getId(), dataApprovalD.getCreator().getId());
    dataApprovalE =
        dataApprovalStore.getDataApproval(
            level1, workflowB12, periodB, sourceB, categoryOptionCombo);
    assertNull(dataApprovalE);
  }

  @Test
  void testDeleteDataApproval() {
    dataApprovalLevelService.addDataApprovalLevel(level1);
    dataApprovalLevelService.addDataApprovalLevel(level2);
    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    DataApproval dataApprovalB =
        new DataApproval(
            level2, workflowB12, periodB, sourceB, categoryOptionCombo, false, date, userB);
    dataApprovalStore.addDataApproval(dataApprovalA);
    dataApprovalStore.addDataApproval(dataApprovalB);
    dataApprovalA =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    assertNotNull(dataApprovalA);
    dataApprovalB =
        dataApprovalStore.getDataApproval(
            level2, workflowB12, periodB, sourceB, categoryOptionCombo);
    assertNotNull(dataApprovalB);
    dataApprovalStore.deleteDataApproval(dataApprovalA);
    dataApprovalA =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    assertNull(dataApprovalA);
    dataApprovalB =
        dataApprovalStore.getDataApproval(
            level2, workflowB12, periodB, sourceB, categoryOptionCombo);
    assertNotNull(dataApprovalB);
    dataApprovalStore.deleteDataApproval(dataApprovalB);
    dataApprovalA =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    assertNull(dataApprovalA);
    dataApprovalB =
        dataApprovalStore.getDataApproval(
            level2, workflowB12, periodB, sourceB, categoryOptionCombo);
    assertNull(dataApprovalB);
  }

  @Test
  @DisplayName("Retrieving DataApprovals by CategoryOptionCombo returns expected results")
  void getByCocTest() {
    // given
    CategoryOptionCombo coc1 = createCategoryOptionCombo('1');
    coc1.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc1);

    CategoryOptionCombo coc2 = createCategoryOptionCombo('2');
    coc2.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc2);

    CategoryOptionCombo coc3 = createCategoryOptionCombo('3');
    coc3.setCategoryCombo(categoryService.getDefaultCategoryCombo());
    categoryService.addCategoryOptionCombo(coc3);

    Date date = new Date();
    DataApproval dataApprovalA =
        new DataApproval(level1, workflowA12, periodA, sourceA, coc1, false, date, userA);
    DataApproval dataApprovalB =
        new DataApproval(level2, workflowA12, periodA, sourceB, coc2, false, date, userA);
    DataApproval dataApprovalC =
        new DataApproval(level1, workflowA12, periodB, sourceA, coc3, false, date, userA);

    dataApprovalStore.addDataApproval(dataApprovalA);
    dataApprovalStore.addDataApproval(dataApprovalB);
    dataApprovalStore.addDataApproval(dataApprovalC);

    // when
    List<DataApproval> allByCoc =
        dataApprovalStore.getByCategoryOptionCombo(UID.of(coc1.getUid(), coc2.getUid()));

    // then
    assertEquals(2, allByCoc.size());
    assertTrue(
        allByCoc.containsAll(List.of(dataApprovalA, dataApprovalB)),
        "Retrieved result set should contain both DataApprovals");
  }

  // -------------------------------------------------------------------------
  // JPA Annotation Verification Tests
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("All ManyToOne associations are properly persisted and loaded")
  void testAllAssociationsPersistedAndLoaded() {
    // given
    Date date = new Date();
    DataApproval approval =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    approval.setLastUpdatedBy(userB);

    // when
    dataApprovalStore.addDataApproval(approval);
    DataApproval loaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);

    // then
    assertNotNull(loaded, "DataApproval should be loaded from database");
    assertNotNull(loaded.getDataApprovalLevel(), "DataApprovalLevel association should be loaded");
    assertNotNull(loaded.getWorkflow(), "Workflow association should be loaded");
    assertNotNull(loaded.getPeriod(), "Period association should be loaded");
    assertNotNull(loaded.getOrganisationUnit(), "OrganisationUnit association should be loaded");
    assertNotNull(
        loaded.getAttributeOptionCombo(), "AttributeOptionCombo association should be loaded");
    assertNotNull(loaded.getCreator(), "Creator association should be loaded");
    assertNotNull(loaded.getLastUpdatedBy(), "LastUpdatedBy association should be loaded");

    assertEquals(
        level1.getId(), loaded.getDataApprovalLevel().getId(), "DataApprovalLevel should match");
    assertEquals(workflowA12.getId(), loaded.getWorkflow().getId(), "Workflow should match");
    assertEquals(periodA.getId(), loaded.getPeriod().getId(), "Period should match");
    assertEquals(
        sourceA.getId(), loaded.getOrganisationUnit().getId(), "OrganisationUnit should match");
    assertEquals(
        categoryOptionCombo.getId(),
        loaded.getAttributeOptionCombo().getId(),
        "AttributeOptionCombo should match");
    assertEquals(userA.getId(), loaded.getCreator().getId(), "Creator should match");
    assertEquals(userB.getId(), loaded.getLastUpdatedBy().getId(), "LastUpdatedBy should match");
  }

  @Test
  @DisplayName("Required associations enforce not-null constraint")
  void testRequiredAssociationsNotNull() {
    // given
    Date date = new Date();
    DataApproval approval =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);

    // when
    dataApprovalStore.addDataApproval(approval);
    DataApproval loaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);

    // then - verify required fields are not null
    assertNotNull(
        loaded.getDataApprovalLevel(), "Required field dataApprovalLevel should not be null");
    assertNotNull(loaded.getPeriod(), "Required field period should not be null");
    assertNotNull(
        loaded.getOrganisationUnit(), "Required field organisationUnit should not be null");
    assertNotNull(loaded.getCreated(), "Required field created should not be null");
    assertNotNull(loaded.getCreator(), "Required field creator should not be null");
  }

  @Test
  @DisplayName("Optional associations can be null")
  void testOptionalAssociationsCanBeNull() {
    // given
    Date date = new Date();
    DataApproval approval =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    // lastUpdatedBy is nullable - don't set it
    approval.setLastUpdatedBy(null);

    // when
    dataApprovalStore.addDataApproval(approval);
    DataApproval loaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);

    // then - verify entity can be loaded with null optional associations
    assertNotNull(loaded, "DataApproval should be loaded even with null optional associations");
    assertNull(loaded.getLastUpdatedBy(), "Optional lastUpdatedBy association can be null");
  }

  @Test
  @DisplayName("Update operation preserves all associations")
  void testUpdatePreservesAssociations() {
    // given
    Date date = new Date();
    DataApproval approval =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);
    dataApprovalStore.addDataApproval(approval);

    // when - update the accepted status and lastUpdatedBy
    DataApproval loaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    loaded.setAccepted(true, userB);
    dataApprovalStore.updateDataApproval(loaded);

    // then - reload and verify all associations are still intact
    DataApproval reloaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);
    assertNotNull(reloaded);
    assertTrue(reloaded.isAccepted(), "Accepted status should be updated");
    assertEquals(
        userB.getId(), reloaded.getLastUpdatedBy().getId(), "LastUpdatedBy should be updated");
    assertNotNull(reloaded.getLastUpdated(), "LastUpdated timestamp should be set");

    // Verify other associations are preserved
    assertEquals(level1.getId(), reloaded.getDataApprovalLevel().getId());
    assertEquals(workflowA12.getId(), reloaded.getWorkflow().getId());
    assertEquals(periodA.getId(), reloaded.getPeriod().getId());
    assertEquals(sourceA.getId(), reloaded.getOrganisationUnit().getId());
    assertEquals(categoryOptionCombo.getId(), reloaded.getAttributeOptionCombo().getId());
    assertEquals(userA.getId(), reloaded.getCreator().getId());
    assertEquals(date, reloaded.getCreated(), "Created date should be unchanged");
  }

  @Test
  @DisplayName("ID is properly generated and can be retrieved")
  void testIdGeneration() {
    // given
    Date date = new Date();
    DataApproval approval =
        new DataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo, false, date, userA);

    // when
    dataApprovalStore.addDataApproval(approval);
    DataApproval loaded =
        dataApprovalStore.getDataApproval(
            level1, workflowA12, periodA, sourceA, categoryOptionCombo);

    // then
    assertNotNull(loaded);
    assertTrue(loaded.getId() > 0, "ID should be auto-generated and greater than 0");
  }
}
