package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

public class DefaultGatewayAdministrationService
    implements GatewayAdministrationService
{

    private Map<String, SmsGatewayConfig> gatewayConfigurationMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    // -------------------------------------------------------------------------
    // GatewayAdministrationService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<SmsGatewayConfig> listGateways()
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        return smsConfig != null ? smsConfig.getGateways() : Collections.emptyList();
    }

    @Override
    public String setDefaultGateway( String uid )
    {
        List<SmsGatewayConfig> list = listGateways();

        String gatewayName = "";

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
    public boolean addOrUpdateGateway( SmsGatewayConfig payLoad, Class<?> klass )
    {
        SmsConfiguration smsConfig = getSmsConfiguration();

        if ( smsConfig != null )
        {
            SmsGatewayConfig gatewayConfig = smsConfigurationManager.checkInstanceOfGateway( klass );

            int index = -1;

            if ( gatewayConfig != null )
            {
                index = smsConfig.getGateways().indexOf( gatewayConfig );
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

                gatewayConfigurationMap.put( gatewayConfig.getName(), gatewayConfig );
            }
            else
            {
                smsConfig.getGateways().add( gatewayConfig );

                gatewayConfigurationMap.put( gatewayConfig.getName(), gatewayConfig );
            }

            smsConfigurationManager.updateSmsConfiguration( smsConfig );

            return true;
        }

        return false;
    }

    @Override
    public boolean removeGatewayByUid( String uid )
    {
        List<SmsGatewayConfig> list = listGateways();

        for ( SmsGatewayConfig gateway : list )
        {
            if ( gateway.getUid().equals( uid ) )
            {
                SmsConfiguration smsConfiguration = getSmsConfiguration();

                smsConfiguration.getGateways().remove( gateway );

                gatewayConfigurationMap.remove( gateway.getName() );

                smsConfigurationManager.updateSmsConfiguration( smsConfiguration );

                return true;
            }
        }

        return false;
    }

    @Override
    public SmsGatewayConfig getGatewayConfigurationByUid( String uid )
    {
        List<SmsGatewayConfig> list = listGateways();

        if ( !list.isEmpty() )
        {
            for ( SmsGatewayConfig gw : list )
            {
                if ( gw.getUid().equals( uid ) )
                {
                    return gw;
                }
            }
        }

        return null;
    }

    @Override
    public SmsGatewayConfig getGatewayConfigurationByName( String gatewayName )
    {
        return getGatewayConfigurationByUid( getUidByGatewayName( gatewayName ) );
    }

    @Override
    public boolean removeGatewayByName( String gatewayName )
    {
        return removeGatewayByUid( getUidByGatewayName( gatewayName ) );
    }

    @Override
    public SmsGatewayConfig getDefaultGateway()
    {
        List<SmsGatewayConfig> list = listGateways();

        if (  !list.isEmpty() )
        {
            for ( SmsGatewayConfig gw : list )
            {
                if ( gw.isDefault() )
                {
                    return gw;
                }
            }
        }

        return null;
    }

    @Override
    public boolean loadGatewayConfigurationMap( SmsConfiguration smsConfiguration )
    {
        List<SmsGatewayConfig> gatewayList = smsConfiguration.getGateways();

        if ( gatewayList != null && !gatewayList.isEmpty() )
        {
            for ( SmsGatewayConfig smsGatewayConfig : gatewayList )
            {
                gatewayConfigurationMap.put( smsGatewayConfig.getName(), smsGatewayConfig );
            }

            return true;
        }

        return false;
    }

    @Override
    public Map<String, SmsGatewayConfig> getGatewayConfigurationMap()
    {
        return gatewayConfigurationMap;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getUidByGatewayName( String gatewayName )
    {
        List<SmsGatewayConfig> list = listGateways();

        for ( SmsGatewayConfig gw : list )
        {
            if ( gw.getName().equals( gatewayName ) )
            {
                return gw.getUid();
            }
        }

        return null;
    }

    private SmsConfiguration getSmsConfiguration()
    {
        return smsConfigurationManager.getSmsConfiguration();
    }
}
