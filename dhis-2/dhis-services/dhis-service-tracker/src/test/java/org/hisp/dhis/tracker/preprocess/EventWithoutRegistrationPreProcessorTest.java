package org.hisp.dhis.tracker.preprocess;

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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Luciano Fiandesio
 */
public class EventWithoutRegistrationPreProcessorTest
{
    private EventWithoutRegistrationPreProcessor preProcessorToTest;

    @Before
    public void setUp()
    {
        this.preProcessorToTest = new EventWithoutRegistrationPreProcessor();
    }

    @Test
    public void testEnrollmentIsAddedIntoEventWhenItBelongsToProgramWithoutRegistration()
    {
        //Given
        Event event = new Event();
        event.setProgram( "programUid" );
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( "programInstanceUid" );
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.putProgramInstancesWithoutRegistration( "programUid", programInstance );
        bundle.setPreheat( preheat );

        //When
        preProcessorToTest.process( bundle );

        //Then
        assertEquals( "programInstanceUid", bundle.getEvents().get( 0 ).getEnrollment() );
    }
}