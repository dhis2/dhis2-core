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
package org.hisp.dhis.programrule.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.hamcrest.Matchers;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.option.OptionSet;
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
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
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
import org.hisp.dhis.rules.models.*;
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
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
class ProgramRuleEngineTest extends TransactionalIntegrationTest
{

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    private Program programA;

    private Program programB;

    private Program programS;

    private ProgramRule programRuleA;

    private ProgramRule programRuleA2;

    private ProgramRule programRuleC;

    private ProgramRule programRuleE;

    private ProgramRule programRuleS;

    private ProgramRule programRuleToTestOptionSet;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private DataElement dataElementDate;

    private DataElement dataElementAge;

    private DataElement assignedDataElement;

    private DataElement dataElementWithOptionSet;

    private OptionSet optionSet;

    private Option option1;

    private Option option2;

    private EventDataValue eventDataValueDate;

    private EventDataValue eventDataValueAge;

    private EventDataValue eventDataValueWithOptionSet;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private TrackedEntityAttribute attributeEmail;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramNotificationTemplate pnt;

    private String scheduledDate;

    private String dob = "1984-01-01";

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionC = "A{C1234567890}=='test'";

    private String expressionE = "d2:hasValue('attribute_email')";

    private String expressionS = "A{S1234567890}=='xmen'";

    private String dataExpression = "d2:addDays('2018-04-15', '2')";

    private String calculatedDateExpression = "true";

    private Date psEventDate;

    @Qualifier( "notificationRuleEngine" )
    @Autowired
    ProgramRuleEngine programRuleEngine;

    @Autowired
    private ProgramRuleEngineService programRuleEngineService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private TrackedEntityService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private EventService eventService;

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

    @Autowired
    private ProgramNotificationInstanceService programNotificationInstanceService;

    @Autowired
    private NotificationLoggingService notificationLoggingService;

    @Autowired
    private OptionService optionService;

    @Override
    public void setUpTest()
        throws ParseException,
        ConflictException
    {
        optionSet = new OptionSet( "optionSetName", ValueType.TEXT );
        option1 = new Option( "optionName1", "optionCode1" );
        option2 = new Option( "optionName2", "optionCode2" );
        optionSet.addOption( option1 );
        optionSet.addOption( option2 );

        optionService.saveOptionSet( optionSet );

        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementC = createDataElement( 'C', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementD = createDataElement( 'D', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementDate = createDataElement( 'T', ValueType.DATE, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementAge = createDataElement( 'G', ValueType.AGE, AggregationType.NONE, DataElementDomain.TRACKER );
        assignedDataElement = createDataElement( 'K', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementWithOptionSet = createDataElement( 'L', ValueType.TEXT, AggregationType.NONE,
            DataElementDomain.TRACKER );
        dataElementWithOptionSet.setOptionSet( optionSet );

        attributeA = createTrackedEntityAttribute( 'A' );
        attributeB = createTrackedEntityAttribute( 'B' );
        attributeEmail = createTrackedEntityAttribute( 'E' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        dataElementService.addDataElement( dataElementDate );
        dataElementService.addDataElement( dataElementAge );
        dataElementService.addDataElement( assignedDataElement );
        dataElementService.addDataElement( dataElementWithOptionSet );

        attributeService.addTrackedEntityAttribute( attributeA );
        attributeService.addTrackedEntityAttribute( attributeB );
        attributeService.addTrackedEntityAttribute( attributeEmail );
        Calendar cal = Calendar.getInstance();
        cal.setTime( simpleDateFormat.parse( dob ) );
        cal.add( Calendar.YEAR, 10 );
        psEventDate = cal.getTime();
        setupEvents();
        setupProgramRuleEngine();
    }

    @Test
    void testSendMessageForEnrollment()
    {
        ProgramRule programRule = setUpSendMessageForEnrollment();
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-P1" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( enrollment, Sets.newHashSet(),
            List.of( programRule ) );
        assertEquals( 1, ruleEffects.size() );
        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, ruleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;
        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    void testSendMessageForEnrollmentAndEvents()
    {
        ProgramRule programRule = setUpSendMessageForEnrollment();
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-P1" );
        List<RuleEffects> ruleEffects = programRuleEngine.evaluateEnrollmentAndEvents( enrollment,
            Sets.newHashSet(), Lists.newArrayList() );
        assertEquals( 1, ruleEffects.size() );
        RuleEffects enrollmentRuleEffects = ruleEffects.get( 0 );
        assertTrue( enrollmentRuleEffects.isEnrollment() );
        assertEquals( "UID-P1", enrollmentRuleEffects.getTrackerObjectUid() );
        RuleAction ruleAction = enrollmentRuleEffects.getRuleEffects().get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, ruleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;
        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    void testNotificationWhenUsingD2HasValueWithTEA()
    {
        ProgramRule programRule = setUpNotificationForD2HasValue();
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-P2" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( enrollment, Sets.newHashSet(),
            List.of( programRule ) );
        assertEquals( 1, ruleEffects.size() );
        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, ruleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;
        assertEquals( "PNT-2", ruleActionSendMessage.notification() );
        ProgramNotificationTemplate template = programNotificationTemplateStore.getByUid( "PNT-2" );
        assertNotNull( template );
        assertEquals( NotificationTrigger.PROGRAM_RULE, template.getNotificationTrigger() );
        assertEquals( ProgramNotificationRecipient.PROGRAM_ATTRIBUTE, template.getNotificationRecipient() );
        assertEquals( "message_template", template.getMessageTemplate() );
    }

    @Test
    void testNotificationWhenUsingD2HasValueWithTEAForEnrollmentAndEvents()
    {
        setUpNotificationForD2HasValue();
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-P2" );
        List<RuleEffects> ruleEffects = programRuleEngine.evaluateEnrollmentAndEvents( enrollment,
            Sets.newHashSet(), Lists.newArrayList() );
        assertEquals( 1, ruleEffects.size() );
        RuleEffects enrollmentRuleEffects = ruleEffects.get( 0 );
        assertTrue( enrollmentRuleEffects.isEnrollment() );
        assertEquals( "UID-P2", enrollmentRuleEffects.getTrackerObjectUid() );
        RuleAction ruleAction = enrollmentRuleEffects.getRuleEffects().get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, ruleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;
        assertEquals( "PNT-2", ruleActionSendMessage.notification() );
        ProgramNotificationTemplate template = programNotificationTemplateStore.getByUid( "PNT-2" );
        assertNotNull( template );
        assertEquals( NotificationTrigger.PROGRAM_RULE, template.getNotificationTrigger() );
        assertEquals( ProgramNotificationRecipient.PROGRAM_ATTRIBUTE, template.getNotificationRecipient() );
        assertEquals( "message_template", template.getMessageTemplate() );
    }

    @Test
    void testSendMessageForEvent()
    {
        ProgramRule programRule = setUpSendMessageForEnrollment();
        Event event = eventService.getEvent( "UID-PS1" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( event.getEnrollment(),
            event, Sets.newHashSet(), List.of( programRule ) );
        assertEquals( 1, ruleEffects.size() );
        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, ruleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;
        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
        ProgramNotificationTemplate template = programNotificationTemplateStore.getByUid( "PNT-1" );
        assertNotNull( template );
        assertEquals( NotificationTrigger.PROGRAM_RULE, template.getNotificationTrigger() );
        assertEquals( ProgramNotificationRecipient.USER_GROUP, template.getNotificationRecipient() );
        assertEquals( "message_template", template.getMessageTemplate() );
    }

    @Test
    void testSendMessageForEnrollmentAndEvent()
    {
        setUpSendMessageForEnrollment();
        Event event = eventService.getEvent( "UID-PS1" );
        List<RuleEffects> ruleEffects = programRuleEngine.evaluateEnrollmentAndEvents(
            event.getEnrollment(), Sets.newHashSet( event ), Lists.newArrayList() );
        assertEquals( 2, ruleEffects.size() );
        RuleEffects enrollmentRuleEffects = ruleEffects.stream().filter( RuleEffects::isEnrollment ).findFirst().get();
        RuleEffects eventRuleEffects = ruleEffects.stream().filter( RuleEffects::isEvent ).findFirst().get();
        assertEquals( "UID-PS1", eventRuleEffects.getTrackerObjectUid() );
        RuleAction eventRuleAction = eventRuleEffects.getRuleEffects().get( 0 ).ruleAction();
        RuleAction enrollmentRuleAction = enrollmentRuleEffects.getRuleEffects().get( 0 ).ruleAction();
        assertInstanceOf( RuleActionSendMessage.class, eventRuleAction );
        assertInstanceOf( RuleActionSendMessage.class, enrollmentRuleAction );
        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) eventRuleAction;
        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
        ProgramNotificationTemplate template = programNotificationTemplateStore.getByUid( "PNT-1" );
        assertNotNull( template );
        assertEquals( NotificationTrigger.PROGRAM_RULE, template.getNotificationTrigger() );
        assertEquals( ProgramNotificationRecipient.USER_GROUP, template.getNotificationRecipient() );
        assertEquals( "message_template", template.getMessageTemplate() );
    }

    @Test
    void testSchedulingByProgramRule()
    {
        setUpScheduleMessage();
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-PS" );
        List<RuleEffect> ruleEffects = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( enrollment.getId() );
        assertEquals( 1, ruleEffects.size() );
        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();
        assertInstanceOf( RuleActionScheduleMessage.class, ruleAction );
        RuleActionScheduleMessage ruleActionScheduleMessage = (RuleActionScheduleMessage) ruleAction;
        assertEquals( "PNT-1-SCH", ruleActionScheduleMessage.notification() );
        assertEquals( scheduledDate, ruleEffects.get( 0 ).data() );
        // For duplication detection
        List<RuleEffect> ruleEffects2 = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( enrollment.getId() );
        assertNotNull( ruleEffects2.get( 0 ) );
        assertInstanceOf( RuleActionScheduleMessage.class, ruleEffects2.get( 0 ).ruleAction() );
        RuleActionScheduleMessage ruleActionScheduleMessage2 = (RuleActionScheduleMessage) ruleEffects2.get( 0 )
            .ruleAction();
        assertNotNull( programNotificationTemplateStore.getByUid( ruleActionScheduleMessage2.notification() ) );
        assertEquals( 1, programNotificationInstanceService.getProgramNotificationInstances(
            ProgramNotificationInstanceParam.builder().enrollment( enrollment ).build() ).size() );
    }

    @Test
    void testSendRepeatableTemplates()
    {
        setUpScheduleMessage();
        pnt.setSendRepeatable( true );
        Enrollment enrollment = enrollmentService.getEnrollment( "UID-PS" );
        List<RuleEffect> ruleEffects = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( enrollment.getId() );
        assertEquals( 1, ruleEffects.size() );
        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();
        assertInstanceOf( RuleActionScheduleMessage.class, ruleAction );
        RuleActionScheduleMessage ruleActionScheduleMessage = (RuleActionScheduleMessage) ruleAction;
        assertEquals( "PNT-1-SCH", ruleActionScheduleMessage.notification() );
        assertEquals( scheduledDate, ruleEffects.get( 0 ).data() );
        List<RuleEffect> ruleEffects2 = programRuleEngineService
            .evaluateEnrollmentAndRunEffects( enrollment.getId() );
        assertNotNull( ruleEffects2.get( 0 ) );
        assertInstanceOf( RuleActionScheduleMessage.class, ruleEffects2.get( 0 ).ruleAction() );
        RuleActionScheduleMessage ruleActionScheduleMessage2 = (RuleActionScheduleMessage) ruleEffects2.get( 0 )
            .ruleAction();
        assertNotNull( programNotificationTemplateStore.getByUid( ruleActionScheduleMessage2.notification() ) );
        List<ProgramNotificationInstance> instances = programNotificationInstanceService
            .getProgramNotificationInstances(
                ProgramNotificationInstanceParam.builder().enrollment( enrollment ).build() );
        assertEquals( 2, instances.size() );
        assertEquals( instances.get( 0 ).getProgramNotificationTemplateId(),
            instances.get( 1 ).getProgramNotificationTemplateId() );
        ExternalNotificationLogEntry logEntry = notificationLoggingService.getByTemplateUid( pnt.getUid() );
        assertTrue( logEntry.isAllowMultiple() );
    }

    @Test
    void testAssignValueTypeDate()
    {
        ProgramRule programRule = setUpAssignValueDate();
        Event event = eventService.getEvent( "UID-PS12" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( event.getEnrollment(),
            event, Sets.newHashSet(), List.of( programRule ) );
        assertNotNull( ruleEffects );
        assertEquals( ruleEffects.get( 0 ).data(), "10" );
    }

    @Test
    void testAssignValueTypeDateEnrollmentAndEvent()
    {
        setUpAssignValueDate();
        Event event = eventService.getEvent( "UID-PS12" );
        List<RuleEffects> ruleEffects = programRuleEngine.evaluateEnrollmentAndEvents(
            event.getEnrollment(), Sets.newHashSet( event ), Lists.newArrayList() );
        assertNotNull( ruleEffects );
        assertEquals( 2, ruleEffects.size() );
        assertTrue( ruleEffects.stream().filter( e -> e.isEnrollment() ).findFirst().get().getRuleEffects().isEmpty() );
        assertEquals(
            ruleEffects.stream().filter( e -> e.isEvent() ).findFirst().get().getRuleEffects().get( 0 ).data(), "10" );
    }

    @Test
    void testAssignValueTypeAge()
    {
        ProgramRule programRule = setUpAssignValueAge();
        Event event = eventService.getEvent( "UID-PS13" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( event.getEnrollment(),
            event, Sets.newHashSet(), List.of( programRule ) );
        assertNotNull( ruleEffects );
        assertEquals( ruleEffects.get( 0 ).data(), "10" );
    }

    @Test
    void testAssignOptionSetName()
    {
        setUpRuleActionForOptionSetName();

        Event event = eventService.getEvent( "UID-PS13" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( event.getEnrollment(),
            event, Sets.newHashSet(), List.of( programRuleToTestOptionSet ) );

        assertNotNull( ruleEffects );
        assertInstanceOf( RuleActionShowWarning.class, ruleEffects.get( 0 ).ruleAction() );
        assertThat( ruleEffects.get( 0 ).data(), Matchers.is( option1.getName() ) );
    }

    @Test
    void testAssignOptionSetCode()
    {
        setUpRuleActionForOptionSetCode();

        Event event = eventService.getEvent( "UID-PS13" );
        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( event.getEnrollment(),
            event, Sets.newHashSet(), List.of( programRuleToTestOptionSet ) );

        assertNotNull( ruleEffects );
        assertInstanceOf( RuleActionShowError.class, ruleEffects.get( 0 ).ruleAction() );
        assertThat( ruleEffects.get( 0 ).data(), Matchers.is( option1.getCode() ) );
    }

    private void setupEvents()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnitA );
        organisationUnitB = createOrganisationUnit( 'B' );
        organisationUnitService.addOrganisationUnit( organisationUnitB );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programS = createProgram( 'S', new HashSet<>(), organisationUnitB );
        programB = createProgram( 'E', new HashSet<>(), organisationUnitA );
        programService.addProgram( programA );
        programService.addProgram( programS );
        programService.addProgram( programB );
        ProgramTrackedEntityAttribute attribute = createProgramTrackedEntityAttribute( programS, attributeB );
        attribute.setUid( "ATTR-UID" );
        ProgramTrackedEntityAttribute programAttributeEmail = createProgramTrackedEntityAttribute( programB,
            attributeEmail );
        attribute.setUid( "ATTR-UID2" );
        programTrackedEntityAttributeStore.save( attribute );
        programTrackedEntityAttributeStore.save( attribute );
        programA.setProgramAttributes( Arrays.asList( attribute ) );
        programS.setProgramAttributes( Arrays.asList( attribute ) );
        programB.setProgramAttributes( Arrays.asList( programAttributeEmail ) );
        programService.updateProgram( programA );
        programService.updateProgram( programS );
        programService.updateProgram( programB );
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

        ProgramStageDataElement programStageDEWithOptionSet = createProgramStageDataElement( programStageAge,
            dataElementWithOptionSet, 6 );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementC );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementD );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementDate );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementAge );

        programStageDataElementService.addProgramStageDataElement( programStageDEWithOptionSet );
        programStageA.setSortOrder( 1 );
        programStageB.setSortOrder( 2 );
        programStageC.setSortOrder( 3 );
        programStageA.setProgramStageDataElements(
            Sets.newHashSet( programStageDataElementA, programStageDataElementB, programStageDataElementDate ) );
        programStageAge
            .setProgramStageDataElements( Sets.newHashSet( programStageDataElementDate, programStageDEWithOptionSet ) );
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
        TrackedEntity entityInstanceA = createTrackedEntity( organisationUnitA );
        entityInstanceService.addTrackedEntity( entityInstanceA );
        TrackedEntity entityInstanceB = createTrackedEntity( organisationUnitB );
        entityInstanceService.addTrackedEntity( entityInstanceB );
        TrackedEntity entityInstanceS = createTrackedEntity( organisationUnitB );
        entityInstanceService.addTrackedEntity( entityInstanceS );
        TrackedEntity entityInstanceE = createTrackedEntity( organisationUnitA );
        entityInstanceService.addTrackedEntity( entityInstanceE );
        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue( attributeA, entityInstanceA,
            "test" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValue );
        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceB,
            "xmen" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueB );
        TrackedEntityAttributeValue attributeValueS = new TrackedEntityAttributeValue( attributeB, entityInstanceS,
            "xmen" );
        TrackedEntityAttributeValue attributeValueEmail = new TrackedEntityAttributeValue( attributeEmail,
            entityInstanceE, "zubair@dhis2.org" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueS );
        entityInstanceA.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValue ) );
        entityInstanceService.updateTrackedEntity( entityInstanceA );
        entityInstanceB.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueB ) );
        entityInstanceService.updateTrackedEntity( entityInstanceB );
        entityInstanceS.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueS ) );
        entityInstanceService.updateTrackedEntity( entityInstanceS );
        entityInstanceE.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueEmail ) );
        entityInstanceService.updateTrackedEntity( entityInstanceE );
        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        Date incidentDate = testDate1.toDate();
        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        Date enrollmentDate = testDate2.toDate();
        Enrollment enrollmentA = enrollmentService.enrollTrackedEntity( entityInstanceA,
            programA, enrollmentDate, incidentDate, organisationUnitA );
        enrollmentA.setUid( "UID-P1" );
        enrollmentService.updateEnrollment( enrollmentA );
        Enrollment enrollmentE = enrollmentService.enrollTrackedEntity( entityInstanceE,
            programB, enrollmentDate, incidentDate, organisationUnitA );
        enrollmentE.setUid( "UID-P2" );
        enrollmentService.updateEnrollment( enrollmentE );
        Enrollment enrollmentS = enrollmentService.enrollTrackedEntity( entityInstanceS,
            programS, enrollmentDate, incidentDate, organisationUnitB );
        enrollmentS.setUid( "UID-PS" );
        enrollmentService.updateEnrollment( enrollmentS );
        Event eventA = new Event( enrollmentA, programStageA );
        eventA.setDueDate( enrollmentDate );
        eventA.setExecutionDate( new Date() );
        eventA.setUid( "UID-PS1" );
        eventService.addEvent( eventA );
        eventDataValueDate = new EventDataValue();
        eventDataValueDate.setDataElement( dataElementDate.getUid() );
        eventDataValueDate.setAutoFields();
        eventDataValueDate.setValue( dob );

        eventDataValueAge = new EventDataValue();
        eventDataValueAge.setDataElement( dataElementAge.getUid() );
        eventDataValueAge.setAutoFields();
        eventDataValueAge.setValue( dob );

        eventDataValueWithOptionSet = new EventDataValue();
        eventDataValueWithOptionSet.setDataElement( dataElementWithOptionSet.getUid() );
        eventDataValueWithOptionSet.setAutoFields();
        eventDataValueWithOptionSet.setValue( option1.getCode() );

        Event eventDate = new Event( enrollmentA, programStageAge );
        eventDate.setDueDate( enrollmentDate );
        eventDate.setExecutionDate( psEventDate );
        eventDate.setUid( "UID-PS12" );
        eventDate.setEventDataValues( Sets.newHashSet( eventDataValueDate ) );
        eventService.addEvent( eventDate );
        Event eventAge = new Event( enrollmentA, programStageAge );
        eventAge.setDueDate( enrollmentDate );
        eventAge.setExecutionDate( psEventDate );
        eventAge.setUid( "UID-PS13" );
        eventAge.setEventDataValues( Sets.newHashSet( eventDataValueAge, eventDataValueWithOptionSet ) );
        eventService.addEvent( eventAge );
        Event eventB = new Event( enrollmentA, programStageB );
        eventB.setDueDate( enrollmentDate );
        eventB.setExecutionDate( new Date() );
        eventB.setUid( "UID-PS2" );
        eventService.addEvent( eventB );
        Event eventC = new Event( enrollmentA, programStageC );
        eventC.setDueDate( enrollmentDate );
        eventC.setExecutionDate( new Date() );
        eventC.setUid( "UID-PS3" );
        eventService.addEvent( eventC );
        enrollmentA.getEvents().addAll( Sets.newHashSet( eventA,
            eventB, eventC, eventAge ) );
        enrollmentService.updateEnrollment( enrollmentA );
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
        programRuleE = createProgramRule( 'E', programB );
        programRuleE.setCondition( expressionE );
        programRuleService.addProgramRule( programRuleE );
        programRuleS = createProgramRule( 'S', programS );
        programRuleS.setCondition( expressionS );
        programRuleService.addProgramRule( programRuleS );

        programRuleToTestOptionSet = createProgramRule( 'O', programA );
        programRuleToTestOptionSet.setCondition( "true" );
        programRuleService.addProgramRule( programRuleToTestOptionSet );

        ProgramRuleVariable programRuleVariableEmail = createProgramRuleVariableWithTEA( 'E', programB,
            attributeEmail );
        programRuleVariableEmail.setName( "attribute_email" );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableEmail );
        ProgramRuleVariable programRuleVariableA = createProgramRuleVariableWithDataElement( 'A', programA,
            dataElementA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );
        ProgramRuleVariable programRuleVariableB = createProgramRuleVariableWithDataElement( 'B', programA,
            dataElementB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );
        ProgramRuleVariable programRuleVariableDate = createProgramRuleVariableWithDataElement( 'X', programA,
            dataElementDate );
        programRuleVariableDate.setName( "DOB" );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableDate );
        ProgramRuleVariable programRuleVariableAge = createProgramRuleVariableWithDataElement( 'K', programA,
            dataElementAge );
        programRuleVariableAge.setName( "AGE" );

        ProgramRuleVariable programRuleVariableWithDeOpName = createProgramRuleVariableWithDataElement( 'O', programA,
            dataElementWithOptionSet );
        programRuleVariableWithDeOpName.setName( "prv-de-option-set-name" );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableWithDeOpName );

        ProgramRuleVariable programRuleVariableWithDeOpCode = createProgramRuleVariableWithDataElement( 'L', programA,
            dataElementWithOptionSet );
        programRuleVariableWithDeOpCode.setName( "prv-de-option-set-code" );
        programRuleVariableWithDeOpCode.setUseCodeForOptionSet( true );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableWithDeOpCode );

        programRuleVariableService.addProgramRuleVariable( programRuleVariableAge );
        ProgramRuleVariable programRuleVariableD = createProgramRuleVariableWithTEA( 'D', programA, attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableD );

        ProgramRuleVariable programRuleVariableS = createConstantProgramRuleVariable( 'S', programS );
        programRuleVariableS.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableS.setAttribute( attributeB );
        programRuleVariableS.setValueType( attributeB.getValueType() );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableS );
        ProgramRuleVariable programRuleVariableC = createConstantProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableC.setAttribute( attributeA );
        programRuleVariableC.setValueType( attributeA.getValueType() );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );

    }

    private ProgramRule setUpSendMessageForEnrollment()
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
        return programRuleC;
    }

    private ProgramRule setUpNotificationForD2HasValue()
    {
        ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
        pnt.setName( "Test-PNT" );
        pnt.setMessageTemplate( "message_template" );
        pnt.setSubjectTemplate( "subject_template" );
        pnt.setNotificationTrigger( NotificationTrigger.PROGRAM_RULE );
        pnt.setRecipientProgramAttribute( attributeEmail );
        pnt.setNotificationRecipient( ProgramNotificationRecipient.PROGRAM_ATTRIBUTE );
        pnt.setAutoFields();
        pnt.setUid( "PNT-2" );
        programNotificationTemplateStore.save( pnt );
        ProgramRuleAction programRuleActionForSendMessage = createProgramRuleAction( 'C', programRuleE );
        programRuleActionForSendMessage.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionForSendMessage.setTemplateUid( pnt.getUid() );
        programRuleActionForSendMessage.setContent( "STATIC-TEXT" );
        programRuleActionService.addProgramRuleAction( programRuleActionForSendMessage );
        programRuleE.setProgramRuleActions( Sets.newHashSet( programRuleActionForSendMessage ) );
        programRuleService.updateProgramRule( programRuleE );

        return programRuleE;
    }

    private ProgramRule setUpScheduleMessage()
    {
        scheduledDate = "2018-04-17";
        pnt = createNotification();
        programNotificationTemplateStore.save( pnt );
        ProgramRuleAction programRuleActionForScheduleMessage = createProgramRuleAction( 'S', programRuleS );
        programRuleActionForScheduleMessage.setProgramRuleActionType( ProgramRuleActionType.SCHEDULEMESSAGE );
        programRuleActionForScheduleMessage.setTemplateUid( pnt.getUid() );
        programRuleActionForScheduleMessage.setContent( "STATIC-TEXT-SCHEDULE" );
        programRuleActionForScheduleMessage.setData( dataExpression );
        programRuleActionService.addProgramRuleAction( programRuleActionForScheduleMessage );
        programRuleS.setProgramRuleActions( Sets.newHashSet( programRuleActionForScheduleMessage ) );
        programRuleService.updateProgramRule( programRuleS );

        return programRuleS;
    }

    private ProgramRule setUpAssignValueDate()
    {
        ProgramNotificationTemplate pnt = createNotification();
        programNotificationTemplateStore.save( pnt );
        ProgramRuleAction programRuleActionAssignValueDate = createProgramRuleAction( 'P', programRuleA2 );
        programRuleActionAssignValueDate.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionAssignValueDate.setData( " d2:yearsBetween(#{DOB}, V{event_date})" );
        programRuleActionService.addProgramRuleAction( programRuleActionAssignValueDate );
        programRuleA2.setProgramRuleActions( Sets.newHashSet( programRuleActionAssignValueDate ) );
        programRuleA2.setCondition( " d2:hasValue(#{DOB})" );
        programRuleService.updateProgramRule( programRuleA2 );

        return programRuleA2;
    }

    private ProgramRule setUpAssignValueAge()
    {
        ProgramNotificationTemplate pnt = createNotification();
        programNotificationTemplateStore.save( pnt );
        ProgramRuleAction programRuleActionAssignValueAge = createProgramRuleAction( 'P', programRuleA2 );
        programRuleActionAssignValueAge.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionAssignValueAge.setData( " d2:yearsBetween(#{AGE}, V{event_date})" );
        programRuleActionService.addProgramRuleAction( programRuleActionAssignValueAge );
        programRuleA2.setProgramRuleActions( Sets.newHashSet( programRuleActionAssignValueAge ) );
        programRuleService.updateProgramRule( programRuleA2 );

        return programRuleA2;
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

    private void setUpRuleActionForOptionSetName()
    {
        ProgramRuleAction programRuleAction = createProgramRuleAction( 'X', programRuleToTestOptionSet );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleAction.setData( "#{prv-de-option-set-name}" );

        programRuleActionService.addProgramRuleAction( programRuleAction );
        programRuleToTestOptionSet.getProgramRuleActions().clear();
        programRuleToTestOptionSet.getProgramRuleActions().add( programRuleAction );

        programRuleService.updateProgramRule( programRuleToTestOptionSet );
    }

    private void setUpRuleActionForOptionSetCode()
    {
        ProgramRuleAction programRuleAction = createProgramRuleAction( 'Z', programRuleToTestOptionSet );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.SHOWERROR );
        programRuleAction.setData( "#{prv-de-option-set-code}" );

        programRuleActionService.addProgramRuleAction( programRuleAction );
        programRuleToTestOptionSet.getProgramRuleActions().clear();
        programRuleToTestOptionSet.getProgramRuleActions().add( programRuleAction );

        programRuleService.updateProgramRule( programRuleToTestOptionSet );
    }
}
