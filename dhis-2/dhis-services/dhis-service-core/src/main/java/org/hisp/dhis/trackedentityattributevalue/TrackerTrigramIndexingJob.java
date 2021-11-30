/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.trackedentityattributevalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Component;

/**
 * @author Ameen
 */
@Slf4j
@Component( "trackerTrigramIndexingJob" )
public class TrackerTrigramIndexingJob implements Job
{
    private final TrackedEntityAttributeService trackedEntityAttributeService;

    public TrackerTrigramIndexingJob( TrackedEntityAttributeService trackedEntityAttributeService )
    {
        checkNotNull( trackedEntityAttributeService );
        this.trackedEntityAttributeService = trackedEntityAttributeService;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public JobType getJobType()
    {
        return JobType.TRACKER_SEARCH_OPTIMIZATION;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration, JobProgress progress )
    {
        TrackerTrigramIndexJobParameters parameters = (TrackerTrigramIndexJobParameters) jobConfiguration
            .getJobParameters();

        log.info( "Starting Trigram Indexing Job. Attributes Provided to Index: {}", parameters.getAttributes() );
        progress.startingProcess( "Starting Trigram indexing process" );

        if ( !CollectionUtils.isEmpty( parameters.getAttributes() ) )
        {
            log.debug( "Fetching all indexable attributes from db" );
            Set<TrackedEntityAttribute> indexableAttributes = trackedEntityAttributeService
                .getAllTrigramIndexableTrackedEntityAttributes();
            indexableAttributes = indexableAttributes.stream()
                .filter( itea -> parameters.getAttributes().contains( itea.getUid() ) ).collect( Collectors.toSet() );
            progress.startingStage( "Creating trigram indexes attributes", parameters.getAttributes().size() );
            progress.runStage( indexableAttributes.stream(), TrackedEntityAttribute::getName,
                tea -> trackedEntityAttributeService.createTrigramIndex( tea ),
                TrackerTrigramIndexingJob::computeTrigramIndexingSummary );
            progress.completedStage( "Trigram indexes created" );
        }

        if ( parameters.isRunVacuum() )
        {
            progress.startingStage( "Running VACUUM on tracker tables", 1 );
            progress.runStage( () -> trackedEntityAttributeService.runVacuum() );
            progress.completedStage( "VACUUM run completed" );
        }

        if ( parameters.isRunAnalyze() )
        {
            progress.startingStage( "Running ANALYZE on tracker tables", 1 );
            progress.runStage( () -> trackedEntityAttributeService.runAnalyze() );
            progress.completedStage( "ANALYZE run completed" );
        }

        progress.completedProcess( "Job completed" );
    }

    private static String computeTrigramIndexingSummary( int successful, int failed )
    {
        log.info(
            format( "%d trigram indexes have been created for corresponding trackedentityattributes", successful ) );
        if ( failed == 0 )
        {
            return null;
        }
        String summary = format( "%d trigram index creation failed", failed );
        log.warn( summary );
        return summary;
    }
}
