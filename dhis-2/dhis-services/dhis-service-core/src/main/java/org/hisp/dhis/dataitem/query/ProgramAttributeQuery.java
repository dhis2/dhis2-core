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
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.programIdFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.shortNameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.valueTypeFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesColumnsFor;
import static org.hisp.dhis.dataitem.query.shared.NameTranslationStatement.translationNamesJoinsOn;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasNonBlankStringPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of
 * ProgramTrackedEntityAttributeDimensionItem objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class ProgramAttributeQuery implements DataItemQuery {
  private static final String COMMON_COLUMNS =
      "program.name as program_name, program.uid as program_uid,"
          + " program.shortname as program_shortname, trackedentityattribute.uid as item_uid,"
          + " trackedentityattribute.name as item_name, trackedentityattribute.shortname as item_shortname,"
          + " trackedentityattribute.valuetype as item_valuetype, trackedentityattribute.code as item_code,"
          + " trackedentityattribute.sharing as item_sharing, cast (null as text) as item_domaintype,"
          + " cast ('PROGRAM_ATTRIBUTE' as text) as item_type,"
          + " cast (null as text) as expression";

  private static final String COMMON_UIDS = "program.uid, trackedentityattribute.uid";

  private static final String JOINS =
      " join program_attributes on program_attributes.trackedentityattributeid = trackedentityattribute.trackedentityattributeid"
          + " join program on program_attributes.programid = program.programid";

  private static final String SPACED_FROM_TRACKED_ENTITY_ATTRIBUTE =
      " from trackedentityattribute ";

  @Override
  public String getStatement(final MapSqlParameterSource paramsMap) {
    final StringBuilder sql = new StringBuilder();

    sql.append("(");

    // Creating a temp translated table to be queried.
    sql.append(SPACED_SELECT + "* from (");

    if (hasNonBlankStringPresence(paramsMap, LOCALE)) {
      // Selecting translated names.
      sql.append(selectRowsContainingTranslatedName());
    } else {
      // Retrieving all rows ignoring translation as no locale is defined.
      sql.append(selectAllRowsIgnoringAnyTranslation());
    }

    sql.append(
        " group by program.name, program.shortname, item_name, "
            + COMMON_UIDS
            + ", item_valuetype, item_code, item_sharing, item_shortname,"
            + " i18n_first_name, i18n_first_shortname, i18n_second_name, i18n_second_shortname");

    // Closing the temp table.
    sql.append(" ) t");

    sql.append(SPACED_WHERE);

    // Applying filters, ordering and limits.

    // Mandatory filters. They do not respect the root junction filtering.
    sql.append(always(sharingConditions("t.item_sharing", READ_ACCESS, paramsMap)));
    sql.append(" and");
    sql.append(ifSet(valueTypeFiltering("t.item_valuetype", paramsMap)));

    // Optional filters, based on the current root junction.
    final OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder(paramsMap);
    optionalFilters.append(
        ifSet(displayNameFiltering("t.i18n_first_name", "t.i18n_second_name", paramsMap)));
    optionalFilters.append(
        ifSet(
            displayShortNameFiltering(
                "t.i18n_first_shortname", "t.i18n_second_shortname", paramsMap)));
    optionalFilters.append(ifSet(nameFiltering("t.program_name", "t.item_name", paramsMap)));
    optionalFilters.append(
        ifSet(shortNameFiltering("t.program_shortname", "t.item_shortname", paramsMap)));
    optionalFilters.append(ifSet(programIdFiltering("t.program_uid", paramsMap)));
    optionalFilters.append(ifSet(uidFiltering("t.item_uid", paramsMap)));
    sql.append(ifAny(optionalFilters.toString()));

    final String identifiableStatement =
        identifiableTokenFiltering(
            "t.item_uid", "t.item_code", "t.i18n_second_name", "t.i18n_first_name", paramsMap);

    if (isNotBlank(identifiableStatement)) {
      sql.append(rootJunction(paramsMap));
      sql.append(identifiableStatement);
    }

    sql.append(
        ifSet(
            ordering(
                "t.i18n_first_name, t.i18n_second_name, t.item_uid",
                "t.program_name, t.item_name, t.item_uid",
                "t.i18n_first_shortname," + " t.i18n_second_shortname, t.item_uid",
                "t.program_shortname, t.item_shortname, t.item_uid",
                paramsMap)));
    sql.append(ifSet(maxLimit(paramsMap)));
    sql.append(")");

    final String fullStatement = sql.toString();

    log.trace("Full SQL: " + fullStatement);

    return fullStatement;
  }

  /**
   * No rules required.
   *
   * @param paramsMap
   * @return true
   */
  @Override
  public boolean matchQueryRules(final MapSqlParameterSource paramsMap) {
    return true;
  }

  @Override
  public Class<? extends BaseIdentifiableObject> getRootEntity() {
    return QueryableDataItem.PROGRAM_ATTRIBUTE.getEntity();
  }

  private String selectRowsContainingTranslatedName() {
    return new StringBuilder()
        .append(SPACED_SELECT + COMMON_COLUMNS)
        .append(translationNamesColumnsFor("trackedentityattribute", true))
        .append(SPACED_FROM_TRACKED_ENTITY_ATTRIBUTE)
        .append(JOINS)
        .append(translationNamesJoinsOn("trackedentityattribute", true))
        .toString();
  }

  private String selectAllRowsIgnoringAnyTranslation() {
    return new StringBuilder()
        .append(SPACED_SELECT + COMMON_COLUMNS)
        .append(
            ", program.name as i18n_first_name, trackedentityattribute.name as i18n_second_name")
        .append(
            ", program.shortname as i18n_first_shortname, trackedentityattribute.shortname as i18n_second_shortname")
        .append(SPACED_FROM_TRACKED_ENTITY_ATTRIBUTE)
        .append(JOINS)
        .toString();
  }
}
