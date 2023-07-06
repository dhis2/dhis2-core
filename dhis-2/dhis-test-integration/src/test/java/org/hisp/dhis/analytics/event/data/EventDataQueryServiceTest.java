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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.AnalyticsType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.EventDataQueryRequest;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.legend.LegendSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
class EventDataQueryServiceTest extends SingleSetupIntegrationTestBase {

  private Program prA;

  private ProgramStage psA;

  private Period peA;

  private Period peB;

  private OrganisationUnit ouA;

  private OrganisationUnit ouB;

  private DataElement deA;

  private DataElement deB;

  private DataElement deC;

  private TrackedEntityAttribute atA;

  private TrackedEntityAttribute atB;

  private LegendSet legendSetA;

  private Legend legendA;

  private Legend legendB;

  @Autowired private EventDataQueryService dataQueryService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private DataElementService dataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private LegendSetService legendSetService;

  @Override
  public void setUpTest() {
    peA = PeriodType.getPeriodFromIsoString("201401");
    peB = PeriodType.getPeriodFromIsoString("201402");
    ouA = createOrganisationUnit('A');
    ouB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(ouA);
    organisationUnitService.addOrganisationUnit(ouB);
    deA = createDataElement('A');
    deB = createDataElement('B');
    deC = createDataElement('C');
    deC.setValueType(ValueType.COORDINATE);
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deC);
    atA = createTrackedEntityAttribute('A');
    atB = createTrackedEntityAttribute('B');
    attributeService.addTrackedEntityAttribute(atA);
    attributeService.addTrackedEntityAttribute(atB);
    prA = createProgram('A', null, Sets.newHashSet(atA, atB), Sets.newHashSet(ouA, ouB), null);
    programService.addProgram(prA);
    psA = createProgramStage('A', 0);
    psA.addDataElement(deA, 0);
    psA.addDataElement(deB, 1);
    prA.getProgramStages().add(psA);
    programStageService.saveProgramStage(psA);
    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 10d, 20d);
    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);
    legendSetService.addLegendSet(legendSetA);
  }

  @Test
  void testGetFromUrlA() {
    final String coordinateField = "EVENT";
    final String fallbackCoordinateField = "ougeometry";
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401;201402");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .coordinateField(coordinateField)
            .fallbackCoordinateField(fallbackCoordinateField)
            .coordinateOuFallback(true)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(2, params.getFilterPeriods().size());
    assertEquals("psigeometry", params.getCoordinateField());
    assertEquals(fallbackCoordinateField, params.getFallbackCoordinateField());
    assertTrue(params.isCoordinateOuFallback());
  }

  @Test
  void testGetPeriods() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";");
    dimensionParams.add("pe:LAST_WEEK;TODAY:LAST_UPDATED;20220101_20220201:INCIDENT_DATE");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    DimensionalObject pe = params.getDimension("pe");
    assertEquals(3, pe.getItems().size());
    assertTrue(streamOfPeriods(pe).anyMatch(Period::isDefault));
    assertTrue(
        streamOfPeriods(pe).map(Period::getDateField).anyMatch(s -> s.equals("LAST_UPDATED")));
    assertTrue(
        streamOfPeriods(pe).map(Period::getDateField).anyMatch(s -> s.equals("INCIDENT_DATE")));
    assertTrue(
        streamOfPeriods(pe)
            .filter(period -> "INCIDENT_DATE".equals(period.getDateField()))
            .anyMatch(
                period ->
                    period.getStartDate().equals(of(2022, 1, 1))
                        && period.getEndDate().equals(of(2022, 2, 1))));
  }

  private Date of(int year, int month, int dayOfMonth) {
    return Date.from(
        LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private Stream<Period> streamOfPeriods(DimensionalObject pe) {
    return pe.getItems().stream().map(dimensionalItemObject -> (Period) dimensionalItemObject);
  }

  @Test
  void testGetFromUrlB() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .value(deA.getUid())
            .aggregationType(AggregationType.AVERAGE)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(deA, params.getValue());
    assertEquals(AnalyticsAggregationType.AVERAGE, params.getAggregationType());
    assertFalse(params.isCoordinateOuFallback());
    assertEquals("psigeometry", params.getCoordinateField());
    assertEquals(
        "psigeometry,pigeometry,teigeometry,ougeometry", params.getFallbackCoordinateField());
  }

  @Test
  void testGetFromUrlWithEventDateSorting() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401");
    Set<String> desc = new HashSet<>();
    desc.add("eventdate");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .value(deA.getUid())
            .aggregationType(AggregationType.AVERAGE)
            .desc(desc)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(deA, params.getValue());
    assertEquals(1, params.getDesc().size());
    assertEquals("executiondate", params.getDesc().get(0).getItem().getName());
    assertEquals(AnalyticsAggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testGetFromUrlWithOrgUnitNameSorting() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401");
    Set<String> desc = new HashSet<>();
    desc.add("ouname");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .value(deA.getUid())
            .aggregationType(AggregationType.AVERAGE)
            .desc(desc)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(deA, params.getValue());
    assertEquals(1, params.getDesc().size());
    assertEquals("ouname", params.getDesc().get(0).getItem().getName());
    assertEquals(AnalyticsAggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testGetFromUrlWithDataElementSorting() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401");
    Set<String> desc = new HashSet<>();
    desc.add(deA.getUid());
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .value(deA.getUid())
            .aggregationType(AggregationType.AVERAGE)
            .desc(desc)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(deA, params.getValue());
    assertEquals(1, params.getDesc().size());
    assertEquals(deA.getUid(), params.getDesc().get(0).getItem().getUid());
    assertEquals(AnalyticsAggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testGetFromUrlWithProgramAttributeSorting() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add("ou:" + ouA.getUid() + ";" + ouB.getId());
    dimensionParams.add(atA.getUid() + ":LE:5");
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401");
    Set<String> desc = new HashSet<>();
    desc.add(atA.getUid());
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .value(deA.getUid())
            .aggregationType(AggregationType.AVERAGE)
            .desc(desc)
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(1, params.getItems().size());
    assertEquals(1, params.getFilterPeriods().size());
    assertEquals(deA, params.getValue());
    assertEquals(1, params.getDesc().size());
    assertEquals(atA.getUid(), params.getDesc().get(0).getItem().getUid());
    assertEquals(AnalyticsAggregationType.AVERAGE, params.getAggregationType());
  }

  @Test
  void testGetFromAnalyticalObjectA() {
    EventChart eventChart = new EventChart();
    eventChart.setAutoFields();
    eventChart.setProgram(prA);
    eventChart.getColumnDimensions().add(atA.getUid());
    eventChart.getRowDimensions().add(DimensionalObject.ORGUNIT_DIM_ID);
    eventChart.getFilterDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    eventChart.getAttributeDimensions().add(new TrackedEntityAttributeDimension(atA, null, "LE:5"));
    eventChart.getPeriods().add(peA);
    eventChart.getPeriods().add(peB);
    eventChart.getOrganisationUnits().add(ouA);
    eventChart.getOrganisationUnits().add(ouB);
    EventQueryParams params = dataQueryService.getFromAnalyticalObject(eventChart);
    assertNotNull(params);
    assertEquals(1, params.getItems().size());
    assertEquals(2, params.getOrganisationUnits().size());
    assertEquals(2, params.getFilterPeriods().size());
  }

  @Test
  void testGetFromAnalyticalObjectB() {
    EventChart eventChart = new EventChart();
    eventChart.setAutoFields();
    eventChart.setProgram(prA);
    eventChart.getColumnDimensions().add(atA.getUid());
    eventChart.getColumnDimensions().add(deA.getUid());
    eventChart.getRowDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    eventChart.getFilterDimensions().add(DimensionalObject.ORGUNIT_DIM_ID);
    eventChart.getAttributeDimensions().add(new TrackedEntityAttributeDimension(atA, null, "LE:5"));
    eventChart
        .getDataElementDimensions()
        .add(new TrackedEntityDataElementDimension(deA, null, null, "GE:100"));
    eventChart.getPeriods().add(peA);
    eventChart.getPeriods().add(peB);
    eventChart.getOrganisationUnits().add(ouA);
    eventChart.getOrganisationUnits().add(ouB);
    EventQueryParams params = dataQueryService.getFromAnalyticalObject(eventChart);
    assertNotNull(params);
    assertEquals(2, params.getItems().size());
    assertEquals(2, params.getPeriods().size());
    assertEquals(2, params.getFilterOrganisationUnits().size());
  }

  @Test
  void testGetFromAnalyticalObjectC() {
    EventChart eventChart = new EventChart();
    eventChart.setAutoFields();
    eventChart.setProgram(prA);
    eventChart.getColumnDimensions().add(deA.getUid());
    eventChart.getColumnDimensions().add(atA.getUid());
    eventChart.getRowDimensions().add(DimensionalObject.ORGUNIT_DIM_ID);
    eventChart.getFilterDimensions().add(DimensionalObject.PERIOD_DIM_ID);
    eventChart
        .getDataElementDimensions()
        .add(new TrackedEntityDataElementDimension(deA, null, null, "GT:2000"));
    eventChart.getAttributeDimensions().add(new TrackedEntityAttributeDimension(atA, null, "LE:5"));
    eventChart.getPeriods().add(peA);
    eventChart.getPeriods().add(peB);
    eventChart.getOrganisationUnits().add(ouA);
    EventQueryParams params = dataQueryService.getFromAnalyticalObject(eventChart);
    assertNotNull(params);
    assertEquals(2, params.getItems().size());
    assertEquals(1, params.getOrganisationUnits().size());
    assertEquals(2, params.getFilterPeriods().size());
  }

  @Test
  void testSetItemsForDimensionFilters() {
    TrackedEntityAttribute tea = new TrackedEntityAttribute();
    tea.setAutoFields();
    TrackedEntityAttributeDimension tead = new TrackedEntityAttributeDimension(tea, null, "EQ:2");
    EventChart eventChart = new EventChart();
    eventChart.setAutoFields();
    eventChart.getColumnDimensions().add(tea.getUid());
    eventChart.getAttributeDimensions().add(tead);
    Grid grid = new ListGrid();
    grid.addHeader(new GridHeader(tea.getUid(), tea.getName()));
    grid.addRow().addValue("1");
    grid.addRow().addValue("2");
    grid.addRow().addValue("3");
    grid.addRow().addValue(null);
    eventChart.populateAnalyticalProperties();
    DimensionalObject dim = eventChart.getColumns().get(0);
    DimensionalObjectUtils.setDimensionItemsForFilters(dim, grid, true);
    assertNotNull(dim);
    assertEquals(DimensionType.PROGRAM_ATTRIBUTE, dim.getDimensionType());
    assertEquals(AnalyticsType.EVENT, dim.getAnalyticsType());
    assertEquals(tead.getFilter(), dim.getFilter());
    List<DimensionalItemObject> items = dim.getItems();
    assertEquals(4, items.size());
    assertNotNull(items.get(0).getUid());
    assertNotNull(items.get(0).getName());
    assertNotNull(items.get(0).getCode());
    assertNotNull(items.get(0).getShortName());
  }

  @Test
  void testGetFromUrlLegendSet() {
    Set<String> dimensionParams = new HashSet<>();
    dimensionParams.add(
        deA.getUid()
            + "-"
            + legendSetA.getUid()
            + ":IN:"
            + legendA.getUid()
            + ";"
            + legendB.getUid());
    Set<String> filterParams = new HashSet<>();
    filterParams.add("pe:201401;201402");
    filterParams.add(atA.getUid() + ":LE:5");
    EventDataQueryRequest request =
        EventDataQueryRequest.builder()
            .program(prA.getUid())
            .dimension(Set.of(dimensionParams))
            .filter(Set.of(filterParams))
            .build();
    EventQueryParams params = dataQueryService.getFromRequest(request);
    assertEquals(prA, params.getProgram());
    assertEquals(1, params.getItems().size());
    assertEquals(legendSetA, params.getItems().get(0).getLegendSet());
    assertEquals(1, params.getItemFilters().size());
    assertEquals(2, params.getFilterPeriods().size());
  }

  @Test
  void testGetCoordinateField() {
    assertEquals(
        "psigeometry",
        dataQueryService.getCoordinateField(EventQueryParams.EVENT_COORDINATE_FIELD));
    assertEquals(
        "pigeometry",
        dataQueryService.getCoordinateField(EventQueryParams.ENROLLMENT_COORDINATE_FIELD));
    assertEquals("psigeometry", dataQueryService.getCoordinateField(null));
    assertEquals(deC.getUid(), dataQueryService.getCoordinateField(deC.getUid()));
  }

  @Test
  void testGetInvalidCoordinateFieldException() {
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getCoordinateField("someField"));
  }

  @Test
  void testGetNonCoordinateValueTypeCoordinateFieldException() {
    assertThrows(
        IllegalQueryException.class, () -> dataQueryService.getCoordinateField(deA.getUid()));
  }
}
