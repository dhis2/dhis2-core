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
package org.hisp.dhis.tracker.export.trackerevent;

import static java.util.Map.entry;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.FilterJdbcPredicate.addPredicates;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOwnershipClause;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.export.Geometries;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.OrderJdbcClause;
import org.hisp.dhis.tracker.export.OrderJdbcClause.SqlOrder;
import org.hisp.dhis.tracker.export.UserInfoSnapshots;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository("org.hisp.dhis.tracker.export.trackerevent.EventStore")
@RequiredArgsConstructor
class JdbcTrackerEventStore {
  private static final String EVENT_NOTE_QUERY =
      """
      select evn.eventid as evn_id,\
       n.noteid as note_id,\
       n.notetext as note_text,\
       n.created as note_created,\
       n.creator as note_creator,\
       n.uid as note_uid,\
       userinfo.userinfoid as note_user_id,\
       userinfo.code as note_user_code,\
       userinfo.uid as note_user_uid,\
       userinfo.username as note_user_username,\
       userinfo.firstname as note_user_firstname,\
       userinfo.surname as note_user_surname\
       from trackerevent_notes evn\
       inner join note n\
       on evn.noteid = n.noteid\
       left join userinfo on n.lastupdatedby = userinfo.userinfoid\s""";

  private static final String DEFAULT_ORDER = "ev_created desc, ev_id desc";
  private static final String PK_COLUMN = "ev_id";

  /**
   * Events can be ordered by given fields which correspond to fields on {@link TrackerEvent}. Maps
   * fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", "ev_uid"),
          entry("enrollment.program.uid", "p_uid"),
          entry("programStage.uid", "ps_uid"),
          entry("enrollment.uid", "en_uid"),
          entry("enrollment.status", "en_status"),
          entry("enrollment.enrollmentDate", "en_enrollmentdate"),
          entry("organisationUnit.uid", "orgunit_uid"),
          entry("enrollment.trackedEntity.uid", "te_uid"),
          entry("occurredDate", "ev_occurreddate"),
          entry("enrollment.followUp", "en_followup"),
          entry("status", "ev_status"),
          entry("scheduledDate", "ev_scheduleddate"),
          entry("storedBy", "ev_storedby"),
          entry("lastUpdatedBy", "ev_lastupdatedbyuserinfo"),
          entry("createdBy", "ev_createdbyuserinfo"),
          entry("created", "ev_created"),
          entry("createdAtClient", "ev_createdatclient"),
          entry("lastUpdated", "ev_lastupdated"),
          entry("lastUpdatedAtClient", "ev_lastupdatedatclient"),
          entry("completedBy", "ev_completedby"),
          entry("attributeOptionCombo.uid", "coc_uid"),
          entry("completedDate", "ev_completeddate"),
          entry("deleted", "ev_deleted"),
          entry("assignedUser", "user_assigned_username"),
          entry("assignedUser.displayName", "user_assigned_name"));

  // Cannot use DefaultRenderService mapper. Does not work properly -
  // DHIS2-6102
  private static final ObjectReader eventDataValueJsonReader =
      JsonBinaryType.MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});

  private final NamedParameterJdbcTemplate jdbcTemplate;

  public List<TrackerEvent> getEvents(TrackerEventQueryParams queryParams) {
    return fetchEvents(queryParams, null);
  }

  public Page<TrackerEvent> getEvents(TrackerEventQueryParams queryParams, PageParams pageParams) {
    List<TrackerEvent> events = fetchEvents(queryParams, pageParams);
    return new Page<>(events, pageParams, () -> getEventCount(queryParams));
  }

  private List<TrackerEvent> fetchEvents(
      TrackerEventQueryParams queryParams, PageParams pageParams) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();

    Map<String, TrackerEvent> eventsByUid;
    if (pageParams == null) {
      eventsByUid = new HashMap<>();
    } else {
      eventsByUid =
          new HashMap<>(
              pageParams.getPageSize() + 1); // get extra event to determine if there is a nextPage
    }
    List<TrackerEvent> events = new ArrayList<>();

    final MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql = buildSql(queryParams, pageParams, sqlParameters, currentUser);

    TrackerIdSchemeParam dataElementIdScheme =
        queryParams.getIdSchemeParams().getDataElementIdScheme();

    return jdbcTemplate.query(
        sql,
        sqlParameters,
        resultSet -> {
          Set<String> notes = new HashSet<>();
          // data elements per event
          Map<String, Set<String>> dataElementUids = new HashMap<>();

          while (resultSet.next()) {
            if (resultSet.getString("ev_uid") == null) {
              continue;
            }

            String eventUid = resultSet.getString("ev_uid");

            TrackerEvent event;
            if (eventsByUid.containsKey(eventUid)) {
              event = eventsByUid.get(eventUid);
            } else {
              event = new TrackerEvent();
              event.setUid(eventUid);
              eventsByUid.put(eventUid, event);
              dataElementUids.put(eventUid, new HashSet<>());

              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(resultSet.getString("orgunit_uid"));
              orgUnit.setCode(resultSet.getString("orgunit_code"));
              orgUnit.setName(resultSet.getString("orgunit_name"));
              orgUnit.setAttributeValues(
                  AttributeValues.of(resultSet.getString("orgunit_attributevalues")));

              TrackedEntity te = new TrackedEntity();
              te.setUid(resultSet.getString("te_uid"));
              OrganisationUnit teOrgUnit = new OrganisationUnit();
              teOrgUnit.setUid(resultSet.getString("te_org_unit_uid"));
              te.setOrganisationUnit(teOrgUnit);
              event.setStatus(EventStatus.valueOf(resultSet.getString("ev_status")));

              Program program = new Program();
              if (queryParams.hasEnrolledInTrackerProgram()) {
                Program p = queryParams.getEnrolledInTrackerProgram();
                program.setUid(p.getUid());
                program.setCode(p.getCode());
                program.setName(p.getName());
                program.setAttributeValues(p.getAttributeValues());
                program.setProgramType(p.getProgramType());
              } else {
                program.setUid(resultSet.getString("p_uid"));
                program.setCode(resultSet.getString("p_code"));
                program.setName(resultSet.getString("p_name"));
                program.setAttributeValues(
                    AttributeValues.of(resultSet.getString("p_attributevalues")));
                program.setProgramType(ProgramType.fromValue(resultSet.getString("p_type")));
              }

              Enrollment enrollment = new Enrollment();
              enrollment.setUid(resultSet.getString("en_uid"));
              enrollment.setProgram(program);
              enrollment.setTrackedEntity(te);
              event.setOrganisationUnit(orgUnit);

              ProgramStage ps = new ProgramStage();
              if (queryParams.hasProgramStage()) {
                ProgramStage qps = queryParams.getProgramStage();
                ps.setUid(qps.getUid());
                ps.setCode(qps.getCode());
                ps.setName(qps.getName());
                ps.setAttributeValues(qps.getAttributeValues());
              } else {
                ps.setUid(resultSet.getString("ps_uid"));
                ps.setCode(resultSet.getString("ps_code"));
                ps.setName(resultSet.getString("ps_name"));
                ps.setAttributeValues(
                    AttributeValues.of(resultSet.getString("ps_attributevalues")));
              }
              ps.setProgram(program);
              event.setDeleted(resultSet.getBoolean("ev_deleted"));

              enrollment.setStatus(EnrollmentStatus.valueOf(resultSet.getString("en_status")));
              enrollment.setFollowup(resultSet.getBoolean("en_followup"));
              event.setEnrollment(enrollment);
              event.setProgramStage(ps);

              CategoryOptionCombo coc = new CategoryOptionCombo();
              coc.setUid(resultSet.getString("coc_uid"));
              coc.setCode(resultSet.getString("coc_code"));
              coc.setName(resultSet.getString("coc_name"));
              coc.setAttributeValues(
                  AttributeValues.of(resultSet.getString("coc_attributevalues")));

              String cosString = resultSet.getString("co_values");
              JsonMixed cosJson = JsonMixed.of(cosString);
              JsonObject object = cosJson.asObject();
              Set<CategoryOption> options = new HashSet<>(object.names().size());
              for (String uid : object.names()) {
                JsonObject categoryOptionJson = object.getObject(uid);
                CategoryOption option = new CategoryOption();
                option.setUid(uid);
                option.setCode(categoryOptionJson.getString("code").string(""));
                option.setName(categoryOptionJson.getString("name").string(""));
                option.setAttributeValues(
                    AttributeValues.of(categoryOptionJson.getObject("attributeValues").toJson()));
                options.add(option);
              }
              coc.setCategoryOptions(options);
              event.setAttributeOptionCombo(coc);

              event.setStoredBy(resultSet.getString("ev_storedby"));
              event.setScheduledDate(resultSet.getTimestamp("ev_scheduleddate"));
              event.setOccurredDate(resultSet.getTimestamp("ev_occurreddate"));
              event.setCreated(resultSet.getTimestamp("ev_created"));
              event.setCreatedAtClient(resultSet.getTimestamp("ev_createdatclient"));
              event.setCreatedByUserInfo(
                  UserInfoSnapshots.fromJson(resultSet.getString("ev_createdbyuserinfo")));
              event.setLastUpdated(resultSet.getTimestamp("ev_lastupdated"));
              event.setLastUpdatedAtClient(resultSet.getTimestamp("ev_lastupdatedatclient"));
              event.setLastUpdatedByUserInfo(
                  UserInfoSnapshots.fromJson(resultSet.getString("ev_lastupdatedbyuserinfo")));

              event.setCompletedBy(resultSet.getString("ev_completedby"));
              event.setCompletedDate(resultSet.getTimestamp("ev_completeddate"));

              event.setGeometry(Geometries.fromWkb(resultSet.getBytes("ev_geometry")));

              if (resultSet.getObject("user_assigned") != null) {
                User eventUser = new User();
                eventUser.setUid(resultSet.getString("user_assigned"));
                eventUser.setUsername(resultSet.getString("user_assigned_username"));
                eventUser.setName(resultSet.getString("user_assigned_name"));
                eventUser.setFirstName(resultSet.getString("user_assigned_first_name"));
                eventUser.setSurname(resultSet.getString("user_assigned_surname"));
                event.setAssignedUser(eventUser);
              }

              if (TrackerIdScheme.UID == dataElementIdScheme.getIdScheme()
                  && !StringUtils.isEmpty(resultSet.getString("ev_eventdatavalues"))) {
                event
                    .getEventDataValues()
                    .addAll(
                        convertEventDataValueJsonIntoSet(
                            resultSet.getString("ev_eventdatavalues")));
              }

              events.add(event);
            }

            if (TrackerIdScheme.UID != dataElementIdScheme.getIdScheme()) {
              // We get one row per eventdatavalue for idSchemes other than UID due to the
              // need to
              // join on the dataelement table to get idScheme information. There can only
              // be one
              // data value per data element. The same data element can be in the result set
              // multiple times if the event also has notes.
              String dataElementUid = resultSet.getString("de_uid");
              if (!dataElementUids.get(eventUid).contains(dataElementUid)) {
                EventDataValue eventDataValue = parseEventDataValue(dataElementIdScheme, resultSet);
                if (eventDataValue != null) {
                  event.getEventDataValues().add(eventDataValue);
                  dataElementUids.get(eventUid).add(dataElementUid);
                }
              }
            }

            if (resultSet.getString("note_text") != null
                && !notes.contains(resultSet.getString("note_id"))) {
              Note note = new Note();
              note.setUid(resultSet.getString("note_uid"));
              note.setNoteText(resultSet.getString("note_text"));
              note.setCreated(resultSet.getTimestamp("note_created"));
              note.setCreator(resultSet.getString("note_creator"));

              if (resultSet.getObject("note_user_id") != null) {
                User noteLastUpdatedBy = new User();
                noteLastUpdatedBy.setId(resultSet.getLong("note_user_id"));
                noteLastUpdatedBy.setCode(resultSet.getString("note_user_code"));
                noteLastUpdatedBy.setUid(resultSet.getString("note_user_uid"));
                noteLastUpdatedBy.setUsername(resultSet.getString("note_user_username"));
                noteLastUpdatedBy.setFirstName(resultSet.getString("note_user_firstname"));
                noteLastUpdatedBy.setSurname(resultSet.getString("note_user_surname"));
                note.setLastUpdatedBy(noteLastUpdatedBy);
              }

              event.getNotes().add(note);
              notes.add(resultSet.getString("note_id"));
            }
          }

          return events;
        });
  }

  private EventDataValue parseEventDataValue(
      TrackerIdSchemeParam dataElementIdScheme, ResultSet resultSet) throws SQLException {
    String dataValueResult = resultSet.getString("ev_eventdatavalue");
    if (StringUtils.isEmpty(dataValueResult)) {
      return null;
    }
    String dataElement = getDataElementIdentifier(dataElementIdScheme, resultSet);
    if (StringUtils.isEmpty(dataElement)) {
      return null;
    }

    EventDataValue eventDataValue = new EventDataValue();
    eventDataValue.setDataElement(dataElement);
    JsonObject dataValueJson = JsonMixed.of(dataValueResult).asObject();
    eventDataValue.setValue(dataValueJson.getString("value").string(""));
    eventDataValue.setProvidedElsewhere(
        dataValueJson.getBoolean("providedElsewhere").booleanValue(false));
    eventDataValue.setStoredBy(dataValueJson.getString("storedBy").string(null));

    eventDataValue.setCreated(DateUtils.parseDate(dataValueJson.getString("created").string("")));
    if (dataValueJson.has("createdByUserInfo")) {
      eventDataValue.setCreatedByUserInfo(
          UserInfoSnapshots.from(dataValueJson.getObject("createdByUserInfo")));
    }

    eventDataValue.setLastUpdated(
        DateUtils.parseDate(dataValueJson.getString("lastUpdated").string("")));
    if (dataValueJson.has("lastUpdatedByUserInfo")) {
      eventDataValue.setLastUpdatedByUserInfo(
          UserInfoSnapshots.from(dataValueJson.getObject("lastUpdatedByUserInfo")));
    }

    return eventDataValue;
  }

  private String getDataElementIdentifier(
      TrackerIdSchemeParam dataElementIdScheme, ResultSet resultSet) throws SQLException {
    switch (dataElementIdScheme.getIdScheme()) {
      case CODE:
        return resultSet.getString("de_code");
      case NAME:
        return resultSet.getString("de_name");
      case ATTRIBUTE:
        String attributeValuesString = resultSet.getString("de_attributevalues");
        if (StringUtils.isEmpty(attributeValuesString)) {
          return null;
        }
        JsonObject attributeValuesJson = JsonMixed.of(attributeValuesString).asObject();
        String attributeUid = dataElementIdScheme.getAttributeUid();
        AttributeValues attributeValues = AttributeValues.of(attributeValuesJson.toJson());
        return attributeValues.get(attributeUid);
      default:
        return resultSet.getString("de_uid");
    }
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  private long getEventCount(TrackerEventQueryParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    MapSqlParameterSource sqlParams = new MapSqlParameterSource();
    String sql = getCountQuery(params, sqlParams, currentUser);
    Long count = jdbcTemplate.queryForObject(sql, sqlParams, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Query is based on three sub queries on event, data value and note, which are joined using event
   * id. The purpose of the separate queries is to be able to page properly on events.
   */
  private String buildSql(
      TrackerEventQueryParams queryParams,
      PageParams pageParams,
      MapSqlParameterSource mapSqlParameterSource,
      UserDetails user) {
    StringBuilder sqlBuilder = new StringBuilder("select *");
    if (TrackerIdScheme.UID
        != queryParams.getIdSchemeParams().getDataElementIdScheme().getIdScheme()) {
      sqlBuilder.append(
          ", eventdatavalue.value as ev_eventdatavalue, de.uid as de_uid, de.code as"
              + " de_code, de.name as de_name, de.attributevalues as de_attributevalues");
    }
    sqlBuilder.append(" from (");

    sqlBuilder.append(getQuery(queryParams, mapSqlParameterSource, user));

    sqlBuilder.append(getOrderQuery(queryParams));

    if (pageParams != null) {
      sqlBuilder.append(getLimitAndOffsetClause(pageParams));
    }

    sqlBuilder.append(") as event left join (");
    sqlBuilder.append(EVENT_NOTE_QUERY);
    sqlBuilder.append(") as cm on event.ev_id=cm.evn_id ");

    if (TrackerIdScheme.UID
        != queryParams.getIdSchemeParams().getDataElementIdScheme().getIdScheme()) {
      sqlBuilder.append(
"""
left join
    lateral jsonb_each(
        coalesce(event.ev_eventdatavalues, '{}')
    ) as eventdatavalue(dataelement_uid, value)
    on true
left join dataelement de on de.uid = eventdatavalue.dataelement_uid
""");
    }

    sqlBuilder.append(getOrderQuery(queryParams));

    return sqlBuilder.toString();
  }

  /**
   * Builds the inner event query:
   *
   * <pre>
   * select ...
   * from trackerevent ev
   *   inner join enrollment en on ...
   *   inner join program p on ...            -- conditional
   *   inner join programstage ps on ...      -- conditional
   *   inner join trackedentity te on ...
   *   inner join trackedentityprogramowner po on ...
   *   inner join organisationunit ou on ...
   *   inner join organisationunit evou on ...
   *   inner join organisationunit teou on ...
   *   left join userinfo au on ...
   *   left join on attributes ...           -- conditional
   *   inner join (...) coc_agg on ...
   * where ...
   * </pre>
   */
  private String getQuery(
      TrackerEventQueryParams params, MapSqlParameterSource sqlParams, UserDetails user) {
    StringBuilder sql = new StringBuilder();
    addSelect(sql, params);
    sql.append(" from trackerevent ev ");
    addJoinOnEnrollment(sql);
    if (!params.hasEnrolledInTrackerProgram()) {
      addJoinOnProgram(sql);
    }
    if (!params.hasProgramStage()) {
      addJoinOnProgramStage(sql);
    }
    addJoinOnTrackedEntity(sql);
    addJoinOnProgramOwner(sql);
    addJoinOnOwnerOrgUnit(sql);
    addJoinOnEventOrgUnit(sql);
    addJoinOnTrackedEntityOrgUnit(sql);
    addLeftJoinOnAssignedUser(sql);
    addJoinOnAttributes(sql, params);
    addJoinOnCategoryOptionCombo(sql, user);
    addWhereConditions(sql, sqlParams, params);
    return sql.toString();
  }

  /**
   * Builds the count query. Only includes joins needed for filtering:
   *
   * <ul>
   *   <li>{@code enrollment} - always needed (links event to program and tracked entity)
   *   <li>{@code program} + {@code trackedentity} - only when no specific program is given. The
   *       program join is needed for {@code p.accesslevel} and {@code p.programid in (...)}; the
   *       tracked entity join is needed for the PROTECTED temp owner check. When a program is
   *       known, conditions use {@code en.programid} / {@code en.trackedentityid} directly and both
   *       joins are skipped.
   *   <li>{@code trackedentityprogramowner} + {@code organisationunit} - ownership and org unit
   *       filtering
   *   <li>{@code categoryoptioncombo} - attribute option combo access control
   * </ul>
   *
   * <p>Always skips: {@code programstage} (uses {@code ev.programstageid} directly), event {@code
   * organisationunit} (evou), tracked entity {@code organisationunit} (teou), {@code userinfo}
   * (au), and attribute LEFT JOINs.
   *
   * <p>Uses {@code count(*)} instead of {@code count(distinct ev.uid)} because all remaining joins
   * are many-to-one from {@code trackerevent}, so no duplicate rows are possible.
   */
  private String getCountQuery(
      TrackerEventQueryParams params, MapSqlParameterSource sqlParams, UserDetails user) {
    boolean needsProgramAndTrackedEntityJoin = !params.hasEnrolledInTrackerProgram();

    StringBuilder sql = new StringBuilder();
    sql.append("select count(*) from trackerevent ev ");
    addJoinOnEnrollment(sql);
    if (needsProgramAndTrackedEntityJoin) {
      addJoinOnProgram(sql);
      addJoinOnTrackedEntity(sql);
    }
    addJoinOnProgramOwner(sql);
    addJoinOnOwnerOrgUnit(sql);
    addJoinOnCategoryOptionCombo(sql, user);
    addCountWhereConditions(sql, sqlParams, params, needsProgramAndTrackedEntityJoin);

    return sql.toString();
  }

  private void addCountWhereConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      boolean hasTrackedEntityJoin) {
    SqlHelper hlp = new SqlHelper(true);
    addDataElementConditions(sql, sqlParams, params);
    if (hasTrackedEntityJoin) {
      addTrackedEntityConditions(sql, sqlParams, params, hlp);
    } else {
      addTrackedEntitySubqueryConditions(sql, sqlParams, params, hlp);
    }
    addCountProgramConditions(sql, sqlParams, params, hlp);
    addCountProgramStageConditions(sql, sqlParams, params, hlp);
    addEnrollmentConditions(sql, sqlParams, params, hlp);
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addCategoryOptionComboConditions(sql, sqlParams, params, hlp);
    addCountOrgUnitConditions(sql, sqlParams, params, hlp, hasTrackedEntityJoin);
    addOccurredDateConditions(sql, sqlParams, params, hlp);
    addEventStatusConditions(sql, sqlParams, params, hlp);
    addEventConditions(sql, sqlParams, params, hlp);
    addAssignedUserConditions(sql, sqlParams, params, hlp);
    addDeletedCondition(sql, params, hlp);
    addEnrollmentUidConditions(sql, sqlParams, params, hlp);
  }

  /**
   * Program condition for count query. When a specific program is known, filters on {@code
   * en.programid} directly (no program table join needed). Otherwise delegates to the standard
   * method which uses {@code p.programid}.
   */
  private void addCountProgramConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.hasEnrolledInTrackerProgram()) {
      sqlParams.addValue("programid", params.getEnrolledInTrackerProgram().getId());
      sql.append(hlp.whereAnd()).append(" en.programid = :programid ");
    } else {
      addProgramConditions(sql, sqlParams, params, hlp);
    }
  }

  /**
   * Program stage condition for count query. Always filters on {@code ev.programstageid} directly
   * (no programstage table join needed) since the FK exists on the trackerevent table.
   */
  private void addCountProgramStageConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.hasProgramStage()) {
      sqlParams.addValue("programstageid", params.getProgramStage().getId());
      sql.append(hlp.whereAnd()).append(" ev.programstageid = :programstageid ");
    } else {
      sqlParams.addValue(
          "programstageid",
          params.getAccessibleTrackerProgramStages().isEmpty()
              ? null
              : getIdentifiers(params.getAccessibleTrackerProgramStages()));
      sql.append(hlp.whereAnd()).append(" ev.programstageid in (:programstageid) ");
    }
  }

  private void addCountOrgUnitConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp,
      boolean hasTrackedEntityJoin) {
    if (params.getOrgUnit() != null) {
      buildOrgUnitModeClause(
          sql,
          sqlParams,
          Set.of(params.getOrgUnit()),
          params.getOrgUnitMode(),
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
          hasTrackedEntityJoin ? "te" : "en",
          hlp::whereAnd);
    } else {
      buildOwnershipClause(sql, sqlParams, params.getOrgUnitMode(), "p", "ou", "te", hlp::whereAnd);
    }
  }

  /**
   * Filters by tracked entity using a subquery when the {@code trackedentity} table is not joined.
   */
  private void addTrackedEntitySubqueryConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getTrackedEntity() != null) {
      sqlParams.addValue("trackedentityid", params.getTrackedEntity().getId());
      sql.append(hlp.whereAnd()).append(" en.trackedentityid = :trackedentityid ");
    }
  }

  private void addSelect(StringBuilder sql, TrackerEventQueryParams params) {
    sql.append(
        """
            select ev.uid as ev_uid,
            evou.uid as orgunit_uid, evou.code as orgunit_code, evou.name as orgunit_name,
            evou.attributevalues as orgunit_attributevalues,\s""");

    if (!params.hasEnrolledInTrackerProgram()) {
      sql.append(
          """
            p.uid as p_uid, p.code as p_code, p.name as p_name,
            p.attributevalues as p_attributevalues,\s""");
    }

    if (!params.hasProgramStage()) {
      sql.append(
          """
            ps.uid as ps_uid, ps.code as ps_code, ps.name as ps_name,
            ps.attributevalues as ps_attributevalues,\s""");
    }

    sql.append(
        """
            ev.eventid as ev_id, ev.status as ev_status,
            ev.occurreddate as ev_occurreddate, ev.scheduleddate as ev_scheduleddate,
            ev.eventdatavalues as ev_eventdatavalues,
            ev.completedby as ev_completedby, ev.storedby as ev_storedby,
            ev.created as ev_created, ev.createdatclient as ev_createdatclient,
            ev.createdbyuserinfo as ev_createdbyuserinfo,
            ev.lastupdated as ev_lastupdated, ev.lastupdatedatclient as ev_lastupdatedatclient,
            ev.lastupdatedbyuserinfo as ev_lastupdatedbyuserinfo,
            ev.completeddate as ev_completeddate, ev.deleted as ev_deleted,
            ST_AsBinary(ev.geometry) as ev_geometry,
            au.uid as user_assigned,
            (au.firstName || ' ' || au.surName) as user_assigned_name,
            au.firstName as user_assigned_first_name, au.surName as user_assigned_surname,
            au.username as user_assigned_username,
            coc_agg.uid as coc_uid, coc_agg.code as coc_code, coc_agg.name as coc_name,
            coc_agg.attributevalues as coc_attributevalues,
            coc_agg.co_values as co_values, coc_agg.co_count as option_size,\s""");
    addOrderFieldsToSelect(sql, params.getOrder());
    sql.append(
        """
            en.uid as en_uid, en.status as en_status, en.followup as en_followup,
            en.enrollmentdate as en_enrollmentdate, en.occurreddate as en_occurreddate,\s""");

    if (!params.hasEnrolledInTrackerProgram()) {
      sql.append(" p.type as p_type, ");
    }

    sql.append(
        """
            te.trackedentityid as te_id, te.uid as te_uid,
            teou.uid as te_org_unit_uid""");
  }

  private void addOrderFieldsToSelect(StringBuilder sql, List<Order> orders) {
    for (Order order : orders) {
      if (order.getField() instanceof TrackedEntityAttribute tea) {
        sql.append(quote(tea.getUid()))
            .append(".value AS ")
            .append(tea.getUid())
            .append("_value, ");
      } else if (order.getField() instanceof DataElement de) {
        final String dataValueValueSql = "ev.eventdatavalues #>> '{" + de.getUid() + ", value}'";
        sql.append(
                de.getValueType().isNumeric()
                    ? SqlUtils.castToNumeric(dataValueValueSql)
                    : lower(dataValueValueSql))
            .append(" as ")
            .append(de.getUid())
            .append(", ");
      }
    }
  }

  private void addJoinOnEnrollment(StringBuilder sql) {
    sql.append(" inner join enrollment en on en.enrollmentid = ev.enrollmentid ");
  }

  private void addJoinOnProgram(StringBuilder sql) {
    sql.append(" inner join program p on p.programid = en.programid ");
  }

  private void addJoinOnProgramStage(StringBuilder sql) {
    sql.append(" inner join programstage ps on ps.programstageid = ev.programstageid ");
  }

  private void addJoinOnTrackedEntity(StringBuilder sql) {
    sql.append(" inner join trackedentity te on te.trackedentityid = en.trackedentityid ");
  }

  private void addJoinOnProgramOwner(StringBuilder sql) {
    sql.append(
        " inner join trackedentityprogramowner po"
            + " on po.trackedentityid = en.trackedentityid and po.programid = en.programid ");
  }

  private void addJoinOnOwnerOrgUnit(StringBuilder sql) {
    sql.append(" inner join organisationunit ou on ou.organisationunitid = po.organisationunitid ");
  }

  private void addJoinOnEventOrgUnit(StringBuilder sql) {
    sql.append(
        " inner join organisationunit evou on evou.organisationunitid = ev.organisationunitid ");
  }

  private void addJoinOnTrackedEntityOrgUnit(StringBuilder sql) {
    sql.append(
        " inner join organisationunit teou on teou.organisationunitid = te.organisationunitid ");
  }

  private void addLeftJoinOnAssignedUser(StringBuilder sql) {
    sql.append(" left join userinfo au on au.userinfoid = ev.assigneduserid ");
  }

  /**
   * Adds joins on tracked entity attribute values for filtering and sorting. Attributes with
   * non-empty filters use INNER JOIN (the WHERE clause eliminates NULLs anyway), which lets the
   * planner use the join as a filter. Order-only attributes use LEFT JOIN to preserve rows without
   * a value.
   */
  private void addJoinOnAttributes(StringBuilder sql, TrackerEventQueryParams params) {
    for (TrackedEntityAttribute attribute : params.getInnerJoinAttributes()) {
      addAttributeJoin(sql, "inner", attribute);
    }
    for (TrackedEntityAttribute attribute : params.getLeftJoinAttributes()) {
      addAttributeJoin(sql, "left", attribute);
    }
  }

  private void addAttributeJoin(
      StringBuilder sql, String joinType, TrackedEntityAttribute attribute) {
    sql.append(" ")
        .append(joinType)
        .append(" join trackedentityattributevalue as ")
        .append(quote(attribute.getUid()))
        .append(" on ")
        .append(quote(attribute.getUid()))
        .append(".trackedentityid = TE.trackedentityid and ")
        .append(quote(attribute.getUid()))
        .append(".trackedentityattributeid = ")
        .append(attribute.getId())
        .append(" ");
  }

  /**
   * Returns the joins and sub-queries needed to fulfill all the needs regarding category option
   * combo and category options. Category option combos (COC) are composed of category options (CO),
   * one per category of the COCs category combination (CC).
   *
   * <p>Important constraints leading to this query:
   *
   * <ul>
   *   <li>While COCs are pre-computed and can be seen as a de-normalization of the possible
   *       permutations the COs in a COC are stored in a normalized way. The final event should have
   *       its attributeCategoryOptions field populated with a semicolon separated string of its
   *       COCs COs. We thus need to aggregate these COs for each event.
   *   <li>COCs should be returned in the user specified idScheme. So in order to have access to
   *       uid, code, name, attributes we need another join as all of these fields cannot be added
   *       to the above aggregation.
   *   <li>A user must have access to all COs of the events COC to have access to an event.
   * </ul>
   */
  private void addJoinOnCategoryOptionCombo(StringBuilder sql, UserDetails user) {
    sql.append(
        """
 inner join (select coc.uid, coc.code, coc.name, coc.attributevalues, coc.categoryoptioncomboid as id,
    jsonb_object_agg(
        co.uid,
        jsonb_build_object(
            'name', co.name,
            'code', co.code,
            'attributeValues', co.attributevalues
        )
    ) as co_values,
 count(co.categoryoptionid) as co_count
 from categoryoptioncombo coc
 inner join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid
 inner join categoryoption co on cocco.categoryoptionid = co.categoryoptionid
 group by coc.categoryoptioncomboid\s""");

    if (isNotSuperUser(user)) {
      sql.append(" having bool_and(case when ")
          .append(
              JpaQueryUtils.generateSQlQueryForSharingCheck(
                  "co.sharing", user, AclService.LIKE_READ_DATA))
          .append(" then true else false end) = true ");
    }

    sql.append(") as coc_agg on coc_agg.id = ev.attributeoptioncomboid ");
  }

  private void addWhereConditions(
      StringBuilder sql, MapSqlParameterSource sqlParams, TrackerEventQueryParams params) {
    SqlHelper hlp = new SqlHelper(true);
    addDataElementConditions(sql, sqlParams, params);
    addAttributeFilterConditions(sql, sqlParams, params, hlp);
    addTrackedEntityConditions(sql, sqlParams, params, hlp);
    addProgramConditions(sql, sqlParams, params, hlp);
    addProgramStageConditions(sql, sqlParams, params, hlp);
    addEnrollmentConditions(sql, sqlParams, params, hlp);
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addCategoryOptionComboConditions(sql, sqlParams, params, hlp);
    addOrgUnitConditions(sql, sqlParams, params, hlp);
    addOccurredDateConditions(sql, sqlParams, params, hlp);
    addEventStatusConditions(sql, sqlParams, params, hlp);
    addEventConditions(sql, sqlParams, params, hlp);
    addAssignedUserConditions(sql, sqlParams, params, hlp);
    addDeletedCondition(sql, params, hlp);
    addEnrollmentUidConditions(sql, sqlParams, params, hlp);
  }

  private void addDataElementConditions(
      StringBuilder sql, MapSqlParameterSource sqlParams, TrackerEventQueryParams params) {
    if (!params.getDataElements().isEmpty()) {
      sql.append(" and ");
      addPredicates(sql, sqlParams, params.getDataElements());
      sql.append(" ");
    }
  }

  /**
   * Generates the WHERE-clause related to the user provided attribute filters. It will find the
   * tracked entity attributes that match the given user filter criteria. This condition only
   * applies when an attribute filter is specified.
   */
  private void addAttributeFilterConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getAttributes().isEmpty()) {
      return;
    }

    sql.append(hlp.whereAnd());
    addPredicates(sql, sqlParams, params.getAttributes());
    sql.append(" ");
  }

  private void addTrackedEntityConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getTrackedEntity() != null) {
      sqlParams.addValue("trackedentityid", params.getTrackedEntity().getId());
      sql.append(hlp.whereAnd()).append(" te.trackedentityid= ").append(":trackedentityid ");
    }
  }

  private void addProgramConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.hasEnrolledInTrackerProgram()) {
      sqlParams.addValue("programid", params.getEnrolledInTrackerProgram().getId());
      sql.append(hlp.whereAnd()).append(" en.programid = :programid ");
    } else {
      sqlParams.addValue(
          "programid",
          params.getAccessibleTrackerPrograms().isEmpty()
              ? null
              : getIdentifiers(params.getAccessibleTrackerPrograms()));
      sql.append(hlp.whereAnd()).append(" p.programid in (").append(":programid").append(") ");
    }
  }

  private void addProgramStageConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.hasProgramStage()) {
      sqlParams.addValue("programstageid", params.getProgramStage().getId());
      sql.append(hlp.whereAnd()).append(" ev.programstageid = :programstageid ");
    } else {
      sqlParams.addValue(
          "programstageid",
          params.getAccessibleTrackerProgramStages().isEmpty()
              ? null
              : getIdentifiers(params.getAccessibleTrackerProgramStages()));
      sql.append(hlp.whereAnd()).append(" ev.programstageid in (:programstageid) ");
    }
  }

  private void addEnrollmentConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getEnrollmentStatus() != null) {
      sqlParams.addValue("program_status", params.getEnrollmentStatus().name());
      sql.append(hlp.whereAnd()).append(" en.status = ").append(":program_status ");
    }

    if (params.getEnrollmentEnrolledBefore() != null) {
      sqlParams.addValue(
          "enrollmentEnrolledBefore", params.getEnrollmentEnrolledBefore(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" (en.enrollmentdate <= :enrollmentEnrolledBefore ) ");
    }

    if (params.getEnrollmentEnrolledAfter() != null) {
      sqlParams.addValue(
          "enrollmentEnrolledAfter", params.getEnrollmentEnrolledAfter(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" (en.enrollmentdate >= :enrollmentEnrolledAfter ) ");
    }

    if (params.getEnrollmentOccurredBefore() != null) {
      sqlParams.addValue(
          "enrollmentOccurredBefore", params.getEnrollmentOccurredBefore(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" (en.occurreddate <= :enrollmentOccurredBefore ) ");
    }

    if (params.getEnrollmentOccurredAfter() != null) {
      sqlParams.addValue(
          "enrollmentOccurredAfter", params.getEnrollmentOccurredAfter(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" (en.occurreddate >= :enrollmentOccurredAfter ) ");
    }

    if (params.getScheduleAtStartDate() != null) {
      sqlParams.addValue("startScheduledDate", params.getScheduleAtStartDate(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd())
          .append(" (ev.scheduleddate is not null and ev.scheduleddate >= :startScheduledDate ) ");
    }

    if (params.getScheduleAtEndDate() != null) {
      sqlParams.addValue("endScheduledDate", params.getScheduleAtEndDate(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd())
          .append(" (ev.scheduleddate is not null and ev.scheduleddate <= :endScheduledDate ) ");
    }

    if (params.getFollowUp() != null) {
      sql.append(hlp.whereAnd())
          .append(" en.followup is ")
          .append(Boolean.TRUE.equals(params.getFollowUp()) ? "true" : "false")
          .append(" ");
    }
  }

  private void addLastUpdatedConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.hasUpdatedAtDuration()) {
      sqlParams.addValue(
          "lastUpdated",
          DateUtils.offSetDateTimeFrom(DateUtils.nowMinusDuration(params.getUpdatedAtDuration())),
          Types.TIMESTAMP_WITH_TIMEZONE);
      sql.append(hlp.whereAnd()).append(" ev.lastupdated >= ").append(":lastUpdated ");
    } else {
      if (params.hasUpdatedAtStartDate()) {
        sqlParams.addValue("lastUpdatedStart", params.getUpdatedAtStartDate(), Types.TIMESTAMP);
        sql.append(hlp.whereAnd()).append(" ev.lastupdated >= ").append(":lastUpdatedStart ");
      }

      if (params.hasUpdatedAtEndDate()) {
        sqlParams.addValue("lastUpdatedEnd", params.getUpdatedAtEndDate(), Types.TIMESTAMP);
        sql.append(hlp.whereAnd()).append(" ev.lastupdated <= ").append(":lastUpdatedEnd ");
      }
    }
  }

  private void addCategoryOptionComboConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getCategoryOptionCombo() != null) {
      sqlParams.addValue("attributeoptioncomboid", params.getCategoryOptionCombo().getId());
      sql.append(hlp.whereAnd())
          .append(" ev.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid ");
    }
  }

  private void addOrgUnitConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getOrgUnit() != null) {
      buildOrgUnitModeClause(
          sql,
          sqlParams,
          Set.of(params.getOrgUnit()),
          params.getOrgUnitMode(),
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
          "te",
          hlp::whereAnd);
    } else {
      buildOwnershipClause(sql, sqlParams, params.getOrgUnitMode(), "p", "ou", "te", hlp::whereAnd);
    }
  }

  private void addOccurredDateConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getOccurredStartDate() != null) {
      sqlParams.addValue("startOccurredDate", params.getOccurredStartDate(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" ev.occurreddate >= :startOccurredDate ");
    }

    if (params.getOccurredEndDate() != null) {
      sqlParams.addValue("endOccurredDate", params.getOccurredEndDate(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" ev.occurreddate <= :endOccurredDate ");
    }
  }

  private void addEventStatusConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getEventStatus() != null) {
      if (params.getEventStatus() == EventStatus.VISITED) {
        sqlParams.addValue("ev_status", EventStatus.ACTIVE.name());
        sql.append(hlp.whereAnd())
            .append(" ev.status = :ev_status")
            .append(" and ev.occurreddate is not null ");
      } else if (params.getEventStatus() == EventStatus.OVERDUE) {
        sqlParams.addValue("ev_status", EventStatus.SCHEDULE.name());
        sql.append(hlp.whereAnd())
            .append(" date(now()) > date(ev.scheduleddate) and ev.status = ")
            .append(":ev_status ");
      } else {
        sqlParams.addValue("ev_status", params.getEventStatus().name());
        sql.append(hlp.whereAnd()).append(" ev.status = ").append(":ev_status ");
      }
    }
  }

  private void addEventConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (!params.getEvents().isEmpty()) {
      sqlParams.addValue("ev_uid", UID.toValueSet(params.getEvents()));
      sql.append(hlp.whereAnd()).append(" (ev.uid in (").append(":ev_uid").append(")) ");
    }
  }

  private void addAssignedUserConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      Set<UID> assignedUsers = params.getAssignedUserQueryParam().getAssignedUsers();
      sql.append(hlp.whereAnd());
      if (assignedUsers.size() == 1) {
        sqlParams.addValue("au_uid", assignedUsers.iterator().next().getValue());
        sql.append(" ev.assigneduserid = (select userinfoid from userinfo where uid = :au_uid) ");
      } else {
        sqlParams.addValue("au_uid", UID.toValueSet(assignedUsers));
        sql.append(
            " ev.assigneduserid in (select userinfoid from userinfo where uid in (:au_uid)) ");
      }
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      sql.append(hlp.whereAnd()).append(" (ev.assigneduserid is null) ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      sql.append(hlp.whereAnd()).append(" (ev.assigneduserid is not null) ");
    }
  }

  private void addDeletedCondition(
      StringBuilder sql, TrackerEventQueryParams params, SqlHelper hlp) {
    if (!params.isIncludeDeleted()) {
      sql.append(hlp.whereAnd()).append(" ev.deleted is false ");
    }
  }

  private void addEnrollmentUidConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      TrackerEventQueryParams params,
      SqlHelper hlp) {
    if (!CollectionUtils.isEmpty(params.getEnrollments())) {
      sqlParams.addValue("enrollment_uid", UID.toValueSet(params.getEnrollments()));
      sql.append(hlp.whereAnd()).append(" (en.uid in (:enrollment_uid)) ");
    }
  }

  private String getLimitAndOffsetClause(final PageParams pageParams) {
    // get extra event to determine if there is a nextPage
    return " limit " + (pageParams.getPageSize() + 1) + " offset " + pageParams.getOffset() + " ";
  }

  private String getOrderQuery(TrackerEventQueryParams params) {
    List<SqlOrder> orderFields = new ArrayList<>();

    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  "Cannot order by '%s'. Supported are data elements, tracked entity attributes and"
                      + " fields '%s'.",
                  field, String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        orderFields.add(SqlOrder.of(ORDERABLE_FIELDS.get(field), order));
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        orderFields.add(SqlOrder.of(tea.getUid() + "_value", order));
      } else if (order.getField() instanceof DataElement de) {
        orderFields.add(SqlOrder.of(de.getUid(), order));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are data elements, tracked entity attributes and"
                    + " fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    return OrderJdbcClause.of(orderFields, DEFAULT_ORDER, PK_COLUMN);
  }

  private boolean isNotSuperUser(UserDetails user) {
    return user != null && !user.isSuper();
  }

  private Set<EventDataValue> convertEventDataValueJsonIntoSet(String jsonString) {
    try {
      Map<String, EventDataValue> data = eventDataValueJsonReader.readValue(jsonString);
      return JsonEventDataValueSetBinaryType.convertEventDataValuesMapIntoSet(data);
    } catch (IOException e) {
      log.error("Parsing EventDataValues json string failed, string value: '{}'", jsonString);
      throw new IllegalArgumentException(e);
    }
  }
}
