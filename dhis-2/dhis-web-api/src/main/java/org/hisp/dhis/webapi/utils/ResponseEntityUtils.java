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
package org.hisp.dhis.webapi.utils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

/**
 * Utilities for {@link ResponseEntity}.
 *
 * @author Lars Helge Overland
 */
public class ResponseEntityUtils
{
    /**
     * Returns a {@link ResponseEntity} for which ETag caching is handled. If
     * the given request and ETag indicates that the requested resource has not
     * been modified, a response entity indicating {@code 304 Not Modified} is
     * returned, otherwise, a response entity indicating {@code 200 OK} is
     * returned. In both cases, the {@code Cache-Control} header is set to no
     * cache, and the {@code ETag} header is set to the given ETag value.
     *
     * @param <T>
     * @param request the {@link WebRequest}.
     * @param etag the ETag value.
     * @param bodySupplier the supplier of the response body.
     * @return a {@link ResponseEntity}.
     */
    public static <T> ResponseEntity<T> withEtagCaching( WebRequest request, String etag, Supplier<T> bodySupplier )
    {
        if ( request.checkNotModified( etag ) )
        {
            return ResponseEntity.status( HttpStatus.NOT_MODIFIED )
                .cacheControl( CacheControl.maxAge( 0, TimeUnit.SECONDS ).cachePrivate().mustRevalidate() )
                .eTag( etag )
                .build();
        }

        return ResponseEntity.ok()
            .cacheControl( CacheControl.maxAge( 0, TimeUnit.SECONDS ).cachePrivate().mustRevalidate() )
            .eTag( etag )
            .body( bodySupplier.get() );
    }
}
