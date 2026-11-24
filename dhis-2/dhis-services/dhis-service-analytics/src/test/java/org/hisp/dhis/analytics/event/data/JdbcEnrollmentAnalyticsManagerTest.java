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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.test.TestBase.createCategory;
import static org.hisp.dhis.test.TestBase.createCategoryCombo;
import static org.hisp.dhis.test.TestBase.createCategoryOption;
import static org.hisp.dhis.test.TestBase.createProgram;
import static org.hisp.dhis.test.TestBase.createProgramStage;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.analytics.OrgUnitField;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagInfoInitializer;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.table.util.ColumnMapper;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.RequestTypeAware.EndpointItem;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.setting.SystemSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class JdbcEnrollmentAnalyticsManagerTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private ProgramIndicatorService programIndicatorService;
  @Mock private ProgramIndicatorSubqueryBuilder programIndicatorSubqueryBuilder;
  @Mock private PiDisagInfoInitializer piDisagInfoInitializer;
  @Mock private PiDisagQueryGenerator piDisagQueryGenerator;
  @Mock private EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;
  @Mock private ExecutionPlanStore executionPlanStore;
  @Mock private SystemSettingsService settingsService;
  @Mock private DhisConfigurationProvider config;
  @Mock private AnalyticsSqlBuilder sqlBuilder;
  @Mock private OrganisationUnitResolver organisationUnitResolver;
  @Mock private ColumnMapper columnMapper;

  @InjectMocks private JdbcEnrollmentAnalyticsManager jdbcEnrollmentAnalyticsManager;

  @Test
  void testAddEventPrefix() {
    String result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ST_Y(ax.geometry)");
    assertEquals("ST_Y(ax.geometry)", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("count() as value");
    assertEquals("count() as value", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ax.value as \"A03MvHHogjR\"");
    assertEquals("ax.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ev.value as \"A03MvHHogjR\"");
    assertEquals("ev.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("value as \"A03MvHHogjR\"");
    assertEquals("ax.value as \"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("\"A03MvHHogjR\"");
    assertEquals("ax.\"A03MvHHogjR\"", result);

    result = jdbcEnrollmentAnalyticsManager.addEnrollmentPrefix("ev.\"A03MvHHogjR\"");
    assertEquals("ev.\"A03MvHHogjR\"", result);
  }

  @Test
  void testGetDescendantsConditionSingleOrgUnit() {
    // Arrange
    EventQueryParams params = mock(EventQueryParams.class);
    SqlHelper hlp = mock(SqlHelper.class);

    OrganisationUnit ou = mock(OrganisationUnit.class);
    when(ou.getLevel()).thenReturn(2);
    when(ou.getUid()).thenReturn("UID123");

    List<DimensionalItemObject> orgUnits = List.of(ou);
    when(params.getDimensionOrFilterItems("ou")).thenReturn(orgUnits);

    OrgUnitField orgUnitField = mock(OrgUnitField.class);
    when(params.getOrgUnitField()).thenReturn(orgUnitField);
    when(orgUnitField.withSqlBuilder(any())).thenReturn(orgUnitField);
    when(orgUnitField.getOrgUnitLevelCol(eq(2), any())).thenReturn("level2");

    when(hlp.whereAnd()).thenReturn(" where ");

    // Act
    String condition = jdbcEnrollmentAnalyticsManager.getDescendantsCondition(params, hlp);

    // Assert
    assertEquals(" where  (level2 in ('UID123')) ", condition);
  }

  @Test
  void testGetDescendantsConditionMultipleLevels() {
    // Arrange
    EventQueryParams params = mock(EventQueryParams.class);
    SqlHelper hlp = mock(SqlHelper.class);

    OrganisationUnit ou1 = mock(OrganisationUnit.class);
    when(ou1.getLevel()).thenReturn(1);
    when(ou1.getUid()).thenReturn("OU1");

    OrganisationUnit ou2 = mock(OrganisationUnit.class);
    when(ou2.getLevel()).thenReturn(2);
    when(ou2.getUid()).thenReturn("OU2");
    when(params.getDimensionOrFilterItems("ou")).thenReturn(List.of(ou1, ou2));

    OrgUnitField orgUnitField = mock(OrgUnitField.class);
    when(params.getOrgUnitField()).thenReturn(orgUnitField);
    when(orgUnitField.withSqlBuilder(any())).thenReturn(orgUnitField);
    when(orgUnitField.getOrgUnitLevelCol(eq(1), any())).thenReturn("level1");
    when(orgUnitField.getOrgUnitLevelCol(eq(2), any())).thenReturn("level2");

    when(hlp.whereAnd()).thenReturn(" and ");

    // Act
    String condition = jdbcEnrollmentAnalyticsManager.getDescendantsCondition(params, hlp);

    // Assert
    assertEquals(" and  (level1 in ('OU1') or level2 in ('OU2')) ", condition);
  }

  @ParameterizedTest
  @MethodSource("dynamicDimensions")
  void testGetWhereClauseIncludesDynamicDimension(DimensionalObject dimension, String expected) {
    // Arrange
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");
    when(sqlBuilder.quoteAx(anyString()))
        .thenAnswer(invocation -> "ax." + invocation.getArgument(0, String.class));

    EventQueryParams params = enrollmentQuery().addDimension(dimension).build();

    // Act
    String whereClause = jdbcEnrollmentAnalyticsManager.getWhereClause(params);

    // Assert
    assertEquals(expected, whereClause);
  }

  static Stream<Arguments> dynamicDimensions() {
    return Stream.of(
        Arguments.of(
            categoryDimension(ENROLLMENT_CATEGORY), "and ax.enrCatUid01 in ('enrOptUid1') "),
        Arguments.of(programStatusDimension("ACTIVE"), "and ax.enrollmentstatus in ('ACTIVE') "));
  }

  @Test
  void testGetWhereClauseSkipsProgramStatusDimensionWithoutItems() {
    // Arrange: a PROGRAM_STATUS dimension without items means group-by only, and must not
    // contribute an IN-list filter to the where clause.
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");

    EventQueryParams params = enrollmentQuery().addDimension(programStatusDimension()).build();

    // Act
    String whereClause = jdbcEnrollmentAnalyticsManager.getWhereClause(params);

    // Assert
    assertEquals("", whereClause);
  }

  @Test
  void testGetWhereClauseLetsDefaultAttributeOptionComboPassCategoryConstraint() {
    // Arrange: a user category constraint must not hide enrollments with the default attribute
    // option combo, which have null in the category columns.
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");
    when(sqlBuilder.quoteAx(anyString()))
        .thenAnswer(invocation -> "ax." + invocation.getArgument(0, String.class));

    EventQueryParams params =
        enrollmentQuery()
            .addFilter(categoryDimension(ENROLLMENT_CATEGORY))
            .addCategoryConstraintDimension(ENROLLMENT_CATEGORY.getUid())
            .build();

    // Act
    String whereClause = jdbcEnrollmentAnalyticsManager.getWhereClause(params);

    // Assert
    assertEquals("and (ax.enrCatUid01 in ('enrOptUid1') or ax.enrCatUid01 is null) ", whereClause);
  }

  @Test
  void testGetWhereClauseSkipsAttributeCategoryWithoutEnrollmentColumn() {
    // Arrange: the category belongs to the event category combo of the program, so the enrollment
    // table has no column for it, as for a user category constraint on such a category.
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");

    EventQueryParams params =
        enrollmentQuery()
            .addFilter(categoryDimension(EVENT_CATEGORY))
            .addCategoryConstraintDimension(EVENT_CATEGORY.getUid())
            .build();

    // Act
    String whereClause = jdbcEnrollmentAnalyticsManager.getWhereClause(params);

    // Assert
    assertEquals("", whereClause);
  }

  @Test
  void testGetWhereClauseRejectsStageScopedCategory() {
    // Arrange: the enrollment table has no program stage column, so a category scoped to a
    // program stage cannot be applied.
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");

    ProgramStage stage = createProgramStage('A', 0);
    BaseDimensionalObject stageCategoryDimension = categoryDimension(ENROLLMENT_CATEGORY);
    stageCategoryDimension.setProgramStage(stage);

    EventQueryParams params = enrollmentQuery().addDimension(stageCategoryDimension).build();

    // Act
    IllegalQueryException ex =
        assertThrows(
            IllegalQueryException.class,
            () -> jdbcEnrollmentAnalyticsManager.getWhereClause(params));

    // Assert
    assertEquals(ErrorCode.E7271, ex.getErrorCode());
  }

  @Test
  void testGetWhereClauseOnlyProcessesCategoryAndProgramStatusAsDynamicDimensions() {
    // Arrange: dimensions of types other than CATEGORY, CATEGORY_OPTION_GROUP_SET and
    // PROGRAM_STATUS must not be picked up by this part of the where clause, even when combined
    // with dimensions of the types that are.
    when(timeFieldSqlRenderer.renderPeriodTimeFieldSql(any())).thenReturn("");
    when(sqlBuilder.quoteAx(anyString()))
        .thenAnswer(invocation -> "ax." + invocation.getArgument(0, String.class));

    // Not part of the CATEGORY / CATEGORY_OPTION_GROUP_SET / PROGRAM_STATUS set, and not handled
    // anywhere else in getWhereClause, so it must be silently ignored.
    DimensionalObject unrelatedDimension =
        new BaseDimensionalObject(
            "degsUid001",
            DimensionType.DATA_ELEMENT_GROUP_SET,
            List.of(new BaseDimensionalItemObject("degsOptUid1")));

    EventQueryParams params =
        enrollmentQuery()
            .addDimension(categoryDimension(ENROLLMENT_CATEGORY))
            .addDimension(programStatusDimension("ACTIVE"))
            .addDimension(unrelatedDimension)
            .build();

    // Act
    String whereClause = jdbcEnrollmentAnalyticsManager.getWhereClause(params);

    // Assert
    assertEquals(
        "and ax.enrCatUid01 in ('enrOptUid1') and ax.enrollmentstatus in ('ACTIVE') ", whereClause);
    assertFalse(whereClause.contains("degsUid001"));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Attribute category of the enrollment category combo of the program. */
  private static final Category ENROLLMENT_CATEGORY =
      attributeCategory("enrCatUid01", createCategoryOption("Enrollment option", "enrOptUid1"));

  /** Attribute category of the (event) category combo of the program. */
  private static final Category EVENT_CATEGORY =
      attributeCategory("evtCatUid01", createCategoryOption("Event option", "evtOptUid1"));

  private static Category attributeCategory(String uid, CategoryOption option) {
    Category category = createCategory("Category " + uid, uid, option);
    category.setDataDimensionType(DataDimensionType.ATTRIBUTE);
    option.getCategories().add(category);
    return category;
  }

  private static EventQueryParams.Builder enrollmentQuery() {
    Program program = createProgram('A');
    program.setCategoryCombo(createCategoryCombo('A', EVENT_CATEGORY));
    program.setEnrollmentCategoryCombo(createCategoryCombo('B', ENROLLMENT_CATEGORY));

    return new EventQueryParams.Builder()
        .withEndpointItem(EndpointItem.ENROLLMENT)
        .withProgram(program);
  }

  private static BaseDimensionalObject categoryDimension(Category category) {
    return new BaseDimensionalObject(
        category.getUid(), DimensionType.CATEGORY, List.copyOf(category.getCategoryOptions()));
  }

  private static DimensionalObject programStatusDimension(String... statuses) {
    return new BaseDimensionalObject(
        "programstatus",
        DimensionType.PROGRAM_STATUS,
        "enrollmentstatus",
        "Program status",
        Stream.of(statuses).<DimensionalItemObject>map(BaseDimensionalItemObject::new).toList());
  }
}
