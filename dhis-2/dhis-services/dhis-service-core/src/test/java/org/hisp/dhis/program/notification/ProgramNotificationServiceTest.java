package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.api.client.util.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ProgramNotificationMessageRenderer;
import org.hisp.dhis.notification.ProgramStageNotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.*;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import java.util.*;

/**
 * @Author Zubair Asghar.
 */
@RunWith( MockitoJUnitRunner.class )
public class ProgramNotificationServiceTest extends DhisConvenienceTest
{
    private static final String SUBJECT = "subject";
    private static final String MESSAGE = "message";
    private static final String TEMPLATE_NAME = "message";
    private static final String OU_PHONE_NUMBER = "471000000";
    private static final String DE_PHONE_NUMBER = "47200000";
    private static final String ATT_PHONE_NUMBER = "473000000";

    @Mock
    private MessageService messageService;

    @Mock
    private ProgramMessageService programMessageService;

    @Mock
    private ProgramNotificationMessageRenderer programNotificationMessageRenderer;

    @Mock
    private ProgramStageNotificationMessageRenderer programStageNotificationMessageRenderer;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private ProgramInstanceStore programInstanceStore;

    @Mock
    private ProgramStageInstanceStore programStageInstanceStore;

    @InjectMocks
    private DefaultProgramNotificationService programNotificationService;

    private Set<ProgramInstance> programInstances = new HashSet<>();
    private Set<ProgramStageInstance> programStageInstances = new HashSet<>();

    private OrganisationUnit root;
    private OrganisationUnit lvlOneLeft;
    private OrganisationUnit lvlOneRight;
    private OrganisationUnit lvlTwoLeftLeft;
    private OrganisationUnit lvlTwoLeftRight;

    private DataElement dataElement;
    private ProgramStageDataElement programStageDataElement;
    private TrackedEntityDataValue dataValue;

    private TrackedEntityAttribute trackedEntityAttribute;
    private ProgramTrackedEntityAttribute programTrackedEntityAttribute;
    private TrackedEntityAttributeValue attributeValue;

    private NotificationMessage notificationMessage;
    private ProgramNotificationTemplate programNotificationTemplate;

    @Before
    public void initTest()
    {
        setUpInstances();

        BatchResponseStatus status = new BatchResponseStatus(Collections.emptyList());
        when( programMessageService.sendMessages( anyList() ) )
            .thenReturn( status );

        when( messageService.sendMessage( any() ) )
            .thenReturn( 1 );

        when( programInstanceStore.getWithScheduledNotifications( any(), any()) )
            .thenReturn( Lists.newArrayList( programInstances ) );
        when( programStageInstanceStore.getWithScheduledNotifications( any(), any() ) )
            .thenReturn( Lists.newArrayList( programStageInstances ) );

        when( manager.getAll( ProgramNotificationTemplate.class ) )
            .thenReturn( Collections.singletonList( programNotificationTemplate ) );

        when( programNotificationMessageRenderer.render( any(), any() ) )
            .thenReturn( notificationMessage );
        when( programStageNotificationMessageRenderer.render( any(), any() ) )
            .thenReturn( notificationMessage );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testIfProgramInstanceIsNull()
    {
        ProgramInstance programInstance = null;

        programNotificationService.sendCompletionNotifications( programInstance );

        verify( manager, never() ).getAll( any() );
    }

    @Test
    public void testIfProgramStageInstanceIsNull()
    {
        ProgramStageInstance programStageInstance = null;

        programNotificationService.sendCompletionNotifications( programStageInstance );

        verify( manager, never() ).getAll( any() );
    }

    @Test
    public void testSendCompletionNotification()
    {
    }

    @Test
    public void testSendEnrollmentNotification()
    {

    }

    @Test
    public void testScheduledNotification()
    {

    }

    @Test
    public void testUserGroupRecipient()
    {

    }

    @Test
    public void testTeiRecipient()
    {

    }

    @Test
    public void testOuContactRecipient()
    {

    }

    @Test
    public void testProgramAttributeRecipient()
    {

    }

    @Test
    public void testDataElementRecipient()
    {

    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        programNotificationTemplate = createProgramNotificationTemplate( TEMPLATE_NAME, 0, NotificationTrigger.COMPLETION );

        root = createOrganisationUnit( 'R' );
        lvlOneLeft = createOrganisationUnit( '1' );
        lvlOneRight = createOrganisationUnit( '2' );
        lvlTwoLeftLeft = createOrganisationUnit( '3' );
        lvlTwoLeftLeft.setPhoneNumber( OU_PHONE_NUMBER );
        lvlTwoLeftRight = createOrganisationUnit( '4' );

        configureHierarchy( root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight );

        // Program
        Program programA = createProgram( 'A' );
        programA.setAutoFields();
        programA.setOrganisationUnits( Sets.newHashSet( lvlTwoLeftLeft,lvlTwoLeftRight ) );
        programA.setNotificationTemplates( Sets.newHashSet( programNotificationTemplate ) );
        programA.getProgramAttributes().add( programTrackedEntityAttribute );

        trackedEntityAttribute = createTrackedEntityAttribute( 'T' );
        trackedEntityAttribute.setValueType( ValueType.PHONE_NUMBER );
        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( 'O' );
        programTrackedEntityAttribute.setAttribute( trackedEntityAttribute );

        // ProgramStage
        ProgramStage programStage = createProgramStage( 'S', programA );

        dataElement = createDataElement( 'D' );
        dataElement.setValueType( ValueType.PHONE_NUMBER );
        programStageDataElement = createProgramStageDataElement( programStage, dataElement, 1 );


        // ProgramInstance & TEI
        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setAutoFields();
        tei.setOrganisationUnit( lvlTwoLeftLeft );

        attributeValue = createTrackedEntityAttributeValue( 'P', tei, trackedEntityAttribute );
        attributeValue.setValue( ATT_PHONE_NUMBER );
        tei.getTrackedEntityAttributeValues().add( attributeValue );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setAutoFields();
        programInstance.setProgram( programA );
        programInstance.setOrganisationUnit( lvlTwoLeftLeft );
        programInstance.setEntityInstance( tei );

        // ProgramStageInstance
        ProgramStageInstance programStageInstance = createProgramStageInstance();
        programStageInstance.setAutoFields();
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setOrganisationUnit( lvlTwoLeftLeft );
        programStageInstance.setProgramStage( programStage );
        dataValue = new TrackedEntityDataValue();
        dataValue.setAutoFields();
        dataValue.setDataElement( dataElement );
        dataValue.setValue( DE_PHONE_NUMBER );
        programStageInstance.getDataValues().add( dataValue );

        // lists returned by stubs
        programStageInstances.add( programStageInstance );
        programInstances.add( programInstance );

        notificationMessage = new NotificationMessage( SUBJECT, MESSAGE );
    }
}
