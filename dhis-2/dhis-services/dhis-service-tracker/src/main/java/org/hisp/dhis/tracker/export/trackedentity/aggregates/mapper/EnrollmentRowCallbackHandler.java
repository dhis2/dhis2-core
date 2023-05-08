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

import static org.hisp.dhis.tracker.export.trackedentity.aggregates.mapper.JsonbToObjectHelper.setUserInfoSnapshot;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.EnrollmentQuery;
import org.hisp.dhis.tracker.export.trackedentity.aggregates.query.EnrollmentQuery.COLUMNS;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentRowCallbackHandler extends AbstractMapper<Enrollment>
{
    @Override
    Enrollment getItem( ResultSet rs )
        throws SQLException
    {
        return getEnrollment( rs );
    }

    @Override
    String getKeyColumn()
    {
        return "tei_uid";
    }

    private Enrollment getEnrollment( ResultSet rs )
        throws SQLException
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setId( rs.getLong( EnrollmentQuery.getColumnName( COLUMNS.ID ) ) );
        enrollment.setUid( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.UID ) ) );

        MapperGeoUtils
            .resolveGeometry( rs.getBytes( EnrollmentQuery.getColumnName( COLUMNS.GEOMETRY ) ) )
            .ifPresent( enrollment::setGeometry );

        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setUid( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.TEI_UID ) ) );
        TrackedEntityType trackedEntityType = new TrackedEntityType();
        trackedEntityType
            .setUid( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.TEI_TYPE_UID ) ) );
        trackedEntity.setTrackedEntityType( trackedEntityType );
        enrollment.setEntityInstance( trackedEntity );

        OrganisationUnit orgUnit = new OrganisationUnit();
        orgUnit.setUid( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.ORGUNIT_UID ) ) );
        orgUnit.setName( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.ORGUNIT_NAME ) ) );
        enrollment.setOrganisationUnit( orgUnit );

        enrollment.setCreated( rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.CREATED ) ) );
        enrollment.setCreatedAtClient(
            rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.CREATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, EnrollmentQuery.getColumnName( COLUMNS.CREATED_BY ),
            enrollment::setCreatedByUserInfo );
        enrollment
            .setLastUpdated( rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.UPDATED ) ) );
        enrollment.setLastUpdatedAtClient(
            rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.UPDATEDCLIENT ) ) );
        setUserInfoSnapshot( rs, EnrollmentQuery.getColumnName( COLUMNS.LAST_UPDATED_BY ),
            enrollment::setLastUpdatedByUserInfo );

        Program program = new Program();
        program.setUid( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.PROGRAM_UID ) ) );
        enrollment.setProgram( program );

        final boolean followup = rs.getBoolean( EnrollmentQuery.getColumnName( COLUMNS.FOLLOWUP ) );
        enrollment.setFollowup( rs.wasNull() ? null : followup );
        enrollment.setStatus(
            ProgramStatus.valueOf( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.STATUS ) ) ) );
        enrollment.setEnrollmentDate(
            rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.ENROLLMENTDATE ) ) );
        enrollment.setIncidentDate(
            rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.INCIDENTDATE ) ) );
        enrollment.setEndDate( rs.getTimestamp( EnrollmentQuery.getColumnName( COLUMNS.COMPLETED ) ) );
        enrollment
            .setCompletedBy( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.COMPLETEDBY ) ) );
        enrollment.setStoredBy( rs.getString( EnrollmentQuery.getColumnName( COLUMNS.STOREDBY ) ) );
        enrollment.setDeleted( rs.getBoolean( EnrollmentQuery.getColumnName( COLUMNS.DELETED ) ) );

        return enrollment;
    }
}
