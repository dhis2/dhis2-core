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

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.analytics.DataType.BOOLEAN;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.programindicator.disag.PiDisagQueryGenerator;
import org.hisp.dhis.analytics.util.sql.Condition;
import org.hisp.dhis.analytics.util.sql.SimpleCondition;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FallbackCoordinateFieldType;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicatorService;

/**
 * Builder class for constructing WHERE clauses for enrollment analytics queries.
 *
 * <p>This builder provides a fluent API for constructing complex WHERE clauses by composing
 * individual conditions. Each method adds a specific type of condition based on the query
 * parameters.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * String whereClause = new EnrollmentWhereClauseBuilder(params, timeFieldSqlRenderer, ...)
 *     .withPeriodCondition()
 *     .withOrgUnitCondition()
 *     .withCategoryCondition()
 *     .build();
 * }</pre>
 *
 * @author DHIS2 Team
 */
public class EnrollmentWhereClauseBuilder {
  private final EventQueryParams params;
  private final EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer;
  private final AnalyticsSqlBuilder sqlBuilder;
  private final ProgramIndicatorService programIndicatorService;
  private final PiDisagQueryGenerator piDisagQueryGenerator;
  private final Function<String, String> quoteAliasFunction;
  private final Function<List<String>, String> getCoalesceFunction;
  private final QueryItemsWhereClauseGenerator queryItemsWhereClauseGenerator;

  private final List<Condition> conditions = new ArrayList<>();

  /**
   * Constructs a new EnrollmentWhereClauseBuilder.
   *
   * @param params the query parameters
   * @param timeFieldSqlRenderer renderer for time/period fields
   * @param sqlBuilder SQL builder for database-specific SQL generation
   * @param programIndicatorService service for program indicator operations
   * @param piDisagQueryGenerator generator for program indicator disaggregation queries
   * @param quoteAliasFunction function to quote column aliases
   * @param getCoalesceFunction function to generate COALESCE SQL
   * @param queryItemsWhereClauseGenerator generator for query items WHERE clauses
   */
  public EnrollmentWhereClauseBuilder(
      EventQueryParams params,
      EnrollmentTimeFieldSqlRenderer timeFieldSqlRenderer,
      AnalyticsSqlBuilder sqlBuilder,
      ProgramIndicatorService programIndicatorService,
      PiDisagQueryGenerator piDisagQueryGenerator,
      Function<String, String> quoteAliasFunction,
      Function<List<String>, String> getCoalesceFunction,
      QueryItemsWhereClauseGenerator queryItemsWhereClauseGenerator) {
    this.params = params;
    this.timeFieldSqlRenderer = timeFieldSqlRenderer;
    this.sqlBuilder = sqlBuilder;
    this.programIndicatorService = programIndicatorService;
    this.piDisagQueryGenerator = piDisagQueryGenerator;
    this.quoteAliasFunction = quoteAliasFunction;
    this.getCoalesceFunction = getCoalesceFunction;
    this.queryItemsWhereClauseGenerator = queryItemsWhereClauseGenerator;
  }

  /**
   * Adds period/time field condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withPeriodCondition() {
    String timeFieldSql = timeFieldSqlRenderer.renderPeriodTimeFieldSql(params);
    if (StringUtils.isNotBlank(timeFieldSql)) {
      conditions.add(Condition.raw(timeFieldSql));
    }
    return this;
  }

  /**
   * Adds organization unit condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withOrgUnitCondition() {
    buildOrgUnitCondition().ifPresent(conditions::add);
    return this;
  }

  /**
   * Adds category dimension conditions to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withCategoryCondition() {
    buildCategoryCondition().ifPresent(conditions::add);
    return this;
  }

  /**
   * Adds organization unit group set conditions to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withOrgUnitGroupSetCondition() {
    buildOrgUnitGroupSetCondition().ifPresent(conditions::add);
    return this;
  }

  /**
   * Adds program stage condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withProgramStageCondition() {
    if (params.hasProgramStage()) {
      conditions.add(new SimpleCondition("ps = '" + params.getProgramStage().getUid() + "'"));
    }
    return this;
  }

  /**
   * Adds query items and filters conditions to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withQueryItemsCondition() {
    String queryItemsClause = queryItemsWhereClauseGenerator.generate(params);
    if (StringUtils.isNotBlank(queryItemsClause)) {
      conditions.add(Condition.raw(queryItemsClause));
    }
    return this;
  }

  /**
   * Adds program indicator filter expression condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withFilterExpressionCondition() {
    if (params.hasProgramIndicatorDimension() && params.getProgramIndicator().hasFilter()) {
      String filter =
          programIndicatorService.getAnalyticsSql(
              params.getProgramIndicator().getFilter(),
              BOOLEAN,
              params.getProgramIndicator(),
              params.getEarliestStartDate(),
              params.getLatestEndDate());

      String sqlFilter = ExpressionUtils.asSql(filter);
      conditions.add(Condition.raw("(" + sqlFilter + ")"));
    }
    return this;
  }

  /**
   * Adds enrollment status condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withEnrollmentStatusCondition() {
    if (params.hasEnrollmentStatuses()) {
      String statusList =
          params.getEnrollmentStatus().stream()
              .map(p -> singleQuote(p.name()))
              .collect(joining(","));
      conditions.add(new SimpleCondition("enrollmentstatus in (" + statusList + ")"));
    }
    return this;
  }

  /**
   * Adds coordinates condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withCoordinatesCondition() {
    if (params.isCoordinatesOnly()) {
      conditions.add(new SimpleCondition("longitude is not null and latitude is not null"));
    }
    return this;
  }

  /**
   * Adds geometry condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withGeometryCondition() {
    if (params.isGeometryOnly()) {
      String geometryCondition =
          getCoalesceFunction.apply(List.of(params.getCoordinateFields().toArray(new String[0])))
              + " is not null";

      // Fallback to default geometry field if no coordinate fields specified
      if (params.getCoordinateFields().isEmpty()) {
        geometryCondition =
            getCoalesceFunction.apply(
                    List.of(FallbackCoordinateFieldType.ENROLLMENT_GEOMETRY.getValue()))
                + " is not null";
      }

      conditions.add(Condition.raw(geometryCondition));
    }
    return this;
  }

  /**
   * Adds completed date condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withCompletedCondition() {
    if (params.isCompletedOnly()) {
      conditions.add(new SimpleCondition("completeddate is not null"));
    }
    return this;
  }

  /**
   * Adds bounding box (bbox) condition to the WHERE clause.
   *
   * @return this builder for method chaining
   */
  public EnrollmentWhereClauseBuilder withBboxCondition() {
    if (params.hasBbox()) {
      String bboxColumn =
          getCoalesceFunction.apply(List.of(params.getCoordinateFields().toArray(new String[0])));

      // Fallback to default geometry field if no coordinate fields specified
      if (params.getCoordinateFields().isEmpty()) {
        bboxColumn =
            getCoalesceFunction.apply(
                List.of(FallbackCoordinateFieldType.ENROLLMENT_GEOMETRY.getValue()));
      }

      String bboxCondition = bboxColumn + " && ST_MakeEnvelope(" + params.getBbox() + ",4326)";
      conditions.add(Condition.raw(bboxCondition));
    }
    return this;
  }

  /**
   * Builds and returns the complete WHERE clause as a SQL string.
   *
   * @return the WHERE clause SQL string
   */
  public String build() {
    return Condition.and(conditions).toSql();
  }

  /**
   * Builds and returns the complete WHERE clause as a Condition object.
   *
   * <p>This is useful when the condition needs to be further composed with other conditions.
   *
   * @return the WHERE clause as a Condition
   */
  public Condition buildAsCondition() {
    return Condition.and(conditions);
  }

  // -------------------------------------------------------------------------
  // Private helper methods
  // -------------------------------------------------------------------------

  /**
   * Builds organization unit condition based on the selection mode.
   *
   * @return Optional containing the condition, or empty if not applicable
   */
  private Optional<Condition> buildOrgUnitCondition() {
    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)) {
      return buildSelectedOrgUnitCondition();
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
      return buildChildrenOrgUnitCondition();
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
      return buildDescendantsOrgUnitCondition();
    }
    return Optional.empty();
  }

  /**
   * Builds condition for SELECTED organization unit mode.
   *
   * @return Optional containing the condition
   */
  private Optional<Condition> buildSelectedOrgUnitCondition() {
    String uids =
        getQuotedCommaDelimitedString(getUids(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID)));
    return Optional.of(new SimpleCondition("ou in (" + uids + ")"));
  }

  /**
   * Builds condition for CHILDREN organization unit mode.
   *
   * @return Optional containing the condition
   */
  private Optional<Condition> buildChildrenOrgUnitCondition() {
    String uids = getQuotedCommaDelimitedString(getUids(params.getOrganisationUnitChildren()));
    return Optional.of(new SimpleCondition("ou in (" + uids + ")"));
  }

  /**
   * Builds condition for DESCENDANTS organization unit mode.
   *
   * @return Optional containing the condition
   */
  private Optional<Condition> buildDescendantsOrgUnitCondition() {
    List<Condition> unitConditions =
        params.getDimensionOrFilterItems(ORGUNIT_DIM_ID).stream()
            .map(obj -> (OrganisationUnit) obj)
            .map(this::buildOrgUnitLevelCondition)
            .collect(Collectors.toList());

    return unitConditions.isEmpty() ? Optional.empty() : Optional.of(Condition.or(unitConditions));
  }

  /**
   * Builds condition for a specific organization unit level.
   *
   * @param unit the organization unit
   * @return the condition for the organization unit level
   */
  private Condition buildOrgUnitLevelCondition(OrganisationUnit unit) {
    String column =
        params
            .getOrgUnitField()
            .withSqlBuilder(sqlBuilder)
            .getOrgUnitLevelCol(unit.getLevel(), AnalyticsType.ENROLLMENT);
    return new SimpleCondition(column + " = '" + unit.getUid() + "'");
  }

  /**
   * Builds category dimension conditions.
   *
   * @return Optional containing the combined category conditions
   */
  private Optional<Condition> buildCategoryCondition() {
    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.CATEGORY));

    List<Condition> categoryConditions =
        dynamicDimensions.stream()
            .filter(dim -> !isAttributeCategory(dim))
            .map(this::buildSingleCategoryCondition)
            .collect(Collectors.toList());

    return categoryConditions.isEmpty()
        ? Optional.empty()
        : Optional.of(Condition.and(categoryConditions));
  }

  /**
   * Builds a condition for a single category dimension.
   *
   * @param dim the dimensional object
   * @return the condition for the category dimension
   */
  private Condition buildSingleCategoryCondition(DimensionalObject dim) {
    String dimName = dim.getDimensionName();
    String col =
        params.isPiDisagDimension(dimName)
            ? piDisagQueryGenerator.getColumnForWhereClause(params, dimName)
            : quoteAliasFunction.apply(dimName);

    return new SimpleCondition(
        col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ")");
  }

  /**
   * Checks if a category dimension is an attribute category.
   *
   * @param categoryDim the category dimension to check
   * @return true if it's an attribute category, false otherwise
   */
  private boolean isAttributeCategory(DimensionalObject categoryDim) {
    return ((CategoryOption) categoryDim.getItems().get(0))
            .getCategories()
            .iterator()
            .next()
            .getDataDimensionType()
        == ATTRIBUTE;
  }

  /**
   * Builds organization unit group set conditions.
   *
   * @return Optional containing the combined OUGS conditions
   */
  private Optional<Condition> buildOrgUnitGroupSetCondition() {
    List<DimensionalObject> dynamicDimensions =
        params.getDimensionsAndFilters(Sets.newHashSet(DimensionType.ORGANISATION_UNIT_GROUP_SET));

    List<Condition> ougsConditions =
        dynamicDimensions.stream()
            .filter(dim -> !dim.isAllItems())
            .map(this::buildSingleOrgUnitGroupSetCondition)
            .collect(Collectors.toList());

    return ougsConditions.isEmpty() ? Optional.empty() : Optional.of(Condition.and(ougsConditions));
  }

  /**
   * Builds a condition for a single organization unit group set.
   *
   * @param dim the dimensional object
   * @return the condition for the OUGS
   */
  private Condition buildSingleOrgUnitGroupSetCondition(DimensionalObject dim) {
    String col = quoteAliasFunction.apply(dim.getDimensionName());
    return new SimpleCondition(
        col + " in (" + getQuotedCommaDelimitedString(getUids(dim.getItems())) + ")");
  }

  /**
   * Wraps a value in single quotes using the SQL builder.
   *
   * @param value the value to quote
   * @return the quoted value
   */
  private String singleQuote(String value) {
    return sqlBuilder.singleQuote(value);
  }

  /**
   * Functional interface for generating query items WHERE clause.
   *
   * <p>This allows the complex query items logic to be injected as a dependency.
   */
  @FunctionalInterface
  public interface QueryItemsWhereClauseGenerator {
    /**
     * Generates the WHERE clause for query items and filters.
     *
     * @param params the query parameters
     * @return the WHERE clause SQL string
     */
    String generate(EventQueryParams params);
  }
}
