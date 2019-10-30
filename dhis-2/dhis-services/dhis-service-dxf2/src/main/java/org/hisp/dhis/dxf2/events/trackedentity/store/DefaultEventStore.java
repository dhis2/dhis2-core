package org.hisp.dhis.dxf2.events.trackedentity.store;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.util.List;

import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.EventDataValueRowCallbackHandler;
import org.hisp.dhis.dxf2.events.trackedentity.store.mapper.EventRowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
    private final static String GET_EVENTS_SQL = "select psi.programstageinstanceid, psi.uid, " +
            "       psi.status, " +
            "       psi.executiondate, " +
            "       psi.duedate, " +
            "       psi.storedby, " +
            "       psi.completedby, " +
            "       psi.completeddate, " +
            "       psi.created, " +
            "       psi.createdatclient, " +
            "       psi.lastupdated, " +
            "       psi.lastupdatedatclient, " +
            "       psi.deleted, " +
            "       psi.geometry, " +
            "       pi.uid                       as enruid, " +
            "       pi.followup                  as enrfollowup, " +
            "       pi.status                    as enrstatus, " +
            "       p.uid                        as prguid, " +
            "       ps.uid                       as prgstguid, " +
            "       coc.uid                      as cocuid, " +
            "       array_to_string(array( " +
            "                               SELECT opt.uid " +
            "                               FROM dataelementcategoryoption opt " +
            "                                        join categoryoptioncombos_categoryoptions ccc " +
            "                                             on opt.categoryoptionid = ccc.categoryoptionid " +
            "                               WHERE coc.categoryoptioncomboid = ccc.categoryoptioncomboid " +
            "                           ), ', ') AS catoptions " +
            "from programstageinstance psi " +
            "         join programinstance pi on psi.programinstanceid = pi.programinstanceid " +
            "         join program p on pi.programid = p.programid " +
            "         join programstage ps on psi.programstageid = ps.programstageid " +
            "         join categoryoptioncombo coc on psi.attributeoptioncomboid = coc.categoryoptioncomboid " +
            "where pi.programinstanceid in (:ids)";

    private final static String GET_DATAVALUES_SQL = "select psi.uid as eventuid, " +
            "       psi.eventdatavalues " +
            "from programstageinstance psi " +
            "where psi.programinstanceid in (:ids)";

    public DefaultEventStore( JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    String getRelationshipEntityColumn()
    {
        return "programstageinstanceid";
    }

    @Override
    public Multimap<String, Event> getEventsByEnrollmentIds(List<Long> enrollmentsId )
    {
        EventRowCallbackHandler handler = new EventRowCallbackHandler();
        jdbcTemplate.query( GET_EVENTS_SQL, createIdsParam( enrollmentsId ), handler );
        return handler.getItems();
    }

    @Override
    public Multimap<String, List<DataValue>> getDataValues( List<Long> enrollmentsId )
    {
        EventDataValueRowCallbackHandler handler = new EventDataValueRowCallbackHandler();
        jdbcTemplate.query( GET_DATAVALUES_SQL, createIdsParam( enrollmentsId ), handler );
        return handler.getItems();
    }
}
