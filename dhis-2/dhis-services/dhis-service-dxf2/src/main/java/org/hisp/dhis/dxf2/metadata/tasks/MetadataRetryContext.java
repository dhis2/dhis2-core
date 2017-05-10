package org.hisp.dhis.dxf2.metadata.tasks;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.springframework.retry.RetryContext;

import java.util.List;

/**
 * Defines retry mechanism for metadata sync scheduling
 *
 * @author aamerm
 */

public class MetadataRetryContext
{
    private static final Log log = LogFactory.getLog( MetadataRetryContext.class );

    private RetryContext retryContext;

    public RetryContext getRetryContext()
    {
        return retryContext;
    }

    public void setRetryContext( RetryContext retryContext )
    {
        this.retryContext = retryContext;
        log.info( "Now trying. Current count: " + (retryContext.getRetryCount() + 1) );
    }

    public void updateRetryContext( String stepKey,
        String message, MetadataVersion version )
    {
        retryContext.setAttribute( stepKey, message );

        if ( version != null )
        {
            retryContext.setAttribute( MetadataSyncTask.VERSION_KEY, version );
        }
    }

    public void updateRetryContext( String stepKey, String message, MetadataVersion version, MetadataSyncSummary summary )
    {
        updateRetryContext( stepKey, message, version );

        if ( summary != null )
        {
            setupImportReport( summary.getImportReport() );
        }
    }

    //----------------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------------

    private void setupImportReport( ImportReport importReport )
    {
        Status status = importReport.getStatus();

        if ( Status.ERROR.equals( status ) )
        {
            StringBuilder report = new StringBuilder();
            List<ErrorReport> errorReports = importReport.getErrorReports();

            for ( ErrorReport errorReport : errorReports )
            {

                if ( errorReport != null )
                {
                    report.append( errorReport.toString() + "\n" );
                }

            }

            retryContext.setAttribute( MetadataSyncTask.METADATA_SYNC_REPORT, report.toString() );
        }
    }
}
