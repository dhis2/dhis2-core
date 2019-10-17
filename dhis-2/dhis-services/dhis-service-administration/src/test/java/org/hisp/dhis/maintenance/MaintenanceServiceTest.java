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
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DeliveryChannel;
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
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Enrico Colasante
 */
public class MaintenanceServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private TrackedEntityDataValueAuditService trackedEntityDataValueAuditService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private MaintenanceService maintenanceService;

    private Date incidenDate;

    private Date enrollmentDate;

    private Program program;

    private OrganisationUnit organisationUnit;

    private ProgramInstance programInstance;

    private TrackedEntityInstance entityInstance;

    private Collection<Long> orgunitIds;

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

        ProgramStage stageA = createProgramStage( 'A', program );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = createProgramStage( 'B', program );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        entityInstance = createTrackedEntityInstance( organisationUnit );
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

        programInstanceService.addProgramInstance( programInstance );
    }

    @Test
    public void testDeleteSoftDeletedProgramInstanceWithAProgramMessage()
    {
        ProgramMessageRecipients programMessageRecipients = new ProgramMessageRecipients();
        programMessageRecipients.setEmailAddresses( Sets.newHashSet( "testemail" ) );
        programMessageRecipients.setPhoneNumbers( Sets.newHashSet( "testphone" ) );
        programMessageRecipients.setOrganisationUnit( organisationUnit );
        programMessageRecipients.setTrackedEntityInstance( entityInstance );

        ProgramMessage message = new ProgramMessage( "subject", "text", programMessageRecipients,
            Sets.newHashSet( DeliveryChannel.EMAIL ),
            programInstance );

        long idA = programInstanceService.addProgramInstance( programInstance );

        programMessageService.saveProgramMessage( message );

        assertNotNull( programInstanceService.getProgramInstance( idA ) );

        programInstanceService.deleteProgramInstance( programInstance );

        assertNull( programInstanceService.getProgramInstance( idA ) );

        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );

        maintenanceService.deleteSoftDeletedProgramInstances();

        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
    }

    @Test
    public void testDeleteSoftDeletedProgramInstanceLinkedToATrackedEntityDataValueAudit()
    {
        DataElement dataElement = createDataElement( 'A' );
        dataElementService.addDataElement( dataElement );

        ProgramStageInstance programStageInstanceA = new ProgramStageInstance( programInstance,
            program.getProgramStageByStage( 1 ) );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setUid( "UID-A" );

        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        TrackedEntityDataValueAudit trackedEntityDataValueAudit = new TrackedEntityDataValueAudit( dataElement,
            programStageInstanceA, "value", "modifiedBy", false, AuditType.UPDATE );

        trackedEntityDataValueAuditService.addTrackedEntityDataValueAudit( trackedEntityDataValueAudit );

        long idA = programInstanceService.addProgramInstance( programInstance );

        assertNotNull( programInstanceService.getProgramInstance( idA ) );

        programInstanceService.deleteProgramInstance( programInstance );

        assertNull( programInstanceService.getProgramInstance( idA ) );

        assertTrue( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );

        maintenanceService.deleteSoftDeletedProgramInstances();

        assertFalse( programInstanceService.programInstanceExistsIncludingDeleted( programInstance.getUid() ) );
    }
}
