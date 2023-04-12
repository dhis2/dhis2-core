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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPostProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.sync.SynchronizationJob;
import org.hisp.dhis.dxf2.synch.SynchronizationManager;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.metadata.version.MetadataVersion;
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
@Component( "metadataSyncJob" )
public class MetadataSyncJob extends SynchronizationJob
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

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    public MetadataSyncJob( SystemSettingManager systemSettingManager, RetryTemplate retryTemplate,
        SynchronizationManager synchronizationManager, MetadataSyncPreProcessor metadataSyncPreProcessor,
        MetadataSyncPostProcessor metadataSyncPostProcessor, MetadataSyncService metadataSyncService,
        MetadataRetryContext metadataRetryContext )
    {

        checkNotNull( systemSettingManager );
        checkNotNull( retryTemplate );
        checkNotNull( synchronizationManager );
        checkNotNull( metadataSyncPreProcessor );
        checkNotNull( metadataSyncPostProcessor );
        checkNotNull( metadataSyncService );
        checkNotNull( metadataRetryContext );

        this.systemSettingManager = systemSettingManager;
        this.retryTemplate = retryTemplate;
        this.synchronizationManager = synchronizationManager;
        this.metadataSyncPreProcessor = metadataSyncPreProcessor;
        this.metadataSyncPostProcessor = metadataSyncPostProcessor;
        this.metadataSyncService = metadataSyncService;
        this.metadataRetryContext = metadataRetryContext;
    }

    @Override
    public JobType getJobType()
    {
        return JobType.META_DATA_SYNC;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        log.info( "Metadata Sync cron Job started" );

        try
        {
            MetadataSyncJobParameters jobParameters = (MetadataSyncJobParameters) jobConfiguration.getJobParameters();
            retryTemplate.execute( retryContext -> {
                metadataRetryContext.setRetryContext( retryContext );
                clearFailedVersionSettings();
                runSyncTask( metadataRetryContext, jobParameters );
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

    synchronized void runSyncTask( MetadataRetryContext context, MetadataSyncJobParameters jobParameters )
        throws MetadataSyncServiceException,
        DhisVersionMismatchException
    {
        metadataSyncPreProcessor.setUp( context );

        metadataSyncPreProcessor.handleDataValuePush( context, jobParameters );

        metadataSyncPreProcessor.handleEventProgramsDataPush( context, jobParameters );
        metadataSyncPreProcessor.handleCompleteDataSetRegistrationDataPush( context );
        metadataSyncPreProcessor.handleTrackerProgramsDataPush( context, jobParameters );

        MetadataVersion metadataVersion = metadataSyncPreProcessor.handleCurrentMetadataVersion( context );

        List<MetadataVersion> metadataVersionList = metadataSyncPreProcessor.handleMetadataVersionsList( context,
            metadataVersion );

        if ( metadataVersionList != null )
        {
            for ( MetadataVersion dataVersion : metadataVersionList )
            {
                MetadataSyncParams syncParams = new MetadataSyncParams( new MetadataImportParams(), dataVersion );
                boolean isSyncRequired = metadataSyncService.isSyncRequired( syncParams );
                MetadataSyncSummary metadataSyncSummary = null;

                if ( isSyncRequired )
                {
                    metadataSyncSummary = handleMetadataSync( context, dataVersion );
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
        }

        log.info( "Metadata sync cron job ended " );
    }

    // ----------------------------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------------------------

    private MetadataSyncSummary handleMetadataSync( MetadataRetryContext context, MetadataVersion dataVersion )
        throws DhisVersionMismatchException
    {

        MetadataSyncParams syncParams = new MetadataSyncParams( new MetadataImportParams(), dataVersion );
        MetadataSyncSummary metadataSyncSummary = null;

        try
        {
            metadataSyncSummary = metadataSyncService.doMetadataSync( syncParams );
        }
        catch ( MetadataSyncServiceException e )
        {
            log.error( "Exception happened  while trying to do metadata sync  " + e.getMessage(), e );
            context.updateRetryContext( METADATA_SYNC, e.getMessage(), dataVersion );
            throw e;
        }
        catch ( DhisVersionMismatchException e )
        {
            context.updateRetryContext( METADATA_SYNC, e.getMessage(), dataVersion );
            throw e;
        }
        return metadataSyncSummary;

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
