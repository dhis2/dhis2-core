package org.hisp.dhis.webapi.controller.metadata;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.created;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.webapi.controller.exception.NotFoundException.notFoundUid;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.metadata.MetadataProposal;
import org.hisp.dhis.metadata.MetadataProposalParams;
import org.hisp.dhis.metadata.MetadataProposalService;
import org.hisp.dhis.webapi.controller.AbstractGistReadOnlyController;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * @author Jan Bernitt
 */
@Controller
@RequestMapping( "/metadata/proposals" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class MetadataProposalController extends AbstractGistReadOnlyController<MetadataProposal>
{

    private final MetadataProposalService service;

    @GetMapping( value = "/{uid}", produces = APPLICATION_JSON_VALUE )
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
        service.propose( params );
        return created();
    }

    @PostMapping( value = "/{uid}/accept", produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage acceptProposal( @PathVariable( "uid" ) String uid )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        service.accept( proposal );
        return ok();
    }

    @PutMapping( value = "/{uid}/comment", consumes = MediaType.TEXT_PLAIN_VALUE, produces = APPLICATION_JSON_VALUE )
    @ResponseBody
    public WebMessage commentProposal( @PathVariable( "uid" ) String uid, @RequestBody String comment )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        service.comment( proposal, comment );
        return ok();
    }

    @DeleteMapping( "/{uid}" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void rejectProposal( @PathVariable( "uid" ) String uid )
        throws NotFoundException
    {
        MetadataProposal proposal = service.getByUid( uid );
        if ( proposal == null )
            throw notFoundUid( uid );
        service.reject( proposal );
    }
}
