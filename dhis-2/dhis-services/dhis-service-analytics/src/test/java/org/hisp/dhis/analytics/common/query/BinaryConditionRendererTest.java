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
package org.hisp.dhis.analytics.common.query;

import static org.hisp.dhis.analytics.common.query.Field.of;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.GE;
import static org.hisp.dhis.common.QueryOperator.GT;
import static org.hisp.dhis.common.QueryOperator.IEQ;
import static org.hisp.dhis.common.QueryOperator.ILIKE;
import static org.hisp.dhis.common.QueryOperator.IN;
import static org.hisp.dhis.common.QueryOperator.LE;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.common.QueryOperator.LT;
import static org.hisp.dhis.common.QueryOperator.NEQ;
import static org.hisp.dhis.common.QueryOperator.NILIKE;
import static org.hisp.dhis.common.QueryOperator.NLIKE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.params.dimension.AnalyticsQueryOperator;
import org.hisp.dhis.analytics.trackedentity.query.RenderableDataValue;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryOperator;
import org.junit.jupiter.api.Test;

class BinaryConditionRendererTest {
  @Test
  void testInWithSingleValueProduceCorrectSql() {
    genericTestExecutor(
        IN,
        List.of("v1"),
        ValueTypeMapping.STRING,
        "\"field\" = :1",
        List.of(getQueryContextAssertEqualsConsumer("v1")));
  }

  @Test
  void testNegatedInWithSingleValueProduceCorrectSql() {
    genericTestExecutor(
        AnalyticsQueryOperator.of(IN).negate(),
        List.of("v1"),
        ValueTypeMapping.STRING,
        "not (\"field\" = :1)",
        List.of(getQueryContextAssertEqualsConsumer("v1")));
  }

  @Test
  void testInNVWithSingleValueProduceCorrectSql() {
    genericTestExecutor(
        IN,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "\"field\" is null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  @Test
  void testInWithMultipleValueProduceCorrectSql() {
    genericTestExecutor(
        IN,
        List.of("v1", "v2"),
        ValueTypeMapping.STRING,
        "\"field\" in (:1)",
        List.of(getQueryContextAssertEqualsConsumer(List.of("v1", "v2"))));
  }

  @Test
  void testInNVWithMultipleValuesProduceCorrectSql() {
    genericTestExecutor(
        IN,
        List.of("NV", "v2"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or \"field\" in (:1))",
        List.of(getQueryContextAssertEqualsConsumer("v2")));
  }

  @Test
  void testEqWithSingleValueProduceCorrectSql() {
    genericTestExecutor(
        EQ,
        List.of("value"),
        ValueTypeMapping.STRING,
        "\"field\" = :1",
        List.of(getQueryContextAssertEqualsConsumer("value")));
  }

  @Test
  void testIEqWithSingleValueProduceCorrectSql() {
    genericTestExecutor(
        IEQ,
        List.of("vAlUe"),
        ValueTypeMapping.STRING,
        "lower(\"field\") = :1",
        List.of(getQueryContextAssertEqualsConsumer("value")));
  }

  @Test
  void testEqWithNVProduceCorrectSql() {
    genericTestExecutor(
        EQ,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "\"field\" is null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  @Test
  void testIEqWithNVProduceCorrectSql() {
    genericTestExecutor(
        IEQ,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "lower(\"field\") is null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  @Test
  void testEqWithMultipleValueProduceCorrectSql() {
    genericTestExecutor(
        EQ,
        List.of("v1", "v2"),
        ValueTypeMapping.STRING,
        "\"field\" in (:1)",
        List.of(getQueryContextAssertEqualsConsumer(List.of("v1", "v2"))));
  }

  @Test
  void testIEqWithMultipleValueProduceCorrectSql() {
    genericTestExecutor(
        IEQ,
        List.of("V1", "V2"),
        ValueTypeMapping.STRING,
        "lower(\"field\") in (:1)",
        List.of(getQueryContextAssertEqualsConsumer(List.of("v1", "v2"))));
  }

  @Test
  void testEqWithNVMultipleValueProduceCorrectSql() {
    genericTestExecutor(
        EQ,
        List.of("v1", "NV"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or \"field\" in (:1))",
        List.of(
            getQueryContextAssertEqualsConsumer("v1"),
            queryContext -> assertEquals(1, queryContext.getParametersPlaceHolder().size())));
  }

  @Test
  void testIEqWithNVMultipleValueProduceCorrectSql() {
    genericTestExecutor(
        IEQ,
        List.of("V1", "NV"),
        ValueTypeMapping.STRING,
        "(lower(\"field\") is null or lower(\"field\") in (:1))",
        List.of(
            getQueryContextAssertEqualsConsumer("v1"),
            queryContext -> assertEquals(1, queryContext.getParametersPlaceHolder().size())));
  }

  @Test
  void testLikeProduceCorrectSql() {
    genericTestExecutor(
        LIKE,
        List.of("value"),
        ValueTypeMapping.STRING,
        "\"field\" like :1",
        List.of(getQueryContextAssertEqualsConsumer("%value%")));
  }

  @Test
  void testNotLikeProduceCorrectSql() {
    genericTestExecutor(
        NLIKE,
        List.of("value"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or \"field\" not like :1)",
        List.of(getQueryContextAssertEqualsConsumer("%value%")));
  }

  @Test
  void testLikeCaseInsensitiveProduceCorrectSql() {
    genericTestExecutor(
        ILIKE,
        List.of("VaLuE"),
        ValueTypeMapping.STRING,
        "lower(\"field\") like :1",
        List.of(getQueryContextAssertEqualsConsumer("%value%")));
  }

  @Test
  void testNotLikeCaseInsensitiveProduceCorrectSql() {
    genericTestExecutor(
        NILIKE,
        List.of("VaLuE"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or lower(\"field\") not like :1)",
        List.of(getQueryContextAssertEqualsConsumer("%value%")));
  }

  @Test
  void testLikeWithNVProduceCorrectSql() {
    Stream.of(QueryOperator.LIKE, QueryOperator.ILIKE)
        .forEach(
            operator -> {
              genericTestExecutor(
                  operator,
                  List.of("NV"),
                  ValueTypeMapping.STRING,
                  "\"field\" is null",
                  List.of(getQueryContextAssertEmptyConsumer()));
            });
  }

  @Test
  void testNotLikeWithNVProduceCorrectSql() {
    Stream.of(QueryOperator.NLIKE, QueryOperator.NILIKE)
        .forEach(
            operator -> {
              genericTestExecutor(
                  operator,
                  List.of("NV"),
                  ValueTypeMapping.STRING,
                  "\"field\" is not null",
                  List.of(getQueryContextAssertEmptyConsumer()));
            });
  }

  @Test
  void testComparisonOperatorsWithBigIntegers() {
    Stream.of(GT, GE, LT, LE)
        .forEach(
            operator -> {
              genericTestExecutor(
                  operator,
                  List.of("100"),
                  ValueTypeMapping.NUMERIC,
                  "\"field\" " + operator.getValue() + " :1",
                  List.of(getQueryContextAssertEqualsConsumer(new BigInteger("100"))));
            });
  }

  @Test
  void testComparisonOperatorsWithBigDecimal() {
    Stream.of(GT, GE, LT, LE)
        .forEach(
            operator -> {
              genericTestExecutor(
                  operator,
                  List.of("100.1"),
                  ValueTypeMapping.DECIMAL,
                  "\"field\" " + operator.getValue() + " :1",
                  List.of(getQueryContextAssertEqualsConsumer(new BigDecimal("100.1"))));
            });
  }

  @Test
  void testNeWithNV() {
    genericTestExecutor(
        NEQ,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "\"field\" is not null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  @Test
  void testNeShouldIncludeNull() {
    genericTestExecutor(
        NEQ,
        List.of("test"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or \"field\" != :1)",
        List.of(getQueryContextAssertEqualsConsumer("test")));
  }

  @Test
  void testNeNV() {
    genericTestExecutor(
        NEQ,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "\"field\" is not null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  @Test
  void testNotLikeShouldIncludeNull() {
    genericTestExecutor(
        NLIKE,
        List.of("test"),
        ValueTypeMapping.STRING,
        "(\"field\" is null or \"field\" not like :1)",
        List.of(getQueryContextAssertEqualsConsumer("%test%")));
  }

  @Test
  void testNotLikeNV() {
    genericTestExecutor(
        NLIKE,
        List.of("NV"),
        ValueTypeMapping.STRING,
        "\"field\" is not null",
        List.of(getQueryContextAssertEmptyConsumer()));
  }

  private void genericTestExecutor(
      QueryOperator operator,
      List<String> values,
      ValueTypeMapping valueTypeMapping,
      String expectedSql,
      List<Consumer<QueryContext>> queryContextConsumers) {
    genericTestExecutor(
        AnalyticsQueryOperator.of(operator),
        values,
        valueTypeMapping,
        expectedSql,
        queryContextConsumers);
  }

  private void genericTestExecutor(
      AnalyticsQueryOperator analyticsQueryOperator,
      List<String> values,
      ValueTypeMapping valueTypeMapping,
      String expectedSql,
      List<Consumer<QueryContext>> queryContextConsumers) {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);
    String render =
        BinaryConditionRenderer.of(
                of("field"), analyticsQueryOperator, values, valueTypeMapping, queryContext)
            .render();
    assertEquals(expectedSql, render);
    queryContextConsumers.forEach(
        queryContextConsumer -> queryContextConsumer.accept(queryContext));
  }

  private Consumer<QueryContext> getQueryContextAssertEqualsConsumer(Object expectedValue) {
    return queryContext ->
        assertEquals(expectedValue, queryContext.getParametersPlaceHolder().get("1"));
  }

  private Consumer<QueryContext> getQueryContextAssertEmptyConsumer() {
    return queryContext -> assertTrue(queryContext.getParametersPlaceHolder().isEmpty());
  }

  @Test
  void testUnrecognizedOpThrowsIllegalArgumentException() {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);
    List<String> values = List.of("v1", "v2");
    BinaryConditionRenderer binaryConditionRenderer =
        BinaryConditionRenderer.of(
            of("field"), QueryOperator.EW, values, ValueTypeMapping.STRING, queryContext);
    IllegalQueryException exception =
        assertThrows(IllegalQueryException.class, binaryConditionRenderer::render);
    assertEquals("Operator not supported: `EW`", exception.getMessage());
  }

  @Test
  void testDataElementWithBooleanParameter() {
    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);
    List<String> values = List.of("1", "true", "0", "", "whatever", "TRUE", "TrUe");
    BinaryConditionRenderer binaryConditionRenderer =
        BinaryConditionRenderer.of(
            RenderableDataValue.of("alias", "dataValue", ValueTypeMapping.BOOLEAN),
            EQ,
            values,
            ValueTypeMapping.BOOLEAN,
            queryContext);
    String render = binaryConditionRenderer.render();

    assertEquals("(alias.\"eventdatavalues\" -> 'dataValue' ->> 'value')::BOOLEAN in (:1)", render);
    assertEquals(1, queryContext.getParametersPlaceHolder().size());
    assertInstanceOf(Collection.class, queryContext.getParametersPlaceHolder().get("1"));

    List<?> parameters =
        ((Collection) queryContext.getParametersPlaceHolder().get("1")).stream().toList();

    assertEquals(7, ((Collection) queryContext.getParametersPlaceHolder().get("1")).size());
    assertEquals(Boolean.TRUE, parameters.get(0));
    assertEquals(Boolean.TRUE, parameters.get(1));
    assertEquals(Boolean.FALSE, parameters.get(2));
    assertEquals(Boolean.FALSE, parameters.get(3));
    assertEquals(Boolean.FALSE, parameters.get(4));
    assertEquals(Boolean.TRUE, parameters.get(5));
    assertEquals(Boolean.TRUE, parameters.get(6));
  }
}
