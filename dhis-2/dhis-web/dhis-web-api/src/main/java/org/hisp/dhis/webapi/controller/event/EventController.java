package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.EVENT_IMPORT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.PagerUtils;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.ImportEventsTask;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.events.report.EventRowService;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.fileresource.ImageFileDimension;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.grid.GridUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.io.ParseException;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = EventController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class EventController
{
    public static final String RESOURCE_PATH = "/events";

    private static final String META_DATA_KEY_DE = "de";

    //--------------------------------------------------------------------------
    // Dependencies
    //--------------------------------------------------------------------------

    private final CurrentUserService currentUserService;

    private final SchedulingManager schedulingManager;

    private final EventService eventService;

    private final CsvEventService csvEventService;

    private final EventRowService eventRowService;

    private final DataElementService dataElementService;

    private final WebMessageService webMessageService;

    private final InputUtils inputUtils;

    private final RenderService renderService;

    private final ProgramStageInstanceService programStageInstanceService;

    private final FileResourceService fileResourceService;

    private final FieldFilterService fieldFilterService;

    private final ContextService contextService;

    private final SchemaService schemaService;

    protected final TrackedEntityInstanceService entityInstanceService;

    private final ContextUtils contextUtils;

    public EventController( CurrentUserService currentUserService, SchedulingManager schedulingManager,
        EventService eventService, CsvEventService csvEventService, EventRowService eventRowService,
        DataElementService dataElementService, WebMessageService webMessageService, InputUtils inputUtils,
        RenderService renderService, ProgramStageInstanceService programStageInstanceService,
        FileResourceService fileResourceService, FieldFilterService fieldFilterService, ContextService contextService,
        SchemaService schemaService, TrackedEntityInstanceService entityInstanceService, ContextUtils contextUtils )
    {
        this.currentUserService = currentUserService;
        this.schedulingManager = schedulingManager;
        this.eventService = eventService;
        this.csvEventService = csvEventService;
        this.eventRowService = eventRowService;
        this.dataElementService = dataElementService;
        this.webMessageService = webMessageService;
        this.inputUtils = inputUtils;
        this.renderService = renderService;
        this.programStageInstanceService = programStageInstanceService;
        this.fileResourceService = fileResourceService;
        this.fieldFilterService = fieldFilterService;
        this.contextService = contextService;
        this.schemaService = schemaService;
        this.entityInstanceService = entityInstanceService;
        this.contextUtils = contextUtils;
    }

    private Schema schema;

    protected Schema getSchema()
    {
        if ( schema == null )
        {
            schema = schemaService.getDynamicSchema( Event.class );
        }

        return schema;
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Query Read
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = { ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_JAVASCRIPT } )
    public @ResponseBody Grid getEventsJson( @Valid GetEventsCriteria requestParams, IdSchemes idSchemes,
        HttpServletResponse response )
        throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        EventSearchParams params = toEventSearchParams( requestParams, idSchemes, null,
            getGridOrderParams( requestParams.getOrder() ), false, requestParams.getDataElement(),
            requestParams.isIncludeAllDataElements() );

        contextUtils.configureResponse( response, ContextUtils.CONTENT_TYPE_JSON, CacheStrategy.NO_CACHE );

        return eventService.getEventsGrid( params );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_XML )
    public @ResponseBody void getEventsXml( @Valid GetEventsCriteria requestParams, IdSchemes idSchemes,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXml( getEventsJson( requestParams, idSchemes, response ), response.getOutputStream() );
    }

    @RequestMapping( value = "/query", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_EXCEL )
    public void queryEventsXls( @Valid GetEventsCriteria requestParams, IdSchemes idSchemes,
        HttpServletResponse response )
        throws Exception
    {
        GridUtils.toXls( getEventsJson( requestParams, idSchemes, response ), response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // Object Read
    // -------------------------------------------------------------------------

    @GetMapping
    public @ResponseBody RootNode getEvents(
            @Valid GetEventsCriteria requestParams,
            @RequestParam Map<String, String> parameters,
            IdSchemes idSchemes,
            Model model, HttpServletRequest request,
            HttpServletResponse response) throws WebMessageException {

        WebOptions options = new WebOptions( parameters );
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        Map<String, String> dataElementOrders = getDataElementsFromOrder( requestParams.getOrder() );

        EventSearchParams params = toEventSearchParams( requestParams, idSchemes,
                getOrderParams( requestParams.getOrder() ),
                getGridOrderParams( requestParams.getOrder(), dataElementOrders ), requestParams.isSkipEventId(),
                dataElementOrders.keySet(), false );

        Events events = eventService.getEvents( params );

        if ( hasHref( fields, requestParams.isSkipEventId() ) )
        {
            events.getEvents().forEach( e -> e.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + e.getEvent() ) );
        }

        if ( !requestParams.isSkipMeta() && params.getProgram() != null )
        {
            events.setMetaData( getMetaData( params.getProgram() ) );
        }

        model.addAttribute( "model", events );
        model.addAttribute( "viewClass", options.getViewClass( "detailed" ) );

        RootNode rootNode = NodeUtils.createMetadata();

        if ( events.getPager() != null )
        {
            rootNode.addChild( NodeUtils.createPager( events.getPager() ) );
        }

        if ( !StringUtils.isEmpty( requestParams.getAttachment() ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_DISPOSITION, "attachment; filename=" + requestParams.getAttachment() );
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
        }

        rootNode.addChild( fieldFilterService.toCollectionNode( Event.class, new FieldFilterParams( events.getEvents(), fields ) ) );

        return rootNode;

    }

    @GetMapping( produces = { "application/xml", "application/xml+gzip", "text/xml" } )
    public @ResponseBody
    RootNode getXmlEvents(@Valid GetEventsCriteria requestParams,
                          @RequestParam Map<String, String> parameters,
                          IdSchemes idSchemes,
                          Model model, HttpServletRequest request,
                          HttpServletResponse response )
        throws WebMessageException
    {
       // TODO do we need this ?
//        RootNode rootNode = NodeUtils.createEvents();
//
//        if ( events.getPager() != null )
//        {
//            rootNode.addChild( NodeUtils.createPager( events.getPager() ) );
//        }

        return getEvents( requestParams, parameters, idSchemes, model, request, response);
    }

    @GetMapping( produces = { "application/csv", "application/csv+gzip", "text/csv" } )
    public void getCsvEvents(@Valid GetEventsCriteria requestParams,
                             IdSchemes idSchemes,
                             HttpServletRequest request,
                             HttpServletResponse response ) throws IOException, WebMessageException
    {
        Map<String, String> dataElementOrders = getDataElementsFromOrder( requestParams.getOrder() );

        EventSearchParams params = toEventSearchParams( requestParams, idSchemes,
                getOrderParams( requestParams.getOrder() ),
                getGridOrderParams( requestParams.getOrder(), dataElementOrders ), requestParams.isSkipEventId(),
                dataElementOrders.keySet(), false );

        Events events = eventService.getEvents( params );

        OutputStream outputStream = response.getOutputStream();
        response.setContentType( "application/csv" );

        if ( ContextUtils.isAcceptCsvGzip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            outputStream = new GZIPOutputStream( outputStream );
            response.setContentType( "application/csv+gzip" );
        }

        if ( !StringUtils.isEmpty( requestParams.getAttachment() ) )
        {
            response.addHeader( "Content-Disposition", "attachment; filename=" + requestParams.getAttachment() );
        }

        csvEventService.writeEvents( outputStream, events, !requestParams.isSkipHeader() );
    }

    // -------------------------------------------------------------------------
    // Rows Read
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/eventRows", method = RequestMethod.GET )
    public @ResponseBody EventRows getEventRows(
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) String orgUnit,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) String attributeCc,
        @RequestParam( required = false ) String attributeCos,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) Boolean skipPaging,
        @RequestParam( required = false ) Boolean paging,
        @RequestParam( required = false ) String order,
        @RequestParam( required = false ) Boolean skipEventId,
        @RequestParam( required = false, defaultValue = "false" ) boolean includeDeleted,
        IdSchemes idSchemes)
    {
        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( attributeCc, attributeCos, true );

        skipPaging = PagerUtils.isSkipPaging( skipPaging, paging );

        EventSearchParams params = eventService.getFromUrl( program, null, programStatus, null,
            orgUnit, ouMode, null, startDate, endDate, null, null,
            null, null, null, eventStatus, attributeOptionCombo,
            idSchemes, page, pageSize, totalPages, skipPaging, getOrderParams( order ),
            null, true, null, skipEventId, null, null, null,
            null, false, includeDeleted );

        return eventRowService.getEventRows( params );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.GET )
    public @ResponseBody Event getEvent( @PathVariable( "uid" ) String uid, HttpServletRequest request ) throws Exception
    {
        Event event = eventService.getEvent( programStageInstanceService.getProgramStageInstance( uid ) );

        if ( event == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        event.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + uid );

        return event;
    }

    @RequestMapping( value = "/files", method = RequestMethod.GET )
    public void getEventDataValueFile( @RequestParam String eventUid, @RequestParam String dataElementUid, @RequestParam( required = false ) ImageFileDimension dimension,
        HttpServletResponse response ) throws Exception
    {
        Event event = eventService.getEvent( programStageInstanceService.getProgramStageInstance( eventUid ) );

        if ( event == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + eventUid ) );
        }

        DataElement dataElement = dataElementService.getDataElement( dataElementUid );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataElement not found for ID " + dataElementUid ) );
        }

        if ( !dataElement.isFileType() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "DataElement must be of type file" ) );
        }

        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        String uid = null;

        for ( DataValue value : event.getDataValues() )
        {
            if ( value.getDataElement() != null && value.getDataElement().equals( dataElement.getUid() ) )
            {
                uid = value.getValue();
                break;
            }
        }

        if ( uid == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "DataElement must be of type file" ) );
        }


        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "A data value file resource with id " + uid + " does not exist." ) );
        }

        if ( fileResource.getStorageStatus() != FileResourceStorageStatus.STORED )
        {
            // -----------------------------------------------------------------
            // The FileResource exists and is tied to DataValue, however the
            // underlying file content still not stored to external file store
            // -----------------------------------------------------------------

            WebMessage webMessage = WebMessageUtils.conflict( "The content is being processed and is not available yet. Try again later.",
                "The content requested is in transit to the file store and will be available at a later time." );

            webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

            throw new WebMessageException( webMessage );
        }

        FileResourceUtils.setImageFileDimensions( fileResource, MoreObjects.firstNonNull( dimension, ImageFileDimension.ORIGINAL ) );

        response.setContentType( fileResource.getContentType() );
        response.setContentLength( new Long( fileResource.getContentLength() ).intValue() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related" ) );
        }
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @PostMapping( consumes = { "application/xml", "application/json" } )
    public void postEvent(@RequestParam( defaultValue = "CREATE_AND_UPDATE" ) ImportStrategy strategy,
                          ImportOptions importOptions,
                          @RequestHeader("Accept") MediaType accept,
                          HttpServletRequest request,
                          HttpServletResponse response )
        throws Exception
    {
        importOptions.setImportStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        importOptions.setIdSchemes(
            getIdSchemesFromParameters( importOptions.getIdSchemes(), contextService.getParameterValuesMap() ) );

        if ( !importOptions.isAsync() )
        {
//            ImportSummaries importSummaries = accept.isCompatibleWith( Me )
//                ? eventService.addEventsXml( inputStream, importOptions )
//                : eventService.addEventsJson( inputStream, importOptions );
            ImportSummaries importSummaries = eventService.addEventsJson(inputStream, importOptions);
            importSummaries.setImportOptions( importOptions );

            importSummaries.getImportSummaries().stream().filter( importSummary -> !importOptions.isDryRun()
                && !importSummary.getStatus().equals( ImportStatus.ERROR )
                && !importOptions.getImportStrategy().isDelete()
                && (!importOptions.getImportStrategy().isSync() || importSummary.getImportCount().getDeleted() == 0) )
                .forEach( importSummary -> importSummary.setHref(
                    ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() ) );

            if ( importSummaries.getImportSummaries().size() == 1 )
            {
                ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );
                importSummary.setImportOptions( importOptions );

                if ( !importOptions.isDryRun() )
                {
                    if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                    {
                        response.setHeader( "Location",
                            ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() );
                    }
                }
            }
        }
        else
        {
            List<Event> events = accept.isCompatibleWith( MediaType.APPLICATION_XML )
                ? eventService.getEventsXml( inputStream )
                : eventService.getEventsJson( inputStream );
            startAsyncImport( importOptions, events, request, response );
        }
    }

    @RequestMapping( value = "/{uid}/note", method = RequestMethod.POST, consumes = "application/json" )
    public void postJsonEventForNote( @PathVariable( "uid" ) String uid,
        HttpServletResponse response, HttpServletRequest request ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        Event event = renderService.fromJson( inputStream, Event.class );
        event.setEvent( uid );

        eventService.updateEventForNote( event );
        webMessageService.send( WebMessageUtils.ok( "Event updated: " + uid ), response, request );
    }

    @RequestMapping( method = RequestMethod.POST, consumes = { "application/csv", "text/csv" } )
    public void postCsvEvents( @RequestParam( required = false, defaultValue = "false" ) boolean skipFirst,
        HttpServletResponse response, HttpServletRequest request, ImportOptions importOptions )
        throws IOException, ParseException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        Events events = csvEventService.readEvents( inputStream, skipFirst );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = null; // eventService.addEvents( events.getEvents(), importOptions, null );
            importSummaries.setImportOptions( importOptions );
            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            startAsyncImport( importOptions, events.getEvents(), request, response );
        }
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { "application/xml", "text/xml" } )
    public void putXmlEvent( HttpServletResponse response, HttpServletRequest request,
        @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        Event updatedEvent = renderService.fromXml( inputStream, Event.class );
        updatedEvent.setEvent( uid );

        updateEvent( updatedEvent, false, importOptions, request, response );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonEvent( HttpServletResponse response, HttpServletRequest request,
        @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException
    {
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        Event updatedEvent = renderService.fromJson( inputStream, Event.class );
        updatedEvent.setEvent( uid );

        updateEvent( updatedEvent, false, importOptions, request, response );
    }

    private void updateEvent( Event updatedEvent, boolean singleValue, ImportOptions importOptions, HttpServletRequest request, HttpServletResponse response )
    {
        ImportSummary importSummary = eventService.updateEvent( updatedEvent, singleValue, importOptions, false );
        importSummary.setImportOptions( importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{uid}/{dataElementUid}", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonEventSingleValue( HttpServletResponse response, HttpServletRequest request,
        @PathVariable( "uid" ) String uid, @PathVariable( "dataElementUid" ) String dataElementUid ) throws IOException
    {
        DataElement dataElement = dataElementService.getDataElement( dataElementUid );

        if ( dataElement == null )
        {
            WebMessage webMsg = WebMessageUtils.notFound( "DataElement not found for ID " + dataElementUid );
            webMessageService.send( webMsg, response, request );
        }

        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        Event updatedEvent = renderService.fromJson( inputStream, Event.class );
        updatedEvent.setEvent( uid );

        updateEvent( updatedEvent, true, null, request, response );
    }

    @RequestMapping( value = "/{uid}/eventDate", method = RequestMethod.PUT, consumes = "application/json" )
    public void putJsonEventForEventDate( HttpServletResponse response, HttpServletRequest request,
        @PathVariable( "uid" ) String uid ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );
        Event updatedEvent = renderService.fromJson( inputStream, Event.class );
        updatedEvent.setEvent( uid );

        eventService.updateEventForEventDate( updatedEvent );
        webMessageService.send( WebMessageUtils.ok( "Event updated " + uid ), response, request );
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    public void deleteEvent( HttpServletResponse response, HttpServletRequest request,
        @PathVariable( "uid" ) String uid )
    {
        ImportSummary importSummary = eventService.deleteEvent( uid );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Map<String, String> getDataElementsFromOrder( String allOrders )
    {
        Map<String, String> dataElements = new HashMap<>();

        if ( allOrders != null )
        {
            for ( String order : TextUtils.splitToArray( allOrders, TextUtils.SEMICOLON ) )
            {
                String[] orderParts = order.split( ":" );
                DataElement de = dataElementService.getDataElement( orderParts[0] );
                if ( de != null )
                {
                    String direction = "asc";
                    if ( orderParts.length == 2 && orderParts[1].toLowerCase().equals( "desc" ) )
                    {
                        direction = "desc";
                    }
                    dataElements.put( de.getUid(), direction );
                }
            }
        }
        return dataElements;
    }

    /**
     * Starts an asynchronous import task.
     *
     * @param importOptions the ImportOptions.
     * @param events        the events to import.
     * @param request       the HttpRequest.
     * @param response      the HttpResponse.
     */
    private void startAsyncImport( ImportOptions importOptions, List<Event> events, HttpServletRequest request, HttpServletResponse response )
    {
        JobConfiguration jobId = new JobConfiguration( "inMemoryEventImport",
            EVENT_IMPORT, currentUserService.getCurrentUser().getUid(), true );
        schedulingManager.executeJob( new ImportEventsTask( events, eventService, importOptions, jobId ) );

        response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + EVENT_IMPORT );
        webMessageService.send( jobConfigurationReport( jobId ), response, request );
    }

    protected boolean hasHref( List<String> fields, Boolean skipEventId )
    {
        return (skipEventId == null || !skipEventId)
            && fields.stream().anyMatch( f -> f.contains( "href" ) || f.equals( "*" ) || f.startsWith( ":" ) );
    }

    private List<Order> getOrderParams( String order )
    {
        if ( order != null && !StringUtils.isEmpty( order ) )
        {
            OrderParams op = new OrderParams( Sets.newLinkedHashSet( Arrays.asList( order.split( "," ) ) ) );
            return op.getOrders( getSchema() );
        }

        return null;
    }

    private List<String> getGridOrderParams( String order )
    {
        if ( order != null && !StringUtils.isEmpty( order ) )
        {
            return Arrays.asList( order.split( "," ) );
        }

        return null;
    }

    private List<String> getGridOrderParams( String order, Map<String, String> dataElementOrders )
    {
        List<String> dataElementOrderList = new ArrayList<>();

        if ( !StringUtils.isEmpty( order ) && dataElementOrders != null && dataElementOrders.size() > 0 )
        {
            String[] orders = order.split( ";" );

            for ( String orderItem : orders )
            {
                String dataElementCandidate = orderItem.split( ":" )[0];
                if ( dataElementOrders.containsKey( dataElementCandidate ) )
                {
                    dataElementOrderList
                        .add( dataElementCandidate + ":" + dataElementOrders.get( dataElementCandidate ) );
                }
            }
        }

        return dataElementOrderList;
    }

    private Map<Object, Object> getMetaData( Program program )
    {
        Map<Object, Object> metaData = new HashMap<>();

        if ( program != null )
        {
            Map<String, String> dataElements = new HashMap<>();

            for ( DataElement de : program.getDataElements() )
            {
                dataElements.put( de.getUid(), de.getDisplayName() );
            }

            metaData.put( META_DATA_KEY_DE, dataElements );
        }

        return metaData;
    }

    private IdSchemes getIdSchemesFromParameters( IdSchemes idSchemes, Map<String, List<String>> params )
    {
        String idScheme = getParamValue( params, "idScheme" );

        if ( idScheme != null )
        {
            idSchemes.setIdScheme( idScheme );
        }

        String programStageInstanceIdScheme = getParamValue( params, "programStageInstanceIdScheme" );

        if ( programStageInstanceIdScheme != null )
        {
            idSchemes.setProgramStageInstanceIdScheme( programStageInstanceIdScheme );
        }

        return idSchemes;
    }

    private String getParamValue( Map<String, List<String>> params, String key )
    {
        return params.get( key ) != null ? params.get( key ).get( 0 ) : null;
    }

    private EventSearchParams toEventSearchParams( GetEventsCriteria criteria, IdSchemes idSchemes, List<Order> orders,
                                                   List<String> getGridOrderParams, boolean skipEventId,
                                                   Set<String> dateElements, boolean includeAllDataElements )
            throws WebMessageException
    {

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( criteria.getAttributeCc(),
                criteria.getAttributeCos(), false );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal attribute option combo identifier: "
                    + criteria.getAttributeCc() + " " + criteria.getAttributeCos() ) );
        }

        Set<String> eventIds = TextUtils.splitToArray( criteria.getEvent(), TextUtils.SEMICOLON );

        Set<String> assignedUserIds = TextUtils.splitToArray( criteria.getAssignedUser(), TextUtils.SEMICOLON );

        Date lastUpdatedStartDate = criteria.getLastUpdatedStartDate() != null ? criteria.getLastUpdatedStartDate()
                : criteria.getLastUpdated();

        boolean skipPaging = PagerUtils.isSkipPaging( criteria.getSkipPaging(), criteria.getPaging() );

        return eventService.getFromUrl( criteria.getProgram(), criteria.getProgramStage(), criteria.getProgramStatus(),
                criteria.getFollowUp(), criteria.getOrgUnit(), criteria.getOuMode(), criteria.getTrackedEntityInstance(),
                criteria.getStartDate(), criteria.getEndDate(), criteria.getDueDateStart(), criteria.getDueDateEnd(),
                lastUpdatedStartDate, criteria.getLastUpdatedEndDate(), criteria.getLastUpdatedDuration(), criteria.getStatus(),
                attributeOptionCombo, idSchemes, criteria.getPage(), criteria.getPageSize(), criteria.isTotalPages(),
                skipPaging, orders, getGridOrderParams, false, eventIds, skipEventId,
                criteria.getAssignedUserMode(), assignedUserIds, criteria.getFilter(), dateElements,
                includeAllDataElements, criteria.isIncludeDeleted() );
    }
}
