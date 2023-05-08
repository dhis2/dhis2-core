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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LastUpdateImportTest extends TrackerTest
{
    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private TrackedEntityService trackedEntityService;

    private org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        injectAdminUser();
        TrackerImportParams trackerImportParams = fromJson( "tracker/single_tei.json" );
        assertNoErrors( trackerImportService.importTracker( trackerImportParams ) );
        trackedEntity = trackerImportParams.getTrackedEntities().get( 0 );
        assertNoErrors( trackerImportService.importTracker( fromJson( "tracker/single_enrollment.json" ) ) );
        manager.flush();
    }

    @Test
    void shouldUpdateTeiIfTeiIsUpdated()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/single_tei.json" );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        Attribute attribute = Attribute.builder()
            .attribute( MetadataIdentifier.ofUid( "toUpdate000" ) )
            .value( "value" )
            .build();
        trackedEntity.setAttributes( Collections.singletonList( attribute ) );
        Date lastUpdateBefore = trackedEntityService
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getLastUpdated();
        assertNoErrors( trackerImportService.importTracker( trackerImportParams ) );
        assertTrue( manager.get( TrackedEntity.class, trackedEntity.getTrackedEntity() ).getLastUpdated()
            .getTime() > lastUpdateBefore.getTime() );
    }

    @Test
    void shouldUpdateTeiIfEventIsUpdated()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_with_data_values.json" );
        Date lastUpdateBefore = trackedEntityService
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getLastUpdated();
        assertNoErrors( trackerImportService.importTracker( trackerImportParams ) );

        trackerImportParams = fromJson( "tracker/event_with_updated_data_values.json" );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        assertNoErrors( trackerImportService.importTracker( trackerImportParams ) );
        manager.clear();
        assertTrue( manager.get( TrackedEntity.class, trackedEntity.getTrackedEntity() ).getLastUpdated()
            .getTime() > lastUpdateBefore.getTime() );
    }

    @Test
    void shouldUpdateTeiIfEnrollmentIsUpdated()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/single_enrollment.json" );
        Date lastUpdateBefore = trackedEntityService
            .getTrackedEntityInstance( trackedEntity.getTrackedEntity() ).getLastUpdated();
        Enrollment enrollment = trackerImportParams.getEnrollments().get( 0 );
        enrollment.setStatus( EnrollmentStatus.COMPLETED );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        ImportReport importReport = trackerImportService.importTracker( trackerImportParams );
        assertNoErrors( importReport );
        manager.clear();
        assertTrue( manager.get( TrackedEntity.class, trackedEntity.getTrackedEntity() ).getLastUpdated()
            .getTime() > lastUpdateBefore.getTime() );
    }
}
