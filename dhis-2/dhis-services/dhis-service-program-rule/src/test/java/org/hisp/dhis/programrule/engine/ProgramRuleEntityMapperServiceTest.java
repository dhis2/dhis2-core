package org.hisp.dhis.programrule.engine;

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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleDataValue;
import org.hisp.dhis.rules.models.RuleEnrollment;
import org.hisp.dhis.rules.models.RuleEvent;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @Author Zubair Asghar.
 */
public class ProgramRuleEntityMapperServiceTest extends DhisConvenienceTest
{
    private static final String SAMPLE_VALUE_A = "textValueA";
    private static final String SAMPLE_VALUE_B = "textValueB";

    private List<ProgramRule> programRules = new ArrayList<>();
    private List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

    private Program program;
    private ProgramStage programStage;

    private ProgramRule programRuleA = null;
    private ProgramRule programRuleB = null;
    private ProgramRule programRuleC = null;
    private ProgramRule programRuleD = null;

    private ProgramRuleAction assignAction = null;
    private ProgramRuleAction sendMessageAction = null;
    private ProgramRuleAction displayText = null;

    private ProgramRuleVariable programRuleVariableA = null;
    private ProgramRuleVariable programRuleVariableB = null;

    private OrganisationUnit organisationUnit;

    private TrackedEntityAttribute trackedEntityAttribute;
    private TrackedEntityAttributeValue trackedEntityAttributeValue;
    private DataElement dataElement;
    private EventDataValue eventDataValueA;
    private EventDataValue eventDataValueB;

    private ProgramInstance programInstance;
    private ProgramInstance programInstanceB;
    private TrackedEntityInstance trackedEntityInstance;

    private ProgramStageInstance programStageInstanceA;
    private ProgramStageInstance programStageInstanceB;
    private ProgramStageInstance programStageInstanceC;

    @org.junit.Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @org.junit.Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramRuleVariableService programRuleVariableService;

    @Mock
    private DataElementService dataElementService;

    private DefaultProgramRuleEntityMapperService subject;

    @Before
    public void initTest()
    {
        subject = new DefaultProgramRuleEntityMapperService( programRuleService, programRuleVariableService,
            dataElementService );

        setUpProgramRules();

        when( programRuleService.getAllProgramRule() ).thenReturn( programRules );
        when( programRuleVariableService.getAllProgramRuleVariable() ).thenReturn( programRuleVariables );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );
    }

    @Test
    public void testMappedProgramRules()
    {
        List<Rule> rules = subject.toMappedProgramRules();

        assertEquals( 3, rules.size() );
    }

    @Test
    public void testWhenProgramRuleConditionIsNull()
    {
        programRules.get( 0 ).setCondition( null );

        List<Rule> rules = subject.toMappedProgramRules();

        programRules.get( 0 ).setCondition( "" );

        assertEquals( 2, rules.size() );
    }

    @Test
    public void testWhenProgramRuleActionIsNull()
    {
        programRules.get( 0 ).setProgramRuleActions( null );

        List<Rule> rules = subject.toMappedProgramRules();

        programRules.get( 0 ).setProgramRuleActions( Sets.newHashSet( assignAction ) );

        assertEquals( 2, rules.size() );
    }

    @Test
    public void testMappedRuleVariableValues()
    {
        List<RuleVariable> ruleVariables = subject.toMappedProgramRuleVariables();

        assertEquals( ruleVariables.size(), 2 );
    }

    @Test
    public void testExceptionWhenMandatoryFieldIsMissingInRuleEvent()
    {
        thrown.expect( IllegalStateException.class );

        subject.toMappedRuleEvent( programStageInstanceC );
    }

    @Test
    public void testExceptionIfDataElementIsNull()
    {
        thrown.expect( RuntimeException.class );
        thrown.expectMessage( "Required DataElement(" + dataElement.getUid() + ") was not found." );

        when( dataElementService.getDataElement( anyString() ) ).thenReturn( null );

        subject.toMappedRuleEvent( programStageInstanceA );
    }

    @Test
    public void testMappedRuleEvent()
    {
        RuleEvent ruleEvent = subject.toMappedRuleEvent( programStageInstanceA );

        assertEquals( ruleEvent.event(), programStageInstanceA.getUid() );
        assertEquals( ruleEvent.programStage(), programStageInstanceA.getProgramStage().getUid() );
        assertEquals( ruleEvent.organisationUnit(), programStageInstanceA.getOrganisationUnit().getUid() );
        assertEquals( ruleEvent.organisationUnitCode(), programStageInstanceA.getOrganisationUnit().getCode() );
        assertEquals( ruleEvent.programStageName(), programStageInstanceA.getProgramStage().getName() );
        assertEquals( ruleEvent.dataValues().size(), 1 );

        RuleDataValue ruleDataValue = ruleEvent.dataValues().get( 0 );

        assertEquals( ruleDataValue.dataElement(), dataElement.getUid() );
        assertEquals( ruleDataValue.eventDate(), ruleEvent.eventDate() );
        assertEquals( ruleDataValue.programStage(), programStageInstanceA.getProgramStage().getUid() );
        assertEquals( ruleDataValue.value(), SAMPLE_VALUE_A );
    }

    @Test
    public void testMappedRuleEventsWithFilter()
    {
        List<RuleEvent> ruleEvents = subject.toMappedRuleEvents( Sets.newHashSet( programStageInstanceA, programStageInstanceB ), programStageInstanceA  );

        assertEquals( ruleEvents.size(), 1 );
        RuleEvent ruleEvent = ruleEvents.get( 0 );

        assertEquals( ruleEvent.event(), programStageInstanceB.getUid() );
        assertEquals( ruleEvent.programStage(), programStageInstanceB.getProgramStage().getUid() );
        assertEquals( ruleEvent.organisationUnit(), programStageInstanceB.getOrganisationUnit().getUid() );
        assertEquals( ruleEvent.organisationUnitCode(), programStageInstanceB.getOrganisationUnit().getCode() );
        assertEquals( ruleEvent.programStageName(), programStageInstanceB.getProgramStage().getName() );
        assertEquals( ruleEvent.dataValues().size(), 1 );

        RuleDataValue ruleDataValue = ruleEvent.dataValues().get( 0 );

        assertEquals( ruleDataValue.dataElement(), dataElement.getUid() );
        assertEquals( ruleDataValue.eventDate(), ruleEvent.eventDate() );
        assertEquals( ruleDataValue.programStage(), programStageInstanceB.getProgramStage().getUid() );
        assertEquals( ruleDataValue.value(), SAMPLE_VALUE_B );
    }

    @Test
    public void testMappedRuleEvents()
    {
        List<RuleEvent> ruleEvents = subject.toMappedRuleEvents( Sets.newHashSet( programStageInstanceA, programStageInstanceB )  );

        assertEquals( ruleEvents.size(), 2 );
    }

    @Test
    public void testExceptionWhenMandatoryValueMissingMappedEnrollment()
    {
        thrown.expect( IllegalStateException.class );

        subject.toMappedRuleEnrollment( programInstanceB );
    }

    @Test
    public void testMappedEnrollment()
    {
        RuleEnrollment ruleEnrollment = subject.toMappedRuleEnrollment( programInstance );

        assertEquals( ruleEnrollment.enrollment(), programInstance.getUid() );
        assertEquals( ruleEnrollment.organisationUnit(), programInstance.getOrganisationUnit().getUid() );
        assertEquals( ruleEnrollment.attributeValues().size(), 1 );
        assertEquals( ruleEnrollment.attributeValues().get( 0 ).value(), SAMPLE_VALUE_A );
    }

    private void setUpProgramRules()
    {
        program = createProgram('P' );
        programStage = createProgramStage( 'S', program );

        programRuleVariableA = createProgramRuleVariable( 'V', program );
        programRuleVariableB = createProgramRuleVariable( 'W', program );
        programRuleVariableA = setProgramRuleVariable( programRuleVariableA, ProgramRuleVariableSourceType.CALCULATED_VALUE, program, null, createDataElement( 'D' ), null );
        programRuleVariableB = setProgramRuleVariable( programRuleVariableB, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, program, null, null, createTrackedEntityAttribute( 'Z' ) );

        programRuleVariables.add( programRuleVariableA );
        programRuleVariables.add( programRuleVariableB );

        programRuleA = createProgramRule( 'A', program );
        programRuleB = createProgramRule( 'B', program );
        programRuleD = createProgramRule( 'D', program );

        assignAction = createProgramRuleAction( 'I' );
        sendMessageAction = createProgramRuleAction( 'J' );
        displayText = createProgramRuleAction( 'D' );

        assignAction = setProgramRuleAction( assignAction, ProgramRuleActionType.ASSIGN, "test_variable", "2+2" );
        displayText = setProgramRuleAction( displayText, ProgramRuleActionType.DISPLAYTEXT, "test_variable", "2+2" );
        sendMessageAction = setProgramRuleAction( sendMessageAction, ProgramRuleActionType.SENDMESSAGE, null, null );

        programRuleA = setProgramRule( programRuleA, "", Sets.newHashSet( assignAction, displayText ), 1 );
        programRuleB = setProgramRule( programRuleB, "", Sets.newHashSet( sendMessageAction ), 4 );
        programRuleD = setProgramRule( programRuleD, "", Sets.newHashSet( sendMessageAction ), null );

        programRules.add( programRuleA );
        programRules.add( programRuleB );
        programRules.add( programRuleC );
        programRules.add( programRuleD );

        dataElement = createDataElement( 'D' );
        dataElement.setValueType( ValueType.TEXT );
        organisationUnit = createOrganisationUnit( 'O' );

        trackedEntityAttribute = createTrackedEntityAttribute( 'A', ValueType.TEXT );
        trackedEntityInstance = createTrackedEntityInstance( 'I', organisationUnit, trackedEntityAttribute );
        trackedEntityAttributeValue = createTrackedEntityAttributeValue( 'E', trackedEntityInstance, trackedEntityAttribute );
        trackedEntityAttributeValue.setValue( SAMPLE_VALUE_A );
        trackedEntityInstance.setTrackedEntityAttributeValues( Sets.newHashSet( trackedEntityAttributeValue ) );

        eventDataValueA = new EventDataValue();
        eventDataValueA.setDataElement( dataElement.getUid() );
        eventDataValueA.setValue( SAMPLE_VALUE_A );
        eventDataValueA.setAutoFields();

        eventDataValueB = new EventDataValue();
        eventDataValueB.setDataElement( dataElement.getUid() );
        eventDataValueB.setValue( SAMPLE_VALUE_B );
        eventDataValueB.setAutoFields();

        programInstanceB = new ProgramInstance( new Date(), new Date(), trackedEntityInstance, program );
        programInstance = new ProgramInstance( new Date(), new Date(), trackedEntityInstance, program );
        programInstance.setOrganisationUnit( organisationUnit );
        programInstance.setStatus( ProgramStatus.ACTIVE );
        programInstance.setAutoFields();
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setEntityInstance( trackedEntityInstance );

        programStageInstanceA = new ProgramStageInstance( programInstance, programStage );
        programStageInstanceB = new ProgramStageInstance( programInstance, programStage );
        programStageInstanceC = new ProgramStageInstance( programInstance, programStage );

        programStageInstanceA.setOrganisationUnit( organisationUnit );
        programStageInstanceA.setAutoFields();
        programStageInstanceA.setDueDate( new Date() );
        programStageInstanceA.setExecutionDate( new Date() );
        programStageInstanceA.setEventDataValues( Sets.newHashSet( eventDataValueA ) );

        programStageInstanceB.setOrganisationUnit( organisationUnit );
        programStageInstanceB.setAutoFields();
        programStageInstanceB.setDueDate( new Date() );
        programStageInstanceB.setExecutionDate( new Date() );
        programStageInstanceB.setEventDataValues( Sets.newHashSet( eventDataValueB ) );
    }

    private ProgramRule setProgramRule( ProgramRule programRule, String condition, Set<ProgramRuleAction> ruleActions, Integer priority )
    {
        programRule.setPriority( priority );
        programRule.setCondition( condition );
        programRule.setProgramRuleActions( ruleActions );

        return programRule;
    }

    private ProgramRuleAction setProgramRuleAction( ProgramRuleAction programRuleActionA, ProgramRuleActionType type, String content, String data )
    {
        programRuleActionA.setProgramRuleActionType( type );

        if( type == ProgramRuleActionType.ASSIGN )
        {
            programRuleActionA.setContent( content );
            programRuleActionA.setData( data );
        }

        if ( type == ProgramRuleActionType.DISPLAYTEXT )
        {
            programRuleActionA.setLocation( "feedback" );
            programRuleActionA.setContent( "content" );
            programRuleActionA.setData( "true" );
        }

        if( type == ProgramRuleActionType.SENDMESSAGE )
        {
            ProgramNotificationTemplate notificationTemplate = new ProgramNotificationTemplate();
            notificationTemplate.setUid( "uid0" );
            programRuleActionA.setTemplateUid( notificationTemplate.getUid() );
        }

        return programRuleActionA;
    }

    private ProgramRuleVariable setProgramRuleVariable( ProgramRuleVariable variable,
       ProgramRuleVariableSourceType sourceType, Program program, ProgramStage programStage, DataElement dataElement, TrackedEntityAttribute attribute )
    {
        variable.setSourceType( sourceType );
        variable.setProgram( program );
        variable.setProgramStage( programStage );
        variable.setDataElement( dataElement );
        variable.setAttribute( attribute );

        return variable;
    }
}
