/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.schema.descriptors.ApiTokenSchemaDescriptor;
import org.hisp.dhis.security.apikey.ApiToken;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.webdomain.WebOptions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping( value = ApiTokenSchemaDescriptor.API_ENDPOINT )
@RequiredArgsConstructor
@Slf4j
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class ApiTokenController extends AbstractCrudController<ApiToken>
{
    private final ApiTokenService apiTokenService;

    @Override
    public void partialUpdateObject( String pvUid, Map<String, String> rpParameters,
        HttpServletRequest request )
        throws Exception
    {
        throw new IllegalStateException( "Operation not supported on ApiKeys" );
    }

    @Override
    public void updateObjectProperty( String pvUid, String pvProperty, Map<String, String> rpParameters,
        HttpServletRequest request )
        throws Exception
    {
        throw new IllegalStateException( "Operation not supported on ApiKeys" );
    }

    protected void prePatchEntity( ApiToken oldToken, ApiToken newToken )
    {
        validateKeyEqual( newToken, oldToken );
        validateExpiry( newToken );
    }

    protected void prePatchEntity( ApiToken newToken )
    {
        validateKeyEqual( newToken, apiTokenService.getWithUid( newToken.getUid() ) );
        validateExpiry( newToken );
    }

    protected void preUpdateEntity( ApiToken existingToken, ApiToken newToken )
    {
        validateKeyEqual( newToken, existingToken );
        validateExpiry( newToken );
    }

    protected void preCreateEntity( ApiToken apiToken )
    {
        final String key = apiToken.getKey();
        if ( key != null )
        {
            throw new IllegalArgumentException( "ApiToken key can not be pre-specified." );
        }

        validateExpiry( apiToken );

        apiTokenService.initToken( apiToken );
    }

    private void validateKeyEqual( ApiToken token1, ApiToken token2 )
    {
        if ( !token2.getKey().equals( token1.getKey() ) )
        {
            throw new IllegalArgumentException( "ApiToken key can not be modified." );
        }
    }

    private void validateExpiry( ApiToken apiToken )
    {
        final Long expire = apiToken.getExpire();
        if ( expire != null && expire < System.currentTimeMillis() )
        {
            throw new IllegalArgumentException( "ApiToken expire must be greater than current time" );
        }
    }

    @Override
    protected List<ApiToken> getEntity( String uid, WebOptions options )
    {
        ArrayList<ApiToken> list = new ArrayList<>();
        java.util.Optional.ofNullable( manager.get( ApiToken.class, uid ) ).ifPresent( list::add );
        return list;
    }
}
