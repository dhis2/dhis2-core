package org.hisp.dhis.webapi.controller;
/*
 * Copyright (c) 2016, IntraHealth International
 * All rights reserved.
 * Apache 2.0
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

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.scriptlibrary.ExecutionContextHttpInterface;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.DefaultServerSideAppEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
/**
 * @author Carl Leitner <litlfred@gmail.com>
 */
abstract public class ServerSideAppEngineController
{
    protected static final Log log = LogFactory.getLog( DefaultDataValueService.class );

    @Autowired
    protected ApplicationContext applicationContext;
    @Autowired
    protected ContextService contextService;
    @Autowired
    protected CurrentUserService currentUserService;
    @Autowired
    //@Qualifier ( "org.hisp.dhis.webapi.service.DefaultServerSideAppEngineService" ) //for some reason we are also getting "defaultEngineBuilder" as a bean
    protected DefaultServerSideAppEngineService engineService;


    protected void sendError( HttpServletResponse httpResponse, int sc, String msg )
    {
        try
        {
            httpResponse.sendError( sc, msg );
        }

        catch ( Exception e )
        {
            log.info( "Could not send error:" + e.toString() );
        }

    }


    protected void initExecutionContext( ExecutionContextHttpInterface execContext, String appKey, HttpServletRequest httpRequest, HttpServletResponse httpResponse )
    {
        try
        {
            log.info( "Setting script context appKey:" + appKey );
            execContext.setAppKey( appKey );
            execContext.setUser( currentUserService.getCurrentUser() );

            log.info( "Setting application context" );
            execContext.setApplicationContext( applicationContext );

            log.info( "Setting http stream context" );
            execContext.setOut( new OutputStreamWriter( httpResponse.getOutputStream() ) );
            execContext.setIn( new InputStreamReader( httpRequest.getInputStream() ) );

            log.info( "Setting http request/response context" );
            execContext.setHttpServletRequest( httpRequest );
            execContext.setHttpServletResponse( httpResponse );


        }

        catch ( Exception e )
        {
            log.info( "Could not initialize execution context:" + e.toString() );
        }
    }


}