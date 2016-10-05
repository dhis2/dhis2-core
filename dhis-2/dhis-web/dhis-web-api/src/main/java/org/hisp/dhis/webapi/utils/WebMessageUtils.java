package org.hisp.dhis.webapi.utils;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.dxf2.common.Status;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.responses.ErrorReportsWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ImportReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ObjectReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.TypeReportWebMessageResponse;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class WebMessageUtils
{
    public static WebMessage createWebMessage( String message, Status status, HttpStatus httpStatus )
    {
        WebMessage webMessage = new WebMessage( status, httpStatus );
        webMessage.setMessage( message );

        return webMessage;
    }

    public static WebMessage createWebMessage( String message, String devMessage, Status status, HttpStatus httpStatus )
    {
        WebMessage webMessage = new WebMessage( status, httpStatus );
        webMessage.setMessage( message );
        webMessage.setDevMessage( devMessage );

        return webMessage;
    }

    public static WebMessage ok( String message )
    {
        return createWebMessage( message, Status.OK, HttpStatus.OK );
    }

    public static WebMessage ok( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.OK, HttpStatus.OK );
    }

    public static WebMessage created( String message )
    {
        return createWebMessage( message, Status.OK, HttpStatus.CREATED );
    }

    public static WebMessage created( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.OK, HttpStatus.CREATED );
    }

    public static WebMessage notFound( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage notFound( Class<?> klass, String id )
    {
        String message = klass.getSimpleName() + " with id " + id + " could not be found.";
        return createWebMessage( message, Status.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage notFound( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.NOT_FOUND );
    }

    public static WebMessage conflict( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.CONFLICT );
    }

    public static WebMessage conflict( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.CONFLICT );
    }

    public static WebMessage error( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.INTERNAL_SERVER_ERROR );
    }

    public static WebMessage error( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.INTERNAL_SERVER_ERROR );
    }

    public static WebMessage badRequest( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.BAD_REQUEST );
    }

    public static WebMessage badRequest( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.BAD_REQUEST );
    }

    public static WebMessage forbidden( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.FORBIDDEN );
    }

    public static WebMessage forbidden( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.FORBIDDEN );
    }

    public static WebMessage serviceUnavailable( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.SERVICE_UNAVAILABLE );
    }

    public static WebMessage serviceUnavailable( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.SERVICE_UNAVAILABLE );
    }

    public static WebMessage unprocessableEntity( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.UNPROCESSABLE_ENTITY );
    }

    public static WebMessage unprocessableEntity( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.UNPROCESSABLE_ENTITY );
    }

    public static WebMessage unathorized( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.UNAUTHORIZED );
    }

    public static WebMessage unathorized( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.ERROR, HttpStatus.UNAUTHORIZED );
    }

    public static WebMessage importSummary( ImportSummary importSummary )
    {
        WebMessage webMessage = new WebMessage();

        if ( importSummary.isStatus( ImportStatus.ERROR ) )
        {
            webMessage.setMessage( "An error occurred, please check import summary." );
            webMessage.setStatus( Status.ERROR );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else if ( !importSummary.getConflicts().isEmpty() )
        {
            webMessage.setMessage( "One more conflicts encountered, please check import summary." );
            webMessage.setStatus( Status.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else
        {
            webMessage.setMessage( "Import was successful." );
            webMessage.setStatus( Status.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }

        webMessage.setResponse( importSummary );

        return webMessage;
    }

    public static WebMessage importSummaries( ImportSummaries importSummaries )
    {
        WebMessage webMessage = new WebMessage();

        if ( importSummaries.getIgnored() > 0 )
        {
            webMessage.setMessage( "One more conflicts encountered, please check import summary." );
            webMessage.setStatus( Status.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }
        else
        {
            webMessage.setMessage( "Import was successful." );
            webMessage.setStatus( Status.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }

        webMessage.setResponse( importSummaries );

        return webMessage;
    }

    public static WebMessage importReport( ImportReport importReport )
    {
        WebMessage webMessage = new WebMessage();
        webMessage.setResponse( new ImportReportWebMessageResponse( importReport ) );

        webMessage.setStatus( importReport.getStatus() );

        if ( webMessage.getStatus() != Status.OK )
        {
            webMessage.setMessage( "One more more errors occurred, please see full details in import report." );
            webMessage.setStatus( Status.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }

        return webMessage;
    }

    public static WebMessage typeReport( TypeReport typeReport )
    {
        WebMessage webMessage = new WebMessage();
        webMessage.setResponse( new TypeReportWebMessageResponse( typeReport ) );

        if ( typeReport.getErrorReports().isEmpty() )
        {
            webMessage.setStatus( Status.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }
        else
        {
            webMessage.setMessage( "One more more errors occurred, please see full details in import report." );
            webMessage.setStatus( Status.ERROR );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }

        return webMessage;
    }

    public static WebMessage objectReport( ImportReport importReport )
    {
        WebMessage webMessage = new WebMessage( Status.OK, HttpStatus.OK );

        if ( !importReport.getTypeReports().isEmpty() )
        {
            TypeReport typeReport = importReport.getTypeReports().get( 0 );

            if ( !typeReport.getObjectReports().isEmpty() )
            {
                return objectReport( typeReport.getObjectReports().get( 0 ) );
            }
        }

        return webMessage;
    }

    public static WebMessage objectReport( ObjectReport objectReport )
    {
        WebMessage webMessage = new WebMessage();
        webMessage.setResponse( new ObjectReportWebMessageResponse( objectReport ) );

        if ( objectReport.isEmpty() )
        {
            webMessage.setStatus( Status.OK );
            webMessage.setHttpStatus( HttpStatus.OK );
        }
        else
        {
            webMessage.setMessage( "One more more errors occurred, please see full details in import report." );
            webMessage.setStatus( Status.WARNING );
            webMessage.setHttpStatus( HttpStatus.CONFLICT );
        }

        return webMessage;
    }

    public static WebMessage errorReports( List<ErrorReport> errorReports )
    {
        WebMessage webMessage = new WebMessage();
        webMessage.setResponse( new ErrorReportsWebMessageResponse( errorReports ) );

        if ( !errorReports.isEmpty() )
        {
            webMessage.setStatus( Status.ERROR );
            webMessage.setHttpStatus( HttpStatus.BAD_REQUEST );
        }

        return webMessage;
    }

    private WebMessageUtils()
    {
    }
}
