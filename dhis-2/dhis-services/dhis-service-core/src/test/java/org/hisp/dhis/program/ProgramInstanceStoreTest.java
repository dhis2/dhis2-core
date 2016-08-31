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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Chau Thu Tran
 */
public class ProgramInstanceStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramInstanceStore programInstanceStore;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    private Date incidentDate;

    private Date enrollmentDate;

    private Program programA;

    private Program programB;

    private Program programC;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramInstance programInstanceC;

    private ProgramInstance programInstanceD;

    private TrackedEntityInstance entityInstanceA;

    private Collection<Integer> orgunitIds;

    @Override
    public void setUpTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        int idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        int idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );

        TrackedEntityInstanceReminder reminderA = createTrackedEntityInstanceReminder( 'A', 0,
            "Test program message template", TrackedEntityInstanceReminder.ENROLLEMENT_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE, null, TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH );

        TrackedEntityInstanceReminder reminderB = createTrackedEntityInstanceReminder( 'B', 0,
            "Test program message template", TrackedEntityInstanceReminder.ENROLLEMENT_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE, TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH );

        Set<TrackedEntityInstanceReminder> reminders = new HashSet<>();
        reminders.add( reminderA );
        reminders.add( reminderB );
        programA.setInstanceReminders( reminders );

        programService.addProgram( programA );

        ProgramStage stageA = new ProgramStage( "StageA", programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = new ProgramStage( "StageB", programA );
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

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidentDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-A" );

        programInstanceB = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programB );
        programInstanceB.setUid( "UID-B" );
        programInstanceB.setStatus( ProgramStatus.CANCELLED );

        programInstanceC = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceA, programC );
        programInstanceC.setUid( "UID-C" );
        programInstanceC.setStatus( ProgramStatus.COMPLETED );

        programInstanceD = new ProgramInstance( enrollmentDate, incidentDate, entityInstanceB, programA );
        programInstanceD.setUid( "UID-D" );
    }

    @Test
    public void testProgramStageInstanceExists()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );

        assertTrue( programInstanceStore.exists( programInstanceA.getUid() ) );
        assertTrue( programInstanceStore.exists( programInstanceB.getUid() ) );
        assertFalse( programInstanceStore.exists( "aaaabbbbccc" ) );
        assertFalse( programInstanceStore.exists( null ) );
    }

    @Test
    public void testGetProgramInstancesByProgram()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceStore.get( programA );
        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceD ) );

        programInstances = programInstanceStore.get( programB );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceB ) );
    }

    @Test
    public void testGetProgramInstancesByProgramList()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceC );
        programInstanceStore.save( programInstanceD );

        List<Program> programs = new ArrayList<>();
        programs.add( programA );
        programs.add( programB );

        List<ProgramInstance> programInstances = programInstanceStore.get( programs );
        assertEquals( 3, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceB ) );
        assertTrue( programInstances.contains( programInstanceD ) );
    }

    @Test
    public void testGetProgramInstancesByEntityInstanceProgramStatus()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceC );
        programInstanceStore.save( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceStore.get( entityInstanceA, programC, ProgramStatus.COMPLETED );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceC ) );

        programInstances = programInstanceStore.get( entityInstanceA, programA, ProgramStatus.ACTIVE );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
    }

    @Test
    public void testGetProgramInstancesByOuProgram()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceC );
        programInstanceStore.save( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceStore.get( programA, organisationUnitA, 0, 10 );
        assertEquals( 1, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
    }

    @Test
    public void testGetProgramInstancesByOuListProgramPeriod()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceStore.get( programA, orgunitIds, incidentDate,
            enrollmentDate, null, null );
        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceD ) );
    }

    @Test
    public void testCountProgramInstancesByOuListProgramPeriod()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );

        int count = programInstanceStore.count( programA, orgunitIds, incidentDate, enrollmentDate );
        assertEquals( 2, count );
    }

    @Test
    public void testGetProgramInstancesByOuListProgramStatusPeriod()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );

        List<ProgramInstance> programInstances = programInstanceStore.getByStatus( ProgramStatus.ACTIVE, programA,
            orgunitIds, incidentDate, enrollmentDate );

        assertEquals( 2, programInstances.size() );
        assertTrue( programInstances.contains( programInstanceA ) );
        assertTrue( programInstances.contains( programInstanceD ) );
    }

    @Test
    public void testCountProgramInstancesByStatus()
    {
        programInstanceStore.save( programInstanceA );
        programInstanceStore.save( programInstanceB );
        programInstanceStore.save( programInstanceD );

        int count = programInstanceStore.countByStatus( ProgramStatus.ACTIVE, programA, orgunitIds, incidentDate, enrollmentDate );
        assertEquals( 2, count );
    }
}
