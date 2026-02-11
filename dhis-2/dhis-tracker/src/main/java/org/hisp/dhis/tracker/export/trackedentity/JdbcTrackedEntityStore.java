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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Map.entry;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.FilterJdbcPredicate.addPredicates;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityStore")
@RequiredArgsConstructor
class JdbcTrackedEntityStore {

  private static final String MAIN_QUERY_ALIAS = "te";

  private static final String ENROLLMENT_ALIAS = "en";

  private static final String DEFAULT_ORDER = MAIN_QUERY_ALIAS + ".trackedentityid desc";

  private static final String ENROLLMENT_DATE_ALIAS = "en_enrollmentdate";

  private static final String ENROLLMENT_DATE_KEY = "enrollment.enrollmentDate";

  private static final String EVENT_ALIAS = "ev";

  private static final String INVALID_ORDER_FIELD_MESSAGE =
      "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.";

  private static final String BASE_SELECT = "select te.trackedentityid, te.uid";

  /**
   * Tracked entities can be ordered by given fields which correspond to fields on {@link
   * TrackedEntity}. Maps fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", "uid"),
          entry("created", "created"),
          entry("createdAtClient", "createdatclient"),
          entry("lastUpdated", "lastupdated"),
          entry("lastUpdatedAtClient", "lastupdatedatclient"),
          entry(ENROLLMENT_DATE_KEY, ENROLLMENT_DATE_ALIAS),
          entry("inactive", "inactive"));

  private final SystemSettingsProvider settingsProvider;

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  public List<TrackedEntityIdentifiers> getTrackedEntityIds(TrackedEntityQueryParams params) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!params.hasEnrolledInTrackerProgram() && params.getAccessibleTrackerPrograms().isEmpty()) {
      return List.of();
    }

    validateMaxTeLimit(params);

    final MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql = getQuery(params, null, sqlParameters);
    SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet(sql, sqlParameters);

    List<TrackedEntityIdentifiers> ids = new ArrayList<>();
    while (rowSet.next()) {
      ids.add(
          new TrackedEntityIdentifiers(rowSet.getLong("trackedentityid"), rowSet.getString("uid")));
    }
    return ids;
  }

  public Page<TrackedEntityIdentifiers> getTrackedEntityIds(
      TrackedEntityQueryParams params, PageParams pageParams) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!params.hasEnrolledInTrackerProgram() && params.getAccessibleTrackerPrograms().isEmpty()) {
      return Page.empty();
    }

    validateMaxTeLimit(params);

    MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql = getQuery(params, pageParams, sqlParameters);
    SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet(sql, sqlParameters);

    List<TrackedEntityIdentifiers> ids = new ArrayList<>();
    while (rowSet.next()) {
      ids.add(
          new TrackedEntityIdentifiers(rowSet.getLong("trackedentityid"), rowSet.getString("uid")));
    }

    return new Page<>(ids, pageParams, () -> getTrackedEntityCount(params));
  }

  private void validateMaxTeLimit(TrackedEntityQueryParams params) {
    if (!params.isSearchOutsideCaptureScope()) {
      return;
    }

    int maxTeLimit = getMaxTeLimit(params);
    if (maxTeLimit > 0 && getTrackedEntityCountWithMaxLimit(params) > maxTeLimit) {
      throw new IllegalQueryException("maxteicountreached");
    }
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  private Long getTrackedEntityCount(TrackedEntityQueryParams params) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!params.hasEnrolledInTrackerProgram() && params.getAccessibleTrackerPrograms().isEmpty()) {
      return 0L;
    }

    final MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql = getCountQuery(params, sqlParameters);
    return namedParameterJdbcTemplate.queryForObject(sql, sqlParameters, Long.class);
  }

  private int getTrackedEntityCountWithMaxLimit(TrackedEntityQueryParams params) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!params.hasEnrolledInTrackerProgram() && params.getAccessibleTrackerPrograms().isEmpty()) {
      return 0;
    }

    MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql = getCountQueryWithMaxTrackedEntityLimit(params, sqlParameters);
    Integer count = namedParameterJdbcTemplate.queryForObject(sql, sqlParameters, Integer.class);
    return count != null ? count : 0;
  }

  /**
   * Generates SQL based on "params". The purpose of the SQL is to retrieve a list of tracked
   * entities.
   *
   * <p>The general structure of the query is as follows:
   *
   * <p>select (main_projection) from (constraint_subquery) left join (additional_information) group
   * by (main_groupby) order by (order)
   *
   * <p>The constraint_subquery looks as follows:
   *
   * <p>select (subquery_projection) from (tracked entities) inner join (attribute_constraints)
   * [inner join (program_owner)] inner join (organisation units) left join (attribute_orderby)
   * where exist(program_constraint) order by (order) limit (limit_offset)
   *
   * <p>main_projection: Will have an aggregate string of attributevalues (uid:value) as well as
   * basic te-info. constraint_subquery: Includes all SQL related to narrowing down the number of
   * te's we are looking for. We use inner join primarily for this, as well as exists for program
   * instances. Do make sure we get the right selection, we also use left join on attributes, when
   * we are sorting by attributes, before we sort and finally limit the selection.
   * subquery_projection: Has all the required information for knowing what tracked entities to
   * return and how to order them attribute_constraints: We inner join the attributes, and add 3
   * conditions: te id, tea id and value. This uses a (te, tea, lower(value)) index. For each
   * attribute constraints, we add subsequent inner joins. program_owner: Only included when a
   * program is specified. If included, it will join on 3 columns: te, program and ou. We have an
   * index for this (program, ou, te) which allows a scan only lookup attribute_orderby: When a user
   * specified an attribute in the order param, we need to join that attribute (We do left join, in
   * case the value is not there. This join is not for removing resulting records). After joining it
   * and projecting it, we can order by it. program_constraint: If a program is specified, it
   * indicates the te must be enrolled in that program. Since the relation between te and
   * enrollments are not 1:1, but 1:many, we use exists to avoid duplicate rows of te, allowing us
   * to avoid grouping the result before we order and limit. This saves a lot of time. NOTE: Within
   * the program_constraint, we also have a sub-query to deal with any event-related constraints.
   * These can either be constraints on any static properties, or user assignment. For user
   * assignment, we also join with the userinfo table. For events, we have an index (status,
   * occurreddate) which speeds up the lookup significantly order: Order is used both in the
   * sub-query and the main query. The sort depends on the params (see more info on the related
   * method). We order the sub-query to make sure we get correct results before we limit. We order
   * the main query since the aggregation mixes up the order, so to return a consistent order, we
   * order again. limit_offset: The limit and offset is set based on a combination of params:
   * program and tet can have a maxte limit, which only applies during a search outside the users
   * capture scope. If applied, it will throw an error if the number of results exceeds the limit.
   * Otherwise, we use paging. If no paging is set, there is no limit. additional_information: Here
   * we do a left join with any relevant information needed for the result: tet name, any attributes
   * to project, etc. We left join, since we don't want to reduce the results, just add information.
   * main_groupby: The purpose of this group by, is to aggregate any attributes added in
   * additional_information
   */
  private String getQuery(
      TrackedEntityQueryParams params, PageParams pageParams, MapSqlParameterSource sqlParameters) {
    StringBuilder sql = new StringBuilder();
    addSelect(sql, params);
    sql.append(" from ");
    addTrackedEntityFromItem(sql, sqlParameters, params, pageParams, false);
    addOrderBy(sql, params);
    // LIMIT must be in outer query for DISTINCT ON (applied after final ORDER BY)
    if (isOrderingByEnrolledAt(params)) {
      sql.append(" ");
      addLimitAndOffset(sql, pageParams);
    }
    return sql.toString();
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count and
   * ignores order and limit
   */
  private String getCountQuery(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters) {
    StringBuilder sql = new StringBuilder("select count(trackedentityid) from (");
    addSelect(sql, params);
    sql.append(" from ");
    addTrackedEntityFromItem(sql, sqlParameters, params, null, true);
    sql.append(" ) tecount");
    return sql.toString();
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count, ignores
   * order but uses the te limit set on the program if higher than 0
   */
  private String getCountQueryWithMaxTrackedEntityLimit(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters) {
    StringBuilder sql = new StringBuilder("select count(trackedentityid) from (");
    addSelect(sql, params);
    sql.append(" from ");
    addTrackedEntityFromItem(sql, sqlParameters, params, null, true);
    sql.append("limit ").append(getMaxTeLimit(params) + 1);
    sql.append(" ) tecount");
    return sql.toString();
  }

  private void addSelect(StringBuilder sql, TrackedEntityQueryParams params) {
    sql.append(BASE_SELECT);
    if (isOrderingByEnrolledAt(params)) {
      sql.append(", ").append(ENROLLMENT_DATE_ALIAS);
    }
  }

  /**
   * Generates the SQL of the sub-query, used to find the correct subset of tracked entities to
   * return. Orchestrates all the different segments of the SQL into a complete sub-query.
   */
  private void addTrackedEntityFromItem(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      PageParams pageParams,
      boolean isCountQuery) {
    sql.append("(");
    addTrackedEntityFromItemSelect(sql, params);
    sql.append(" from trackedentity ").append(MAIN_QUERY_ALIAS).append(" ");

    addJoinOnProgram(sql, sqlParameters, params);
    sql.append(" ");
    addJoinOnProgramOwner(sql, sqlParameters, params);
    sql.append(" ");
    addJoinOnOrgUnit(sql, sqlParameters, params);
    sql.append(" ");
    addJoinOnEnrollment(sql, sqlParameters, params);
    sql.append(" ");
    addJoinOnAttributes(sql, params);
    sql.append(" ");

    SqlHelper sqlHelper = new SqlHelper(true);
    addAttributeFilterConditions(sql, sqlParameters, params, sqlHelper);
    addTrackedEntityConditions(sql, sqlParameters, params, sqlHelper);
    addEnrollmentAndEventExistsCondition(sql, sqlParameters, params, sqlHelper);

    if (!isCountQuery) {
      sql.append(" ");
      // DISTINCT ON requires ORDER BY to start with the DISTINCT columns
      if (isOrderingByEnrolledAt(params)) {
        addDistinctOnOrderBy(sql, params);
        // LIMIT must be in outer query for DISTINCT ON (after final ORDER BY)
      } else {
        addOrderBy(sql, params);
        sql.append(" ");
        addLimitAndOffset(sql, pageParams);
      }
    }

    sql.append(") ").append(MAIN_QUERY_ALIAS).append(" ");
  }

  /**
   * Adds ORDER BY for DISTINCT ON queries. DISTINCT ON requires ORDER BY to start with the DISTINCT
   * columns (trackedentityid), followed by the enrollment date in the requested direction.
   */
  private void addDistinctOnOrderBy(StringBuilder sql, TrackedEntityQueryParams params) {
    sql.append("order by te.trackedentityid, ")
        .append(ENROLLMENT_ALIAS)
        .append(".enrollmentdate ")
        .append(getEnrolledAtOrder(params).getDirection().name());
  }

  /**
   * Adds the SELECT for the inner subquery. By default SELECTs only trackedentityid (the PK) and
   * uid to avoid expensive DISTINCT comparisons on all columns. When ORDER BY is used, those
   * columns must also be in the SELECT list because PostgreSQL requires it for SELECT DISTINCT.
   * Adding them does not affect deduplication since the PK already guarantees uniqueness.
   *
   * <p>The column names here must stay in sync with {@link #addJoinOnAttributes(StringBuilder,
   * TrackedEntityQueryParams)}, {@link #addJoinOnEnrollment(StringBuilder, MapSqlParameterSource,
   * TrackedEntityQueryParams)} and {@link #addOrderBy(StringBuilder, TrackedEntityQueryParams)}.
   */
  private void addTrackedEntityFromItemSelect(StringBuilder sql, TrackedEntityQueryParams params) {
    if (isOrderingByEnrolledAt(params)) {
      // When ordering by enrolledAt, use DISTINCT ON to pick one enrollment per TE.
      sql.append("select distinct on (te.trackedentityid) te.trackedentityid");
    } else if (params.hasEnrolledInTrackerProgram()) {
      // No DISTINCT needed: trackedentityprogramowner's unique index on (trackedentityid,
      // programid) guarantees one row per TE. This relies on enrollment filters using EXISTS
      // (addEnrollmentAndEventExistsCondition), not a JOIN. If enrollment is ever joined
      // directly in this path, DISTINCT must be restored.
      sql.append("select te.trackedentityid");
    } else {
      // Without a program, the left join on trackedentityprogramowner can produce
      // multiple rows per TE (one per program enrollment). DISTINCT is needed.
      sql.append("select distinct te.trackedentityid");
    }

    // TE columns needed by the outer query
    sql.append(", te.uid");

    // Add order-by columns so they are available for ORDER BY (required for DISTINCT)
    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  INVALID_ORDER_FIELD_MESSAGE,
                  field,
                  String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        if (ENROLLMENT_DATE_KEY.equals(field)) {
          sql.append(", ")
              .append(ENROLLMENT_ALIAS)
              .append(".enrollmentdate as ")
              .append(ENROLLMENT_DATE_ALIAS);
        } else {
          // TE column needed in SELECT for DISTINCT ORDER BY
          sql.append(", te.").append(ORDERABLE_FIELDS.get(field));
        }
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        sql.append(", ")
            .append(quote(tea.getUid()))
            .append(".value as ")
            .append(quote(tea.getUid()));
      } else {
        throw new IllegalArgumentException(
            String.format(
                INVALID_ORDER_FIELD_MESSAGE,
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }
  }

  private void addJoinOnProgram(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      return;
    }

    sql.append("inner join program p on p.trackedentitytypeid = te.trackedentitytypeid");
    sql.append(" and p.programid in (:accessiblePrograms)");
    sqlParameters.addValue(
        "accessiblePrograms", getIdentifiers(params.getAccessibleTrackerPrograms()));
  }

  private void addJoinOnProgramOwner(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      sql.append(
          """
          inner join trackedentityprogramowner po \
           on po.programid = :enrolledInTrackerProgram\
           and po.trackedentityid = te.trackedentityid""");
      sqlParameters.addValue(
          "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());
      return;
    }

    sql.append(
        """
        left join trackedentityprogramowner po \
        on po.trackedentityid = te.trackedentityid \
        and p.programid = po.programid""");
  }

  /**
   * Adds an INNER JOIN for organisation units.
   *
   * <p>If a program is specified, the join is based on program ownership (po). If no program is
   * specified, the join is based either on program ownership or the tracked entity's registering
   * unit.
   *
   * <p>The specific JOIN conditions depend on the {@code ouMode}:
   *
   * <ul>
   *   <li>{@code DESCENDANTS} – matches organisation units using the org unit's PATH
   *   <li>{@code CHILDREN} – matches the org unit's PATH or any of its immediate children
   *   <li>{@code SELECTED} – matches the specified org unit UID directly
   *   <li>{@code ALL} – no org unit constraints are applied
   * </ul>
   *
   * <p>If the org unit mode is not {@code ALL} the method also ensures that the tracked entity
   * owner falls within the appropriate access scope (either search or capture). This validation,
   * besides making sure the user has ownership access to the te, also covers the case where the org
   * unit is {@code ACCESSIBLE} or {@code CAPTURE}.
   */
  private void addJoinOnOrgUnit(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    String orgUnitTableAlias = "ou";

    sql.append("inner join organisationunit ");
    sql.append(orgUnitTableAlias);
    sql.append(" on ou.organisationunitid = ");
    if (params.hasEnrolledInTrackerProgram()) {
      sql.append("po.organisationunitid ");
    } else {
      sql.append("coalesce(po.organisationunitid, te.organisationunitid) ");
    }

    if (params.hasOrganisationUnits()) {
      buildOrgUnitModeClause(
          sql,
          sqlParameters,
          params.getOrgUnits(),
          params.getOrgUnitMode(),
          orgUnitTableAlias,
          "and ");
    }

    if (params.hasEnrolledInTrackerProgram() && params.getOwnershipScope() != null) {
      buildOwnershipClause(
          sql,
          sqlParameters,
          params.getEnrolledInTrackerProgram(),
          params.getOwnershipScope(),
          orgUnitTableAlias,
          MAIN_QUERY_ALIAS,
          () -> "and ");
    } else if (!params.hasEnrolledInTrackerProgram()) {
      buildOwnershipClause(
          sql,
          sqlParameters,
          params.getOrgUnitMode(),
          "p",
          orgUnitTableAlias,
          MAIN_QUERY_ALIAS,
          () -> "and ");
    }
  }

  /**
   * Adds an INNER JOIN on enrollments when ordering by {@code enrolledAt}.
   *
   * <p>Query strategy depends on enrollment usage:
   *
   * <ul>
   *   <li>No enrollment filters, no order by enrolledAt: no enrollment table needed
   *   <li>Enrollment filters, no order by enrolledAt: EXISTS subquery via {@link
   *       #addEnrollmentAndEventExistsCondition} (short-circuits, avoids duplicates)
   *   <li>Order by enrolledAt: JOIN with DISTINCT ON (this method)
   * </ul>
   *
   * <p>When ordering by enrolledAt, filters must be in the JOIN (not EXISTS) to ensure ordering
   * uses a matching enrollment. A TE can have multiple enrollments, so DISTINCT ON
   * (te.trackedentityid) picks one row per TE. DISTINCT ON requires ORDER BY to start with the
   * DISTINCT columns, so inner query must order by (trackedentityid, enrollmentdate) - not the
   * user's requested order. Outer query applies the user's order (enrollmentdate), so LIMIT must be
   * in outer query (after final ORDER BY).
   *
   * <p>Potential optimization: using DISTINCT ON (te.uid) instead would allow LIMIT in inner query
   * when user orders by (uid, enrolledAt), since inner ORDER BY would match. But users rarely order
   * by uid,enrolledAt together, so not worth the added complexity.
   */
  private void addJoinOnEnrollment(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (!isOrderingByEnrolledAt(params)) {
      return;
    }
    if (!params.hasEnrolledInTrackerProgram()) {
      throw new IllegalArgumentException(
          "Program is required when ordering by enrollment.enrollmentDate");
    }

    sql.append("inner join enrollment ")
        .append(ENROLLMENT_ALIAS)
        .append(" on ")
        .append(ENROLLMENT_ALIAS)
        .append(".trackedentityid = te.trackedentityid");

    sql.append(" and ").append(ENROLLMENT_ALIAS).append(".programid = :enrolledInTrackerProgram");
    sqlParameters.addValue(
        "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());

    addEnrollmentFilterConditions(sql, sqlParameters, params);
    if (params.hasFilterForEvents()) {
      sql.append(" and exists (");
      addEventExistsForEnrollmentJoin(sql, sqlParameters, params);
      sql.append(")");
    }
  }

  /** Appends enrollment filter conditions to SQL. Used by both JOIN and EXISTS paths. */
  private void addEnrollmentFilterConditions(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.hasEnrollmentStatus()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".status = :enrollmentStatus");
      sqlParameters.addValue("enrollmentStatus", params.getEnrollmentStatus().name());
    }
    if (params.hasFollowUp()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".followup = :followUp");
      sqlParameters.addValue("followUp", params.getFollowUp());
    }
    if (params.hasProgramEnrollmentStartDate()) {
      sql.append(" and ")
          .append(ENROLLMENT_ALIAS)
          .append(".enrollmentdate >= :enrollmentStartDate");
      sqlParameters.addValue(
          "enrollmentStartDate", timestampParameter(params.getProgramEnrollmentStartDate()));
    }
    if (params.hasProgramEnrollmentEndDate()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".enrollmentdate <= :enrollmentEndDate");
      sqlParameters.addValue(
          "enrollmentEndDate", timestampParameter(params.getProgramEnrollmentEndDate()));
    }
    if (params.hasProgramIncidentStartDate()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".occurreddate >= :occurredStartDate");
      sqlParameters.addValue(
          "occurredStartDate", timestampParameter(params.getProgramIncidentStartDate()));
    }
    if (params.hasProgramIncidentEndDate()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".occurreddate <= :occurredEndDate");
      sqlParameters.addValue(
          "occurredEndDate", timestampParameter(params.getProgramIncidentEndDate()));
    }
    if (!params.isIncludeDeleted()) {
      sql.append(" and ").append(ENROLLMENT_ALIAS).append(".deleted is false");
    }
  }

  /**
   * Adds an EXISTS subquery for event filters to be used in the enrollment JOIN condition. This
   * ensures we only consider enrollments that have matching events when ordering by enrolledAt.
   */
  private void addEventExistsForEnrollmentJoin(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append("select 1 from trackerevent ").append(EVENT_ALIAS).append(" ");

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      sql.append("inner join (")
          .append("select userinfoid as userid from userinfo where uid in (:assignedUserUids)")
          .append(") au on au.userid = ")
          .append(EVENT_ALIAS)
          .append(".assigneduserid ");
      sqlParameters.addValue(
          "assignedUserUids",
          UID.toValueSet(params.getAssignedUserQueryParam().getAssignedUsers()));
    }

    sql.append("where ")
        .append(EVENT_ALIAS)
        .append(".enrollmentid = ")
        .append(ENROLLMENT_ALIAS)
        .append(".enrollmentid");

    if (params.hasEventStatus()) {
      sql.append(" and ");
      addEventDateRangeCondition(sql, sqlParameters, params);
      sql.append(" and ");
      addEventStatusCondition(sql, sqlParameters, params);
    }

    if (params.hasProgramStage()) {
      sql.append(" and ").append(EVENT_ALIAS).append(".programstageid = :programStageId");
      sqlParameters.addValue("programStageId", params.getProgramStage().getId());
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      sql.append(" and ").append(EVENT_ALIAS).append(".assigneduserid is null");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      sql.append(" and ").append(EVENT_ALIAS).append(".assigneduserid is not null");
    }

    if (!params.isIncludeDeleted()) {
      sql.append(" and ").append(EVENT_ALIAS).append(".deleted is false");
    }
  }

  /** Appends event date range condition to SQL. Reusable across EXISTS subquery and JOIN paths. */
  private void addEventDateRangeCondition(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    String dateColumn =
        EVENT_ALIAS
            + "."
            + switch (params.getEventStatus()) {
              case COMPLETED, VISITED, ACTIVE -> "occurreddate";
              case SCHEDULE, OVERDUE, SKIPPED -> "scheduleddate";
            };
    sql.append(dateColumn)
        .append(" >= :eventStartDate and ")
        .append(dateColumn)
        .append(" <= :eventEndDate");
    sqlParameters.addValue("eventStartDate", timestampParameter(params.getEventStartDate()));
    sqlParameters.addValue("eventEndDate", timestampParameter(params.getEventEndDate()));
  }

  /** Appends event status condition to SQL. Reusable across EXISTS subquery and JOIN paths. */
  private void addEventStatusCondition(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.isEventStatus(EventStatus.COMPLETED)) {
      sql.append(EVENT_ALIAS).append(".status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.COMPLETED.name());
    } else if (params.isEventStatus(EventStatus.VISITED)
        || params.isEventStatus(EventStatus.ACTIVE)) {
      sql.append(EVENT_ALIAS).append(".status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.ACTIVE.name());
    } else if (params.isEventStatus(EventStatus.SKIPPED)) {
      sql.append(EVENT_ALIAS).append(".status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.SKIPPED.name());
    } else if (params.isEventStatus(EventStatus.SCHEDULE)) {
      sql.append(EVENT_ALIAS)
          .append(".status is not null and ")
          .append(EVENT_ALIAS)
          .append(".occurreddate is null and date(now()) <= date(")
          .append(EVENT_ALIAS)
          .append(".scheduleddate)");
    } else if (params.isEventStatus(EventStatus.OVERDUE)) {
      sql.append(EVENT_ALIAS)
          .append(".status is not null and ")
          .append(EVENT_ALIAS)
          .append(".occurreddate is null and date(now()) > date(")
          .append(EVENT_ALIAS)
          .append(".scheduleddate)");
    }
  }

  /**
   * Adds joins on tracked entity attribute values for filtering and sorting. Attributes with
   * non-null filters use INNER JOIN (the WHERE clause eliminates NULLs anyway), which lets the
   * planner use the join as a filter. Order-only attributes and attributes with a {@code null}
   * operator filter use LEFT JOIN to preserve rows without a value.
   *
   * <p>Attribute filtering is handled in {@link #addAttributeFilterConditions(StringBuilder,
   * MapSqlParameterSource, TrackedEntityQueryParams, SqlHelper)}.
   */
  private void addJoinOnAttributes(StringBuilder sql, TrackedEntityQueryParams params) {
    for (TrackedEntityAttribute attribute : params.getInnerJoinAttributes()) {
      addAttributeJoin(sql, "inner", attribute);
    }
    for (TrackedEntityAttribute attribute : params.getLeftJoinAttributes()) {
      addAttributeJoin(sql, "left", attribute);
    }
  }

  private void addAttributeJoin(
      StringBuilder sql, String joinType, TrackedEntityAttribute attribute) {
    String col = quote(attribute.getUid());
    sql.append(" ")
        .append(joinType)
        .append(" join trackedentityattributevalue as ")
        .append(col)
        .append(" on ")
        .append(col)
        .append(".trackedentityid = te.trackedentityid and ")
        .append(col)
        .append(".trackedentityattributeid = ")
        .append(attribute.getId())
        .append(" ");
  }

  /** Adds the WHERE-clause conditions related to the user provided filters. */
  private void addAttributeFilterConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      SqlHelper hlp) {
    if (params.getFilters().isEmpty()) {
      return;
    }

    sql.append(hlp.whereAnd());
    addPredicates(sql, sqlParameters, params.getFilters());
    sql.append(" ");
  }

  /** Adds the WHERE-clause conditions related to tracked entities. */
  private void addTrackedEntityConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      SqlHelper whereAnd) {
    if (params.hasTrackedEntities()) {
      sql.append(whereAnd.whereAnd()).append("te.uid IN (:trackedEntities) ");
      sqlParameters.addValue("trackedEntities", UID.toValueSet(params.getTrackedEntities()));
    }

    if (params.hasTrackedEntityType()) {
      sql.append(whereAnd.whereAnd()).append("te.trackedentitytypeid = :trackedEntityTypeId ");
      sqlParameters.addValue("trackedEntityTypeId", params.getTrackedEntityType().getId());
    } else if (params.hasEnrolledInTrackerProgram()) {
      sql.append(whereAnd.whereAnd()).append("te.trackedentitytypeid = :trackedEntityTypeId ");
      sqlParameters.addValue(
          "trackedEntityTypeId",
          params.getEnrolledInTrackerProgram().getTrackedEntityType().getId());
    } else {
      sql.append(whereAnd.whereAnd()).append("te.trackedentitytypeid in (:trackedEntityTypeIds) ");
      sqlParameters.addValue(
          "trackedEntityTypeIds", getIdentifiers(params.getTrackedEntityTypes()));
    }

    if (params.hasLastUpdatedDuration()) {
      sql.append(whereAnd.whereAnd()).append(" te.lastupdated >= :lastUpdatedDuration ");
      sqlParameters.addValue(
          "lastUpdatedDuration",
          timestampParameter(DateUtils.nowMinusDuration(params.getLastUpdatedDuration())));
    } else {
      if (params.hasLastUpdatedStartDate()) {
        sql.append(whereAnd.whereAnd()).append(" te.lastupdated >= :lastUpdatedStartDate ");
        sqlParameters.addValue(
            "lastUpdatedStartDate", timestampParameter(params.getLastUpdatedStartDate()));
      }
      if (params.hasLastUpdatedEndDate()) {
        sql.append(whereAnd.whereAnd()).append(" te.lastupdated <= :lastUpdatedEndDate ");
        sqlParameters.addValue(
            "lastUpdatedEndDate", timestampParameter(params.getLastUpdatedEndDate()));
      }
    }

    if (!params.isIncludeDeleted()) {
      sql.append(whereAnd.whereAnd()).append("te.deleted is false ");
    }

    if (params.hasPotentialDuplicateFilter()) {
      sql.append(whereAnd.whereAnd()).append("te.potentialduplicate = :potentialDuplicate ");
      sqlParameters.addValue("potentialDuplicate", params.getPotentialDuplicate());
    }
  }

  /**
   * Adds an EXISTS condition for enrollment (and event if specified). The EXIST will allow us to
   * filter by enrollments with a low overhead. This condition only applies when a program is
   * specified.
   *
   * <p>When ordering by enrolledAt, the enrollment JOIN already includes all filters, so this
   * EXISTS is skipped to avoid redundant checks.
   */
  private void addEnrollmentAndEventExistsCondition(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      SqlHelper whereAnd) {
    if (!params.hasEnrolledInTrackerProgram()) {
      return;
    }
    // When ordering by enrolledAt, the enrollment JOIN already includes all filters
    if (isOrderingByEnrolledAt(params)) {
      return;
    }

    sql.append(whereAnd.whereAnd())
        .append("exists (")
        .append("select en.trackedentityid ")
        .append("from enrollment en ");

    if (params.hasFilterForEvents()) {
      sql.append("inner join (");
      addEventFilter(sql, sqlParameters, params);
      sql.append(") ev on ev.enrollmentid = en.enrollmentid ");
    }

    sql.append(
        "where en.trackedentityid = te.trackedentityid and en.programid = :enrolledInTrackerProgram");
    sqlParameters.addValue(
        "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());

    addEnrollmentFilterConditions(sql, sqlParameters, params);

    sql.append(")");
  }

  /** Adds event query with event related query params to given {@code sql}. */
  private void addEventFilter(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append("select ev.enrollmentid ").append("from trackerevent ev ");

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      sql.append("inner join (")
          .append("select userinfoid as userid ")
          .append("from userinfo ")
          .append("where uid in (:assignedUserUids) ")
          .append(") au on au.userid = ev.assigneduserid");
      sqlParameters.addValue(
          "assignedUserUids",
          UID.toValueSet(params.getAssignedUserQueryParam().getAssignedUsers()));
    }

    SqlHelper whereHlp = new SqlHelper(true);
    if (params.hasEventStatus()) {
      sql.append(whereHlp.whereAnd());
      addEventDateRangeCondition(sql, sqlParameters, params);
      sql.append(whereHlp.whereAnd());
      addEventStatusCondition(sql, sqlParameters, params);
    }

    if (params.hasProgramStage()) {
      sql.append(whereHlp.whereAnd()).append("ev.programstageid = :programStageId ");
      sqlParameters.addValue("programStageId", params.getProgramStage().getId());
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      sql.append(whereHlp.whereAnd()).append("ev.assigneduserid is null ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      sql.append(whereHlp.whereAnd()).append("ev.assigneduserid is not null ");
    }

    if (!params.isIncludeDeleted()) {
      sql.append(whereHlp.whereAnd()).append("ev.deleted is false");
    }
  }

  /**
   * Adds the ORDER BY clause. This clause is used both in the sub-query and main query. When using
   * it in the sub-query, we want to make sure we get the right tracked entities. When we order in
   * the main query, it's to make sure we return the results in the correct order, since order might
   * be mixed after GROUP BY.
   */
  private void addOrderBy(StringBuilder sql, TrackedEntityQueryParams params) {
    List<String> orderFields = new ArrayList<>();
    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  INVALID_ORDER_FIELD_MESSAGE,
                  field,
                  String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        orderFields.add(ORDERABLE_FIELDS.get(field) + " " + order.getDirection());
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        orderFields.add(quote(tea.getUid()) + " " + order.getDirection());
      } else {
        throw new IllegalArgumentException(
            String.format(
                INVALID_ORDER_FIELD_MESSAGE,
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    sql.append("order by ");

    if (orderFields.isEmpty()) {
      sql.append(DEFAULT_ORDER);
      return;
    }

    sql.append(StringUtils.join(orderFields, ',')).append(", ").append(DEFAULT_ORDER);
  }

  /**
   * Adds the LIMIT and OFFSET part of the sub-query. The limit is decided by the page size, page
   * offset and the system setting KeyTrackedEntityMaxLimit.
   *
   * <p>If the page parameters are not null, we use the page size and its offset. The validation in
   * {@link TrackedEntityOperationParamsMapper} guarantees that if the page parameters are set, the
   * page size will always be smaller than the system limit.
   *
   * <p>The limit is set in the sub-query, so the latter joins have fewer rows to consider.
   */
  private void addLimitAndOffset(StringBuilder sql, PageParams pageParams) {
    int systemMaxLimit = settingsProvider.getCurrentSettings().getTrackedEntityMaxLimit();

    if (pageParams != null) {
      sql.append("limit ")
          .append(pageParams.getPageSize() + 1) // get extra te to determine if there is a nextPage
          .append(" offset ")
          .append(pageParams.getOffset());
    } else if (systemMaxLimit > 0) {
      sql.append("limit ").append(systemMaxLimit);
    }
  }

  /** Returns the maximum te retrieval limit. 0 no limit. */
  private int getMaxTeLimit(TrackedEntityQueryParams params) {
    if (params.hasTrackedEntityType()) {
      return params.getTrackedEntityType().getMaxTeiCountToReturn();
    } else if (params.hasEnrolledInTrackerProgram()) {
      return params.getEnrolledInTrackerProgram().getMaxTeiCountToReturn();
    }

    return 0;
  }

  @Nonnull
  private static SqlParameterValue timestampParameter(Date date) {
    return new SqlParameterValue(Types.TIMESTAMP, date);
  }

  /** Returns true if ordering by enrolledAt (enrollment.enrollmentDate). */
  private static boolean isOrderingByEnrolledAt(TrackedEntityQueryParams params) {
    return getEnrolledAtOrder(params) != null;
  }

  /** Returns the Order for enrolledAt, or null if not ordering by it. */
  private static Order getEnrolledAtOrder(TrackedEntityQueryParams params) {
    return params.getOrder().stream()
        .filter(o -> o.getField() instanceof String)
        .filter(o -> ENROLLMENT_DATE_KEY.equals(o.getField()))
        .findFirst()
        .orElse(null);
  }
}
