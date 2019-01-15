package org.hisp.dhis.programrule.engine;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.eventdatavalue.EventDataValueService;
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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngineTest extends DhisSpringTest
{
    private Program programA;

    private Program programS;

    private ProgramRule programRuleA;

    private ProgramRule programRuleC;

    private ProgramRule programRuleS;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private String scheduledDate;

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionC = "A{ProgramRuleVariableC}=='test'";

    private String expressionS = "A{ProgramRuleVariableS}=='xmen'";

    private String dataExpression = "d2:addDays('2018-04-15', '2')";

    @Autowired
    ProgramRuleEngine programRuleEngine;

    @Autowired
    private ProgramRuleEngineService programRuleEngineService;

    @Autowired
    private List<RuleActionImplementer> ruleActionImplementers;

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

    @Autowired
    private EventDataValueService eventDataValueService;

    @Override
    public void setUpTest()
    {
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementC = createDataElement( 'C', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementD = createDataElement( 'D', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        attributeA = createTrackedEntityAttribute( 'A' );
        attributeB = createTrackedEntityAttribute( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        attributeService.addTrackedEntityAttribute( attributeA );
        attributeService.addTrackedEntityAttribute( attributeB );

        setupEvents();
        setupProgramRuleEngine();
    }

    @Test
    public void testSendMessageForEnrollment() throws Exception
    {
        setUpSendMessageForEnrollment();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-P1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionSendMessage );

        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;

        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    public void testSendMessageForEvent() throws Exception
    {
        setUpSendMessageForEnrollment();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionSendMessage );

        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;

        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    public void testSchedulingByProgramRule() throws Exception
    {
        setUpScheduleMessage();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-PS" );

        List<RuleEffect> ruleEffects = programRuleEngineService.evaluate( programInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionScheduleMessage );

        RuleActionScheduleMessage ruleActionScheduleMessage = (RuleActionScheduleMessage) ruleAction;

        assertEquals( "PNT-1-SCH", ruleActionScheduleMessage.notification() );
        assertEquals( scheduledDate, ruleEffects.get( 0 ).data() );

        // For duplication detection

        List<RuleEffect> ruleEffects2 = programRuleEngineService.evaluate( programInstance );

        assertNotNull( ruleEffects2.get( 0 ) );

        RuleActionImplementer ruleActionImplementer = ruleActionImplementers.stream()
            .filter( r -> r.accept( ruleEffects2.get( 0 ).ruleAction() ) ).findFirst().get();

        RuleActionScheduleMessageImplementer messageImplementer2 = (RuleActionScheduleMessageImplementer) ruleActionImplementer;

        RuleActionScheduleMessage ruleActionScheduleMessage2 = (RuleActionScheduleMessage) ruleEffects2.get( 0 ).ruleAction();

        assertNotNull( programNotificationTemplateStore.getByUid( ruleActionScheduleMessage2.notification() ) );
        // duplicate enrollment/events will be ignored and validation will be failed.
        assertFalse( messageImplementer2.validate( ruleEffects2.get( 0 ), programInstance ) );
    }

    private void setupEvents()
    {
        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );
        int idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        OrganisationUnit organisationUnitB = createOrganisationUnit( 'B' );
        int idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programS = createProgram( 'S', new HashSet<>(), organisationUnitB );

        programService.addProgram( programA );
        programService.addProgram( programS );

        ProgramTrackedEntityAttribute attribute = createProgramTrackedEntityAttribute( 'A' );
        attribute.setUid( "ATTR-UID" );
        attribute.setAttribute( attributeB );
        attribute.setProgram( programA );
        attribute.setProgram( programS );

        programTrackedEntityAttributeStore.save( attribute );

        programA.setProgramAttributes( Arrays.asList( attribute ) );
        programS.setProgramAttributes( Arrays.asList( attribute ) );

        programService.updateProgram( programA );
        programService.updateProgram( programS );

        ProgramStage programStageA = createProgramStage( 'A', programA );
        ProgramStage programStageB = createProgramStage( 'B', programA );
        ProgramStage programStageC = createProgramStage( 'C', programA );

        programStageService.saveProgramStage( programStageA );
        programStageService.saveProgramStage( programStageB );
        programStageService.saveProgramStage( programStageC );

        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA, 1 );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( programStageB, dataElementB, 2 );
        ProgramStageDataElement programStageDataElementC = createProgramStageDataElement( programStageC, dataElementC, 3 );
        ProgramStageDataElement programStageDataElementD = createProgramStageDataElement( programStageC, dataElementD, 4 );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementC );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementD );

        programStageA.setSortOrder( 1 );
        programStageB.setSortOrder( 2 );
        programStageC.setSortOrder( 3 );

        programStageA.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageB.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageC.setProgramStageDataElements( Sets.newHashSet( programStageDataElementC, programStageDataElementD ) );

        programStageService.updateProgramStage( programStageA );
        programStageService.updateProgramStage( programStageB );
        programStageService.updateProgramStage( programStageC );

        programA.setProgramStages( Sets.newHashSet( programStageA, programStageB, programStageC ) );

        programService.updateProgram( programA );

        TrackedEntityInstance entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        TrackedEntityInstance entityInstanceS = createTrackedEntityInstance( 'S', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceS );

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue( attributeA, entityInstanceA, "test" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValue );

        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceB, "xmen" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueB );

        TrackedEntityAttributeValue attributeValueS = new TrackedEntityAttributeValue( attributeB, entityInstanceS, "xmen" );
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

        ProgramInstance programInstanceA = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programA, enrollmentDate, incidenDate, organisationUnitA );
        programInstanceA.setUid("UID-P1");
        programInstanceService.updateProgramInstance( programInstanceA );

        ProgramInstance programInstanceS = programInstanceService.enrollTrackedEntityInstance( entityInstanceS, programS, enrollmentDate, incidenDate, organisationUnitB );
        programInstanceS.setUid("UID-PS");
        programInstanceService.updateProgramInstance( programInstanceS );

        ProgramStageInstance programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setUid( "UID-PS1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

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

        ProgramStageInstance programStageInstanceTempA = programStageInstanceService.getProgramStageInstance( "UID-PS1" );
        ProgramStageInstance programStageInstanceTempB = programStageInstanceService.getProgramStageInstance( "UID-PS2" );
        ProgramStageInstance programStageInstanceTempC = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        String storedBy = "test-user";
        EventDataValue diagnosis = new EventDataValue( dataElementA.getUid(), "malaria", storedBy );
        EventDataValue bcgdoze = new EventDataValue( dataElementB.getUid(), "bcgdoze", storedBy );
        EventDataValue weight = new EventDataValue( dataElementC.getUid(), "80", storedBy );
        EventDataValue height = new EventDataValue( dataElementD.getUid(), "165", storedBy );

        eventDataValueService.saveEventDataValue( programStageInstanceTempA, diagnosis );
        eventDataValueService.saveEventDataValue( programStageInstanceTempA, bcgdoze );
        eventDataValueService.saveEventDataValue( programStageInstanceTempB, diagnosis );
        eventDataValueService.saveEventDataValue( programStageInstanceTempC, weight );
        eventDataValueService.saveEventDataValue( programStageInstanceTempC, height );

        programInstanceA.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceA, programStageInstanceB, programStageInstanceC ) );
        programInstanceService.updateProgramInstance( programInstanceA );
    }

    private void setupProgramRuleEngine()
    {
        programRuleA = createProgramRule( 'C', programA );
        programRuleA.setCondition( expressionA );
        programRuleService.addProgramRule( programRuleA );

        programRuleC = createProgramRule( 'C', programA );
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

        ProgramRuleVariable programRuleVariableC = createProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableC.setAttribute( attributeA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );

        ProgramRuleVariable programRuleVariableD = createProgramRuleVariable( 'D', programA );
        programRuleVariableD.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableD.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableD );

        ProgramRuleVariable programRuleVariableS = createProgramRuleVariable( 'S', programS );
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
        programRuleActionForSendMessage.setTemplateUid(  pnt.getUid() );
        programRuleActionForSendMessage.setContent( "STATIC-TEXT" );
        programRuleActionService.addProgramRuleAction( programRuleActionForSendMessage );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForSendMessage ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpScheduleMessage()
    {
        scheduledDate = "2018-04-17";

        ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
        pnt.setName( "Test-PNT-Schedule" );
        pnt.setMessageTemplate( "message_template" );
        pnt.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );
        pnt.setSubjectTemplate( "subject_template" );
        pnt.setNotificationTrigger( NotificationTrigger.PROGRAM_RULE );
        pnt.setAutoFields();
        pnt.setUid( "PNT-1-SCH" );

        programNotificationTemplateStore.save( pnt );

        ProgramRuleAction programRuleActionForScheduleMessage = createProgramRuleAction( 'S', programRuleS );
        programRuleActionForScheduleMessage.setProgramRuleActionType( ProgramRuleActionType.SCHEDULEMESSAGE );
        programRuleActionForScheduleMessage.setTemplateUid(  pnt.getUid() );
        programRuleActionForScheduleMessage.setContent( "STATIC-TEXT-SCHEDULE" );
        programRuleActionForScheduleMessage.setData( dataExpression );
        programRuleActionService.addProgramRuleAction( programRuleActionForScheduleMessage );

        programRuleS.setProgramRuleActions( Sets.newHashSet( programRuleActionForScheduleMessage ) );
        programRuleService.updateProgramRule( programRuleS );
    }
}
