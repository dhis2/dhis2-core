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
package org.hisp.dhis.webapi.controller;

import java.util.Optional;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.cache.ExpressionDimensionItemAnalyticsCache;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.expressiondimensionitem.ExpressionDimensionItem;
import org.hisp.dhis.schema.descriptors.ExpressionDimensionItemSchemaDescriptor;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CRUD Controller for ExpressionDimensionItem entity
 */
@OpenApi.Tags( "analytics" )
@Controller
@RequestMapping( value = ExpressionDimensionItemSchemaDescriptor.API_ENDPOINT )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@AllArgsConstructor
public class ExpressionDimensionItemController extends AbstractCrudController<ExpressionDimensionItem>
{
    private final AnalyticsCache analyticsCache;

    private final ExpressionDimensionItemAnalyticsCache expressionDimensionItemAnalyticsCache;

    /**
     * Extension of basic method is handling of update event for expression
     * dimension item
     *
     * @param patch
     * @param entityAfter
     */
    @Override
    protected void postPatchEntity( JsonPatch patch, ExpressionDimensionItem entityAfter )
    {
        super.postPatchEntity( patch, entityAfter );

        Optional<Set<String>> dataQueryParamsKeySet = expressionDimensionItemAnalyticsCache.get( entityAfter.getUid() );

        // All entries will be invalidated in analytics cache to provide new calculated data of expression dimension item
        // in further analytics requests.
        dataQueryParamsKeySet.ifPresent( keys -> keys.forEach( analyticsCache::invalidate ) );

        // reference/instance of updated expression dimension item will be removed from cache as well.
        // Next put of key/value will happen in process of analytics request (expression dimension item must be included)
        expressionDimensionItemAnalyticsCache.invalidate( entityAfter.getUid() );
    }
}
