/*
 * Copyright (c) 2004-2026, University of Oslo
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

import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.QueryOperator.IN;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.feedback.ErrorCode;
import org.springframework.stereotype.Service;

/**
 * Utility class for building SQL filter conditions from QueryItems in event and enrollment
 * analytics queries.
 *
 * <p>This class handles:
 *
 * <ul>
 *   <li>No-value filter semantics
 *   <li>Organisation unit keyword resolution (USER_ORGUNIT, etc.)
 *   <li>IN clause generation with proper quoting
 *   <li>Date/time format conversion
 * </ul>
 */
@RequiredArgsConstructor
@Service
public class QueryItemFilterBuilder {

  private final OrganisationUnitResolver organisationUnitResolver;
  private final AnalyticsSqlBuilder sqlBuilder;

  /**
   * Transforms the query item filters into an "and" separated SQL string. For instance, if the
   * query item has filters with values "a" and "b" and the operator is "eq", the resulting SQL
   * string will be "column = 'a' and column = 'b'". If the query item has no filters, an empty
   * string is returned.
   *
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @return the SQL string.
   */
  public String extractFiltersAsSql(QueryItem item, String columnName) {
    return item.getFilters().stream()
        .map(f -> buildFilterCondition(f, item, columnName, f.getFilter()))
        .collect(Collectors.joining(" and "));
  }

  /**
   * Transforms the query item filters into an "and" separated SQL string, resolving special
   * organisation unit keywords like USER_ORGUNIT. For instance, if the query item has filters with
   * values "a" and "b" and the operator is "eq", the resulting SQL string will be "column = 'a' and
   * column = 'b'". If the query item has no filters, an empty string is returned.
   *
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @param params the {@link EventQueryParams} used to resolve special org unit keywords.
   * @return the SQL string.
   */
  public String extractFiltersAsSql(QueryItem item, String columnName, EventQueryParams params) {
    return item.getFilters().stream()
        .map(
            f -> {
              boolean needsResolution = requiresOrgUnitResolution(item);
              String resolvedFilter =
                  needsResolution
                      ? organisationUnitResolver.resolveOrgUnits(f, params.getUserOrgUnits(), item)
                      : f.getFilter();

              return buildFilterCondition(f, item, columnName, resolvedFilter);
            })
        .collect(Collectors.joining(" and "));
  }

  /**
   * Checks if the query item requires organisation unit resolution.
   *
   * @param item the {@link QueryItem}.
   * @return true if the item is either a stage.ou dimension or a data element of type
   *     ORGANISATION_UNIT.
   */
  public boolean requiresOrgUnitResolution(QueryItem item) {
    // Data element of type ORGANISATION_UNIT (this catches stage.ou dimension as well
    // since it's created with ValueType.ORGANISATION_UNIT in DefaultQueryItemLocator)
    if (item.getValueType() == ValueType.ORGANISATION_UNIT) {
      return true;
    }
    // Also check the item ID/name for robustness (stage.ou dimension)
    return OrganisationUnitResolver.isStageOuDimension(item);
  }

  /**
   * Builds a SQL filter condition for the given filter, handling the no-value keyword for the IN
   * operator. For IN with only the keyword: generates IS NULL. For IN without it: generates a
   * standard IN clause. For a mix of the keyword and actual values: generates (column IN (...) OR
   * column IS NULL).
   *
   * @param queryFilter the {@link QueryFilter}.
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @param filterValue the filter value (may be resolved for org units).
   * @return the SQL condition string.
   */
  public String buildFilterCondition(
      QueryFilter queryFilter, QueryItem item, String columnName, String filterValue) {
    if (queryFilter.getOperator() == IN) {
      return buildInFilterCondition(item, columnName, filterValue);
    }

    String sqlFilterValue = getSqlFilterWithResolvedValue(queryFilter, item, filterValue);
    return "%s %s %s".formatted(columnName, queryFilter.getOperator().getValue(), sqlFilterValue);
  }

  /**
   * Builds a SQL IN filter condition, properly handling the no-value keyword.
   *
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @param filterValue the filter value.
   * @return the SQL IN condition with proper no-value handling.
   */
  public String buildInFilterCondition(QueryItem item, String columnName, String filterValue) {
    List<String> filterItems = QueryFilter.getFilterItems(filterValue);

    boolean hasNoValue = filterItems.stream().anyMatch(item::isNoValue);
    List<String> actualValues = filterItems.stream().filter(v -> !item.isNoValue(v)).toList();

    if (actualValues.isEmpty() && hasNoValue) {
      // Keyword only: generate IS NULL
      return "%s is null".formatted(columnName);
    } else if (!actualValues.isEmpty() && hasNoValue) {
      // Mixed: generate (column IN (...) OR column IS NULL)
      String inClause = buildInClause(item, columnName, actualValues);
      return "(%s or %s is null)".formatted(inClause, columnName);
    } else {
      // No keyword: standard IN clause
      return buildInClause(item, columnName, actualValues);
    }
  }

  /**
   * Builds a SQL IN clause for the given items.
   *
   * @param item the {@link QueryItem}.
   * @param columnName the column name.
   * @param values the list of values for the IN clause.
   * @return the SQL IN clause.
   */
  public String buildInClause(QueryItem item, String columnName, List<String> values) {
    String quotedValues =
        values.stream()
            .map(v -> item.isNumeric() ? v : "'" + sqlBuilder.escape(v) + "'")
            .collect(Collectors.joining(","));
    return "%s in (%s)".formatted(columnName, quotedValues);
  }

  /**
   * Checks if the item has filters that carry at least one actual value. No-value filters must NOT
   * be pushed into CTEs, because their semantics require checking whether the most recent event's
   * value is null, rather than finding events with null values.
   *
   * @param item the query item
   * @return true if the item has a filter with at least one actual value
   */
  public boolean hasActualValueFilter(QueryItem item) {
    if (!item.hasFilter()) {
      return false;
    }
    return item.getFilters().stream()
        .anyMatch(
            filter -> {
              List<String> filterItems = QueryFilter.getFilterItems(filter.getFilter());
              return filterItems.stream().anyMatch(v -> !item.isNoValue(v));
            });
  }

  /**
   * Extracts filters as SQL, skipping filters that only ask for the no-value keyword. For a filter
   * that mixes the keyword with actual values, only the actual values are included. No-value
   * filters stay in the WHERE clause rather than moving into the CTE.
   *
   * @param item the query item
   * @param columnName the column name
   * @param params the event query parameters
   * @return SQL conditions for the actual values only
   */
  public String extractActualValueFiltersAsSql(
      QueryItem item, String columnName, EventQueryParams params) {
    return item.getFilters().stream()
        .filter(f -> hasActualValues(f, item))
        .map(
            f -> {
              boolean needsResolution = requiresOrgUnitResolution(item);
              String resolvedFilter =
                  needsResolution
                      ? organisationUnitResolver.resolveOrgUnits(f, params.getUserOrgUnits(), item)
                      : f.getFilter();

              // For IN operator with mixed values, only include the actual values
              if (f.getOperator() == IN) {
                List<String> filterItems = QueryFilter.getFilterItems(resolvedFilter);
                List<String> actualValues =
                    filterItems.stream().filter(v -> !item.isNoValue(v)).toList();
                if (!actualValues.isEmpty()) {
                  return buildInClause(item, columnName, actualValues);
                }
                return "";
              }

              return buildFilterCondition(f, item, columnName, resolvedFilter);
            })
        .filter(s -> !s.isEmpty())
        .collect(Collectors.joining(" and "));
  }

  /**
   * Checks if a filter carries any actual value, i.e. any value other than the no-value keyword.
   *
   * @param filter the query filter
   * @param item the query item the filter belongs to, which selects the no-value keyword
   * @return true if the filter contains at least one actual value
   */
  public boolean hasActualValues(QueryFilter filter, QueryItem item) {
    List<String> filterItems = QueryFilter.getFilterItems(filter.getFilter());
    return filterItems.stream().anyMatch(v -> !item.isNoValue(v));
  }

  /**
   * Returns the SQL filter value using a pre-resolved filter string.
   *
   * @param queryFilter the {@link QueryFilter}.
   * @param item the {@link QueryItem}.
   * @param resolvedFilter the resolved filter value.
   * @return the SQL filter string.
   */
  private String getSqlFilterWithResolvedValue(
      QueryFilter queryFilter, QueryItem item, String resolvedFilter) {
    String filter = getFilter(resolvedFilter, item);
    return item.getSqlFilter(queryFilter, sqlBuilder.escape(filter), true);
  }

  /**
   * Returns a filter string, handling date/time format conversion.
   *
   * @param filter the filter string.
   * @param item the {@link QueryItem}.
   * @return a filter string.
   */
  private String getFilter(String filter, QueryItem item) {
    try {
      if (!item.isNoValue(filter) && item.getValueType() == ValueType.DATETIME) {
        return DateFormatUtils.format(
            DateUtils.parseDate(
                filter,
                // known formats
                "yyyy-MM-dd'T'HH.mm",
                "yyyy-MM-dd'T'HH.mm.ss"),
            // postgres format
            "yyyy-MM-dd HH:mm:ss");
      }
    } catch (ParseException pe) {
      throwIllegalQueryEx(ErrorCode.E7135, filter);
    }

    return filter;
  }
}
