package org.hisp.dhis.sms.config;

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

import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.sms.outbound.OutboundSmsTransportService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class DefaultGatewayAdministrationService
    implements GatewayAdministratonService
{
    @Autowired
    private SmsConfigurationManager smsConfigMgr;

    @Autowired
    private OutboundSmsTransportService transportService;

    // -------------------------------------------------------------------------
    // GatewayAdministratonService implementation
    // -------------------------------------------------------------------------

    @Override
    public SmsConfiguration listGateways()
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        return smsConfig == null ? null : smsConfig;
    }

    @Override
    public String setDefault( String uid )
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        List<SmsGatewayConfig> list = smsConfig.getGateways();

        String gatewayName = null;

        if ( !checkGateway( list, uid ) )
        {
            return null;
        }
        
        for ( SmsGatewayConfig gateway : list )
        {
            if ( gateway.getUid().equals( uid ) )
            {
                gateway.setDefault( true );

                gatewayName = gateway.getName();
            }
            else
            {
                gateway.setDefault( false );
            }
        }

        return gatewayName;
    }

    @Override
    public String addOrUpdateGateway( SmsGatewayConfig payLoad, Class<?> klass )
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        boolean updated = false;

        if ( smsConfig != null )
        {
            SmsGatewayConfig gatewayConfig = smsConfigMgr.checkInstanceOfGateway( klass );

            int index = -1;

            if ( gatewayConfig != null )
            {
                index = smsConfig.getGateways().indexOf( gatewayConfig );

                updated = true;
            }

            payLoad.setUid( CodeGenerator.generateCode( 10 ) );
            gatewayConfig = payLoad;

            if ( smsConfig.getGateways() == null || smsConfig.getGateways().isEmpty() )
            {
                gatewayConfig.setDefault( true );
            }

            if ( index >= 0 )
            {
                smsConfig.getGateways().set( index, gatewayConfig );
            }
            else
            {
                smsConfig.getGateways().add( gatewayConfig );
            }

            smsConfigMgr.updateSmsConfiguration( smsConfig );

            return updated ? "Gateway updated successfully" : "Gateway added successfully";
        }

        return "No sms configuration found";
    }

    @Override
    public boolean removeGateway( String uid )
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        List<SmsGatewayConfig> list = smsConfig.getGateways();

        for ( SmsGatewayConfig gateway : list )
        {
            if ( gateway.getUid().equals( uid ) )
            {
                smsConfig.getGateways().remove( gateway );

                transportService.updateGatewayMap( gateway.getClass().getTypeName() );

                smsConfigMgr.updateSmsConfiguration( smsConfig );

                return true;
            }
        }

        return false;
    }

    @Override
    public SmsGatewayConfig getGatewayConfiguration( String uid )
    {
        SmsConfiguration smsConfig = getSmsConfiguration();
        
        List<SmsGatewayConfig> list = smsConfig.getGateways();
        
        for ( SmsGatewayConfig gw : list )
        {
            if ( gw.getUid().equals( uid ) )
            {
                return gw;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean checkGateway( List<SmsGatewayConfig> list, String uid )
    {
        for ( SmsGatewayConfig gateway : list )
        {
            if ( gateway.getUid().equals( uid ) )
            {
                return true;
            }
        }

        return false;
    }

    private SmsConfiguration getSmsConfiguration()
    {
        return smsConfigMgr.getSmsConfiguration();
    }
}
