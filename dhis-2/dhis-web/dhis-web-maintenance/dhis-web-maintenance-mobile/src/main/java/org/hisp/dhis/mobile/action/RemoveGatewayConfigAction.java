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

import java.util.Iterator;

import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.ModemGatewayConfig;
import org.hisp.dhis.sms.config.SMPPGatewayConfig;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.hisp.dhis.sms.outbound.OutboundSmsTransportService;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.Action;

/**
 * @author Dang Duy Hieu
 */
public class RemoveGatewayConfigAction
    implements Action
{
    private final String BULK_GATEWAY = "bulk_gw";

    private final String CLICKATELL_GATEWAY = "clickatell_gw";

    private final String HTTP_GATEWAY = "generic_http_gw";

    private final String MODEM_GATEWAY = "modem_gw";

    private final String SMPP_GATEWAY = "smpp_gw";
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SmsConfigurationManager smsConfigurationManager;
    
    @Autowired
    private OutboundSmsTransportService transportService;

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        SmsConfiguration smsConfig = smsConfigurationManager.getSmsConfiguration();

        Iterator<SmsGatewayConfig> it = smsConfig.getGateways().iterator();

        while ( it.hasNext() )
        {
            if ( smsConfig.getGateways().indexOf( it.next() ) == id )
            {
                SmsGatewayConfig gatewayConfig = smsConfig.getGateways().get( id );
                
                it.remove();

                smsConfigurationManager.updateSmsConfiguration( smsConfig );
                
                if ( gatewayConfig instanceof BulkSmsGatewayConfig )
                {
                    transportService.getGatewayMap().remove( BULK_GATEWAY);
                }
                
                if ( gatewayConfig instanceof ClickatellGatewayConfig )
                {
                    transportService.getGatewayMap().remove( CLICKATELL_GATEWAY );
                }
                
                if ( gatewayConfig instanceof ModemGatewayConfig )
                {
                    transportService.getGatewayMap().remove( MODEM_GATEWAY );
                }
                
                if ( gatewayConfig instanceof GenericHttpGatewayConfig)
                {
                    transportService.getGatewayMap().remove( HTTP_GATEWAY );
                }
                
                if ( gatewayConfig instanceof SMPPGatewayConfig )
                {
                    transportService.getGatewayMap().remove( SMPP_GATEWAY );
                }
                
                break;
            }
        }
        return SUCCESS;
    }
}
