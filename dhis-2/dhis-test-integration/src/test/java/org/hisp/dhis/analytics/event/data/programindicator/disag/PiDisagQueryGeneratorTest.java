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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
class PiDisagQueryGeneratorTest extends AbstractPiDisagTest {

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
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDimensionEventQueryParams);

    Set<String> expectedColumns =
        Set.of(
            "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"",
            "(case when 'filterC' = '' then 'catOption0C' else '' end || case when 'filterD' = '' then 'catOption0D' else '' end) as \"CategoryId2\"",
            "(case when 'filterE' = '' then 'catOption0E' else '' end || case when 'filterF' = '' then 'catOption0F' else '' end) as \"CategoryId3\"",
            "(case when 'filterG' = '' then 'catOption0G' else '' end || case when 'filterH' = '' then 'catOption0H' else '' end) as \"CategoryId4\"");

    // When
    List<String> columns = target.getCocSelectColumns(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetCocSelectColumns_WithDimension() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    Set<String> expectedColumns =
        Set.of(
            "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"",
            "(case when 'filterE' = '' then 'catOption0E' else '' end || case when 'filterF' = '' then 'catOption0F' else '' end) as \"CategoryId3\"",
            "(case when 'filterG' = '' then 'catOption0G' else '' end || case when 'filterH' = '' then 'catOption0H' else '' end) as \"CategoryId4\"");

    // When
    List<String> columns = target.getCocSelectColumns(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetCocSelectColumns_WithoutDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDataValueSetEventQueryParams);

    Set<String> expectedColumns = emptySet();

    // When
    List<String> columns = target.getCocSelectColumns(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationGroupByColumns_WithoutDimension() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDimensionEventQueryParams);

    Set<String> expectedColumns =
        Set.of(
            "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end)",
            "(case when 'filterC' = '' then 'catOption0C' else '' end || case when 'filterD' = '' then 'catOption0D' else '' end)",
            "(case when 'filterE' = '' then 'catOption0E' else '' end || case when 'filterF' = '' then 'catOption0F' else '' end)",
            "(case when 'filterG' = '' then 'catOption0G' else '' end || case when 'filterH' = '' then 'catOption0H' else '' end)");

    // When
    List<String> columns = target.getCocColumnsForGroupBy(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationGroupByColumns_WithDimension() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    Set<String> expectedColumns =
        Set.of(
            "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end)",
            "(case when 'filterE' = '' then 'catOption0E' else '' end || case when 'filterF' = '' then 'catOption0F' else '' end)",
            "(case when 'filterG' = '' then 'catOption0G' else '' end || case when 'filterH' = '' then 'catOption0H' else '' end)");

    // When
    List<String> columns = target.getCocColumnsForGroupBy(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetDisaggregationGroupByColumns_WithoutDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDataValueSetEventQueryParams);

    Set<String> expectedColumns = emptySet();

    // When
    List<String> columns = target.getCocColumnsForGroupBy(params);

    // Then (in any order)
    assertEquals(expectedColumns, new HashSet<>(columns));
  }

  @Test
  void testGetColumnForSelectOrGroupBy_Select() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    String expectedColumn =
        "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end) as \"CategoryId1\"";

    // When
    String column = target.getColumnForSelectOrGroupBy(params, "CategoryId1", false);

    // Then
    assertEquals(expectedColumn, column);
  }

  @Test
  void testGetColumnForSelectOrGroupBy_GroupBy() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    String expectedColumn =
        "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end)";

    // When
    String column = target.getColumnForSelectOrGroupBy(params, "CategoryId1", true);

    // Then
    assertEquals(expectedColumn, column);
  }

  @Test
  void testGetCocWhereConditions_withDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    List<String> expectedConditions = emptyList();

    // When
    List<String> conditions = target.getCocWhereConditions(params);

    // Then
    assertEquals(expectedConditions, conditions);
  }

  @Test
  void testGetCocWhereConditions_withoutDataValueSet() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(nonDataValueSetEventQueryParams);

    Set<String> expectedConditions =
        Set.of(
            " length((case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end)) = 11 ",
            " length((case when 'filterE' = '' then 'catOption0E' else '' end || case when 'filterF' = '' then 'catOption0F' else '' end)) = 11 ");

    // When
    List<String> conditions = target.getCocWhereConditions(params);

    // Then (in any order)
    assertEquals(expectedConditions, new HashSet<>(conditions));
  }

  @Test
  void testGetColumnForWhereClause() {

    // Given
    EventQueryParams params =
        infoInitializer.getParamsWithDisaggregationInfo(dataValueSetEventQueryParams);

    String expectedColumn =
        "(case when 'filterA' = '' then 'catOption0A' else '' end || case when 'filterB' = '' then 'catOption0B' else '' end)";

    // When
    String column = target.getColumnForWhereClause(params, "CategoryId1");

    // Then
    assertEquals(expectedColumn, column);
  }
}
