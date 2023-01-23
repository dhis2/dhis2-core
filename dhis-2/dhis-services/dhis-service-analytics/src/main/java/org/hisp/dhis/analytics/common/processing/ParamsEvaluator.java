/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.util.Assert.notEmpty;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hisp.dhis.common.GridHeader;
import org.springframework.stereotype.Component;

/**
 * Analytics component responsible for centralizing, evaluating and processing
 * query parameters provided by the consumer. It should act on the most granular
 * param objects to make it more reusable across all analytics queries.
 *
 * @author maikel arabori
 */
@Component
public class ParamsEvaluator
{
    /**
     * Applies the logic to retain only the headers name present in the URL
     * param, if any. If no "paramHeaders" are present it returns the given
     * "headers". It does not change the state of the original arguments.
     *
     * @param headers all headers represent by a set of {@link GridHeader}
     * @param paramHeaders the set of headers name from the param
     * @return a new set of {@link GridHeader} containing elements that only
     *         match the param headers name
     */
    public Set<GridHeader> applyHeaders( Set<GridHeader> headers, Set<String> paramHeaders )
    {
        notEmpty( headers, "The 'headers' must not be null/empty" );

        boolean hasHeadersParam = isNotEmpty( paramHeaders );

        if ( hasHeadersParam )
        {
            Set<GridHeader> expectedHeaders = new LinkedHashSet<>();

            // Retains only the headers defined in the URL (in the defined order).
            paramHeaders.forEach( name -> headers.forEach( header -> {
                if ( header.getName().equals( name ) )
                {
                    expectedHeaders.add( header );
                }
            } ) );

            return expectedHeaders;
        }

        return headers;
    }
}
