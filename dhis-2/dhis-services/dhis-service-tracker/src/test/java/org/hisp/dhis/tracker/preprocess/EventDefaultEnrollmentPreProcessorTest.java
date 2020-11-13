package org.hisp.dhis.tracker.preprocess;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class EventDefaultEnrollmentPreProcessorTest
{
    private EventDefaultEnrollmentPreProcessor preProcessor;

    private BeanRandomizer rnd = new BeanRandomizer();

    @Before
    public void setUp()
    {
        preProcessor = new EventDefaultEnrollmentPreProcessor();
    }

    @Test
    public void verifyTrackedEntityValueIsNotModifiedWhenPresent()
    {
        Event event = new Event();
        event.setTrackedEntity( CodeGenerator.generateUid() );
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );
        assertThat( event.getTrackedEntity(), is( bundle.getEvents().get( 0 ).getTrackedEntity() ) );
    }

    @Test
    public void verifyTrackedEntityValueIsNotModifiedWhenTrackedEntityValueAndEnrollmentValueAreMissing()
    {
        Event event = new Event();
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( nullValue() ) );
    }

    @Test
    public void verifyTrackedEntityValueIsSetToParentEnrollmentTeiValue()
    {
        String enrollmentUid = CodeGenerator.generateUid();
        String teiUid = CodeGenerator.generateUid();

        ProgramInstance programInstance = rnd.randomObject( ProgramInstance.class );
        programInstance.setUid( enrollmentUid );
        TrackedEntityInstance trackedEntityInstance = rnd.randomObject( TrackedEntityInstance.class );
        trackedEntityInstance.setUid( teiUid );
        programInstance.setEntityInstance( trackedEntityInstance );

        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setEnrollments(
            ImmutableMap.of( TrackerIdScheme.UID, ImmutableMap.of( enrollmentUid, programInstance ) ) );

        Event event = new Event();
        event.setEnrollment( enrollmentUid );

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( preheat )
            .events( Collections.singletonList( event ) ).build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( teiUid ) );

        assertThat( preheat.getTrackedEntity( TrackerIdScheme.UID, teiUid ), is( notNullValue() ) );
    }

    @Test
    public void verifyTrackedEntityValueIsSetToParentEnrollmentTeiValueFromRef()
    {
        String teiUid = CodeGenerator.generateUid();

        // Create an enrollment with a reference to a parent tei
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setTrackedEntity( teiUid );

        // create an event without a tei reference
        Event event = new Event();
        event.setEnrollment( enrollment.getEnrollment() );

        // Add to preheat and make sure the enrollment and event uid are added to the
        // reference tree
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.putEnrollments( TrackerIdScheme.UID, new ArrayList<>(), Collections.singletonList( enrollment ) );
        preheat.putEvents( TrackerIdScheme.UID, new ArrayList<>(), Collections.singletonList( event ) );

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( preheat )
            .enrollments( Collections.singletonList( enrollment ) )
            .events( Collections.singletonList( event ) )
            .build();

        preProcessor.process( bundle );

        assertThat( event.getTrackedEntity(), is( teiUid ) );
    }

}