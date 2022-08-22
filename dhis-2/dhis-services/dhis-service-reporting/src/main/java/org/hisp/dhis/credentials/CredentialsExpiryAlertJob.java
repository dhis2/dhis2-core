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

import static java.lang.String.format;
import static java.time.ZoneId.systemDefault;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Sends an alert to users whose user credentials are soon expiring so that they
 * have a chance to update their credentials.
 *
 * @author zubair (original)
 * @author Jan Bernitt (job progress tracking, sending on specific days only)
 */
@Slf4j
@Component
@AllArgsConstructor
public class CredentialsExpiryAlertJob implements Job
{
    private final UserService userService;

    private final EmailService emailService;

    private final LocaleManager localeManager;

    private final I18nManager i18nManager;

    private final SystemSettingManager systemSettingManager;

    @Override
    public JobType getJobType()
    {
        return JobType.CREDENTIALS_EXPIRY_ALERT;
    }

    @Override
    public ErrorReport validate()
    {
        if ( !emailService.emailConfigured() )
        {
            return new ErrorReport( CredentialsExpiryAlertJob.class, ErrorCode.E7010,
                "EMAIL gateway configuration does not exist" );
        }
        return Job.super.validate();
    }

    @Override
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        if ( !systemSettingManager.getBoolSetting( SettingKey.CREDENTIALS_EXPIRY_ALERT ) )
        {
            log.info( "credentialsExpiryAlertTask aborted. Expiry alerts are disabled" );
            return;
        }
        progress.startingProcess( "User password expiry alerts" );
        int daysUntilDisable = systemSettingManager.getIntSetting( SettingKey.CREDENTIALS_EXPIRES_REMINDER_IN_DAYS );
        int daysBeforePasswordChangeRequired = systemSettingManager
            .getIntSetting( SettingKey.CREDENTIALS_EXPIRES ) * 30;
        if ( daysBeforePasswordChangeRequired <= 0 )
        {
            progress.completedProcess( "Passwords expire after n months is configured to zero (feature disabled)" );
            return;
        }
        LocalDate lastUpdatedBefore = LocalDate.now().minusDays( daysBeforePasswordChangeRequired );
        do
        {
            sendReminderEmail( lastUpdatedBefore, daysUntilDisable, progress );
            daysUntilDisable = daysUntilDisable / 2;
        }
        while ( daysUntilDisable > 0 );
        progress.completedProcess( null );
    }

    private void sendReminderEmail( LocalDate lastUpdatedBefore, int daysUntilChangeRequired, JobProgress progress )
    {
        LocalDate referenceDay = lastUpdatedBefore.plusDays( daysUntilChangeRequired );
        ZonedDateTime nDaysPriorToChangeRequired = referenceDay.atStartOfDay( systemDefault() );
        ZonedDateTime nDaysPriorToChangeRequiredEod = referenceDay.plusDays( 1 ).atStartOfDay( systemDefault() );

        progress.startingStage(
            format( "Fetching users for reminder, %d days until password change is required", daysUntilChangeRequired ),
            SKIP_STAGE );
        Map<String, Optional<Locale>> receivers = progress.runStage( Map.of(),
            map -> format( "Found %d receivers", map.size() ),
            () -> userService.findNotifiableUsersWithPasswordLastUpdatedBetween(
                Date.from( nDaysPriorToChangeRequired.toInstant() ),
                Date.from( nDaysPriorToChangeRequiredEod.toInstant() ) ) );
        if ( receivers.isEmpty() )
        {
            return;
        }
        // send reminders by language
        Locale fallback = localeManager.getFallbackLocale();
        Map<Locale, Set<String>> receiversByLocale = new HashMap<>();
        for ( Map.Entry<String, Optional<Locale>> e : receivers.entrySet() )
        {
            receiversByLocale.computeIfAbsent( e.getValue().orElse( fallback ), key -> new HashSet<>() )
                .add( e.getKey() );
        }
        progress.startingStage(
            format( "Sending reminder for %d days until password change is required", daysUntilChangeRequired ),
            receiversByLocale.size(), JobProgress.FailurePolicy.SKIP_ITEM );
        progress.runStage( receiversByLocale.entrySet().stream(),
            e -> format( "Sending email to %d user(s) in %s", e.getValue().size(), e.getKey().getDisplayLanguage() ),
            OutboundMessageResponse::getDescription,
            e -> sendReminderEmailInLanguage( e.getKey(), e.getValue(), daysUntilChangeRequired ), null );
    }

    private OutboundMessageResponse sendReminderEmailInLanguage( Locale language, Set<String> receiverEmails,
        int daysUntilChangeRequired )
    {
        I18n i18n = i18nManager.getI18n( language );
        String subject = i18n.getString( "notification.password_change_required.subject",
            "Password Expiry Alert" );
        String body = i18n.getString( "notification.password_change_required.body",
            "Please change your password. It will expire in {0} day(s)." );
        OutboundMessageResponse response = emailService.sendEmail( subject,
            format( body.replace( "{0}", "%d" ), daysUntilChangeRequired ), receiverEmails );

        if ( response.getResponseObject() != EmailResponse.SENT )
        {
            throw new UncheckedIOException( response.getDescription(), new IOException() );
        }
        return response;
    }
}
