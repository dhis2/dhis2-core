/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.rules.models.TrackerObjectType.ENROLLMENT;
import static org.hisp.dhis.rules.models.TrackerObjectType.EVENT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.implementers.SetMandatoryFieldValidator;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith( MockitoJUnitRunner.class )
public class SetMandatoryFieldValidatorTest
    extends DhisConvenienceTest
{

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String FIRST_EVENT_ID = "EventUid";

    private final static String SECOND_EVENT_ID = "CompletedEventUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ATTRIBUTE_ID = "AttributeId";

    private final static String TEI_ID = "TeiId";

    private final static String DATA_ELEMENT_VALUE = "1.0";

    private final static String ATTRIBUTE_VALUE = "23.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private SetMandatoryFieldValidator implementerToTest = new SetMandatoryFieldValidator();

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Before
    public void setUpTest()
    {
        firstProgramStage = createProgramStage( 'A', 0 );
        firstProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( firstProgramStage,
            dataElementA, 0 );
        firstProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );

        secondProgramStage = createProgramStage( 'B', 0 );
        secondProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        dataElementB = createDataElement( 'B' );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( secondProgramStage,
            dataElementB, 0 );
        secondProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        when( preheat.get( ProgramStage.class, firstProgramStage.getUid() ) ).thenReturn( firstProgramStage );
        when( preheat.get( ProgramStage.class, secondProgramStage.getUid() ) ).thenReturn( secondProgramStage );

        bundle = TrackerBundle.builder().build();
        bundle.setRuleEffects( getRuleEventAndEnrollmentEffects() );
        bundle.setPreheat( preheat );
    }

    @Test
    public void testValidateOkMandatoryFieldsForEvents()
    {
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testValidateWithErrorMandatoryFieldsForEvents()
    {
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet(), getEventWithMandatoryValueNOTSet() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertFalse( errors.isEmpty() );

        List<ProgramRuleIssue> errorMessages = errors.values()
            .stream()
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        assertFalse( errorMessages.isEmpty() );

        errorMessages
            .forEach(
                e -> {
                    assertEquals( "RULE_DATA_VALUE", e.getRuleUid() );
                    assertEquals( TrackerErrorCode.E1301, e.getIssueCode() );
                    assertEquals( IssueType.ERROR, e.getIssueType() );
                    assertEquals( Lists.newArrayList( dataElementA.getUid() ), e.getArgs() );
                } );
    }

    @Test
    public void testValidateOkMandatoryFieldsForValidEventAndNotValidEventInDifferentProgramStage()
    {
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testValidateOkMandatoryFieldsForEnrollment()
    {
        bundle.setEnrollments( Lists.newArrayList( getEnrollmentWithMandatoryAttributeSet() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertTrue( errors.isEmpty() );
    }

    @Test
    public void testValidateWithErrorMandatoryFieldsForEnrollments()
    {
        bundle.setEnrollments(
            Lists.newArrayList( getEnrollmentWithMandatoryAttributeSet(),
                getEnrollmentWithMandatoryAttributeNOTSet() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEnrollments( bundle );

        assertFalse( errors.isEmpty() );

        List<ProgramRuleIssue> errorMessages = errors.values()
            .stream()
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );

        assertFalse( errorMessages.isEmpty() );

        errorMessages
            .forEach(
                e -> {
                    assertEquals( "RULE_ATTRIBUTE", e.getRuleUid() );
                    assertEquals( TrackerErrorCode.E1306, e.getIssueCode() );
                    assertEquals( IssueType.ERROR, e.getIssueType() );
                    assertEquals( Lists.newArrayList( ATTRIBUTE_ID ), e.getArgs() );
                } );
    }

    private Event getEventWithMandatoryValueSet()
    {
        Event event = new Event();
        event.setEvent( FIRST_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );
        event.setDataValues( getActiveEventDataValues() );

        return event;
    }

    private Event getEventWithMandatoryValueNOTSet()
    {
        Event event = new Event();
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );

        return event;
    }

    private Event getEventWithMandatoryValueNOTSetInDifferentProgramStage()
    {
        Event event = new Event();
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( secondProgramStage.getUid() );

        return event;
    }

    private Set<DataValue> getActiveEventDataValues()
    {
        DataValue dataValue = new DataValue();
        dataValue.setValue( DATA_ELEMENT_VALUE );
        dataValue.setDataElement( DATA_ELEMENT_ID );
        return Sets.newHashSet( dataValue );
    }

    private Enrollment getEnrollmentWithMandatoryAttributeSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( ACTIVE_ENROLLMENT_ID );
        enrollment.setTrackedEntity( TEI_ID );
        enrollment.setStatus( EnrollmentStatus.ACTIVE );
        enrollment.setAttributes( getAttributes() );

        return enrollment;
    }

    private Enrollment getEnrollmentWithMandatoryAttributeNOTSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( COMPLETED_ENROLLMENT_ID );
        enrollment.setTrackedEntity( TEI_ID );
        enrollment.setStatus( EnrollmentStatus.COMPLETED );

        return enrollment;
    }

    private List<Attribute> getAttributes()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( ATTRIBUTE_ID );
        attribute.setValue( ATTRIBUTE_VALUE );
        return Lists.newArrayList( attribute );
    }

    private List<RuleEffects> getRuleEventAndEnrollmentEffects()
    {
        List<RuleEffects> ruleEffectsByEvent = Lists.newArrayList();
        ruleEffectsByEvent.add( new RuleEffects( EVENT, FIRST_EVENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent.add( new RuleEffects( EVENT, SECOND_EVENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent.add( new RuleEffects( ENROLLMENT, ACTIVE_ENROLLMENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent
            .add( new RuleEffects( ENROLLMENT, COMPLETED_ENROLLMENT_ID, getRuleEffects() ) );
        return ruleEffectsByEvent;
    }

    private List<RuleEffect> getRuleEffects()
    {
        RuleAction ruleActionSetMandatoryDataValue = RuleActionSetMandatoryField
            .create( DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction ruleActionSetMandatoryAttribute = RuleActionSetMandatoryField
            .create( ATTRIBUTE_ID, TRACKED_ENTITY_ATTRIBUTE );

        return Lists.newArrayList( RuleEffect.create( "RULE_ATTRIBUTE", ruleActionSetMandatoryAttribute ),
            RuleEffect.create( "RULE_DATA_VALUE", ruleActionSetMandatoryDataValue ) );
    }
}