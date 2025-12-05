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
package org.hisp.dhis.tracker.export.relationship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component("org.hisp.dhis.tracker.export.relationship.JdbcRelationshipStore")
@RequiredArgsConstructor
class JdbcRelationshipStore {
  @Qualifier("readOnlyNamedParameterJdbcTemplate")
  private final NamedParameterJdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper;

  private static final org.hisp.dhis.tracker.export.Order DEFAULT_ORDER =
      new org.hisp.dhis.tracker.export.Order("id", SortDirection.DESC);

  /**
   * Relationships can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.relationship.Relationship}.
   */
  private static final Set<String> ORDERABLE_FIELDS = Set.of("created", "createdAtClient");

  public Optional<TrackedEntity> findTrackedEntity(UID trackedEntity, boolean includeDeleted) {
    String sql =
        """
            select te.trackedentityid teId, te.uid as teUid, te.deleted as teDeleted, tet.uid as tetUid,
                   tet.sharing as tetSharing, o.uid as orgUnitUid, o.path as orgUnitPath
            from trackedentity te
            join trackedentitytype tet on tet.trackedentitytypeid = te.trackedentitytypeid
            join organisationunit o on te.organisationunitid = o.organisationunitid
            where te.uid = :uid
        """;

    if (!includeDeleted) {
      sql += " and te.deleted = false ";
    }

    List<TrackedEntity> items =
        jdbcTemplate.query(
            sql,
            new MapSqlParameterSource().addValue("uid", trackedEntity.getValue()),
            (rs, rn) -> {
              TrackedEntity te = new TrackedEntity();
              te.setId(rs.getLong("teId"));
              te.setUid(rs.getString("teUid"));
              te.setDeleted(rs.getBoolean("teDeleted"));

              TrackedEntityType trackedEntityType = new TrackedEntityType();
              trackedEntityType.setUid(rs.getString("tetUid"));
              te.setTrackedEntityType(trackedEntityType);
              String sharingJson = rs.getString("tetSharing");
              if (sharingJson != null && !sharingJson.isEmpty()) {
                try {
                  trackedEntityType.setSharing(objectMapper.readValue(sharingJson, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for tracked entity tracked entity type", e);
                }
              }

              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(rs.getString("orgUnitUid"));
              orgUnit.setPath(rs.getString("orgUnitPath"));
              te.setOrganisationUnit(orgUnit);
              return te;
            });
    return items.stream().findFirst();
  }

  public Optional<Enrollment> findEnrollment(UID enrollment, boolean includeDeleted) {
    String sql =
        """
        select en.enrollmentid as enId, en.uid as enUid, en.deleted as enDeleted, te.uid as teUid,
               tet.uid as tetUid, tet.sharing as tetSharing, p.uid as pUid, o.uid as orgUnitUid, o.path as orgUnitPath
        from enrollment en
        join trackedentity te on en.trackedentityid = te.trackedentityid
        join trackedentitytype tet on tet.trackedentitytypeid = te.trackedentitytypeid
        join program p on en.programid = p.programid
        join organisationunit o on te.organisationunitid = o.organisationunitid
        where en.uid = :uid
    """;

    List<Enrollment> items =
        jdbcTemplate.query(
            sql,
            new MapSqlParameterSource().addValue("uid", enrollment.getValue()),
            (rs, rn) -> {
              Enrollment en = new Enrollment();
              en.setId(rs.getLong("enId"));
              en.setUid(rs.getString("enUid"));
              en.setDeleted(rs.getBoolean("enDeleted"));

              Program program = new Program();
              program.setUid(rs.getString("pUid"));
              TrackedEntityType trackedEntityType = new TrackedEntityType();
              trackedEntityType.setUid(rs.getString("tetUid"));
              String sharingJson = rs.getString("tetSharing");
              if (sharingJson != null && !sharingJson.isEmpty()) {
                try {
                  trackedEntityType.setSharing(objectMapper.readValue(sharingJson, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for enrollment tracked entity type", e);
                }
              }
              en.setProgram(program);

              TrackedEntity trackedEntity = new TrackedEntity();
              trackedEntity.setUid(rs.getString("teUid"));
              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(rs.getString("orgUnitUid"));
              orgUnit.setPath(rs.getString("orgUnitPath"));
              trackedEntity.setOrganisationUnit(orgUnit);
              en.setTrackedEntity(trackedEntity);

              return en;
            });
    Optional<Enrollment> res = items.stream().findFirst();
    if (res.isPresent() && !includeDeleted && res.get().isDeleted()) {
      return Optional.empty();
    }
    return res;
  }

  public Optional<TrackerEvent> findTrackerEvent(UID event, boolean includeDeleted) {
    String sql =
        """
        select tev.eventid as tevId, tev.uid as tevUid, tev.deleted as tevDeleted, ps.uid as psUid, ps.sharing as psSharing, p.uid as pUid, p.accesslevel as pAccessLevel, tet.uid as tetUid, tet.sharing as tetSharing, en.uid as enUid, te.uid as teUid, o.uid as orgUnitUid, o.path as orgUnitPath, coc.uid as cocUid, co.uid as coUid, co.sharing as coSharing
        from trackerevent tev
        join programstage ps on tev.programstageid = ps.programstageid
        join program p on ps.programid = p.programid
        join trackedentitytype tet on p.trackedentitytypeid =  tet.trackedentitytypeid
        join enrollment en on tev.enrollmentid = en.enrollmentid
        join trackedentity te on en.trackedentityid = te.trackedentityid
        join organisationunit o on te.organisationunitid = o.organisationunitid
        left join categoryoptioncombo coc on tev.attributeoptioncomboid = coc.categoryoptioncomboid
        left join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid
        left join categoryoption co on cocco.categoryoptionid = co.categoryoptionid
        where tev.uid = :uid
    """;

    List<TrackerEvent> items =
        jdbcTemplate.query(
            sql,
            new MapSqlParameterSource().addValue("uid", event.getValue()),
            (rs, rn) -> {
              TrackerEvent trackerEvent = new TrackerEvent();
              trackerEvent.setId(rs.getLong("tevId"));
              trackerEvent.setUid(rs.getString("tevUid"));
              trackerEvent.setDeleted(rs.getBoolean("tevDeleted"));

              ProgramStage programStage = new ProgramStage();
              programStage.setUid(rs.getString("psUid"));
              String programStageSharing = rs.getString("psSharing");
              if (programStageSharing != null && !programStageSharing.isEmpty()) {
                try {
                  programStage.setSharing(
                      objectMapper.readValue(programStageSharing, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for tracker event program stage", e);
                }
              }
              trackerEvent.setProgramStage(programStage);
              Program program = new Program();
              program.setUid(rs.getString("pUid"));
              program.setAccessLevel(AccessLevel.valueOf(rs.getString("pAccessLevel")));
              TrackedEntityType trackedEntityType = new TrackedEntityType();
              trackedEntityType.setUid(rs.getString("tetUid"));
              String trackedEntityTypeSharing = rs.getString("tetSharing");
              if (trackedEntityTypeSharing != null && !trackedEntityTypeSharing.isEmpty()) {
                try {
                  trackedEntityType.setSharing(
                      objectMapper.readValue(trackedEntityTypeSharing, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for tracker event program tracked entity type",
                      e);
                }
              }
              program.setTrackedEntityType(trackedEntityType);
              programStage.setProgram(program);

              Enrollment enrollment = new Enrollment();
              enrollment.setUid(rs.getString("enUid"));
              trackerEvent.setEnrollment(enrollment);

              TrackedEntity trackedEntity = new TrackedEntity();
              trackedEntity.setUid(rs.getString("teUid"));
              trackedEntity.setTrackedEntityType(trackedEntityType);
              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(rs.getString("orgUnitUid"));
              orgUnit.setPath(rs.getString("orgUnitPath"));
              trackedEntity.setOrganisationUnit(orgUnit);
              enrollment.setTrackedEntity(trackedEntity);
              CategoryOptionCombo coc = new CategoryOptionCombo();
              coc.setUid(rs.getString("cocUid"));
              CategoryOption categoryOption = new CategoryOption();
              String categoryOptionSharing = rs.getString("coSharing");
              if (categoryOptionSharing != null && !categoryOptionSharing.isEmpty()) {
                try {
                  categoryOption.setSharing(
                      objectMapper.readValue(categoryOptionSharing, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for tracker event category option", e);
                }
              }
              coc.setCategoryOptions(Set.of(categoryOption));
              trackerEvent.setAttributeOptionCombo(coc);

              return trackerEvent;
            });
    Optional<TrackerEvent> res = items.stream().findFirst();
    if (res.isPresent() && !includeDeleted && res.get().isDeleted()) {
      return Optional.empty();
    }
    return res;
  }

  public Optional<SingleEvent> findSingleEvent(UID event, boolean includeDeleted) {
    String sql =
        """
        select sev.eventid as sevId, sev.uid as sevUid, sev.deleted as sevDeleted, ps.uid as psUid, p.uid as pUid, p.sharing as pSharing, p.accesslevel as pAccessLevel, o.uid as orgUnitUid, o.path as orgUnitPath, coc.uid as cocUid, co.uid as coUid, co.sharing as coSharing
        from singleevent sev
        join programstage ps on sev.programstageid = ps.programstageid
        join program p on  ps.programid = p.programid
        join organisationunit o on sev.organisationunitid = o.organisationunitid
        left join categoryoptioncombo coc on sev.attributeoptioncomboid = coc.categoryoptioncomboid
        left join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid
        left join categoryoption co on cocco.categoryoptionid = co.categoryoptionid
        where sev.uid = :uid
    """;

    List<SingleEvent> items =
        jdbcTemplate.query(
            sql,
            new MapSqlParameterSource().addValue("uid", event.getValue()),
            (rs, rn) -> {
              SingleEvent singleEvent = new SingleEvent();
              singleEvent.setId(rs.getLong("sevId"));
              singleEvent.setUid(rs.getString("sevUid"));
              singleEvent.setDeleted(rs.getBoolean("sevDeleted"));

              ProgramStage programStage = new ProgramStage();
              programStage.setUid(rs.getString("psUid"));
              singleEvent.setProgramStage(programStage);

              Program program = new Program();
              program.setUid(rs.getString("pUid"));
              program.setAccessLevel(AccessLevel.valueOf(rs.getString("pAccessLevel")));
              String programSharing = rs.getString("pSharing");
              if (programSharing != null && !programSharing.isEmpty()) {
                try {
                  program.setSharing(objectMapper.readValue(programSharing, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for single event program", e);
                }
              }
              programStage.setProgram(program);

              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(rs.getString("orgUnitUid"));
              orgUnit.setPath(rs.getString("orgUnitPath"));
              singleEvent.setOrganisationUnit(orgUnit);

              CategoryOptionCombo coc = new CategoryOptionCombo();
              coc.setUid(rs.getString("cocUid"));
              CategoryOption categoryOption = new CategoryOption();
              String categoryOptionSharing = rs.getString("coSharing");
              if (categoryOptionSharing != null && !categoryOptionSharing.isEmpty()) {
                try {
                  categoryOption.setSharing(
                      objectMapper.readValue(categoryOptionSharing, Sharing.class));
                } catch (JsonProcessingException e) {
                  throw new RuntimeException(
                      "Failed to parse Sharing JSON for tracker event category option", e);
                }
              }
              coc.setCategoryOptions(Set.of(categoryOption));
              singleEvent.setAttributeOptionCombo(coc);

              return singleEvent;
            });
    Optional<SingleEvent> res = items.stream().findFirst();
    if (res.isPresent() && !includeDeleted && res.get().isDeleted()) {
      return Optional.empty();
    }
    return res;
  }

  /*  public List<Relationship> getRelationships(@Nonnull RelationshipQueryParams queryParams) {
    return relationshipsList(queryParams, null);
  }

  public Page<Relationship> getRelationships(
      @Nonnull final RelationshipQueryParams queryParams, @Nonnull PageParams pageParams) {
    List<Relationship> relationships = relationshipsList(queryParams, pageParams);
    return new Page<>(relationships, pageParams, () -> countRelationships(queryParams));
  }*/

  public List<Relationship> getRelationshipsByRelationshipKeys(
      List<RelationshipKey> relationshipKeys) {
    if (CollectionUtils.isEmpty(relationshipKeys)) {
      return Collections.emptyList();
    }

    List<String> keys = relationshipKeys.stream().map(RelationshipKey::asString).toList();
    String sql =
        "select r.relationshipid as r_id, r.uid as r_uid, r.created, r.lastupdated, r.createdatclient, r.deleted,"
            + " rt.uid as rt_uid, r.from_relationshipitemid as from_id, r.to_relationshipitemid as to_id,"
            + " fte.uid as f_te_uid, fen.uid as f_en_uid, ftev.uid as f_tev_uid, fsev.uid as f_sev_uid,"
            + " tte.uid as t_te_uid, ten.uid as t_en_uid, ttev.uid as t_tev_uid, tsev.uid as t_sev_uid,"
            + " f_en_te.uid as f_en_te_uid, t_en_te.uid as t_en_te_uid,"
            + " fprog.uid as f_en_prog_uid, tprog.uid as t_en_prog_uid,"
            + " fou.uid as f_en_ou_uid, tou.uid as t_en_ou_uid,"
            + " coalesce(fen.followup,false) as f_en_followup, coalesce(ten.followup,false) as t_en_followup "
            + " from relationship r "
            + " join relationshiptype rt on rt.relationshiptypeid = r.relationshiptypeid "
            + " left join relationshipitem fi on fi.relationshipitemid = r.from_relationshipitemid "
            + " left join relationshipitem ti on ti.relationshipitemid = r.to_relationshipitemid "
            + " left join trackedentity fte on fte.trackedentityid = fi.trackedentityid "
            + " left join enrollment fen on fen.enrollmentid = fi.enrollmentid "
            + " left join program fprog on fprog.programid = fen.programid "
            + " left join organisationunit fou on fou.organisationunitid = fen.organisationunitid "
            + " left join trackerevent ftev on ftev.eventid = fi.trackereventid "
            + " left join singleevent fsev on fsev.eventid = fi.singleeventid "
            + " left join trackedentity f_en_te on f_en_te.trackedentityid = fen.trackedentityid "
            + " left join trackedentity tte on tte.trackedentityid = ti.trackedentityid "
            + " left join enrollment ten on ten.enrollmentid = ti.enrollmentid "
            + " left join program tprog on tprog.programid = ten.programid "
            + " left join organisationunit tou on tou.organisationunitid = ten.organisationunitid "
            + " left join trackerevent ttev on ttev.eventid = ti.trackereventid "
            + " left join singleevent tsev on tsev.eventid = ti.singleeventid "
            + " left join trackedentity t_en_te on t_en_te.trackedentityid = ten.trackedentityid "
            + " where r.deleted = false and (r.key in (:keys) or (r.inverted_key in (:keys) and rt.bidirectional = true))";
    return jdbcTemplate.query(
        sql, new MapSqlParameterSource().addValue("keys", keys), relationshipRowMapper());
  }

  public List<RelationshipItem> getRelationshipItemsByTrackedEntity(
      UID trackedEntity, boolean includeDeleted) {
    String sql =
        "select ri.relationshipitemid as ri_id, ri.relationshipid as r_id,"
            + " r.uid as r_uid, r.created, r.lastupdated, r.createdatclient, r.deleted,"
            + " rt.uid as rt_uid, r.from_relationshipitemid as from_id, r.to_relationshipitemid as to_id,"
            + " te.uid as te_uid"
            + " from relationshipitem ri"
            + " join relationship r on r.relationshipid = ri.relationshipid"
            + " join relationshiptype rt on rt.relationshiptypeid = r.relationshiptypeid"
            + " join trackedentity te on te.trackedentityid = ri.trackedentityid"
            + " where te.uid = :teuid"
            + (includeDeleted ? "" : " and r.deleted = false")
            + " and (r.from_relationshipitemid = ri.relationshipitemid or rt.bidirectional = true)";

    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource().addValue("teuid", trackedEntity.getValue()),
        relationshipItemRowMapper());
  }

  public List<RelationshipItem> getRelationshipItemsByEnrollment(
      UID enrollment, boolean includeDeleted) {
    String sql =
        "select ri.relationshipitemid as ri_id, ri.relationshipid as r_id,"
            + " r.uid as r_uid, r.created, r.lastupdated, r.createdatclient, r.deleted,"
            + " rt.uid as rt_uid, r.from_relationshipitemid as from_id, r.to_relationshipitemid as to_id,"
            + " en.uid as en_uid"
            + " from relationshipitem ri"
            + " join relationship r on r.relationshipid = ri.relationshipid"
            + " join relationshiptype rt on rt.relationshiptypeid = r.relationshiptypeid"
            + " join enrollment en on en.enrollmentid = ri.enrollmentid"
            + " where en.uid = :enuid"
            + (includeDeleted ? "" : " and r.deleted = false")
            + " and (r.from_relationshipitemid = ri.relationshipitemid or rt.bidirectional = true)";

    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource().addValue("enuid", enrollment.getValue()),
        relationshipItemRowMapper());
  }

  public List<RelationshipItem> getRelationshipItemsByEvent(UID event, boolean includeDeleted) {
    String sql =
        "select ri.relationshipitemid as ri_id, ri.relationshipid as r_id,"
            + " r.uid as r_uid, r.created, r.lastupdated, r.createdatclient, r.deleted,"
            + " rt.uid as rt_uid, r.from_relationshipitemid as from_id, r.to_relationshipitemid as to_id"
            + " from relationshipitem ri"
            + " join relationship r on r.relationshipid = ri.relationshipid"
            + " join relationshiptype rt on rt.relationshiptypeid = r.relationshiptypeid"
            + " left join trackerevent tev on tev.eventid = ri.trackereventid"
            + " left join singleevent sev on sev.eventid = ri.singleeventid"
            + " where (coalesce(sev.uid,'') = :evuid or coalesce(tev.uid,'') = :evuid)"
            + (includeDeleted ? "" : " and r.deleted = false")
            + " and (r.from_relationshipitemid = ri.relationshipitemid or rt.bidirectional = true)";

    return jdbcTemplate.query(
        sql,
        new MapSqlParameterSource().addValue("evuid", event.getValue()),
        relationshipItemRowMapper());
  }

  /*  private List<Relationship> relationshipsList(
        RelationshipQueryParams queryParams, PageParams pageParams) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      StringBuilder sql = new StringBuilder();
      sql.append(
          "select r.relationshipid as r_id, r.uid as r_uid, r.created, r.lastupdated, r.createdatclient, r.deleted,"
              + " rt.uid as rt_uid, r.from_relationshipitemid as from_id, r.to_relationshipitemid as to_id,"
              + " fte.uid as f_te_uid, fen.uid as f_en_uid, ftev.uid as f_tev_uid, fsev.uid as f_sev_uid,"
              + " tte.uid as t_te_uid, ten.uid as t_en_uid, ttev.uid as t_tev_uid, tsev.uid as t_sev_uid,"
              + " f_en_te.uid as f_en_te_uid, t_en_te.uid as t_en_te_uid,"
              + " fprog.uid as f_en_prog_uid, tprog.uid as t_en_prog_uid,"
              + " fou.uid as f_en_ou_uid, tou.uid as t_en_ou_uid,"
              + " coalesce(fen.followup,false) as f_en_followup, coalesce(ten.followup,false) as t_en_followup "
              + " from relationship r join relationshiptype rt on rt.relationshiptypeid = r.relationshiptypeid "
              + " left join relationshipitem fi on fi.relationshipitemid = r.from_relationshipitemid "
              + " left join relationshipitem ti on ti.relationshipitemid = r.to_relationshipitemid "
              + " left join trackedentity fte on fte.trackedentityid = fi.trackedentityid "
              + " left join enrollment fen on fen.enrollmentid = fi.enrollmentid "
              + " left join program fprog on fprog.programid = fen.programid "
              + " left join organisationunit fou on fou.organisationunitid = fen.organisationunitid "
              + " left join trackerevent ftev on ftev.eventid = fi.trackereventid "
              + " left join singleevent fsev on fsev.eventid = fi.singleeventid "
              + " left join trackedentity f_en_te on f_en_te.trackedentityid = fen.trackedentityid "
              + " left join trackedentity tte on tte.trackedentityid = ti.trackedentityid "
              + " left join enrollment ten on ten.enrollmentid = ti.enrollmentid "
              + " left join program tprog on tprog.programid = ten.programid "
              + " left join organisationunit tou on tou.organisationunitid = ten.organisationunitid "
              + " left join trackerevent ttev on ttev.eventid = ti.trackereventid "
              + " left join singleevent tsev on tsev.eventid = ti.singleeventid "
              + " left join trackedentity t_en_te on t_en_te.trackedentityid = ten.trackedentityid ");

      // where conditions
      List<String> wheres = new ArrayList<>();
      if (queryParams.getEntity() != null) {
        String discriminator = relationshipEntityDiscriminator(queryParams.getEntity());
        wheres.add(
            "exists (select 1 from relationshipitem x where x.relationshipitemid = r.from_relationshipitemid and x."
                + discriminator
                + " = :entId)"
                + " or exists (select 1 from relationshipitem y where y.relationshipitemid = r.to_relationshipitemid and y."
                + discriminator
                + " = :entId)");
        params.addValue("entId", queryParams.getEntity().getId());
      } else if (queryParams.getRelationships() != null
          && !queryParams.getRelationships().isEmpty()) {
        wheres.add("r.uid in (:uids)");
        params.addValue("uids", UID.toValueList(queryParams.getRelationships()));
      }
      if (!queryParams.isIncludeDeleted()) {
        wheres.add("r.deleted = false");
      }
      if (!wheres.isEmpty()) {
        sql.append(" where ").append(String.join(" and ", wheres));
      }

      // order by
      String orderBy = orderBy(queryParams.getOrder());
      if (!orderBy.isBlank()) {
        sql.append(" order by ").append(orderBy).append(", r.relationshipid desc");
      } else {
        sql.append(" order by r.relationshipid desc");
      }

      if (pageParams != null) {
        sql.append(" limit :limit offset :offset");
        params.addValue("limit", pageParams.getPageSize() + 1);
        params.addValue("offset", pageParams.getOffset());
      }

      return jdbcTemplate.query(sql.toString(), params, relationshipRowMapper());
    }

    private long countRelationships(RelationshipQueryParams queryParams) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      StringBuilder sql = new StringBuilder("select count(*) from relationship r");
      List<String> wheres = new ArrayList<>();
      if (queryParams.getEntity() != null) {
        String discriminator = relationshipEntityDiscriminator(queryParams.getEntity());
        wheres.add(
            "(exists (select 1 from relationshipitem x where x.relationshipitemid = r.from_relationshipitemid and x."
                + discriminator
                + " = :entId) or exists (select 1 from relationshipitem y where y.relationshipitemid = r.to_relationshipitemid and y."
                + discriminator
                + " = :entId))");
        params.addValue("entId", queryParams.getEntity().getId());
      } else if (queryParams.getRelationships() != null
          && !queryParams.getRelationships().isEmpty()) {
        wheres.add("r.uid in (:uids)");
        params.addValue("uids", UID.toValueList(queryParams.getRelationships()));
      }
      if (!queryParams.isIncludeDeleted()) {
        wheres.add("r.deleted = false");
      }
      if (!wheres.isEmpty()) {
        sql.append(" where ").append(String.join(" and ", wheres));
      }
      return jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
    }

    private String relationshipEntityDiscriminator(SoftDeletableObject entity) {
      if (entity instanceof TrackedEntity) return "trackedentityid";
      else if (entity instanceof Enrollment) return "enrollmentid";
      else if (entity instanceof TrackerEvent) return "trackereventid";
      else if (entity instanceof SingleEvent) return "singleeventid";
      throw new IllegalArgumentException(entity.getClass().getSimpleName() + " not supported");
    }

    private String orderBy(List<org.hisp.dhis.tracker.export.Order> orderList) {
      if (orderList == null || orderList.isEmpty()) return "";
      Map<String, String> columns =
          new HashMap<>(
              Map.ofEntries(
                  entry("created", "r.created"), entry("createdAtClient", "r.createdatclient")));
      return orderList.stream()
          .map(
              o ->
                  columns.containsKey(o.getField())
                      ? columns.get(o.getField())
                          + (o.getDirection().isAscending() ? " asc" : " desc")
                      : null)
          .filter(s -> s != null && !s.isBlank())
          .collect(Collectors.joining(", "));
    }

    public Set<String> getOrderableFields() {
      return ORDERABLE_FIELDS;
    }
  */

  private RowMapper<Relationship> relationshipRowMapper() {
    return new RowMapper<>() {
      @Override
      public Relationship mapRow(ResultSet rs, int rowNum) throws SQLException {
        Relationship r = new Relationship();
        r.setId(rs.getLong("r_id"));
        r.setUid(rs.getString("r_uid"));
        r.setCreated(rs.getTimestamp("created"));
        r.setLastUpdated(rs.getTimestamp("lastupdated"));
        r.setCreatedAtClient(rs.getTimestamp("createdatclient"));
        r.setDeleted(rs.getBoolean("deleted"));

        RelationshipType type = new RelationshipType();
        type.setUid(rs.getString("rt_uid"));
        r.setRelationshipType(type);

        Long fromId = getLongOrNull(rs, "from_id");
        if (fromId != null) {
          RelationshipItem from = new RelationshipItem();
          from.setId((int) (long) fromId);
          // populate minimal entity reference with UID
          String fTe = rs.getString("f_te_uid");
          String fEn = rs.getString("f_en_uid");
          String fTev = rs.getString("f_tev_uid");
          String fSev = rs.getString("f_sev_uid");
          if (fTe != null) {
            TrackedEntity te = new TrackedEntity();
            te.setUid(fTe);
            from.setTrackedEntity(te);
          } else if (fEn != null) {
            Enrollment en = new Enrollment();
            en.setUid(fEn);
            String fEnTe = rs.getString("f_en_te_uid");
            if (fEnTe != null) {
              TrackedEntity te = new TrackedEntity();
              te.setUid(fEnTe);
              en.setTrackedEntity(te);
            }
            String fEnProg = rs.getString("f_en_prog_uid");
            if (fEnProg != null) {
              org.hisp.dhis.program.Program p = new org.hisp.dhis.program.Program();
              p.setUid(fEnProg);
              en.setProgram(p);
            }
            String fEnOu = rs.getString("f_en_ou_uid");
            if (fEnOu != null) {
              org.hisp.dhis.organisationunit.OrganisationUnit ou =
                  new org.hisp.dhis.organisationunit.OrganisationUnit();
              ou.setUid(fEnOu);
              en.setOrganisationUnit(ou);
            }
            en.setFollowup(rs.getBoolean("f_en_followup"));
            from.setEnrollment(en);
          } else if (fTev != null) {
            TrackerEvent ev = new TrackerEvent();
            ev.setUid(fTev);
            from.setTrackerEvent(ev);
          } else if (fSev != null) {
            SingleEvent ev = new SingleEvent();
            ev.setUid(fSev);
            from.setSingleEvent(ev);
          }
          r.setFrom(from);
        } else {
          r.setFrom(null);
        }

        Long toId = getLongOrNull(rs, "to_id");
        if (toId != null) {
          RelationshipItem to = new RelationshipItem();
          to.setId((int) (long) toId);
          String tTe = rs.getString("t_te_uid");
          String tEn = rs.getString("t_en_uid");
          String tTev = rs.getString("t_tev_uid");
          String tSev = rs.getString("t_sev_uid");
          if (tTe != null) {
            TrackedEntity te = new TrackedEntity();
            te.setUid(tTe);
            to.setTrackedEntity(te);
          } else if (tEn != null) {
            Enrollment en = new Enrollment();
            en.setUid(tEn);
            String tEnTe = rs.getString("t_en_te_uid");
            if (tEnTe != null) {
              TrackedEntity te = new TrackedEntity();
              te.setUid(tEnTe);
              en.setTrackedEntity(te);
            }
            String tEnProg = rs.getString("t_en_prog_uid");
            if (tEnProg != null) {
              org.hisp.dhis.program.Program p = new org.hisp.dhis.program.Program();
              p.setUid(tEnProg);
              en.setProgram(p);
            }
            String tEnOu = rs.getString("t_en_ou_uid");
            if (tEnOu != null) {
              org.hisp.dhis.organisationunit.OrganisationUnit ou =
                  new org.hisp.dhis.organisationunit.OrganisationUnit();
              ou.setUid(tEnOu);
              en.setOrganisationUnit(ou);
            }
            en.setFollowup(rs.getBoolean("t_en_followup"));
            to.setEnrollment(en);
          } else if (tTev != null) {
            TrackerEvent ev = new TrackerEvent();
            ev.setUid(tTev);
            to.setTrackerEvent(ev);
          } else if (tSev != null) {
            SingleEvent ev = new SingleEvent();
            ev.setUid(tSev);
            to.setSingleEvent(ev);
          }
          r.setTo(to);
        } else {
          r.setTo(null);
        }
        return r;
      }
    };
  }

  private RowMapper<RelationshipItem> relationshipItemRowMapper() {
    return (rs, rowNum) -> {
      Relationship rel = new Relationship();
      rel.setId(rs.getLong("r_id"));
      rel.setUid(rs.getString("r_uid"));
      rel.setCreated(rs.getTimestamp("created"));
      rel.setLastUpdated(rs.getTimestamp("lastupdated"));
      rel.setCreatedAtClient(rs.getTimestamp("createdatclient"));
      rel.setDeleted(rs.getBoolean("deleted"));
      RelationshipType type = new RelationshipType();
      type.setUid(rs.getString("rt_uid"));
      rel.setRelationshipType(type);

      RelationshipItem item = new RelationshipItem();
      item.setId((int) rs.getLong("ri_id"));
      item.setRelationship(rel);
      // Note: the specific TE/EN/EV references are not required for ACL here
      return item;
    };
  }

  private Long getLongOrNull(ResultSet rs, String col) throws SQLException {
    long v = rs.getLong(col);
    return rs.wasNull() ? null : v;
  }
}
