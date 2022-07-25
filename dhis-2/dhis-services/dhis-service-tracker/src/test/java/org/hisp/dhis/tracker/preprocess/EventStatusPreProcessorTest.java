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
package org.hisp.dhis.tracker.preprocess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 */
class EventStatusPreProcessorTest
{

    private EventStatusPreProcessor preProcessorToTest;

    @BeforeEach
    void setUp()
    {
        this.preProcessorToTest = new EventStatusPreProcessor();
    }

    @Test
    void testVisitedStatusIsConvertedToActive()
    {
        // Given
        Event event = new Event();
        event.setStatus( EventStatus.VISITED );
        event.setProgramStage( MetadataIdentifier.ofUid( "programStageUid" ) );
        TrackerBundle bundle = TrackerBundle.builder().events( Collections.singletonList( event ) ).build();
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( "programInstanceUid" );
        Program program = new Program();
        program.setUid( "programUid" );
        ProgramStage programStage = new ProgramStage();
        programStage.setUid( "programStageUid" );
        programStage.setProgram( program );
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.putProgramInstancesWithoutRegistration( "programUid", programInstance );
        preheat.put( programStage );
        bundle.setPreheat( preheat );
        // When
        preProcessorToTest.process( bundle );
        // Then
        assertEquals( EventStatus.ACTIVE, bundle.getEvents().get( 0 ).getStatus() );
    }
}
