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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

@Controller
@RequestMapping( value = SmsConfigurationController.RESOURCE_PATH )
public class SmsConfigurationController
{
    public static final String RESOURCE_PATH = "/config/sms";

    private static final Log log = LogFactory.getLog( SmsConfigurationController.class );

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    @RequestMapping( method = RequestMethod.GET )
    public String getSmsConfiguration( Model model )
    {
        SmsConfiguration smsConfiguration = smsConfigurationManager.getSmsConfiguration();

        if ( smsConfiguration == null )
        {
            smsConfiguration = new SmsConfiguration();
        }

        model.addAttribute( "model", smsConfiguration );

        return "smsConfiguration";
    }

    @RequestMapping( value = "test", method = RequestMethod.GET )
    public String getTest( Model model )
    {

        SmsConfiguration smsConfiguration = new SmsConfiguration();

        SmsGatewayConfig gatewayConfig = new GenericHttpGatewayConfig( "http://storset.org/", new HashMap<>() );
        smsConfiguration.setGateways( Collections.singletonList( gatewayConfig ) );

        model.addAttribute( "model", smsConfiguration );
        model.addAttribute( "viewClass", "detailed" );

        return "smsConfiguration";
    }


    //--------------------------------------------------------------------------
    // POST
    //--------------------------------------------------------------------------

    @RequestMapping( method = RequestMethod.PUT )
    public String putSmsConfig( @RequestBody SmsConfiguration smsConfiguration, Model model ) throws Exception
    {
        if ( smsConfiguration == null )
        {
            throw new IllegalArgumentException( "SMS configuration not set" );
        }

        smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
        return getSmsConfiguration( model );
    }


    @ExceptionHandler
    public void mapException( IllegalArgumentException exception, HttpServletResponse response ) throws IOException
    {
        log.info( "Exception", exception );
        response.setStatus( HttpServletResponse.SC_CONFLICT );
        response.setContentType( ContextUtils.CONTENT_TYPE_TEXT );
        response.getWriter().write( exception.getMessage() );
    }
}
