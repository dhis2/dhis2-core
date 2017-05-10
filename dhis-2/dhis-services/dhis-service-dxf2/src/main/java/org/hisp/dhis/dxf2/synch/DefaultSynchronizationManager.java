package org.hisp.dhis.dxf2.synch;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportSummariesResponseExtractor;
import org.hisp.dhis.dxf2.common.ImportSummaryResponseExtractor;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.Metadata;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.webmessage.utils.WebMessageParseUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Lars Helge Overland
 */
public class DefaultSynchronizationManager
    implements SynchronizationManager
{
    private static final Log log = LogFactory.getLog( DefaultSynchronizationManager.class );

    private static final String PING_PATH = "/api/system/ping";

    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MetadataImportService importService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EventService eventService;

    @Autowired
    private RenderService renderService;

    // -------------------------------------------------------------------------
    // SynchronizatonManager implementation
    // -------------------------------------------------------------------------

    @Override
    public AvailabilityStatus isRemoteServerAvailable()
    {
        Configuration config = configurationService.getConfiguration();

        if ( !isRemoteServerConfigured( config ) )
        {
            return new AvailabilityStatus( false, "Remote server is not configured", HttpStatus.BAD_GATEWAY );
        }

        String url = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + PING_PATH;
        String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

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
        catch ( HttpClientErrorException ex )
        {
            sc = ex.getStatusCode();
            st = ex.getStatusText();
        }
        catch ( HttpServerErrorException ex )
        {
            sc = ex.getStatusCode();
            st = ex.getStatusText();
        }
        catch ( ResourceAccessException ex )
        {
            return new AvailabilityStatus( false, "Network is unreachable", HttpStatus.BAD_GATEWAY );
        }

        log.debug( "Response status code: " + sc );

        if ( HttpStatus.FOUND.equals( sc ) )
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
        else if ( HttpStatus.OK.equals( sc ) )
        {
            status = new AvailabilityStatus( true, "Authentication was successful", sc );
        }
        else
        {
            status = new AvailabilityStatus( false, "Server is not available: " + st, sc );
        }

        log.info( "Status: " + status );

        return status;
    }

    @Override
    public ImportSummary executeDataPush() throws WebMessageParseException
    {
        AvailabilityStatus availability = isRemoteServerAvailable();

        if ( !availability.isAvailable() )
        {
            log.info( "Aborting synch, server not available" );
            return null;
        }

        String url = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + "/api/dataValueSets";
        String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        SystemInstance instance = new SystemInstance( url, username, password );

        return executeDataPush( instance );
    }

    /**
     * Executes a push of data values to the given remote instance.
     *
     * @param instance the remote system instance.
     * @return an ImportSummary.
     */
    private ImportSummary executeDataPush( SystemInstance instance ) throws WebMessageParseException
    {
        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------

        final Date startTime = new Date();
        final Date lastSuccessTime = getLastDataSynchSuccessFallback();

        final int lastUpdatedCount = dataValueService.getDataValueCountLastUpdatedAfter( lastSuccessTime, true );

        log.info( "Values: " + lastUpdatedCount + " since last synch success: " + lastSuccessTime );

        if ( lastUpdatedCount == 0 )
        {
            log.debug( "Skipping synch, no new or updated data values" );
            return null;
        }

        log.info( "Values: " + lastUpdatedCount + " since last synch success: " + lastSuccessTime );

        log.info( "Remote server POST URL: " + instance.getUrl() );

        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );

            dataValueSetService.writeDataValueSetJson( lastSuccessTime, request.getBody(), new IdSchemes() );
        };

        ResponseExtractor<ImportSummary> responseExtractor = new ImportSummaryResponseExtractor();
        ImportSummary summary = null;
        try
        {
            summary = restTemplate
                .execute( instance.getUrl(), HttpMethod.POST, requestCallback, responseExtractor );
        }

        catch ( HttpClientErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            summary = WebMessageParseUtils.fromWebMessageResponse( responseBody, ImportSummary.class );
        }
        catch ( HttpServerErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            log.error( "Internal error happened during event data push: " + responseBody, ex );
            throw ex;
        }
        catch ( ResourceAccessException ex )
        {
            log.error( "Exception during event data push: " + ex.getMessage(), ex );
            throw ex;
        }

        log.info( "Synch summary: " + summary );

        if ( summary != null && ImportStatus.SUCCESS.equals( summary.getStatus() ) )
        {
            setLastDataSynchSuccess( startTime );
            log.info( "Synch successful, setting last success time: " + startTime );
        }
        else
        {
            log.warn( "Sync failed: " + summary );
        }

        return summary;
    }

    @Override
    public ImportSummaries executeEventPush() throws WebMessageParseException
    {
        AvailabilityStatus availability = isRemoteServerAvailable();

        if ( !availability.isAvailable() )
        {
            log.info( "Aborting synch, server not available" );
            return null;
        }

        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------

        final Date startTime = new Date();
        final Date lastSuccessTime = getLastEventSynchSuccessFallback();

        int lastUpdatedEventsCount = eventService.getAnonymousEventValuesCountLastUpdatedAfter( lastSuccessTime );

        log.info( "Events: " + lastUpdatedEventsCount + " since last synch success: " + lastSuccessTime );

        if ( lastUpdatedEventsCount == 0 )
        {
            log.info( "Skipping synch, no new or updated data values for events" );
            return null;
        }

        String url = systemSettingManager.getSystemSetting(
            SettingKey.REMOTE_INSTANCE_URL ) + "/api/events";

        log.info( "Remote server events POST URL: " + url );

        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        final RequestCallback requestCallback = new RequestCallback()
        {
            @Override
            public void doWithRequest( ClientHttpRequest request )
                throws IOException
            {
                request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
                request.getHeaders().add( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
                Events result = eventService.getAnonymousEventValuesLastUpdatedAfter( lastSuccessTime );
                renderService.toJson( request.getBody(), result );
            }
        };

        ResponseExtractor<ImportSummaries> responseExtractor = new ImportSummariesResponseExtractor();
        ImportSummaries summaries = null;
        try
        {
            summaries = restTemplate.execute( url, HttpMethod.POST, requestCallback, responseExtractor );
        }
        catch ( HttpClientErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            summaries = WebMessageParseUtils.fromWebMessageResponse( responseBody, ImportSummaries.class );
        }
        catch ( HttpServerErrorException ex )
        {
            String responseBody = ex.getResponseBodyAsString();
            log.error( "Internal error happened during event data push: " + responseBody, ex );
            throw ex;
        }
        catch ( ResourceAccessException ex )
        {
            log.error( "Exception during event data push: " + ex.getMessage(), ex );
            throw ex;
        }

        log.info( "Event synch summary: " + summaries );
        boolean isError = false;

        if ( summaries != null )
        {

            for ( ImportSummary summary : summaries.getImportSummaries() )
            {
                if ( ImportStatus.ERROR.equals( summary.getStatus() ) || ImportStatus.WARNING.equals( summary.getStatus() ) )
                {
                    isError = true;
                    log.debug( "Sync failed: " + summaries );
                    break;
                }
            }
        }

        if ( !isError )
        {
            setLastEventSynchSuccess( startTime );
            log.info( "Synch successful, setting last success time: " + startTime );
        }

        return summaries;
    }

    @Override
    public Date getLastDataSynchSuccess()
    {
        return (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_SYNC );
    }

    @Override
    public Date getLastEventSynchSuccess()
    {
        return (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_EVENT_DATA_SYNC );
    }

    @Override
    public ImportReport executeMetadataPull( String url )
    {
        User user = currentUserService.getCurrentUser();

        String userUid = user != null ? user.getUid() : null;

        log.info( String.format( "Metadata pull, url: %s, user: %s", url, userUid ) );

        String json = restTemplate.getForObject( url, String.class );

        Metadata metadata = null;

        try
        {
            metadata = DefaultRenderService.getJsonMapper().readValue( json, Metadata.class );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to parse remote JSON document", ex );
        }

        MetadataImportParams importParams = new MetadataImportParams();
        importParams.setSkipSharing( true );
        importParams.setAtomicMode( AtomicMode.NONE );
        importParams.addMetadata( schemaService.getMetadataSchemas(), metadata );

        return importService.importMetadata( importParams );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the time of the last successful data synchronization operation. If not set,
     * the current date subtracted by three days is returned.
     */
    private Date getLastDataSynchSuccessFallback()
    {
        Date fallback = new DateTime().minusDays( 3 ).toDate();

        return (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_SYNC, fallback );
    }

    /**
     * Gets the time of the last successful event synchronization operation. If not set,
     * the current date subtracted by three days is returned.
     */
    private Date getLastEventSynchSuccessFallback()
    {
        Date fallback = new DateTime().minusDays( 3 ).toDate();

        return (Date) systemSettingManager.getSystemSetting( SettingKey.LAST_SUCCESSFUL_EVENT_DATA_SYNC, fallback );
    }

    /**
     * Sets the time of the last successful data synchronization operation.
     */
    private void setLastDataSynchSuccess( Date time )
    {
        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_DATA_SYNC, time );
    }

    /**
     * Sets the time of the last successful event synchronization operation.
     */
    private void setLastEventSynchSuccess( Date time )
    {
        systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_EVENT_DATA_SYNC, time );
    }

    /**
     * Indicates whether a remote server has been properly configured.
     */
    private boolean isRemoteServerConfigured( Configuration config )
    {
        String url = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL );
        String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

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
    private <T> HttpEntity<T> getBasicAuthRequestEntity( String username, String password )
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
        return new HttpEntity<>( headers );
    }
}
