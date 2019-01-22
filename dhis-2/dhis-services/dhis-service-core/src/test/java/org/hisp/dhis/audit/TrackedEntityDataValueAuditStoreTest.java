package org.hisp.dhis.audit;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.eventdatavalue.EventDataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditStore;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class TrackedEntityDataValueAuditStoreTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityDataValueAuditStore auditStore;

    @Autowired
    private EventDataValueService eventDataValueService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private OrganisationUnit organisationUnit;

    private Program program;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "StageA", program );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "StageB", program );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            new Date(), new Date(), organisationUnit );
        ProgramStageInstance stageInstance = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, new Date(), new Date(), organisationUnit );

        String storedBy = "test-user";
        EventDataValue dataValueA = new EventDataValue( dataElementA.getUid(), "1", storedBy );
        EventDataValue dataValueB = new EventDataValue( dataElementB.getUid(), "2", storedBy );

        Map<DataElement, EventDataValue> dataElementEventDataValueMap = new HashMap<>();
        dataElementEventDataValueMap.put( dataElementA, dataValueA );
        dataElementEventDataValueMap.put( dataElementB, dataValueB );
        eventDataValueService.validateAuditAndHandleFilesForEventDataValuesSave( stageInstance, dataElementEventDataValueMap );

        programStageInstanceService.updateProgramStageInstance( stageInstance );
    }

    @Test
    public void testGetTrackedEntityDataValueAudits()
    {
        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            new Date(), new Date(), organisationUnit );
        ProgramStageInstance stageInstance = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, new Date(), new Date(), organisationUnit );

        String storedBy = "test-user";
        EventDataValue dataValueA = new EventDataValue( dataElementA.getUid(), "1", storedBy );
        EventDataValue dataValueB = new EventDataValue( dataElementB.getUid(), "2", storedBy );

        Map<DataElement, EventDataValue> dataElementEventDataValueMap = new HashMap<>();
        dataElementEventDataValueMap.put( dataElementA, dataValueA );
        dataElementEventDataValueMap.put( dataElementB, dataValueB );
        eventDataValueService.validateAuditAndHandleFilesForEventDataValuesSave( stageInstance, dataElementEventDataValueMap );

        programStageInstanceService.updateProgramStageInstance( stageInstance );

        TrackedEntityDataValueAudit dataValueAudit = new TrackedEntityDataValueAudit( dataElementA, stageInstance, dataValueA.getAuditValue(), "userA", dataValueA.getProvidedElsewhere(), AuditType.UPDATE );
        auditStore.addTrackedEntityDataValueAudit( dataValueAudit );

        Assert.assertEquals( 1, auditStore.getTrackedEntityDataValueAudits( Lists.newArrayList( dataElementA ), Lists.newArrayList( stageInstance ), AuditType.UPDATE ).size() );
        Assert.assertEquals( 1, auditStore.countTrackedEntityDataValueAudits(  Lists.newArrayList( dataElementA, dataElementB ), Lists.newArrayList( stageInstance ), AuditType.UPDATE ) );
    }
}
