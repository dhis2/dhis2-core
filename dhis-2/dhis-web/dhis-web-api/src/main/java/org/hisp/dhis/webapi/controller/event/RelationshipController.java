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

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.schema.descriptors.RelationshipSchemaDescriptor;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@RestController
@RequestMapping( value = RelationshipSchemaDescriptor.API_ENDPOINT )
@ApiVersion( include = { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class RelationshipController
{

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @GetMapping
    public List<Relationship> getRelationships(
        @RequestParam( required = false, defaultValue = "") String tei,
        @RequestParam( required = false, defaultValue = "" ) String pi,
        @RequestParam( required = false, defaultValue = "" ) String psi
    )
        throws WebMessageException
    {
        if ( !tei.isEmpty() )
        {
            TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( tei );

            if ( trackedEntityInstance != null )
            {
                return relationshipService.getRelationshipsByTrackedEntityInstance( trackedEntityInstance, false );
            }
            else
            {
                throw new WebMessageException(
                    WebMessageUtils.notFound( "No trackedEntityInstance '" + tei + "' found." ) );
            }
        }
        else if ( !pi.isEmpty() )
        {
            ProgramInstance programInstance = programInstanceService.getProgramInstance( pi );

            if ( programInstance != null )
            {
                return relationshipService.getRelationshipsByProgramInstance( programInstance, false );
            }
            else
            {
                throw new WebMessageException( WebMessageUtils.notFound( "No programInstance '" + pi + "' found." ) );
            }
        }
        else if ( !psi.isEmpty() )
        {
            ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( psi );

            if ( programStageInstance != null )
            {
                return relationshipService.getRelationshipsByProgramStageInstance( programStageInstance, false );
            }
            else
            {
                throw new WebMessageException(
                    WebMessageUtils.notFound( "No programStageInstance '" + psi + "' found." ) );
            }
        }
        else
        {
            throw new WebMessageException(
                WebMessageUtils.badRequest( "Missing required parameter 'tei', 'pi' or 'psi'." ) );
        }
    }

    @GetMapping("/{id}")
    public Relationship getRelationship(
        @PathVariable String id
    )
        throws WebMessageException
    {
        Relationship relationship = relationshipService.getRelationshipByUid( id );

        if (relationship == null)
        {
            throw new WebMessageException( WebMessageUtils.notFound( "No relationship with id '" + id + "' was found." ) );
        }

        return relationship;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @PostMapping( value = "", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_UTF8_VALUE )
    public WebMessage postRelationshipJson(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions,
        HttpServletRequest request
    )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = relationshipService.addRelationshipsJson( inputStream, importOptions );

        importSummaries.getImportSummaries().stream()
            .filter( filterImportSummary( importOptions ) )
            .forEach( setImportSummaryHref( request ) );

        return WebMessageUtils.importSummaries( importSummaries );
    }

    @PostMapping( value = "", consumes = APPLICATION_XML_VALUE, produces = APPLICATION_XML_VALUE )
    public WebMessage postRelationshipXml(
        @RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
        ImportOptions importOptions,
        HttpServletRequest request
    )
        throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = relationshipService.addRelationshipsXml( inputStream, importOptions );

        importSummaries.getImportSummaries().stream()
            .filter( filterImportSummary( importOptions ) )
            .forEach( setImportSummaryHref( request ) );

        return WebMessageUtils.importSummaries( importSummaries );
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @PutMapping( path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_UTF8_VALUE )
    public WebMessage updateRelationshipJson(
        @PathVariable String id,
        ImportOptions importOptions,
        HttpServletRequest request
    )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = relationshipService.updateRelationshipJson( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return WebMessageUtils.importSummary( importSummary );
    }

    @PutMapping( path = "/{id}", consumes = MediaType.APPLICATION_XML_VALUE, produces = APPLICATION_XML_VALUE )
    public ImportSummary updateRelationshipXml(
        @PathVariable String id,
        ImportOptions importOptions,
        HttpServletRequest request
    )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = relationshipService.updateRelationshipXml( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );

        return importSummary;
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @DeleteMapping( value = "/{id}" )
    public WebMessage deleteRelationship(
        @PathVariable String id
    )
    {
        return WebMessageUtils.importSummary( relationshipService.deleteRelationship( id ) );
    }

    // -------------------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------------------

    /**
     * Returns a Predicate that filters out ImportSummary depending on importOptions and the summary itself
     *
     * @param importOptions
     * @return a Predicate for ImportSummary
     */
    private Predicate<ImportSummary> filterImportSummary( ImportOptions importOptions )
    {
        return importSummary -> !importOptions.isDryRun() &&
            !importSummary.getStatus().equals( ImportStatus.ERROR ) &&
            !importOptions.getImportStrategy().isDelete() &&
            (!importOptions.getImportStrategy().isSync() || importSummary.getImportCount().getDeleted() == 0);
    }

    /**
     * Creates a Consumer that takes an ImportSummary and sets the href property, based on the request root path,
     * api endpoint and the reference from the import summary.
     *
     * @param request
     * @return a Consumer for ImportSummary
     */
    private Consumer<ImportSummary> setImportSummaryHref( HttpServletRequest request )
    {
        return importSummary -> importSummary.setHref(
            ContextUtils.getRootPath( request ) +
                RelationshipSchemaDescriptor.API_ENDPOINT + "/" +
                importSummary.getReference() );
    }
}