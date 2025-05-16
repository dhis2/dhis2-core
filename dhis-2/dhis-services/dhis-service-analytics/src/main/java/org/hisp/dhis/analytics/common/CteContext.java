/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.common;

import static org.hisp.dhis.analytics.common.CteUtils.computeKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStage;

/**
 * This class collects the CTE definitions that are generated during the SQL generation process for
 * enrollment and event analytics. The CTE definitions are stored in a map where the key is the CTE
 * name and the value is the CTE definition (the SQL query). Each CteDefinition object contains
 * additional information such as the program stage UID and other identifiers that are used to
 * generate the SQL query.
 */
@Slf4j
public class CteContext {
  private final Map<String, CteDefinition> cteDefinitions = new LinkedHashMap<>();
  public static final String ENROLLMENT_AGGR_BASE = "enrollment_aggr_base";

  /** The type of analytics query being executed. This can be either EVENT or ENROLLMENT. */
  @Getter private final AnalyticsQueryType analyticsQueryType;

  public CteDefinition getDefinitionByItemUid(String itemUid) {
    return cteDefinitions.get(itemUid);
  }

  public CteContext(AnalyticsQueryType analyticsQueryType) {
    this.analyticsQueryType = analyticsQueryType;
  }

  /**
   * Adds a CTE definition to the context.
   *
   * @param programStage The program stage
   * @param item The query item
   * @param cteDefinition The CTE definition (the SQL query)
   * @param offset The calculated offset
   * @param isRowContext Whether the CTE is a row context
   */
  public void addCte(
      ProgramStage programStage,
      QueryItem item,
      String cteDefinition,
      int offset,
      boolean isRowContext) {
    String key = computeKey(item);
    if (cteDefinitions.containsKey(key)) {
      cteDefinitions.get(key).getOffsets().add(offset);
    } else {
      var cteDef =
          new CteDefinition(
              programStage.getUid(), item.getItemId(), cteDefinition, offset, isRowContext);
      cteDefinitions.put(key, cteDef);
    }
  }

  /**
   * Adds a aggregated base CTE definition to the context. An aggregated base CTE definition is used
   * when creating enrollment and events aggregated queries. The `enrollment_base` CTE isolates and
   * pre-filters the core enrollment data based on specific conditions such as enrollment date,
   * organization level, and demographic attributes. This improves query readability, avoids
   * redundant filtering in multiple query sections, and ensures consistent filtering criteria
   * across the query, especially when joining with event data later in the query.
   *
   * @param cteDefinition The CTE definition (the SQL query)
   * @param whereClauses The where clause that is used to filter the data (required for later
   *     processing)
   */
  public void addBaseAggregateCte(String cteDefinition, String whereClauses) {
    cteDefinitions.put(ENROLLMENT_AGGR_BASE, new CteDefinition(cteDefinition, whereClauses));
  }

  /**
   * Adds a special "exists" CTE definition to the context. This CTE definition is required when a
   * non-aggregated query has the flag "rowContext" set to true
   *
   * @param programStage the ProgramStage object
   * @param item the QueryItem object
   * @param cteDefinition the CTE definition (the SQL query)
   */
  public void addExistsCte(ProgramStage programStage, QueryItem item, String cteDefinition) {
    var cteDef =
        new CteDefinition(programStage.getUid(), item.getItemId(), cteDefinition, -999, false)
            .setExists(true);
    cteDefinitions.put(programStage.getUid(), cteDef);
  }

  /**
   * Adds a CTE definition to the context.
   *
   * @param programIndicator The program indicator
   * @param cteDefinition The CTE definition (the SQL query)
   * @param functionRequiresCoalesce Whether the function requires to be "wrapped" in coalesce to
   *     avoid null values (e.g. avg, sum)
   */
  public void addProgramIndicatorCte(
      ProgramIndicator programIndicator, String cteDefinition, boolean functionRequiresCoalesce) {
    cteDefinitions.put(
        programIndicator.getUid(),
        CteDefinition.forProgramIndicator(
            programIndicator.getUid(),
            programIndicator.getAnalyticsType(),
            cteDefinition,
            functionRequiresCoalesce));
  }

  /**
   * Adds a "Variable CTE" definition to the context. These CTEs are used to replace nested
   * subqueries originating from V{...} variables in Program Indicators.
   *
   * @param key A unique key identifying this variable CTE instance (e.g.,
   *     "varcte_column_piUid_offset").
   * @param cteDefinitionSql The SQL body for the CTE.
   * @param joinColumn The column to use when joining this CTE (e.g., "enrollment").
   */
  public void addVariableCte(String key, String cteDefinitionSql, String joinColumn) {
    CteDefinition cteDef = CteDefinition.forVariable(key, cteDefinitionSql, joinColumn);
    cteDefinitions.put(key, cteDef);
  }

  /**
   * Adds a Program Stage / Data Element CTE definition to the context. This method now directly
   * accepts a pre-constructed CteDefinition object.
   *
   * @param key The unique key identifying this PS/DE CTE instance.
   * @param cteDefinition The fully constructed CteDefinition object.
   */
  public void addProgramStageDataElementCte(String key, CteDefinition cteDefinition) {
    if (cteDefinition != null && key != null) {
      cteDefinitions.put(key, cteDefinition);
    } else {
      log.error("Attempted to add null key or CteDefinition to CteContext");
    }
  }

  /**
   * Adds a CTE definition to the context that represents a filter for a specific query item. The
   * name of the CTE is computed based on the query item.
   *
   * @param item the query item
   * @param cteDefinition the CTE definition (the SQL query)
   */
  public void addCteFilter(QueryItem item, String cteDefinition) {
    addCteFilter(computeKey(item), item, cteDefinition);
  }

  /**
   * Adds a CTE definition to the context that represents a filter for a specific query item.
   *
   * @param key the key of the CTE definition
   * @param item the query item
   * @param cteDefinition The CTE definition (the SQL query)
   */
  public void addCteFilter(String key, QueryItem item, String cteDefinition) {
    if (!cteDefinitions.containsKey(key)) {
      ProgramStage programStage = item.getProgramStage();
      cteDefinitions.put(
          key,
          CteDefinition.forFilter(
              item.getItemId(),
              programStage == null ? null : programStage.getUid(),
              cteDefinition));
    }
  }

  /**
   * Adds a generic "Filter CTE" definition to the context, typically generated from analyzing PI
   * filter strings.
   *
   * @param key A unique key identifying this filter CTE instance (e.g.,
   *     "filtercte_column_op_value_piUid").
   * @param cteDefinitionSql The SQL body for the CTE.
   */
  public void addFilterCte(String key, String cteDefinitionSql) {
    // Use the existing constructor for Filter CTEs, providing the key as the 'itemId'
    // and null for programStageUid, marking it as a filter.
    CteDefinition cteDef =
        CteDefinition.forFilter(
            key, null, cteDefinitionSql); // key -> itemId, null -> psUid, true -> isFilter
    cteDefinitions.put(key, cteDef);
  }

  /**
   * Adds a D2 Function CTE definition to the context.
   *
   * @param key The unique key identifying this D2 Function CTE instance.
   * @param cteDefinition The fully constructed CteDefinition object (should have type D2_FUNCTION).
   */
  public void addD2FunctionCte(String key, CteDefinition cteDefinition) {
    if (cteDefinition != null
        && key != null
        && cteDefinition.getCteType() == CteDefinition.CteType.D2_FUNCTION) {
      cteDefinitions.put(key, cteDefinition);
    } else {
      log.warn("Attempted to add invalid D2 Function CTE definition for key: {}", key);
    }
  }

  public CteDefinition getBaseAggregatedCte() {
    return cteDefinitions.get(ENROLLMENT_AGGR_BASE);
  }

  /**
   * Returns the CTE definitions as a map. The key is the CTE name and the value is the CTE
   * definition (the SQL query).
   *
   * @return the CTE definitions
   */
  public Map<String, String> getCteDefinitions() {
    return cteDefinitions.entrySet().stream()
        .collect(
            LinkedHashMap::new,
            (map, entry) -> {
              if (entry.getValue() != null) {
                map.put(entry.getKey(), entry.getValue().getCteDefinition());
              }
            },
            Map::putAll);
  }

  /**
   * Returns a map suitable for building the final SQL query's WITH clause. The map keys are the
   * short, generated aliases for the CTEs, and the values are the corresponding CTE definition SQL
   * bodies. Preserves insertion order.
   *
   * @return A map where key=short alias, value=cte definition Sql + cte type.
   */
  public Map<String, SqlWithCteType> getAliasAndDefinitionSqlMap() {
    Map<String, SqlWithCteType> aliasMap = new LinkedHashMap<>();
    for (Map.Entry<String, CteDefinition> entry : cteDefinitions.entrySet()) {
      CteDefinition definition = entry.getValue();
      if (definition != null) {
        String alias = useKeyAsAlias(definition) ? entry.getKey() : definition.getAlias();
        String definitionSql = definition.getCteDefinition();
        if (alias != null && definitionSql != null) {
          if (aliasMap.containsKey(alias)) {
            // This should be rare with random aliases, but log if it happens
            log.warn(
                "Duplicate CTE alias encountered: '{}'. Overwriting previous definition for key '{}' with definition for key '{}'.",
                alias,
                findKeyForAlias(alias),
                entry.getKey());
          }
          aliasMap.put(alias, new SqlWithCteType(definitionSql, definition.getCteType()));
        } else {
          log.warn("Skipping CTE with null alias or definition for key: {}", entry.getKey());
        }
      }
    }
    return aliasMap;
  }

  /**
   * Determines whether to use the key as the alias for the CTE definition. We do want to use the
   * key as the alias for program stages, program indicators, and filters. This simplifies the left
   * join logic.
   *
   * @param definition the CTE definition
   * @return true if the key should be used as the alias, false otherwise
   */
  private boolean useKeyAsAlias(CteDefinition definition) {
    return definition.isProgramStage() || definition.isProgramIndicator() || definition.isFilter();
  }

  private String findKeyForAlias(String alias) {
    for (Map.Entry<String, CteDefinition> entry : cteDefinitions.entrySet()) {
      if (entry.getValue() != null && alias.equals(entry.getValue().getAlias())) {
        return entry.getKey();
      }
    }
    return null; // Should not happen if called after alias exists
  }

  public Set<String> getCteKeys() {
    return cteDefinitions.keySet();
  }

  public Set<String> getCteKeysExcluding(String... exclude) {
    final List<String> toExclude = List.of(exclude);
    return cteDefinitions.keySet().stream()
        .filter(key -> !toExclude.contains(key))
        .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Determines if there are any CTE (Common Table Expression) definitions present in the context.
   *
   * @return true if there are CTE definitions, false otherwise
   */
  public boolean hasCteDefinitions() {
    return !cteDefinitions.isEmpty();
  }

  /**
   * Retrieves a CTE definition by its unique key.
   *
   * @param key the unique key of the CTE definition.
   * @return the CteDefinition or null if not found.
   */
  public CteDefinition getDefinitionByKey(String key) {
    return cteDefinitions.get(key);
  }

  /**
   * Checks if the analytics query type is for events.
   *
   * @return true if the query type is EVENT, false otherwise.
   */
  public boolean isEventsAnalytics() {
    return analyticsQueryType == AnalyticsQueryType.EVENT;
  }

  public boolean containsCte(String cteName) {
    return cteDefinitions.containsKey(cteName);
  }

  public record SqlWithCteType(String cteDefinitionSql, CteDefinition.CteType cteType) {}
}
