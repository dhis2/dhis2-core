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
import static org.hisp.dhis.util.DateUtils.addDays;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonEventDataValueSetBinaryType;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.SqlUtils;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Repository("org.hisp.dhis.tracker.export.event.EventStore")
@RequiredArgsConstructor
class JdbcEventStore implements EventStore {
  private static final String DEFAULT_ORDER = "ev_id desc";

  private static final String RELATIONSHIP_IDS_QUERY =
      " left join (select ri.eventid as ri_ev_id, json_agg(ri.relationshipid) as ev_rl FROM relationshipitem ri"
          + " GROUP by ri_ev_id)  as fgh on fgh.ri_ev_id=event.ev_id ";

  private static final String EVENT_COMMENT_QUERY =
      "select evc.eventid as evc_id,"
          + " evnote.trackedentitycommentid as evnote_id,"
          + " evnote.commenttext            as evnote_value,"
          + " evnote.created                as evnote_storeddate,"
          + " evnote.creator                as evnote_storedby,"
          + " evnote.uid                    as evnote_uid,"
          + " evnote.lastupdated            as evnote_lastupdated,"
          + " userinfo.userinfoid            as evnote_user_id,"
          + " userinfo.code                  as evnote_user_code,"
          + " userinfo.uid                   as evnote_user_uid,"
          + " userinfo.username              as evnote_user_username,"
          + " userinfo.firstname             as evnote_user_firstname,"
          + " userinfo.surname               as evnote_user_surname"
          + " from eventcomments evc"
          + " inner join trackedentitycomment evnote"
          + " on evc.trackedentitycommentid = evnote.trackedentitycommentid"
          + " left join userinfo on evnote.lastupdatedby = userinfo.userinfoid ";

  private static final String EVENT_STATUS = "ev_status";

  private static final String EVENT_STATUS_EQ = " ev.status = ";

  private static final String EVENT_LASTUPDATED_GT = " ev.lastupdated >= ";

  private static final String DOT_NAME = ".name)";

  private static final String SPACE = " ";

  private static final String EQUALS = " = ";

  private static final String AND = " AND ";

  /**
   * Events can be ordered by given fields which correspond to fields on {@link
   * org.hisp.dhis.program.Event}. Maps fields to DB columns.
   */
  private static final Map<String, String> ORDERABLE_FIELDS =
      Map.ofEntries(
          entry("uid", "ev_uid"),
          entry("enrollment.program.uid", "p_uid"),
          entry("programStage.uid", "en_uid"),
          entry("enrollment.uid", "en_uid"),
          entry("enrollment.status", "en_status"),
          entry("enrollment.enrollmentDate", "en_enrollmentdate"),
          entry("organisationUnit.uid", "ou_uid"),
          entry("organisationUnit.name", "ou_name"),
          entry("enrollment.trackedEntity.uid", "tei_uid"),
          entry("executionDate", "ev_executiondate"),
          entry("enrollment.followup", "en_followup"),
          entry("status", EVENT_STATUS),
          entry("dueDate", "ev_duedate"),
          entry("storedBy", "ev_storedby"),
          entry("lastUpdatedBy", "ev_lastupdatedbyuserinfo"),
          entry("createdBy", "ev_createdbyuserinfo"),
          entry("created", "ev_created"),
          entry("lastUpdated", "ev_lastupdated"),
          entry("completedBy", "ev_completedby"),
          entry("attributeOptionCombo.uid", "ev_aoc"),
          entry("completedDate", "ev_completeddate"),
          entry("deleted", "ev_deleted"),
          entry("assignedUser", "user_assigned_username"),
          entry("assignedUser.displayName", "user_assigned_name"));

  private static final String PATH_LIKE = "path LIKE";

  private static final String PATH_EQ = "path =";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  // Cannot use DefaultRenderService mapper. Does not work properly -
  // DHIS2-6102
  private static final ObjectReader eventDataValueJsonReader =
      JsonBinaryType.MAPPER.readerFor(new TypeReference<Map<String, EventDataValue>>() {});

  private final StatementBuilder statementBuilder;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Qualifier("dataValueJsonMapper")
  private final ObjectMapper jsonMapper;

  private final CurrentUserService currentUserService;

  private final IdentifiableObjectManager manager;

  private final RelationshipStore relationshipStore;

  // -------------------------------------------------------------------------
  // EventStore implementation
  // -------------------------------------------------------------------------

  @Override
  public List<Event> getEvents(
      EventQueryParams params, Map<String, Set<String>> psdesWithSkipSyncTrue) {
    User user = currentUserService.getCurrentUser();

    setAccessiblePrograms(user, params);

    Map<String, Event> eventsByUid = new HashMap<>(params.getPageSizeWithDefault());
    List<Event> events = new ArrayList<>();
    List<Long> relationshipIds = new ArrayList<>();

    final Gson gson = new Gson();

    final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    String sql = buildSql(params, mapSqlParameterSource, user);

    return jdbcTemplate.query(
        sql,
        mapSqlParameterSource,
        resultSet -> {
          log.debug("Event query SQL: " + sql);

          Set<String> notes = new HashSet<>();

          while (resultSet.next()) {
            if (resultSet.getString("ev_uid") == null) {
              continue;
            }

            String eventUid = resultSet.getString("ev_uid");

            validateIdentifiersPresence(resultSet, params.getIdSchemes());

            Event event;
            if (eventsByUid.containsKey(eventUid)) {
              event = eventsByUid.get(eventUid);
            } else {
              event = new Event();
              eventsByUid.put(eventUid, event);

              if (!params.isSkipEventId()) {
                event.setUid(eventUid);
              }

              TrackedEntity te = new TrackedEntity();
              te.setUid(resultSet.getString("te_uid"));
              event.setStatus(EventStatus.valueOf(resultSet.getString(EVENT_STATUS)));
              ProgramType programType = ProgramType.fromValue(resultSet.getString("p_type"));
              Program program = new Program();
              program.setUid(resultSet.getString("p_identifier"));
              program.setProgramType(programType);
              Enrollment enrollment = new Enrollment();
              enrollment.setUid(resultSet.getString("en_uid"));
              enrollment.setProgram(program);
              enrollment.setTrackedEntity(te);
              OrganisationUnit ou = new OrganisationUnit();
              ou.setUid(resultSet.getString("ou_uid"));
              ou.setName(resultSet.getString("ou_name"));
              ProgramStage ps = new ProgramStage();
              ps.setUid(resultSet.getString("ps_identifier"));
              event.setDeleted(resultSet.getBoolean("ev_deleted"));

              enrollment.setStatus(ProgramStatus.valueOf(resultSet.getString("en_status")));
              enrollment.setFollowup(resultSet.getBoolean("en_followup"));
              event.setEnrollment(enrollment);
              event.setProgramStage(ps);
              event.setOrganisationUnit(ou);

              CategoryOptionCombo coc = new CategoryOptionCombo();
              coc.setUid(resultSet.getString("coc_identifier"));
              Set<CategoryOption> options =
                  Arrays.stream(resultSet.getString("co_uids").split(";"))
                      .map(
                          optionUid -> {
                            CategoryOption option = new CategoryOption();
                            option.setUid(optionUid);
                            return option;
                          })
                      .collect(Collectors.toSet());
              coc.setCategoryOptions(options);
              event.setAttributeOptionCombo(coc);

              event.setStoredBy(resultSet.getString("ev_storedby"));
              event.setDueDate(resultSet.getTimestamp("ev_duedate"));
              event.setExecutionDate(resultSet.getTimestamp("ev_executiondate"));
              event.setCreated(resultSet.getTimestamp("ev_created"));
              event.setCreatedByUserInfo(
                  EventUtils.jsonToUserInfo(
                      resultSet.getString("ev_createdbyuserinfo"), jsonMapper));
              event.setLastUpdated(resultSet.getTimestamp("ev_lastupdated"));
              event.setLastUpdatedByUserInfo(
                  EventUtils.jsonToUserInfo(
                      resultSet.getString("ev_lastupdatedbyuserinfo"), jsonMapper));

              event.setCompletedBy(resultSet.getString("ev_completedby"));
              event.setCompletedDate(resultSet.getTimestamp("ev_completeddate"));

              if (resultSet.getObject("ev_geometry") != null) {
                try {
                  Geometry geom = new WKTReader().read(resultSet.getString("ev_geometry"));

                  event.setGeometry(geom);
                } catch (ParseException e) {
                  log.error("Unable to read geometry for event '" + event.getUid() + "': ", e);
                }
              }

              if (resultSet.getObject("user_assigned") != null) {
                User eventUser = new User();
                eventUser.setUid(resultSet.getString("user_assigned"));
                eventUser.setUsername(resultSet.getString("user_assigned_username"));
                eventUser.setName(resultSet.getString("user_assigned_name"));
                eventUser.setFirstName(resultSet.getString("user_assigned_first_name"));
                eventUser.setSurname(resultSet.getString("user_assigned_surname"));
                event.setAssignedUser(eventUser);
              }

              if (!StringUtils.isEmpty(resultSet.getString("ev_eventdatavalues"))) {
                Set<EventDataValue> eventDataValues =
                    convertEventDataValueJsonIntoSet(resultSet.getString("ev_eventdatavalues"));

                event.getEventDataValues().addAll(eventDataValues);
              }

              if (params.isIncludeRelationships() && resultSet.getObject("ev_rl") != null) {
                PGobject pGobject = (PGobject) resultSet.getObject("ev_rl");

                if (pGobject != null) {
                  String value = pGobject.getValue();

                  relationshipIds.addAll(Lists.newArrayList(gson.fromJson(value, Long[].class)));
                }
              }

              events.add(event);
            }

            if (resultSet.getString("evnote_value") != null
                && !notes.contains(resultSet.getString("evnote_id"))) {
              TrackedEntityComment note = new TrackedEntityComment();
              note.setUid(resultSet.getString("evnote_uid"));
              note.setCommentText(resultSet.getString("evnote_value"));
              note.setCreated(resultSet.getDate("evnote_storeddate"));
              note.setCreator(resultSet.getString("evnote_storedby"));

              if (resultSet.getObject("evnote_user_id") != null) {
                User noteLastUpdatedBy = new User();
                noteLastUpdatedBy.setId(resultSet.getLong("evnote_user_id"));
                noteLastUpdatedBy.setCode(resultSet.getString("evnote_user_code"));
                noteLastUpdatedBy.setUid(resultSet.getString("evnote_user_uid"));
                noteLastUpdatedBy.setUsername(resultSet.getString("evnote_user_username"));
                noteLastUpdatedBy.setFirstName(resultSet.getString("evnote_user_firstname"));
                noteLastUpdatedBy.setSurname(resultSet.getString("evnote_user_surname"));
                note.setLastUpdatedBy(noteLastUpdatedBy);
              }

              note.setLastUpdated(resultSet.getDate("evnote_lastupdated"));

              event.getComments().add(note);
              notes.add(resultSet.getString("evnote_id"));
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

  @Override
  public Set<String> getOrderableFields() {
    return ORDERABLE_FIELDS.keySet();
  }

  private String getIdSqlBasedOnIdScheme(
      IdScheme idScheme, String uidSql, String attributeSql, String codeSql) {
    if (idScheme == IdScheme.ID || idScheme == IdScheme.UID) {
      return uidSql;
    } else if (idScheme.isAttribute()) {
      return String.format(attributeSql, idScheme.getAttribute());
    } else {
      return codeSql;
    }
  }

  private void validateIdentifiersPresence(ResultSet rowSet, IdSchemes idSchemes)
      throws SQLException {

    if (StringUtils.isEmpty(rowSet.getString("p_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "Program %s does not have a value assigned for idScheme %s",
              rowSet.getString("p_uid"), idSchemes.getProgramIdScheme().name()));
    }

    if (StringUtils.isEmpty(rowSet.getString("ps_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "ProgramStage %s does not have a value assigned for idScheme %s",
              rowSet.getString("ps_uid"), idSchemes.getProgramStageIdScheme().name()));
    }

    if (StringUtils.isEmpty(rowSet.getString("ou_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "OrgUnit %s does not have a value assigned for idScheme %s",
              rowSet.getString("ou_uid"), idSchemes.getOrgUnitIdScheme().name()));
    }

    if (StringUtils.isEmpty(rowSet.getString("coc_identifier"))) {
      throw new IllegalStateException(
          String.format(
              "CategoryOptionCombo %s does not have a value assigned for idScheme %s",
              rowSet.getString("coc_uid"), idSchemes.getCategoryOptionComboIdScheme().name()));
    }
  }

  private String getEventSelectIdentifiersByIdScheme(EventQueryParams params) {
    IdSchemes idSchemes = params.getIdSchemes();

    StringBuilder sqlBuilder = new StringBuilder();

    String ouTableName = getOuTableName(params);

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getOrgUnitIdScheme(),
            ouTableName + ".uid as ou_identifier, ",
            ouTableName + ".attributevalues #>> '{%s, value}' as ou_identifier, ",
            ouTableName + ".code as ou_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getProgramIdScheme(),
            "p.uid as p_identifier, ",
            "p.attributevalues #>> '{%s, value}' as p_identifier, ",
            "p.code as p_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getProgramStageIdScheme(),
            "ps.uid as ps_identifier, ",
            "ps.attributevalues #>> '{%s, value}' as ps_identifier, ",
            "ps.code as ps_identifier, "));

    sqlBuilder.append(
        getIdSqlBasedOnIdScheme(
            idSchemes.getCategoryOptionComboIdScheme(),
            "coc.uid as coc_identifier, ",
            "coc.attributevalues #>> '{%s, value}' as coc_identifier, ",
            "coc.code as coc_identifier, "));

    return sqlBuilder.toString();
  }

  @Override
  public int getEventCount(EventQueryParams params) {
    User user = currentUserService.getCurrentUser();
    setAccessiblePrograms(user, params);

    String sql;

    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

    sql = getEventSelectQuery(params, mapSqlParameterSource, user);

    sql = sql.replaceFirst("select .*? from", "select count(*) from");

    sql = sql.replaceFirst("order .*? (desc|asc)", "");

    sql = sql.replaceFirst("limit \\d+ offset \\d+", "");

    log.debug("Event query count SQL: " + sql);

    return jdbcTemplate.queryForObject(sql, mapSqlParameterSource, Integer.class);
  }

  /**
   * Query is based on three sub queries on event, data value and comment, which are joined using
   * program stage instance id. The purpose of the separate queries is to be able to page properly
   * on events.
   */
  private String buildSql(
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource, User user) {
    StringBuilder sqlBuilder = new StringBuilder().append("select * from (");

    sqlBuilder.append(getEventSelectQuery(params, mapSqlParameterSource, user));

    sqlBuilder.append(getOrderQuery(params));

    sqlBuilder.append(getEventPagingQuery(params));

    sqlBuilder.append(") as event left join (");

    if (params.isIncludeAttributes()) {
      sqlBuilder.append(getAttributeValueQuery());

      sqlBuilder.append(") as att on event.te_id=att.pav_id left join (");
    }

    sqlBuilder.append(EVENT_COMMENT_QUERY);

    sqlBuilder.append(") as cm on event.ev_id=cm.evc_id ");

    if (params.isIncludeRelationships()) {
      sqlBuilder.append(RELATIONSHIP_IDS_QUERY);
    }

    sqlBuilder.append(getOrderQuery(params));

    return sqlBuilder.toString();
  }

  /**
   * Generates a single INNER JOIN for each attribute we are searching on. We can search by a range
   * of operators. All searching is using lower() since attribute values are case-insensitive.
   */
  private void joinAttributeValueWithoutQueryParameter(
      StringBuilder sql, Map<TrackedEntityAttribute, List<QueryFilter>> attributes) {
    for (Entry<TrackedEntityAttribute, List<QueryFilter>> queryItem : attributes.entrySet()) {
      TrackedEntityAttribute tea = queryItem.getKey();
      String teaUid = tea.getUid();
      String teaValueCol = statementBuilder.columnQuote(teaUid);
      String teaCol = statementBuilder.columnQuote(teaUid + "ATT");

      sql.append(" INNER JOIN trackedentityattributevalue ")
          .append(teaValueCol)
          .append(" ON ")
          .append(teaValueCol + ".trackedentityid")
          .append(" = TE.trackedentityid ")
          .append(" INNER JOIN trackedentityattribute ")
          .append(teaCol)
          .append(" ON ")
          .append(teaValueCol + ".trackedentityattributeid")
          .append(EQUALS)
          .append(teaCol + ".trackedentityattributeid")
          .append(AND)
          .append(teaCol + ".UID")
          .append(EQUALS)
          .append(statementBuilder.encode(teaUid, true));

      sql.append(
          getAttributeFilterQuery(
              queryItem.getValue(), teaCol, teaValueCol, tea.getValueType().isNumeric()));
    }
  }

  private String getAttributeFilterQuery(
      List<QueryFilter> filters, String teaCol, String teaValueCol, boolean isNumericTea) {

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
          .append(" CASE WHEN ")
          .append(lower(teaCol + ".valueType"))
          .append(" in (")
          .append(
              NUMERIC_TYPES.stream()
                  .map(Enum::name)
                  .map(StringUtils::lowerCase)
                  .map(SqlUtils::singleQuote)
                  .collect(Collectors.joining(",")))
          .append(")")
          .append(" THEN ");
    }

    List<String> filterStrings = new ArrayList<>();
    for (QueryFilter filter : filters) {
      StringBuilder filterString = new StringBuilder();
      final String queryCol =
          isNumericTea ? castToNumber(teaValueCol + ".value") : lower(teaValueCol + ".value");
      final Object encodedFilter =
          isNumericTea
              ? Double.valueOf(filter.getFilter())
              : StringUtils.lowerCase(filter.getSqlFilter(filter.getFilter()));
      filterString
          .append(queryCol)
          .append(SPACE)
          .append(filter.getSqlOperator())
          .append(SPACE)
          .append(encodedFilter);
      filterStrings.add(filterString.toString());
    }
    query.append(String.join(AND, filterStrings));

    if (isNumericTea) {
      query.append(" END ");
    }

    return query.toString();
  }

  private String getEventSelectQuery(
      EventQueryParams params, MapSqlParameterSource mapSqlParameterSource, User user) {
    SqlHelper hlp = new SqlHelper();

    StringBuilder selectBuilder =
        new StringBuilder()
            .append("select ")
            .append(getEventSelectIdentifiersByIdScheme(params))
            .append(" ev.uid as ev_uid, ")
            .append("ou.uid as ou_uid, p.uid as p_uid, ps.uid as ps_uid, ")
            .append(
                "ev.eventid as ev_id, ev.status as ev_status, ev.executiondate as ev_executiondate, ")
            .append(
                "ev.eventdatavalues as ev_eventdatavalues, ev.duedate as ev_duedate, ev.completedby as ev_completedby, ev.storedby as ev_storedby, ")
            .append(
                "ev.created as ev_created, ev.createdbyuserinfo as ev_createdbyuserinfo, ev.lastupdated as ev_lastupdated, ev.lastupdatedbyuserinfo as ev_lastupdatedbyuserinfo, ")
            .append("ev.completeddate as ev_completeddate, ev.deleted as ev_deleted, ")
            .append(
                "ST_AsText( ev.geometry ) as ev_geometry, au.uid as user_assigned, (au.firstName || ' ' || au.surName) as user_assigned_name,")
            .append(
                "au.firstName as user_assigned_first_name, au.surName as user_assigned_surname, ")
            .append("au.username as user_assigned_username,")
            .append("coc.uid as coc_uid, ")
            .append("coc_agg.co_uids AS co_uids, ")
            .append("coc_agg.co_count AS option_size, ");

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
            "en.uid as en_uid, en.status as en_status, en.followup as en_followup, en.enrollmentdate as en_enrollmentdate, en.incidentdate as en_incidentdate, ")
        .append("p.type as p_type, ps.uid as ps_uid, ou.name as ou_name, ")
        .append(
            "te.trackedentityid as te_id, te.uid as te_uid, teou.uid as te_ou, teou.name as te_ou_name, te.created as te_created, te.inactive as te_inactive ")
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

  private String getOuTableName(EventQueryParams params) {
    return checkForOwnership(params) ? " evou" : " ou";
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
              "inner join organisationunit evou on (coalesce(po.organisationunitid, ev.organisationunitid)=evou.organisationunitid) ")
          .append(
              "inner join organisationunit ou on (ev.organisationunitid=ou.organisationunitid) ");
    } else {
      fromBuilder.append(
          "inner join organisationunit ou on ev.organisationunitid=ou.organisationunitid ");
    }

    fromBuilder
        .append("left join trackedentity te on te.trackedentityid=en.trackedentityid ")
        .append(
            "left join organisationunit teou on (te.organisationunitid=teou.organisationunitid) ")
        .append("left join userinfo au on (ev.assigneduserid=au.userinfoid) ");

    if (!params.getAttributes().isEmpty()) {
      joinAttributeValueWithoutQueryParameter(fromBuilder, params.getAttributes());
    }

    fromBuilder.append(getCategoryOptionComboQuery(user));

    fromBuilder.append(dataElementAndFiltersSql);

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

    if (params.getProgramStatus() != null) {
      mapSqlParameterSource.addValue("program_status", params.getProgramStatus().name());

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
          .append(" (en.incidentdate <= :enrollmentOccurredBefore ) ");
    }

    if (params.getEnrollmentOccurredAfter() != null) {
      mapSqlParameterSource.addValue(
          "enrollmentOccurredAfter", params.getEnrollmentOccurredAfter(), Types.TIMESTAMP);
      fromBuilder.append(hlp.whereAnd()).append(" (en.incidentdate >= :enrollmentOccurredAfter ) ");
    }

    if (params.getScheduleAtStartDate() != null) {
      mapSqlParameterSource.addValue(
          "startDueDate", params.getScheduleAtStartDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.duedate is not null and ev.duedate >= :startDueDate ) ");
    }

    if (params.getScheduleAtEndDate() != null) {
      mapSqlParameterSource.addValue("endDueDate", params.getScheduleAtEndDate(), Types.TIMESTAMP);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.duedate is not null and ev.duedate <= :endDueDate ) ");
    }

    if (params.getFollowUp() != null) {
      fromBuilder
          .append(hlp.whereAnd())
          .append(" en.followup is ")
          .append(Boolean.TRUE.equals(params.getFollowUp()) ? "true" : "false")
          .append(" ");
    }

    fromBuilder.append(addLastUpdatedFilters(params, mapSqlParameterSource, hlp));

    // Comparing milliseconds instead of always creating new Date( 0 );
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

    String orgUnitSql = getOrgUnitSql(params, getOuTableName(params));

    if (!Strings.isNullOrEmpty(orgUnitSql)) {
      fromBuilder.append(hlp.whereAnd()).append(" (").append(orgUnitSql).append(") ");
    }

    if (params.getStartDate() != null) {
      mapSqlParameterSource.addValue("startDate", params.getStartDate(), Types.DATE);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.executiondate >= ")
          .append(":startDate")
          .append(" or (ev.executiondate is null and ev.duedate >= ")
          .append(":startDate")
          .append(" )) ");
    }

    if (params.getEndDate() != null) {
      mapSqlParameterSource.addValue("endDate", addDays(params.getEndDate(), 1), Types.DATE);

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (ev.executiondate < ")
          .append(":endDate")
          .append(" or (ev.executiondate is null and ev.duedate < ")
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
      mapSqlParameterSource.addValue("ev_uid", params.getEvents());

      fromBuilder.append(hlp.whereAnd()).append(" (ev.uid in (").append(":ev_uid").append(")) ");
    }

    if (params.getAssignedUserQueryParam().hasAssignedUsers()) {
      mapSqlParameterSource.addValue(
          "au_uid", params.getAssignedUserQueryParam().getAssignedUsers());

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
          params.getAccessiblePrograms().isEmpty() ? null : params.getAccessiblePrograms());

      fromBuilder
          .append(hlp.whereAnd())
          .append(" (p.uid in (")
          .append(":program_uid")
          .append(")) ");

      mapSqlParameterSource.addValue(
          "programstage_uid",
          params.getAccessibleProgramStages().isEmpty()
              ? null
              : params.getAccessibleProgramStages());

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
      mapSqlParameterSource.addValue("enrollment_uid", params.getEnrollments());

      fromBuilder.append(hlp.whereAnd()).append(" (en.uid in (:enrollment_uid)) ");
    }

    return fromBuilder;
  }

  private String getOrgUnitSql(EventQueryParams params, String ouTable) {
    return switch (params.getOrgUnitMode()) {
      case SELECTED -> getSelectedOrgUnitPath(params.getAccessibleOrgUnits(), ouTable);
      case CHILDREN -> getChildrenOrgUnitsPath(params.getAccessibleOrgUnits(), ouTable);
      case ALL -> null;
      default -> getOrgUnitsPath(params.getAccessibleOrgUnits(), ouTable);
    };
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
        mapSqlParameterSource.addValue(EVENT_STATUS, EventStatus.ACTIVE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(EVENT_STATUS_EQ)
            .append(":" + EVENT_STATUS)
            .append(" and ev.executiondate is not null ");
      } else if (params.getEventStatus() == EventStatus.OVERDUE) {
        mapSqlParameterSource.addValue(EVENT_STATUS, EventStatus.SCHEDULE.name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(" date(now()) > date(ev.duedate) and ev.status = ")
            .append(":" + EVENT_STATUS)
            .append(" ");
      } else {
        mapSqlParameterSource.addValue(EVENT_STATUS, params.getEventStatus().name());

        stringBuilder
            .append(hlp.whereAnd())
            .append(EVENT_STATUS_EQ)
            .append(":" + EVENT_STATUS)
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
            "lastUpdatedEnd", addDays(params.getUpdatedAtEndDate(), 1), Types.TIMESTAMP);

        sqlBuilder
            .append(hlp.whereAnd())
            .append(" ev.lastupdated < ")
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
        "inner join categoryoptioncombo coc on coc.categoryoptioncomboid = ev.attributeoptioncomboid "
            + " inner join (select coc.categoryoptioncomboid as id,"
            + " string_agg(co.uid, ';') as co_uids, count(co.categoryoptionid) as co_count"
            + " from categoryoptioncombo coc "
            + " inner join categoryoptioncombos_categoryoptions cocco on coc.categoryoptioncomboid = cocco.categoryoptioncomboid"
            + " inner join dataelementcategoryoption co on cocco.categoryoptionid = co.categoryoptionid"
            + " group by coc.categoryoptioncomboid ";

    if (!isSuper(user)) {
      joinCondition =
          joinCondition
              + " having bool_and(case when "
              + JpaQueryUtils.generateSQlQueryForSharingCheck(
                  "co.sharing", user, AclService.LIKE_READ_DATA)
              + " then true else false end) = True ";
    }

    return joinCondition + ") as coc_agg on coc_agg.id = ev.attributeoptioncomboid ";
  }

  private String getEventPagingQuery(final EventQueryParams params) {
    final StringBuilder sqlBuilder = new StringBuilder().append(" ");
    int pageSize = params.getPageSizeWithDefault();

    // When the clients choose to not show the total of pages.
    if (!params.isTotalPages()) {
      // Get pageSize + 1, so we are able to know if there is another
      // page available. It adds one additional element into the list,
      // as consequence. The caller needs to remove the last element.
      pageSize++;
    }

    if (!params.isSkipPaging()) {
      sqlBuilder
          .append("limit ")
          .append(pageSize)
          .append(" offset ")
          .append(params.getOffset())
          .append(" ");
    }

    return sqlBuilder.toString();
  }

  private String getOrderQuery(EventQueryParams params) {
    ArrayList<String> orderFields = new ArrayList<>();

    for (Order order : params.getOrder()) {
      if (order.getField() instanceof String field) {
        if (!ORDERABLE_FIELDS.containsKey(field)) {
          throw new IllegalArgumentException(
              String.format(
                  "Cannot order by '%s'. Supported are data elements, tracked entity attributes and fields '%s'.",
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
                "Cannot order by '%s'. Supported are data elements, tracked entity attributes and fields '%s'.",
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
    return "select pav.trackedentityid as pav_id, pav.created as pav_created, pav.lastupdated as pav_lastupdated, "
        + "pav.value as pav_value, ta.uid as ta_uid, ta.name as ta_name, ta.valuetype as ta_valuetype "
        + "from trackedentityattributevalue pav "
        + "inner join trackedentityattribute ta on pav.trackedentityattributeid=ta.trackedentityattributeid ";
  }

  private boolean isSuper(User user) {
    return user == null || user.isSuper();
  }

  private Set<EventDataValue> convertEventDataValueJsonIntoSet(String jsonString) {
    try {
      Map<String, EventDataValue> data = eventDataValueJsonReader.readValue(jsonString);
      return JsonEventDataValueSetBinaryType.convertEventDataValuesMapIntoSet(data);
    } catch (IOException e) {
      log.error("Parsing EventDataValues json string failed. String value: " + jsonString);
      throw new IllegalArgumentException(e);
    }
  }

  private void setAccessiblePrograms(User user, EventQueryParams params) {
    if (!isSuper(user)) {
      params.setAccessiblePrograms(
          manager.getDataReadAll(Program.class).stream()
              .map(Program::getUid)
              .collect(Collectors.toSet()));

      params.setAccessibleProgramStages(
          manager.getDataReadAll(ProgramStage.class).stream()
              .map(ProgramStage::getUid)
              .collect(Collectors.toSet()));
    }
  }

  private String getChildrenOrgUnitsPath(List<OrganisationUnit> orgUnits, String ouTable) {
    StringJoiner orgUnitSqlJoiner = new StringJoiner(" or ");

    for (OrganisationUnit orgUnit : orgUnits) {
      orgUnitSqlJoiner.add(
          ouTable
              + "."
              + PATH_LIKE
              + " '"
              + orgUnit.getPath()
              + "%' "
              + " and "
              + ouTable
              + "."
              + "hierarchylevel = "
              + orgUnit.getLevel());
    }

    return orgUnitSqlJoiner.toString();
  }

  private String getSelectedOrgUnitPath(List<OrganisationUnit> orgUnits, String ouTable) {
    return orgUnits.isEmpty()
        ? null
        : ouTable + "." + PATH_EQ + " '" + orgUnits.get(0).getPath() + "' ";
  }

  private String getOrgUnitsPath(List<OrganisationUnit> orgUnits, String ouTable) {
    StringJoiner orgUnitSqlJoiner = new StringJoiner(" or ");

    for (OrganisationUnit orgUnit : orgUnits) {
      orgUnitSqlJoiner.add(ouTable + "." + PATH_LIKE + " '" + orgUnit.getPath() + "%' ");
    }

    return orgUnitSqlJoiner.toString();
  }
}
