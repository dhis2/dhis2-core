/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Geometries;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.UserInfoSnapshots;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("org.hisp.dhis.tracker.export.enrollment.EnrollmentStore")
@RequiredArgsConstructor
class JdbcEnrollmentStore {

  private static final String DEFAULT_ORDER = "e.enrollmentid desc";
  private static final Set<String> ORDERABLE_FIELDS =
      Set.of(
          "completedDate",
          "created",
          "createdAtClient",
          "enrollmentDate",
          "lastUpdated",
          "lastUpdatedAtClient");

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public List<Enrollment> getEnrollments(EnrollmentQueryParams enrollmentParams) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!enrollmentParams.hasEnrolledInTrackerProgram()
        && enrollmentParams.getAccessibleTrackerPrograms().isEmpty()) {
      return List.of();
    }

    MapSqlParameterSource sqlParams = new MapSqlParameterSource();
    String sql = getQuery(enrollmentParams, sqlParams);
    return jdbcTemplate.query(
        sql,
        sqlParams,
        new EnrollmentRowMapper(
            enrollmentParams.isIncludeAttributes(),
            enrollmentParams.getEnrolledInTrackerProgram()));
  }

  /**
   * Builds the enrollment query:
   *
   * <pre>
   * select ...
   * from enrollment e
   *   inner join program ...
   *   inner join trackedentity ...
   *   inner join trackedentitytype ...
   *   inner join trackedentityprogramowner ...
   *   inner join organisationunit ou ...
   *   inner join organisationunit en_ou ...
   *   inner join (...) as coc ...
   *   left join lateral (...) notes on true
   *   left join lateral (...) attrs on true   -- if includeAttributes
   * where ...
   * order by ...
   * </pre>
   */
  private String getQuery(EnrollmentQueryParams enrollmentParams, MapSqlParameterSource sqlParams) {
    StringBuilder sql = new StringBuilder();
    addSelect(sql, enrollmentParams);
    sql.append(" from enrollment e ");
    addJoinOnTrackedEntity(sql);
    if (!enrollmentParams.hasEnrolledInTrackerProgram()) {
      addJoinOnProgram(sql);
      addJoinOnTrackedEntityType(sql);
    }
    if (isSelectedModeWithProgram(enrollmentParams)) {
      addSelectedOrgUnitCte(sql, sqlParams, enrollmentParams);
      addSelectedOrgUnitJoin(sql);
    } else {
      addJoinOnProgramOwner(sql);
      addJoinOnOwnerOrgUnit(sql);
    }
    addJoinOnEnrollmentOrgUnit(sql);
    addJoinOnCategoryOptionCombo(sql);
    addLeftJoinOnNotes(sql);
    addLeftJoinOnAttributes(sql, enrollmentParams);
    addWhereConditions(sql, sqlParams, enrollmentParams);
    addOrderBy(sql, enrollmentParams);

    return sql.toString();
  }

  private void addSelect(StringBuilder sql, EnrollmentQueryParams params) {
    sql.append(
        """
            select e.enrollmentid, e.uid, e.created, e.createdatclient, e.createdbyuserinfo,
            e.lastupdated, e.lastupdatedatclient, e.lastupdatedbyuserinfo, e.occurreddate,
            e.enrollmentdate, e.completeddate, e.followup, e.completedby, e.storedby, e.deleted, e.status,
            ST_AsBinary(e.geometry) as geometry,
        """);

    if (params.hasEnrolledInTrackerProgram()) {
      // program and trackedentitytype columns are not needed; the RowMapper
      // reuses the already loaded Program entity instead
      sql.append(
          """
            te.uid as tracked_entity_uid, te.code as tracked_entity_code,
            en_ou.uid as en_org_unit_uid,
            notes.jsonnotes as notes,
            coc.uid as coc_uid
          """);
    } else {
      sql.append(
          """
            p.programid as program_id, p.uid as program_uid, p.name as program_name, p.code as program_code, p.sharing as program_sharing,
            p.description as program_description, p.created as program_created, p.lastupdated as program_lastupdated,
            p.shortname as program_short_name, p.type as program_type, p.accesslevel as program_accesslevel,
            te.uid as tracked_entity_uid, te.code as tracked_entity_code,
            en_ou.uid as en_org_unit_uid,
            tet.uid as tet_uid, tet.allowauditlog as tet_allowauditlog, tet.enablechangelog as tet_enablechangelog, tet.sharing as tet_sharing, notes.jsonnotes as notes,
            coc.uid as coc_uid
          """);
    }

    if (params.isIncludeAttributes()) {
      sql.append(
          """
          , attrs.jsonattributes as attributes
          """);
    }
  }

  private void addWhereConditions(
      StringBuilder sql, MapSqlParameterSource sqlParams, EnrollmentQueryParams params) {
    SqlHelper hlp = new SqlHelper(true);
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addOrgUnitConditions(sql, sqlParams, params, hlp);
    addProgramConditions(sql, sqlParams, params, hlp);
    addEnrollmentConditions(sql, sqlParams, params, hlp);
    addTrackedEntityConditions(sql, sqlParams, params, hlp);
    addAttributeOptionComboConditions(sql, sqlParams, params, hlp);
  }

  private void addJoinOnProgram(StringBuilder sql) {
    sql.append(
        """
      inner join program p on p.programid = e.programid
      """);
  }

  private void addJoinOnTrackedEntity(StringBuilder sql) {
    sql.append(
        """
      inner join trackedentity te on te.trackedentityid = e.trackedentityid
      """);
  }

  private void addJoinOnTrackedEntityType(StringBuilder sql) {
    sql.append(
        """
      inner join trackedentitytype tet on tet.trackedentitytypeid = te.trackedentitytypeid
      """);
  }

  private void addJoinOnProgramOwner(StringBuilder sql) {
    sql.append(
        """
      inner join trackedentityprogramowner po \
      on po.trackedentityid = e.trackedentityid and po.programid = e.programid
      """);
  }

  private void addJoinOnOwnerOrgUnit(StringBuilder sql) {
    sql.append(
        """
      inner join organisationunit ou on ou.organisationunitid = po.organisationunitid
      """);
  }

  private void addJoinOnEnrollmentOrgUnit(StringBuilder sql) {
    sql.append(
        """
      inner join organisationunit en_ou on en_ou.organisationunitid = e.organisationunitid
      """);
  }

  private void addJoinOnCategoryOptionCombo(StringBuilder sql) {
    sql.append(
        """
        inner join (
          select coc.categoryoptioncomboid as id, coc.uid
          from categoryoptioncombo coc
        """);

    if (isNotSuperUser(getCurrentUserDetails())) {
      sql.append(
          """
            inner join categoryoptioncombos_categoryoptions cocco \
          on coc.categoryoptioncomboid = cocco.categoryoptioncomboid \
          inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid \
          group by coc.categoryoptioncomboid \
          having bool_and(case when \
          """);
      sql.append(
          JpaQueryUtils.generateSQlQueryForSharingCheck(
              "co.sharing", getCurrentUserDetails(), AclService.LIKE_READ_DATA));
      sql.append(" then true else false end) = true ");
    }

    sql.append(") as coc on coc.id = e.attributeoptioncomboid ");
  }

  /**
   * orgUnitMode=SELECTED with a known program can use a subquery on {@code
   * trackedentityprogramowner} instead of joining {@code trackedentityprogramowner} + {@code
   * organisationunit}. This lets PostgreSQL use the {@code (programid, organisationunitid)}
   * composite index to find the small set of tracked entities at the selected org units, instead of
   * scanning all enrollments and filtering.
   */
  private static boolean isSelectedModeWithProgram(EnrollmentQueryParams params) {
    return params.hasEnrolledInTrackerProgram()
        && params.hasOrganisationUnits()
        && params.getOrganisationUnitMode() == OrganisationUnitSelectionMode.SELECTED;
  }

  private static boolean isNotSuperUser(UserDetails user) {
    return !user.isSuper();
  }

  private void addLeftJoinOnNotes(StringBuilder sql) {
    sql.append(
        """
      left join lateral (
        select json_agg(json_build_object('uid', n.uid, 'text', n.notetext,
          'creator', n.creator, 'created', n.created, 'updatedByUid', u.uid,
          'updatedByUsername', u.username, 'updatedByFirstname', u.firstname,
          'updatedBySurname', u.surname, 'updatedByName', u.name)) as jsonnotes
          from enrollment_notes en
          join note n on n.noteid = en.noteid
          join userinfo u on u.userinfoid = n.lastupdatedby
          where en.enrollmentid = e.enrollmentid
      ) notes on true
    """);
  }

  private void addLeftJoinOnAttributes(StringBuilder sql, EnrollmentQueryParams params) {
    if (params.isIncludeAttributes()) {
      sql.append(
          """
          left join lateral (
              select json_agg(json_build_object('uid', tea.uid, 'name', tea.name,
              'code', tea.code, 'value', teav.value, 'encryptedValue', teav.encryptedvalue,
              'valueType', tea.valuetype, 'confidential', tea.confidential, 'created', teav.created,
              'lastUpdated', teav.lastupdated, 'storedBy', teav.storedby)) as jsonattributes
              from trackedentityattributevalue teav
              join trackedentityattribute tea ON tea.trackedentityattributeid = teav.trackedentityattributeid
              where teav.trackedentityid = e.trackedentityid
          ) attrs on true
          """);
    }
  }

  private void addLastUpdatedConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams enrollmentParams,
      SqlHelper hlp) {
    if (enrollmentParams.hasLastUpdatedDuration()) {
      sql.append(hlp.whereAnd()).append("e.lastupdated >= :lastupdated");
      sqlParams.addValue(
          "lastupdated",
          new Timestamp(nowMinusDuration(enrollmentParams.getLastUpdatedDuration()).getTime()));
    } else if (enrollmentParams.hasLastUpdated()) {
      sql.append(hlp.whereAnd()).append("e.lastupdated >= :lastupdated");
      sqlParams.addValue("lastupdated", new Timestamp(enrollmentParams.getLastUpdated().getTime()));
    }
  }

  private void addOrgUnitConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp) {
    addOrgUnitConditions(sql, sqlParams, params, hlp, "te");
  }

  private void addOrgUnitConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp,
      @Nullable String trackedEntityTableAlias) {
    if (isSelectedModeWithProgram(params)) {
      // CTE and join already added by getQuery/getCountQuery
      return;
    }

    if (params.hasOrganisationUnits()) {
      buildOrgUnitModeClause(
          sql,
          sqlParams,
          params.getOrganisationUnits(),
          params.getOrganisationUnitMode(),
          "ou",
          hlp.whereAnd());
    }

    if (params.hasEnrolledInTrackerProgram()) {
      buildOwnershipClause(
          sql,
          sqlParams,
          params.getEnrolledInTrackerProgram(),
          params.getQuerySearchScope(),
          "ou",
          trackedEntityTableAlias,
          hlp::whereAnd);
    } else {
      buildOwnershipClause(
          sql, sqlParams, params.getOrganisationUnitMode(), "p", "ou", "te", hlp::whereAnd);
    }
  }

  /**
   * For orgUnitMode=SELECTED with a known program, uses a materialized CTE to find tracked entities
   * owned at the selected org units. The CTE uses the composite index on {@code (programid,
   * organisationunitid)} to efficiently find the small set of tracked entity IDs, which then drives
   * the join to enrollment. Without materialization, PostgreSQL flattens the subquery into a
   * semi-join and scans all enrollments instead.
   *
   * <p>The ownership access control clause is not needed here because the mapper already validates
   * that the user has appropriate access (search or capture scope depending on program access
   * level) to the requested org units before they reach the store.
   */
  private void addSelectedOrgUnitCte(
      StringBuilder sql, MapSqlParameterSource sqlParams, EnrollmentQueryParams params) {
    sql.insert(
        0,
        "with selected_tes as materialized ("
            + "select po.trackedentityid from trackedentityprogramowner po"
            + " where po.programid = :programId"
            + " and po.organisationunitid in (:selectedOrgUnits)) ");
    sqlParams.addValue("selectedOrgUnits", getIdentifiers(params.getOrganisationUnits()));
  }

  private void addSelectedOrgUnitJoin(StringBuilder sql) {
    sql.append(
        """
      inner join selected_tes on selected_tes.trackedentityid = e.trackedentityid
      """);
  }

  private void addProgramConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp) {
    if (params.hasEnrolledInTrackerProgram()) {
      sql.append(hlp.whereAnd()).append("e.programid = :programId");
      sqlParams.addValue("programId", params.getEnrolledInTrackerProgram().getId());
    } else {
      sql.append(hlp.whereAnd()).append("p.type = :programType");
      sqlParams.addValue("programType", ProgramType.WITH_REGISTRATION.name());
      sql.append(" and p.programid in (:accessiblePrograms)");
      sqlParams.addValue(
          "accessiblePrograms", getIdentifiers(params.getAccessibleTrackerPrograms()));
    }

    if (params.hasProgramStartDate()) {
      sql.append(hlp.whereAnd()).append("e.enrollmentdate >= :programStartDate");
      sqlParams.addValue("programStartDate", new Timestamp(params.getProgramStartDate().getTime()));
    }

    if (params.hasProgramEndDate()) {
      sql.append(hlp.whereAnd()).append("e.enrollmentdate <= :programEndDate");
      sqlParams.addValue("programEndDate", new Timestamp(params.getProgramEndDate().getTime()));
    }
  }

  private void addEnrollmentConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp) {
    if (params.hasEnrollmentUids()) {
      sql.append(hlp.whereAnd()).append("e.uid in (:enrollmentUids)");
      sqlParams.addValue("enrollmentUids", UID.toValueList(params.getEnrollments()));
    }

    if (params.hasEnrollmentStatus()) {
      sql.append(hlp.whereAnd()).append("e.status = :enrollmentStatus");
      sqlParams.addValue("enrollmentStatus", params.getEnrollmentStatus().name());
    }

    if (params.hasFollowUp()) {
      sql.append(hlp.whereAnd()).append("e.followup = :followUp");
      sqlParams.addValue("followUp", params.getFollowUp());
    }

    if (!params.isIncludeDeleted()) {
      sql.append(hlp.whereAnd()).append("e.deleted = false");
    }
  }

  private void addTrackedEntityConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp) {
    if (params.hasTrackedEntities()) {
      sql.append(hlp.whereAnd()).append("te.uid in (:trackedEntityUids)");
      sqlParams.addValue(
          "trackedEntityUids", params.getTrackedEntities().stream().map(UID::getValue).toList());
    }
  }

  private void addAttributeOptionComboConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams enrollmentParams,
      SqlHelper hlp) {
    if (enrollmentParams.getAttributeOptionCombo() != null) {
      sqlParams.addValue(
          "attributeoptioncomboid", enrollmentParams.getAttributeOptionCombo().getId());

      sql.append(hlp.whereAnd())
          .append(" e.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid")
          .append(" ");
    }
  }

  private void addOrderBy(StringBuilder sql, EnrollmentQueryParams params) {
    sql.append(" order by ");
    sql.append(orderBy(params.getOrder()));
  }

  public Page<Enrollment> getEnrollments(
      EnrollmentQueryParams enrollmentParams, PageParams pageParams) {
    // A te which is not enrolled can only be accessed by a user that is able to enroll it into a
    // tracker program. Return an empty result if there are no tracker programs or the user does
    // not have access to one.
    if (!enrollmentParams.hasEnrolledInTrackerProgram()
        && enrollmentParams.getAccessibleTrackerPrograms().isEmpty()) {
      return Page.empty();
    }

    MapSqlParameterSource sqlParams = new MapSqlParameterSource();
    String sql = getQuery(enrollmentParams, sqlParams);
    sql +=
        String.format(" LIMIT %d OFFSET %d", pageParams.getPageSize() + 1, pageParams.getOffset());

    List<Enrollment> enrollments =
        jdbcTemplate.query(
            sql,
            sqlParams,
            new EnrollmentRowMapper(
                enrollmentParams.isIncludeAttributes(),
                enrollmentParams.getEnrolledInTrackerProgram()));
    return new Page<>(enrollments, pageParams, () -> countEnrollments(enrollmentParams));
  }

  private long countEnrollments(EnrollmentQueryParams params) {
    MapSqlParameterSource sqlParams = new MapSqlParameterSource();
    String sql = getCountQuery(params, sqlParams);
    Long count = jdbcTemplate.queryForObject(sql, sqlParams, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Builds the count query. Only includes joins needed for filtering:
   *
   * <ul>
   *   <li>{@code program} - only when no specific program is given (needed for {@code
   *       p.accesslevel} and {@code p.programid in (...)}). When a program is known, its conditions
   *       are resolved in Java and the join is skipped.
   *   <li>{@code trackedentity} - only when the program is unknown (ownership clause needs {@code
   *       te.trackedentityid} for the PROTECTED temp owner check resolved at query time) or when
   *       the program is PROTECTED. Skipped for OPEN/AUDITED programs. Tracked entity UID filters
   *       use a subquery instead of a join when the table is absent.
   *   <li>{@code trackedentityprogramowner} + {@code organisationunit} - ownership and org unit
   *       filtering
   *   <li>{@code categoryoptioncombo} - attribute option combo access control
   * </ul>
   *
   * <p>Skips joins only needed for SELECT columns: {@code trackedentitytype}, enrollment {@code
   * organisationunit}, notes and attributes lateral joins.
   *
   * <p>Uses {@code count(*)} instead of {@code count(distinct e.uid)} because all remaining joins
   * are many-to-one from {@code enrollment}, so no duplicate rows are possible.
   */
  private String getCountQuery(
      EnrollmentQueryParams enrollmentParams, MapSqlParameterSource sqlParams) {
    boolean needsTrackedEntityJoin = needsTrackedEntityJoinForCount(enrollmentParams);

    StringBuilder sql = new StringBuilder();
    sql.append("select count(*) from enrollment e ");
    if (!enrollmentParams.hasEnrolledInTrackerProgram()) {
      addJoinOnProgram(sql);
    }
    if (needsTrackedEntityJoin) {
      addJoinOnTrackedEntity(sql);
    }
    if (isSelectedModeWithProgram(enrollmentParams)) {
      addSelectedOrgUnitCte(sql, sqlParams, enrollmentParams);
      addSelectedOrgUnitJoin(sql);
    } else {
      addJoinOnProgramOwner(sql);
      addJoinOnOwnerOrgUnit(sql);
    }
    addJoinOnCategoryOptionCombo(sql);
    addCountWhereConditions(sql, sqlParams, enrollmentParams, needsTrackedEntityJoin);

    return sql.toString();
  }

  /**
   * The {@code trackedentity} join is needed in the count query only when no specific program is
   * given (the ownership clause resolves access level at query time via the program table and needs
   * {@code te.trackedentityid} for the PROTECTED temp owner check). When a program is known, the
   * temp owner check uses {@code e.trackedentityid} instead, so the join can be skipped. Tracked
   * entity UID filters use a subquery instead when the table is absent.
   */
  private static boolean needsTrackedEntityJoinForCount(EnrollmentQueryParams params) {
    return !params.hasEnrolledInTrackerProgram();
  }

  private void addCountWhereConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      boolean hasTrackedEntityJoin) {
    SqlHelper hlp = new SqlHelper(true);
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addCountOrgUnitConditions(sql, sqlParams, params, hlp, hasTrackedEntityJoin);
    addProgramConditions(sql, sqlParams, params, hlp);
    addEnrollmentConditions(sql, sqlParams, params, hlp);
    if (hasTrackedEntityJoin) {
      addTrackedEntityConditions(sql, sqlParams, params, hlp);
    } else {
      addTrackedEntitySubqueryConditions(sql, sqlParams, params, hlp);
    }
    addAttributeOptionComboConditions(sql, sqlParams, params, hlp);
  }

  private void addCountOrgUnitConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp,
      boolean hasTrackedEntityJoin) {
    addOrgUnitConditions(sql, sqlParams, params, hlp, hasTrackedEntityJoin ? "te" : "e");
  }

  /**
   * Filters by tracked entity UIDs using a subquery when the {@code trackedentity} table is not
   * joined.
   */
  private void addTrackedEntitySubqueryConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      EnrollmentQueryParams params,
      SqlHelper hlp) {
    if (params.hasTrackedEntities()) {
      sql.append(hlp.whereAnd())
          .append(
              "e.trackedentityid in (select trackedentityid from trackedentity where uid in (:trackedEntityUids))");
      sqlParams.addValue(
          "trackedEntityUids", params.getTrackedEntities().stream().map(UID::getValue).toList());
    }
  }

  private static String orderBy(List<Order> orders) {
    if (orders == null || orders.isEmpty()) {
      return DEFAULT_ORDER;
    }

    StringBuilder orderBy = new StringBuilder();
    for (Order order : orders) {
      if (!orderBy.isEmpty()) {
        orderBy.append(", ");
      }
      orderBy.append("e.").append(order.getField()).append(" ").append(order.getDirection());
    }

    return orderBy + ", " + DEFAULT_ORDER;
  }

  private static class EnrollmentRowMapper implements RowMapper<Enrollment> {
    private final boolean isIncludeAttributes;
    private final Program program;

    EnrollmentRowMapper(boolean isIncludeAttributes, @Nullable Program program) {
      this.isIncludeAttributes = isIncludeAttributes;
      this.program = program;
    }

    @Override
    public Enrollment mapRow(ResultSet rs, int rowNum) throws SQLException {
      Enrollment enrollment = new Enrollment();
      enrollment.setId(rs.getLong("enrollmentid"));
      enrollment.setUid(rs.getString("uid"));
      enrollment.setCreated(formatDate(rs.getTimestamp("created")));
      enrollment.setCreatedAtClient(formatDate(rs.getTimestamp("createdatclient")));
      enrollment.setCreatedByUserInfo(
          UserInfoSnapshots.fromJson(rs.getString("createdbyuserinfo")));
      enrollment.setLastUpdated(formatDate(rs.getTimestamp("lastupdated")));
      enrollment.setLastUpdatedByUserInfo(
          UserInfoSnapshots.fromJson(rs.getString("lastupdatedbyuserinfo")));
      enrollment.setLastUpdatedAtClient(formatDate(rs.getTimestamp("lastupdatedatclient")));
      enrollment.setOccurredDate(formatDate(rs.getTimestamp("occurreddate")));
      enrollment.setEnrollmentDate(formatDate(rs.getTimestamp("enrollmentdate")));
      enrollment.setCompletedDate(formatDate(rs.getTimestamp("completeddate")));
      enrollment.setFollowup(rs.getBoolean("followup"));
      enrollment.setCompletedBy(rs.getString("completedby"));
      enrollment.setStoredBy(rs.getString("storedby"));
      enrollment.setDeleted(rs.getBoolean("deleted"));
      enrollment.setStatus(EnrollmentStatus.valueOf(rs.getString("status")));
      enrollment.setGeometry(Geometries.fromWkb(rs.getBytes("geometry")));

      Program enrollmentProgram;
      TrackedEntityType trackedEntityType;
      if (program != null) {
        enrollmentProgram = program;
        trackedEntityType = program.getTrackedEntityType();
      } else {
        trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid(rs.getString("tet_uid"));
        trackedEntityType.setAllowAuditLog(rs.getBoolean("tet_allowauditlog"));
        trackedEntityType.setEnableChangeLog(rs.getBoolean("tet_enablechangelog"));
        trackedEntityType.setSharing(mapSharingJsonIntoSharingObject(rs.getString("tet_sharing")));

        enrollmentProgram = new Program();
        enrollmentProgram.setId(rs.getLong("program_id"));
        enrollmentProgram.setUid(rs.getString("program_uid"));
        enrollmentProgram.setName(rs.getString("program_name"));
        enrollmentProgram.setShortName(rs.getString("program_short_name"));
        enrollmentProgram.setCode(rs.getString("program_code"));
        enrollmentProgram.setDescription(rs.getString("program_description"));
        enrollmentProgram.setCreated(formatDate(rs.getTimestamp("program_created")));
        enrollmentProgram.setLastUpdated(formatDate(rs.getTimestamp("program_lastupdated")));
        enrollmentProgram.setTrackedEntityType(trackedEntityType);
        enrollmentProgram.setProgramType(ProgramType.valueOf(rs.getString("program_type")));
        enrollmentProgram.setAccessLevel(AccessLevel.valueOf(rs.getString("program_accesslevel")));
        enrollmentProgram.setSharing(
            mapSharingJsonIntoSharingObject(rs.getString("program_sharing")));
      }
      enrollment.setProgram(enrollmentProgram);

      TrackedEntity trackedEntity = new TrackedEntity();
      trackedEntity.setUid(rs.getString("tracked_entity_uid"));
      trackedEntity.setCode(rs.getString("tracked_entity_code"));
      trackedEntity.setTrackedEntityType(trackedEntityType);
      enrollment.setTrackedEntity(trackedEntity);

      OrganisationUnit enrollmentOrgUnit = new OrganisationUnit();
      enrollmentOrgUnit.setUid(rs.getString("en_org_unit_uid"));
      enrollment.setOrganisationUnit(enrollmentOrgUnit);

      String jsonNotes = rs.getString("notes");
      if (jsonNotes != null) {
        enrollment.setNotes(mapEnrollmentNotes(jsonNotes));
      }

      if (isIncludeAttributes) {
        String jsonAttributes = rs.getString("attributes");
        if (jsonAttributes != null) {
          enrollment
              .getTrackedEntity()
              .setTrackedEntityAttributeValues(
                  mapTrackedEntityAttributeValues(jsonAttributes, trackedEntity));
        }
      }

      CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
      categoryOptionCombo.setUid(rs.getString("coc_uid"));
      enrollment.setAttributeOptionCombo(categoryOptionCombo);

      return enrollment;
    }

    private Sharing mapSharingJsonIntoSharingObject(String jsonSharing) {
      if (StringUtils.isEmpty(jsonSharing)) {
        return null;
      }

      try {
        return JsonBinaryType.MAPPER
            .readerFor(new TypeReference<Sharing>() {})
            .readValue(jsonSharing);
      } catch (IOException e) {
        log.error("Error mapping enrollment sharing: {}", jsonSharing);
        return null;
      }
    }

    private List<Note> mapEnrollmentNotes(String jsonNotes) {
      List<JdbcNote> jdbcNotes;
      ObjectMapper mapper = new ObjectMapper();
      try {
        jdbcNotes = mapper.readValue(jsonNotes, new TypeReference<>() {});
      } catch (JsonProcessingException e) {
        log.error("Error mapping enrollment notes: {}", jsonNotes);
        return List.of();
      }

      List<Note> notes = new ArrayList<>();
      for (JdbcNote jdbcNote : jdbcNotes) {
        Note note = new Note();
        note.setUid(jdbcNote.getUid());
        note.setNoteText(jdbcNote.getText());
        note.setCreator(jdbcNote.getCreator());
        note.setCreated(DateUtils.safeParseDate(jdbcNote.getCreated()));
        User user = new User();
        user.setUid(jdbcNote.getUpdatedByUid());
        user.setUsername(jdbcNote.getUpdatedByUsername());
        user.setFirstName(jdbcNote.getUpdatedByFirstname());
        user.setSurname(jdbcNote.getUpdatedBySurname());
        user.setName(jdbcNote.getUpdatedByName());
        note.setLastUpdatedBy(user);
        notes.add(note);
      }

      return notes;
    }

    private Set<TrackedEntityAttributeValue> mapTrackedEntityAttributeValues(
        String jsonAttributes, TrackedEntity trackedEntity) {
      List<JdbcAttribute> attributes;
      ObjectMapper mapper = new ObjectMapper();
      try {
        attributes = mapper.readValue(jsonAttributes, new TypeReference<>() {});
      } catch (JsonProcessingException e) {
        log.error("Error mapping enrollment attributes: {}", jsonAttributes);
        throw new IllegalArgumentException(
            String.format(
                "Enrollment attributes cannot be mapped: %s. Request enrollments without attributes to receive a valid response.",
                jsonAttributes));
      }

      Set<TrackedEntityAttributeValue> trackedEntityAttributeValues = new HashSet<>();
      for (JdbcAttribute attribute : attributes) {
        TrackedEntityAttributeValue teav = new TrackedEntityAttributeValue();
        TrackedEntityAttribute tea = new TrackedEntityAttribute();
        tea.setUid(attribute.getUid());
        tea.setValueType(ValueType.valueOf(attribute.getValueType()));
        tea.setName(attribute.getName());
        tea.setCode(attribute.getCode());
        tea.setConfidential(attribute.isConfidential());
        teav.setAttribute(tea);
        teav.setStoredBy(attribute.getStoredBy());
        teav.setCreated(DateUtils.safeParseDate(attribute.getCreated()));
        teav.setLastUpdated(DateUtils.safeParseDate(attribute.getLastUpdated()));
        teav.setEncryptedValue(attribute.getEncryptedValue());
        teav.setPlainValue(attribute.getValue());
        teav.setTrackedEntity(trackedEntity);

        trackedEntityAttributeValues.add(teav);
      }

      return trackedEntityAttributeValues;
    }

    private Date formatDate(Timestamp timestamp) {
      return timestamp == null ? null : new Date(timestamp.getTime());
    }
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }

  @Getter
  @Setter
  private static class JdbcNote {
    private String uid;
    private String text;
    private String creator;
    private String created;
    private String updatedByUid;
    private String updatedByUsername;
    private String updatedByFirstname;
    private String updatedBySurname;
    private String updatedByName;
  }

  @Getter
  @Setter
  private static class JdbcAttribute {
    private String uid;
    private String name;
    private String code;
    private String value;
    private String encryptedValue;
    private String valueType;
    private boolean confidential;
    private String created;
    private String lastUpdated;
    private String storedBy;
  }
}
