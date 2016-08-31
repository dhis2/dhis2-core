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
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.mock.MockI18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceReminder;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Chau Thu Tran
 */
public class ProgramStageInstanceServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private ProgramStage stageC;

    private ProgramStage stageD;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStageDataElement stageDataElementA;

    private ProgramStageDataElement stageDataElementB;

    private ProgramStageDataElement stageDataElementC;

    private ProgramStageDataElement stageDataElementD;

    private Date incidenDate;

    private Date enrollmentDate;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private ProgramStageInstance programStageInstanceC;

    private ProgramStageInstance programStageInstanceD1;

    private ProgramStageInstance programStageInstanceD2;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private Program programA;

    private MockI18nFormat mockFormat;

    @Override
    public void setUpTest()
    {
        mockFormat = new MockI18nFormat();

        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        attribute.setValueType( ValueType.PHONE_NUMBER );
        attributeService.addTrackedEntityAttribute( attribute );

        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstanceA, attribute );
        attributeValue.setValue( "123456789" );
        attributeValueService.addTrackedEntityAttributeValue( attributeValue );

        entityInstanceA.getTrackedEntityAttributeValues().add( attributeValue );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA );

        /**
         * Program A
         */
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programService.addProgram( programA );

        stageA = createProgramStage( 'A', 0 );
        stageA.setProgram( programA );
        stageA.setSortOrder( 1 );

        TrackedEntityInstanceReminder reminderA = createTrackedEntityInstanceReminder( 'A', 0,
            "Test program stage message template", TrackedEntityInstanceReminder.DUE_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE, null, TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH );

        TrackedEntityInstanceReminder reminderB = createTrackedEntityInstanceReminder( 'B', 0,
            "Test program stage message template", TrackedEntityInstanceReminder.DUE_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE, TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH );

        Set<TrackedEntityInstanceReminder> reminders = new HashSet<>();
        reminders.add( reminderA );
        reminders.add( reminderB );
        stageA.setReminders( reminders );

        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "B", programA );
        stageB.setSortOrder( 2 );
        TrackedEntityInstanceReminder reminderC = createTrackedEntityInstanceReminder( 'C', 0,
            "Test program stage message template", TrackedEntityInstanceReminder.DUE_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE, TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_BOTH );

        reminders = new HashSet<>();
        reminders.add( reminderC );
        stageB.setReminders( reminders );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        stageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        stageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
        stageDataElementC = new ProgramStageDataElement( stageB, dataElementA, false, 1 );
        stageDataElementD = new ProgramStageDataElement( stageB, dataElementB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );

        /**
         * Program B
         */

        Program programB = createProgram( 'B', new HashSet<>(), organisationUnitB );
        programService.addProgram( programB );

        stageC = new ProgramStage( "C", programB );
        stageC.setSortOrder( 1 );
        programStageService.saveProgramStage( stageC );

        stageD = new ProgramStage( "D", programB );
        stageB.setSortOrder( 2 );
        stageC.setRepeatable( true );
        programStageService.saveProgramStage( stageD );

        programStages = new HashSet<>();
        programStages.add( stageC );
        programStages.add( stageD );
        programB.setProgramStages( programStages );
        programService.updateProgram( programB );

        /**
         * Program Instance and Program Stage Instance
         */

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-PIA" );
        programInstanceService.addProgramInstance( programInstanceA );

        programInstanceB = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceB, programB );
        programInstanceService.addProgramInstance( programInstanceB );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, stageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setUid( "UID-A" );

        programStageInstanceB = new ProgramStageInstance( programInstanceA, stageB );
        programStageInstanceB.setDueDate( enrollmentDate );
        programStageInstanceB.setUid( "UID-B" );

        programStageInstanceC = new ProgramStageInstance( programInstanceB, stageC );
        programStageInstanceC.setDueDate( enrollmentDate );
        programStageInstanceC.setUid( "UID-C" );

        programStageInstanceD1 = new ProgramStageInstance( programInstanceB, stageD );
        programStageInstanceD1.setDueDate( enrollmentDate );
        programStageInstanceD1.setUid( "UID-D1" );

        programStageInstanceD2 = new ProgramStageInstance( programInstanceB, stageD );
        programStageInstanceD2.setDueDate( enrollmentDate );
        programStageInstanceD2.setUid( "UID-D2" );
    }

    @Test
    public void testAddProgramStageInstance()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        int idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testDeleteProgramStageInstance()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        int idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );

        programStageInstanceService.deleteProgramStageInstance( programStageInstanceA );

        assertNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNotNull( programStageInstanceService.getProgramStageInstance( idB ) );

        programStageInstanceService.deleteProgramStageInstance( programStageInstanceB );

        assertNull( programStageInstanceService.getProgramStageInstance( idA ) );
        assertNull( programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testUpdateProgramStageInstance()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        assertNotNull( programStageInstanceService.getProgramStageInstance( idA ) );

        programStageInstanceA.setName( "B" );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceA );

        assertEquals( "B", programStageInstanceService.getProgramStageInstance( idA ).getName() );
    }

    @Test
    public void testGetProgramStageInstanceById()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        int idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( idA ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( idB ) );
    }

    @Test
    public void testGetProgramStageInstanceByUid()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        int idB = programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( idA ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( idB ) );

        assertEquals( programStageInstanceA, programStageInstanceService.getProgramStageInstance( "UID-A" ) );
        assertEquals( programStageInstanceB, programStageInstanceService.getProgramStageInstance( "UID-B" ) );
    }

    @Test
    public void testGetProgramStageInstanceByProgramInstanceStage()
    {
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance(
            programInstanceA, stageA );
        assertEquals( programStageInstanceA, programStageInstance );

        programStageInstance = programStageInstanceService.getProgramStageInstance( programInstanceA, stageB );
        assertEquals( programStageInstanceB, programStageInstance );
    }

    @Test
    public void testGetProgramStageInstancesByInstanceListComplete()
    {
        programStageInstanceA.setStatus( EventStatus.COMPLETED );
        programStageInstanceB.setStatus( EventStatus.ACTIVE );
        programStageInstanceC.setStatus( EventStatus.COMPLETED );
        programStageInstanceD1.setStatus( EventStatus.ACTIVE );

        programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );
        programStageInstanceService.addProgramStageInstance( programStageInstanceC );
        programStageInstanceService.addProgramStageInstance( programStageInstanceD1 );

        List<ProgramInstance> programInstances = new ArrayList<>();
        programInstances.add( programInstanceA );
        programInstances.add( programInstanceB );

        List<ProgramStageInstance> stageInstances = programStageInstanceService.getProgramStageInstances(
            programInstances, EventStatus.COMPLETED );
        assertEquals( 2, stageInstances.size() );
        assertTrue( stageInstances.contains( programStageInstanceA ) );
        assertTrue( stageInstances.contains( programStageInstanceC ) );

        stageInstances = programStageInstanceService.getProgramStageInstances( programInstances, EventStatus.ACTIVE );
        assertEquals( 2, stageInstances.size() );
        assertTrue( stageInstances.contains( programStageInstanceB ) );
        assertTrue( stageInstances.contains( programStageInstanceD1 ) );
    }

    @Test
    public void testGetProgramStageInstancesByStatus()
    {
        programStageInstanceA.setStatus( EventStatus.COMPLETED );
        programStageInstanceB.setStatus( EventStatus.ACTIVE );
        programStageInstanceC.setStatus( EventStatus.COMPLETED );
        programStageInstanceD1.setStatus( EventStatus.ACTIVE );

        programStageInstanceService.addProgramStageInstance( programStageInstanceA );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );
        programStageInstanceService.addProgramStageInstance( programStageInstanceC );
        programStageInstanceService.addProgramStageInstance( programStageInstanceD1 );

        List<ProgramStageInstance> stageInstances = programStageInstanceService.getProgramStageInstances( entityInstanceA,
            EventStatus.COMPLETED );
        assertEquals( 1, stageInstances.size() );
        assertTrue( stageInstances.contains( programStageInstanceA ) );

        stageInstances = programStageInstanceService.getProgramStageInstances( entityInstanceA, EventStatus.ACTIVE );
        assertEquals( 1, stageInstances.size() );
        assertTrue( stageInstances.contains( programStageInstanceB ) );
    }

    @Test
    public void testCompleteProgramStageInstance()
    {
        int idA = programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programStageInstanceService.completeProgramStageInstance( programStageInstanceA, false, mockFormat );

        assertEquals( true, programStageInstanceService.getProgramStageInstance( idA ).isCompleted() );
    }

    @Test
    public void testCreateProgramStageInstance()
    {
        programA.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        programService.updateProgram( programA );

        ProgramStageInstance programStageInstance = programStageInstanceService.createProgramStageInstance( entityInstanceA,
            programA, enrollmentDate, organisationUnitA );

        assertNotNull( programStageInstanceService.getProgramStageInstance( programStageInstance.getUid() ) );
    }
}