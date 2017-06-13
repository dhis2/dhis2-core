package org.hisp.dhis.message;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.email.EmailConfiguration;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.scheduling.annotation.Async;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class EmailMessageSender
    implements MessageSender
{
    private static final Log log = LogFactory.getLog( EmailMessageSender.class );

    private static final String DEFAULT_APPLICATION_TITLE = "DHIS 2";

    private static final String DEFAULT_FROM_NAME = DEFAULT_APPLICATION_TITLE + " Message [No reply]";

    private static final String DEFAULT_SUBJECT_PREFIX = "[" + DEFAULT_APPLICATION_TITLE + "] ";

    private static final String LB = System.getProperty( "line.separator" );

    private static final String MESSAGE_EMAIL_TEMPLATE = "message_email";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }

    // -------------------------------------------------------------------------
    // MessageSender implementation
    // -------------------------------------------------------------------------

    /**
     * Note this methods is invoked asynchronously.
     */
    @Async
    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, String footer, User sender, Set<User> users,
        boolean forceSend )
    {
        EmailConfiguration emailConfig = getEmailConfiguration();

        String errorMessage = "No recipient found";

        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( emailConfig.getHostName() == null )
        {
            status.setOk( false );
            status.setResponseObject( EmailResponse.NOT_CONFIGURED );

            return status;
        }

        String plainContent = renderPlainContent( text, sender );
        String htmlContent = renderHtmlContent( text, footer, sender );

        try
        {
            HtmlEmail email = getHtmlEmail( emailConfig.getHostName(), emailConfig.getPort(), emailConfig.getUsername(),
                    emailConfig.getPassword(), emailConfig.isTls(), emailConfig.getFrom() );
            email.setSubject( customizeTitle( DEFAULT_SUBJECT_PREFIX ) + subject );
            email.setTextMsg( plainContent );
            email.setHtmlMsg( htmlContent );

            boolean hasRecipients = false;

            for ( User user : users )
            {
                boolean doSend = forceSend
                    || (Boolean) userSettingService.getUserSetting( UserSettingKey.MESSAGE_EMAIL_NOTIFICATION, user );

                if ( doSend && ValidationUtils.emailIsValid( user.getEmail() ) )
                {
                    if ( isEmailValid( user.getEmail() ) )
                    {
                        email.addBcc( user.getEmail() );

                        log.info( "Sending email to user: " + user.getUsername() + " with email address: "
                            + user.getEmail() + " to host: " + emailConfig.getHostName() + ":" + emailConfig.getPort() );

                        hasRecipients = true;
                    }
                    else
                    {
                        log.error( user.getEmail() + " is not a valid email for user: " + user.getUsername() );

                        errorMessage = "No valid email address found";
                    }
                }
            }

            if ( hasRecipients )
            {
                email.send();

                log.info( "Email sent using host: " + emailConfig.getHostName() + ":" + emailConfig.getPort() + " with TLS: " + emailConfig.isTls() );

                status = new OutboundMessageResponse( "Email sent", EmailResponse.SENT, true );
            }
            else
            {
                status = new OutboundMessageResponse( errorMessage, EmailResponse.ABORTED, false );
            }
        }
        catch ( EmailException ex )
        {
            log.warn( "Could not send email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace( ex ) );

            status = new OutboundMessageResponse( "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false );
        }
        catch ( RuntimeException ex )
        {
            log.warn( "Error while sending email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace( ex ) );

            status = new OutboundMessageResponse( "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false );
        }

        return status;
    }

    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, Set<String> recipients )
    {
        EmailConfiguration emailConfig = getEmailConfiguration();

        String errorMessage = "No recipient found";

        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( emailConfig.getHostName() == null )
        {
            status.setOk( false );
            status.setResponseObject( EmailResponse.NOT_CONFIGURED );

            return status;
        }

        try
        {
            HtmlEmail email = getHtmlEmail( emailConfig.getHostName(), emailConfig.getPort(), emailConfig.getUsername(),
                    emailConfig.getPassword(), emailConfig.isTls(), emailConfig.getFrom() );
            email.setSubject( customizeTitle( DEFAULT_SUBJECT_PREFIX ) + subject );
            email.setTextMsg( text );

            boolean hasRecipients = false;

            for ( String recipient : recipients )
            {
                if ( isEmailValid( recipient ) )
                {
                    email.addBcc( recipient );

                    hasRecipients = true;

                    log.info( "Sending email to : " + recipient + " to host: " + emailConfig.getHostName() + ":" + emailConfig.getPort() );
                }
                else
                {
                    log.error( recipient + " is not a valid email" );

                    errorMessage = "No valid email address found";
                }
            }

            if ( hasRecipients )
            {
                email.send();

                log.info( "Email sent using host: " + emailConfig.getHostName() + ":" + emailConfig.getPort() + " with TLS: " + emailConfig.isTls() );

                return new OutboundMessageResponse( "Email sent", EmailResponse.SENT, true );
            }
            else
            {
                status = new OutboundMessageResponse( errorMessage, EmailResponse.ABORTED, false );
            }
        }
        catch ( EmailException ex )
        {
            log.warn( "Error while sending email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace( ex ) );

            status = new OutboundMessageResponse( "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false );
        }
        catch ( RuntimeException ex )
        {
            log.warn( "Error while sending email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace( ex ) );

            status = new OutboundMessageResponse( "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false );
        }

        return status;
    }

    @Override
    public OutboundMessageResponse sendMessage( String subject, String text, String recipient )
    {
        return sendMessage( subject, text, Sets.newHashSet( recipient ) );
    }

    @Override
    public OutboundMessageResponseSummary sendMessageBatch( OutboundMessageBatch batch )
    {
        List<OutboundMessageResponse> statuses = batch.getMessages().stream()
            .map( m -> sendMessage( m.getSubject(), m.getText(), m.getRecipients() ) )
            .collect( Collectors.toList() );

        return generateSummary( statuses );
    }

    @Override
    public boolean isConfigured()
    {
        return getEmailConfiguration().isOk();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private HtmlEmail getHtmlEmail( String hostName, int port, String username, String password, boolean tls,
        String sender )
        throws EmailException
    {
        HtmlEmail email = new HtmlEmail();
        email.setHostName( hostName );
        email.setFrom( sender, customizeTitle( DEFAULT_FROM_NAME ) );
        email.setSmtpPort( port );
        email.setStartTLSEnabled( tls );

        if ( username != null && password != null )
        {
            email.setAuthenticator( new DefaultAuthenticator( username, password ) );
        }

        return email;
    }

    private String renderPlainContent( String text, User sender )
    {
        return sender == null ? text
            : (text + LB + LB + sender.getName() + LB
                + (sender.getOrganisationUnitsName() != null ? (sender.getOrganisationUnitsName() + LB)
                    : StringUtils.EMPTY)
                + (sender.getEmail() != null ? (sender.getEmail() + LB) : StringUtils.EMPTY)
                + (sender.getPhoneNumber() != null ? (sender.getPhoneNumber() + LB) : StringUtils.EMPTY));
    }

    private String renderHtmlContent( String text, String footer, User sender )
    {
        HashMap<String, Object> content = new HashMap<>();

        if ( !Strings.isNullOrEmpty( text ) )
        {
            content.put( "text", text.replaceAll( "\\r?\\n", "<br>" ) );
        }

        if ( !Strings.isNullOrEmpty( footer ) )
        {
            content.put( "footer", footer );
        }

        if ( sender != null )
        {
            content.put( "senderName", sender.getName() );

            if ( sender.getOrganisationUnitsName() != null )
            {
                content.put( "organisationUnitsName", sender.getOrganisationUnitsName() );
            }

            if ( sender.getEmail() != null )
            {
                content.put( "email", sender.getEmail() );
            }

            if ( sender.getPhoneNumber() != null )
            {
                content.put( "phoneNumber", sender.getPhoneNumber() );
            }
        }

        return new VelocityManager().render( content, MESSAGE_EMAIL_TEMPLATE );
    }

    private String customizeTitle( String title )
    {
        String appTitle = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );

        if ( appTitle != null && !appTitle.isEmpty() )
        {
            title = title.replace( DEFAULT_APPLICATION_TITLE, appTitle );
        }

        return title;
    }

    private boolean isEmailValid( String email )
    {
        return ValidationUtils.emailIsValid( email );
    }

    private EmailConfiguration getEmailConfiguration()
    {
        String hostName = (String) systemSettingManager.getSystemSetting( SettingKey.EMAIL_HOST_NAME );
        String username = (String) systemSettingManager.getSystemSetting( SettingKey.EMAIL_USERNAME );
        String password = (String) systemSettingManager.getSystemSetting( SettingKey.EMAIL_PASSWORD );
        String from = (String) systemSettingManager.getSystemSetting( SettingKey.EMAIL_SENDER );
        int port = (int) systemSettingManager.getSystemSetting( SettingKey.EMAIL_PORT );
        boolean tls = (boolean) systemSettingManager.getSystemSetting( SettingKey.EMAIL_TLS );

        return new EmailConfiguration( hostName, username, password, from, port, tls );
    }

    private OutboundMessageResponseSummary generateSummary( List<OutboundMessageResponse> statuses )
    {
        OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary();

        int total, sent = 0;

        boolean ok = true;

        String errorMessage = StringUtils.EMPTY;

        total = statuses.size();

        for ( OutboundMessageResponse status : statuses )
        {
            if ( EmailResponse.SENT.equals( status.getResponseObject() ) )
            {
                sent++;
            }
            else
            {
                ok = false;

                errorMessage = status.getDescription();
            }
        }

        summary.setTotal( total );
        summary.setChannel( DeliveryChannel.EMAIL );
        summary.setSent( sent );
        summary.setFailed( total - sent );

        if ( !ok )
        {
            summary.setBatchStatus( OutboundMessageBatchStatus.FAILED );
            summary.setErrorMessage( errorMessage );

            log.error( errorMessage );
        }
        else
        {
            summary.setBatchStatus( OutboundMessageBatchStatus.COMPLETED );
            summary.setResponseMessage( "SENT" );

            log.info( "EMAIL batch processed successfully" );
        }

        return summary;
    }
}
