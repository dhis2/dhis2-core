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
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.program.message.ProgramMessageStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

public class ProgramMessageStoreTest
    extends DhisSpringTest
{
    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private ProgramStageInstance psiA;

    private TrackedEntityInstance teiA;

    private ProgramMessageStatus messageStatus = ProgramMessageStatus.SENT;

    private Set<DeliveryChannel> channels = new HashSet<>();

    private ProgramMessageQueryParams params;

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

    private String subject = "subjectText";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramMessageStore programMessageStore;

    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private TrackedEntityInstanceService teiService;

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

        Set<OrganisationUnit> ouSet = new HashSet<>();
        ouSet.add( ouA );

        Set<String> ouUids = new HashSet<>();
        ouUids.add( ouA.getUid() );
        // ouSet.add( ouB );

        teiA = createTrackedEntityInstance( 'Z', ouA );
        teiService.addTrackedEntityInstance( teiA );

        recipientsA = new ProgramMessageRecipients();
        recipientsA.setOrganisationUnit( ouA );
        recipientsA.setTrackedEntityInstance( teiA );

        recipientsB = new ProgramMessageRecipients();
        recipientsB.setOrganisationUnit( ouA );
        recipientsB.setTrackedEntityInstance( teiA );

        recipientsC = new ProgramMessageRecipients();
        recipientsC.setOrganisationUnit( ouA );
        recipientsC.setTrackedEntityInstance( teiA );

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

        pmsgA = new ProgramMessage();
        pmsgA.setText( text );
        pmsgA.setSubject( subject );
        pmsgA.setRecipients( recipientsA );
        pmsgA.setMessageStatus( messageStatus );
        pmsgA.setDeliveryChannels( channels );

        pmsgB = new ProgramMessage();
        pmsgB.setText( text );
        pmsgB.setSubject( subject );
        pmsgB.setRecipients( recipientsB );
        pmsgB.setMessageStatus( messageStatus );
        pmsgB.setDeliveryChannels( channels );

        pmsgC = new ProgramMessage();
        pmsgC.setText( text );
        pmsgC.setSubject( subject );
        pmsgC.setRecipients( recipientsC );
        pmsgC.setMessageStatus( messageStatus );
        pmsgC.setDeliveryChannels( channels );

        uidA = CodeGenerator.generateCode( 10 );
        uidB = CodeGenerator.generateCode( 10 );
        uidC = CodeGenerator.generateCode( 10 );

        pmsgA.setUid( uidA );
        pmsgB.setUid( uidB );
        pmsgC.setUid( uidC );

        params = new ProgramMessageQueryParams();
        params.setOrganisationUnit( ouUids );
        params.setProgramStageInstance( psiA );
        params.setMessageStatus( messageStatus );

    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetProgramMessage()
    {
        Integer id = programMessageStore.save( pmsgA );
        ProgramMessage actual = programMessageStore.get( id.intValue() );

        assertNotNull( id );
        assertNotNull( actual );
        assertTrue( actual.equals( pmsgA ) );
    }

    @Test
    public void testGetProgramMessages()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );
        programMessageStore.save( pmsgC );

        assertTrue( equals( programMessageStore.getAll(), pmsgA, pmsgB, pmsgC ) );
    }

    @Test
    public void testDeleteProgramMessage()
    {
        int pmsgAId = programMessageStore.save( pmsgA );

        programMessageStore.delete( pmsgA );

        assertNull( programMessageStore.get( pmsgAId ) );
    }

    @Test
    public void testProgramMessageExists()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );

        assertTrue( programMessageStore.exists( pmsgA.getUid() ) );
        assertTrue( programMessageStore.exists( pmsgB.getUid() ) );
        assertFalse( programMessageStore.exists( "22343" ) );
        assertFalse( programMessageStore.exists( null ) );
    }

    @Test
    public void testGetProgramMessagesByParams()
    {
        programMessageStore.save( pmsgA );
        programMessageStore.save( pmsgB );

        List<ProgramMessage> list = programMessageStore.getProgramMessages( params );

        assertNotNull( list );
        assertTrue( equals( list, pmsgA, pmsgB ) );
        assertTrue( channels.equals( list.get( 0 ).getDeliveryChannels() ) );
    }
}
