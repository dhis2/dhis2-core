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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CteContext;
import org.hisp.dhis.analytics.event.data.programindicator.ctefactory.CteSqlFactoryRegistry;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;

@Slf4j
public class ProgramIndicatorPlaceholderUtils {

  private final CteSqlFactoryRegistry cteSqlFactoryRegistry;

  public ProgramIndicatorPlaceholderUtils(DataElementService dataElementService) {
    this.cteSqlFactoryRegistry = new CteSqlFactoryRegistry(dataElementService);
  }

  /**
   * Processes a raw SQL string, from a Program Indicator expression or filter, looking for specific
   * placeholders representing V{...} Variables that require fetching the latest value from an event
   * table (e.g., V{event_date}, V{creation_date}).
   *
   * <p>For each unique placeholder found (combination of column, program indicator, and offset):
   *
   * <ul>
   *   <li>Generates a unique key (e.g., "varcte_occurreddate_piUid_0").
   *   <li>If a CTE with this key doesn't already exist in the provided {@code cteContext}:
   *       <ul>
   *         <li>Constructs the SQL body for a "Value CTE". This CTE uses the `ROW_NUMBER()` window
   *             function partitioned by enrollment and ordered by `occurreddate` descending to
   *             select the enrollment, the target column's value (aliased as 'value'), and the row
   *             number ('rn'). It selects from the relevant event analytics table (e.g.,
   *             "analytics_event_programUid").
   *         <li>Adds this Value CTE definition to the {@code cteContext} using {@link
   *             CteContext#addVariableCte(String, String, String)}.
   *         <li>Retrieves the randomly generated alias for the new CTE.
   *         <li>Stores the mapping from the original placeholder string to the generated alias in
   *             the {@code variableAliasMap}.
   *       </ul>
   *   <li>Replaces the placeholder string in the original SQL with a reference to the corresponding
   *       CTE's value column (e.g., "generatedAlias.value").
   * </ul>
   *
   * <p>This process ensures that for each required latest event value, a single CTE is generated,
   * and all references to that value within the SQL string point to the same CTE via its alias. The
   * generated CTEs are self-contained and designed to be LEFT JOINed later by the main query
   * builder using the condition `ON cteAlias.enrollment = mainTableAlias.enrollment AND cteAlias.rn
   * = 1`.
   *
   * @param rawSql The raw SQL string (from PI expression or filter) potentially containing
   *     placeholders.
   * @param programIndicator The Program Indicator context, used to determine the event table name.
   * @param earliestStartDate Reporting start date (currently unused in CTE generation logic but
   *     kept for signature consistency).
   * @param latestDate Reporting end date (currently unused in CTE generation logic but kept for
   *     signature consistency).
   * @param cteContext The context object where generated Value CTE definitions will be added.
   * @param variableAliasMap A map to be populated with placeholder-to-alias mappings. This allows
   *     the caller (e.g., DefaultProgramIndicatorSubqueryBuilder) to know which aliases were
   *     generated and potentially construct LEFT JOINs later.
   * @param sqlBuilder The SqlBuilder instance used for database-specific quoting.
   * @return The processed SQL string with all recognized placeholders replaced by "alias.value"
   *     references. Returns the original {@code rawSql} if it's null or contains no placeholders.
   */
  public String processPlaceholdersAndGenerateVariableCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext,
      Map<String, String> variableAliasMap,
      SqlBuilder sqlBuilder) {

    return cteSqlFactoryRegistry
        .factoryFor(rawSql)
        .process(
            rawSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            variableAliasMap,
            sqlBuilder);
  }

  /**
   * Analyzes the program indicator filter string. For simple comparisons involving V{...} variables
   * it generates dedicated "Filter CTEs". It populates the filterAliases list and returns the
   * remaining parts of the filter string.
   *
   * @param programIndicator The program indicator.
   * @param cteContext The CTE context to add Filter CTEs to.
   * @param filterAliases An output list to be populated with aliases of generated Filter CTEs.
   * @param sqlBuilder The SqlBuilder instance for quoting.
   * @return The remaining filter string parts. Returns "" if the filter is null, empty, or fully
   *     processed into Filter CTEs.
   */
  public String analyzeFilterAndGenerateFilterCtes(
      ProgramIndicator programIndicator,
      CteContext cteContext,
      List<String> filterAliases,
      SqlBuilder sqlBuilder,
      Date earliestStartDate,
      Date latestDate) {
    if (!programIndicator.hasFilter() || StringUtils.isBlank(programIndicator.getFilter())) {
      return "";
    }

    String filter = programIndicator.getFilter();

    Map<String, String> aliasMap = new LinkedHashMap<>();

    String remaining =
        cteSqlFactoryRegistry
            .factoryFor(filter)
            .process(
                filter,
                programIndicator,
                earliestStartDate,
                latestDate,
                cteContext,
                aliasMap,
                sqlBuilder);
    aliasMap.values().stream()
        .filter(Objects::nonNull)
        .forEach(
            a -> {
              if (!filterAliases.contains(a)) filterAliases.add(a);
            });

    return remaining;
  }

  /**
   * Processes PS/DE placeholders, generates necessary CTEs, adds them to the context, and replaces
   * the placeholders with references to the CTE values.
   *
   * @param rawSql The raw SQL string potentially containing PS/DE placeholders.
   * @param programIndicator The Program Indicator context.
   * @param earliestStartDate Reporting start date for boundary calculations.
   * @param latestDate Reporting end date for boundary calculations.
   * @param cteContext The context object where generated PS/DE CTE definitions will be added.
   * @param psdeAliasMap A map to be populated with placeholder-string-to-alias mappings.
   * @param sqlBuilder The SqlBuilder instance used for database-specific quoting.
   * @return The processed SQL string with placeholders replaced.
   */
  public String processPsDePlaceholdersAndGenerateCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext,
      Map<String, String> psdeAliasMap,
      SqlBuilder sqlBuilder) {

    return cteSqlFactoryRegistry
        .factoryFor(rawSql)
        .process(
            rawSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            psdeAliasMap,
            sqlBuilder);
  }

  /**
   * Processes rich d2:function placeholders (__D2FUNC__(...)), generates necessary CTEs, adds them
   * to the context, and replaces the placeholders with references to the CTE values.
   *
   * @param rawSql The raw SQL string potentially containing rich d2 function placeholders.
   * @param programIndicator The Program Indicator context.
   * @param earliestStartDate Reporting start date for boundary calculations.
   * @param latestDate Reporting end date for boundary calculations.
   * @param cteContext The context object where generated CTE definitions will be added.
   * @param d2FunctionAliasMap Output map populated with placeholder-string-to-alias mappings.
   * @param sqlBuilder The SqlBuilder instance used for database-specific quoting.
   * @return The processed SQL string with placeholders replaced.
   */
  public String processD2FunctionPlaceholdersAndGenerateCtes(
      String rawSql,
      ProgramIndicator programIndicator,
      Date earliestStartDate,
      Date latestDate,
      CteContext cteContext,
      Map<String, String> d2FunctionAliasMap,
      SqlBuilder sqlBuilder) {

    return cteSqlFactoryRegistry
        .factoryFor(rawSql)
        .process(
            rawSql,
            programIndicator,
            earliestStartDate,
            latestDate,
            cteContext,
            d2FunctionAliasMap,
            sqlBuilder);
  }
}
