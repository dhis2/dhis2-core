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

import java.util.function.Consumer;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.Expected;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * A generic error JSON as usually returned by DHIS2.
 *
 * @author Jan Bernitt
 */
public interface JsonError extends JsonObject
{
    @Expected
    default String getHttpStatus()
    {
        return getString( "httpStatus" ).string();
    }

    @Expected
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

    default String getDevMessage()
    {
        return getString( "devMessage" ).string();
    }

    default ErrorCode getErrorCode()
    {
        return getString( "errorCode" ).parsed( ErrorCode::valueOf );
    }

    /**
     * OBS! This property only exists in some error responses.
     */
    default JsonTypeReport getTypeReport()
    {
        return get( "response", JsonTypeReport.class );
    }

    /**
     * OBS! This is not a getter for a property in the response but a helper to
     * extract a summary description based on the error response.
     *
     * @return a error summary suitable to be used as assert message.
     */
    default String summary()
    {
        if ( !isA( JsonError.class ) )
        {
            return node().getDeclaration();
        }
        StringBuilder str = new StringBuilder();
        Consumer<JsonList<JsonErrorReport>> printer = errors -> {
            if ( errors.exists() )
            {
                for ( JsonErrorReport error : errors )
                {
                    str.append( "\n  " ).append( error.getErrorCode() ).append( ' ' ).append( error.getMessage() );
                }
            }
        };
        String message = getMessage();
        str.append( message != null
            ? message
            : String.format( "(no error message in %d %s response)", getHttpStatusCode(),
                getHttpStatus() ) );
        JsonTypeReport report = getTypeReport();
        if ( report.exists() )
        {
            printer.accept( report.getErrorReports() );
            if ( report.getObjectReports().exists() )
            {
                for ( JsonObjectReport objectReport : report.getObjectReports() )
                {
                    str.append( "\n* " ).append( objectReport.getKlass() );
                    printer.accept( objectReport.getErrorReports() );
                }
            }
            addSummaries( str, report );
        }
        return str.toString();
    }

    static void addSummaries( StringBuilder str, JsonTypeReport report )
    {
        JsonList<JsonImportSummary> summaries = report.getImportSummaries();
        if ( summaries.exists() )
        {
            for ( JsonImportSummary summary : summaries )
            {
                for ( JsonImportConflict conflict : summary.getConflicts() )
                {
                    str.append( "\n  " ).append( conflict.getObject() ).append( ' ' ).append( conflict.getValue() );
                }
            }
        }
    }
}
