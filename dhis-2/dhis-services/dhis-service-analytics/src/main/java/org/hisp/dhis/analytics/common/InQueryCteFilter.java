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
package org.hisp.dhis.analytics.common;

import static org.hisp.dhis.analytics.QueryKey.NV;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.QueryFilter;

/** Mimics the logic of {@link org.hisp.dhis.common.InQueryFilter} to be used in CTEs */
public class InQueryCteFilter {

  private final String filter;

  private final CteDefinition cteDefinition;

  private final String field;

  private final boolean isText;

  public InQueryCteFilter(
      String field, String encodedFilter, boolean isText, CteDefinition cteDefinition) {
    this.filter = encodedFilter;
    this.field = field;
    this.isText = isText;
    this.cteDefinition = cteDefinition;
  }

  /**
   * Generates a "IN" SQL filter condition based on the configured filter parameters and CTE (Common
   * Table Expression) definition. The method handles three main scenarios:
   *
   * <p>1. Regular values (non-missing): - For numeric fields: generates "alias_offset.field in
   * (value1,value2,...)" - For text fields: generates "alias_offset.field in
   * ('value1','value2',...)"
   *
   * <p>2. Missing values only (NV): - Generates "alias_offset.enrollment is not null and
   * alias_offset.field is null"
   *
   * <p>3. Mixed values (regular values and NV): - Currently throws UnsupportedOperationException
   *
   * @param offset An integer value used to generate the unique CTE alias in the form
   *     "alias_offset". This allows for multiple references to the same CTE in different parts of
   *     the query. This value can be null to signal that there is no need for an offset index.
   * @return A String containing the SQL filter condition. The exact format depends on the filter
   *     type: - For regular values with numeric field: "alias_0.field in (10,11,12)" - For regular
   *     values with text field: "alias_0.field in ('value1','value2')" - For missing values (NV):
   *     "alias_0.enrollment is not null and alias_0.field is null"
   * @throws UnsupportedOperationException when the filter contains both regular values and NV
   *     (missing values)
   * @see org.hisp.dhis.analytics.QueryKey#NV
   * @see CteDefinition#getAlias(int)
   *     <p>Example usage:
   *     <pre>
   * // For numeric fields
   * InQueryCteFilter filter1 = new InQueryCteFilter("age", "10;11;12", false, cteDef);
   * String sql1 = filter1.getSqlFilter(0); // Returns: "alias_0.age in (10,11,12)"
   *
   * // For text fields
   * InQueryCteFilter filter2 = new InQueryCteFilter("name", "john;jack", true, cteDef);
   * String sql2 = filter2.getSqlFilter(1); // Returns: "alias_1.name in ('john','jack')"
   *
   * // For missing values
   * InQueryCteFilter filter3 = new InQueryCteFilter("name", "NV", true, cteDef);
   * String sql3 = filter3.getSqlFilter(0); // Returns: "alias_0.enrollment is not null and alias_0.name is null"
   * </pre>
   */
  public String getSqlFilter(Integer offset) {
    List<String> filterItems = QueryFilter.getFilterItems(this.filter);

    StringBuilder condition = new StringBuilder();
    String alias = offset == null ? cteDefinition.getAlias() : cteDefinition.getAlias(offset);
    if (hasNonMissingValue(filterItems)) {
      condition
          .append("%s.%s in".formatted(alias, field))
          .append(
              streamOfNonMissingValues(filterItems)
                  .filter(Objects::nonNull)
                  .map(String::trim)
                  .map(this::quoteIfNecessary)
                  .collect(Collectors.joining(",", " (", ")")));
      if (hasMissingValue(filterItems)) {
        throw new UnsupportedOperationException("Not implemented yet");
        // TODO GIUSEPPE!
      }
    } else {
      if (hasMissingValue(filterItems)) {
        condition.append("%s.enrollment is not null".formatted(alias));
        condition.append(" and ");
        condition.append("%s.%s is null".formatted(alias, field));
      }
    }

    return condition.toString();
  }

  /**
   * Checks if the filter items contain any non-missing values (values that are not {@link
   * org.hisp.dhis.analytics.QueryKey#NV}). Non-missing values represent actual values that should
   * be included in the SQL IN clause. This method is used to determine if the generated SQL
   * condition needs to include an IN clause.
   *
   * @param filterItems the list of filter items to check for non-missing values
   * @return true if any item in the list is not equal to {@link
   *     org.hisp.dhis.analytics.QueryKey#NV}, indicating at least one actual value that should be
   *     included in the SQL IN clause; false if all values are missing
   */
  private boolean hasNonMissingValue(List<String> filterItems) {
    return anyMatch(filterItems, this::isNotMissingItem);
  }

  private boolean isNotMissingItem(String filterItem) {
    return !isMissingItem(filterItem);
  }

  private boolean isMissingItem(String filterItem) {
    return NV.equals(filterItem);
  }

  /**
   * Checks if any item in the list matches the given predicate.
   *
   * @param filterItems the list of items to check
   * @param predi the predicate to test against
   * @return true if any item matches the predicate, false otherwise
   */
  private boolean anyMatch(List<String> filterItems, Predicate<String> predi) {
    return filterItems.stream().anyMatch(predi);
  }

  /**
   * Checks if the filter items contain any missing values represented by the special marker {@link
   * org.hisp.dhis.analytics.QueryKey#NV}. Missing values indicate that the corresponding database
   * field should be treated as NULL in the SQL query. This method is used to determine if the
   * generated SQL condition needs to include an IS NULL clause.
   *
   * @param filterItems the list of filter items to check for missing values
   * @return true if any item in the list equals{@link org.hisp.dhis.analytics.QueryKey#NV},
   *     indicating a missing value that should be treated as NULL in the SQL query; false otherwise
   * @see org.hisp.dhis.analytics.QueryKey#NV
   */
  private boolean hasMissingValue(List<String> filterItems) {
    return anyMatch(filterItems, this::isMissingItem);
  }

  private Stream<String> streamOfNonMissingValues(List<String> filterItems) {
    return filterItems.stream().filter(this::isNotMissingItem);
  }

  private String quoteIfNecessary(String item) {
    return isText ? quote(item) : item;
  }

  protected String quote(String filterItem) {
    // Make sure to escape single quotes in the filter item
    return "'" + filterItem.replace("'", "''") + "'";
  }
}
