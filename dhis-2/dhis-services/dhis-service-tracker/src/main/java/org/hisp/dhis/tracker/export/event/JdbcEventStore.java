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
package org.hisp.dhis.tracker.export.event;

import static java.util.Map.entry;
import static org.hisp.dhis.common.ValueType.NUMERIC_TYPES;
import static org.hisp.dhis.system.util.SqlUtils.castToNumber;
import static org.hisp.dhis.system.util.SqlUtils.lower;
import static org.hisp.dhis.system.util.SqlUtils.quote;
import static org.hisp.dhis.system.util.SqlUtils.singleQuote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.export.relationship.RelationshipStore;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository("org.hisp.dhis.tracker.export.event.EventStore")
@RequiredArgsConstructor
class JdbcEventStore {
  private static final String RELATIONSHIP_IDS_QUERY =
      " left join (select ri.eventid as ri_ev_id, json_agg(ri.relationshipid) as ev_rl from"
          + " relationshipitem ri group by ri_ev_id) as fgh on fgh.ri_ev_id=event.ev_id ";

  private static final String EVENT_NOTE_QUERY =
      """
      select evn.eventid as evn_id,\
       n.noteid as note_id,\
       n.notetext as note_text,\
       n.created as note_created,\
       n.creator as note_creator,\
       n.uid as note_uid,\
       n.lastupdated as note_lastupdated,\
       userinfo.userinfoid as note_user_id,\
       userinfo.code as note_user_code,\
       userinfo.uid as note_user_uid,\
       userinfo.username as note_user_username,\
       userinfo.firstname as note_user_firstname,\
       userinfo.surname as note_user_surname\
       from event_notes evn\
       inner join note n\
       on evn.noteid = n.noteid\
       left join userinfo on n.lastupdatedby = userinfo.userinfoid\s""";

  private static final String EVENT_STATUS_EQ = " ev.status = ";

  private static final String EVENT_LASTUPDATED_GT = " ev.lastupdated >= ";

  private static final String DOT_NAME = ".name)";

  private static final String SPACE = " ";

  private static final String EQUALS = " = ";

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
  private static final String COLUMN_ENROLLMENT_STATUS = "en_status";
  private static final String COLUMN_ENROLLMENT_DATE = "en_enrollmentdate";
  private static final String COLUMN_ORG_UNIT_UID = "orgunit_uid";
  private static final String COLUMN_ORG_UNIT_CODE = "orgunit_code";
  private static final String COLUMN_ORG_UNIT_NAME = "orgunit_name";
  private static final String COLUMN_ORG_UNIT_ATTRIBUTE_VALUES = "orgunit_attributevalues";
  private static final String COLUMN_TRACKEDENTITY_UID = "te_uid";
  private static final String COLUMN_EVENT_OCCURRED_DATE = "ev_occurreddate";
  private static final String COLUMN_ENROLLMENT_FOLLOWUP = "en_followup";
  private static final String COLUMN_EVENT_STATUS = "ev_status";
  private static final String COLUMN_EVENT_SCHEDULED_DATE = "ev_scheduleddate";
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
   * Events can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.program.Event}. Maps fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", COLUMN_EVENT_UID),
          entry("enrollment.program.uid", COLUMN_PROGRAM_UID),
          entry("programStage.uid", COLUMN_PROGRAM_STAGE_UID),
          entry("enrollment.uid", COLUMN_ENROLLMENT_UID),
          entry("enrollment.status", COLUMN_ENROLLMENT_STATUS),
          entry("enrollment.enrollmentDate", COLUMN_ENROLLMENT_DATE),
          entry("organisationUnit.uid", COLUMN_ORG_UNIT_UID),
          entry("enrollment.trackedEntity.uid", COLUMN_TRACKEDENTITY_UID),
          entry("occurredDate", COLUMN_EVENT_OCCURRED_DATE),
          entry("enrollment.followUp", COLUMN_ENROLLMENT_FOLLOWUP),
          entry("status", COLUMN_EVENT_STATUS),
          entry("scheduledDate", COLUMN_EVENT_SCHEDULED_DATE),
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

  private final RelationshipStore relationshipStore;

  public List<Event> getEvents(EventQueryParams queryParams) {
    return fetchEvents(queryParams, null);
  }

  public Page<Event> getEvents(EventQueryParams queryParams, PageParams pageParams) {
    List<Event> events = fetchEvents(queryParams, pageParams);
    LongSupplier eventCount = () -> getEventCount(queryParams);
    return getPage(pageParams, events, eventCount);
  }

  private List<Event> fetchEvents(EventQueryParams queryParams, PageParams pageParams) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    setAccessiblePrograms(currentUser, queryParams);

    Map<String, Event> eventsByUid;
    if (pageParams == null) {
      eventsByUid = new HashMap<>();
    } else {
      eventsByUid = new HashMap<>(pageParams.getPageSize());
    }
    List<Event> events = new ArrayList<>();
    List<Long> relationshipIds = new ArrayList<>();

    final Gson gson = new Gson();

    final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    String sql = buildSql(queryParams, pageParams, mapSqlParameterSource, currentUser);

    return jdbcTemplate.query(
        sql,
        mapSqlParameterSource,
        resultSet -> {
          Set<String> notes = new HashSet<>();

          while (resultSet.next()) {
            if (resultSet.getString(COLUMN_EVENT_UID) == null) {
              continue;
            }

            String eventUid = resultSet.getString(COLUMN_EVENT_UID);

            Event event;
            if (eventsByUid.containsKey(eventUid)) {
              event = eventsByUid.get(eventUid);
            } else {
              event = new Event();
              event.setId(resultSet.getLong(COLUMN_EVENT_ID));
              event.setUid(eventUid);
              eventsByUid.put(eventUid, event);

              TrackedEntity te = new TrackedEntity();
              te.setUid(resultSet.getString(COLUMN_TRACKEDENTITY_UID));
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
              enrollment.setTrackedEntity(te);

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

              enrollment.setStatus(
                  EnrollmentStatus.valueOf(resultSet.getString(COLUMN_ENROLLMENT_STATUS)));
              enrollment.setFollowup(resultSet.getBoolean(COLUMN_ENROLLMENT_FOLLOWUP));
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
              event.setScheduledDate(resultSet.getTimestamp(COLUMN_EVENT_SCHEDULED_DATE));
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

              String dataValuesResult = resultSet.getString("ev_eventdatavalues_idschemes");
              if (!StringUtils.isEmpty(dataValuesResult)) {
                JsonMixed dataValuesJson = JsonMixed.of(dataValuesResult);
                JsonObject dataValuesObject = dataValuesJson.asObject();
                Set<EventDataValue> eventDataValues =
                    new HashSet<>(dataValuesObject.names().size());
                for (String uid : dataValuesObject.names()) {
                  JsonObject dataValueJson = dataValuesObject.getObject(uid);
                  EventDataValue eventDataValue = new EventDataValue();
                  // TODO(ivo) EventDataValues are different than other data. It has no reference to
                  // metadata but only a String field. To support idSchemes on export we can either
                  // set the String to uid, code, name or the attribute value of given attribute:uid
                  // or we need to refactor the type to use either a reference to DataElement or to
                  // use the MedatadataIdentifier. I assume String was chosen to allow storing this
                  // as JSONB
                  eventDataValue.setDataElement(
                      dataValueJson.getString("dataElementCode").string(""));
                  // eventDataValue.setDataElement(uid);
                  eventDataValue.setValue(dataValueJson.getString("value").string(""));
                  eventDataValue.setProvidedElsewhere(
                      dataValueJson.getBoolean("providedElsewhere").booleanValue(false));
                  eventDataValue.setStoredBy(dataValueJson.getString("storedBy").string(""));
                  // TODO(ivo) I also need to map these
                  // @Mapping(target = "createdAt", source = "created")
                  // @Mapping(target = "updatedAt", source = "lastUpdated")
                  // @Mapping(target = "createdBy", source = "createdByUserInfo")
                  // @Mapping(target = "updatedBy", source = "lastUpdatedByUserInfo")
                  eventDataValues.add(eventDataValue);
                }
                event.getEventDataValues().addAll(eventDataValues);
              }

              if (queryParams.isIncludeRelationships() && resultSet.getObject("ev_rl") != null) {
                PGobject pGobject = (PGobject) resultSet.getObject("ev_rl");

                if (pGobject != null) {
                  String value = pGobject.getValue();

                  relationshipIds.addAll(Lists.newArrayList(gson.fromJson(value, Long[].class)));
                }
              }

              events.add(event);
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

              note.setLastUpdated(resultSet.getTimestamp("note_lastupdated"));

              event.getNotes().add(note);
              notes.add(resultSet.getString("note_id"));
            }
          }

          List<Relationship> relationships = relationshipStore.getById(relationshipIds);

          Multimap<String, RelationshipItem> map = LinkedListMultimap.create();

          for (Relationship relationship : relationships) {
            if (relationship.getFrom().getEvent() != null) {
              map.put(relationship.getFrom().getEvent().getUid(), relationship.getFrom());
            }
            if (relationship.getTo().getEvent() != null) {
              map.put(relationship.getTo().getEvent().getUid(), relationship.getTo());
            }
          }

          if (!map.isEmpty()) {
            events.forEach(e -> e.getRelationshipItems().addAll(map.get(e.getUid())));
          }

          return events;
        });
  }

  private Page<Event> getPage(PageParams pageParams, List<Event> events, LongSupplier eventCount) {
    if (pageParams.isPageTotal()) {
      return Page.withTotals(
          events, pageParams.getPage(), pageParams.getPageSize(), eventCount.getAsLong());
    }

    return Page.withoutTotals(events, pageParams.getPage(), pageParams.getPageSize());
  }

  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  private long getEventCount(EventQueryParams params) {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    setAccessiblePrograms(currentUser, params);

    String sql;

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    sql = getEventSelectQuery(params, mapSqlParameterSource, currentUser);

    sql = sql.replaceFirst("select .*? from", "select count(*) as ev_count from");

    sql = sql.replaceFirst("order .*? (desc|asc)", "");

    sql = sql.replaceFirst("limit \\d+ offset \\d+", "");

    RowCountHandler rowCountHandler = new RowCountHandler();
    jdbcTemplate.query(sql, mapSqlParameterSource, rowCountHandler);
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
      EventQueryParams queryParams,
      PageParams pageParams,
      MapSqlParameterSource mapSqlParameterSource,
      User user) {
    StringBuilder sqlBuilder = new StringBuilder().append("select * from (");

    sqlBuilder.append(getEventSelectQuery(queryParams, mapSqlParameterSource, user));

    sqlBuilder.append(getOrderQuery(queryParams));

    if (pageParams != null) {
      sqlBuilder.append(getLimitAndOffsetClause(pageParams));
    }

    sqlBuilder.append(") as event left join (");

    if (queryParams.isIncludeAttributes()) {
      sqlBuilder.append(getAttributeValueQuery());

      sqlBuilder.append(") as att on event.te_id=att.pav_id left join (");
    }

    sqlBuilder.append(EVENT_NOTE_QUERY);

    sqlBuilder.append(") as cm on event.");
    sqlBuilder.append(COLUMN_EVENT_ID);
    sqlBuilder.append("=cm.evn_id ");

    if (queryParams.isIncludeRelationships()) {
      sqlBuilder.append(RELATIONSHIP_IDS_QUERY);
    }

    sqlBuilder.append(getOrderQuery(queryParams));

    return sqlBuilder.toString();
  }

  /**
   * Generates a single INNER JOIN for each attribute we are filtering or ordering on. We can search
   * by a range of operators. All searching is using lower() since attribute values are
   * case-insensitive.
   */
  private String joinAttributeValue(
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource) {
    StringBuilder sql = new StringBuilder();

    for (Entry<TrackedEntityAttribute, List<QueryFilter>> queryItem :
        params.getAttributes().entrySet()) {

      TrackedEntityAttribute tea = queryItem.getKey();
      String teaUid = tea.getUid();
      String teaValueCol = quote(teaUid);
      String teaCol = quote(teaUid + "ATT");

      sql.append(" inner join trackedentityattributevalue ")
          .append(teaValueCol)
          .append(" on ")
          .append(teaValueCol + ".trackedentityid")
          .append(" = TE.trackedentityid ")
          .append(" inner join trackedentityattribute ")
          .append(teaCol)
          .append(" on ")
          .append(teaValueCol + ".trackedentityattributeid")
          .append(EQUALS)
          .append(teaCol + ".trackedentityattributeid")
          .append(AND)
          .append(teaCol + ".UID")
          .append(EQUALS)
          .append(singleQuote(teaUid));

      sql.append(
          getAttributeFilterQuery(
              mapSqlParameterSource,
              queryItem.getValue(),
              teaCol,
              teaValueCol,
              tea.getValueType().isNumeric()));
    }

    return sql.toString();
  }

  private String getAttributeFilterQuery(
      MapSqlParameterSource mapSqlParameterSource,
      List<QueryFilter> filters,
      String teaCol,
      String teaValueCol,
      boolean isNumericTea) {

    if (filters.isEmpty()) {
      return "";
    }

    StringBuilder query = new StringBuilder(AND);
    // In SQL the order of expressions linked by AND is not
    // guaranteed.
    // So when casting to number we need to be sure that the value
    // to cast is really a number.
    if (isNumericTea) {
      query
          .append(" case when ")
          .append(lower(teaCol + ".valueType"))
          .append(" in (")
          .append(
              NUMERIC_TYPES.stream()
                  .map(Enum::name)
                  .map(StringUtils::lowerCase)
                  .map(SqlUtils::singleQuote)
                  .collect(Collectors.joining(",")))
          .append(")")
          .append(" then ");
    }

    List<String> filterStrings = new ArrayList<>();

    for (int i = 0; i < filters.size(); i++) {
      QueryFilter filter = filters.get(i);
      StringBuilder filterString = new StringBuilder();
      final String queryCol =
          isNumericTea ? castToNumber(teaValueCol + ".value") : lower(teaValueCol + ".value");
      int itemType = isNumericTea ? Types.NUMERIC : Types.VARCHAR;
      String parameterKey = "attributeFilter_%s_%d".formatted(teaValueCol.replace("\"", ""), i);
      mapSqlParameterSource.addValue(
          parameterKey,
          isNumericTea
              ? Double.valueOf(filter.getSqlBindFilter())
              : StringUtils.lowerCase(filter.getSqlBindFilter()),
          itemType);

      filterString
          .append(queryCol)
          .append(SPACE)
          .append(filter.getSqlOperator())
          .append(SPACE)
          .append(":" + parameterKey);
      filterStrings.add(filterString.toString());
    }
    query.append(String.join(AND, filterStrings));

    if (isNumericTea) {
      query.append(" END ");
    }

    return query.toString();
  }

  /**
   * Generates the LEFT JOINs used for attributes we are ordering by (If any). We use LEFT JOIN to
   * avoid removing any rows if there is no value for a given attribute and te. The result of this
   * LEFT JOIN is used in the sub-query projection, and ordering in the sub-query and main query.
   *
   * @return a SQL LEFT JOIN for attributes used for ordering, or empty string if no attributes is
   *     used in order.
   */
  private String getFromSubQueryJoinOrderByAttributes(EventQueryParams params) {
    StringBuilder joinOrderAttributes = new StringBuilder();

    for (TrackedEntityAttribute orderAttribute : params.leftJoinAttributes()) {

      joinOrderAttributes
          .append(" left join trackedentityattributevalue as ")
          .append(quote(orderAttribute.getUid()))
          .append(" on ")
          .append(quote(orderAttribute.getUid()))
          .append(".trackedentityid = TE.trackedentityid ")
          .append("and ")
          .append(quote(orderAttribute.getUid()))
          .append(".trackedentityattributeid = ")
          .append(orderAttribute.getId())
          .append(SPACE);
    }

    return joinOrderAttributes.toString();
  }

  private String getEventSelectQuery(
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource, User user) {
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
            .append(", ev.scheduleddate as ")
            .append(COLUMN_EVENT_SCHEDULED_DATE)
            .append(", ev.eventdatavalues as ")
            .append(COLUMN_EVENT_DATAVALUES)
            // TODO(ivo) use case to use the same 'ev_eventdatavalues' alias if idScheme!=UID and
            // when UID and subquery is not present
            .append(", eventdatavalues.eventdatavalues_idschemes as ev_eventdatavalues_idschemes")
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
            .append(", coc_agg.co_values AS co_values, coc_agg.co_count AS option_size, ");

    for (Order order : params.getOrder()) {
      if (order.getField() instanceof TrackedEntityAttribute tea)
        selectBuilder
            .append(quote(tea.getUid()))
            .append(".value AS ")
            .append(tea.getUid())
            .append("_value, ");
    }

    return selectBuilder
        .append(
            "en.uid as "
                + COLUMN_ENROLLMENT_UID
                + ", en.status as "
                + COLUMN_ENROLLMENT_STATUS
                + ", en.followup as "
                + COLUMN_ENROLLMENT_FOLLOWUP
                + ", en.enrollmentdate as "
                + COLUMN_ENROLLMENT_DATE
                + ", en.occurreddate as en_occurreddate, ")
        .append("p.type as p_type, ")
        .append("te.trackedentityid as te_id, te.uid as ")
        .append(COLUMN_TRACKEDENTITY_UID)
        .append(
            getFromWhereClause(
                params,
                mapSqlParameterSource,
                user,
                hlp,
                dataElementAndFiltersSql(params, mapSqlParameterSource, hlp, selectBuilder)))
        .toString();
  }

  private boolean checkForOwnership(EventQueryParams params) {
    return Optional.ofNullable(params.getProgram())
        .filter(
            p ->
                Objects.nonNull(p.getProgramType())
                    && p.getProgramType() == ProgramType.WITH_REGISTRATION)
        .isPresent();
  }

  private StringBuilder getFromWhereClause(
      EventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource,
      User user,
      SqlHelper hlp,
      StringBuilder dataElementAndFiltersSql) {
    StringBuilder fromBuilder =
        new StringBuilder(" from event ev ")
            .append("inner join enrollment en on en.enrollmentid=ev.enrollmentid ")
            .append("inner join program p on p.programid=en.programid ")
            .append("inner join programstage ps on ps.programstageid=ev.programstageid ");

    if (checkForOwnership(params)) {
      fromBuilder
          .append(
              "left join trackedentityprogramowner po on (en.trackedentityid=po.trackedentityid) ")
          .append(
              "inner join organisationunit evou on (coalesce(po.organisationunitid,"
                  + " ev.organisationunitid)=evou.organisationunitid) ")
          .append(
              "inner join organisationunit ou on (ev.organisationunitid=ou.organisationunitid) ");
    } else {
      fromBuilder.append(
          "inner join organisationunit ou on ev.organisationunitid=ou.organisationunitid ");
    }

    fromBuilder
        .append("left join trackedentity te on te.trackedentityid=en.trackedentityid ")
        .append("left join userinfo au on (ev.assigneduserid=au.userinfoid) ");

    // JOIN attributes we need to filter on.
    fromBuilder.append(joinAttributeValue(params, mapSqlParameterSource));

    // LEFT JOIN not filterable attributes we need to sort on.
    fromBuilder.append(getFromSubQueryJoinOrderByAttributes(params));

    fromBuilder.append(getCategoryOptionComboQuery(user));

    fromBuilder.append(dataElementAndFiltersSql);

    fromBuilder.append(
        """
left join
    (
        select
            ev_eventdatavalues.eventid,
            jsonb_object_agg(
                de.uid,
                jsonb_build_object(
                    'value',
                    eventdatavalue.value ->> 'value',
                    'created',
                    eventdatavalue.value ->> 'created',
                    'storedBy',
                    eventdatavalue.value ->> 'storedBy',
                    'lastUpdated',
                    eventdatavalue.value ->> 'lastUpdated',
                    'providedElsewhere',
                    eventdatavalue.value -> 'providedElsewhere',
                    'dataElementCode',
                    coalesce(de.code, ''),
                    'dataElementName',
                    coalesce(de.name, ''),
                    'dataElementAttributeValues',
                    coalesce(de.attributevalues, '{}')
                )
            ) as eventdatavalues_idschemes
        from event ev_eventdatavalues
        left join
            lateral jsonb_each(
                coalesce(ev_eventdatavalues.eventdatavalues, '{}')
            ) as eventdatavalue(dataelement_uid, value)
            on true
        left join dataelement de on de.uid = eventdatavalue.dataelement_uid
        where eventdatavalue.dataelement_uid is not null
        group by ev_eventdatavalues.eventid
    ) eventdatavalues
    on ev.eventid = eventdatavalues.eventid
""");

    if (params.getTrackedEntity() != null) {
      mapSqlParameterSource.addValue("trackedentityid", params.getTrackedEntity().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" te.trackedentityid= ")
          .append(":trackedentityid")
          .append(" ");
    }

    if (params.getProgram() != null) {
      mapSqlParameterSource.addValue("programid", params.getProgram().getId());

      fromBuilder.append(hlp.whereAnd()).append(" p.programid = ").append(":programid").append(" ");
    }

    if (params.getProgramStage() != null) {
      mapSqlParameterSource.addValue("programstageid", params.getProgramStage().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" ps.programstageid = ")
          .append(":programstageid")
          .append(" ");
    }

    if (params.getEnrollmentStatus() != null) {
      mapSqlParameterSource.addValue("program_status", params.getEnrollmentStatus().name());

      fromBuilder.append(hlp.whereAnd()).append(" en.status = ").append(":program_status ");
    }

    if (params.getEnrollmentEnrolledBefore() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentEnrolledBefore", params.getEnrollmentEnrolledBefore(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (en.enrollmentdate <= :enrollmentEnrolledBefore ) ");
    }

    if (params.getEnrollmentEnrolledAfter() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentEnrolledAfter", params.getEnrollmentEnrolledAfter(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (en.enrollmentdate >= :enrollmentEnrolledAfter ) ");
    }

    if (params.getEnrollmentOccurredBefore() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentOccurredBefore", params.getEnrollmentOccurredBefore(), Types.TIMESTAMP);
      fromBuilder
          .append(hlp.whereAnd())
          .append(" (en.occurreddate <= :enrollmentOccurredBefore ) ");
    }

    if (params.getEnrollmentOccurredAfter() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentOccurredAfter", params.getEnrollmentOccurredAfter(), Types.TIMESTAMP);
      fromBuilder.append(hlp.whereAnd()).append(" (en.occurreddate >= :enrollmentOccurredAfter ) ");
    }

    if (params.getScheduleAtStartDate() != null) {
      mapSqlParameterSource.addValue(
          "startScheduledDate", params.getScheduleAtStartDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.scheduleddate is not null and ev.scheduleddate >= :startScheduledDate ) ");
    }

    if (params.getScheduleAtEndDate() != null) {
      mapSqlParameterSource.addValue(
          "endScheduledDate", params.getScheduleAtEndDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.scheduleddate is not null and ev.scheduleddate <= :endScheduledDate ) ");
    }

    if (params.getFollowUp() != null) {
      fromBuilder
          .append(hlp.whereAnd())
          .append(" en.followup is ")
          .append(Boolean.TRUE.equals(params.getFollowUp()) ? "true" : "false")
          .append(" ");
    }

    fromBuilder.append(addLastUpdatedFilters(params, mapSqlParameterSource, hlp));

    // Comparing milliseconds instead of always creating new Date(0)
    if (params.getSkipChangedBefore() != null && params.getSkipChangedBefore().getTime() > 0) {
      mapSqlParameterSource.addValue(
          "skipChangedBefore", params.getSkipChangedBefore(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(EVENT_LASTUPDATED_GT)
          .append(":skipChangedBefore")
          .append(" ");
    }

    if (params.getCategoryOptionCombo() != null) {
      mapSqlParameterSource.addValue(
          "attributeoptioncomboid", params.getCategoryOptionCombo().getId());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" ev.attributeoptioncomboid = ")
          .append(":attributeoptioncomboid")
          .append(" ");
    }

    String orgUnitSql = getOrgUnitSql(params, user, mapSqlParameterSource);

    if (!Strings.isNullOrEmpty(orgUnitSql)) {
      fromBuilder.append(hlp.whereAnd()).append(orgUnitSql);
    }

    if (params.getOccurredStartDate() != null) {
      mapSqlParameterSource.addValue("startDate", params.getOccurredStartDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.occurreddate >= ")
          .append(":startDate")
          .append(" or (ev.occurreddate is null and ev.scheduleddate >= ")
          .append(":startDate")
          .append(" )) ");
    }

    if (params.getOccurredEndDate() != null) {
      mapSqlParameterSource.addValue("endDate", params.getOccurredEndDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.occurreddate <= ")
          .append(":endDate")
          .append(" or (ev.occurreddate is null and ev.scheduleddate <=")
          .append(":endDate")
          .append(" )) ");
    }

    if (params.getProgramType() != null) {
      mapSqlParameterSource.addValue("programType", params.getProgramType().name());

      fromBuilder.append(hlp.whereAnd()).append(" p.type = ").append(":programType").append(" ");
    }

    fromBuilder.append(eventStatusSql(params, mapSqlParameterSource, hlp));

    if (params.getEvents() != null
        && !params.getEvents().isEmpty()
        && !params.hasDataElementFilter()) {
      mapSqlParameterSource.addValue(COLUMN_EVENT_UID, UID.toValueSet(params.getEvents()));
      fromBuilder.append(hlp.whereAnd()).append(" (ev.uid in (").append(":ev_uid").append(")) ");
    }

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      mapSqlParameterSource.addValue(
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
      mapSqlParameterSource.addValue(
          "program_uid",
          params.getAccessiblePrograms().isEmpty()
              ? null
              : UID.toValueSet(params.getAccessiblePrograms()));

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (p.uid in (")
          .append(":program_uid")
          .append(")) ");

      mapSqlParameterSource.addValue(
          "programstage_uid",
          params.getAccessibleProgramStages().isEmpty()
              ? null
              : UID.toValueSet(params.getAccessibleProgramStages()));

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ps.uid in (")
          .append(":programstage_uid")
          .append(")) ");
    }

    if (params.isSynchronizationQuery()) {
      fromBuilder.append(hlp.whereAnd()).append(" ev.lastupdated > ev.lastsynchronized ");
    }

    if (!CollectionUtils.isEmpty(params.getEnrollments())) {
      mapSqlParameterSource.addValue("enrollment_uid", UID.toValueSet(params.getEnrollments()));

      fromBuilder.append(hlp.whereAnd()).append(" (en.uid in (:enrollment_uid)) ");
    }

    return fromBuilder;
  }

  private String getOrgUnitSql(
      EventQueryParams params, User user, MapSqlParameterSource mapSqlParameterSource) {
    return switch (params.getOrgUnitMode()) {
      case CAPTURE -> createCaptureSql(user, mapSqlParameterSource);
      case ACCESSIBLE -> createAccessibleSql(user, params, mapSqlParameterSource);
      case DESCENDANTS -> createDescendantsSql(user, params, mapSqlParameterSource);
      case CHILDREN -> createChildrenSql(user, params, mapSqlParameterSource);
      case SELECTED -> createSelectedSql(user, params, mapSqlParameterSource);
      case ALL -> null;
    };
  }

  private String createCaptureSql(User user, MapSqlParameterSource mapSqlParameterSource) {
    return createCaptureScopeQuery(user, mapSqlParameterSource, "");
  }

  private String createAccessibleSql(
      User user, EventQueryParams params, MapSqlParameterSource mapSqlParameterSource) {

    if (isProgramRestricted(params.getProgram()) || isUserSearchScopeNotSet(user)) {
      return createCaptureSql(user, mapSqlParameterSource);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(USER_SCOPE_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
  }

  private String createDescendantsSql(
      User user, EventQueryParams params, MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getPath());

    if (isProgramRestricted(params.getProgram())) {
      return createCaptureScopeQuery(
          user, mapSqlParameterSource, AND + CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
    }

    mapSqlParameterSource.addValue(COLUMN_USER_UID, user.getUid());
    return getSearchAndCaptureScopeOrgUnitPathMatchQuery(CUSTOM_ORG_UNIT_PATH_LIKE_MATCH_QUERY);
  }

  private String createChildrenSql(
      User user, EventQueryParams params, MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getPath());

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
      User user, EventQueryParams params, MapSqlParameterSource mapSqlParameterSource) {
    mapSqlParameterSource.addValue(COLUMN_ORG_UNIT_PATH, params.getOrgUnit().getPath());

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
   * Generates a sql to match the org unit event to the org unit(s) in the user's capture scope
   *
   * @param orgUnitMatcher specific condition to add depending on the ou mode
   * @return a sql clause to add to the main query
   */
  private String createCaptureScopeQuery(
      User user, MapSqlParameterSource mapSqlParameterSource, String orgUnitMatcher) {
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
   * Generates a sql to match the org unit event to the org unit(s) in the user's search and capture
   * scope
   *
   * @param orgUnitMatcher specific condition to add depending on the ou mode
   * @return a sql clause to add to the main query
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

  private boolean isUserSearchScopeNotSet(User user) {
    return user.getTeiSearchOrganisationUnits().isEmpty();
  }

  /**
   * For dataElement params, restriction is set in inner join. For query params, restriction is set
   * in where clause.
   */
  private StringBuilder dataElementAndFiltersSql(
      EventQueryParams params,
      MapSqlParameterSource mapSqlParameterSource,
      SqlHelper hlp,
      StringBuilder selectBuilder) {
    int filterCount = 0;

    StringBuilder optionValueJoinBuilder = new StringBuilder();
    StringBuilder optionValueConditionBuilder = new StringBuilder();
    StringBuilder eventDataValuesWhereSql = new StringBuilder();
    Set<String> joinedColumns = new HashSet<>();

    for (Entry<DataElement, List<QueryFilter>> item : params.getDataElements().entrySet()) {
      ++filterCount;

      DataElement de = item.getKey();
      List<QueryFilter> filters = item.getValue();
      final String deUid = de.getUid();

      final String dataValueValueSql = "ev.eventdatavalues #>> '{" + deUid + ", value}'";

      selectBuilder
          .append(", ")
          .append(
              de.getValueType().isNumeric()
                  ? castToNumber(dataValueValueSql)
                  : lower(dataValueValueSql))
          .append(" as ")
          .append(deUid);

      String optValueTableAs = "opt_" + filterCount;

      if (!joinedColumns.contains(deUid) && de.hasOptionSet() && !filters.isEmpty()) {
        String optSetBind = "optset_" + filterCount;

        mapSqlParameterSource.addValue(optSetBind, de.getOptionSet().getId());

        optionValueJoinBuilder
            .append("inner join optionvalue as ")
            .append(optValueTableAs)
            .append(" on lower(")
            .append(optValueTableAs)
            .append(".code) = ")
            .append("lower(")
            .append(dataValueValueSql)
            .append(") and ")
            .append(optValueTableAs)
            .append(".optionsetid = ")
            .append(":")
            .append(optSetBind)
            .append(" ");

        joinedColumns.add(deUid);
      }

      if (!filters.isEmpty()) {
        for (QueryFilter filter : filters) {
          ++filterCount;

          final String queryCol =
              de.getValueType().isNumeric()
                  ? castToNumber(dataValueValueSql)
                  : lower(dataValueValueSql);

          String bindParameter = "parameter_" + filterCount;
          int itemType = de.getValueType().isNumeric() ? Types.NUMERIC : Types.VARCHAR;

          if (!de.hasOptionSet()) {
            eventDataValuesWhereSql.append(hlp.whereAnd());

            if (QueryOperator.IN.getValue().equalsIgnoreCase(filter.getSqlOperator())) {
              mapSqlParameterSource.addValue(
                  bindParameter,
                  QueryFilter.getFilterItems(StringUtils.lowerCase(filter.getFilter())),
                  itemType);

              eventDataValuesWhereSql.append(inCondition(filter, bindParameter, queryCol));
            } else {
              mapSqlParameterSource.addValue(
                  bindParameter, StringUtils.lowerCase(filter.getSqlBindFilter()), itemType);

              eventDataValuesWhereSql
                  .append(" ")
                  .append(queryCol)
                  .append(" ")
                  .append(filter.getSqlOperator())
                  .append(" ")
                  .append(":")
                  .append(bindParameter)
                  .append(" ");
            }
          } else {
            if (QueryOperator.IN.getValue().equalsIgnoreCase(filter.getSqlOperator())) {
              mapSqlParameterSource.addValue(
                  bindParameter,
                  QueryFilter.getFilterItems(StringUtils.lowerCase(filter.getFilter())),
                  itemType);

              optionValueConditionBuilder.append(" and ");
              optionValueConditionBuilder.append(inCondition(filter, bindParameter, queryCol));
            } else {
              mapSqlParameterSource.addValue(
                  bindParameter, StringUtils.lowerCase(filter.getSqlBindFilter()), itemType);

              optionValueConditionBuilder
                  .append("and lower(")
                  .append(optValueTableAs)
                  .append(DOT_NAME)
                  .append(" ")
                  .append(filter.getSqlOperator())
                  .append(" ")
                  .append(":")
                  .append(bindParameter)
                  .append(" ");
            }
          }
        }
      }
    }

    return optionValueJoinBuilder
        .append(optionValueConditionBuilder)
        .append(eventDataValuesWhereSql)
        .append(" ");
  }

  private String inCondition(QueryFilter filter, String boundParameter, String queryCol) {
    return new StringBuilder()
        .append(" ")
        .append(queryCol)
        .append(" ")
        .append(filter.getSqlOperator())
        .append(" ")
        .append("(")
        .append(":")
        .append(boundParameter)
        .append(") ")
        .toString();
  }

  private String eventStatusSql(
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp) {
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
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource, SqlHelper hlp) {
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
   *       to the above aggregation. IdSchemes SELECT are handled in {@link
   *       #getEventSelectIdentifiersByIdScheme}.
   *   <li>A user must have access to all COs of the events COC to have access to an event.
   * </ul>
   */
  private String getCategoryOptionComboQuery(User user) {
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

    if (!isSuper(user)) {
      joinCondition =
          joinCondition
              + " having bool_and(case when "
              + JpaQueryUtils.generateSQlQueryForSharingCheck(
                  "co.sharing", UserDetails.fromUser(user), AclService.LIKE_READ_DATA)
              + " then true else false end) = True ";
    }

    return joinCondition + ") as coc_agg on coc_agg.id = ev.attributeoptioncomboid ";
  }

  private String getLimitAndOffsetClause(final PageParams pageParams) {
    int pageSize = pageParams.getPageSize();
    int offset = (pageParams.getPage() - 1) * pageParams.getPageSize();
    return " limit " + pageSize + " offset " + offset + " ";
  }

  private String getOrderQuery(EventQueryParams params) {
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
      return "order by " + StringUtils.join(orderFields, ',') + " ";
    } else {
      return "order by " + DEFAULT_ORDER + " ";
    }
  }

  private String getAttributeValueQuery() {
    return """
           select pav.trackedentityid as pav_id, pav.created as pav_created, pav.lastupdated as\
            pav_lastupdated, pav.value as pav_value, ta.uid as ta_uid, ta.name as ta_name,\
            ta.valuetype as ta_valuetype from trackedentityattributevalue pav inner join\
            trackedentityattribute ta on\
            pav.trackedentityattributeid=ta.trackedentityattributeid\s""";
  }

  private boolean isSuper(User user) {
    return user == null || user.isSuper();
  }

  private void setAccessiblePrograms(User user, EventQueryParams params) {
    if (!isSuper(user)) {
      params.setAccessiblePrograms(
          manager.getDataReadAll(Program.class).stream().map(UID::of).collect(Collectors.toSet()));

      params.setAccessibleProgramStages(
          manager.getDataReadAll(ProgramStage.class).stream()
              .map(UID::of)
              .collect(Collectors.toSet()));
    }
  }
}
