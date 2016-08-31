package org.hisp.dhis.sms.outbound;

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

import java.io.IOException;
import java.lang.Character.UnicodeBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.sms.SmsPublisher;
import org.hisp.dhis.sms.SmsServiceException;
import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.ClickatellGatewayConfig;
import org.hisp.dhis.sms.config.GateWayFactory;
import org.hisp.dhis.sms.config.GenericHttpGatewayConfig;
import org.hisp.dhis.sms.config.ModemGatewayConfig;
import org.hisp.dhis.sms.config.SMPPGatewayConfig;
import org.hisp.dhis.sms.config.SMSGatewayStatus;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsGatewayConfig;
import org.smslib.AGateway;
import org.smslib.AGateway.GatewayStatuses;
import org.smslib.GatewayException;
import org.smslib.IInboundMessageNotification;
import org.smslib.IOutboundMessageNotification;
import org.smslib.OutboundMessage;
import org.smslib.OutboundMessage.MessageStatuses;
import org.smslib.SMSLibException;
import org.smslib.Service;
import org.smslib.Message.MessageEncodings;
import org.smslib.Service.ServiceStatus;

public class DefaultOutboundSmsTransportService
    implements OutboundSmsTransportService
{
    private static final Log log = LogFactory.getLog( DefaultOutboundSmsTransportService.class );

    public static final Map<String, String> GATEWAY_MAP = new HashMap<>();

    private SmsConfiguration config;

    private String message = "success";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private IInboundMessageNotification smppInboundMessageNotification;

    public void setSmppInboundMessageNotification( IInboundMessageNotification smppInboundMessageNotification )
    {
        this.smppInboundMessageNotification = smppInboundMessageNotification;
    }

    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    private SmsPublisher smsPublisher;

    public void setSmsPublisher( SmsPublisher smsPublisher )
    {
        this.smsPublisher = smsPublisher;
    }

    // -------------------------------------------------------------------------
    // OutboundSmsTransportService implementation
    // -------------------------------------------------------------------------

    @Override
    public Map<String, String> getGatewayMap()
    {
        if ( GATEWAY_MAP == null || GATEWAY_MAP.isEmpty() )
        {
            reloadConfig();
        }

        return GATEWAY_MAP;
    }

    public void updateGatewayMap( String key )
    {
        GATEWAY_MAP.remove( key );
    }

    @Override
    public void stopService()
    {
        message = "success";

        try
        {
            getService().stopService();
            smsPublisher.stop();
        }
        catch ( SMSLibException e )
        {
            message = "Unable to stop smsLib service " + e.getCause().getMessage();
            log.warn( "Unable to stop smsLib service", e );
        }
        catch ( IOException e )
        {
            message = "Unable to stop smsLib service" + e.getCause().getMessage();
            log.warn( "Unable to stop smsLib service", e );
        }
        catch ( InterruptedException e )
        {
            message = "Unable to stop smsLib service" + e.getCause().getMessage();
            log.warn( "Unable to stop smsLib service", e );
        }
    }

    @Override
    public void startService()
    {
        message = "success";

        if ( config != null && config.isEnabled() && (config.getGateways() != null && !config.getGateways().isEmpty()) )
        {
            try
            {
                getService().startService();
                if ( GATEWAY_MAP.containsKey( SMPPGatewayConfig.class.getTypeName() ) )
                {
                    getService().setInboundMessageNotification( smppInboundMessageNotification );
                }

                try
                {
                    smsPublisher.start();
                }
                catch ( Exception e1 )
                {
                    message = "Unable to start smsConsumer service " + e1.getMessage();
                    log.warn( "Unable to start smsConsumer service ", e1 );
                }

            }
            catch ( SMSLibException e )
            {
                message = "Unable to start smsLib service " + e.getMessage();
                log.warn( "Unable to start smsLib service", e );
            }
            catch ( IOException e )
            {
                message = "Unable to start smsLib service" + e.getMessage();
                log.warn( "Unable to start smsLib service", e );
            }
            catch ( InterruptedException e )
            {
                message = "Unable to start smsLib service" + e.getMessage();
                log.warn( "Unable to start smsLib service", e );
            }
        }
        else
        {
            message = "sms_unable_or_there_is_no_gateway_service_not_started";
            log.debug( "Sms not enabled or there is no any gateway, won't start service" );
        }
    }

    @Override
    public void reloadConfig()
        throws SmsServiceException
    {
        Service service = Service.getInstance();

        service.setOutboundMessageNotification( new OutboundNotification() );

        service.getGateways().clear();

        AGateway gateway = null;

        message = "success";

        if ( config == null )
        {
            message = "unable_to_load_configure";
        }
        else if ( config.getGateways() == null || config.getGateways().isEmpty() )
        {
            message = "unable_load_configuration_cause_of_there_is_no_gateway";
        }
        else
        {
            GateWayFactory gatewayFactory = new GateWayFactory();

            for ( SmsGatewayConfig gatewayConfig : config.getGateways() )
            {
                try
                {
                    gateway = gatewayFactory.create( gatewayConfig );

                    service.addGateway( gateway );

                    if ( gatewayConfig instanceof BulkSmsGatewayConfig )
                    {
                        GATEWAY_MAP.put( BulkSmsGatewayConfig.class.getTypeName(), gateway.getGatewayId() );
                    }
                    else if ( gatewayConfig instanceof ClickatellGatewayConfig )
                    {
                        GATEWAY_MAP.put( ClickatellGatewayConfig.class.getTypeName(), gateway.getGatewayId() );
                    }
                    else if ( gatewayConfig instanceof GenericHttpGatewayConfig )
                    {
                        GATEWAY_MAP.put( GenericHttpGatewayConfig.class.getTypeName(), gateway.getGatewayId() );
                    }
                    else if ( gatewayConfig instanceof SMPPGatewayConfig )
                    {
                        GATEWAY_MAP.put( SMPPGatewayConfig.class.getTypeName(), gateway.getGatewayId() );
                    }
                    else
                    {
                        GATEWAY_MAP.put( ModemGatewayConfig.class.getTypeName(), gateway.getGatewayId() );
                    }

                    log.debug( "Added gateway " + gatewayConfig.getName() );
                }
                catch ( GatewayException e )
                {
                    log.warn( "Unable to load gateway " + gatewayConfig.getName(), e );
                    message = "Unable to load gateway " + gatewayConfig.getName() + e.getCause().getMessage();
                }
            }
        }

    }

    @Override
    public String getServiceStatus()
    {
        ServiceStatus serviceStatus = getService().getServiceStatus();

        if ( serviceStatus == ServiceStatus.STARTED )
        {
            return "service_started";
        }
        else if ( serviceStatus == ServiceStatus.STARTING )
        {
            return "service_starting";
        }
        else if ( serviceStatus == ServiceStatus.STOPPED )
        {
            return "service_stopped";
        }
        else
        {
            return "service_stopping";
        }
    }

    @Override
    public String getMessageStatus()
    {
        return message;
    }

    @Override
    public String getDefaultGateway()
    {
        if ( config == null )
        {
            return null;
        }

        SmsGatewayConfig gatewayConfig = config.getDefaultGateway();

        if ( gatewayConfig == null )
        {
            return null;
        }

        if ( getGatewayMap() == null )
        {
            return null;
        }

        String gatewayId = null;

        if ( gatewayConfig instanceof BulkSmsGatewayConfig )
        {
            gatewayId = GATEWAY_MAP.get( BulkSmsGatewayConfig.class.getTypeName() );
        }
        else if ( gatewayConfig instanceof ClickatellGatewayConfig )
        {
            gatewayId = GATEWAY_MAP.get( ClickatellGatewayConfig.class.getTypeName() );
        }
        else if ( gatewayConfig instanceof GenericHttpGatewayConfig )
        {
            gatewayId = GATEWAY_MAP.get( GenericHttpGatewayConfig.class.getTypeName() );
        }
        else if ( gatewayConfig instanceof SMPPGatewayConfig )
        {
            gatewayId = GATEWAY_MAP.get( SMPPGatewayConfig.class.getTypeName() );
        }
        else
        {
            gatewayId = GATEWAY_MAP.get( ModemGatewayConfig.class.getTypeName() );
        }

        return gatewayId;
    }

    @Override
    public boolean isEnabled()
    {
        return config != null && config.isEnabled();
    }

    @Override
    public String initialize( SmsConfiguration smsConfiguration )
        throws SmsServiceException
    {
        log.debug( "Initializing SmsLib" );

        this.config = smsConfiguration;

        ServiceStatus status = getService().getServiceStatus();

        if ( status == ServiceStatus.STARTED || status == ServiceStatus.STARTING )
        {
            log.debug( "Stopping SmsLib" );
            stopService();

            if ( message != null && !message.equals( "success" ) )
            {
                return message;
            }
        }

        log.debug( "Loading configuration" );
        reloadConfig();

        if ( message != null && !message.equals( "success" ) )
        {
            return message;
        }

        log.debug( "Starting SmsLib" );
        startService();

        if ( message != null && !message.equals( "success" ) )
        {
            return message;
        }

        return message;
    }

    @Override
    public String sendMessage( OutboundSms sms, String gatewayId )
        throws SmsServiceException
    {
        message = getServiceStatus();

        if ( message != null && (message.equals( "service_stopped" ) || message.equals( "service_stopping" )) )
        {
            return message = "service_stopped_cannot_send_sms";
        }

        String recipient = null;

        Set<String> recipients = sms.getRecipients();

        if ( recipients.size() == 0 )
        {
            log.warn( "Trying to send sms without recipients: " + sms );

            return message = "no_recipient";
        }
        else if ( recipients.size() == 1 )
        {
            recipient = recipients.iterator().next();
        }
        else
        {
            recipient = createTmpGroup( recipients );
        }

        OutboundMessage outboundMessage = new OutboundMessage( recipient, sms.getMessage() );

        // Check if text contain any specific unicode character

        for ( char each : sms.getMessage().toCharArray() )
        {
            if ( !Character.UnicodeBlock.of( each ).equals( UnicodeBlock.BASIC_LATIN ) )
            {
                outboundMessage.setEncoding( MessageEncodings.ENCUCS2 );
                break;
            }
        }

        outboundMessage.setStatusReport( true );

        String longNumber = config.getLongNumber();

        if ( longNumber != null && !longNumber.isEmpty() )
        {
            outboundMessage.setFrom( longNumber );
        }

        try
        {
            log.info( "Sending message " + sms );

            if ( gatewayId == null || gatewayId.isEmpty() )
            {
                getService().sendMessage( outboundMessage );
            }
            else
            {
                getService().sendMessage( outboundMessage, gatewayId );
            }
        }
        catch ( SMSLibException e )
        {
            log.warn( "Unable to send message: " + sms, e );
            message = "Unable to send message: " + sms + " " + e.getCause().getMessage();
        }
        catch ( IOException e )
        {
            log.warn( "Unable to send message: " + sms, e );
            message = "Unable to send message: " + sms + " " + e.getCause().getMessage();
        }
        catch ( InterruptedException e )
        {
            log.warn( "Unable to send message: " + sms, e );
            message = "Unable to send message: " + sms + " " + e.getCause().getMessage();
        }
        catch ( Exception e )
        {
            log.warn( "Unable to send message: " + sms, e );
            message = "Unable to send message: " + sms + " " + e.getCause().getMessage();
        }
        finally
        {
            if ( recipients.size() > 1 )
            {
                removeGroup( recipient );
            }
        }

        if ( outboundMessage.getMessageStatus() == MessageStatuses.SENT )
        {
            message = "success";
            sms.setStatus( OutboundSmsStatus.SENT );
        }
        else
        {
            log.error( "Message not sent Failure " + outboundMessage.getFailureCause().toString() );
            log.error( "Message not sent Status " + outboundMessage.getMessageStatus().toString() );
            message = "message_not_sent";
            sms.setStatus( OutboundSmsStatus.ERROR );
        }

        if ( sms.getId() == 0 )
        {
            outboundSmsService.saveOutboundSms( sms );
        }
        else
        {
            outboundSmsService.updateOutboundSms( sms );
        }

        return message;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Service getService()
    {
        return Service.getInstance();
    }

    private static class OutboundNotification
        implements IOutboundMessageNotification
    {
        @Override
        public void process( AGateway gateway, OutboundMessage msg )
        {
            log.debug( "Sent message through gateway " + gateway.getGatewayId() + ": " + msg );
        }
    }

    private String createTmpGroup( Set<String> recipients )
    {
        String groupName = Thread.currentThread().getName();

        getService().createGroup( groupName );

        for ( String recepient : recipients )
        {
            getService().addToGroup( groupName, recepient );
        }

        return groupName;
    }

    private void removeGroup( String groupName )
    {
        getService().removeGroup( groupName );
    }

    @Override
    public SMSServiceStatus getServiceStatusEnum()
    {
        ServiceStatus serviceStatus = getService().getServiceStatus();

        if ( serviceStatus == ServiceStatus.STARTED )
        {
            return SMSServiceStatus.STARTED;
        }
        else if ( serviceStatus == ServiceStatus.STARTING )
        {
            return SMSServiceStatus.STARTING;
        }
        else if ( serviceStatus == ServiceStatus.STOPPED )
        {
            return SMSServiceStatus.STOPPED;
        }
        else
        {
            return SMSServiceStatus.STOPPING;
        }
    }

    @Override
    public SMSGatewayStatus getGatewayStatus()
    {
        if ( getDefaultGateway() == null )
        {
            return SMSGatewayStatus.UNDEFINED;
        }

        AGateway aGateway = getService().getGateway( getDefaultGateway() );

        if ( aGateway.getStatus() == GatewayStatuses.STARTED )
        {
            return SMSGatewayStatus.STARTED;
        }

        if ( aGateway.getStatus() == GatewayStatuses.STOPPED )
        {
            return SMSGatewayStatus.STOPPED;
        }

        if ( aGateway.getStatus() == GatewayStatuses.STARTING )
        {
            return SMSGatewayStatus.STARTING;
        }

        if ( aGateway.getStatus() == GatewayStatuses.STOPPING )
        {
            return SMSGatewayStatus.STOPPING;
        }

        return SMSGatewayStatus.UNDEFINED;
    }
}
