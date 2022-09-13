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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Saptarshi
 * @version $Id$
 */
class AppTest
{

    private App app;

    @BeforeEach
    void setUp()
        throws IOException
    {
        String appJson = FileUtils.readFileToString(
            new File( this.getClass().getResource( "/manifest.webapp" ).getFile() ), StandardCharsets.UTF_8 );
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        this.app = mapper.readValue( appJson, App.class );
        this.app.init( "https://example.com" );
    }

    @AfterEach
    void tearDown()
    {
    }

    // TODO: Verify missing property
    @Test
    void testRequiredProperties()
    {
        Assertions.assertEquals( "0.1", app.getVersion() );
        Assertions.assertEquals( "Test App", app.getName() );
        Assertions.assertEquals( "/index.html", app.getLaunchPath() );
        Assertions.assertEquals( "/plugin.html", app.getPluginLaunchPath() );
        Assertions.assertEquals( "*", app.getInstallsAllowedFrom()[0] );
        Assertions.assertEquals( "en", app.getDefaultLocale() );
    }

    // TODO: Complete test for skipped optional properties
    @Test
    void testOptionalProperties()
    {
        Assertions.assertEquals( "Test Description", app.getDescription() );
        Assertions.assertEquals( false, app.getSettings().getDashboardWidget().getHideTitle() );
    }

    @Test
    void testIcons()
    {
        Assertions.assertEquals( "/img/icons/mortar-16.png", app.getIcons().getIcon16() );
        Assertions.assertEquals( "/img/icons/mortar-48.png", app.getIcons().getIcon48() );
        Assertions.assertEquals( "/img/icons/mortar-128.png", app.getIcons().getIcon128() );
    }

    @Test
    void testDeveloper()
    {
        Assertions.assertEquals( "Test Developer", app.getDeveloper().getName() );
        Assertions.assertEquals( "http://test", app.getDeveloper().getUrl() );
        Assertions.assertNull( app.getDeveloper().getEmail() );
        Assertions.assertNull( app.getDeveloper().getCompany() );
    }

    @Test
    void testActivities()
    {
        AppDhis dhisActivity = app.getActivities().getDhis();
        Assertions.assertEquals( "http://localhost:8080/dhis", dhisActivity.getHref() );
        dhisActivity.setHref( "ALL TEST" );
        Assertions.assertEquals( "ALL TEST", dhisActivity.getHref() );
    }

    @Test
    void testGetAuthorities()
    {
        Set<String> authorities = app.getAuthorities();
        Assertions.assertNotNull( authorities );
        Assertions.assertEquals( 4, authorities.size() );
    }

    @Test
    void testGetSeeAppAuthority()
    {
        assertEquals( "M_Test_App", app.getSeeAppAuthority() );
    }

    @Test
    void testGetUrlFriendlyName()
    {
        App appA = new App();
        appA.setName( "Org [Facility] &Registry@" );
        App appB = new App();
        appB.setName( null );
        assertEquals( "Org-Facility-Registry", appA.getUrlFriendlyName() );
        assertNull( appB.getUrlFriendlyName() );
    }

    @Test
    void testGetLaunchUrl()
    {
        Assertions.assertEquals( "https://example.com/api/apps/Test-App/index.html", app.getLaunchUrl() );
    }

    @Test
    void testGetPluginLaunchUrl()
    {
        Assertions.assertEquals( "https://example.com/api/apps/Test-App/plugin.html", app.getPluginLaunchUrl() );

        App appWithoutPlugin = new App();
        appWithoutPlugin.setName( "Test App" );
        appWithoutPlugin.setLaunchPath( "/index.html" );
        appWithoutPlugin.init( "https://example.com" );
        Assertions.assertEquals( "https://example.com/api/apps/Test-App/index.html", appWithoutPlugin.getLaunchUrl() );
        Assertions.assertEquals( null, appWithoutPlugin.getPluginLaunchUrl() );

    }
}
