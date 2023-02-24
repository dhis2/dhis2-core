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

import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.feedback.ErrorCode.E4014;
import static org.hisp.dhis.feedback.ErrorCode.E7222;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.QueryRequest;
import org.hisp.dhis.analytics.common.processing.CommonQueryRequestValidator;
import org.hisp.dhis.analytics.common.processing.Validator;
import org.hisp.dhis.common.IllegalQueryException;
import org.springframework.stereotype.Component;

/**
 * Component responsible for validation rules on top of analytics tracker entity
 * request queries.
 */
@Component
@RequiredArgsConstructor
public class TeiQueryRequestValidator implements Validator<QueryRequest<TeiQueryRequest>>
{
    private final CommonQueryRequestValidator commonQueryRequestValidator;

    /**
     * Runs a validation on the given query request object {@link QueryRequest},
     * preventing basic syntax and consistency issues.
     *
     * @param queryRequest the {@link QueryRequest} of type
     *        {@link TeiQueryRequest}.
     * @throws IllegalQueryException if some invalid state is found.
     */
    @Override
    public void validate( QueryRequest<TeiQueryRequest> queryRequest )
    {
        String trackedEntityType = queryRequest.getRequest().getTrackedEntityType();

        if ( !isValidUid( trackedEntityType ) )
        {
            throwIllegalQueryEx( E4014, trackedEntityType, "trackedEntityType" );
        }

        commonQueryRequestValidator.validate( queryRequest.getCommonQueryRequest() );

        checkAllowedDimensions( queryRequest );
    }

    /**
     * Looks for invalid or unsupported queries/filters/dimensions.
     *
     * @param queryRequest the {@link QueryRequest} of
     *        type{@link TeiQueryRequest}.
     * @throws IllegalQueryException if some invalid state is found.
     */
    private void checkAllowedDimensions( QueryRequest<TeiQueryRequest> queryRequest )
    {
        Set<String> dimensions = queryRequest.getCommonQueryRequest().getDimension();

        dimensions.forEach( dim -> {
            // The "pe" dimension is not supported for TEI queries.
            if ( containsPe( dim ) )
            {
                throwIllegalQueryEx( E7222, dim );
            }
        } );
    }

    private boolean containsPe( String dimension )
    {
        return dimension.contains( "pe:" );
    }
}
