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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dxf2.deprecated.tracker.aggregates.AggregateContext;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Note;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.EventDataValueRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.EventRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.mapper.NoteRowCallbackHandler;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.store.query.EventQuery;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Repository
public class DefaultEventStore
    extends
    AbstractStore
    implements
    EventStore
{
    private static final String GET_EVENTS_SQL = EventQuery.getQuery();

    private static final String GET_DATAVALUES_SQL = "select psi.uid as key, " +
        "psi.eventdatavalues " +
        "from event psi " +
        "where psi.eventid in (:ids)";

    private static final String GET_NOTES_SQL = "select psi.uid as key, tec.uid, tec.commenttext, " +
        "tec.creator, tec.created " +
        "from trackedentitycomment tec " +
        "join eventcomments psic " +
        "on tec.trackedentitycommentid = psic.trackedentitycommentid " +
        "join event psi on psic.eventid = psi.eventid " +
        "where psic.eventid in (:ids)";

    private static final String ACL_FILTER_SQL = "CASE WHEN p.type = 'WITH_REGISTRATION' THEN " +
        "p.trackedentitytypeid in (:trackedEntityTypeIds) else true END " +
        "AND psi.programstageid in (:programStageIds) AND pi.programid IN (:programIds)";

    private static final String ACL_FILTER_SQL_NO_PROGRAM_STAGE = "CASE WHEN p.type = 'WITH_REGISTRATION' THEN " +
        "p.trackedentitytypeid in (:trackedEntityTypeIds) else true END " +
        "AND pi.programid IN (:programIds)";

    private static final String FILTER_OUT_DELETED_EVENTS = "psi.deleted=false";

    public DefaultEventStore( JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    String getRelationshipEntityColumn()
    {
        return "eventid";
    }

    @Override
    public Multimap<String, Event> getEventsByEnrollmentIds( List<Long> enrollmentsId, AggregateContext ctx )
    {
        List<List<Long>> enrollmentIdsPartitions = Lists.partition( enrollmentsId, PARITITION_SIZE );

        Multimap<String, Event> eventMultimap = ArrayListMultimap.create();

        enrollmentIdsPartitions
            .forEach( partition -> eventMultimap.putAll( getEventsByEnrollmentIdsPartitioned( partition, ctx ) ) );

        return eventMultimap;
    }

    private String getAttributeOptionComboClause( AggregateContext ctx )
    {
        return " and psi.attributeoptioncomboid not in (" +
            "select distinct(cocco.categoryoptioncomboid) " +
            "from categoryoptioncombos_categoryoptions as cocco " +
            // Get inaccessible category options
            "where cocco.categoryoptionid not in ( " +
            "select co.categoryoptionid " +
            "from dataelementcategoryoption co  " +
            " where "
            + JpaQueryUtils.generateSQlQueryForSharingCheck( "co.sharing", ctx.getUserUid(), ctx.getUserGroups(),
                AclService.LIKE_READ_DATA )
            + ") )";
    }

    private Multimap<String, Event> getEventsByEnrollmentIdsPartitioned( List<Long> enrollmentsId,
        AggregateContext ctx )
    {
        EventRowCallbackHandler handler = new EventRowCallbackHandler();

        List<Long> programStages = ctx.getProgramStages();

        String aocSql = ctx.isSuperUser() ? "" : getAttributeOptionComboClause( ctx );

        if ( programStages.isEmpty() )
        {
            jdbcTemplate.query(
                getQuery( GET_EVENTS_SQL, ctx, ACL_FILTER_SQL_NO_PROGRAM_STAGE + aocSql, FILTER_OUT_DELETED_EVENTS ),
                createIdsParam( enrollmentsId )
                    .addValue( "trackedEntityTypeIds", ctx.getTrackedEntityTypes() )
                    .addValue( "programIds", ctx.getPrograms() ),
                handler );
        }
        else
        {
            jdbcTemplate.query(
                getQuery( GET_EVENTS_SQL, ctx, ACL_FILTER_SQL + aocSql, FILTER_OUT_DELETED_EVENTS ),
                createIdsParam( enrollmentsId )
                    .addValue( "trackedEntityTypeIds", ctx.getTrackedEntityTypes() )
                    .addValue( "programStageIds", programStages )
                    .addValue( "programIds", ctx.getPrograms() ),
                handler );
        }

        return handler.getItems();
    }

    @Override
    public Map<String, List<DataValue>> getDataValues( List<Long> programStageInstanceId )
    {
        List<List<Long>> psiIdsPartitions = Lists.partition( programStageInstanceId, PARITITION_SIZE );

        Map<String, List<DataValue>> dataValueListMultimap = new HashMap<>();

        psiIdsPartitions.forEach( partition -> dataValueListMultimap.putAll( getDataValuesPartitioned( partition ) ) );

        return dataValueListMultimap;
    }

    private Map<String, List<DataValue>> getDataValuesPartitioned( List<Long> programStageInstanceId )
    {
        EventDataValueRowCallbackHandler handler = new EventDataValueRowCallbackHandler();

        jdbcTemplate.query( GET_DATAVALUES_SQL, createIdsParam( programStageInstanceId ), handler );

        return handler.getItems();
    }

    @Override
    public Multimap<String, Note> getNotes( List<Long> eventIds )
    {
        return fetch( GET_NOTES_SQL, new NoteRowCallbackHandler(), eventIds );
    }
}
