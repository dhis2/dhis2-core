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
package org.hisp.dhis.tracker.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed
 */
public class EnrollmentTest
    extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    private User userA;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );

        userA = userService.getUser( "M5zQapPyTZI" );

        TrackerImportParams teiParams = fromJson( "tracker/single_tei.json", userA.getUid() );
        assertNoImportErrors( trackerImportService.importTracker( teiParams ) );

        TrackerImportParams enrollmentParams = fromJson( "tracker/single_enrollment.json", userA.getUid() );
        assertNoImportErrors( trackerImportService.importTracker( enrollmentParams ) );

    }

    @Test
    public void testClientDatesForTeiEnrollmentEvent()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/single_event.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        TrackerImportParams teiParams = fromJson( "tracker/single_tei.json", userA.getUid() );
        TrackerImportParams enrollmentParams = fromJson( "tracker/single_enrollment.json", userA.getUid() );

        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<TrackedEntityInstance> teis = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, teis.size() );

        TrackedEntityInstance tei = teis.get( 0 );

        assertNotNull( tei.getCreatedAtClient() );
        assertNotNull( tei.getLastUpdatedAtClient() );

        assertEquals(
            DateUtils.fromInstant( teiParams.getTrackedEntities().get( 0 ).getCreatedAtClient() ),
            tei.getCreatedAtClient() );
        assertEquals(
            DateUtils.fromInstant( teiParams.getTrackedEntities().get( 0 ).getUpdatedAtClient() ),
            tei.getLastUpdatedAtClient() );

        Set<ProgramInstance> pis = tei.getProgramInstances();
        assertEquals( 1, pis.size() );
        ProgramInstance pi = pis.iterator().next();

        assertNotNull( pi.getCreatedAtClient() );
        assertNotNull( pi.getLastUpdatedAtClient() );

        assertEquals( DateUtils.fromInstant( enrollmentParams.getEnrollments().get( 0 ).getCreatedAtClient() ),
            pi.getCreatedAtClient() );
        assertEquals(
            DateUtils.fromInstant( enrollmentParams.getEnrollments().get( 0 ).getUpdatedAtClient() ),
            pi.getLastUpdatedAtClient() );

        Set<ProgramStageInstance> psis = pi.getProgramStageInstances();
        assertEquals( 1, psis.size() );
        ProgramStageInstance psi = psis.iterator().next();

        assertNotNull( psi.getCreatedAtClient() );
        assertNotNull( psi.getLastUpdatedAtClient() );

        assertEquals(
            DateUtils.fromInstant( trackerImportParams.getEvents().get( 0 ).getCreatedAtClient() ),
            psi.getCreatedAtClient() );
        assertEquals(
            DateUtils.fromInstant( trackerImportParams.getEvents().get( 0 ).getUpdatedAtClient() ),
            psi.getLastUpdatedAtClient() );
    }

    @Test
    public void testUpdateEnrollment()
        throws IOException
    {
        TrackerImportParams enrollmentParams = fromJson( "tracker/single_enrollment.json", userA.getUid() );

        List<ProgramInstance> pis = manager.getAll( ProgramInstance.class );

        assertEquals( 1, pis.size() );
        ProgramInstance pi = pis.iterator().next();

        compareEnrollmentBasicProperties( pi, enrollmentParams.getEnrollments().get( 0 ) );

        Enrollment updatedEnrollment = enrollmentParams.getEnrollments().get( 0 );
        updatedEnrollment.setStatus( EnrollmentStatus.COMPLETED );
        updatedEnrollment.setCompletedBy( "admin" );
        updatedEnrollment.setCompletedAt( Instant.now() );
        updatedEnrollment.setCreatedAtClient( Instant.now() );
        updatedEnrollment.setUpdatedAtClient( Instant.now() );
        updatedEnrollment.setEnrolledAt( Instant.now() );
        updatedEnrollment.setOccurredAt( Instant.now() );

        enrollmentParams.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );
        TrackerImportReport updatedReport = trackerImportService.importTracker( enrollmentParams );
        assertNoImportErrors( updatedReport );
        assertEquals( 1, updatedReport.getStats().getUpdated() );

        pis = manager.getAll( ProgramInstance.class );

        assertEquals( 1, pis.size() );
        pi = pis.iterator().next();
        compareEnrollmentBasicProperties( pi, updatedEnrollment );
    }

    private void compareEnrollmentBasicProperties( ProgramInstance pi, Enrollment enrollment )
    {
        assertEquals( DateUtils.fromInstant( enrollment.getCompletedAt() ), pi.getEndDate() );
        assertEquals( DateUtils.fromInstant( enrollment.getEnrolledAt() ), pi.getEnrollmentDate() );
        assertEquals( DateUtils.fromInstant( enrollment.getOccurredAt() ), pi.getIncidentDate() );
        assertEquals( DateUtils.fromInstant( enrollment.getCreatedAtClient() ), pi.getCreatedAtClient() );
        assertEquals( DateUtils.fromInstant( enrollment.getUpdatedAtClient() ), pi.getLastUpdatedAtClient() );
        assertEquals( enrollment.getCompletedBy(), pi.getCompletedBy() );
        assertEquals( enrollment.getStatus().toString(), pi.getStatus().toString() );

    }

}
