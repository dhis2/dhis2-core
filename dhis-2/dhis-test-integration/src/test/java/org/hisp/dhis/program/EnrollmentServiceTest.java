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
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Chau Thu Tran
 */
class EnrollmentServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private EnrollmentService enrollmentService;

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

    private TrackedEntity entityInstanceA;

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
        TrackedEntity entityInstanceB = createTrackedEntityInstance( organisationUnitB );
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
    void testAddEnrollment()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idB = enrollmentService.addEnrollment( enrollmentB );
        assertNotNull( enrollmentService.getEnrollment( idA ) );
        assertNotNull( enrollmentService.getEnrollment( idB ) );
    }

    @Test
    void testDeleteEnrollment()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idB = enrollmentService.addEnrollment( enrollmentB );
        assertNotNull( enrollmentService.getEnrollment( idA ) );
        assertNotNull( enrollmentService.getEnrollment( idB ) );
        enrollmentService.deleteEnrollment( enrollmentA );
        assertNull( enrollmentService.getEnrollment( idA ) );
        assertNotNull( enrollmentService.getEnrollment( idB ) );
        enrollmentService.deleteEnrollment( enrollmentB );
        assertNull( enrollmentService.getEnrollment( idA ) );
        assertNull( enrollmentService.getEnrollment( idB ) );
    }

    @Test
    void testSoftDeleteEnrollmentAndLinkedEvent()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long eventIdA = eventService.addEvent( eventA );
        enrollmentA.setEvents( Sets.newHashSet( eventA ) );
        enrollmentService.updateEnrollment( enrollmentA );
        assertNotNull( enrollmentService.getEnrollment( idA ) );
        assertNotNull( eventService.getEvent( eventIdA ) );
        enrollmentService.deleteEnrollment( enrollmentA );
        assertNull( enrollmentService.getEnrollment( idA ) );
        assertNull( eventService.getEvent( eventIdA ) );
    }

    @Test
    void testUpdateEnrollment()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        assertNotNull( enrollmentService.getEnrollment( idA ) );
        enrollmentA.setIncidentDate( enrollmentDate );
        enrollmentService.updateEnrollment( enrollmentA );
        assertEquals( enrollmentDate, enrollmentService.getEnrollment( idA ).getIncidentDate() );
    }

    @Test
    void testGetEnrollmentById()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idB = enrollmentService.addEnrollment( enrollmentB );
        assertEquals( enrollmentA, enrollmentService.getEnrollment( idA ) );
        assertEquals( enrollmentB, enrollmentService.getEnrollment( idB ) );
    }

    @Test
    void testGetEnrollmentByUid()
    {
        enrollmentService.addEnrollment( enrollmentA );
        enrollmentService.addEnrollment( enrollmentB );
        assertEquals( "UID-A", enrollmentService.getEnrollment( "UID-A" ).getUid() );
        assertEquals( "UID-B", enrollmentService.getEnrollment( "UID-B" ).getUid() );
    }

    @Test
    void testGetEnrollmentsByProgram()
    {
        enrollmentService.addEnrollment( enrollmentA );
        enrollmentService.addEnrollment( enrollmentB );
        enrollmentService.addEnrollment( enrollmentD );
        List<Enrollment> enrollments = enrollmentService.getEnrollments( programA );
        assertEquals( 2, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
        assertTrue( enrollments.contains( enrollmentD ) );
        enrollments = enrollmentService.getEnrollments( programB );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentB ) );
    }

    @Test
    void testGetEnrollmentsByEntityInstanceProgramStatus()
    {
        enrollmentService.addEnrollment( enrollmentA );
        Enrollment enrollment1 = enrollmentService.enrollTrackedEntityInstance( entityInstanceA,
            programA, enrollmentDate, incidentDate, organisationUnitA );
        enrollment1.setStatus( ProgramStatus.COMPLETED );
        enrollmentService.updateEnrollment( enrollment1 );
        Enrollment enrollment2 = enrollmentService.enrollTrackedEntityInstance( entityInstanceA,
            programA, enrollmentDate, incidentDate, organisationUnitA );
        enrollment2.setStatus( ProgramStatus.COMPLETED );
        enrollmentService.updateEnrollment( enrollment2 );
        List<Enrollment> enrollments = enrollmentService.getEnrollments( entityInstanceA, programA,
            ProgramStatus.COMPLETED );
        assertEquals( 2, enrollments.size() );
        assertTrue( enrollments.contains( enrollment1 ) );
        assertTrue( enrollments.contains( enrollment2 ) );
        enrollments = enrollmentService.getEnrollments( entityInstanceA, programA,
            ProgramStatus.ACTIVE );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
    }

    @Test
    void testGetEnrollmentsByOuProgram()
    {
        enrollmentService.addEnrollment( enrollmentA );
        enrollmentService.addEnrollment( enrollmentC );
        enrollmentService.addEnrollment( enrollmentD );
        List<Enrollment> enrollments = enrollmentService
            .getEnrollments( new EnrollmentQueryParams().setProgram( programA )
                .setOrganisationUnits( Sets.newHashSet( organisationUnitA ) )
                .setOrganisationUnitMode( OrganisationUnitSelectionMode.SELECTED ) );
        assertEquals( 1, enrollments.size() );
        assertTrue( enrollments.contains( enrollmentA ) );
    }

    @Test
    void testEnrollTrackedEntityInstance()
    {
        Enrollment enrollment = enrollmentService.enrollTrackedEntityInstance( entityInstanceA, programB,
            enrollmentDate, incidentDate, organisationUnitA );
        assertNotNull( enrollmentService.getEnrollment( enrollment.getId() ) );
    }

    @Test
    void testCompleteEnrollmentStatus()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idD = enrollmentService.addEnrollment( enrollmentD );
        enrollmentService.completeEnrollmentStatus( enrollmentA );
        enrollmentService.completeEnrollmentStatus( enrollmentD );
        assertEquals( ProgramStatus.COMPLETED, enrollmentService.getEnrollment( idA ).getStatus() );
        assertEquals( ProgramStatus.COMPLETED, enrollmentService.getEnrollment( idD ).getStatus() );
    }

    @Test
    void testIncompleteEnrollmentStatus()
    {
        enrollmentA.setStatus( ProgramStatus.COMPLETED );
        enrollmentD.setStatus( ProgramStatus.COMPLETED );
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idD = enrollmentService.addEnrollment( enrollmentD );
        enrollmentService.incompleteEnrollmentStatus( enrollmentA );
        enrollmentService.incompleteEnrollmentStatus( enrollmentD );
        assertEquals( ProgramStatus.ACTIVE, enrollmentService.getEnrollment( idA ).getStatus() );
        assertEquals( ProgramStatus.ACTIVE, enrollmentService.getEnrollment( idD ).getStatus() );
    }

    @Test
    void testCancelEnrollmentStatus()
    {
        long idA = enrollmentService.addEnrollment( enrollmentA );
        long idD = enrollmentService.addEnrollment( enrollmentD );
        enrollmentService.cancelEnrollmentStatus( enrollmentA );
        enrollmentService.cancelEnrollmentStatus( enrollmentD );
        assertEquals( ProgramStatus.CANCELLED, enrollmentService.getEnrollment( idA ).getStatus() );
        assertEquals( ProgramStatus.CANCELLED, enrollmentService.getEnrollment( idD ).getStatus() );
    }
}
