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
package org.hisp.dhis.analytics.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith(MockitoExtension.class)
class JdbcAnalyticsManagerTest {
  @Mock private SystemSettingsService settingsService;

  @Mock private PartitionManager partitionManager;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private SqlRowSet rowSet;

  @Mock private NestedIndicatorCyclicDependencyInspector nestedIndicatorCyclicDependencyInspector;

  @Mock private QueryPlanner queryPlanner;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @Captor private ArgumentCaptor<String> sql;

  @Mock private ExecutionPlanStore executionPlanStore;

  @InjectMocks private JdbcAnalyticsManager subject;

  @Test
  void verifyQueryGeneratedWhenDataElementHasLastAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.LAST);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedLastSql("desc");
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasLastAvgOrgUnitAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.LAST_AVERAGE_ORG_UNIT);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedLastSql("desc");
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasLastLastOrgUnitAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.LAST_LAST_ORG_UNIT);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    String subquery =
        "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\",\"daysno\","
            + "\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\","
            + "row_number() over (partition by ax.\"dx\" order by peenddate desc, pestartdate desc) as pe_rank "
            + "from analytics as ax where ax.\"pestartdate\" >= '2005-01-31' and ax.\"peenddate\" <= '2015-01-31' "
            + "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

    assertThat(sql.getValue(), containsString(subquery));
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasLastInPeriodAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.LAST_IN_PERIOD);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedLastInPeriodSql("desc");
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasLastInPeriodAvgOrgUnitAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.LAST_IN_PERIOD_AVERAGE_ORG_UNIT);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedLastInPeriodSql("desc");
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasMaxSumOrgUnitAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.MAX_SUM_ORG_UNIT);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedMaxMinSumOrgUnitSql("max");
  }

  @Test
  void verifyQueryGeneratedWhenDataElementHasMinSumOrgUnitAggregationType() {
    mockRowSet();

    DataQueryParams params = createParams(AggregationType.MIN_SUM_ORG_UNIT);

    subject.getAggregatedDataValues(params, AnalyticsTableType.DATA_VALUE, 20000);

    assertExpectedMaxMinSumOrgUnitSql("min");
  }

  @Test
  void testToQuotedFunctionString() {
    assertEquals(
        "min(\"value\") as \"value\",min(\"textvalue\") as \"textvalue\"",
        subject.toQuotedFunctionString("min", List.of("value", "textvalue")));

    assertEquals(
        "max(\"daysxvalue\") as \"daysxvalue\",max(\"daysno\") as \"daysno\"",
        subject.toQuotedFunctionString("max", List.of("daysxvalue", "daysno")));
  }

  @Test
  void testToQuotedList() {
    assertEquals(
        List.of("\"a\"\"b\"\"c\"", "\"d\"\"e\"\"f\""),
        subject.toQuotedList(List.of("a\"b\"c", "d\"e\"f")));

    assertEquals(
        List.of("\"ab\"", "\"cd\"", "\"ef\""), subject.toQuotedList(List.of("ab", "cd", "ef")));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void mockRowSet() {
    when(rowSet.next()).thenReturn(false);
    when(jdbcTemplate.queryForRowSet(sql.capture())).thenReturn(rowSet);
  }

  private DataQueryParams createParams(AggregationType aggregationType) {
    DataElement deA = createDataElement('A', ValueType.INTEGER, aggregationType);
    OrganisationUnit ouA = createOrganisationUnit('A');
    PeriodDimension peA = PeriodDimension.of("201501");

    return DataQueryParams.newBuilder()
        .withDataType(DataType.NUMERIC)
        .withTableName("analytics")
        .withAggregationType(AnalyticsAggregationType.fromAggregationType(aggregationType))
        .addDimension(new BaseDimensionalObject(DATA_X_DIM_ID, DimensionType.DATA_X, getList(deA)))
        .addFilter(
            new BaseDimensionalObject(
                ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, getList(ouA)))
        .addDimension(new BaseDimensionalObject(PERIOD_DIM_ID, DimensionType.PERIOD, getList(peA)))
        .build();
  }

  private void assertExpectedLastSql(String sortOrder) {
    String lastAggregationTypeSql =
        "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\","
            + "\"daysno\",\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\","
            + "row_number() over (partition by ax.\"dx\",ax.\"ou\",ax.\"co\",ax.\"ao\" order by peenddate "
            + sortOrder
            + ", pestartdate "
            + sortOrder
            + ") as pe_rank "
            + "from analytics as ax where ax.\"pestartdate\" >= '2005-01-31' and ax.\"peenddate\" <= '2015-01-31' "
            + "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

    assertThat(sql.getValue(), containsString(lastAggregationTypeSql));
  }

  private void assertExpectedLastInPeriodSql(String sortOrder) {
    String lastAggregationTypeSql =
        "(select \"year\",\"pestartdate\",\"peenddate\",\"oulevel\",\"daysxvalue\","
            + "\"daysno\",\"value\",\"textvalue\",\"dx\",cast('201501' as text) as \"pe\",\"ou\","
            + "row_number() over (partition by ax.\"dx\",ax.\"ou\",ax.\"co\",ax.\"ao\" order by peenddate "
            + sortOrder
            + ", pestartdate "
            + sortOrder
            + ") as pe_rank "
            + "from analytics as ax where ax.\"pestartdate\" >= '2015-01-01' and ax.\"peenddate\" <= '2015-01-31' "
            + "and (ax.\"value\" is not null or ax.\"textvalue\" is not null))";

    assertThat(sql.getValue(), containsString(lastAggregationTypeSql));
  }

  private void assertExpectedMaxMinSumOrgUnitSql(String maxOrMin) {
    String maxMinTypeSql =
        "(select ax.\"ou\",ax.\"dx\",ax.\"pe\","
            + maxOrMin
            + "(\"daysxvalue\") as \"daysxvalue\","
            + maxOrMin
            + "(\"daysno\") as \"daysno\","
            + maxOrMin
            + "(\"value\") as \"value\","
            + maxOrMin
            + "(\"textvalue\") as \"textvalue\" "
            + "from analytics as ax "
            + "where ax.\"dx\" in ('deabcdefghA') and ax.\"pe\" in ('201501') and ( ax.\"ou\" in ('ouabcdefghA') ) "
            + "group by ax.\"ou\",ax.\"dx\",ax.\"pe\")";

    assertThat(sql.getValue(), containsString(maxMinTypeSql));
  }
}
