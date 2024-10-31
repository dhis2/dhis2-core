/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data.sql;

import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.hisp.dhis.test.TestBase.createDataElement;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.data.JdbcAnalyticsManager;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class SelectClauseBuilderTest extends TestBase {

  private SqlBuilder sqlBuilder;
  private SelectClauseBuilder builder;

  @BeforeEach
  void setUp() {
    sqlBuilder = new PostgreSqlBuilder();
  }

  /** Tests the case where we have an aggregation query with SUM aggregation type */
  @Test
  void shouldBuildSelectClauseForSumAggregation() {
    // Given
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(List.of(createDimensionalObject("dx", "data")))
            .withValueColumn("value_column")
            .withAggregationType(AnalyticsAggregationType.SUM)
            .build();

    builder = new SelectClauseBuilder(params, sqlBuilder);

    // When
    String result = builder.buildForPostgres();

    var expected = invokeOld(params);
    // Then
    assertEquals(expected, result);
  }

  /** Tests the case with no dimensions */
  @Test
  void shouldBuildSelectClauseWithNoDimensions() {
    // Given
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withValueColumn("value_column")
            .withAggregationType(AnalyticsAggregationType.COUNT)
            .build();

    builder = new SelectClauseBuilder(params, sqlBuilder);

    // When
    String result = builder.buildForPostgres();
    String expected = invokeOld(params);
    // Then
    assertEquals(expected, result);
  }

  /** Tests the case where we have an average aggregation type */
  @Test
  void shouldBuildSelectClauseForAverageAggregation() {
    // Given
    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(List.of(createDimensionalObject("dx", "data")))
            .withValueColumn("value_column")
            .withAggregationType(AnalyticsAggregationType.AVERAGE)
            .build();

    builder = new SelectClauseBuilder(params, sqlBuilder);

    // When
    String result = builder.buildForPostgres();

    String expected = invokeOld(params);
    System.out.println("old: " + expected);
    System.out.println("new: " + result);
    // Then
    assertEquals(expected, result);
  }

  @Test
  public void withFIlter() {

    DataQueryParams params =
        DataQueryParams.newBuilder()
            .withDimensions(
                // PERIOD DIMENSION
                List.of(
                    new BaseDimensionalObject(
                        "pe",
                        DimensionType.PERIOD,
                        createPeriods("2020Q1", "2020Q2", "2020Q3", "2020Q4")),
                    new BaseDimensionalObject(
                        "dx",
                        DimensionType.DATA_X,
                        DISPLAY_NAME_DATA_X,
                        "display name",
                        List.of(
                            createDataElement('A', new CategoryCombo()),
                            createDataElement(
                                'B',
                                ValueType.TEXT,
                                AggregationType.COUNT,
                                DataElementDomain.AGGREGATE)))))
            .withFilters(
                List.of(
                    // OU FILTER
                    new BaseDimensionalObject(
                        "ou",
                        DimensionType.ORGANISATION_UNIT,
                        null,
                        DISPLAY_NAME_ORGUNIT,
                        List.of(new OrganisationUnit("bbb", "bbb", "OU_2", null, null, "c2")))))
            .withAggregationType(AnalyticsAggregationType.AVERAGE)
            .build();

    builder = new SelectClauseBuilder(params, sqlBuilder);

    // When
    String result = builder.buildForPostgres();
    String expected = invokeOld(params);
    // Then
    assertEquals(expected, result);
  }

  private DimensionalObject createDimensionalObject(String uid, String dimensionName) {
    DimensionalObject dim = mock(DimensionalObject.class);
    when(dim.getUid()).thenReturn(uid);
    when(dim.getDimensionName()).thenReturn(dimensionName);
    when(dim.isFixed()).thenReturn(false);
    return dim;
  }

  private String invokeOld(DataQueryParams params) {
    var jdbcAnalyticsManager =
        new JdbcAnalyticsManager(
            mock(QueryPlanner.class),
            mock(JdbcTemplate.class),
            mock(ExecutionPlanStore.class),
            sqlBuilder);
    return ReflectionTestUtils.invokeMethod(jdbcAnalyticsManager, "getSelectClause", params);
  }
}
