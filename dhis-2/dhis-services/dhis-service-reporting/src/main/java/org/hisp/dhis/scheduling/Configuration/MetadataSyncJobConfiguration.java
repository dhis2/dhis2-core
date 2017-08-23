package org.hisp.dhis.scheduling.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.sync.*;
import org.hisp.dhis.dxf2.metadata.sync.exception.DhisVersionMismatchException;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.metadata.tasks.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.tasks.MetadataSyncTask;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;

import java.util.Date;
import java.util.List;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MetadataSyncJobConfiguration extends JobConfiguration
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
    public void run()
    {
        log.info( "Metadata Sync cron Job started" );

        try
        {
            retryTemplate.execute( retryContext ->
                {
                    metadataRetryContext.setRetryContext( retryContext );
                    clearFailedVersionSettings();
                    runSyncTask( metadataRetryContext );
                    return null;
                }
                , retryContext ->
                {
                    log.info( "Metadata Sync failed! Sending mail to Admin" );
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

    public synchronized void runSyncTask( MetadataRetryContext context ) throws MetadataSyncServiceException,
        DhisVersionMismatchException
    {
        metadataSyncPreProcessor.setUp( context );

        metadataSyncPreProcessor.handleAggregateDataPush( context );

        metadataSyncPreProcessor.handleEventDataPush( context );

        MetadataVersion metadataVersion = metadataSyncPreProcessor.handleCurrentMetadataVersion( context );

        List<MetadataVersion> metadataVersionList = metadataSyncPreProcessor.handleMetadataVersionsList( context, metadataVersion );

        if ( metadataVersionList != null )
        {
            for ( MetadataVersion dataVersion : metadataVersionList )
            {
                MetadataSyncParams syncParams = new MetadataSyncParams( new MetadataImportParams(), dataVersion );
                boolean isSyncRequired = metadataSyncService.isSyncRequired(syncParams);
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

                boolean abortStatus = metadataSyncPostProcessor.handleSyncNotificationsAndAbortStatus( metadataSyncSummary, context, dataVersion );

                if ( abortStatus )
                {
                    break;
                }

                systemSettingManager.saveSystemSetting( SettingKey.LAST_SUCCESSFUL_METADATA_SYNC, dataVersion.getImportDate() );
                clearFailedVersionSettings();
            }
        }

        log.info( "Metadata sync cron job ended " );
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private MetadataSyncSummary handleMetadataSync( MetadataRetryContext context, MetadataVersion dataVersion ) throws DhisVersionMismatchException
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
