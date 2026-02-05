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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Geometries;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.UserInfoSnapshots;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityProgramOwner;
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

  private static final String PROGRAM_OWNER_OU_UID = "po_ou_uid";
  private static final String PROGRAM_OWNER_PRG_UID = "po_prg_uid";

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

  public List<TrackedEntity> getTrackedEntities(TrackedEntityQueryParams params) {
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

    // Use LinkedHashMap to deduplicate by trackedentityid while preserving order.
    // Duplicates can occur when ordering by enrollment date and a TE has multiple enrollments.
    Map<Long, TrackedEntity> result = new LinkedHashMap<>();
    while (rowSet.next()) {
      TrackedEntity te = mapRowToTrackedEntity(rowSet, params.hasEnrolledInTrackerProgram());
      result.putIfAbsent(te.getId(), te);
    }
    return new ArrayList<>(result.values());
  }

  public Page<TrackedEntity> getTrackedEntities(
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

    // Use LinkedHashMap to deduplicate by trackedentityid while preserving order.
    // Duplicates can occur when ordering by enrollment date and a TE has multiple enrollments.
    Map<Long, TrackedEntity> result = new LinkedHashMap<>();
    while (rowSet.next()) {
      TrackedEntity te = mapRowToTrackedEntity(rowSet, params.hasEnrolledInTrackerProgram());
      result.putIfAbsent(te.getId(), te);
    }

    return new Page<>(
        new ArrayList<>(result.values()), pageParams, () -> getTrackedEntityCount(params));
  }

  private TrackedEntity mapRowToTrackedEntity(SqlRowSet rs, boolean hasProgramOwner) {
    TrackedEntity te = new TrackedEntity();
    te.setId(rs.getLong("trackedentityid"));
    te.setUid(rs.getString("uid"));
    te.setCreated(rs.getTimestamp("created"));
    te.setLastUpdated(rs.getTimestamp("lastupdated"));
    te.setCreatedAtClient(rs.getTimestamp("createdatclient"));
    te.setLastUpdatedAtClient(rs.getTimestamp("lastupdatedatclient"));
    te.setInactive(rs.getBoolean("inactive"));
    te.setPotentialDuplicate(rs.getBoolean("potentialduplicate"));
    te.setDeleted(rs.getBoolean("deleted"));
    te.setCreatedByUserInfo(UserInfoSnapshots.fromJson(rs.getString("createdbyuserinfo")));
    te.setLastUpdatedByUserInfo(UserInfoSnapshots.fromJson(rs.getString("lastupdatedbyuserinfo")));
    te.setGeometry(Geometries.fromWkb((byte[]) rs.getObject("geometry")));

    // Tracked entity type
    TrackedEntityType tet = new TrackedEntityType();
    tet.setId(rs.getLong("tet_id"));
    tet.setUid(rs.getString("tet_uid"));
    tet.setCode(rs.getString("tet_code"));
    tet.setName(rs.getString("tet_name"));
    String tetAttrValues = rs.getString("tet_attributevalues");
    if (tetAttrValues != null) {
      tet.setAttributeValues(AttributeValues.of(tetAttrValues));
    }
    tet.setAllowAuditLog(rs.getBoolean("tet_allowauditlog"));
    tet.setEnableChangeLog(rs.getBoolean("tet_enablechangelog"));
    te.setTrackedEntityType(tet);

    // TE's registering org unit
    OrganisationUnit orgUnit = new OrganisationUnit();
    orgUnit.setUid(rs.getString("te_ou_uid"));
    orgUnit.setCode(rs.getString("te_ou_code"));
    orgUnit.setName(rs.getString("te_ou_name"));
    orgUnit.setPath(rs.getString("te_ou_path"));
    String orgUnitAttrValues = rs.getString("te_ou_attributevalues");
    if (orgUnitAttrValues != null) {
      orgUnit.setAttributeValues(AttributeValues.of(orgUnitAttrValues));
    }
    te.setOrganisationUnit(orgUnit);

    // Program owner (only available when querying with a specific program)
    if (hasProgramOwner) {
      String ownerOrgUnitUid = rs.getString(PROGRAM_OWNER_OU_UID);
      String ownerProgramUid = rs.getString(PROGRAM_OWNER_PRG_UID);
      if (ownerOrgUnitUid != null && ownerProgramUid != null) {
        TrackedEntityProgramOwner programOwner = new TrackedEntityProgramOwner();
        programOwner.setTrackedEntity(te);
        Program program = new Program();
        program.setUid(ownerProgramUid);
        programOwner.setProgram(program);
        OrganisationUnit ownerOrgUnit = new OrganisationUnit();
        ownerOrgUnit.setUid(ownerOrgUnitUid);
        programOwner.setOrganisationUnit(ownerOrgUnit);
        te.setProgramOwners(Set.of(programOwner));
      }
    }

    return te;
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
   * Generates the main SQL query to retrieve tracked entities.
   *
   * <p>The query uses a deferred join pattern for performance - LIMIT/OFFSET is applied before
   * joining the tet/te_ou tables:
   *
   * <pre>
   * SELECT (columns)
   * FROM (
   *   SELECT DISTINCT te.*, (order_columns)
   *   FROM trackedentity te
   *   JOIN program p ON ...
   *   JOIN trackedentityprogramowner po ON ...
   *   JOIN organisationunit ou ON ...
   *   [JOIN enrollment en ON ...]  -- only when ordering by enrollment date
   *   [LEFT JOIN trackedentityattributevalue ON ...]  -- for attribute filters/ordering
   *   WHERE (filters)
   *   [AND EXISTS (enrollment_subquery)]  -- when program is specified
   *   ORDER BY (order) LIMIT/OFFSET
   * ) te
   * JOIN trackedentitytype tet ON ...
   * JOIN organisationunit te_ou ON ...
   * [JOIN program owner tables ON ...]  -- when program is specified
   * ORDER BY (order)
   * </pre>
   *
   * <p>Key design decisions:
   *
   * <ul>
   *   <li>Inner query uses DISTINCT on te.* plus order columns to deduplicate while preserving sort
   *   <li>LIMIT/OFFSET applied in inner query before expensive joins to tet/te_ou tables
   *   <li>Program enrollment check uses EXISTS to avoid row multiplication
   *   <li>Attribute ordering uses LEFT JOIN so TEs without the attribute are still included
   * </ul>
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

  // language=SQL
  private static final String SELECT =
      """
      select te.trackedentityid, te.uid, te.created, te.lastupdated, te.createdatclient,
      te.lastupdatedatclient, te.inactive, te.potentialduplicate, te.deleted,
      te.createdbyuserinfo, te.lastupdatedbyuserinfo, te.geometry,
      te.tet_id, te.tet_uid, te.tet_code, te.tet_name, te.tet_attributevalues,
      te.tet_allowauditlog, te.tet_enablechangelog,
      te.te_ou_uid, te.te_ou_code, te.te_ou_name, te.te_ou_path, te.te_ou_attributevalues,
      po_ou_uid, po_prg_uid""";

  // language=SQL
  private static final String SELECT_WITH_ENROLLMENT_DATE = SELECT + ", " + ENROLLMENT_DATE_ALIAS;

  private void addSelect(StringBuilder sql, TrackedEntityQueryParams params) {
    sql.append(isOrderingByEnrolledAt(params) ? SELECT_WITH_ENROLLMENT_DATE : SELECT);
  }

  /**
   * Generates the SQL of the sub-query, used to find the correct subset of tracked entities to
   * return. Orchestrates all the different segments of the SQL into a complete sub-query.
   *
   * <p>The query uses a "narrow DISTINCT" pattern for performance: the inner subquery uses DISTINCT
   * only on trackedentityid (and order-by columns), then joins back to fetch all required columns.
   * This reduces query planning time from ~41ms to ~9ms compared to DISTINCT over all columns.
   *
   * <p>DISTINCT is required because:
   *
   * <ul>
   *   <li>Without a specific program, the join on accessible programs multiplies rows
   *   <li>When ordering by enrollment date, multiple enrollments produce duplicate rows
   * </ul>
   */
  private void addTrackedEntityFromItem(
      StringBuilder sql,
      MapSqlParameterSource sqlParameters,
      TrackedEntityQueryParams params,
      PageParams pageParams,
      boolean isCountQuery) {
    // Outer query: join back to fetch all columns after the inner query finds the TE IDs
    sql.append("(select ");
    addOuterSelectColumns(sql, params);
    sql.append(" from (");

    // Inner query: narrow DISTINCT on trackedentityid only (plus order-by columns)
    addInnerSelect(sql, params);
    sql.append(" from trackedentity ").append(MAIN_QUERY_ALIAS).append(" ");

    // Filter joins (required for WHERE conditions and access control)
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

    // Data joins (fetch tet/te_ou/po data after LIMIT reduces row count)
    // No need to join trackedentity again - te.* already has all TE columns from inner query
    sql.append("join organisationunit te_ou on te.organisationunitid = te_ou.organisationunitid ");
    sql.append("join trackedentitytype tet on te.trackedentitytypeid = tet.trackedentitytypeid ");
    if (params.hasEnrolledInTrackerProgram()) {
      sql.append(
          "join trackedentityprogramowner po_outer on po_outer.trackedentityid = te.trackedentityid and po_outer.programid = :enrolledInTrackerProgram ");
      sql.append(
          "join organisationunit po_ou on po_ou.organisationunitid = po_outer.organisationunitid ");
      sql.append("join program p_outer on p_outer.programid = po_outer.programid ");
    }

    if (!isCountQuery) {
      sql.append(" ");
      addOrderBy(sql, params);
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
   * Adds the SELECT columns for the inner subquery. Uses DISTINCT on te.* (all tracked entity
   * columns) without joining tet/te_ou tables. This keeps planning time low (~2ms) while still
   * providing all TE data needed by the mapper.
   *
   * <p>When ordering by enrolledAt, uses DISTINCT ON (trackedentityid) to pick one enrollment per
   * TE, fixing pagination when a TE has multiple enrollments (DHIS2-20811).
   */
  private void addInnerSelect(StringBuilder sql, TrackedEntityQueryParams params) {
    // When ordering by enrolledAt, use DISTINCT ON to pick one enrollment per TE.
    // This fixes pagination when a TE has multiple enrollments (DHIS2-20811).
    if (isOrderingByEnrolledAt(params)) {
      sql.append("select distinct on (te.trackedentityid) te.*");
    } else {
      sql.append("select distinct te.*");
    }

    // Add order-by columns from joined tables (enrollment date, attribute values)
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

  /**
   * Adds the SELECT columns for the outer query. The inner query provides all te.* columns via
   * DISTINCT te.*, and the outer query adds tet/te_ou/po data from joins.
   */
  private void addOuterSelectColumns(StringBuilder sql, TrackedEntityQueryParams params) {
    LinkedHashSet<String> columns =
        new LinkedHashSet<>(
            List.of(
                // TE core fields from inner query (te.* columns)
                "te.trackedentityid",
                "te.uid",
                "te.created",
                "te.lastupdated",
                "te.createdatclient",
                "te.lastupdatedatclient",
                "te.inactive",
                "te.potentialduplicate",
                "te.deleted",
                "te.createdbyuserinfo",
                "te.lastupdatedbyuserinfo",
                "ST_AsBinary(te.geometry) as geometry",
                // Tracked entity type fields
                "tet.trackedentitytypeid as tet_id",
                "tet.uid as tet_uid",
                "tet.code as tet_code",
                "tet.name as tet_name",
                "tet.attributevalues as tet_attributevalues",
                "tet.allowauditlog as tet_allowauditlog",
                "tet.enableChangeLog as tet_enablechangelog",
                // TE's registering org unit fields
                "te_ou.uid as te_ou_uid",
                "te_ou.code as te_ou_code",
                "te_ou.name as te_ou_name",
                "te_ou.path as te_ou_path",
                "te_ou.attributevalues as te_ou_attributevalues"));

    // Add order-by columns so outer ORDER BY works
    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field && ENROLLMENT_DATE_KEY.equals(field)) {
        columns.add(ENROLLMENT_DATE_ALIAS);
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        columns.add(quote(tea.getUid()));
      }
    }

    // Program owner data
    if (params.hasEnrolledInTrackerProgram()) {
      columns.add("po_ou.uid as " + PROGRAM_OWNER_OU_UID);
      columns.add("p_outer.uid as " + PROGRAM_OWNER_PRG_UID);
    } else {
      columns.add("null as " + PROGRAM_OWNER_OU_UID);
      columns.add("null as " + PROGRAM_OWNER_PRG_UID);
    }

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
   * Adds the ORDER BY clause. Applied at multiple query levels: in the inner subquery to ensure
   * correct LIMIT results, and in outer queries to preserve order after joins.
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
