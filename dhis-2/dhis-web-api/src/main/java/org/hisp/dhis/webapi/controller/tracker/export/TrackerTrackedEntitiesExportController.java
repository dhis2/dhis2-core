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
import static org.hisp.dhis.webapi.controller.tracker.export.fieldsmapper.TrackedEntityFieldsParamMapper.map;
import static org.hisp.dhis.webapi.utils.ContextUtils.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.tracker.view.TrackedEntity;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

@OpenApi.Tags( "tracker" )
@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerTrackedEntitiesExportController.TRACKED_ENTITIES )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerTrackedEntitiesExportController
{
    protected static final String TRACKED_ENTITIES = "trackedEntities";

    private static final String DEFAULT_FIELDS_PARAM = "*,!relationships,!enrollments,!events,!programOwners";

    private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER = Mappers.getMapper( TrackedEntityMapper.class );

    @Nonnull
    private final TrackerTrackedEntityCriteriaMapper criteriaMapper;

    @Nonnull
    private final TrackedEntityInstanceService trackedEntityInstanceService;

    @Nonnull
    private final TrackedEntitiesSupportService trackedEntitiesSupportService;

    @Nonnull
    private final FieldFilterService fieldFilterService;

    @Nonnull
    private final CsvEventService<TrackedEntity> csvEventService;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    PagingWrapper<ObjectNode> getInstances( TrackerTrackedEntityCriteria criteria,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
        throws BadRequestException,
        ForbiddenException
    {

        TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        TrackedEntityInstanceParams trackedEntityInstanceParams = map( fields )
            .withIncludeDeleted( criteria.isIncludeDeleted() );

        List<TrackedEntity> trackedEntityInstances = TRACKED_ENTITY_MAPPER
            .fromCollection( trackedEntityInstanceService.getTrackedEntityInstances( queryParams,
                trackedEntityInstanceParams, false, false ) );

        PagingWrapper<ObjectNode> pagingWrapper = new PagingWrapper<>();

        if ( criteria.isPagingRequest() )
        {

            Long count = criteria.isTotalPages()
                ? (long) trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, true, true )
                : null;

            pagingWrapper = pagingWrapper.withPager(
                PagingWrapper.Pager.builder()
                    .page( queryParams.getPageWithDefault() )
                    .total( count )
                    .pageSize( queryParams.getPageSizeWithDefault() )
                    .build() );
        }

        List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes( trackedEntityInstances, fields );
        return pagingWrapper.withInstances( objectNodes );
    }

    @GetMapping( produces = { CONTENT_TYPE_CSV, CONTENT_TYPE_CSV_GZIP, CONTENT_TYPE_CSV_ZIP, CONTENT_TYPE_TEXT_CSV } )
    public void getCsvTrackedEntities( TrackerTrackedEntityCriteria criteria,
        HttpServletResponse response,
        HttpServletRequest request,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
        throws IOException,
        BadRequestException,
        ForbiddenException
    {
        TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        TrackedEntityInstanceParams trackedEntityInstanceParams = map( fields )
            .withIncludeDeleted( criteria.isIncludeDeleted() );

        List<TrackedEntity> trackedEntityInstances = TRACKED_ENTITY_MAPPER
            .fromCollection( trackedEntityInstanceService.getTrackedEntityInstances( queryParams,
                trackedEntityInstanceParams, false, false ) );

        OutputStream outputStream = response.getOutputStream();

        if ( ContextUtils.isAcceptCsvGzip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            outputStream = new GZIPOutputStream( outputStream );
            response.setContentType( CONTENT_TYPE_CSV_GZIP );
            response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trackedEntities.csv.gz\"" );
        }
        else if ( ContextUtils.isAcceptCsvZip( request ) )
        {
            response.addHeader( ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING, "binary" );
            response.setContentType( CONTENT_TYPE_CSV_ZIP );
            response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trackedEntities.csv.zip\"" );
            ZipOutputStream zos = new ZipOutputStream( outputStream );
            zos.putNextEntry( new ZipEntry( "trackedEntities.csv" ) );
            outputStream = zos;
        }
        else
        {
            response.setContentType( CONTENT_TYPE_CSV );
            response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trackedEntities.csv\"" );
        }

        csvEventService.writeEvents( outputStream, trackedEntityInstances, !skipHeader );
    }

    @GetMapping( value = "{id}" )
    public ResponseEntity<ObjectNode> getTrackedEntityInstanceById( @PathVariable String id,
        @RequestParam( required = false ) String program,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
    {
        TrackedEntityInstanceParams trackedEntityInstanceParams = map( fields );

        TrackedEntity trackedEntity = TRACKED_ENTITY_MAPPER.from(
            trackedEntitiesSupportService.getTrackedEntityInstance( id, program, trackedEntityInstanceParams ) );
        return ResponseEntity.ok( fieldFilterService.toObjectNode( trackedEntity, fields ) );
    }

    @GetMapping( value = "{id}", produces = { CONTENT_TYPE_CSV, CONTENT_TYPE_CSV_GZIP, CONTENT_TYPE_TEXT_CSV } )
    public void getCsvTrackedEntityInstanceById( @PathVariable String id,
        HttpServletResponse response,
        @RequestParam( required = false, defaultValue = "false" ) boolean skipHeader,
        @RequestParam( required = false ) String program,
        @RequestParam( defaultValue = DEFAULT_FIELDS_PARAM ) List<String> fields )
        throws IOException
    {
        TrackedEntityInstanceParams trackedEntityInstanceParams = map( fields );

        TrackedEntity trackedEntity = TRACKED_ENTITY_MAPPER.from(
            trackedEntitiesSupportService.getTrackedEntityInstance( id, program, trackedEntityInstanceParams ) );

        OutputStream outputStream = response.getOutputStream();
        response.setContentType( CONTENT_TYPE_CSV );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trackedEntity.csv\"" );
        csvEventService.writeEvents( outputStream, List.of( trackedEntity ), !skipHeader );
    }
}
