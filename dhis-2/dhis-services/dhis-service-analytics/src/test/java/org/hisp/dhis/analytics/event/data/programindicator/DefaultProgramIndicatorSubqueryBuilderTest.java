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
package org.hisp.dhis.analytics.event.data.programindicator;

import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.EVENT_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.SCHEDULED_DATE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD;
import static org.hisp.dhis.program.AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProgramIndicatorSubqueryBuilderTest {

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private SystemSettingsService settingsService;

  @Mock private DataElementService dataElementService;

  @Spy private SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  @InjectMocks private DefaultProgramIndicatorSubqueryBuilder builder;

  private ProgramIndicator programIndicator;
  private CteContext cteContext;
  private final Date startDate = new Date();
  private final Date endDate = new Date();
  private final String piUid = "TestPiUid1";
  private final String psUid1 = "PgmStgUid1";
  private final String progUid = "programUid1";
  private final String deUid2 = "DataElmUid2";
  private final String programUid = "TestProgUid1";
  private final String eventTable = "analytics_event_" + progUid;
  private final String enrollmentTable = "analytics_enrollment_" + progUid.toLowerCase();
  private final String subax = "subax";
  private ProgramIndicator testPI;

  @BeforeEach
  void setUp() {

    Program program = new Program();
    program.setUid(progUid);

    programIndicator = new ProgramIndicator();
    programIndicator.setUid(piUid);
    programIndicator.setProgram(program);
    programIndicator.setAnalyticsType(AnalyticsType.ENROLLMENT); // Focus on enrollment context

    cteContext = new CteContext(EndpointItem.ENROLLMENT);

    program = new Program();
    program.setUid(programUid);
    testPI = new ProgramIndicator();
    testPI.setUid(piUid);
    testPI.setProgram(program);
    testPI.setAnalyticsPeriodBoundaries(Collections.emptySet());

    builder.init();
  }

  @Test
  void testAddCteWithExpressionOnlyValuePlaceholder() {
    programIndicator.setAggregationType(AggregationType.SUM);
    programIndicator.setExpression("V{event_date}");
    programIndicator.setFilter(null);
    String expectedValueCteKey = "varcte_occurreddate_" + piUid + "_0";

    when(programIndicatorService.getAnalyticsSql(
            eq("V{event_date}"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn(
            "FUNC_CTE_VAR( type='vEventDate', column='occurreddate', piUid='"
                + piUid
                + "', psUid='null', offset='0')");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    assertTrue(cteContext.containsCte(expectedValueCteKey), "Value CTE should be added");
    CteDefinition valueCte = cteContext.getDefinitionByKey(expectedValueCteKey);
    assertNotNull(valueCte, "Value CTE Definition should not be null");
    assertTrue(valueCte.isVariable(), "Should be marked as Variable CTE");
    assertTrue(
        valueCte.getCteDefinition().contains("select enrollment, \"occurreddate\" as value"),
        "Value CTE SQL content check");
    assertTrue(
        valueCte
            .getCteDefinition()
            .contains(
                "row_number() over (partition by enrollment order by occurreddate desc) as rn"),
        "Value CTE SQL content check");
    assertTrue(
        valueCte.getCteDefinition().contains("from " + eventTable), "Value CTE SQL content check");
    assertFalse(
        valueCte.getCteDefinition().contains("limit 1"),
        "Value CTE SQL should not contain LIMIT 1");
    assertFalse(
        valueCte.getCteDefinition().contains("= subax.enrollment"),
        "Value CTE SQL should not contain subax correlation");

    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should be added");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid); // Get the definition
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition(); // Get the actual SQL
    String valueCteAlias = valueCte.getAlias(); // Get the generated alias

    // Assert directly on the mainCteSql string
    assertTrue(mainCteSql.startsWith("select subax.enrollment, "), "Main CTE SQL start check");
    // Check aggregation function (assuming SUM based on default non-custom type)
    assertTrue(
        mainCteSql.contains("sum(" + valueCteAlias + ".value)"),
        "Main CTE SQL placeholder replacement check");
    assertTrue(
        mainCteSql.contains(" from " + enrollmentTable.toLowerCase() + " as " + subax),
        "Main CTE SQL FROM clause check");
    assertTrue(
        mainCteSql.contains(
            "left join "
                + valueCteAlias
                + " "
                + valueCteAlias
                + " on "
                + valueCteAlias
                + ".enrollment = "
                + subax
                + ".enrollment and "
                + valueCteAlias
                + ".rn = 1"),
        "Main CTE SQL LEFT JOIN check");
    assertFalse(mainCteSql.contains(" inner join "), "Main CTE SQL should not contain INNER JOIN");
    assertFalse(mainCteSql.contains(" where "), "Main CTE SQL should not contain WHERE");
    assertTrue(mainCteSql.endsWith(" group by subax.enrollment"), "Main CTE SQL end check");
  }

  @Test
  void testAddCteWithSimpleFilterOnly() {
    programIndicator.setExpression("1"); // Simple expression
    programIndicator.setFilter("V{event_status} == 'ACTIVE'");
    String expectedFilterCteKey = "filtercte_eventstatus_eqeq_active_" + piUid;

    when(programIndicatorService.getAnalyticsSql(
            eq("1"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn("1");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Assert
    assertTrue(cteContext.containsCte(expectedFilterCteKey), "Filter CTE should be added");
    CteDefinition filterCte = cteContext.getDefinitionByKey(expectedFilterCteKey);
    assertNotNull(filterCte, "Filter CTE Definition should not be null");
    assertTrue(filterCte.isFilter(), "Should be marked as Filter CTE");
    // Verify filtering CTE structure using normalized comparison
    String expectedFilterSql =
        String.format(
            "select enrollment from ( "
                + "select enrollment, \"eventstatus\", row_number() over (partition by enrollment order by occurreddate desc) as rn "
                + "from %s where \"eventstatus\" is not null"
                + " ) latest "
                + "where rn = 1 and \"eventstatus\" = 'ACTIVE'",
            eventTable);
    assertEquals(
        normalizeSql(expectedFilterSql),
        normalizeSql(filterCte.getCteDefinition()),
        "Filter CTE SQL structure check");

    // Verify Main PI CTE was added and uses INNER JOIN
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should be added");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid); // Get the definition
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition(); // Get SQL directly from definition
    String filterCteAlias = filterCte.getAlias();

    assertTrue(mainCteSql.startsWith("select subax.enrollment, "), "Main CTE SQL start check");
    assertTrue(
        mainCteSql.contains("avg(1)"),
        "Main CTE SQL expression check"); // Simple expression used, assuming SUM default
    assertTrue(
        mainCteSql.contains(" from " + enrollmentTable + " as " + subax),
        "Main CTE SQL FROM clause check");
    assertTrue(
        mainCteSql.contains(
            "inner join "
                + expectedFilterCteKey
                + " "
                + filterCteAlias
                + " on "
                + filterCteAlias
                + ".enrollment = "
                + subax
                + ".enrollment"),
        "Main CTE SQL INNER JOIN check"); // Correct INNER JOIN
    assertFalse(
        mainCteSql.contains(" left join "),
        "Main CTE SQL should not contain LEFT JOIN"); // No value joins
    assertFalse(
        mainCteSql.contains(" where "),
        "Main CTE SQL should not contain WHERE"); // No complex filter where clause
    assertTrue(mainCteSql.endsWith(" group by subax.enrollment"), "Main CTE SQL end check");

    // Verify no Value CTEs were generated
    Set<String> cteKeys = cteContext.getCteKeys();
    assertEquals(
        2,
        cteKeys.size(),
        "Should only contain Main PI and Filter CTE"); // Only main PI and Filter CTE
    assertTrue(
        cteKeys.stream().noneMatch(key -> key.startsWith("varcte_")),
        "Should contain no Value CTEs");
  }

  @Test
  void testAddCteWithComplexFilterOnly() {
    String complexFilter = "d2:daysBetween(V{creation_date}, V{scheduled_date}) > 10";
    programIndicator.setExpression("1");
    programIndicator.setFilter(complexFilter);
    String expectedCreatedCteKey = "varcte_created_" + piUid + "_0";
    String expectedScheduledCteKey = "varcte_scheduleddate_" + piUid + "_0";

    String rawFilterSql =
        "daysBetween(FUNC_CTE_VAR( type='vCreationDate', column='created', piUid='"
            + piUid
            + "', psUid='null', offset='0'), FUNC_CTE_VAR( type='vDueDate', column='scheduleddate', piUid='"
            + piUid
            + "', psUid='null', offset='0')) > 10";
    when(programIndicatorService.getAnalyticsSql(
            eq(complexFilter), eq(BOOLEAN), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn(rawFilterSql);
    when(programIndicatorService.getAnalyticsSql(
            eq("1"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn("1");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Assert
    assertTrue(
        cteContext.containsCte(expectedCreatedCteKey), "Created Date Value CTE should be added");
    assertTrue(
        cteContext.containsCte(expectedScheduledCteKey),
        "Scheduled Date Value CTE should be added");
    CteDefinition createdCte = cteContext.getDefinitionByKey(expectedCreatedCteKey);
    CteDefinition scheduledCte = cteContext.getDefinitionByKey(expectedScheduledCteKey);
    assertNotNull(createdCte, "Created CTE Definition should not be null");
    assertNotNull(scheduledCte, "Scheduled CTE Definition should not be null");
    assertTrue(createdCte.isVariable(), "Created CTE should be Variable CTE");
    assertTrue(scheduledCte.isVariable(), "Scheduled CTE should be Variable CTE");

    // Verify Main PI CTE was added and uses WHERE clause with replaced placeholders
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should be added");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition();
    String createdAlias = createdCte.getAlias();
    String scheduledAlias = scheduledCte.getAlias();

    assertTrue(mainCteSql.startsWith("select subax.enrollment, "), "Main CTE SQL start check");
    assertTrue(mainCteSql.contains("avg(1)"), "Main CTE SQL expression check"); // Simple expression
    assertTrue(
        mainCteSql.contains(" from " + enrollmentTable + " as " + subax),
        "Main CTE SQL FROM clause check");
    assertFalse(
        mainCteSql.contains(" inner join "),
        "Main CTE SQL should not contain INNER JOIN"); // No filter joins
    assertTrue(
        mainCteSql.contains("left join " + createdAlias + " " + createdAlias + " on "),
        "Main CTE SQL LEFT JOIN check (created)");
    assertTrue(
        mainCteSql.contains("left join " + scheduledAlias + " " + scheduledAlias + " on "),
        "Main CTE SQL LEFT JOIN check (scheduled)");
    assertTrue(
        mainCteSql.contains(
            " where daysBetween(" + createdAlias + ".value, " + scheduledAlias + ".value) > 10"),
        "Main CTE SQL WHERE clause check");
    assertTrue(mainCteSql.endsWith(" group by subax.enrollment"), "Main CTE SQL end check");

    // Verify no Filter CTEs were generated
    Set<String> cteKeys = cteContext.getCteKeys();
    assertEquals(
        3, cteKeys.size(), "Should contain Main PI + 2 Value CTEs"); // Main PI + 2 Value CTEs
    assertTrue(
        cteKeys.stream().noneMatch(key -> key.startsWith("filtercte_")),
        "Should contain no Filter CTEs");
  }

  @Test
  void testAddCteWithValueExpressionAndSimpleFilter() {
    programIndicator.setExpression("V{creation_date}");
    programIndicator.setFilter("V{event_status} == 'SKIPPED'");
    String expectedValueCteKey = "varcte_created_" + piUid + "_0";
    String expectedFilterCteKey = "filtercte_eventstatus_eqeq_skipped_" + piUid;

    when(programIndicatorService.getAnalyticsSql(
            eq("V{creation_date}"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn(
            "FUNC_CTE_VAR( type='vCreationDate', column='created', piUid='"
                + piUid
                + "', psUid='null', offset='0')");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Verify both CTE types were added
    assertTrue(cteContext.containsCte(expectedValueCteKey), "Value CTE should be added");
    assertTrue(cteContext.containsCte(expectedFilterCteKey), "Filter CTE should be added");
    CteDefinition valueCte = cteContext.getDefinitionByKey(expectedValueCteKey);
    CteDefinition filterCte = cteContext.getDefinitionByKey(expectedFilterCteKey);
    assertNotNull(valueCte, "Value CTE Definition should not be null");
    assertNotNull(filterCte, "Filter CTE Definition should not be null");
    assertTrue(valueCte.isVariable(), "Value CTE should be Variable CTE");
    assertTrue(filterCte.isFilter(), "Filter CTE should be Filter CTE");

    // Verify Main PI CTE joins correctly
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should be added");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition();
    String valueAlias = valueCte.getAlias();
    String filterAlias = filterCte.getAlias();

    assertTrue(mainCteSql.startsWith("select subax.enrollment, "), "Main CTE SQL start check");
    assertTrue(
        mainCteSql.contains("avg(" + valueAlias + ".value)"),
        "Main CTE SQL expression check"); // Expression uses value alias
    assertTrue(
        mainCteSql.contains(" from " + enrollmentTable + " as " + subax),
        "Main CTE SQL FROM clause check");
    assertTrue(
        mainCteSql.contains("inner join " + expectedFilterCteKey + " " + filterAlias + " on "),
        "Main CTE SQL INNER JOIN check");
    assertTrue(
        mainCteSql.contains("left join " + valueAlias + " " + valueAlias + " on "),
        "Main CTE SQL LEFT JOIN check");
    assertFalse(mainCteSql.contains(" where "), "Main CTE SQL should not contain WHERE");
    assertTrue(mainCteSql.endsWith(" group by subax.enrollment"), "Main CTE SQL end check");
  }

  @Test
  void testAddCteWithMultipleIdenticalValuePlaceholders() {
    programIndicator.setExpression("V{creation_date} + V{creation_date}");
    programIndicator.setFilter(null);
    String expectedValueCteKey = "varcte_created_" + piUid + "_0";

    String placeholder =
        "FUNC_CTE_VAR( type='vCreationDate', column='created', piUid='"
            + piUid
            + "', psUid='null', offset='0')";
    when(programIndicatorService.getAnalyticsSql(
            eq("V{creation_date} + V{creation_date}"),
            eq(NUMERIC),
            eq(programIndicator),
            any(),
            any(),
            eq(subax)))
        .thenReturn(placeholder + " + " + placeholder);

    // method under test
    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Verify ONLY ONE Value CTE was added
    assertTrue(cteContext.containsCte(expectedValueCteKey), "Value CTE should exist");
    assertEquals(2, cteContext.getCteKeys().size(), "Should contain Main PI + 1 Value CTE");
    CteDefinition valueCte = cteContext.getDefinitionByKey(expectedValueCteKey);
    assertNotNull(valueCte, "Value CTE Definition should not be null");
    assertTrue(valueCte.isVariable(), "Value CTE should be Variable CTE");

    // Verify Main PI CTE uses the SAME alias twice and has ONE LEFT JOIN
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should exist");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition();
    String valueAlias = valueCte.getAlias();

    assertTrue(
        mainCteSql.contains("avg(" + valueAlias + ".value + " + valueAlias + ".value)"),
        "Main CTE SQL should use same alias twice");
    assertTrue(
        mainCteSql.contains("left join " + valueAlias + " " + valueAlias + " on "),
        "Main CTE SQL should contain one LEFT JOIN");

    String expectedMainSql =
        String.format(
            "select subax.enrollment, avg(%s.value + %s.value) as value "
                + "from %s as subax "
                + "left join %s %s on %s.enrollment = subax.enrollment and %s.rn = 1 "
                + " "
                + // Placeholder for potential WHERE clause
                "group by subax.enrollment",
            valueAlias,
            valueAlias,
            enrollmentTable,
            valueAlias,
            valueAlias,
            valueAlias,
            valueAlias);
    assertEquals(
        normalizeSql(expectedMainSql),
        normalizeSql(mainCteSql),
        "Main CTE SQL full structure check");
  }

  @Test
  void testAddCteWithMultipleIdenticalSimpleFilters() {
    programIndicator.setExpression("1");
    programIndicator.setFilter("V{event_status} == 'ACTIVE' AND V{event_status} == 'ACTIVE'");
    String expectedFilterCteKey = "filtercte_eventstatus_eqeq_active_" + piUid;

    when(programIndicatorService.getAnalyticsSql(
            eq("1"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn("1");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Assert
    assertTrue(cteContext.containsCte(expectedFilterCteKey), "Filter CTE should exist");
    assertEquals(2, cteContext.getCteKeys().size(), "Should contain Main PI + 1 Filter CTE");
    CteDefinition filterCte = cteContext.getDefinitionByKey(expectedFilterCteKey);
    assertNotNull(filterCte, "Filter CTE Definition should not be null");
    assertTrue(filterCte.isFilter(), "Filter CTE should be marked as filter");

    // Verify Main PI CTE uses ONLY ONE INNER JOIN
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should exist");
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");

    String mainCteSql = mainPiCte.getCteDefinition();
    String filterAlias = filterCte.getAlias();

    assertTrue(
        mainCteSql.contains("inner join " + expectedFilterCteKey + " " + filterAlias + " on "),
        "Main CTE SQL should contain one INNER JOIN");

    String expectedMainSql =
        String.format(
            "select subax.enrollment, avg(1) as value "
                + "from %s as subax "
                + "inner join %s %s on %s.enrollment = subax.enrollment "
                + // The one inner join
                " "
                + // Placeholder for left joins
                " "
                + // Placeholder for where clause
                "group by subax.enrollment",
            enrollmentTable, expectedFilterCteKey, filterAlias, filterAlias);
    assertEquals(
        normalizeSql(expectedMainSql),
        normalizeSql(mainCteSql),
        "Main CTE SQL full structure check");
    assertFalse(mainCteSql.contains(" where "), "Main CTE SQL should not contain WHERE");
    assertFalse(mainCteSql.contains(" left join "), "Main CTE SQL should not contain LEFT JOIN");
  }

  @Test
  void testAddCteWithNoRelevantVariables() {
    programIndicator.setExpression("100");
    // Uses attribute, not V{...}
    programIndicator.setFilter("\"some_attribute\" == 'ABC'");

    when(programIndicatorService.getAnalyticsSql(
            eq("100"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn("100");
    when(programIndicatorService.getAnalyticsSql(
            eq("\"some_attribute\" == 'ABC'"),
            eq(BOOLEAN),
            eq(programIndicator),
            any(),
            any(),
            eq(subax)))
        .thenReturn("\"some_attribute\" = 'ABC'");

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // Verify only the Main PI CTE was added
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should exist");
    Set<String> cteKeys = cteContext.getCteKeys();
    assertEquals(1, cteKeys.size(), "Should contain only Main PI CTE");
    assertTrue(
        cteKeys.stream().noneMatch(key -> key.startsWith("varcte_")),
        "Should contain no Value CTEs");
    assertTrue(
        cteKeys.stream().noneMatch(key -> key.startsWith("filtercte_")),
        "Should contain no Filter CTEs");

    // Verify Main PI CTE SQL is simple
    CteDefinition mainPiCte = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiCte, "Main PI CTE Definition should not be null");
    String mainCteSql = mainPiCte.getCteDefinition();

    assertTrue(mainCteSql.startsWith("select subax.enrollment, "), "Main CTE SQL start check");
    assertTrue(
        mainCteSql.contains("avg(100)"),
        "Main CTE SQL expression check"); // Assuming SUM default agg type
    assertTrue(
        mainCteSql.contains(" from " + enrollmentTable + " as " + subax),
        "Main CTE SQL FROM clause check");
    assertFalse(mainCteSql.contains(" inner join "), "Main CTE SQL should not contain INNER JOIN");
    assertFalse(mainCteSql.contains(" left join "), "Main CTE SQL should not contain LEFT JOIN");
    assertTrue(
        mainCteSql.contains(" where \"some_attribute\" = 'ABC'"),
        "Main CTE SQL WHERE clause check");
    assertTrue(mainCteSql.endsWith(" group by subax.enrollment"), "Main CTE SQL end check");
  }

  @Test
  @DisplayName(
      "addCte should include Start Boundary (AFTER_START + Offset) in Value CTE WHERE clause")
  void testAddCte_ValueCteWithStartBoundary() {
    // Boundary: Event Date, offset by +5 Days relative to AFTER_START_OF_REPORTING_PERIOD
    AnalyticsPeriodBoundary startBoundary = new AnalyticsPeriodBoundary();
    startBoundary.setBoundaryTarget(
        EVENT_DATE); // Target column is occurreddate (from V{event_date})
    // Use a valid boundary type that works with offsets
    startBoundary.setAnalyticsPeriodBoundaryType(AFTER_START_OF_REPORTING_PERIOD); // Use valid type
    startBoundary.setOffsetPeriodType(new DailyPeriodType());
    startBoundary.setOffsetPeriods(5); // 5 days AFTER start date

    // Calculate expected boundary date string
    Calendar cal = Calendar.getInstance();
    cal.setTime(startDate); // Base is start date for AFTER_START...
    cal.add(Calendar.DAY_OF_MONTH, 5); // Apply offset
    String expectedDateStr = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT).format(cal.getTime());
    // Start boundary uses >= and occurreddate (because V{event_date} maps to occurreddate)
    String expectedBoundarySql = "\"occurreddate\" >= '" + expectedDateStr + "'";

    // Configure PI
    programIndicator.setExpression("V{event_date}");
    programIndicator.setFilter(null);
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(startBoundary);
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);

    String expectedValueCteKey = "varcte_occurreddate_" + piUid + "_0";
    String placeholder =
        "FUNC_CTE_VAR( type='vEventDate', column='occurreddate', piUid='"
            + piUid
            + "', psUid='null', offset='0')";

    when(programIndicatorService.getAnalyticsSql(
            eq("V{event_date}"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn(placeholder);

    // Method under test
    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    assertTrue(cteContext.containsCte(expectedValueCteKey), "Value CTE should be added");
    CteDefinition valueCte = cteContext.getDefinitionByKey(expectedValueCteKey);
    assertNotNull(valueCte);
    String cteSql = valueCte.getCteDefinition();
    assertTrue(cteSql.contains("where \"occurreddate\" is not null"), "Should contain null check");
    assertTrue(
        cteSql.contains("and " + expectedBoundarySql),
        "Value CTE SQL should contain start boundary condition: " + expectedBoundarySql);
    assertTrue(
        normalizeSql(cteSql)
            .matches(
                ".* where \"occurreddate\" is not null and "
                    + normalizeSql(expectedBoundarySql)
                    + ".*"),
        "Boundary condition should be in WHERE clause");
  }

  @Test
  @DisplayName(
      "addCte should include End Boundary (BEFORE_END + Offset) in Filter CTE inner WHERE clause")
  void testAddCte_FilterCteWithEndBoundary() {
    // Boundary: Scheduled Date, offset by -3 Days relative to BEFORE_END_OF_REPORTING_PERIOD
    AnalyticsPeriodBoundary endBoundary = new AnalyticsPeriodBoundary();
    // Target column is scheduleddate
    endBoundary.setBoundaryTarget(SCHEDULED_DATE);
    // Use a valid boundary type that works with offsets
    endBoundary.setAnalyticsPeriodBoundaryType(BEFORE_END_OF_REPORTING_PERIOD); // Use valid type
    endBoundary.setOffsetPeriodType(new DailyPeriodType());
    // 3 days BEFORE end date + 1 day adjustment
    endBoundary.setOffsetPeriods(-3);

    // Calculate expected boundary date string
    Calendar cal = Calendar.getInstance();
    cal.setTime(endDate);
    // Adjustment by getBoundaryDate for BEFORE_END... type
    cal.add(Calendar.DAY_OF_MONTH, 1);
    // Apply offset
    cal.add(Calendar.DAY_OF_MONTH, -3);
    String expectedDateStr = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT).format(cal.getTime());
    // End boundary uses '<' and the boundary target column 'scheduleddate'
    String expectedBoundarySql =
        "\"scheduleddate\" < '" + expectedDateStr + "'"; // Expect check on scheduleddate

    // Configure PI
    programIndicator.setExpression("1");
    programIndicator.setFilter("V{event_status} == 'ACTIVE'");
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(endBoundary);
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);

    String expectedFilterCteKey = "filtercte_eventstatus_eqeq_active_" + piUid;

    when(programIndicatorService.getAnalyticsSql(
            eq("1"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn("1");

    // method under test
    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    assertTrue(cteContext.containsCte(expectedFilterCteKey), "Filter CTE should be added");
    CteDefinition filterCte = cteContext.getDefinitionByKey(expectedFilterCteKey);
    assertNotNull(filterCte);

    String cteSql = filterCte.getCteDefinition();
    // Verify the inner select's WHERE clause contains the boundary condition using the correct
    // column
    assertTrue(
        normalizeSql(cteSql)
            .matches(
                ".* from "
                    + eventTable
                    + " where \"eventstatus\" is not null and "
                    + normalizeSql(expectedBoundarySql)
                    + ".*\\) latest where rn = 1.*"),
        "Filter CTE SQL should contain end boundary condition on scheduleddate in inner select: "
            + expectedBoundarySql);
    assertTrue(
        cteSql.contains("\"scheduleddate\" <"),
        "Filter CTE boundary should use scheduleddate column");
  }

  @Test
  @DisplayName("addCte should ignore unsupported Custom Boundary in Value CTE")
  void testAddCte_ValueCteWithUnsupportedBoundary() {
    // Boundary: Custom DE/Attribute based - Unsupported by our helper
    AnalyticsPeriodBoundary startBoundary = new AnalyticsPeriodBoundary();
    startBoundary.setBoundaryTarget("#{someStageUid.someDeUid}");
    // Assign a type that exists but isn't handled by getBoundarySqlCondition's specific checks
    startBoundary.setAnalyticsPeriodBoundaryType(AFTER_START_OF_REPORTING_PERIOD);

    // Configure PI
    programIndicator.setExpression("V{event_date}");
    programIndicator.setFilter(null);
    Set<AnalyticsPeriodBoundary> boundaries = new HashSet<>();
    boundaries.add(startBoundary);
    programIndicator.setAnalyticsPeriodBoundaries(boundaries);

    String expectedValueCteKey = "varcte_occurreddate_" + piUid + "_0";
    String placeholder =
        "FUNC_CTE_VAR( type='vEventDate', column='occurreddate', piUid='"
            + piUid
            + "', psUid='null', offset='0')";

    when(programIndicatorService.getAnalyticsSql(
            eq("V{event_date}"), eq(NUMERIC), eq(programIndicator), any(), any(), eq(subax)))
        .thenReturn(placeholder);

    builder.addCte(
        programIndicator, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    assertTrue(cteContext.containsCte(expectedValueCteKey), "Value CTE should still be added");
    CteDefinition valueCte = cteContext.getDefinitionByKey(expectedValueCteKey);
    assertNotNull(valueCte);

    String cteSql = valueCte.getCteDefinition();
    assertTrue(
        normalizeSql(cteSql)
            .matches(".* from " + eventTable + " where \"occurreddate\" is not null\\s*"),
        "Value CTE SQL should only contain base WHERE conditions, no boundaries added. SQL: "
            + cteSql);
    assertFalse(cteSql.contains(">="), "SQL should not contain '>=' from boundary");
    assertFalse(cteSql.contains("<"), "SQL should not contain '<' from boundary");
  }

  @Test
  void testBuildLeftJoins_HandlesDifferentCteTypes() {
    CteContext localCteContext = new CteContext(EndpointItem.ENROLLMENT);
    String varKey = "varKey1";
    String psdeKey1 = "psdeKey1";
    String psdeKey2 = "psdeKey2";
    String d2FuncKey = "d2FuncKey1";
    String filterKey = "filterKey1";

    CteDefinition variableDef = CteDefinition.forVariable(varKey, "SELECT ...", "enrollment");
    CteDefinition psdeDef1 =
        CteDefinition.forProgramStageDataElement(psdeKey1, "SELECT ...", "enrollment", 1);
    CteDefinition psdeDef2 =
        CteDefinition.forProgramStageDataElement(psdeKey2, "SELECT ...", "enrollment", 3);
    CteDefinition d2FuncDef = CteDefinition.forVariable(d2FuncKey, "SELECT COUNT...", "enrollment");
    CteDefinition filterDef = CteDefinition.forFilter(filterKey, "psUid", "SELECT ...");

    localCteContext.addVariableCte(varKey, variableDef.getCteDefinition(), "enrollment");
    localCteContext.addProgramStageDataElementCte(psdeKey1, psdeDef1);
    localCteContext.addProgramStageDataElementCte(psdeKey2, psdeDef2);
    localCteContext.addVariableCte(d2FuncKey, d2FuncDef.getCteDefinition(), "enrollment");
    localCteContext.addFilterCte(filterKey, filterDef.getCteDefinition());

    variableDef = localCteContext.getDefinitionByKey(varKey);
    psdeDef1 = localCteContext.getDefinitionByKey(psdeKey1);
    psdeDef2 = localCteContext.getDefinitionByKey(psdeKey2);
    d2FuncDef = localCteContext.getDefinitionByKey(d2FuncKey);

    String varAlias = variableDef.getAlias();
    String psdeAlias1 = psdeDef1.getAlias();
    String psdeAlias2 = psdeDef2.getAlias();
    String d2FuncAlias = d2FuncDef.getAlias();

    String expectedVarJoin =
        String.format(
            "left join %s %s on %s.enrollment = subax.enrollment and %s.rn = 1",
            varAlias, varAlias, varAlias, varAlias);
    String expectedPsde1Join =
        String.format(
            "left join %s %s on %s.enrollment = subax.enrollment and %s.rn = 1",
            psdeAlias1, psdeAlias1, psdeAlias1, psdeAlias1);
    String expectedPsde2Join =
        String.format(
            "left join %s %s on %s.enrollment = subax.enrollment and %s.rn = 3",
            psdeAlias2, psdeAlias2, psdeAlias2, psdeAlias2);
    String expectedD2FuncJoin =
        String.format(
            "left join %s %s on %s.enrollment = subax.enrollment",
            d2FuncAlias, d2FuncAlias, d2FuncAlias);

    String result = builder.buildLeftJoinsForAllValueCtes(localCteContext);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains(expectedVarJoin), "Should contain join for Variable CTE (rn=1)");
    assertTrue(result.contains(expectedPsde1Join), "Should contain join for PSDE CTE (rn=1)");
    assertTrue(result.contains(expectedPsde2Join), "Should contain join for PSDE CTE (rn=3)");
    assertTrue(
        result.contains(expectedD2FuncJoin), "Should contain join for D2 Function CTE (no rn)");
    assertFalse(result.contains(filterKey), "Should NOT contain join for Filter CTE (by key)");

    assertEquals(
        4,
        countOccurrences(result, "left join"),
        "Should generate the correct number of LEFT JOIN clauses");
  }

  @Test
  void addCte_shouldProcessPlaceholdersAndGenerateCorrectSqlWithJoins() {
    String expression =
        "100 + V{event_date} + #{PgmStgUid1.DataElmUid1} + d2:countIfValue(#{PgmStgUid1.DataElmUid2}, 5)";
    String filter = "V{creation_date} > '2024-01-01'";
    testPI.setExpression(expression);
    testPI.setFilter(filter);
    testPI.setAggregationType(AggregationType.SUM); // Example aggregation

    // Define expected placeholders (as returned by mocked service)
    String varPlaceholderEventDt =
        "FUNC_CTE_VAR( type='vEventDate', column='occurreddate', piUid='"
            + piUid
            + "', psUid='null', offset='0')";
    String psdePlaceholder1 =
        "__PSDE_CTE_PLACEHOLDER__(psUid='PgmStgUid1', deUid='DataElmUid1', offset='0', boundaryHash='noboundaries', piUid='"
            + piUid
            + "')";
    String d2FuncValueSql =
        "cast(5 as double precision)"; // Expected SQL for '5' with postgres builder
    String d2FuncValueSqlEncoded =
        Base64.getEncoder().encodeToString(d2FuncValueSql.getBytes(StandardCharsets.UTF_8));
    String expectedArgType = "val64";
    String richD2FuncPlaceholder1 =
        String.format(
            "__D2FUNC__(func='%s', ps='%s', de='%s', argType='%s', arg64='%s', hash='%s', pi='%s')__",
            "countIfValue",
            psUid1,
            deUid2,
            expectedArgType,
            d2FuncValueSqlEncoded,
            "noboundaries",
            piUid);

    // Define expected RAW SQL returned by the service
    String rawExpressionSql =
        "100 + "
            + varPlaceholderEventDt
            + " + "
            + psdePlaceholder1
            + " + "
            + richD2FuncPlaceholder1;

    // Mock ProgramIndicatorService.getAnalyticsSql to return raw SQL
    // No need to simulate context population here, as the builder calls the utils
    when(programIndicatorService.getAnalyticsSql(
            eq(expression), eq(NUMERIC), eq(testPI), eq(startDate), eq(endDate), any()))
        .thenReturn(rawExpressionSql);

    // 4. Define expected CTE keys (derived from placeholders)
    String varCteKeyED = "varcte_occurreddate_" + piUid + "_0";
    String psdeCteKey1 = "psdecte_PgmStgUid1_DataElmUid1_0_noboundaries_" + piUid;
    String d2FuncValueHash = generateSha1Hash(d2FuncValueSql);
    String d2FuncCteKey1 =
        "d2countifvalue_PgmStgUid1_DataElmUid2_" + d2FuncValueHash + "_noboundaries_" + piUid;
    String filterVarCteKeyCD = "varcte_occurreddate_" + piUid + "_0";

    // method under test
    builder.addCte(testPI, null, AnalyticsType.ENROLLMENT, startDate, endDate, cteContext);

    // 1. Verify main PI CTE exists
    assertTrue(cteContext.containsCte(piUid), "Main PI CTE should be added");
    CteDefinition mainPiDef = cteContext.getDefinitionByKey(piUid);
    assertNotNull(mainPiDef);
    String mainPiSql = mainPiDef.getCteDefinition();

    // 2. Verify context contains expected value/function CTEs generated by utils
    assertTrue(cteContext.containsCte(varCteKeyED), "Context should contain event_date CTE");
    assertTrue(cteContext.containsCte(psdeCteKey1), "Context should contain PSDE CTE");
    assertTrue(cteContext.containsCte(d2FuncCteKey1), "Context should contain D2Func CTE");
    assertTrue(
        cteContext.containsCte(filterVarCteKeyCD),
        "Context should contain creation_date filter CTE");

    // 3. Retrieve Aliases (needed for checking replacement and joins)
    String varAliasED = cteContext.getDefinitionByKey(varCteKeyED).getAlias();
    String psdeAlias1 = cteContext.getDefinitionByKey(psdeCteKey1).getAlias();
    String d2FuncAlias1 = cteContext.getDefinitionByKey(d2FuncCteKey1).getAlias();
    String filterVarAliasCD = cteContext.getDefinitionByKey(filterVarCteKeyCD).getAlias();

    // 4. Verify placeholder replacement in main PI SQL expression part
    String expectedProcessedExpression =
        "100 + "
            + varAliasED
            + ".value + coalesce("
            + psdeAlias1
            + ".value, 0) + coalesce("
            + d2FuncAlias1
            + ".value, 0)";
    assertTrue(
        mainPiSql.contains("sum(" + expectedProcessedExpression + ")"), // Check inside SUM()
        "Main PI SQL should contain correctly replaced expression part inside aggregation");
    // Ensure original placeholders are gone
    assertFalse(mainPiSql.contains(varPlaceholderEventDt));
    assertFalse(mainPiSql.contains(psdePlaceholder1));
    assertFalse(mainPiSql.contains(richD2FuncPlaceholder1));

    // Verify placeholder replacement in main PI SQL filter part (WHERE clause)
    // Assuming V{creation_date} > literal results in a WHERE clause condition
    // after processing by processPlaceholdersAndGenerateVariableCtes
    String filterCteKey =
        "filtercte_created_gt_2024_01_01_" + piUid; // Key generated by analyzeFilter...
    assertTrue(cteContext.containsCte(filterCteKey), "Context should contain the Filter CTE key");
    CteDefinition filterCteDef = cteContext.getDefinitionByKey(filterCteKey);
    assertNotNull(filterCteDef);
    String filterCteAlias = filterCteDef.getAlias(); // Get the actual alias

    // Construct the expected INNER JOIN clause fragment
    String expectedInnerJoin =
        String.format(
            "inner join %s %s on %s.enrollment = subax.enrollment",
            filterCteKey, filterCteAlias, filterCteAlias);

    assertTrue(
        mainPiSql.contains(expectedInnerJoin),
        "Main PI SQL should contain INNER JOIN for the simple filter CTE");
    // Verify the original filter condition is NOT in a WHERE clause
    assertFalse(
        mainPiSql.contains("where coalesce(" + filterVarAliasCD + ".value"),
        "Main PI SQL should NOT contain the filter condition in the WHERE clause");

    // 6. Verify Joins in main PI SQL (using buildLeftJoinsForAllValueCtes)
    String expectedVarJoin = String.format("left join %s %s on", varAliasED, varAliasED);
    String expectedPsdeJoin = String.format("left join %s %s on", psdeAlias1, psdeAlias1);
    String expectedD2FuncJoin = String.format("left join %s %s on", d2FuncAlias1, d2FuncAlias1);
    String expectedFilterVarJoin =
        String.format("left join %s %s on", filterVarAliasCD, filterVarAliasCD);

    assertTrue(
        mainPiSql.contains(expectedVarJoin), "Main PI SQL should contain join for V{event_date}");
    assertTrue(mainPiSql.contains(expectedPsdeJoin), "Main PI SQL should contain join for #{...}");
    assertTrue(
        mainPiSql.contains(expectedD2FuncJoin),
        "Main PI SQL should contain join for d2:countIfValue");
    assertTrue(
        mainPiSql.contains(expectedFilterVarJoin),
        "Main PI SQL should contain join for filter V{creation_date}");

    // Check rn conditions specifically
    assertTrue(mainPiSql.contains(varAliasED + ".rn = 1"), "V{event_date} join should use rn = 1");
    assertTrue(
        mainPiSql.contains(psdeAlias1 + ".rn = 1"), "#{...} (offset 0) join should use rn = 1");
    assertFalse(mainPiSql.contains(d2FuncAlias1 + ".rn ="), "d2:func join should not use rn");
    assertTrue(
        mainPiSql.contains(filterVarAliasCD + ".rn = 1"), "Filter V{...} join should use rn = 1");
  }

  private int countOccurrences(String haystack, String needle) {
    if (haystack == null || needle == null || haystack.isEmpty() || needle.isEmpty()) {
      return 0;
    }
    int count = 0;
    int lastIndex = 0;
    while ((lastIndex = haystack.indexOf(needle, lastIndex)) != -1) {
      count++;
      lastIndex += needle.length();
    }
    return count;
  }

  private String generateSha1Hash(String input) {
    if (input == null) {
      return "null_sql"; // Or handle as appropriate
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      return String.format("%040x", no);
    } catch (NoSuchAlgorithmException e) {
      return "hash_error_" + input.hashCode();
    }
  }

  private String normalizeSql(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }
}
