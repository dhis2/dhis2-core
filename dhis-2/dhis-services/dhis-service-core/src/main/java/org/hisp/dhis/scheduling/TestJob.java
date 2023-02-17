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
package org.hisp.dhis.scheduling;

import static java.lang.String.format;

import java.util.stream.IntStream;

import org.hisp.dhis.scheduling.JobProgress.FailurePolicy;
import org.hisp.dhis.scheduling.parameters.TestJobParameters;
import org.springframework.stereotype.Component;

/**
 * A job for testing purposes that allows to fail in a controlled way.
 *
 * @author Jan Bernitt
 */
@Component
public class TestJob implements Job
{
    @Override
    public JobType getJobType()
    {
        return JobType.TEST;
    }

    @Override
    @SuppressWarnings( "java:S3776" ) // better keeping all processing in one loop
    public void execute( JobConfiguration conf, JobProgress progress )
    {
        TestJobParameters params = (TestJobParameters) conf.getJobParameters();
        progress.startingProcess( "Test job" );

        // initial wait stage
        long waitTime = params.getWaitMillis() == null ? 0L : params.getWaitMillis();
        progress.startingStage( format( "Waiting for %dms", waitTime ) );
        simulateWorkForDuration( waitTime );
        progress.completedStage( format( "waited for %dms", waitTime ) );

        // additional stages (potentially with items)
        int stages = params.getStages() == null ? 1 : params.getStages();
        int items = params.getItems() == null ? 0 : params.getItems();
        int failAtStage = params.getFailAtStage() == null ? -1 : params.getFailAtStage();
        int failAtItemDefault = items > 0 && failAtStage >= 0 ? 0 : -1;
        int failAtItem = params.getFailAtItem() == null ? failAtItemDefault : params.getFailAtItem();
        String msg = params.getFailWithMessage() == null ? "Simulated error" : params.getFailWithMessage();
        FailurePolicy policy = params.getFailWithPolicy() == null
            ? FailurePolicy.PARENT
            : params.getFailWithPolicy();
        if ( stages > 0 )
        {
            for ( int stage = 0; stage < stages; stage++ )
            {
                progress.startingStage( format( "Stage %d", stage ), items, policy );
                if ( items > 0 )
                {
                    if ( params.isFailWithException() )
                    {
                        progress.runStage( IntStream.range( 0, items ).boxed(), item -> "Item " + item, item -> {
                            simulateWorkForDuration( params.getItemDuration() );
                            if ( item == failAtItem )
                                throw new RuntimeException( msg );
                        } );
                    }
                    else
                    {
                        for ( int item = 0; item < items; item++ )
                        {
                            progress.startingWorkItem( item );
                            simulateWorkForDuration( params.getItemDuration() );
                            if ( item == failAtItem )
                            {
                                progress.failedWorkItem( msg );
                            }
                            else
                            {
                                progress.completedWorkItem( null );
                            }
                        }
                    }
                }
                else if ( failAtStage == stage )
                {
                    if ( params.isFailWithException() )
                        throw new RuntimeException( msg );
                    progress.failedStage( msg );
                }
                else
                {
                    progress.completedStage( format( "Stage %d complete", stage ) );
                }
            }
        }
        progress.completedProcess( null );
    }

    private void simulateWorkForDuration( Long waitTime )
    {
        if ( waitTime == null || waitTime == 0L )
        {
            return;
        }
        try
        {
            Thread.sleep( waitTime );
        }
        catch ( InterruptedException ex )
        {
            Thread.currentThread().interrupt();
        }
    }
}
