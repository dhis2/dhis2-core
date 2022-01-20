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
package org.hisp.dhis.tracker.programrule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultTrackerProgramRuleServiceTest
{

    ProgramRuleEngine programRuleEngine;

    EnrollmentTrackerConverterService enrollmentTrackerConverterService;

    EventTrackerConverterService eventTrackerConverterService;

    TrackerConverterService trackerConverterService;

    DefaultTrackerProgramRuleService service;

    @BeforeEach
    void setUp()
    {
        programRuleEngine = mock( ProgramRuleEngine.class );
        enrollmentTrackerConverterService = mock( EnrollmentTrackerConverterService.class );
        eventTrackerConverterService = mock( EventTrackerConverterService.class );
        trackerConverterService = mock( TrackerConverterService.class );
        service = new DefaultTrackerProgramRuleService( programRuleEngine, enrollmentTrackerConverterService,
            eventTrackerConverterService, trackerConverterService );
    }

    @Test
    void calculateRuleEffectsForwardsDataElementIdSchemeToRuleEngine()
    {
        TrackerPreheat preheat = mock( TrackerPreheat.class );

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setEnrollment( enrollment.getEnrollment() );

        TrackerBundle bundle = TrackerBundle.builder()
            .preheat( preheat )
            .events( List.of( event ) )
            .build();
        when( preheat.getEnrollment( TrackerIdScheme.UID, enrollment.getUid() ) ).thenReturn( null );
        Program program = new Program();
        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .idScheme( TrackerIdentifier.UID )
            .dataElementIdScheme( TrackerIdentifier.CODE )
            .build();
        when( preheat.getIdentifiers() ).thenReturn( identifierParams );

        service.calculateRuleEffects( bundle );

        verify( programRuleEngine ).evaluateProgramEvents( Collections.emptySet(), program, IdScheme.CODE );
    }

}