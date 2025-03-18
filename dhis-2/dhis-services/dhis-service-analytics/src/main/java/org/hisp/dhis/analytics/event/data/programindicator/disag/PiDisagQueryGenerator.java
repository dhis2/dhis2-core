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
import static org.hisp.dhis.analytics.DataType.BOOLEAN;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Methods for adding SQL query components to disaggregate prgoram indicators
 *
 * @author Jim Grace
 */
@Component
@RequiredArgsConstructor
public class PiDisagQueryGenerator {

  private final ProgramIndicatorService programIndicatorService;

  private final SqlBuilder sqlBuilder;

  /**
   * Returns a list of additional SQL columns for the SELECT clause if needed for a disaggregated
   * {@link ProgramIndicator} to form the categoryOptionCombo and/or attributeOptionCombo. If
   * disaggregated categories are already included as query dimensions, they are excluded from this
   * list.
   *
   * <p>If no additional columns are needed, returns an empty list.
   *
   * @param params the {@link EventQueryParams}
   * @return additional SQL query SELECT columns if needed for PI disaggregation
   */
  public List<String> getCocSelectColumns(EventQueryParams params) {
    if (!params.hasPiDisagInfo()) {
      return emptyList();
    }

    return params.getPiDisagInfo().getCocCategories().stream()
        .map(id -> getColumnSqlWithAlias(params, id))
        .toList();
  }

  /**
   * Returns a list of additional SQL columns for the GROUP BY clause if needed for a disaggregated
   * {@link ProgramIndicator} to form the categoryOptionCombo and/or attributeOptionCombo. If
   * disaggregated categories are already included as query dimensions, they are excluded from this
   * list.
   *
   * <p>If no additional columns are needed, returns an empty list.
   *
   * @param params the {@link EventQueryParams}
   * @return additional SQL query GROUP BY columns if needed for PI disaggregation
   */
  public List<String> getCocColumnsForGroupBy(EventQueryParams params) {
    if (!params.hasPiDisagInfo()) {
      return emptyList();
    }

    return params.getPiDisagInfo().getCocCategories().stream().map(sqlBuilder::quote).toList();
  }

  /**
   * Returns the SQL for a dimension column that is dynamically generated, for either the SELECT or
   * GROUP BY clause. The dimension column may be either for a {@link Category} or a {@link
   * CategoryOptionGroupSet}
   *
   * @param params the {@link EventQueryParams}
   * @param dimension the dimension for which to generate the SQL column
   * @param isGroupByClause true if GROUP BY, false if SELECT
   * @return the SQL column
   */
  public String getColumnForSelectOrGroupBy(
      EventQueryParams params, String dimension, boolean isGroupByClause) {
    return isGroupByClause
        ? getColumnSql(params, dimension)
        : getColumnSqlWithAlias(params, dimension);
  }

  /**
   * Returns the SQL for a dimension column that is dynamically generated, for the WHERE clause. The
   * dimension column may be either for a {@link Category} or a {@link CategoryOptionGroupSet}
   *
   * @param params the {@link EventQueryParams}
   * @param dimension the dimension for which to generate the SQL column
   * @return the SQL column
   */
  public String getColumnForWhereClause(EventQueryParams params, String dimension) {
    return getColumnSql(params, dimension);
  }

  /** Constructs a SQL column expression with the Category UID as the alis */
  private String getColumnSqlWithAlias(EventQueryParams params, String dimension) {
    return getColumnSql(params, dimension) + " as " + sqlBuilder.quote(dimension);
  }

  private String getColumnSql(EventQueryParams params, String dimension) {
    PiDisagInfo info = params.getPiDisagInfo();
    Assert.notNull(info, "getColumn called but no PiDisagInfo");

    ProgramCategoryMapping mapping = info.categoryMappings.get(dimension);
    Assert.notNull(
        mapping,
        String.format(
            "Program indicator %s dimension %s not found",
            params.getProgramIndicator().getUid(), dimension));

    return getCategoryColumnSql(params, info.categoryMappings.get(dimension));
  }

  /**
   * Constructs a SQL column expression to find the category option for a {@link Category}. The SQL
   * has the form (in PostgreSql syntax):
   *
   * <pre>{@code
   * concat(
   * case when <filter01> then 'optionuid01' else '' end,
   * case when <filter02> then 'optionuid02' else '' end,
   * ...
   * )
   * }</pre>
   *
   * <p>If one option filter for the category is true, the column will return an 11-character UID
   * (the desired case). If no option filters are true, the column will return an empty string
   * (could be missing data or a malformed set of filters). If multiple option filters are true,
   * this is a logic error in defining the set of option filters.
   */
  private String getCategoryColumnSql(EventQueryParams params, ProgramCategoryMapping mapping) {
    List<String> options =
        mapping.getOptionMappings().stream().map(om -> getOptionSql(params, om)).toList();

    return sqlBuilder.concat(options);
  }

  /** Constructs a SQL ifThenElse expression to test for one category option. */
  private String getOptionSql(EventQueryParams params, ProgramCategoryOptionMapping optionMapping) {
    return sqlBuilder.ifThenElse(
        getFilterSql(params, optionMapping),
        sqlBuilder.singleQuote(optionMapping.getOptionId()),
        sqlBuilder.singleQuote(""));
  }

  /** Gets the filter SQL for a category option */
  private String getFilterSql(EventQueryParams params, ProgramCategoryOptionMapping optionMapping) {
    return programIndicatorService.getAnalyticsSqlAllowingNulls(
        optionMapping.getFilter(),
        BOOLEAN,
        params.getProgramIndicator(),
        params.getEarliestStartDate(),
        params.getLatestEndDate());
  }
}
