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
package org.hisp.dhis.credentials;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Sends an alert to users whose user credentials are soon expiring so that they
 * have a chance to update their credentials.
 *
 * @author zubair (original)
 * @author Jan Bernitt (job progress tracking)
 */
@Slf4j
@Component
public class CredentialsExpiryAlertJob implements Job
{
    private static final String SUBJECT = "Password Expiry Alert";

    private static final String TEXT_EXPIRES_SOON = "Dear %s, Please change your password. It will expire in %d days.";

    private static final String TEXT_EXPIRED = "Dear %s, Please change your password. It expired %d days ago.";

    private static final String KEY_TASK = "credentialsExpiryAlertTask";

    private final UserService userService;

    private final MessageSender emailMessageSender;

    private final SystemSettingManager systemSettingManager;

    public CredentialsExpiryAlertJob( UserService userService,
        @Qualifier( "emailMessageSender" ) MessageSender emailMessageSender, SystemSettingManager systemSettingManager )
    {
        checkNotNull( userService );
        checkNotNull( emailMessageSender );
        checkNotNull( systemSettingManager );

        this.userService = userService;
        this.emailMessageSender = emailMessageSender;
        this.systemSettingManager = systemSettingManager;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.CREDENTIALS_EXPIRY_ALERT;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        if ( !systemSettingManager.getBoolSetting( SettingKey.CREDENTIALS_EXPIRY_ALERT ) )
        {
            log.info( format( "%s aborted. Expiry alerts are disabled", KEY_TASK ) );
            return;
        }

        progress.startingProcess( "User account expire alerts" );
        progress.startingStage( "finding and preparing email recipients" );
        List<User> users = progress.runStage( List.of(),
            recipients -> format( "%d recipients", recipients.size() ), userService::getExpiringUsers );
        sendExpiryAlert( users, progress );
        progress.completedProcess( null );
    }

    @Override
    public ErrorReport validate()
    {
        if ( !emailMessageSender.isConfigured() )
        {
            return new ErrorReport( CredentialsExpiryAlertJob.class, ErrorCode.E7010,
                "EMAIL gateway configuration does not exist" );
        }
        return Job.super.validate();
    }

    private void sendExpiryAlert( List<User> users, JobProgress progress )
    {
        progress.startingStage( "sending expiry alert emails", users.size(), SKIP_ITEM );
        if ( emailMessageSender.isConfigured() )
        {
            progress.runStage( users, this::createItemDescription, this::sendEmail );
        }
        else
        {
            progress.failedStage( "Email service is not configured" );
        }
    }

    private void sendEmail( User user )
    {
        OutboundMessageResponse response = emailMessageSender.sendMessage( SUBJECT,
            createEmailBodyText( user ), user.getEmail() );
        if ( response.getResponseObject() != EmailResponse.SENT )
        {
            throw new UncheckedIOException( response.getDescription(), new IOException() );
        }
    }

    private String createItemDescription( User user )
    {
        int remainingDays = getRemainingDays( user );
        return remainingDays < 0
            ? format( "to: %s, expired since %d days", user.getUsername(), abs( remainingDays ) )
            : format( "to: %s, %d days until expiry", user.getUsername(), remainingDays );
    }

    private String createEmailBodyText( User user )
    {
        int remainingDays = getRemainingDays( user );
        return remainingDays < 0
            ? format( TEXT_EXPIRED, user.getUsername(), abs( remainingDays ) )
            : format( TEXT_EXPIRES_SOON, user.getUsername(), remainingDays );
    }

    private int getRemainingDays( User user )
    {
        int daysBeforeChangeRequired = systemSettingManager.getIntSetting( SettingKey.CREDENTIALS_EXPIRES ) * 30;
        Date passwordLastUpdated = user.getPasswordLastUpdated();
        return (daysBeforeChangeRequired - DateUtils.daysBetween( passwordLastUpdated, new Date() ));
    }
}
