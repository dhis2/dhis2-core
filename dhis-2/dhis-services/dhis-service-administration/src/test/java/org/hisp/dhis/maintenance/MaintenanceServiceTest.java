package org.hisp.dhis.maintenance;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Enrico Colasante
 */
public class MaintenanceServiceTest
    extends DhisTest
{
    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private MaintenanceService maintenanceService;

    private Date incidenDate;

    private Date enrollmentDate;

    private Program program;

    private OrganisationUnit organisationUnit;

    private ProgramInstance programInstance;

    private TrackedEntityInstance entityInstance;

    private Collection<Long> orgunitIds;

    private int programInstanceId;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        long idA = organisationUnitService.addOrganisationUnit( organisationUnit );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );

        programService.addProgram( program );

        DataElement dataElement = createDataElement( 'A' );
        dataElementService.addDataElement( dataElement );

        ProgramStage stageA = createProgramStage( 'A', program );
        stageA.setSortOrder( 1 );
        stageA.addDataElement( dataElement, 1 );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = createProgramStage( 'B', program );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstance = new ProgramInstance( enrollmentDate, incidenDate, entityInstance, program );
        programInstance.setUid( "UID-A" );
        programInstance.setOrganisationUnit( organisationUnit );

        programInstanceId = programInstanceService.addProgramInstance( programInstance );

        ProgramStageInstance stageInstanceA = programStageInstanceService
            .createProgramStageInstance( programInstance, stageA, testDate1.toDate(), testDate2.toDate(),
                organisationUnit );
        ProgramStageInstance stageInstanceB = programStageInstanceService
            .createProgramStageInstance( programInstance, stageA, testDate1.toDate(), testDate2.toDate(),
                organisationUnit );

        programStageInstanceService.addProgramStageInstance( stageInstanceA );
        programStageInstanceService.addProgramStageInstance( stageInstanceB );

        TrackedEntityDataValue trackedEntityDataValueA = new TrackedEntityDataValue( stageInstanceA, dataElement,
            "99" );

        trackedEntityDataValueService.saveTrackedEntityDataValue( trackedEntityDataValueA );

        programInstance.setProgramStageInstances( Sets.newHashSet( stageInstanceA, stageInstanceB ) );
        programInstanceService.updateProgramInstance( programInstance );
    }

    @Test
    public void testDeleteSoftDeletedProgramInstanceWithAProgramMessage()
    {
        ProgramMessage message = new ProgramMessage( "subject", "text", new ProgramMessageRecipients(),
            Sets.newHashSet(),
            programInstance );

        programMessageService.saveProgramMessage( message );

        assertNotNull( programInstanceService.getProgramInstance( programInstanceId ) );

        programInstanceService.deleteProgramInstance( programInstance );

        assertNull( programInstanceService.getProgramInstance( programInstanceId ) );

        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );

        maintenanceService.deleteSoftDeletedProgramInstances();

        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
    }
}
