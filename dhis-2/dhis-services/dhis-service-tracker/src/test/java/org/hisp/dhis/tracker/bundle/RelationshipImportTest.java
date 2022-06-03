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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.Assertions.assertNoImportErrors;

import java.io.IOException;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class RelationshipImportTest extends TrackerTest
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
        assertNoImportErrors(
            trackerImportService.importTracker( fromJson( "tracker/single_tei.json", userA.getUid() ) ) );
        assertNoImportErrors(
            trackerImportService.importTracker( fromJson( "tracker/single_enrollment.json", userA.getUid() ) ) );
        assertNoImportErrors(
            trackerImportService.importTracker( fromJson( "tracker/single_event.json", userA.getUid() ) ) );
        manager.flush();
    }

    @Test
    void successImportingRelationships()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/relationships.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertThat( trackerImportReport.getStatus(), is( TrackerStatus.OK ) );
        assertThat( trackerImportReport.getStats().getCreated(), is( 2 ) );
    }

    @Test
    void successUpdateRelationships()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/relationships.json" );
        trackerImportService.importTracker( trackerImportParams );
        trackerImportParams = fromJson( "tracker/relationshipToUpdate.json" );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertThat( trackerImportReport.getStatus(), is( TrackerStatus.OK ) );
        assertThat( trackerImportReport.getStats().getCreated(), is( 0 ) );
        assertThat( trackerImportReport.getStats().getIgnored(), is( 1 ) );
    }
}
