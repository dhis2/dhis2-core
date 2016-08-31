package org.hisp.dhis.webapi.controller.event;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.ImportEventTask;
import org.hisp.dhis.dxf2.events.event.ImportEventsTask;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.events.report.EventRowService;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.render.RenderService;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.scheduling.TaskCategory;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = EventController.RESOURCE_PATH )
public class EventController
{
    public static final String RESOURCE_PATH = "/events";

    private static final String META_DATA_KEY_DE = "de";

    //--------------------------------------------------------------------------
    // Dependencies
    //--------------------------------------------------------------------------

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private EventService eventService;

    @Autowired
    private CsvEventService csvEventService;

    @Autowired
    private EventRowService eventRowService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private InputUtils inputUtils;

    @Autowired
    private RenderService renderService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @RequestMapping( value = "", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public String getEvents(
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) String programStage,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) String orgUnit,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) EventStatus status,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String attributeCc,
        @RequestParam( required = false ) String attributeCos,
        @RequestParam( required = false ) boolean skipMeta,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false ) String attachment,
        @RequestParam Map<String, String> parameters, IdSchemes idSchemes, Model model, HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        WebOptions options = new WebOptions( parameters );

        DataElementCategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( attributeCc, attributeCos );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal attribute option combo identifier: " + attributeCc + " " + attributeCos ) );
        }

        EventSearchParams params = eventService.getFromUrl( program, programStage, programStatus, followUp, orgUnit, ouMode,
            trackedEntityInstance, startDate, endDate, status, lastUpdated, attributeOptionCombo, idSchemes, page, pageSize, totalPages, skipPaging, false );

        Events events = eventService.getEvents( params );

        if ( options.hasLinks() )
        {
            for ( Event event : events.getEvents() )
            {
                event.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + event.getEvent() );
            }
        }

        if ( !skipMeta && params.getProgram() != null )
        {
            events.setMetaData( getMetaData( params.getProgram() ) );
        }

        model.addAttribute( "model", events );
        model.addAttribute( "viewClass", options.getViewClass( "detailed" ) );

        if ( !StringUtils.isEmpty( attachment ) )
        {
            response.addHeader( "Content-Disposition", "attachment; filename=" + attachment );
        }

        return "events";
    }

    @RequestMapping( value = "", method = RequestMethod.GET, produces = { "application/csv", "application/csv+gzip", "text/csv" } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void getCsvEvents(
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) String programStage,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) String trackedEntityInstance,
        @RequestParam( required = false ) String orgUnit,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) EventStatus status,
        @RequestParam( required = false ) Date lastUpdated,
        @RequestParam( required = false ) String attributeCc,
        @RequestParam( required = false ) String attributeCos,
        @RequestParam( required = false ) Integer page,
        @RequestParam( required = false ) Integer pageSize,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam( required = false ) String attachment,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        IdSchemes idSchemes, HttpServletResponse response, HttpServletRequest request ) throws IOException, WebMessageException
    {
        DataElementCategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( attributeCc, attributeCos );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal attribute option combo identifier: " + attributeCc + " " + attributeCos ) );
        }

        EventSearchParams params = eventService.getFromUrl( program, programStage, programStatus, followUp, orgUnit, ouMode,
            trackedEntityInstance, startDate, endDate, status, lastUpdated, attributeOptionCombo, idSchemes, page, pageSize, totalPages, skipPaging, false );

        Events events = eventService.getEvents( params );

        OutputStream outputStream = response.getOutputStream();
        response.setContentType( "application/csv" );

        if ( ContextUtils.isAcceptGzip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            outputStream = new GZIPOutputStream( outputStream );
            response.setContentType( "application/csv+gzip" );
        }

        if ( !StringUtils.isEmpty( attachment ) )
        {
            response.addHeader( "Content-Disposition", "attachment; filename=" + attachment );
        }

        csvEventService.writeEvents( outputStream, events, !skipHeader );
    }

    @RequestMapping( value = "/eventRows", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public String getEventRows(
        @RequestParam( required = false ) String program,
        @RequestParam( required = false ) String orgUnit,
        @RequestParam( required = false ) OrganisationUnitSelectionMode ouMode,
        @RequestParam( required = false ) ProgramStatus programStatus,
        @RequestParam( required = false ) EventStatus eventStatus,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false ) String attributeCc,
        @RequestParam( required = false ) String attributeCos,
        @RequestParam( required = false ) boolean totalPages,
        @RequestParam( required = false ) boolean skipPaging,
        @RequestParam Map<String, String> parameters, Model model )
        throws WebMessageException

    {
        WebOptions options = new WebOptions( parameters );

        DataElementCategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( attributeCc, attributeCos );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal attribute option combo identifier: " + attributeCc + " " + attributeCos ) );
        }

        EventSearchParams params = eventService.getFromUrl( program, null, programStatus, null,
            orgUnit, ouMode, null, startDate, endDate, eventStatus, null, attributeOptionCombo, null, null, null, totalPages, skipPaging, true );

        EventRows eventRows = eventRowService.getEventRows( params );

        model.addAttribute( "model", eventRows );
        model.addAttribute( "viewClass", options.getViewClass( "detailed" ) );

        return "eventRows";
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public String getEvent( @PathVariable( "uid" ) String uid, @RequestParam Map<String, String> parameters,
        Model model, HttpServletRequest request ) throws Exception
    {
        WebOptions options = new WebOptions( parameters );
        Event event = eventService.getEvent( uid );

        if ( event == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        if ( options.hasLinks() )
        {
            event.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + uid );
        }

        model.addAttribute( "model", event );
        model.addAttribute( "viewClass", options.getViewClass( "detailed" ) );

        return "event";
    }

    private Map<Object, Object> getMetaData( Program program )
    {
        Map<Object, Object> metaData = new HashMap<>();

        if ( program != null )
        {
            Map<String, String> dataElements = new HashMap<>();

            for ( DataElement de : program.getAllDataElements() )
            {
                dataElements.put( de.getUid(), de.getDisplayName() );
            }

            metaData.put( META_DATA_KEY_DE, dataElements );
        }

        return metaData;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.POST, consumes = "application/xml" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void postXmlEvent( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy, HttpServletResponse response, HttpServletRequest request, ImportOptions importOptions ) throws Exception
    {
        importOptions.setImportStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = eventService.addEventsXml( inputStream, importOptions );

            importSummaries.getImportSummaries().stream()
                .filter( importSummary -> !importOptions.isDryRun() && !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                .forEach( importSummary -> importSummary.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() ) );

            if ( importSummaries.getImportSummaries().size() == 1 )
            {
                ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );

                if ( !importOptions.isDryRun() )
                {
                    if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                    {
                        response.setHeader( "Location", ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() );
                    }
                }
            }

            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            TaskId taskId = new TaskId( TaskCategory.EVENT_IMPORT, currentUserService.getCurrentUser() );
            scheduler.executeTask( new ImportEventTask( inputStream, eventService, importOptions, taskId, false ) );
            response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + TaskCategory.EVENT_IMPORT );
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
    }

    @RequestMapping( method = RequestMethod.POST, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void postJsonEvent( @RequestParam( defaultValue = "CREATE" ) ImportStrategy strategy, HttpServletResponse response, HttpServletRequest request, ImportOptions importOptions ) throws Exception
    {
        importOptions.setImportStrategy( strategy );
        InputStream inputStream = StreamUtils.wrapAndCheckCompressionFormat( request.getInputStream() );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = eventService.addEventsJson( inputStream, importOptions );

            importSummaries.getImportSummaries().stream()
                .filter( importSummary -> !importOptions.isDryRun() && !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                .forEach( importSummary -> importSummary.setHref( ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() ) );

            if ( importSummaries.getImportSummaries().size() == 1 )
            {
                ImportSummary importSummary = importSummaries.getImportSummaries().get( 0 );

                if ( !importOptions.isDryRun() )
                {
                    if ( !importSummary.getStatus().equals( ImportStatus.ERROR ) )
                    {
                        response.setHeader( "Location", ContextUtils.getRootPath( request ) + RESOURCE_PATH + "/" + importSummary.getReference() );
                    }
                }
            }

            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            TaskId taskId = new TaskId( TaskCategory.EVENT_IMPORT, currentUserService.getCurrentUser() );
            scheduler.executeTask( new ImportEventTask( inputStream, eventService, importOptions, taskId, true ) );
            response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + TaskCategory.EVENT_IMPORT );
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
    }


    @RequestMapping( method = RequestMethod.POST, consumes = { "application/csv", "text/csv" } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void postCsvEvents(
        @RequestParam( required = false, defaultValue = "false" ) boolean skipFirst,
        HttpServletResponse response, HttpServletRequest request, ImportOptions importOptions ) throws IOException
    {
        InputStream inputStream = ContextUtils.isAcceptGzip( request ) ? new GZIPInputStream( request.getInputStream() ) : request.getInputStream();

        Events events = csvEventService.readEvents( inputStream, skipFirst );

        if ( !importOptions.isAsync() )
        {
            ImportSummaries importSummaries = eventService.addEvents( events.getEvents(), importOptions, null );
            webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
        }
        else
        {
            TaskId taskId = new TaskId( TaskCategory.EVENT_IMPORT, currentUserService.getCurrentUser() );
            scheduler.executeTask( new ImportEventsTask( events.getEvents(), eventService, importOptions, taskId ) );
            response.setHeader( "Location", ContextUtils.getRootPath( request ) + "/system/tasks/" + TaskCategory.EVENT_IMPORT );
            response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = { "application/xml", "text/xml" } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void putXmlEvent( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        Event updatedEvent = renderService.fromXml( request.getInputStream(), Event.class );
        updatedEvent.setEvent( uid );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, false, importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void putJsonEvent( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        Event updatedEvent = renderService.fromJson( request.getInputStream(), Event.class );
        updatedEvent.setEvent( uid );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, false, importOptions );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{uid}/{dataElementUid}", method = RequestMethod.PUT, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void putJsonEventSingleValue( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid, @PathVariable( "dataElementUid" ) String dataElementUid ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        DataElement dataElement = dataElementService.getDataElement( dataElementUid );

        if ( dataElement == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "DataElement not found for ID " + dataElementUid ) );
        }

        Event updatedEvent = renderService.fromJson( request.getInputStream(), Event.class );
        updatedEvent.setEvent( uid );

        ImportSummary importSummary = eventService.updateEvent( updatedEvent, true );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }

    @RequestMapping( value = "/{uid}/addNote", method = RequestMethod.PUT, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void putJsonEventForNote( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        Event updatedEvent = renderService.fromJson( request.getInputStream(), Event.class );
        updatedEvent.setEvent( uid );

        eventService.updateEventForNote( updatedEvent );
        webMessageService.send( WebMessageUtils.ok( "Event updated: " + uid ), response, request );
    }

    @RequestMapping( value = "/{uid}/updateEventDate", method = RequestMethod.PUT, consumes = "application/json" )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_ADD')" )
    public void putJsonEventForEventDate( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid, ImportOptions importOptions ) throws IOException, WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        Event updatedEvent = renderService.fromJson( request.getInputStream(), Event.class );
        updatedEvent.setEvent( uid );

        eventService.updateEventForEventDate( updatedEvent );
        webMessageService.send( WebMessageUtils.ok( "Event updated " + uid ), response, request );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_TRACKED_ENTITY_DATAVALUE_DELETE')" )
    public void deleteEvent( HttpServletResponse response, HttpServletRequest request, @PathVariable( "uid" ) String uid ) throws WebMessageException
    {
        if ( !programStageInstanceService.programStageInstanceExists( uid ) )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "Event not found for ID " + uid ) );
        }

        response.setStatus( HttpServletResponse.SC_OK );
        ImportSummary importSummary = eventService.deleteEvent( uid );
        webMessageService.send( WebMessageUtils.importSummary( importSummary ), response, request );
    }
}