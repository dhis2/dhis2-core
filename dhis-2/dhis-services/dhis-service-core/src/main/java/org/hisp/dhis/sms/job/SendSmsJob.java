/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.job;

import static java.lang.String.format;

import java.util.HashSet;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.SmsJobParameters;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SendSmsJob implements Job
{
    @Qualifier( "smsMessageSender" )
    private final MessageSender smsSender;

    private final OutboundSmsService outboundSmsService;

    @Override
    public JobType getJobType()
    {
        return JobType.SMS_SEND;
    }

    @Override
    public void execute( JobConfiguration config, JobProgress progress )
    {
        SmsJobParameters params = (SmsJobParameters) config.getJobParameters();
        OutboundSms sms = new OutboundSms();
        sms.setSubject( params.getSmsSubject() );
        sms.setMessage( params.getMessage() );
        sms.setRecipients( new HashSet<>( params.getRecipientsList() ) );

        progress.startingProcess( "Send SMS" );

        progress.startingStage( format( "Sending SMS to %d recipients", sms.getRecipients().size() ) );
        OutboundMessageResponse status = progress.runStage( (OutboundMessageResponse) null,
            () -> smsSender.sendMessage( sms.getSubject(), sms.getMessage(), sms.getRecipients() ) );

        sms.setStatus( status != null && status.isOk() ? OutboundSmsStatus.SENT : OutboundSmsStatus.FAILED );
        progress.startingStage( format( "Persisting outcome as %s", sms.getStatus().name() ) );
        progress.runStage( () -> outboundSmsService.save( sms ) );

        progress.completedProcess( null );
    }

}
