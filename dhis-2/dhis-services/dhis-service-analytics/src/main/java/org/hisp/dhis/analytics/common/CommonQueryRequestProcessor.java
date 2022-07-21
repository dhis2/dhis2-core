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
package org.hisp.dhis.analytics.common;

import static org.hisp.dhis.common.CustomDateHelper.getCustomDateFilters;
import static org.hisp.dhis.common.CustomDateHelper.getDimensionsWithRefactoredPeDimension;
import static org.hisp.dhis.commons.collection.CollectionUtils.emptyIfNull;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.AnalyticsDateFilter;
import org.springframework.stereotype.Component;

/**
 * Processor class for CommonQueryRequest objects.
 *
 * @see Processor
 */
@Component
@RequiredArgsConstructor
public class CommonQueryRequestProcessor implements Processor<CommonQueryRequest>
{

    @Override
    public CommonQueryRequest process( final CommonQueryRequest commonQueryRequest )
    {
        return commonQueryRequest.toBuilder()
            .dimension(
                getDimensionsWithRefactoredPeDimension(
                    emptyIfNull( commonQueryRequest.getDimension() ),
                    getCustomDateFilters(
                        AnalyticsDateFilter::appliesToTei,
                        analyticsDateFilter -> o -> analyticsDateFilter.getTeiExtractor()
                            .apply( (CommonQueryRequest) o ),
                        commonQueryRequest ) ) )
            .build();
    }
}
