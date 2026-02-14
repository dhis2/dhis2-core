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
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.JdbcPredicate.mapPredicatesToSql;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.util.DateUtils.toLongDate;
import static org.hisp.dhis.util.DateUtils.toLongDateWithMillis;
import static org.hisp.dhis.util.DateUtils.toLongGmtDate;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityStore")
class HibernateTrackedEntityStore extends SoftDeleteHibernateObjectStore<TrackedEntity> {

  private static final String MAIN_QUERY_ALIAS = "TE";

  private static final String ENROLLMENT_ALIAS = "EN";

  private static final String DEFAULT_ORDER = MAIN_QUERY_ALIAS + ".trackedentityid desc";

  private static final String OFFSET = "OFFSET";

  private static final String LIMIT = "LIMIT";

  private static final String ENROLLMENT_DATE_ALIAS = "en_enrollmentdate";

  private static final String ENROLLMENT_DATE_KEY = "enrollment.enrollmentDate";

  private static final String EV_OCCURREDDATE = "EV.occurreddate";

  private static final String INVALID_ORDER_FIELD_MESSAGE =
      "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.";

  private static final String EV_SCHEDULEDDATE = "EV.scheduleddate";

  private static final String IS_NULL = "IS NULL";

  private static final String IS_NOT_NULL = "IS NOT NULL";

  private static final String SPACE = " ";

  private static final String SINGLE_QUOTE = "'";

  private static final String EQUALS = " = ";

  private static final String EV_STATUS = "EV.status";

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

  public void updateTrackedEntitiesSyncTimestamp(Set<UID> trackedEntities, Date lastSynchronized) {
    if (trackedEntities.isEmpty()) {
      return;
    }

    String sql =
        """
              UPDATE trackedentity SET lastsynchronized = :lastSynchronized WHERE uid IN (:uids)
              """;

    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("lastSynchronized", new java.sql.Timestamp(lastSynchronized.getTime()))
            .addValue("uids", trackedEntities.stream().map(UID::toString).toList());

    namedParameterJdbcTemplate.update(sql, parameters);
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

  private String encodeAndQuote(Collection<String> elements) {
    return getQuotedCommaDelimitedString(elements.stream().map(SqlUtils::escape).toList());
  }

  public Long getTrackedEntityCount(TrackedEntityQueryParams params) {
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
    stringBuilder
        .append("FROM ")
        .append(getFromSubQuery(params, sqlParameters, false, pageParams))
        .append(getQueryOrderBy(params, false));
    // LIMIT must be in outer query for DISTINCT ON (applied after final ORDER BY)
    if (isOrderingByEnrolledAt(params)) {
      stringBuilder.append(getFromSubQueryLimitAndOffset(pageParams));
    }
    return stringBuilder.toString();
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

    if (isOrderingByEnrolledAt(params)) {
      select.add(ENROLLMENT_DATE_ALIAS);
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
    StringBuilder fromSubQuery =
        new StringBuilder()
            .append("(")
            .append(getFromSubQuerySelect(params))
            .append(" FROM trackedentity " + MAIN_QUERY_ALIAS + " ")

            // INNER JOIN on constraints
            .append(joinPrograms(params))
            .append(getFromSubQueryJoinProgramOwnerConditions(params))
            .append(getFromSubQueryJoinOrgUnitConditions(params, sqlParameters))
            .append(getFromSubQueryJoinEnrollmentConditions(params))

            // LEFT JOIN attributes we need to sort on and/or filter by.
            .append(getLeftJoinFromAttributes(params))

            // WHERE
            .append(getWhereClauseFromFilterConditions(params, sqlParameters, whereAnd))
            .append(getFromSubQueryTrackedEntityConditions(whereAnd, params))
            .append(getFromSubQueryEnrollmentConditions(whereAnd, params));

    if (!isCountQuery) {
      // DISTINCT ON requires ORDER BY to start with the DISTINCT columns
      if (isOrderingByEnrolledAt(params)) {
        fromSubQuery.append(getDistinctOnOrderBy(params));
        // LIMIT must be in outer query for DISTINCT ON (after final ORDER BY)
      } else {
        fromSubQuery
            .append(getQueryOrderBy(params, true))
            .append(getFromSubQueryLimitAndOffset(pageParams));
      }
    }

    return fromSubQuery.append(") ").append(MAIN_QUERY_ALIAS).append(" ").toString();
  }

  /**
   * Adds ORDER BY for DISTINCT ON queries. DISTINCT ON requires ORDER BY to start with the DISTINCT
   * columns (trackedentityid), followed by the enrollment date in the requested direction.
   */
  private String getDistinctOnOrderBy(TrackedEntityQueryParams params) {
    Order enrolledAtOrder = getEnrolledAtOrder(params);
    return "ORDER BY TE.trackedentityid, "
        + ENROLLMENT_ALIAS
        + ".enrollmentdate "
        + enrolledAtOrder.getDirection().name()
        + SPACE;
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
                  INVALID_ORDER_FIELD_MESSAGE,
                  field,
                  String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
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
                INVALID_ORDER_FIELD_MESSAGE,
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    // When ordering by enrolledAt, use DISTINCT ON to pick one enrollment per TE.
    // This fixes pagination when a TE has multiple enrollments (DHIS2-20811).
    if (isOrderingByEnrolledAt(params)) {
      return "SELECT DISTINCT ON (TE.trackedentityid) " + String.join(", ", columns);
    }
    return "SELECT DISTINCT " + String.join(", ", columns);
  }

  private String joinPrograms(TrackedEntityQueryParams params) {
    StringBuilder trackedEntity = new StringBuilder();

    trackedEntity.append(" INNER JOIN program P ");
    trackedEntity.append(" ON P.trackedentitytypeid = TE.trackedentitytypeid ");

    if (!params.hasEnrolledInTrackerProgram()) {
      trackedEntity
          .append("AND P.programid IN (")
          .append(getCommaDelimitedString(getIdentifiers(params.getAccessibleTrackerPrograms())))
          .append(")");
    }

    return trackedEntity.toString();
  }

  /**
   * Generates the WHERE-clause of the sub-query SQL related to tracked entities.
   *
   * @param whereAnd tracking if where has been invoked or not
   * @return a SQL segment for the WHERE clause used in the sub-query
   */
  private String getFromSubQueryTrackedEntityConditions(
      SqlHelper whereAnd, TrackedEntityQueryParams params) {
    StringBuilder trackedEntity = new StringBuilder();

    if (params.hasTrackedEntities()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.uid IN (")
          .append(encodeAndQuote(UID.toValueSet(params.getTrackedEntities())))
          .append(") ");
    }

    if (params.hasTrackedEntityType()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid = ")
          .append(params.getTrackedEntityType().getId());
    } else if (!params.hasEnrolledInTrackerProgram()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid in (")
          .append(getCommaDelimitedString(getIdentifiers(params.getTrackedEntityTypes())))
          .append(")");
    }

    if (params.hasLastUpdatedDuration()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append(" TE.lastupdated >= '")
          .append(toLongGmtDate(DateUtils.nowMinusDuration(params.getLastUpdatedDuration())))
          .append(SINGLE_QUOTE);
    } else {
      if (params.hasLastUpdatedStartDate()) {
        trackedEntity
            .append(whereAnd.whereAnd())
            .append(" TE.lastupdated >= '")
            .append(toLongDateWithMillis(params.getLastUpdatedStartDate()))
            .append(SINGLE_QUOTE);
      }
      if (params.hasLastUpdatedEndDate()) {
        trackedEntity
            .append(whereAnd.whereAnd())
            .append(" TE.lastupdated <= '")
            .append(toLongDateWithMillis(params.getLastUpdatedEndDate()))
            .append(SINGLE_QUOTE);
      }
    }

    if (params.isSynchronizationQuery()) {
      trackedEntity.append(whereAnd.whereAnd()).append(" TE.lastupdated > TE.lastsynchronized ");
      if (params.getSkipChangedBefore() != null) {
        trackedEntity
            .append(" AND TE.lastupdated >= '")
            .append(toLongDate(params.getSkipChangedBefore()))
            .append(SINGLE_QUOTE);
      }
    }

    if (!params.isIncludeDeleted()) {
      trackedEntity.append(whereAnd.whereAnd()).append("TE.deleted IS FALSE ");
    }

    if (params.hasPotentialDuplicateFilter()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.potentialduplicate=")
          .append(params.getPotentialDuplicate())
          .append(SPACE);
    }

    return trackedEntity.toString();
  }

  /**
   * Generates a single LEFT JOIN for each attribute used for filtering and sorting. The result of
   * this LEFT JOIN is used in the subquery projection and for ordering in both the subquery and the
   * main query.
   *
   * <p>Attribute filtering is handled in {@link
   * #getWhereClauseFromFilterConditions(TrackedEntityQueryParams, MapSqlParameterSource,
   * SqlHelper)}.
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

  /**
   * Generates an INNER JOIN for program owner. This segment is only included if program is
   * specified.
   *
   * @return a SQL INNER JOIN for program owner, or a LEFT JOIN if no program is specified.
   */
  private String getFromSubQueryJoinProgramOwnerConditions(TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      return " INNER JOIN trackedentityprogramowner PO "
          + " ON PO.programid = "
          + params.getEnrolledInTrackerProgram().getId()
          + " AND PO.trackedentityid = TE.trackedentityid "
          + " AND P.programid = PO.programid";
    }

    return "LEFT JOIN trackedentityprogramowner PO ON "
        + " PO.trackedentityid = TE.trackedentityid"
        + " AND P.programid = PO.programid";
  }

  /**
   * Generates an INNER JOIN for organisation units. If a program is specified, we join on program
   * ownership (PO), if not we check whether the user has access to the TE/program pair owner. Based
   * on the ouMode, they will boil down to either DESCENDANTS (requiring matching on the org unit's
   * PATH), CHILDREN (matching on the org unit's PATH or any of its immediate children), SELECTED
   * (matching the specified org unit id) or ALL (no constraints).
   *
   * @return a SQL INNER JOIN for organisation units
   */
  private String getFromSubQueryJoinOrgUnitConditions(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters) {
    StringBuilder orgUnits = new StringBuilder();
    String orgUnitTableAlias = "ou";
    String programTableAlias = "p";

    handleOrganisationUnits(params);

    orgUnits
        .append(" inner join organisationunit ")
        .append(orgUnitTableAlias)
        .append(" on ou.organisationunitid = ")
        .append(getOwnerOrgUnit(params));

    if (params.hasOrganisationUnits()) {
      if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
        orgUnits.append(getDescendantsQuery(params));
      } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
        orgUnits.append(getChildrenQuery(params));
      } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.SELECTED)) {
        orgUnits.append(getSelectedQuery(params));
      }
    }

    buildOwnershipClause(
        orgUnits,
        sqlParameters,
        params.getOrgUnitMode(),
        programTableAlias,
        orgUnitTableAlias,
        MAIN_QUERY_ALIAS,
        () -> "and ");

    return orgUnits.toString();
  }

  /**
   * Prepares the organisation units of the given parameters to simplify querying. Mode ACCESSIBLE
   * is converted to DESCENDANTS for organisation units linked to the search scope of the given
   * user. Mode CAPTURE is converted to DESCENDANTS too, but using organisation units linked to the
   * user's capture scope, and mode CHILDREN is converted to SELECTED for organisation units
   * including all their children. Mode can be DESCENDANTS, SELECTED, ALL only after invoking this
   * method.
   */
  private void handleOrganisationUnits(TrackedEntityQueryParams params) {
    UserDetails user = getCurrentUserDetails();
    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.ACCESSIBLE)) {
      params.setOrgUnits(
          new HashSet<>(organisationUnitStore.getByUid(user.getUserEffectiveSearchOrgUnitIds())));
      params.setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CAPTURE)) {
      params.setOrgUnits(new HashSet<>(organisationUnitStore.getByUid(user.getUserOrgUnitIds())));
      params.setOrgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS);
    }
  }

  private String getOwnerOrgUnit(TrackedEntityQueryParams params) {
    if (params.hasEnrolledInTrackerProgram()) {
      return "PO.organisationunitid ";
    }

    return "COALESCE(PO.organisationunitid, TE.organisationunitid) ";
  }

  private String getDescendantsQuery(TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();
    SqlHelper orHlp = new SqlHelper(true);

    orgUnits.append("AND (");

    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
      orgUnits
          .append(orHlp.or())
          .append("OU.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%'");
    }

    orgUnits.append(") ");

    return orgUnits.toString();
  }

  private String getChildrenQuery(TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();
    SqlHelper orHlp = new SqlHelper(true);

    orgUnits.append("AND (");

    for (OrganisationUnit organisationUnit : params.getOrgUnits()) {
      orgUnits
          .append(orHlp.or())
          .append(" OU.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%'")
          .append(" AND (ou.hierarchylevel = ")
          .append(organisationUnit.getHierarchyLevel())
          .append(" OR ou.hierarchylevel = ")
          .append((organisationUnit.getHierarchyLevel() + 1))
          .append(")");
    }

    orgUnits.append(") ");

    return orgUnits.toString();
  }

  private String getSelectedQuery(TrackedEntityQueryParams params) {
    return "AND OU.organisationunitid IN ("
        + getCommaDelimitedString(getIdentifiers(params.getOrgUnits()))
        + ") ";
  }

  /**
   * Adds an INNER JOIN on enrollments when ordering by {@code enrolledAt}.
   *
   * <p>Query strategy depends on enrollment usage:
   *
   * <ul>
   *   <li>No enrollment filters, no order by enrolledAt: no enrollment table needed
   *   <li>Enrollment filters, no order by enrolledAt: EXISTS subquery via {@link
   *       #getFromSubQueryEnrollmentConditions} (short-circuits, avoids duplicates)
   *   <li>Order by enrolledAt: JOIN with DISTINCT ON (this method)
   * </ul>
   *
   * <p>When ordering by enrolledAt, filters must be in the JOIN (not EXISTS) to ensure ordering
   * uses a matching enrollment. A TE can have multiple enrollments, so DISTINCT ON
   * (TE.trackedentityid) picks one row per TE. DISTINCT ON requires ORDER BY to start with the
   * DISTINCT columns, so inner query must order by (trackedentityid, enrollmentdate) - not the
   * user's requested order. Outer query applies the user's order (enrollmentdate), so LIMIT must be
   * in outer query (after final ORDER BY).
   *
   * @return a SQL INNER JOIN for enrollments
   */
  private String getFromSubQueryJoinEnrollmentConditions(TrackedEntityQueryParams params) {
    if (!isOrderingByEnrolledAt(params)) {
      return "";
    }
    if (!params.hasEnrolledInTrackerProgram()) {
      throw new IllegalArgumentException(
          "Program is required when ordering by enrollment.enrollmentDate");
    }

    StringBuilder sql = new StringBuilder();
    sql.append(" INNER JOIN enrollment ")
        .append(ENROLLMENT_ALIAS)
        .append(" ON ")
        .append(ENROLLMENT_ALIAS)
        .append(".trackedentityid = TE.trackedentityid");

    sql.append(" AND ")
        .append(ENROLLMENT_ALIAS)
        .append(".programid = ")
        .append(params.getEnrolledInTrackerProgram().getId());

    appendEnrollmentFilterConditions(sql, params);

    if (params.hasFilterForEvents()) {
      sql.append(" AND EXISTS (");
      appendEventExistsForEnrollmentJoin(sql, params);
      sql.append(")");
    }

    return sql.toString();
  }

  /** Appends enrollment filter conditions to SQL. Used by both JOIN and EXISTS paths. */
  private void appendEnrollmentFilterConditions(
      StringBuilder sql, TrackedEntityQueryParams params) {
    if (params.hasEnrollmentStatus()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".status = '")
          .append(params.getEnrollmentStatus())
          .append("'");
    }
    if (params.hasFollowUp()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".followup IS ")
          .append(params.getFollowUp());
    }
    if (params.hasProgramEnrollmentStartDate()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".enrollmentdate >= '")
          .append(toLongDateWithMillis(params.getProgramEnrollmentStartDate()))
          .append("'");
    }
    if (params.hasProgramEnrollmentEndDate()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".enrollmentdate <= '")
          .append(toLongDateWithMillis(params.getProgramEnrollmentEndDate()))
          .append("'");
    }
    if (params.hasProgramIncidentStartDate()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".occurreddate >= '")
          .append(toLongDateWithMillis(params.getProgramIncidentStartDate()))
          .append("'");
    }
    if (params.hasProgramIncidentEndDate()) {
      sql.append(" AND ")
          .append(ENROLLMENT_ALIAS)
          .append(".occurreddate <= '")
          .append(toLongDateWithMillis(params.getProgramIncidentEndDate()))
          .append("'");
    }
    if (!params.isIncludeDeleted()) {
      sql.append(" AND ").append(ENROLLMENT_ALIAS).append(".deleted IS FALSE");
    }
  }

  /**
   * Adds an EXISTS subquery for event filters to be used in the enrollment JOIN condition. This
   * ensures we only consider enrollments that have matching events when ordering by enrolledAt.
   */
  private void appendEventExistsForEnrollmentJoin(
      StringBuilder sql, TrackedEntityQueryParams params) {
    sql.append("SELECT 1 FROM event EV ");

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      sql.append("INNER JOIN (")
          .append("SELECT userinfoid AS userid FROM userinfo WHERE uid IN (")
          .append(
              encodeAndQuote(UID.toValueSet(params.getAssignedUserQueryParam().getAssignedUsers())))
          .append(")) AU ON AU.userid = EV.assigneduserid ");
    }

    sql.append("WHERE EV.enrollmentid = ").append(ENROLLMENT_ALIAS).append(".enrollmentid");

    if (params.hasEventStatus()) {
      String start = toLongDateWithMillis(params.getEventStartDate());
      String end = toLongDateWithMillis(params.getEventEndDate());

      if (params.isEventStatus(EventStatus.COMPLETED)) {
        sql.append(" AND EV.occurreddate >= '")
            .append(start)
            .append("' AND EV.occurreddate <= '")
            .append(end)
            .append("'");
        sql.append(" AND EV.status = '").append(EventStatus.COMPLETED.name()).append("'");
      } else if (params.isEventStatus(EventStatus.VISITED)
          || params.isEventStatus(EventStatus.ACTIVE)) {
        sql.append(" AND EV.occurreddate >= '")
            .append(start)
            .append("' AND EV.occurreddate <= '")
            .append(end)
            .append("'");
        sql.append(" AND EV.status = '").append(EventStatus.ACTIVE.name()).append("'");
      } else if (params.isEventStatus(EventStatus.SCHEDULE)) {
        sql.append(" AND EV.scheduleddate >= '")
            .append(start)
            .append("' AND EV.scheduleddate <= '")
            .append(end)
            .append("'");
        sql.append(
            " AND EV.status IS NOT NULL AND EV.occurreddate IS NULL AND date(now()) <= date(EV.scheduleddate)");
      } else if (params.isEventStatus(EventStatus.OVERDUE)) {
        sql.append(" AND EV.scheduleddate >= '")
            .append(start)
            .append("' AND EV.scheduleddate <= '")
            .append(end)
            .append("'");
        sql.append(
            " AND EV.status IS NOT NULL AND EV.occurreddate IS NULL AND date(now()) > date(EV.scheduleddate)");
      } else if (params.isEventStatus(EventStatus.SKIPPED)) {
        sql.append(" AND EV.scheduleddate >= '")
            .append(start)
            .append("' AND EV.scheduleddate <= '")
            .append(end)
            .append("'");
        sql.append(" AND EV.status = '").append(EventStatus.SKIPPED.name()).append("'");
      }
    }

    if (params.hasProgramStage()) {
      sql.append(" AND EV.programstageid = ").append(params.getProgramStage().getId());
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      sql.append(" AND EV.assigneduserid IS NULL");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      sql.append(" AND EV.assigneduserid IS NOT NULL");
    }

    if (!params.isIncludeDeleted()) {
      sql.append(" AND EV.deleted IS FALSE");
    }
  }

  /**
   * Generates an EXISTS condition for enrollment (and event if specified). The EXIST will allow us
   * to filter by enrollments with a low overhead. This condition only applies when a program is
   * specified.
   *
   * <p>When ordering by enrolledAt, the enrollment JOIN already includes all filters, so this
   * EXISTS is skipped to avoid redundant checks.
   *
   * @param whereAnd indicator tracking whether WHERE has been invoked or not
   * @return an SQL EXISTS clause for enrollment, or empty string if not program is specified.
   */
  private String getFromSubQueryEnrollmentConditions(
      SqlHelper whereAnd, TrackedEntityQueryParams params) {
    StringBuilder program = new StringBuilder();

    if (!params.hasEnrolledInTrackerProgram()) {
      return "";
    }
    // When ordering by enrolledAt, the enrollment JOIN already includes all filters
    if (isOrderingByEnrolledAt(params)) {
      return "";
    }

    program
        .append(whereAnd.whereAnd())
        .append("EXISTS (")
        .append("SELECT ")
        .append(ENROLLMENT_ALIAS)
        .append(".trackedentityid ")
        .append("FROM enrollment ")
        .append(ENROLLMENT_ALIAS)
        .append(SPACE);

    if (params.hasFilterForEvents()) {
      program.append(getFromSubQueryEvent(params));
    }

    program
        .append("WHERE ")
        .append(ENROLLMENT_ALIAS)
        .append(".trackedentityid = TE.trackedentityid ")
        .append("AND ")
        .append(ENROLLMENT_ALIAS)
        .append(".programid = ")
        .append(params.getEnrolledInTrackerProgram().getId());

    appendEnrollmentFilterConditions(program, params);

    program.append(") ");

    return program.toString();
  }

  /**
   * Generates the WHERE-clause related to the user provided filters. It will find the tracked
   * entity attributes that match the given user filter criteria. This condition only applies when a
   * filter is specified.
   */
  private String getWhereClauseFromFilterConditions(
      TrackedEntityQueryParams params, MapSqlParameterSource sqlParameters, SqlHelper hlp) {
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

  /**
   * Generates an INNER JOIN with the enrollments if event-filters are specified. In the case of
   * user assignment is part of the filter, we join with the userinfo table as well.
   *
   * @return an SQL INNER JOIN for filtering on events.
   */
  private String getFromSubQueryEvent(TrackedEntityQueryParams params) {
    StringBuilder events = new StringBuilder();
    SqlHelper whereHlp = new SqlHelper(true);

    events.append("INNER JOIN (").append("SELECT EV.enrollmentid ").append("FROM event EV ");

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      events
          .append("INNER JOIN (")
          .append("SELECT userinfoid AS userid ")
          .append("FROM userinfo ")
          .append("WHERE uid IN (")
          .append(
              encodeAndQuote(UID.toValueSet(params.getAssignedUserQueryParam().getAssignedUsers())))
          .append(") ")
          .append(") AU ON AU.userid = EV.assigneduserid");
    }

    if (params.hasEventStatus()) {
      String start = toLongDateWithMillis(params.getEventStartDate());
      String end = toLongDateWithMillis(params.getEventEndDate());

      if (params.isEventStatus(EventStatus.COMPLETED)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_OCCURREDDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(EventStatus.COMPLETED.name())
            .append(SINGLE_QUOTE)
            .append(SPACE);
      } else if (params.isEventStatus(EventStatus.VISITED)
          || params.isEventStatus(EventStatus.ACTIVE)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_OCCURREDDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(EventStatus.ACTIVE.name())
            .append(SINGLE_QUOTE)
            .append(SPACE);
      } else if (params.isEventStatus(EventStatus.SCHEDULE)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_SCHEDULEDDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(SPACE)
            .append(IS_NOT_NULL)
            .append(whereHlp.whereAnd())
            .append(EV_OCCURREDDATE)
            .append(SPACE)
            .append(IS_NULL)
            .append(whereHlp.whereAnd())
            .append("date(now()) <= date(EV.scheduleddate) ");
      } else if (params.isEventStatus(EventStatus.OVERDUE)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_SCHEDULEDDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(SPACE)
            .append(IS_NOT_NULL)
            .append(whereHlp.whereAnd())
            .append(EV_OCCURREDDATE)
            .append(SPACE)
            .append(IS_NULL)
            .append(whereHlp.whereAnd())
            .append("date(now()) > date(EV.scheduleddate) ");
      } else if (params.isEventStatus(EventStatus.SKIPPED)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_SCHEDULEDDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(EventStatus.SKIPPED.name())
            .append(SINGLE_QUOTE)
            .append(SPACE);
      }
    }

    if (params.hasProgramStage()) {
      events
          .append(whereHlp.whereAnd())
          .append("EV.programstageid = ")
          .append(params.getProgramStage().getId())
          .append(SPACE);
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      events.append(whereHlp.whereAnd()).append("EV.assigneduserid IS NULL ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      events.append(whereHlp.whereAnd()).append("EV.assigneduserid IS NOT NULL ");
    }

    if (!params.isIncludeDeleted()) {
      events.append(whereHlp.whereAnd()).append("EV.deleted IS FALSE");
    }

    events.append(") EV ON EV.enrollmentid = ").append(ENROLLMENT_ALIAS).append(".enrollmentid ");

    return events.toString();
  }

  /**
   * Helper method for making a date condition. The format is "[WHERE|AND] date >= start AND date <=
   * end".
   *
   * @param whereHelper tracking whether WHERE has been invoked or not
   * @param column the column for filter on
   * @param start the start date
   * @param end the end date
   * @return a SQL filter for finding dates between two dates.
   */
  private String getQueryDateConditionBetween(
      SqlHelper whereHelper, String column, String start, String end) {
    return whereHelper.whereAnd()
        + column
        + " >= '"
        + start
        + SINGLE_QUOTE
        + whereHelper.whereAnd()
        + column
        + " <= '"
        + end
        + "' ";
  }

  private String getLimitClause(int limit) {
    return "LIMIT " + limit;
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
                  INVALID_ORDER_FIELD_MESSAGE,
                  field,
                  String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
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
                INVALID_ORDER_FIELD_MESSAGE,
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    if (!orderFields.isEmpty()) {
      return "ORDER BY " + StringUtils.join(orderFields, ',') + ", " + DEFAULT_ORDER + SPACE;
    }

    return "ORDER BY " + DEFAULT_ORDER + SPACE;
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
