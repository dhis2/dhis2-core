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

    private Program programB;

    private Program programC;

    private ProgramStage programStageA;

    private ProgramStage programStageB1;

    private ProgramStage programStageB;

    private ProgramStage programStageC;

    private ProgramStage programStageE;

    private ProgramRule programRuleA;

    private ProgramRuleAction programRuleActionForDisplayTextA;

    private ProgramRuleAction programRuleActionForDisplayTextB;

    private ProgramRuleAction programRuleActionForAssign;

    private ProgramRuleAction programRuleActionForDisplayKeyValue;

    private ProgramRuleAction programRuleActionForErrorOnComplete;

    private ProgramRuleAction programRuleActionForHideField;

    private ProgramRuleAction programRuleActionForShowError;

    private ProgramRuleAction programRuleActionForShowWarning;

    private ProgramRuleAction programRuleActionForMandatoryField;

    private ProgramRuleAction programRuleActionForWarningOnComplete;

    private ProgramRuleAction programRuleActionForSendMessage;

    private ProgramRuleAction programRuleActionForDisplayTextForEnrollment;

    private ProgramRuleVariable programRuleVariableA;

    private ProgramRuleVariable programRuleVariableB;

    private ProgramRuleVariable programRuleVariableC;

    private ProgramRuleVariable programRuleVariableD;

    private ProgramRuleVariable programRuleVariableE;

    private ProgramRuleVariable programRuleVariableF;

    private ProgramRule programRuleB;

    private ProgramRule programRuleC;

    private ProgramRule programRuleD;

    private ProgramRule programRuleE;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramInstance programInstanceB;

    private ProgramInstance programInstanceC;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB1;

    private ProgramStageInstance programStageInstanceB;

    private ProgramStageInstance programStageInstanceC;

    private TrackedEntityInstance entityInstanceA;

    private TrackedEntityInstance entityInstanceB;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private ProgramStageDataElement programStageDataElementA;

    private ProgramStageDataElement programStageDataElementB;

    private ProgramStageDataElement programStageDataElementC;

    private ProgramStageDataElement programStageDataElementD;

    private ProgramStageDataElement programStageDataElementB1;

    private TrackedEntityDataValue diagnosis;

    private TrackedEntityDataValue bcgdoze;

    private TrackedEntityDataValue weight;

    private TrackedEntityDataValue height;

    private TrackedEntityAttribute attributeA;

    private TrackedEntityAttribute attributeB;

    private Collection<Integer> orgunitIds;

    private Date incidenDate;

    private Date enrollmentDate;

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionB = "#{ProgramRuleVariableB}=='bcgdoze'";

    private String expressionC = "#{ProgramRuleVariableC} < #{ProgramRuleVariableD}";

    private String expressionE = "A{ProgramRuleVariableE}=='test'";

    private String expressionF = "A{ProgramRuleVariableF}=='xmen'";

    private String location = "feedback";

    private String programRuleActionCData = "#{ProgramRuleVariableC} + #{ProgramRuleVariableD}";

    @Autowired
    ProgramRuleEngine programRuleEngine;

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
    public void testEnrollment() throws Exception
    {
        setUpEnrollmentTest();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-P2" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );

        assertEquals( 1, ruleEffects.size() );
    }

    @Test
    public void testSendMessageForEnrollment() throws Exception
    {
        setUpSendMessageForEnrollment();

        ProgramInstance programInstance = programInstanceService.getProgramInstance( "UID-P3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleAction ruleAction = ruleEffects.get( 0 ).ruleAction();

        assertTrue( ruleAction instanceof RuleActionSendMessage );

        RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) ruleAction;

        assertEquals( "PNT-1", ruleActionSendMessage.notification() );
    }

    @Test
    public void testDisplayTextAction() throws Exception
    {
        setUpDisplayTextAction();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 2, ruleEffects.size() );
    }

   @Test
    public void testDisplayKeyValuePair() throws Exception
    {
        setUpDisplayKeyValueAction();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionDisplayKeyValuePair );

        RuleActionDisplayKeyValuePair ruleActionDisplayKeyValuePair = (RuleActionDisplayKeyValuePair) ruleEffects.iterator().next().ruleAction();

        assertEquals( location, ruleActionDisplayKeyValuePair.location() );
    }

    @Test
    public void testAssignAction() throws Exception
    {
        setUpAssignRuleAction();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionAssign );
    }

    @Test
    public void testErrorComplete() throws Exception
    {
        setUpErrorOnComplete();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionErrorOnCompletion);

        RuleActionErrorOnCompletion ruleActionErrorOnCompletion = (RuleActionErrorOnCompletion) ruleEffects.iterator().next().ruleAction();

        assertEquals( dataElementC.getUid(), ruleActionErrorOnCompletion.field() );
    }

    @Test
    public void testHideField() throws Exception
    {
        setUpHideField();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionHideField );

        RuleActionHideField ruleActionHideField = (RuleActionHideField) ruleEffects.iterator().next().ruleAction();

        assertEquals( "HIDE-FIELD-TEST", ruleActionHideField.content() );
        assertEquals( dataElementC.getUid(), ruleActionHideField.field() );
    }

    @Test
    public void testShowError() throws Exception
    {
        setUpShowError();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionShowError );

        RuleActionShowError ruleActionShowError = (RuleActionShowError) ruleEffects.iterator().next().ruleAction();

        assertEquals( "SHOW-ERROR-TEST", ruleActionShowError.content() );
        assertEquals( attributeA.getUid(), ruleActionShowError.field() );
    }

    @Test
    public void testShowWarning() throws Exception
    {
        setUpShowWarning();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionShowWarning );

        RuleActionShowWarning ruleActionShowWarning = (RuleActionShowWarning) ruleEffects.iterator().next().ruleAction();

        assertEquals( "SHOW-WARNING-TEST", ruleActionShowWarning.content() );
        assertEquals( attributeA.getUid(), ruleActionShowWarning.field() );
    }

    @Test
    public void testSetMandatoryField() throws Exception
    {
        setUpMandatoryField();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionSetMandatoryField );

        RuleActionSetMandatoryField ruleActionSetMandatoryField  = (RuleActionSetMandatoryField) ruleEffects.iterator().next().ruleAction();

        assertNotNull( ruleActionSetMandatoryField.field() );
        assertEquals( dataElementC.getUid(), ruleActionSetMandatoryField.field() );
    }

    @Test
    public void testWarningOnComplete() throws Exception
    {
        setUpWarningOnComplete();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEvent( programStageInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleEffect ruleEffect = ruleEffects.iterator().next();

        assertTrue( ruleEffect.ruleAction() instanceof RuleActionWarningOnCompletion );
        assertEquals( "10", ruleEffect.data() );

        RuleActionWarningOnCompletion ruleActionWarningOnCompletion  = (RuleActionWarningOnCompletion) ruleEffects.iterator().next().ruleAction();

        assertNotNull( ruleActionWarningOnCompletion.field() );
        assertEquals( dataElementC.getUid(), ruleActionWarningOnCompletion.field() );
        assertEquals( "STATIC-TEXT", ruleActionWarningOnCompletion.content() );
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
        programB = createProgram( 'B', new HashSet<>(), organisationUnitB );
        programC = createProgram( 'C', new HashSet<>(), organisationUnitB );

        programService.addProgram( programA );
        programService.addProgram( programB );
        programService.addProgram( programC );

        ProgramTrackedEntityAttribute attribute = createProgramTrackedEntityAttribute( 'A' );
        attribute.setUid( "ATTR-UID" );
        attribute.setAttribute( attributeB );
        attribute.setProgram( programA );
        attribute.setProgram( programB );
        attribute.setProgram( programC );

        programTrackedEntityAttributeStore.save( attribute );

        programA.setProgramAttributes( Arrays.asList( attribute ) );
        programB.setProgramAttributes( Arrays.asList( attribute ) );
        programC.setProgramAttributes( Arrays.asList( attribute ) );

        programService.updateProgram( programA );
        programService.updateProgram( programB );
        programService.updateProgram( programC );

        programStageA = createProgramStage( 'A', programA );
        programStageB = createProgramStage( 'B', programA );
        programStageC = createProgramStage( 'C', programA );

        programStageB1 = createProgramStage( 'I', programB );
        programStageE = createProgramStage( 'I', programC );

        programStageService.saveProgramStage( programStageA );
        programStageService.saveProgramStage( programStageB );
        programStageService.saveProgramStage( programStageC );
        programStageService.saveProgramStage( programStageB1 );
        programStageService.saveProgramStage( programStageE );

        programStageDataElementA = createProgramStageDataElement( programStageA, dataElementA, 1 );
        programStageDataElementB = createProgramStageDataElement( programStageB, dataElementB, 2 );
        programStageDataElementC = createProgramStageDataElement( programStageC, dataElementC, 3 );
        programStageDataElementD = createProgramStageDataElement( programStageC, dataElementD, 4 );
        programStageDataElementB1 = createProgramStageDataElement( programStageB1, dataElementD, 1 );

        programStageDataElementService.addProgramStageDataElement( programStageDataElementA );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementC );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementD );
        programStageDataElementService.addProgramStageDataElement( programStageDataElementB1 );

        programStageA.setSortOrder( 1 );
        programStageB.setSortOrder( 2 );
        programStageC.setSortOrder( 3 );
        programStageB1.setSortOrder( 1 );
        programStageE.setSortOrder( 1 );

        programStageA.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageB.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA, programStageDataElementB ) );
        programStageC.setProgramStageDataElements( Sets.newHashSet( programStageDataElementC, programStageDataElementD ) );
        programStageB1.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB1 ) );
        programStageE.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );

        programStageService.updateProgramStage( programStageA );
        programStageService.updateProgramStage( programStageB );
        programStageService.updateProgramStage( programStageC );
        programStageService.updateProgramStage( programStageB1 );
        programStageService.updateProgramStage( programStageE );

        programA.setProgramStages( Sets.newHashSet( programStageA, programStageB, programStageC ) );
        programB.setProgramStages( Sets.newHashSet( programStageB1 ) );
        programC.setProgramStages( Sets.newHashSet( programStageE ) );

        programService.updateProgram( programA );
        programService.updateProgram( programB );
        programService.updateProgram( programC );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );

        TrackedEntityAttributeValue attributeValue = new TrackedEntityAttributeValue( attributeB, entityInstanceA, "test" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValue );

        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( attributeB, entityInstanceB, "xmen" );
        trackedEntityAttributeValueService.addTrackedEntityAttributeValue( attributeValueB );

        entityInstanceA.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValue ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceA );

        entityInstanceB.setTrackedEntityAttributeValues( Sets.newHashSet( attributeValueB ) );
        entityInstanceService.updateTrackedEntityInstance( entityInstanceB );


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

        programInstanceB = programInstanceService.enrollTrackedEntityInstance( entityInstanceA, programB, enrollmentDate, incidenDate, organisationUnitB );
        programInstanceB.setUid("UID-P2");
        programInstanceService.updateProgramInstance( programInstanceB );

        programInstanceC = programInstanceService.enrollTrackedEntityInstance( entityInstanceB, programC, enrollmentDate, incidenDate, organisationUnitB );
        programInstanceC.setUid("UID-P3");
        programInstanceService.updateProgramInstance( programInstanceC );

        programStageInstanceA = new ProgramStageInstance( programInstanceA, programStageA );
        programStageInstanceA.setDueDate( enrollmentDate );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setUid( "UID-PS1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceA );

        programStageInstanceB1 = new ProgramStageInstance( programInstanceB, programStageA );
        programStageInstanceB1.setDueDate( enrollmentDate );
        programStageInstanceB1.setExecutionDate( new Date() );
        programStageInstanceB1.setUid( "UID-PS2-1" );
        programStageInstanceService.addProgramStageInstance( programStageInstanceB1 );

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
        ProgramStageInstance programStageInstanceTempB1 = programStageInstanceService.getProgramStageInstance( "UID-PS2-1" );

        diagnosis = new TrackedEntityDataValue( programStageInstanceTempA, dataElementA, "malaria" );
        bcgdoze = new TrackedEntityDataValue( programStageInstanceTempA, dataElementB, "bcgdoze" );
        weight = new TrackedEntityDataValue( programStageInstanceTempC, dataElementC, "80" );
        height = new TrackedEntityDataValue( programStageInstanceTempC, dataElementD, "165" );

        valueService.saveTrackedEntityDataValue( diagnosis );
        valueService.saveTrackedEntityDataValue( bcgdoze );
        valueService.saveTrackedEntityDataValue( weight );
        valueService.saveTrackedEntityDataValue( height );

        programStageInstanceTempA.setDataValues( Sets.newHashSet( diagnosis, bcgdoze ) );
        programStageInstanceTempB1.setDataValues( Sets.newHashSet( diagnosis ) );
        programStageInstanceTempB.setDataValues( Sets.newHashSet( diagnosis ) );
        programStageInstanceTempC.setDataValues( Sets.newHashSet( weight, height ) );

        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempA );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempB1 );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempB );
        programStageInstanceService.updateProgramStageInstance( programStageInstanceTempC );

        programInstanceA.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceA, programStageInstanceB, programStageInstanceC ) );
        programInstanceB.getProgramStageInstances().addAll( Sets.newHashSet( programStageInstanceTempB1 ) );
        programInstanceService.updateProgramInstance( programInstanceA );
        programInstanceService.updateProgramInstance( programInstanceB );

        trackedEntityDataValueService.saveTrackedEntityDataValue( diagnosis );
        trackedEntityDataValueService.saveTrackedEntityDataValue( bcgdoze );
        trackedEntityDataValueService.saveTrackedEntityDataValue( weight );
        trackedEntityDataValueService.saveTrackedEntityDataValue( height );
    }

    private void setupProgramRuleEngine()
    {
        programRuleA = createProgramRule( 'A', programA );
        programRuleA.setCondition( expressionA );
        programRuleService.addProgramRule( programRuleA );

        programRuleB = createProgramRule( 'B', programA );
        programRuleB.setCondition( expressionB );
        programRuleService.addProgramRule( programRuleB );

        programRuleC = createProgramRule( 'C', programA );
        programRuleC.setCondition( expressionC );
        programRuleService.addProgramRule( programRuleC );

        programRuleD = createProgramRule( 'D', programB );
        programRuleD.setCondition( expressionE );
        programRuleService.addProgramRule( programRuleD );

        programRuleE = createProgramRule( 'E', programC );
        programRuleE.setCondition( expressionF );
        programRuleService.addProgramRule( programRuleE );

        programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableA.setDataElement( dataElementA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );

        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableB.setDataElement( dataElementB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );

        programRuleVariableC = createProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableC.setDataElement( dataElementC );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );

        programRuleVariableD = createProgramRuleVariable( 'D', programA );
        programRuleVariableD.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableD.setDataElement( dataElementD );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableD );

        programRuleVariableE = createProgramRuleVariable( 'E', programB );
        programRuleVariableE.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableE.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableE );

        programRuleVariableF = createProgramRuleVariable( 'F', programC );
        programRuleVariableF.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableF.setAttribute( attributeB );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableF );
    }

    private void setUpEnrollmentTest()
    {
        programRuleActionForDisplayTextForEnrollment = createProgramRuleAction( 'J', programRuleD );
        programRuleActionForDisplayTextForEnrollment.setProgramRuleActionType( ProgramRuleActionType.DISPLAYTEXT );
        programRuleActionForDisplayTextForEnrollment.setLocation( location );
        programRuleActionForDisplayTextForEnrollment.setContent("DUMMY-CONTENT-ENROLLMENT");
        programRuleActionForDisplayTextForEnrollment.setData("NO-EXPRESSION-ENROLLMENT");
        programRuleActionService.addProgramRuleAction( programRuleActionForDisplayTextForEnrollment );

        programRuleD.setProgramRuleActions( Sets.newHashSet( programRuleActionForDisplayTextForEnrollment ) );
        programRuleService.updateProgramRule( programRuleD );
    }

    private void setUpAssignRuleAction()
    {
        programRuleActionForAssign = createProgramRuleAction( 'C', programRuleC );
        programRuleActionForAssign.setProgramRuleActionType( ProgramRuleActionType.ASSIGN );
        programRuleActionForAssign.setDataElement( dataElementC );
        programRuleActionForAssign.setContent("DUMMY-CONTENT3");
        programRuleActionForAssign.setData( programRuleActionCData );
        programRuleActionService.addProgramRuleAction( programRuleActionForAssign );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForAssign ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpDisplayTextAction()
    {
        programRuleActionForDisplayTextA = createProgramRuleAction( 'A', programRuleA );
        programRuleActionForDisplayTextA.setProgramRuleActionType( ProgramRuleActionType.DISPLAYTEXT );
        programRuleActionForDisplayTextA.setContent("DUMMY-CONTENT");
        programRuleActionForDisplayTextA.setLocation( location );
        programRuleActionForDisplayTextA.setData("NO-EXPRESSION");
        programRuleActionService.addProgramRuleAction( programRuleActionForDisplayTextA );

        programRuleActionForDisplayTextB = createProgramRuleAction( 'A', programRuleB );
        programRuleActionForDisplayTextB.setProgramRuleActionType( ProgramRuleActionType.DISPLAYTEXT );
        programRuleActionForDisplayTextB.setLocation( location );
        programRuleActionForDisplayTextB.setContent("DUMMY-CONTENT2");
        programRuleActionForDisplayTextB.setData("NO-EXPRESSION2");
        programRuleActionService.addProgramRuleAction( programRuleActionForDisplayTextB );

        programRuleA.setProgramRuleActions( Sets.newHashSet( programRuleActionForDisplayTextA ) );
        programRuleB.setProgramRuleActions( Sets.newHashSet( programRuleActionForDisplayTextB ) );

        programRuleService.updateProgramRule( programRuleA );
        programRuleService.updateProgramRule( programRuleB );
    }

    private void setUpDisplayKeyValueAction()
    {
        programRuleActionForDisplayKeyValue = createProgramRuleAction( 'C', programRuleC );
        programRuleActionForDisplayKeyValue.setProgramRuleActionType( ProgramRuleActionType.DISPLAYKEYVALUEPAIR );
        programRuleActionForDisplayKeyValue.setDataElement( dataElementC );
        programRuleActionForDisplayKeyValue.setContent("KEY-VALUE-CONTENT");
        programRuleActionForDisplayKeyValue.setLocation( location );
        programRuleActionForDisplayKeyValue.setData( programRuleActionCData );
        programRuleActionService.addProgramRuleAction( programRuleActionForDisplayKeyValue );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForDisplayKeyValue ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpErrorOnComplete()
    {
        programRuleActionForErrorOnComplete = createProgramRuleAction( 'D', programRuleC );
        programRuleActionForErrorOnComplete.setProgramRuleActionType( ProgramRuleActionType.ERRORONCOMPLETE );
        programRuleActionForErrorOnComplete.setDataElement( dataElementC );
        programRuleActionForErrorOnComplete.setContent("ERROR-ON-COMPLETE-TEST");
        programRuleActionForErrorOnComplete.setData( programRuleActionCData );
        programRuleActionService.addProgramRuleAction( programRuleActionForErrorOnComplete );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForErrorOnComplete ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpHideField()
    {
        programRuleActionForHideField = createProgramRuleAction( 'E', programRuleC );
        programRuleActionForHideField.setProgramRuleActionType( ProgramRuleActionType.HIDEFIELD );
        programRuleActionForHideField.setDataElement( dataElementC );
        programRuleActionForHideField.setContent( "HIDE-FIELD-TEST" );

        programRuleActionService.addProgramRuleAction( programRuleActionForHideField );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForHideField ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpShowError()
    {
        programRuleActionForShowError = createProgramRuleAction( 'F', programRuleC );
        programRuleActionForShowError.setProgramRuleActionType( ProgramRuleActionType.SHOWERROR );
        programRuleActionForShowError.setAttribute( attributeA );
        programRuleActionForShowError.setContent( "SHOW-ERROR-TEST" );

        programRuleActionService.addProgramRuleAction( programRuleActionForShowError );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForShowError ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpShowWarning()
    {
        programRuleActionForShowWarning = createProgramRuleAction( 'G', programRuleC );
        programRuleActionForShowWarning.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleActionForShowWarning.setAttribute( attributeA );
        programRuleActionForShowWarning.setContent( "SHOW-WARNING-TEST" );

        programRuleActionService.addProgramRuleAction( programRuleActionForShowWarning );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForShowWarning ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpMandatoryField()
    {
        programRuleActionForMandatoryField = createProgramRuleAction( 'H', programRuleC );
        programRuleActionForMandatoryField.setProgramRuleActionType( ProgramRuleActionType.SETMANDATORYFIELD );
        programRuleActionForMandatoryField.setDataElement( dataElementC );
        programRuleActionService.addProgramRuleAction( programRuleActionForMandatoryField );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForMandatoryField ) );
        programRuleService.updateProgramRule( programRuleC );
    }

    private void setUpWarningOnComplete()
    {
        programRuleActionForWarningOnComplete = createProgramRuleAction( 'I', programRuleC );
        programRuleActionForWarningOnComplete.setProgramRuleActionType( ProgramRuleActionType.WARNINGONCOMPLETE );
        programRuleActionForWarningOnComplete.setDataElement( dataElementC );
        programRuleActionForWarningOnComplete.setData( "5+5" );
        programRuleActionForWarningOnComplete.setContent( "STATIC-TEXT" );
        programRuleActionService.addProgramRuleAction( programRuleActionForWarningOnComplete );

        programRuleC.setProgramRuleActions( Sets.newHashSet( programRuleActionForWarningOnComplete ) );
        programRuleService.updateProgramRule( programRuleC );
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

        programRuleActionForSendMessage = createProgramRuleAction( 'K', programRuleE );
        programRuleActionForSendMessage.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionForSendMessage.setProgramNotificationTemplate(  pnt );
        programRuleActionForSendMessage.setContent( "STATIC-TEXT" );
        programRuleActionService.addProgramRuleAction( programRuleActionForSendMessage );

        programRuleE.setProgramRuleActions( Sets.newHashSet( programRuleActionForSendMessage ) );
        programRuleService.updateProgramRule( programRuleC );
    }
}