package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Saptarshi Purkayastha
 */
public class DefaultAppManager
    implements AppManager
{
    private static final Log log = LogFactory.getLog( DefaultAppManager.class );

    @Autowired
    private SystemSettingManager appSettingManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private LocalAppStorageService localAppStorageService;

    @Autowired
    private JCloudsAppStorageService jCloudsAppStorageService;

    @Autowired
    private KeyJsonValueService keyJsonValueService;

    // -------------------------------------------------------------------------
    // AppManagerService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<App> getApps( String contextPath )
    {
        List<App> apps = Lists.newArrayList( getAppMap().values() );
        
        apps.forEach( a -> a.init( contextPath ) );
        
        return apps;
    }

    @Override
    public List<App> getApps( AppType appType, int max )
    {
        return getApps( null ).stream()
            .filter( app -> appType == app.getAppType() )
            .limit( max )
            .collect( Collectors.toList() );
    }
    
    @Override
    public App getApp( String appName )
    {
        // Checks for app.getUrlFriendlyName which is the key of AppMap
        if ( getAppMap().containsKey( appName ) )
        {
            return getAppMap().get( appName );
        }

        // If no apps are found, check for original name
        for ( App app : getAppMap().values() )
        {
            if ( app.getName().equals( appName ) )
            {
                return app;
            }
        }

        return null;
    }

    @Override
    public List<App> getAppsByType( AppType appType, Collection<App> apps )
    {
        return apps.stream()
            .filter( app -> app.getAppType() == appType )
            .collect( Collectors.toList() );
    }

    @Override
    public List<App> getAppsByName( final String name, Collection<App> apps, final String operator )
    {
        return apps.stream().filter( app -> (
            ("ilike".equalsIgnoreCase( operator ) && app.getName().toLowerCase().contains( name.toLowerCase() )) ||
                ("eq".equalsIgnoreCase( operator ) && app.getName().equals( name ))) ).
            collect( Collectors.toList() );
    }

    @Override
    public List<App> filterApps( List<String> filters, String contextPath )
    {
        List<App> apps = getApps( contextPath );
        Set<App> returnList = new HashSet<>( apps );

        for ( String filter : filters )
        {
            String[] split = filter.split( ":" );

            if ( split.length != 3 )
            {
                throw new QueryParserException( "Invalid filter: " + filter );
            }

            if ( "appType".equalsIgnoreCase( split[0] ) )
            {
                String appType = split[2] != null ? split[2].toUpperCase() : null;
                returnList.retainAll( getAppsByType( AppType.valueOf( appType ), returnList ) );
            }
            else if ( "name".equalsIgnoreCase( split[0] ) )
            {
                returnList.retainAll( getAppsByName( split[2], returnList, split[1] ) );
            }
        }

        return new ArrayList<>( returnList );
    }

    @Override
    public App getApp( String key, String contextPath )
    {
        List<App> apps = getApps( contextPath );

        for ( App app : apps )
        {
            if ( key.equals( app.getKey() ) )
            {
                return app;
            }
        }

        return null;
    }

    @Override
    public List<App> getAccessibleApps( String contextPath )
    {
        User user = currentUserService.getCurrentUser();

        return getApps( contextPath ).stream().filter( a -> this.isAccessible( a, user ) )
            .collect( Collectors.toList() );
    }

    @Override
    public AppStatus installApp( File file, String fileName )
    {
        return jCloudsAppStorageService.installApp( file, fileName );
    }

    @Override
    public boolean exists( String appName )
    {
        return getApp( appName ) != null;
    }

    @Override
    public boolean deleteApp( App app, boolean deleteAppData )
    {
        boolean deleted = false;

        if ( app != null )
        {
            deleted = getAppStorageServiceByApp( app ).deleteApp( app );
        }

        if ( deleted && deleteAppData )
        {
            keyJsonValueService.deleteNamespace( app.getActivities().getDhis().getNamespace() );
            log.info( String.format( "Deleted app namespace '%s'", app.getActivities().getDhis().getNamespace() ) );
        }

        return deleted;
    }

    @Override
    public String getAppStoreUrl()
    {
        return StringUtils.trimToNull( (String) appSettingManager.getSystemSetting( SettingKey.APP_STORE_URL ) );
    }

    @Override
    public void setAppStoreUrl( String appStoreUrl )
    {
        appSettingManager.saveSystemSetting( SettingKey.APP_STORE_URL, appStoreUrl );
    }

    /**
     * Triggers AppStorageServices to re-discover apps
     */
    @Override
    public void reloadApps()
    {
        localAppStorageService.discoverInstalledApps();
        jCloudsAppStorageService.discoverInstalledApps();
    }

    @Override
    public boolean isAccessible( App app )
    {
        return isAccessible( app, currentUserService.getCurrentUser() );
    }

    @Override
    public boolean isAccessible( App app, User user )
    {
        if ( user == null || user.getUserCredentials() == null || app == null || app.getName() == null )
        {
            return false;
        }

        UserCredentials userCredentials = user.getUserCredentials();

        return userCredentials.getAllAuthorities().contains( "ALL" ) ||
            userCredentials.getAllAuthorities().contains( "M_dhis-web-maintenance-appmanager" ) ||
            userCredentials.getAllAuthorities().contains( "See " + app.getName().trim() );
    }

    @Override
    public App getAppByNamespace( String namespace )
    {
        return getNamespaceMap().get( namespace );
    }

    @Override
    public Resource getAppResource( App app, String pageName )
        throws IOException
    {
        return getAppStorageServiceByApp( app ).getAppResource( app, pageName );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private AppStorageService getAppStorageServiceByApp( App app )
    {
        if ( app != null && app.getAppStorageSource().equals( AppStorageSource.LOCAL ) )
        {
            return localAppStorageService;
        }
        else
        {
            return jCloudsAppStorageService;
        }
    }

    private Map<String, App> getAppMap()
    {
        Map<String, App> apps = new HashMap<>();

        apps.putAll( jCloudsAppStorageService.getApps() );
        apps.putAll( localAppStorageService.getApps() );

        return apps;
    }

    private Map<String, App> getNamespaceMap()
    {
        Map<String, App> apps = new HashMap<>();

        apps.putAll( jCloudsAppStorageService.getReservedNamespaces() );
        apps.putAll( localAppStorageService.getReservedNamespaces() );

        return apps;
    }
}
