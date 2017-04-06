package org.hisp.dhis.credentials;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by zubair on 29.03.17.
 */
public class CredentialsExpiryAlertTask implements Runnable
{
    private static final Log log = LogFactory.getLog( CredentialsExpiryAlertTask.class );

    private static final String SUBJECT = "Password Expiry Alert";

    private static final String TEXT = "Dear %s, Please change your password. It will expire in %d days.";

    public static final String KEY_TASK = "credentialsExpiryAlertTask";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private UserService userService;

    @Autowired
    @Resource ( name = "emailMessageSender" )
    private MessageSender emailMessageSender;

    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public void run()
    {
        boolean isExpiryAlertEnabled = (Boolean) systemSettingManager.getSystemSetting( SettingKey.CREDENTIALS_EXPIRY_ALERT );

        if ( !isExpiryAlertEnabled )
        {
            log.info( String.format( "%s aborted. Expiry alerts are disabled", KEY_TASK ) );

            return;
        }

        log.info( String.format( "%s has started", KEY_TASK ) );

        List<User> users = userService.getExpiringUsers();

        Map<String, String> content = new HashMap<>();

        for ( User user :  users )
        {
            if ( user.getEmail() != null )
            {
                content.put( user.getEmail(), createText( user ) );
            }
        }

        log.info( String.format( "Users added for alert: %d", content.size() ) );

        sendExpiryAlert( content );
    }

    private void sendExpiryAlert( Map<String,String> content )
    {
        if ( emailMessageSender.isConfigured() )
        {
            for ( String email : content.keySet() )
            {
                emailMessageSender.sendMessage( SUBJECT, content.get( email ), email );
            }
        }
        else
        {
            log.error( "Email service is not configured" );
        }
    }

    private String createText( User user )
    {
        return String.format( TEXT, user.getUsername(), getRemainingDays( user.getUserCredentials() ) );
    }

    private int getRemainingDays( UserCredentials userCredentials )
    {
        int daysBeforeChangeRequired = (Integer) systemSettingManager.getSystemSetting( SettingKey.CREDENTIALS_EXPIRES ) * 30;

        Date passwordLastUpdated = userCredentials.getPasswordLastUpdated();

        return ( daysBeforeChangeRequired - DateUtils.daysBetween( passwordLastUpdated, new Date() ) );
    }
}