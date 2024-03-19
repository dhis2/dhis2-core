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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dataapproval.DataApprovalState.APPROVED_ABOVE;
import static org.hisp.dhis.dataapproval.DataApprovalState.APPROVED_HERE;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalPermissions;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalStatus;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class DataApprovalControllerTest {

  @Mock private DataApprovalService dataApprovalService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private DataSetService dataSetService;

  @Mock private DataApprovalWorkflow workflowA;

  private String workflowAUid = "daWorkflowA";

  @Mock private DataApprovalWorkflow workflowB;

  private String workflowBUid = "daWorkflowB";

  @Mock private DataApprovalLevel levelA;

  private String levelAUid = "datApLevelA";

  private int levelAlevel = 1;

  @Mock private DataApprovalLevel levelB;

  private String levelBUid = "datApLevelB";

  private int levelBlevel = 2;

  private String periodAIso = "202310";

  private Period periodA = PeriodType.getPeriodFromIsoString(periodAIso);

  @Mock private OrganisationUnit orgUnitA;

  private String orgUnitAUid = "orgUnitAUid";

  private String orgUnitAName = "OrgUnit A";

  @Mock private OrganisationUnit orgUnitB;

  private String orgUnitBUid = "orgUnitBUid";

  private String orgUnitBName = "OrgUnit B";

  @Mock private DataSet dataSetA;

  private String dataSetAUid = "dataSetAUid";

  @Mock private DataSet dataSetB;

  private String dataSetBUid = "dataSetBUid";

  @Mock private CategoryCombo catComboA;

  private String catComboAUid = "catComboAid";

  @Mock private CategoryCombo catComboB;

  private String catComboBUid = "catComboBid";

  @Mock private CategoryOptionCombo aocA;

  private String aocAUid = "attOptCombA";

  @Mock private CategoryOptionCombo aocB;

  private String aocBUid = "attOptCombB";

  @InjectMocks private DataApprovalController target;

  private DataApprovalPermissions permissionsA =
      DataApprovalPermissions.builder().mayApprove(false).mayAccept(true).build();

  private DataApprovalPermissions permissionsB =
      DataApprovalPermissions.builder().mayApprove(true).mayAccept(false).build();

  @Test
  void testGetApprovalByCategoryOptionCombos1() throws WebMessageException {
    when(organisationUnitService.getOrganisationUnit(null)).thenReturn(null);
    when(organisationUnitService.getOrganisationUnit(orgUnitAUid)).thenReturn(orgUnitA);
    when(dataSetService.getDataSet(dataSetAUid)).thenReturn(dataSetA);
    when(dataSetService.getDataSet(dataSetBUid)).thenReturn(dataSetB);
    when(categoryService.getCategoryOptionCombo(aocAUid)).thenReturn(aocA);
    when(categoryService.getCategoryOptionCombo(aocBUid)).thenReturn(aocB);
    when(levelA.getUid()).thenReturn(levelAUid);
    when(levelB.getUid()).thenReturn(levelBUid);
    when(levelA.getLevel()).thenReturn(levelAlevel);
    when(levelB.getLevel()).thenReturn(levelBlevel);
    when(workflowA.getDataSets()).thenReturn(Set.of(dataSetA));
    when(workflowB.getDataSets()).thenReturn(Set.of(dataSetB));
    when(dataSetA.getWorkflow()).thenReturn(workflowA);
    when(dataSetB.getWorkflow()).thenReturn(workflowB);
    when(dataSetA.getCategoryCombo()).thenReturn(catComboA);
    when(dataSetB.getCategoryCombo()).thenReturn(catComboB);
    Set<CategoryOptionCombo> aocs = Set.of(aocA, aocB);

    when(dataApprovalService.getUserDataApprovalsAndPermissions(
            workflowA, periodA, null, orgUnitA, catComboA, aocs))
        .thenReturn(
            List.of(
                DataApprovalStatus.builder()
                    .state(APPROVED_HERE)
                    .approvedLevel(levelA)
                    .actionLevel(levelA)
                    .organisationUnitUid(orgUnitAUid)
                    .organisationUnitName(orgUnitAName)
                    .attributeOptionComboUid(aocAUid)
                    .accepted(false)
                    .permissions(permissionsA)
                    .build()));

    when(dataApprovalService.getUserDataApprovalsAndPermissions(
            workflowB, periodA, null, orgUnitA, catComboB, aocs))
        .thenReturn(
            List.of(
                DataApprovalStatus.builder()
                    .state(APPROVED_ABOVE)
                    .approvedLevel(levelB)
                    .actionLevel(levelB)
                    .organisationUnitUid(orgUnitBUid)
                    .organisationUnitName(orgUnitBName)
                    .attributeOptionComboUid(aocBUid)
                    .accepted(true)
                    .permissions(permissionsB)
                    .build()));

    List<Map<String, Object>> result =
        target.getApprovalByCategoryOptionCombos(
            Set.of(dataSetAUid, dataSetBUid),
            null,
            periodAIso,
            null,
            orgUnitAUid,
            Set.of(aocAUid, aocBUid));

    assertContainsOnly(
        List.of(
            Map.of(
                "level", Map.of("id", levelAUid, "level", String.valueOf(levelAlevel)),
                "ou", orgUnitAUid,
                "ouName", orgUnitAName,
                "permissions", permissionsA,
                "accepted", false,
                "id", aocAUid),
            Map.of(
                "level", Map.of("id", levelBUid, "level", String.valueOf(levelBlevel)),
                "ou", orgUnitBUid,
                "ouName", orgUnitBName,
                "permissions", permissionsB,
                "accepted", true,
                "id", aocBUid)),
        result);
  }

  @Test
  void testGetApprovalByCategoryOptionCombos2() throws WebMessageException {
    when(organisationUnitService.getOrganisationUnit(null)).thenReturn(null);
    when(organisationUnitService.getOrganisationUnit(orgUnitAUid)).thenReturn(orgUnitA);
    when(dataApprovalService.getWorkflow(workflowAUid)).thenReturn(workflowA);
    when(dataApprovalService.getWorkflow(workflowBUid)).thenReturn(workflowB);
    when(levelA.getUid()).thenReturn(levelAUid);
    when(levelB.getUid()).thenReturn(levelBUid);
    when(levelA.getLevel()).thenReturn(levelAlevel);
    when(levelB.getLevel()).thenReturn(levelBlevel);
    when(workflowA.getDataSets()).thenReturn(Set.of(dataSetA));
    when(workflowB.getDataSets()).thenReturn(Set.of(dataSetB));
    when(dataSetA.getCategoryCombo()).thenReturn(catComboA);
    when(dataSetB.getCategoryCombo()).thenReturn(catComboB);

    when(dataApprovalService.getUserDataApprovalsAndPermissions(
            workflowA, periodA, orgUnitA, null, catComboA, null))
        .thenReturn(
            List.of(
                DataApprovalStatus.builder()
                    .state(APPROVED_HERE)
                    .approvedLevel(levelA)
                    .actionLevel(levelA)
                    .organisationUnitUid(orgUnitAUid)
                    .organisationUnitName(orgUnitAName)
                    .attributeOptionComboUid(aocAUid)
                    .accepted(false)
                    .permissions(permissionsA)
                    .build()));

    when(dataApprovalService.getUserDataApprovalsAndPermissions(
            workflowB, periodA, orgUnitA, null, catComboB, null))
        .thenReturn(
            List.of(
                DataApprovalStatus.builder()
                    .state(APPROVED_ABOVE)
                    .approvedLevel(levelB)
                    .actionLevel(levelB)
                    .organisationUnitUid(orgUnitBUid)
                    .organisationUnitName(orgUnitBName)
                    .attributeOptionComboUid(aocBUid)
                    .accepted(true)
                    .permissions(permissionsB)
                    .build()));

    List<Map<String, Object>> result =
        target.getApprovalByCategoryOptionCombos(
            null, Set.of(workflowAUid, workflowBUid), periodAIso, orgUnitAUid, null, null);

    assertContainsOnly(
        List.of(
            Map.of(
                "level", Map.of("id", levelAUid, "level", String.valueOf(levelAlevel)),
                "ou", orgUnitAUid,
                "ouName", orgUnitAName,
                "permissions", permissionsA,
                "accepted", false,
                "id", aocAUid),
            Map.of(
                "level", Map.of("id", levelBUid, "level", String.valueOf(levelBlevel)),
                "ou", orgUnitBUid,
                "ouName", orgUnitBName,
                "permissions", permissionsB,
                "accepted", true,
                "id", aocBUid)),
        result);
  }
}
