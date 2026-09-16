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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.ClickHouseSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests the SQL rendered in place of a program stage data element placeholder. */
@ExtendWith(MockitoExtension.class)
class ProgramStageDataElementCteFactoryTest extends TestBase {

  private static final String PLACEHOLDER_TEMPLATE =
      "__PSDE_CTE_PLACEHOLDER__(psUid='psUid00001A', deUid='deUid00001A', offset='0', "
          + "boundaryHash='hash', piUid='piUid00001A', replaceNulls='%b')";

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

  @Test
  void nullPreservingPlaceholderRendersValueWithoutCoalesce() {
    assertEquals("select d1.value as col", render(ValueType.TEXT, false));
  }

  @Test
  void textDataElementCteNormalisesEmptyStringToNullOnClickHouse() {
    String body = cteBody(ValueType.TEXT, new ClickHouseSqlBuilder("dhis2"));

    assertTrue(
        body.contains("where nullif(\"deUid00001A\", '') is not null"),
        "ClickHouse stores '' for an absent text value, so the CTE must normalise it: " + body);
  }

  @Test
  void numericDataElementCteTestsTheColumnDirectlyOnClickHouse() {
    String body = cteBody(ValueType.NUMBER, new ClickHouseSqlBuilder("dhis2"));

    assertTrue(
        body.contains("where \"deUid00001A\" is not null"),
        "Numeric columns already hold NULL for an absent value: " + body);
  }

  @Test
  void textDataElementCteTestsTheColumnDirectlyOnPostgres() {
    String body = cteBody(ValueType.TEXT, new PostgreSqlBuilder());

    assertTrue(
        body.contains("where \"deUid00001A\" is not null"),
        "Postgres stores NULL for an absent value, so no normalisation is needed: " + body);
  }

  @Test
  void repeatedPlaceholdersLookUpTheDataElementOnce() {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(ValueType.TEXT);
    when(dataElementService.getDataElement("deUid00001A")).thenReturn(dataElement);

    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

    when(cteDefinition.getAlias()).thenReturn("d1");
    when(cteContext.containsCte(anyString())).thenReturn(false);
    when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

    String placeholder = String.format(PLACEHOLDER_TEMPLATE, true);

    factory.process(
        "select " + placeholder + " + " + placeholder + " + " + placeholder + " as col",
        programIndicator,
        new Date(),
        new Date(),
        cteContext,
        aliasMap,
        sqlBuilder);

    // getDataElement runs an uncached query against the transactional database.
    verify(dataElementService, times(1)).getDataElement("deUid00001A");
  }

  private String cteBody(ValueType valueType, SqlBuilder builder) {
    DataElement dataElement = createDataElement('A');
    dataElement.setValueType(valueType);
    when(dataElementService.getDataElement("deUid00001A")).thenReturn(dataElement);

    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

    when(cteContext.containsCte(anyString())).thenReturn(false);

    factory.process(
        "select " + String.format(PLACEHOLDER_TEMPLATE, false) + " as col",
        programIndicator,
        new Date(),
        new Date(),
        cteContext,
        aliasMap,
        builder);

    ArgumentCaptor<CteDefinition> captor = ArgumentCaptor.forClass(CteDefinition.class);
    verify(cteContext).addProgramStageDataElementCte(anyString(), captor.capture());

    return captor.getValue().getCteDefinition();
  }

  private String render(ValueType valueType) {
    return render(valueType, true);
  }

  private String render(ValueType valueType, boolean replaceNulls) {
    if (replaceNulls) {
      DataElement dataElement = createDataElement('A');
      dataElement.setValueType(valueType);
      when(dataElementService.getDataElement("deUid00001A")).thenReturn(dataElement);
    }

    Program program = createProgram('A');
    ProgramIndicator programIndicator = createProgramIndicator('A', program, "1+1", "1+1");

    when(cteDefinition.getAlias()).thenReturn("d1");
    when(cteContext.containsCte(anyString())).thenReturn(true);
    when(cteContext.getDefinitionByKey(anyString())).thenReturn(cteDefinition);

    return factory
        .process(
            "select " + String.format(PLACEHOLDER_TEMPLATE, replaceNulls) + " as col",
            programIndicator,
            new Date(),
            new Date(),
            cteContext,
            aliasMap,
            sqlBuilder)
        .trim();
  }
}
