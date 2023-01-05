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
package org.hisp.dhis.apphub;

import static org.hisp.dhis.apphub.AppHubUtils.getJsonRequestEntity;
import static org.hisp.dhis.apphub.AppHubUtils.sanitizeQuery;
import static org.hisp.dhis.apphub.AppHubUtils.validateApiVersion;
import static org.hisp.dhis.apphub.AppHubUtils.validateQuery;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author Zubair Asghar
 * @author Lars Helge Overland
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAppHubService implements AppHubService
{
    private final RestTemplate restTemplate;

    private final AppManager appManager;

    private final DhisConfigurationProvider dhisConfigurationProvider;

    @Override
    public String getAppHubApiResponse( String apiVersion, String query )
        throws URISyntaxException
    {
        validateApiVersion( apiVersion );
        validateQuery( query );

        apiVersion = sanitizeQuery( apiVersion );
        query = sanitizeQuery( query );

        String appHubApiUrl = dhisConfigurationProvider.getProperty( ConfigurationKey.APPHUB_API_URL );

        log.info( "Get App Hub response, base URL: '{}', API version: '{}', query: '{}'", appHubApiUrl, apiVersion,
            query );

        String url = String.format( "%s/%s/%s", appHubApiUrl, apiVersion, query );

        log.info( "App Hub proxy request URL: '{}'", url );

        return restTemplate.exchange( new URI( url ), HttpMethod.GET, getJsonRequestEntity(), String.class ).getBody();
    }

    @Override
    public List<WebApp> getAppHub()
    {
        String appHubApiUrl = dhisConfigurationProvider.getProperty( ConfigurationKey.APPHUB_API_URL );
        String allAppsUrl = appHubApiUrl + "/apps";

        WebApp[] apps = restTemplate.getForObject( allAppsUrl, WebApp[].class );

        return Arrays.asList( apps );
    }

    @Override
    public AppStatus installAppFromAppHub( String id )
    {
        if ( id == null )
        {
            return AppStatus.NOT_FOUND;
        }

        try
        {
            Optional<AppVersion> webAppVersion = getWebAppVersion( id );

            if ( webAppVersion.isPresent() )
            {
                AppVersion version = webAppVersion.get();

                URL url = new URL( version.getDownloadUrl() );

                String filename = version.getFilename();

                return appManager.installApp( getFile( url ), filename );
            }

            log.info( String.format( "No version found for id %s", id ) );

            return AppStatus.NOT_FOUND;
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to install app", ex );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Optional<AppVersion> getWebAppVersion( String id )
    {
        for ( WebApp app : getAppHub() )
        {
            for ( AppVersion version : app.getVersions() )
            {
                if ( id.equals( version.getId() ) )
                {
                    return Optional.of( version );
                }
            }
        }

        return Optional.empty();
    }

    private static File getFile( URL url )
        throws IOException
    {
        URLConnection connection = url.openConnection();

        BufferedInputStream in = new BufferedInputStream( connection.getInputStream() );

        File tempFile = File.createTempFile( "dhis", null );

        tempFile.deleteOnExit();

        try ( FileOutputStream out = new FileOutputStream( tempFile ) )
        {
            IOUtils.copy( in, out );
        }
        return tempFile;
    }
}
