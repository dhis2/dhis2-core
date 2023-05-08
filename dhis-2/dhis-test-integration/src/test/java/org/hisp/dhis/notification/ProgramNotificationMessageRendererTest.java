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
package org.hisp.dhis.notification;

import static org.hisp.dhis.notification.BaseNotificationMessageRenderer.formatDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeStore;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar
 */
class ProgramNotificationMessageRendererTest extends TransactionalIntegrationTest
{

    private String dataElementUid = CodeGenerator.generateUid();

    private String trackedEntityAttributeUid = CodeGenerator.generateUid();

    private String programUid = CodeGenerator.generateUid();

    private String programStageUid = CodeGenerator.generateUid();

    private String orgUnitUid = CodeGenerator.generateUid();

    private String enrollmentUid = CodeGenerator.generateUid();

    private String trackedEntityUid = CodeGenerator.generateUid();

    private Program programA;

    private ProgramStage programStageA;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private TrackedEntityAttribute trackedEntityAttributeB;

    private TrackedEntityAttributeValue trackedEntityAttributeValueA;

    private ProgramTrackedEntityAttribute programTrackedEntityAttributeA;

    private ProgramTrackedEntityAttribute programTrackedEntityAttributeB;

    private ProgramStageDataElement programStageDataElementA;

    private ProgramStageDataElement programStageDataElementB;

    private TrackedEntity trackedEntityA;

    private Enrollment enrollmentA;

    private Event eventA;

    private EventDataValue eventDataValueA;

    private EventDataValue eventDataValueB;

    private OrganisationUnit organisationUnitA;

    private ProgramNotificationTemplate programNotificationTemplate;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ProgramTrackedEntityAttributeStore programTrackedEntityAttributeStore;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private TrackedEntityService entityInstanceService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramNotificationMessageRenderer programNotificationMessageRenderer;

    @Autowired
    private ProgramStageNotificationMessageRenderer programStageNotificationMessageRenderer;

    @Override
    protected void setUpTest()
        throws Exception
    {
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        Date incidentDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        Date enrollmentDate = testDate2.toDate();
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementA.setUid( dataElementUid );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB.setUid( "DEB-UID" );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );
        trackedEntityAttributeA.setUid( trackedEntityAttributeUid );
        trackedEntityAttributeB = createTrackedEntityAttribute( 'B' );
        attributeService.addTrackedEntityAttribute( trackedEntityAttributeA );
        attributeService.addTrackedEntityAttribute( trackedEntityAttributeB );
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitA.setUid( orgUnitUid );
        organisationUnitService.addOrganisationUnit( organisationUnitA );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programA.setUid( programUid );
        programService.addProgram( programA );
        programTrackedEntityAttributeA = createProgramTrackedEntityAttribute( programA, trackedEntityAttributeA );
        programTrackedEntityAttributeB = createProgramTrackedEntityAttribute( programA, trackedEntityAttributeB );
        programTrackedEntityAttributeStore.save( programTrackedEntityAttributeA );
        programTrackedEntityAttributeStore.save( programTrackedEntityAttributeB );
        programA
            .setProgramAttributes( Arrays.asList( programTrackedEntityAttributeA, programTrackedEntityAttributeB ) );
        programService.updateProgram( programA );
        programStageA = createProgramStage( 'A', programA );
        programStageA.setUid( programStageUid );
        programStageService.saveProgramStage( programStageA );
        programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA, 1 );
        programStageDataElementB = createProgramStageDataElement( programStageA, dataElementB, 2 );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageA
            .setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageService.updateProgramStage( programStageA );
        programA.setProgramStages( Sets.newHashSet( programStageA ) );
        programService.updateProgram( programA );
        trackedEntityA = createTrackedEntityInstance( organisationUnitA );
        trackedEntityA.setUid( trackedEntityUid );
        entityInstanceService.addTrackedEntityInstance( trackedEntityA );
        trackedEntityAttributeValueA = new TrackedEntityAttributeValue( trackedEntityAttributeA, trackedEntityA,
            "attribute-test" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( trackedEntityAttributeValueA );
        trackedEntityA.setTrackedEntityAttributeValues( Sets.newHashSet( trackedEntityAttributeValueA ) );
        entityInstanceService.updateTrackedEntityInstance( trackedEntityA );
        // Enrollment to be provided in message renderer
        enrollmentA = enrollmentService.enrollTrackedEntityInstance( trackedEntityA, programA,
            enrollmentDate, incidentDate, organisationUnitA );
        enrollmentA.setUid( enrollmentUid );
        enrollmentService.updateEnrollment( enrollmentA );
        // Event to be provided in message renderer
        eventA = new Event( enrollmentA, programStageA );
        eventA.setOrganisationUnit( organisationUnitA );
        eventA.setDueDate( enrollmentDate );
        eventA.setExecutionDate( new Date() );
        eventA.setUid( "PSI-UID" );
        eventDataValueA = new EventDataValue();
        eventDataValueA.setDataElement( dataElementA.getUid() );
        eventDataValueA.setAutoFields();
        eventDataValueA.setValue( "dataElementA-Text" );
        eventDataValueB = new EventDataValue();
        eventDataValueB.setDataElement( dataElementB.getUid() );
        eventDataValueB.setAutoFields();
        eventDataValueB.setValue( "dataElementB-Text" );
        eventA.setEventDataValues( Sets.newHashSet( eventDataValueA, eventDataValueB ) );
        eventService.addEvent( eventA );
        enrollmentA.getEvents().add( eventA );
        enrollmentService.updateEnrollment( enrollmentA );
        programNotificationTemplate = new ProgramNotificationTemplate();
        programNotificationTemplate.setName( "Test-PNT" );
        programNotificationTemplate.setMessageTemplate( "message_template" );
        programNotificationTemplate.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );
        programNotificationTemplate.setSubjectTemplate( "subject_template" );
        programNotificationTemplate.setNotificationTrigger( NotificationTrigger.PROGRAM_RULE );
        programNotificationTemplate.setAutoFields();
        programNotificationTemplate.setUid( "PNT-1" );
        programNotificationTemplateStore.save( programNotificationTemplate );
    }

    @Test
    void testRendererForSimpleMessage()
    {
        NotificationMessage notificationMessage = programNotificationMessageRenderer.render( enrollmentA,
            programNotificationTemplate );
        assertEquals( "message_template", notificationMessage.getMessage() );
        assertEquals( "subject_template", notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithAttribute()
    {
        programNotificationTemplate.setMessageTemplate( "message is A{" + trackedEntityAttributeUid + "}" );
        programNotificationTemplate.setSubjectTemplate( "subject is A{" + trackedEntityAttributeUid + "}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programNotificationMessageRenderer.render( enrollmentA,
            programNotificationTemplate );
        assertEquals( "message is attribute-test", notificationMessage.getMessage() );
        assertEquals( "subject is attribute-test", notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithDataElement()
    {
        programNotificationTemplate.setMessageTemplate( "message is #{" + dataElementUid + "}" );
        programNotificationTemplate.setSubjectTemplate( "subject is #{" + dataElementUid + "}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programStageNotificationMessageRenderer.render( eventA,
            programNotificationTemplate );
        assertEquals( "message is dataElementA-Text", notificationMessage.getMessage() );
        assertEquals( "subject is dataElementA-Text", notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithVariableName()
    {
        programNotificationTemplate.setMessageTemplate( "message is V{org_unit_name} and V{enrollment_org_unit_id}" );
        programNotificationTemplate.setSubjectTemplate( "subject is V{program_name}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programNotificationMessageRenderer.render( enrollmentA,
            programNotificationTemplate );
        assertEquals( "message is OrganisationUnitA and " + orgUnitUid, notificationMessage.getMessage() );
        assertEquals( "subject is ProgramA", notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithVariableId()
    {
        programNotificationTemplate.setMessageTemplate( "message is V{program_id} and V{event_org_unit_id}" );
        programNotificationTemplate.setSubjectTemplate( "subject is V{program_stage_id} and V{enrollment_id}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programStageNotificationMessageRenderer.render( eventA,
            programNotificationTemplate );
        assertEquals( "message is " + programA.getUid() + " and " + orgUnitUid, notificationMessage.getMessage() );
        assertEquals( "subject is " + programStageA.getUid() + " and " + enrollmentUid,
            notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithTrackedEntity()
    {
        programNotificationTemplate.setMessageTemplate( "message is V{tracked_entity_id}" );
        programNotificationTemplate.setSubjectTemplate( "subject is V{tracked_entity_id}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programStageNotificationMessageRenderer.render( eventA,
            programNotificationTemplate );
        assertEquals( "message is " + trackedEntityA.getUid(), notificationMessage.getMessage() );
        assertEquals( "subject is " + trackedEntityA.getUid(), notificationMessage.getSubject() );
    }

    @Test
    void testRendererForMessageWithEventDate()
    {
        programNotificationTemplate.setMessageTemplate( "message is V{event_date}" );
        programNotificationTemplate.setSubjectTemplate( "subject is V{event_date}" );
        programNotificationTemplateStore.update( programNotificationTemplate );
        NotificationMessage notificationMessage = programStageNotificationMessageRenderer.render( eventA,
            programNotificationTemplate );
        assertEquals( "message is " + formatDate( eventA.getExecutionDate() ),
            notificationMessage.getMessage() );
        assertEquals( "subject is " + formatDate( eventA.getExecutionDate() ),
            notificationMessage.getSubject() );
    }
}
