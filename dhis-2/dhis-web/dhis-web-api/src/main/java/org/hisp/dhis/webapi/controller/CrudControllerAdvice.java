package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.core.JsonParseException;
import org.hibernate.exception.ConstraintViolationException;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.dxf2.adx.AdxException;
import org.hisp.dhis.dxf2.metadata.MetadataExportException;
import org.hisp.dhis.dxf2.metadata.MetadataImportException;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fieldfilter.FieldFilterException;
import org.hisp.dhis.query.QueryException;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.webapi.controller.exception.BadRequestException;
import org.hisp.dhis.webapi.controller.exception.MetadataImportConflictException;
import org.hisp.dhis.webapi.controller.exception.MetadataSyncException;
import org.hisp.dhis.webapi.controller.exception.MetadataVersionException;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.exception.OperationNotAllowedException;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyEditorSupport;
import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ControllerAdvice
public class CrudControllerAdvice
{
    @Autowired
    private WebMessageService webMessageService;

    @InitBinder
    protected void initBinder( WebDataBinder binder )
    {
        binder.registerCustomEditor( Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText( String value ) throws IllegalArgumentException
            {
                setValue( DateUtils.parseDate( value ) );
            }
        } );
    }

    @ExceptionHandler( { IllegalQueryException.class, DeleteNotAllowedException.class, InvalidIdentifierReferenceException.class } )
    public void conflictsExceptionHandler( Exception ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { DataApprovalException.class, AdxException.class, IllegalStateException.class } )
    public void dataApprovalExceptionHandler( Exception ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { JsonParseException.class, MetadataImportException.class, MetadataExportException.class } )
    public void jsonParseExceptionHandler( Exception ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { QueryParserException.class, QueryException.class } )
    public void queryExceptionHandler( Exception ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( FieldFilterException.class )
    public void fieldFilterExceptionHandler( FieldFilterException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { NotAuthenticatedException.class } )
    public void notAuthenticatedExceptionHandler( NotAuthenticatedException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.unathorized( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { NotFoundException.class } )
    public void notFoundExceptionHandler( NotFoundException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.notFound( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( ConstraintViolationException.class )
    public void constraintViolationExceptionHandler( ConstraintViolationException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.error( getExceptionMessage( ex ) ), response, request );
    }

    @ExceptionHandler( MaintenanceModeException.class )
    public void maintenanceModeExceptionHandler( MaintenanceModeException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.serviceUnavailable( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( AccessDeniedException.class )
    public void accessDeniedExceptionHandler( AccessDeniedException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.forbidden( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( WebMessageException.class )
    public void webMessageExceptionHandler( WebMessageException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( ex.getWebMessage(), response, request );
    }

    @ExceptionHandler( HttpStatusCodeException.class )
    public void httpStatusCodeExceptionHandler( HttpStatusCodeException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, ex.getStatusCode() ), response, request );
    }

    @ExceptionHandler( HttpClientErrorException.class )
    public void httpClientErrorExceptionHandler( HttpClientErrorException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, ex.getStatusCode() ), response, request );
    }

    @ExceptionHandler( HttpServerErrorException.class )
    public void httpServerErrorExceptionHandler( HttpServerErrorException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, ex.getStatusCode() ), response, request );
    }

    @ExceptionHandler( HttpRequestMethodNotSupportedException.class )
    public void httpRequestMethodNotSupportedExceptionHandler( HttpRequestMethodNotSupportedException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, HttpStatus.METHOD_NOT_ALLOWED ), response, request );
    }

    @ExceptionHandler( HttpMediaTypeNotAcceptableException.class )
    public void httpMediaTypeNotAcceptableExceptionHandler( HttpMediaTypeNotAcceptableException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, HttpStatus.NOT_ACCEPTABLE ), response, request );
    }

    @ExceptionHandler( HttpMediaTypeNotSupportedException.class )
    public void httpMediaTypeNotSupportedExceptionHandler( HttpMediaTypeNotSupportedException ex, HttpServletRequest request, HttpServletResponse response )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), Status.ERROR, HttpStatus.UNSUPPORTED_MEDIA_TYPE ), response, request );
    }

    @ExceptionHandler( ServletException.class )
    public void servletExceptionHandler( ServletException ex ) throws ServletException
    {
        throw ex;
    }

    @ExceptionHandler( BadRequestException.class )
    public void handleBadRequest( BadRequestException badRequestException, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.badRequest( badRequestException.getMessage() ), response, request );
    }

    @ExceptionHandler( MetadataVersionException.class )
    public void handleMetaDataVersionException( MetadataVersionException metadataVersionException, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.error( metadataVersionException.getMessage() ), response, request );
    }

    @ExceptionHandler( MetadataSyncException.class )
    public void handleMetaDataSyncException( MetadataSyncException metadataSyncException, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.error( metadataSyncException.getMessage() ), response, request );
    }

    @ExceptionHandler( DhisVersionMismatchException.class )
    public void handleDhisVersionMismatchException( DhisVersionMismatchException versionMismatchException, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.forbidden( versionMismatchException.getMessage() ), response, request );
    }

    @ExceptionHandler( MetadataImportConflictException.class )
    public void handleMetadataImportConflictException( MetadataImportConflictException conflictException, HttpServletResponse response, HttpServletRequest request )
    {
        if ( conflictException.getMetadataSyncSummary() == null )
        {
            webMessageService.send( WebMessageUtils.conflict( conflictException.getMessage() ), response, request );
        }
        else
        {
            WebMessage message = new WebMessage( Status.ERROR, HttpStatus.CONFLICT );
            message.setResponse( conflictException.getMetadataSyncSummary() );
            webMessageService.send( message, response, request );
        }
    }

    @ExceptionHandler( OperationNotAllowedException.class )
    public void handleOperationNotAllowedException( OperationNotAllowedException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.forbidden( ex.getMessage() ), response, request );
    }

    /**
     * Catches default exception and send back to user, but re-throws internally so it still ends up in server logs.
     */
    @ExceptionHandler( Exception.class )
    public void defaultExceptionHandler( Exception ex, HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        webMessageService.send( WebMessageUtils.error( getExceptionMessage( ex ) ), response, request );
        ex.printStackTrace();
    }

    private String getExceptionMessage( Exception ex )
    {
        String message = ex.getMessage();

        if ( ex.getCause() != null )
        {
            message = ex.getCause().getMessage();
        }

        return message;
    }
}
