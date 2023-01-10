/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.appmanager;

import static org.hisp.dhis.util.FileUtils.getResourceFileAsString;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.appmanager.webmodules.ConfigurableWebModuleComparator;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppMenuManager
{
    private static final List<String> MODULE_ORDER = List.of( "dhis-web-dashboard-integration",
        "dhis-web-visualizer", "dhis-web-mapping", "dhis-web-event-reports", "dhis-web-event-visualizer",
        "dhis-web-dataentry", "dhis-web-tracker-capture", "dhis-web-reporting", "dhis-web-dashboard" );

    private static final Comparator<WebModule> MODULE_COMPARATOR = new ConfigurableWebModuleComparator( MODULE_ORDER );

    private static final Set<String> MENU_MODULE_EXCLUSIONS = Set.of( "dhis-web-apps" );

    private final I18nManager i18nManager;

    private final UserService userService;

    private final LocaleManager localeManager;

    private final List<WebModule> menuModules = new ArrayList<>();

    private Locale currentLocale;

    private void detectModules()
        throws IOException
    {
        String content = getResourceFileAsString( "apps-to-bundle_COPY.json" );

        for ( String moduleUri : getAppUriList( content ) )
        {
            String key = "dhis-web-" + moduleUri.split( "/" )[4].replace( "-app", "" );
            String displayName = i18nManager.getI18n().getString( key );

            WebModule module = new WebModule( key, "/" + key, "../" + key + "/index.html" );
            module.setDisplayName( displayName );
            module.setIcon( "../icons/" + key + ".png" );

            if ( !MENU_MODULE_EXCLUSIONS.contains( key ) )
                menuModules.add( module );
        }

        menuModules.sort( MODULE_COMPARATOR );

        currentLocale = localeManager.getCurrentLocale();
    }

    private void detectLocaleChange()
    {
        if ( localeManager.getCurrentLocale().equals( currentLocale ) )
        {
            return;
        }

        menuModules.forEach( m -> m.setDisplayName( i18nManager.getI18n().getString( m.getName() ) ) );

        currentLocale = localeManager.getCurrentLocale();
    }

    private static List<String> getAppUriList( String content )
    {
        List<String> uriList = new Gson().fromJson( content, new TypeToken<List<String>>()
        {
        }.getType() );

        if ( uriList == null )
        {
            throw new IllegalStateException( "Failed to parse apps-to-bundle.json content" );
        }

        return uriList;
    }

    public List<WebModule> getAppMenu( String username )
    {
        if ( menuModules.isEmpty() )
        {
            try
            {
                detectModules();
            }
            catch ( IOException e )
            {
                log.error( "Failed to read web modules configuration", e );
            }
        }

        detectLocaleChange();

        return getAccessibleModules( menuModules, username );
    }

    private List<WebModule> getAccessibleModules( List<WebModule> modules, String username )
    {
        return modules.stream()
            .filter( module -> module != null && hasAccess( username, module.getName() ) )
            .collect( Collectors.toList() );
    }

    private boolean hasAccess( String username, String module )
    {
        User userByUsername = userService.getUserByUsername( username );
        Set<String> allAuthorities = userByUsername.getAllAuthorities();

        boolean containsAuth = allAuthorities.contains( "M_" + module );
        boolean containsAll = allAuthorities.contains( "ALL" );

        return containsAll || containsAuth;
    }
}
