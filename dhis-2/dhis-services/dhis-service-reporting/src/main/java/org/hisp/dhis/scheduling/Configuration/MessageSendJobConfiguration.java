package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;

import java.util.Date;
import java.util.List;

import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MessageSendJobConfiguration extends JobConfiguration
{
    private OutboundSmsService outboundSmsService;

    public void setOutboundSmsService( OutboundSmsService outboundSmsService )
    {
        this.outboundSmsService = outboundSmsService;
    }

    private MessageSender smsSender;

    public void setSmsSender( MessageSender smsSender )
    {
        this.smsSender = smsSender;
    }

    private Notifier notifier;

    public void setNotifier( Notifier notifier )
    {
        this.notifier = notifier;
    }

    // -------------------------------------------------------------------------
    // Params
    // -------------------------------------------------------------------------

    private TaskId taskId;

    public void setTaskId( TaskId taskId )
    {
        this.taskId = taskId;
    }

    // -------------------------------------------------------------------------
    // Runnable implementation
    // -------------------------------------------------------------------------

    @Override
    public void run()
    {
        final int cpuCores = SystemUtils.getCpuCores();

        Clock clock = new Clock().startClock().logTime(
            "Aggregate process started, number of CPU cores: " + cpuCores + ", " + SystemUtils.getMemoryString() );

        clock.logTime( "Starting to send messages in outbound" );
        notifier.notify( taskId, INFO, "Start to send messages in outbound", true );

        sendMessages();

        clock.logTime( "Sending messages in outbound completed" );
        notifier.notify( taskId, INFO, "Sending messages in outbound completed", true );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void sendMessages()
    {
        List<OutboundSms> outboundSmsList = outboundSmsService.getOutboundSms( OutboundSmsStatus.OUTBOUND );

        if ( outboundSmsList != null )
        {
            for ( OutboundSms outboundSms : outboundSmsList )
            {
                outboundSms.setDate( new Date() );
                outboundSms.setStatus( OutboundSmsStatus.SENT );
                smsSender.sendMessage( null, outboundSms.getMessage(), outboundSms.getRecipients() );
            }
        }
    }
}
