package org.hisp.dhis.program;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramStore programStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private Program programA;

    private Program programB;

    private Program programC;

    @Override
    public void setUpTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setUid( "UID-A" );

        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programB.setUid( "UID-B" );

        programC = createProgram( 'C', new HashSet<>(), organisationUnitB );
        programC.setUid( "UID-C" );
    }

    @Test
    public void testGetProgramsByType()
    {
        programStore.save( programA );
        programStore.save( programB );

        programC.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programStore.save( programC );

        List<Program> programs = programStore.getByType( ProgramType.WITH_REGISTRATION );
        assertTrue( equals( programs, programA, programB ) );

        programs = programStore.getByType( ProgramType.WITHOUT_REGISTRATION );
        assertTrue( equals( programs, programC ) );
    }

    @Test
    public void testGetProgramsByTypeOu()
    {
        programStore.save( programA );
        programStore.save( programB );
        programStore.save( programC );

        List<Program> programs = programStore.get( ProgramType.WITH_REGISTRATION, organisationUnitA );
        
        assertTrue( equals( programs, programA, programB ) );
    }

    @Test
    public void testGetProgramsByDataEntryForm()
    {
        DataEntryForm formX = createDataEntryForm( 'X' );
        DataEntryForm formY = createDataEntryForm( 'Y' );

        dataEntryFormService.addDataEntryForm( formX );
        dataEntryFormService.addDataEntryForm( formY );

        programA.setDataEntryForm( formX );
        programB.setDataEntryForm( formX );

        programStore.save( programA );
        programStore.save( programB );
        programStore.save( programC );

        List<Program> withFormX = programStore.getByDataEntryForm( formX );
        assertEquals( 2, withFormX.size() );
        assertFalse( withFormX.contains( programC ) );

        programC.setDataEntryForm( formY );

        List<Program> withFormY = programStore.getByDataEntryForm( formY );
        assertEquals( 1, withFormY.size() );
        assertEquals( programC, withFormY.get( 0 ) );
    }
}