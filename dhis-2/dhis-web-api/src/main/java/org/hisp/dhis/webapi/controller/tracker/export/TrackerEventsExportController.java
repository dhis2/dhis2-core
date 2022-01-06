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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_CSV_GZIP;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_TEXT_CSV;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.tracker.domain.mapper.EventMapper;
import org.hisp.dhis.webapi.controller.event.mapper.RequestToSearchParamsMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.event.webrequest.tracker.TrackerEventCriteria;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Lists;

@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerEventsExportController.EVENTS )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerEventsExportController
{
    protected static final String EVENTS = "events";

    private static final EventMapper EVENTS_MAPPER = Mappers.getMapper( EventMapper.class );

    private final EventService eventService;

    private final ContextService contextService;

    private final RequestToSearchParamsMapper requestToSearchParamsMapper;

    private final ProgramStageInstanceService programStageInstanceService;

    private final CsvEventService<org.hisp.dhis.tracker.domain.Event> csvEventService;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public PagingWrapper<org.hisp.dhis.tracker.domain.Event> getEvents(
        TrackerEventCriteria eventCriteria, @RequestParam Map<String, String> parameters, HttpServletRequest request )
        throws WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        EventSearchParams eventSearchParams = requestToSearchParamsMapper.map( eventCriteria );

        if ( areAllEnrollmentsInvalid( eventCriteria, eventSearchParams ) )
        {
            return new PagingWrapper<org.hisp.dhis.tracker.domain.Event>().withInstances( Collections.emptyList() );
        }

        Events events = eventService.getEvents( eventSearchParams );

        if ( hasHref( fields, eventCriteria.getSkipEventId() ) )
        {
            events.getEvents().forEach( e -> e.setHref( getUri( e.getEvent(), request ) ) );
        }

        PagingWrapper<org.hisp.dhis.tracker.domain.Event> eventPagingWrapper = new PagingWrapper<>();

        if ( eventCriteria.isPagingRequest() )
        {
            eventPagingWrapper = eventPagingWrapper.withPager(
                PagingWrapper.Pager.fromLegacy( eventCriteria, events.getPager() ) );
        }

        return eventPagingWrapper.withInstances( EVENTS_MAPPER.fromCollection( events.getEvents() ) );

    }

    @GetMapping( produces = { CONTENT_TYPE_CSV, CONTENT_TYPE_CSV_GZIP, CONTENT_TYPE_TEXT_CSV } )
    public void getCsvEvents(
        TrackerEventCriteria eventCriteria,
        HttpServletResponse response,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        @RequestParam Map<String, String> parameters, HttpServletRequest request )
        throws IOException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
        }

        EventSearchParams eventSearchParams = requestToSearchParamsMapper.map( eventCriteria );

        if ( areAllEnrollmentsInvalid( eventCriteria, eventSearchParams ) )
        {
            return;
        }

        Events events = eventService.getEvents( eventSearchParams );

        if ( hasHref( fields, eventCriteria.getSkipEventId() ) )
        {
            events.getEvents().forEach( e -> e.setHref( getUri( e.getEvent(), request ) ) );
        }

        OutputStream outputStream = response.getOutputStream();
        response.setContentType( CONTENT_TYPE_CSV );

        if ( ContextUtils.isAcceptCsvGzip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            outputStream = new GZIPOutputStream( outputStream );
            response.setContentType( CONTENT_TYPE_CSV_GZIP );
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

    @GetMapping( "/{uid}" )
    public org.hisp.dhis.tracker.domain.Event getEvent(
        @PathVariable( "uid" ) String uid,
        @RequestParam Map<String, String> parameters,
        HttpServletRequest request )
        throws Exception
    {
        Event event = eventService.getEvent( programStageInstanceService.getProgramStageInstance( uid ) );

        if ( event == null )
        {
            throw new WebMessageException( notFound( "Event not found for ID " + uid ) );
        }

        event.setHref( getUri( uid, request ) );

        return EVENTS_MAPPER.from( event );
    }

}
