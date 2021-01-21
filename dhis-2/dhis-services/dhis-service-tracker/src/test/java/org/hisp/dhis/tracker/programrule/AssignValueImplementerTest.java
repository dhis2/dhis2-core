package org.hisp.dhis.tracker.programrule;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.implementers.AssignValueImplementer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.rules.models.AttributeType.TRACKED_ENTITY_ATTRIBUTE;
import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class AssignValueImplementerTest
    extends DhisConvenienceTest
{
    private final static String FIRST_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String SECOND_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String FIRST_EVENT_ID = "EventUid";

    private final static String SECOND_EVENT_ID = "CompletedEventUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

    private final static String ATTRIBUTE_ID = "AttributeId";

    private final static String DATA_ELEMENT_OLD_VALUE = "1";

    private final static String DATA_ELEMENT_NEW_VALUE_PAYLOAD = "23";

    private final static String DATA_ELEMENT_NEW_VALUE = "23.0";

    private final static String TEI_ATTRIBUTE_OLD_VALUE = "10.0";

    private final static String TEI_ATTRIBUTE_NEW_VALUE = "24.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private static TrackedEntityAttribute attributeA;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private SystemSettingManager systemSettingManager;

    @InjectMocks
    private AssignValueImplementer implementerToTest;

    @Before
    public void setUpTest()
    {
        firstProgramStage = createProgramStage( 'A', 0 );
        firstProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        attributeA = createTrackedEntityAttribute( 'A' );
        attributeA.setUid( ATTRIBUTE_ID );
        attributeA.setValueType( ValueType.NUMBER );

        dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( firstProgramStage,
            dataElementA, 0 );
        firstProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );

        secondProgramStage = createProgramStage( 'B', 0 );
        secondProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        dataElementB = createDataElement( 'B' );
        dataElementB.setUid( ANOTHER_DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( secondProgramStage,
            dataElementB, 0 );
        secondProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        when( preheat.get( ProgramStage.class, firstProgramStage.getUid() ) ).thenReturn( firstProgramStage );
        when( preheat.get( ProgramStage.class, secondProgramStage.getUid() ) ).thenReturn( secondProgramStage );
        when( preheat.get( DataElement.class, dataElementA.getUid() ) ).thenReturn( dataElementA );
        when( preheat.get( TrackedEntityAttribute.class, attributeA.getUid() ) ).thenReturn( attributeA );

        bundle = new TrackerBundle();
        bundle.setPreheat( preheat );

        when( systemSettingManager.getSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.FALSE );
    }

    @Test
    public void testAssignDataElementValueForEventsWhenDataElementIsEmpty()
    {
        List<Event> events = Lists.newArrayList( getEventWithDataValueNOTSet() );
        bundle.setEvents( events );
        bundle.setEventRuleEffects( getRuleEventEffects( events ) );
        Map<String, List<ProgramRuleIssue>> eventIssues = implementerToTest.validateEvents( bundle );

        Event event = bundle.getEvents().stream().filter( e -> e.getEvent().equals( SECOND_EVENT_ID ) )
            .findAny().get();
        Optional<DataValue> newDataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementA.getUid() ) ).findAny();

        assertTrue( newDataValue.isPresent() );
        assertEquals( DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue() );
        assertEquals( 1, eventIssues.size() );
        assertEquals( 1, eventIssues.get( SECOND_EVENT_ID ).size() );
        assertEquals( WARNING, eventIssues.get( SECOND_EVENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignDataElementValueForEventsWhenDataElementIsEmptyAndFromDifferentProgramStage()
    {
        List<Event> events = Lists.newArrayList( getEventWithDataValueNOTSetInDifferentProgramStage() );
        bundle.setEvents( events );
        bundle.setEventRuleEffects( getRuleEventEffects( events ) );
        Map<String, List<ProgramRuleIssue>> eventIssues = implementerToTest.validateEvents( bundle );

        Event event = bundle.getEvents().stream().filter( e -> e.getEvent().equals( SECOND_EVENT_ID ) )
            .findAny().get();
        Optional<DataValue> newDataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementA.getUid() ) ).findAny();

        assertTrue( !newDataValue.isPresent() );
        assertTrue( eventIssues.isEmpty() );
    }

    @Test
    public void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresent()
    {
        List<Event> events = Lists.newArrayList( getEventWithDataValueSet() );
        bundle.setEvents( events );
        bundle.setEventRuleEffects( getRuleEventEffects( events ) );
        Map<String, List<ProgramRuleIssue>> eventIssues = implementerToTest.validateEvents( bundle );

        Event event = bundle.getEvents().stream().filter( e -> e.getEvent().equals( FIRST_EVENT_ID ) )
            .findAny().get();
        Optional<DataValue> newDataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementA.getUid() ) ).findAny();

        assertTrue( newDataValue.isPresent() );
        assertEquals( DATA_ELEMENT_OLD_VALUE, newDataValue.get().getValue() );
        assertEquals( 1, eventIssues.size() );
        assertEquals( 1, eventIssues.get( FIRST_EVENT_ID ).size() );
        assertEquals( ERROR, eventIssues.get( FIRST_EVENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresentAndHasSameValue()
    {
        List<Event> events = Lists.newArrayList( getEventWithDataValueSetSameValue() );
        bundle.setEvents( events );
        bundle.setEventRuleEffects( getRuleEventEffects( events ) );
        Map<String, List<ProgramRuleIssue>> eventIssues = implementerToTest.validateEvents( bundle );

        Event event = bundle.getEvents().stream().filter( e -> e.getEvent().equals( FIRST_EVENT_ID ) )
            .findAny().get();
        Optional<DataValue> newDataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementA.getUid() ) ).findAny();

        assertTrue( newDataValue.isPresent() );
        assertEquals( DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue() );
        assertEquals( 1, eventIssues.size() );
        assertEquals( 1, eventIssues.get( FIRST_EVENT_ID ).size() );
        assertEquals( WARNING, eventIssues.get( FIRST_EVENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignDataElementValueForEventsWhenDataElementIsAlreadyPresentAndSystemSettingToOverwriteIsTrue()
    {
        List<Event> events = Lists.newArrayList( getEventWithDataValueSet() );
        bundle.setEvents( events );
        bundle.setEventRuleEffects( getRuleEventEffects( events ) );
        when( systemSettingManager.getSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.TRUE );
        Map<String, List<ProgramRuleIssue>> eventIssues = implementerToTest.validateEvents( bundle );

        Event event = bundle.getEvents().stream().filter( e -> e.getEvent().equals( FIRST_EVENT_ID ) )
            .findAny().get();
        Optional<DataValue> newDataValue = event.getDataValues().stream()
            .filter( dv -> dv.getDataElement().equals( dataElementA.getUid() ) ).findAny();

        assertTrue( newDataValue.isPresent() );
        assertEquals( DATA_ELEMENT_NEW_VALUE, newDataValue.get().getValue() );
        assertEquals( 1, eventIssues.size() );
        assertEquals( 1, eventIssues.get( FIRST_EVENT_ID ).size() );
        assertEquals( WARNING, eventIssues.get( FIRST_EVENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignAttributeValueForEnrollmentsWhenAttributeIsEmpty()
    {
        List<Enrollment> enrollments = Lists.newArrayList( getEnrollmentWithAttributeNOTSet() );
        bundle.setEnrollments( enrollments );
        bundle.setEnrollmentRuleEffects( getRuleEnrollmentEffects( enrollments ) );
        Map<String, List<ProgramRuleIssue>> enrollmentIssues = implementerToTest.validateEnrollments( bundle );

        Enrollment enrollment = bundle.getEnrollments().stream().filter( e -> e.getEnrollment().equals(
            SECOND_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( ATTRIBUTE_ID ) ).findAny();

        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertEquals( 1, enrollmentIssues.size() );
        assertEquals( 1, enrollmentIssues.get( SECOND_ENROLLMENT_ID ).size() );
        assertEquals( WARNING, enrollmentIssues.get( SECOND_ENROLLMENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresent()
    {
        List<Enrollment> enrollments = Lists.newArrayList( getEnrollmentWithAttributeSet() );
        bundle.setEnrollments( enrollments );
        bundle.setEnrollmentRuleEffects( getRuleEnrollmentEffects( enrollments ) );
        Map<String, List<ProgramRuleIssue>> enrollmentIssues = implementerToTest.validateEnrollments( bundle );

        Enrollment enrollment = bundle.getEnrollments().stream().filter( e -> e.getEnrollment().equals(
            FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( ATTRIBUTE_ID ) ).findAny();

        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_OLD_VALUE, attribute.get().getValue() );
        assertEquals( 1, enrollmentIssues.size() );
        assertEquals( 1, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).size() );
        assertEquals( ERROR, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndHasTheSameValue()
    {
        List<Enrollment> enrollments = Lists.newArrayList( getEnrollmentWithAttributeSetSameValue() );
        bundle.setEnrollments( enrollments );
        bundle.setEnrollmentRuleEffects( getRuleEnrollmentEffects( enrollments ) );
        Map<String, List<ProgramRuleIssue>> enrollmentIssues = implementerToTest.validateEnrollments( bundle );

        Enrollment enrollment = bundle.getEnrollments().stream().filter( e -> e.getEnrollment().equals(
            FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( ATTRIBUTE_ID ) ).findAny();

        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertEquals( 1, enrollmentIssues.size() );
        assertEquals( 1, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).size() );
        assertEquals( WARNING, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).get( 0 ).getIssueType() );
    }

    @Test
    public void testAssignAttributeValueForEnrollmentsWhenAttributeIsAlreadyPresentAndSystemSettingToOverwriteIsTrue()
    {
        List<Enrollment> enrollments = Lists.newArrayList( getEnrollmentWithAttributeSet() );
        bundle.setEnrollments( enrollments );
        bundle.setEnrollmentRuleEffects( getRuleEnrollmentEffects( enrollments ) );
        when( systemSettingManager.getSystemSetting( SettingKey.RULE_ENGINE_ASSIGN_OVERWRITE ) )
            .thenReturn( Boolean.TRUE );
        Map<String, List<ProgramRuleIssue>> enrollmentIssues = implementerToTest.validateEnrollments( bundle );

        Enrollment enrollment = bundle.getEnrollments().stream().filter( e -> e.getEnrollment().equals(
            FIRST_ENROLLMENT_ID ) ).findAny().get();
        Optional<Attribute> attribute = enrollment.getAttributes().stream()
            .filter( at -> at.getAttribute().equals( ATTRIBUTE_ID ) ).findAny();

        assertTrue( attribute.isPresent() );
        assertEquals( TEI_ATTRIBUTE_NEW_VALUE, attribute.get().getValue() );
        assertEquals( 1, enrollmentIssues.size() );
        assertEquals( 1, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).size() );
        assertEquals( WARNING, enrollmentIssues.get( FIRST_ENROLLMENT_ID ).get( 0 ).getIssueType() );
    }

    private Event getEventWithDataValueSet()
    {
        Event event = new Event();
        event.setUid( FIRST_EVENT_ID );
        event.setEvent( FIRST_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );
        event.setDataValues( getEventDataValues() );

        return event;
    }

    private Event getEventWithDataValueSetSameValue()
    {
        Event event = new Event();
        event.setUid( FIRST_EVENT_ID );
        event.setEvent( FIRST_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );
        event.setDataValues( getEventDataValuesSameValue() );

        return event;
    }

    private Event getEventWithDataValueNOTSet()
    {
        Event event = new Event();
        event.setUid( SECOND_EVENT_ID );
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );

        return event;
    }

    private Event getEventWithDataValueNOTSetInDifferentProgramStage()
    {
        Event event = new Event();
        event.setUid( SECOND_EVENT_ID );
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( secondProgramStage.getUid() );

        return event;
    }

    private Set<DataValue> getEventDataValues()
    {
        DataValue dataValue = new DataValue();
        dataValue.setValue( DATA_ELEMENT_OLD_VALUE );
        dataValue.setDataElement( DATA_ELEMENT_ID );
        return Sets.newHashSet( dataValue );
    }

    private Set<DataValue> getEventDataValuesSameValue()
    {
        DataValue dataValue = new DataValue();
        dataValue.setValue( DATA_ELEMENT_NEW_VALUE_PAYLOAD );
        dataValue.setDataElement( DATA_ELEMENT_ID );
        return Sets.newHashSet( dataValue );
    }

    private Enrollment getEnrollmentWithAttributeSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( FIRST_ENROLLMENT_ID );
        enrollment.setEnrollment( FIRST_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.ACTIVE );
        enrollment.setAttributes( getAttributes() );

        return enrollment;
    }

    private Enrollment getEnrollmentWithAttributeSetSameValue()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( FIRST_ENROLLMENT_ID );
        enrollment.setEnrollment( FIRST_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.ACTIVE );
        enrollment.setAttributes( getAttributesSameValue() );

        return enrollment;
    }

    private Enrollment getEnrollmentWithAttributeNOTSet()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( SECOND_ENROLLMENT_ID );
        enrollment.setEnrollment( SECOND_ENROLLMENT_ID );
        enrollment.setStatus( EnrollmentStatus.COMPLETED );

        return enrollment;
    }

    private List<Attribute> getAttributes()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( ATTRIBUTE_ID );
        attribute.setValue( TEI_ATTRIBUTE_OLD_VALUE );
        return Lists.newArrayList( attribute );
    }

    private List<Attribute> getAttributesSameValue()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( ATTRIBUTE_ID );
        attribute.setValue( TEI_ATTRIBUTE_NEW_VALUE );
        return Lists.newArrayList( attribute );
    }

    private Map<String, List<RuleEffect>> getRuleEventEffects( List<Event> events )
    {
        Map<String, List<RuleEffect>> ruleEffectsByEvent = Maps.newHashMap();

        for ( Event event : events )
        {
            ruleEffectsByEvent.put( event.getEvent(), getRuleEventEffects() );

        }
        return ruleEffectsByEvent;
    }

    private Map<String, List<RuleEffect>> getRuleEnrollmentEffects( List<Enrollment> enrollments )
    {
        Map<String, List<RuleEffect>> ruleEffectsByEnrollment = Maps.newHashMap();

        for ( Enrollment enrollment : enrollments )
        {
            ruleEffectsByEnrollment.put( enrollment.getEnrollment(), getRuleEnrollmentEffects() );

        }
        return ruleEffectsByEnrollment;
    }

    private List<RuleEffect> getRuleEventEffects()
    {
        RuleAction actionAssign = RuleActionAssign
            .create( null, DATA_ELEMENT_NEW_VALUE, dataElementA.getUid(), DATA_ELEMENT );

        return Lists.newArrayList( RuleEffect.create( actionAssign, DATA_ELEMENT_NEW_VALUE ) );
    }

    private List<RuleEffect> getRuleEnrollmentEffects()
    {
        RuleAction actionAssign = RuleActionAssign
            .create( null, TEI_ATTRIBUTE_NEW_VALUE, ATTRIBUTE_ID, TRACKED_ENTITY_ATTRIBUTE );

        return Lists.newArrayList( RuleEffect.create( actionAssign, TEI_ATTRIBUTE_NEW_VALUE ) );
    }
}