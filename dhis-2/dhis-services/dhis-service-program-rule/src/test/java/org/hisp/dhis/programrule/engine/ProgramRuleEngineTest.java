package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeStore;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.ProgramRuleVariableSourceType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionScheduleMessage;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Sets;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngineTest extends DhisSpringTest
{
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    private Program programA;

    private Program programS;

    private ProgramRule programRuleA;

    private ProgramRule programRuleA2;

    private ProgramRule programRuleC;

    private ProgramRule programRuleS;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataElement dataElementDate;

    private DataElement dataElementAge;

    private DataElement assignedDataElement;

    private EventDataValue eventDataValueDate;

    private EventDataValue eventDataValueAge;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private String scheduledDate;

    private String dob = "1984-01-01";

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionC = "A{C1234567890}=='test'";

    private String expressionS = "A{S1234567890}=='xmen'";

    private String dataExpression = "d2:addDays('2018-04-15', '2')";

    private String calculatedDateExpression = "true";

    private Date psEventDate;

    @Qualifier( "oldRuleEngine" )
    @Autowired
    ProgramRuleEngine programRuleEngine;

    @Autowired
    private ProgramRuleEngineService programRuleEngineService;

    @Autowired
    private RuleActionScheduleMessageImplementer ruleActionImplementers;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

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
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private ProgramTrackedEntityAttributeStore programTrackedEntityAttributeStore;

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Override
    public void setUpTest()
        throws ParseException
    {
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementC = createDataElement( 'C', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementD = createDataElement( 'D', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );

        dataElementDate = createDataElement( 'T', ValueType.DATE, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementAge = createDataElement( 'G', ValueType.AGE, AggregationType.NONE, DataElementDomain.TRACKER );
        assignedDataElement = createDataElement( 'K', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );

        attributeA = createTrackedEntityAttribute( 'A' );
        attributeB = createTrackedEntityAttribute( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );

        dataElementService.addDataElement( dataElementDate );
        dataElementService.addDataElement( dataElementAge );
        dataElementService.addDataElement( assignedDataElement );

        attributeService.addTrackedEntityAttribute( attributeA );
        attributeService.addTrackedEntityAttribute( attributeB );

        Calendar cal = Calendar.getInstance();
        cal.setTime( simpleDateFormat.parse( dob ) );
        cal.add( Calendar.YEAR, 10 );

        psEventDate = cal.getTime();

        setupEvents();
        setupProgramRuleEngine();
    }

    @Test
    public void testSendMessageForEnrollment()
    {
        setUpSendMessageForEnrollment();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-P1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance, Optional.empty(),
            Sets.newHashSet() );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionSendMessage );

        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;

        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    public void testSendMessageForEvent()
    {
        setUpSendMessageForEnrollment();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programStageInstance.getProgramInstance(),
            Optional.of( programStageInstance ), Sets.newHashSet() );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionSendMessage );

        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;

        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    public void testSchedulingByProgramRule()
    {
        setUpScheduleMessage();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-PS" );

        List<RuleEffect> ruleEffects = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( programInstance.getId() );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionScheduleMessage );

        RuleActionScheduleMessage ruleActionScheduleMessage = (RuleActionScheduleMessage) ruleAction;

        assertEquals( "PNT-1-SCH", ruleActionScheduleMessage.notification() );
        assertEquals( scheduledDate, ruleEffects.get( 0 ).data() );

        // For duplication detection

        List<RuleEffect> ruleEffects2 = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( programInstance.getId() );

        assertNotNull( ruleEffects2.get( 0 ) );

        assertTrue( ruleEffects2.get( 0 ).ruleAction() instanceof RuleActionScheduleMessage );

        RuleActionScheduleMessage ruleActionScheduleMessage2 = (RuleActionScheduleMessage) ruleEffects2.get( 0 )
            .ruleAction();

        assertNotNull( programNotificationTemplateStore.getByUid( ruleActionScheduleMessage2.notification() ) );

        // duplicate enrollment/events will be ignored and validation will be failed.
        assertFalse( ruleActionImplementers.validate( ruleEffects2.get( 0 ), programInstance ) );
    }

    @Test
    public void testAssignValueTypeDate()
    {
        setUpAssignValueDate();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS12" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programStageInstance.getProgramInstance(),
            Optional.of( programStageInstance ), Sets.newHashSet() );

        assertNotNull( ruleEffects );
        assertEquals( ruleEffects.get( 0 ).data(), "10" );
    }

    @Test
    public void testAssignValueTypeAge()
    {
        setUpAssignValueAge();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS13" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programStageInstance.getProgramInstance(),
            Optional.of( programStageInstance ), Sets.newHashSet() );

        assertNotNull( ruleEffects );
        assertEquals( ruleEffects.get( 0 ).data(), "10" );
    }

    private void setupEvents()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programS = createProgram( 'S', new HashSet<>(), organisationUnitB );

        programService.addProgram( programA );
        programService.addProgram( programS );

        ProgramTrackedEntityAttribute attribute = createProgramTrackedEntityAttribute( programS, attributeB );
        attribute.setUid( "ATTR-UID" );

        programTrackedEntityAttributeStore.save( attribute );

        programA.setProgramAttributes( Arrays.asList( attribute ) );
        programS.setProgramAttributes( Arrays.asList( attribute ) );

        programService.updateProgram( programA );
        programService.updateProgram( programS );

        ProgramStage programStageA = createProgramStage( 'A', programA );
        ProgramStage programStageAge = createProgramStage( 'S', programA );
        ProgramStage programStageB = createProgramStage( 'B', programA );
        ProgramStage programStageC = createProgramStage( 'C', programA );

        programStageService.saveProgramStage( programStageA );
        programStageService.saveProgramStage( programStageAge );
        programStageService.saveProgramStage( programStageB );
        programStageService.saveProgramStage( programStageC );

        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA,
            1 );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( programStageB, dataElementB,
            2 );
        ProgramStageDataElement programStageDataElementC = createProgramStageDataElement( programStageC, dataElementC,
            3 );
        ProgramStageDataElement programStageDataElementD = createProgramStageDataElement( programStageC, dataElementD,
            4 );

        ProgramStageDataElement programStageDataElementDate = createProgramStageDataElement( programStageAge,
            dataElementDate, 5 );
        ProgramStageDataElement programStageDataElementAge = createProgramStageDataElement( programStageAge,
            dataElementAge, 6 );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementC );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementD );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementDate );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementAge );

        programStageA.setSortOrder( 1 );
        programStageB.setSortOrder( 2 );
        programStageC.setSortOrder( 3 );

        programStageA.setProgramStageDataElements(
            Sets.newHashSet( programStageDataElementA, programStageDataElementB, programStageDataElementDate ) );
        programStageAge.setProgramStageDataElements( Sets.newHashSet( programStageDataElementDate ) );
        programStageB
            .setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageC
            .setProgramStageDataElements( Sets.newHashSet( programStageDataElementC, programStageDataElementD ) );

        programStageService.updateProgramStage( programStageA );
        programStageService.updateProgramStage( programStageB );
        programStageService.updateProgramStage( programStageC );
        programStageService.updateProgramStage( programStageAge );

        programA.setProgramStages( Sets.newHashSet( programStageA, programStageB, programStageC, programStageAge ) );

        programService.updateProgram( programA );

        TrackedEntityInstance entityInstanceA = createTrackedEntityInstance( organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        TrackedEntityInstance entityInstanceS = createTrackedEntityInstance( organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceS );

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue( attributeA, entityInstanceA,
            "test" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValue );

        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceB,
            "xmen" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueB );

        TrackedEntityAttributeValue attributeValueS = new TrackedEntityAttributeValue( attributeB, entityInstanceS,
            "xmen" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueS );

        entityInstanceA.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValue ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA );

        entityInstanceB.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueB ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceB );

        entityInstanceS.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueS ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceS );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        Date incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        Date enrollmentDate = testDate2.toDate();

        ProgramInstance programInstanceA = programInstanceService.enrollTrackedEntityInstance( entityInstanceA,
            programA, enrollmentDate, incidenDate, organisationUnitA );
        programInstanceA.setUid( "UID-P1" );
        programInstanceService.updateProgramInstance( programInstanceA );

        ProgramInstance programInstanceS = programInstanceService.enrollTrackedEntityInstance( entityInstanceS,
            programS, enrollmentDate, incidenDate, organisationUnitB );
        programInstanceS.setUid( "UID-PS" );
        programInstanceService.updateProgramInstance( programInstanceS );

        ProgramStageInstance programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setUid( "UID-PS1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        eventDataValueDate = new EventDataValue();
        eventDataValueDate.setDataElement( dataElementDate.getUid() );
        eventDataValueDate.setAutoFields();
        eventDataValueDate.setValue( dob );

        eventDataValueAge = new EventDataValue();
        eventDataValueAge.setDataElement( dataElementAge.getUid() );
        eventDataValueAge.setAutoFields();
        eventDataValueAge.setValue( dob );

        ProgramStageInstance programStageInstanceDate = new ProgramStageInstance( programInstanceA, programStageAge );
        programStageInstanceDate.setDueDate( enrollmentDate );
        programStageInstanceDate.setExecutionDate( psEventDate );
        programStageInstanceDate.setUid( "UID-PS12" );
        programStageInstanceDate.setEventDataValues( Sets.newHashSet( eventDataValueDate ) );
        programStageInstanceService.addProgramStageInstance( programStageInstanceDate );

        ProgramStageInstance programStageInstanceAge = new ProgramStageInstance( programInstanceA, programStageAge );
        programStageInstanceAge.setDueDate( enrollmentDate );
        programStageInstanceAge.setExecutionDate( psEventDate );
        programStageInstanceAge.setUid( "UID-PS13" );
        programStageInstanceAge.setEventDataValues( Sets.newHashSet( eventDataValueAge ) );
        programStageInstanceService.addProgramStageInstance( programStageInstanceAge );

        ProgramStageInstance programStageInstanceB = new ProgramStageInstance( programInstanceA, programStageB );
        programStageInstanceB.setDueDate( enrollmentDate );
        programStageInstanceB.setExecutionDate( new Date() );
        programStageInstanceB.setUid( "UID-PS2" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        ProgramStageInstance programStageInstanceC = new ProgramStageInstance( programInstanceA, programStageC );
        programStageInstanceC.setDueDate( enrollmentDate );
        programStageInstanceC.setExecutionDate( new Date() );
        programStageInstanceC.setUid( "UID-PS3" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceC );

        programInstanceA.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceA,
            programStageInstanceB, programStageInstanceC, programStageInstanceAge ) );
        programInstanceService.updateProgramInstance( programInstanceA );
    }

    private void setupProgramRuleEngine()
    {
        programRuleA = createProgramRule( 'C', programA );
        programRuleA.setCondition( expressionA );
        programRuleService.addProgramRule( programRuleA );

        programRuleA2 = createProgramRule( 'Z', programA );
        programRuleA2.setCondition( calculatedDateExpression );
        programRuleService.addProgramRule( programRuleA2 );

        programRuleC = createProgramRule( 'X', programA );
        programRuleC.setCondition( expressionC );
        programRuleService.addProgramRule( programRuleC );

        programRuleS = createProgramRule( 'S', programS );
        programRuleS.setCondition( expressionS );
        programRuleService.addProgramRule( programRuleS );

        ProgramRuleVariable programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableA.setDataElement( dataElementA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );

        ProgramRuleVariable programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableB.setDataElement( dataElementB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );

        ProgramRuleVariable programRuleVariableDate = createProgramRuleVariable( 'X', programA );
        programRuleVariableDate.setName( "DOB" );
        programRuleVariableDate.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableDate.setDataElement( dataElementDate );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableDate );

        ProgramRuleVariable programRuleVariableAge = createProgramRuleVariable( 'K', programA );
        programRuleVariableAge.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableAge.setName( "AGE" );
        programRuleVariableAge.setDataElement( dataElementAge );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableAge );

        ProgramRuleVariable programRuleVariableC = createConstantProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableC.setAttribute( attributeA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );

        ProgramRuleVariable programRuleVariableD = createProgramRuleVariable( 'D', programA );
        programRuleVariableD.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableD.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableD );

        ProgramRuleVariable programRuleVariableS = createConstantProgramRuleVariable( 'S', programS );
        programRuleVariableS.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableS.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableS );
    }

    private void setUpSendMessageForEnrollment()
    {
        ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
        pnt.setName( "Test-PNT" );
        pnt.setMessageTemplate( "message_template" );
        pnt.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );
        pnt.setSubjectTemplate( "subject_template" );
        pnt.setNotificationTrigger( NotificationTrigger.PROGRAM_RULE );
        pnt.setAutoFields();
        pnt.setUid( "PNT-1" );

        programNotificationTemplateStore.save( pnt );

        ProgramRuleAction programRuleActionForSendMessage = createProgramRuleAction( 'C', programRuleC );
        programRuleActionForSendMessage.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionForSendMessage.setTemplateUid( pnt.getUid() );
        programRuleActionForSendMessage.setContent( "STATIC-TEXT" );
        programRuleActionService.addProgramRuleAction( programRuleActionForSendMessage );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForSendMessage ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpScheduleMessage()
    {
        scheduledDate = "2018-04-17";

        ProgramNotificationTemplate pnt = createNotification();
        programNotificationTemplateStore.save( pnt );

        ProgramRuleAction programRuleActionForScheduleMessage = createProgramRuleAction( 'S', programRuleS );
        programRuleActionForScheduleMessage.setProgramRuleActionType( ProgramRuleActionType.SCHEDULEMESSAGE );
        programRuleActionForScheduleMessage.setTemplateUid( pnt.getUid() );
        programRuleActionForScheduleMessage.setContent( "STATIC-TEXT-SCHEDULE" );
        programRuleActionForScheduleMessage.setData( dataExpression );
        programRuleActionService.addProgramRuleAction( programRuleActionForScheduleMessage );

        programRuleS.setProgramRuleActions( Sets.newHashSet( programRuleActionForScheduleMessage ) );
        programRuleService.updateProgramRule( programRuleS );
    }

    private void setUpAssignValueDate()
    {
        ProgramNotificationTemplate pnt = createNotification();
        programNotificationTemplateStore.save( pnt );

        ProgramRuleAction programRuleActionAssignValueDate = createProgramRuleAction( 'P', programRuleA2 );
        programRuleActionAssignValueDate.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionAssignValueDate.setData( " d2:yearsBetween(#{DOB}, V{event_date})" );

        programRuleActionService.addProgramRuleAction( programRuleActionAssignValueDate );

        programRuleA2.setProgramRuleActions( Sets.newHashSet( programRuleActionAssignValueDate ) );
        programRuleService.updateProgramRule( programRuleA2 );
    }

    private void setUpAssignValueAge()
    {
        ProgramNotificationTemplate pnt = createNotification();
        programNotificationTemplateStore.save( pnt );

        ProgramRuleAction programRuleActionAssignValueAge = createProgramRuleAction( 'P', programRuleA2 );
        programRuleActionAssignValueAge.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionAssignValueAge.setData( " d2:yearsBetween(#{AGE}, V{event_date})" );

        programRuleActionService.addProgramRuleAction( programRuleActionAssignValueAge );

        programRuleA2.setProgramRuleActions( Sets.newHashSet( programRuleActionAssignValueAge ) );
        programRuleService.updateProgramRule( programRuleA2 );
    }

    private ProgramNotificationTemplate createNotification()
    {
        ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
        pnt.setName( "Test-PNT-Schedule" );
        pnt.setMessageTemplate( "message_template" );
        pnt.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );
        pnt.setSubjectTemplate( "subject_template" );
        pnt.setNotificationTrigger( NotificationTrigger.PROGRAM_RULE );
        pnt.setAutoFields();
        pnt.setUid( "PNT-1-SCH" );

        return pnt;
    }
}
