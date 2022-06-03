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

import static org.hisp.dhis.tracker.Assertions.assertNoImportErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackedEntityProgramAttributeFileResourceTest extends TrackerTest
{

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private FileResourceService fileResourceService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/te_program_with_tea_fileresource_metadata.json" );
        injectAdminUser();
    }

    @Test
    void testTrackedEntityProgramAttributeFileResourceValue()
        throws IOException
    {
        FileResource fileResource = new FileResource( "test.pdf", "application/pdf", 0,
            "d41d8cd98f00b204e9800998ecf8427e", FileResourceDomain.DOCUMENT );
        fileResource.setUid( "Jzf6hHNP7jx" );
        File file = File.createTempFile( "file-resource", "test" );
        fileResourceService.saveFileResource( fileResource, file );
        assertFalse( fileResource.isAssigned() );
        TrackerImportParams trackerImportParams = fromJson( "tracker/te_program_with_tea_fileresource_data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertNoImportErrors( trackerImportReport );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );
        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues( trackedEntityInstance );
        assertEquals( 5, attributeValues.size() );
        fileResource = fileResourceService.getFileResource( fileResource.getUid() );
        assertTrue( fileResource.isAssigned() );
    }
}
