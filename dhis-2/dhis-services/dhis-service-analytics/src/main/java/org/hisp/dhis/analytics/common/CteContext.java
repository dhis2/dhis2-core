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
public class CteContext {
  private final Map<String, CteDefinition> cteDefinitions = new LinkedHashMap<>();
  public static final String ENROLLMENT_AGGR_BASE = "enrollment_aggr_base";

  public CteDefinition getDefinitionByItemUid(String itemUid) {
    return cteDefinitions.get(itemUid);
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
        new CteDefinition(programIndicator.getUid(), cteDefinition, functionRequiresCoalesce));
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
          new CteDefinition(
              item.getItemId(),
              programStage == null ? null : programStage.getUid(),
              cteDefinition,
              true));
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
            (map, entry) -> map.put(entry.getKey(), entry.getValue().getCteDefinition()),
            Map::putAll);
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

  public boolean containsCte(String cteName) {
    return cteDefinitions.containsKey(cteName);
  }
}
