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
package org.hisp.dhis.tracker.export.singleevent;

import static java.util.Map.entry;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.tracker.export.FilterJdbcPredicate.addPredicates;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildAccessLevelClauseForSingleEvents;
import static org.hisp.dhis.tracker.export.OrgUnitQueryBuilder.buildOrgUnitModeClause;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
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
import org.hisp.dhis.tracker.export.OrgUnitQueryBuilder;
import org.hisp.dhis.tracker.export.UserInfoSnapshots;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.util.DateUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository("org.hisp.dhis.tracker.export.singleevent.EventStore")
@RequiredArgsConstructor
class JdbcSingleEventStore {
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
       from singleevent_notes evn\
       inner join note n\
       on evn.noteid = n.noteid\
       left join userinfo on n.lastupdatedby = userinfo.userinfoid\s""";

  private static final String DEFAULT_ORDER = "ev_created desc, ev_id desc";
  private static final String PK_COLUMN = "ev_id";

  /**
   * Events can be ordered by given fields which correspond to fields on {@link SingleEvent}. Maps
   * fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", "ev_uid"),
          // Order by program is here for backwards compatibility but program is mandatory in the
          // API
          entry("enrollment.program.uid", "p_uid"),
          entry("organisationUnit.uid", "orgunit_uid"),
          entry("occurredDate", "ev_occurreddate"),
          entry("status", "ev_status"),
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

  public List<SingleEvent> getEvents(SingleEventQueryParams queryParams) {
    return fetchEvents(queryParams, null);
  }

  public Page<SingleEvent> getEvents(SingleEventQueryParams queryParams, PageParams pageParams) {
    List<SingleEvent> events = fetchEvents(queryParams, pageParams);
    return new Page<>(events, pageParams, () -> getEventCount(queryParams));
  }

  private List<SingleEvent> fetchEvents(SingleEventQueryParams queryParams, PageParams pageParams) {
    Map<String, SingleEvent> eventsByUid;
    if (pageParams == null) {
      eventsByUid = new HashMap<>();
    } else {
      eventsByUid =
          new HashMap<>(
              pageParams.getPageSize() + 1); // get extra event to determine if there is a nextPage
    }
    List<SingleEvent> events = new ArrayList<>();

    final MapSqlParameterSource sqlParameters = new MapSqlParameterSource();
    String sql =
        buildSql(queryParams, pageParams, sqlParameters, CurrentUserUtil.getCurrentUserDetails());

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

            SingleEvent event;
            if (eventsByUid.containsKey(eventUid)) {
              event = eventsByUid.get(eventUid);
            } else {
              event = new SingleEvent();
              event.setUid(eventUid);
              eventsByUid.put(eventUid, event);
              dataElementUids.put(eventUid, new HashSet<>());

              event.setStatus(EventStatus.valueOf(resultSet.getString("ev_status")));

              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(resultSet.getString("orgunit_uid"));
              orgUnit.setCode(resultSet.getString("orgunit_code"));
              orgUnit.setName(resultSet.getString("orgunit_name"));
              orgUnit.setAttributeValues(
                  AttributeValues.of(resultSet.getString("orgunit_attributevalues")));
              event.setOrganisationUnit(orgUnit);

              if (queryParams.getProgram() != null) {
                Program program = new Program();
                Program queryProgram = queryParams.getProgram();
                program.setUid(queryProgram.getUid());
                program.setCode(queryProgram.getCode());
                program.setName(queryProgram.getName());
                program.setAttributeValues(queryProgram.getAttributeValues());
                program.setProgramType(ProgramType.WITHOUT_REGISTRATION);

                ProgramStage ps = new ProgramStage();
                ProgramStage queryProgramStage = queryParams.getProgramStage();
                ps.setUid(queryProgramStage.getUid());
                ps.setCode(queryProgramStage.getCode());
                ps.setName(queryProgramStage.getName());
                ps.setAttributeValues(queryProgramStage.getAttributeValues());
                ps.setProgram(program);

                event.setProgramStage(ps);
              } else {
                ProgramType programType = ProgramType.fromValue(resultSet.getString("p_type"));
                Program program = new Program();
                program.setUid(resultSet.getString("p_uid"));
                program.setCode(resultSet.getString("p_code"));
                program.setName(resultSet.getString("p_name"));
                program.setAttributeValues(
                    AttributeValues.of(resultSet.getString("p_attributevalues")));
                program.setProgramType(programType);

                ProgramStage ps = new ProgramStage();
                ps.setUid(resultSet.getString("ps_uid"));
                ps.setCode(resultSet.getString("ps_code"));
                ps.setName(resultSet.getString("ps_name"));
                ps.setAttributeValues(
                    AttributeValues.of(resultSet.getString("ps_attributevalues")));
                ps.setProgram(program);

                event.setProgramStage(ps);
              }

              event.setDeleted(resultSet.getBoolean("ev_deleted"));

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

  public void updateEventsSyncTimestamp(List<String> eventUids, Date lastSynchronized) {
    if (eventUids.isEmpty()) {
      return;
    }

    String sql =
        """
                UPDATE event SET lastsynchronized = :lastSynchronized WHERE uid IN (:uids)
                """;

    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("lastSynchronized", new java.sql.Timestamp(lastSynchronized.getTime()))
            .addValue("uids", eventUids);

    jdbcTemplate.update(sql, parameters);
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
    eventDataValue.setCreatedByUserInfo(
        UserInfoSnapshots.from(dataValueJson.getObject("createdByUserInfo")));

    eventDataValue.setLastUpdated(
        DateUtils.parseDate(dataValueJson.getString("lastUpdated").string("")));
    eventDataValue.setLastUpdatedByUserInfo(
        UserInfoSnapshots.from(dataValueJson.getObject("lastUpdatedByUserInfo")));

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

  public long getEventCount(SingleEventQueryParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    MapSqlParameterSource sqlParams = new MapSqlParameterSource();
    String sql = getCountQuery(params, sqlParams, currentUser);
    Long count = jdbcTemplate.queryForObject(sql, sqlParams, Long.class);
    return count != null ? count : 0L;
  }

  /**
   * Builds the count query with only the joins needed for filtering:
   *
   * <ul>
   *   <li>{@code organisationunit} - always needed (org unit mode and access level filtering)
   *   <li>{@code categoryoptioncombo} - always needed (COC access control)
   *   <li>{@code programstage} + {@code program} - only when no specific program is given (needed
   *       for {@code p.accesslevel} in the access level clause). When a program is known, its
   *       conditions use {@code ev.programstageid} directly and both joins are skipped.
   * </ul>
   *
   * <p>Always skips: {@code userinfo} (au) which is only used in SELECT.
   */
  private String getCountQuery(
      SingleEventQueryParams params, MapSqlParameterSource sqlParams, UserDetails user) {
    boolean needsProgramJoin = params.getProgram() == null;

    StringBuilder sql = new StringBuilder();
    sql.append("select count(*) from singleevent ev ");
    if (needsProgramJoin) {
      addJoinOnProgramStage(sql);
      addJoinOnProgram(sql);
    }
    addJoinOnOrgUnit(sql);
    addJoinOnCategoryOptionCombo(sql, user);
    addCountWhereConditions(sql, sqlParams, params, needsProgramJoin);

    return sql.toString();
  }

  private void addCountWhereConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      boolean hasProgramJoin) {
    SqlHelper hlp = new SqlHelper(true);
    addDataElementConditions(sql, sqlParams, params);
    if (hasProgramJoin) {
      addProgramConditions(sql, sqlParams, params, hlp);
    } else {
      addCountProgramConditions(sql, sqlParams, params, hlp);
    }
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addCategoryOptionComboConditions(sql, sqlParams, params, hlp);
    addOrgUnitConditions(sql, sqlParams, params, hlp);
    addOccurredDateConditions(sql, sqlParams, params, hlp);
    addSyncConditions(sql, sqlParams, params, hlp);
    addEventStatusConditions(sql, sqlParams, params, hlp);
    addEventConditions(sql, sqlParams, params, hlp);
    addAssignedUserConditions(sql, sqlParams, params, hlp);
    addDeletedCondition(sql, params, hlp);
  }

  /**
   * Program conditions for the count query when the program is known. Uses {@code
   * ev.programstageid} directly instead of joining the program and programstage tables.
   */
  private void addCountProgramConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      SqlHelper hlp) {
    if (params.getProgram() != null) {
      sqlParams.addValue("programstageid", params.getProgramStage().getId());
      sql.append(hlp.whereAnd()).append(" ev.programstageid = :programstageid ");
    }
  }

  /**
   * Query is based on three sub queries on event, data value and note, which are joined using event
   * id. The purpose of the separate queries is to be able to page properly on events.
   */
  private String buildSql(
      SingleEventQueryParams queryParams,
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
   * from singleevent ev
   *   inner join programstage ps on ...    -- conditional
   *   inner join program p on ...          -- conditional
   *   inner join organisationunit ou on ...
   *   left join userinfo au on ...
   *   inner join (...) coc_agg on ...
   * where ...
   * </pre>
   */
  private String getQuery(
      SingleEventQueryParams params, MapSqlParameterSource sqlParams, UserDetails user) {
    StringBuilder sql = new StringBuilder();
    addSelect(sql, params, sqlParams);
    sql.append(" from singleevent ev ");
    if (params.getProgram() == null) {
      addJoinOnProgramStage(sql);
      addJoinOnProgram(sql);
    }
    addJoinOnOrgUnit(sql);
    addLeftJoinOnAssignedUser(sql);
    addJoinOnCategoryOptionCombo(sql, user);
    addWhereConditions(sql, sqlParams, params);
    return sql.toString();
  }

  private void addSelect(
      StringBuilder sql, SingleEventQueryParams params, MapSqlParameterSource sqlParams) {
    if (params.getProgram() != null) {
      // program and programstage columns are not needed; the RowMapper
      // reuses the already loaded Program and ProgramStage entities instead
      sql.append(
          """
            select ev.uid as ev_uid,
            ou.uid as orgunit_uid, ou.code as orgunit_code, ou.name as orgunit_name,
            ou.attributevalues as orgunit_attributevalues,
            ev.eventid as ev_id, ev.status as ev_status,
            ev.occurreddate as ev_occurreddate,\s""");
    } else {
      sql.append(
          """
            select ev.uid as ev_uid,
            ou.uid as orgunit_uid, ou.code as orgunit_code, ou.name as orgunit_name,
            ou.attributevalues as orgunit_attributevalues,
            p.uid as p_uid, p.code as p_code, p.name as p_name,
            p.attributevalues as p_attributevalues, p.type as p_type,
            ps.uid as ps_uid, ps.code as ps_code, ps.name as ps_name,
            ps.attributevalues as ps_attributevalues,
            ev.eventid as ev_id, ev.status as ev_status,
            ev.occurreddate as ev_occurreddate,\s""");
    }
    sql.append(getEventDataValuesProjectionForSelectClause(params, sqlParams));
    sql.append(
        """
            \s as ev_eventdatavalues,
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
            au.username as user_assigned_username, \s""");
    addOrderFieldsToSelect(sql, params.getOrder());
    sql.append(
        """
            coc_agg.uid as coc_uid, coc_agg.code as coc_code, coc_agg.name as coc_name,
                        coc_agg.attributevalues as coc_attributevalues,
                        coc_agg.co_values as co_values, coc_agg.co_count as option_size""");
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

  private void addJoinOnProgramStage(StringBuilder sql) {
    sql.append(" inner join programstage ps on ps.programstageid = ev.programstageid ");
  }

  private void addJoinOnProgram(StringBuilder sql) {
    sql.append(" inner join program p on p.programid = ps.programid ");
  }

  private void addJoinOnOrgUnit(StringBuilder sql) {
    sql.append(" inner join organisationunit ou on ou.organisationunitid = ev.organisationunitid ");
  }

  private void addLeftJoinOnAssignedUser(StringBuilder sql) {
    sql.append(" left join userinfo au on au.userinfoid = ev.assigneduserid ");
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
      StringBuilder sql, MapSqlParameterSource sqlParams, SingleEventQueryParams params) {
    SqlHelper hlp = new SqlHelper(true);
    addDataElementConditions(sql, sqlParams, params);
    addProgramConditions(sql, sqlParams, params, hlp);
    addLastUpdatedConditions(sql, sqlParams, params, hlp);
    addCategoryOptionComboConditions(sql, sqlParams, params, hlp);
    addOrgUnitConditions(sql, sqlParams, params, hlp);
    addOccurredDateConditions(sql, sqlParams, params, hlp);
    addSyncConditions(sql, sqlParams, params, hlp);
    addEventStatusConditions(sql, sqlParams, params, hlp);
    addEventConditions(sql, sqlParams, params, hlp);
    addAssignedUserConditions(sql, sqlParams, params, hlp);
    addDeletedCondition(sql, params, hlp);
  }

  private void addDataElementConditions(
      StringBuilder sql, MapSqlParameterSource sqlParams, SingleEventQueryParams params) {
    if (!params.getDataElements().isEmpty()) {
      sql.append(" and ");
      addPredicates(sql, sqlParams, params.getDataElements());
      sql.append(" ");
    }
  }

  private void addProgramConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      SqlHelper hlp) {
    if (params.getProgram() != null) {
      sqlParams.addValue("programstageid", params.getProgramStage().getId());
      sql.append(hlp.whereAnd()).append(" ev.programstageid = :programstageid ");
    }
  }

  private void addLastUpdatedConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
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
      SingleEventQueryParams params,
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
      SingleEventQueryParams params,
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

    if (params.getProgram() == null) {
      buildAccessLevelClauseForSingleEvents(
          sql, sqlParams, params.getOrgUnitMode(), "p", "ou", hlp::whereAnd);
    } else {
      OrgUnitQueryBuilder.buildAccessLevelClauseForSingleEvents(
          sql, sqlParams, params.getProgram(), params.getQuerySearchScope(), "ou", hlp::whereAnd);
    }
  }

  private void addOccurredDateConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
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

  private void addSyncConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      SqlHelper hlp) {
    if (params.isSynchronizationQuery()) {
      sql.append(hlp.whereAnd()).append(" ev.lastupdated > ev.lastsynchronized ");
    }

    if (params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0) {
      sqlParams.addValue("skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP);
      sql.append(hlp.whereAnd()).append(" ev.lastupdated >= ").append(":skipChangedBefore ");
    }
  }

  private void addEventStatusConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      SqlHelper hlp) {
    if (params.getEventStatus() != null) {
      if (params.getEventStatus() == EventStatus.VISITED) {
        sqlParams.addValue("ev_status", EventStatus.ACTIVE.name());
        sql.append(hlp.whereAnd())
            .append(" ev.status = :ev_status")
            .append(" and ev.occurreddate is not null ");
      } else {
        sqlParams.addValue("ev_status", params.getEventStatus().name());
        sql.append(hlp.whereAnd()).append(" ev.status = ").append(":ev_status ");
      }
    }
  }

  private void addEventConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
      SqlHelper hlp) {
    if (!params.getEvents().isEmpty()) {
      sqlParams.addValue("ev_uid", UID.toValueSet(params.getEvents()));
      sql.append(hlp.whereAnd()).append(" (ev.uid in (").append(":ev_uid").append(")) ");
    }
  }

  private void addAssignedUserConditions(
      StringBuilder sql,
      MapSqlParameterSource sqlParams,
      SingleEventQueryParams params,
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
      StringBuilder sql, SingleEventQueryParams params, SqlHelper hlp) {
    if (!params.isIncludeDeleted()) {
      sql.append(hlp.whereAnd()).append(" ev.deleted is false ");
    }
  }

  private String getLimitAndOffsetClause(final PageParams pageParams) {
    // get extra event to determine if there is a nextPage
    return " limit " + (pageParams.getPageSize() + 1) + " offset " + pageParams.getOffset() + " ";
  }

  private String getOrderQuery(SingleEventQueryParams params) {
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

  /**
   * Returns either the original event data values or a filtered version, depending on the "skip
   * synchronization" configuration of the ProgramStageDataElements. This logic applies only during
   * single event synchronization.
   */
  /**
   * Returns the event data values projection for the SELECT clause. During synchronization, data
   * elements marked as "skip sync" are filtered out. Since the program stage is always known for
   * sync queries, the skip-sync data elements are looked up directly without a CASE expression.
   */
  private String getEventDataValuesProjectionForSelectClause(
      SingleEventQueryParams params, MapSqlParameterSource sqlParameters) {
    if (!params.isSynchronizationQuery()
        || params.getSkipSyncDataElementsByProgramStage() == null
        || params.getSkipSyncDataElementsByProgramStage().isEmpty()) {
      return "ev.eventdatavalues";
    }

    String programStageUid = params.getProgramStage().getUid();
    Set<String> dataElementUids =
        params.getSkipSyncDataElementsByProgramStage().get(programStageUid);

    if (dataElementUids == null || dataElementUids.isEmpty()) {
      return "ev.eventdatavalues";
    }

    sqlParameters.addValue("skipSyncDataElements", dataElementUids);
    return """
        (SELECT jsonb_object_agg(key, value)
        FROM jsonb_each(ev.eventdatavalues)
        WHERE key NOT IN (:skipSyncDataElements))""";
  }
}
