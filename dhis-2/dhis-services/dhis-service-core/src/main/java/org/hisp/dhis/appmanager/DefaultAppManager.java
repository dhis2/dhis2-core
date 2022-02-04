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
package org.hisp.dhis.appmanager;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilderProvider;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Saptarshi Purkayastha
 */
@Slf4j
@Component( "org.hisp.dhis.appmanager.AppManager" )
public class DefaultAppManager
    implements AppManager
{
    public static final String INVALID_FILTER_MSG = "Invalid filter: ";

    private final DhisConfigurationProvider dhisConfigurationProvider;

    private final CurrentUserService currentUserService;

    private final AppStorageService localAppStorageService;

    private final AppStorageService jCloudsAppStorageService;

    private final DatastoreService datastoreService;

    /**
     * In-memory storage of installed apps. Initially loaded on startup. Should
     * not be cleared during runtime.
     */
    private final Cache<App> appCache;

    public DefaultAppManager( DhisConfigurationProvider dhisConfigurationProvider,
        CurrentUserService currentUserService,
        @Qualifier( "org.hisp.dhis.appmanager.LocalAppStorageService" ) AppStorageService localAppStorageService,
        @Qualifier( "org.hisp.dhis.appmanager.JCloudsAppStorageService" ) AppStorageService jCloudsAppStorageService,
        DatastoreService datastoreService, CacheBuilderProvider cacheBuilderProvider )
    {
        checkNotNull( dhisConfigurationProvider );
        checkNotNull( currentUserService );
        checkNotNull( localAppStorageService );
        checkNotNull( jCloudsAppStorageService );
        checkNotNull( datastoreService );
        checkNotNull( cacheBuilderProvider );

        this.dhisConfigurationProvider = dhisConfigurationProvider;
        this.currentUserService = currentUserService;
        this.localAppStorageService = localAppStorageService;
        this.jCloudsAppStorageService = jCloudsAppStorageService;
        this.datastoreService = datastoreService;
        this.appCache = cacheBuilderProvider.<App> newCacheBuilder()
            .forRegion( "appCache" )
            .build();
    }

    // -------------------------------------------------------------------------
    // AppManagerService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<App> getApps( String contextPath )
    {
        List<App> apps = appCache.getAll().filter( app -> app.getAppState() != AppStatus.DELETION_IN_PROGRESS )
            .collect( Collectors.toList() );

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
        return appCache.getAll().filter( app -> app.getShortName().equals( appName ) ).findFirst().orElse( null );
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
        return apps.stream().filter(
            app -> (("ilike".equalsIgnoreCase( operator ) && app.getName().toLowerCase().contains( name.toLowerCase() ))
                ||
                ("eq".equalsIgnoreCase( operator ) && app.getName().equals( name ))) )
            .collect( Collectors.toList() );
    }

    @Override
    public List<App> getAppsByShortName( final String name, Collection<App> apps, final String operator )
    {
        return apps.stream()
            .filter( app -> (("ilike".equalsIgnoreCase( operator )
                && app.getShortName().toLowerCase().contains( name.toLowerCase() )) ||
                ("eq".equalsIgnoreCase( operator ) && app.getShortName().equals( name ))) )
            .collect( Collectors.toList() );
    }

    @Override
    public List<App> getAppsByIsBundled( final boolean isBundled, Collection<App> apps )
    {
        return apps
            .stream()
            .filter( app -> app.isBundled() == isBundled )
            .collect( Collectors.toList() );
    }

    private void applyFilter( Set<App> apps, String key, String operator, String value )
    {
        if ( "appType".equalsIgnoreCase( key ) )
        {
            String appType = value != null ? value.toUpperCase() : null;
            apps.retainAll( getAppsByType( AppType.valueOf( appType ), apps ) );
        }
        else if ( "name".equalsIgnoreCase( key ) )
        {
            apps.retainAll( getAppsByName( value, apps, operator ) );
        }
        else if ( "shortName".equalsIgnoreCase( key ) )
        {
            apps.retainAll( getAppsByShortName( value, apps, operator ) );
        }
        else if ( "bundled".equalsIgnoreCase( key ) )
        {
            boolean isBundled = "true".equalsIgnoreCase( value );
            apps.retainAll( getAppsByIsBundled( isBundled, apps ) );
        }
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
                throw new QueryParserException( INVALID_FILTER_MSG + filter );
            }

            if ( !"name".equalsIgnoreCase( split[0] ) && !"shortName".equalsIgnoreCase( split[0] )
                && !"eq".equalsIgnoreCase( split[1] ) )
            {
                throw new QueryParserException( INVALID_FILTER_MSG + filter );
            }

            if ( "bundled".equalsIgnoreCase( split[0] ) && !"true".equalsIgnoreCase( split[2] )
                && !"false".equalsIgnoreCase( split[2] ) )
            {
                throw new QueryParserException( INVALID_FILTER_MSG + filter );
            }

            applyFilter( returnList, split[0], split[1], split[2] );
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
            registerKeyJsonValueProtection( app );
        }

        return app.getAppState();
    }

    @Override
    public boolean exists( String appName )
    {
        return getApp( appName ) != null;
    }

    @Override
    public void deleteApp( App app, boolean deleteAppData )
    {
        if ( app != null )
        {
            getAppStorageServiceByApp( app ).deleteApp( app );
            unregisterKeyJsonValueProtection( app );
            if ( deleteAppData )
            {
                deleteAppData( app );
            }

            appCache.invalidate( app.getKey() );
        }
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
        String baseUrl = StringUtils
            .trimToNull( dhisConfigurationProvider.getProperty( ConfigurationKey.APPHUB_BASE_URL ) );
        String apiUrl = StringUtils
            .trimToNull( dhisConfigurationProvider.getProperty( ConfigurationKey.APPHUB_API_URL ) );

        return "{" +
            "\"baseUrl\": \"" + baseUrl + "\", " +
            "\"apiUrl\": \"" + apiUrl + "\"" +
            "}";
    }

    /**
     * Reloads apps by triggering the process to discover apps from local
     * filesystem and remote cloud storage and installing all detected apps.
     * This method is invoked automatically on startup.
     */
    @Override
    @PostConstruct
    public void reloadApps()
    {
        localAppStorageService.discoverInstalledApps().values().stream()
            .filter( app -> !exists( app.getKey() ) )
            .forEach( this::installApp );

        jCloudsAppStorageService.discoverInstalledApps().values().stream()
            .filter( app -> !exists( app.getKey() ) )
            .forEach( this::installApp );

    }

    private void installApp( App app )
    {
        appCache.put( app.getKey(), app );
        registerKeyJsonValueProtection( app );
    }

    @Override
    public boolean isAccessible( App app )
    {
        return isAccessible( app, currentUserService.getCurrentUser() );
    }

    @Override
    public boolean isAccessible( App app, User user )
    {
        if ( app == null || app.getShortName() == null || user == null )
        {
            return false;
        }

        Set<String> auths = user.getAllAuthorities();

        return auths.contains( "ALL" ) ||
            auths.contains( WEB_MAINTENANCE_APPMANAGER_AUTHORITY ) ||
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

    private void deleteAppData( App app )
    {
        String namespace = app.getActivities().getDhis().getNamespace();
        if ( namespace != null && !namespace.isEmpty() )
        {
            datastoreService.deleteNamespace( namespace );
            log.info( String.format( "Deleted app namespace '%s'", namespace ) );
        }
    }

    private void registerKeyJsonValueProtection( App app )
    {
        String namespace = app.getActivities().getDhis().getNamespace();
        if ( namespace != null && !namespace.isEmpty() )
        {
            String[] authorities = app.getShortName() == null
                ? new String[] { WEB_MAINTENANCE_APPMANAGER_AUTHORITY }
                : new String[] { WEB_MAINTENANCE_APPMANAGER_AUTHORITY, app.getSeeAppAuthority() };
            datastoreService.addProtection(
                new DatastoreNamespaceProtection( namespace, ProtectionType.RESTRICTED, true, authorities ) );
        }
    }

    private void unregisterKeyJsonValueProtection( App app )
    {
        String namespace = app.getActivities().getDhis().getNamespace();
        if ( namespace != null && !namespace.isEmpty() )
        {
            datastoreService.removeProtection( namespace );
        }
    }
}
