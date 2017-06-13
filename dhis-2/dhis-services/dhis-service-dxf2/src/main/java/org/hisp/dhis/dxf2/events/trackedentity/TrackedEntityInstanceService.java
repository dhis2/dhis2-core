package org.hisp.dhis.dxf2.events.trackedentity;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface TrackedEntityInstanceService
{
    int FLUSH_FREQUENCY = 50;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    List<TrackedEntityInstance> getTrackedEntityInstances( TrackedEntityInstanceQueryParams queryParams, TrackedEntityInstanceParams params );

    int getTrackedEntityInstanceCount( TrackedEntityInstanceQueryParams params );

    TrackedEntityInstance getTrackedEntityInstance( String uid );

    TrackedEntityInstance getTrackedEntityInstance( String uid, TrackedEntityInstanceParams params );

    TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance );

    TrackedEntityInstance getTrackedEntityInstance( org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance,
        TrackedEntityInstanceParams params );

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    ImportSummaries addTrackedEntityInstanceXml( InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummaries addTrackedEntityInstanceJson( InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummaries addTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions );

    ImportSummary addTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions );

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    ImportSummary updateTrackedEntityInstanceXml( String id, InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummary updateTrackedEntityInstanceJson( String id, InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummaries updateTrackedEntityInstances( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions );

    ImportSummary updateTrackedEntityInstance( TrackedEntityInstance trackedEntityInstance, ImportOptions importOptions );

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    ImportSummary deleteTrackedEntityInstance( String uid );

    ImportSummaries deleteTrackedEntityInstances( List<String> uids );
}
