/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.trackedentity.query;

import static org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.common.IdScheme.UID;
import static org.hisp.dhis.util.DateUtils.toMediumDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParamType;
import org.hisp.dhis.analytics.common.params.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.trackedentity.query.context.sql.SqlParameterManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.Test;

/**
 * Tests for PeriodStaticDimensionCondition SQL generation. Validates SQL conditions for
 * period-based static dimensions with various filter formats.
 */
class PeriodStaticDimensionConditionTest {

  @Test
  void testEnrollmentDateWithDateRangeProducesCorrectSql() {
    // dimension=programUid.ENROLLMENT_DATE:2025-01-01_2025-12-31
    List<String> values = List.of("2025-01-01_2025-12-31");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENTDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"enrollmentdate\" >= :"),
        "Should contain GE condition. Actual: " + rendered);
    assertTrue(
        rendered.contains("\"enrollmentdate\" < :"),
        "Should contain LT condition. Actual: " + rendered);
    assertDateParameter(queryContext, "1", "2025-01-01");
    assertDateParameter(queryContext, "2", "2026-01-01");
  }

  @Test
  void testIncidentDateWithOperatorProducesCorrectSql() {
    // dimension=programUid.INCIDENT_DATE:GT:2025-01-01
    List<String> values = List.of("GT:2025-01-01");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.OCCURREDDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"occurreddate\" > :"),
        "Should contain GT condition. Actual: " + rendered);
    assertDateParameter(queryContext, "1", "2025-01-01");
  }

  @Test
  void testEnrollmentDateWithIsoPeriodProducesCorrectSql() {
    // dimension=programUid.ENROLLMENT_DATE:2025
    List<String> values = List.of("2025");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENTDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"enrollmentdate\" >= :"),
        "Should contain GE condition. Actual: " + rendered);
    assertTrue(
        rendered.contains("\"enrollmentdate\" < :"),
        "Should contain LT condition. Actual: " + rendered);
    assertDateParameter(queryContext, "1", "2025-01-01");
    assertDateParameter(queryContext, "2", "2026-01-01");
  }

  @Test
  void testEnrollmentDateWithQuarterlyPeriodProducesCorrectSql() {
    // dimension=programUid.ENROLLMENT_DATE:2025Q1
    List<String> values = List.of("2025Q1");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENTDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"enrollmentdate\" >= :"),
        "Should contain GE condition. Actual: " + rendered);
    assertTrue(
        rendered.contains("\"enrollmentdate\" < :"),
        "Should contain LT condition. Actual: " + rendered);
    assertDateParameter(queryContext, "1", "2025-01-01");
    assertDateParameter(queryContext, "2", "2025-04-01");
  }

  @Test
  void testIncidentDateWithNullValueProducesCorrectSql() {
    // dimension=programUid.INCIDENT_DATE:EQ:NV
    List<String> values = List.of("EQ:NV");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.OCCURREDDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.toLowerCase().contains("is null"),
        "EQ:NV should produce IS NULL condition. Actual: " + rendered);
  }

  @Test
  void testIncidentDateWithNotNullValueProducesCorrectSql() {
    // dimension=programUid.INCIDENT_DATE:NE:NV
    List<String> values = List.of("NE:NV");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.OCCURREDDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.toLowerCase().contains("is not null"),
        "NE:NV should produce IS NOT NULL condition. Actual: " + rendered);
  }

  @Test
  void testEnrollmentDateWithMultipleOperatorsProducesCorrectSql() {
    // dimension=programUid.ENROLLMENT_DATE:GE:2025-01-01;LE:2025-12-31
    List<String> values = List.of("GE:2025-01-01", "LE:2025-12-31");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENTDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"enrollmentdate\" >= :"),
        "Should contain GE condition. Actual: " + rendered);
    assertTrue(
        rendered.contains("\"enrollmentdate\" <= :"),
        "Should contain LE condition. Actual: " + rendered);
    assertDateParameter(queryContext, "1", "2025-01-01");
    assertDateParameter(queryContext, "2", "2025-12-31");
  }

  @Test
  void testEnrollmentDateWithRelativePeriodProducesCorrectSql() {
    // dimension=programUid.ENROLLMENT_DATE:THIS_YEAR
    List<String> values = List.of("THIS_YEAR");

    DimensionIdentifier<DimensionParam> dimensionIdentifier =
        getProgramDimensionIdentifier(
            "programUid", StringUtils.EMPTY, DimensionParam.StaticDimension.ENROLLMENTDATE, values);

    SqlParameterManager sqlParameterManager = new SqlParameterManager();
    QueryContext queryContext = QueryContext.of(null, sqlParameterManager);

    PeriodStaticDimensionCondition condition =
        PeriodStaticDimensionCondition.of(dimensionIdentifier, queryContext);

    String rendered = condition.render();

    assertTrue(
        rendered.contains("\"enrollmentdate\" >= :"),
        "Should contain GE condition. Actual: " + rendered);
    assertTrue(
        rendered.contains("\"enrollmentdate\" < :"),
        "Should contain LT condition. Actual: " + rendered);

    // Verify parameters are Date objects (actual dates depend on current date)
    Object param1 = queryContext.getParametersPlaceHolder().get("1");
    Object param2 = queryContext.getParametersPlaceHolder().get("2");
    assertTrue(param1 instanceof Date, "First parameter should be a Date: " + param1);
    assertTrue(param2 instanceof Date, "Second parameter should be a Date: " + param2);
  }

  private void assertDateParameter(
      QueryContext queryContext, String paramKey, String expectedDate) {
    Object param = queryContext.getParametersPlaceHolder().get(paramKey);
    assertTrue(param instanceof Date, "Parameter should be a Date: " + param);
    assertEquals(toMediumDate(expectedDate), param);
  }

  private DimensionIdentifier<DimensionParam> getProgramDimensionIdentifier(
      String programUid,
      String programStageUid,
      DimensionParam.StaticDimension dimension,
      List<String> items) {
    DimensionParam dimensionParam =
        DimensionParam.ofObject(dimension.name(), DimensionParamType.DIMENSIONS, UID, items);

    Program program = new Program();
    program.setUid(programUid);

    ElementWithOffset<ProgramStage> programStageElementWithOffset = emptyElementWithOffset();

    if (StringUtils.isNotBlank(programStageUid)) {
      ProgramStage programStage = new ProgramStage();
      programStage.setUid(programStageUid);
      programStageElementWithOffset = ElementWithOffset.of(programStage, 0);
    }

    return DimensionIdentifier.of(
        ElementWithOffset.of(program, 0), programStageElementWithOffset, dimensionParam);
  }
}
