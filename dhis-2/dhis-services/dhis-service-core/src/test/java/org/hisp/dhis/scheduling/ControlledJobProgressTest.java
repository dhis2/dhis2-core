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
package org.hisp.dhis.scheduling;

import static org.hisp.dhis.scheduling.JobProgress.FaultTolerance.SKIP_ITEM;
import static org.hisp.dhis.scheduling.JobProgress.FaultTolerance.SKIP_ITEM_OUTLIER;
import static org.hisp.dhis.scheduling.JobProgress.FaultTolerance.SKIP_STAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hisp.dhis.common.CodeGenerator;
import org.junit.jupiter.api.Test;

/**
 * Tests the processing (in particular the fault tolerance logic) of the
 * {@link ControlledJobProgress} implementation.
 *
 * @author Jan Bernitt
 */
class ControlledJobProgressTest
{
    private final JobConfiguration config = createJobConfig();

    private final JobProgress progress = new ControlledJobProgress( config );

    @Test
    void testSkipItem_FirstItemFails()
    {
        progress.startingStage( "test", 3, SKIP_ITEM );
        boolean stageSuccessful = progress.runStage( Stream.of( 1, 2, 3 ), String::valueOf, alwaysFail,
            assertSummary( 0, 3 ) );

        assertTrue( stageSuccessful, "the stage should be considered successful as failing items are skipped" );
        assertProcessCanContinue();
    }

    @Test
    void testSkipItemOutlier_FirstItemFailsStageFails()
    {
        progress.startingStage( "test", 3, SKIP_ITEM_OUTLIER );
        boolean stageSuccessful = progress.runStage( Stream.of( 1, 2, 3 ), String::valueOf, alwaysFail,
            assertSummary( 0, 1 ) );

        assertFalse( stageSuccessful, "the stage should be considered failed as first item failed" );
        assertProcessCanNotContinue();
    }

    @Test
    void testSkipItemOutlier_SecondItemFailsStageContinues()
    {
        progress.startingStage( "test", 3, SKIP_ITEM_OUTLIER );
        boolean stageSuccessful = progress.runStage( Stream.of( 1, 2, 3 ), String::valueOf, failsAfter( 1 ),
            assertSummary( 1, 2 ) );

        assertTrue( stageSuccessful, "the stage should be considered successful as first item was successful" );
        assertProcessCanContinue();
    }

    @Test
    void testSkipStage_ItemFails()
    {
        progress.startingStage( "test", 5, SKIP_STAGE );
        boolean stageSuccessful = progress.runStage( Stream.of( 1, 2, 3, 4, 5 ), String::valueOf, failsAfter( 2 ),
            assertSummary( 2, 1 ) );

        assertFalse( stageSuccessful, "the stage should be considered failed when an item fails" );
        assertProcessCanContinue();
    }

    private void assertProcessCanContinue()
    {
        assertFalse( progress.isCancellationRequested() );
        assertDoesNotThrow( () -> progress.startingStage( "another" ),
            "execution should be possible to continue with next stage" );
    }

    private void assertProcessCanNotContinue()
    {
        assertTrue( progress.isCancellationRequested() );
        assertThrows( CancellationException.class, () -> progress.startingStage( "another" ),
            "execution should not be possible to continue with next stage" );
    }

    private BiFunction<Integer, Integer, String> assertSummary( int successes, int failures )
    {
        return ( actualSuccesses, actualFailures ) -> {
            assertEquals( successes, actualSuccesses, "successes" );
            assertEquals( failures, actualFailures, "failures" );
            return null;
        };
    }

    private final Consumer<Integer> alwaysFail = item -> {
        throw new IllegalArgumentException( "failing" );
    };

    private static Consumer<Integer> failsAfter( int n )
    {
        AtomicInteger callCount = new AtomicInteger();
        return item -> {
            if ( callCount.incrementAndGet() > n )
                throw new IllegalStateException( "now failing" );
        };
    }

    private static JobConfiguration createJobConfig()
    {
        JobConfiguration config = new JobConfiguration();
        config.setJobType( JobType.PREDICTOR );
        config.setUid( CodeGenerator.generateUid() );
        return config;
    }
}
