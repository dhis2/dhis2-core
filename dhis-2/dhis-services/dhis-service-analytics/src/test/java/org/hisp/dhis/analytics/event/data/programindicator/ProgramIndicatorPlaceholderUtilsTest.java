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

import static org.hisp.dhis.analytics.common.CteDefinition.CteType.D2_FUNCTION;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.PROGRAM_STAGE_DATE_ELEMENT;
import static org.hisp.dhis.analytics.common.CteDefinition.CteType.VARIABLE;
import static org.hisp.dhis.program.AnalyticsPeriodBoundary.EVENT_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.AnalyticsQueryType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsPeriodBoundaryType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProgramIndicatorPlaceholderUtilsTest extends TestBase {

  private ProgramIndicator programIndicator;

  private CteContext cteContext;
  private Map<String, String> variableAliasMap;
  private SqlBuilder sqlBuilder;
  private Map<String, String> psdeAliasMap;
  private Map<String, String> d2FunctionAliasMap;

  private final String piUid = "programInd1";
  private final String progUid = "programUid1";
  private final String eventTable = "analytics_event_" + progUid;
  private final Date startDate = new Date();
  private final Date endDate = new Date();
  private final String psUid = "PgmStgUid1";
  private final String deUid = "DataElmUid1";
  private final SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);
  private DataElementService dataElementService;

  @BeforeEach
  void setUp() {
    Program program = new Program();
    program.setUid(progUid);

    programIndicator = new ProgramIndicator();
    programIndicator.setUid(piUid);
    programIndicator.setProgram(program);

    cteContext = new CteContext(AnalyticsQueryType.ENROLLMENT);
    variableAliasMap = new HashMap<>();

    sqlBuilder = new PostgreSqlBuilder();
    psdeAliasMap = new HashMap<>();
    d2FunctionAliasMap = new HashMap<>();
    dataElementService = mock(DataElementService.class);
    DataElement dataElement = createDataElement('A');
    when(dataElementService.getDataElement(deUid)).thenReturn(dataElement);
  }

  @Nested
  class NoPlaceholderTests {
    @Test
    void testProcessPlaceholders_withNullSql() {
      assertNull(
          processPlaceholdersAndGenerateVariableCtes(
              null,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder));
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(variableAliasMap.isEmpty());
    }

    @Test
    void testProcessPlaceholders_withEmptySql() {
      String result =
          processPlaceholdersAndGenerateVariableCtes(
              "", programIndicator, startDate, endDate, cteContext, variableAliasMap, sqlBuilder);
      assertEquals("", result);
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(variableAliasMap.isEmpty());
    }

    @Test
    void testProcessPlaceholders_withNoPlaceholders() {
      String rawSql = "1 + d2:daysBetween(enrollmentDate, incidentDate)";
      String result =
          processPlaceholdersAndGenerateVariableCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder);
      assertEquals(rawSql, result);
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(variableAliasMap.isEmpty());
    }
  }

  /**
   * Tests for placeholder emitted by these Program variables: - vEventStatus - vCreationDate -
   * vDueDate - vEventDate - vScheduledDate
   */
  @Nested
  class FunctionCteTests {

    @Test
    void testProcessPlaceholders_withSinglePlaceholder() {
      String placeholder = buildVariablePlaceholder("vCreationDate", "created", piUid, null, "0");
      String rawSql = "someFunction(" + placeholder + ")";
      String expectedCteKey = "varcte_created_" + piUid + "_0";

      String result =
          processPlaceholdersAndGenerateVariableCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder);

      String expectedCteSql =
          String.format(
              "select enrollment, \"created\" as value, "
                  + "row_number() over (partition by enrollment order by occurreddate desc) as rn "
                  + "from %s "
                  + "where \"created\" is not null ",
              eventTable);
      assertCteSqlMatches(expectedCteKey, expectedCteSql);
      String alias = assertPlaceholderMapped(placeholder, expectedCteKey, variableAliasMap);
      // Verify result SQL
      assertEquals("someFunction(" + alias + ".value)", result);
    }

    @Test
    void testProcessPlaceholders_withMultipleDifferentPlaceholders() {
      String placeholder1 = buildVariablePlaceholder("vCreationDate", "created", piUid, null, "0");
      String placeholder2 =
          buildVariablePlaceholder("vEventDate", "occurreddate", piUid, null, "0");
      String rawSql = "daysBetween(" + placeholder1 + ", " + placeholder2 + ")";
      String expectedCteKey1 = "varcte_created_" + piUid + "_0";
      String expectedCteKey2 = "varcte_occurreddate_" + piUid + "_0";

      String result =
          processPlaceholdersAndGenerateVariableCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder);

      assertCteCreated(expectedCteKey1, VARIABLE, "enrollment");
      assertCteCreated(expectedCteKey2, VARIABLE, "enrollment");
      assertEquals(2, cteContext.getCteKeys().size());
      String alias1 = assertPlaceholderMapped(placeholder1, expectedCteKey1, variableAliasMap);
      String alias2 = assertPlaceholderMapped(placeholder2, expectedCteKey2, variableAliasMap);

      // Verify result SQL
      assertEquals("daysBetween(" + alias1 + ".value, " + alias2 + ".value)", result);
    }

    @Test
    void testProcessPlaceholders_withMultipleIdenticalPlaceholders() {
      String placeholder = buildVariablePlaceholder("vCreationDate", "created", piUid, null, "0");
      String rawSql = placeholder + " + " + placeholder;
      String expectedCteKey = "varcte_created_" + piUid + "_0";

      String result =
          processPlaceholdersAndGenerateVariableCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder);

      // Verify only one CTE added
      assertEquals(1, cteContext.getCteKeys().size());
      assertCteCreated(expectedCteKey, VARIABLE, "enrollment");
      String alias = assertPlaceholderMapped(placeholder, expectedCteKey, variableAliasMap);

      // Verify result SQL uses same alias twice
      assertEquals(alias + ".value + " + alias + ".value", result);
    }

    @Test
    void testProcessPlaceholders_withProgramStageUid() {
      String psUid = "stageUid123";
      String placeholder = buildVariablePlaceholder("vCreationDate", "created", piUid, psUid, "0");
      String expectedCteKey =
          "varcte_created_" + piUid + "_0"; // Offset is part of key, PS UID isn't currently

      processPlaceholdersAndGenerateVariableCtes(
          placeholder,
          programIndicator,
          startDate,
          endDate,
          cteContext,
          variableAliasMap,
          sqlBuilder);

      CteDefinition cteDef = assertCteCreated(expectedCteKey, VARIABLE, "enrollment");
      assertTrue(cteDef.getCteDefinition().contains("and ps = '" + psUid + "'"));
      assertPlaceholderMapped(placeholder, expectedCteKey, variableAliasMap);
    }

    @Test
    void testProcessPlaceholders_withDifferentOffsets() {
      String placeholder1 =
          buildVariablePlaceholder("vEventDate", "occurreddate", piUid, null, "0");
      String placeholder2 =
          buildVariablePlaceholder("vEventDate", "occurreddate", piUid, null, "-1");
      String rawSql = placeholder1 + " - " + placeholder2;
      String expectedCteKey1 = "varcte_occurreddate_" + piUid + "_0";
      String expectedCteKey2 = "varcte_occurreddate_" + piUid + "_-1"; // Key includes offset

      String result =
          processPlaceholdersAndGenerateVariableCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              variableAliasMap,
              sqlBuilder);

      CteDefinition cteDef1 = assertCteCreated(expectedCteKey1, VARIABLE, "enrollment");
      CteDefinition cteDef2 = assertCteCreated(expectedCteKey2, VARIABLE, "enrollment");
      assertPlaceholderMapped(placeholder1, expectedCteKey1, variableAliasMap);
      assertPlaceholderMapped(placeholder2, expectedCteKey2, variableAliasMap);
      String alias1 = cteDef1.getAlias();
      String alias2 = cteDef2.getAlias();
      assertNotEquals(alias1, alias2);
      assertEquals(2, variableAliasMap.size());

      // Verify result SQL
      assertEquals(alias1 + ".value - " + alias2 + ".value", result);
      // Verify the CTE SQL for offset -1 doesn't contain the word "offset" (it's handled by rn=1
      // join)
      // Note: The actual CTE body generation doesn't use the offset value directly,
      // the join condition rn=1 handles latest. If older values were needed, CTE gen would change.
      String expectedCteSql =
          String.format(
              "select enrollment, \"occurreddate\" as value, "
                  + "row_number() over (partition by enrollment order by occurreddate desc) as rn "
                  + "from %s "
                  + "where \"occurreddate\" is not null ",
              eventTable);
      assertCteSqlMatches(expectedCteKey1, expectedCteSql);
      assertCteSqlMatches(expectedCteKey2, expectedCteSql);
    }
  }

  /**
   * Tests for placeholder emitted by this Program expression: #{uid1.uid2} where uid1 is a
   * ProgramStage UID and uid2 is a DataElement UID. - ProgramItemStageElement
   */
  @Nested
  class PsDeCteTests {
    @Test
    void testProcessPsDePlaceholders_withNullSql() {
      assertNull(
          processPsDePlaceholdersAndGenerateCtes(
              null, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder));
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(psdeAliasMap.isEmpty());
    }

    @Test
    void testProcessPsDePlaceholders_withEmptySql() {
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              "", programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);
      assertEquals("", result);
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(psdeAliasMap.isEmpty());
    }

    @Test
    void testProcessPsDePlaceholders_withNoPlaceholders() {
      String rawSql = "1 + d2:daysBetween(V{enrollment_date}, V{incident_date})"; // Example only
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);
      assertEquals(rawSql, result);
      assertTrue(cteContext.getCteKeys().isEmpty());
      assertTrue(psdeAliasMap.isEmpty());
    }

    @Test
    void testProcessPsDePlaceholders_withSinglePlaceholder_offset0_noBoundaries() {
      int offset = 0;
      String boundaryHash = "noboundaries";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String rawSql = "someFunction(" + placeholderString + ")";
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
      int expectedTargetRank = 1; // offset 0 -> rank 1
      String expectedOrderBy = "\"occurreddate\" desc"; // Default order

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      CteDefinition cteDef =
          assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      assertFalse(cteDef.isVariable());
      assertEquals(expectedTargetRank, cteDef.getTargetRank());
      assertCteSqlMatches(
          expectedCteKey, getExpectedPsdeSql(deUid, expectedOrderBy, eventTable, psUid));
      String alias = assertPlaceholderMapped(placeholderString, expectedCteKey, psdeAliasMap);
      // Verify result SQL
      assertEquals("someFunction(coalesce(" + alias + ".value, 0))", result);
    }

    @Test
    void testProcessPsDePlaceholders_withSinglePlaceholder_negativeOffset_noBoundaries() {
      int offset = -1; // Second latest
      String boundaryHash = "noboundaries";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
      int expectedTargetRank = 2; // offset -1 -> rank 2
      String expectedOrderBy = "\"occurreddate\" desc"; // Negative offset -> desc

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              placeholderString,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              psdeAliasMap,
              sqlBuilder);

      CteDefinition cteDef =
          assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      assertEquals(expectedTargetRank, cteDef.getTargetRank());

      assertCteSqlMatches(
          expectedCteKey, getExpectedPsdeSql(deUid, expectedOrderBy, eventTable, psUid));
      String alias = assertPlaceholderMapped(placeholderString, expectedCteKey, psdeAliasMap);
      assertEquals("coalesce(" + alias + ".value, 0)", result);
    }

    @Test
    void testProcessPsDePlaceholders_withSinglePlaceholder_positiveOffset_noBoundaries() {
      int offset = 2; // Second earliest
      String boundaryHash = "noboundaries";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
      int expectedTargetRank = 2; // offset 2 -> rank 2
      String expectedOrderBy = "\"occurreddate\" asc"; // Positive offset -> asc

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              placeholderString,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              psdeAliasMap,
              sqlBuilder);

      CteDefinition cteDef =
          assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      assertEquals(expectedTargetRank, cteDef.getTargetRank());
      assertCteSqlMatches(
          expectedCteKey, getExpectedPsdeSql(deUid, expectedOrderBy, eventTable, psUid));
      // Verify alias map and result
      String alias = assertPlaceholderMapped(placeholderString, expectedCteKey, psdeAliasMap);
      assertEquals("coalesce(" + alias + ".value, 0)", result);
    }

    @Test
    void testProcessPsDePlaceholders_withMultipleDifferentPlaceholders() {
      int offset1 = 0;
      int offset2 = -1;
      String deUid2 = "DataElmUid2";
      String boundaryHash = "noboundaries";
      String placeholder1 = buildPsDePlaceholder(psUid, deUid, offset1, boundaryHash, piUid);
      String placeholder2 = buildPsDePlaceholder(psUid, deUid2, offset2, boundaryHash, piUid);
      String rawSql = placeholder1 + " + " + placeholder2;

      String expectedCteKey1 =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset1, boundaryHash, piUid);
      String expectedCteKey2 =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid2, offset2, boundaryHash, piUid);

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      assertCteCreated(expectedCteKey1, PROGRAM_STAGE_DATE_ELEMENT, "enrollment", 1);
      assertCteCreated(expectedCteKey2, PROGRAM_STAGE_DATE_ELEMENT, "enrollment", 2);
      assertEquals(2, cteContext.getCteKeys().size());

      // Verify alias map
      String alias1 = assertPlaceholderMapped(placeholder1, expectedCteKey1, psdeAliasMap);
      String alias2 = assertPlaceholderMapped(placeholder2, expectedCteKey2, psdeAliasMap);
      assertEquals(2, psdeAliasMap.size());

      // Verify result SQL
      assertEquals("coalesce(" + alias1 + ".value, 0) + coalesce(" + alias2 + ".value, 0)", result);
    }

    @Test
    void testProcessPsDePlaceholders_withMultipleIdenticalPlaceholders() {
      int offset = 0;
      String boundaryHash = "noboundaries";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String rawSql = placeholderString + " + " + placeholderString; // Identical placeholder twice
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      assertEquals(1, cteContext.getCteKeys().size());

      String alias = assertPlaceholderMapped(placeholderString, expectedCteKey, psdeAliasMap);
      assertEquals(1, psdeAliasMap.size()); // Only one distinct placeholder string

      // Verify result SQL uses same alias twice
      assertEquals("coalesce(" + alias + ".value, 0) + coalesce(" + alias + ".value, 0)", result);
    }

    @Test
    void testProcessPsDePlaceholders_withBoundaries() throws ParseException {
      int offset = 0;
      String boundaryHash = "a3b8c5d3e9f2a1b7c6d0e8f4a3b1c7d5e0f6a2b8";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);

      // Setup boundaries on the ProgramIndicator mock
      AnalyticsPeriodBoundary boundary1 =
          createEventDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD);

      AnalyticsPeriodBoundary boundary2 =
          createScheduledDateBoundary(AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD);
      Set<AnalyticsPeriodBoundary> boundaries = Set.of(boundary1, boundary2);
      programIndicator.setAnalyticsPeriodBoundaries(boundaries); // Set boundaries on the mock

      Date boundaryDate1 = dateFormat.parse("2024-02-15");
      Date boundaryDate2 = dateFormat.parse("2024-11-30");

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              placeholderString,
              programIndicator,
              boundaryDate1,
              boundaryDate2,
              cteContext,
              psdeAliasMap,
              sqlBuilder);

      CteDefinition cteDef =
          assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");

      // Verify CTE SQL includes boundary conditions (order might vary based on Set iteration)
      String cteSql = cteDef.getCteDefinition();
      assertTrue(cteSql.contains("and \"occurreddate\" >= '2024-02-15'"));
      assertTrue(cteSql.contains("and \"scheduleddate\" < '2024-12-01'")); // set to plusDays(1) by
      // AnalyticsPeriodBoundary
      assertTrue(cteSql.contains("ps = '" + psUid + "'")); // Ensure PS condition is still there
      assertTrue(
          cteSql.contains(
              "row_number() over (partition by enrollment order by \"occurreddate\" desc) as rn")); // Check order by

      // Verify alias map and result
      String alias = assertPlaceholderMapped(placeholderString, expectedCteKey, psdeAliasMap);
      assertEquals(1, psdeAliasMap.size());
      assertEquals("coalesce(" + alias + ".value, 0)", result);
    }

    private AnalyticsPeriodBoundary createEventDateBoundary(
        AnalyticsPeriodBoundaryType boundaryType) {
      AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
      boundary.setUid("event-boundary");
      boundary.setAnalyticsPeriodBoundaryType(boundaryType);
      boundary.setBoundaryTarget(EVENT_DATE);
      return boundary;
    }

    private AnalyticsPeriodBoundary createScheduledDateBoundary(
        AnalyticsPeriodBoundaryType boundaryType) {
      AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
      boundary.setUid("scheduled-boundary");
      boundary.setAnalyticsPeriodBoundaryType(boundaryType);
      boundary.setBoundaryTarget(AnalyticsPeriodBoundary.SCHEDULED_DATE);
      return boundary;
    }

    @Test
    void testProcessPsDePlaceholders_withExistingCteInContext() {
      int offset = 0;
      String boundaryHash = "noboundaries"; // Matches default empty boundaries mock
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);

      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
      int expectedTargetRank = 1;

      // Create the pre-existing definition using the factory method
      String preExistingSql = "select enrollment, 'dummy' as value, 1 as rn from dummy_table";
      CteDefinition preExistingDef =
          CteDefinition.forProgramStageDataElement(
              expectedCteKey, preExistingSql, "enrollment", expectedTargetRank);
      assertNotNull(preExistingDef, "Pre-existing definition should be created");
      String preExistingAlias = preExistingDef.getAlias(); // Capture the generated alias
      assertNotNull(preExistingAlias, "Pre-existing definition should have an alias");

      // Pre-populate the *real* CteContext
      //    Use the CteContext method that accepts the definition object (assuming it was updated)
      cteContext.addProgramStageDataElementCte(expectedCteKey, preExistingDef);

      // Verify pre-population state
      assertEquals(
          1, cteContext.getCteKeys().size(), "Context should have 1 key before processing");
      assertTrue(
          cteContext.containsCte(expectedCteKey), "Context should contain the pre-existing key");

      // method under test
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              placeholderString, // Pass the raw SQL containing the placeholder
              programIndicator,
              startDate,
              endDate,
              cteContext, // Pass the real, pre-populated context
              psdeAliasMap, // Pass the real map
              sqlBuilder);

      // Verify NO new CTE was added
      assertEquals(
          1,
          cteContext.getCteKeys().size(),
          "Context should still have only 1 key after processing");

      // Retrieve the definition *from the context* and verify it's the original one
      CteDefinition cteDefRetrieved = cteContext.getDefinitionByKey(expectedCteKey);
      assertNotNull(cteDefRetrieved, "Definition should still exist in context");
      assertEquals(
          preExistingSql,
          cteDefRetrieved.getCteDefinition(),
          "CTE SQL definition should not be overwritten");
      assertEquals(
          preExistingAlias, cteDefRetrieved.getAlias(), "CTE alias should be the original one");
      assertEquals(
          expectedTargetRank,
          cteDefRetrieved.getTargetRank(),
          "CTE target rank should be the original one");
      assertTrue(cteDefRetrieved.isPsDe(), "Definition should be marked as PsDeCte");

      // Verify alias map was populated correctly with the existing alias
      assertEquals(1, psdeAliasMap.size(), "Alias map should contain one entry");
      assertEquals(
          preExistingAlias,
          psdeAliasMap.get(placeholderString),
          "Alias map should map placeholder to the existing alias");

      // Verify result SQL uses the existing alias
      assertEquals(
          "coalesce(" + preExistingAlias + ".value, 0)",
          result,
          "Result SQL should use the existing alias");
    }

    @Test
    void testProcessPsDePlaceholders_withMalformedPlaceholder_shouldBeIgnored() {
      // Malformed: Missing closing parenthesis - *SHOULD NOT MATCH* the defined pattern
      String malformedPlaceholder1 =
          "__PSDE_CTE_PLACEHOLDER__(psUid='PgmStgUid1', deUid='DataElmUid1', offset='0', boundaryHash='noboundaries', piUid='programInd1'";
      // Malformed: Incorrect keyword casing - *MIGHT NOT MATCH* if regex implies case sensitivity
      // on
      // keyword (it doesn't here, but good to test)
      String malformedPlaceholder2 =
          "__psde_cte_placeholder__(psUid='PgmStgUid1', deUid='DataElmUid1', offset='0', boundaryHash='noboundaries', piUid='programInd1')";
      // Malformed: Missing required key (e.g., deUid) - *SHOULD NOT MATCH* the defined pattern
      String malformedPlaceholder3 =
          "__PSDE_CTE_PLACEHOLDER__(psUid='PgmStgUid1', offset='0', boundaryHash='noboundaries', piUid='programInd1')";
      // Malformed: Extra comma
      String malformedPlaceholder4 =
          "__PSDE_CTE_PLACEHOLDER__(psUid='PgmStgUid1',, deUid='DataElmUid1', offset='0', boundaryHash='noboundaries', piUid='programInd1')";

      String rawSql1 = "someFunction(" + malformedPlaceholder1;
      String rawSql2 = "someFunction(" + malformedPlaceholder2 + ")";
      String rawSql3 = "someFunction(" + malformedPlaceholder3 + ")";
      String rawSql4 = "someFunction(" + malformedPlaceholder4 + ")";

      String result1 =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql1, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);
      String result2 =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql2, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);
      String result3 =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql3, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);
      String result4 =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql4, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      assertEquals(rawSql1, result1, "Malformed placeholder 1 (missing ')') should be ignored");
      assertEquals(rawSql2, result2, "Malformed placeholder 2 (keyword case) should be ignored");
      assertEquals(rawSql3, result3, "Malformed placeholder 3 (missing key) should be ignored");
      assertEquals(rawSql4, result4, "Malformed placeholder 4 (extra comma) should be ignored");

      // Context and map should remain empty as no valid placeholders were processed
      assertTrue(
          cteContext.getCteKeys().isEmpty(),
          "CTE context should be empty for malformed placeholders");
      assertTrue(psdeAliasMap.isEmpty(), "Alias map should be empty for malformed placeholders");
    }

    @Test
    void testProcessPsDePlaceholders_withDifferentUidCases_shouldGenerateSeparateCtes() {
      // Arrange
      int offset = 0;
      String boundaryHash = "noboundaries";
      String psUidUpper = "PgmStgUid1";
      String psUidLower = "pgmstguid1"; // Same UID, different case
      String deUidCase1 = "DataElmUid1";
      String deUidCase2 = "dataelmuid1"; // Same UID, different case

      // Placeholder 1: Upper case PS, Mixed case DE
      String placeholderString1 =
          buildPsDePlaceholder(psUidUpper, deUidCase1, offset, boundaryHash, piUid);
      // Placeholder 2: Lower case PS, Lower case DE
      String placeholderString2 =
          buildPsDePlaceholder(psUidLower, deUidCase2, offset, boundaryHash, piUid);

      String rawSql = placeholderString1 + " + " + placeholderString2;

      // Expected keys should differ due to different casing in UIDs within the key format
      String expectedCteKey1 =
          String.format(
              "psdecte_%s_%s_%d_%s_%s", psUidUpper, deUidCase1, offset, boundaryHash, piUid);
      String expectedCteKey2 =
          String.format(
              "psdecte_%s_%s_%d_%s_%s", psUidLower, deUidCase2, offset, boundaryHash, piUid);

      // Assume ProgramIndicator mock is set up with no boundaries

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      assertEquals(
          2, cteContext.getCteKeys().size(), "Should generate 2 CTEs for different UID casing");
      CteDefinition cteDef1 =
          assertCteCreated(expectedCteKey1, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      CteDefinition cteDef2 =
          assertCteCreated(expectedCteKey2, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");

      // Verify the CTE SQL uses the correct casing from the placeholder
      assertTrue(
          cteDef1.getCteDefinition().contains("ps = '" + psUidUpper + "'"),
          "CTE 1 SQL should use upper case PS UID");
      assertTrue(
          cteDef1.getCteDefinition().contains(sqlBuilder.quote(deUidCase1)),
          "CTE 1 SQL should use mixed case DE UID");

      assertTrue(
          cteDef2.getCteDefinition().contains("ps = '" + psUidLower + "'"),
          "CTE 2 SQL should use lower case PS UID");
      assertTrue(
          cteDef2.getCteDefinition().contains(sqlBuilder.quote(deUidCase2)),
          "CTE 2 SQL should use lower case DE UID");

      String alias1 = assertPlaceholderMapped(placeholderString1, expectedCteKey1, psdeAliasMap);
      String alias2 = assertPlaceholderMapped(placeholderString2, expectedCteKey2, psdeAliasMap);
      assertEquals(2, psdeAliasMap.size());

      // Verify result SQL uses the respective aliases
      assertEquals("coalesce(" + alias1 + ".value, 0) + coalesce(" + alias2 + ".value, 0)", result);
    }

    @Test
    void testProcessPsDePlaceholders_withPrePopulatedAliasMap_shouldOverwriteWithRealAlias() {
      int offset = 0;
      String boundaryHash = "noboundaries";
      String placeholderString = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String rawSql = "calculate(" + placeholderString + ")";
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);
      int expectedTargetRank = 1;

      // 1. Pre-populate CteContext with a definition for the key
      String realCteSql = "select enrollment, 'real_val' as value, 1 as rn from real_table";
      CteDefinition realCteDef =
          CteDefinition.forProgramStageDataElement(
              expectedCteKey, realCteSql, "enrollment", expectedTargetRank);
      String realAlias = realCteDef.getAlias(); // Capture the *real* generated alias
      cteContext.addProgramStageDataElementCte(expectedCteKey, realCteDef); // Add the real def

      // Pre-populate psdeAliasMap with a *different*, dummy alias for the same placeholder
      String dummyAlias = "pre_existing_alias";
      psdeAliasMap.put(placeholderString, dummyAlias);

      // Verify initial state
      assertEquals(1, cteContext.getCteKeys().size());
      assertEquals(1, psdeAliasMap.size());
      assertEquals(dummyAlias, psdeAliasMap.get(placeholderString));

      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      // Verify no new CTE was added
      assertEquals(1, cteContext.getCteKeys().size(), "Context should still have only 1 key");
      CteDefinition cteDefRetrieved = cteContext.getDefinitionByKey(expectedCteKey);
      assertNotNull(cteDefRetrieved);
      assertEquals(
          realAlias,
          cteDefRetrieved.getAlias(),
          "Context should contain the real alias"); // Check context alias didn't change

      // Verify alias map was *overwritten* with the real alias from the context's definition
      assertEquals(1, psdeAliasMap.size(), "Alias map should still contain one entry");
      assertEquals(
          realAlias,
          psdeAliasMap.get(placeholderString),
          "Alias map should be updated with the real alias from CteContext");

      // Verify result SQL uses the *real* alias from the definition in the context
      assertEquals(
          "calculate(coalesce(" + realAlias + ".value, 0))",
          result,
          "Result SQL should use the real alias");
    }

    @Test
    void testProcessPsDePlaceholders_whenProgramIndicatorHasNullProgram_shouldSkipProcessing() {
      int offset = 0;
      String boundaryHash = "noboundaries";
      String currentPiUid = programIndicator.getUid();
      String placeholderString =
          buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, currentPiUid);
      String rawSql = "process(" + placeholderString + ")";

      programIndicator.setProgram(null);
      programIndicator.setAnalyticsPeriodBoundaries(Collections.emptySet());

      // method under test
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      // Verify the placeholder was NOT replaced, and the original SQL is returned
      assertEquals(
          rawSql, result, "SQL should remain unchanged when Program is null on ProgramIndicator");

      // Verify no CTE was generated
      assertTrue(
          cteContext.getCteKeys().isEmpty(), "CTE context should be empty when Program is null");

      // Verify the alias map was not populated
      assertTrue(psdeAliasMap.isEmpty(), "Alias map should be empty when Program is null");
    }

    @Test
    void testProcessPsDePlaceholders_withVariousSqlStructures() {
      String boundaryHash = "noboundaries"; // Keep it simple for this test

      // Placeholder 1: Offset 0 (latest)
      int offset1 = 0;
      String deUid1 = "DataElmUid1";
      String placeholder1 = buildPsDePlaceholder(psUid, deUid1, offset1, boundaryHash, piUid);
      String expectedCteKey1 =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid1, offset1, boundaryHash, piUid);
      int expectedRank1 = 1;

      // Placeholder 2: Offset -1 (second latest)
      int offset2 = -1;
      String deUid2 = "DataElmUid2"; // Different DE
      String placeholder2 = buildPsDePlaceholder(psUid, deUid2, offset2, boundaryHash, piUid);
      String expectedCteKey2 =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid2, offset2, boundaryHash, piUid);
      int expectedRank2 = 2;

      // Placeholder 3: Offset 1 (earliest) - Reuse DE1
      int offset3 = 1;
      String placeholder3 = buildPsDePlaceholder(psUid, deUid1, offset3, boundaryHash, piUid);
      String expectedCteKey3 =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid1, offset3, boundaryHash, piUid);
      int expectedRank3 = 1;

      // Construct complex raw SQL with placeholders in various positions
      String rawSql =
          placeholder1 // At start
              + " + d2:round("
              + placeholder2 // Inside function
              + ") / (SELECT 5) * "
              + placeholder3 // In middle
              + " - 10"; // Placeholder not at very end, but close

      // method under test
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      // Verify 3 distinct CTEs were generated
      assertEquals(3, cteContext.getCteKeys().size(), "Should generate 3 distinct CTEs");
      assertCteCreated(expectedCteKey1, PROGRAM_STAGE_DATE_ELEMENT, "enrollment", expectedRank1);
      assertCteCreated(expectedCteKey2, PROGRAM_STAGE_DATE_ELEMENT, "enrollment", expectedRank2);
      assertCteCreated(expectedCteKey3, PROGRAM_STAGE_DATE_ELEMENT, "enrollment", expectedRank3);

      // Verify alias map contains entries for all 3 placeholders
      String alias1 = assertPlaceholderMapped(placeholder1, expectedCteKey1, psdeAliasMap);
      String alias2 = assertPlaceholderMapped(placeholder2, expectedCteKey2, psdeAliasMap);
      String alias3 = assertPlaceholderMapped(placeholder3, expectedCteKey3, psdeAliasMap);
      assertEquals(3, psdeAliasMap.size());

      // Verify the final SQL string has all replacements correct
      String expectedSql =
          "coalesce("
              + alias1
              + ".value, 0)" // Replacement for placeholder1
              + " + d2:round("
              + "coalesce("
              + alias2
              + ".value, 0)" // Replacement for placeholder2
              + ") / (SELECT 5) * "
              + "coalesce("
              + alias3
              + ".value, 0)" // Replacement for placeholder3
              + " - 10";
      assertEquals(expectedSql, result, "Final SQL string replacements should be correct");
    }

    @Test
    void testProcessPsDePlaceholders_AdjacentToSyntax() {
      String boundaryHash = "noboundaries";
      int offset = 0;
      String placeholder = buildPsDePlaceholder(psUid, deUid, offset, boundaryHash, piUid);
      String expectedCteKey =
          String.format("psdecte_%s_%s_%d_%s_%s", psUid, deUid, offset, boundaryHash, piUid);

      String rawSql = "5+" + placeholder + "*10"; // Adjacent to operators

      // method under test
      String result =
          processPsDePlaceholdersAndGenerateCtes(
              rawSql, programIndicator, startDate, endDate, cteContext, psdeAliasMap, sqlBuilder);

      assertEquals(1, cteContext.getCteKeys().size());
      assertCteCreated(expectedCteKey, PROGRAM_STAGE_DATE_ELEMENT, "enrollment");
      String alias = assertPlaceholderMapped(placeholder, expectedCteKey, psdeAliasMap);
      assertEquals(1, psdeAliasMap.size());

      String expectedSql = "5+coalesce(" + alias + ".value, 0)*10";
      assertEquals(expectedSql, result);
    }
  }

  /** Tests for placeholder emitted by */
  @Nested
  class D2FunctionCteTests {
    @Test
    void testProcessD2FunctionPlaceholders_GeneratesCteAndReplacesPlaceholder() {
      final String funcName = "countIfValue";
      final String psUid = "TestPs1";
      final String deUid = "TestDe1";
      final String valueSql = "cast(123 as numeric)";
      final String boundaryHash = "boundaryHash123";
      final String piUid = "TestPi1";
      final String argType = "val64";

      ProgramIndicator programIndicator = mock(ProgramIndicator.class);
      Program testProgram = new Program();
      testProgram.setUid(piUid);
      when(programIndicator.getProgram()).thenReturn(testProgram);

      // Create the rich placeholder
      String encodedValueSql =
          Base64.getEncoder().encodeToString(valueSql.getBytes(StandardCharsets.UTF_8));
      String richPlaceholder =
          buildRichPlaceholder(
              funcName, psUid, deUid, argType, encodedValueSql, boundaryHash, piUid);
      String rawSql = "IF(" + richPlaceholder + " > 0, 1, 0)";

      // Expected values
      String valueHash = generateTestSqlHash(valueSql);
      String expectedCteKey =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              funcName.toLowerCase(), psUid, deUid, valueHash, boundaryHash, piUid);

      // method under test
      String resultSql =
          processD2FunctionPlaceholdersAndGenerateCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              d2FunctionAliasMap,
              sqlBuilder);

      assertCteCreated(expectedCteKey, D2_FUNCTION, "enrollment");
      assertCteSqlMatches(expectedCteKey, getExpectedD2FunctionSql(deUid, piUid, psUid, valueSql));
      String alias = assertPlaceholderMapped(richPlaceholder, expectedCteKey, d2FunctionAliasMap);
      assertEquals("IF(coalesce(" + alias + ".value, 0) > 0, 1, 0)", resultSql);
    }

    @Test
    void testProcessD2FunctionPlaceholders_MultipleDistinctAndIdenticalRichPlaceholders() {
      ProgramIndicator programIndicator = mock(ProgramIndicator.class);
      when(programIndicator.getProgram()).thenReturn(mock(Program.class));
      String funcName = "countIfValue";
      String testPsUid = "PsMulti";
      String testDeUid = "DeMulti";
      String valueSql1 = "cast(1 as numeric)";
      String valueSql2 = "'Active'"; // Different value, different type simulation
      String boundaryHash1 = "hash1";
      String boundaryHash2 = "hash2"; // Different boundaries
      String testPiUid = "PiMulti";
      String expectedArgType = "val64";

      String encodedValueSql1 =
          Base64.getEncoder().encodeToString(valueSql1.getBytes(StandardCharsets.UTF_8));
      String encodedValueSql2 =
          Base64.getEncoder().encodeToString(valueSql2.getBytes(StandardCharsets.UTF_8));

      String placeholder1 =
          buildRichPlaceholder(
              funcName,
              testPsUid,
              testDeUid,
              expectedArgType,
              encodedValueSql1,
              boundaryHash1,
              testPiUid);
      // Placeholder 2: value2, hash1 (different value)
      String placeholder2 =
          buildRichPlaceholder(
              funcName,
              testPsUid,
              testDeUid,
              expectedArgType,
              encodedValueSql2,
              boundaryHash1,
              testPiUid);
      // Placeholder 3: value1, hash2 (different hash)
      String placeholder3 =
          buildRichPlaceholder(
              funcName,
              testPsUid,
              testDeUid,
              expectedArgType,
              encodedValueSql1,
              boundaryHash2,
              testPiUid);
      // Placeholder 4: value1, hash1 (identical config to placeholder 1)
      String placeholder4 =
          buildRichPlaceholder(
              funcName,
              testPsUid,
              testDeUid,
              expectedArgType,
              encodedValueSql1,
              boundaryHash1,
              testPiUid);

      String rawSql = placeholder1 + "/" + placeholder2 + "-" + placeholder3 + "+" + placeholder4;

      // Expected keys
      String valueHash1 = generateTestSqlHash(valueSql1);
      String valueHash2 = generateTestSqlHash(valueSql2);
      String expectedCteKey1 =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              funcName.toLowerCase(),
              testPsUid,
              testDeUid,
              valueHash1,
              boundaryHash1,
              testPiUid); // P1 & P4
      String expectedCteKey2 =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              funcName.toLowerCase(),
              testPsUid,
              testDeUid,
              valueHash2,
              boundaryHash1,
              testPiUid); // P2
      String expectedCteKey3 =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              funcName.toLowerCase(),
              testPsUid,
              testDeUid,
              valueHash1,
              boundaryHash2,
              testPiUid); // P3

      // Mock PI program for table name generation
      Program testProgram = createProgram('A');
      when(programIndicator.getProgram()).thenReturn(testProgram);

      // Act
      String resultSql =
          processD2FunctionPlaceholdersAndGenerateCtes(
              rawSql,
              programIndicator,
              startDate,
              endDate,
              cteContext,
              d2FunctionAliasMap,
              sqlBuilder);

      // Assert CTE Generation
      assertEquals(3, cteContext.getCteKeys().size(), "Should generate 3 distinct CTEs");
      assertCteCreated(expectedCteKey1, D2_FUNCTION, "enrollment");
      assertCteCreated(expectedCteKey2, D2_FUNCTION, "enrollment");
      assertCteCreated(expectedCteKey3, D2_FUNCTION, "enrollment");

      // Assert Alias Map Population
      String alias1 = assertPlaceholderMapped(placeholder1, expectedCteKey1, d2FunctionAliasMap);
      String alias2 = assertPlaceholderMapped(placeholder2, expectedCteKey2, d2FunctionAliasMap);
      String alias3 = assertPlaceholderMapped(placeholder3, expectedCteKey3, d2FunctionAliasMap);
      assertEquals(3, d2FunctionAliasMap.size(), "Alias map should contain 3 entries");
      assertEquals(alias1, d2FunctionAliasMap.get(placeholder4)); // Reuse alias1

      // Assert Result SQL Replacement
      String expectedResultSql =
          "coalesce("
              + alias1
              + ".value, 0)/coalesce("
              + alias2
              + ".value, 0)-coalesce("
              + alias3
              + ".value, 0)+coalesce("
              + alias1
              + ".value, 0)";
      assertEquals(
          expectedResultSql, resultSql, "Result SQL should have placeholders replaced correctly");
    }

    @Test
    void testProcessD2FunctionPlaceholders_ParsesAndGeneratesCorrectCteKey() {
      ProgramIndicator programIndicator = mock(ProgramIndicator.class);
      when(programIndicator.getProgram()).thenReturn(mock(Program.class));

      // Define expected values "embedded" in the placeholder
      String funcName = "countIfValue";
      String testPsUid = "TestPs1";
      String testDeUid = "TestDe1";
      String valueSql = "cast(123 as numeric)";
      String boundaryHash = "boundaryHash123";
      String testPiUid = "TestPi1";
      String expectedArgType = "val64";

      // Encode the value SQL
      String encodedValueSql =
          Base64.getEncoder().encodeToString(valueSql.getBytes(StandardCharsets.UTF_8));

      // Construct the rich placeholder string
      String richPlaceholderString =
          buildRichPlaceholder(
              funcName,
              testPsUid,
              testDeUid,
              expectedArgType,
              encodedValueSql,
              boundaryHash,
              testPiUid);

      String rawSql = "IF(" + richPlaceholderString + " > 0, 1, 0)"; // Embed in SQL

      // Calculate the expected CTE key based on the *embedded* values
      String expectedValueHash = generateTestSqlHash(valueSql); // Hash of the *decoded* valueSql
      String expectedCteKey =
          String.format(
              "d2%s_%s_%s_%s_%s_%s",
              funcName.toLowerCase(),
              testPsUid,
              testDeUid,
              expectedValueHash,
              boundaryHash,
              testPiUid);

      when(programIndicator.getUid()).thenReturn(testPiUid);

      // method under test
      processD2FunctionPlaceholdersAndGenerateCtes(
          rawSql,
          programIndicator, // PI object is still needed for context (e.g., getting program)
          startDate,
          endDate,
          cteContext,
          d2FunctionAliasMap,
          sqlBuilder);

      // Verify CTE with the key derived from parsed values exists
      assertTrue(
          cteContext.containsCte(expectedCteKey),
          "Context should contain CTE key derived from parsed placeholder values. Key: "
              + expectedCteKey);
      assertEquals(
          1,
          cteContext.getCteKeys().size(),
          "Should have generated exactly one CTE from the placeholder");

      // Retrieve definition and perform basic checks
      CteDefinition cteDef = assertCteCreated(expectedCteKey, D2_FUNCTION, "enrollment");

      String expectedCondition = sqlBuilder.quote(testDeUid) + " = " + valueSql;
      assertTrue(
          normalizeSql(cteDef.getCteDefinition()).contains(normalizeSql(expectedCondition)),
          "Generated CTE SQL body should contain the condition with decoded value SQL");
      assertTrue(
          normalizeSql(cteDef.getCteDefinition()).contains("ps = '" + testPsUid + "'"),
          "Generated CTE SQL body should contain the correct program stage condition");
    }
  }

  // Helper to normalize SQL for comparison
  private String normalizeSql(String sql) {
    if (sql == null) return null;
    return sql.replaceAll("\\s+", " ").trim();
  }

  private String processPlaceholdersAndGenerateVariableCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      CteContext cteContext,
      Map<String, String> variableAliasMap,
      SqlBuilder sqlBuilder) {
    return new ProgramIndicatorPlaceholderUtils(dataElementService)
        .processPlaceholdersAndGenerateVariableCtes(
            rawSql, programIndicator, startDate, endDate, cteContext, variableAliasMap, sqlBuilder);
  }

  private String processPsDePlaceholdersAndGenerateCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      CteContext cteContext,
      Map<String, String> aliasMap,
      SqlBuilder sqlBuilder) {
    return new ProgramIndicatorPlaceholderUtils(dataElementService)
        .processPsDePlaceholdersAndGenerateCtes(
            rawSql, programIndicator, startDate, endDate, cteContext, aliasMap, sqlBuilder);
  }

  private String processD2FunctionPlaceholdersAndGenerateCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date startDate,
      Date endDate,
      CteContext cteContext,
      Map<String, String> d2FunctionAliasMap,
      SqlBuilder sqlBuilder) {
    return new ProgramIndicatorPlaceholderUtils(dataElementService)
        .processD2FunctionPlaceholdersAndGenerateCtes(
            rawSql,
            programIndicator,
            startDate,
            endDate,
            cteContext,
            d2FunctionAliasMap,
            sqlBuilder);
  }

  private String buildRichPlaceholder(
      String funcName,
      String psUid,
      String deUid,
      String argType,
      String arg64,
      String hash,
      String piUid) {
    return String.format(
        "__D2FUNC__(func='%s', ps='%s', de='%s', argType='%s', arg64='%s', hash='%s', pi='%s')__",
        funcName, psUid, deUid, argType, arg64, hash, piUid);
  }

  private String buildVariablePlaceholder(
      String type, String column, String piUid, String psUid, String offset) {
    return String.format(
        "FUNC_CTE_VAR( type='%s', column='%s', piUid='%s', psUid='%s', offset='%s')",
        type, column, piUid, psUid, offset);
  }

  private String buildPsDePlaceholder(
      String psUid, String deUid, int offset, String boundaryHash, String piUid) {
    return String.format(
        "__PSDE_CTE_PLACEHOLDER__(psUid='%s', deUid='%s', offset='%d', boundaryHash='%s', piUid='%s')",
        psUid, deUid, offset, boundaryHash, piUid);
  }

  private CteDefinition assertCteCreated(
      String expectedCteKey, CteDefinition.CteType expectedType, String expectedJoinColumn) {
    assertTrue(cteContext.containsCte(expectedCteKey));
    CteDefinition cteDef = cteContext.getDefinitionByKey(expectedCteKey);
    assertNotNull(cteDef);
    assertEquals(expectedType, cteDef.getCteType());
    assertEquals(expectedJoinColumn, cteDef.getJoinColumn());
    return cteDef;
  }

  private CteDefinition assertCteCreated(
      String expectedCteKey,
      CteDefinition.CteType expectedType,
      String expectedJoinColumn,
      Integer targetRank) {
    CteDefinition cteDef = assertCteCreated(expectedCteKey, expectedType, expectedJoinColumn);
    assertEquals(targetRank, cteDef.getTargetRank());
    return cteDef;
  }

  private void assertCteSqlMatches(String expectedCteKey, String expectedSql) {
    CteDefinition cteDef = cteContext.getDefinitionByKey(expectedCteKey);
    assertNotNull(cteDef);
    assertEquals(normalizeSql(expectedSql), normalizeSql(cteDef.getCteDefinition()));
  }

  private String assertPlaceholderMapped(
      String placeholder, String expectedCteKey, Map<String, String> aliasMap) {
    CteDefinition cteDef = cteContext.getDefinitionByKey(expectedCteKey);
    String alias = cteDef.getAlias();
    assertNotNull(alias);
    assertEquals(alias, aliasMap.get(placeholder));
    return alias;
  }

  private String getExpectedPsdeSql(String deUid, String orderBy, String eventTable, String psUid) {
    return String.format(
        "select enrollment, %s as value, "
            + "row_number() over (partition by enrollment order by %s) as rn "
            + "from %s "
            + "where %s is not null and ps = '%s' ",
        sqlBuilder.quote(deUid), orderBy, eventTable, sqlBuilder.quote(deUid), psUid);
  }

  private String getExpectedD2FunctionSql(
      String deUid, String piUid, String psUid, String valueSql) {
    String quotedDeUid = sqlBuilder.quote(deUid);
    return String.format(
        "select enrollment, count(%s) as value from analytics_event_%s "
            + "where ps = '%s' and %s is not null and %s = %s group by enrollment",
        quotedDeUid, piUid, psUid, quotedDeUid, quotedDeUid, valueSql);
  }

  private String generateTestSqlHash(String sql) {
    if (sql == null) {
      return "null_sql";
    }
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] messageDigest = md.digest(sql.getBytes(StandardCharsets.UTF_8));
      BigInteger no = new BigInteger(1, messageDigest);
      return String.format("%040x", no);
    } catch (NoSuchAlgorithmException e) {
      return "hash_error_" + String.valueOf(sql.hashCode()).replace('-', 'N');
    }
  }
}
