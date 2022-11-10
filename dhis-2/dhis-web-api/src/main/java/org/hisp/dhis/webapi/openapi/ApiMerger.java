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
package org.hisp.dhis.webapi.openapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Merge same path and request method endpoints (which reflect Java reality) to
 * be one that OpenAPI can express.
 *
 * In OpenAPI a responses are organised:
 * <ol>
 * <li>query {@code path}</li>
 * <li>{@link RequestMethod}</li>
 * <li>{@link HttpStatus} (response)</li>
 * <li>{@link MediaType} (response)</li>
 * </ol>
 * This means there cannot be two endpoints for same path and method which is
 * how they usually occur in Java when they work with different response
 * {@link MediaType}s. As a consequence endpoints (which reflect the Java world)
 * with same path and request method need to be merged together. In this merge
 * one of the endpoint's parameters have to be discarded. Only the responses can
 * be merged assuming that never is more than one endpoint handling a certain
 * {@link MediaType}.
 *
 * The strategy here is to favour the endpoint that is handling JSON.
 *
 * Since merging is non-trivial this deserved its own file.
 *
 * @author Jan Bernitt
 */
public class ApiMerger
{

    /**
     * Merges two endpoints for the same path and request method.
     *
     * A heuristic is used to decide which of the two endpoints is preserved
     * fully.
     *
     * @param a
     * @param b
     * @return
     */
    static Api.Endpoint mergeEndpoints( Api.Endpoint a, Api.Endpoint b )
    {
        if ( a == null )
            return b;
        if ( b == null )
            return a;
        // FIXME - first see if one returns JSON => pick that
        // otherwise pick the one accepting JSON
        return a;
    }
}
