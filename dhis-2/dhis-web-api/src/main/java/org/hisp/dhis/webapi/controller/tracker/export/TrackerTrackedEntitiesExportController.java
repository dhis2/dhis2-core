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

import static org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport.RESOURCE_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.mapper.TrackedEntityMapper;
import org.hisp.dhis.webapi.controller.event.mapper.TrackedEntityCriteriaMapper;
import org.hisp.dhis.webapi.controller.event.webrequest.PagingWrapper;
import org.hisp.dhis.webapi.controller.event.webrequest.tracker.TrackerTrackedEntityCriteria;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.TrackedEntityInstanceSupportService;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping( value = RESOURCE_PATH + "/" + TrackerTrackedEntitiesExportController.TRACKED_ENTITIES )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class TrackerTrackedEntitiesExportController
{
    protected final static String TRACKED_ENTITIES = "trackedEntities";

    private final ContextService contextService;

    private final TrackedEntityCriteriaMapper criteriaMapper;

    private final TrackedEntityInstanceService trackedEntityInstanceService;

    private static final TrackedEntityMapper TRACKED_ENTITY_MAPPER = Mappers.getMapper( TrackedEntityMapper.class );

    private final TrackedEntityInstanceSupportService trackedEntityInstanceSupportService;

    @GetMapping( produces = APPLICATION_JSON_VALUE )
    PagingWrapper<TrackedEntity> getInstances( TrackerTrackedEntityCriteria criteria )
    {
        List<String> fields = contextService.getFieldsFromRequestOrAll();

        TrackedEntityInstanceQueryParams queryParams = criteriaMapper.map( criteria );

        Collection<TrackedEntity> trackedEntityInstances = TRACKED_ENTITY_MAPPER
            .fromCollection( trackedEntityInstanceService.getTrackedEntityInstances( queryParams,
                trackedEntityInstanceSupportService.getTrackedEntityInstanceParams( fields ), false, false ) );

        PagingWrapper<TrackedEntity> trackedEntityInstancePagingWrapper = new PagingWrapper<>();

        if ( criteria.isPagingRequest() )
        {

            Long count = criteria.isTotalPages()
                ? (long) trackedEntityInstanceService.getTrackedEntityInstanceCount( queryParams, true, true )
                : null;

            trackedEntityInstancePagingWrapper = trackedEntityInstancePagingWrapper.withPager(
                PagingWrapper.Pager.builder()
                    .page( queryParams.getPageWithDefault() )
                    .total( count )
                    .pageSize( queryParams.getPageSizeWithDefault() )
                    .build() );
        }

        return trackedEntityInstancePagingWrapper.withInstances( trackedEntityInstances );
    }

    @GetMapping( value = "{id}" )
    public TrackedEntity getTrackedEntityInstanceById( @PathVariable String id,
        @RequestParam( required = false ) String program )
    {
        return TRACKED_ENTITY_MAPPER.from( trackedEntityInstanceSupportService.getTrackedEntityInstance( id, program,
            contextService.getFieldsFromRequestOrAll() ) );
    }
}
