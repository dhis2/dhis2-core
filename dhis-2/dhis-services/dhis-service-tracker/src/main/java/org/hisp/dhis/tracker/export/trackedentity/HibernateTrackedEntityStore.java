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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.JdbcPredicate.mapPredicatesToSql;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;

import jakarta.persistence.EntityManager;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.UserDetails;
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

  private static final String MAIN_QUERY_ALIAS = "TE";

  private static final String ENROLLMENT_ALIAS = "en";

  private static final String DEFAULT_ORDER = MAIN_QUERY_ALIAS + ".trackedentityid desc";

  private static final String OFFSET = "OFFSET";

  private static final String LIMIT = "LIMIT";

  private static final String ENROLLMENT_DATE_ALIAS = "en_enrollmentdate";

  private static final String ENROLLMENT_DATE_KEY = "enrollment.enrollmentDate";

  private static final String SPACE = " ";

  private static final String SELECT_COUNT_INSTANCE_FROM = "SELECT count(trackedentityid) FROM ( ";

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

  private final OrganisationUnitStore organisationUnitStore;

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

    this.organisationUnitStore = organisationUnitStore;
    this.settingsProvider = settingsProvider;
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
  }

  public List<TrackedEntityIdentifiers> getTrackedEntityIds(TrackedEntityQueryParams params) {
    // A TE which is not enrolled can only be accessed by a user that is able to enroll it into a
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
    // A TE which is not enrolled can only be accessed by a user that is able to enroll it into a
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
    // A TE which is not enrolled can only be accessed by a user that is able to enroll it into a
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
    // A TE which is not enrolled can only be accessed by a user that is able to enroll it into a
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
   * Generates SQL based on "params". The purpose of the SQL is to retrieve a list of tracked entity
   * instances.
   *
   * <p>The params are validated before we generate the SQL, so the only access-related SQL is the
   * inner join o organisation units.
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
    StringBuilder stringBuilder = new StringBuilder(getQuerySelect(params));
    return stringBuilder
        .append("FROM ")
        .append(getFromSubQuery(params, sqlParameters, false, pageParams))
        .append(getQueryOrderBy(params, false))
        .toString();
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count and
   * ignores order and limit
   */
  private String getCountQuery(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters) {
    return SELECT_COUNT_INSTANCE_FROM
        + getQuerySelect(params)
        + "FROM "
        + getFromSubQuery(params, sqlParameters, true, null)
        + " ) tecount";
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count, ignores
   * order but uses the TE limit set on the program if higher than 0
   */
  private String getCountQueryWithMaxTrackedEntityLimit(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters) {
    return SELECT_COUNT_INSTANCE_FROM
        + getQuerySelect(params)
        + "FROM "
        + getFromSubQuery(params, sqlParameters, true, null)
        + getLimitClause(getMaxTeLimit(params) + 1)
        + " ) tecount";
  }

  /**
   * Generates the projection of the main query
   *
   * @return an SQL projection
   */
  private String getQuerySelect(TrackedEntityQueryParams params) {
    LinkedHashSet<String> select =
        new LinkedHashSet<>(
            List.of(
                "TE.trackedentityid",
                "TE.uid",
                "TE.created",
                "TE.lastupdated",
                "TE.createdatclient",
                "TE.lastupdatedatclient",
                "TE.inactive",
                "TE.potentialduplicate",
                "TE.deleted",
                "TE.trackedentitytypeid"));

    // all orderable fields are already in the select. Only when ordering by enrollment date do we
    // need to add a column, so we can order by it
    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field && ENROLLMENT_DATE_KEY.equals(field)) {
        select.add(ENROLLMENT_DATE_ALIAS);
      }
    }

    return "select "
        + select.stream().filter(c -> !c.isEmpty()).collect(Collectors.joining(", "))
        + SPACE;
  }

  /**
   * Generates the SQL of the sub-query, used to find the correct subset of tracked entities to
   * return. Orchestrates all the different segments of the SQL into a complete sub-query.
   *
   * @return an SQL sub-query
   */
  private String getFromSubQuery(
      TrackedEntityQueryParams params,
      MapSqlParameterSource sqlParameters,
      boolean isCountQuery,
      PageParams pageParams) {
    SqlHelper whereAnd = new SqlHelper(true);
    StringBuilder sql =
        new StringBuilder()
            .append("(")
            .append(getFromSubQuerySelect(params))
            .append(" FROM trackedentity " + MAIN_QUERY_ALIAS + " ");

    addJoinOnProgram(sql, sqlParameters, params);
    sql.append(" ");
    addJoinOnProgramOwner(sql, sqlParameters, params);
    sql.append(" ")
        .append(getFromSubQueryJoinOrgUnitConditions(sqlParameters, params))
        .append(getFromSubQueryJoinEnrollmentConditions(params))

        // LEFT JOIN attributes we need to sort on and/or filter by.
        .append(getLeftJoinFromAttributes(params))

        // WHERE
        .append(getWhereClauseFromFilterConditions(whereAnd, sqlParameters, params))
        .append(getFromSubQueryTrackedEntityConditions(whereAnd, sqlParameters, params))
        .append(getFromSubQueryEnrollmentConditions(whereAnd, sqlParameters, params));

    if (!isCountQuery) {
      sql.append(getQueryOrderBy(params, true)).append(getFromSubQueryLimitAndOffset(pageParams));
    }

    return sql.append(") ").append(MAIN_QUERY_ALIAS).append(" ").toString();
  }

  /**
   * The sub-query projection. If we are sorting by attribute, we need to include the value in the
   * sub-query projection.
   *
   * @return a SQL projection
   */
  private String getFromSubQuerySelect(TrackedEntityQueryParams params) {
    LinkedHashSet<String> columns =
        new LinkedHashSet<>(
            List.of(
                "TE.trackedentityid as trackedentityid",
                "TE.trackedentitytypeid as trackedentitytypeid",
                "TE.uid as uid",
                "TE.created as created",
                "TE.lastupdated as lastupdated",
                "TE.createdatclient as createdatclient",
                "TE.lastupdatedatclient as lastupdatedatclient",
                "TE.inactive as inactive",
                "TE.potentialduplicate as potentialduplicate",
                "TE.deleted as deleted"));

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
        columns.add(quote(tea.getUid()) + ".value AS " + quote(tea.getUid()));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    return "SELECT DISTINCT " + String.join(", ", columns);
  }

  private void addJoinOnProgram(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append("inner join program P on P.trackedentitytypeid = TE.trackedentitytypeid");

    if (!params.hasEnrolledInTrackerProgram()) {
      sql.append(" and P.programid in (:accessiblePrograms)");
      sqlParameters.addValue(
          "accessiblePrograms", getIdentifiers(params.getAccessibleTrackerPrograms()));
    }
  }

  /**
   * Generates the WHERE-clause of the sub-query SQL related to tracked entities.
   *
   * @param whereAnd tracking if where has been invoked or not
   * @return a SQL segment for the WHERE clause used in the sub-query
   */
  private String getFromSubQueryTrackedEntityConditions(
      SqlHelper whereAnd, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    StringBuilder trackedEntity = new StringBuilder();

    if (params.hasTrackedEntities()) {
      trackedEntity.append(whereAnd.whereAnd()).append("TE.uid IN (:trackedEntities) ");
      sqlParameters.addValue("trackedEntities", UID.toValueSet(params.getTrackedEntities()));
    }

    if (params.hasTrackedEntityType()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid = :trackedEntityTypeId ");
      sqlParameters.addValue("trackedEntityTypeId", params.getTrackedEntityType().getId());
    } else if (!params.hasEnrolledInTrackerProgram()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid in (:trackedEntityTypeIds) ");
      sqlParameters.addValue(
          "trackedEntityTypeIds", getIdentifiers(params.getTrackedEntityTypes()));
    }

    if (params.hasLastUpdatedDuration()) {
      trackedEntity.append(whereAnd.whereAnd()).append(" TE.lastupdated >= :lastUpdatedDuration ");
      sqlParameters.addValue(
          "lastUpdatedDuration",
          timestampParameter(DateUtils.nowMinusDuration(params.getLastUpdatedDuration())));
    } else {
      if (params.hasLastUpdatedStartDate()) {
        trackedEntity
            .append(whereAnd.whereAnd())
            .append(" TE.lastupdated >= :lastUpdatedStartDate ");
        sqlParameters.addValue(
            "lastUpdatedStartDate", timestampParameter(params.getLastUpdatedStartDate()));
      }
      if (params.hasLastUpdatedEndDate()) {
        trackedEntity.append(whereAnd.whereAnd()).append(" TE.lastupdated <= :lastUpdatedEndDate ");
        sqlParameters.addValue(
            "lastUpdatedEndDate", timestampParameter(params.getLastUpdatedEndDate()));
      }
    }

    if (!params.isIncludeDeleted()) {
      trackedEntity.append(whereAnd.whereAnd()).append("TE.deleted IS FALSE ");
    }

    if (params.hasPotentialDuplicateFilter()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.potentialduplicate = :potentialDuplicate ");
      sqlParameters.addValue("potentialDuplicate", params.getPotentialDuplicate());
    }

    return trackedEntity.toString();
  }

  /**
   * Generates a single LEFT JOIN for each attribute used for filtering and sorting. The result of
   * this LEFT JOIN is used in the subquery projection and for ordering in both the subquery and the
   * main query.
   *
   * <p>Attribute filtering is handled in {@link #getWhereClauseFromFilterConditions(SqlHelper,
   * MapSqlParameterSource, TrackedEntityQueryParams)}.
   *
   * @return a SQL LEFT JOIN for the relevant attributes, or an empty string if none are provided.
   */
  private String getLeftJoinFromAttributes(TrackedEntityQueryParams params) {
    StringBuilder attributes = new StringBuilder();

    for (TrackedEntityAttribute attribute : params.getLeftJoinAttributes()) {
      String col = quote(attribute.getUid());
      attributes
          .append(" LEFT JOIN trackedentityattributevalue AS ")
          .append(col)
          .append(" ON ")
          .append(col)
          .append(".trackedentityid = TE.trackedentityid AND ")
          .append(col)
          .append(".trackedentityattributeid = ")
          .append(attribute.getId())
          .append(SPACE);
    }

    return attributes.toString();
  }

  private void addJoinOnProgramOwner(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      sql.append(
          """
          inner join trackedentityprogramowner PO \
          on PO.programid = :enrolledInTrackerProgram\
          and PO.trackedentityid = TE.trackedentityid \
          and P.programid = PO.programid""");
      sqlParameters.addValue(
          "enrolledInTrackerProgram", params.getEnrolledInTrackerProgram().getId());
      return;
    }

    sql.append(
        """
        left join trackedentityprogramowner PO on \
        PO.trackedentityid = TE.trackedentityid \
        and P.programid = PO.programid""");
  }

  /**
   * Generates an INNER JOIN for organisation units in a SQL query.
   *
   * <p>If a program is specified, the join is based on program ownership (PO). If no program is
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
   * besides making sure the user has ownership access to the TE, also covers the case where the org
   * unit is {@code ACCESSIBLE} or {@code CAPTURE}.
   *
   * @return a SQL INNER JOIN clause for organisation units
   */
  private String getFromSubQueryJoinOrgUnitConditions(
      MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();

    UserDetails userDetails = getCurrentUserDetails();
    Set<OrganisationUnit> effectiveSearchOrgUnits =
        getOrgUnitsFromUids(userDetails.getUserEffectiveSearchOrgUnitIds());
    Set<OrganisationUnit> captureScopeOrgUnits =
        getOrgUnitsFromUids(userDetails.getUserOrgUnitIds());

    orgUnits
        .append(" inner join organisationunit OU ")
        .append("on OU.organisationunitid = ")
        .append(getOwnerOrgUnit(params));

    if (params.hasOrganisationUnits()) {
      if (params.isOrganisationUnitMode(DESCENDANTS)) {
        orgUnits.append(getDescendantsQuery(params));
      } else if (params.isOrganisationUnitMode(CHILDREN)) {
        orgUnits.append(getChildrenQuery(params));
      } else if (params.isOrganisationUnitMode(SELECTED)) {
        orgUnits.append("and OU.organisationunitid in (:orgUnits) ");
        sqlParameters.addValue("orgUnits", getIdentifiers(params.getOrgUnits()));
      }
    }

    if (params.isOrganisationUnitMode(ALL) || getCurrentUserDetails().isSuper()) {
      return orgUnits.toString();
    }

    orgUnits.append(
        "and ((P.accesslevel in ('OPEN', 'AUDITED') and (OU.path like any (:effectiveSearchScopePaths))) ");
    sqlParameters.addValue(
        "effectiveSearchScopePaths", getOrgUnitsPathArray(effectiveSearchOrgUnits));

    orgUnits.append(
        "or (P.accesslevel in ('PROTECTED', 'CLOSED') and (OU.path like any (:captureScopePaths)))) ");
    sqlParameters.addValue("captureScopePaths", getOrgUnitsPathArray(captureScopeOrgUnits));

    return orgUnits.toString();
  }

  private Set<OrganisationUnit> getOrgUnitsFromUids(Set<String> uids) {
    return new HashSet<>(organisationUnitStore.getByUid(uids));
  }

  private String[] getOrgUnitsPathArray(Set<OrganisationUnit> orgUnits) {
    return orgUnits.stream().map(ou -> ou.getStoredPath() + "%").toArray(String[]::new);
  }

  private String getOwnerOrgUnit(TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      return "PO.organisationunitid ";
    }

    return "coalesce(PO.organisationunitid, TE.organisationunitid) ";
  }

  private String getDescendantsQuery(TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();
    SqlHelper orHlp = new SqlHelper(true);

    orgUnits.append("and (");

    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
      orgUnits
          .append(orHlp.or())
          .append("OU.path like '")
          .append(organisationUnit.getStoredPath())
          .append("%'");
    }

    orgUnits.append(") ");

    return orgUnits.toString();
  }

  private String getChildrenQuery(TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();
    SqlHelper orHlp = new SqlHelper(true);

    orgUnits.append("and (");

    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
      orgUnits
          .append(orHlp.or())
          .append(" OU.path like '")
          .append(organisationUnit.getStoredPath())
          .append("%'")
          .append(" and (ou.hierarchylevel = ")
          .append(organisationUnit.getHierarchyLevel())
          .append(" or ou.hierarchylevel = ")
          .append((organisationUnit.getHierarchyLevel() + 1))
          .append(")");
    }

    orgUnits.append(") ");

    return orgUnits.toString();
  }

  /**
   * Generates an INNER JOIN for enrollments. If the param we need to order by is enrolledAt, we
   * need to join the enrollment table to be able to select and order by this value. We restrict the
   * join condition to a specific program if specified in the request.
   *
   * @return a SQL INNER JOIN for enrollments
   */
  private String getFromSubQueryJoinEnrollmentConditions(TrackedEntityQueryParams params) {
    if (params.getOrder().stream()
        .filter(o -> o.getField() instanceof String)
        .noneMatch(p -> ENROLLMENT_DATE_KEY.equals(p.getField()))) {
      return "";
    }

    String join =
        """
          inner join enrollment %1$s
          on %1$s.trackedentityid = TE.trackedentityid
          """;
    return !params.hasEnrolledInTrackerProgram()
        ? join.formatted(ENROLLMENT_ALIAS)
        : join.concat(" and %1$s.programid = %2$s")
            .formatted(ENROLLMENT_ALIAS, params.getEnrolledInTrackerProgram().getId());
  }

  /**
   * Generates an EXISTS condition for enrollment (and event if specified). The EXIST will allow us
   * to filter by enrollments with a low overhead. This condition only applies when a program is
   * specified.
   *
   * @param whereAnd indicator tracking whether WHERE has been invoked or not
   * @return an SQL EXISTS clause for enrollment, or empty string if not program is specified.
   */
  private String getFromSubQueryEnrollmentConditions(
      SqlHelper whereAnd, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (!params.hasEnrolledInTrackerProgram()) {
      return "";
    }

    StringBuilder sql = new StringBuilder();
    sql.append(whereAnd.whereAnd())
        .append("exists (")
        .append("select en.trackedentityid ")
        .append("from enrollment en ");

    if (params.hasFilterForEvents()) {
      sql.append("inner join (");
      addEventFilter(sql, sqlParameters, params);
      sql.append(") EV on EV.enrollmentid = en.enrollmentid ");
    }

    sql.append("where en.trackedentityid = TE.trackedentityid ")
        .append("and en.programid = :enrolledProgramId ");
    sqlParameters.addValue("enrolledProgramId", params.getEnrolledInTrackerProgram().getId());

    if (params.hasEnrollmentStatus()) {
      sql.append("and en.status = :enrollmentStatus ");
      sqlParameters.addValue("enrollmentStatus", params.getEnrollmentStatus());
    }

    if (params.hasFollowUp()) {
      sql.append("and en.followup IS :followUp ");
      sqlParameters.addValue("followUp", params.getFollowUp());
    }

    if (params.hasProgramEnrollmentStartDate()) {
      sql.append("and en.enrollmentdate >= :enrollmentStartDate ");
      sqlParameters.addValue("enrollmentStartDate", params.getProgramEnrollmentStartDate());
    }

    if (params.hasProgramEnrollmentEndDate()) {
      sql.append("and en.enrollmentdate <= :enrollmentEndDate ");
      sqlParameters.addValue("enrollmentEndDate", params.getProgramEnrollmentEndDate());
    }

    if (params.hasProgramIncidentStartDate()) {
      sql.append("and en.occurreddate >= :occurredStartDate ");
      sqlParameters.addValue("occurredStartDate", params.getProgramIncidentStartDate());
    }

    if (params.hasProgramIncidentEndDate()) {
      sql.append("and en.occurreddate <= :occurredEndDate ");
      sqlParameters.addValue("occurredEndDate", params.getProgramIncidentEndDate());
    }

    if (!params.isIncludeDeleted()) {
      sql.append("and en.deleted is false ");
    }

    sql.append(") ");

    return sql.toString();
  }

  /**
   * Generates the WHERE-clause related to the user provided filters. It will find the tracked
   * entity attributes that match the given user filter criteria. This condition only applies when a
   * filter is specified.
   */
  private String getWhereClauseFromFilterConditions(
      SqlHelper hlp, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    if (params.getFilters().isEmpty()) {
      return "";
    }

    StringBuilder sql = new StringBuilder();
    String predicates = mapPredicatesToSql(params.getFilters(), sqlParameters);
    if (!predicates.isEmpty()) {
      sql.append(hlp.whereAnd());
      sql.append(predicates);
      sql.append(SPACE);
    }
    return sql.toString();
  }

  /** Adds event query with event related query params to given {@code sql}. */
  private void addEventFilter(
      StringBuilder sql, MapSqlParameterSource sqlParameters, TrackedEntityQueryParams params) {
    sql.append("select ev.enrollmentid ").append("from event ev ");

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
          "ev.status is not null and ev.occurreddate is null and date(now()) <= date(EV.scheduleddate) ");
    } else if (params.isEventStatus(EventStatus.OVERDUE)) {
      sql.append(
          "ev.status is not null and ev.occurreddate is null and date(now()) > date(EV.scheduleddate) ");
    }
  }

  private String getLimitClause(int limit) {
    return "limit " + limit;
  }

  /**
   * Generates the ORDER BY clause. This clause is used both in the sub-query and main query. When
   * using it in the sub-query, we want to make sure we get the right tracked entities. When we
   * order in the main query, it's to make sure we return the results in the correct order, since
   * order might be mixed after GROUP BY.
   *
   * @param innerOrder indicates whether this is the sub-query order by or main query order by
   * @return a SQL ORDER BY clause.
   */
  private String getQueryOrderBy(TrackedEntityQueryParams params, boolean innerOrder) {
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
        String orderField =
            innerOrder
                ? quote(tea.getUid()) + ".value "
                : MAIN_QUERY_ALIAS + "." + quote(tea.getUid());

        orderFields.add(orderField + SPACE + order.getDirection());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    if (!orderFields.isEmpty()) {
      return "order by " + StringUtils.join(orderFields, ',') + ", " + DEFAULT_ORDER + SPACE;
    }

    return "order by " + DEFAULT_ORDER + SPACE;
  }

  /**
   * Generates the LIMIT and OFFSET part of the sub-query. The limit is decided by the page size,
   * page offset and the system setting KeyTrackedEntityMaxLimit.
   *
   * <p>If the page parameters are not null, we use the page size and its offset. The validation in
   * {@link TrackedEntityOperationParamsMapper} guarantees that if the page parameters are set, the
   * page size will always be smaller than the system limit.
   *
   * <p>The limit is set in the sub-query, so the latter joins have fewer rows to consider.
   *
   * @return a SQL LIMIT and OFFSET clause, or empty string if no LIMIT can be determined.
   */
  private String getFromSubQueryLimitAndOffset(PageParams pageParams) {
    StringBuilder limitOffset = new StringBuilder();
    int systemMaxLimit = settingsProvider.getCurrentSettings().getTrackedEntityMaxLimit();

    if (pageParams != null) {
      return limitOffset
          .append(LIMIT)
          .append(SPACE)
          .append(pageParams.getPageSize() + 1) // get extra TE to determine if there is a nextPage
          .append(SPACE)
          .append(OFFSET)
          .append(SPACE)
          .append(pageParams.getOffset())
          .append(SPACE)
          .toString();
    } else if (systemMaxLimit > 0) {
      return limitOffset
          .append(LIMIT)
          .append(SPACE)
          .append(systemMaxLimit)
          .append(SPACE)
          .toString();
    }

    return limitOffset.toString();
  }

  /** Returns the maximum TE retrieval limit. 0 no limit. */
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
