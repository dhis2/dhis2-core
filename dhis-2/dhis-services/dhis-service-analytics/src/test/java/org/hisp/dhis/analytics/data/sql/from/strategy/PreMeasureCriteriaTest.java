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
package org.hisp.dhis.analytics.data.sql.from.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.MeasureFilter;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreMeasureCriteriaTest {

  private PreMeasureCriteria preMeasureCriteria;
  private DataQueryParams params;
  private PostgreSqlBuilder sqlBuilder;

  @BeforeEach
  void setUp() {
    // Initialize SqlBuilder and params for each test
    sqlBuilder = new PostgreSqlBuilder();
  }

  @Test
  void testBuildSubqueryWithoutPreMeasureCriteria() {
    // No pre-aggregate measure criteria
    params = createParamsWithPreAggregateMeasureCriteria(Map.of()); // No measure criteria
    preMeasureCriteria = new PreMeasureCriteria(params, sqlBuilder);

    String expectedSubquery =
        "(select * from analytics_table as ax)"; // No WHERE clause (no criteria)

    // Act
    String subquery = preMeasureCriteria.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithSinglePreMeasureCriteria() {
    // Arrange: Define one pre-aggregate measure criterion (filter > 100)
    params = createParamsWithPreAggregateMeasureCriteria(Map.of(MeasureFilter.GT, 100.0));
    preMeasureCriteria = new PreMeasureCriteria(params, sqlBuilder);

    // Expected SQL with a single criterion where 'value > 100'
    String expectedSubquery = "(select * from analytics_table as ax WHERE value > 100.0)";

    // Act
    String subquery = preMeasureCriteria.buildSubquery();

    // Assert
    assertEquals(expectedSubquery, subquery);
  }

  @Test
  void testBuildSubqueryWithMultiplePreMeasureCriteria() {
    // Arrange: Define multiple pre-measure criteria
    params =
        createParamsWithPreAggregateMeasureCriteria(
            Map.of(
                MeasureFilter.GT, 100.0, // value > 100
                MeasureFilter.LE, 200.0 // value <= 200
                ));
    preMeasureCriteria = new PreMeasureCriteria(params, sqlBuilder);

    String subquery = preMeasureCriteria.buildSubquery();

    // Assert
    assertTrue(subquery.contains("value > 100.0"));
    assertTrue(subquery.contains("value <= 200.0"));
    assertTrue(subquery.contains("WHERE"));

    assertTrue(subquery.startsWith("(select * from analytics_table as ax"));
    assertTrue(subquery.endsWith(")"));
  }

  private DataQueryParams createParamsWithPreAggregateMeasureCriteria(
      Map<MeasureFilter, Double> preAggregateMeasureCriteria) {
    return DataQueryParams.newBuilder()
        .withTableName("analytics_table") // Use a test table
        .withPreAggregationMeasureCriteria(
            preAggregateMeasureCriteria) // Provide pre-aggregate criteria
        .build();
  }
}
