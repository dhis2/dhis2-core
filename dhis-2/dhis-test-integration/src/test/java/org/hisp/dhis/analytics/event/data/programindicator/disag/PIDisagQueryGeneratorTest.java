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
package org.hisp.dhis.analytics.event.data.programindicator.disag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.db.sql.PostgreSqlBuilder;
import org.hisp.dhis.program.ProgramCategoryMappingValidator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class PIDisagQueryGeneratorTest extends AbstractPIDisagTest {

  @Autowired private ProgramIndicatorService programIndicatorService;

  @Autowired private ProgramCategoryMappingValidator mappingValidator;

  private PiDisagInfoInitializer infoInitializer;

  private PiDisagQueryGenerator target;

  @Override
  @BeforeAll
  protected void setUp() {
    super.setUp();
    infoInitializer = new PiDisagInfoInitializer(mappingValidator);
    target = new PiDisagQueryGenerator(programIndicatorService, new PostgreSqlBuilder());
  }

  @Test
  void testGetCocSelectColumns_WithoutDimension() {

    // Given
    EventQueryParams testParams =
        new EventQueryParams.Builder()
            .withProgramIndicator(programIndicator)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();

    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(testParams);

    Set<String> expectedColumns =
        Set.of(
            "concat(case when 'filterA' = '' then 'catOption0A' else '' end, case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"",
            "concat(case when 'filterC' = '' then 'catOption0C' else '' end, case when 'filterD' = '' then 'catOption0D' else '' end) as \"CategoryId2\"",
            "concat(case when 'filterE' = '' then 'catOption0E' else '' end, case when 'filterF' = '' then 'catOption0F' else '' end) as \"CategoryId3\"");

    // When
    List<String> columns = target.getCocSelectColumns(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationColumns_WithDimension() {

    // Given
    EventQueryParams testParams =
        new EventQueryParams.Builder()
            .withProgramIndicator(programIndicator)
            .addDimension(category2)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();

    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(testParams);

    Set<String> expectedColumns =
        Set.of(
            "concat(case when 'filterA' = '' then 'catOption0A' else '' end, case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"",
            "concat(case when 'filterE' = '' then 'catOption0E' else '' end, case when 'filterF' = '' then 'catOption0F' else '' end) as \"CategoryId3\"");

    // When
    List<String> columns = target.getCocSelectColumns(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationGroupByColumns_WithoutDimension() {

    // Given
    EventQueryParams testParams =
        new EventQueryParams.Builder()
            .withProgramIndicator(programIndicator)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();

    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(testParams);

    Set<String> expectedColumns = Set.of("\"CategoryId1\"", "\"CategoryId2\"", "\"CategoryId3\"");

    // When
    List<String> columns = target.getCocColumnsForGroupBy(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationGroupByColumns_WithDimension() {

    // Given
    EventQueryParams testParams =
        new EventQueryParams.Builder()
            .withProgramIndicator(programIndicator)
            .addDimension(category2)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();

    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(testParams);

    Set<String> expectedColumns = Set.of("\"CategoryId1\"", "\"CategoryId3\"");

    // When
    List<String> columns = target.getCocColumnsForGroupBy(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetColumnForSelectOrGroupBy_Select() {

    // Given
    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(eventQueryParams);
    String expectedColumn =
        "concat(case when 'filterA' = '' then 'catOption0A' else '' end, case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"";

    // When
    String column = target.getColumnForSelectOrGroupBy(params, "CategoryId1", false);

    // Then
    assertEquals(expectedColumn, column);
  }

  @Test
  void testGetColumnForSelectOrGroupBy_GroupBy() {

    // Given
    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(eventQueryParams);
    String expectedColumn =
        "concat(case when 'filterA' = '' then 'catOption0A' else '' end, case when 'filterB' = '' then 'catOption0B' else '' end)";

    // When
    String column = target.getColumnForSelectOrGroupBy(params, "CategoryId1", true);

    // Then
    assertEquals(expectedColumn, column);
  }

  @Test
  void testGetColumnForWhereClause() {

    // Given
    EventQueryParams params = infoInitializer.getParamsWithDisaggregationInfo(eventQueryParams);
    String expectedColumn =
        "concat(case when 'filterA' = '' then 'catOption0A' else '' end, case when 'filterB' = '' then 'catOption0B' else '' end)";

    // When
    String column = target.getColumnForWhereClause(params, "CategoryId1");

    // Then
    assertEquals(expectedColumn, column);
  }
}
