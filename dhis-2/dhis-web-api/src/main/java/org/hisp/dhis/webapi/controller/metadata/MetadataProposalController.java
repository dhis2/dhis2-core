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
package org.hisp.dhis.webapi.controller.metadata;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importReport;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.webapi.controller.exception.NotFoundException.notFoundUid;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.metadata.MetadataValidationException;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.MetadataProposal;
import org.hisp.dhis.metadata.MetadataProposalAdjustParams;
import org.hisp.dhis.metadata.MetadataProposalParams;
import org.hisp.dhis.metadata.MetadataProposalSchemaDescriptor;
import org.hisp.dhis.metadata.MetadataProposalService;
import org.hisp.dhis.metadata.MetadataProposalType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.webapi.controller.AbstractGistReadOnlyController;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;

/**
 * REST API for going through the states of {@link MetadataProposal}s.
 *
 * @author Jan Bernitt
 */
@Controller
@RequestMapping( "/metadata/proposals" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class MetadataProposalController extends AbstractGistReadOnlyController<MetadataProposal>
{

    private final MetadataProposalService service;

    @GetMapping( value = { "/{uid}/", "/{uid}" }, produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<JsonNode> getProposal( @PathVariable( "uid" ) String uid,
        HttpServletRequest request,
        HttpServletResponse response )
        throws NotFoundException
    {
        return getObjectGist( uid, request, response );
    }

    @GetMapping( value = "/", produces = APPLICATION_JSON_VALUE )
    public ResponseEntity<JsonNode> getProposals(
        HttpServletRequest request, HttpServletResponse response )
    {
        return getObjectListGist( request, response );
    }

    @PostMapping( value = "/", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage proposeProposal( @RequestBody MetadataProposalParams params )
    {
        try
        {
            MetadataProposal proposal = service.propose( params );
            return created().setLocation( MetadataProposalSchemaDescriptor.API_ENDPOINT + "/" + proposal.getUid() );
        }
        catch ( IllegalStateException ex )
        {
            return badRequest( ex.getMessage() );
        }
        catch ( MetadataValidationException ex )
        {
            return importReport( ex.getReport() );
        }
    }

    @PutMapping( value = { "/{uid}/", "/{uid}" }, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage adjustProposal( @PathVariable( "uid" ) String uid,
        @RequestBody MetadataProposalAdjustParams params )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        try
        {
            service.adjust( uid, params );
            return ok();
        }
        catch ( IllegalStateException ex )
        {
            return badRequest( ex.getMessage() );
        }
        catch ( MetadataValidationException ex )
        {
            return importReport( ex.getReport() );
        }
    }

    @PostMapping( value = { "/{uid}/", "/{uid}" }, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage acceptProposal( @PathVariable( "uid" ) String uid )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        ImportReport report = service.accept( proposal );
        if ( report.getStatus() != Status.OK )
        {
            return importReport( report );
        }
        if ( proposal.getType() == MetadataProposalType.ADD )
        {
            String objUid = report.getFirstObjectReport().getUid();
            Schema schema = schemaService.getSchema( proposal.getTarget().getType() );
            return created( schema.getSingular() + " created" )
                .setLocation( schema.getRelativeApiEndpoint() + "/" + objUid );
        }
        return ok();
    }

    @PatchMapping( value = { "/{uid}/", "/{uid}" }, consumes = MediaType.TEXT_PLAIN_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void opposeProposal( @PathVariable( "uid" ) String uid, @RequestBody( required = false ) String reason )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        service.oppose( proposal, reason );
    }

    @DeleteMapping( value = { "/{uid}/", "/{uid}" }, consumes = MediaType.TEXT_PLAIN_VALUE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void rejectProposal( @PathVariable( "uid" ) String uid, @RequestBody( required = false ) String reason )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        service.reject( proposal, reason );
    }
}
