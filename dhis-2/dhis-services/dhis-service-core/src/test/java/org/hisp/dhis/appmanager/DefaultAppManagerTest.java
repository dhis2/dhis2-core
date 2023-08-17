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

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.hisp.dhis.appmanager.AppType.DASHBOARD_WIDGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilder;
import org.hisp.dhis.cache.DefaultCacheBuilderProvider;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DefaultAppManager}.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class DefaultAppManagerTest
{
    @Mock
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AppStorageService localAppStorageService;

    @Mock
    private AppStorageService jCloudsAppStorageService;

    @Mock
    private Cache<App> appCache;

    @Mock
    private DefaultCacheBuilderProvider cacheBuilderProvider;

    @Mock
    private KeyJsonValueService keyJsonValueService;

    @Mock
    private CacheBuilder cacheBuilder;

    private AppManager appManager;

    @BeforeEach
    void beforeEach()
    {
        requiredByAllTests();
    }

    @Test
    void testGetDashboardPlugins()
    {
        // Given
        appManager = Mockito.spy( appManager );

        // Then
        stubAppsStream();
        List<App> apps = appManager.getApps( DASHBOARD_WIDGET, 2, false );
        assertEquals( 2, apps.size() );

        stubAppsStream();
        apps = appManager.getApps( DASHBOARD_WIDGET, 3, false );
        assertEquals( 3, apps.size() );

        stubAppsStream();
        apps = appManager.getApps( DASHBOARD_WIDGET, 5, false );
        assertEquals( 3, apps.size() );

        stubAppsStream();
        apps = appManager.getApps( DASHBOARD_WIDGET, 5, true );
        assertEquals( 1, apps.size() );
        assertEquals( "App 3", apps.get( 0 ).getName() );

        stubAppsStream();
        apps = appManager.getApps( DASHBOARD_WIDGET, 1, true );
        assertEquals( 1, apps.size() );
        assertEquals( "App 3", apps.get( 0 ).getName() );

        stubAppsStream();
        apps = appManager.getApps( DASHBOARD_WIDGET, 0, true );
        assertEquals( 0, apps.size() );
    }

    /**
     * Required by all tests to work.
     */
    private void requiredByAllTests()
    {
        doReturn( cacheBuilder ).when( cacheBuilderProvider ).newCacheBuilder();
        doReturn( cacheBuilder ).when( cacheBuilder ).forRegion( "appCache" );
        doReturn( appCache ).when( cacheBuilder ).build();

        appManager = new DefaultAppManager( dhisConfigurationProvider, currentUserService, localAppStorageService,
            jCloudsAppStorageService, keyJsonValueService, cacheBuilderProvider );
    }

    /**
     * Used multiple times before each test (if applicable). Needed because
     * streams can be used only once.
     */
    private void stubAppsStream()
    {
        when( appCache.getAll() ).thenReturn( stubApps() );
    }

    private Stream<App> stubApps()
    {
        return unmodifiableList( asList(
            stubApp( "Line Listing", false ),
            stubApp( "Data Visualizer", true ),
            stubApp( "App 3", false ) ) )
                .stream();
    }

    private App stubApp( String name, boolean isCoreApp )
    {
        App app = new App();
        app.setName( name );
        app.setCoreApp( isCoreApp );
        app.setAppType( DASHBOARD_WIDGET );

        return app;
    }
}
