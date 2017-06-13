package org.hisp.dhis.mobile.action;

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

import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

import org.hisp.dhis.common.CodeGenerator;

/**
 * @author Dang Duy Hieu
 */
public class UpdateClickatellGateWayConfigAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    @Autowired
    private GatewayAdministrationService gatewayAdminService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String password;

    public void setPassword( String password )
    {
        this.password = password;
    }

    private String username;

    public void setUsername( String username )
    {
        this.username = username;
    }
    
    private String authToken;

    public void setAuthToken( String authToken )
    {
        this.authToken = authToken;
    }

    private String gatewayType;

    public void setGatewayType( String gatewayType )
    {
        this.gatewayType = gatewayType;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( gatewayType != null && gatewayType.equals( "clickatell" ) )
        {
            SmsConfiguration config = smsConfigurationManager.getSmsConfiguration();

            if ( config != null )
            {
                ClickatellGatewayConfig gatewayConfig = (ClickatellGatewayConfig) smsConfigurationManager
                    .checkInstanceOfGateway( ClickatellGatewayConfig.class );

                int index = -1;

                if ( gatewayConfig == null )
                {
                    gatewayConfig = new ClickatellGatewayConfig();
                }
                else
                {
                    index = config.getGateways().indexOf( gatewayConfig );
                }

                gatewayConfig.setName( name );
                gatewayConfig.setPassword( password );
                gatewayConfig.setUsername( username );
                gatewayConfig.setUid( CodeGenerator.generateCode( 10 ) );
                gatewayConfig.setAuthToken( authToken );

                if ( config.getGateways() == null || config.getGateways().isEmpty() )
                {
                    gatewayConfig.setDefault( true );
                }

                if ( index >= 0 )
                {
                    config.getGateways().set( index, gatewayConfig );
                }
                else
                {
                    config.getGateways().add( gatewayConfig );
                }

                gatewayAdminService.getGatewayConfigurationMap().put( name, gatewayConfig );

                smsConfigurationManager.updateSmsConfiguration( config );
            }
        }

        return SUCCESS;
    }
}
