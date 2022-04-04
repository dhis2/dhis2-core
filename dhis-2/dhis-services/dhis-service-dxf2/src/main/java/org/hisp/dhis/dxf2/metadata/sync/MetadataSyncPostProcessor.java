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
package org.hisp.dhis.dxf2.metadata.sync;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataRetryContext;
import org.hisp.dhis.dxf2.metadata.jobs.MetadataSyncJob;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.hisp.dhis.metadata.version.VersionType;
import org.springframework.stereotype.Component;

/**
 * Performs the tasks after metadata sync happens
 *
 * @author aamerm
 */
@Slf4j
@Component
@AllArgsConstructor
public class MetadataSyncPostProcessor
{
    private final EmailService emailService;

    public boolean handleSyncNotificationsAndAbortStatus( MetadataSyncSummary metadataSyncSummary,
        MetadataRetryContext retryContext, MetadataVersion dataVersion )
    {
        ImportReport importReport = metadataSyncSummary.getImportReport();

        if ( importReport == null )
        {
            handleImportFailedContext( null, retryContext, dataVersion );

            return true;
        }

        Status syncStatus = importReport.getStatus();
        log.info( "Import completed. Import Status: " + syncStatus );

        if ( Status.OK.equals( syncStatus )
            || (Status.WARNING.equals( syncStatus ) && VersionType.BEST_EFFORT.equals( dataVersion.getType() )) )
        {
            sendSuccessMailToAdmin( metadataSyncSummary );
            return false;
        }

        if ( Status.ERROR.equals( syncStatus ) )
        {
            handleImportFailedContext( metadataSyncSummary, retryContext, dataVersion );
            return true;
        }

        return false;
    }

    public void handleVersionAlreadyExists( MetadataRetryContext retryContext, MetadataVersion dataVersion )
    {
        retryContext.updateRetryContext( MetadataSyncJob.METADATA_SYNC,
            "Version already exists in system and hence stopping the sync", dataVersion, null );
        sendFailureMailToAdmin( retryContext );
        log.info(
            "Aborting Metadata sync. Version already exists in system and hence stopping the sync. Check mail and logs for more details." );
    }

    private void handleImportFailedContext( MetadataSyncSummary metadataSyncSummary, MetadataRetryContext retryContext,
        MetadataVersion dataVersion )
    {
        retryContext.updateRetryContext( MetadataSyncJob.METADATA_SYNC, "Import of metadata objects was unsuccessful",
            dataVersion, metadataSyncSummary );
        sendFailureMailToAdmin( retryContext );
        log.info( "Aborting Metadata sync Import Failure happened. Check mail and logs for more details." );
    }

    public void sendSuccessMailToAdmin( MetadataSyncSummary metadataSyncSummary )
    {
        ImportReport importReport = metadataSyncSummary.getImportReport();
        StringBuilder text = new StringBuilder(
            "Successful Import Report for the scheduler run for Metadata synchronization \n\n" )
                .append( "Imported Version Details \n " )
                .append( "Version Name: " + metadataSyncSummary.getMetadataVersion().getName() + "\n" )
                .append( "Version Type: " + metadataSyncSummary.getMetadataVersion().getType() + "\n" );

        if ( importReport.getTypeReportCount() == 0 )
        {
            text.append( "New Version created. It does not have any metadata changes. \n" );
        }
        else
        {
            text.append( "Imported Object Details: \n" );

            importReport.forEachTypeReport( typeReport -> {
                Stats stats = typeReport.getStats();

                text.append( "Metadata Object Type: " )
                    .append( typeReport.getKlass() )
                    .append( "\n" )
                    .append( "Stats: \n" )
                    .append( "total: " + stats.getTotal() + "\n" );

                if ( stats.getCreated() > 0 )
                {
                    text.append( " created: " + stats.getCreated() + "\n" );
                }

                if ( stats.getUpdated() > 0 )
                {
                    text.append( " updated: " + stats.getUpdated() + "\n" );
                }

                if ( stats.getIgnored() > 0 )
                {
                    text.append( " ignored: " + stats.getIgnored() + "\n" );
                }
            } );

            text.append( "\n\n" );

        }

        if ( text.length() > 0 )
        {
            log.info( "Success mail will be sent with the following message: " + text );
            emailService
                .sendSystemEmail( new Email( "Success Notification: Metadata Synchronization", text.toString() ) );
        }
    }

    public void sendFailureMailToAdmin( MetadataRetryContext retryContext )
    {
        StringBuilder text = new StringBuilder(
            "Following Exceptions were encountered while the scheduler run for metadata sync \n\n" );

        for ( String name : MetadataSyncJob.keys )
        {
            Object value = retryContext.getRetryContext().getAttribute( name );

            if ( value != null )
            {
                text.append( "ERROR_CATEGORY " )
                    .append( ": " ).append( name )
                    .append( "\n ERROR_VALUE : " ).append( value )
                    .append( "\n\n" );
            }
        }

        Object report = retryContext.getRetryContext().getAttribute( MetadataSyncJob.METADATA_SYNC_REPORT );

        if ( report != null )
        {
            String reportString = (String) report;

            text.append( MetadataSyncJob.METADATA_SYNC_REPORT )
                .append( "\n " )
                .append( reportString );
        }
        else
        {
            if ( retryContext.getRetryContext().getLastThrowable() != null )
            {
                text.append( retryContext.getRetryContext().getLastThrowable().getMessage() );
            }
        }

        if ( text.length() > 0 )
        {
            log.info( "Failure mail will be sent with the following message: " + text );
            emailService
                .sendSystemEmail( new Email( "Action Required: MetadataSync Failed Notification", text.toString() ) );
        }
    }
}
