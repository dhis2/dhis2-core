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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.DhisConvenienceTest.createProgram;
import static org.hisp.dhis.DhisConvenienceTest.createProgramStage;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hibernate.SessionFactory;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceQueryParams;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AclService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
public class ProgramObjectBundleHookTest
{
    private ProgramObjectBundleHook subject;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramService programService;

    @Mock
    private ProgramStageService programStageService;

    @Mock
    private AclService aclService;

    @Mock
    private SessionFactory sessionFactory;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private Program programA;

    @Before
    public void setUp()
    {
        this.subject = new ProgramObjectBundleHook( programInstanceService, programService, programStageService,
            aclService );

        programA = createProgram( 'A' );
        programA.setId( 100 );
    }

    @Test
    public void verifyNullObjectIsIgnored()
    {
        subject.preCreate( null, null );

        verifyNoInteractions( programInstanceService );
    }

    @Test
    public void verifyMissingBundleIsIgnored()
    {
        ProgramInstance programInstance = new ProgramInstance();

        subject.preCreate( programA, null );

        verifyNoInteractions( programInstanceService );
    }

    @Test
    public void verifyProgramInstanceIsSavedForEventProgram()
    {
        ArgumentCaptor<ProgramInstance> argument = ArgumentCaptor.forClass( ProgramInstance.class );

        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        subject.postCreate( programA, null );

        verify( programInstanceService ).addProgramInstance( argument.capture() );

        assertThat( argument.getValue().getEnrollmentDate(), is( notNullValue() ) );
        assertThat( argument.getValue().getIncidentDate(), is( notNullValue() ) );
        assertThat( argument.getValue().getProgram(), is( programA ) );
        assertThat( argument.getValue().getStatus(), is( ProgramStatus.ACTIVE ) );
        assertThat( argument.getValue().getStoredBy(), is( "system-process" ) );
    }

    @Test
    public void verifyProgramInstanceIsNotSavedForTrackerProgram()
    {
        ArgumentCaptor<ProgramInstance> argument = ArgumentCaptor.forClass( ProgramInstance.class );

        programA.setProgramType( ProgramType.WITH_REGISTRATION );
        subject.postCreate( programA, null );

        verify( programInstanceService, times( 0 ) ).addProgramInstance( argument.capture() );
    }

    @Test
    public void verifyProgramValidates()
    {
        assertEquals( 0, subject.validate( programA, null ).size() );
    }

    @Test
    public void verifyProgramFailsValidation()
    {
        ProgramInstanceQueryParams programInstanceQueryParams = new ProgramInstanceQueryParams();
        programInstanceQueryParams.setProgram( programA );
        programInstanceQueryParams.setProgramStatus( ProgramStatus.ACTIVE );

        when( programInstanceService.getProgramInstances( programA, ProgramStatus.ACTIVE ) )
            .thenReturn( Lists.newArrayList( new ProgramInstance(), new ProgramInstance() ) );

        List<ErrorReport> errors = subject.validate( programA, null );

        assertEquals( 1, errors.size() );
        assertEquals( errors.get( 0 ).getErrorCode(), ErrorCode.E6000 );
        assertEquals( errors.get( 0 ).getMessage(), "Program `ProgramA` has more than one Program Instances" );
    }

    @Test
    public void verifyValidationIsSkippedWhenObjectIsTransient()
    {
        Program transientObj = createProgram( 'A' );
        subject.validate( transientObj, null );

        verifyNoInteractions( programInstanceService );
    }

    @Test
    public void verifyUpdateProgramStage()
    {
        ProgramStage programStage = createProgramStage( 'A', 1 );
        programA.getProgramStages().add( programStage );

        ArgumentCaptor<Program> argument = ArgumentCaptor.forClass( Program.class );
        ArgumentCaptor<ProgramStage> argPS = ArgumentCaptor.forClass( ProgramStage.class );

        programService.addProgram( programA );

        subject.postCreate( programA, null );

        verify( programService ).updateProgram( argument.capture() );

        verify( programStageService ).saveProgramStage( argPS.capture() );

        assertThat( argPS.getValue().getName(), is( equalToIgnoringCase( "ProgramStageA" ) ) );
        assertThat( argPS.getValue().getProgram(), is( programA ) );

        assertThat( argument.getValue().getName(), is( equalToIgnoringCase( "ProgramA" ) ) );
        assertThat( argument.getValue().getProgramStages().size(), is( 1 ) );
        assertThat( argument.getValue().getProgramStages().iterator().next().getName(),
            is( equalToIgnoringCase( "ProgramStageA" ) ) );
    }
}
