package org.hisp.dhis.tracker.validation.hooks;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.junit.Assert.*;

public class EventCountValidationHookTest
{
    private EventCountValidationHook hook;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Before
    public void setUp()
        throws Exception
    {
        this.hook = new EventCountValidationHook( programInstanceService );
    }

    @Test
    public void verifyEventIsValid()
    {
        // Given
        Program programA = createProgram( 'A' );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.generateUid() );

        // create an Event and associate a Program Instance UID
        Event event = new Event();
        event.setProgram( programA.getUid() );
        event.setEnrollment( programInstance.getUid() );

        TrackerPreheat trackerPreheat = new TrackerPreheat();
        trackerPreheat.put( TrackerIdentifier.UID, programA );
        // Add the Program Instance to the preheat
        trackerPreheat.putEnrollment( TrackerIdScheme.UID, programInstance.getUid(), programInstance );

        TrackerBundle bundle = new TrackerBundle();
        bundle.setPreheat( trackerPreheat );
        TrackerImportValidationContext ctx = new TrackerImportValidationContext( bundle );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, Integer.class );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        //
        // Event is valid because the Event's Program Instance was found in the pre-heat
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void verifyEventIsInvalidNoProgramInstance()
    {
        // Given
        Program programA = createProgram( 'A' );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.generateUid() );

        Event event = new Event();
        event.setProgram( programA.getUid() );
        event.setEnrollment( programInstance.getUid() );

        // Pre-heat has no Program Instance Set
        TrackerPreheat trackerPreheat = new TrackerPreheat();
        trackerPreheat.put( TrackerIdentifier.UID, programA );

        TrackerBundle bundle = new TrackerBundle();
        bundle.setPreheat( trackerPreheat );
        TrackerImportValidationContext ctx = new TrackerImportValidationContext( bundle );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, Integer.class );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        //
        // Event is invalid because there are no Program Instance matching Event Program + Event Tei
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        final TrackerErrorReport error = reporter.getReportList().get( 0 );
        assertThat( error.getErrorCode(), is( TrackerErrorCode.E1037 ) );
        // TODO not sure how to test the arguments on TrackerErrorReport...
    }

    @Test
    public void verifyEventIsInvalidMultipleProgramInstance()
    {
        // Given
        Program programA = createProgram( 'A' );
        programA.setProgramType( ProgramType.WITH_REGISTRATION );

        ProgramInstance programInstance1 = new ProgramInstance();
        programInstance1.setUid( CodeGenerator.generateUid() );
        ProgramInstance programInstance2 = new ProgramInstance();
        programInstance2.setUid( CodeGenerator.generateUid() );

        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setProgram( programA.getUid() );
        // no enrollment set

        TrackerPreheat trackerPreheat = new TrackerPreheat();
        trackerPreheat.put( TrackerIdentifier.UID, programA );

        Map<String, List<ProgramInstance>> programInstancesByProgramAndTeiMap = new HashMap<>();
        programInstancesByProgramAndTeiMap.put( event.getUid(),
            Lists.newArrayList( programInstance1, programInstance2 ) );
        trackerPreheat.setProgramInstancesByProgramAndTei( programInstancesByProgramAndTeiMap );

        TrackerBundle bundle = new TrackerBundle();
        bundle.setPreheat( trackerPreheat );
        TrackerImportValidationContext ctx = new TrackerImportValidationContext( bundle );

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, Integer.class );

        // When
        this.hook.validateEvent( reporter, event );

        // Then
        //
        // Event is invalid because there are 2 Program Instances matching Event Program + Event Tei
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        final TrackerErrorReport error = reporter.getReportList().get( 0 );
        assertThat( error.getErrorCode(), is( TrackerErrorCode.E1038 ) );
        // TODO not sure how to test the arguments on TrackerErrorReport...
    }
}