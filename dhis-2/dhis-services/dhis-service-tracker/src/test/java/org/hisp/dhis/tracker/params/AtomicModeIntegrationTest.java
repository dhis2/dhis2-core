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
package org.hisp.dhis.tracker.params;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AtomicModeIntegrationTest extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Override
    public void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        injectAdminUser();
    }

    @Test
    void testImportSuccessWithAtomicModeObjectIfThereIsAnErrorInOneTEI()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/one_valid_tei_and_one_invalid.json" );
        params.setAtomicMode( AtomicMode.OBJECT );

        TrackerImportReport trackerImportTeiReport = trackerImportService.importTracker( params );

        assertNotNull( trackerImportTeiReport );
        assertEquals( TrackerStatus.OK, trackerImportTeiReport.getStatus() );
        assertEquals( 1, trackerImportTeiReport.getValidationReport().getErrors().size() );
        assertNotNull( trackedEntityInstanceService.getTrackedEntityInstance( "VALIDTEIAAA" ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( "INVALIDTEIA" ) );
    }

    @Test
    void testImportFailWithAtomicModeAllIfThereIsAnErrorInOneTEI()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/one_valid_tei_and_one_invalid.json" );
        params.setAtomicMode( AtomicMode.ALL );

        TrackerImportReport trackerImportTeiReport = trackerImportService.importTracker( params );
        assertNotNull( trackerImportTeiReport );
        assertEquals( TrackerStatus.ERROR, trackerImportTeiReport.getStatus() );
        assertEquals( 1, trackerImportTeiReport.getValidationReport().getErrors().size() );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( "VALIDTEIAAA" ) );
        assertNull( trackedEntityInstanceService.getTrackedEntityInstance( "INVALIDTEIA" ) );
    }
}
