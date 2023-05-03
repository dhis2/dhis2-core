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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Chau Thu Tran
 */
class ProgramInstanceServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private EventService eventService;

    private Date incidentDate;

    private Date enrollmentDate;

    private Program programA;

    private Program programB;

    private Program programC;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private Event eventA;

    private Enrollment enrollmentA;

    private Enrollment enrollmentB;

    private Enrollment enrollmentC;

    private Enrollment enrollmentD;

    private TrackedEntityInstance entityInstanceA;

    @Override
    public void setUpTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );
        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programService.addProgram( programA );
        ProgramStage stageA = createProgramStage( 'A', programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        ProgramStage stageB = createProgramStage( 'B', programA );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );
        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programService.addProgram( programB );
        programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programService.addProgram( programC );
        entityInstanceA = createTrackedEntityInstance( organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );
        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidentDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();
        enrollmentA = new Enrollment( enrollmentDate, incidentDate, entityInstanceA, programA );
        enrollmentA.setUid( "UID-A" );
        enrollmentA.setOrganisationUnit( organisationUnitA );
        eventA = new Event( enrollmentA, stageA );
        eventA.setUid( "UID-PSI-A" );
        eventA.setOrganisationUnit( organisationUnitA );
        enrollmentB = new Enrollment( enrollmentDate, incidentDate, entityInstanceA, programB );
        enrollmentB.setUid( "UID-B" );
        enrollmentB.setStatus( ProgramStatus.CANCELLED );
        enrollmentB.setOrganisationUnit( organisationUnitB );
        enrollmentC = new Enrollment( enrollmentDate, incidentDate, entityInstanceA, programC );
        enrollmentC.setUid( "UID-C" );
        enrollmentC.setStatus( ProgramStatus.COMPLETED );
        enrollmentC.setOrganisationUnit( organisationUnitA );
        enrollmentD = new Enrollment( enrollmentDate, incidentDate, entityInstanceB, programA );
        enrollmentD.setUid( "UID-D" );
        enrollmentD.setOrganisationUnit( organisationUnitB );
    }

    @Test
    void testAddProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idB = programInstanceService.addProgramInstance( enrollmentB );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    void testDeleteProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idB = programInstanceService.addProgramInstance( enrollmentB );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );
        programInstanceService.deleteProgramInstance( enrollmentA );
        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( programInstanceService.getProgramInstance( idB ) );
        programInstanceService.deleteProgramInstance( enrollmentB );
        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertNull( programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    void testSoftDeleteProgramInstanceAndLinkedEvent()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long eventIdA = eventService.addEvent( eventA );
        enrollmentA.setEvents( Sets.newHashSet( eventA ) );
        programInstanceService.updateProgramInstance( enrollmentA );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        assertNotNull( eventService.getEvent( eventIdA ) );
        programInstanceService.deleteProgramInstance( enrollmentA );
        assertNull( programInstanceService.getProgramInstance( idA ) );
        assertNull( eventService.getEvent( eventIdA ) );
    }

    @Test
    void testUpdateProgramInstance()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        assertNotNull( programInstanceService.getProgramInstance( idA ) );
        enrollmentA.setIncidentDate( enrollmentDate );
        programInstanceService.updateProgramInstance( enrollmentA );
        assertEquals( enrollmentDate, programInstanceService.getProgramInstance( idA ).getIncidentDate() );
    }

    @Test
    void testGetProgramInstanceById()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idB = programInstanceService.addProgramInstance( enrollmentB );
        assertEquals( enrollmentA, programInstanceService.getProgramInstance( idA ) );
        assertEquals( enrollmentB, programInstanceService.getProgramInstance( idB ) );
    }

    @Test
    void testGetProgramInstanceByUid()
    {
        programInstanceService.addProgramInstance( enrollmentA );
        programInstanceService.addProgramInstance( enrollmentB );
        assertEquals( "UID-A", programInstanceService.getProgramInstance( "UID-A" ).getUid() );
        assertEquals( "UID-B", programInstanceService.getProgramInstance( "UID-B" ).getUid() );
    }

    @Test
    void testGetProgramInstancesByProgram()
    {
        programInstanceService.addProgramInstance( enrollmentA );
        programInstanceService.addProgramInstance( enrollmentB );
        programInstanceService.addProgramInstance( enrollmentD );
        List<Enrollment> enrollments = programInstanceService.getProgramInstances( programA );
        assertEquals( 2, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
        assertTrue( enrollments.contains( enrollmentD ) );
        enrollments = programInstanceService.getProgramInstances( programB );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentB ) );
    }

    @Test
    void testGetProgramInstancesByEntityInstanceProgramStatus()
    {
        programInstanceService.addProgramInstance( enrollmentA );
        Enrollment enrollment1 = programInstanceService.enrollTrackedEntityInstance( entityInstanceA,
            programA, enrollmentDate, incidentDate, organisationUnitA );
        enrollment1.setStatus( ProgramStatus.COMPLETED );
        programInstanceService.updateProgramInstance( enrollment1 );
        Enrollment enrollment2 = programInstanceService.enrollTrackedEntityInstance( entityInstanceA,
            programA, enrollmentDate, incidentDate, organisationUnitA );
        enrollment2.setStatus( ProgramStatus.COMPLETED );
        programInstanceService.updateProgramInstance( enrollment2 );
        List<Enrollment> enrollments = programInstanceService.getProgramInstances( entityInstanceA, programA,
            ProgramStatus.COMPLETED );
        assertEquals( 2, enrollments.size() );
        assertTrue( enrollments.contains( enrollment1 ) );
        assertTrue( enrollments.contains( enrollment2 ) );
        enrollments = programInstanceService.getProgramInstances( entityInstanceA, programA,
            ProgramStatus.ACTIVE );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
    }

    @Test
    void testGetProgramInstancesByOuProgram()
    {
        programInstanceService.addProgramInstance( enrollmentA );
        programInstanceService.addProgramInstance( enrollmentC );
        programInstanceService.addProgramInstance( enrollmentD );
        List<Enrollment> enrollments = programInstanceService
            .getProgramInstances( new ProgramInstanceQueryParams().setProgram( programA )
                .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED ) );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
    }

    @Test
    void testEnrollTrackedEntityInstance()
    {
        Enrollment enrollment = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programB,
            enrollmentDate, incidentDate, organisationUnitA );
        assertNotNull( programInstanceService.getProgramInstance( enrollment.getId() ) );
    }

    @Test
    void testCompleteProgramInstanceStatus()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idD = programInstanceService.addProgramInstance( enrollmentD );
        programInstanceService.completeProgramInstanceStatus( enrollmentA );
        programInstanceService.completeProgramInstanceStatus( enrollmentD );
        assertEquals( ProgramStatus.COMPLETED, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.COMPLETED, programInstanceService.getProgramInstance( idD ).getStatus() );
    }

    @Test
    void testIncompleteProgramInstanceStatus()
    {
        enrollmentA.setStatus( ProgramStatus.COMPLETED );
        enrollmentD.setStatus( ProgramStatus.COMPLETED );
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idD = programInstanceService.addProgramInstance( enrollmentD );
        programInstanceService.incompleteProgramInstanceStatus( enrollmentA );
        programInstanceService.incompleteProgramInstanceStatus( enrollmentD );
        assertEquals( ProgramStatus.ACTIVE, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.ACTIVE, programInstanceService.getProgramInstance( idD ).getStatus() );
    }

    @Test
    void testCancelProgramInstanceStatus()
    {
        long idA = programInstanceService.addProgramInstance( enrollmentA );
        long idD = programInstanceService.addProgramInstance( enrollmentD );
        programInstanceService.cancelProgramInstanceStatus( enrollmentA );
        programInstanceService.cancelProgramInstanceStatus( enrollmentD );
        assertEquals( ProgramStatus.CANCELLED, programInstanceService.getProgramInstance( idA ).getStatus() );
        assertEquals( ProgramStatus.CANCELLED, programInstanceService.getProgramInstance( idD ).getStatus() );
    }
}
