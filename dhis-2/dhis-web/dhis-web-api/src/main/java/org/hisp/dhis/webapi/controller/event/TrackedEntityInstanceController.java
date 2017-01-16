package org.hisp.dhis.webapi.controller.event;

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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.descriptors.TrackedEntityInstanceSchemaDescriptor;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 *         <p>
 *         The following statements are added not to cause api break.
 *         They need to be remove say in 2.26 or so once users are aware of the changes.
 *         <p>
 *         programEnrollmentStartDate= ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
 *         programEnrollmentEndDate= ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
 */
@Controller
@RequestMapping( value = TrackedEntityInstanceSchemaDescriptor.API_ENDPOINT )
@PreAuthorize( "hasAnyRole('ALL', 'F_TRACKED_ENTITY_INSTANCE_SEARCH')" )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class TrackedEntityInstanceController
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService instanceService;

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private RenderService renderService;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.GET )
    public @ResponseBody RootNode getTrackedEntityInstances(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );

        RootNode rootNode = NodeUtils.createMetadata();

        List<TrackedEntityInstance> trackedEntityInstances;

        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntity,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging );

        if ( trackedEntityInstance == null )
        {
            trackedEntityInstances = trackedEntityInstanceService.getTrackedEntityInstances( params );
        }
        else
        {
            Set<String> trackedEntityInstanceIds = TextUtils.splitToArray( trackedEntityInstance, TextUtils.SEMICOLON );

            trackedEntityInstances = trackedEntityInstanceIds != null ? trackedEntityInstanceIds.stream().map( id -> trackedEntityInstanceService.getTrackedEntityInstance( id ) ).collect( Collectors.toList() ) : null;
        }

        if ( params.isPaging() && params.isTotalPages() )
        {
            int count = trackedEntityInstanceService.getTrackedEntityInstanceCount( params );
            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            rootNode.addChild( NodeUtils.createPager( pager ) );
        }

        rootNode.addChild( fieldFilterService.filter( TrackedEntityInstance.class, trackedEntityInstances, fields ) );

        return rootNode;
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    public @ResponseBody Grid queryTrackedEntityInstancesJson(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        Model model,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );
        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntity,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE );
        return instanceService.getTrackedEntityInstancesGrid( params );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_XML )
    public void queryTrackedEntityInstancesXml(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );
        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntity,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_XML, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toXml( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_EXCEL )
    public void queryTrackedEntityInstancesXls(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );
        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntity,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_EXCEL, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toXls( grid, response.getOutputStream() );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_CSV )
    public void queryTrackedEntityInstancesCsv(
        @RequestParam( required = false ) String query,
        @RequestParam( required = false ) Set<String> attribute,
        @RequestParam( required = false ) Set<String> filter,
        @RequestParam( required = false ) String ou,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) Date programStartDate,
        @RequestParam( required = false ) Date programEnrollmentStartDate,
        @RequestParam( required = false ) Date programEndDate,
        @RequestParam( required = false ) Date programEnrollmentEndDate,
        @RequestParam( required = false ) Date programIncidentStartDate,
        @RequestParam( required = false ) Date programIncidentEndDate,
        @RequestParam( required = false ) String trackedEntity,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date eventStartDate,
        @RequestParam( required = false ) Date eventEndDate,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        HttpServletResponse response ) throws Exception
    {
        programEnrollmentStartDate = ObjectUtils.firstNonNull( programEnrollmentStartDate, programStartDate );
        programEnrollmentEndDate = ObjectUtils.firstNonNull( programEnrollmentEndDate, programEndDate );
        Set<String> orgUnits = TextUtils.splitToArray( ou, TextUtils.SEMICOLON );
        TrackedEntityInstanceQueryParams params = instanceService.getFromUrl( query, attribute, filter, orgUnits, ouMode,
            program, programStatus, followUp, programEnrollmentStartDate, programEnrollmentEndDate, programIncidentStartDate, programIncidentEndDate, trackedEntity,
            eventStatus, eventStartDate, eventEndDate, skipMeta, page, pageSize, totalPages, skipPaging );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_CSV, CacheStrategy.NO_CACHE );
        Grid grid = instanceService.getTrackedEntityInstancesGrid( params );
        GridUtils.toCsv( grid, response.getWriter() );
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_SEARCH')" )
    public @ResponseBody RootNode getTrackedEntityInstanceById( @PathVariable( "id" ) String pvId )
        throws NotFoundException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.add( ":all" );
        }

        CollectionNode collectionNode = fieldFilterService.filter( TrackedEntityInstance.class, Lists.newArrayList( getTrackedEntityInstance( pvId ) ), fields );

        RootNode rootNode = new RootNode( collectionNode.getChildren().get( 0 ) );
        rootNode.setDefaultNamespace( DxfNamespaces.DXF_2_0 );
        rootNode.setNamespace( DxfNamespaces.DXF_2_0 );

        return rootNode;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_XML_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_ADD')" )
    public void postTrackedEntityInstanceXml( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = trackedEntityInstanceService.addTrackedEntityInstanceXml( inputStream, importOptions );
        response.setContentType( MediaType.APPLICATION_XML_VALUE );

        if ( importSummaries.getImportSummaries().size() > 1 )
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            renderService.toXml( response.getOutputStream(), importSummaries );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            ImportSummary importSummary;

            if ( !importSummaries.getImportSummaries().isEmpty() )
            {
                importSummary = importSummaries.getImportSummaries().get( 0 );
                importSummary.setImportOptions( importOptions );

                if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                {
                    response.setHeader( "Location", getResourcePath( request, importSummary ) );
                }
            }
            else
            {
                importSummary = new ImportSummary( ImportStatus.SUCCESS, "Empty list of tracked entity instances given." );
                importSummary.setImportOptions( importOptions );
                importSummary.setImportCount( null );
            }

            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
    }

    @RequestMapping( value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_ADD')" )
    public void postTrackedEntityInstanceJson( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy,
        ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        importOptions.setStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummaries importSummaries = trackedEntityInstanceService.addTrackedEntityInstanceJson( inputStream, importOptions );
        importSummaries.setImportOptions( importOptions );
        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        if ( importSummaries.getImportSummaries().size() > 1 )
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            renderService.toJson( response.getOutputStream(), importSummaries );
        }
        else
        {
            response.setStatus( HttpServletResponse.SC_CREATED );
            ImportSummary importSummary;

            if ( !importSummaries.getImportSummaries().isEmpty() )
            {
                importSummary = importSummaries.getImportSummaries().get( 0 );
                importSummary.setImportOptions( importOptions );

                if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                {
                    response.setHeader( "Location", getResourcePath( request, importSummary ) );
                }
            }
            else
            {
                importSummary = new ImportSummary( ImportStatus.SUCCESS, "Empty list of tracked entity instances given." );
                importSummary.setImportOptions( importOptions );
                importSummary.setImportCount( null );
            }

            webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_XML_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_ADD')" )
    public void updateTrackedEntityInstanceXml( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceXml( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_ADD')" )
    public void updateTrackedEntityInstanceJson( @PathVariable String id, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
        throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        ImportSummary importSummary = trackedEntityInstanceService.updateTrackedEntityInstanceJson( id, inputStream, importOptions );
        importSummary.setImportOptions( importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{id}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_INSTANCE_DELETE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteTrackedEntityInstance( @PathVariable String id ) throws WebMessageException
    {
        if ( !instanceService.trackedEntityInstanceExists( id ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Tracked entity instance not found: " + id ) );
        }

        trackedEntityInstanceService.deleteTrackedEntityInstance( id );
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private TrackedEntityInstance getTrackedEntityInstance( String id )
        throws NotFoundException
    {
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceService.getTrackedEntityInstance( id );

        if ( trackedEntityInstance == null )
        {
            throw new NotFoundException( "TrackedEntityInstance", id );
        }
        return trackedEntityInstance;
    }

    private String getResourcePath( HttpServletRequest request, ImportSummary importSummary )
    {
        return ContextUtils.getContextPath( request ) + "/api/" + "trackedEntityInstances" + "/" + importSummary.getReference();
    }
}
