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
package org.hisp.dhis.trackedentity.job;

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.scheduling.parameters.TrackerTrigramIndexJobParameters;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentityattributevalue.TrackerTrigramIndexingJob;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link TrackerTrigramIndexingJob} using mocks to only test the
 * logic of the job.
 *
 * @author Ameen
 */
public class TrackerTrigramIndexingJobTest
{

    private final TrackedEntityAttributeService trackedEntityAttributeService = mock(
        TrackedEntityAttributeService.class );

    private final TrackerTrigramIndexingJob job = new TrackerTrigramIndexingJob( trackedEntityAttributeService );

    @Before
    public void setUp()
    {
        // mock normal run conditions
        when( trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes() ).thenReturn(
            Collections.emptySet() );
        when( trackedEntityAttributeService.runAnalyze() ).thenReturn( true );
        when( trackedEntityAttributeService.runVacuum() ).thenReturn( true );
    }

    @Test
    public void testRunJobWithoutAnyAttributesInJobParameters()
    {
        JobConfiguration jobConfiguration = new JobConfiguration();
        TrackerTrigramIndexJobParameters jp = new TrackerTrigramIndexJobParameters();
        jp.setRunAnalyze( true );
        jp.setRunVacuum( true );
        jobConfiguration.setJobParameters( jp );

        job.execute( jobConfiguration, NoopJobProgress.INSTANCE );

        verify( trackedEntityAttributeService, atLeastOnce() ).runAnalyze();
        verify( trackedEntityAttributeService, atLeastOnce() ).runVacuum();
        verify( trackedEntityAttributeService, never() ).createTrigramIndex( any() );
    }

    @Test
    public void testRunJobWithNonIndexableAttributesInJobParameters()
    {
        when( trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes() ).thenReturn(
            Collections.singleton( new TrackedEntityAttribute() ) );
        when( trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes() ).thenReturn(
            Collections.emptySet() );
        JobConfiguration jobConfiguration = new JobConfiguration();
        TrackerTrigramIndexJobParameters jp = new TrackerTrigramIndexJobParameters();
        jp.setRunAnalyze( true );
        jp.setRunVacuum( true );
        jp.setAttributes( Collections.singleton( "aaaa" ) );
        jobConfiguration.setJobParameters( jp );

        job.execute( jobConfiguration, NoopJobProgress.INSTANCE );

        verify( trackedEntityAttributeService, atMostOnce() ).runAnalyze();
        verify( trackedEntityAttributeService, atMostOnce() ).runVacuum();
        verify( trackedEntityAttributeService, never() ).createTrigramIndex( any() );
    }

    @Test
    public void testRunJobWithTwoIndexableAttributesInJobParameters()
    {
        Set<TrackedEntityAttribute> indexableAttributes = new HashSet<>();
        TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
        tea1.setUid( "tea1" );
        TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
        tea2.setUid( "tea2" );
        TrackedEntityAttribute tea3 = new TrackedEntityAttribute();
        tea3.setUid( "tea3" );
        indexableAttributes.add( tea1 );
        indexableAttributes.add( tea2 );
        indexableAttributes.add( tea3 );

        when( trackedEntityAttributeService.getAllTrigramIndexableTrackedEntityAttributes() ).thenReturn(
            indexableAttributes );
        when( trackedEntityAttributeService.createTrigramIndex( any() ) ).thenReturn(
            true );
        JobConfiguration jobConfiguration = new JobConfiguration();
        TrackerTrigramIndexJobParameters jp = new TrackerTrigramIndexJobParameters();
        jp.setRunAnalyze( true );
        jp.setRunVacuum( true );
        jp.setAttributes( Stream.of( "tea2", "tea3" ).collect( Collectors.toSet() ) );
        jobConfiguration.setJobParameters( jp );

        job.execute( jobConfiguration, NoopJobProgress.INSTANCE );

        verify( trackedEntityAttributeService, atMostOnce() ).runAnalyze();
        verify( trackedEntityAttributeService, atMostOnce() ).runVacuum();
        verify( trackedEntityAttributeService, atMost( 2 ) ).createTrigramIndex( any() );
    }

}
