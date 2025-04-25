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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.common.CteDefinition;
import org.hisp.dhis.analytics.common.CteDefinition.CteType;
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
   * Adds the main Program Indicator CTE definition to the context, handling filter and expression
   * processing including placeholder substitution and CTE generation for V{...} variables.
   * Implements a hybrid approach: simple V{...} comparisons in the filter generate dedicated Filter
   * CTEs (joined via INNER JOIN), while V{...} used in the expression or complex filter parts
   * generate Value CTEs (joined via LEFT JOIN) and the complex filter logic is applied in the WHERE
   * clause of the main PI CTE.
   */
  @Override
  public void addCte(
      ProgramIndicator programIndicator,
      RelationshipType relationshipType,
      AnalyticsType outerSqlEntity,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext) {
    ProgramIndicatorPlaceholderUtils placeholderUtils =
        new ProgramIndicatorPlaceholderUtils(dataElementService);
    List<String> filterAliases = new ArrayList<>();
    // For V{...} CTEs
    Map<String, String> variableAliasMap = new HashMap<>();
    // For #{...} CTEs
    Map<String, String> psdeAliasMap = new HashMap<>();
    // For d2 function CTEs
    Map<String, String> d2FunctionAliasMap = new HashMap<>(); // Map for d2 func aliases

    String function =
        TextUtils.emptyIfEqual(
            programIndicator.getAggregationTypeFallback().getValue(),
            AggregationType.CUSTOM.getValue());

    // Analyze Filter & Generate Filter CTEs
    String complexFilterString =
        new ProgramIndicatorPlaceholderUtils(dataElementService)
            .analyzeFilterAndGenerateFilterCtes(
                programIndicator,
                cteContext,
                filterAliases,
                sqlBuilder,
                earliestStartDate,
                latestDate);

    //  Get raw SQL (with potential placeholders from expression items)
    String rawExpressionSql =
        getProgramIndicatorSql(
            programIndicator.getExpression(),
            NUMERIC,
            programIndicator,
            earliestStartDate,
            latestDate);
    String rawComplexFilterSql = "";
    if (!Strings.isNullOrEmpty(complexFilterString)) {
      rawComplexFilterSql =
          getProgramIndicatorSql(
              complexFilterString, BOOLEAN, programIndicator, earliestStartDate, latestDate);
    }

    // Process V{...} placeholders
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

    // Process #{...} placeholders
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

    // Process D2 Function placeholders
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

    // Construct INNER JOIN SQL for Filters
    String innerJoinSql =
        filterAliases.stream()
            .distinct() // Avoid joining the same filter CTE multiple times
            .map(
                alias -> {
                  String key = findKeyForAlias(alias, cteContext);
                  if (key == null) {
                    log.error(
                        "Cannot generate INNER JOIN for unknown filter CTE alias: {} for PI: {}",
                        alias,
                        programIndicator.getUid());
                    return "";
                  }
                  return String.format(
                      "inner join %s %s on %s.enrollment = %s.enrollment",
                      key, alias, alias, SUBQUERY_TABLE_ALIAS);
                })
            .filter(join -> !join.isEmpty())
            .collect(Collectors.joining(" "));

    // Construct LEFT JOIN SQL for Value CTEs
    String leftJoinSql = buildLeftJoinsForAllValueCtes(cteContext);

    // Construct WHERE Clause for Complex Filters
    String whereClause = "";
    if (!Strings.isNullOrEmpty(finalProcessedFilterSql)) {
      whereClause = "where " + finalProcessedFilterSql;
    }

    // Construct Main PI CTE SQL
    String cteSql =
        String.format(
            "select %s.enrollment, %s(%s) as value from %s as %s %s %s %s group by %s.enrollment",
            SUBQUERY_TABLE_ALIAS,
            function,
            finalProcessedExpressionSql,
            getTableName(programIndicator),
            SUBQUERY_TABLE_ALIAS,
            innerJoinSql,
            leftJoinSql,
            whereClause,
            SUBQUERY_TABLE_ALIAS);

    // Register Main PI CTE
    cteContext.addProgramIndicatorCte(programIndicator, cteSql, requireCoalesce(function));
  }

  /**
   * Builds the LEFT JOIN clause string for all relevant value-providing CTEs based on their type.
   *
   * @param cteContext The CTE context containing definitions.
   * @return The combined LEFT JOIN SQL string.
   */
  String buildLeftJoinsForAllValueCtes(CteContext cteContext) {
    List<String> joinClauses = new ArrayList<>();

    for (String key : cteContext.getCteKeys()) {
      CteDefinition definition = cteContext.getDefinitionByKey(key);

      if (definition == null) {
        log.warn("Skipping join generation for null definition with key: {}", key);
        continue;
      }

      // Skip types that shouldn't be joined here
      if (definition.getCteType() == CteType.FILTER
          || definition.getCteType() == CteType.PROGRAM_INDICATOR
          || definition.getCteType() == CteType.BASE_AGGREGATION
          || definition.getCteType()
              == CteType.PROGRAM_STAGE) { // Assuming PROGRAM_STAGE CTEs are handled differently
        continue;
      }

      String alias = definition.getAlias();
      if (alias == null) {
        log.warn("Skipping join generation for definition with null alias, key: {}", key);
        continue;
      }

      String joinSql = null;
      switch (definition.getCteType()) {
        case VARIABLE:
          // Variable CTEs (V{...}) always join on rn = 1
          joinSql =
              String.format(
                  "left join %s %s on %s.enrollment = %s.enrollment and %s.rn = 1",
                  key, alias, alias, SUBQUERY_TABLE_ALIAS, alias);
          break;
        case PSDE:
          // PSDE CTEs (#{...}) join on rn = targetRank
          Integer targetRank = definition.getTargetRank();
          if (targetRank != null) {
            joinSql =
                String.format(
                    "left join %s %s on %s.enrollment = %s.enrollment and %s.rn = %d",
                    key, alias, alias, SUBQUERY_TABLE_ALIAS, alias, targetRank);
          } else {
            log.error(
                "PSDE CTE definition for key '{}' is missing targetRank. Cannot generate join.",
                key);
          }
          break;

        case D2_FUNCTION:
          // D2 Function CTEs (count(*)... group by enrollment) join only on enrollment
          joinSql =
              String.format(
                  "left join %s %s on %s.enrollment = %s.enrollment",
                  key, alias, alias, SUBQUERY_TABLE_ALIAS);
          break;
        default:
          log.debug(
              "Skipping join generation for unhandled or non-joinable CTE type: {} with key: {}",
              definition.getCteType(),
              key);
          break;
      }

      if (joinSql != null) {
        joinClauses.add(joinSql);
      }
    }

    return String.join(" ", joinClauses);
  }

  private boolean requireCoalesce(String function) {
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
          condition = useExperimentalAnalyticsQueryEngine() ? "" : "event = ax.event";
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
}
