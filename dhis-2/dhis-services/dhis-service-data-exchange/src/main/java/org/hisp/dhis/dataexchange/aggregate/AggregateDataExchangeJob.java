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
package org.hisp.dhis.dataexchange.aggregate;

import static java.lang.String.format;

import java.util.List;

import lombok.AllArgsConstructor;

import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.AggregateDataExchangeJobParameters;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AggregateDataExchangeJob implements Job
{
    private final AggregateDataExchangeService dataExchangeService;

    private final Notifier notifier;

    @Override
    public JobType getJobType()
    {
        return JobType.AGGREGATE_DATA_EXCHANGE;
    }

    @Override
    public void execute( JobConfiguration config, JobProgress progress )
    {
        notifier.clear( config );
        AggregateDataExchangeJobParameters params = (AggregateDataExchangeJobParameters) config.getJobParameters();

        List<String> dataExchangeIds = params.getDataExchangeIds();
        progress.startingProcess( format( "Aggregate data exchange of %d exchange(s)", dataExchangeIds.size() ) );
        ImportSummaries allSummaries = new ImportSummaries();
        for ( String dataExchangeId : dataExchangeIds )
        {
            AggregateDataExchange exchange = dataExchangeService.getById( dataExchangeId );
            allSummaries.addImportSummaries( dataExchangeService.exchangeData( exchange, progress ) );
        }
        notifier.addJobSummary( config, NotificationLevel.INFO, allSummaries, ImportSummaries.class );
        ImportStatus status = allSummaries.getStatus();
        if ( status == ImportStatus.ERROR )
        {
            progress.failedProcess( "Aggregate data exchange completed with errors" );
        }
        else
        {
            progress.completedProcess( "Aggregate data exchange completed with status " + status );
        }
    }
}
