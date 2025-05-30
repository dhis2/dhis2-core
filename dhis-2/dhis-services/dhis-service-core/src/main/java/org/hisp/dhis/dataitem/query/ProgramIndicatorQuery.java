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
package org.hisp.dhis.dataitem.query;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayShortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.shortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasValueTypePresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of ProgramIndicator
 * objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class ProgramIndicatorQuery implements DataItemQuery {
  private static final String COMMON_COLUMNS =
      List.of(
              Pair.of("program_name", CAST_NULL_AS_TEXT),
              Pair.of("program_uid", "program.uid"),
              Pair.of("program_shortname", CAST_NULL_AS_TEXT),
              Pair.of("item_uid", "programindicator.uid"),
              Pair.of("item_name", "programindicator.name"),
              Pair.of("item_shortname", "programindicator.shortname"),
              Pair.of("item_valuetype", CAST_NULL_AS_TEXT),
              Pair.of("item_code", "programindicator.code"),
              Pair.of("item_sharing", "programindicator.sharing"),
              Pair.of("item_domaintype", CAST_NULL_AS_TEXT),
              Pair.of("item_type", "cast ('PROGRAM_INDICATOR' as text)"),
              Pair.of("expression", CAST_NULL_AS_TEXT),
              Pair.of("optionset_uid", CAST_NULL_AS_TEXT),
              Pair.of("optionvalue_uid", CAST_NULL_AS_TEXT),
              Pair.of("optionvalue_name", CAST_NULL_AS_TEXT),
              Pair.of("optionvalue_code", CAST_NULL_AS_TEXT))
          .stream()
          .map(pair -> pair.getRight() + " as " + pair.getLeft())
          .collect(joining(", "));

  private static final String COMMON_UIDS = "program.uid, programindicator.uid";

  private static final String JOINS =
      "join program on program.programid = programindicator.programid";

  private static final String SPACED_FROM_PROGRAM_INDICATOR = " from programindicator ";

  @Override
  public String getStatement(MapSqlParameterSource paramsMap) {
    StringBuilder sql = new StringBuilder();

    sql.append("(");

    // Creating a temp translated table to be queried.
    sql.append(SPACED_SELECT + "distinct * from (");

    if (hasNonBlankStringPresence(paramsMap, LOCALE)) {
      // Selecting translated names.
      sql.append(selectRowsContainingTranslatedName());
    } else {
      // Retrieving all rows ignoring translation as no locale is defined.
      sql.append(selectAllRowsIgnoringAnyTranslation());
    }

    sql.append(
        " group by item_name, "
            + COMMON_UIDS
            + ", item_code, item_sharing, item_shortname,"
            + " i18n_first_name, i18n_first_shortname, i18n_second_name, i18n_second_shortname");

    // Closing the temp table.
    sql.append(" ) t");

    sql.append(SPACED_WHERE);

    // Applying filters, ordering and limits.

    // Mandatory filters. They do not respect the root junction filtering.
    sql.append(always(sharingConditions("t.item_sharing", READ_ACCESS, paramsMap)));

    // Optional filters, based on the current root junction.
    OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder(paramsMap);
    optionalFilters.append(ifSet(displayNameFiltering("t.i18n_first_name", paramsMap)));
    optionalFilters.append(ifSet(displayShortNameFiltering("t.i18n_first_shortname", paramsMap)));
    optionalFilters.append(ifSet(nameFiltering("t.item_name", paramsMap)));
    optionalFilters.append(ifSet(shortNameFiltering("t.item_shortname", paramsMap)));
    optionalFilters.append(ifSet(programIdFiltering("t.program_uid", paramsMap)));
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
                "t.i18n_first_name, t.item_uid",
                "t.item_name, t.item_uid",
                "t.i18n_first_shortname, t.item_uid",
                " t.item_shortname, t.item_uid",
                paramsMap)));
    sql.append(ifSet(maxLimit(paramsMap)));
    sql.append(")");

    String fullStatement = sql.toString();

    log.trace("Full SQL: " + fullStatement);

    return fullStatement;
  }

  /**
   * Very specific case, for Program Indicator objects, needed to handle filter by value type
   * NUMBER. When the value type filter does not have a NUMBER type, we should not execute this
   * query.
   *
   * @param paramsMap
   * @return true if rules are matched.
   */
  @Override
  public boolean matchQueryRules(MapSqlParameterSource paramsMap) {
    return hasValueTypePresence(paramsMap, NUMBER);
  }

  @Override
  public Class<? extends IdentifiableObject> getRootEntity() {
    return QueryableDataItem.PROGRAM_INDICATOR.getEntity();
  }

  private String selectRowsContainingTranslatedName() {
    return new StringBuilder()
        .append(SPACED_SELECT + COMMON_COLUMNS)
        .append(translationNamesColumnsFor("programindicator", false))
        .append(SPACED_FROM_PROGRAM_INDICATOR)
        .append(JOINS)
        .append(translationNamesJoinsOn("programindicator", false))
        .toString();
  }

  private String selectAllRowsIgnoringAnyTranslation() {
    return new StringBuilder()
        .append(SPACED_SELECT + COMMON_COLUMNS)
        .append(
            ", programindicator.name as i18n_first_name, cast (null as text) as i18n_second_name")
        .append(
            ", programindicator.shortname as i18n_first_shortname, cast (null as text) as i18n_second_shortname")
        .append(SPACED_FROM_PROGRAM_INDICATOR)
        .append(JOINS)
        .toString();
  }
}
