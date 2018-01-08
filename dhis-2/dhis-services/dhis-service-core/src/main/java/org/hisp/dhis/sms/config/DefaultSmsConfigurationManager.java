package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Manages the {@link SmsConfiguration} for the instance.
 */
public class DefaultSmsConfigurationManager
    implements SmsConfigurationManager
{
    @Autowired
    private SystemSettingManager systemSettingManager;

    @Override
    public SmsConfiguration getSmsConfiguration()
    {
        return (SmsConfiguration) systemSettingManager.getSystemSetting( SettingKey.SMS_CONFIG );
    }

    @Override
    public void updateSmsConfiguration( SmsConfiguration config )
    {
        systemSettingManager.saveSystemSetting( SettingKey.SMS_CONFIG, config );
    }

    @Override
    public SmsGatewayConfig checkInstanceOfGateway( Class<?> clazz )
    {
        if ( getSmsConfiguration() == null )
        {
            SmsConfiguration smsConfig = new SmsConfiguration( true );
            updateSmsConfiguration( smsConfig );
        }

        for ( SmsGatewayConfig gateway : getSmsConfiguration().getGateways() )
        {
            if ( gateway.getClass().equals( clazz ) )
            {
                return gateway;
            }
        }

        return null;
    }

    @Override
    public boolean setDefaultSMSGateway( String gatewayId )
    {
        boolean result = false;

        SmsConfiguration config = getSmsConfiguration();

        if ( config == null )
        {
            return result;
        }

        List<SmsGatewayConfig> smsGatewayList = config.getGateways();

        for ( SmsGatewayConfig gw : smsGatewayList )
        {
            if ( gw.getName().equals( gatewayId ) )
            {
                gw.setDefault( true );
                
                result = true;
            }
            else
            {
                gw.setDefault( false );
            }
        }

        updateSmsConfiguration( config );

        return result;
    }

    @Override
    public boolean gatewayExists( String gatewayId )
    {
        SmsConfiguration config = getSmsConfiguration();
        List<SmsGatewayConfig> gatewayList = config.getGateways();

        for ( SmsGatewayConfig gw : gatewayList )
        {
            if ( gw.getName().equals( gatewayId ) )
            {
                return true;
            }
        }

        return false;
    }
}
