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

import static org.hisp.dhis.dxf2.Constants.PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS;
import static org.hisp.dhis.feedback.ErrorCode.E4051;
import static org.hisp.dhis.feedback.ErrorCode.E4052;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luca Cambi
 */
public class ProgramRuleVariableObjectBundleHookTest
{

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private ProgramRuleVariableObjectBundleHook programRuleVariableObjectBundleHook;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private ProgramRuleVariable programRuleVariable;

    @Mock
    private Query<ProgramRuleVariable> query;

    @Mock
    private Program program;

    @Mock
    private ObjectBundle objectBundle;

    @Captor
    private ArgumentCaptor<Class<ProgramRuleVariable>> classArgumentCaptor;

    @Before
    public void setUp()
    {
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.createQuery( anyString(), classArgumentCaptor.capture() ) ).thenReturn( query );
        when( program.getUid() ).thenReturn( "uid" );
    }

    @Test
    public void shouldExitObjectNotInstanceOfProgramRuleVariable()
    {
        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( new ProgramRule(),
            objectBundle );
        verifyNoInteractions( sessionFactory );
        assertEquals( 0, errorReports.size() );
    }

    @Test
    public void shouldFailInsertAlreadyExisting()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE );
        when( query.getResultList() ).thenReturn( Collections.singletonList( new ProgramRuleVariable() ) );

        when( programRuleVariable.getName() ).thenReturn( "word" );
        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
            objectBundle );
        assertEquals( 1, errorReports.size() );
        assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4051 ) ) );
    }

    @Test
    public void shouldNotFailUpdateExistingSameUid()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE_AND_UPDATE );

        ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
        existingProgramRuleVariable.setName( "word" );
        existingProgramRuleVariable.setUid( "uid1" );

        when( query.getResultList() ).thenReturn( Collections.singletonList( existingProgramRuleVariable ) );

        when( programRuleVariable.getName() ).thenReturn( "word" );
        when( programRuleVariable.getUid() ).thenReturn( "uid1" );

        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
                objectBundle );

        assertEquals( 0, errorReports.size() );
    }

    @Test
    public void shouldNotFailUpdateExistingMoreThanOneSameUid()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE_AND_UPDATE );

        ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
        existingProgramRuleVariable.setName( "word" );
        existingProgramRuleVariable.setUid( "uid1" );

        ProgramRuleVariable anotherExistingProgramRuleVariable = new ProgramRuleVariable();
        anotherExistingProgramRuleVariable.setName( "word" );
        anotherExistingProgramRuleVariable.setUid( "uid2" );

        when( query.getResultList() ).thenReturn( List.of( existingProgramRuleVariable, anotherExistingProgramRuleVariable ) );

        when( programRuleVariable.getName() ).thenReturn( "word" );
        when( programRuleVariable.getUid() ).thenReturn( "uid1" );

        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
                objectBundle );

        assertEquals( 0, errorReports.size() );
    }

    @Test
    public void shouldFailUpdateExistingDifferentUid()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE_AND_UPDATE );

        ProgramRuleVariable existingProgramRuleVariable = new ProgramRuleVariable();
        existingProgramRuleVariable.setName( "word" );
        existingProgramRuleVariable.setUid( "uid1" );

        when( query.getResultList() ).thenReturn( Collections.singletonList( existingProgramRuleVariable ) );

        when( programRuleVariable.getName() ).thenReturn( "word" );
        when( programRuleVariable.getUid() ).thenReturn( "uid2" );

        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
            objectBundle );

        assertEquals( 1, errorReports.size() );
        assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4051 ) ) );
    }

    @Test
    public void shouldFailValidationInvalidCountAndInvalidName()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE );
        when( query.getResultList() ).thenReturn( Collections.singletonList( new ProgramRuleVariable() ) );
        when( programRuleVariable.getName() )
            .thenReturn( "Word " + PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS.get( 0 ) + " Word" );

        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
            objectBundle );

        assertEquals( 2, errorReports.size() );
        assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4051 ) ) );
        assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4052 ) ) );
    }

    @Test
    public void shouldFailValidationInvalidName()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE_AND_UPDATE );
        List<ErrorReport> errorReports;

        for ( String invalidKeyWord : PROGRAM_RULE_VARIABLE_NAME_INVALID_KEYWORDS )
        {
            when( programRuleVariable.getName() ).thenReturn( "Word " + invalidKeyWord + " Word" );
            errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
                objectBundle );
            assertEquals( 1, errorReports.size() );
            assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4052 ) ) );

            when( programRuleVariable.getName() ).thenReturn( invalidKeyWord + " Word" );
            errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
                objectBundle );
            assertEquals( 1, errorReports.size() );
            assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4052 ) ) );

            when( programRuleVariable.getName() ).thenReturn( "Word " + invalidKeyWord );
            errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
                objectBundle );
            assertEquals( 1, errorReports.size() );
            assertTrue( errorReports.stream().anyMatch( e -> e.getErrorCode().equals( E4052 ) ) );
        }
    }

    @Test
    public void shouldPassValidationWithValidName()
    {
        when( programRuleVariable.getProgram() ).thenReturn( program );
        when( programRuleVariable.getName() ).thenReturn( "WordAndWord" );
        when( objectBundle.getImportMode() ).thenReturn( ImportStrategy.CREATE_AND_UPDATE );

        List<ErrorReport> errorReports = programRuleVariableObjectBundleHook.validate( programRuleVariable,
            objectBundle );
        assertEquals( 0, errorReports.size() );

        when( programRuleVariable.getName() ).thenReturn( "Word and_another Word" );

        List<ErrorReport> errorReports1 = programRuleVariableObjectBundleHook.validate( programRuleVariable,
            objectBundle );
        assertEquals( 0, errorReports1.size() );
    }
}
