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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.util.DateUtils.getMediumDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataQueryRequest;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSetDimension;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriodEnum;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class DataQueryServiceTest extends DhisSpringTest {

  private Program prA;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private DataElement deD;

  private ProgramDataElementDimensionItem pdA;

  private ProgramDataElementDimensionItem pdB;

  private CategoryOptionCombo cocA;

  private ReportingRate rrA;

  private ReportingRate rrB;

  private ReportingRate rrC;

  private IndicatorType itA;

  private Indicator inA;

  private Indicator inB;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private ProgramTrackedEntityAttributeDimensionItem patA;

  private ProgramTrackedEntityAttributeDimensionItem patB;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnit ouD;

  private OrganisationUnit ouE;

  private OrganisationUnitGroup ouGroupA;

  private OrganisationUnitGroup ouGroupB;

  private OrganisationUnitGroup ouGroupC;

  private OrganisationUnitGroupSet ouGroupSetA;

  private IndicatorGroup inGroupA;

  private DataElementGroup deGroupA;

  private DataElementGroup deGroupB;

  private DataElementGroup deGroupC;

  private DataElementGroupSet deGroupSetA;

  private PeriodType monthly = PeriodType.getPeriodTypeByName(MonthlyPeriodType.NAME);

  @Autowired private DataQueryService dataQueryService;

  @Autowired private DataElementService dataElementService;

  @Autowired private CategoryService categoryService;

  @Autowired private DataSetService dataSetService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private ProgramService programService;

  @Autowired private UserService internalUserService;

  @Override
  public void setUpTest() {
    super.userService = internalUserService;
    prA = createProgram('A');
    programService.addProgram(prA);
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    deD = createDataElement('D');
    DataElement deE = createDataElement('E');
    DataElement deF = createDataElement('F');
    deE.setDomainType(DataElementDomain.TRACKER);
    deF.setDomainType(DataElementDomain.TRACKER);
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    dataElementService.addDataElement(deD);
    dataElementService.addDataElement(deE);
    dataElementService.addDataElement(deF);
    pdA = new ProgramDataElementDimensionItem(prA, deE);
    pdB = new ProgramDataElementDimensionItem(prA, deF);
    cocA = categoryService.getDefaultCategoryOptionCombo();
    DataSet dsA = createDataSet('A', monthly);
    DataSet dsB = createDataSet('B', monthly);
    dataSetService.addDataSet(dsA);
    dataSetService.addDataSet(dsB);
    rrA = new ReportingRate(dsA, ReportingRateMetric.REPORTING_RATE);
    rrB = new ReportingRate(dsB, ReportingRateMetric.REPORTING_RATE);
    rrC = new ReportingRate(dsB, ReportingRateMetric.ACTUAL_REPORTS);
    itA = createIndicatorType('A');
    idObjectManager.save(itA);
    inA = createIndicator('A', itA);
    inB = createIndicator('B', itA);
    idObjectManager.save(inA);
    idObjectManager.save(inB);
    inGroupA = createIndicatorGroup('A');
    inGroupA.getMembers().add(inA);
    inGroupA.getMembers().add(inB);
    idObjectManager.save(inGroupA);
    atA = createTrackedEntityAttribute('A');
    atB = createTrackedEntityAttribute('B');
    idObjectManager.save(atA);
    idObjectManager.save(atB);
    patA = new ProgramTrackedEntityAttributeDimensionItem(prA, atA);
    patB = new ProgramTrackedEntityAttributeDimensionItem(prA, atB);
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    ouC = createOrganisationUnit('C');
    ouD = createOrganisationUnit('D');
    ouE = createOrganisationUnit('E');
    ouB.updateParent(ouA);
    ouC.updateParent(ouA);
    ouD.updateParent(ouB);
    ouE.updateParent(ouB);
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    organisationUnitService.addOrganisationUnit(ouC);
    organisationUnitService.addOrganisationUnit(ouD);
    organisationUnitService.addOrganisationUnit(ouE);
    ouGroupSetA = createOrganisationUnitGroupSet('A');
    organisationUnitGroupService.addOrganisationUnitGroupSet(ouGroupSetA);
    ouGroupA = createOrganisationUnitGroup('A');
    ouGroupA.setPublicAccess(AccessStringHelper.FULL);
    ouGroupB = createOrganisationUnitGroup('B');
    ouGroupB.setPublicAccess(AccessStringHelper.FULL);
    ouGroupC = createOrganisationUnitGroup('C');
    ouGroupC.setPublicAccess(AccessStringHelper.FULL);
    ouGroupA.addOrganisationUnit(ouA);
    ouGroupA.addOrganisationUnit(ouB);
    ouGroupA.addOrganisationUnit(ouC);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupA);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupB);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupC);
    ouGroupSetA.addOrganisationUnitGroup(ouGroupA);
    ouGroupSetA.addOrganisationUnitGroup(ouGroupB);
    ouGroupSetA.addOrganisationUnitGroup(ouGroupC);
    organisationUnitGroupService.updateOrganisationUnitGroupSet(ouGroupSetA);
    deGroupSetA = createDataElementGroupSet('A');
    dataElementService.addDataElementGroupSet(deGroupSetA);
    deGroupA = createDataElementGroup('A');
    deGroupB = createDataElementGroup('B');
    deGroupC = createDataElementGroup('C');
    deGroupA.getGroupSets().add(deGroupSetA);
    deGroupB.getGroupSets().add(deGroupSetA);
    deGroupC.getGroupSets().add(deGroupSetA);
    deGroupA.getGroupSets().add(deGroupSetA);
    deGroupA.addDataElement(deA);
    deGroupA.addDataElement(deB);
    deGroupA.addDataElement(deC);
    dataElementService.addDataElementGroup(deGroupA);
    dataElementService.addDataElementGroup(deGroupB);
    dataElementService.addDataElementGroup(deGroupC);
    deGroupSetA.addDataElementGroup(deGroupA);
    deGroupSetA.addDataElementGroup(deGroupB);
    deGroupSetA.addDataElementGroup(deGroupC);
    dataElementService.updateDataElementGroupSet(deGroupSetA);
    // ---------------------------------------------------------------------
    // Inject user
    // ---------------------------------------------------------------------
    UserRole role = createUserRole('A', "ALL");
    userService.addUserRole(role);
    User user = createUser('A');
    user.addOrganisationUnit(ouA);
    user.getUserRoles().add(role);
    saveAndInjectUserSecurityContext(user);
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------
  @Test
  void testGetDimensionalObjects() {
    Set<String> dimensionParams = new LinkedHashSet<>();
    dimensionParams.add(
        DimensionalObject.DATA_X_DIM_ID
            + DIMENSION_NAME_SEP
            + deA.getDimensionItem()
            + OPTION_SEP
            + deB.getDimensionItem()
            + OPTION_SEP
            + rrA.getDimensionItem());
    dimensionParams.add(
        DimensionalObject.ORGUNIT_DIM_ID
            + DIMENSION_NAME_SEP
            + ouA.getDimensionItem()
            + OPTION_SEP
            + ouB.getDimensionItem());
    List<DimensionalObject> dimensionalObject =
        dataQueryService.getDimensionalObjects(
            dimensionParams, null, null, null, false, IdScheme.UID);
    DimensionalObject dxObject = dimensionalObject.get(0);
    DimensionalObject ouObject = dimensionalObject.get(1);
    List<DimensionalItemObject> dxItems = Lists.newArrayList(deA, deB, rrA);
    List<DimensionalItemObject> ouItems = Lists.newArrayList(ouA, ouB);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, dxObject.getDimension());
    assertEquals(DimensionType.DATA_X, dxObject.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, dxObject.getDimensionDisplayName());
    assertEquals(dxItems, dxObject.getItems());
    assertEquals(DimensionalObject.ORGUNIT_DIM_ID, ouObject.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT, ouObject.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_ORGUNIT, ouObject.getDimensionDisplayName());
    assertEquals(ouItems, ouObject.getItems());
  }

  @Test
  void testGetDimensionalObjectsReportingRates() {
    Set<String> dimensionParams = new LinkedHashSet<>();
    dimensionParams.add(
        DimensionalObject.DATA_X_DIM_ID
            + DIMENSION_NAME_SEP
            + deA.getDimensionItem()
            + OPTION_SEP
            + rrA.getDimensionItem()
            + OPTION_SEP
            + rrB.getDimensionItem()
            + OPTION_SEP
            + rrC.getDimensionItem());
    dimensionParams.add(
        DimensionalObject.ORGUNIT_DIM_ID + DIMENSION_NAME_SEP + ouA.getDimensionItem());
    List<DimensionalObject> dimensionalObject =
        dataQueryService.getDimensionalObjects(
            dimensionParams, null, null, null, false, IdScheme.UID);
    DimensionalObject dxObject = dimensionalObject.get(0);
    DimensionalObject ouObject = dimensionalObject.get(1);
    List<DimensionalItemObject> dxItems = Lists.newArrayList(deA, rrA, rrB, rrC);
    List<DimensionalItemObject> ouItems = Lists.newArrayList(ouA);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, dxObject.getDimension());
    assertEquals(DimensionType.DATA_X, dxObject.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, dxObject.getDimensionDisplayName());
    assertEquals(dxItems, dxObject.getItems());
    assertEquals(DimensionalObject.ORGUNIT_DIM_ID, ouObject.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT, ouObject.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_ORGUNIT, ouObject.getDimensionDisplayName());
    assertEquals(ouItems, ouObject.getItems());
  }

  @Test
  void testGetDimensionData() {
    List<DimensionalItemObject> items = Lists.newArrayList(deA, deB, deC, rrA, rrB);
    List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds(items);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.DATA_X_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.DATA_X, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetDimensionDataByCode() {
    List<DimensionalItemObject> items = Lists.newArrayList(deA, deB, deC);
    List<String> itemCodes = Lists.newArrayList(deA.getCode(), deB.getCode(), deC.getCode());
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.DATA_X_DIM_ID,
            itemCodes,
            null,
            null,
            null,
            false,
            false,
            IdScheme.CODE);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.DATA_X, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetOrgUnitGroupSetDimensionByCode() {
    List<DimensionalItemObject> items = Lists.newArrayList(ouGroupA, ouGroupB, ouGroupC);
    List<String> itemCodes =
        Lists.newArrayList(ouGroupA.getCode(), ouGroupB.getCode(), ouGroupC.getCode());
    DimensionalObject actual =
        dataQueryService.getDimension(
            ouGroupSetA.getCode(), itemCodes, null, null, null, false, false, IdScheme.CODE);
    assertEquals(ouGroupSetA.getDimension(), actual.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT_GROUP_SET, actual.getDimensionType());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetDimensionOperand() {
    DataElementOperand opA = new DataElementOperand(deA, cocA);
    DataElementOperand opB = new DataElementOperand(deB, cocA);
    DataElementOperand opC = new DataElementOperand(deC, cocA);
    List<DimensionalItemObject> items = Lists.newArrayList(opA, opB, opC);
    List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds(items);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.DATA_X_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.DATA_X, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetDimensionOrgUnit() {
    List<DimensionalItemObject> items = Lists.newArrayList(ouA, ouB, ouC);
    List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds(items);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.ORGUNIT_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.ORGUNIT_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_ORGUNIT, actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetDimensionOrgUnitGroup() {
    String ouGroupAUid = OrganisationUnit.KEY_ORGUNIT_GROUP + ouGroupA.getUid();
    List<String> itemUids = Lists.newArrayList(ouGroupAUid);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.ORGUNIT_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.ORGUNIT_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_ORGUNIT, actual.getDimensionDisplayName());
    assertEquals(ouGroupA.getMembers(), Sets.newHashSet(actual.getItems()));
  }

  @Test
  void testGetDimensionDataElementGroup() {
    String deGroupAId = DataQueryParams.KEY_DE_GROUP + deGroupA.getUid();
    List<String> itemUids = Lists.newArrayList(deGroupAId);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.DATA_X_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.DATA_X, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDimensionDisplayName());
    assertEquals(deGroupA.getMembers(), Sets.newHashSet(actual.getItems()));
  }

  @Test
  void testGetDimensionIndicatorGroup() {
    String inGroupAId = DataQueryParams.KEY_IN_GROUP + inGroupA.getUid();
    List<String> itemUids = Lists.newArrayList(inGroupAId);
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.DATA_X_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.DATA_X_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.DATA_X, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_DATA_X, actual.getDimensionDisplayName());
    assertEquals(inGroupA.getMembers(), Sets.newHashSet(actual.getItems()));
  }

  @Test
  void testGetDimensionPeriod() {
    List<String> itemUids =
        Lists.newArrayList(
            "199501",
            "1999",
            RelativePeriodEnum.LAST_4_QUARTERS.toString(),
            RelativePeriodEnum.THIS_YEAR.toString());
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.PERIOD_DIM_ID,
            itemUids,
            null,
            null,
            null,
            false,
            false,
            IdScheme.UID);
    assertEquals(DimensionalObject.PERIOD_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.PERIOD, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_PERIOD, actual.getDimensionDisplayName());
    assertEquals(7, actual.getItems().size());
  }

  @Test
  void testGetDimensionPeriodAndStartEndDates() {
    DimensionalObject actual =
        dataQueryService.getDimension(
            DimensionalObject.PERIOD_DIM_ID,
            Lists.newArrayList(),
            null,
            null,
            null,
            false,
            true,
            IdScheme.UID);
    assertEquals(DimensionalObject.PERIOD_DIM_ID, actual.getDimension());
    assertEquals(DimensionType.PERIOD, actual.getDimensionType());
    assertEquals(DataQueryParams.DISPLAY_NAME_PERIOD, actual.getDimensionDisplayName());
    assertEquals(0, actual.getItems().size());
  }

  @Test
  void testGetDimensionOrgUnitGroupSet() {
    List<DimensionalItemObject> items = Lists.newArrayList(ouGroupA, ouGroupB);
    List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds(items);
    DimensionalObject actual =
        dataQueryService.getDimension(
            ouGroupSetA.getUid(), itemUids, null, null, null, false, false, IdScheme.UID);
    assertEquals(ouGroupSetA.getUid(), actual.getDimension());
    assertEquals(DimensionType.ORGANISATION_UNIT_GROUP_SET, actual.getDimensionType());
    assertEquals(ouGroupSetA.getName(), actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetDimensionDataElementGroupSet() {
    List<DimensionalItemObject> items = Lists.newArrayList(deGroupA, deGroupB);
    List<String> itemUids = DimensionalObjectUtils.getDimensionalItemIds(items);
    DimensionalObject actual =
        dataQueryService.getDimension(
            deGroupSetA.getUid(), itemUids, null, null, null, false, false, IdScheme.UID);
    assertEquals(deGroupSetA.getUid(), actual.getDimension());
    assertEquals(DimensionType.DATA_ELEMENT_GROUP_SET, actual.getDimensionType());
    assertEquals(deGroupSetA.getName(), actual.getDimensionDisplayName());
    assertEquals(items, actual.getItems());
  }

  @Test
  void testGetFromUrlA() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid());
    dimensionParams.add("pe:2012;2012S1;2012S2");
    dimensionParams.add(
        ouGroupSetA.getUid()
            + ":"
            + ouGroupA.getUid()
            + ";"
            + ouGroupB.getUid()
            + ";"
            + ouGroupC.getUid());
    Set<String> filterParams = new HashSet<>();
    filterParams.add(
        "ou:"
            + ouA.getUid()
            + ";"
            + ouB.getUid()
            + ";"
            + ouC.getUid()
            + ";"
            + ouD.getUid()
            + ";"
            + ouE.getUid());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(4, params.getDataElements().size());
    assertEquals(3, params.getPeriods().size());
    assertEquals(5, params.getFilterOrganisationUnits().size());
    assertEquals(3, params.getDimensionOptions(ouGroupSetA.getUid()).size());
  }

  @Test
  void testGetFromUrlB() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getDimensionItem()
            + ";"
            + deB.getDimensionItem()
            + ";"
            + deC.getDimensionItem()
            + ";"
            + deD.getUid());
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getUid());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(4, params.getDataElements().size());
    assertEquals(1, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromUrlC() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getDimensionItem()
            + ";"
            + deB.getDimensionItem()
            + ";"
            + pdA.getDimensionItem()
            + ";"
            + pdB.getDimensionItem());
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getDimensionItem());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getProgramDataElements().size());
    assertEquals(1, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromUrlD() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getDimensionItem()
            + ";"
            + deB.getDimensionItem()
            + ";"
            + patA.getDimensionItem()
            + ";"
            + patB.getDimensionItem());
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getDimensionItem());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getProgramAttributes().size());
    assertEquals(1, params.getFilterOrganisationUnits().size());
  }

  @Test
  @Disabled("Not working for composite identifiers with non-UID identifier schemes")
  void testGetFromUrlWithCodeA() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getCode()
            + ";"
            + deB.getCode()
            + ";"
            + patA.getDimensionItem(IdScheme.CODE)
            + ";"
            + patB.getDimensionItem(IdScheme.CODE));
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getCode());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder()
            .dimension(dimensionParams)
            .filter(filterParams)
            .inputIdScheme(IdScheme.CODE)
            .build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getProgramAttributes().size());
    assertEquals(1, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromUrlWithCodeB() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("dx:" + deA.getCode() + ";" + deB.getCode() + ";" + inA.getCode());
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getCode());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder()
            .dimension(dimensionParams)
            .filter(filterParams)
            .inputIdScheme(IdScheme.CODE)
            .build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(2, params.getDataElements().size());
    assertEquals(1, params.getIndicators().size());
    assertEquals(1, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromUrlOrgUnitGroupSetAllItems() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getDimensionItem()
            + ";"
            + deB.getDimensionItem()
            + ";"
            + deC.getDimensionItem());
    dimensionParams.add("pe:2012;2012S1");
    dimensionParams.add(ouGroupSetA.getUid());
    Set<String> filterParams = new HashSet<>();
    filterParams.add(
        "ou:"
            + ouA.getDimensionItem()
            + ";"
            + ouB.getDimensionItem()
            + ";"
            + ouC.getDimensionItem());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(3, params.getDataElements().size());
    assertEquals(2, params.getPeriods().size());
    assertEquals(3, params.getFilterOrganisationUnits().size());
    assertEquals(3, params.getDimensionOptions(ouGroupSetA.getUid()).size());
  }

  @Test
  void testGetFromUrlRelativePeriods() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:"
            + deA.getDimensionItem()
            + ";"
            + deB.getDimensionItem()
            + ";"
            + deC.getDimensionItem()
            + ";"
            + deD.getDimensionItem());
    dimensionParams.add("pe:LAST_12_MONTHS");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getDimensionItem() + ";" + ouB.getDimensionItem());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(4, params.getDataElements().size());
    assertEquals(12, params.getPeriods().size());
    assertEquals(2, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromUrlUserOrgUnit() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + OrganisationUnit.KEY_USER_ORGUNIT);
    dimensionParams.add("dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem());
    dimensionParams.add("pe:2011;2012");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getPeriods().size());
  }

  @Test
  void testGetFromUrlOrgUnitGroup() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:OU_GROUP-" + ouGroupA.getUid());
    dimensionParams.add("dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem());
    dimensionParams.add("pe:2011;2012");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(3, params.getOrganisationUnits().size());
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getPeriods().size());
  }

  @Test
  void testGetFromUrlOrgUnitLevel() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:LEVEL-2");
    dimensionParams.add("dx:" + deA.getDimensionItem() + ";" + deB.getDimensionItem());
    dimensionParams.add("pe:2011;2012");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(2, params.getOrganisationUnits().size());
    assertEquals(2, params.getDataElements().size());
    assertEquals(2, params.getPeriods().size());
  }

  @Test
  void testGetFromUrlNoDx() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("dx");
    dimensionParams.add("pe:2012,2012S1,2012S2");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getFromRequest(dataQueryRequest));
  }

  @Test
  void testGetFromUrlNoPeriods() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D");
    dimensionParams.add("pe");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getFromRequest(dataQueryRequest));
  }

  @Test
  void testGetFromUrlNoOrganisationUnits() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D");
    dimensionParams.add("ou");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getFromRequest(dataQueryRequest));
  }

  @Test
  void testGetFromUrlInvalidDimension() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D");
    dimensionParams.add("yebo:2012,2012S1,2012S2");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getFromRequest(dataQueryRequest));
  }

  @Test
  void testGetFromUrlInvalidOrganisationUnits() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + BASE_UID + "A;" + BASE_UID + "B;" + BASE_UID + "C;" + BASE_UID + "D");
    dimensionParams.add("ou:aTr6yTgX7t5;gBgf2G2j4GR");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).build();
    assertIllegalQueryEx(
        assertThrows(
            IllegalQueryException.class, () -> dataQueryService.getFromRequest(dataQueryRequest)),
        ErrorCode.E7124);
  }

  @Test
  void testGetFromUrlPeriodOrder() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid());
    dimensionParams.add("pe:2013;2012Q4;2012S2");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("ou:" + ouA.getUid());
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).filter(filterParams).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    List<DimensionalItemObject> periods = params.getPeriods();
    assertEquals(3, periods.size());
    assertEquals("2013", periods.get(0).getUid());
    assertEquals("2012Q4", periods.get(1).getUid());
    assertEquals("2012S2", periods.get(2).getUid());
  }

  @Test
  void testGetFromUrlNoPeriodsAllowAllPeriods() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        "dx:" + deA.getUid() + ";" + deB.getUid() + ";" + deC.getUid() + ";" + deD.getUid());
    dimensionParams.add("pe");
    DataQueryRequest dataQueryRequest =
        DataQueryRequest.newBuilder().dimension(dimensionParams).allowAllPeriods(true).build();
    DataQueryParams params = dataQueryService.getFromRequest(dataQueryRequest);
    assertEquals(0, params.getPeriods().size());
  }

  @Test
  void testGetFromAnalyticalObjectA() {
    final Visualization visualization = new Visualization();
    visualization.setRowDimensions(Arrays.asList(DimensionalObject.ORGUNIT_DIM_ID));
    visualization.setColumnDimensions(Arrays.asList(DimensionalObject.DATA_X_DIM_ID));
    visualization.getFilterDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    visualization.addDataDimensionItem(deA);
    visualization.addDataDimensionItem(deB);
    visualization.addDataDimensionItem(deC);
    visualization.getOrganisationUnits().add(ouA);
    visualization.getOrganisationUnits().add(ouB);
    visualization.getPeriods().add(PeriodType.getPeriodFromIsoString("2012"));
    DataQueryParams params = dataQueryService.getFromAnalyticalObject(visualization);
    assertNotNull(params);
    assertEquals(3, params.getDataElements().size());
    assertEquals(2, params.getOrganisationUnits().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(2, params.getDimensions().size());
    assertEquals(1, params.getFilters().size());
  }

  @Test
  void testGetFromAnalyticalObjectB() {
    final Visualization visualization = new Visualization();
    visualization.setColumnDimensions(Arrays.asList(DimensionalObject.DATA_X_DIM_ID));
    visualization.setRowDimensions(Arrays.asList(ouGroupSetA.getUid()));
    visualization.getFilterDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    visualization.addDataDimensionItem(deA);
    visualization.addDataDimensionItem(deB);
    visualization.addDataDimensionItem(deC);
    OrganisationUnitGroupSetDimension ouGroupSetDim = new OrganisationUnitGroupSetDimension();
    ouGroupSetDim.setDimension(ouGroupSetA);
    ouGroupSetDim.setItems(Lists.newArrayList(ouGroupA, ouGroupB, ouGroupC));
    visualization.getOrganisationUnitGroupSetDimensions().add(ouGroupSetDim);
    visualization.getPeriods().add(PeriodType.getPeriodFromIsoString("2012"));
    DataQueryParams params = dataQueryService.getFromAnalyticalObject(visualization);
    assertNotNull(params);
    assertEquals(3, params.getDataElements().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(2, params.getDimensions().size());
    assertEquals(1, params.getFilters().size());
    assertEquals(3, params.getDimensionOptions(ouGroupSetA.getUid()).size());
  }

  @Test
  void testGetFromAnalyticalObjectC() {
    final Visualization visualization = new Visualization();
    visualization.setColumnDimensions(Arrays.asList(DimensionalObject.DATA_X_DIM_ID));
    visualization.setRowDimensions(Arrays.asList(ouGroupSetA.getUid()));
    visualization.getFilterDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    visualization.addDataDimensionItem(deA);
    visualization.addDataDimensionItem(pdA);
    visualization.addDataDimensionItem(pdB);
    OrganisationUnitGroupSetDimension ouGroupSetDim = new OrganisationUnitGroupSetDimension();
    ouGroupSetDim.setDimension(ouGroupSetA);
    ouGroupSetDim.setItems(Lists.newArrayList(ouGroupA, ouGroupB, ouGroupC));
    visualization.getOrganisationUnitGroupSetDimensions().add(ouGroupSetDim);
    visualization.getPeriods().add(PeriodType.getPeriodFromIsoString("2012"));
    DataQueryParams params = dataQueryService.getFromAnalyticalObject(visualization);
    assertNotNull(params);
    assertEquals(1, params.getDataElements().size());
    assertEquals(2, params.getProgramDataElements().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(2, params.getDimensions().size());
    assertEquals(1, params.getFilters().size());
    assertEquals(3, params.getDimensionOptions(ouGroupSetA.getUid()).size());
  }

  @Test
  void testGetUserOrgUnits() {
    String ouParam = ouA.getUid() + ";" + ouB.getUid();
    List<OrganisationUnit> expected = Lists.newArrayList(ouA, ouB);
    assertEquals(expected, dataQueryService.getUserOrgUnits(null, ouParam));
  }

  @Test
  void testPeriodGetDimension() {
    DimensionalObject dimension =
        dataQueryService.getDimension(
            PERIOD_DIM_ID,
            List.of(
                "TODAY:EVENT_DATE",
                "YESTERDAY:ENROLLMENT_DATE",
                "20210101:INCIDENT_DATE",
                "20210101_20210201:LAST_UPDATED",
                "2021-02-01_2021-03-01:SCHEDULED_DATE"),
            null,
            null,
            null,
            true,
            true,
            null);

    assertThat(dimension.getItems(), hasSize(5));
    assertThat(
        dimension.getItems().stream()
            .map(dimensionalItemObject -> (Period) dimensionalItemObject)
            .map(Period::getDateField)
            .collect(Collectors.toList()),
        containsInAnyOrder(
            "EVENT_DATE", "ENROLLMENT_DATE", "INCIDENT_DATE", "LAST_UPDATED", "SCHEDULED_DATE"));

    assertEquals(getPeriod(dimension, "INCIDENT_DATE").getStartDate(), getMediumDate("2021-01-01"));
    assertEquals(getPeriod(dimension, "INCIDENT_DATE").getEndDate(), getMediumDate("2021-01-01"));

    assertEquals(getPeriod(dimension, "LAST_UPDATED").getStartDate(), getMediumDate("2021-01-01"));
    assertEquals(getPeriod(dimension, "LAST_UPDATED").getEndDate(), getMediumDate("2021-02-01"));

    assertEquals(
        getPeriod(dimension, "SCHEDULED_DATE").getStartDate(), getMediumDate("2021-02-01"));
    assertEquals(getPeriod(dimension, "SCHEDULED_DATE").getEndDate(), getMediumDate("2021-03-01"));
  }

  private Period getPeriod(DimensionalObject dimension, String dateField) {
    return dimension.getItems().stream()
        .map(dimensionalItemObject -> (Period) dimensionalItemObject)
        .filter(period -> period.getDateField().equals(dateField))
        .findFirst()
        .orElse(null);
  }
}
