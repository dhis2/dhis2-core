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
package org.hisp.dhis.dxf2.events.trackedentity.store.mapper;

import static org.hisp.dhis.dxf2.events.trackedentity.store.mapper.MapperGeoUtils.resolveGeometry;
import static org.hisp.dhis.dxf2.events.trackedentity.store.query.EventQuery.COLUMNS;
import static org.hisp.dhis.dxf2.events.trackedentity.store.query.EventQuery.getColumnName;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.util.DateUtils;

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
    String getKeyColumn()
    {
        return "enruid";
    }

    private Event getEvent( ResultSet rs )
        throws SQLException
    {
        Event event = new Event();
        event.setEvent( rs.getString( getColumnName( COLUMNS.UID ) ) );
        event.setId( rs.getLong( getColumnName( COLUMNS.ID ) ) );
        event.setTrackedEntityInstance( rs.getString( getColumnName( COLUMNS.TEI_UID ) ) );
        final boolean followup = rs.getBoolean( getColumnName( COLUMNS.ENROLLMENT_FOLLOWUP ) );
        event.setFollowup( rs.wasNull() ? null : followup );
        event.setEnrollmentStatus(
            EnrollmentStatus.fromStatusString( rs.getString( getColumnName( COLUMNS.ENROLLMENT_STATUS ) ) ) );
        event.setStatus( EventStatus.valueOf( rs.getString( getColumnName( COLUMNS.STATUS ) ) ) );
        event.setEventDate( DateUtils.getIso8601NoTz( rs.getDate( getColumnName( COLUMNS.EXECUTION_DATE ) ) ) );
        event.setDueDate( DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.DUE_DATE ) ) ) );
        event.setStoredBy( rs.getString( getColumnName( COLUMNS.STOREDBY ) ) );
        event.setCompletedBy( rs.getString( getColumnName( COLUMNS.COMPLETEDBY ) ) );
        event.setCompletedDate( DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.COMPLETEDDATE ) ) ) );
        event.setCreated( DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.CREATED ) ) ) );
        event.setCreatedAtClient(
            DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.CREATEDCLIENT ) ) ) );
        event.setLastUpdated( DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.UPDATED ) ) ) );
        event.setLastUpdatedAtClient(
            DateUtils.getIso8601NoTz( rs.getTimestamp( getColumnName( COLUMNS.UPDATEDCLIENT ) ) ) );

        resolveGeometry( rs.getBytes( getColumnName( COLUMNS.GEOMETRY ) ) ).ifPresent( event::setGeometry );

        event.setDeleted( rs.getBoolean( getColumnName( COLUMNS.DELETED ) ) );

        event.setProgram( rs.getString( getColumnName( COLUMNS.PROGRAM_UID ) ) );
        event.setOrgUnit( rs.getString( getColumnName( COLUMNS.ORGUNIT_UID ) ) );
        event.setOrgUnitName( rs.getString( getColumnName( COLUMNS.ORGUNIT_NAME ) ) );
        event.setEnrollment( rs.getString( getColumnName( COLUMNS.ENROLLMENT_UID ) ) );
        event.setProgramStage( rs.getString( getColumnName( COLUMNS.PROGRAM_STAGE_UID ) ) );
        event.setAttributeOptionCombo( rs.getString( getColumnName( COLUMNS.COC_UID ) ) );
        event.setAttributeCategoryOptions( rs.getString( getColumnName( COLUMNS.CAT_OPTIONS ) ) );

        return event;
    }
}
