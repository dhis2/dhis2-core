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
package org.hisp.dhis.program;

import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramIndicatorServiceD2FunctionTest extends PostgresIntegrationTestBase {

  @Autowired private ProgramIndicatorService programIndicatorService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private SystemSettingsService systemSettingsService;

  private ProgramStage psA;

  private ProgramStage psB;

  private Program programA;

  private Program programB;

  private DataElement deA;

  private DataElement deB;

  private DataElement deD;

  private ProgramIndicator piA;

  private ProgramIndicator piB;

  private RelationshipType relationshipTypeA;

  private final Date newDate = new GregorianCalendar(2020, Calendar.JANUARY, 9).getTime();

  @BeforeAll
  void setUp() {
    systemSettingsService.clearCurrentSettings();

    OrganisationUnit organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    // ---------------------------------------------------------------------
    // Program
    // ---------------------------------------------------------------------
    programA = createProgram('A', new HashSet<>(), organisationUnit);
    programA.setUid("Program000A");
    programService.addProgram(programA);
    psA = new ProgramStage("StageA", programA);
    psA.setSortOrder(1);
    psA.setUid("ProgrmStagA");
    programStageService.saveProgramStage(psA);
    psB = new ProgramStage("StageB", programA);
    psB.setSortOrder(2);
    psB.setUid("ProgrmStagB");
    programStageService.saveProgramStage(psB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(psA);
    programStages.add(psB);
    programA.setProgramStages(programStages);
    programService.updateProgram(programA);
    programB = createProgram('B', new HashSet<>(), organisationUnit);
    programB.setUid("Program000B");
    programService.addProgram(programB);
    // ---------------------------------------------------------------------
    // Program Stage DE
    // ---------------------------------------------------------------------
    deA = createDataElement('A');
    deA.setDomainType(DataElementDomain.TRACKER);
    deA.setUid("DataElmentA");
    deB = createDataElement('B');
    deB.setDomainType(DataElementDomain.TRACKER);
    deB.setUid("DataElmentB");
    deD = createDataElement('D');
    deD.setDomainType(DataElementDomain.TRACKER);
    deD.setUid("DataElmentD");
    deD.setValueType(ValueType.DATE);
    dataElementService.addDataElement(deA);
    dataElementService.addDataElement(deB);
    dataElementService.addDataElement(deD);
    ProgramStageDataElement stageDataElementA = new ProgramStageDataElement(psA, deA, false, 1);
    ProgramStageDataElement stageDataElementB = new ProgramStageDataElement(psA, deB, false, 2);
    ProgramStageDataElement stageDataElementD = new ProgramStageDataElement(psA, deD, false, 3);
    ProgramStageDataElement stageDataElementE = new ProgramStageDataElement(psB, deA, false, 1);
    ProgramStageDataElement stageDataElementF = new ProgramStageDataElement(psB, deB, false, 2);
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    programStageDataElementService.addProgramStageDataElement(stageDataElementD);
    programStageDataElementService.addProgramStageDataElement(stageDataElementE);
    programStageDataElementService.addProgramStageDataElement(stageDataElementF);
    // ---------------------------------------------------------------------
    // ProgramIndicator
    // ---------------------------------------------------------------------
    piA = createProgramIndicator('A', programA, "20", null);
    programA.getProgramIndicators().add(piA);
    piB = createProgramIndicator('B', programA, "70", null);
    piB.setAnalyticsType(AnalyticsType.ENROLLMENT);
    programA.getProgramIndicators().add(piB);
    // ---------------------------------------------------------------------
    // RelationshipType
    // ---------------------------------------------------------------------
    relationshipTypeA = createRelationshipType('A');
    relationshipTypeA.setUid("RelatioTypA");
  }

  private String getSql(String expression) {
    return programIndicatorService.getAnalyticsSql(expression, NUMERIC, piA, newDate, newDate);
  }

  private String getSqlEnrollment(String expression) {
    return programIndicatorService.getAnalyticsSql(expression, NUMERIC, piB, newDate, newDate);
  }

  // -------------------------------------------------------------------------
  // D2 function tests (in alphabetical order)
  // -------------------------------------------------------------------------
  @Test
  void testD2Condition() {
    assertEquals(
        "case when ((case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end is not null)) then 1::numeric + 4::numeric else "
            + "nullif(cast((case when case when ax.\"ps\" = 'Program000B' then \"DataElmentB\" else null end >= 0 then 1 else 0 end) as double precision),0) end",
        getSql(
            "d2:condition( 'd2:hasValue(#{ProgrmStagA.DataElmentA})', 1+4, d2:zpvc(#{Program000B.DataElmentB}) )"));
    String enrollmentSql =
        getSqlEnrollment(
            "d2:condition( \"d2:hasValue(#{ProgrmStagA.DataElmentA})\", 1+4, d2:zpvc(#{Program000B.DataElmentB}) )");
    assertPlaceholder(enrollmentSql, "ProgrmStagA", "DataElmentA", "0", piB.getUid());
    assertPlaceholder(enrollmentSql, "Program000B", "DataElmentB", "0", piB.getUid());
  }

  @Test
  void testD2Count() {
    assertEquals(
        "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and \"DataElmentA\" is not null and \"DataElmentA\" is not null and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        normalizeSql(getSql("d2:count(#{ProgrmStagA.DataElmentA})")));
    assertD2Func(
        normalizeSql(getSqlEnrollment("d2:count(#{ProgrmStagA.DataElmentA})")),
        "count",
        "ProgrmStagA",
        "DataElmentA",
        "none",
        piB.getUid());
  }

  @Test
  void testD2CountIfCondition() {
    assertEquals(
        "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment "
            + "and \"DataElmentA\" is not null and \"DataElmentA\"::numeric >= coalesce(case when ax.\"ps\" = 'Program000B' then \"DataElmentB\" else null end::numeric,0) "
            + "and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        normalizeSql(
            getSql(
                "d2:countIfCondition( #{ProgrmStagA.DataElmentA}, ' >= #{Program000B.DataElmentB}')")));
    String enrollmentSql =
        normalizeSql(
            getSqlEnrollment(
                "d2:countIfCondition( #{ProgrmStagA.DataElmentA}, \" >= #{Program000B.DataElmentB}\")"));
    assertD2Func(
        enrollmentSql, "countIfCondition", "ProgrmStagA", "DataElmentA", "condLit64", piB.getUid());
  }

  @Test
  void testD2CountIfValue() {
    assertEquals(
        "(select count(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and \"DataElmentA\" is not null and \"DataElmentA\" = 10::numeric and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        normalizeSql(getSql("d2:countIfValue(#{ProgrmStagA.DataElmentA}, 10)")));
    String enrollmentSql =
        normalizeSql(getSqlEnrollment("d2:countIfValue(#{ProgrmStagA.DataElmentA}, 10)"));
    assertD2Func(
        enrollmentSql, "countIfValue", "ProgrmStagA", "DataElmentA", "val64", piB.getUid());
  }

  @Test
  void testD2DaysBetween() {
    assertEquals(
        "(cast(occurreddate as date) - cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date))",
        getSql("d2:daysBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"));
    assertPlaceholder(
        getSqlEnrollment("d2:daysBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"),
        "ProgrmStagA",
        "DataElmentD",
        "0",
        piB.getUid());
  }

  @Test
  void testD2HasValue() {
    assertEquals(
        "(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end is not null)",
        getSql("d2:hasValue(#{ProgrmStagA.DataElmentA})"));
    assertPlaceholder(
        getSqlEnrollment("d2:hasValue(#{ProgrmStagA.DataElmentA})"),
        "ProgrmStagA",
        "DataElmentA",
        "0",
        piB.getUid());
  }

  @Test
  void testD2MaxValue() {
    assertEquals("\"DataElmentA\"", getSql("d2:maxValue(#{ProgrmStagA.DataElmentA})"));
    assertEquals(
        "(select max(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        getSqlEnrollment("d2:maxValue(#{ProgrmStagA.DataElmentA})"));
    assertEquals("\"occurreddate\"", getSql("d2:maxValue(PS_EVENTDATE:ProgrmStagA)"));
    assertEquals(
        "(select max(\"occurreddate\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        getSqlEnrollment("d2:maxValue(PS_EVENTDATE:ProgrmStagA)"));
  }

  @Test
  void testD2MinutesBetween() {
    assertEquals(
        "(extract(epoch from (cast(occurreddate as timestamp) - cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as timestamp))) / 60)",
        getSql("d2:minutesBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"));
    assertPlaceholder(
        getSqlEnrollment("d2:minutesBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"),
        "ProgrmStagA",
        "DataElmentD",
        "0",
        piB.getUid());
  }

  @Test
  void testD2MinValue() {
    assertEquals("\"DataElmentA\"", getSql("d2:minValue(#{ProgrmStagA.DataElmentA})"));
    assertEquals(
        "(select min(\"DataElmentA\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        getSqlEnrollment("d2:minValue(#{ProgrmStagA.DataElmentA})"));
    assertEquals("\"occurreddate\"", getSql("d2:minValue(PS_EVENTDATE:ProgrmStagA)"));
    assertEquals(
        "(select min(\"occurreddate\") from analytics_event_Program000A where analytics_event_Program000A.enrollment = ax.enrollment and occurreddate < cast( '2020-01-10' as date ) and occurreddate >= cast( '2020-01-09' as date ) and ps = 'ProgrmStagA')",
        getSqlEnrollment("d2:minValue(PS_EVENTDATE:ProgrmStagA)"));
  }

  @Test
  void testD2MonthsBetween() {
    assertEquals(
        "((date_part('year',age(cast(occurreddate as date), cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date)))) * 12 "
            + "+ date_part('month',age(cast(occurreddate as date), cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentD\" else null end as date))))",
        getSql("d2:monthsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"));
    assertPlaceholder(
        getSqlEnrollment("d2:monthsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"),
        "ProgrmStagA",
        "DataElmentD",
        "0",
        piB.getUid());
  }

  @Test
  void testD2Oizp() {
    assertEquals(
        "((date_part('year',age(cast(occurreddate as date), cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end as date)))) * 12 "
            + "+ date_part('month',age(cast(occurreddate as date), cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end as date))))",
        getSql("d2:monthsBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)"));
    assertEquals(
        "coalesce(case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end >= 0 then 1 else 0 end, 0)",
        getSql("d2:oizp(#{ProgrmStagA.DataElmentA})"));
    assertPlaceholder(
        getSqlEnrollment("d2:oizp(#{ProgrmStagA.DataElmentA})"),
        "ProgrmStagA",
        "DataElmentA",
        "0",
        piB.getUid());
  }

  @Test
  void testD2RelationshipCount() {
    assertEquals(
        """
                    (select sum(relationship_count)
                     from analytics_rs_relationship arr
                     where arr.trackedentityid = ax.trackedentity)
                    """,
        getSql("d2:relationshipCount()"));
    assertEquals(
        """
                    (select sum(relationship_count)
                     from analytics_rs_relationship arr
                     where arr.trackedentityid = ax.trackedentity)
                    """,
        getSqlEnrollment("d2:relationshipCount()"));
    assertEquals(
        normalizeSql(
            """
                    (select relationship_count
                     from analytics_rs_relationship arr
                     where arr.trackedentityid = ax.trackedentity and relationshiptypeuid = 'RelatioTypA')
                    """),
        normalizeSql(getSql("d2:relationshipCount('RelatioTypA')")));
    assertEquals(
        normalizeSql(
            """
                    (select relationship_count
                     from analytics_rs_relationship arr
                     where arr.trackedentityid = ax.trackedentity and relationshiptypeuid = 'RelatioTypA')"""),
        normalizeSql(getSqlEnrollment("d2:relationshipCount('RelatioTypA')")));
  }

  @Test
  void testD2WeeksBetween() {
    assertEquals(
        "((cast(occurreddate as date) - cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end as date)) / 7)",
        getSql("d2:weeksBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)"));
    assertPlaceholder(
        getSqlEnrollment("d2:weeksBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)"),
        "ProgrmStagA",
        "DataElmentD",
        "0",
        piB.getUid());
  }

  @Test
  void testD2YearsBetween() {
    assertEquals(
        "(date_part('year',age(cast(occurreddate as date), cast(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end as date))))",
        getSql("d2:yearsBetween(#{ProgrmStagA.DataElmentA}, PS_EVENTDATE:ProgrmStagA)"));
    var enrol =
        getSqlEnrollment("d2:yearsBetween(#{ProgrmStagA.DataElmentD}, PS_EVENTDATE:ProgrmStagA)");
    assertPlaceholder(enrol, "ProgrmStagA", "DataElmentD", "0", piB.getUid());
  }

  /**
   * Parses a SQL string containing {@code __PSDE_CTE_PLACEHOLDER__(...)} tokens and asserts that a
   * placeholder with the given psUid, deUid, offset and piUid exists. The boundaryHash is asserted
   * to be non-null (its value is non-deterministic across test runs).
   */
  private static void assertPlaceholder(
      String sql, String psUid, String deUid, String offset, String piUid) {
    Pattern pattern = Pattern.compile("__PSDE_CTE_PLACEHOLDER__\\(([^)]+)\\)");
    Matcher matcher = pattern.matcher(sql);
    while (matcher.find()) {
      Map<String, String> fields = parsePlaceholderFields(matcher.group(1));
      if (psUid.equals(fields.get("psUid"))
          && deUid.equals(fields.get("deUid"))
          && offset.equals(fields.get("offset"))
          && piUid.equals(fields.get("piUid"))) {
        assertNotNull(fields.get("boundaryHash"), "boundaryHash should not be null");
        return;
      }
    }
    fail(
        String.format(
            "No __PSDE_CTE_PLACEHOLDER__ found with psUid='%s', deUid='%s', offset='%s', piUid='%s' in:%n%s",
            psUid, deUid, offset, piUid, sql));
  }

  @Test
  void testD2Zing() {
    assertEquals(
        "greatest(0,coalesce(case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end::numeric,0) + 5::numeric)",
        getSql("d2:zing(#{ProgrmStagA.DataElmentA} + 5)"));
    assertPlaceholder(
        getSqlEnrollment("d2:zing(#{ProgrmStagA.DataElmentA} + 5)"),
        "ProgrmStagA",
        "DataElmentA",
        "0",
        piB.getUid());
  }

  @Test
  void testD2Zpvc() {
    assertEquals(
        "nullif(cast((case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentA\" else null end >= 0 then 1 else 0 end "
            + "+ case when case when ax.\"ps\" = 'ProgrmStagA' then \"DataElmentB\" else null end >= 0 then 1 else 0 end) as double precision),0)",
        getSql("d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagA.DataElmentB})"));
    String enrollmentSql =
        getSqlEnrollment("d2:zpvc(#{ProgrmStagA.DataElmentA},#{ProgrmStagB.DataElmentB})");
    assertPlaceholder(enrollmentSql, "ProgrmStagA", "DataElmentA", "0", piB.getUid());
    assertPlaceholder(enrollmentSql, "ProgrmStagB", "DataElmentB", "0", piB.getUid());
  }

  private String normalizeSql(String sql) {
    return sql.replaceAll("\\s+", " ").trim();
  }

  /**
   * Parses a SQL string containing {@code __D2FUNC__(...)__} tokens and asserts that a placeholder
   * with the given func, ps, de, argType and pi exists. The hash is asserted to be non-null (its
   * value is non-deterministic across test runs).
   */
  private static void assertD2Func(
      String sql, String func, String ps, String de, String argType, String piUid) {
    Pattern pattern = Pattern.compile("__D2FUNC__\\(([^)]+)\\)__");
    Matcher matcher = pattern.matcher(sql);
    while (matcher.find()) {
      Map<String, String> fields = parsePlaceholderFields(matcher.group(1));
      if (func.equals(fields.get("func"))
          && ps.equals(fields.get("ps"))
          && de.equals(fields.get("de"))
          && argType.equals(fields.get("argType"))
          && piUid.equals(fields.get("pi"))) {
        assertNotNull(fields.get("hash"), "hash should not be null");
        return;
      }
    }
    fail(
        String.format(
            "No __D2FUNC__ found with func='%s', ps='%s', de='%s', argType='%s', pi='%s' in:%n%s",
            func, ps, de, argType, piUid, sql));
  }

  /** Parses {@code key='value', key='value'} pairs from a placeholder body. */
  private static Map<String, String> parsePlaceholderFields(String body) {
    Map<String, String> fields = new java.util.LinkedHashMap<>();
    Pattern fieldPattern = Pattern.compile("(\\w+)='([^']*)'");
    Matcher m = fieldPattern.matcher(body);
    while (m.find()) {
      fields.put(m.group(1), m.group(2));
    }
    return fields;
  }
}
