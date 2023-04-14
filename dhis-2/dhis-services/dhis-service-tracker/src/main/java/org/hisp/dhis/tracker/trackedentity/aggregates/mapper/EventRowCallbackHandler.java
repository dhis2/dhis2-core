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
package org.hisp.dhis.tracker.trackedentity.aggregates.mapper;

import static org.hisp.dhis.tracker.trackedentity.aggregates.mapper.JsonbToObjectHelper.setUserInfoSnapshot;
import static org.hisp.dhis.tracker.trackedentity.aggregates.mapper.MapperGeoUtils.resolveGeometry;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EventQuery.COLUMNS;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EventQuery.getColumnName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;

/**
 * @author Luciano Fiandesio
 */
public class EventRowCallbackHandler
    extends
    AbstractMapper<ProgramStageInstance>
{

    @Override
    ProgramStageInstance getItem( ResultSet rs )
        throws SQLException
    {
        return getEvent( rs );
    }

    @Override
    String getKeyColumn()
    {
        return "enruid";
    }

    private ProgramStageInstance getEvent( ResultSet rs )
        throws SQLException
    {
        ProgramStageInstance event = new ProgramStageInstance();
        event.setUid( rs.getString( getColumnName( COLUMNS.UID ) ) );
        event.setId( rs.getLong( getColumnName( COLUMNS.ID ) ) );

        event.setStatus( EventStatus.valueOf( rs.getString( getColumnName( COLUMNS.STATUS ) ) ) );
        event.setExecutionDate( rs.getTimestamp( getColumnName( COLUMNS.EXECUTION_DATE ) ) );
        event.setDueDate( rs.getTimestamp( getColumnName( COLUMNS.DUE_DATE ) ) );
        event.setStoredBy( rs.getString( getColumnName( COLUMNS.STOREDBY ) ) );
        event.setCompletedBy( rs.getString( getColumnName( COLUMNS.COMPLETEDBY ) ) );
        event.setCompletedDate( rs.getTimestamp( getColumnName( COLUMNS.COMPLETEDDATE ) ) );
        event.setCreated( rs.getTimestamp( getColumnName( COLUMNS.CREATED ) ) );
        event.setCreatedAtClient( rs.getTimestamp( getColumnName( COLUMNS.CREATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( COLUMNS.CREATED_BY ), event::setCreatedByUserInfo );
        event.setLastUpdated( rs.getTimestamp( getColumnName( COLUMNS.UPDATED ) ) );
        event.setLastUpdatedAtClient( rs.getTimestamp( getColumnName( COLUMNS.UPDATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( COLUMNS.LAST_UPDATED_BY ), event::setLastUpdatedByUserInfo );
        resolveGeometry( rs.getBytes( getColumnName( COLUMNS.GEOMETRY ) ) ).ifPresent( event::setGeometry );
        event.setDeleted( rs.getBoolean( getColumnName( COLUMNS.DELETED ) ) );

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( rs.getString( getColumnName( COLUMNS.ORGUNIT_UID ) ) );
        orgUnit.setName( rs.getString( getColumnName( COLUMNS.ORGUNIT_NAME ) ) );
        event.setOrganisationUnit( orgUnit );

        ProgramInstance enrollment = new ProgramInstance();
        enrollment.setUid( rs.getString( getColumnName( COLUMNS.ENROLLMENT_UID ) ) );
        Program program = new Program();
        program.setUid( rs.getString( getColumnName( COLUMNS.PROGRAM_UID ) ) );
        enrollment.setProgram( program );
        final boolean followup = rs.getBoolean( getColumnName( COLUMNS.ENROLLMENT_FOLLOWUP ) );
        enrollment.setFollowup( rs.wasNull() ? null : followup );
        enrollment.setStatus( ProgramStatus.valueOf( rs.getString( getColumnName( COLUMNS.ENROLLMENT_STATUS ) ) ) );
        TrackedEntityInstance trackedEntity = new TrackedEntityInstance();
        trackedEntity.setUid( rs.getString( getColumnName( COLUMNS.TEI_UID ) ) );
        enrollment.setEntityInstance( trackedEntity );
        event.setProgramInstance( enrollment );

        ProgramStage programStage = new ProgramStage();
        programStage.setUid( rs.getString( getColumnName( COLUMNS.PROGRAM_STAGE_UID ) ) );
        event.setProgramStage( programStage );

        CategoryOptionCombo categoryOptionCombo = new CategoryOptionCombo();
        categoryOptionCombo.setUid( rs.getString( getColumnName( COLUMNS.COC_UID ) ) );
        Set<CategoryOption> categoryOptions = TextUtils
            .splitToSet( rs.getString( getColumnName( COLUMNS.CAT_OPTIONS ) ), TextUtils.SEMICOLON ).stream()
            .map( o -> {
                CategoryOption co = new CategoryOption();
                co.setUid( o );
                return co;
            } ).collect( Collectors.toSet() );
        categoryOptionCombo.setCategoryOptions( categoryOptions );
        event.setAttributeOptionCombo( categoryOptionCombo );

        User assignedUser = new User();
        assignedUser.setUid( rs.getString( getColumnName( COLUMNS.ASSIGNED_USER ) ) );
        assignedUser.setUsername( rs.getString( getColumnName( COLUMNS.ASSIGNED_USER_USERNAME ) ) );
        assignedUser.setFirstName( rs.getString( getColumnName( COLUMNS.ASSIGNED_USER_FIRST_NAME ) ) );
        assignedUser.setSurname( rs.getString( getColumnName( COLUMNS.ASSIGNED_USER_SURNAME ) ) );
        if ( assignedUser.getFirstName() != null && assignedUser.getSurname() != null )
        {
            assignedUser.setName( assignedUser.getFirstName() + " " + assignedUser.getSurname() );
        }
        event.setAssignedUser( assignedUser );

        return event;
    }
}
