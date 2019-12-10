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

package org.hisp.dhis.dxf2.events.trackedentity.store.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Geometry;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.util.DateUtils;
import org.postgresql.util.PGobject;

import static org.hisp.dhis.dxf2.events.trackedentity.store.mapper.MapperGeoUtils.resolveGeometry;

/**
 * @author Luciano Fiandesio
 */
public class EventRowCallbackHandler
    extends
    AbstractMapper<Event>
{

    @Override
    Event getItem( ResultSet rs )
        throws SQLException
    {
        return getEvent( rs );
    }

    @Override
    String getKeyColumn() {
        return "enruid";
    }

    private Event getEvent( ResultSet rs )
        throws SQLException
    {
        Event event = new Event();
        event.setEvent( rs.getString( "uid" ) );
        event.setId( rs.getLong("programstageinstanceid") );
        event.setTrackedEntityInstance( rs.getString( "enruid" ) );
        event.setFollowup( rs.getBoolean( "enrfollowup" ) );
        event.setEnrollmentStatus( EnrollmentStatus.fromStatusString( rs.getString("enrstatus") ) );
        event.setStatus( EventStatus.valueOf( rs.getString( "status" ) ) );
        event.setEventDate( DateUtils.getIso8601NoTz( rs.getDate( "executiondate" ) ) );
        event.setDueDate( DateUtils.getIso8601NoTz( rs.getDate( "duedate" ) ) );
        event.setStoredBy( rs.getString( "storedby" ) );
        event.setCompletedBy( rs.getString( "completedby" ) );
        event.setCompletedDate( DateUtils.getIso8601NoTz( rs.getDate( "completeddate" ) ) );
        event.setCreated( DateUtils.getIso8601NoTz( rs.getDate( "created" ) ) );
        event.setCreatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "createdatclient" ) ) );
        event.setLastUpdated( DateUtils.getIso8601NoTz( rs.getDate( "lastupdated" ) ) );
        event.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "lastupdatedatclient" ) ) );

        resolveGeometry( rs.getBytes( "geometry" ) ).ifPresent( event::setGeometry );

        event.setDeleted( rs.getBoolean( "deleted" ) );

        event.setProgram( rs.getString("prguid") );
        //event.setEnrollment( programStageInstance.getProgramInstance().getUid() ); TODO do we need to duplicate this value? we have already setTrackedEntityInstance
        event.setProgramStage( rs.getString("prgstguid") );
        event.setAttributeOptionCombo( rs.getString("cocuid") );
        event.setAttributeCategoryOptions( rs.getString("catoptions") );

//        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
//        {
//            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
//        } TODO

        return event;
    }
}
