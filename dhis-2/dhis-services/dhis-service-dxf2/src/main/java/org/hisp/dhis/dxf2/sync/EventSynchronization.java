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
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
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
public class EventSynchronization
{
    private static final Log log = LogFactory.getLog( EventSynchronization.class );

    private final EventService eventService;

    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    private final RenderService renderService;

    @Autowired
    public EventSynchronization( EventService eventService, SystemSettingManager systemSettingManager, RestTemplate restTemplate, RenderService renderService )
    {
        this.eventService = eventService;
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.renderService = renderService;
    }

    public void syncEventProgramData()
    {
        if ( !SyncUtils.testServerAvailability( systemSettingManager, restTemplate ).isAvailable() )
        {
            return;
        }

        log.info( "Starting anonymous event program data synchronization job." );

        // ---------------------------------------------------------------------
        // Set time for last success to start of process to make data saved
        // subsequently part of next synch process without being ignored
        // ---------------------------------------------------------------------

        final Date startTime = new Date();
        final int objectsToSync = eventService.getAnonymousEventReadyForSynchronizationCount();

        if ( objectsToSync == 0 )
        {
            log.info( "Skipping sync, no new or updated events found" );
            return;
        }

        log.info( objectsToSync + " anonymous Events to sync were found." );

        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
        final int eventSyncPageSize = (int) systemSettingManager.getSystemSetting( SettingKey.EVENT_SYNC_PAGE_SIZE );
        final int pages = (objectsToSync / eventSyncPageSize) + ((objectsToSync % eventSyncPageSize == 0) ? 0 : 1);  //Have to use this as (int) Match.ceil doesn't work until I am casting int to double
        final String syncUrl = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + SyncEndpoint.EVENTS_ENDPOINT.getPath() + SyncUtils.IMPORT_STRATEGY_SYNC_SUFFIX;

        log.info( "Remote server URL for Events POST sync: " + syncUrl );
        log.info( "Events sync job has " + pages + " pages to sync. With page size: " + eventSyncPageSize );

        boolean syncResult = true;

        for ( int i = 1; i <= pages; i++ )
        {
            Events events = eventService.getAnonymousEventsForSync( eventSyncPageSize );
            filterOutDataValuesMarkedWithSkipSynchronizationFlag( events );
            log.info( String.format( "Syncing page %d, page size is: %d", i, eventSyncPageSize ) );

            if ( log.isDebugEnabled() )
            {
                log.debug( "Events that are going to be synced are: " + events );
            }

            if ( sendEventsSyncRequest( events, username, password ) )
            {
                List<String> eventsUIDs = events.getEvents().stream()
                    .map( Event::getEvent )
                    .collect( Collectors.toList() );
                log.info( "The lastSynchronized flag of these Events will be updated: " + eventsUIDs );
                eventService.updateEventsSyncTimestamp( eventsUIDs, startTime );
            }
            else
            {
                syncResult = false;
            }
        }

        if ( syncResult )
        {
            long syncDuration = System.currentTimeMillis() - startTime.getTime();
            log.info( "SUCCESS! Events sync was successfully done! It took " + syncDuration + " ms." );
        }
    }

    private void filterOutDataValuesMarkedWithSkipSynchronizationFlag( Events events )
    {
        for ( Event event : events.getEvents() )
        {
            event.setDataValues(
                event.getDataValues().stream()
                    .filter( dv -> !dv.isSkipSynchronization() )
                    .collect( Collectors.toList() )
            );
        }
    }

    private boolean sendEventsSyncRequest( Events events, String username, String password )
    {
        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
            renderService.toJson( request.getBody(), events );
        };

        return SyncUtils.sendSyncRequest( systemSettingManager, restTemplate, requestCallback, SyncEndpoint.EVENTS_ENDPOINT );
    }
}
