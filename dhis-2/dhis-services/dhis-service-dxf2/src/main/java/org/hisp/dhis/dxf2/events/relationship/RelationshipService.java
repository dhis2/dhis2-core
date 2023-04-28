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
package org.hisp.dhis.dxf2.events.relationship;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingAndSortingCriteriaAdapter;

/**
 * @author Stian Sandvold
 */
public interface RelationshipService
{

    int FLUSH_FREQUENCY = 100;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    List<Relationship> getRelationshipsByTrackedEntityInstance( TrackedEntityInstance tei,
        PagingAndSortingCriteriaAdapter criteria,
        boolean skipAccessValidation );

    List<Relationship> getRelationshipsByProgramInstance( ProgramInstance pi,
        PagingAndSortingCriteriaAdapter criteria,
        boolean skipAccessValidation );

    List<Relationship> getRelationshipsByEvent( Event psi,
        PagingAndSortingCriteriaAdapter pagingAndSortingCriteriaAdapter,
        boolean skipAccessValidation );

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    ImportSummaries addRelationshipsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries addRelationshipsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries addRelationships( List<Relationship> relationships, ImportOptions importOptions );

    ImportSummary addRelationship( Relationship relationships, ImportOptions importOptions );

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    ImportSummary updateRelationshipXml( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummary updateRelationshipJson( String id, InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries updateRelationships( List<Relationship> relationships, ImportOptions importOptions );

    ImportSummary updateRelationship( Relationship relationship, ImportOptions importOptions );

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    ImportSummary deleteRelationship( String uid );

    ImportSummaries deleteRelationships( List<Relationship> relationships, ImportOptions importOptions );

    Optional<Relationship> findRelationshipByUid( String id );

    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------

    Optional<Relationship> findRelationship( org.hisp.dhis.relationship.Relationship dao, RelationshipParams params,
        User user );

    ImportSummaries processRelationshipList( List<Relationship> relationships, ImportOptions importOptions );
}
