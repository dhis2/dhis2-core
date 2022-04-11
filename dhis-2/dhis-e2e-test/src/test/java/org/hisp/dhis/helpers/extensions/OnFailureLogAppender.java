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

package org.hisp.dhis.helpers.extensions;

import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.log.LogRepository;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.TextStringBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class OnFailureLogAppender
    implements ResponseValidationFailureListener
{
    final String[] includedRequestParts = { "method", "URI", "Body" };

    final String[] ignoredResponseParts = { "Cache-Control", "ETag", "X-Content-Type-Options", "X-XSS-Protection",
        "X-Frame-Options", "Date", "Keep-Alive", "Connection" };

    @Override
    public void onFailure( RequestSpecification requestSpecification, ResponseSpecification responseSpecification,
        Response response )
    {
        LogRepository logRepository = ((RestAssuredResponseImpl) response).getLogRepository();

        TextStringBuilder builder = new TextStringBuilder();

        builder.appendNewLine()
            .appendln( filteredRequestLogs( logRepository ) )
            .appendNewLine()
            .appendln( filteredResponseLog( logRepository ) );

        responseSpecification.onFailMessage( builder.build() );
    }

    private String filteredRequestLogs( LogRepository logRepository )
    {
        return Arrays.stream( logRepository.getRequestLog().split( System.lineSeparator() ) )
            .filter( p -> StringUtils.containsAny( p, includedRequestParts ) )
            .collect( Collectors.joining( System.lineSeparator() ) );
    }

    private String filteredResponseLog( LogRepository logRepository )
    {
        return Arrays.stream( logRepository.getResponseLog().split( System.lineSeparator() ) )
            .filter( p -> !StringUtils.containsAny( p, ignoredResponseParts ) )
            .collect( Collectors.joining( System.lineSeparator() ) );
    }

}
