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
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.*;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;

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

    private ProgramStage programStageB;

    private ProgramStage programStageC;

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

    private ProgramRuleVariable programRuleVariableA;

    private ProgramRuleVariable programRuleVariableB;

    private ProgramRuleVariable programRuleVariableC;

    private ProgramRuleVariable programRuleVariableD;

    private ProgramRule programRuleB;

    private ProgramRule programRuleC;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private ProgramInstance programInstanceA;

    private ProgramStageInstance programStageInstanceA;

    private ProgramStageInstance programStageInstanceB;

    private ProgramStageInstance programStageInstanceC;

    private TrackedEntityInstance entityInstanceA;

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

    private Collection<Integer> orgunitIds;

    private Date incidenDate;

    private Date enrollmentDate;

    private String expressionA = "#{ProgramRuleVariableA}=='malaria'";

    private String expressionB = "#{ProgramRuleVariableB}=='bcgdoze'";

    private String expressionC = "#{ProgramRuleVariableC} < #{ProgramRuleVariableD}";

    private String location = "feedback";

    private int weightA = 80;

    private int heightA = 165;

    private String programRuleActionCData = "#{ProgramRuleVariableC} + #{ProgramRuleVariableD}";

    private Integer programRuleActionCResult = weightA + heightA;

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

    @Override
    public void setUpTest()
    {
        dataElementA = createDataElement( 'A', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementC = createDataElement( 'C', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        dataElementD = createDataElement( 'D', ValueType.INTEGER, AggregationType.NONE, DataElementDomain.TRACKER );
        attributeA = createTrackedEntityAttribute('B', ValueType.TEXT );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        attributeService.addTrackedEntityAttribute(attributeA);

        setupEnrollment();
        setupProgramRuleEngine();
    }

    @Test
    public void testDisplayTextAction() throws Exception
    {
        setUpDisplayTextAction();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS1" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

        assertEquals( 2, ruleEffects.size() );
    }

    @Test
    public void testDisplayKeyValuePair() throws Exception
    {
        setUpDisplayKeyValueAction();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

        assertEquals( 1, ruleEffects.size() );
        assertTrue( ruleEffects.iterator().next().ruleAction() instanceof RuleActionAssign );
        assertEquals( programRuleActionCResult.toString(), ruleEffects.iterator().next().data() );
    }

    @Test
    public void testErrorComplete() throws Exception
    {
        setUpErrorOnComplete();

        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( "UID-PS3" );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

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

        List<RuleEffect> ruleEffects = programRuleEngine.evaluateEnrollment( programStageInstance );

        assertEquals( 1, ruleEffects.size() );

        RuleEffect ruleEffect = ruleEffects.iterator().next();

        assertTrue( ruleEffect.ruleAction() instanceof RuleActionWarningOnCompletion );
        assertEquals( "10", ruleEffect.data() );

        RuleActionWarningOnCompletion ruleActionWarningOnCompletion  = (RuleActionWarningOnCompletion) ruleEffects.iterator().next().ruleAction();

        assertNotNull( ruleActionWarningOnCompletion.field() );
        assertEquals( dataElementC.getUid(), ruleActionWarningOnCompletion.field() );
        assertEquals( "STATIC-TEXT", ruleActionWarningOnCompletion.content() );
    }

    @Test
    public void testSendMessage() throws Exception
    {
        //TODO For RuleActionSendMessage
    }

    private void setupEnrollment()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        int idA = organisationUnitService.addOrganisationUnit( organisationUnitA );

        organisationUnitB = createOrganisationUnit( 'B' );
        int idB = organisationUnitService.addOrganisationUnit( organisationUnitB );

        orgunitIds = new HashSet<>();
        orgunitIds.add( idA );
        orgunitIds.add( idB );

        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );

        programService.addProgram( programA );

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

        programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
        programService.addProgram( programB );

        programC = createProgram( 'C', new HashSet<>(), organisationUnitA );
        programService.addProgram( programC );

        entityInstanceA = createTrackedEntityInstance( 'A', organisationUnitA );
        entityInstanceService.addTrackedEntityInstance( entityInstanceA );

        TrackedEntityInstance entityInstanceB = createTrackedEntityInstance( 'B', organisationUnitB );
        TrackedEntityInstance entityInstanceC = createTrackedEntityInstance( 'C', organisationUnitB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceB );
        entityInstanceService.addTrackedEntityInstance( entityInstanceC );

        DateTime testDate1 = DateTime.now();
        testDate1.withTimeAtStartOfDay();
        testDate1 = testDate1.minusDays( 70 );
        incidenDate = testDate1.toDate();

        DateTime testDate2 = DateTime.now();
        testDate2.withTimeAtStartOfDay();
        enrollmentDate = testDate2.toDate();

        programInstanceA = new ProgramInstance( enrollmentDate, incidenDate, entityInstanceA, programA );
        programInstanceA.setUid( "UID-P" );
        programInstanceA.setOrganisationUnit( organisationUnitA );

        programInstanceService.addProgramInstance( programInstanceA );

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
        programRuleActionForDisplayTextA.setData("NO-EXPRESSION");
        programRuleActionService.addProgramRuleAction( programRuleActionForDisplayTextA );

        programRuleActionForDisplayTextB = createProgramRuleAction( 'A', programRuleB );
        programRuleActionForDisplayTextB.setProgramRuleActionType( ProgramRuleActionType.DISPLAYTEXT );
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
}