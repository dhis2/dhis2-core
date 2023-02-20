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
package org.hisp.dhis.webapi.controller.tei;

import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.Processor;
import org.hisp.dhis.analytics.common.processing.Validator;
import org.hisp.dhis.analytics.dimensions.AnalyticsDimensionsPagingWrapper;
import org.hisp.dhis.analytics.tei.TeiAnalyticsDimensionsService;
import org.hisp.dhis.analytics.tei.TeiAnalyticsQueryService;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.TeiQueryRequest;
import org.hisp.dhis.analytics.tei.TeiQueryRequestMapper;
import org.hisp.dhis.common.DimensionsCriteria;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.webapi.dimension.DimensionFilteringAndPagingService;
import org.hisp.dhis.webapi.dimension.DimensionMapperService;
import org.hisp.dhis.webapi.dimension.TeiAnalyticsPrefixStrategy;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Controller class responsible exclusively for querying operations on top of
 * tracker entity instances objects. Methods in this controller should not
 * change any state.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping( TeiQueryController.TRACKED_ENTITIES )
class TeiQueryController
{
    static final String TRACKED_ENTITIES = "analytics/trackedEntities";

    @Nonnull
    private final TeiAnalyticsQueryService teiAnalyticsQueryService;

    @Nonnull
    private final Processor<TeiQueryRequest> teiQueryRequestProcessor;

    @Nonnull
    private final Processor<CommonQueryRequest> commonQueryRequestProcessor;

    @Nonnull
    private final Validator<QueryRequest<TeiQueryRequest>> teiQueryRequestValidator;

    @Nonnull
    private final Validator<CommonQueryRequest> commonQueryRequestValidator;

    @Nonnull
    private final DimensionFilteringAndPagingService dimensionFilteringAndPagingService;

    @Nonnull
    private final DimensionMapperService dimensionMapperService;

    @Nonnull
    private final TeiAnalyticsDimensionsService teiAnalyticsDimensionsService;

    @Nonnull
    private final TeiQueryRequestMapper mapper;

    @Nonnull
    private final ContextUtils contextUtils;

    @GetMapping( value = "query/{trackedEntityType}", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    Grid getGrid(
        @PathVariable String trackedEntityType,
        TeiQueryRequest teiQueryRequest,
        CommonQueryRequest commonQueryRequest )
    {
        return getGrid( trackedEntityType, teiQueryRequest, commonQueryRequest, teiAnalyticsQueryService::getGrid );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_ANALYTICS_EXPLAIN')" )
    @GetMapping( value = "query/{trackedEntityType}/explain", produces = { APPLICATION_JSON_VALUE,
        "application/javascript" } )
    Grid getGridExplain(
        @PathVariable String trackedEntityType,
        TeiQueryRequest teiQueryRequest,
        CommonQueryRequest commonQueryRequest )
    {
        return getGrid( trackedEntityType, teiQueryRequest, commonQueryRequest,
            teiAnalyticsQueryService::getGridExplain );
    }

    private Grid getGrid( String trackedEntityType, TeiQueryRequest teiQueryRequest,
        CommonQueryRequest commonQueryRequest,
        Function<TeiQueryParams, Grid> executor )
    {
        QueryRequest<TeiQueryRequest> queryRequest = QueryRequest.<TeiQueryRequest> builder()
            .request( teiQueryRequestProcessor.process(
                teiQueryRequest.withTrackedEntityType( trackedEntityType ) ) )
            .commonQueryRequest( commonQueryRequestProcessor.process( commonQueryRequest ) )
            .build();

        teiQueryRequestValidator.validate( queryRequest );

        TeiQueryParams params = mapper.map( queryRequest );

        return executor.apply( params );
    }

    /**
     * This method returns the collection of all possible dimensions that can be
     * applied for the given "trackedEntityType".
     */
    @GetMapping( "/query/dimensions" )
    public AnalyticsDimensionsPagingWrapper<ObjectNode> getQueryDimensions(
        @RequestParam String trackedEntityType,
        @RequestParam( required = false ) Set<String> program,
        @RequestParam( defaultValue = "*" ) List<String> fields,
        DimensionsCriteria dimensionsCriteria,
        HttpServletResponse response )
    {
        contextUtils.configureResponse( response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING );

        return dimensionFilteringAndPagingService
            .pageAndFilter(
                dimensionMapperService.toDimensionResponse(
                    teiAnalyticsDimensionsService.getQueryDimensionsByTrackedEntityTypeId( trackedEntityType, program ),
                    TeiAnalyticsPrefixStrategy.INSTANCE ),
                dimensionsCriteria,
                fields );
    }
}
