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
package org.hisp.dhis.metadata;

import static java.util.Collections.singletonList;
import static org.hisp.dhis.util.JsonUtils.jsonToObject;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.MetadataValidationException;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.jsonpatch.JsonPatchManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@AllArgsConstructor
public class DefaultMetadataWorkflowService implements MetadataWorkflowService
{

    private final MetadataProposalStore store;

    private final CurrentUserService currentUserService;

    private final UserService userService;

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
        try
        {
            return store.getByUid( uid );
        }
        catch ( NoResultException ex )
        {
            return null;
        }
    }

    @Override
    @Transactional
    public MetadataProposal propose( MetadataProposeParams params )
    {
        validateConsistency( params.getType(), params.getTargetId(), params.getChange() );
        MetadataProposal proposal = MetadataProposal.builder()
            .createdBy( currentUserService.getCurrentUser() )
            .type( params.getType() )
            .target( params.getTarget() )
            .targetId( params.getTargetId() )
            .comment( params.getComment() )
            .change( params.getChange() )
            .build();
        validationDryRun( proposal );
        store.save( proposal );
        return proposal;
    }

    @Override
    @Transactional
    public MetadataProposal adjust( String uid, MetadataAdjustParams params )
    {
        MetadataProposal proposal = getByUid( uid );
        checkHasStatus( proposal, MetadataProposalStatus.NEEDS_UPDATE );
        if ( params != null && (params.getTargetId() != null || params.getChange() != null) )
        {
            validateSameUser( proposal );
            proposal.setTargetId( params.getTargetId() );
            proposal.setChange( params.getChange() );
            proposal.setAutoFields();
        }
        validateConsistency( proposal.getType(), proposal.getTargetId(), proposal.getChange() );
        proposal.setStatus( MetadataProposalStatus.PROPOSED );
        validationDryRun( proposal );
        store.update( proposal );
        return proposal;
    }

    @Override
    @Transactional
    public ImportReport accept( MetadataProposal proposal )
    {
        checkHasStatus( proposal, MetadataProposalStatus.PROPOSED );
        ImportReport report = accept( proposal, ObjectBundleMode.COMMIT );
        proposal.setFinalisedBy( currentUserService.getCurrentUser() );
        proposal.setFinalised( new Date() );
        boolean failed = report.getStatus() != Status.OK || report.hasErrorReports();
        proposal.setStatus( failed
            ? MetadataProposalStatus.NEEDS_UPDATE
            : MetadataProposalStatus.ACCEPTED );
        if ( failed )
        {
            proposal.setReason( createReason( report ) );
        }
        store.update( proposal );
        return report;
    }

    private ImportReport accept( MetadataProposal proposal, ObjectBundleMode mode )
    {
        MetadataProposalType type = proposal.getType();
        if ( type != MetadataProposalType.ADD
            && objectManager.get( proposal.getTarget().getType(), proposal.getTargetId() ) == null )
        {
            return createImportReportWithError( proposal, ErrorCode.E4015, "targetId", "targetId",
                proposal.getTargetId() );
        }
        switch ( type )
        {
        default:
        case ADD:
            return acceptAdd( proposal, mode );
        case REMOVE:
            return acceptRemove( proposal, mode );
        case UPDATE:
            return acceptUpdate( proposal, mode );
        }
    }

    @Override
    @Transactional
    public void oppose( MetadataProposal proposal, String reason )
    {
        checkHasStatus( proposal, MetadataProposalStatus.PROPOSED );
        proposal.setStatus( MetadataProposalStatus.NEEDS_UPDATE );
        proposal.setReason( reason );
        store.update( proposal );
    }

    @Override
    @Transactional
    public void reject( MetadataProposal proposal, String reason )
    {
        checkHasStatus( proposal, MetadataProposalStatus.PROPOSED );
        proposal.setStatus( MetadataProposalStatus.REJECTED );
        proposal.setReason( reason );
        proposal.setFinalised( new Date() );
        proposal.setFinalisedBy( currentUserService.getCurrentUser() );
        store.update( proposal );
    }

    private ImportReport acceptAdd( MetadataProposal proposal, ObjectBundleMode mode )
    {
        Class<? extends IdentifiableObject> objType = proposal.getTarget().getType();
        IdentifiableObject obj = mapJsonChangeToObject( proposal.getChange(), objType );
        if ( obj == null )
        {
            return createJsonErrorReport( proposal );
        }
        MetadataImportParams params = createImportParams( mode, ImportStrategy.CREATE, obj );
        ImportReport report = importService.importMetadata( params );
        if ( report.getStatus() == Status.OK )
        {
            TypeReport typeReport = report.getTypeReport( objType );
            ObjectReport objectReport = new ObjectReport( obj, object -> 0 );
            typeReport.addObjectReport( objectReport );
        }
        return report;
    }

    private ImportReport acceptUpdate( MetadataProposal proposal, ObjectBundleMode mode )
    {
        JsonPatch patch = mapJsonChangeToObject( proposal.getChange(), JsonPatch.class );
        Class<? extends IdentifiableObject> objType = proposal.getTarget().getType();
        if ( patch == null )
        {
            return createJsonErrorReport( proposal );
        }
        IdentifiableObject patched = null;
        try
        {
            patched = patchManager.apply( patch,
                objectManager.get( objType, proposal.getTargetId() ) );
        }
        catch ( JsonPatchException ex )
        {
            log.error( "Failed to apply proposed object update: " + proposal.getChange(), ex );
            return createJsonErrorReport( proposal );
        }
        return importService.importMetadata( createImportParams( mode, ImportStrategy.UPDATE, patched ) );
    }

    private ImportReport acceptRemove( MetadataProposal proposal, ObjectBundleMode mode )
    {
        return importService.importMetadata(
            createImportParams( mode, ImportStrategy.DELETE, objectManager.get(
                proposal.getTarget().getType(), proposal.getTargetId() ) ) );
    }

    private MetadataImportParams createImportParams( ObjectBundleMode mode, ImportStrategy strategy,
        IdentifiableObject obj )
    {
        return new MetadataImportParams()
            .addObject( obj )
            .setImportStrategy( strategy )
            .setImportMode( mode )
            .setUser( mode == ObjectBundleMode.VALIDATE
                ? userService.getUserByUsername( "system" )
                : currentUserService.getCurrentUser() );
    }

    private void checkHasStatus( MetadataProposal proposal, MetadataProposalStatus expected )
    {
        if ( proposal.getStatus() != expected )
        {
            throw new IllegalStateException( "Proposal must be in status " + expected + " for this action" );
        }
    }

    private <T> T mapJsonChangeToObject( JsonNode change, Class<T> type )
    {
        return jsonToObject( change, type, null, jsonMapper );
    }

    private ImportReport createJsonErrorReport( MetadataProposal proposal )
    {
        return createImportReportWithError( proposal, ErrorCode.E4031, "change", "change",
            proposal.getChange().toString() );
    }

    private ImportReport createImportReportWithError( MetadataProposal proposal, ErrorCode errorCode, String property,
        Object... args )
    {
        Class<? extends IdentifiableObject> objType = proposal.getTarget().getType();
        ImportReport importReport = new ImportReport();
        importReport.setStatus( Status.ERROR );
        ObjectReport objectReport = new ObjectReport( objType, null );
        ErrorReport errorReport = new ErrorReport( MetadataProposal.class, errorCode, args );
        errorReport.setErrorProperty( property );
        errorReport.setErrorProperties( singletonList( property ) );
        objectReport.addErrorReport( errorReport );
        TypeReport typeReport = new TypeReport( objType );
        typeReport.addObjectReport( objectReport );
        importReport.addTypeReport( typeReport );
        return importReport;
    }

    private String createReason( ImportReport report )
    {
        StringBuilder reason = new StringBuilder();
        report.forEachErrorReport(
            error -> reason.append( error.getErrorCode() ).append( " " ).append( error.getMessage() ).append( "\n" ) );
        if ( reason.length() > 1024 )
        {
            reason.setLength( 1021 );
            reason.append( "..." );
        }
        return reason.toString();
    }

    private void validateConsistency( MetadataProposalType type, String targetId, JsonNode change )
    {
        if ( type != MetadataProposalType.ADD && targetId == null )
        {
            throw new IllegalStateException( "`targetId` is required for type " + type );
        }
        if ( type != MetadataProposalType.REMOVE && (change == null || change.isNull()) )
        {
            throw new IllegalStateException( "`change` is required for type " + type );
        }
        if ( type == MetadataProposalType.ADD && (!change.isObject() || change.isEmpty()) )
        {
            throw new IllegalStateException( "`change` must be a non empty object for type " + type );
        }
        if ( type == MetadataProposalType.UPDATE && (!change.isArray() || change.isEmpty()) )
        {
            throw new IllegalStateException( "`change` must be a non empty array for type " + type );
        }
    }

    private void validateSameUser( MetadataProposal proposal )
    {
        User currentUser = currentUserService.getCurrentUser();
        if ( currentUser != null && !currentUser.isSuper()
            && currentUser.getUid().equals( proposal.getCreatedBy().getUid() ) )
        {
            throw new IllegalStateException( "Only the user created the proposal can adjust it later." );
        }
    }

    private void validationDryRun( MetadataProposal proposal )
    {
        ImportReport report = accept( proposal, ObjectBundleMode.VALIDATE );
        if ( report.getStatus() != Status.OK || report.hasErrorReports() )
        {
            throw new MetadataValidationException( report, "Proposal contains errors in its definition" );
        }
    }
}
