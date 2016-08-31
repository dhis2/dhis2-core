package org.hisp.dhis.interceptor;

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

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;

/**
 * @author Lars Helge Overland
 */
public class SystemSettingInterceptor
    implements Interceptor
{
    private static final String DATE_FORMAT = "dateFormat";
    private static final String SYSPROP_PORTAL = "runningAsPortal";

    private static final Set<SettingKey> SETTINGS = Sets.newHashSet( SettingKey.APPLICATION_TITLE, SettingKey.APPLICATION_INTRO,
        SettingKey.APPLICATION_NOTIFICATION, SettingKey.APPLICATION_FOOTER, SettingKey.APPLICATION_RIGHT_FOOTER,
        SettingKey.FLAG, SettingKey.START_MODULE, SettingKey.MULTI_ORGANISATION_UNIT_FORMS, SettingKey.ACCOUNT_RECOVERY,
        SettingKey.INSTANCE_BASE_URL, SettingKey.GOOGLE_ANALYTICS_UA, SettingKey.OPENID_PROVIDER,
        SettingKey.OPENID_PROVIDER_LABEL, SettingKey.HELP_PAGE_LINK, SettingKey.REQUIRE_ADD_TO_VIEW, SettingKey.ALLOW_OBJECT_ASSIGNMENT,
        SettingKey.CALENDAR, SettingKey.DATE_FORMAT );
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService( ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    @Autowired
    private CalendarService calendarService;

    // -------------------------------------------------------------------------
    // AroundInterceptor implementation
    // -------------------------------------------------------------------------

    @Override
    public void destroy()
    {
    }

    @Override
    public void init()
    {
    }

    @Override
    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        Map<String, Object> map = new HashMap<>();
        
        map.put( DATE_FORMAT, calendarService.getSystemDateFormat() );
        map.put( SettingKey.CONFIGURATION.getName(), configurationService.getConfiguration() );
        map.put( SettingKey.FLAG_IMAGE.getName(), systemSettingManager.getFlagImage() );
        map.put( SettingKey.CREDENTIALS_EXPIRES.getName(), systemSettingManager.credentialsExpires() );
        map.put( SettingKey.SELF_REGISTRATION_NO_RECAPTCHA.getName(), systemSettingManager.selfRegistrationNoRecaptcha() );
        map.put( SYSPROP_PORTAL, defaultIfEmpty( System.getProperty( SYSPROP_PORTAL ), String.valueOf( false ) ) );
        
        map.putAll( systemSettingManager.getSystemSettings( SETTINGS ) );
        
        invocation.getStack().push( map );

        return invocation.invoke();
    }
}
