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
package org.hisp.dhis.dxf2.events.importer.insert.validation;

import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.hisp.dhis.DhisConvenienceTest.createTrackedEntityInstance;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.events.importer.validation.BaseValidationTest;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentRepeatableStageCheckTest extends BaseValidationTest
{

    private EnrollmentRepeatableStageCheck rule;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp()
    {
        rule = new EnrollmentRepeatableStageCheck();
    }

    @Test
    void failOnNonRepeatableStageAndExistingEvents()
    {
        // Data preparation
        Program program = createProgram( 'P' );
        TrackedEntity tei = createTrackedEntityInstance( 'A', createOrganisationUnit( 'A' ) );
        event.setProgramStage( CodeGenerator.generateUid() );
        event.setProgram( program.getUid() );
        event.setTrackedEntityInstance( tei.getUid() );
        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.setRepeatable( false );
        when( workContext.getProgramStage( programStageIdScheme, event.getProgramStage() ) ).thenReturn( programStage );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        Map<String, Enrollment> programInstanceMap = new HashMap<>();
        Enrollment enrollment = new Enrollment();
        programInstanceMap.put( event.getUid(), enrollment );
        Pair<TrackedEntity, Boolean> teiPair = Pair.of( tei, true );
        Map<String, Pair<TrackedEntity, Boolean>> teiMap = new HashMap<>();
        teiMap.put( event.getUid(), teiPair );
        when( workContext.getTrackedEntityInstanceMap() ).thenReturn( teiMap );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        when( workContext.getServiceDelegator() ).thenReturn( serviceDelegator );
        when( serviceDelegator.getJdbcTemplate() ).thenReturn( jdbcTemplate );
        when( jdbcTemplate.queryForObject( anyString(), eq( Boolean.class ), eq( enrollment.getId() ),
            eq( programStage.getId() ), eq( tei.getId() ) ) ).thenReturn( true );
        // Method under test
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertHasError( summary, event, "Program stage is not repeatable and an event already exists" );
    }

    @Test
    void successOnNonRepeatableStageAndExistingEventsOnNewEnrollment()
    {
        // Data preparation
        Program program = createProgram( 'P' );
        TrackedEntity tei = createTrackedEntityInstance( 'A', createOrganisationUnit( 'A' ) );
        event.setProgramStage( CodeGenerator.generateUid() );
        event.setProgram( program.getUid() );
        event.setTrackedEntityInstance( tei.getUid() );
        ProgramStage programStage = createProgramStage( 'A', program );
        programStage.setRepeatable( false );
        when( workContext.getProgramStage( programStageIdScheme, event.getProgramStage() ) ).thenReturn( programStage );
        Map<String, Program> programMap = new HashMap<>();
        programMap.put( program.getUid(), program );
        Map<String, Enrollment> programInstanceMap = new HashMap<>();
        Enrollment enrollment = new Enrollment();
        programInstanceMap.put( event.getUid(), enrollment );
        Pair<TrackedEntity, Boolean> teiPair = Pair.of( tei, true );
        Map<String, Pair<TrackedEntity, Boolean>> teiMap = new HashMap<>();
        teiMap.put( event.getUid(), teiPair );
        when( workContext.getTrackedEntityInstanceMap() ).thenReturn( teiMap );
        when( workContext.getProgramsMap() ).thenReturn( programMap );
        when( workContext.getProgramInstanceMap() ).thenReturn( programInstanceMap );
        when( workContext.getServiceDelegator() ).thenReturn( serviceDelegator );
        when( serviceDelegator.getJdbcTemplate() ).thenReturn( jdbcTemplate );
        when( jdbcTemplate.queryForObject( anyString(), eq( Boolean.class ), eq( enrollment.getId() ),
            eq( programStage.getId() ), eq( tei.getId() ) ) ).thenReturn( false );
        // Method under test
        ImportSummary summary = rule.check( new ImmutableEvent( event ), workContext );
        assertNoError( summary );
    }
}
