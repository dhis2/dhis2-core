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
package org.hisp.dhis.tracker.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.tracker.report.Status;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportSummaryIntegrationTest extends TrackerTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    private User userA;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );

        userA = userService.getUser( "M5zQapPyTZI" );
        injectSecurityContext( userA );
    }

    @Test
    void testStatsCountForOneCreatedTEI()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );
        params.setAtomicMode( AtomicMode.OBJECT );

        ImportReport trackerImportTeiReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportTeiReport );
        assertEquals( 1, trackerImportTeiReport.getStats().getCreated() );
        assertEquals( 0, trackerImportTeiReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportTeiReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportTeiReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneCreatedAndOneUpdatedTEI()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_tei_and_one_new_tei.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportTeiReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportTeiReport );
        assertEquals( 1, trackerImportTeiReport.getStats().getCreated() );
        assertEquals( 1, trackerImportTeiReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportTeiReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportTeiReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneCreatedAndOneUpdatedTEIAndOneInvalidTEI()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_tei_and_one_new_tei_and_one_invalid_tei.json" );
        params.setUserId( userA.getUid() );
        params.setAtomicMode( AtomicMode.OBJECT );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportTeiReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportTeiReport );
        assertEquals( Status.OK, trackerImportTeiReport.getStatus() );
        assertEquals( 1, trackerImportTeiReport.getValidationReport().getErrors().size() );
        assertEquals( 1, trackerImportTeiReport.getStats().getCreated() );
        assertEquals( 1, trackerImportTeiReport.getStats().getUpdated() );
        assertEquals( 1, trackerImportTeiReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportTeiReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneCreatedEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEnrollmentReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEnrollmentReport );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getCreated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneCreatedEnrollmentAndUpdateSameEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEnrollmentReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEnrollmentReport );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getCreated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getDeleted() );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        trackerImportEnrollmentReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEnrollmentReport );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getCreated() );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneUpdateEnrollmentAndOneCreatedEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/one_update_tei_and_one_new_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_enrollment_and_one_new_enrollment.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEnrollmentReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEnrollmentReport );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getCreated() );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneUpdateEnrollmentAndOneCreatedEnrollmentAndOneInvalidEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/three_teis.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_and_one_new_and_one_invalid_enrollment.json" );

        params.setUserId( userA.getUid() );
        params.setAtomicMode( AtomicMode.OBJECT );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEnrollmentReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportEnrollmentReport );
        assertEquals( Status.OK, trackerImportEnrollmentReport.getStatus() );
        assertEquals( 1, trackerImportEnrollmentReport.getValidationReport().getErrors().size() );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getCreated() );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getUpdated() );
        assertEquals( 1, trackerImportEnrollmentReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEnrollmentReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneCreatedEvent()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_event.json" );
        params.setUserId( userA.getUid() );

        ImportReport trackerImportEventReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEventReport );
        assertEquals( 1, trackerImportEventReport.getStats().getCreated() );
        assertEquals( 0, trackerImportEventReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEventReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEventReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneUpdateEventAndOneNewEvent()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_event.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_event_and_one_new_event.json" );
        params.setUserId( userA.getUid() );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEventReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportEventReport );
        assertEquals( 1, trackerImportEventReport.getStats().getCreated() );
        assertEquals( 1, trackerImportEventReport.getStats().getUpdated() );
        assertEquals( 0, trackerImportEventReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEventReport.getStats().getDeleted() );
    }

    @Test
    void testStatsCountForOneUpdateEventAndOneNewEventAndOneInvalidEvent()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/single_tei.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_enrollment.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/single_event.json" );
        params.setUserId( userA.getUid() );

        trackerImportService.importTracker( params );

        params = fromJson( "tracker/one_update_and_one_new_and_one_invalid_event.json" );
        params.setUserId( userA.getUid() );
        params.setAtomicMode( AtomicMode.OBJECT );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        ImportReport trackerImportEventReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportEventReport );
        assertEquals( Status.OK, trackerImportEventReport.getStatus() );
        assertEquals( 1, trackerImportEventReport.getValidationReport().getErrors().size() );
        assertEquals( 1, trackerImportEventReport.getStats().getCreated() );
        assertEquals( 1, trackerImportEventReport.getStats().getUpdated() );
        assertEquals( 1, trackerImportEventReport.getStats().getIgnored() );
        assertEquals( 0, trackerImportEventReport.getStats().getDeleted() );
    }
}
