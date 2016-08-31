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

import org.hisp.dhis.sms.SmsServiceException;
import org.smslib.AGateway;
import org.smslib.AGateway.Protocols;
import org.smslib.http.BulkSmsHTTPGateway;
import org.smslib.http.BulkSmsHTTPGateway.Regions;
import org.smslib.http.ClickatellHTTPGateway;
import org.smslib.modem.SerialModemGateway;
import org.smslib.smpp.BindAttributes;
import org.smslib.smpp.BindAttributes.BindType;
import org.smslib.smpp.jsmpp.JSMPPGateway;

public class GateWayFactory
{
    public AGateway create( SmsGatewayConfig config )
    {
        if ( config instanceof BulkSmsGatewayConfig )
        {
            return createBulkSmsGateway( (BulkSmsGatewayConfig) config );
        }
        else if ( config instanceof GenericHttpGatewayConfig )
        {
            return createSimplisticHttpGetGateway( (GenericHttpGatewayConfig) config );
        }
        else if ( config instanceof ClickatellGatewayConfig )
        {
            return createClickatellGateway( (ClickatellGatewayConfig) config );
        }
        else if ( config instanceof ModemGatewayConfig )
        {
            return createModemGateway( (ModemGatewayConfig) config );
        }
        else if ( config instanceof SMPPGatewayConfig )
        {
            return createSMPPGatewayConfig( (SMPPGatewayConfig) config );
        }

        throw new SmsServiceException( "Gateway config of unknown type: " + config.getClass().getName() );
    }

    public AGateway createSMPPGatewayConfig( SMPPGatewayConfig config )
    {
        AGateway gateway = new JSMPPGateway( config.getName(), config.getAddress(), config.getPort(),
            new BindAttributes( config.getUsername(), config.getPassword(), "cp", BindType.TRANSCEIVER ) );
        gateway.setInbound( true );
        gateway.setOutbound( true );
        return gateway;
    }

    public AGateway createBulkSmsGateway( BulkSmsGatewayConfig config )
    {
        BulkSmsHTTPGateway gateway = new BulkSmsHTTPGateway( "bulksms.http.1", config.getUsername(),
            config.getPassword(), this.getRegion( config.getRegion() ) );
        gateway.setOutbound( true );
        gateway.setInbound( false );
        return gateway;
    }

    private Regions getRegion( String region )
    {
        if ( region.equals( "INTERNATIONAL" ) )
        {
            return BulkSmsHTTPGateway.Regions.INTERNATIONAL;
        }
        else if ( region.equals( "UNITEDKINGDOM" ) )
        {
            return BulkSmsHTTPGateway.Regions.UNITEDKINGDOM;
        }
        else if ( region.equals( "SOUTHAFRICA" ) )
        {
            return BulkSmsHTTPGateway.Regions.SOUTHAFRICA;
        }
        else if ( region.equals( "SPAIN" ) )
        {
            return BulkSmsHTTPGateway.Regions.SPAIN;
        }
        else if ( region.equals( "USA" ) )
        {
            return BulkSmsHTTPGateway.Regions.USA;
        }
        else
        {
            return BulkSmsHTTPGateway.Regions.GERMANY;
        }
    }

    public AGateway createModemGateway( ModemGatewayConfig c )
    {
        // TODO Detect modem class and instantiate
        SerialModemGateway gateway = new SerialModemGateway( c.getName(), c.getPort(), c.getBaudRate(),
            c.getManufacturer(), c.getModel() );

        if ( c.getSimMemLocation() != null )
        {
            gateway.getATHandler().setStorageLocations( c.getSimMemLocation() );
        }

        if ( c.getPin() != null )
        {
            gateway.setSimPin( c.getPin() );
        }

        gateway.setProtocol( Protocols.PDU );
        gateway.setInbound( c.isInbound() );
        gateway.setOutbound( c.isOutbound() );

        return gateway;
    }

    public AGateway createClickatellGateway( ClickatellGatewayConfig c )
    {
        ClickatellHTTPGateway gateway = new ClickatellHTTPGateway( c.getName(), c.getApiId(), c.getUsername(),
            c.getPassword() );
        gateway.setOutbound( true );
        gateway.setInbound( false );
        return gateway;
    }

    public AGateway createSimplisticHttpGetGateway( GenericHttpGatewayConfig c )
    {
        SimplisticHttpGetGateWay gateway = new SimplisticHttpGetGateWay( c.getName(), c.getUrlTemplate(),
            c.getParameters() );
        gateway.setOutbound( true );
        gateway.setInbound( false );
        return gateway;
    }
}
