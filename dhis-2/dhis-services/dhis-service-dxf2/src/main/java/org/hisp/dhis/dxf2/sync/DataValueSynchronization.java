package org.hisp.dhis.dxf2.sync;
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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportSummaryResponseExtractor;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.dxf2.webmessage.utils.WebMessageParseUtils;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CodecUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak
 */
public class DataValueSynchronization
{
    private static final Log log = LogFactory.getLog( DataValueSynchronization.class );

    private final DataValueService dataValueService;

    private final DataValueSetService dataValueSetService;

    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    @Autowired
    public DataValueSynchronization( DataValueService dataValueService, DataValueSetService dataValueSetService,
        SystemSettingManager systemSettingManager, RestTemplate restTemplate )
    {
        this.dataValueService = dataValueService;
        this.dataValueSetService = dataValueSetService;
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
    }

    public SynchronizationResult syncDataValuesData()
    {
        if ( !SyncUtils.testServerAvailability( systemSettingManager, restTemplate ).isAvailable() )
        {
            return SynchronizationResult
                .newFailureResultWithMessage( "DataValueSynchronization failed. Remote server is unavailable." );
        }

        log.info( "Starting DataValueSynchronization job." );

        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------

        final Clock clock = new Clock( log ).startClock().logTime( "Starting DataValueSynchronization job" );
        final Date lastSuccessTime = SyncUtils.getLastSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC );
        final Date skipChangedBefore = (Date) systemSettingManager.getSystemSetting( SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE );

        final int objectsToSynchronize = dataValueService.getDataValueCountLastUpdatedAndChangedAfter( lastSuccessTime, skipChangedBefore, true );

        log.info( "DataValues last changed before " + skipChangedBefore + " will not be synchronized." );

        if ( objectsToSynchronize == 0 )
        {
            log.info( "Skipping synchronization, no new or updated DataValues" );
            return SynchronizationResult
                .newSuccessResultWithMessage( "Skipping synchronization, no new or updated DataValues" );
        }

        final String syncUrl = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL )
            + SyncEndpoint.DATA_VALUE_SETS.getPath();
        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
        final SystemInstance instance = new SystemInstance( syncUrl, username, password );

        final int pageSize = (int) systemSettingManager.getSystemSetting( SettingKey.DATA_VALUES_SYNC_PAGE_SIZE );

        // Using this approach as (int) Match.ceil doesn't work until I cast int to double
        final int pages = (objectsToSynchronize / pageSize) + ((objectsToSynchronize % pageSize == 0) ? 0 : 1);

        log.info( objectsToSynchronize + " DataValues to synchronize were found." );
        log.info( "Remote server URL for DataValues POST sync: " + instance.getUrl() );
        log.info( "DataValueSynchronization job has " + pages + " pages to sync. With page size: " + pageSize );

        boolean syncResult = true;

        for ( int i = 1; i <= pages; i++ )
        {
            log.info( String.format( "Synchronizing page %d with page size %d", i, pageSize ) );

            if ( !sendDataValueSyncRequest( instance, lastSuccessTime, skipChangedBefore, pageSize, i, SyncEndpoint.DATA_VALUE_SETS ) )
            {
                syncResult = false;
            }
        }

        if ( syncResult )
        {
            clock.logTime( "SUCCESS! DataValueSynchronization job is done. It took" );
            SyncUtils.setLastSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_DATA_VALUE_SYNC, new Date( clock.getStartTime() ));
            return SynchronizationResult
                .newSuccessResultWithMessage( "DataValueSynchronization done. It took " + clock.getTime() + " ms." );
        }

        return SynchronizationResult.newFailureResultWithMessage( "DataValueSynchronization failed." );
    }

    private boolean sendDataValueSyncRequest( SystemInstance instance, Date lastSuccessTime, Date skipChangedBefore,
        int syncPageSize, int page, SyncEndpoint endpoint )
    {
        final RequestCallback requestCallback = request -> {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION,
                CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );

            dataValueSetService.writeDataValueSetJson( lastSuccessTime, skipChangedBefore, request.getBody(),
                new IdSchemes(), syncPageSize, page );
        };

        final int maxSyncAttempts = (int) systemSettingManager.getSystemSetting( SettingKey.MAX_SYNC_ATTEMPTS );

        boolean networkErrorOccurred = true;
        int syncAttemptsDone = 0;

        ResponseExtractor<ImportSummary> responseExtractor = new ImportSummaryResponseExtractor();
        ImportSummary summary = null;

        while ( networkErrorOccurred )
        {
            networkErrorOccurred = false;
            syncAttemptsDone++;
            try
            {
                summary = restTemplate.execute( instance.getUrl(), HttpMethod.POST, requestCallback,
                    responseExtractor );
            }
            catch ( HttpClientErrorException ex )
            {
                String responseBody = ex.getResponseBodyAsString();
                try
                {
                    summary = WebMessageParseUtils.fromWebMessageResponse( responseBody, ImportSummary.class );
                }
                catch ( WebMessageParseException e )
                {
                    log.error( "Parsing WebMessageResponse failed.", e );
                    return false;
                }
            }
            catch ( HttpServerErrorException ex )
            {
                String responseBody = ex.getResponseBodyAsString();
                log.error( "Internal error happened during DataValues push: " + responseBody, ex );

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
                log.error( "Exception during DataValues data push: " + ex.getMessage(), ex );
                throw ex;
            }
        }

        log.info( "Sync summary: " + summary );

        return SyncUtils.checkSummaryStatus( summary, endpoint );
    }
}wan