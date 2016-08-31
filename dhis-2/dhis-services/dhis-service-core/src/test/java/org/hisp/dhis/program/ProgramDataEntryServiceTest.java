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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.mock.MockI18n;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Chau Thu Tran
 */
public class ProgramDataEntryServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramDataEntryService programDataEntryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    @Autowired
    private ProgramStageService programStageService;

    private OrganisationUnit organisationUnit;

    private ProgramStage stageA;

    private MockI18n mockI18n = new MockI18n();

    private String htmlCode;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        DataEntryForm dataEntryFormA = new DataEntryForm( "DataEntryForm-A" );
        dataEntryFormService.addDataEntryForm( dataEntryFormA );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "A", program );
        program.getProgramStages().add( stageA );
        stageA.setUid( "StageA" );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = new ProgramStage( "B", program );
        program.getProgramStages().add( stageB );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( "DeA" );
        DataElement dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        ProgramStageDataElement programStageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        stageA.getProgramStageDataElements().add( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );

        ProgramStageDataElement programStageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
        stageA.getProgramStageDataElements().add( programStageDataElementB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );

        htmlCode = "<input id=\"StageA-DeA-val\" style=\"width:4em;text-align:center\" value=\"\" title=\"\" />";
    }

    @Test
    public void testPrepareDataEntryFormForAdd()
    {
        String expected = "<input id=\"StageA-DeA-val\" style=\"width:4em;text-align:center\" value=\"\" title=\"[ DeA - DataElementA - INTEGER ]\"  "
            + " name=\"entryfield\" tabIndex=\"1\"  data=\"{compulsory:false, deName:\'DataElementA\', deType:\'INTEGER\'}\" options=\'false\' "
            + "maxlength=255  onchange=\"saveVal( \'DeA\', this.value )\" onkeypress=\"return keyPress(event, this)\"  />";
        String actual = programDataEntryService.prepareDataEntryFormForAdd( htmlCode, mockI18n, stageA );
        assertEquals( expected, actual );
    }

    @Test
    public void testPrepareDataEntryFormForEdit()
    {
        String expected = "<input id=\"StageA-DeA-val\" style=\"width:4em;text-align:center\" value=\"[DataElementA]\" title=\"[ DeA - DataElementA - INTEGER ]\" />";
        String actual = programDataEntryService.prepareDataEntryFormForEdit( htmlCode );
        assertEquals( expected, actual );
    }
}
