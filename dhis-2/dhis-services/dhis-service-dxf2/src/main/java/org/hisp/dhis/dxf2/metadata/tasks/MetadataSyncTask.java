/*
 * Copyright (c) 2004-2016, University of Oslo
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

package org.hisp.dhis.dxf2.metadata.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncParams;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPostProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncPreProcessor;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncService;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.retry.support.RetryTemplate;

import java.util.Date;
import java.util.List;

/**
 * This is the runnable that takes care of the Metadata Synchronization.
 * Leverages Spring RetryTemplate to exhibit retries. The retries are configurable
 * through the dhis.conf.
 *
 * @author anilkumk
 */
public class MetadataSyncTask
    implements Runnable, ApplicationListener<ContextRefreshedEvent>
{
    private static final Log log = LogFactory.getLog( MetadataSyncTask.class );

    public static String VERSION_KEY = "version";
    public static String DATA_PUSH_SUMMARY = "dataPushSummary";
    public static String EVENT_PUSH_SUMMARY = "eventPushSummary";
    public static String GET_METADATAVERSION = "getMetadataVersion";
    public static String GET_METADATAVERSIONSLIST = "getMetadataVersionsList";
    public static String METADATA_SYNC = "metadataSync";
    public static String METADATA_SYNC_REPORT = "metadataSyncReport";
    public static String[] keys = { DATA_PUSH_SUMMARY, EVENT_PUSH_SUMMARY, GET_METADATAVERSION, GET_METADATAVERSIONSLIST, METADATA_SYNC, VERSION_KEY };

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private RetryTemplate retryTemplate;

    @Autowired
    private MetadataSyncPreProcessor metadataSyncPreProcessor;

    @Autowired
    private MetadataSyncPostProcessor metadataSyncPostProcessor;

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private MetadataRetryContext metadataRetryContext;

    @Override
    public void onApplicationEvent( ContextRefreshedEvent contextRefreshedEvent )
    {
        try
        {
            systemSettingManager.saveSystemSetting( SettingKey.METADATAVERSION_ENABLED, true );
        }
        catch ( Exception e )
        {
            log.error( "Exception occurred while saving system setting 'keyVersionEnabled' " + e.getMessage(), e );
        }
    }

    @Override
    public void run()
    {
        log.info( "Metadata Sync cron Job started" );

        try
        {
            retryTemplate.execute( retryContext -> {
                    metadataRetryContext.setRetryContext( retryContext );
                    clearFailedVersionSettings();
                    runSyncTask( metadataRetryContext );
                    return null;
                }
                , retryContext -> {
                    log.info( "Retries Exhausted. Sending mail to Admin" );
                    updateMetadataVersionFailureDetails( metadataRetryContext );
                    metadataSyncPostProcessor.sendFailureMailToAdmin( metadataRetryContext );
                    return null;
                } );
        }
        catch ( Exception e )
        {
            log.error( "Exception occurred while executing metadata sync task." + e.getMessage(), e );
        }
    }

    public void runSyncTask( MetadataRetryContext context ) throws MetadataSyncServiceException
    {

        metadataSyncPreProcessor.handleAggregateDataPush( context );

        metadataSyncPreProcessor.handleEventDataPush( context );

        MetadataVersion metadataVersion = metadataSyncPreProcessor.handleCurrentMetadataVersion( context );

        List<MetadataVersion> metadataVersionList = metadataSyncPreProcessor.handleMetadataVersionsList( context, metadataVersion );

        if ( metadataVersionList != null )
        {
            for ( MetadataVersion dataVersion : metadataVersionList )
            {
                MetadataSyncSummary metadataSyncSummary = handleMetadataSync( context, dataVersion );
                boolean abortStatus = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, context, dataVersion );

                if ( abortStatus )
                {
                    break;
                }

                systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_METADATA_SYNC, dataVersion.getImportDate() );
                clearFailedVersionSettings();
            }
        }

        log.info( "Metadata Sync cron Job ended " );
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private MetadataSyncSummary handleMetadataSync( MetadataRetryContext context, MetadataVersion dataVersion )
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
