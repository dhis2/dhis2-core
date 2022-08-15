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
package org.hisp.dhis.common;

import javax.annotation.PreDestroy;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * @author Jan Bernitt
 */
@Service
public class DefaultRequestInfoService implements RequestInfoService
{
    /**
     * Key used for slf4j logging of the X-Request header.
     */
    private static final String X_REQUEST_ID = "xRequestID";

    private final ThreadLocal<RequestInfo> currentInfo = new ThreadLocal<>();

    /**
     * This method is by intention not part of the {@link RequestInfoService}
     * interface as it should only be used in one place to update the info for
     * the current thread at the beginning of a request.
     *
     * @param info the info to set
     */
    public void setCurrentInfo( RequestInfo info )
    {
        info = sanitised( info );
        currentInfo.set( info );
        if ( info == null )
        {
            MDC.remove( X_REQUEST_ID );
            return;
        }
        String xRequestID = info.getHeaderXRequestID();
        if ( xRequestID == null )
        {
            MDC.remove( X_REQUEST_ID );
        }
        else
        {
            MDC.put( X_REQUEST_ID, xRequestID );
        }
    }

    private RequestInfo sanitised( RequestInfo info )
    {
        if ( info == null )
        {
            return null;
        }
        String xRequestID = info.getHeaderXRequestID();
        if ( !RequestInfo.isValidXRequestID( xRequestID ) )
        {
            return info.toBuilder().headerXRequestID( "(illegal)" ).build();
        }
        return info;
    }

    @Override
    public RequestInfo getCurrentInfo()
    {
        return currentInfo.get();
    }

    @PreDestroy
    public void preDestroy()
    {
        currentInfo.remove();
    }
}
