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
package org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Relationship;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.AbstractMapper;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.RelationshipRowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Luciano Fiandesio
 */
public abstract class AbstractStore {
  protected final int PARITITION_SIZE = 20000;

  protected final NamedParameterJdbcTemplate jdbcTemplate;

  private static final String GET_RELATIONSHIP_ID_BY_ENTITY_ID_SQL =
      "select ri.%s as id, r.relationshipid "
          + "FROM relationshipitem ri left join relationship r on ri.relationshipid = r.relationshipid "
          + "where ri.%s in (:ids)";

  private static final String GET_RELATIONSHIP_BY_RELATIONSHIP_ID =
      "select "
          + "r.uid as rel_uid, r.created, r.lastupdated, rst.name as reltype_name, rst.uid as reltype_uid, rst.bidirectional as reltype_bi, "
          + "coalesce((select 'tei|' || tei.uid from trackedentityinstance tei "
          + "join relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid "
          + "where ri.relationshipitemid = r.to_relationshipitemid) , (select 'pi|' || pi.uid "
          + "from programinstance pi "
          + "join relationshipitem ri on pi.programinstanceid = ri.programinstanceid "
          + "where ri.relationshipitemid = r.to_relationshipitemid), (select 'psi|' || psi.uid "
          + "from event psi "
          + "join relationshipitem ri on psi.eventid = ri.eventid "
          + "where ri.relationshipitemid = r.to_relationshipitemid)) to_uid, "
          + "coalesce((select 'tei|' || tei.uid from trackedentityinstance tei "
          + "join relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid "
          + "where ri.relationshipitemid = r.from_relationshipitemid) , (select 'pi|' || pi.uid "
          + "from programinstance pi "
          + "join relationshipitem ri on pi.programinstanceid = ri.programinstanceid "
          + "where ri.relationshipitemid = r.from_relationshipitemid), (select 'psi|' || psi.uid "
          + "from event psi "
          + "join relationshipitem ri on psi.eventid = ri.eventid "
          + "where ri.relationshipitemid = r.from_relationshipitemid)) from_uid "
          + "from relationship r join relationshiptype rst on r.relationshiptypeid = rst.relationshiptypeid "
          + "where r.relationshipid in (:ids)";

  public AbstractStore(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
  }

  MapSqlParameterSource createIdsParam(List<Long> ids) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", ids);
    return parameters;
  }

  MapSqlParameterSource createIdsParam(List<Long> ids, Long userId) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("ids", ids);
    parameters.addValue("userId", userId);
    return parameters;
  }

  public Multimap<String, Relationship> getRelationships(List<Long> ids, AggregateContext ctx) {
    List<List<Long>> partitionedIds = Lists.partition(ids, PARITITION_SIZE);

    Multimap<String, Relationship> relationshipMultimap = ArrayListMultimap.create();

    partitionedIds.forEach(
        partition -> relationshipMultimap.putAll(getRelationshipsPartitioned(partition, ctx)));

    return relationshipMultimap;
  }

  private Multimap<String, Relationship> getRelationshipsPartitioned(
      List<Long> ids, AggregateContext ctx) {
    StringBuilder getRelationshipsHavingIdSQL =
        new StringBuilder(
            String.format(
                GET_RELATIONSHIP_ID_BY_ENTITY_ID_SQL,
                getRelationshipEntityColumn(),
                getRelationshipEntityColumn()));

    if (!ctx.getParams().isIncludeDeleted()) {
      getRelationshipsHavingIdSQL.append(" AND r.deleted is false");
    }
    // Get all the relationship ids that have at least one relationship item
    // having
    // the ids in the tei|pi|psi column (depending on the subclass)

    List<Long> relationshipIds =
        getRelationshipIds(getRelationshipsHavingIdSQL.toString(), createIdsParam(ids));

    if (!relationshipIds.isEmpty()) {
      RelationshipRowCallbackHandler handler = new RelationshipRowCallbackHandler();
      jdbcTemplate.query(
          GET_RELATIONSHIP_BY_RELATIONSHIP_ID, createIdsParam(relationshipIds), handler);
      return handler.getItems();
    }
    return ArrayListMultimap.create();
  }

  public Multimap<String, Relationship> getRelationshipsByIds(
      List<Long> ids, EventSearchParams params) {
    List<List<Long>> partitionedIds = Lists.partition(ids, PARITITION_SIZE);

    Multimap<String, Relationship> relationshipMultimap = ArrayListMultimap.create();

    partitionedIds.forEach(
        partition ->
            relationshipMultimap.putAll(getRelationshipsByIdsPartitioned(partition, params)));

    return relationshipMultimap;
  }

  private Multimap<String, Relationship> getRelationshipsByIdsPartitioned(
      List<Long> ids, EventSearchParams params) {
    if (!ids.isEmpty()) {
      RelationshipRowCallbackHandler handler = new RelationshipRowCallbackHandler();
      StringBuilder query = new StringBuilder(GET_RELATIONSHIP_BY_RELATIONSHIP_ID);
      if (!params.isIncludeDeleted()) {
        query.append(" AND r.deleted is false");
      }
      jdbcTemplate.query(query.toString(), createIdsParam(ids), handler);
      return handler.getItems();
    }
    return ArrayListMultimap.create();
  }

  abstract String getRelationshipEntityColumn();

  /**
   * @param sql an sql statement to which we want to "attach" the ACL sharing and the include delete
   *     condition
   * @param ctx the {@see AggregateContext} object containing information about the inclusion of
   *     deleted records and the current user
   * @param aclSql the sql statement as WHERE condition to filter out elements for which the user
   *     has no sharing access
   * @param deletedSql the sql statement as WHERE condition to filter out elements that are soft
   *     deleted
   * @return a merge between the sql, the aclSql and the include delete condition
   */
  protected String getQuery(String sql, AggregateContext ctx, String aclSql, String deletedSql) {
    if (!ctx.isSuperUser()) {
      sql = sql + " AND " + aclSql;
    }
    if (!ctx.getQueryParams().isIncludeDeleted()) {
      sql = sql + " AND " + deletedSql;
    }
    return sql;
  }

  protected String applySortOrder(String sql, String sortOrderIds, String idColumn) {
    StringBuilder qb = new StringBuilder();
    qb.append("select * from (");
    qb.append(sql);
    qb.append(") as t JOIN unnest('{");
    qb.append(sortOrderIds);
    qb.append("}'::bigint[]) WITH ORDINALITY s(");
    qb.append(idColumn);
    qb.append(", sortorder) USING (");
    qb.append(idColumn);
    qb.append(")ORDER  BY s.sortorder");
    return qb.toString();
  }

  /**
   * Execute a SELECT statement and maps the results to the specified Mapper
   *
   * @param sql The SELECT statement to execute
   * @param handler the {@see RowCallbackHandler} to use for mapping a Resultset to an object
   * @param ids the list of primary keys mapped to the :ids parameter
   * @return a Multimap where the keys are of the same type as the specified {@see
   *     RowCallbackHandler}
   */
  protected <T> Multimap<String, T> fetch(String sql, AbstractMapper<T> handler, List<Long> ids) {
    List<List<Long>> idPartitions = Lists.partition(ids, PARITITION_SIZE);

    Multimap<String, T> multimap = ArrayListMultimap.create();

    idPartitions.forEach(partition -> multimap.putAll(fetchPartitioned(sql, handler, partition)));
    return multimap;
  }

  private <T> Multimap<String, T> fetchPartitioned(
      String sql, AbstractMapper<T> handler, List<Long> ids) {
    jdbcTemplate.query(sql, createIdsParam(ids), handler);
    return handler.getItems();
  }

  protected static String buildSelect(Map<String, TableColumn> columnMap) {
    return "SELECT "
        + columnMap.values().stream()
            .map(TableColumn::useInSelect)
            .collect(Collectors.joining(", "))
        + " ";
  }

  private List<Long> getRelationshipIds(String sql, MapSqlParameterSource parameterSource) {
    List<Map<String, Object>> relationshipIdsList = jdbcTemplate.queryForList(sql, parameterSource);

    List<Long> relationshipIds = new ArrayList<>();
    for (Map<String, Object> relationshipIdsMap : relationshipIdsList) {
      relationshipIds.add((Long) relationshipIdsMap.get("relationshipid"));
    }

    return relationshipIds;
  }
}
