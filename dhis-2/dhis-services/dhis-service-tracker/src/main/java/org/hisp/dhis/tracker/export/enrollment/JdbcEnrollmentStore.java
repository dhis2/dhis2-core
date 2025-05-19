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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.nowMinusDuration;
import static org.hisp.dhis.util.DateUtils.toLongDateWithMillis;
import static org.hisp.dhis.util.DateUtils.toLongGmtDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component("org.hisp.dhis.tracker.export.enrollment.EnrollmentStore")
@RequiredArgsConstructor
public class JdbcEnrollmentStore {

  private static final String DEFAULT_ORDER = "e.enrollmentid desc";
  private static final Set<String> ORDERABLE_FIELDS =
      Set.of(
          "completedDate",
          "created",
          "createdAtClient",
          "enrollmentDate",
          "lastUpdated",
          "lastUpdatedAtClient");

  private final JdbcTemplate jdbcTemplate;

  public List<Enrollment> getEnrollments(EnrollmentQueryParams params) {
    return jdbcTemplate.query(
        getQuery(params), new EnrollmentRowMapper(params.isIncludeAttributes()));
  }

  private String getQuery(EnrollmentQueryParams params) {
    StringBuilder sql = new StringBuilder();
    addSelect(sql, params);
    addEnrollmentFromItem(sql, params);
    addOrderBy(sql, params);

    return sql.toString();
  }

  private void addSelect(StringBuilder sql, EnrollmentQueryParams params) {
    sql.append(
        """
            select e.*,
            p.programid as program_id, p.uid as program_uid, p.name as program_name, p.code as program_code, p.sharing as program_sharing,
            p.description as program_description, p.created as program_created, p.lastupdated as program_lastupdated,
            p.shortname as program_short_name, p.type as program_type, p.accesslevel as program_accesslevel,
            te.uid as tracked_entity_uid, te.code as tracked_entity_code,
            en_ou.uid as en_org_unit_uid, en_ou.path as en_org_unit_path,
            te_ou.uid as te_org_unit_uid, te_ou.path as te_org_unit_path,
            tet.uid as tet_uid, tet.sharing as tet_sharing, notes.jsonnotes as notes
        """);

    if (params.isIncludeAttributes()) {
      sql.append(
          """
          , attrs.jsonattributes as attributes
          """);
    }
  }

  private void addCountSelect(StringBuilder sql) {
    sql.append(" select count(distinct e.uid) ");
  }

  private void addEnrollmentFromItem(StringBuilder sql, EnrollmentQueryParams params) {
    sql.append(" from enrollment e ");
    addInnerJoins(sql);
    addLeftLateralNoteJoin(sql);
    addLeftLateralAttributeJoin(sql, params);

    SqlHelper hlp = new SqlHelper(true);
    addLastUpdatedConditions(sql, params, hlp);
    addOrgUnitConditions(sql, params, hlp);
    addProgramConditions(sql, params, hlp);
    addEnrollmentConditions(sql, params, hlp);
    addTrackedEntityConditions(sql, params, hlp);
  }

  private void addInnerJoins(StringBuilder sql) {
    sql.append(
        """
      join trackedentity te on te.trackedentityid = e.trackedentityid
      join trackedentitytype tet on tet.trackedentitytypeid = te.trackedentitytypeid
      join organisationunit en_ou on en_ou.organisationunitid = e.organisationunitid
      join organisationunit te_ou on te_ou.organisationunitid = te.organisationunitid
      join program p on p.programid = e.programid
      """);
  }

  private void addLeftLateralNoteJoin(StringBuilder sql) {
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

  private void addLeftLateralAttributeJoin(StringBuilder sql, EnrollmentQueryParams params) {
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
      StringBuilder sql, EnrollmentQueryParams params, SqlHelper hlp) {
    if (params.hasLastUpdatedDuration()) {
      sql.append(hlp.whereAnd())
          .append("e.lastupdated >= '")
          .append(toLongGmtDate(nowMinusDuration(params.getLastUpdatedDuration())))
          .append("'");
    } else if (params.hasLastUpdated()) {
      sql.append(hlp.whereAnd())
          .append("e.lastupdated >= '")
          .append(toLongDateWithMillis(params.getLastUpdated()))
          .append("'");
    }
  }

  private void addOrgUnitConditions(
      StringBuilder sql, EnrollmentQueryParams params, SqlHelper hlp) {
    if (params.hasOrganisationUnits()) {
      if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)) {
        sql.append(hlp.whereAnd()).append(getDescendantsQuery(params.getOrganisationUnits()));
      } else if (params.isOrganisationUnitMode(OrganisationUnitSelectionMode.CHILDREN)) {
        sql.append(hlp.whereAnd()).append(getChildrenQuery(params.getOrganisationUnits()));
      } else {
        sql.append(hlp.whereAnd())
            .append("en_ou.uid IN (")
            .append(getQuotedCommaDelimitedString(getUids(params.getOrganisationUnits())))
            .append(")");
      }
    }
  }

  private void addProgramConditions(
      StringBuilder sql, EnrollmentQueryParams params, SqlHelper hlp) {
    sql.append(hlp.whereAnd())
        .append("p.type = '")
        .append(ProgramType.WITH_REGISTRATION)
        .append("'");

    if (params.hasProgram()) {
      sql.append(hlp.whereAnd())
          .append("p.uid = '")
          .append(params.getProgram().getUid())
          .append("'");
    }

    if (params.hasProgramStartDate()) {
      sql.append(hlp.whereAnd())
          .append("e.enrollmentdate >= '")
          .append(toLongDateWithMillis(params.getProgramStartDate()))
          .append("'");
    }

    if (params.hasProgramEndDate()) {
      sql.append(hlp.whereAnd())
          .append("e.enrollmentdate <= '")
          .append(toLongDateWithMillis(params.getProgramEndDate()))
          .append("'");
    }
  }

  private void addEnrollmentConditions(
      StringBuilder sql, EnrollmentQueryParams params, SqlHelper hlp) {
    if (params.hasEnrollmentUids()) {
      sql.append(hlp.whereAnd())
          .append("e.uid in (")
          .append(getQuotedCommaDelimitedString(UID.toValueList(params.getEnrollments())))
          .append(")");
    }

    if (params.hasEnrollmentStatus()) {
      sql.append(hlp.whereAnd())
          .append("e.status = '")
          .append(params.getEnrollmentStatus())
          .append("'");
    }

    if (params.hasFollowUp()) {
      sql.append(hlp.whereAnd()).append("e.followup = ").append(params.getFollowUp());
    }

    if (!params.isIncludeDeleted()) {
      sql.append(hlp.whereAnd()).append("e.deleted = false");
    }
  }

  private void addTrackedEntityConditions(
      StringBuilder sql, EnrollmentQueryParams params, SqlHelper hlp) {
    if (params.hasTrackedEntity()) {
      sql.append(hlp.whereAnd())
          .append(" te.uid = '")
          .append(params.getTrackedEntity().getUid())
          .append("' ");
    }
  }

  private void addOrderBy(StringBuilder sql, EnrollmentQueryParams params) {
    sql.append(" order by ");
    sql.append(orderBy(params.getOrder()));
  }

  public Page<Enrollment> getEnrollments(EnrollmentQueryParams params, PageParams pageParams) {
    String sql = getQuery(params);
    sql +=
        String.format(" LIMIT %d OFFSET %d", pageParams.getPageSize() + 1, pageParams.getOffset());

    List<Enrollment> enrollments =
        jdbcTemplate.query(sql, new EnrollmentRowMapper(params.isIncludeAttributes()));
    return new Page<>(enrollments, pageParams, () -> countEnrollments(params));
  }

  private long countEnrollments(EnrollmentQueryParams params) {
    String sql = getCountQuery(params);
    return jdbcTemplate.queryForObject(sql, Long.class);
  }

  private String getCountQuery(EnrollmentQueryParams params) {
    StringBuilder sql = new StringBuilder();
    addCountSelect(sql);
    addEnrollmentFromItem(sql, params);

    return sql.toString()
        .replaceFirst(
            "(?s)^.*?from enrollment e", "select count(distinct e.uid) from enrollment e");
  }

  private String getDescendantsQuery(Set<OrganisationUnit> organisationUnits) {
    StringBuilder ouClause = new StringBuilder();
    ouClause.append("(");

    SqlHelper orHlp = new SqlHelper(true);

    for (OrganisationUnit organisationUnit : organisationUnits) {
      ouClause
          .append(orHlp.or())
          .append("en_ou.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%'");
    }

    ouClause.append(")");
    return ouClause.toString();
  }

  private String getChildrenQuery(Set<OrganisationUnit> organisationUnits) {
    SqlHelper hlp = new SqlHelper(true);
    StringBuilder orgUnits = new StringBuilder();
    for (OrganisationUnit organisationUnit : organisationUnits) {
      orgUnits
          .append(hlp.or())
          .append("(en_ou.path LIKE '")
          .append(organisationUnit.getStoredPath())
          .append("%' AND (en_ou.hierarchylevel = ")
          .append(organisationUnit.getHierarchyLevel())
          .append(" OR en_ou.hierarchylevel = ")
          .append(organisationUnit.getHierarchyLevel() + 1)
          .append("))");
    }
    return orgUnits.toString();
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

    EnrollmentRowMapper(boolean isIncludeAttributes) {
      this.isIncludeAttributes = isIncludeAttributes;
    }

    @Override
    public Enrollment mapRow(ResultSet rs, int rowNum) throws SQLException {
      Enrollment enrollment = new Enrollment();
      enrollment.setId(rs.getLong("enrollmentid"));
      enrollment.setUid(rs.getString("uid"));
      enrollment.setCreated(formatDate(rs.getTimestamp("created")));
      enrollment.setCreatedAtClient(formatDate(rs.getTimestamp("createdatclient")));
      enrollment.setCreatedByUserInfo(mapUserInfo(rs.getString("createdbyuserinfo")));
      enrollment.setLastUpdated(formatDate(rs.getTimestamp("lastupdated")));
      enrollment.setLastUpdatedByUserInfo(mapUserInfo(rs.getString("lastupdatedbyuserinfo")));
      enrollment.setLastUpdatedAtClient(formatDate(rs.getTimestamp("lastupdatedatclient")));
      enrollment.setOccurredDate(formatDate(rs.getTimestamp("occurreddate")));
      enrollment.setEnrollmentDate(formatDate(rs.getTimestamp(("enrollmentdate"))));
      enrollment.setCompletedDate(formatDate(rs.getTimestamp(("completeddate"))));
      enrollment.setFollowup(rs.getBoolean("followup"));
      enrollment.setCompletedBy(rs.getString("completedby"));
      enrollment.setStoredBy(rs.getString("storedby"));
      enrollment.setDeleted(rs.getBoolean("deleted"));
      enrollment.setStatus(EnrollmentStatus.valueOf(rs.getString("status")));
      enrollment.setGeometry(mapGeometry(rs.getString("geometry")));

      TrackedEntityType trackedEntityType = new TrackedEntityType();
      trackedEntityType.setUid(rs.getString("tet_uid"));
      trackedEntityType.setSharing(mapSharingJsonIntoSharingObject(rs.getString("tet_sharing")));

      Program program = new Program();
      program.setId(rs.getLong("program_id"));
      program.setUid(rs.getString("program_uid"));
      program.setName(rs.getString("program_name"));
      program.setShortName(rs.getString("program_short_name"));
      program.setCode(rs.getString("program_code"));
      program.setDescription(rs.getString("program_description"));
      program.setCreated(formatDate(rs.getTimestamp("program_created")));
      program.setLastUpdated(formatDate(rs.getTimestamp("program_lastupdated")));
      program.setTrackedEntityType(trackedEntityType);
      program.setProgramType(ProgramType.valueOf(rs.getString("program_type")));
      program.setAccessLevel(AccessLevel.valueOf(rs.getString("program_accesslevel")));
      program.setSharing(mapSharingJsonIntoSharingObject(rs.getString("program_sharing")));
      enrollment.setProgram(program);

      TrackedEntity trackedEntity = new TrackedEntity();
      trackedEntity.setUid(rs.getString("tracked_entity_uid"));
      trackedEntity.setCode(rs.getString("tracked_entity_code"));
      trackedEntity.setTrackedEntityType(trackedEntityType);
      OrganisationUnit teOrgUnit = new OrganisationUnit();
      teOrgUnit.setUid(rs.getString("te_org_unit_uid"));
      teOrgUnit.setPath(rs.getString("te_org_unit_path"));
      trackedEntity.setOrganisationUnit(teOrgUnit);
      enrollment.setTrackedEntity(trackedEntity);

      OrganisationUnit enrollmentOrgUnit = new OrganisationUnit();
      enrollmentOrgUnit.setUid(rs.getString("en_org_unit_uid"));
      enrollmentOrgUnit.setPath(rs.getString("en_org_unit_path"));
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

      return enrollment;
    }

    private Geometry mapGeometry(String geometry) {
      if (Strings.isNullOrEmpty(geometry)) {
        return null;
      }

      try {
        WKBReader reader = new WKBReader();
        byte[] bytes = WKBReader.hexToBytes(geometry);
        return reader.read(bytes);
      } catch (ParseException e) {
        log.error("Error mapping enrollment geometry: {}", geometry);
        return null;
      }
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

    private UserInfoSnapshot mapUserInfo(String jsonUser) {
      if (StringUtils.isEmpty(jsonUser)) {
        return null;
      }

      ObjectMapper mapper = new ObjectMapper();
      try {
        return mapper.readValue(jsonUser, UserInfoSnapshot.class);
      } catch (JsonProcessingException e) {
        log.error("Error mapping enrollment user info: {}", jsonUser);
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
        note.setCreated(formatDate(jdbcNote.getCreated()));
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
        return Set.of();
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
        teav.setCreated(formatDate(attribute.getCreated()));
        teav.setLastUpdated(formatDate(attribute.getLastUpdated()));
        teav.setEncryptedValue(attribute.getEncryptedValue());
        teav.setPlainValue(attribute.getValue());
        teav.setTrackedEntity(trackedEntity);

        trackedEntityAttributeValues.add(teav);
      }

      return trackedEntityAttributeValues;
    }

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private Date formatDate(String date) {
      return date == null ? null : toDate(LocalDateTime.parse(date).atZone(ZONE_ID));
    }

    private Date formatDate(Timestamp timestamp) {
      return timestamp == null ? null : toDate(timestamp.toInstant().atZone(ZONE_ID));
    }

    private Date toDate(ZonedDateTime zonedDateTime) {
      return Date.from(zonedDateTime.toInstant());
    }
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS;
  }

  public void delete(@Nonnull Enrollment enrollment) {
    String sql = "UPDATE enrollment SET deleted = true WHERE enrollmentid = ?";
    jdbcTemplate.update(sql, enrollment.getId());
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
