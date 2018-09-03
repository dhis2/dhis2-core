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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David Katuscak
 */
public class TrackerSynchronization
{
    private static final Log log = LogFactory.getLog( TrackerSynchronization.class );

    private final TrackedEntityInstanceService teiService;

    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    private final RenderService renderService;

    @Autowired
    public TrackerSynchronization( TrackedEntityInstanceService teiService, SystemSettingManager systemSettingManager, RestTemplate restTemplate, RenderService renderService )
    {
        this.teiService = teiService;
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.renderService = renderService;
    }

    public SynchronizationResult syncTrackerProgramData()
    {
        if ( !SyncUtils.testServerAvailability( systemSettingManager, restTemplate ).isAvailable() )
        {
            return SynchronizationResult.newFailureResultWithMessage( "Tracker synchronization failed. Remote server is unavailable." );
        }

        final Clock clock = new Clock( log ).startClock().logTime( "Starting Tracker program data synchronization job." );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setIncludeDeleted( true );
        queryParams.setSynchronizationQuery( true );

        final int objectsToSynchronize = teiService.getTrackedEntityInstanceCount( queryParams, true, true );

        if ( objectsToSynchronize == 0 )
        {
            log.info( "Skipping synchronization. No new tracker data to synchronize were found." );
            return SynchronizationResult.newSuccessResultWithMessage( "Tracker synchronization skipped. No new or updated events found." );
        }

        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
        final String syncUrl = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + SyncEndpoint.TRACKED_ENTITY_INSTANCES.getPath() + SyncUtils.IMPORT_STRATEGY_SYNC_SUFFIX;
        final SystemInstance instance = new SystemInstance( syncUrl, username, password );

        final int pageSize = (int) systemSettingManager.getSystemSetting( SettingKey.TRACKER_SYNC_PAGE_SIZE );
        final int pages = (objectsToSynchronize / pageSize) + ((objectsToSynchronize % pageSize == 0) ? 0 : 1);  //Have to use this as (int) Match.ceil doesn't work until I am casting int to double

        log.info( objectsToSynchronize + " TEIs to sync were found." );
        log.info( "Remote server URL for Tracker POST synchronization: " + syncUrl );
        log.info( "Tracker synchronization job has " + pages + " pages to synchronize. With page size: " + pageSize );

        queryParams.setPageSize( pageSize );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.DATA_SYNCHRONIZATION;
        boolean syncResult = true;

        for ( int i = 1; i <= pages; i++ )
        {
            queryParams.setPage( i );

            List<TrackedEntityInstance> dtoTeis = teiService.getTrackedEntityInstances( queryParams, params, true );
            log.info( String.format( "Synchronizing page %d with page size %d", i, pageSize ) );

            if ( log.isDebugEnabled() )
            {
                log.debug( "TEIs that are going to be synchronized are: " + dtoTeis );
            }

            if ( sendTrackerSyncRequest( dtoTeis, instance ) )
            {
                List<String> teiUIDs = dtoTeis.stream()
                    .map( TrackedEntityInstance::getTrackedEntityInstance )
                    .collect( Collectors.toList() );
                log.info( "The lastSynchronized flag of these TEIs will be updated: " + teiUIDs );
                teiService.updateTrackedEntityInstancesSyncTimestamp( teiUIDs, new Date( clock.getStartTime() ) );
            }
            else
            {
                syncResult = false;
            }
        }

        if ( syncResult )
        {
            clock.logTime( "SUCCESS! Tracker synchronization was successfully done! It took " );
            return SynchronizationResult.newSuccessResultWithMessage( "Tracker synchronization done. It took " + clock.getTime() + " ms." );
        }
        return SynchronizationResult.newFailureResultWithMessage( "Tracker synchronization failed." );
    }


    private boolean sendTrackerSyncRequest( List<TrackedEntityInstance> dtoTeis, SystemInstance instance )
    {
        TrackedEntityInstances teis = new TrackedEntityInstances();
        teis.setTrackedEntityInstances( dtoTeis );

        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );
            renderService.toJson( request.getBody(), teis );
        };

        return SyncUtils.sendSyncRequest( systemSettingManager, restTemplate, requestCallback, instance, SyncEndpoint.TRACKED_ENTITY_INSTANCES );
    }
}
