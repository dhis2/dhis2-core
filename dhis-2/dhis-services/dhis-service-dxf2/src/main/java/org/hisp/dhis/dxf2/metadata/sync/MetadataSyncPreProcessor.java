package org.hisp.dhis.dxf2.metadata.sync;

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
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataSyncJob;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.version.MetadataVersionDelegate;
import org.hisp.dhis.dxf2.metadata.version.exception.MetadataVersionServiceException;
import org.hisp.dhis.dxf2.sync.DataValueSynchronization;
import org.hisp.dhis.dxf2.sync.EventSynchronization;
import org.hisp.dhis.dxf2.sync.SynchronizationResult;
import org.hisp.dhis.dxf2.sync.SynchronizationStatus;
import org.hisp.dhis.dxf2.sync.TrackerSynchronization;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.MetadataVersionService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Performs the tasks before metadata sync happens
 *
 * @author aamerm
 */
public class MetadataSyncPreProcessor
{
    private static final Log log = LogFactory.getLog( MetadataSyncPreProcessor.class );

    private final SystemSettingManager systemSettingManager;
    private final MetadataVersionService metadataVersionService;
    private final MetadataVersionDelegate metadataVersionDelegate;
    private final TrackerSynchronization trackerSync;
    private final EventSynchronization eventSync;
    private final DataValueSynchronization dataValueSync;

    @Autowired
    public MetadataSyncPreProcessor(
        SystemSettingManager systemSettingManager,
        MetadataVersionService metadataVersionService,
        MetadataVersionDelegate metadataVersionDelegate,
        TrackerSynchronization trackerSync,
        EventSynchronization eventSync,
        DataValueSynchronization dataValueSync )
    {
        this.systemSettingManager = systemSettingManager;
        this.metadataVersionService = metadataVersionService;
        this.metadataVersionDelegate = metadataVersionDelegate;
        this.trackerSync = trackerSync;
        this.eventSync = eventSync;
        this.dataValueSync = dataValueSync;
    }


    public void setUp( MetadataRetryContext context )
    {
        systemSettingManager.saveSystemSetting( SettingKey.METADATAVERSION_ENABLED, true );
    }

    public void handleAggregateDataPush( MetadataRetryContext context )
    {
        SynchronizationResult dataValuesSynchronizationResult = dataValueSync.syncDataValuesData();

        if ( dataValuesSynchronizationResult.status == SynchronizationStatus.FAILURE )
        {
            context.updateRetryContext( MetadataSyncJob.DATA_PUSH_SUMMARY, dataValuesSynchronizationResult.message, null, null );
            throw new MetadataSyncServiceException( dataValuesSynchronizationResult.message );
        }
    }

    public void handleTrackerDataPush( MetadataRetryContext context )
    {
        SynchronizationResult trackerSynchronizationResult = trackerSync.syncTrackerProgramData();

        if ( trackerSynchronizationResult.status == SynchronizationStatus.FAILURE )
        {
            context.updateRetryContext( MetadataSyncJob.TRACKER_PUSH_SUMMARY, trackerSynchronizationResult.message, null, null );
            throw new MetadataSyncServiceException( trackerSynchronizationResult.message );
        }
    }

    public void handleEventDataPush( MetadataRetryContext context )
    {
        SynchronizationResult eventsSynchronizationResult = eventSync.syncEventProgramData();

        if ( eventsSynchronizationResult.status == SynchronizationStatus.FAILURE )
        {
            context.updateRetryContext( MetadataSyncJob.EVENT_PUSH_SUMMARY, eventsSynchronizationResult.message, null, null );
            throw new MetadataSyncServiceException( eventsSynchronizationResult.message );
        }
    }

    public List<MetadataVersion> handleMetadataVersionsList( MetadataRetryContext context, MetadataVersion metadataVersion )
    {
        log.debug( "Fetching the list of remote versions" );

        List<MetadataVersion> metadataVersionList = new ArrayList<>();

        try
        {
            metadataVersionList = metadataVersionDelegate.getMetaDataDifference( metadataVersion );

            if ( metadataVersion == null )
            {
                log.info( "There is no initial version in the system" );
            }

            if ( isRemoteVersionEmpty( metadataVersion, metadataVersionList ) )
            {
                log.info( "There are no metadata versions created in the remote instance." );
                return metadataVersionList;
            }

            if ( isUsingLatestVersion( metadataVersion, metadataVersionList ) )
            {
                log.info( "Your instance is already using the latest version:" + metadataVersion );
                return metadataVersionList;
            }

            MetadataVersion latestVersion = getLatestVersion( metadataVersionList );
            assert latestVersion != null;

            systemSettingManager.saveSystemSetting( SettingKey.REMOTE_METADATA_VERSION, latestVersion.getName() );
            log.info( "Remote system is at version: " + latestVersion.getName() );

        }
        catch ( MetadataVersionServiceException e )
        {
            String message = setVersionListErrorInfoInContext( context, metadataVersion, e );
            throw new MetadataSyncServiceException( message, e );
        }
        catch ( Exception ex )
        {
            if ( ex instanceof MetadataSyncServiceException )
            {
                log.error( ex.getMessage(), ex );
                throw ex;
            }

            String message = setVersionListErrorInfoInContext( context, metadataVersion, ex );
            log.error( message, ex );
            throw new MetadataSyncServiceException( message, ex );
        }

        return metadataVersionList;
    }

    private String setVersionListErrorInfoInContext( MetadataRetryContext context, MetadataVersion metadataVersion, Exception e )
    {
        String message = "Exception happened while trying to get remote metadata versions difference " + e.getMessage();
        context.updateRetryContext( MetadataSyncJob.GET_METADATAVERSIONSLIST, e.getMessage(), metadataVersion, null );
        return message;
    }

    private boolean isUsingLatestVersion( MetadataVersion metadataVersion, List<MetadataVersion> metadataVersionList )
    {
        return metadataVersion != null && metadataVersionList.size() == 0;
    }

    private boolean isRemoteVersionEmpty( MetadataVersion metadataVersion, List<MetadataVersion> metadataVersionList )
    {
        return metadataVersion == null && metadataVersionList.size() == 0;
    }

    public MetadataVersion handleCurrentMetadataVersion( MetadataRetryContext context )
    {
        log.debug( "Getting the current version of the system" );
        MetadataVersion metadataVersion = null;

        try
        {
            metadataVersion = metadataVersionService.getCurrentVersion();
            log.info( "Current Metadata Version of the system: " + metadataVersion );
        }
        catch ( MetadataVersionServiceException ex )
        {
            context.updateRetryContext( MetadataSyncJob.GET_METADATAVERSION, ex.getMessage(), null, null );
            throw new MetadataSyncServiceException( ex.getMessage(), ex );
        }

        return metadataVersion;
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------


    private void handleAggregateImportSummary( ImportSummary importSummary, MetadataRetryContext context )
    {
        if ( importSummary != null )
        {
            ImportStatus status = importSummary.getStatus();

            if ( ImportStatus.ERROR.equals( status ) || ImportStatus.WARNING.equals( status ) )
            {
                log.error( "Import Summary description: " + importSummary.getDescription() );
                context.updateRetryContext( MetadataSyncJob.DATA_PUSH_SUMMARY, importSummary.getDescription(), null, null );
                throw new MetadataSyncServiceException( "The Data Push was not successful. " );
            }
        }

    }

    private MetadataVersion getLatestVersion( List<MetadataVersion> metadataVersionList )
    {
        Collection<Date> dateCollection = new ArrayList<Date>();

        for ( MetadataVersion metadataVersion : metadataVersionList )
        {
            dateCollection.add( metadataVersion.getCreated() );
        }

        Date maxDate = DateUtils.max( dateCollection );

        for ( MetadataVersion metadataVersion : metadataVersionList )
        {
            if ( metadataVersion.getCreated().equals( maxDate ) )
            {
                return metadataVersion;
            }
        }

        return null;
    }

    public void handleDataSetCompletenessPush(MetadataRetryContext context) {
        SynchronizationResult completenessSynchronizationResult = dataValueSync.syncCompleteness();

        if ( completenessSynchronizationResult.status == SynchronizationStatus.FAILURE )
        {
            context.updateRetryContext( MetadataSyncJob.DATA_PUSH_SUMMARY, completenessSynchronizationResult.message, null, null );
            throw new MetadataSyncServiceException( completenessSynchronizationResult.message );
        }
    }
}
