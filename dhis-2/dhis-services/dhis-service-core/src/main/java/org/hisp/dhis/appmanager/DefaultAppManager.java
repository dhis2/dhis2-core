package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Saptarshi Purkayastha
 */
@Slf4j
@Component( "org.hisp.dhis.appmanager.AppManager" )
public class DefaultAppManager
    implements AppManager
{
    private final DhisConfigurationProvider dhisConfigurationProvider;

    private final CurrentUserService currentUserService;

    private final LocalAppStorageService localAppStorageService;

    private final JCloudsAppStorageService jCloudsAppStorageService;

    private final KeyJsonValueService keyJsonValueService;

    private final CacheProvider cacheProvider;

    public DefaultAppManager( DhisConfigurationProvider dhisConfigurationProvider, CurrentUserService currentUserService,
        LocalAppStorageService localAppStorageService, JCloudsAppStorageService jCloudsAppStorageService,
        KeyJsonValueService keyJsonValueService, CacheProvider cacheProvider )
    {
        checkNotNull( dhisConfigurationProvider );
        checkNotNull( currentUserService );
        checkNotNull( localAppStorageService );
        checkNotNull( jCloudsAppStorageService );
        checkNotNull( keyJsonValueService );
        checkNotNull( cacheProvider );

        this.dhisConfigurationProvider = dhisConfigurationProvider;
        this.currentUserService = currentUserService;
        this.localAppStorageService = localAppStorageService;
        this.jCloudsAppStorageService = jCloudsAppStorageService;
        this.keyJsonValueService = keyJsonValueService;
        this.cacheProvider = cacheProvider;
    }

    private Cache<App> appCache;

    // -------------------------------------------------------------------------
    // AppManagerService implementation
    // -------------------------------------------------------------------------

    @PostConstruct
    public void initCache()
    {
        appCache = cacheProvider.newCacheBuilder( App.class ).forRegion( "appCache" ).build();
        reloadApps();
    }

    @Override
    public List<App> getApps( String contextPath )
    {
        List<App> apps = appCache.getAll().stream().filter( app -> app.getAppState() != AppStatus.DELETION_IN_PROGRESS ).collect( Collectors.toList() );

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

        Optional<App> appOptional = appCache.getIfPresent( appName );
        if ( appOptional.isPresent() )
        {
            return appOptional.get();
        }

        // If no apps are found, check for original name
        for ( App app : appCache.getAll() )
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
        Collection<App> apps = getApps( contextPath );

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
        App app = jCloudsAppStorageService.installApp( file, fileName, appCache );

        if ( app.getAppState().ok() )
        {
            appCache.put( app.getKey(), app );
        }

        return app.getAppState();
    }

    @Override
    public boolean exists( String appName )
    {
        return getApp( appName ) != null;
    }

    @Override
    @Async
    public void deleteApp( App app, boolean deleteAppData )
    {
        if ( app != null )
        {
            getAppStorageServiceByApp( app ).deleteApp( app );
        }

        if ( deleteAppData )
        {
            keyJsonValueService.deleteNamespace( app.getActivities().getDhis().getNamespace() );
            log.info( String.format( "Deleted app namespace '%s'", app.getActivities().getDhis().getNamespace() ) );
        }

        appCache.invalidate( app.getKey() );
    }

    @Override
    public boolean markAppToDelete( App app )
    {
        boolean markedAppToDelete = false;

        Optional<App> appOpt = appCache.get( app.getKey() );

        if ( appOpt.isPresent() )
        {
            markedAppToDelete = true;
            App appFromCache = appOpt.get();
            appFromCache.setAppState( AppStatus.DELETION_IN_PROGRESS );
            appCache.put( app.getKey(), appFromCache );
        }

        return markedAppToDelete;
    }

    @Override
    public String getAppHubUrl()
    {
        return StringUtils.trimToNull( dhisConfigurationProvider.getProperty( ConfigurationKey.APP_HUB_URL ) );
    }

    /**
     * Triggers AppStorageServices to re-discover apps
     */
    @Override
    public void reloadApps()
    {
        localAppStorageService.discoverInstalledApps().entrySet().stream()
            .filter( entry -> !appCache.getIfPresent( entry.getKey() ).isPresent() )
            .forEach( entry -> appCache.put( entry.getKey(), entry.getValue() ) );

        jCloudsAppStorageService.discoverInstalledApps().entrySet().stream()
            .filter( entry -> !appCache.getIfPresent( entry.getKey() ).isPresent() )
            .forEach( entry -> appCache.put( entry.getKey(), entry.getValue() ) );
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

        Set<String> auths = user.getUserCredentials().getAllAuthorities();

        return auths.contains( "ALL" ) ||
            auths.contains( "M_dhis-web-maintenance-appmanager" ) ||
            auths.contains( app.getSeeAppAuthority() );
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

    @Override
    @EventListener
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        appCache.invalidateAll();
        log.info( "App cache cleared" );
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

    private Map<String, App> getNamespaceMap()
    {
        Map<String, App> apps = new HashMap<>();

        apps.putAll( jCloudsAppStorageService.getReservedNamespaces() );
        apps.putAll( localAppStorageService.getReservedNamespaces() );

        return apps;
    }
}
