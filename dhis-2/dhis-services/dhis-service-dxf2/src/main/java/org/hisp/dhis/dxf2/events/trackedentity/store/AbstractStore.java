/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.events.trackedentity.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.AbstractMapper;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.RelationshipRowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
public abstract class AbstractStore
{
    protected final NamedParameterJdbcTemplate jdbcTemplate;

    private final static String GET_RELATIONSHIP_ID_BY_ENTITYTYPE_SQL = "select ri.%s as id, r.relationshipid "
        + "FROM relationshipitem ri left join relationship r on ri.relationshipid = r.relationshipid "
        + "where ri.%s in (:ids)";

    private final static String GET_RELATIONSHIP_SQL = "select "
        + "r.uid as rel_uid, r.created, r.lastupdated, rst.name as reltype_name, rst.uid as reltype_uid, rst.bidirectional as reltype_bi, "
        + "coalesce((select 'tei|' || tei.uid from trackedentityinstance tei "
        + "join relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid "
        + "where ri.relationshipitemid = r.to_relationshipitemid) , (select 'pi|' || pi.uid "
        + "from programinstance pi "
        + "join relationshipitem ri on pi.programinstanceid = ri.programinstanceid "
        + "where ri.relationshipitemid = r.to_relationshipitemid), (select 'psi|' || psi.uid "
        + "from programstageinstance psi "
        + "join relationshipitem ri on psi.programstageinstanceid = ri.programstageinstanceid "
        + "where ri.relationshipitemid = r.to_relationshipitemid)) to_uid, "
        + "coalesce((select 'tei|' || tei.uid from trackedentityinstance tei "
        + "join relationshipitem ri on tei.trackedentityinstanceid = ri.trackedentityinstanceid "
        + "where ri.relationshipitemid = r.from_relationshipitemid) , (select 'pi|' || pi.uid "
        + "from programinstance pi "
        + "join relationshipitem ri on pi.programinstanceid = ri.programinstanceid "
        + "where ri.relationshipitemid = r.from_relationshipitemid), (select 'psi|' || psi.uid "
        + "from programstageinstance psi "
        + "join relationshipitem ri on psi.programstageinstanceid = ri.programstageinstanceid "
        + "where ri.relationshipitemid = r.from_relationshipitemid)) from_uid "
        + "from relationship r join relationshiptype rst on r.relationshiptypeid = rst.relationshiptypeid "
        + "where r.relationshipid in (:ids)";

    public AbstractStore( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    MapSqlParameterSource createIdsParam( List<Long> ids )
    {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", ids );
        return parameters;
    }

    MapSqlParameterSource createIdsParam( List<Long> ids, Long userId )
    {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", ids );
        parameters.addValue( "userId", userId );
        return parameters;
    }

    public Multimap<String, Relationship> getRelationships( List<Long> ids )
    {
        String getRelationshipsHavingIdSQL = String.format( GET_RELATIONSHIP_ID_BY_ENTITYTYPE_SQL,
            getRelationshipEntityColumn(), getRelationshipEntityColumn() );

        // Get all the relationship ids that have at least one relationship item
        // having
        // the ids in the tei|pi|psi column (depending on the subclass)

        List<Map<String, Object>> relationshipIdsList = jdbcTemplate.queryForList( getRelationshipsHavingIdSQL,
            createIdsParam( ids ) );

        List<Long> relationshipIds = new ArrayList<>();
        for ( Map<String, Object> relationshipIdsMap : relationshipIdsList )
        {
            relationshipIds.add( (Long) relationshipIdsMap.get( "relationshipid" ) );
        }

        if ( !relationshipIds.isEmpty() )
        {
            RelationshipRowCallbackHandler handler = new RelationshipRowCallbackHandler();
            jdbcTemplate.query( GET_RELATIONSHIP_SQL, createIdsParam( relationshipIds ), handler );
            return handler.getItems();
        }
        return ArrayListMultimap.create();
    }

    abstract String getRelationshipEntityColumn();

    /**
     * @param sql an sql statement to which we want to "attach" the ACL sharing
     *        condition
     * @param ctx the {@see AAggregateContext} object containing information
     *        about the current user
     * @param aclSql the sql statement as WHERE condition to filter out elements
     *        for which the user has no sharing access
     * @return a merge between the sql and the aclSql
     */
    protected String withAclCheck( String sql, AggregateContext ctx, String aclSql )
    {
        return ctx.isSuperUser() ? sql : sql + " AND " + aclSql;
    }

    protected String applySortOrder( String sql, String sortOrderIds, String idColumn )
    {
        StringBuilder qb = new StringBuilder();
        qb.append( "select * from (" );
        qb.append( sql );
        qb.append( ") as t JOIN unnest('{" );
        qb.append( sortOrderIds );
        qb.append( "}'::bigint[]) WITH ORDINALITY s(" );
        qb.append( idColumn );
        qb.append( ", sortorder) USING (" );
        qb.append( idColumn );
        qb.append( ")ORDER  BY s.sortorder" );
        return qb.toString();
    }

    /**
     * Execute a SELECT statement and maps the results to the specified Mapper
     *
     * @param sql The SELECT statement to execute
     * @param handler the {@see RowCallbackHandler} to use for mapping a
     *        Resultset to an object
     * @param ids the list of primary keys mapped to the :ids parameter
     *
     * @return a Multimap where the keys are of the same type as the specified
     *         {@see RowCallbackHandler}
     */
    protected <T> Multimap<String, T> fetch( String sql, AbstractMapper<T> handler, List<Long> ids )
    {
        jdbcTemplate.query( sql, createIdsParam( ids ), handler );
        return handler.getItems();
    }

    protected static String buildSelect( Map<String, TableColumn> columnMap )
    {
        return "SELECT "
            + columnMap.values().stream().map( TableColumn::useInSelect ).collect( Collectors.joining( ", " ) ) + " ";
    }
}
