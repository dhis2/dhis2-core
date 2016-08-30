package org.hisp.dhis.mobile.action;

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

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.sms.config.SmsGatewayConfig;

import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 * @version $Id$
 */

public class GetSmsConfigurationAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String index;

    public String getIndex()
    {
        return index;
    }

    public void setIndex( String index )
    {
        this.index = index;
    }

    private Map<Integer, SmsGatewayConfig> gatewayConfigMap = new HashMap<>();

    public Map<Integer, SmsGatewayConfig> getGatewayConfigMap()
    {
        return gatewayConfigMap;
    }

    private SmsConfiguration smsConfig;

    public SmsConfiguration getSmsConfig()
    {
        return smsConfig;
    }

    public Integer bulkIndex;

    public Integer getBulkIndex()
    {
        return bulkIndex;
    }

    public Integer clickatellIndex;

    public Integer getClickatellIndex()
    {
        return clickatellIndex;
    }

    public Integer httpIndex;

    public Integer getHttpIndex()
    {
        return httpIndex;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {

        smsConfig = smsConfigurationManager.getSmsConfiguration();

        if ( smsConfig != null )
        {
            int index = 0;

            for ( SmsGatewayConfig gw : smsConfig.getGateways() )
            {
                index = smsConfig.getGateways().indexOf( gw );

                gatewayConfigMap.put( index, gw );

                if ( gw instanceof BulkSmsGatewayConfig )
                {
                    bulkIndex = index;
                }
                else if ( gw instanceof ClickatellGatewayConfig )
                {
                    clickatellIndex = index;
                }
                else
                {
                    httpIndex = index;
                }
            }
        }
        else
        {
            smsConfig = new SmsConfiguration( true );

            smsConfigurationManager.updateSmsConfiguration( smsConfig );
        }

        return SUCCESS;
    }
}
