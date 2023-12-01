/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.dataitem.query;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayShortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.shortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_FROM;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_LEFT_PARENTHESIS;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_RIGHT_PARENTHESIS;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.checkOwnerConditions;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of ExpressionDimensionItem
 * objects.
 */
@Slf4j
@Component
public class ExpressionDimensionItemQuery implements DataItemQuery {
  private static final List<String> COMMON_COLUMNS = new ArrayList<>();

  static {
    COMMON_COLUMNS.add("cast (null as text) as program_name, cast (null as text) as program_uid");
    COMMON_COLUMNS.add(
        "cast (null as text) as program_shortname, expressiondimensionitem.uid as item_uid, expressiondimensionitem.name as item_name");
    COMMON_COLUMNS.add(
        "expressiondimensionitem.shortname as item_shortname, cast (null as text) as item_valuetype, expressiondimensionitem.code as item_code");
    COMMON_COLUMNS.add(
        "expressiondimensionitem.sharing as item_sharing, cast (null as text) as item_domaintype, cast ('EXPRESSION_DIMENSION_ITEM' as text) as item_type");
    COMMON_COLUMNS.add("expressiondimensionitem.expression");
  }

  /**
   * Builds and returns the SQL statement required by the implementation.
   *
   * @param paramsMap
   * @return the full SQL statement
   */
  @Override
  public String getStatement(MapSqlParameterSource paramsMap) {
    StringBuilder sql = new StringBuilder();

    sql.append(SPACED_LEFT_PARENTHESIS);

    // Creating a temp translated table to be queried.
    sql.append(SPACED_SELECT + "*" + SPACED_FROM + SPACED_LEFT_PARENTHESIS);

    if (hasNonBlankStringPresence(paramsMap, LOCALE)) {
      // Selecting translated names.
      sql.append(selectRowsContainingTranslatedName());
    } else {
      // Retrieving all rows ignoring translation as no locale is defined.
      sql.append(selectAllRowsIgnoringAnyTranslation());
    }

    sql.append(
        " group by item_name, item_uid, item_code, item_sharing, item_shortname,"
            + " i18n_first_name, i18n_first_shortname, i18n_second_name, i18n_second_shortname, expression");

    // Closing the temp table.
    sql.append(SPACED_RIGHT_PARENTHESIS + " t");

    sql.append(SPACED_WHERE);

    // Applying filters, ordering and limits.

    // Mandatory filters. They do not respect the root junction filtering.
    sql.append(always(checkOwnerConditions("t.item_sharing")));

    // Optional filters, based on the current root junction.
    OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder(paramsMap);
    optionalFilters.append(ifSet(displayNameFiltering("t.i18n_first_name", paramsMap)));
    optionalFilters.append(ifSet(displayShortNameFiltering("t.i18n_first_shortname", paramsMap)));
    optionalFilters.append(ifSet(nameFiltering("t.item_name", paramsMap)));
    optionalFilters.append(ifSet(shortNameFiltering("t.item_shortname", paramsMap)));
    optionalFilters.append(ifSet(uidFiltering("t.item_uid", paramsMap)));
    sql.append(ifAny(optionalFilters.toString()));

    String identifiableStatement =
        identifiableTokenFiltering(
            "t.item_uid", "t.item_code", "t.i18n_first_name", null, paramsMap);

    if (isNotBlank(identifiableStatement)) {
      sql.append(rootJunction(paramsMap));
      sql.append(identifiableStatement);
    }

    sql.append(
        ifSet(
            ordering(
                "t.i18n_first_name, t.i18n_second_name, t.item_uid",
                "t.item_name, t.item_uid",
                "t.i18n_first_shortname, t.i18n_second_shortname, t.item_uid",
                "t.item_shortname, t.item_uid",
                paramsMap)));
    sql.append(ifSet(maxLimit(paramsMap)));
    sql.append(SPACED_RIGHT_PARENTHESIS);

    String fullStatement = sql.toString();

    log.trace("Full SQL: " + fullStatement);

    return fullStatement;
  }

  /**
   * Checks if the query rules match the required conditions so the query can be executed. This
   * implementation must return always true.
   *
   * @param paramsMap
   * @return true if matches, false otherwise
   */
  @Override
  public boolean matchQueryRules(MapSqlParameterSource paramsMap) {
    return true;
  }

  /**
   * Simply returns the entity associated with the respective interface/query implementation.
   *
   * @return the entity associated to the interface implementation
   */
  @Override
  public Class<? extends BaseIdentifiableObject> getRootEntity() {
    return QueryableDataItem.EXPRESSION_DIMENSION_ITEM.getEntity();
  }

  private String selectRowsContainingTranslatedName() {
    StringBuilder sql = new StringBuilder();

    sql.append(SPACED_SELECT)
        .append(String.join(", ", COMMON_COLUMNS))
        .append(translationNamesColumnsFor("expressiondimensionitem"));

    sql.append(" from expressiondimensionitem ")
        .append(translationNamesJoinsOn("expressiondimensionitem"));

    return sql.toString();
  }

  private String selectAllRowsIgnoringAnyTranslation() {
    return new StringBuilder()
        .append(SPACED_SELECT + String.join(", ", COMMON_COLUMNS))
        .append(
            ", expressiondimensionitem.name as i18n_first_name, cast (null as text) as i18n_second_name")
        .append(
            ", expressiondimensionitem.shortname as i18n_first_shortname, cast (null as text) as i18n_second_shortname")
        .append(" from expressiondimensionitem ")
        .toString();
  }
}
