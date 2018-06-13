package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.schema.descriptors.RelationshipSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController( value = RelationshipSchemaDescriptor.API_ENDPOINT )
@ApiVersion( DhisApiVersion.ALL )
public class RelationshipController
{

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private org.hisp.dhis.relationship.RelationshipService relationshipService_;

    @Autowired
    private WebMessageService webMessageService;

    @GetMapping
    public @ResponseBody
    List<Relationship> getRelationships()
    {
        return relationshipService_.getAll();
    }

    @PostMapping( value = "", consumes = APPLICATION_JSON_VALUE )
    public void postRelationshipJson(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = relationshipService.addRelationshipsJson( inputStream, importOptions );
        response.setContentType( APPLICATION_JSON_VALUE );

        importSummaries.getImportSummaries().stream()
            .filter(
                importSummary -> !importOptions.isDryRun() &&
                    !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                    !importOptions.getImportStrategy().isDelete() &&
                    (!importOptions.getImportStrategy().isSync() || importSummary.getImportCount().getDeleted() == 0) )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + RelationshipSchemaDescriptor.API_ENDPOINT + "/" +
                    importSummary.getReference() ) );

        if ( importSummaries.getImportSummaries().size() == 1 )
        {
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
            importSummary.setImportOptions( importOptions );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                response.setHeader( "Location", getResourcePath( request, importSummary ) );
            }
        }

        response.setStatus( HttpServletResponse.SC_CREATED );
        webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
    }


    @PostMapping( value = "", consumes = APPLICATION_XML_VALUE )
    public void postRelationshipXml(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = relationshipService.addRelationshipsXml( inputStream, importOptions );
        response.setContentType( APPLICATION_XML_VALUE );

        importSummaries.getImportSummaries().stream()
            .filter(
                importSummary -> !importOptions.isDryRun() &&
                    !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
                    !importOptions.getImportStrategy().isDelete() &&
                    (!importOptions.getImportStrategy().isSync() || importSummary.getImportCount().getDeleted() == 0) )
            .forEach( importSummary -> importSummary.setHref(
                ContextUtils.getRootPath( request ) + RelationshipSchemaDescriptor.API_ENDPOINT + "/" +
                    importSummary.getReference() ) );

        if ( importSummaries.getImportSummaries().size() == 1 )
        {
            ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
            importSummary.setImportOptions( importOptions );

            if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
            {
                response.setHeader( "Location", getResourcePath( request, importSummary ) );
            }
        }

        response.setStatus( HttpServletResponse.SC_CREATED );
        webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
    }

    @PutMapping( value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE )
    public @ResponseBody ImportSummary updateRelationshipJson( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = relationshipService.updateRelationshipJson( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return importSummary;
    }

    @PutMapping( value = "/{id}", consumes = MediaType.APPLICATION_XML_VALUE )
    public @ResponseBody ImportSummary updateRelationshipXml( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = relationshipService.updateRelationshipXml( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return importSummary;
    }

    private String getResourcePath( HttpServletRequest request, ImportSummary importSummary )
    {
        return ContextUtils.getContextPath( request ) + "/api/" + "relationships" + "/" +
            importSummary.getReference();
    }
}