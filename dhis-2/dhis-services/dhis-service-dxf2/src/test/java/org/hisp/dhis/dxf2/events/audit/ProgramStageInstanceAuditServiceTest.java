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
package org.hisp.dhis.dxf2.events.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceAudit;
import org.hisp.dhis.program.ProgramStageInstanceAuditParam;
import org.hisp.dhis.program.ProgramStageInstanceAuditService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
public class ProgramStageInstanceAuditServiceTest extends DhisSpringTest
{
    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private OrganisationUnit organisationUnit;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private TrackedEntityInstance entityInstance;

    @Autowired
    private ProgramStageInstanceAuditService auditService;

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

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "StageA", program );
        stageA.setSortOrder( 1 );
        stageA.setRepeatable( true );
        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "StageB", program );
        stageB.setSortOrder( 2 );
        stageB.setRepeatable( true );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        entityInstance = createTrackedEntityInstance( organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            new Date(), new Date(), organisationUnit );

    }

    @Test
    public void testAuditForProgramStageInstanceDeletion()
    {
        programStageInstance = programStageInstanceService.createProgramStageInstance( programInstance,
            stageA, new Date(), new Date(), organisationUnit );

        programStageInstanceService.deleteProgramStageInstance( programStageInstance );

        ProgramStageInstanceAuditParam params = ProgramStageInstanceAuditParam.builder().skipPaging( true )
            .auditType( AuditType.DELETE ).build();

        List<ProgramStageInstanceAudit> audits = auditService.getAllProgramStageInstanceAudits( params );

        assertFalse( audits.isEmpty() );
        assertEquals( programStageInstance.getUid(), audits.get( 0 ).getProgramStageInstance() );
        assertEquals( AuditType.DELETE, audits.get( 0 ).getAuditType() );
    }
}
