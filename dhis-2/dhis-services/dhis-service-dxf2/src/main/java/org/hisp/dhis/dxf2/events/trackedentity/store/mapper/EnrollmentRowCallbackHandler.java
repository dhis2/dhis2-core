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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.util.DateUtils;
import org.postgresql.util.PGobject;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentRowCallbackHandler
    extends
    AbstractMapper<Enrollment>
{

    @Override
    Enrollment getItem( ResultSet rs )
        throws SQLException
    {
        return getEnrollment( rs );
    }

    @Override
    String getKeyColumn() {
        return "teiuid";
    }

    private Enrollment getEnrollment( ResultSet rs )
        throws SQLException
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( rs.getString("uid") );

        // we set these 4 fields in post-processing TODO

        // enrollment.setTrackedEntityType();
        // enrollment.setTrackedEntityInstance();
        // enrollment.setOrgUnit( programInstance.getOrganisationUnit().getUid() );
        // enrollment.setOrgUnitName( programInstance.getOrganisationUnit().getName() );


        Optional<Geometry> geo = MapperGeoUtils.resolveGeometry( rs.getBytes( "geometry" ) );
        if ( geo.isPresent() )
        {
            enrollment.setGeometry( geo.get() );
            if ( rs.getString( "program_feature_type" ).equalsIgnoreCase( FeatureType.POINT.value() ) )
            {
                com.vividsolutions.jts.geom.Coordinate co = enrollment.getGeometry().getCoordinate();
                enrollment.setCoordinate( new org.hisp.dhis.dxf2.events.event.Coordinate( co.x, co.y ) );
            }
        }

        enrollment.setCreated( DateUtils.getIso8601NoTz( rs.getDate( "created" ) ) );
        enrollment.setCreatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "createdatclient" ) ) );
        enrollment.setLastUpdated( DateUtils.getIso8601NoTz( rs.getDate( "lastupdated" ) ) );
        enrollment.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( rs.getDate( "lastupdatedatclient" ) ) );
        enrollment.setProgram( rs.getString( "program_uid" ) );
        enrollment.setStatus( EnrollmentStatus.fromStatusString( rs.getString( "status" ) ) );
        enrollment.setEnrollmentDate( rs.getDate( "enrollmentdate" ) );
        enrollment.setIncidentDate( rs.getDate( "incidentdate" ) );
        enrollment.setFollowup( rs.getBoolean( "followup" ) );
        enrollment.setCompletedDate( rs.getDate( "enddate" ) );
        enrollment.setCompletedBy( rs.getString( "completedby" ) );
        enrollment.setStoredBy( rs.getString( "storedby" ) );
        enrollment.setDeleted( rs.getBoolean( "deleted" ) );
        enrollment.setId( rs.getLong( "programinstanceid" ) );
        return enrollment;
    }
}
