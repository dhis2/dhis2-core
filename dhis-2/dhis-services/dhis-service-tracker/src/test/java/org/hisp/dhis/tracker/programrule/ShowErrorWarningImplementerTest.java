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
import static org.hisp.dhis.rules.models.AttributeType.UNKNOWN;
import static org.hisp.dhis.rules.models.TrackerObjectType.ENROLLMENT;
import static org.hisp.dhis.rules.models.TrackerObjectType.EVENT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.*;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.implementers.ShowErrorOnCompleteValidator;
import org.hisp.dhis.tracker.programrule.implementers.ShowErrorValidator;
import org.hisp.dhis.tracker.programrule.implementers.ShowWarningOnCompleteValidator;
import org.hisp.dhis.tracker.programrule.implementers.ShowWarningValidator;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith( MockitoJUnitRunner.class )
public class ShowErrorWarningImplementerTest
    extends DhisConvenienceTest
{
    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String DATA = "2 + 2";

    private final static String EVALUATED_DATA = "4.0";

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private final static String ACTIVE_EVENT_ID = "EventUid";

    private final static String COMPLETED_EVENT_ID = "CompletedEventUid";

    private final static String PROGRAM_STAGE_ID = "ProgramStageId";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

    private ShowWarningOnCompleteValidator warningOnCompleteImplementer = new ShowWarningOnCompleteValidator();

    private ShowErrorOnCompleteValidator errorOnCompleteImplementer = new ShowErrorOnCompleteValidator();

    private ShowErrorValidator errorImplementer = new ShowErrorValidator();

    private ShowWarningValidator warningImplementer = new ShowWarningValidator();

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ProgramStage programStage;

    private ProgramStage anotherProgramStage;

    @Before
    public void setUpTest()
    {
        bundle = TrackerBundle.builder().build();
        bundle.setEvents( getEvents() );
        bundle.setEnrollments( getEnrollments() );
        bundle.setRuleEffects( getRuleEventAndEnrollmentEffects() );
        bundle.setPreheat( preheat );

        programStage = createProgramStage( 'A', 0 );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( programStage,
            dataElementA, 0 );
        programStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );

        anotherProgramStage = createProgramStage( 'B', 0 );
        anotherProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setUid( ANOTHER_DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( anotherProgramStage,
            dataElementB, 0 );
        anotherProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );

        when( preheat.get( ProgramStage.class, PROGRAM_STAGE_ID ) ).thenReturn( programStage );
    }

    @Test
    public void testValidateShowErrorRuleActionForEvents()
    {
        Map<String, List<ProgramRuleIssue>> errors = errorImplementer.validateEvents( bundle );

        assertErrors( errors, 2 );
    }

    @Test
    public void testValidateShowErrorRuleActionForEventsWithValidationStrategyOnComplete()
    {
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        bundle.setRuleEffects( getRuleEventEffectsLinkedToDataElement() );
        Map<String, List<ProgramRuleIssue>> errors = errorImplementer.validateEvents( bundle );

        assertErrors( errors, 1 );
        assertErrorsWithDataElement( errors );
    }

    @Test
    public void testValidateShowErrorForEventsInDifferentProgramStages()
    {
        bundle.setRuleEffects( getRuleEventEffectsLinkedTo2DataElementsIn2DifferentProgramStages() );
        Map<String, List<ProgramRuleIssue>> errors = errorImplementer.validateEvents( bundle );

        assertErrors( errors, 1 );
        assertErrorsWithDataElement( errors );
    }

    @Test
    public void testValidateShowErrorRuleActionForEnrollment()
    {
        Map<String, List<ProgramRuleIssue>> errors = errorImplementer.validateEnrollments( bundle );

        assertErrors( errors, 2 );
    }

    @Test
    public void testValidateShowWarningRuleActionForEvents()
    {
        Map<String, List<ProgramRuleIssue>> warnings = warningImplementer.validateEvents( bundle );

        assertWarnings( warnings, 2 );
    }

    @Test
    public void testValidateShowWarningRuleActionForEnrollment()
    {
        Map<String, List<ProgramRuleIssue>> warnings = warningImplementer.validateEnrollments( bundle );

        assertWarnings( warnings, 2 );
    }

    @Test
    public void testValidateShowErrorOnCompleteRuleActionForEvents()
    {
        Map<String, List<ProgramRuleIssue>> errors = errorOnCompleteImplementer.validateEvents( bundle );

        assertErrors( errors, 1 );
    }

    @Test
    public void testValidateShowErrorOnCompleteRuleActionForEnrollment()
    {
        Map<String, List<ProgramRuleIssue>> errors = errorOnCompleteImplementer.validateEnrollments( bundle );

        assertErrors( errors, 1 );
    }

    @Test
    public void testValidateShowWarningOnCompleteRuleActionForEvents()
    {
        Map<String, List<ProgramRuleIssue>> warnings = warningOnCompleteImplementer.validateEvents( bundle );

        assertWarnings( warnings, 1 );
    }

    @Test
    public void testValidateShowWarningOnCompleteRuleActionForEnrollment()
    {
        Map<String, List<ProgramRuleIssue>> warnings = warningOnCompleteImplementer.validateEnrollments( bundle );

        assertWarnings( warnings, 1 );
    }

    public void assertErrors( Map<String, List<ProgramRuleIssue>> errors, int numberOfErrors )
    {
        assertIssues( errors, numberOfErrors, IssueType.ERROR );
    }

    public void assertWarnings( Map<String, List<ProgramRuleIssue>> warnings, int numberOfWarnings )
    {
        assertIssues( warnings, numberOfWarnings, IssueType.WARNING );
    }

    private void assertIssues( Map<String, List<ProgramRuleIssue>> errors, int numberOfErrors, IssueType issueType )
    {
        assertFalse( errors.isEmpty() );

        assertEquals( numberOfErrors, errors.size() );

        errors.forEach( ( key, value ) -> assertEquals( 1, value.size() ) );

        errors
            .values()
            .stream()
            .flatMap( Collection::stream )
            .forEach( e -> {
                assertEquals( "", e.getRuleUid() );
                assertEquals( issueType, e.getIssueType() );
                assertEquals( TrackerErrorCode.E1300, e.getIssueCode() );
                assertTrue( e.getArgs().get( 0 ).contains( issueType.name() + CONTENT + " " + EVALUATED_DATA ) );
            } );
    }

    public void assertErrorsWithDataElement( Map<String, List<ProgramRuleIssue>> errors )
    {
        errors
            .values()
            .stream()
            .flatMap( Collection::stream )
            .forEach( e -> {
                assertEquals( "", e.getRuleUid() );
                assertEquals( IssueType.ERROR, e.getIssueType() );
                assertEquals( TrackerErrorCode.E1300, e.getIssueCode() );
                assertEquals( IssueType.ERROR.name() + CONTENT + " " + EVALUATED_DATA + " (" + DATA_ELEMENT_ID + ")",
                    e.getArgs().get( 0 ) );
            } );
    }

    private List<Event> getEvents()
    {
        Event activeEvent = new Event();
        activeEvent.setEvent( ACTIVE_EVENT_ID );
        activeEvent.setStatus( EventStatus.ACTIVE );
        activeEvent.setProgramStage( PROGRAM_STAGE_ID );

        Event completedEvent = new Event();
        completedEvent.setEvent( COMPLETED_EVENT_ID );
        completedEvent.setStatus( EventStatus.COMPLETED );
        completedEvent.setProgramStage( PROGRAM_STAGE_ID );

        return Lists.newArrayList( activeEvent, completedEvent );
    }

    private List<Enrollment> getEnrollments()
    {
        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setEnrollment( ACTIVE_ENROLLMENT_ID );
        activeEnrollment.setStatus( EnrollmentStatus.ACTIVE );

        Enrollment completedEnrollment = new Enrollment();
        completedEnrollment.setEnrollment( COMPLETED_ENROLLMENT_ID );
        completedEnrollment.setStatus( EnrollmentStatus.COMPLETED );

        return Lists.newArrayList( activeEnrollment, completedEnrollment );
    }

    private List<RuleEffects> getRuleEventEffectsLinkedToDataElement()
    {
        List<RuleEffects> ruleEffectsByEvent = Lists.newArrayList();
        ruleEffectsByEvent
            .add( new RuleEffects( EVENT, ACTIVE_EVENT_ID, getRuleEffectsLinkedToDataElement() ) );
        ruleEffectsByEvent
            .add( new RuleEffects( EVENT, COMPLETED_EVENT_ID, getRuleEffectsLinkedToDataElement() ) );
        return ruleEffectsByEvent;
    }

    private List<RuleEffects> getRuleEventEffectsLinkedTo2DataElementsIn2DifferentProgramStages()
    {
        List<RuleEffects> ruleEffectsByEvent = Lists.newArrayList();
        ruleEffectsByEvent
            .add( new RuleEffects( EVENT, ACTIVE_EVENT_ID, getRuleEffectsLinkedToDataElement() ) );
        ruleEffectsByEvent.add(
            new RuleEffects( EVENT, COMPLETED_EVENT_ID, getRuleEffectsLinkedToDataAnotherElement() ) );
        return ruleEffectsByEvent;
    }

    private List<RuleEffects> getRuleEventAndEnrollmentEffects()
    {
        List<RuleEffects> ruleEffectsByEvent = Lists.newArrayList();
        ruleEffectsByEvent.add( new RuleEffects( EVENT, ACTIVE_EVENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent.add( new RuleEffects( EVENT, COMPLETED_EVENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent.add( new RuleEffects( ENROLLMENT, ACTIVE_ENROLLMENT_ID, getRuleEffects() ) );
        ruleEffectsByEvent
            .add( new RuleEffects( ENROLLMENT, COMPLETED_ENROLLMENT_ID, getRuleEffects() ) );
        return ruleEffectsByEvent;
    }

    private List<RuleEffect> getRuleEffects()
    {
        RuleAction actionShowWarning = RuleActionShowWarning
            .create( IssueType.WARNING.name() + CONTENT, DATA, "", UNKNOWN );
        RuleAction actionShowWarningOnComplete = RuleActionWarningOnCompletion
            .create( IssueType.WARNING.name() + CONTENT, DATA, "", UNKNOWN );
        RuleAction actionShowError = RuleActionShowError
            .create( IssueType.ERROR.name() + CONTENT, DATA, "", UNKNOWN );
        RuleAction actionShowErrorOnCompletion = RuleActionErrorOnCompletion
            .create( IssueType.ERROR.name() + CONTENT, DATA, "", UNKNOWN );

        return Lists.newArrayList( RuleEffect.create( "", actionShowWarning, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowWarningOnComplete, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowError, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowErrorOnCompletion, EVALUATED_DATA ) );
    }

    private List<RuleEffect> getRuleEffectsLinkedToDataElement()
    {
        RuleAction actionShowWarning = RuleActionShowWarning
            .create( IssueType.WARNING.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowWarningOnComplete = RuleActionWarningOnCompletion
            .create( IssueType.WARNING.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowError = RuleActionShowError
            .create( IssueType.ERROR.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowErrorOnCompletion = RuleActionErrorOnCompletion
            .create( IssueType.ERROR.name() + CONTENT, DATA, DATA_ELEMENT_ID, DATA_ELEMENT );

        return Lists.newArrayList( RuleEffect.create( "", actionShowWarning, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowWarningOnComplete, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowError, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowErrorOnCompletion, EVALUATED_DATA ) );
    }

    private List<RuleEffect> getRuleEffectsLinkedToDataAnotherElement()
    {
        RuleAction actionShowWarning = RuleActionShowWarning
            .create( IssueType.WARNING.name() + CONTENT, DATA, ANOTHER_DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowWarningOnComplete = RuleActionWarningOnCompletion
            .create( IssueType.WARNING.name() + CONTENT, DATA, ANOTHER_DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowError = RuleActionShowError
            .create( IssueType.ERROR.name() + CONTENT, DATA, ANOTHER_DATA_ELEMENT_ID, DATA_ELEMENT );
        RuleAction actionShowErrorOnCompletion = RuleActionErrorOnCompletion
            .create( IssueType.ERROR.name() + CONTENT, DATA, ANOTHER_DATA_ELEMENT_ID, DATA_ELEMENT );

        return Lists.newArrayList( RuleEffect.create( "", actionShowWarning, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowWarningOnComplete, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowError, EVALUATED_DATA ),
            RuleEffect.create( "", actionShowErrorOnCompletion, EVALUATED_DATA ) );
    }
}