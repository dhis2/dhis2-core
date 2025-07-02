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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.EndpointItem;
import org.hisp.dhis.analytics.common.ProgramIndicatorSubqueryBuilder;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.setting.SystemSettingsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultProgramIndicatorSubqueryBuilder implements ProgramIndicatorSubqueryBuilder {
  private static final Map<AnalyticsType, AnalyticsTableType> ANALYTICS_TYPE_MAP =
      Map.of(
          AnalyticsType.EVENT, AnalyticsTableType.EVENT,
          AnalyticsType.ENROLLMENT, AnalyticsTableType.ENROLLMENT);

  private static final String SUBQUERY_TABLE_ALIAS = "subax";

  private final ProgramIndicatorService programIndicatorService;
  private final SystemSettingsService settingsService;
  private final SqlBuilder sqlBuilder;
  private final DataElementService dataElementService;
  private ProgramIndicatorPlaceholderUtils placeholderUtils;

  @PostConstruct
  public void init() {
    this.placeholderUtils = new ProgramIndicatorPlaceholderUtils(dataElementService);
  }

  @Override
  public String getAggregateClauseForProgramIndicator(
      ProgramIndicator pi, AnalyticsType outerSqlEntity, Date earliestStartDate, Date latestDate) {
    return getAggregateClauseForPIandRelationshipType(
        pi, null, outerSqlEntity, earliestStartDate, latestDate);
  }

  @Override
  public String getAggregateClauseForProgramIndicator(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate) {
    return getAggregateClauseForPIandRelationshipType(
        programIndicator, relationshipType, outerSqlEntity, earliestStartDate, latestDate);
  }

  @Override
  public void addCte(
      ProgramIndicator programIndicator,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext) {
    addCte(programIndicator, null, outerSqlEntity, earliestStartDate, latestDate, cteContext);
  }

  /**
   * Orchestrates the generation of the main Common Table Expression (CTE) for a given Program
   * Indicator and adds it, along with any necessary supporting value/function CTEs, to the provided
   * {@link CteContext}.
   *
   * <p>This method follows a multi-step process:
   *
   * <ol>
   *   <li><b>Filter Preprocessing:</b> Analyzes the Program Indicator's filter using {@link
   *       ProgramIndicatorPlaceholderUtils#analyzeFilterAndGenerateFilterCtes}. Simple filters
   *       (like {@code V{var} = 'literal'}) are converted into dedicated Filter CTEs (added to the
   *       {@code cteContext}) and joined later via INNER JOIN. The remaining complex filter parts
   *       are returned as a string.
   *   <li><b>Raw SQL Retrieval:</b> Fetches the initial SQL representations (containing
   *       placeholders like {@code V{...}}, {@code #{...}}, {@code d2:func(...)}) for both the main
   *       expression and the complex filter string by calling {@link
   *       #getProgramIndicatorSql(String, DataType, ProgramIndicator, Date, Date)}.
   *   <li><b>Placeholder Processing:</b> Sequentially processes the raw SQL strings using helper
   *       methods from {@link ProgramIndicatorPlaceholderUtils}:
   *       <ul>
   *         <li>{@link ProgramIndicatorPlaceholderUtils#processPlaceholdersAndGenerateVariableCtes}
   *             handles {@code V{...}} variables.
   *         <li>{@link ProgramIndicatorPlaceholderUtils#processPsDePlaceholdersAndGenerateCtes}
   *             handles {@code #{programStage.dataElement}} items.
   *         <li>{@link
   *             ProgramIndicatorPlaceholderUtils#processD2FunctionPlaceholdersAndGenerateCtes}
   *             handles {@code d2:func(...)} rich placeholders.
   *       </ul>
   *       Each processor identifies its relevant placeholders, generates the required supporting
   *       CTE definition (using specific logic like ROW_NUMBER or aggregation), adds the definition
   *       to the {@code cteContext}, and replaces the placeholder in the SQL string with a
   *       reference to the CTE's value (typically {@code coalesce(alias.value, default)}).
   *   <li><b>Join Clause Construction:</b>
   *       <ul>
   *         <li>Builds INNER JOIN clauses for the simple Filter CTEs identified in step 1.
   *         <li>Builds LEFT JOIN clauses for all value/function CTEs (Variable, PSDE, D2_Function)
   *             added to the {@code cteContext} during step 3, using the appropriate join
   *             conditions (e.g., matching row number 'rn' for PSDE/Variable CTEs).
   *       </ul>
   *   <li><b>WHERE Clause Construction:</b> Builds the WHERE clause for the main PI CTE using the
   *       fully processed complex filter string from step 3.
   *   <li><b>Main PI CTE Assembly:</b> Constructs the final SQL for the main Program Indicator CTE.
   *       This SQL selects the enrollment ID and calculates the indicator's value (applying the
   *       PI's aggregation function to the processed expression) by joining the base analytics
   *       table with the generated Filter (INNER) and Value/Function (LEFT) CTEs, applying the
   *       complex filter WHERE clause, and grouping by enrollment.
   *   <li><b>Context Registration:</b> Adds the assembled main PI CTE SQL definition to the {@code
   *       cteContext}, keyed by the Program Indicator's UID.
   * </ol>
   *
   * This approach ensures that complex sub-calculations within the Program Indicator are isolated
   * into reusable CTEs, improving query readability and avoiding issues with database limitations
   * on subquery nesting (like in Apache Doris).
   *
   * @param programIndicator The Program Indicator to generate a CTE for.
   * @param relationshipType The relationship type (if any) associated with the PI usage. (Currently
   *     unused in this CTE generation path but kept for interface compatibility).
   * @param outerSqlEntity The type of the outer query context (e.g., ENROLLMENT). (Currently unused
   *     in this CTE generation path).
   * @param earliestStartDate The start date for the reporting period.
   * @param latestDate The end date for the reporting period.
   * @param cteContext The shared context where all generated CTE definitions (supporting and main)
   *     are stored. This object is modified by this method.
   */
  @Override
  public void addCte(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext) {

    // 1. Pre-process Filter
    FilterProcessingResult filterResult =
        preprocessFilter(programIndicator, cteContext, earliestStartDate, latestDate);

    // 2. Get the raw SQL for the main expression of the Program Indicator
    String rawExpressionSql =
        getRawSqlForExpression(programIndicator, earliestStartDate, latestDate);
    String rawComplexFilterSql =
        getRawSqlForFilter(
            filterResult.complexFilterString(), programIndicator, earliestStartDate, latestDate);

    // 3. Process Placeholders & Populate Context
    ProcessedSql processedSql =
        processAllPlaceholders(
            rawExpressionSql,
            rawComplexFilterSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext);

    // 4. Build Join Clauses
    String innerJoinSql = buildInnerJoinsForFilters(filterResult.filterAliases(), cteContext);
    String leftJoinSql = buildLeftJoinsForAllValueCtes(cteContext);

    // 5. Build Where Clause
    String whereClause = buildWhereClause(processedSql.processedFilterSql());

    // 6. Assemble Main PI CTE SQL
    String mainCteSql =
        assembleMainPiCteSql(
            programIndicator,
            processedSql.processedExpressionSql(),
            innerJoinSql,
            leftJoinSql,
            whereClause,
            cteContext.getEndpointItem());

    // 7. Register Main PI CTE
    cteContext.addProgramIndicatorCte(
        programIndicator, mainCteSql, requireCoalesce(programIndicator));
  }

  private FilterProcessingResult preprocessFilter(
      ProgramIndicator programIndicator, CteContext cteContext, Date startDate, Date endDate) {
    List<String> filterAliases = new ArrayList<>();
    // Assuming static method calls for placeholder utils
    String complexFilterString =
        placeholderUtils.analyzeFilterAndGenerateFilterCtes(
            programIndicator, cteContext, filterAliases, sqlBuilder, startDate, endDate);
    return new FilterProcessingResult(filterAliases, complexFilterString);
  }

  private String getRawSqlForExpression(
      ProgramIndicator programIndicator, Date startDate, Date endDate) {
    return getProgramIndicatorSql(
        programIndicator.getExpression(), NUMERIC, programIndicator, startDate, endDate);
  }

  /** Retrieves the raw SQL for the complex filter string. */
  private String getRawSqlForFilter(
      String complexFilterString, ProgramIndicator programIndicator, Date startDate, Date endDate) {
    if (Strings.isNullOrEmpty(complexFilterString)) {
      return "";
    }
    return getProgramIndicatorSql(
        complexFilterString, BOOLEAN, programIndicator, startDate, endDate);
  }

  /**
   * Orchestrates the sequential processing of all placeholder types (V{}, #{}, d2:func). Populates
   * the CteContext with necessary supporting CTEs. Returns the final processed SQL strings.
   */
  private ProcessedSql processAllPlaceholders(
      String rawExpressionSql,
      String rawComplexFilterSql,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext) {

    // Internal alias maps - implementation detail of this method
    Map<String, String> variableAliasMap = new HashMap<>();
    Map<String, String> psdeAliasMap = new HashMap<>();
    Map<String, String> d2FunctionAliasMap = new HashMap<>();

    // Process V{...}
    String processedSql1 =
        placeholderUtils.processPlaceholdersAndGenerateVariableCtes(
            rawExpressionSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            variableAliasMap,
            sqlBuilder);
    String processedFilterSql1 =
        placeholderUtils.processPlaceholdersAndGenerateVariableCtes(
            rawComplexFilterSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            variableAliasMap,
            sqlBuilder);

    // Process #{...}
    String processedSql2 =
        placeholderUtils.processPsDePlaceholdersAndGenerateCtes(
            processedSql1,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            psdeAliasMap,
            sqlBuilder);
    String processedFilterSql2 =
        placeholderUtils.processPsDePlaceholdersAndGenerateCtes(
            processedFilterSql1,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            psdeAliasMap,
            sqlBuilder);

    // Process d2:func(...)
    String finalProcessedExpressionSql =
        placeholderUtils.processD2FunctionPlaceholdersAndGenerateCtes(
            processedSql2,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            d2FunctionAliasMap,
            sqlBuilder);
    String finalProcessedFilterSql =
        placeholderUtils.processD2FunctionPlaceholdersAndGenerateCtes(
            processedFilterSql2,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            d2FunctionAliasMap,
            sqlBuilder);

    return new ProcessedSql(finalProcessedExpressionSql, finalProcessedFilterSql);
  }

  /** Builds the INNER JOIN clause string for simple Filter CTEs. */
  private String buildInnerJoinsForFilters(List<String> filterAliases, CteContext cteContext) {
    if (filterAliases == null || filterAliases.isEmpty()) {
      return "";
    }
    return filterAliases.stream()
        .distinct()
        .map(
            alias -> {
              String key = findKeyForAlias(alias, cteContext);
              if (key == null) {
                log.error("Cannot generate INNER JOIN for unknown filter CTE alias: {}", alias);
                return null;
              }
              return String.format(
                  "inner join %s %s on %s.enrollment = %s.enrollment",
                  key, alias, alias, SUBQUERY_TABLE_ALIAS);
            })
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" "));
  }

  /** Builds the WHERE clause string from the processed complex filter SQL. */
  private String buildWhereClause(String finalProcessedFilterSql) {
    if (StringUtils.isNotBlank(finalProcessedFilterSql)) {
      return "where " + finalProcessedFilterSql;
    }
    return "";
  }

  /** Assembles the final SQL string for the main Program Indicator CTE definition. */
  private String assembleMainPiCteSql(
      ProgramIndicator programIndicator,
      String finalProcessedExpressionSql,
      String innerJoinSql,
      String leftJoinSql,
      String whereClause,
      EndpointItem endpointItem) {

    String function =
        TextUtils.emptyIfEqual(
            programIndicator.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue());
    String tableName = getTableName(programIndicator);

    // Ensure space separation between join/where clauses only if they exist
    String joinsAndWhere =
        Stream.of(innerJoinSql, leftJoinSql, whereClause)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(" "));

    if (requiresTableAlias(function, finalProcessedExpressionSql)) {
      finalProcessedExpressionSql = SUBQUERY_TABLE_ALIAS + "." + finalProcessedExpressionSql;
    }
    if (endpointItem == EndpointItem.ENROLLMENT) {
      return String.format(
          "select %s.enrollment, %s(%s) as value from %s as %s %s group by %s.enrollment",
          SUBQUERY_TABLE_ALIAS,
          function,
          finalProcessedExpressionSql,
          tableName,
          SUBQUERY_TABLE_ALIAS,
          joinsAndWhere,
          SUBQUERY_TABLE_ALIAS);
    } else {
      return String.format(
          "select %s(%s) as value from %s as %s %s",
          function, finalProcessedExpressionSql, tableName, SUBQUERY_TABLE_ALIAS, joinsAndWhere);
    }
  }

  /**
   * Builds the LEFT JOIN clause string for all relevant value-providing CTEs based on their type.
   * Reads definitions directly from the populated CteContext. Refactored for improved readability
   * and reduced cognitive complexity.
   *
   * @param cteContext The CTE context containing definitions.
   * @return The combined LEFT JOIN SQL string.
   */
  String buildLeftJoinsForAllValueCtes(CteContext cteContext) {
    List<String> joinClauses = new ArrayList<>();
    Set<String> joinedAliases = new HashSet<>(); // Track aliases to avoid duplicate joins

    for (String key : cteContext.getCteKeys()) {
      CteDefinition definition = cteContext.getDefinitionByKey(key);

      // Skip invalid definitions or those already handled
      if (definition == null || joinedAliases.contains(definition.getAlias())) {
        if (definition != null) {
          log.trace("Skipping duplicate/invalid join for alias: {}", definition.getAlias());
        }
        continue;
      }

      // Generate join based on type, skip if not a value CTE type
      String joinSql = generateJoinForCteDefinition(key, definition);

      if (joinSql != null) {
        joinClauses.add(joinSql);
        joinedAliases.add(definition.getAlias()); // Mark alias as joined
      }
    }

    return String.join(" ", joinClauses);
  }

  /**
   * Generates the specific LEFT JOIN SQL fragment for a given CTE definition, based on its type.
   * Returns null if the CTE type should not be joined here.
   *
   * @param key The CTE key.
   * @param definition The CTE definition.
   * @return The LEFT JOIN SQL string or null.
   */
  private String generateJoinForCteDefinition(String key, CteDefinition definition) {
    String alias = definition.getAlias();
    if (alias == null) {
      log.warn("Skipping join generation for definition with null alias, key: {}", key);
      return null;
    }

    return switch (definition.getCteType()) {
      case VARIABLE -> formatVariableJoin(alias);
      case PROGRAM_STAGE_DATE_ELEMENT -> formatPsdeJoin(key, alias, definition.getTargetRank());
      case D2_FUNCTION -> formatD2FunctionJoin(alias);
      default -> {
        log.trace(
            "Skipping join generation for non-value CTE type: {} with key: {}",
            definition.getCteType(),
            key);
        yield null;
      }
    };
  }

  /** Formats the LEFT JOIN for Variable CTEs (rn=1). */
  private String formatVariableJoin(String alias) {
    return String.format(
        "left join %s %s on %s.enrollment = %s.enrollment and %s.rn = 1",
        alias, alias, alias, SUBQUERY_TABLE_ALIAS, alias);
  }

  /** Formats the LEFT JOIN for PSDE CTEs (rn=targetRank). */
  private String formatPsdeJoin(String key, String alias, Integer targetRank) {
    if (targetRank != null) {
      return String.format(
          "left join %s %s on %s.enrollment = %s.enrollment and %s.rn = %d",
          alias, alias, alias, SUBQUERY_TABLE_ALIAS, alias, targetRank);
    } else {
      log.error(
          "PSDE CTE definition for key '{}' is missing targetRank. Cannot generate join.", key);
      return null; // Return null if rank is missing
    }
  }

  /** Formats the LEFT JOIN for D2 Function CTEs (no rn). */
  private String formatD2FunctionJoin(String alias) {
    return String.format(
        "left join %s %s on %s.enrollment = %s.enrollment",
        alias, alias, alias, SUBQUERY_TABLE_ALIAS);
  }

  private boolean requireCoalesce(ProgramIndicator programIndicator) {
    String function =
        TextUtils.emptyIfEqual(
            programIndicator.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue());
    return switch (function.toLowerCase()) {
      case "count", "sum", "min", "max" -> true;
      default -> false;
    };
  }

  private String getTableName(ProgramIndicator programIndicator) {
    return AnalyticsTable.getTableName(
        ANALYTICS_TYPE_MAP.get(programIndicator.getAnalyticsType()), programIndicator.getProgram());
  }

  private String getAggregateClauseForPIandRelationshipType(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate) {
    String function =
        TextUtils.emptyIfEqual(
            programIndicator.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue());
    String aggregateSql =
        getProgramIndicatorSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            earliestStartDate,
            latestDate);
    aggregateSql += ")";
    aggregateSql += getFrom(programIndicator);
    String where = getWhere(outerSqlEntity, programIndicator, relationshipType);
    aggregateSql += where;
    if (!Strings.isNullOrEmpty(programIndicator.getFilter())) {
      aggregateSql +=
          (where.isBlank() ? " WHERE " : " AND ")
              + "("
              + getProgramIndicatorSql(
                  programIndicator.getFilter(),
                  BOOLEAN,
                  programIndicator,
                  earliestStartDate,
                  latestDate)
              + ")";
    }
    return "(SELECT " + function + " (" + aggregateSql + ")";
  }

  private String getFrom(ProgramIndicator pi) {
    AnalyticsTableType tableType = ANALYTICS_TYPE_MAP.get(pi.getAnalyticsType());
    return " FROM "
        + AnalyticsTable.getTableName(tableType, pi.getProgram())
        + " as "
        + SUBQUERY_TABLE_ALIAS;
  }

  private String getWhere(
      AnalyticsType outerSqlEntity,
      ProgramIndicator programIndicator,
      RelationshipType relationshipType) {
    String condition = "";
    if (relationshipType != null) {
      condition =
          RelationshipTypeJoinGenerator.generate(
              SUBQUERY_TABLE_ALIAS, relationshipType, programIndicator.getAnalyticsType());
    } else {
      if (AnalyticsType.ENROLLMENT == outerSqlEntity) {
        condition = useExperimentalAnalyticsQueryEngine() ? "" : "enrollment = ax.enrollment";
      } else {
        if (AnalyticsType.EVENT == programIndicator.getAnalyticsType()) {
          condition = "event = ax.event";
        }
      }
    }
    return !condition.isEmpty() ? " WHERE " + condition : "";
  }

  private String getProgramIndicatorSql(
      String expression,
      DataType dataType,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate) {
    return this.programIndicatorService.getAnalyticsSql(
        expression,
        dataType,
        programIndicator,
        earliestStartDate,
        latestDate,
        SUBQUERY_TABLE_ALIAS);
  }

  protected boolean useExperimentalAnalyticsQueryEngine() {
    return this.settingsService.getCurrentSettings().getUseExperimentalAnalyticsQueryEngine();
  }

  private String findKeyForAlias(String alias, CteContext cteContext) {
    for (String key : cteContext.getCteKeys()) {
      CteDefinition definition = cteContext.getDefinitionByKey(key);
      if (definition != null && alias.equals(definition.getAlias())) {
        return key;
      }
    }
    log.warn("Could not find key for CTE alias: {}", alias);
    return null;
  }

  private boolean requiresTableAlias(String function, String expressionSql) {
    return function.equalsIgnoreCase("count") && expressionSql.equalsIgnoreCase("enrollment");
  }

  private record FilterProcessingResult(List<String> filterAliases, String complexFilterString) {}

  private record ProcessedSql(String processedExpressionSql, String processedFilterSql) {}
}
