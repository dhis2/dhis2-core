package org.hisp.dhis.dxf2.synch;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportSummaryResponseExtractor;
import org.hisp.dhis.dxf2.common.JacksonUtils;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.ImportService;
import org.hisp.dhis.dxf2.metadata.MetaData;
import org.hisp.dhis.setting.Setting;
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
    private ImportService importService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RestTemplate restTemplate;

    // -------------------------------------------------------------------------
    // SynchronizatonManager implementation
    // -------------------------------------------------------------------------

    @Override
    public AvailabilityStatus isRemoteServerAvailable()
    {
        Configuration config = configurationService.getConfiguration();

        if ( !isRemoteServerConfigured( config ) )
        {
            return new AvailabilityStatus( false, "Remote server is not configured" );
        }

        String url = config.getRemoteServerUrl() + PING_PATH;

        log.info( "Remote server ping URL: " + url + ", username: " + config.getRemoteServerUsername() );

        HttpEntity<String> request = getBasicAuthRequestEntity( config.getRemoteServerUsername(), config.getRemoteServerPassword() );

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
            return new AvailabilityStatus( false, "Network is unreachable" );
        }

        log.info( "Response: " + response + ", status code: " + sc );

        if ( HttpStatus.FOUND.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "Server is available but no authentication was provided, status code: " + sc );
        }
        else if ( HttpStatus.UNAUTHORIZED.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "Server is available but authentication failed, status code: " + sc );
        }
        else if ( HttpStatus.INTERNAL_SERVER_ERROR.equals( sc ) )
        {
            status = new AvailabilityStatus( false, "Server is available but experienced an internal error, status code: " + sc );
        }
        else if ( HttpStatus.OK.equals( sc ) )
        {
            status = new AvailabilityStatus( true, "Server is available and authentication was successful" );
        }
        else
        {
            status = new AvailabilityStatus( false, "Server is not available, status code: " + sc + ", text: " + st );
        }

        log.info( status );

        return status;
    }

    @Override
    public ImportSummary executeDataPush()
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
        final Date lastSuccessTime = getLastSynchSuccessFallback();

        int lastUpdatedCount = dataValueService.getDataValueCountLastUpdatedAfter( lastSuccessTime );

        log.info( "Values: " + lastUpdatedCount + " since last synch success: " + lastSuccessTime );

        if ( lastUpdatedCount == 0 )
        {
            log.info( "Skipping synch, no new or updated data values" );
            return null;
        }

        final Configuration config = configurationService.getConfiguration();

        String url = config.getRemoteServerUrl() + "/api/dataValueSets";

        log.info( "Remote server POST URL: " + url );

        final RequestCallback requestCallback = new RequestCallback()
        {
            @Override
            public void doWithRequest( ClientHttpRequest request ) throws IOException
            {
                request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
                request.getHeaders().add( HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( config.getRemoteServerUsername(), config.getRemoteServerPassword() ) );
                dataValueSetService.writeDataValueSetJson( lastSuccessTime, request.getBody(), new IdSchemes() );
            }
        };

        ResponseExtractor<ImportSummary> responseExtractor = new ImportSummaryResponseExtractor();

        ImportSummary summary = restTemplate.execute( url, HttpMethod.POST, requestCallback, responseExtractor );

        log.info( "Synch summary: " + summary );

        if ( summary != null && ImportStatus.SUCCESS.equals( summary.getStatus() ) )
        {
            setLastSynchSuccess( startTime );
            log.info( "Synch successful, setting last success time: " + startTime );
        }

        return summary;
    }

    @Override
    public Date getLastSynchSuccess()
    {
        return (Date) systemSettingManager.getSystemSetting( Setting.LAST_SUCCESSFUL_DATA_SYNC );
    }

    @Override
    public org.hisp.dhis.dxf2.metadata.ImportSummary executeMetadataPull( String url )
    {
        User user = currentUserService.getCurrentUser();
        
        String userUid = user != null ? user.getUid() : null;
        
        log.info( "Metadata pull, url: " + url + ", user: " + userUid );
        
        String json = restTemplate.getForObject( url, String.class );
        
        MetaData metaData = null;
        
        try
        {
            metaData = JacksonUtils.fromJson( json, MetaData.class );
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to parse remote JSON document", ex );
        }
        
        org.hisp.dhis.dxf2.metadata.ImportSummary summary = importService.importMetaData( userUid, metaData );
        
        return summary;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the time of the last successful synchronization operation. If not set,
     * the current date subtracted three days is returned.
     */
    private Date getLastSynchSuccessFallback()
    {
        Date fallback = new DateTime().minusDays( 3 ).toDate();

        return (Date) systemSettingManager.getSystemSetting( Setting.LAST_SUCCESSFUL_DATA_SYNC, fallback );
    }

    /**
     * Sets the time of the last successful synchronization operation.
     */
    private void setLastSynchSuccess( Date time )
    {
        systemSettingManager.saveSystemSetting( Setting.LAST_SUCCESSFUL_DATA_SYNC, time );
    }

    /**
     * Indicates whether a remote server has been properly configured.
     */
    private boolean isRemoteServerConfigured( Configuration config )
    {
        if ( trimToNull( config.getRemoteServerUrl() ) == null )
        {
            log.info( "Remote server URL not set" );
            return false;
        }

        if ( trimToNull( config.getRemoteServerUsername() ) == null || trimToNull( config.getRemoteServerPassword() ) == null )
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
