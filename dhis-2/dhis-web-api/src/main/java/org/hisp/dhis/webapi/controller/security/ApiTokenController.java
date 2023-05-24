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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.objectReport;
import static org.hisp.dhis.security.apikey.ApiKeyTokenGenerator.generatePersonalAccessToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.security.apikey.ApiKeyTokenGenerator;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Tags( { "user", "login" } )
@Controller
@RequestMapping( value = { ApiTokenSchemaDescriptor.API_ENDPOINT_OLD, ApiTokenSchemaDescriptor.API_ENDPOINT_NEW } )
@RequiredArgsConstructor
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ApiTokenController extends AbstractCrudController<ApiToken>
{
    public static final String METHOD_TYPE_IS_NOT_SUPPORTED_MSG = "Method type is not supported";

    private static final List<String> VALID_METHODS = List.of( "GET", "POST", "PATCH", "PUT", "DELETE" );

    public static final long DEFAULT_TOKEN_EXPIRE = TimeUnit.DAYS.toMillis( 30 );

    @Override
    @PostMapping( consumes = "application/json" )
    @ResponseBody
    public WebMessage postJsonObject( HttpServletRequest request )
        throws ForbiddenException,
        IOException,
        ConflictException
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( !aclService.canCreate( currentUser, getEntityClass() ) )
        {
            throw new ForbiddenException( "You don't have the proper permissions to create this object." );
        }

        ApiToken inputToken = deserializeJsonEntity( request );

        try
        {
            validateTokenAttributes( inputToken );
        }
        catch ( Exception e )
        {
            throw new ConflictException( "Failed to validate the token's attributes, message: " + e.getMessage() );
        }

        ApiKeyTokenGenerator.TokenWrapper apiTokenPair = generatePersonalAccessToken( inputToken.getAttributes(),
            inputToken.getExpire() );

        MetadataImportParams params = importService.getParamsFromMap( contextService.getParameterValuesMap() )
            .setImportReportMode( ImportReportMode.FULL )
            .setUser( currentUser )
            .setImportStrategy( ImportStrategy.CREATE )
            .addObject( apiTokenPair.getApiToken() );

        ObjectReport report = importService.importMetadata( params ).getFirstObjectReport();
        WebMessage webMessage = objectReport( report );

        if ( webMessage.getStatus() == Status.OK )
        {
            String uid = report.getUid();
            webMessage.setHttpStatus( HttpStatus.CREATED );
            webMessage.setLocation( getSchema().getRelativeApiEndpoint() + "/" + uid );
            webMessage.setResponse( new ApiTokenCreationResponse( report, apiTokenPair.getPlaintextToken() ) );
            Arrays.fill( apiTokenPair.getPlaintextToken(), '0' );
        }
        else
        {
            webMessage.setStatus( Status.ERROR );
        }

        return webMessage;
    }

    @Override
    @PostMapping( consumes = { "application/xml", "text/xml" } )
    @ResponseBody
    public WebMessage postXmlObject( HttpServletRequest request )
        throws HttpRequestMethodNotSupportedException
    {
        throw new HttpRequestMethodNotSupportedException( METHOD_TYPE_IS_NOT_SUPPORTED_MSG );
    }

    private void validateTokenAttributes( ApiToken token )
    {
        if ( token.getExpire() == null )
        {
            token.setExpire( System.currentTimeMillis() + DEFAULT_TOKEN_EXPIRE );
        }
        if ( token.getIpAllowedList() != null )
        {
            token.getIpAllowedList().getAllowedIps().forEach( this::validateIp );
        }
        if ( token.getMethodAllowedList() != null )
        {
            token.getMethodAllowedList().getAllowedMethods().forEach( this::validateHttpMethod );
        }
        if ( token.getRefererAllowedList() != null )
        {
            token.getRefererAllowedList().getAllowedReferrers().forEach( this::validateReferrer );
        }
    }

    private void validateHttpMethod( String httpMethodName )
    {
        if ( !VALID_METHODS.contains( httpMethodName.toUpperCase( Locale.ROOT ) ) )
        {
            throw new IllegalArgumentException( "Not a valid http method, value=" + httpMethodName );
        }
    }

    private void validateIp( String ip )
    {
        InetAddressValidator validator = new InetAddressValidator();
        if ( !validator.isValid( ip ) )
        {
            throw new IllegalArgumentException( "Not a valid ip address, value=" + ip );
        }
    }

    private void validateReferrer( String referrer )
    {
        UrlValidator urlValidator = new UrlValidator();
        if ( !urlValidator.isValid( referrer ) )
        {
            throw new IllegalArgumentException( "Not a valid referrer url, value=" + referrer );
        }
    }
}
