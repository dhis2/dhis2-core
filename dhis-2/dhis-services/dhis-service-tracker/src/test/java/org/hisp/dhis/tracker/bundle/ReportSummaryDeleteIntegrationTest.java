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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class ReportSummaryDeleteIntegrationTest extends TrackerTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/tracker_basic_metadata.json" );
        injectAdminUser();
        TrackerImportParams params = fromJson( "tracker/tracker_basic_data_before_deletion.json" );
        assertEquals( 13, params.getTrackedEntities().size() );
        assertEquals( 2, params.getEnrollments().size() );
        assertEquals( 2, params.getEvents().size() );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        TrackerBundleReport bundleReport = trackerImportReport.getBundleReport();

        assertImportedObjects( 13, bundleReport, TrackerType.TRACKED_ENTITY );
        assertImportedObjects( 2, bundleReport, TrackerType.ENROLLMENT );
        assertImportedObjects( 2, bundleReport, TrackerType.EVENT );
        assertEquals( 6, manager.getAll( ProgramInstance.class ).size() );
        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.EVENT ).getStats().getCreated(),
            manager.getAll( ProgramStageInstance.class ).size() );
    }

    @Test
    void testTrackedEntityInstanceDeletion()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/tracked_entity_basic_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );
        assertEquals( 9, params.getTrackedEntities().size() );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertDeletedObjects( 9, trackerImportReport.getBundleReport(), TrackerType.TRACKED_ENTITY );
        // remaining
        assertEquals( 4, manager.getAll( TrackedEntityInstance.class ).size() );
        assertEquals( 4, manager.getAll( ProgramInstance.class ).size() );
    }

    @Test
    void testEnrollmentDeletion()
        throws IOException
    {
        dbmsManager.clearSession();
        assertEquals( 2, manager.getAll( ProgramStageInstance.class ).size() );
        TrackerImportParams params = fromJson( "tracker/enrollment_basic_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        assertDeletedObjects( 1, trackerImportReport.getBundleReport(), TrackerType.ENROLLMENT );
        // remaining
        assertEquals( 5, manager.getAll( ProgramInstance.class ).size() );
        // delete associated events as well
        assertEquals( 1, manager.getAll( ProgramStageInstance.class ).size() );
    }

    @Test
    void testEventDeletion()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/event_basic_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertDeletedObjects( 1, trackerImportReport.getBundleReport(), TrackerType.EVENT );
        // remaining
        assertEquals( 1, manager.getAll( ProgramStageInstance.class ).size() );
    }

    @Test
    void testNonExistentEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/non_existent_enrollment_basic_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport importReport = trackerImportService.importTracker( params );

        assertEquals( TrackerStatus.ERROR, importReport.getStatus() );
        assertTrue( importReport.getValidationReport().hasErrors() );
        List<TrackerErrorReport> trackerErrorReports = importReport.getValidationReport().getErrors();
        assertEquals( TrackerErrorCode.E1081, trackerErrorReports.get( 0 ).getErrorCode() );
    }

    @Test
    void testDeleteMultipleEntities()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/tracker_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertDeletedObjects( 1, trackerImportReport.getBundleReport(), TrackerType.ENROLLMENT );
        assertDeletedObjects( 1, trackerImportReport.getBundleReport(), TrackerType.TRACKED_ENTITY );
    }

    private void assertImportedObjects( int expected, TrackerBundleReport bundleReport, TrackerType trackedEntityType )
    {
        assertTrue( bundleReport.getTypeReportMap().containsKey( trackedEntityType ) );
        assertEquals( expected,
            bundleReport.getTypeReportMap().get( trackedEntityType ).getStats().getCreated() );
        assertEquals( expected,
            bundleReport.getTypeReportMap().get( trackedEntityType ).getObjectReportMap().size() );
    }

    private void assertDeletedObjects( int expected, TrackerBundleReport bundleReport, TrackerType trackedEntityType )
    {
        assertTrue( bundleReport.getTypeReportMap().containsKey( trackedEntityType ) );
        assertEquals( expected,
            bundleReport.getTypeReportMap().get( trackedEntityType ).getStats().getDeleted() );
        assertEquals( expected,
            bundleReport.getTypeReportMap().get( trackedEntityType ).getObjectReportMap().size() );
    }
}
