/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.config;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.ui.resourcebundle.DefaultResourceBundleManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.message.EmailMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.DefaultOutboundMessageBatchService;
import org.hisp.dhis.setting.DefaultStyleManager;
import org.hisp.dhis.setting.StyleManager;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.sms.config.SmsMessageSender;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Luciano Fiandesio
 */
@Configuration( "coreServiceConfig" )
public class ServiceConfig
{
    @Bean( "taskScheduler" )
    public ThreadPoolTaskScheduler threadPoolTaskScheduler()
    {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize( 25 );
        return threadPoolTaskScheduler;
    }

    @Bean( "org.hisp.dhis.setting.StyleManager" )
    public StyleManager styleManager( SystemSettingManager systemSettingManager, UserSettingService userSettingService,
        I18nManager i18nManager )
    {
        SortedMap<String, String> styles = new TreeMap<>();
        styles.put( "light_blue", "light_blue/light_blue.css" );
        styles.put( "green", "green/green.css" );
        styles.put( "myanmar", "myanmar/myanmar.css" );
        styles.put( "vietnam", "vietnam/vietnam.css" );
        styles.put( "india", "india/india.css" );

        return new DefaultStyleManager( systemSettingManager, userSettingService, styles, i18nManager );
    }

    @Bean( "org.hisp.dhis.outboundmessage.OutboundMessageService" )
    public DefaultOutboundMessageBatchService defaultOutboundMessageBatchService( SmsMessageSender smsMessageSender,
        EmailMessageSender emailMessageSender )
    {
        Map<DeliveryChannel, MessageSender> channels = new HashMap<>();
        channels.put( DeliveryChannel.SMS, smsMessageSender );
        channels.put( DeliveryChannel.EMAIL, emailMessageSender );

        DefaultOutboundMessageBatchService service = new DefaultOutboundMessageBatchService();

        service.setMessageSenders( channels );

        return service;
    }

    @Bean( "org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager" )
    public ResourceBundleManager resourceBundleManager()
    {
        return new DefaultResourceBundleManager();
    }
}
