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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.Page;
import org.hisp.dhis.tracker.PageParams;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.export.EventUtils;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
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

  private static final String EVENT_STATUS_EQ = " ev.status = ";

  private static final String EVENT_LASTUPDATED_GT = " ev.lastupdated >= ";

  private static final String SPACE = " ";

  private static final String AND = " AND ";

  private static final String COLUMN_EVENT_ID = "ev_id";
  private static final String COLUMN_EVENT_UID = "ev_uid";
  private static final String COLUMN_PROGRAM_UID = "p_uid";
  private static final String COLUMN_PROGRAM_CODE = "p_code";
  private static final String COLUMN_PROGRAM_NAME = "p_name";
  private static final String COLUMN_PROGRAM_ATTRIBUTE_VALUES = "p_attributevalues";
  private static final String COLUMN_PROGRAM_STAGE_UID = "ps_uid";
  private static final String COLUMN_PROGRAM_STAGE_CODE = "ps_code";
  private static final String COLUMN_PROGRAM_STAGE_NAME = "ps_name";
  private static final String COLUMN_PROGRAM_STAGE_ATTRIBUTE_VALUES = "ps_attributevalues";
  private static final String COLUMN_ENROLLMENT_UID = "en_uid";
  private static final String COLUMN_ORG_UNIT_UID = "orgunit_uid";
  private static final String COLUMN_ORG_UNIT_CODE = "orgunit_code";
  private static final String COLUMN_ORG_UNIT_NAME = "orgunit_name";
  private static final String COLUMN_ORG_UNIT_ATTRIBUTE_VALUES = "orgunit_attributevalues";
  private static final String COLUMN_EVENT_OCCURRED_DATE = "ev_occurreddate";
  private static final String COLUMN_EVENT_STATUS = "ev_status";
  private static final String COLUMN_EVENT_DATAVALUES = "ev_eventdatavalues";
  private static final String COLUMN_EVENT_STORED_BY = "ev_storedby";
  private static final String COLUMN_EVENT_LAST_UPDATED_BY = "ev_lastupdatedbyuserinfo";
  private static final String COLUMN_EVENT_CREATED_BY = "ev_createdbyuserinfo";
  private static final String COLUMN_EVENT_CREATED = "ev_created";
  private static final String COLUMN_EVENT_CREATED_AT_CLIENT = "ev_createdatclient";
  private static final String COLUMN_EVENT_LAST_UPDATED = "ev_lastupdated";
  private static final String COLUMN_EVENT_LAST_UPDATED_AT_CLIENT = "ev_lastupdatedatclient";
  private static final String COLUMN_EVENT_COMPLETED_BY = "ev_completedby";
  private static final String COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_UID = "coc_uid";
  private static final String COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_NAME = "coc_name";
  private static final String COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_CODE = "coc_code";
  private static final String COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_ATTRIBUTE_VALUES =
      "coc_attributevalues";
  private static final String COLUMN_EVENT_COMPLETED_DATE = "ev_completeddate";
  private static final String COLUMN_EVENT_DELETED = "ev_deleted";
  private static final String COLUMN_EVENT_ASSIGNED_USER_USERNAME = "user_assigned_username";
  private static final String COLUMN_EVENT_ASSIGNED_USER_DISPLAY_NAME = "user_assigned_name";
  private static final String COLUMN_USER_UID = "u_uid";
  private static final String DEFAULT_ORDER = COLUMN_EVENT_ID + " desc";
  private static final String COLUMN_ORG_UNIT_PATH = "ou_path";
  private static final String USER_SCOPE_ORG_UNIT_PATH_LIKE_MATCH_QUERY =
      " ou.path like CONCAT(orgunit.path, '%') ";
  private static final String CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY =
      " ou.path like CONCAT(:" + COLUMN_ORG_UNIT_PATH + ", '%' ) ";

  /**
   * Events can be ordered by given fields which correspond to fields on {@link SingleEvent}. Maps
   * fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", COLUMN_EVENT_UID),
          entry("enrollment.program.uid", COLUMN_PROGRAM_UID),
          entry("organisationUnit.uid", COLUMN_ORG_UNIT_UID),
          entry("occurredDate", COLUMN_EVENT_OCCURRED_DATE),
          entry("status", COLUMN_EVENT_STATUS),
          entry("storedBy", COLUMN_EVENT_STORED_BY),
          entry("lastUpdatedBy", COLUMN_EVENT_LAST_UPDATED_BY),
          entry("createdBy", COLUMN_EVENT_CREATED_BY),
          entry("created", COLUMN_EVENT_CREATED),
          entry("createdAtClient", COLUMN_EVENT_CREATED_AT_CLIENT),
          entry("lastUpdated", COLUMN_EVENT_LAST_UPDATED),
          entry("lastUpdatedAtClient", COLUMN_EVENT_LAST_UPDATED_AT_CLIENT),
          entry("completedBy", COLUMN_EVENT_COMPLETED_BY),
          entry("attributeOptionCombo.uid", COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_UID),
          entry("completedDate", COLUMN_EVENT_COMPLETED_DATE),
          entry("deleted", COLUMN_EVENT_DELETED),
          entry("assignedUser", COLUMN_EVENT_ASSIGNED_USER_USERNAME),
          entry("assignedUser.displayName", COLUMN_EVENT_ASSIGNED_USER_DISPLAY_NAME));

  // Cannot use DefaultRenderService mapper. Does not work properly -
  // DHIS2-6102
  private static final ObjectReader eventDataValueJsonReader =
      JsonBinaryType.MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("dataValueJsonMapper")
  private final ObjectMapper jsonMapper;

  private final UserService userService;

  private final IdentifiableObjectManager manager;

  public List<SingleEvent> getEvents(SingleEventQueryParams queryParams) {
    return fetchEvents(queryParams, null);
  }

  public Page<SingleEvent> getEvents(SingleEventQueryParams queryParams, PageParams pageParams) {
    List<SingleEvent> events = fetchEvents(queryParams, pageParams);
    return new Page<>(events, pageParams, () -> getEventCount(queryParams));
  }

  private List<SingleEvent> fetchEvents(SingleEventQueryParams queryParams, PageParams pageParams) {
    setAccessiblePrograms(CurrentUserUtil.getCurrentUserDetails(), queryParams);

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
            if (resultSet.getString(COLUMN_EVENT_UID) == null) {
              continue;
            }

            String eventUid = resultSet.getString(COLUMN_EVENT_UID);

            SingleEvent event;
            if (eventsByUid.containsKey(eventUid)) {
              event = eventsByUid.get(eventUid);
            } else {
              event = new SingleEvent();
              event.setUid(eventUid);
              eventsByUid.put(eventUid, event);
              dataElementUids.put(eventUid, new HashSet<>());

              event.setStatus(EventStatus.valueOf(resultSet.getString(COLUMN_EVENT_STATUS)));

              ProgramType programType = ProgramType.fromValue(resultSet.getString("p_type"));
              Program program = new Program();
              program.setUid(resultSet.getString(COLUMN_PROGRAM_UID));
              program.setCode(resultSet.getString(COLUMN_PROGRAM_CODE));
              program.setName(resultSet.getString(COLUMN_PROGRAM_NAME));
              program.setAttributeValues(
                  AttributeValues.of(resultSet.getString(COLUMN_PROGRAM_ATTRIBUTE_VALUES)));
              program.setProgramType(programType);

              Enrollment enrollment = new Enrollment();
              enrollment.setUid(resultSet.getString(COLUMN_ENROLLMENT_UID));
              enrollment.setProgram(program);

              OrganisationUnit orgUnit = new OrganisationUnit();
              orgUnit.setUid(resultSet.getString(COLUMN_ORG_UNIT_UID));
              orgUnit.setCode(resultSet.getString(COLUMN_ORG_UNIT_CODE));
              orgUnit.setName(resultSet.getString(COLUMN_ORG_UNIT_NAME));
              orgUnit.setAttributeValues(
                  AttributeValues.of(resultSet.getString(COLUMN_ORG_UNIT_ATTRIBUTE_VALUES)));
              event.setOrganisationUnit(orgUnit);

              ProgramStage ps = new ProgramStage();
              ps.setUid(resultSet.getString(COLUMN_PROGRAM_STAGE_UID));
              ps.setCode(resultSet.getString(COLUMN_PROGRAM_STAGE_CODE));
              ps.setName(resultSet.getString(COLUMN_PROGRAM_STAGE_NAME));
              ps.setAttributeValues(
                  AttributeValues.of(resultSet.getString(COLUMN_PROGRAM_STAGE_ATTRIBUTE_VALUES)));
              event.setDeleted(resultSet.getBoolean(COLUMN_EVENT_DELETED));

              event.setEnrollment(enrollment);
              event.setProgramStage(ps);

              CategoryOptionCombo coc = new CategoryOptionCombo();
              coc.setUid(resultSet.getString(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_UID));
              coc.setCode(resultSet.getString(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_CODE));
              coc.setName(resultSet.getString(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_NAME));
              coc.setAttributeValues(
                  AttributeValues.of(
                      resultSet.getString(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_ATTRIBUTE_VALUES)));

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

              event.setStoredBy(resultSet.getString(COLUMN_EVENT_STORED_BY));
              event.setOccurredDate(resultSet.getTimestamp(COLUMN_EVENT_OCCURRED_DATE));
              event.setCreated(resultSet.getTimestamp(COLUMN_EVENT_CREATED));
              event.setCreatedAtClient(resultSet.getTimestamp(COLUMN_EVENT_CREATED_AT_CLIENT));
              event.setCreatedByUserInfo(
                  EventUtils.jsonToUserInfo(
                      resultSet.getString(COLUMN_EVENT_CREATED_BY), jsonMapper));
              event.setLastUpdated(resultSet.getTimestamp(COLUMN_EVENT_LAST_UPDATED));
              event.setLastUpdatedAtClient(
                  resultSet.getTimestamp(COLUMN_EVENT_LAST_UPDATED_AT_CLIENT));
              event.setLastUpdatedByUserInfo(
                  EventUtils.jsonToUserInfo(
                      resultSet.getString(COLUMN_EVENT_LAST_UPDATED_BY), jsonMapper));

              event.setCompletedBy(resultSet.getString(COLUMN_EVENT_COMPLETED_BY));
              event.setCompletedDate(resultSet.getTimestamp(COLUMN_EVENT_COMPLETED_DATE));

              if (resultSet.getObject("ev_geometry") != null) {
                try {
                  Geometry geom = new WKTReader().read(resultSet.getString("ev_geometry"));

                  event.setGeometry(geom);
                } catch (ParseException e) {
                  log.error("Unable to read geometry for event: '{}'", event.getUid(), e);
                }
              }

              if (resultSet.getObject("user_assigned") != null) {
                User eventUser = new User();
                eventUser.setUid(resultSet.getString("user_assigned"));
                eventUser.setUsername(resultSet.getString(COLUMN_EVENT_ASSIGNED_USER_USERNAME));
                eventUser.setName(resultSet.getString(COLUMN_EVENT_ASSIGNED_USER_DISPLAY_NAME));
                eventUser.setFirstName(resultSet.getString("user_assigned_first_name"));
                eventUser.setSurname(resultSet.getString("user_assigned_surname"));
                event.setAssignedUser(eventUser);
              }

              if (TrackerIdScheme.UID == dataElementIdScheme.getIdScheme()
                  && !StringUtils.isEmpty(resultSet.getString(COLUMN_EVENT_DATAVALUES))) {
                event
                    .getEventDataValues()
                    .addAll(
                        convertEventDataValueJsonIntoSet(
                            resultSet.getString(COLUMN_EVENT_DATAVALUES)));
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
          EventUtils.jsonToUserInfo(
              dataValueJson.getObject("createdByUserInfo").toJson(), jsonMapper));
    }

    eventDataValue.setLastUpdated(
        DateUtils.parseDate(dataValueJson.getString("lastUpdated").string("")));
    if (dataValueJson.has("lastUpdatedByUserInfo")) {
      eventDataValue.setLastUpdatedByUserInfo(
          EventUtils.jsonToUserInfo(
              dataValueJson.getObject("lastUpdatedByUserInfo").toJson(), jsonMapper));
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

  private long getEventCount(SingleEventQueryParams params) {
    UserDetails currentUser = CurrentUserUtil.getCurrentUserDetails();
    setAccessiblePrograms(currentUser, params);

    String sql;

    MapSqlParameterSource sqlParameters = new MapSqlParameterSource();

    sql = getEventSelectQuery(params, sqlParameters, currentUser);

    sql = sql.replaceFirst("select .*? from", "select count(*) as ev_count from");

    sql = sql.replaceFirst("order .*? (desc|asc)", "");

    sql = sql.replaceFirst("limit \\d+ offset \\d+", "");

    RowCountHandler rowCountHandler = new RowCountHandler();
    jdbcTemplate.query(sql, sqlParameters, rowCountHandler);
    return rowCountHandler.getCount();
  }

  private static class RowCountHandler implements RowCallbackHandler {
    private long count;

    @Override
    public void processRow(ResultSet rs) throws SQLException {
      count = rs.getLong("ev_count");
    }

    public long getCount() {
      return count;
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

    sqlBuilder.append(getEventSelectQuery(queryParams, mapSqlParameterSource, user));

    sqlBuilder.append(getOrderQuery(queryParams));

    if (pageParams != null) {
      sqlBuilder.append(getLimitAndOffsetClause(pageParams));
    }

    sqlBuilder.append(") as event left join (");
    sqlBuilder.append(EVENT_NOTE_QUERY);
    sqlBuilder.append(") as cm on event.");
    sqlBuilder.append(COLUMN_EVENT_ID);
    sqlBuilder.append("=cm.evn_id ");

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

  private String getEventSelectQuery(
      SingleEventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource,
      UserDetails user) {
    SqlHelper hlp = new SqlHelper();

    StringBuilder selectBuilder =
        new StringBuilder()
            .append("select ev.uid as ")
            .append(COLUMN_EVENT_UID)
            .append(", ou.uid as ")
            .append(COLUMN_ORG_UNIT_UID)
            .append(", ou.code as ")
            .append(COLUMN_ORG_UNIT_CODE)
            .append(", ou.name as ")
            .append(COLUMN_ORG_UNIT_NAME)
            .append(", ou.attributevalues as ")
            .append(COLUMN_ORG_UNIT_ATTRIBUTE_VALUES)
            .append(", p.uid as ")
            .append(COLUMN_PROGRAM_UID)
            .append(", p.code as ")
            .append(COLUMN_PROGRAM_CODE)
            .append(", p.name as ")
            .append(COLUMN_PROGRAM_NAME)
            .append(", p.attributevalues as ")
            .append(COLUMN_PROGRAM_ATTRIBUTE_VALUES)
            .append(", ps.uid as ")
            .append(COLUMN_PROGRAM_STAGE_UID)
            .append(", ps.code as ")
            .append(COLUMN_PROGRAM_STAGE_CODE)
            .append(", ps.name as ")
            .append(COLUMN_PROGRAM_STAGE_NAME)
            .append(", ps.attributevalues as ")
            .append(COLUMN_PROGRAM_STAGE_ATTRIBUTE_VALUES)
            .append(", ev.eventid as ")
            .append(COLUMN_EVENT_ID)
            .append(", ev.status as ")
            .append(COLUMN_EVENT_STATUS)
            .append(", ev.occurreddate as ")
            .append(COLUMN_EVENT_OCCURRED_DATE)
            .append(", ev.eventdatavalues as ")
            .append(COLUMN_EVENT_DATAVALUES)
            .append(", ev.completedby as ")
            .append(COLUMN_EVENT_COMPLETED_BY)
            .append(", ev.storedby as ")
            .append(COLUMN_EVENT_STORED_BY)
            .append(", ev.created as ")
            .append(COLUMN_EVENT_CREATED)
            .append(", ev.createdatclient as ")
            .append(COLUMN_EVENT_CREATED_AT_CLIENT)
            .append(", ev.createdbyuserinfo as ")
            .append(COLUMN_EVENT_CREATED_BY)
            .append(", ev.lastupdated as ")
            .append(COLUMN_EVENT_LAST_UPDATED)
            .append(", ev.lastupdatedatclient as ")
            .append(COLUMN_EVENT_LAST_UPDATED_AT_CLIENT)
            .append(", ev.lastupdatedbyuserinfo as ")
            .append(COLUMN_EVENT_LAST_UPDATED_BY)
            .append(", ev.completeddate as ")
            .append(COLUMN_EVENT_COMPLETED_DATE)
            .append(", ev.deleted as ")
            .append(COLUMN_EVENT_DELETED)
            .append(
                ", ST_AsText( ev.geometry ) as ev_geometry, au.uid as user_assigned, (au.firstName"
                    + " || ' ' || au.surName) as ")
            .append(COLUMN_EVENT_ASSIGNED_USER_DISPLAY_NAME)
            .append(",")
            .append(
                "au.firstName as user_assigned_first_name, au.surName as user_assigned_surname, ")
            .append("au.username as ")
            .append(COLUMN_EVENT_ASSIGNED_USER_USERNAME)
            .append(", coc_agg.uid as ")
            .append(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_UID)
            .append(", coc_agg.code as ")
            .append(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_CODE)
            .append(", coc_agg.name as ")
            .append(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_NAME)
            .append(", coc_agg.attributevalues as ")
            .append(COLUMN_EVENT_ATTRIBUTE_OPTION_COMBO_ATTRIBUTE_VALUES)
            .append(", coc_agg.co_values AS co_values, coc_agg.co_count AS option_size, ")
            .append(getOrderFieldsForSelectClause(params.getOrder()));

    return selectBuilder
        .append("en.uid as " + COLUMN_ENROLLMENT_UID + ", ")
        .append("p.type as p_type ")
        .append(getFromWhereClause(params, mapSqlParameterSource, user, hlp))
        .toString();
  }

  private String getOrderFieldsForSelectClause(List<Order> orders) {
    StringBuilder selectBuilder = new StringBuilder();

    for (Order order : orders) {
      if (order.getField() instanceof TrackedEntityAttribute tea) {
        selectBuilder
            .append(quote(tea.getUid()))
            .append(".value AS ")
            .append(tea.getUid())
            .append("_value, ");
      } else if (order.getField() instanceof DataElement de) {
        final String dataValueValueSql = "ev.eventdatavalues #>> '{" + de.getUid() + ", value}'";
        selectBuilder
            .append(
                de.getValueType().isNumeric()
                    ? SqlUtils.castToNumeric(dataValueValueSql)
                    : lower(dataValueValueSql))
            .append(" as ")
            .append(de.getUid())
            .append(", ");
      }
    }

    return selectBuilder.toString();
  }

  private StringBuilder getFromWhereClause(
      SingleEventQueryParams params,
      MapSqlParameterSource sqlParameters,
      UserDetails user,
      SqlHelper hlp) {
    StringBuilder fromBuilder =
        new StringBuilder(" from event ev ")
            .append("inner join enrollment en on en.enrollmentid=ev.enrollmentid ")
            .append("inner join program p on p.programid=en.programid ")
            .append("inner join programstage ps on ps.programstageid=ev.programstageid ");

    fromBuilder
        .append(
            "left join trackedentityprogramowner po on (en.trackedentityid=po.trackedentityid and en.programid=po.programid) ")
        .append(
            "inner join organisationunit evou on (coalesce(po.organisationunitid,"
                + " ev.organisationunitid)=evou.organisationunitid) ")
        .append("inner join organisationunit ou on (ev.organisationunitid=ou.organisationunitid) ");

    fromBuilder
        .append("left join trackedentity te on te.trackedentityid=en.trackedentityid ")
        .append("left join userinfo au on (ev.assigneduserid=au.userinfoid) ");

    fromBuilder.append(getCategoryOptionComboQuery(user));

    if (!params.getDataElements().isEmpty()) {
      fromBuilder.append(AND);
      addPredicates(fromBuilder, sqlParameters, params.getDataElements());
    }
    fromBuilder.append(SPACE);

    if (params.getProgram() != null) {
      sqlParameters.addValue("programid", params.getProgram().getId());

      fromBuilder.append(hlp.whereAnd()).append(" p.programid = ").append(":programid").append(" ");
    }

    fromBuilder.append(addLastUpdatedFilters(params, sqlParameters, hlp));

    if (params.getCategoryOptionCombo() != null) {
      sqlParameters.addValue("attributeoptioncomboid", params.getCategoryOptionCombo().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" ev.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid")
          .append(" ");
    }

    String orgUnitSql = getOrgUnitSql(params, user, sqlParameters);

    if (!Strings.isNullOrEmpty(orgUnitSql)) {
      fromBuilder.append(hlp.whereAnd()).append(orgUnitSql);
    }

    if (params.getOccurredStartDate() != null) {
      sqlParameters.addValue("startOccurredDate", params.getOccurredStartDate(), Types.TIMESTAMP);

      fromBuilder.append(hlp.whereAnd()).append(" ev.occurreddate >= :startOccurredDate ");
    }

    if (params.getOccurredEndDate() != null) {
      sqlParameters.addValue("endOccurredDate", params.getOccurredEndDate(), Types.TIMESTAMP);

      fromBuilder.append(hlp.whereAnd()).append(" ev.occurreddate <= :endOccurredDate ");
    }

    fromBuilder.append(hlp.whereAnd()).append(" p.type = 'WITHOUT_REGISTRATION' ");

    fromBuilder.append(eventStatusSql(params, sqlParameters, hlp));

    if (params.getEvents() != null
        && !params.getEvents().isEmpty()
        && !params.hasDataElementFilter()) {
      sqlParameters.addValue(COLUMN_EVENT_UID, UID.toValueSet(params.getEvents()));
      fromBuilder.append(hlp.whereAnd()).append(" (ev.uid in (").append(":ev_uid").append(")) ");
    }

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      sqlParameters.addValue(
          "au_uid", UID.toValueSet(params.getAssignedUserQueryParam().getAssignedUsers()));

      fromBuilder.append(hlp.whereAnd()).append(" (au.uid in (").append(":au_uid").append(")) ");
    }

    if (AssignedUserSelectionMode.NONE == params.getAssignedUserQueryParam().getMode()) {
      fromBuilder.append(hlp.whereAnd()).append(" (au.uid is null) ");
    }

    if (AssignedUserSelectionMode.ANY == params.getAssignedUserQueryParam().getMode()) {
      fromBuilder.append(hlp.whereAnd()).append(" (au.uid is not null) ");
    }

    if (!params.isIncludeDeleted()) {
      fromBuilder.append(hlp.whereAnd()).append(" ev.deleted is false ");
    }

    if (params.hasSecurityFilter()) {
      sqlParameters.addValue(
          "program_uid",
          params.getAccessiblePrograms().isEmpty()
              ? null
              : UID.toValueSet(params.getAccessiblePrograms()));

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (p.uid in (")
          .append(":program_uid")
          .append(")) ");
    }

    return fromBuilder;
  }

  private String getOrgUnitSql(
      SingleEventQueryParams params,
      UserDetails user,
      MapSqlParameterSource mapSqlParameterSource) {
    return switch (params.getOrgUnitMode()) {
      case CAPTURE -> createCaptureSql(user, mapSqlParameterSource);
      case ACCESSIBLE -> createAccessibleSql(user, params, mapSqlParameterSource);
      case DESCENDANTS -> createDescendantsSql(user, params, mapSqlParameterSource);
      case CHILDREN -> createChildrenSql(user, params, mapSqlParameterSource);
      case SELECTED -> createSelectedSql(user, params, mapSqlParameterSource);
      case ALL -> null;
    };
  }

  private String createCaptureSql(UserDetails user, MapSqlParameterSource mapSqlParameterSource) {
    return createCaptureScopeQuery(user, mapSqlParameterSource, "");
  }

  private String createAccessibleSql(
      UserDetails user,
      SingleEventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource) {

    if (isProgramRestricted(params.getProgram()) || isUserSearchScopeNotSet(user)) {
      return createCaptureSql(user, mapSqlParameterSource);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(USER_SCOPE_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
  }

  private String createDescendantsSql(
      UserDetails user,
      SingleEventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getStoredPath());

    if (isProgramRestricted(params.getProgram())) {
      return createCaptureScopeQuery(
          user, mapSqlParameterSource, AND + CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
  }

  private String createChildrenSql(
      UserDetails user,
      SingleEventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getStoredPath());

    String customChildrenQuery =
        " and (ou.hierarchylevel = "
            + params.getOrgUnit().getHierarchyLevel()
            + " OR ou.hierarchylevel = "
            + (params.getOrgUnit().getHierarchyLevel() + 1)
            + " ) ";

    if (isProgramRestricted(params.getProgram())) {
      return createCaptureScopeQuery(
          user,
          mapSqlParameterSource,
          AND + CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY + customChildrenQuery);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(
        CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY + customChildrenQuery);
  }

  private String createSelectedSql(
      UserDetails user,
      SingleEventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getStoredPath());

    String orgUnitPathEqualsMatchQuery =
        " ou.path = :"
            + COLUMN_ORG_UNIT_PATH
            + " "
            + AND
            + USER_SCOPE_ORG_UNIT_PATH_LIKE_MATCH_QUERY;

    if (isProgramRestricted(params.getProgram())) {
      String customSelectedClause = AND + orgUnitPathEqualsMatchQuery;
      return createCaptureScopeQuery(user, mapSqlParameterSource, customSelectedClause);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(orgUnitPathEqualsMatchQuery);
  }

  /**
   * Generates a getSql to match the org unit event to the org unit(s) in the user's capture scope
   *
   * @param orgUnitMatcher specific condition to add depending on the ou mode
   * @return a getSql clause to add to the main query
   */
  private String createCaptureScopeQuery(
      UserDetails user, MapSqlParameterSource mapSqlParameterSource, String orgUnitMatcher) {
    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());

    return " exists(select cs.organisationunitid "
        + " from usermembership cs "
        + " join organisationunit orgunit on orgunit.organisationunitid = cs.organisationunitid "
        + " join userinfo u on u.userinfoid = cs.userinfoid "
        + " where u.uid = :"
        + COLUMN_USER_UID
        + " and ou.path like concat(orgunit.path, '%') "
        + orgUnitMatcher
        + ") ";
  }

  /**
   * Generates a getSql to match the org unit event to the org unit(s) in the user's search and
   * capture scope
   *
   * @param orgUnitMatcher specific condition to add depending on the ou mode
   * @return a getSql clause to add to the main query
   */
  private static String getSearchAndCaptureScopeOrgUnitPathMatchQuery(String orgUnitMatcher) {
    return " (exists(select ss.organisationunitid "
        + " from userteisearchorgunits ss "
        + " join userinfo u on u.userinfoid = ss.userinfoid "
        + " join organisationunit orgunit on orgunit.organisationunitid = ss.organisationunitid "
        + " where u.uid = :"
        + COLUMN_USER_UID
        + AND
        + orgUnitMatcher
        + " and p.accesslevel in ('OPEN', 'AUDITED')) "
        + " or exists(select cs.organisationunitid "
        + " from usermembership cs "
        + " join userinfo u on u.userinfoid = cs.userinfoid "
        + " join organisationunit orgunit on orgunit.organisationunitid = cs.organisationunitid "
        + " where u.uid = :"
        + COLUMN_USER_UID
        + AND
        + orgUnitMatcher
        + " )) ";
  }

  private boolean isProgramRestricted(Program program) {
    return program != null && (program.isProtected() || program.isClosed());
  }

  private boolean isUserSearchScopeNotSet(UserDetails user) {
    return user.getUserSearchOrgUnitIds().isEmpty();
  }

  private String eventStatusSql(
      SingleEventQueryParams params, MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp) {
    StringBuilder stringBuilder = new StringBuilder();

    if (params.getEventStatus() != null) {
      if (params.getEventStatus() == EventStatus.VISITED) {
        mapSqlParameterSource.addValue(COLUMN_EVENT_STATUS, EventStatus.ACTIVE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(EVENT_STATUS_EQ)
            .append(":" + COLUMN_EVENT_STATUS)
            .append(" and ev.occurreddate is not null ");
      } else if (params.getEventStatus() == EventStatus.OVERDUE) {
        mapSqlParameterSource.addValue(COLUMN_EVENT_STATUS, EventStatus.SCHEDULE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(" date(now()) > date(ev.scheduleddate) and ev.status = ")
            .append(":" + COLUMN_EVENT_STATUS)
            .append(" ");
      } else {
        mapSqlParameterSource.addValue(COLUMN_EVENT_STATUS, params.getEventStatus().name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(EVENT_STATUS_EQ)
            .append(":" + COLUMN_EVENT_STATUS)
            .append(" ");
      }
    }

    return stringBuilder.toString();
  }

  private String addLastUpdatedFilters(
      SingleEventQueryParams params, MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp) {
    StringBuilder sqlBuilder = new StringBuilder();

    if (params.hasUpdatedAtDuration()) {
      mapSqlParameterSource.addValue(
          "lastUpdated",
          DateUtils.offSetDateTimeFrom(DateUtils.nowMinusDuration(params.getUpdatedAtDuration())),
          Types.TIMESTAMP_WITH_TIMEZONE);

      sqlBuilder
          .append(hlp.whereAnd())
          .append(EVENT_LASTUPDATED_GT)
          .append(":lastUpdated")
          .append(" ");
    } else {
      if (params.hasUpdatedAtStartDate()) {
        mapSqlParameterSource.addValue(
            "lastUpdatedStart", params.getUpdatedAtStartDate(), Types.TIMESTAMP);

        sqlBuilder
            .append(hlp.whereAnd())
            .append(EVENT_LASTUPDATED_GT)
            .append(":lastUpdatedStart")
            .append(" ");
      }

      if (params.hasUpdatedAtEndDate()) {
        mapSqlParameterSource.addValue(
            "lastUpdatedEnd", params.getUpdatedAtEndDate(), Types.TIMESTAMP);

        sqlBuilder
            .append(hlp.whereAnd())
            .append(" ev.lastupdated <= ")
            .append(":lastUpdatedEnd")
            .append(" ");
      }
    }

    return sqlBuilder.toString();
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
  private String getCategoryOptionComboQuery(UserDetails user) {
    String joinCondition =
"""
 inner join (select coc.uid, coc.code, coc.name, coc.attributevalues, coc.categoryoptioncomboid as id,\
    jsonb_object_agg(
        co.uid,
        jsonb_build_object(
            'name', co.name,
            'code', co.code,
            'attributeValues', co.attributevalues
        )
    ) AS co_values,
 count(co.categoryoptionid) as co_count from\
 categoryoptioncombo coc  inner join categoryoptioncombos_categoryoptions cocco on\
 coc.categoryoptioncomboid = cocco.categoryoptioncomboid inner join categoryoption\
 co on cocco.categoryoptionid = co.categoryoptionid group by\
 coc.categoryoptioncomboid \
""";

    if (isNotSuperUser(user)) {
      joinCondition =
          joinCondition
              + " having bool_and(case when "
              + JpaQueryUtils.generateSQlQueryForSharingCheck(
                  "co.sharing", user, AclService.LIKE_READ_DATA)
              + " then true else false end) = True ";
    }

    return joinCondition + ") as coc_agg on coc_agg.id = ev.attributeoptioncomboid ";
  }

  private String getLimitAndOffsetClause(final PageParams pageParams) {
    // get extra event to determine if there is a nextPage
    return " limit " + (pageParams.getPageSize() + 1) + " offset " + pageParams.getOffset() + " ";
  }

  private String getOrderQuery(SingleEventQueryParams params) {
    ArrayList<String> orderFields = new ArrayList<>();

    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  "Cannot order by '%s'. Supported are data elements, tracked entity attributes and"
                      + " fields '%s'.",
                  field, String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
        }

        orderFields.add(ORDERABLE_FIELDS.get(field) + " " + order.getDirection());
      } else if (order.getField() instanceof TrackedEntityAttribute tea) {
        orderFields.add(tea.getUid() + "_value " + order.getDirection());
      } else if (order.getField() instanceof DataElement de) {
        orderFields.add(de.getUid() + " " + order.getDirection());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Cannot order by '%s'. Supported are data elements, tracked entity attributes and"
                    + " fields '%s'.",
                order.getField(),
                String.join(", ", ORDERABLE_FIELDS.keySet().stream().sorted().toList())));
      }
    }

    if (!orderFields.isEmpty()) {
      return "order by " + StringUtils.join(orderFields, ',') + ", " + DEFAULT_ORDER + " ";
    } else {
      return "order by " + DEFAULT_ORDER + " ";
    }
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

  private void setAccessiblePrograms(UserDetails user, SingleEventQueryParams params) {
    if (isNotSuperUser(user)) {
      params.setAccessiblePrograms(
          manager.getDataReadAll(Program.class).stream().map(UID::of).collect(Collectors.toSet()));
    }
  }
}
