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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
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
public class ProgramStageStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramStageStore programStageStore;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private ProgramStage stageC;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "A", program );
        stageA.setProgram( program );
        stageA.setUid( "UID-A" );

        stageB = new ProgramStage( "B", program );
        stageB.setProgram( program );
        stageB.setUid( "UID-B" );

        stageC = new ProgramStage( "C", program );
        stageB.setProgram( program );
        stageC.setUid( "UID-C" );
    }

    @Test
    public void testGetProgramStageByNameProgram()
    {
        programStageStore.save( stageA );
        programStageStore.save( stageB );
        
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );
       
        assertEquals( stageA, programStageStore.getByNameAndProgram( "A", program ) );
        assertEquals( stageB, programStageStore.getByNameAndProgram( "B", program ) );
    }

    @Test
    public void testGetByDataEntryForm()
    {
        DataEntryForm formX = createDataEntryForm( 'X' );
        DataEntryForm formY = createDataEntryForm( 'Y' );

        dataEntryFormService.addDataEntryForm( formX );
        dataEntryFormService.addDataEntryForm( formY );

        stageA.setDataEntryForm( formX );
        stageB.setDataEntryForm( formY );

        programStageStore.save( stageA );
        programStageStore.save( stageB );
        programStageStore.save( stageC );

        program.setProgramStages( Sets.newHashSet( stageA, stageB, stageC ) );
        programService.updateProgram( program );

        List<ProgramStage> hasFormX = programStageStore.getByDataEntryForm( formX );

        assertEquals( 1, hasFormX.size() );
        assertEquals( stageA, hasFormX.get( 0 ) );
    }
}