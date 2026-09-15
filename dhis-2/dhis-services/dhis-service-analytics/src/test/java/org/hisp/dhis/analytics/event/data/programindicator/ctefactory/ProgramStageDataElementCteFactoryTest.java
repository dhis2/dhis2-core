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
package org.hisp.dhis.analytics.event.data.programindicator.ctefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsPeriodBoundary;
import org.hisp.dhis.program.AnalyticsPeriodBoundaryType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests the SQL rendered in place of a program stage data element placeholder. */
@ExtendWith(MockitoExtension.class)
class ProgramStageDataElementCteFactoryTest extends TestBase {

  private static final String PLACEHOLDER =
      "__PSDE_CTE_PLACEHOLDER__(psUid='psUid00001A', deUid='deUid00001A', offset='0', "
          + "boundaryHash='hash', piUid='piUid00001A')";

  private ProgramStageDataElementCteFactory factory;

  @Mock private DataElementService dataElementService;

  @Mock private CteContext cteContext;

  @Mock private CteDefinition cteDefinition;

  private final SqlBuilder sqlBuilder = new PostgreSqlBuilder();

  private final Map<String, String> aliasMap = new HashMap<>();

  @BeforeEach
  void setUp() {
    factory = new ProgramStageDataElementCteFactory(dataElementService);
  }

  @Test
  void trueOnlyDataElementRendersNumericCoalesce() {
    assertEquals("select coalesce(d1.value, 0) as col", render(ValueType.TRUE_ONLY));
  }

  @Test
  void booleanDataElementRendersNumericCoalesce() {
    assertEquals("select coalesce(d1.value, 0) as col", render(ValueType.BOOLEAN));
  }

  @Test
  void textDataElementRendersEmptyStringCoalesce() {
    assertEquals("select coalesce(d1.value, '') as col", render(ValueType.TEXT));
  }

  @Test
  void dateDataElementRendersValueWithoutCoalesce() {
    assertEquals("select d1.value as col", render(ValueType.DATE));
  }

  /**
   * The CTE selects from the event analytics table, where the incident date of the owning
   * enrollment is stored in "enrollmentoccurreddate" - "occurreddate" holds the event date.
   * Filtering on "occurreddate" makes incident date bounded program indicators return no events.
   */
  @Test
  void incidentDateBoundariesFilterOnEnrollmentOccurredDate() throws ParseException {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.NUMBER);
    lenient().when(dataElementService.getDataElement("deUid00001A")).thenReturn(dataElement);

    Program program = createProgram('A');
    ProgramIndicator programIndicator =
        createProgramIndicator('A', AnalyticsType.ENROLLMENT, program, "1+1", null);
    programIndicator.setAnalyticsPeriodBoundaries(
        Set.of(
            incidentDateBoundary(AnalyticsPeriodBoundaryType.AFTER_START_OF_REPORTING_PERIOD),
            incidentDateBoundary(AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD)));

    SimpleDateFormat dateFormat = new SimpleDateFormat(Period.DEFAULT_DATE_FORMAT);
    CteContext cteContext = new CteContext(EndpointItem.ENROLLMENT);

    factory.process(
        "select " + PLACEHOLDER + " as col",
        programIndicator,
        dateFormat.parse("2026-01-01"),
        dateFormat.parse("2026-12-31"),
        cteContext,
        aliasMap,
        sqlBuilder);

    CteDefinition definition =
        cteContext.getDefinitionByKey("psdecte_psUid00001A_deUid00001A_0_hash_piUid00001A");
    assertNotNull(definition);

    String cteSql = definition.getCteDefinition();
    assertTrue(cteSql.contains("\"enrollmentoccurreddate\" >= '2026-01-01'"), cteSql);
    assertTrue(cteSql.contains("\"enrollmentoccurreddate\" < '2027-01-01'"), cteSql);
    assertFalse(cteSql.contains("\"occurreddate\" >="), cteSql);
    assertFalse(cteSql.contains("\"occurreddate\" <"), cteSql);
    // events within the enrollment are still ranked by their own event date
    assertTrue(cteSql.contains("order by \"occurreddate\" desc"), cteSql);
  }

  private AnalyticsPeriodBoundary incidentDateBoundary(AnalyticsPeriodBoundaryType boundaryType) {
    AnalyticsPeriodBoundary boundary = new AnalyticsPeriodBoundary();
    boundary.setUid("incident" + boundaryType.name().charAt(0));
    boundary.setAnalyticsPeriodBoundaryType(boundaryType);
    boundary.setBoundaryTarget(AnalyticsPeriodBoundary.INCIDENT_DATE);
    return boundary;
  }

  private String render(ValueType valueType) {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(valueType);
    when(dataElementService.getDataElement("deUid00001A")).thenReturn(dataElement);

    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

    when(cteDefinition.getAlias()).thenReturn("d1");
    when(cteContext.containsCte(anyString())).thenReturn(true);
    when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

    return factory
        .process(
            "select " + PLACEHOLDER + " as col",
            programIndicator,
            new Date(),
            new Date(),
            cteContext,
            aliasMap,
            sqlBuilder)
        .trim();
  }
}
