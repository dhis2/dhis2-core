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
package org.hisp.dhis.dxf2.metadata.jobs;

import static java.lang.String.format;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPostProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.MetadataSyncJobParameters;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * This is the runnable that takes care of the Metadata Synchronization.
 * Leverages Spring RetryTemplate to exhibit retries. The retries are
 * configurable through the dhis.conf.
 *
 * @author anilkumk
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@Slf4j
@Component
@AllArgsConstructor
public class MetadataSyncJob implements Job
{
    public static final String VERSION_KEY = "version";

    public static final String DATA_PUSH_SUMMARY = "dataPushSummary";

    public static final String EVENT_PUSH_SUMMARY = "eventPushSummary";

    public static final String TRACKER_PUSH_SUMMARY = "trackerPushSummary";

    public static final String GET_METADATAVERSION = "getMetadataVersion";

    public static final String GET_METADATAVERSIONSLIST = "getMetadataVersionsList";

    public static final String METADATA_SYNC = "metadataSync";

    public static final String METADATA_SYNC_REPORT = "metadataSyncReport";

    public static final String[] keys = { DATA_PUSH_SUMMARY, EVENT_PUSH_SUMMARY, GET_METADATAVERSION,
        GET_METADATAVERSIONSLIST, METADATA_SYNC, VERSION_KEY };

    private final SystemSettingManager systemSettingManager;

    private final RetryTemplate retryTemplate;

    private final SynchronizationManager synchronizationManager;

    private final MetadataSyncPreProcessor metadataSyncPreProcessor;

    private final MetadataSyncPostProcessor metadataSyncPostProcessor;

    private final MetadataSyncService metadataSyncService;

    private final MetadataRetryContext metadataRetryContext;

    @Override
    public JobType getJobType()
    {
        return JobType.META_DATA_SYNC;
    }

    @Override
    public void execute( JobConfiguration config, JobProgress progress )
    {
        log.info( "Metadata Sync cron Job started" );

        try
        {
            MetadataSyncJobParameters params = (MetadataSyncJobParameters) config.getJobParameters();
            retryTemplate.execute( retryContext -> {
                metadataRetryContext.setRetryContext( retryContext );
                clearFailedVersionSettings();
                runSyncTask( metadataRetryContext, params, progress );
                return null;
            }, retryContext -> {
                log.info( "Metadata Sync failed! Sending mail to Admin" );
                updateMetadataVersionFailureDetails( metadataRetryContext );
                metadataSyncPostProcessor.sendFailureMailToAdmin( metadataRetryContext );
                return null;
            } );
        }
        catch ( Exception e )
        {
            String customMessage = "Exception occurred while executing metadata sync task." + e.getMessage();
            log.error( customMessage, e );
        }
    }

    @Override
    public ErrorReport validate()
    {
        return SyncUtils.validateRemoteServerAvailability( synchronizationManager, MetadataSyncJob.class )
            .orElse( null );
    }

    protected void runSyncTask( MetadataRetryContext context, MetadataSyncJobParameters params,
        JobProgress progress )
        throws MetadataSyncServiceException,
        DhisVersionMismatchException
    {
        metadataSyncPreProcessor.setUp( context, progress );
        metadataSyncPreProcessor.handleDataValuePush( context, params, progress );
        metadataSyncPreProcessor.handleEventProgramsDataPush( context, params, progress );
        metadataSyncPreProcessor.handleCompleteDataSetRegistrationDataPush( context, progress );
        metadataSyncPreProcessor.handleTrackerProgramsDataPush( context, params, progress );

        MetadataVersion version = metadataSyncPreProcessor.handleCurrentMetadataVersion( context, progress );

        List<MetadataVersion> versions = metadataSyncPreProcessor.handleMetadataVersionsList( context, version,
            progress );

        handleMetadataSync( context, versions, progress );

        log.info( "Metadata sync cron job ended " );
    }

    private void handleMetadataSync( MetadataRetryContext context, List<MetadataVersion> versions,
        JobProgress progress )
        throws DhisVersionMismatchException
    {
        if ( versions != null )
        {
            progress.startingProcess( "Synchronize metadata" );
            for ( MetadataVersion dataVersion : versions )
            {
                MetadataSyncParams syncParams = new MetadataSyncParams( new MetadataImportParams(), dataVersion );
                boolean isSyncRequired = metadataSyncService.isSyncRequired( syncParams );
                MetadataSyncSummary metadataSyncSummary = null;

                if ( isSyncRequired )
                {
                    metadataSyncSummary = handleMetadataSync( context, dataVersion, progress );
                }
                else
                {
                    metadataSyncPostProcessor.handleVersionAlreadyExists( context, dataVersion );
                    break;
                }

                boolean abortStatus = metadataSyncPostProcessor
                    .handleSyncNotificationsAndAbortStatus( metadataSyncSummary, context, dataVersion );

                if ( abortStatus )
                {
                    break;
                }

                clearFailedVersionSettings();
            }
            progress.completedProcess( null );
        }
    }

    // ----------------------------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------------------------

    private MetadataSyncSummary handleMetadataSync( MetadataRetryContext context, MetadataVersion dataVersion,
        JobProgress progress )
        throws DhisVersionMismatchException
    {
        progress.startingStage(
            format( "Synchronizing metadata for version %s %s ", dataVersion.getName(), dataVersion.getType() ) );
        MetadataSyncParams syncParams = new MetadataSyncParams( new MetadataImportParams(), dataVersion );

        try
        {
            MetadataSyncSummary summary = metadataSyncService.doMetadataSync( syncParams );
            progress.completedStage( "" + summary.getImportReport().getStatus() );
            return summary;
        }
        catch ( MetadataSyncServiceException | DhisVersionMismatchException ex )
        {
            progress.failedStage( ex );
            context.updateRetryContext( METADATA_SYNC, ex.getMessage(), dataVersion );
            throw ex;
        }
    }

    private void updateMetadataVersionFailureDetails( MetadataRetryContext retryContext )
    {
        Object version = retryContext.getRetryContext().getAttribute( VERSION_KEY );

        if ( version != null )
        {
            MetadataVersion metadataVersion = (MetadataVersion) version;
            systemSettingManager.saveSystemSetting( SettingKey.METADATA_FAILED_VERSION, metadataVersion.getName() );
            systemSettingManager.saveSystemSetting( SettingKey.METADATA_LAST_FAILED_TIME, new Date() );
        }
    }

    private void clearFailedVersionSettings()
    {
        systemSettingManager.deleteSystemSetting( SettingKey.METADATA_FAILED_VERSION );
        systemSettingManager.deleteSystemSetting( SettingKey.METADATA_LAST_FAILED_TIME );
    }
}
