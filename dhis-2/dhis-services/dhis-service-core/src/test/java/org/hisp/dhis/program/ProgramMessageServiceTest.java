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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Date;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.program.message.ProgramMessageRecipients;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.message.ProgramMessageStatus;
import org.hisp.dhis.sms.config.BulkSmsGatewayConfig;
import org.hisp.dhis.sms.config.GatewayAdministrationService;
import org.hisp.dhis.sms.config.SmsConfiguration;
import org.hisp.dhis.sms.config.SmsConfigurationManager;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;

import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static org.junit.Assert.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

public class ProgramMessageServiceTest
    extends DhisSpringTest
{
    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private ProgramInstance piA;

    private TrackedEntityInstance teiA;
    
    private BulkSmsGatewayConfig bulkSmsConfig;

    private ProgramMessageStatus messageStatus = ProgramMessageStatus.OUTBOUND;

    private Set<DeliveryChannel> channels = new HashSet<>();

    private ProgramMessageQueryParams params;

    private ProgramMessage pmsgA;

    private ProgramMessage pmsgB;

    private ProgramMessage pmsgC;
    
    private ProgramMessage pmsgD;

    private ProgramMessageRecipients recipientsA;

    private ProgramMessageRecipients recipientsB;

    private ProgramMessageRecipients recipientsC;
    
    private ProgramMessageRecipients recipientsD;

    private String uidA;

    private String uidB;

    private String uidC;

    private String text = "Hi";

    private String msisdn = "4742312555";

    private String subject = "subjectText";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramMessageService programMessageService;

    @Autowired
    private OrganisationUnitService orgUnitService;

    @Autowired
    private TrackedEntityInstanceService teiService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramService programService;
    
    @Autowired
    private GatewayAdministrationService gatewayAdminService;
    
    @Autowired
    private SmsConfigurationManager smsConfigurationManager;

    // -------------------------------------------------------------------------
    // Prerequisite
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouA.setPhoneNumber( msisdn );
        
        ouB = createOrganisationUnit( 'B' );
        
        orgUnitService.addOrganisationUnit( ouA );
        orgUnitService.addOrganisationUnit( ouB );

        Program program = createProgram( 'A' );
        program.setAutoFields();
        program.setOrganisationUnits( Sets.newSet( ouA, ouB ) );
        program.setName( "programA" );
        program.setDisplayShortName( "programAshortname" );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );

        programService.addProgram( program );

        piA = new ProgramInstance();
        piA.setProgram( program );
        piA.setOrganisationUnit( ouA );
        piA.setName( "programInstanceA" );
        piA.setEnrollmentDate( new Date() );
        piA.setAutoFields();

        programInstanceService.addProgramInstance( piA );

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
        
        recipientsD = new ProgramMessageRecipients();
        recipientsD.setOrganisationUnit( ouA );
        recipientsD.setTrackedEntityInstance( null );

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

        pmsgA = createProgramMessage( text, subject, recipientsA, messageStatus, channels );
        pmsgA.setProgramInstance( piA );
        pmsgA.setStoreCopy( false );

        pmsgB = createProgramMessage( text, subject, recipientsB, messageStatus, channels );
        pmsgB.setProgramInstance( piA );

        pmsgC = createProgramMessage( text, subject, recipientsC, messageStatus, channels );      

        pmsgD = createProgramMessage( text, subject, recipientsD, messageStatus, channels );
        pmsgD.setProgramInstance( piA );
        pmsgD.setStoreCopy( false );

        uidA = CodeGenerator.generateCode( 10 );
        uidB = CodeGenerator.generateCode( 10 );
        uidC = CodeGenerator.generateCode( 10 );

        pmsgA.setUid( uidA );
        pmsgB.setUid( uidB );
        pmsgC.setUid( uidC );

        params = new ProgramMessageQueryParams();
        params.setOrganisationUnit( ouUids );
        params.setProgramInstance( piA );
        params.setMessageStatus( messageStatus );
        
        bulkSmsConfig = new BulkSmsGatewayConfig();
        bulkSmsConfig.setDefault( true );
        bulkSmsConfig.setName( "bulk" );
        bulkSmsConfig.setUsername( "user_uio" );
        bulkSmsConfig.setPassword( "5cKMMQTGNMkD" );
            
        SmsConfiguration smsConfig = new SmsConfiguration();
        smsConfig.getGateways().add( bulkSmsConfig);
        
        smsConfigurationManager.updateSmsConfiguration( smsConfig );
        
        gatewayAdminService.loadGatewayConfigurationMap( smsConfig );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testDeleteProgramMessage()
    {
        Integer pmsgAId = null;

        pmsgAId = programMessageService.saveProgramMessage( pmsgA );

        assertNotNull( pmsgAId );

        programMessageService.deleteProgramMessage( pmsgA );

        ProgramMessage programMessage = programMessageService.getProgramMessage( pmsgAId.intValue() );

        assertNull( programMessage );
    }

    @Test
    public void testExists()
    {
        programMessageService.saveProgramMessage( pmsgA );

        boolean exists = programMessageService.exists( uidA );

        assertTrue( exists );
    }

    @Test
    public void testGetAllProgramMessages()
    {
        programMessageService.saveProgramMessage( pmsgA );
        programMessageService.saveProgramMessage( pmsgB );
        programMessageService.saveProgramMessage( pmsgC );

        List<ProgramMessage> programMessages = programMessageService.getAllProgramMessages();

        assertNotNull( programMessages );
        assertTrue( !programMessages.isEmpty() );
        assertTrue( equals( programMessages, pmsgA, pmsgB, pmsgC ) );
    }

    @Test
    public void testGetProgramMessageById()
    {
        int pmsgAId = programMessageService.saveProgramMessage( pmsgA );

        ProgramMessage programMessage = programMessageService.getProgramMessage( pmsgAId );

        assertNotNull( programMessage );
        assertTrue( pmsgA.equals( programMessage ) );
    }

    @Test
    public void testGetProgramMessageByUid()
    {
        programMessageService.saveProgramMessage( pmsgA );

        ProgramMessage programMessage = programMessageService.getProgramMessage( uidA );

        assertNotNull( programMessage );
        assertTrue( pmsgA.equals( programMessage ) );
    }

    @Test
    public void testGetProgramMessageByQuery()
    {
        programMessageService.saveProgramMessage( pmsgA );
        programMessageService.saveProgramMessage( pmsgB );

        List<ProgramMessage> list = programMessageService.getProgramMessages( params );

        assertNotNull( list );
        assertTrue( equals( list, pmsgA, pmsgB ) );
        assertTrue( channels.equals( list.get( 0 ).getDeliveryChannels() ) );
    }
    
    @Test
    public void testSaveProgramMessage()
    {
        Integer pmsgAId = null;

        pmsgAId = programMessageService.saveProgramMessage( pmsgA );

        assertNotNull( pmsgAId );

        ProgramMessage programMessage = programMessageService.getProgramMessage( pmsgAId.intValue() );

        assertTrue( programMessage.equals( pmsgA ) );
    }

    @Test
    public void testUpdateProgramMessage()
    {
        Integer pmsgAId = programMessageService.saveProgramMessage( pmsgA );

        ProgramMessage programMessage = programMessageService.getProgramMessage( pmsgAId.intValue() );

        programMessage.setText( "hello" );

        programMessageService.updateProgramMessage( programMessage );

        ProgramMessage programMessageUpdated = programMessageService.getProgramMessage( pmsgAId.intValue() );

        assertNotNull( programMessageUpdated );
        assertTrue( programMessageUpdated.getText().equals( "hello" ) );
    }
}
