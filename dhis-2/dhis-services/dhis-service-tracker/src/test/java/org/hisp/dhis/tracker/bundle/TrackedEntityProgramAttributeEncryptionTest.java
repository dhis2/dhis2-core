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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
class TrackedEntityProgramAttributeEncryptionTest extends TrackerTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/te_program_with_tea_encryption_metadata.json" );
        injectAdminUser();
    }

    @Test
    void testTrackedEntityProgramAttributeEncryptedValue()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/te_program_with_tea_encryption_data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        log.info( trackerImportReport.toString() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );
        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues( trackedEntityInstance );
        assertEquals( 5, attributeValues.size() );
        // not really a great test, but we are using a random seed for salt, so
        // it changes on every run... we might want to
        // add another EncryptionConfig test profile
        RowCallbackHandler handler = resultSet -> assertNotNull( resultSet.getString( "encryptedvalue" ) );
        jdbcTemplate.query( "select * from trackedentityattributevalue where encryptedvalue is not null ", handler );
    }
}
