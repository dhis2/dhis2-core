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
package org.hisp.dhis.dxf2.sync;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Date;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.common.ImportSummariesResponseExtractor;
import org.hisp.dhis.dxf2.common.ImportSummaryResponseExtractor;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.dxf2.webmessage.utils.WebMessageParseUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Slf4j
public class SyncUtils
{
    static final String HEADER_AUTHORIZATION = "Authorization";

    static final String IMPORT_STRATEGY_SYNC_SUFFIX = "?strategy=SYNC";

    private static final String PING_PATH = "/api/system/ping";

    private SyncUtils()
    {
    }

    /**
     * Sends a synchronization request to the {@code syncUrl} and analyzes the
     * returned summary
     *
     * @param systemSettingManager Reference to SystemSettingManager
     * @param restTemplate Spring Rest Template instance
     * @param requestCallback Request callback
     * @param instance SystemInstance of remote system
     * @param endpoint Endpoint against which the sync request is run
     * @return True if sync was successful, false otherwise
     */
    static boolean sendSyncRequest( SystemSettingManager systemSettingManager, RestTemplate restTemplate,
        RequestCallback requestCallback, SystemInstance instance, SyncEndpoint endpoint )
    {
        final int maxSyncAttempts = systemSettingManager.getIntSetting( SettingKey.MAX_SYNC_ATTEMPTS );
        Optional<AbstractWebMessageResponse> responseSummaries = runSyncRequest( restTemplate, requestCallback,
            endpoint.getKlass(), instance.getUrl(), maxSyncAttempts );

        if ( responseSummaries.isPresent() )
        {
            if ( ImportSummaries.class.isAssignableFrom( endpoint.getKlass() ) )
            {
                return analyzeResultsInImportSummaries( (ImportSummaries) responseSummaries.get(),
                    (ImportSummaries) responseSummaries.get(), endpoint );
            }
            else if ( ImportSummary.class.isAssignableFrom( endpoint.getKlass() ) )
            {
                return checkSummaryStatus( (ImportSummary) responseSummaries.get(), endpoint );
            }
        }

        return false;
    }

    public static Optional<AbstractWebMessageResponse> runSyncRequest( RestTemplate restTemplate,
        RequestCallback requestCallback, Class<? extends AbstractWebMessageResponse> klass, String syncUrl,
        int maxSyncAttempts )
    {
        boolean networkErrorOccurred = true;
        int syncAttemptsDone = 0;

        ResponseExtractor<? extends AbstractWebMessageResponse> responseExtractor = getResponseExtractor( klass );
        AbstractWebMessageResponse responseSummary = null;

        while ( networkErrorOccurred )
        {
            networkErrorOccurred = false;
            syncAttemptsDone++;
            try
            {
                responseSummary = restTemplate.execute( syncUrl, HttpMethod.POST, requestCallback, responseExtractor );
            }
            catch ( HttpClientErrorException ex )
            {
                String responseBody = ex.getResponseBodyAsString();
                try
                {
                    responseSummary = WebMessageParseUtils.fromWebMessageResponse( responseBody, klass );
                }
                catch ( WebMessageParseException e )
                {
                    log.error( "Parsing WebMessageResponse failed.", e );
                    return Optional.empty();
                }
            }
            catch ( HttpServerErrorException ex )
            {
                String responseBody = ex.getResponseBodyAsString();
                log.error( "Internal error happened during event data push: " + responseBody, ex );

                if ( syncAttemptsDone <= maxSyncAttempts )
                {
                    networkErrorOccurred = true;
                }
                else
                {
                    throw ex;
                }
            }
            catch ( ResourceAccessException ex )
            {
                log.error( "Exception during event data push: " + ex.getMessage(), ex );
                throw ex;
            }
        }

        log.info( "Sync summary: " + responseSummary );
        return Optional.ofNullable( responseSummary );
    }

    private static ResponseExtractor<? extends AbstractWebMessageResponse> getResponseExtractor(
        Class<? extends AbstractWebMessageResponse> klass )
    {
        if ( ImportSummaries.class.isAssignableFrom( klass ) )
        {
            return new ImportSummariesResponseExtractor();
        }
        else if ( ImportSummary.class.isAssignableFrom( klass ) )
        {
            return new ImportSummaryResponseExtractor();
        }
        else
        {
            throw new IllegalStateException( "ResponseExtractor for given class '" + klass + "' is not supported." );
        }
    }

    /**
     * Analyzes results in ImportSummaries. Returns true if everything is OK,
     * false otherwise.
     * <p>
     * THIS METHOD USES RECURSION!!!
     *
     * @param summaries ImportSummaries that should be analyzed
     * @param originalTopSummaries The top level ImportSummaries. Used only for
     *        logging purposes.
     * @param endpoint Specifies against which endpoint the request was run
     * @return true if everything is OK, false otherwise
     */
    private static boolean analyzeResultsInImportSummaries( ImportSummaries summaries,
        ImportSummaries originalTopSummaries, SyncEndpoint endpoint )
    {
        if ( summaries != null )
        {
            for ( ImportSummary summary : summaries.getImportSummaries() )
            {
                if ( !checkSummaryStatus( summary, summaries, originalTopSummaries, endpoint ) )
                {
                    return false;
                }

                // I need recursively check for errors on lower levels of the
                // graph
                if ( endpoint == SyncEndpoint.TRACKED_ENTITY_INSTANCES )
                {
                    // Uses recursion. Correct value of endpoint argument is
                    // critical here.
                    if ( !analyzeResultsInImportSummaries( summary.getEnrollments(), originalTopSummaries,
                        SyncEndpoint.ENROLLMENTS ) )
                    {
                        return false;
                    }
                }
                else if ( endpoint == SyncEndpoint.ENROLLMENTS )
                {
                    // Uses recursion. Correct value of endpoint argument is
                    // critical here.
                    if ( !analyzeResultsInImportSummaries( summary.getEvents(), originalTopSummaries,
                        SyncEndpoint.EVENTS ) )
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Checks the ImportSummary. Returns true if everything is OK, false
     * otherwise
     *
     * @param summary ImportSummary that are checked for error/warning
     * @param topSummaries References to the ImportSummaries from top level of
     *        the graph (Used to create proper log message)
     * @param summaries References to the ImportSummaries 1 level above (Used to
     *        create proper log message)
     * @param endpoint Specifies against which endpoint the request was run
     * @return true if everything is OK, false otherwise
     */
    private static boolean checkSummaryStatus( ImportSummary summary, ImportSummaries summaries,
        ImportSummaries topSummaries, SyncEndpoint endpoint )
    {
        if ( summary.getStatus() == ImportStatus.ERROR || summary.getStatus() == ImportStatus.WARNING )
        {
            log.error( "Sync against endpoint: " + endpoint.name() + " failed: ImportSummaries: " + summaries
                + " |########| Top ImportSummaries: " + topSummaries );
            return false;
        }

        return true;
    }

    /**
     * Checks the availability of remote server.
     *
     * @param systemSettingManager Reference to SystemSettingManager
     * @param restTemplate Reference to RestTemplate
     * @return AvailabilityStatus that says whether the server is available or
     *         not
     */
    static AvailabilityStatus testServerAvailability( SystemSettingManager systemSettingManager,
        RestTemplate restTemplate )
    {
        final int maxAttempts = systemSettingManager
            .getIntSetting( SettingKey.MAX_REMOTE_SERVER_AVAILABILITY_CHECK_ATTEMPTS );
        final int delayBetweenAttempts = systemSettingManager
            .getIntSetting( SettingKey.DELAY_BETWEEN_REMOTE_SERVER_AVAILABILITY_CHECK_ATTEMPTS );

        return SyncUtils.testServerAvailabilityWithRetries(
            systemSettingManager,
            restTemplate,
            maxAttempts,
            delayBetweenAttempts );
    }

    /**
     * Checks the availability of remote server. In case of error it tries
     * {@code maxAttempts} of time with a {@code delaybetweenAttempts} delay
     * between retries before giving up.
     *
     * @param systemSettingManager Reference to SystemSettingManager
     * @param restTemplate Reference to RestTemplate
     * @param maxAttempts Specifies how many retries are done in case of error
     * @param delayBetweenAttempts Specifies delay between retries
     * @return AvailabilityStatus that says whether the server is available or
     *         not
     */
    private static AvailabilityStatus testServerAvailabilityWithRetries( SystemSettingManager systemSettingManager,
        RestTemplate restTemplate, int maxAttempts, long delayBetweenAttempts )
    {
        AvailabilityStatus serverStatus = isRemoteServerAvailable( systemSettingManager, restTemplate );

        for ( int i = 1; i < maxAttempts; i++ )
        {
            if ( serverStatus.isAvailable() )
            {
                return serverStatus;
            }

            try
            {
                log.info( "Remote server is not available. Retry #" + i + " in " + delayBetweenAttempts + " ms." );
                Thread.sleep( delayBetweenAttempts );
            }
            catch ( InterruptedException e )
            {
                log.error( "Sleep between sync retries failed.", e );
                Thread.currentThread().interrupt();
            }

            serverStatus = isRemoteServerAvailable( systemSettingManager, restTemplate );
        }

        log.error( "Remote server is not available. Details: " + serverStatus );
        return serverStatus;
    }

    /**
     * Checks the availability of remote server
     *
     * @param systemSettingManager Reference to SystemSettingManager
     * @param restTemplate Reference to RestTemplate
     * @return AvailabilityStatus that says whether the server is available or
     *         not
     */
    public static AvailabilityStatus isRemoteServerAvailable( SystemSettingManager systemSettingManager,
        RestTemplate restTemplate )
    {
        if ( !isRemoteServerConfigured( systemSettingManager ) )
        {
            return new AvailabilityStatus( false, "Remote server is not configured", HttpStatus.BAD_GATEWAY );
        }

        String url = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_URL ) + PING_PATH;
        String username = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        log.debug( String.format( "Remote server ping URL: %s, username: %s", url, username ) );

        HttpEntity<String> request = getBasicAuthRequestEntity( username, password );

        ResponseEntity<String> response = null;
        HttpStatus sc = null;
        String st = null;
        AvailabilityStatus status = null;

        try
        {
            response = restTemplate.exchange( url, HttpMethod.GET, request, String.class );
            sc = response.getStatusCode();
        }
        catch ( HttpClientErrorException | HttpServerErrorException ex )
        {
            sc = ex.getStatusCode();
            st = ex.getStatusText();
        }
        catch ( ResourceAccessException ex )
        {
            return new AvailabilityStatus( false, "Network is unreachable", HttpStatus.BAD_GATEWAY );
        }

        log.debug( "Response status code: " + sc );

        if ( HttpStatus.OK.equals( sc ) )
        {
            status = new AvailabilityStatus( true, "Authentication was successful", sc );
        }
        else if ( HttpStatus.FOUND.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "No authentication was provided", sc );
        }
        else if ( HttpStatus.UNAUTHORIZED.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "Authentication failed", sc );
        }
        else if ( HttpStatus.INTERNAL_SERVER_ERROR.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "Remote server experienced an internal error", sc );
        }
        else
        {
            status = new AvailabilityStatus( false, "Server is not available: " + st, sc );
        }

        log.info( "Status: " + status );

        return status;
    }

    /**
     * Indicates whether a remote server has been properly configured.
     */
    private static boolean isRemoteServerConfigured( SystemSettingManager systemSettingManager )
    {
        String url = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_URL );
        String username = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = systemSettingManager.getStringSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        if ( isEmpty( url ) )
        {
            log.info( "Remote server URL not set" );
            return false;
        }

        if ( isEmpty( username ) || isEmpty( password ) )
        {
            log.info( "Remote server username or password not set" );
            return false;
        }

        return true;
    }

    /**
     * Creates an HTTP entity for requests with appropriate header for basic
     * authentication.
     */
    private static <T> HttpEntity<T> getBasicAuthRequestEntity( String username, String password )
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
        return new HttpEntity<>( headers );
    }

    /**
     * Sets the time of the last successful synchronization operation for given
     * settingKey.
     *
     * @param systemSettingManager SystemSettingManager
     * @param settingKey SettingKey specifying the sync operation that was
     *        successfully done
     * @param time The date and time of last successful sync
     */
    public static void setLastSyncSuccess( SystemSettingManager systemSettingManager, SettingKey settingKey, Date time )
    {
        systemSettingManager.saveSystemSetting( settingKey, time );
    }

    /**
     * Return the time of last successful synchronization operation for given
     * settingKey.
     *
     * @param systemSettingManager Reference to SystemSettingManager
     * @param settingKey SettingKey specifying the sync operation that was
     *        successfully done
     * @return The date and time of last successful sync
     */
    public static Date getLastSyncSuccess( SystemSettingManager systemSettingManager, SettingKey settingKey )
    {
        return systemSettingManager.getDateSetting( settingKey );
    }

    /**
     * Checks the status of given importSummary and returns true if fine. False
     * otherwise.
     *
     * @param summary ImportSummary to check
     * @param endpoint Endpoint against which the sync was run
     * @return true if sync was successful, false otherwise
     */
    private static boolean checkSummaryStatus( ImportSummary summary, SyncEndpoint endpoint )
    {
        if ( summary.getStatus() == ImportStatus.ERROR || summary.getStatus() == ImportStatus.WARNING )
        {
            log.error( "Sync against endpoint " + endpoint.name() + " failed. ImportSummary: " + summary );
            return false;
        }

        return true;
    }

    static SystemInstance getRemoteInstance( SystemSettingManager settings, SyncEndpoint endpoint )
    {
        String username = settings.getStringSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = settings.getStringSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
        String syncUrl = settings.getStringSetting( SettingKey.REMOTE_INSTANCE_URL ) + endpoint.getPath();

        return new SystemInstance( syncUrl, username, password );
    }

    static SystemInstance getRemoteInstanceWithSyncImportStrategy( SystemSettingManager settings,
        SyncEndpoint syncEndpoint )
    {
        SystemInstance systemInstance = getRemoteInstance( settings, syncEndpoint );
        systemInstance.setUrl( systemInstance.getUrl() + SyncUtils.IMPORT_STRATEGY_SYNC_SUFFIX );

        return systemInstance;
    }
}
