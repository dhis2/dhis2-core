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
package org.hisp.dhis.analytics.event.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.EnrollmentWhereClauseBuilder.QueryItemsWhereClauseGenerator;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.db.sql.PostgreSqlAnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link EnrollmentWhereClauseBuilder}.
 *
 * @author DHIS2 Team
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentWhereClauseBuilderTest {

  @Mock private EventQueryParams params;

  @Mock private EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;

  @Mock private ProgramIndicatorService programIndicatorService;

  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;

  @Mock private QueryItemsWhereClauseGenerator queryItemsWhereClauseGenerator;

  private final AnalyticsSqlBuilder sqlBuilder = new PostgreSqlAnalyticsSqlBuilder();

  private EnrollmentWhereClauseBuilder builder;

  @BeforeEach
  void setUp() {

    builder =
        new EnrollmentWhereClauseBuilder(
            params,
            timeFieldSqlRenderer,
            sqlBuilder,
            programIndicatorService,
            piDisagQueryGenerator,
            columnName -> "ax.\"" + columnName + "\"",
            fields -> "coalesce(" + String.join(",", fields) + ")",
            queryItemsWhereClauseGenerator);
  }

  @Test
  @DisplayName("Should build empty WHERE clause when no conditions are added")
  void testEmptyWhereClause() {
    String result = builder.build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add period condition when time field SQL is not blank")
  void testWithPeriodCondition() {
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(params))
        .thenReturn("enrollmentdate >= '2023-01-01'");

    String result = builder.withPeriodCondition().build();

    assertTrue(result.contains("enrollmentdate >= '2023-01-01'"));
  }

  @Test
  @DisplayName("Should not add period condition when time field SQL is blank")
  void testWithPeriodConditionBlank() {
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(params)).thenReturn("");

    String result = builder.withPeriodCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add org unit condition for SELECTED mode")
  void testWithOrgUnitConditionSelected() {
    OrganisationUnit ou1 = createOrgUnit("OU1");
    OrganisationUnit ou2 = createOrgUnit("OU2");

    lenient().when(params.isOrganisationUnitMode(any())).thenReturn(false);
    when(params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)).thenReturn(true);
    when(params.getDimensionOrFilterItems(anyString())).thenReturn(List.of(ou1, ou2));

    String result = builder.withOrgUnitCondition().build();

    assertTrue(result.contains("ou in"));
    assertTrue(result.contains("'OU1'"));
    assertTrue(result.contains("'OU2'"));
  }

  @Test
  @DisplayName("Should add org unit condition for CHILDREN mode")
  void testWithOrgUnitConditionChildren() {
    OrganisationUnit child1 = createOrgUnit("CHILD1");
    OrganisationUnit child2 = createOrgUnit("CHILD2");

    lenient().when(params.isOrganisationUnitMode(any())).thenReturn(false);
    when(params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)).thenReturn(true);
    when(params.getOrganisationUnitChildren()).thenReturn(Sets.newHashSet(child1, child2));

    String result = builder.withOrgUnitCondition().build();

    assertTrue(result.contains("ou in"));
    assertTrue(result.contains("CHILD1"));
    assertTrue(result.contains("CHILD2"));
  }

  @Test
  @DisplayName("Should not add org unit condition when mode is not matched")
  void testWithOrgUnitConditionNoMode() {
    when(params.isOrganisationUnitMode(any())).thenReturn(false);

    String result = builder.withOrgUnitCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add category condition")
  void testWithCategoryCondition() {
    CategoryOption categoryOption = createCategoryOption("CO1", DataDimensionType.DISAGGREGATION);
    BaseDimensionalObject dimension =
        new BaseDimensionalObject(
            "categoryDim", DimensionType.CATEGORY, List.of((DimensionalItemObject) categoryOption));

    when(params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.CATEGORY)))
        .thenReturn(List.of(dimension));
    when(params.isPiDisagDimension(anyString())).thenReturn(false);

    String result = builder.withCategoryCondition().build();

    assertTrue(result.contains("ax.\"categoryDim\""));
    assertTrue(result.contains(" in "));
    assertTrue(result.contains("'CO1'"));
  }

  @Test
  @DisplayName("Should skip attribute category dimensions")
  void testWithCategoryConditionSkipsAttributeCategory() {
    CategoryOption categoryOption = createCategoryOption("CO1", DataDimensionType.ATTRIBUTE);
    BaseDimensionalObject dimension =
        new BaseDimensionalObject(
            "attrCategory",
            DimensionType.CATEGORY,
            List.of((DimensionalItemObject) categoryOption));

    when(params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.CATEGORY)))
        .thenReturn(List.of(dimension));

    String result = builder.withCategoryCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add org unit group set condition")
  void testWithOrgUnitGroupSetCondition() {
    BaseDimensionalItemObject ougs1 = createDimensionalItemObject("OUGS1");
    BaseDimensionalObject dimension =
        new BaseDimensionalObject(
            "ougsDim",
            DimensionType.ORGANISATION_UNIT_GROUP_SET,
            List.of((DimensionalItemObject) ougs1));

    when(params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.ORGANISATION_UNIT_GROUP_SET)))
        .thenReturn(List.of(dimension));

    String result = builder.withOrgUnitGroupSetCondition().build();

    assertTrue(result.contains("ax.\"ougsDim\""));
    assertTrue(result.contains(" in "));
    assertTrue(result.contains("'OUGS1'"));
  }

  @Test
  @DisplayName("Should add program stage condition")
  void testWithProgramStageCondition() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("PS123");

    when(params.hasProgramStage()).thenReturn(true);
    when(params.getProgramStage()).thenReturn(programStage);

    String result = builder.withProgramStageCondition().build();

    assertTrue(result.contains("ps = 'PS123'"));
  }

  @Test
  @DisplayName("Should not add program stage condition when not present")
  void testWithoutProgramStageCondition() {
    when(params.hasProgramStage()).thenReturn(false);

    String result = builder.withProgramStageCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add query items condition")
  void testWithQueryItemsCondition() {
    when(queryItemsWhereClauseGenerator.generate(params))
        .thenReturn("and column1 = 'value1' and column2 > 100");

    String result = builder.withQueryItemsCondition().build();

    assertTrue(result.contains("column1 = 'value1'"));
    assertTrue(result.contains("column2 > 100"));
  }

  @Test
  @DisplayName("Should not add query items condition when blank")
  void testWithQueryItemsConditionBlank() {
    when(queryItemsWhereClauseGenerator.generate(params)).thenReturn("");

    String result = builder.withQueryItemsCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add filter expression condition for program indicator")
  void testWithFilterExpressionCondition() {
    ProgramIndicator pi = new ProgramIndicator();
    pi.setFilter("#{programStageUid.dataElementUid} > 10");

    when(params.hasProgramIndicatorDimension()).thenReturn(true);
    when(params.getProgramIndicator()).thenReturn(pi);
    lenient()
        .when(programIndicatorService.getAnalyticsSql(anyString(), any(), any(), any(), any()))
        .thenReturn("(column_value > 10)");

    String result = builder.withFilterExpressionCondition().build();

    assertTrue(result.contains("column_value"));
  }

  @Test
  @DisplayName("Should not add filter expression when program indicator has no filter")
  void testWithoutFilterExpressionCondition() {
    ProgramIndicator pi = new ProgramIndicator();

    when(params.hasProgramIndicatorDimension()).thenReturn(true);
    when(params.getProgramIndicator()).thenReturn(pi);

    String result = builder.withFilterExpressionCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add enrollment status condition")
  void testWithEnrollmentStatusCondition() {
    when(params.hasEnrollmentStatuses()).thenReturn(true);
    when(params.getEnrollmentStatus())
        .thenReturn(Sets.newHashSet(EnrollmentStatus.ACTIVE, EnrollmentStatus.COMPLETED));

    String result = builder.withEnrollmentStatusCondition().build();

    assertTrue(result.contains("enrollmentstatus in"));
    assertTrue(result.contains("ACTIVE"));
    assertTrue(result.contains("COMPLETED"));
  }

  @Test
  @DisplayName("Should not add enrollment status condition when not present")
  void testWithoutEnrollmentStatusCondition() {
    when(params.hasEnrollmentStatuses()).thenReturn(false);

    String result = builder.withEnrollmentStatusCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add coordinates condition")
  void testWithCoordinatesCondition() {
    when(params.isCoordinatesOnly()).thenReturn(true);

    String result = builder.withCoordinatesCondition().build();

    assertTrue(result.contains("longitude is not null"));
    assertTrue(result.contains("latitude is not null"));
  }

  @Test
  @DisplayName("Should not add coordinates condition when not required")
  void testWithoutCoordinatesCondition() {
    when(params.isCoordinatesOnly()).thenReturn(false);

    String result = builder.withCoordinatesCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add geometry condition with coordinate fields")
  void testWithGeometryCondition() {
    when(params.isGeometryOnly()).thenReturn(true);
    when(params.getCoordinateFields()).thenReturn(List.of("field1", "field2"));

    String result = builder.withGeometryCondition().build();

    assertTrue(result.contains("coalesce(field1,field2) is not null"));
  }

  @Test
  @DisplayName("Should add geometry condition with default field when no coordinate fields")
  void testWithGeometryConditionDefaultField() {
    when(params.isGeometryOnly()).thenReturn(true);
    when(params.getCoordinateFields()).thenReturn(List.of());

    String result = builder.withGeometryCondition().build();

    assertTrue(result.contains("coalesce(enrollmentgeometry) is not null"));
  }

  @Test
  @DisplayName("Should not add geometry condition when not required")
  void testWithoutGeometryCondition() {
    when(params.isGeometryOnly()).thenReturn(false);

    String result = builder.withGeometryCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add completed condition")
  void testWithCompletedCondition() {
    when(params.isCompletedOnly()).thenReturn(true);

    String result = builder.withCompletedCondition().build();

    assertTrue(result.contains("completeddate is not null"));
  }

  @Test
  @DisplayName("Should not add completed condition when not required")
  void testWithoutCompletedCondition() {
    when(params.isCompletedOnly()).thenReturn(false);

    String result = builder.withCompletedCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should add bbox condition with coordinate fields")
  void testWithBboxCondition() {
    when(params.hasBbox()).thenReturn(true);
    when(params.getBbox()).thenReturn("10.0,20.0,30.0,40.0");
    when(params.getCoordinateFields()).thenReturn(List.of("field1"));

    String result = builder.withBboxCondition().build();

    assertTrue(result.contains("coalesce(field1)"));
    assertTrue(result.contains("ST_MakeEnvelope"));
    assertTrue(result.contains("10.0,20.0,30.0,40.0"));
    assertTrue(result.contains("4326"));
  }

  @Test
  @DisplayName("Should add bbox condition with default field when no coordinate fields")
  void testWithBboxConditionDefaultField() {
    when(params.hasBbox()).thenReturn(true);
    when(params.getBbox()).thenReturn("10.0,20.0,30.0,40.0");
    when(params.getCoordinateFields()).thenReturn(List.of());

    String result = builder.withBboxCondition().build();

    assertTrue(result.contains("coalesce(enrollmentgeometry)"));
    assertTrue(result.contains("ST_MakeEnvelope"));
  }

  @Test
  @DisplayName("Should not add bbox condition when not present")
  void testWithoutBboxCondition() {
    when(params.hasBbox()).thenReturn(false);

    String result = builder.withBboxCondition().build();

    assertEquals("", result);
  }

  @Test
  @DisplayName("Should combine multiple conditions with AND")
  void testCombineMultipleConditions() {
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(params))
        .thenReturn("enrollmentdate >= '2023-01-01'");
    when(params.isCompletedOnly()).thenReturn(true);
    when(params.hasEnrollmentStatuses()).thenReturn(true);
    when(params.getEnrollmentStatus()).thenReturn(Set.of(EnrollmentStatus.ACTIVE));

    String result =
        builder
            .withPeriodCondition()
            .withCompletedCondition()
            .withEnrollmentStatusCondition()
            .build();

    assertFalse(result.isEmpty());
    assertTrue(result.contains("enrollmentdate >= '2023-01-01'"));
    assertTrue(result.contains("completeddate is not null"));
    assertTrue(result.contains("enrollmentstatus in"));
  }

  @Test
  @DisplayName("Should build as Condition object")
  void testBuildAsCondition() {
    when(params.isCompletedOnly()).thenReturn(true);

    String conditionSql = builder.withCompletedCondition().buildAsCondition().toSql();

    assertTrue(conditionSql.contains("completeddate is not null"));
  }

  @Test
  @DisplayName("Should handle fluent API chaining")
  void testFluentApiChaining() {
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(params))
        .thenReturn("enrollmentdate >= '2023-01-01'");
    when(params.isCompletedOnly()).thenReturn(true);

    // This should not throw any exceptions and should return a valid SQL
    String result =
        builder
            .withPeriodCondition()
            .withOrgUnitCondition()
            .withCategoryCondition()
            .withOrgUnitGroupSetCondition()
            .withProgramStageCondition()
            .withQueryItemsCondition()
            .withFilterExpressionCondition()
            .withEnrollmentStatusCondition()
            .withCoordinatesCondition()
            .withGeometryCondition()
            .withCompletedCondition()
            .withBboxCondition()
            .build();

    assertFalse(result.isEmpty());
    assertTrue(result.contains("enrollmentdate"));
    assertTrue(result.contains("completeddate"));
  }

  // -------------------------------------------------------------------------
  // Helper methods
  // -------------------------------------------------------------------------

  private OrganisationUnit createOrgUnit(String uid) {
    OrganisationUnit ou = new OrganisationUnit();
    ou.setUid(uid);
    return ou;
  }

  private CategoryOption createCategoryOption(String uid, DataDimensionType dataDimensionType) {
    CategoryOption co = new CategoryOption();
    co.setUid(uid);

    Category category = new Category();
    category.setDataDimensionType(dataDimensionType);
    co.setCategories(Set.of(category));

    return co;
  }

  private BaseDimensionalItemObject createDimensionalItemObject(String uid) {
    BaseDimensionalItemObject item = new BaseDimensionalItemObject(uid);
    item.setUid(uid);
    return item;
  }
}
