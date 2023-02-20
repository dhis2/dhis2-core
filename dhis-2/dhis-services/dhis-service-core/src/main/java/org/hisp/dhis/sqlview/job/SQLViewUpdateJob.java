/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.sqlview.job;

import static java.lang.String.format;

import java.util.List;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.SQLViewUpdateParameters;
import org.hisp.dhis.sqlview.SqlView;
import org.hisp.dhis.sqlview.SqlViewService;
import org.springframework.stereotype.Component;

/**
 * A job to update a list of {@link SqlView} that are of type
 * {@link SqlView#isMaterializedView()}.
 *
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class SQLViewUpdateJob implements Job
{
    private final SqlViewService sqlViewService;

    @Override
    public JobType getJobType()
    {
        return JobType.SQL_VIEW_UPDATE;
    }

    @Override
    public void execute( JobConfiguration config, JobProgress progress )
    {
        progress.startingProcess( "SQL View update" );
        SQLViewUpdateParameters params = (SQLViewUpdateParameters) config.getJobParameters();
        if ( params == null )
        {
            progress.completedProcess( "No views to update" );
            return;
        }
        List<String> sqlViews = params.getSqlViews();
        progress.startingStage( "Updating SQL views", sqlViews.size(), JobProgress.FailurePolicy.SKIP_ITEM );
        progress.runStage( sqlViews, Function.identity(), uid -> {
            SqlView view = sqlViewService.getSqlViewByUid( uid );
            if ( view != null && view.isMaterializedView() )
            {
                if ( !sqlViewService.refreshMaterializedView( view ) )
                {
                    throw new RuntimeException( "Failed to refresh view" );
                }
            }
            else
            {
                throw new IllegalArgumentException( "View does not exist or is not a materialized view." );
            }
        } );
        progress.completedProcess( format( "Updated %d SQL views", sqlViews.size() ) );
    }
}
