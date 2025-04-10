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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.event.EventStatus.SCHEDULE;
import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.EnrollmentStatus.COMPLETED;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.TestBase.createPeriod;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.MeasureFilter;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.RequestTypeAware;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.relationship.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
abstract class EventAnalyticsTest {
  private static final String COL_NAME_ENROLLMENT_GEOMETRY = "enrollmentgeometry";

  private static final String COL_NAME_EVENT_GEOMETRY = "eventgeometry";

  private static final String COL_NAME_TRACKED_ENTITY_GEOMETRY = "tegeometry";

  private static final String COL_NAME_OU_GEOMETRY = "ougeometry";

  private static final List<String> COL_NAME_GEOMETRY_LIST =
      List.of(
          COL_NAME_EVENT_GEOMETRY, COL_NAME_ENROLLMENT_GEOMETRY,
          COL_NAME_TRACKED_ENTITY_GEOMETRY, COL_NAME_OU_GEOMETRY);

  @Mock protected SqlRowSet rowSet;

  protected ProgramStage programStage;

  protected ProgramStage repeatableProgramStage;

  protected Program programA;

  protected Program programB;

  protected DataElement dataElementA;

  @BeforeEach
  void setUpData() {
    programA = createProgram('A');
    programB = createProgram('B');
    programStage = createProgramStage('B', programA);
    repeatableProgramStage = createProgramStage('A', programA);
    repeatableProgramStage.setRepeatable(true);
    dataElementA = createDataElement('A', ValueType.INTEGER, AggregationType.SUM);
    dataElementA.setUid("fWIAEtYVEGk");
  }

  protected EventQueryParams createRequestParams(
      ProgramIndicator programIndicator, RelationshipType relationshipType) {
    EventQueryParams.Builder params = new EventQueryParams.Builder(_createRequestParams());
    params.addItem(
        new QueryItem(
            programIndicator,
            programIndicator.getProgram(),
            null,
            ValueType.NUMBER,
            programIndicator.getAggregationType(),
            null,
            relationshipType));
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithFilter(
      ProgramStage withProgramStage, ValueType withQueryItemValueType) {
    return createRequestParamsWithFilter(
        withProgramStage, withQueryItemValueType, QueryOperator.GT, "10");
  }

  protected EventQueryParams createRequestParamsMeasureCriteria(
      ProgramStage withProgramStage, ValueType withQueryItemValueType) {
    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(withProgramStage, withQueryItemValueType));

    params.addMeasureCriteria(MeasureFilter.GT, 10.0);
    params.addMeasureCriteria(MeasureFilter.LT, 20.0);
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithFilter(
      ProgramStage withProgramStage,
      ValueType withQueryItemValueType,
      QueryOperator withOperator,
      String withQueryFilter) {
    EventQueryParams.Builder params =
        new EventQueryParams.Builder(createRequestParams(withProgramStage, withQueryItemValueType));
    QueryItem queryItem = params.build().getItems().get(0);
    queryItem.addFilter(new QueryFilter(withOperator, withQueryFilter));
    return params.build();
  }

  protected EventQueryParams createRequestParams() {
    return _createRequestParams();
  }

  protected EventQueryParams.Builder createRequestParamsBuilder() {
    return new EventQueryParams.Builder(_createRequestParams());
  }

  protected EventQueryParams createRequestParams(ValueType queryItemValueType) {
    return createRequestParams(null, queryItemValueType);
  }

  protected EventQueryParams createRequestParams(ProgramStage withProgramStage) {
    return createRequestParams(withProgramStage, null);
  }

  protected EventQueryParams createRequestParams(QueryItem queryItem) {
    EventQueryParams.Builder params = new EventQueryParams.Builder(_createRequestParams());
    params.addItem(queryItem);
    return params.build();
  }

  private EventQueryParams _createRequestParams() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "quarterly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withCoordinateFields(COL_NAME_GEOMETRY_LIST);
    params.withRowContext(true);
    params.withEndpointItem(RequestTypeAware.EndpointItem.ENROLLMENT);
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithStatuses() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "quarterly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withEnrollmentStatuses(new LinkedHashSet<>(List.of(ACTIVE, COMPLETED)));
    params.withEventStatuses(new LinkedHashSet<>(List.of(SCHEDULE)));
    params.withCoordinateFields(COL_NAME_GEOMETRY_LIST);
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithMultipleQueries() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "quarterly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withMultipleQueries(true);
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithTimeField(String timeField) {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "quarterly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withEnrollmentStatuses(new LinkedHashSet<>(List.of(ACTIVE, COMPLETED)));
    params.withCoordinateFields(COL_NAME_GEOMETRY_LIST);
    params.withTimeField(timeField);
    return params.build();
  }

  protected EventQueryParams createRequestParams(
      ProgramStage withProgramStage, ValueType withQueryItemValueType) {
    EventQueryParams.Builder params = new EventQueryParams.Builder(_createRequestParams());
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());
    params.withProgram(programA);
    if (withProgramStage != null) {
      params.withProgramStage(withProgramStage);
    }
    if (withQueryItemValueType != null) {
      QueryItem queryItem = new QueryItem(dio);
      if (withProgramStage != null) {
        queryItem.setProgramStage(withProgramStage);
        if (withProgramStage.getRepeatable()) {
          RepeatableStageParams repeatableStageParams = new RepeatableStageParams();
          repeatableStageParams.setDimension(
              withProgramStage.getUid() + "[-1]." + dataElementA.getUid());
          repeatableStageParams.setIndex(-1);
          queryItem.setRepeatableStageParams(repeatableStageParams);
        }
      }
      queryItem.setProgram(programA);
      queryItem.setValueType(withQueryItemValueType);
      params.addItem(queryItem);
    }
    return params.build();
  }

  void mockEmptyRowSet() {
    when(rowSet.next()).thenReturn(false);
  }

  void mockGivenRowsRowSet(int rows) {
    GivenRowsRowSet fiftyRowsRowSet = new GivenRowsRowSet(rows);
    when(rowSet.next())
        .thenAnswer(
            invocationOnMock -> {
              fiftyRowsRowSet.increaseRow();
              return !fiftyRowsRowSet.isLastRow();
            });
  }

  String getTable(String uid) {
    return getTableName() + "_" + uid;
  }

  abstract String getTableName();

  private static class GivenRowsRowSet {

    private final int rows;
    private int count = 0;

    public GivenRowsRowSet(int rows) {
      this.rows = rows;
    }

    void increaseRow() {
      count++;
    }

    boolean isLastRow() {
      return count > rows;
    }
  }
}
