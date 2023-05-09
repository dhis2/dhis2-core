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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.message.ProgramMessageStore;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
class ProgramMessageStoreTest extends TransactionalIntegrationTest
{

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private Program programA;

    private Enrollment enrollmentA;

    private TrackedEntity teiA;

    private TrackedEntity entityInstanceA;

    private ProgramMessageStatus messageStatus = ProgramMessageStatus.SENT;

    private Set<DeliveryChannel> channels = new HashSet<>();

    private ProgramMessageQueryParams params;

    private Event eventA;

    private ProgramMessage pmsgA;

    private ProgramMessage pmsgB;

    private ProgramMessage pmsgC;

    private ProgramMessageRecipients recipientsA;

    private ProgramMessageRecipients recipientsB;

    private ProgramMessageRecipients recipientsC;

    private String uidA;

    private String uidB;

    private String uidC;

    private String text = "Hi";

    private String msisdn = "4740332255";

    private String notificationTemplate = CodeGenerator.generateUid();

    private Date incidentDate;

    private Date enrollmentDate;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Autowired
    private ProgramMessageStore programMessageStore;

    @Autowired
    private EnrollmentStore enrollmentStore;

    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private TrackedEntityService teiService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityService entityInstanceService;

    @Autowired
    private EventStore eventStore;

    // -------------------------------------------------------------------------
    // Prerequisite
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        orgUnitService.addOrganisationUnit( ouA );
        orgUnitService.addOrganisationUnit( ouB );
        programA = createProgram( 'A', new HashSet<>(), ouA );
        programService.addProgram( programA );
        ProgramStage stageA = new ProgramStage( "StageA", programA );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );
        entityInstanceA = createTrackedEntityInstance( ouA );
        entityInstanceService.addTrackedEntity( entityInstanceA );
        TrackedEntity entityInstanceB = createTrackedEntityInstance( ouA );
        entityInstanceService.addTrackedEntity( entityInstanceB );
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidentDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();
        enrollmentA = new Enrollment( enrollmentDate, incidentDate, entityInstanceA, programA );
        enrollmentA.setUid( "UID-A" );
        eventA = new Event( enrollmentA, stageA );
        eventA.setDueDate( enrollmentDate );
        eventA.setUid( "UID-A" );
        Set<OrganisationUnit> ouSet = new HashSet<>();
        ouSet.add( ouA );
        Set<String> ouUids = new HashSet<>();
        ouUids.add( ouA.getUid() );
        // ouSet.add( ouB );
        teiA = createTrackedEntityInstance( ouA );
        teiService.addTrackedEntity( teiA );
        recipientsA = new ProgramMessageRecipients();
        recipientsA.setOrganisationUnit( ouA );
        recipientsA.setTrackedEntity( teiA );
        recipientsB = new ProgramMessageRecipients();
        recipientsB.setOrganisationUnit( ouA );
        recipientsB.setTrackedEntity( teiA );
        recipientsC = new ProgramMessageRecipients();
        recipientsC.setOrganisationUnit( ouA );
        recipientsC.setTrackedEntity( teiA );
        Set<String> phoneNumberListA = new HashSet<>();
        phoneNumberListA.add( msisdn );
        recipientsA.setPhoneNumbers( phoneNumberListA );
        Set<String> phoneNumberListB = new HashSet<>();
        phoneNumberListB.add( msisdn );
        recipientsB.setPhoneNumbers( phoneNumberListB );
        Set<String> phoneNumberListC = new HashSet<>();
        phoneNumberListC.add( msisdn );
        recipientsC.setPhoneNumbers( phoneNumberListC );
        channels.add( DeliveryChannel.SMS );
        pmsgA = ProgramMessage.builder().subject( text ).text( text ).recipients( recipientsA )
            .messageStatus( messageStatus ).deliveryChannels( channels ).notificationTemplate( notificationTemplate )
            .build();
        pmsgB = ProgramMessage.builder().subject( text ).text( text ).recipients( recipientsB )
            .messageStatus( messageStatus ).deliveryChannels( channels ).notificationTemplate( notificationTemplate )
            .build();
        pmsgC = ProgramMessage.builder().subject( text ).text( text ).recipients( recipientsC )
            .messageStatus( messageStatus ).deliveryChannels( channels ).notificationTemplate( notificationTemplate )
            .build();
        uidA = CodeGenerator.generateCode( 10 );
        uidB = CodeGenerator.generateCode( 10 );
        uidC = CodeGenerator.generateCode( 10 );
        pmsgA.setUid( uidA );
        pmsgB.setUid( uidB );
        pmsgC.setUid( uidC );
        params = new ProgramMessageQueryParams();
        params.setOrganisationUnit( ouUids );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testGetProgramMessage()
    {
        programMessageStore.save( pmsgA );
        Long id = pmsgA.getId();
        ProgramMessage actual = programMessageStore.get( id.intValue() );
        assertNotNull( id );
        assertNotNull( actual );
        assertTrue( actual.equals( pmsgA ) );
    }

    @Test
    void testGetProgramMessages()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        programMessageStore.save( pmsgC );
        assertTrue( equals( programMessageStore.getAll(), pmsgA, pmsgB, pmsgC ) );
    }

    @Test
    void testDeleteProgramMessage()
    {
        programMessageStore.save( pmsgA );
        long pmsgAId = pmsgA.getId();
        programMessageStore.delete( pmsgA );
        assertNull( programMessageStore.get( pmsgAId ) );
    }

    @Test
    void testProgramMessageExists()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        assertTrue( programMessageStore.exists( pmsgA.getUid() ) );
        assertTrue( programMessageStore.exists( pmsgB.getUid() ) );
        assertFalse( programMessageStore.exists( "22343" ) );
        assertFalse( programMessageStore.exists( null ) );
    }

    @Test
    void testGetProgramMessageByEnrollment()
    {
        enrollmentStore.save( enrollmentA );
        pmsgA.setEnrollment( enrollmentA );
        pmsgB.setEnrollment( enrollmentA );
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        params.setEnrollment( enrollmentA );
        List<ProgramMessage> programMessages = programMessageStore.getProgramMessages( params );
        assertNotNull( programMessages );
        assertTrue( equals( programMessages, pmsgA, pmsgB ) );
        assertTrue( channels.equals( programMessages.get( 0 ).getDeliveryChannels() ) );
        assertTrue( enrollmentA.equals( programMessages.get( 0 ).getEnrollment() ) );
    }

    @Test
    void testGetProgramMessageByEvent()
    {
        enrollmentStore.save( enrollmentA );
        eventStore.save( eventA );
        pmsgA.setEvent( eventA );
        pmsgB.setEvent( eventA );
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        params.setEvent( eventA );
        List<ProgramMessage> programMessages = programMessageStore.getProgramMessages( params );
        assertNotNull( programMessages );
        assertTrue( equals( programMessages, pmsgA, pmsgB ) );
        assertTrue( channels.equals( programMessages.get( 0 ).getDeliveryChannels() ) );
        assertTrue( eventA.equals( programMessages.get( 0 ).getEvent() ) );
    }

    @Test
    void testGetProgramMessageByMessageStatus()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        params.setMessageStatus( messageStatus );
        List<ProgramMessage> programMessages = programMessageStore.getProgramMessages( params );
        assertNotNull( programMessages );
        assertTrue( equals( programMessages, pmsgA, pmsgB ) );
        assertTrue( channels.equals( programMessages.get( 0 ).getDeliveryChannels() ) );
        assertTrue( messageStatus.equals( programMessages.get( 0 ).getMessageStatus() ) );
    }

    @Test
    void testGetProgramMessageByMultipleParameters()
    {
        enrollmentStore.save( enrollmentA );
        pmsgA.setEnrollment( enrollmentA );
        pmsgB.setEnrollment( enrollmentA );
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        params.setEnrollment( enrollmentA );
        params.setMessageStatus( messageStatus );
        List<ProgramMessage> programMessages = programMessageStore.getProgramMessages( params );
        assertNotNull( programMessages );
        assertTrue( equals( programMessages, pmsgA, pmsgB ) );
        assertTrue( channels.equals( programMessages.get( 0 ).getDeliveryChannels() ) );
        assertTrue( enrollmentA.equals( programMessages.get( 0 ).getEnrollment() ) );
    }
}
