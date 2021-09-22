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
package org.hisp.dhis.metadata;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.JsonPatchManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@AllArgsConstructor
public class DefaultMetadataProposalService implements MetadataProposalService
{

    private final MetadataProposalStore store;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager objectManager;

    private final MetadataImportService importService;

    private final SchemaService schemaService;

    private final JsonPatchManager patchManager;

    private final ObjectMapper jsonMapper;

    @PostConstruct
    private void init()
    {
        schemaService.register( new MetadataProposalSchemaDescriptor() );
    }

    @Override
    @Transactional( readOnly = true )
    public MetadataProposal getByUid( String uid )
    {
        return store.getByUid( uid );
    }

    @Override
    @Transactional
    public MetadataProposal propose( MetadataProposalParams params )
    {
        MetadataProposal proposal = MetadataProposal.builder()
            .createdBy( currentUserService.getCurrentUser() )
            .type( params.getType() )
            .target( params.getTarget() )
            .targetUid( params.getTargetUid() )
            .comment( params.getComment() )
            .change( params.getChange() )
            .build();
        store.save( proposal );
        return proposal;
    }

    @Override
    @Transactional
    public ImportReport accept( MetadataProposal proposal )
    {
        ImportReport report = new ImportReport();
        switch ( proposal.getType() )
        {
        case ADD:
            report = acceptAdd( proposal );
            break;
        case REMOVE:
            report = acceptRemove( proposal );
            break;
        case UPDATE:
            report = acceptUpdate( proposal );
            break;
        }
        proposal.setAcceptedBy( currentUserService.getCurrentUser() );
        proposal.setStatus( report.hasErrorReports()
            ? MetadataProposalStatus.FAILED
            : MetadataProposalStatus.ACCEPTED );
        store.update( proposal );
        return report;
    }

    @Override
    @Transactional
    public void comment( MetadataProposal proposal, String comment )
    {
        proposal.setComment( comment );
        store.update( proposal );
    }

    @Override
    @Transactional
    public void reject( MetadataProposal proposal )
    {
        if ( proposal.getStatus() != MetadataProposalStatus.PROPOSED )
        {
            throw new IllegalStateException( "Proposal already processed" );
        }
        proposal.setStatus( MetadataProposalStatus.REJECTED );
        store.update( proposal );
    }

    private ImportReport acceptAdd( MetadataProposal proposal )
    {
        IdentifiableObject obj = mapJsonChangeToObject( proposal.getChange(), proposal.getTarget().getType() );
        if ( obj == null )
        {
            // TODO add error
            return new ImportReport();
        }
        MetadataImportParams params = new MetadataImportParams();
        params.addObject( obj );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setUser( currentUserService.getCurrentUser() );
        ImportReport report = importService.importMetadata( params );
        if ( report.getStatus() == Status.OK )
        {
            TypeReport typeReport = report.getTypeReport( proposal.getTarget().getType() );
            ObjectReport objectReport = new ObjectReport( obj, object -> 0 );
            typeReport.addObjectReport( objectReport );
        }
        return report;
    }

    private <T> T mapJsonChangeToObject( JsonNode change, Class<T> type )
    {
        try
        {
            return jsonMapper.treeToValue( change, type );

        }
        catch ( JsonProcessingException ex )
        {
            log.error( "Failed to map proposal change to type " + type.getSimpleName(), ex );
            return null;
        }
    }

    private ImportReport acceptUpdate( MetadataProposal proposal )
    {
        JsonPatch patch = mapJsonChangeToObject( proposal.getChange(), JsonPatch.class );
        IdentifiableObject patched = null;
        try
        {
            patched = patchManager.apply( patch,
                objectManager.get( proposal.getTarget().getType(), proposal.getTargetUid() ) );
        }
        catch ( JsonPatchException ex )
        {
            log.error( "Failed to apply proposed object update: " + proposal.getChange(), ex );
            // TODO add error
            return new ImportReport();
        }
        MetadataImportParams params = new MetadataImportParams();
        params.addObject( patched );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setUser( currentUserService.getCurrentUser() );
        return importService.importMetadata( params );
    }

    private ImportReport acceptRemove( MetadataProposal proposal )
    {
        IdentifiableObject deleted = objectManager.get( proposal.getTargetUid() );
        MetadataImportParams params = new MetadataImportParams();
        params.addObject( deleted );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setUser( currentUserService.getCurrentUser() );
        return importService.importMetadata( params );
    }
}
