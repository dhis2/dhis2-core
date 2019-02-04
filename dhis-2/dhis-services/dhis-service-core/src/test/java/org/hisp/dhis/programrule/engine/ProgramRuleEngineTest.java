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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.UserService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by zubair@dhis2.org on 11.10.17.
 */
public class ProgramRuleEngineTest extends DhisSpringTest
{
    private Program programA;

    private Program programS;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private ProgramStage programStageC;

    private ProgramRuleAction programRuleActionForSendMessage;

    private ProgramRuleAction programRuleActionForScheduleMessage;

    private ProgramRuleVariable programRuleVariableA;

    private ProgramRuleVariable programRuleVariableB;

    private ProgramRuleVariable programRuleVariableC;

    private ProgramRuleVariable programRuleVariableD;

    private ProgramRuleVariable programRuleVariableS;

    private ProgramRule programRuleA;

    private ProgramRule programRuleC;

    private ProgramRule programRuleS;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceS;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private ProgramStageInstance programStageInstanceC;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private TrackedEntityInstance entityInstanceS;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private ProgramStageDataElement programStageDataElementA;

    private ProgramStageDataElement programStageDataElementB;

    private ProgramStageDataElement programStageDataElementC;

    private ProgramStageDataElement programStageDataElementD;

    private TrackedEntityDataValue diagnosis;

    private TrackedEntityDataValue bcgdoze;

    private TrackedEntityDataValue weight;

    private TrackedEntityDataValue height;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private Collection<Integer> orgunitIds;

    private Date incidenDate;

    private Date enrollmentDate;

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
    private TrackedEntityDataValueService trackedEntityDataValueService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityDataValueService valueService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private ProgramTrackedEntityAttributeStore programTrackedEntityAttributeStore;

    @Autowired
    private ProgramNotificationTemplateStore programNotificationTemplateStore;

    @Override
    public void setUpTest()
    {
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementC = createDataElement( 'C', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementD = createDataElement( 'D', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        attributeA = createTrackedEntityAttribute('A', ValueType.TEXT );
        attributeB = createTrackedEntityAttribute('B', ValueType.TEXT );

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
        organisationUnitA = createOrganisationUnit( 'A' );
        int idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        int idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );

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

        programStageA = createProgramStage( 'A', programA );
        programStageB = createProgramStage( 'B', programA );
        programStageC = createProgramStage( 'C', programA );

        programStageService.saveProgramStage( programStageA );
        programStageService.saveProgramStage( programStageB );
        programStageService.saveProgramStage( programStageC );

        programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA, 1 );
        programStageDataElementB = createProgramStageDataElement( programStageB, dataElementB, 2 );
        programStageDataElementC = createProgramStageDataElement( programStageC, dataElementC, 3 );
        programStageDataElementD = createProgramStageDataElement( programStageC, dataElementD, 4 );

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

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        entityInstanceS = createTrackedEntityInstance( 'S', organisationUnitB );
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
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programA, enrollmentDate, incidenDate, organisationUnitA );
        programInstanceA.setUid("UID-P1");
        programInstanceService.updateProgramInstance( programInstanceA );

        programInstanceS = programInstanceService.enrollTrackedEntityInstance( entityInstanceS, programS, enrollmentDate, incidenDate, organisationUnitB );
        programInstanceS.setUid("UID-PS");
        programInstanceService.updateProgramInstance( programInstanceS );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setUid( "UID-PS1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programStageInstanceB = new ProgramStageInstance( programInstanceA, programStageB );
        programStageInstanceB.setDueDate( enrollmentDate );
        programStageInstanceB.setExecutionDate( new Date() );
        programStageInstanceB.setUid( "UID-PS2" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB );

        programStageInstanceC = new ProgramStageInstance( programInstanceA, programStageC );
        programStageInstanceC.setDueDate( enrollmentDate );
        programStageInstanceC.setExecutionDate( new Date() );
        programStageInstanceC.setUid( "UID-PS3" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceC );

        ProgramStageInstance programStageInstanceTempA = programStageInstanceService.getProgramStageInstance( "UID-PS1" );
        ProgramStageInstance programStageInstanceTempB = programStageInstanceService.getProgramStageInstance( "UID-PS2" );
        ProgramStageInstance programStageInstanceTempC = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        diagnosis = new TrackedEntityDataValue( programStageInstanceTempA, dataElementA, "malaria" );
        bcgdoze = new TrackedEntityDataValue( programStageInstanceTempA, dataElementB, "bcgdoze" );
        weight = new TrackedEntityDataValue( programStageInstanceTempC, dataElementC, "80" );
        height = new TrackedEntityDataValue( programStageInstanceTempC, dataElementD, "165" );

        valueService.saveTrackedEntityDataValue( diagnosis );
        valueService.saveTrackedEntityDataValue( bcgdoze );
        valueService.saveTrackedEntityDataValue( weight );
        valueService.saveTrackedEntityDataValue( height );

        programStageInstanceTempA.setDataValues( Sets.newHashSet( diagnosis, bcgdoze ) );
        programStageInstanceTempB.setDataValues( Sets.newHashSet( diagnosis ) );
        programStageInstanceTempC.setDataValues( Sets.newHashSet( weight, height ) );

        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempA );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempB );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempC );

        programInstanceA.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceA, programStageInstanceB, programStageInstanceC ) );
        programInstanceService.updateProgramInstance( programInstanceA );

        trackedEntityDataValueService.saveTrackedEntityDataValue( diagnosis );
        trackedEntityDataValueService.saveTrackedEntityDataValue( bcgdoze );
        trackedEntityDataValueService.saveTrackedEntityDataValue( weight );
        trackedEntityDataValueService.saveTrackedEntityDataValue( height );

        userService = (UserService) getBean( UserService.ID );
        createAndInjectAdminUser();
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
        programS.getProgramRules().add( programRuleS );
        programService.updateProgram( programS );

        programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableA.setDataElement( dataElementA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );

        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableB.setDataElement( dataElementB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );

        programRuleVariableC = createProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableC.setAttribute( attributeA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );

        programRuleVariableD = createProgramRuleVariable( 'D', programA );
        programRuleVariableD.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableD.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableD );

        programRuleVariableS = createProgramRuleVariable( 'S', programS );
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

        programRuleActionForSendMessage = createProgramRuleAction( 'C', programRuleC );
        programRuleActionForSendMessage.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionForSendMessage.setProgramNotificationTemplate(  pnt );
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

        programRuleActionForScheduleMessage = createProgramRuleAction( 'S', programRuleS );
        programRuleActionForScheduleMessage.setProgramRuleActionType( ProgramRuleActionType.SCHEDULEMESSAGE );
        programRuleActionForScheduleMessage.setProgramNotificationTemplate(  pnt );
        programRuleActionForScheduleMessage.setContent( "STATIC-TEXT-SCHEDULE" );
        programRuleActionForScheduleMessage.setData( dataExpression );
        programRuleActionService.addProgramRuleAction( programRuleActionForScheduleMessage );

        programRuleS.setProgramRuleActions( Sets.newHashSet( programRuleActionForScheduleMessage ) );
        programRuleService.updateProgramRule( programRuleS );
    }
}