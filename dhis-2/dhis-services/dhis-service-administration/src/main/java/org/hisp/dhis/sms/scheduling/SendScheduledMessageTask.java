package org.hisp.dhis.sms.scheduling;

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

import static org.hisp.dhis.sms.outbound.OutboundSms.DHIS_SYSTEM_SENDER;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.SchedulingProgramObject;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.SmsServiceException;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.SystemUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.hisp.dhis.scheduling.TaskId;

/**
 * @author Chau Thu Tran
 * 
 * @version SendScheduledMessageTask.java 12:57:53 PM Sep 10, 2012 $
 */

public class SendScheduledMessageTask
    implements Runnable
{
    private ProgramStageInstanceService programStageInstanceService;

    public void setProgramStageInstanceService( ProgramStageInstanceService programStageInstanceService )
    {
        this.programStageInstanceService = programStageInstanceService;
    }

    private ProgramInstanceService programInstanceService;

    public void setProgramInstanceService( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

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

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate( JdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    private Notifier notifier;

    public void setNotifier( Notifier notifier )
    {
        this.notifier = notifier;
    }

    // -------------------------------------------------------------------------
    // Params
    // -------------------------------------------------------------------------

    private Boolean sendingMessage;

    public void setSendingMessage( Boolean sendingMessage )
    {
        this.sendingMessage = sendingMessage;
    }

    private Boolean sendNow;

    public void setSendNow( Boolean sendNow )
    {
        this.sendNow = sendNow;
    }

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

        if ( sendNow )
        {
            clock.logTime( "Starting to prepare reminder messages" );
            notifier.clear( taskId ).notify( taskId, "Start to prepare reminder messages" );

            scheduleProgramStageInstanceMessage();
            scheduleProgramInstanceMessage();

            clock.logTime( "Preparing reminder messages completed" );
            notifier.notify( taskId, INFO, "Preparing reminder messages completed", true );

            clock.logTime( "Starting to send messages in outbound" );
            notifier.notify( taskId, INFO, "Start to send messages in outbound", true );

            sendMessage();

            clock.logTime( "Sending messages in outbound completed" );
            notifier.notify( taskId, INFO, "Sending messages in outbound completed", true );

            return;
        }

        if ( sendingMessage )
        {
            clock.logTime( "Starting to send messages in outbound" );
            notifier.notify( taskId, INFO, "Start to send messages in outbound", true );

            sendMessage();

            clock.logTime( "Sending messages in outbound completed" );
            notifier.notify( taskId, INFO, "Sending messages in outbound completed", true );
        }
        else
        {
            clock.logTime( "Starting to prepare reminder messages" );
            notifier.clear( taskId ).notify( taskId, "Start to prepare reminder messages" );

            scheduleProgramStageInstanceMessage();
            scheduleProgramInstanceMessage();

            clock.logTime( "Preparing reminder messages completed" );
            notifier.notify( taskId, INFO, "Preparing reminder messages completed", true );
        }

    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void scheduleProgramStageInstanceMessage()
    {
        notifier.notify( taskId, "Start to prepare reminder messages for events" );

        Collection<SchedulingProgramObject> schedulingProgramObjects = programStageInstanceService
            .getSendMessageEvents();

        for ( SchedulingProgramObject schedulingProgramObject : schedulingProgramObjects )
        {
            String message = schedulingProgramObject.getMessage();

            try
            {
                OutboundSms outboundSms = new OutboundSms( message, schedulingProgramObject.getPhoneNumber() );
                outboundSms.setSender( DHIS_SYSTEM_SENDER );
                outboundSmsService.saveOutboundSms( outboundSms );

                String sortOrderSql = "SELECT max(sort_order) "
                    + "FROM programstageinstance_outboundsms where programstageinstanceid="
                    + schedulingProgramObject.getProgramStageInstanceId();
                Integer sortOrder = jdbcTemplate.queryForObject( sortOrderSql, Integer.class );
                if ( sortOrder == null )
                {
                    sortOrder = 0;
                }
                sortOrder = sortOrder + 1;

                String sql = "INSERT INTO programstageinstance_outboundsms"
                    + "( programstageinstanceid, outboundsmsid, sort_order) VALUES " + "("
                    + schedulingProgramObject.getProgramStageInstanceId() + ", " + outboundSms.getId() + "," + sortOrder
                    + ") ";

                jdbcTemplate.execute( sql );

                notifier.notify( taskId,
                    "Reminder messages for event of " + outboundSms.getRecipients() + " is created " );
            }
            catch ( SmsServiceException e )
            {
                message = e.getMessage();
            }
        }

        notifier.notify( taskId, INFO, "Preparing reminder messages for events completed", true );
    }

    private void scheduleProgramInstanceMessage()
    {
        notifier.notify( taskId, "Start to prepare remigetScheduledTasksnder messages for enrollements" );

        Collection<SchedulingProgramObject> schedulingProgramObjects = programInstanceService.getScheduledMessages();

        for ( SchedulingProgramObject schedulingProgramObject : schedulingProgramObjects )
        {
            String message = schedulingProgramObject.getMessage();
            try
            {
                OutboundSms outboundSms = new OutboundSms( message, schedulingProgramObject.getPhoneNumber() );
                outboundSms.setSender( DHIS_SYSTEM_SENDER );
                outboundSmsService.saveOutboundSms( outboundSms );

                String sortOrderSql = "select max(sort_order) "
                    + "from programinstance_outboundsms where programinstanceid="
                    + schedulingProgramObject.getProgramInstanceId();
                Integer sortOrder = jdbcTemplate.queryForObject( sortOrderSql, Integer.class );
                if ( sortOrder == null )
                {
                    sortOrder = 0;
                }
                sortOrder = sortOrder + 1;

                String sql = "INSERT INTO programinstance_outboundsms"
                    + "( programinstanceid, outboundsmsid, sort_order) VALUES " + "("
                    + schedulingProgramObject.getProgramInstanceId() + ", " + outboundSms.getId() + "," + sortOrder
                    + ") ";

                jdbcTemplate.execute( sql );

                notifier.notify( taskId,
                    "Reminder messages for enrollement of " + outboundSms.getRecipients() + " is created " );
            }
            catch ( SmsServiceException e )
            {
                message = e.getMessage();
            }
        }

        notifier.notify( taskId, INFO, "Sending reminder messages for enrollement completed", true );

    }

    private void sendMessage()
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
