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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
public class TrackerObjectDeletionServiceTest extends TrackerTest
{
    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/tracker_basic_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson( "tracker/tracker_basic_data_before_deletion.json" );

        assertEquals( 13, trackerImportParams.getTrackedEntities().size() );
        assertEquals( 2, trackerImportParams.getEnrollments().size() );
        assertEquals( 2, trackerImportParams.getEvents().size() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );

        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.EVENT ).getStats().getCreated(),
            manager.getAll( ProgramStageInstance.class ).size() );
        assertEquals( bundleReport.getTypeReportMap().get( TrackerType.TRACKED_ENTITY ).getStats().getCreated(),
            manager.getAll( TrackedEntityInstance.class ).size() );
        assertEquals( 4, manager.getAll( ProgramInstance.class ).size() );
    }

    @Test
    public void testTrackedEntityInstanceDeletion()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/tracked_entity_basic_data_for_deletion.json" );
        assertEquals( 9, trackerImportParams.getTrackedEntities().size() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerBundleReport bundleReport = trackerBundleService.delete( trackerBundle );

        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
        assertTrue( bundleReport.getTypeReportMap().containsKey( TrackerType.TRACKED_ENTITY ) );
        assertEquals( 9, bundleReport.getTypeReportMap().get( TrackerType.TRACKED_ENTITY ).getStats().getDeleted() );

        // remaining
        assertEquals( 4, manager.getAll( TrackedEntityInstance.class ).size() );
        assertEquals( 2, manager.getAll( ProgramInstance.class ).size() );
    }

    @Test
    public void testEnrollmentDeletion()
        throws IOException
    {
        assertEquals( 4, manager.getAll( ProgramInstance.class ).size() );
        assertEquals( 2, manager.getAll( ProgramStageInstance.class ).size() );

        TrackerImportParams trackerImportParams = fromJson( "tracker/enrollment_basic_data_for_deletion.json" );
        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerBundleReport bundleReport = trackerBundleService.delete( trackerBundle );

        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
        assertTrue( bundleReport.getTypeReportMap().containsKey( TrackerType.ENROLLMENT ) );
        assertEquals( 1, bundleReport.getTypeReportMap().get( TrackerType.ENROLLMENT ).getStats().getDeleted() );

        // remaining
        assertEquals( 3, manager.getAll( ProgramInstance.class ).size() );

        // delete associated events as well
        assertEquals( 1, manager.getAll( ProgramStageInstance.class ).size() );
    }

    @Test
    public void testEventDeletion()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_basic_data_for_deletion.json" );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerBundleReport bundleReport = trackerBundleService.delete( trackerBundle );

        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
        assertTrue( bundleReport.getTypeReportMap().containsKey( TrackerType.EVENT ) );
        assertEquals( 1, bundleReport.getTypeReportMap().get( TrackerType.EVENT ).getStats().getDeleted() );

        // remaining
        assertEquals( 1, manager.getAll( ProgramStageInstance.class ).size() );
    }

    @Test
    public void testNonExistentEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/non_existent_enrollment_basic_data_for_deletion.json" );
        params.setImportStrategy( TrackerImportStrategy.DELETE );
        TrackerImportReport importReport = trackerImportService.importTracker( params );

        assertEquals( TrackerStatus.ERROR, importReport.getStatus() );
        assertTrue( importReport.getValidationReport().hasErrors() );

        List<TrackerErrorReport> trackerErrorReports = importReport.getValidationReport().getErrorReports();
        assertEquals( TrackerErrorCode.E1081, trackerErrorReports.get( 0 ).getErrorCode() );
        // assertEquals( trackerErrorReports.get( 0 ).getErrorKlass(),
        // PreCheckExistenceValidationHook.class );
    }
}
