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

import static org.hisp.dhis.common.RequestTypeAware.EndpointItem.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.Processor;
import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.Validator;
import org.hisp.dhis.analytics.tei.TeiAnalyticsQueryService;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.analytics.tei.TeiQueryRequest;
import org.hisp.dhis.analytics.tei.TeiRequestMapper;
import org.hisp.dhis.common.AnalyticsPagingCriteria;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Controller class responsible exclusively for querying operations on top of
 * tracker entity instances objects.
 *
 * Methods in this controller should not change any state.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping( TeiQueryController.TRACKED_ENTITIES )
class TeiQueryController
{

    static final String TRACKED_ENTITIES = "analytics/trackedEntities";

    private final TeiAnalyticsQueryService teiAnalyticsQueryService;

    private final Processor<TeiQueryRequest> teiQueryRequestProcessor;

    private final Processor<CommonQueryRequest> commonQueryRequestProcessor;

    private final Processor<AnalyticsPagingCriteria> pagingCriteriaProcessor;

    private final Validator<QueryRequest<TeiQueryRequest>> teiQueryRequestValidator;

    private final Validator<CommonQueryRequest> commonQueryRequestValidator;

    private final TeiRequestMapper mapper;

    private final ContextUtils contextUtils;

    @GetMapping( "query/{trackedEntityType}" )
    Grid getGrid(
        @PathVariable
        final String trackedEntityType,
        final TeiQueryRequest teiQueryRequest,
        final CommonQueryRequest commonQueryRequest,
        final AnalyticsPagingCriteria pagingRequest,
        final DhisApiVersion apiVersion,
        final HttpServletResponse response )
    {
        QueryRequest<TeiQueryRequest> queryRequest = QueryRequest.<TeiQueryRequest> builder()
            .request( teiQueryRequestProcessor.process(
                teiQueryRequest.withTrackedEntityType( trackedEntityType ) ) )
            .commonQueryRequest( commonQueryRequestProcessor.process( commonQueryRequest ) )
            .pagingCriteria( pagingCriteriaProcessor.process( (AnalyticsPagingCriteria) pagingRequest
                .withEndpointItem( TRACKED_ENTITY_INSTANCE ).withQueryEndpointAction() ) )
            .build();

        commonQueryRequestValidator.validate( queryRequest.getCommonQueryRequest() );
        teiQueryRequestValidator.validate( queryRequest );

        contextUtils.configureResponse( response, CONTENT_TYPE_JSON, RESPECT_SYSTEM_SETTING );

        final TeiQueryParams params = mapper.map( queryRequest, apiVersion );

        return teiAnalyticsQueryService.getTeiGrid( params );
    }

}
