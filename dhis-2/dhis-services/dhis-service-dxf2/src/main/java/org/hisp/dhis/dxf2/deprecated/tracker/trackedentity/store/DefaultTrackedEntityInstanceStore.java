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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.Attribute;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.ProgramOwner;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityProgramOwnerIds;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.OwnedTeiMapper;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.ProgramOwnerRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.TrackedEntityAttributeRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.TrackedEntityInstanceRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.TeiAttributeQuery;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.TrackedEntityInstanceQuery;
import org.hisp.dhis.trackedentity.TrackedEntityOuInfo;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 * @author Ameen Mohamed
 */
@Repository
public class DefaultTrackedEntityInstanceStore extends AbstractStore implements TrackedEntityInstanceStore
{
    private static final String GET_TEIS_SQL = TrackedEntityInstanceQuery.getQuery();

    private static final String GET_TEI_ATTRIBUTES = TeiAttributeQuery.getQuery();

    private static final String GET_PROGRAM_OWNERS = "select tei.uid as key, p.uid as prguid, o.uid as ouuid " +
        "from trackedentityprogramowner teop " +
        "join program p on teop.programid = p.programid " +
        "join organisationunit o on teop.organisationunitid = o.organisationunitid " +
        "join trackedentityinstance tei on teop.trackedentityinstanceid = tei.trackedentityinstanceid " +
        "where teop.trackedentityinstanceid in (:ids)";

    private static final String GET_OWNERSHIP_DATA_FOR_TEIS_FOR_ALL_PROGRAM = "SELECT tei.uid as tei_uid,tpo.trackedentityinstanceid, tpo.programid, tpo.organisationunitid, p.accesslevel,p.uid as pgm_uid "
        +
        "FROM trackedentityprogramowner TPO " +
        "LEFT JOIN program P on P.programid = TPO.programid " +
        "LEFT JOIN organisationunit OU on OU.organisationunitid = TPO.organisationunitid " +
        "LEFT JOIN trackedentityinstance TEI on TEI.trackedentityinstanceid = tpo.trackedentityinstanceid " +
        "WHERE TPO.trackedentityinstanceid in (:ids) " +
        "AND p.programid in (SELECT programid FROM program) " +
        "GROUP BY tei.uid,tpo.trackedentityinstanceid, tpo.programid, tpo.organisationunitid, ou.path, p.accesslevel,p.uid "
        +
        "HAVING (P.accesslevel in ('OPEN', 'AUDITED') AND (EXISTS(SELECT SS.organisationunitid FROM userteisearchorgunits SS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = SS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')) OR EXISTS(SELECT CS.organisationunitid FROM usermembership CS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = CS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')))) "
        +
        "OR (P.accesslevel in ('CLOSED', 'PROTECTED') AND EXISTS(SELECT CS.organisationunitid FROM usermembership CS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = CS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')));";

    private static final String GET_OWNERSHIP_DATA_FOR_TEIS_FOR_SPECIFIC_PROGRAM = "SELECT tei.uid as tei_uid,tpo.trackedentityinstanceid, tpo.programid, tpo.organisationunitid, p.accesslevel,p.uid as pgm_uid "
        +
        "FROM trackedentityprogramowner TPO " +
        "LEFT JOIN program P on P.programid = TPO.programid " +
        "LEFT JOIN organisationunit OU on OU.organisationunitid = TPO.organisationunitid " +
        "LEFT JOIN trackedentityinstance TEI on TEI.trackedentityinstanceid = tpo.trackedentityinstanceid " +
        "WHERE TPO.trackedentityinstanceid in (:ids) " +
        "AND p.uid = :programUid " +
        "GROUP BY tei.uid,tpo.trackedentityinstanceid, tpo.programid, tpo.organisationunitid, ou.path, p.accesslevel,p.uid "
        +
        "HAVING (P.accesslevel in ('OPEN', 'AUDITED') AND (EXISTS(SELECT SS.organisationunitid FROM userteisearchorgunits SS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = SS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')) OR EXISTS(SELECT CS.organisationunitid FROM usermembership CS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = CS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')))) "
        +
        "OR (P.accesslevel in ('CLOSED', 'PROTECTED') AND EXISTS(SELECT CS.organisationunitid FROM usermembership CS LEFT JOIN organisationunit OU2 ON OU2.organisationunitid = CS.organisationunitid WHERE userinfoid = :userInfoId AND OU.path LIKE CONCAT(OU2.path, '%')));";

    private static final String FILTER_OUT_DELETED_TEIS = "tei.deleted=false";

    public DefaultTrackedEntityInstanceStore( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    String getRelationshipEntityColumn()
    {
        return "trackedentityinstanceid";
    }

    @Override
    public Map<String, TrackedEntityInstance> getTrackedEntityInstances( List<Long> ids, AggregateContext ctx )
    {
        List<List<Long>> idPartitions = Lists.partition( ids, PARITITION_SIZE );

        Map<String, TrackedEntityInstance> trackedEntityMap = new LinkedHashMap<>();

        idPartitions
            .forEach( partition -> trackedEntityMap.putAll( getTrackedEntityInstancesPartitioned( partition, ctx ) ) );
        return trackedEntityMap;
    }

    private Map<String, TrackedEntityInstance> getTrackedEntityInstancesPartitioned( List<Long> ids,
        AggregateContext ctx )
    {
        TrackedEntityInstanceRowCallbackHandler handler = new TrackedEntityInstanceRowCallbackHandler();

        if ( !ctx.isSuperUser() && ctx.getTrackedEntityTypes().isEmpty() )
        {
            // If not super user and no tets are accessible. then simply return
            // empty list.
            return new HashMap<>();
        }

        String sql = getQuery( GET_TEIS_SQL, ctx, "tei.trackedentitytypeid in (:teiTypeIds)", FILTER_OUT_DELETED_TEIS );
        jdbcTemplate.query( applySortOrder( sql, StringUtils.join( ids, "," ), "trackedentityinstanceid" ),
            createIdsParam( ids ).addValue( "teiTypeIds", ctx.getTrackedEntityTypes() ), handler );

        return handler.getItems();
    }

    @Override
    public Multimap<String, Attribute> getAttributes( List<Long> ids )
    {
        return fetch( GET_TEI_ATTRIBUTES, new TrackedEntityAttributeRowCallbackHandler(), ids );
    }

    public Multimap<String, ProgramOwner> getProgramOwners( List<Long> ids )
    {
        return fetch( GET_PROGRAM_OWNERS, new ProgramOwnerRowCallbackHandler(), ids );
    }

    @Override
    public Multimap<String, String> getOwnedTeis( List<Long> ids, AggregateContext ctx )
    {
        List<List<Long>> teiIds = Lists.partition( ids, PARITITION_SIZE );

        Multimap<String, String> ownedTeisMultiMap = ArrayListMultimap.create();

        teiIds.forEach( partition -> {
            ownedTeisMultiMap.putAll( getOwnedTeisPartitioned( partition, ctx ) );
        } );

        return ownedTeisMultiMap;
    }

    private Multimap<String, String> getOwnedTeisPartitioned( List<Long> ids, AggregateContext ctx )
    {
        OwnedTeiMapper handler = new OwnedTeiMapper();

        MapSqlParameterSource paramSource = createIdsParam( ids ).addValue( "userInfoId", ctx.getUserId() );

        boolean checkForOwnership = ctx.getQueryParams().isIncludeAllAttributes()
            || ctx.getParams().isIncludeEnrollments() || ctx.getParams().getTeiEnrollmentParams().isIncludeEvents();

        String sql;

        if ( ctx.getQueryParams().hasProgram() )
        {
            sql = GET_OWNERSHIP_DATA_FOR_TEIS_FOR_SPECIFIC_PROGRAM;
            paramSource.addValue( "programUid", ctx.getQueryParams().getProgram().getUid() );
        }
        else if ( checkForOwnership )
        {
            sql = GET_OWNERSHIP_DATA_FOR_TEIS_FOR_ALL_PROGRAM;
        }
        else
        {
            return ArrayListMultimap.create();
        }

        jdbcTemplate.query( sql, paramSource, handler );

        return handler.getItems();
    }

    @Override
    public List<TrackedEntityOuInfo> getTrackedEntityOuInfoByUid( List<String> uids, User user )
    {
        List<List<String>> uidPartitions = Lists.partition( uids, 20000 );

        List<TrackedEntityOuInfo> instances = new ArrayList<>();

        String sql = "select tei.trackedentityinstanceid teiid, tei.uid teiuid, tei.organisationunitid teiorgunit from trackedentityinstance tei where tei.uid in (:uids)";

        for ( List<String> partition : uidPartitions )
        {
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue( "uids", partition );
            List<TrackedEntityOuInfo> infos = jdbcTemplate.query( sql, parameters,
                ( rs, rowNum ) -> new TrackedEntityOuInfo(
                    rs.getLong( "teiid" ),
                    rs.getString( "teiuid" ),
                    rs.getLong( "teiorgunit" ) ) );
            instances.addAll( infos );
        }

        return instances;
    }

    @Override
    public List<TrackedEntityProgramOwnerIds> getTrackedEntityProgramOwnersUids( List<Long> teiIds, long programId )
    {
        List<TrackedEntityProgramOwnerIds> instances = new ArrayList<>();
        List<List<Long>> teiIdsPartitions = Lists.partition( teiIds, 20000 );
        String sql = """
              select te.uid teuid, p.uid programuid, ou.uid orgunituid
                from trackedentityprogramowner tepo
                join trackedentityinstance te on tepo.trackedentityinstanceid = te.trackedentityinstanceid
                join program p on tepo.programid = p.programid
                join organisationunit ou on te.organisationunitid = ou.organisationunitid
                where tepo.trackedentityinstanceid in (:teiIds) and tepo.programid = :programId
            """;
        for ( List<Long> partition : teiIdsPartitions )
        {
            MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue( "programId", programId )
                .addValue( "teiIds", partition );
            List<TrackedEntityProgramOwnerIds> uids = jdbcTemplate.query( sql, parameters,
                ( rs, rowNum ) -> new TrackedEntityProgramOwnerIds(
                    rs.getString( "teuid" ),
                    rs.getString( "programuid" ),
                    rs.getString( "orgunituid" ) ) );
            instances.addAll( uids );
        }
        return instances;

    }
}
