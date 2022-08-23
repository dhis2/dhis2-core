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
package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * Web API equivalent of a {@code WebMessage} or {@code DescriptiveWebMessage}
 *
 * @author Jan Bernitt
 */
public interface JsonWebMessage extends JsonObject
{
    default String getHttpStatus()
    {
        return getString( "httpStatus" ).string();
    }

    default int getHttpStatusCode()
    {
        return getNumber( "httpStatusCode" ).intValue();
    }

    default String getStatus()
    {
        return getString( "status" ).string();
    }

    default String getMessage()
    {
        return getString( "message" ).string();
    }

    default String getDescription()
    {
        return getString( "description" ).string();
    }

    default ErrorCode getErrorCode()
    {
        return getString( "errorCode" ).parsed( ErrorCode::valueOf );
    }

    default JsonObject getResponse()
    {
        return getObject( "response" );
    }
}
