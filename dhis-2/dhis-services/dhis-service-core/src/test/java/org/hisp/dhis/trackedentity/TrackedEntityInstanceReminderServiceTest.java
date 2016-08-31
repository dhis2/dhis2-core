package org.hisp.dhis.trackedentity;

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
import org.hisp.dhis.mock.MockI18nFormat;
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
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Chau Thu Tran
 */
public class TrackedEntityInstanceReminderServiceTest
    extends DhisSpringTest
{
    @Autowired
    private TrackedEntityInstanceReminderService reminderService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService userService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    private MockI18nFormat mockFormat;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private TrackedEntityInstanceReminder reminderA;

    private TrackedEntityInstanceReminder reminderB;

    private TrackedEntityInstanceReminder reminderC;

    private TrackedEntityInstance entityInstance;

    private ProgramStage stageA;

    private User user;

    @Override
    public void setUpTest()
    {
        mockFormat = new MockI18nFormat();

        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        Set<OrganisationUnit> orgUnits = new HashSet<>();
        orgUnits.add( organisationUnit );

        user = new User();
        user.setAutoFields();
        user.setSurname( "A" );
        user.setFirstName( "B" );
        user.setPhoneNumber( "111-222-333" );
        user.updateOrganisationUnits( orgUnits );
        userService.addUser( user );

        entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        reminderA = new TrackedEntityInstanceReminder( "A", 0, "Test program message template",
            TrackedEntityInstanceReminder.ENROLLEMENT_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE,
            TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_DIRECT_SMS );
        Set<TrackedEntityInstanceReminder> reminders = new HashSet<>();
        reminders.add( reminderA );
        program.setInstanceReminders( reminders );
        programService.addProgram( program );

        stageA = new ProgramStage( "A", program );
        reminderB = new TrackedEntityInstanceReminder( "B", 0, "Test event template",
            TrackedEntityInstanceReminder.DUE_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_TRACKED_ENTITY_INSTANCE,
            TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_DIRECT_SMS );
        reminders = new HashSet<>();
        reminders.add( reminderB );
        stageA.setReminders( reminders );
        programStageService.saveProgramStage( stageA );

        ProgramStage stageB = new ProgramStage( "B", program );
        reminderC = new TrackedEntityInstanceReminder( "C", 0, "Test event template",
            TrackedEntityInstanceReminder.DUE_DATE_TO_COMPARE,
            TrackedEntityInstanceReminder.SEND_TO_ALL_USERS_IN_ORGUGNIT_REGISTERED,
            TrackedEntityInstanceReminder.SEND_WHEN_TO_C0MPLETED_EVENT,
            TrackedEntityInstanceReminder.MESSAGE_TYPE_DIRECT_SMS );
        reminders = new HashSet<>();
        reminders.add( reminderB );
        stageB.setReminders( reminders );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programStageService.saveProgramStage( stageB );

        programService.updateProgram( program );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program, new Date(),
            new Date(), organisationUnit );

        programStageInstance = programStageInstanceService.createProgramStageInstance( programInstance, stageA,
            new Date(), new Date(), organisationUnit );
    }

    @Test
    public void testGetMessageFromTemplateByProgram()
    {
        String message = reminderService.getMessageFromTemplate( reminderA, programInstance, mockFormat );
        assertEquals( "Test program message template", message );
    }

    @Test
    public void testGetMessageFromTemplateByProgramStage()
    {
        String message = reminderService.getMessageFromTemplate( reminderA, programStageInstance, mockFormat );
        assertEquals( "Test program message template", message );
    }

    @Test
    public void testGetPhoneNumbers()
    {
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        attribute.setValueType( ValueType.PHONE_NUMBER );
        attributeService.addTrackedEntityAttribute( attribute );

        TrackedEntityAttributeValue attributeValue = createTrackedEntityAttributeValue( 'A', entityInstance, attribute );
        attributeValue.setValue( "123456789" );
        attributeValueService.addTrackedEntityAttributeValue( attributeValue );

        entityInstance.getTrackedEntityAttributeValues().add( attributeValue );
        entityInstanceService.updateTrackedEntityInstance( entityInstance );

        Set<String> phoneNumbers = reminderService.getPhoneNumbers( reminderA, entityInstance );
        assertEquals( 1, phoneNumbers.size() );
        assertTrue( phoneNumbers.contains( "123456789" ) );
    }

    @Test
    public void testGetUsers()
    {
        Set<User> users = reminderService.getUsers( reminderC, entityInstance );
        assertEquals( 1, users.size() );
        assertTrue( users.contains( user ) );
    }
}
