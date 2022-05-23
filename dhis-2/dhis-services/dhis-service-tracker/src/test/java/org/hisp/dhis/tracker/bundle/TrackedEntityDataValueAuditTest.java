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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */
public class TrackedEntityDataValueAuditTest extends TrackerTest
{
    private static final String PSI = "D9PbzJY8bJO";

    public static final String DE = "DATAEL00001";

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        injectAdminUser();
    }

    @Test
    void testTrackedEntityDataValueAuditCreate()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_and_enrollment_with_data_values.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        logTrackerErrors( trackerImportReport );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        trackerImportParams = fromJson( "tracker/event_with_data_values_for_update_audit.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        logTrackerErrors( trackerImportReport );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        trackerImportParams = fromJson( "tracker/event_with_data_values_for_delete_audit.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        logTrackerErrors( trackerImportReport );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        DataElement dataElement = manager.search( DataElement.class, DE );
        ProgramStageInstance psi = manager.search( ProgramStageInstance.class, PSI );
        assertNotNull( dataElement );
        assertNotNull( psi );

        List<TrackedEntityDataValueAudit> createdAudit = dataValueAuditService.getTrackedEntityDataValueAudits(
            Lists.newArrayList( dataElement ), Lists.newArrayList( psi ), AuditType.CREATE );
        List<TrackedEntityDataValueAudit> updatedAudit = dataValueAuditService.getTrackedEntityDataValueAudits(
            Lists.newArrayList( dataElement ), Lists.newArrayList( psi ), AuditType.UPDATE );
        List<TrackedEntityDataValueAudit> deletedAudit = dataValueAuditService.getTrackedEntityDataValueAudits(
            Lists.newArrayList( dataElement ), Lists.newArrayList( psi ), AuditType.DELETE );

        assertNotNull( createdAudit );
        assertNotNull( updatedAudit );
        assertNotNull( deletedAudit );
        assertFalse( createdAudit.isEmpty() );
        assertFalse( updatedAudit.isEmpty() );
        assertFalse( deletedAudit.isEmpty() );

        createdAudit.forEach( a -> {
            assertEquals( a.getAuditType(), AuditType.CREATE );
            assertEquals( a.getDataElement().getUid(), dataElement.getUid() );
            assertEquals( a.getProgramStageInstance().getUid(), psi.getUid() );
        } );

        updatedAudit.forEach( a -> {
            assertEquals( a.getAuditType(), AuditType.UPDATE );
            assertEquals( a.getDataElement().getUid(), dataElement.getUid() );
            assertEquals( a.getProgramStageInstance().getUid(), psi.getUid() );
        } );

        deletedAudit.forEach( a -> {
            assertEquals( a.getAuditType(), AuditType.DELETE );
            assertEquals( a.getDataElement().getUid(), dataElement.getUid() );
            assertEquals( a.getProgramStageInstance().getUid(), psi.getUid() );
        } );
    }
}
