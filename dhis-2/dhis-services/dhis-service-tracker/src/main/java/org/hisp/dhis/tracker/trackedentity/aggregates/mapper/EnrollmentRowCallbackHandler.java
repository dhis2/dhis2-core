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
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.COMPLETED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.COMPLETEDBY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.CREATED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.CREATEDCLIENT;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.CREATED_BY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.DELETED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.ENROLLMENTDATE;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.FOLLOWUP;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.GEOMETRY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.ID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.INCIDENTDATE;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.LAST_UPDATED_BY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.ORGUNIT_NAME;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.ORGUNIT_UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.STATUS;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.STOREDBY;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.TEI_TYPE_UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.TEI_UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.UID;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.UPDATED;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS.UPDATEDCLIENT;
import static org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery.getColumnName;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.trackedentity.aggregates.query.EnrollmentQuery;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentRowCallbackHandler extends AbstractMapper<ProgramInstance>
{
    @Override
    ProgramInstance getItem( ResultSet rs )
        throws SQLException
    {
        return getEnrollment( rs );
    }

    @Override
    String getKeyColumn()
    {
        return "tei_uid";
    }

    private ProgramInstance getEnrollment( ResultSet rs )
        throws SQLException
    {
        ProgramInstance enrollment = new ProgramInstance();
        enrollment.setId( rs.getLong( getColumnName( ID ) ) );
        enrollment.setUid( rs.getString( getColumnName( UID ) ) );

        MapperGeoUtils.resolveGeometry( rs.getBytes( getColumnName( GEOMETRY ) ) )
            .ifPresent( enrollment::setGeometry );

        TrackedEntityInstance trackedEntity = new TrackedEntityInstance();
        trackedEntity.setUid( rs.getString( getColumnName( TEI_UID ) ) );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType.setUid( rs.getString( getColumnName( TEI_TYPE_UID ) ) );
        trackedEntity.setTrackedEntityType( trackedEntityType );
        enrollment.setEntityInstance( trackedEntity );

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( rs.getString( getColumnName( ORGUNIT_UID ) ) );
        orgUnit.setName( rs.getString( getColumnName( ORGUNIT_NAME ) ) );
        enrollment.setOrganisationUnit( orgUnit );

        enrollment.setCreated( rs.getTimestamp( getColumnName( CREATED ) ) );
        enrollment.setCreatedAtClient( rs.getTimestamp( getColumnName( CREATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( CREATED_BY ), enrollment::setCreatedByUserInfo );
        enrollment.setLastUpdated( rs.getTimestamp( getColumnName( UPDATED ) ) );
        enrollment.setLastUpdatedAtClient( rs.getTimestamp( getColumnName( UPDATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, getColumnName( LAST_UPDATED_BY ), enrollment::setLastUpdatedByUserInfo );

        Program program = new Program();
        program.setUid( rs.getString( getColumnName( EnrollmentQuery.COLUMNS.PROGRAM_UID ) ) );
        enrollment.setProgram( program );

        final boolean followup = rs.getBoolean( getColumnName( FOLLOWUP ) );
        enrollment.setFollowup( rs.wasNull() ? null : followup );
        enrollment.setStatus( ProgramStatus.valueOf( rs.getString( getColumnName( STATUS ) ) ) );
        enrollment.setEnrollmentDate( rs.getTimestamp( getColumnName( ENROLLMENTDATE ) ) );
        enrollment.setIncidentDate( rs.getTimestamp( getColumnName( INCIDENTDATE ) ) );
        enrollment.setEndDate( rs.getTimestamp( getColumnName( COMPLETED ) ) );
        enrollment.setCompletedBy( rs.getString( getColumnName( COMPLETEDBY ) ) );
        enrollment.setStoredBy( rs.getString( getColumnName( STOREDBY ) ) );
        enrollment.setDeleted( rs.getBoolean( getColumnName( DELETED ) ) );

        return enrollment;
    }
}
