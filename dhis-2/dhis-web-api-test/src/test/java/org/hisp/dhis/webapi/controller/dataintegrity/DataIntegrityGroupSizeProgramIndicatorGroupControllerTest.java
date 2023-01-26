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
package org.hisp.dhis.webapi.controller.dataintegrity;

import java.util.Set;

import org.hisp.dhis.program.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Minimal test for program indicator groups which contain less than two
 * members.
 * {@see dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/groups/group_size_program_indicator_groups.yaml}
 *
 * @author Jason P. Pickering
 */
class DataIntegrityGroupSizeProgramIndicatorGroupControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "program_indicator_groups_scarce";

    private static final String detailsIdType = "programIndicatorGroups";

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private ProgramService programService;

    private ProgramIndicator testPIb;

    @Test
    void testProgramIndicatorGroupsTooSmall()
    {

        setUpTest();

        //Add a group with one indicator
        ProgramIndicatorGroup programIndicatorGroupB = new ProgramIndicatorGroup( "Test PI Group B" );
        programIndicatorGroupB.setAutoFields();
        programIndicatorGroupB.addProgramIndicator( testPIb );
        programIndicatorService.addProgramIndicatorGroup( programIndicatorGroupB );

        //Add a group with zero program indicators
        ProgramIndicatorGroup programIndicatorGroupC = new ProgramIndicatorGroup( "Test PI Group C" );
        programIndicatorGroupC.setAutoFields();
        programIndicatorService.addProgramIndicatorGroup( programIndicatorGroupC );
        dbmsManager.clearSession();

        Set<String> expected_uids = Set.of( programIndicatorGroupC.getUid(), programIndicatorGroupB.getUid() );
        Set<String> expected_names = Set.of( programIndicatorGroupC.getName(), programIndicatorGroupB.getName() );

        assertHasDataIntegrityIssues( detailsIdType, check, 66, expected_uids, expected_names,
            Set.of( "0", "1" ), true );
    }

    @Test
    void testProgramIndicatorGroupSizeOK()
    {

        setUpTest();

        dbmsManager.clearSession();

        assertHasNoDataIntegrityIssues( detailsIdType, check, true );

    }

    @Test
    void testProgramIndicatorGroupSizeRuns()
    {

        assertHasNoDataIntegrityIssues( detailsIdType, check, false );

    }

    public void setUpTest()
    {

        Program programA = new Program();
        programA.setName( "Program A" );
        programA.setShortName( "Program A" );
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        categoryService.getCategoryCombo( getDefaultCatCombo() );
        programA.setCategoryCombo( categoryService.getCategoryCombo( getDefaultCatCombo() ) );
        programService.addProgram( programA );

        ProgramIndicatorGroup programIndicatorGroupA = new ProgramIndicatorGroup( "Test PI Group A" );
        programIndicatorGroupA.setAutoFields();
        programIndicatorService.addProgramIndicatorGroup( programIndicatorGroupA );

        ProgramIndicator testPIa = new ProgramIndicator();
        testPIa.setAutoFields();
        testPIa.setName( "Test PI A" );
        testPIa.setShortName( "Test PI A" );
        testPIa.setProgram( programA );
        programIndicatorService.addProgramIndicator( testPIa );

        testPIb = new ProgramIndicator();
        testPIb.setAutoFields();
        testPIb.setName( "Test PI B" );
        testPIb.setShortName( "Test PI B" );
        testPIb.setProgram( programA );
        programIndicatorService.addProgramIndicator( testPIb );

        //Add two indicators to this group
        programIndicatorGroupA.addProgramIndicator( testPIa );
        programIndicatorGroupA.addProgramIndicator( testPIb );
        programIndicatorService.addProgramIndicatorGroup( programIndicatorGroupA );

    }
}