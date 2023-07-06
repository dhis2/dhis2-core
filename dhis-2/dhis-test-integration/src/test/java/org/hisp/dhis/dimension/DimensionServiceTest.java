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
package org.hisp.dhis.dimension;

import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DimensionItemType.REPORTING_RATE;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_LEVEL;
import static org.hisp.dhis.organisationunit.OrganisationUnit.KEY_USER_ORGUNIT;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_12_MONTHS;
import static org.hisp.dhis.utils.Assertions.assertMapEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryModifiers;
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
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class DimensionServiceTest extends TransactionalIntegrationTest {
  private Attribute atA;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private CategoryOptionCombo cocA;

  private DataSet dsA;

  private Program prA;

  private ProgramStage psA;

  private TrackedEntityAttribute teaA;

  private ProgramIndicator piA;

  private Period peA;

  private Period peB;

  private DimensionalItemObject peLast12Months;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private OrganisationUnit ouC;

  private OrganisationUnit ouD;

  private OrganisationUnit ouE;

  private DimensionalItemObject ouUser;

  private DimensionalItemObject ouLevel2;

  private DataElementGroupSet deGroupSetA;

  private DataElementGroup deGroupA;

  private DataElementGroup deGroupB;

  private DataElementGroup deGroupC;

  private OrganisationUnitGroupSet ouGroupSetA;

  private OrganisationUnitGroup ouGroupA;

  private OrganisationUnitGroup ouGroupB;

  private OrganisationUnitGroup ouGroupC;

  private QueryModifiers queryModsA;

  private QueryModifiers queryModsB;

  private QueryModifiers queryModsC;

  private DimensionalItemObject itemObjectA;

  private DimensionalItemObject itemObjectB;

  private DimensionalItemObject itemObjectC;

  private DimensionalItemObject itemObjectD;

  private DimensionalItemObject itemObjectE;

  private DimensionalItemObject itemObjectF;

  private DimensionalItemObject itemObjectG;

  private DimensionalItemObject itemObjectH;

  private DimensionalItemId itemIdA;

  private DimensionalItemId itemIdB;

  private DimensionalItemId itemIdC;

  private DimensionalItemId itemIdD;

  private DimensionalItemId itemIdE;

  private DimensionalItemId itemIdF;

  private DimensionalItemId itemIdG;

  private DimensionalItemId itemIdH;

  private Set<DimensionalItemId> itemIds;

  private Map<DimensionalItemId, DimensionalItemObject> itemMap;

  @Autowired private AttributeService attributeService;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private OrganisationUnitGroupService organisationUnitGroupService;

  @Autowired private DataSetService dataSetService;

  @Autowired private CategoryService categoryService;

  @Autowired private IdentifiableObjectManager idObjectManager;

  @Autowired private DimensionService dimensionService;

  @Override
  public void setUpTest() {
    atA = createAttribute('A');
    atA.setUnique(true);
    atA.setDataElementAttribute(true);
    atA.setDataSetAttribute(true);
    attributeService.addAttribute(atA);
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    deC.setDomainType(DataElementDomain.TRACKER);
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    cocA = categoryService.getDefaultCategoryOptionCombo();
    dsA = createDataSet('A');
    dataSetService.addDataSet(dsA);
    prA = createProgram('A');
    idObjectManager.save(prA);
    psA = createProgramStage('A', 1);
    idObjectManager.save(psA);
    teaA = createTrackedEntityAttribute('A');
    idObjectManager.save(teaA);
    piA = createProgramIndicator('A', prA, null, null);
    idObjectManager.save(piA);
    peA = createPeriod("201201");
    peB = createPeriod("201202");
    peLast12Months = new BaseDimensionalItemObject(LAST_12_MONTHS.toString());
    peA.setUid("201201");
    peB.setUid("201202");
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
    String level2 = KEY_LEVEL + 2;
    ouUser = new BaseDimensionalItemObject(KEY_USER_ORGUNIT);
    ouLevel2 = new BaseDimensionalItemObject(level2);
    deGroupSetA = createDataElementGroupSet('A');
    dataElementService.addDataElementGroupSet(deGroupSetA);
    deGroupA = createDataElementGroup('A');
    deGroupB = createDataElementGroup('B');
    deGroupC = createDataElementGroup('C');
    deGroupA.getGroupSets().add(deGroupSetA);
    deGroupB.getGroupSets().add(deGroupSetA);
    deGroupC.getGroupSets().add(deGroupSetA);
    dataElementService.addDataElementGroup(deGroupA);
    dataElementService.addDataElementGroup(deGroupB);
    dataElementService.addDataElementGroup(deGroupC);
    deGroupSetA.getMembers().add(deGroupA);
    deGroupSetA.getMembers().add(deGroupB);
    deGroupSetA.getMembers().add(deGroupC);
    dataElementService.updateDataElementGroupSet(deGroupSetA);
    ouGroupSetA = createOrganisationUnitGroupSet('A');
    organisationUnitGroupService.addOrganisationUnitGroupSet(ouGroupSetA);
    ouGroupA = createOrganisationUnitGroup('A');
    ouGroupB = createOrganisationUnitGroup('B');
    ouGroupC = createOrganisationUnitGroup('C');
    ouGroupA.getGroupSets().add(ouGroupSetA);
    ouGroupB.getGroupSets().add(ouGroupSetA);
    ouGroupC.getGroupSets().add(ouGroupSetA);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupA);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupB);
    organisationUnitGroupService.addOrganisationUnitGroup(ouGroupC);
    ouGroupSetA.getOrganisationUnitGroups().add(ouGroupA);
    ouGroupSetA.getOrganisationUnitGroups().add(ouGroupB);
    ouGroupSetA.getOrganisationUnitGroups().add(ouGroupC);
    organisationUnitGroupService.updateOrganisationUnitGroupSet(ouGroupSetA);
    attributeService.addAttributeValue(deA, new AttributeValue(atA, "DEA"));
    attributeService.addAttributeValue(deB, new AttributeValue(atA, "DEB"));
    attributeService.addAttributeValue(deC, new AttributeValue(atA, "DEC"));
    attributeService.addAttributeValue(dsA, new AttributeValue(atA, "DSA"));
    queryModsA = QueryModifiers.builder().periodOffset(10).build();
    queryModsB = QueryModifiers.builder().minDate(new Date()).build();
    queryModsC = QueryModifiers.builder().maxDate(new Date()).build();
    itemObjectA = deA;
    itemObjectB = new DataElementOperand(deA, cocA);
    itemObjectC = new DataElementOperand(deA, null, cocA);
    itemObjectD = new DataElementOperand(deA, cocA, cocA);
    itemObjectE = new ReportingRate(dsA);
    itemObjectF = new ProgramDataElementDimensionItem(prA, deA);
    itemObjectG = new ProgramTrackedEntityAttributeDimensionItem(prA, teaA);
    itemObjectH = piA;
    itemIdA = new DimensionalItemId(DATA_ELEMENT, deA.getUid());
    itemIdB = new DimensionalItemId(DATA_ELEMENT_OPERAND, deA.getUid(), cocA.getUid());
    itemIdC = new DimensionalItemId(DATA_ELEMENT_OPERAND, deA.getUid(), null, cocA.getUid());
    itemIdD =
        new DimensionalItemId(DATA_ELEMENT_OPERAND, deA.getUid(), cocA.getUid(), cocA.getUid());
    itemIdE =
        new DimensionalItemId(
            REPORTING_RATE, dsA.getUid(), ReportingRateMetric.REPORTING_RATE.name());
    itemIdF = new DimensionalItemId(PROGRAM_DATA_ELEMENT, prA.getUid(), deA.getUid());
    itemIdG = new DimensionalItemId(PROGRAM_ATTRIBUTE, prA.getUid(), teaA.getUid());
    itemIdH = new DimensionalItemId(PROGRAM_INDICATOR, piA.getUid());
    itemIds = new HashSet<>();
    itemIds.add(itemIdA);
    itemIds.add(itemIdB);
    itemIds.add(itemIdC);
    itemIds.add(itemIdD);
    itemIds.add(itemIdE);
    itemIds.add(itemIdF);
    itemIds.add(itemIdG);
    itemIds.add(itemIdH);
    itemMap =
        ImmutableMap.<DimensionalItemId, DimensionalItemObject>builder()
            .put(itemIdA, itemObjectA)
            .put(itemIdB, itemObjectB)
            .put(itemIdC, itemObjectC)
            .put(itemIdD, itemObjectD)
            .put(itemIdE, itemObjectE)
            .put(itemIdF, itemObjectF)
            .put(itemIdG, itemObjectG)
            .put(itemIdH, itemObjectH)
            .build();
  }

  @Test
  void testMergeEventAnalyticalObject() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization.setValue(deC);
    // When
    dimensionService.mergeEventAnalyticalObject(eventVisualization);
    // Then
    assertNotNull(eventVisualization.getDataElementValueDimension());
    assertNull(eventVisualization.getAttributeValueDimension());
  }

  @Test
  void testMergeAnalyticalObjectA() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC, ouD, ouE)));
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(2, visualization.getPeriods().size());
    assertEquals(5, visualization.getOrganisationUnits().size());
  }

  @Test
  void testMergeAnalyticalEventObjectA() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC, ouD, ouE)));
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(2, eventVisualization.getPeriods().size());
    assertEquals(5, eventVisualization.getOrganisationUnits().size());
  }

  @Test
  void testMergeAnalyticalObjectB() {
    Visualization visualization = new Visualization();
    BaseDimensionalObject deCDim =
        new BaseDimensionalObject(
            deC.getUid(), DimensionType.PROGRAM_DATA_ELEMENT, null, null, null, psA, "EQ:uidA");
    visualization.getColumns().add(deCDim);
    visualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC)));
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(1, visualization.getDataElementDimensions().size());
    assertEquals(2, visualization.getPeriods().size());
    assertEquals(3, visualization.getOrganisationUnits().size());
    TrackedEntityDataElementDimension teDeDim = visualization.getDataElementDimensions().get(0);
    assertEquals(deC, teDeDim.getDataElement());
    assertEquals(psA, teDeDim.getProgramStage());
  }

  @Test
  void testMergeAnalyticalEventObjectB() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    BaseDimensionalObject deCDim =
        new BaseDimensionalObject(
            deC.getUid(), DimensionType.PROGRAM_DATA_ELEMENT, null, null, null, psA, "EQ:uidA");
    eventVisualization.getColumns().add(deCDim);
    eventVisualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC)));
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(1, eventVisualization.getDataElementDimensions().size());
    assertEquals(2, eventVisualization.getPeriods().size());
    assertEquals(3, eventVisualization.getOrganisationUnits().size());
    TrackedEntityDataElementDimension teDeDim =
        eventVisualization.getDataElementDimensions().get(0);
    assertEquals(deC, teDeDim.getDataElement());
    assertEquals(psA, teDeDim.getProgramStage());
  }

  @Test
  void testMergeAnalyticalObjectUserOrgUnit() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouUser)));
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList(peA)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(1, visualization.getPeriods().size());
    assertEquals(0, visualization.getOrganisationUnits().size());
    assertTrue(visualization.isUserOrganisationUnit());
  }

  @Test
  void testMergeAnalyticalEventObjectUserOrgUnit() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouUser)));
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList(peA)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(1, eventVisualization.getPeriods().size());
    assertEquals(0, eventVisualization.getOrganisationUnits().size());
    assertTrue(eventVisualization.isUserOrganisationUnit());
  }

  @Test
  void testMergeAnalyticalObjectOrgUnitLevel() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouLevel2, ouA)));
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList(peA)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(1, visualization.getPeriods().size());
    assertEquals(1, visualization.getOrganisationUnits().size());
    assertEquals(Integer.valueOf(2), visualization.getOrganisationUnitLevels().get(0));
  }

  @Test
  void testMergeAnalyticalEventObjectOrgUnitLevel() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouLevel2, ouA)));
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList(peA)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(1, eventVisualization.getPeriods().size());
    assertEquals(1, eventVisualization.getOrganisationUnits().size());
    assertEquals(Integer.valueOf(2), eventVisualization.getOrganisationUnitLevels().get(0));
  }

  @Test
  void testMergeAnalyticalObjectRelativePeriods() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC, ouD, ouE)));
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peLast12Months)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(0, visualization.getPeriods().size());
    assertTrue(visualization.getRelatives().isLast12Months());
    assertEquals(5, visualization.getOrganisationUnits().size());
  }

  @Test
  void testMergeAnalyticalEventObjectRelativePeriods() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization
        .getRows()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.ORGUNIT_DIM_ID,
                DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList(ouA, ouB, ouC, ouD, ouE)));
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peLast12Months)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(0, eventVisualization.getPeriods().size());
    assertTrue(eventVisualization.getRelatives().isLast12Months());
    assertEquals(5, eventVisualization.getOrganisationUnits().size());
  }

  @Test
  void testMergeAnalyticalObjectOrgUnitGroupSet() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization.getRows().add(ouGroupSetA);
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(2, visualization.getPeriods().size());
    assertEquals(1, visualization.getOrganisationUnitGroupSetDimensions().size());
    assertEquals(3, visualization.getOrganisationUnitGroupSetDimensions().get(0).getItems().size());
  }

  @Test
  void testMergeAnalyticalEventObjectOrgUnitGroupSet() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization.getRows().add(ouGroupSetA);
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(2, eventVisualization.getPeriods().size());
    assertEquals(1, eventVisualization.getOrganisationUnitGroupSetDimensions().size());
    assertEquals(
        3, eventVisualization.getOrganisationUnitGroupSetDimensions().get(0).getItems().size());
  }

  @Test
  void testMergeAnalyticalObjectDataElementGroupSet() {
    Visualization visualization = new Visualization();
    visualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    visualization.getRows().add(deGroupSetA);
    visualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    dimensionService.mergeAnalyticalObject(visualization);
    assertEquals(2, visualization.getDataDimensionItems().size());
    assertEquals(2, visualization.getPeriods().size());
    assertEquals(1, visualization.getDataElementGroupSetDimensions().size());
    assertEquals(3, visualization.getDataElementGroupSetDimensions().get(0).getItems().size());
  }

  @Test
  void testMergeAnalyticalEventObjectDataElementGroupSet() {
    // Given
    EventVisualization eventVisualization = new EventVisualization("any");
    eventVisualization
        .getColumns()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.DATA_X_DIM_ID,
                DimensionType.DATA_X,
                Lists.newArrayList(deA, deB)));
    eventVisualization.getRows().add(deGroupSetA);
    eventVisualization
        .getFilters()
        .add(
            new BaseDimensionalObject(
                DimensionalObject.PERIOD_DIM_ID,
                DimensionType.PERIOD,
                Lists.newArrayList(peA, peB)));
    // When
    dimensionService.mergeAnalyticalObject(eventVisualization);
    // Then
    assertEquals(2, eventVisualization.getDataDimensionItems().size());
    assertEquals(2, eventVisualization.getPeriods().size());
    assertEquals(1, eventVisualization.getDataElementGroupSetDimensions().size());
    assertEquals(3, eventVisualization.getDataElementGroupSetDimensions().get(0).getItems().size());
  }

  @Test
  void testGetDimensionalItemObject() {
    String idA = deA.getUid();
    String idB = prA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + deA.getUid();
    String idC = prA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + teaA.getUid();
    String idD =
        dsA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + ReportingRateMetric.REPORTING_RATE.name();
    String idE = dsA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + "UNKNOWN_METRIC";
    String idF = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + cocA.getUid();
    String idG = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + SYMBOL_WILDCARD;
    String idH = deA.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + "UNKNOWN_SYMBOL";
    String idI =
        deA.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocA.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocA.getUid();
    String idJ =
        deA.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocA.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + SYMBOL_WILDCARD;
    String idK =
        deA.getUid()
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + SYMBOL_WILDCARD
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + cocA.getUid();
    ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem(prA, deA);
    ProgramTrackedEntityAttributeDimensionItem ptaA =
        new ProgramTrackedEntityAttributeDimensionItem(prA, teaA);
    ReportingRate rrA = new ReportingRate(dsA, ReportingRateMetric.REPORTING_RATE);
    DataElementOperand deoA = new DataElementOperand(deA, cocA);
    DataElementOperand deoB = new DataElementOperand(deA, null);
    DataElementOperand deoC = new DataElementOperand(deA, cocA, cocA);
    DataElementOperand deoD = new DataElementOperand(deA, cocA, null);
    DataElementOperand deoE = new DataElementOperand(deA, null, cocA);
    assertNotNull(dimensionService.getDataDimensionalItemObject(idA));
    assertEquals(deA, dimensionService.getDataDimensionalItemObject(idA));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idB));
    assertEquals(pdeA, dimensionService.getDataDimensionalItemObject(idB));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idC));
    assertEquals(ptaA, dimensionService.getDataDimensionalItemObject(idC));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idD));
    assertEquals(rrA, dimensionService.getDataDimensionalItemObject(idD));
    assertNull(dimensionService.getDataDimensionalItemObject(idE));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idF));
    assertEquals(deoA, dimensionService.getDataDimensionalItemObject(idF));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idG));
    assertEquals(deoB, dimensionService.getDataDimensionalItemObject(idG));
    assertNull(dimensionService.getDataDimensionalItemObject(idH));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idI));
    assertEquals(deoC, dimensionService.getDataDimensionalItemObject(idI));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idJ));
    assertEquals(deoD, dimensionService.getDataDimensionalItemObject(idJ));
    assertNotNull(dimensionService.getDataDimensionalItemObject(idK));
    assertEquals(deoE, dimensionService.getDataDimensionalItemObject(idK));
  }

  @Test
  void testGetDataDimensionalItemObjectWithAttributeIdScheme() {
    assertEquals(deA, dimensionService.getDataDimensionalItemObject(IdScheme.from(atA), "DEA"));
    assertEquals(deB, dimensionService.getDataDimensionalItemObject(IdScheme.from(atA), "DEB"));
    assertEquals(deC, dimensionService.getDataDimensionalItemObject(IdScheme.from(atA), "DEC"));
    assertNull(dimensionService.getDataDimensionalItemObject(IdScheme.from(atA), "DSX"));
  }

  @Test
  void testGetDataDimensionalItemObjectMap() {
    Map<DimensionalItemId, DimensionalItemObject> result;
    result = dimensionService.getDataDimensionalItemObjectMap(new HashSet<>());
    assertNotNull(result);
    assertEquals(0, result.size());
    result = dimensionService.getDataDimensionalItemObjectMap(itemIds);
    assertEquals(itemMap, result);
  }

  @Test
  void testGetIndicatorDataDimensionalItemMapWithQueryMods() {
    Map<DimensionalItemId, DimensionalItemObject> result;
    // Given
    Set<DimensionalItemId> dimensionalItemIds = new HashSet<>();
    deA.setQueryMods(queryModsA);
    deB.setQueryMods(queryModsB);
    deC.setQueryMods(queryModsC);
    DimensionalItemId itemIdA = new DimensionalItemId(DATA_ELEMENT, deA.getUid(), queryModsA);
    DimensionalItemId itemIdB = new DimensionalItemId(DATA_ELEMENT, deB.getUid(), queryModsB);
    DimensionalItemId itemIdC = new DimensionalItemId(DATA_ELEMENT, deC.getUid(), queryModsC);
    dimensionalItemIds.add(itemIdA);
    dimensionalItemIds.add(itemIdB);
    dimensionalItemIds.add(itemIdC);
    Map<DimensionalItemId, DimensionalItemObject> dimensionalItemMap =
        Map.of(itemIdA, deA, itemIdB, deB, itemIdC, deC);
    // When
    result = dimensionService.getDataDimensionalItemObjectMap(dimensionalItemIds);
    // Then
    assertEquals(dimensionalItemMap, result);
  }

  @Test
  void testGetDataDimensionalItemObjectMapReturnsItemsWithAllOffsets() {
    Map<DimensionalItemId, DimensionalItemObject> result;
    DimensionalItemId deAId = new DimensionalItemId(DATA_ELEMENT, deA.getUid());
    DimensionalItemId deBId = new DimensionalItemId(DATA_ELEMENT, deB.getUid());
    DimensionalItemId deCId = new DimensionalItemId(DATA_ELEMENT, deC.getUid());
    DimensionalItemId deAOffset1Id = new DimensionalItemId(DATA_ELEMENT, deA.getUid(), queryModsA);
    DimensionalItemId deBOffset1Id = new DimensionalItemId(DATA_ELEMENT, deB.getUid(), queryModsA);
    DimensionalItemId deCOffset1Id = new DimensionalItemId(DATA_ELEMENT, deC.getUid(), queryModsA);
    DimensionalItemId deAOffset2Id = new DimensionalItemId(DATA_ELEMENT, deA.getUid(), queryModsB);
    DimensionalItemId deBOffset2Id = new DimensionalItemId(DATA_ELEMENT, deB.getUid(), queryModsB);
    DimensionalItemId deCOffset2Id = new DimensionalItemId(DATA_ELEMENT, deC.getUid(), queryModsB);
    DataElement deAOffset1 = makeDataElementWithQueryModsFrom(deA, queryModsA);
    DataElement deBOffset1 = makeDataElementWithQueryModsFrom(deB, queryModsA);
    DataElement deCOffset1 = makeDataElementWithQueryModsFrom(deC, queryModsA);
    DataElement deAOffset2 = makeDataElementWithQueryModsFrom(deA, queryModsB);
    DataElement deBOffset2 = makeDataElementWithQueryModsFrom(deB, queryModsB);
    DataElement deCOffset2 = makeDataElementWithQueryModsFrom(deC, queryModsB);
    Set<DimensionalItemId> dimensionalItemIds =
        Sets.newHashSet(
            deAId,
            deBId,
            deCId,
            deAOffset1Id,
            deBOffset1Id,
            deCOffset1Id,
            deAOffset2Id,
            deBOffset2Id,
            deCOffset2Id);
    ImmutableMap<DimensionalItemId, DimensionalItemObject> dimensionalItemMap =
        ImmutableMap.<DimensionalItemId, DimensionalItemObject>builder()
            .put(deAId, deA)
            .put(deBId, deB)
            .put(deCId, deC)
            .put(deAOffset1Id, deAOffset1)
            .put(deBOffset1Id, deBOffset1)
            .put(deCOffset1Id, deCOffset1)
            .put(deAOffset2Id, deAOffset2)
            .put(deBOffset2Id, deBOffset2)
            .put(deCOffset2Id, deCOffset2)
            .build();
    // When
    result = dimensionService.getDataDimensionalItemObjectMap(dimensionalItemIds);
    // Then
    assertMapEquals(dimensionalItemMap, result);
  }

  @Test
  void testGetDimensionalObjectEventReport() {
    EventReport report = new EventReport();
    report.setAutoFields();
    DataElement deA = createDataElement('A');
    LegendSet lsA = createLegendSet('A');
    ProgramStage psA = createProgramStage('A', 1);
    TrackedEntityDataElementDimension teDeDim =
        new TrackedEntityDataElementDimension(deA, lsA, psA, "EQ:1");
    report.addTrackedEntityDataElementDimension(teDeDim);
    report.getOrganisationUnits().addAll(Lists.newArrayList(ouA, ouB, ouC));
    report.getColumnDimensions().add(deA.getUid());
    report.getRowDimensions().add(DimensionalObject.ORGUNIT_DIM_ID);
    report.populateAnalyticalProperties();
    assertEquals(1, report.getColumns().size());
    assertEquals(1, report.getRows().size());
    DimensionalObject dim = report.getColumns().get(0);
    assertEquals(lsA, dim.getLegendSet());
    assertEquals(psA, dim.getProgramStage());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------
  /** Make a DataElement with query modifiers based on another DataElement. */
  private DataElement makeDataElementWithQueryModsFrom(
      DataElement dataElement, QueryModifiers queryMods) {
    DataElement de = SerializationUtils.clone(dataElement);
    de.setQueryMods(queryMods);
    return de;
  }
}
