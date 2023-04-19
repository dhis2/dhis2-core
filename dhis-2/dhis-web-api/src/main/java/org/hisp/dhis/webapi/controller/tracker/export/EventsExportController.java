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

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.tracker.export.event.EventParams;
import org.hisp.dhis.tracker.export.event.EventSearchParams;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.tracker.export.event.Events;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.tracker.export.csv.CsvService;
import org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.EventFieldsParamMapper;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
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

import com.fasterxml.jackson.databind.node.ObjectNode;

@OpenApi.Tags( "tracker" )
@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + EventsExportController.EVENTS )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class EventsExportController
{
    protected static final String EVENTS = "events";

    private static final String DEFAULT_FIELDS_PARAM = "*,!relationships";

    private static final EventMapper EVENTS_MAPPER = Mappers.getMapper( EventMapper.class );

    @Nonnull
    private final EventService eventService;

    @Nonnull
    private final EventCriteriaMapper requestToSearchParams;

    @Nonnull
    private final ProgramStageInstanceService programStageInstanceService;

    @Nonnull
    private final CsvService<Event> csvEventService;

    @Nonnull
    private final FieldFilterService fieldFilterService;

    private final EventFieldsParamMapper eventsMapper;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    public PagingWrapper<ObjectNode> getEvents(
        EventCriteria eventCriteria,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<FieldPath> fields )
        throws BadRequestException,
        ForbiddenException
    {
        EventSearchParams eventSearchParams = requestToSearchParams.map( eventCriteria );

        if ( areAllEnrollmentsInvalid( eventCriteria, eventSearchParams ) )
        {
            return new PagingWrapper<ObjectNode>().withInstances( Collections.emptyList() );
        }

        Events events = eventService.getEvents( eventSearchParams );

        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

        if ( eventCriteria.isPagingRequest() )
        {
            pagingWrapper = pagingWrapper.withPager(
                PagingWrapper.Pager.fromLegacy( eventCriteria, events.getPager() ) );
        }

        List<ObjectNode> objectNodes = fieldFilterService
            .toObjectNodes( EVENTS_MAPPER.fromCollection( events.getEvents() ), fields );
        return pagingWrapper.withInstances( objectNodes );
    }

    @GetMapping( produces = { CONTENT_TYPE_CSV, CONTENT_TYPE_CSV_GZIP, CONTENT_TYPE_TEXT_CSV } )
    public void getCsvEvents(
        EventCriteria eventCriteria,
        HttpServletResponse response,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        HttpServletRequest request )
        throws IOException,
        BadRequestException,
        ForbiddenException
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

        csvEventService.write( outputStream, EVENTS_MAPPER.fromCollection( events.getEvents() ), !skipHeader );
    }

    private boolean areAllEnrollmentsInvalid( EventCriteria eventCriteria, EventSearchParams eventSearchParams )
    {
        return !CollectionUtils.isEmpty( eventCriteria.getEnrollments() ) &&
            CollectionUtils.isEmpty( eventSearchParams.getProgramInstances() );
    }

    @GetMapping( "{uid}" )
    public ResponseEntity<ObjectNode> getEvent(
        @PathVariable String uid,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<FieldPath> fields )
        throws NotFoundException
    {
        EventParams eventParams = eventsMapper.map( fields );
        ProgramStageInstance event = eventService.getEvent( programStageInstanceService.getProgramStageInstance( uid ),
            eventParams );
        if ( event == null )
        {
            throw new NotFoundException( Event.class, uid );
        }

        return ResponseEntity
            .ok( fieldFilterService.toObjectNode( EVENTS_MAPPER.from( event ), fields ) );
    }
}
