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
package org.hisp.dhis.dxf2.webmessage;

import java.util.List;
import java.util.function.Supplier;

import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.scheduling.JobConfigurationWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ErrorReportsWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ImportReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.ObjectReportWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.responses.TypeReportWebMessageResponse;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.springframework.http.HttpStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class WebMessageUtils
{
    public static WebMessage createWebMessage( String message, Status status, HttpStatus httpStatus )
    {
        return new WebMessage( status, httpStatus )
            .setMessage( message );
    }

    public static WebMessage createWebMessage( String message, Status status, HttpStatus httpStatus,
        ErrorCode errorCode )
    {
        return new WebMessage( status, httpStatus )
            .setErrorCode( errorCode )
            .setMessage( message );
    }

    public static WebMessage createWebMessage( String message, String devMessage, Status status, HttpStatus httpStatus )
    {
        return new WebMessage( status, httpStatus )
            .setMessage( message )
            .setDevMessage( devMessage );
    }

    public static WebMessage ok()
    {
        return ok( null );
    }

    public static WebMessage ok( String message )
    {
        return createWebMessage( message, Status.OK, HttpStatus.OK );
    }

    public static WebMessage ok( String message, String devMessage )
    {
        return createWebMessage( message, devMessage, Status.OK, HttpStatus.OK );
    }

    public static WebMessage created()
    {
        return created( null );
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

    public static WebMessage conflict( String message, ErrorCode errorCode )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.CONFLICT, errorCode );
    }

    public static WebMessage conflict( ErrorCode errorCode, Object... args )
    {
        return conflict( new ErrorMessage( errorCode, args ).getMessage(), errorCode );
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

    public static WebMessage badRequest( String message, ErrorCode errorCode )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.BAD_REQUEST, errorCode );
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

    public static WebMessage unauthorized( String message )
    {
        return createWebMessage( message, Status.ERROR, HttpStatus.UNAUTHORIZED );
    }

    public static WebMessage importSummary( ImportSummary importSummary )
    {
        if ( importSummary.isStatus( ImportStatus.ERROR ) )
        {
            return conflict( "An error occurred, please check import summary." )
                .setResponse( importSummary );
        }
        if ( importSummary.isStatus( ImportStatus.WARNING ) )
        {
            return new WebMessage( Status.WARNING, HttpStatus.CONFLICT )
                .setMessage( "One more conflicts encountered, please check import summary." )
                .setResponse( importSummary );
        }
        return ok( "Import was successful." )
            .setResponse( importSummary );
    }

    public static WebMessage importSummaries( ImportSummaries importSummaries )
    {
        if ( importSummaries.isStatus( ImportStatus.ERROR ) )
        {
            return conflict( "An error occurred, please check import summary." )
                .setResponse( importSummaries );
        }
        if ( importSummaries.isStatus( ImportStatus.WARNING ) )
        {
            return new WebMessage( Status.WARNING, HttpStatus.CONFLICT )
                .setMessage( "One or more conflicts encountered, please check import summary." )
                .setResponse( importSummaries );
        }
        return ok( "Import was successful." )
            .setResponse( importSummaries );
    }

    public static WebMessage importReport( ImportReport importReport )
    {
        if ( importReport.getStatus() != Status.OK )
        {
            return new WebMessage( Status.WARNING, HttpStatus.CONFLICT )
                .setMessage( "One or more errors occurred, please see full details in import report." )
                .setResponse( new ImportReportWebMessageResponse( importReport ) );
        }
        return ok()
            .setResponse( new ImportReportWebMessageResponse( importReport ) );
    }

    public static WebMessage typeReport( TypeReport typeReport )
    {
        if ( !typeReport.hasErrorReports() )
        {
            return ok()
                .setResponse( new TypeReportWebMessageResponse( typeReport ) );
        }
        return conflict( "One or more errors occurred, please see full details in import report." )
            .setResponse( new TypeReportWebMessageResponse( typeReport ) );
    }

    public static WebMessage objectReport( ImportReport importReport )
    {
        ObjectReport firstObjectReport = importReport.getFirstObjectReport();
        return firstObjectReport == null
            ? ok()
            : objectReport( firstObjectReport );
    }

    public static WebMessage objectReport( ObjectReport objectReport )
    {
        if ( objectReport.isEmpty() )
        {
            return ok()
                .setResponse( new ObjectReportWebMessageResponse( objectReport ) );
        }
        return new WebMessage( Status.WARNING, HttpStatus.CONFLICT )
            .setMessage( "One or more errors occurred, please see full details in import report." )
            .setResponse( new ObjectReportWebMessageResponse( objectReport ) );
    }

    public static WebMessage jobConfigurationReport( JobConfiguration jobConfiguration )
    {
        return ok( "Initiated " + jobConfiguration.getName() )
            .setResponse( new JobConfigurationWebMessageResponse( jobConfiguration ) );
    }

    public static WebMessage errorReports( List<ErrorReport> errorReports )
    {
        if ( !errorReports.isEmpty() )
        {
            return badRequest( null )
                .setResponse( new ErrorReportsWebMessageResponse( errorReports ) );
        }
        return ok()
            .setResponse( new ErrorReportsWebMessageResponse( errorReports ) );
    }

    public static TypeReport typeReport( Class<?> clazz, List<ErrorReport> errorReports )
    {
        ObjectReport objectReport = new ObjectReport( clazz, 0 );
        objectReport.addErrorReports( errorReports );
        TypeReport typeReport = new TypeReport( clazz );
        typeReport.addObjectReport( objectReport );
        return typeReport;
    }

    /**
     * Runs the provided validation and throws a {@link WebMessageException}
     * with the {@link #errorReports(List)} in case there are any.
     *
     * @param validation a validation computation to run to see if there are
     *        {@link ErrorReport}s.
     * @throws WebMessageException In case there were any {@link ErrorReport}s
     */
    public static void validateAndThrowErrors( Supplier<List<ErrorReport>> validation )
        throws WebMessageException
    {
        List<ErrorReport> errors = validation.get();
        if ( !errors.isEmpty() )
        {
            throw new WebMessageException( errorReports( errors ) );
        }
    }

    private WebMessageUtils()
    {
    }
}
