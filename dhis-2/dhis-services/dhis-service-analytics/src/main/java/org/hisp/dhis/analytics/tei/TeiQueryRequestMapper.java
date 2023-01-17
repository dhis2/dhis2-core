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
package org.hisp.dhis.analytics.tei;

import static org.hisp.dhis.analytics.tei.query.TeiFields.getTrackedEntityAttributes;

import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestMapper;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Component;

/**
 * This component maps query request objects into query params objects. The
 * query params objects represents the preparation of data and elements that are
 * ready to be queried at database level.
 */
@Component
@RequiredArgsConstructor
public class TeiQueryRequestMapper
{
    private final CommonQueryRequestMapper commonQueryRequestMapper;

    private final TrackedEntityTypeService trackedEntityTypeService;

    /**
     * Maps incoming query requests into a valid and usable
     * {@link TeiQueryParams}.
     *
     * @param queryRequest the {@link QueryRequest} of type
     *        {@link TeiQueryRequest}
     * @return the populated {@link TeiQueryParams}
     * @throws IllegalArgumentException if the current TrackedEntityType in the
     *         given request is invalid or non-existent
     */
    public TeiQueryParams map( QueryRequest<TeiQueryRequest> queryRequest )
    {
        TrackedEntityType trackedEntityType = getTrackedEntityType( queryRequest );

        // adding tracked entity type attributes to the list of dimensions
        queryRequest.getCommonQueryRequest().getDimension().addAll(
            getTrackedEntityAttributes( trackedEntityType )
                .map( BaseIdentifiableObject::getUid )
                .collect( Collectors.toList() ) );

        return TeiQueryParams.builder()
            .trackedEntityType( getTrackedEntityType( queryRequest ) )
            .commonParams( commonQueryRequestMapper.map(
                queryRequest.getCommonQueryRequest() ) )
            .build();
    }

    private TrackedEntityType getTrackedEntityType( QueryRequest<TeiQueryRequest> queryRequest )
    {
        return Optional.of( queryRequest.getRequest().getTrackedEntityType() )
            .map( trackedEntityTypeService::getTrackedEntityType )
            .orElseThrow( () -> new IllegalArgumentException( "Unable to find TrackedEntityType with UID: "
                + queryRequest.getRequest().getTrackedEntityType() ) );
    }
}
