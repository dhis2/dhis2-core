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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.EventFieldsParamMapper;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerEventsExportController.EVENTS )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerEventsExportController
{
    protected static final String EVENTS = "events";

    private static final String DEFAULT_FIELDS_PARAM = "*,!relationships";

    private static final EventMapper EVENTS_MAPPER = Mappers.getMapper( EventMapper.class );

    @NonNull
    private final EventService eventService;

    @NonNull
    private final TrackerEventCriteriaMapper requestToSearchParams;

    @NonNull
    private final ProgramStageInstanceService programStageInstanceService;

    @NonNull
    private final CsvEventService<org.hisp.dhis.webapi.controller.tracker.view.Event> csvEventService;

    @NonNull
    private final FieldFilterService fieldFilterService;

    private final EventFieldsParamMapper eventsMapper;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public PagingWrapper<ObjectNode> getEvents(
        TrackerEventCriteria eventCriteria, HttpServletRequest request,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
        throws WebMessageException
    {
        EventSearchParams eventSearchParams = requestToSearchParams.map( eventCriteria );

        if ( areAllEnrollmentsInvalid( eventCriteria, eventSearchParams ) )
        {
            return new PagingWrapper<ObjectNode>().withInstances( Collections.emptyList() );
        }

        Events events = eventService.getEvents( eventSearchParams );

        if ( hasHref( fields, eventCriteria.getSkipEventId() ) )
        {
            events.getEvents().forEach( e -> e.setHref( getUri( e.getEvent(), request ) ) );
        }

        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

        if ( eventCriteria.isPagingRequest() )
        {
            pagingWrapper = pagingWrapper.withPager(
                PagingWrapper.Pager.fromLegacy( eventCriteria, events.getPager() ) );
        }

        FieldFilterParams<org.hisp.dhis.webapi.controller.tracker.view.Event> filterParams = FieldFilterParams
            .of( EVENTS_MAPPER.fromCollection( events.getEvents() ), fields );
        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( filterParams );
        return pagingWrapper.withInstances( objectNodes );

    }

    @GetMapping( produces = { CONTENT_TYPE_CSV, CONTENT_TYPE_CSV_GZIP, CONTENT_TYPE_TEXT_CSV } )
    public void getCsvEvents(
        TrackerEventCriteria eventCriteria,
        HttpServletResponse response,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        HttpServletRequest request )
        throws IOException
    {
        EventSearchParams eventSearchParams = requestToSearchParams.map( eventCriteria );

        if ( areAllEnrollmentsInvalid( eventCriteria, eventSearchParams ) )
        {
            return;
        }

        Events events = eventService.getEvents( eventSearchParams );

        OutputStream outputStream = response.getOutputStream();
        response.setContentType( CONTENT_TYPE_CSV );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"events.csv\"" );

        if ( ContextUtils.isAcceptCsvGzip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            outputStream = new GZIPOutputStream( outputStream );
            response.setContentType( CONTENT_TYPE_CSV_GZIP );
            response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"events.csv.gz\"" );
        }

        csvEventService.writeEvents( outputStream, EVENTS_MAPPER.fromCollection( events.getEvents() ), !skipHeader );
    }

    private boolean areAllEnrollmentsInvalid( TrackerEventCriteria eventCriteria, EventSearchParams eventSearchParams )
    {
        return !CollectionUtils.isEmpty( eventCriteria.getEnrollments() ) &&
            CollectionUtils.isEmpty( eventSearchParams.getProgramInstances() );
    }

    private String getUri( String eventUid, HttpServletRequest request )
    {
        return UriComponentsBuilder.fromUriString( ContextUtils.getRootPath( request ) )
            .pathSegment( RESOURCE_PATH, EVENTS, eventUid )
            .build()
            .toString();
    }

    protected boolean hasHref( List<String> fields, Boolean skipEventId )
    {
        return (skipEventId == null || !skipEventId) && fieldsContainsHref( fields );
    }

    private boolean fieldsContainsHref( List<String> fields )
    {
        for ( String field : fields )
        {
            // For now assume href/access if * or preset is requested
            if ( field.contains( "href" ) || field.equals( "*" ) || field.startsWith( ":" ) )
            {
                return true;
            }
        }

        return false;
    }

    @GetMapping( "{uid}" )
    public ResponseEntity<ObjectNode> getEvent(
        @PathVariable String uid,
        HttpServletRequest request,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<FieldPath> fields )
        throws NotFoundException
    {
        EventParams eventParams = eventsMapper.map( fields );
        Event event = eventService.getEvent( programStageInstanceService.getProgramStageInstance( uid ),
            eventParams );
        if ( event == null )
        {
            throw new NotFoundException( "Event", uid );
        }

        event.setHref( getUri( uid, request ) );
        return ResponseEntity
            .ok( fieldFilterService.toObjectNode( EVENTS_MAPPER.from( event ), fields ) );
    }
}
