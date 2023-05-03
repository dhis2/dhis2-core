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
package org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.EventQuery;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.EventQuery.COLUMNS;
import org.hisp.dhis.user.User;

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
        event.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.UID ) ) );
        event.setId( rs.getLong( EventQuery.getColumnName( COLUMNS.ID ) ) );

        event.setStatus( EventStatus.valueOf( rs.getString( EventQuery.getColumnName( COLUMNS.STATUS ) ) ) );
        event.setExecutionDate( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.EXECUTION_DATE ) ) );
        event.setDueDate( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.DUE_DATE ) ) );
        event.setStoredBy( rs.getString( EventQuery.getColumnName( COLUMNS.STOREDBY ) ) );
        event.setCompletedBy( rs.getString( EventQuery.getColumnName( COLUMNS.COMPLETEDBY ) ) );
        event.setCompletedDate( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.COMPLETEDDATE ) ) );
        event.setCreated( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.CREATED ) ) );
        event.setCreatedAtClient( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.CREATEDCLIENT ) ) );
        JsonbToObjectHelper.setUserInfoSnapshot( rs, EventQuery.getColumnName( COLUMNS.CREATED_BY ),
            event::setCreatedByUserInfo );
        event.setLastUpdated( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.UPDATED ) ) );
        event.setLastUpdatedAtClient( rs.getTimestamp( EventQuery.getColumnName( COLUMNS.UPDATEDCLIENT ) ) );
        JsonbToObjectHelper.setUserInfoSnapshot( rs, EventQuery.getColumnName( COLUMNS.LAST_UPDATED_BY ),
            event::setLastUpdatedByUserInfo );
        MapperGeoUtils.resolveGeometry( rs.getBytes( EventQuery.getColumnName( COLUMNS.GEOMETRY ) ) )
            .ifPresent( event::setGeometry );
        event.setDeleted( rs.getBoolean( EventQuery.getColumnName( COLUMNS.DELETED ) ) );

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.ORGUNIT_UID ) ) );
        orgUnit.setName( rs.getString( EventQuery.getColumnName( COLUMNS.ORGUNIT_NAME ) ) );
        event.setOrganisationUnit( orgUnit );

        Enrollment enrollment = new Enrollment();
        enrollment.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.ENROLLMENT_UID ) ) );
        Program program = new Program();
        program.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.PROGRAM_UID ) ) );
        enrollment.setProgram( program );
        final boolean followup = rs.getBoolean( EventQuery.getColumnName( COLUMNS.ENROLLMENT_FOLLOWUP ) );
        enrollment.setFollowup( rs.wasNull() ? null : followup );
        enrollment.setStatus(
            ProgramStatus.valueOf( rs.getString( EventQuery.getColumnName( COLUMNS.ENROLLMENT_STATUS ) ) ) );
        TrackedEntityInstance trackedEntity = new TrackedEntityInstance();
        trackedEntity.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.TEI_UID ) ) );
        enrollment.setEntityInstance( trackedEntity );
        event.setEnrollment( enrollment );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.PROGRAM_STAGE_UID ) ) );
        event.setProgramStage( programStage );

        CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
        categoryOptionCombo.setUid( rs.getString( EventQuery.getColumnName( COLUMNS.COC_UID ) ) );
        Set<CategoryOption> categoryOptions = TextUtils
            .splitToSet( rs.getString( EventQuery.getColumnName( COLUMNS.CAT_OPTIONS ) ),
                TextUtils.SEMICOLON )
            .stream()
            .map( o -> {
                CategoryOption co = new CategoryOption();
                co.setUid( o );
                return co;
            } ).collect( Collectors.toSet() );
        categoryOptionCombo.setCategoryOptions( categoryOptions );
        event.setAttributeOptionCombo( categoryOptionCombo );

        String assignedUserUid = rs.getString( EventQuery.getColumnName( COLUMNS.ASSIGNED_USER ) );
        String assignedUserUsername = rs
            .getString( EventQuery.getColumnName( COLUMNS.ASSIGNED_USER_USERNAME ) );
        String assignedUserFirstName = rs
            .getString( EventQuery.getColumnName( COLUMNS.ASSIGNED_USER_FIRST_NAME ) );
        String assignedUserSurname = rs
            .getString( EventQuery.getColumnName( COLUMNS.ASSIGNED_USER_SURNAME ) );
        if ( StringUtils.isNotEmpty( assignedUserUid ) || StringUtils.isNotEmpty( assignedUserUsername )
            || StringUtils.isNotEmpty( assignedUserFirstName ) || StringUtils.isNotEmpty( assignedUserSurname ) )
        {
            User assignedUser = new User();
            assignedUser.setUid( assignedUserUid );
            assignedUser.setUsername( assignedUserUsername );
            assignedUser.setFirstName( assignedUserFirstName );
            assignedUser.setSurname( assignedUserSurname );
            if ( assignedUser.getFirstName() != null && assignedUser.getSurname() != null )
            {
                assignedUser.setName( assignedUser.getFirstName() + " " + assignedUser.getSurname() );
            }
            event.setAssignedUser( assignedUser );
        }

        return event;
    }
}
