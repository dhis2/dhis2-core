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

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.event.EventStatus.SCHEDULE;
import static org.hisp.dhis.program.ProgramStatus.ACTIVE;
import static org.hisp.dhis.program.ProgramStatus.COMPLETED;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
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

  @Mock protected SqlRowSet rowSet;

  protected ProgramStage programStage;

  protected ProgramStage programStageWithRepeatableParams;

  protected Program programA;

  protected Program programB;

  protected DataElement dataElementA;

  @BeforeEach
  void setUpData() {
    programA = createProgram('A');
    programB = createProgram('B');
    programStage = createProgramStage('B', programA);
    programStageWithRepeatableParams = createProgramStage('C', programB);
    programStageWithRepeatableParams.setRepeatable(true);
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
    params.withPeriods(getList(createPeriod("2000Q1")), "monthly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithStatuses() {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "monthly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withProgramStatuses(new LinkedHashSet<>(List.of(ACTIVE, COMPLETED)));
    params.withEventStatuses(new LinkedHashSet<>(List.of(SCHEDULE)));
    return params.build();
  }

  protected EventQueryParams createRequestParamsWithTimeField(final String timeField) {
    OrganisationUnit ouA = createOrganisationUnit('A');
    ouA.setPath("/" + ouA.getUid());
    EventQueryParams.Builder params = new EventQueryParams.Builder();
    params.withPeriods(getList(createPeriod("2000Q1")), "monthly");
    params.withOrganisationUnits(getList(ouA));
    params.withTableName(getTableName() + "_" + programA.getUid());
    params.withProgram(programA);
    params.withProgramStatuses(new LinkedHashSet<>(List.of(ACTIVE, COMPLETED)));
    params.withTimeField(timeField);
    return params.build();
  }

  protected EventQueryParams createRequestParams(
      ProgramStage withProgramStage, ValueType withQueryItemValueType) {
    EventQueryParams.Builder params = new EventQueryParams.Builder(_createRequestParams());
    DimensionalItemObject dio = new BaseDimensionalItemObject(dataElementA.getUid());
    params.withProgram(programA);
    if (withProgramStage != null) {
      params.withProgramStage(programStage);
    }
    if (withQueryItemValueType != null) {
      QueryItem queryItem = new QueryItem(dio);
      if (withProgramStage != null) {
        queryItem.setProgramStage(programStage);
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

  String getTable(String uid) {
    return getTableName() + "_" + uid;
  }

  abstract String getTableName();
}
