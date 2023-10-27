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
package org.hisp.dhis.tracker.export.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Map.entry;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.commons.util.TextUtils.getCommaDelimitedString;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.addDays;
import static org.hisp.dhis.util.DateUtils.getLongGmtDateString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.hibernate.SoftDeleteHibernateObjectStore;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("org.hisp.dhis.tracker.export.trackedentity.TrackedEntityStore")
class HibernateTrackedEntityStore extends SoftDeleteHibernateObjectStore<TrackedEntity>
    implements TrackedEntityStore {

  private static final String MAIN_QUERY_ALIAS = "TE";

  private static final String ENROLLMENT_ALIAS = "en";

  private static final String DEFAULT_ORDER = MAIN_QUERY_ALIAS + ".trackedentityid desc";

  private static final String OFFSET = "OFFSET";

  private static final String LIMIT = "LIMIT";

  private static final String ENROLLMENT_DATE_ALIAS = "en_enrollmentdate";

  private static final String ENROLLMENT_DATE_KEY = "enrollment.enrollmentDate";

  private static final String EV_EXECUTIONDATE = "EV.executiondate";

  private static final String EV_DUEDATE = "EV.duedate";

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

  private final StatementBuilder statementBuilder;

  private final SystemSettingManager systemSettingManager;

  public HibernateTrackedEntityStore(
      SessionFactory sessionFactory,
      JdbcTemplate jdbcTemplate,
      ApplicationEventPublisher publisher,
      CurrentUserService currentUserService,
      AclService aclService,
      StatementBuilder statementBuilder,
      OrganisationUnitStore organisationUnitStore,
      SystemSettingManager systemSettingManager) {
    super(
        sessionFactory,
        jdbcTemplate,
        publisher,
        TrackedEntity.class,
        currentUserService,
        aclService,
        false);

    checkNotNull(statementBuilder);
    checkNotNull(organisationUnitStore);
    checkNotNull(systemSettingManager);

    this.statementBuilder = statementBuilder;
    this.organisationUnitStore = organisationUnitStore;
    this.systemSettingManager = systemSettingManager;
  }

  @Override
  public List<Long> getTrackedEntityIds(TrackedEntityQueryParams params) {
    String sql = getQuery(params, null);
    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

    checkMaxTrackedEntityCountReached(params, rowSet);

    List<Long> ids = new ArrayList<>();

    while (rowSet.next()) {
      ids.add(rowSet.getLong("trackedentityid"));
    }

    return ids;
  }

  @Override
  public Page<Long> getTrackedEntityIds(TrackedEntityQueryParams params, PageParams pageParams) {
    String sql = getQuery(params, pageParams);
    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

    checkMaxTrackedEntityCountReached(params, rowSet);

    List<Long> ids = new ArrayList<>();

    while (rowSet.next()) {
      ids.add(rowSet.getLong("trackedentityid"));
    }

    IntSupplier teCount = () -> getTrackedEntityCount(params);
    return getPage(pageParams, ids, teCount);
  }

  private Page<Long> getPage(PageParams pageParams, List<Long> teIds, IntSupplier enrollmentCount) {
    if (pageParams.isPageTotal()) {
      Pager pager =
          new Pager(pageParams.getPage(), enrollmentCount.getAsInt(), pageParams.getPageSize());
      return Page.of(teIds, pager);
    }

    Pager pager = new Pager(pageParams.getPage(), 0, pageParams.getPageSize());
    pager.force(pageParams.getPage(), pageParams.getPageSize());
    return Page.of(teIds, pager);
  }

  @Override
  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  private String encodeAndQuote(Collection<String> elements) {
    return getQuotedCommaDelimitedString(
        elements.stream()
            .map(element -> statementBuilder.encode(element, false))
            .collect(Collectors.toList()));
  }

  private void checkMaxTrackedEntityCountReached(
      TrackedEntityQueryParams params, SqlRowSet rowSet) {
    if (params.getMaxTeLimit() > 0 && rowSet.last()) {
      if (rowSet.getRow() > params.getMaxTeLimit()) {
        throw new IllegalQueryException("maxteicountreached");
      }
      rowSet.beforeFirst();
    }
  }

  @Override
  public int getTrackedEntityCount(TrackedEntityQueryParams params) {
    String sql = getCountQuery(params);
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }

  @Override
  public int getTrackedEntityCountWithMaxTrackedEntityLimit(TrackedEntityQueryParams params) {
    String sql = getCountQueryWithMaxTrackedEntityLimit(params);
    return jdbcTemplate.queryForObject(sql, Integer.class);
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
   * <p>select (subquery_projection) from (tracked entity instances) inner join
   * (attribute_constraints) [inner join (program_owner)] inner join (organisation units) left join
   * (attribute_orderby) where exist(program_constraint) order by (order) limit (limit_offset)
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
   * executiondate) which speeds up the lookup significantly order: Order is used both in the
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
   *
   * @param params params defining the query
   * @return SQL string
   */
  private String getQuery(TrackedEntityQueryParams params, PageParams pageParams) {
    StringBuilder stringBuilder = new StringBuilder(getQuerySelect(params));
    return stringBuilder
        .append("FROM ")
        .append(getFromSubQuery(params, false, pageParams))
        .append(getQueryOrderBy(params, false))
        .toString();
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count and
   * ignores order and limit
   *
   * @param params params defining the query
   * @return a count SQL query
   */
  private String getCountQuery(TrackedEntityQueryParams params) {
    return SELECT_COUNT_INSTANCE_FROM
        + getQuerySelect(params)
        + "FROM "
        + getFromSubQuery(params, true, null)
        + " ) tecount";
  }

  /**
   * Uses the same basis as the getQuery method, but replaces the projection with a count, ignores
   * order but uses the TE limit set on the program if higher than 0
   *
   * @param params params defining the query
   * @return a count SQL query
   */
  private String getCountQueryWithMaxTrackedEntityLimit(TrackedEntityQueryParams params) {
    return SELECT_COUNT_INSTANCE_FROM
        + getQuerySelect(params)
        + "FROM "
        + getFromSubQuery(params, true, null)
        + (params.getProgram().getMaxTeiCountToReturn() > 0
            ? getLimitClause(params.getProgram().getMaxTeiCountToReturn() + 1)
            : "")
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
                "TE.orgunit_uid",
                "TE.orgunit_name",
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
   * Generates the SQL of the sub-query, used to find the correct subset of tracked entity instances
   * to return. Orchestrates all the different segments of the SQL into a complete sub-query.
   *
   * @return an SQL sub-query
   */
  private String getFromSubQuery(
      TrackedEntityQueryParams params, boolean isCountQuery, PageParams pageParams) {
    SqlHelper whereAnd = new SqlHelper(true);
    StringBuilder fromSubQuery =
        new StringBuilder()
            .append("(")
            .append(getFromSubQuerySelect(params))
            .append(" FROM trackedentity " + MAIN_QUERY_ALIAS + " ")

            // INNER JOIN on constraints
            .append(joinAttributeValue(params))
            .append(getFromSubQueryJoinProgramOwnerConditions(params))
            .append(getFromSubQueryJoinOrgUnitConditions(params))
            .append(getFromSubQueryJoinEnrollmentConditions(params))

            // LEFT JOIN attributes we need to sort on.
            .append(getFromSubQueryJoinOrderByAttributes(params))

            // WHERE
            .append(getFromSubQueryTrackedEntityConditions(whereAnd, params))
            .append(getFromSubQueryEnrollmentConditions(whereAnd, params));

    if (!isCountQuery) {
      // SORT
      fromSubQuery
          .append(getQueryOrderBy(params, true))
          // LIMIT, OFFSET
          .append(getFromSubQueryLimitAndOffset(params, pageParams));
    }

    return fromSubQuery.append(") TE ").toString();
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
                "TE.deleted as deleted",
                "OU.uid as orgunit_uid",
                "OU.name as orgunit_name"));

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
        columns.add(
            statementBuilder.columnQuote(tea.getUid())
                + ".value AS "
                + statementBuilder.columnQuote(tea.getUid()));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    return "SELECT " + String.join(", ", columns);
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

    if (params.hasTrackedEntityType()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid = ")
          .append(params.getTrackedEntityType().getId())
          .append(SPACE);
    } else if (!CollectionUtils.isEmpty(params.getTrackedEntityTypes())) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.trackedentitytypeid IN (")
          .append(getCommaDelimitedString(getIdentifiers(params.getTrackedEntityTypes())))
          .append(") ");
    }

    if (params.hasTrackedEntities()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append("TE.uid IN (")
          .append(encodeAndQuote(params.getTrackedEntityUids()))
          .append(") ");
    }

    if (params.hasLastUpdatedDuration()) {
      trackedEntity
          .append(whereAnd.whereAnd())
          .append(" TE.lastupdated >= '")
          .append(getLongGmtDateString(DateUtils.nowMinusDuration(params.getLastUpdatedDuration())))
          .append(SINGLE_QUOTE);
    } else {
      if (params.hasLastUpdatedStartDate()) {
        trackedEntity
            .append(whereAnd.whereAnd())
            .append(" TE.lastupdated >= '")
            .append(getMediumDateString(params.getLastUpdatedStartDate()))
            .append(SINGLE_QUOTE);
      }
      if (params.hasLastUpdatedEndDate()) {
        trackedEntity
            .append(whereAnd.whereAnd())
            .append(" TE.lastupdated < '")
            .append(getMediumDateString(addDays(params.getLastUpdatedEndDate(), 1)))
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
   * Generates a single INNER JOIN for each attribute we are searching on. We can search by a range
   * of operators. All searching is using lower() since attribute values are case-insensitive.
   */
  private String joinAttributeValue(TrackedEntityQueryParams params) {
    StringBuilder attributes = new StringBuilder();

    for (Map.Entry<TrackedEntityAttribute, List<QueryFilter>> filters :
        params.getFilters().entrySet()) {
      String col = statementBuilder.columnQuote(filters.getKey().getUid());
      String teaId = col + ".trackedentityattributeid";
      String teav = "lower(" + col + ".value)";
      String ted = col + ".trackedentityid";

      attributes
          .append(" INNER JOIN trackedentityattributevalue ")
          .append(col)
          .append(" ON ")
          .append(teaId)
          .append(EQUALS)
          .append(filters.getKey().getId())
          .append(" AND ")
          .append(ted)
          .append(" = TE.trackedentityid ");

      for (QueryFilter filter : filters.getValue()) {
        String encodedFilter = statementBuilder.encode(filter.getFilter(), false);
        attributes
            .append("AND ")
            .append(teav)
            .append(SPACE)
            .append(filter.getSqlOperator())
            .append(SPACE)
            .append(StringUtils.lowerCase(filter.getSqlFilter(encodedFilter)));
      }
    }

    return attributes.toString();
  }

  /**
   * Generates the LEFT JOINs used for attributes we are ordering by (If any). We use LEFT JOIN to
   * avoid removing any rows if there is no value for a given attribute and te. The result of this
   * LEFT JOIN is used in the sub-query projection, and ordering in the sub-query and main query.
   *
   * @return a SQL LEFT JOIN for attributes used for ordering, or empty string if no attributes is
   *     used in order.
   */
  private String getFromSubQueryJoinOrderByAttributes(TrackedEntityQueryParams params) {
    StringBuilder joinOrderAttributes = new StringBuilder();

    for (TrackedEntityAttribute orderAttribute : params.getLeftJoinAttributes()) {

      joinOrderAttributes
          .append(" LEFT JOIN trackedentityattributevalue AS ")
          .append(statementBuilder.columnQuote(orderAttribute.getUid()))
          .append(" ON ")
          .append(statementBuilder.columnQuote(orderAttribute.getUid()))
          .append(".trackedentityid = TE.trackedentityid ")
          .append("AND ")
          .append(statementBuilder.columnQuote(orderAttribute.getUid()))
          .append(".trackedentityattributeid = ")
          .append(orderAttribute.getId())
          .append(SPACE);
    }

    return joinOrderAttributes.toString();
  }

  /**
   * Generates an INNER JOIN for program owner. This segment is only included if program is
   * specified or user is not super.
   *
   * @return a SQL INNER JOIN for program owner, or empty string if no program is specified.
   */
  private String getFromSubQueryJoinProgramOwnerConditions(TrackedEntityQueryParams params) {
    if (!params.hasProgram() || skipOwnershipCheck(params)) {
      return "";
    }

    return " INNER JOIN trackedentityprogramowner PO "
        + "ON PO.programid = "
        + params.getProgram().getId()
        + " AND PO.trackedentityid = TE.trackedentityid ";
  }

  /**
   * Generates an INNER JOIN for organisation units. If a program is specified, we join on program
   * ownership (PO), if not we join by tracked entity (TE). Based on the ouMode, they will boil down
   * to either DESCENDANTS (requiring matching on PATH), ALL (No constraints) or not DESCENDANTS or
   * ALL (SELECTED) which will match against a collection of ids.
   *
   * @return a SQL INNER JOIN for organisation units
   */
  private String getFromSubQueryJoinOrgUnitConditions(TrackedEntityQueryParams params) {
    StringBuilder orgUnits = new StringBuilder();

    params.handleOrganisationUnits();

    orgUnits
        .append(" INNER JOIN organisationunit OU ")
        .append("ON OU.organisationunitid = ")
        .append(
            params.hasProgram() && !skipOwnershipCheck(params)
                ? "PO.organisationunitid "
                : "TE.organisationunitid ");

    if (!params.hasOrganisationUnits()) {
      return orgUnits.toString();
    }

    if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
      SqlHelper orHlp = new SqlHelper(true);

      orgUnits.append("AND (");

      for (OrganisationUnit organisationUnit : params.getOrgUnits()) {

        OrganisationUnit ou = organisationUnitStore.getByUid(organisationUnit.getUid());
        if (ou != null) {
          orgUnits.append(orHlp.or()).append("OU.path LIKE '").append(ou.getPath()).append("%'");
        }
      }

      orgUnits.append(") ");
    } else if (!params.isOrganisationUnitMode(OrganisationUnitSelectionMode.ALL)) {
      orgUnits
          .append("AND OU.organisationunitid IN (")
          .append(getCommaDelimitedString(getIdentifiers(params.getOrgUnits())))
          .append(") ");
    }

    return orgUnits.toString();
  }

  /**
   * Generates an INNER JOIN for enrollments. If the param we need to order by is enrolledAt, we
   * need to join the enrollment table to be able to select and order by this value
   *
   * @return a SQL INNER JOIN for enrollments
   */
  private String getFromSubQueryJoinEnrollmentConditions(TrackedEntityQueryParams params) {
    if (params.getOrder().stream()
        .filter(o -> o.getField() instanceof String)
        .anyMatch(p -> ENROLLMENT_DATE_KEY.equals(p.getField()))) {
      return " INNER JOIN enrollment "
          + ENROLLMENT_ALIAS
          + " ON "
          + ENROLLMENT_ALIAS
          + "."
          + "trackedentityid"
          + "= TE.trackedentityid ";
    }

    return "";
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
      SqlHelper whereAnd, TrackedEntityQueryParams params) {
    StringBuilder program = new StringBuilder();

    if (!params.hasProgram()) {
      return "";
    }

    program
        .append(whereAnd.whereAnd())
        .append("EXISTS (")
        .append("SELECT EN.trackedentityid ")
        .append("FROM enrollment EN ");

    if (params.hasFilterForEvents()) {
      program.append(getFromSubQueryEvent(params));
    }

    program
        .append("WHERE EN.trackedentityid = TE.trackedentityid ")
        .append("AND EN.programid = ")
        .append(params.getProgram().getId())
        .append(SPACE);

    if (params.hasProgramStatus()) {
      program.append("AND EN.status = '").append(params.getProgramStatus()).append("' ");
    }

    if (params.hasFollowUp()) {
      program.append("AND EN.followup IS ").append(params.getFollowUp()).append(SPACE);
    }

    if (params.hasProgramEnrollmentStartDate()) {
      program
          .append("AND EN.enrollmentdate >= '")
          .append(getMediumDateString(params.getProgramEnrollmentStartDate()))
          .append("' ");
    }

    if (params.hasProgramEnrollmentEndDate()) {
      program
          .append("AND EN.enrollmentdate <= '")
          .append(getMediumDateString(params.getProgramEnrollmentEndDate()))
          .append("' ");
    }

    if (params.hasProgramIncidentStartDate()) {
      program
          .append("AND EN.incidentdate >= '")
          .append(getMediumDateString(params.getProgramIncidentStartDate()))
          .append("' ");
    }

    if (params.hasProgramIncidentEndDate()) {
      program
          .append("AND EN.incidentdate <= '")
          .append(getMediumDateString(params.getProgramIncidentEndDate()))
          .append("' ");
    }

    if (!params.isIncludeDeleted()) {
      program.append("AND EN.deleted is false ");
    }

    program.append(") ");

    return program.toString();
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
          .append(encodeAndQuote(params.getAssignedUserQueryParam().getAssignedUsers()))
          .append(") ")
          .append(") AU ON AU.userid = EV.assigneduserid");
    }

    if (params.hasEventStatus()) {
      String start = getMediumDateString(params.getEventStartDate());
      String end = getMediumDateString(params.getEventEndDate());

      if (params.isEventStatus(EventStatus.COMPLETED)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_EXECUTIONDATE, start, end))
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
            .append(getQueryDateConditionBetween(whereHlp, EV_EXECUTIONDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(EQUALS)
            .append(SINGLE_QUOTE)
            .append(EventStatus.ACTIVE.name())
            .append(SINGLE_QUOTE)
            .append(SPACE);
      } else if (params.isEventStatus(EventStatus.SCHEDULE)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_DUEDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(SPACE)
            .append(IS_NOT_NULL)
            .append(whereHlp.whereAnd())
            .append(EV_EXECUTIONDATE)
            .append(SPACE)
            .append(IS_NULL)
            .append(whereHlp.whereAnd())
            .append("date(now()) <= date(EV.duedate) ");
      } else if (params.isEventStatus(EventStatus.OVERDUE)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_DUEDATE, start, end))
            .append(whereHlp.whereAnd())
            .append(EV_STATUS)
            .append(SPACE)
            .append(IS_NOT_NULL)
            .append(whereHlp.whereAnd())
            .append(EV_EXECUTIONDATE)
            .append(SPACE)
            .append(IS_NULL)
            .append(whereHlp.whereAnd())
            .append("date(now()) > date(EV.duedate) ");
      } else if (params.isEventStatus(EventStatus.SKIPPED)) {
        events
            .append(getQueryDateConditionBetween(whereHlp, EV_DUEDATE, start, end))
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

    events.append(") EV ON EV.enrollmentid = EN.enrollmentid ");

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
                  "Cannot order by '%s'. Supported are tracked entity attributes and fields '%s'.",
                  field, String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        orderFields.add(ORDERABLE_FIELDS.get(field) + " " + order.getDirection());
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        String orderField =
            innerOrder
                ? statementBuilder.columnQuote(tea.getUid()) + ".value "
                : MAIN_QUERY_ALIAS + "." + statementBuilder.columnQuote(tea.getUid());

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
      return "ORDER BY " + StringUtils.join(orderFields, ',') + SPACE;
    }

    return "ORDER BY " + DEFAULT_ORDER + " ";
  }

  /**
   * Generates the LIMIT and OFFSET part of the sub-query. The limit is decided by several factors:
   * 1. maxtelimit in a TET or Program 2. PageSize and Offset 3. No paging
   * (TRACKER_TRACKED_ENTITY_QUERY_LIMIT will apply in this case)
   *
   * <p>If maxtelimit is not 0, it means this is the hard limit of the number of results. In the
   * case where there exists more results than maxtelimit, we should return an error to the user
   * (This prevents snooping outside the users capture scope to some degree). 0 means no maxtelimit,
   * or it's not applicable.
   *
   * <p>If we have maxtelimit and paging on, we set the limit to maxtelimit.
   *
   * <p>If we don't have maxtelimit, and paging on, we set normal paging parameters
   *
   * <p>If neither maxtelimit nor paging is set, we have no limit set by the user, so system will
   * set the limit to TRACKED_ENTITY_MAX_LIMIT which can be configured in system settings.
   *
   * <p>The limit is set in the sub-query, so the latter joins have fewer rows to consider.
   *
   * @return a SQL LIMIT and OFFSET clause, or empty string if no LIMIT can be deducted.
   */
  private String getFromSubQueryLimitAndOffset(
      TrackedEntityQueryParams params, PageParams pageParams) {
    StringBuilder limitOffset = new StringBuilder();
    int limit = params.getMaxTeLimit();
    int teQueryLimit = systemSettingManager.getIntSetting(SettingKey.TRACKED_ENTITY_MAX_LIMIT);

    if (limit == 0 && pageParams == null) {
      if (teQueryLimit > 0) {
        return limitOffset
            .append(LIMIT)
            .append(SPACE)
            .append(teQueryLimit)
            .append(SPACE)
            .toString();
      }

      return limitOffset.toString();
    } else if (limit == 0 && pageParams != null) {
      return limitOffset
          .append(LIMIT)
          .append(SPACE)
          .append(pageParams.getPageSize())
          .append(SPACE)
          .append(OFFSET)
          .append(SPACE)
          .append((pageParams.getPage() - 1) * pageParams.getPageSize())
          .append(SPACE)
          .toString();
    } else if (pageParams != null) {
      return limitOffset
          .append(LIMIT)
          .append(SPACE)
          .append(Math.min(limit + 1, pageParams.getPageSize()))
          .append(SPACE)
          .append(OFFSET)
          .append(SPACE)
          .append((pageParams.getPage() - 1) * pageParams.getPageSize())
          .append(SPACE)
          .toString();
    } else {
      return limitOffset
          .append(LIMIT)
          .append(SPACE)
          .append(limit + 1) // We add +1, since we use this limit to
          // restrict a user to search to wide.
          .append(SPACE)
          .toString();
    }
  }

  @Override
  protected void preProcessPredicates(
      CriteriaBuilder builder, List<Function<Root<TrackedEntity>, Predicate>> predicates) {
    predicates.add(root -> builder.equal(root.get("deleted"), false));
  }

  @Override
  protected TrackedEntity postProcessObject(TrackedEntity trackedEntity) {
    return (trackedEntity == null || trackedEntity.isDeleted()) ? null : trackedEntity;
  }

  private boolean skipOwnershipCheck(TrackedEntityQueryParams params) {
    return params.getUser() != null && params.getUser().isSuper();
  }
}
