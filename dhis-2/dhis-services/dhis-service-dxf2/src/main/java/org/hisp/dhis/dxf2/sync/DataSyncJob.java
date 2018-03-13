package org.hisp.dhis.dxf2.sync;/*
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
import org.hisp.dhis.dxf2.synch.AvailabilityStatus;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.AbstractJob;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;

public class DataSyncJob extends AbstractJob
{
    private static final Log log = LogFactory.getLog( DataSyncJob.class );

    //TODO: Use SystemSettings? (But a new one with periodic refresh)
    private static final int TRACKER_SYNC_PAGE_SIZE = 20;
    private static final int SYNC_RETRIES = 3;
    private static final int DELAY_BETWEEN_SYNC_RETRIES = 500; //in ms

    private static final String IMPORT_STRATEGY_DELETE_SUFFIX = "?importStrategy=DELETE";

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TrackedEntityInstanceService teiDBService;

    @Autowired
    private org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private RenderService renderService;


    @Override public JobType getJobType()
    {
        return JobType.DATA_SYNC;
    }

    @Override public void execute( JobConfiguration jobConfiguration ) throws Exception
    {
        //TODO: Check logic in DataSynchronizationJob.execute()


        try
        {
            syncEventProgramData();
        }
        catch ( Exception e )
        {
            //TODO: Handle exception
        }

        try
        {
            syncTrackerProgramData();
        }
        catch ( Exception e )
        {
            //TODO: Handle exception
        }
    }

    //TODO: Depends whether we need it or not as there is already one implementation of Events sync
    private void syncEventProgramData()
    {

    }

    private void syncTrackerProgramData()
    {
        boolean syncOK = true;
        final Date startTime = new Date();
        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );

        //Sync creates and updates
        Date lastSuccessfulSync = SyncUtil.getLastSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_TRACKER_DATA_SYNC );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setLastUpdatedStartDate( lastSuccessfulSync );

        syncOK = syncTrackerCreatesAndUpdates( queryParams, username, password );

        //Sync deletes
//        if(!syncDelete) {
//            syncOK = false;
//        }

        if ( syncOK )
        {
            SyncUtil.setSyncSuccess( systemSettingManager, SettingKey.LAST_SUCCESSFUL_TRACKER_DATA_SYNC, startTime );
        }
    }

    private boolean syncTrackerCreatesAndUpdates( TrackedEntityInstanceQueryParams queryParams, String username, String password )
    {
        boolean syncResult = true;

        int objectsToSync = teiDBService.getTrackedEntityInstanceCount( queryParams, true );
        int pages = (int) Math.ceil( objectsToSync / TRACKER_SYNC_PAGE_SIZE );
        log.info( objectsToSync + " TEIs to sync where found." );

        if ( objectsToSync == 0 )
        {
            log.info( "Nothing to sync. " );
            return true;
        }

        String trackerSyncUrl = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + SyncEndpoint.TEIS_ENDPOINT.getPath();
        log.info( "Remote server URL for Tracker creates and updates POST sync: " + trackerSyncUrl );

        //TODO: Add functionality (to the query/queryParams) to order by timestamp? (Then I can always start by the oldest one and move to the newest ones.)

        queryParams.setPageSize( TRACKER_SYNC_PAGE_SIZE );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.TRUE;
        for ( int i = 1; i <= pages; i++ )
        {
            queryParams.setPage( i );

            List<TrackedEntityInstance> dtoTeis = teiService.getTrackedEntityInstances( queryParams, params );
            log.info( "Going to sync TEIs (and related Enrollments and Events) numbers: " + ((i * TRACKER_SYNC_PAGE_SIZE) + 1) + " - " + ((i * TRACKER_SYNC_PAGE_SIZE) + dtoTeis.size()) );

            if ( log.isDebugEnabled() )
            {
                log.debug( "TEIs that are going to be synced are: " + dtoTeis );
            }

            if ( !sendTrackerSyncRequest( dtoTeis, username, password, trackerSyncUrl, SyncEndpoint.TEIS_ENDPOINT ) )
            {
                syncResult = false;
            }
        }

        return syncResult;
    }

    private boolean sendTrackerSyncRequest( List<TrackedEntityInstance> dtoTeis, String username, String password, String trackerSyncUrl, SyncEndpoint endpoint )
    {
        if ( !testServerAvailability( trackerSyncUrl ) )
        {
            return false;
        }

        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtil.HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
            renderService.toJson( request.getBody(), dtoTeis );
        };

        return SyncUtil.runSyncRequestAndAnalyzeResponse( restTemplate, requestCallback, trackerSyncUrl, endpoint );
    }

    //TODO: Maybe move into Util class?
    //TODO: DO I need it at all? validate() method below does basically the same, just without retries
    private boolean testServerAvailability( String syncUrl )
    {
        int retriesCounter = 0;

        //TODO: This is not sync yet, So maybe just try it 3 times and if fail, ok, skip. In sync I should again try 3 times I guess?
        while ( !SyncUtil.isRemoteServerAvailable( systemSettingManager, restTemplate ).isAvailable() )
        {
            if ( retriesCounter >= SYNC_RETRIES )
            {
                log.error( "Sync against '" + syncUrl + "' failed." );
                return false;
            }

            try
            {
                retriesCounter++;
                log.info( "Remote server is not available. Retry #" + retriesCounter + " in " + DELAY_BETWEEN_SYNC_RETRIES + " ms." );
                Thread.sleep( DELAY_BETWEEN_SYNC_RETRIES );
            }
            catch ( InterruptedException e )
            {
                log.error( "Sleep between sync retries failed.", e );
            }
        }

        return true;
    }

//    private void fetchTrackerDataForCreateAndUpdateSync( TrackedEntityInstanceQueryParams queryParams )
//    {
//    }

    //Tracker sync functionality:
    //TODO: DB method to get all deleted TEI since
    //TODO: DB method to get all deleted Enrollments since
    //TODO: DB method to get all deleted Events (for program with registration) since

    //Events sync functionality
    //TODO: DB method to get all created/updated Events (for programs without registration) since
    //TODO: DB method to get all deleted Events (for program withoutb registration) since

    @Override
    public ErrorReport validate()
    {
        AvailabilityStatus isRemoteServerAvailable = SyncUtil.isRemoteServerAvailable( systemSettingManager, restTemplate );

        if ( !isRemoteServerAvailable.isAvailable() )
        {
            return new ErrorReport( DataSyncJob.class, ErrorCode.E7010, isRemoteServerAvailable.getMessage() );
        }

        return super.validate();
    }
}
