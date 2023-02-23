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

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM;
import static org.hisp.dhis.setting.SettingKey.SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.synch.SystemInstance;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 * @author Jan Bernitt (job progress tracking refactoring)
 */
@Slf4j
@Component
@AllArgsConstructor
public class TrackerSynchronization implements DataSynchronizationWithPaging
{
    private final TrackedEntityInstanceService teiService;

    private final SystemSettingManager settings;

    private final RestTemplate restTemplate;

    private final RenderService renderService;

    @Override
    public SynchronizationResult synchronizeData( final int pageSize, JobProgress progress )
    {
        progress.startingProcess( "Starting Tracker programs data synchronization job." );
        if ( !SyncUtils.testServerAvailability( settings, restTemplate ).isAvailable() )
        {
            String msg = "Tracker programs data synchronization failed. Remote server is unavailable.";
            progress.failedProcess( msg );
            return SynchronizationResult.failure( msg );
        }

        TrackedEntityInstanceQueryParams params = initializeQueryParams();

        progress.startingStage( "Counting TEIs to synchronise" );
        PagedDataSynchronisationContext context = progress.runStage(
            new PagedDataSynchronisationContext( null, pageSize ),
            ctx -> "TrackedEntityInstances last changed before " + ctx.getSkipChangedBefore()
                + " will not be synchronized.",
            () -> createContext( params, pageSize ) );

        if ( context.getObjectsToSynchronize() == 0 )
        {
            String msg = "Tracker programs data synchronization skipped. No new or updated TEIs found.";
            progress.completedProcess( msg );
            return SynchronizationResult.success( msg );
        }

        if ( runSyncWithPaging( params, context, progress ) )
        {
            progress.completedProcess( "SUCCESS! Tracker programs data synchronization was successfully done!" );
            return SynchronizationResult.success( "Tracker programs data synchronization done." );

        }
        String msg = "Tracker programs data synchronization failed. Not all pages were synchronised successfully.";
        progress.failedProcess( msg );
        return SynchronizationResult.failure( msg );
    }

    private PagedDataSynchronisationContext createContext( TrackedEntityInstanceQueryParams queryParams,
        final int pageSize )
    {
        final Date skipChangedBefore = settings.getDateSetting( SKIP_SYNCHRONIZATION_FOR_DATA_CHANGED_BEFORE );
        queryParams.setSkipChangedBefore( skipChangedBefore );
        int objectsToSynchronize = teiService.getTrackedEntityInstanceCount( queryParams, true, true );

        if ( objectsToSynchronize == 0 )
        {
            return new PagedDataSynchronisationContext( skipChangedBefore, pageSize );
        }
        SystemInstance instance = SyncUtils.getRemoteInstanceWithSyncImportStrategy( settings,
            SyncEndpoint.TRACKED_ENTITY_INSTANCES );

        queryParams.setPageSize( pageSize );
        return new PagedDataSynchronisationContext( skipChangedBefore, objectsToSynchronize, instance, pageSize );
    }

    private TrackedEntityInstanceQueryParams initializeQueryParams()
    {
        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setIncludeDeleted( true );
        queryParams.setSynchronizationQuery( true );

        return queryParams;
    }

    private boolean runSyncWithPaging( TrackedEntityInstanceQueryParams queryParams,
        PagedDataSynchronisationContext context, JobProgress progress )
    {
        String msg = context.getObjectsToSynchronize() + " TEIs to sync were found.\n";
        msg += "Remote server URL for Tracker programs POST synchronization: " + context.getInstance().getUrl() + "\n";
        msg += "Tracker programs data synchronization job has " + context.getPages()
            + " pages to synchronize. With page size: " + context.getPageSize();

        progress.startingStage( msg, context.getPages(), SKIP_ITEM );
        progress.runStage( IntStream.range( 1, context.getPages() + 1 ).boxed(),
            page -> format( "Synchronizing page %d with page size %d", page, context.getPageSize() ),
            page -> {
                queryParams.setPage( page );
                synchronizePage( queryParams, context );
            } );
        return !progress.isSkipCurrentStage();
    }

    private void synchronizePage( TrackedEntityInstanceQueryParams queryParams,
        PagedDataSynchronisationContext context )
    {
        List<TrackedEntityInstance> dtoTeis = teiService.getTrackedEntityInstances(
            queryParams.setSynchronizationQuery( true ),
            TrackedEntityInstanceParams.TRUE, true, true );

        if ( log.isDebugEnabled() )
        {
            log.debug( "TEIs that are going to be synchronized are: " + dtoTeis );
        }

        if ( sendSyncRequest( context, dtoTeis ) )
        {
            List<String> teiUIDs = dtoTeis.stream()
                .map( TrackedEntityInstance::getTrackedEntityInstance )
                .collect( Collectors.toList() );
            log.info( "The lastSynchronized flag of these TEIs will be updated: " + teiUIDs );
            teiService.updateTrackedEntityInstancesSyncTimestamp( teiUIDs, context.getStartTime() );
            return;
        }
        throw new MetadataSyncServiceException( format( "Page %d synchronisation failed.", queryParams.getPage() ) );
    }

    private boolean sendSyncRequest( PagedDataSynchronisationContext context, List<TrackedEntityInstance> dtoTeis )
    {
        TrackedEntityInstances teis = new TrackedEntityInstances();
        teis.setTrackedEntityInstances( dtoTeis );
        SystemInstance instance = context.getInstance();

        RequestCallback requestCallback = request -> {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION,
                CodecUtils.getBasicAuthString( instance.getUsername(), instance.getPassword() ) );
            renderService.toJson( request.getBody(), teis );
        };

        return SyncUtils.sendSyncRequest( settings, restTemplate, requestCallback, instance,
            SyncEndpoint.TRACKED_ENTITY_INSTANCES );
    }
}
