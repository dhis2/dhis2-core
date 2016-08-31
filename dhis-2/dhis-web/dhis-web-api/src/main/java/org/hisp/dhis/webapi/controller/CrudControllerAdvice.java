package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalException;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageStatus;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.webapi.controller.exception.NotAuthenticatedException;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
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
        webMessageService.send( WebMessageUtils.unprocessableEntity( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( { IllegalQueryException.class, DeleteNotAllowedException.class, InvalidIdentifierReferenceException.class } )
    public void conflictsExceptionHandler( Exception ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( MaintenanceModeException.class )
    public void maintenanceModeExceptionHandler( MaintenanceModeException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.serviceUnavailable( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( DataApprovalException.class )
    public void dataApprovalExceptionHandler( DataApprovalException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request ); //TODO fix message
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

    @ExceptionHandler( JsonParseException.class )
    public void jsonParseExceptionHandler( JsonParseException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.conflict( ex.getMessage() ), response, request );
    }

    @ExceptionHandler( HttpStatusCodeException.class )
    public void httpStatusCodeExceptionHandler( HttpStatusCodeException ex, HttpServletResponse response, HttpServletRequest request )
    {
        webMessageService.send( WebMessageUtils.createWebMessage( ex.getMessage(), WebMessageStatus.ERROR, ex.getStatusCode() ), response, request );
    }
}
