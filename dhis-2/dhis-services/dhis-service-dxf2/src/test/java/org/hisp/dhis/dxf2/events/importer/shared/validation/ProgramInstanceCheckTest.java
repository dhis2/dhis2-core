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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstanceCheckTest extends BaseValidationTest
{
    private ProgramInstanceCheck rule;

    private Program program;

    @Before
    public void setUp()
    {
        rule = new ProgramInstanceCheck();

        //
        // Program
        //
        program = createProgram( 'P' );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );

        when( workContext.getProgramsMap() ).thenReturn( programMap );
    }

    @Test
    public void failOnNoProgramInstanceByActiveProgramAndTei()
    {
        // Data preparation

        //
        // Program Instance
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );

        //
        // Tracked Entity Instance
        //
        TrackedEntityInstance tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
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
    public void failOnMultipleProgramInstanceByActiveProgramAndTei()
    {
        // Data preparation

        //
        // Program Instance
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );

        //
        // Tracked Entity Instance
        //
        TrackedEntityInstance tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
        when( workContext.getTrackedEntityInstance( event.getUid() ) ).thenReturn( Optional.of( tei ) );

        ProgramInstance programInstance1 = new ProgramInstance();
        ProgramInstance programInstance2 = new ProgramInstance();
        when( this.programInstanceStore.get( tei, program, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( programInstance1, programInstance2 ) );

        event.setProgram( program.getUid() );

        //
        // Method under test
        //
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Tracked entity instance: " + tei.getUid() + " has multiple active enrollments in program: "
                + program.getUid() );
    }

    @Test
    public void failOnMultipleProgramInstance()
    {
        // Data preparation

        Program programNoReg = createProgram( 'P' );
        programNoReg.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( programNoReg.getUid(), programNoReg );

        when( workContext.getProgramsMap() ).thenReturn( programMap );

        //
        // Program Instance
        //
        when( workContext.getProgramInstanceMap() ).thenReturn( new HashMap<>() );

        //
        // Tracked Entity Instance
        //
        TrackedEntityInstance tei = createTrackedEntityInstance( createOrganisationUnit( 'A' ) );
        Map<String, Pair<TrackedEntityInstance, Boolean>> teiMap = new HashMap<>();
        teiMap.put( event.getUid(), Pair.of( tei, true ) );
        when( workContext.getTrackedEntityInstanceMap() ).thenReturn( teiMap );

        ProgramInstance programInstance1 = new ProgramInstance();
        ProgramInstance programInstance2 = new ProgramInstance();
        when( this.programInstanceStore.get( programNoReg, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( programInstance1, programInstance2 ) );

        event.setProgram( programNoReg.getUid() );

        //
        // Method under test
        //
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event,
            "Multiple active program instances exists for program: " + programNoReg.getUid() );
    }
}
