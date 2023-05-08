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
package org.hisp.dhis.dxf2.events.importer.shared.validation;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityInstance;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
class EnrollmentCheckTest extends BaseValidationTest
{

    private EnrollmentCheck rule;

    private Program program;

    @BeforeEach
    void setUp()
    {
        rule = new EnrollmentCheck();
        //
        // Program
        //
        program = createProgram( 'P' );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
    }

    @Test
    void failOnNoProgramInstanceByActiveProgramAndTei()
    {
        // Data preparation
        //
        // Enrollment
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );
        //
        // Tracked Entity Instance
        //
        TrackedEntity tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
        when( workContext.getTrackedEntityInstance( event.getUid() ) ).thenReturn( Optional.of( tei ) );
        event.setProgram( program.getUid() );
        //
        // Method under test
        //
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Tracked entity instance: " + tei.getUid() + " is not enrolled in program: " + program.getUid() );
    }

    @Test
    void failOnMultipleProgramInstanceByActiveProgramAndTei()
    {
        // Data preparation
        //
        // Enrollment
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );
        //
        // Tracked Entity Instance
        //
        TrackedEntity tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
        when( workContext.getTrackedEntityInstance( event.getUid() ) ).thenReturn( Optional.of( tei ) );
        Enrollment enrollment1 = new Enrollment();
        Enrollment enrollment2 = new Enrollment();
        when( this.enrollmentStore.get( tei, program, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( enrollment1, enrollment2 ) );
        event.setProgram( program.getUid() );
        //
        // Method under test
        //
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event, "Tracked entity instance: " + tei.getUid()
            + " has multiple active enrollments in program: " + program.getUid() );
    }

    @Test
    void failOnMultipleProgramInstance()
    {
        // Data preparation
        Program programNoReg = createProgram( 'P' );
        programNoReg.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( programNoReg.getUid(), programNoReg );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        //
        // Enrollment
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );
        //
        // Tracked Entity Instance
        //
        TrackedEntity tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
        Map<String, Pair<TrackedEntity, Boolean>> teiMap = new HashMap<>();
        teiMap.put( event.getUid(), Pair.of( tei, true ) );
        when( workContext.getTrackedEntityInstanceMap() ).thenReturn( teiMap );
        Enrollment enrollment1 = new Enrollment();
        Enrollment enrollment2 = new Enrollment();
        when( this.enrollmentStore.get( programNoReg, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( enrollment1, enrollment2 ) );
        event.setProgram( programNoReg.getUid() );
        //
        // Method under test
        //
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Multiple active enrollments exists for program: " + programNoReg.getUid() );
    }
}
