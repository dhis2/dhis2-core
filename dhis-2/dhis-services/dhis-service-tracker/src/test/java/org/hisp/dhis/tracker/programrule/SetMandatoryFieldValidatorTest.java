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

package org.hisp.dhis.tracker.programrule;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerReportUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hisp.dhis.rules.models.AttributeType.DATA_ELEMENT;
import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class SetMandatoryFieldValidatorTest
    extends DhisConvenienceTest
{

    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String DATA = "2 + 2";

    private final static String EVALUATED_DATA = "4.0";

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String FIRST_EVENT_ID = "EventUid";

    private final static String SECOND_EVENT_ID = "CompletedEventUid";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private TrackerBundle bundle;

    private final static String DATA_ELEMENT_OLD_VALUE = "1.0";

    private final static String DATA_ELEMENT_NEW_VALUE = "23.0";

    private final static String TEI_ATTRIBUTE_NEW_VALUE = "24.0";

    private static ProgramStage firstProgramStage;

    private static ProgramStage secondProgramStage;

    private static DataElement dataElementA;

    private static DataElement dataElementB;

    private SetMandatoryFieldValidator implementerToTest = new SetMandatoryFieldValidator();

    @Mock
    private TrackerPreheat preheat;

    @Before
    public void setUpTest()
    {
        firstProgramStage = createProgramStage( 'A', 0 );
        firstProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );

        dataElementA = createDataElement( 'A' );
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

        bundle = new TrackerBundle();
        bundle.setEnrollments( getEnrollments() );
        bundle.setEnrollmentRuleEffects( getRuleEnrollmentEffects() );
        bundle.setEventRuleEffects( getRuleEventEffects() );
        bundle.setPreheat( preheat );
    }

    @Test
    public void testValidateOkMandatoryFieldsForEvents()
    {
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertFalse( errors.isEmpty() );

        errors.entrySet()
            .forEach( e -> assertTrue( e.getValue().isEmpty() ) );
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

        boolean isErrorMessageCorrect =
            errorMessages
                .stream()
                .allMatch(
                    e -> e.getMessage()
                        .equals( TrackerReportUtils.formatMessage( TrackerErrorCode.E1303, dataElementA.getUid() ) ) );

        assertTrue( isErrorMessageCorrect );
    }

    @Test
    public void testValidateOkMandatoryFieldsForValidEventAndNotValidEventInDifferentProgramStage()
    {
        bundle.setEvents( Lists.newArrayList( getEventWithMandatoryValueSet(),
            getEventWithMandatoryValueNOTSetInDifferentProgramStage() ) );
        Map<String, List<ProgramRuleIssue>> errors = implementerToTest.validateEvents( bundle );

        assertFalse( errors.isEmpty() );

        errors.entrySet()
            .forEach( e -> assertTrue( e.getValue().isEmpty() ) );
    }

    private Event getEventWithMandatoryValueSet()
    {
        Event event = new Event();
        event.setUid( FIRST_EVENT_ID );
        event.setEvent( FIRST_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );
        event.setDataValues( getActiveEventDataValues() );

        return event;
    }

    private Event getEventWithMandatoryValueNOTSet()
    {
        Event event = new Event();
        event.setUid( SECOND_EVENT_ID );
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( firstProgramStage.getUid() );

        return event;
    }

    private Event getEventWithMandatoryValueNOTSetInDifferentProgramStage()
    {
        Event event = new Event();
        event.setUid( SECOND_EVENT_ID );
        event.setEvent( SECOND_EVENT_ID );
        event.setStatus( EventStatus.ACTIVE );
        event.setProgramStage( secondProgramStage.getUid() );

        return event;
    }

    private Set<DataValue> getActiveEventDataValues()
    {
        DataValue dataValue = new DataValue();
        dataValue.setValue( DATA_ELEMENT_OLD_VALUE );
        dataValue.setDataElement( dataElementA.getUid() );
        return Sets.newHashSet( dataValue );
    }

    private List<Enrollment> getEnrollments()
    {
        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setUid( ACTIVE_ENROLLMENT_ID );
        activeEnrollment.setEnrollment( ACTIVE_ENROLLMENT_ID );
        activeEnrollment.setStatus( EnrollmentStatus.ACTIVE );

        Enrollment completedEnrollment = new Enrollment();
        completedEnrollment.setUid( COMPLETED_ENROLLMENT_ID );
        completedEnrollment.setEnrollment( COMPLETED_ENROLLMENT_ID );
        completedEnrollment.setStatus( EnrollmentStatus.COMPLETED );

        return Lists.newArrayList( activeEnrollment, completedEnrollment );
    }

    private Map<String, List<RuleEffect>> getRuleEventEffectsLinkedToDataElement()
    {
        Map<String, List<RuleEffect>> ruleEffectsByEvent = Maps.newHashMap();
        ruleEffectsByEvent.put( FIRST_EVENT_ID, getRuleEffectsLinkedToDataElement() );
        ruleEffectsByEvent.put( SECOND_EVENT_ID, getRuleEffectsLinkedToDataElement() );
        return ruleEffectsByEvent;
    }

    private Map<String, List<RuleEffect>> getRuleEventEffects()
    {
        Map<String, List<RuleEffect>> ruleEffectsByEvent = Maps.newHashMap();
        ruleEffectsByEvent.put( FIRST_EVENT_ID, getRuleEffects() );
        ruleEffectsByEvent.put( SECOND_EVENT_ID, getRuleEffects() );
        return ruleEffectsByEvent;
    }

    private Map<String, List<RuleEffect>> getRuleEnrollmentEffects()
    {
        Map<String, List<RuleEffect>> ruleEffectsByEnrollment = Maps.newHashMap();
        ruleEffectsByEnrollment.put( ACTIVE_ENROLLMENT_ID, getRuleEffects() );
        ruleEffectsByEnrollment.put( COMPLETED_ENROLLMENT_ID, getRuleEffects() );
        return ruleEffectsByEnrollment;
    }

    private List<RuleEffect> getRuleEffects()
    {
        RuleAction actionAssign = RuleActionSetMandatoryField.create( dataElementA.getUid(), DATA_ELEMENT );

        return Lists.newArrayList( RuleEffect.create( actionAssign ) );
    }

    private List<RuleEffect> getRuleEffectsLinkedToDataElement()
    {
        RuleAction actionShowWarning = RuleActionShowWarning
            .create( IssueType.WARNING.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowWarningOnComplete = RuleActionWarningOnCompletion
            .create( IssueType.WARNING.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowError = RuleActionShowError
            .create( ERROR.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowErrorOnCompletion = RuleActionErrorOnCompletion
            .create( ERROR.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );

        return Lists.newArrayList( RuleEffect.create( actionShowWarning, EVALUATED_DATA ),
            RuleEffect.create( actionShowWarningOnComplete, EVALUATED_DATA ),
            RuleEffect.create( actionShowError, EVALUATED_DATA ),
            RuleEffect.create( actionShowErrorOnCompletion, EVALUATED_DATA ) );
    }
}