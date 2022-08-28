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
package org.hisp.dhis.programstagefilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramStageInstanceFilterTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private ProgramStageInstanceFilterService psiFilterService;

    @Autowired
    private ProgramService programService;

    private Program programA;

    private Program programB;

    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A' );
        programB = createProgram( 'B' );
        programService.addProgram( programA );
        programService.addProgram( programB );
    }

    @Test
    void testValidatenvalidEventFilterWithMissingProgram()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', null, null );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( "Program should be specified for event filters" ) );
    }

    @Test
    void testValidateInvalidEventFilterWithInvalidProgram()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', "ABCDEF12345", null );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( "Program is specified but does not exist" ) );
    }

    @Test
    void testValidateInvalidEventFilterWithInvalidProgramStage()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(),
            "ABCDEF12345" );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( "Program stage is specified but does not exist" ) );
    }

    @Test
    void testValidateInvalidEventFilterWithInvalidOrganisationUnit()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        EventQueryCriteria eqc = new EventQueryCriteria();
        eqc.setOrganisationUnit( "ABCDEF12345" );
        psiFilter.setEventQueryCriteria( eqc );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( "Org unit is specified but does not exist" ) );
    }

    @Test
    void testValidateInvalidEventFilterWithDataFilterAndEventUids()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        EventQueryCriteria eqc = new EventQueryCriteria();
        eqc.setEvents( Collections.singleton( "abcdefghijklm" ) );
        eqc.setDataFilters( Collections.singletonList( new EventDataFilter() ) );
        psiFilter.setEventQueryCriteria( eqc );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue( errors.get( 0 ).contains( "Event UIDs and filters can not be specified at the same time" ) );
    }

    @Test
    void testValidateInvalidEventFilterWithIncorrectAssignedUserMode()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        EventQueryCriteria eqc = new EventQueryCriteria();
        eqc.setAssignedUserMode( AssignedUserSelectionMode.CURRENT );
        eqc.setAssignedUsers( Collections.singleton( "abcdefghijklm" ) );
        psiFilter.setEventQueryCriteria( eqc );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 1, errors.size() );
        assertTrue(
            errors.get( 0 ).contains( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" ) );
    }

    @Test
    void testValidateEventFilterSuccessfully()
    {
        ProgramStageInstanceFilter psiFilter = createProgramStageInstanceFilter( '1', programA.getUid(), null );
        EventQueryCriteria eqc = new EventQueryCriteria();
        eqc.setAssignedUserMode( AssignedUserSelectionMode.CURRENT );
        psiFilter.setEventQueryCriteria( eqc );
        List<String> errors = psiFilterService.validate( psiFilter );
        assertNotNull( errors );
        assertEquals( 0, errors.size() );
    }

    private static ProgramStageInstanceFilter createProgramStageInstanceFilter( char uniqueCharacter, String program,
        String programStage )
    {
        ProgramStageInstanceFilter psiFilter = new ProgramStageInstanceFilter();
        psiFilter.setAutoFields();
        psiFilter.setName( "eventFilterName" + uniqueCharacter );
        psiFilter.setCode( "eventFilterCode" + uniqueCharacter );
        psiFilter.setDescription( "eventFilterDescription" + uniqueCharacter );
        if ( program != null )
        {
            psiFilter.setProgram( program );
        }
        if ( programStage != null )
        {
            psiFilter.setProgramStage( programStage );
        }
        return psiFilter;
    }
}
