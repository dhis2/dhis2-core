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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Map.entry;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.FilterJdbcPredicate.addPredicates;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;

import jakarta.persistence.EntityManager;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityStore")
class HibernateTrackedEntityStore extends SoftDeleteHibernateObjectStore<TrackedEntity> {

  private static final String MAIN_QUERY_ALIAS = "te";

  private static final String ENROLLMENT_ALIAS = "en";

  private static final String DEFAULT_ORDER = MAIN_QUERY_ALIAS + ".trackedentityid desc";

  private static final String ENROLLMENT_DATE_ALIAS = "en_enrollmentdate";

  private static final String ENROLLMENT_DATE_KEY = "enrollment.enrollmentDate";

  /**
   * Tracked entities can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.trackedentity.TrackedEntity}. Maps fields to DB columns.
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

  public HibernateTrackedEntityStore(
      EntityManager entityManager,
      JdbcTemplate jdbcTemplate,
      NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      ApplicationEventPublisher publisher,
      AclService aclService,
      OrganisationUnitStore organisationUnitStore,
      SystemSettingsProvider settingsProvider) {
    super(entityManager, jdbcTemplate, publisher, TrackedEntity.class, aclService, false);

    checkNotNull(organisationUnitStore);
    checkNotNull(settingsProvider);

    this.settingsProvider = settingsProvider;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
  }

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
    LinkedHashSet<String> columns =
        new LinkedHashSet<>(
            List.of(
                "te.trackedentityid",
                "te.uid",
                "te.created",
                "te.lastupdated",
                "te.createdatclient",
                "te.lastupdatedatclient",
                "te.inactive",
                "te.potentialduplicate",
                "te.deleted",
                "te.trackedentitytypeid"));

    // all orderable fields are already in the select. Only when ordering by enrollment date do we
    // need to add a column, so we can order by it
    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field && ENROLLMENT_DATE_KEY.equals(field)) {
        columns.add(ENROLLMENT_DATE_ALIAS);
      }
    }

    sql.append("select ")
        .append(columns.stream().filter(c -> !c.isEmpty()).collect(Collectors.joining(", ")));
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
    sql.append(" from trackedentity " + MAIN_QUERY_ALIAS + " ");

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
      addOrderBy(sql, params);
      sql.append(" ");
      addLimitAndOffset(sql, pageParams);
    }

    sql.append(") ").append(MAIN_QUERY_ALIAS).append(" ");
  }

  /**
   * Add the SELECT to the {@code sql}. Columns for attribute values and the {@code enrolledAt} date
   * are only included if tracked entities should be ordered by them. The column names in here and
   * {@link #addJoinOnAttributes(StringBuilder, TrackedEntityQueryParams)} and {@link
   * #addJoinOnEnrollment(StringBuilder, MapSqlParameterSource, TrackedEntityQueryParams)} and
   * {@link #addOrderBy(StringBuilder, TrackedEntityQueryParams)} have to stay in sync.
   */
  private void addTrackedEntityFromItemSelect(StringBuilder sql, TrackedEntityQueryParams params) {
    LinkedHashSet<String> columns =
        new LinkedHashSet<>(
            List.of(
                "te.trackedentityid as trackedentityid",
                "te.trackedentitytypeid as trackedentitytypeid",
                "te.uid as uid",
                "te.created as created",
                "te.lastupdated as lastupdated",
                "te.createdatclient as createdatclient",
                "te.lastupdatedatclient as lastupdatedatclient",
                "te.inactive as inactive",
                "te.potentialduplicate as potentialduplicate",
                "te.deleted as deleted"));

    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                  field, String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        // all orderable fields are already in the select
        if (ENROLLMENT_DATE_KEY.equals(field)) {
          columns.add(ENROLLMENT_ALIAS + ".enrollmentdate as " + ENROLLMENT_DATE_ALIAS);
        }
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        columns.add(quote(tea.getUid()) + ".value as " + quote(tea.getUid()));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    sql.append("select distinct ");
    sql.append(String.join(", ", columns));
  }

  private void addJoinOnProgram(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append("inner join program p on p.trackedentitytypeid = te.trackedentitytypeid");

    if (params.hasEnrolledInTrackerProgram()) {
      return;
    }

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
           and po.trackedentityid = te.trackedentityid \
           and p.programid = po.programid""");
      sqlParameters.addValue(
          "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());
      return;
    }

    sql.append(
        "left join trackedentityprogramowner po on "
            + " po.trackedentityid = te.trackedentityid"
            + " and p.programid = po.programid");
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
    String programTableAlias = "p";

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

    buildOwnershipClause(
        sql,
        sqlParameters,
        params.getOrgUnitMode(),
        programTableAlias,
        orgUnitTableAlias,
        MAIN_QUERY_ALIAS,
        () -> "and ");
  }

  /**
   * Adds an INNER JOIN on enrollments when order by contains {@code enrolledAt}. We restrict the
   * join condition to a specific program if specified in the request.
   */
  private void addJoinOnEnrollment(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.getOrder().stream()
        .filter(o -> o.getField() instanceof String)
        .noneMatch(p -> ENROLLMENT_DATE_KEY.equals(p.getField()))) {
      return;
    }

    sql.append("inner join enrollment ")
        .append(ENROLLMENT_ALIAS)
        .append(" on ")
        .append(ENROLLMENT_ALIAS)
        .append(".trackedentityid = te.trackedentityid");

    if (!params.hasEnrolledInTrackerProgram()) {
      return;
    }

    sql.append(" and ").append(ENROLLMENT_ALIAS).append(".programid = :enrolledInTrackerProgram");
    sqlParameters.addValue(
        "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());
  }

  /**
   * Adds a single LEFT JOIN for each attribute used for filtering and sorting. The result of this
   * LEFT JOIN is used in the subquery projection and for ordering in both the subquery and the main
   * query.
   *
   * <p>Attribute filtering is handled in {@link #addAttributeFilterConditions(StringBuilder,
   * MapSqlParameterSource, TrackedEntityQueryParams, SqlHelper)}.
   */
  private void addJoinOnAttributes(StringBuilder sql, TrackedEntityQueryParams params) {
    for (TrackedEntityAttribute attribute : params.getLeftJoinAttributes()) {
      String col = quote(attribute.getUid());
      sql.append(" left join trackedentityattributevalue as ")
          .append(col)
          .append(" on ")
          .append(col)
          .append(".trackedentityid = te.trackedentityid and ")
          .append(col)
          .append(".trackedentityattributeid = ")
          .append(attribute.getId())
          .append(" ");
    }
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
    } else if (!params.hasEnrolledInTrackerProgram()) {
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
   */
  private void addEnrollmentAndEventExistsCondition(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      SqlHelper whereAnd) {
    if (!params.hasEnrolledInTrackerProgram()) {
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

    sql.append("where en.trackedentityid = te.trackedentityid ")
        .append("and en.programid = :enrolledInTrackerProgram ");
    sqlParameters.addValue(
        "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());

    if (params.hasEnrollmentStatus()) {
      sql.append("and en.status = :enrollmentStatus ");
      sqlParameters.addValue("enrollmentStatus", params.getEnrollmentStatus().name());
    }

    if (params.hasFollowUp()) {
      sql.append("and en.followup = :followUp ");
      sqlParameters.addValue("followUp", params.getFollowUp());
    }

    if (params.hasProgramEnrollmentStartDate()) {
      sql.append("and en.enrollmentdate >= :enrollmentStartDate ");
      sqlParameters.addValue(
          "enrollmentStartDate", timestampParameter(params.getProgramEnrollmentStartDate()));
    }

    if (params.hasProgramEnrollmentEndDate()) {
      sql.append("and en.enrollmentdate <= :enrollmentEndDate ");
      sqlParameters.addValue(
          "enrollmentEndDate", timestampParameter(params.getProgramEnrollmentEndDate()));
    }

    if (params.hasProgramIncidentStartDate()) {
      sql.append("and en.occurreddate >= :occurredStartDate ");
      sqlParameters.addValue(
          "occurredStartDate", timestampParameter(params.getProgramIncidentStartDate()));
    }

    if (params.hasProgramIncidentEndDate()) {
      sql.append("and en.occurreddate <= :occurredEndDate ");
      sqlParameters.addValue(
          "occurredEndDate", timestampParameter(params.getProgramIncidentEndDate()));
    }

    if (!params.isIncludeDeleted()) {
      sql.append("and en.deleted is false ");
    }

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
      addEventDateRange(sql, sqlParameters, params);
      sql.append(whereHlp.whereAnd());
      addEventStatus(sql, sqlParameters, params);
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

  private void addEventDateRange(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append(
        switch (params.getEventStatus()) {
          case COMPLETED, VISITED, ACTIVE ->
              "ev.occurreddate >= :eventStartDate and ev.occurreddate <= :eventEndDate";
          case SCHEDULE, OVERDUE, SKIPPED ->
              "ev.scheduleddate >= :eventStartDate and ev.scheduleddate <= :eventEndDate";
        });
    sqlParameters.addValue("eventStartDate", timestampParameter(params.getEventStartDate()));
    sqlParameters.addValue("eventEndDate", timestampParameter(params.getEventEndDate()));
  }

  private void addEventStatus(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.isEventStatus(EventStatus.COMPLETED)) {
      sql.append("ev.status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.COMPLETED.name());
    } else if (params.isEventStatus(EventStatus.VISITED)
        || params.isEventStatus(EventStatus.ACTIVE)) {
      sql.append("ev.status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.ACTIVE.name());
    } else if (params.isEventStatus(EventStatus.SKIPPED)) {
      sql.append("ev.status = :eventStatus");
      sqlParameters.addValue("eventStatus", EventStatus.SKIPPED.name());
    } else if (params.isEventStatus(EventStatus.SCHEDULE)) {
      sql.append(
          "ev.status is not null and ev.occurreddate is null and date(now()) <= date(ev.scheduleddate) ");
    } else if (params.isEventStatus(EventStatus.OVERDUE)) {
      sql.append(
          "ev.status is not null and ev.occurreddate is null and date(now()) > date(ev.scheduleddate) ");
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
                  "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                  field, String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        orderFields.add(ORDERABLE_FIELDS.get(field) + " " + order.getDirection());
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        orderFields.add(quote(tea.getUid()) + " " + order.getDirection());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
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
}
