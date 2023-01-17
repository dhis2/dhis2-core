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
package org.hisp.dhis.tracker.programrule.implementers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.EventActionRule;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ShowErrorWarningExecutorTest extends DhisConvenienceTest
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

    @BeforeEach
    void setUpTest()
    {
        bundle = TrackerBundle.builder().build();
        bundle.setEvents( getEvents() );
        bundle.setPreheat( preheat );
        programStage = createProgramStage( 'A', 0 );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( programStage, dataElementA,
            0 );
        programStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementA ) );
        anotherProgramStage = createProgramStage( 'B', 0 );
        anotherProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setUid( ANOTHER_DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( anotherProgramStage,
            dataElementB, 0 );
        anotherProgramStage.setProgramStageDataElements( Sets.newHashSet( programStageDataElementB ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) ).thenReturn( programStage );
    }

    @Test
    void testValidateShowErrorRuleActionForEvents()
    {
        List<ProgramRuleIssue> errors = errorImplementer.validateEvent( bundle, getErrorRuleEffects(), activeEvent() );
        assertErrors( errors, 1 );

        errors = errorImplementer.validateEvent( bundle, getErrorRuleEffects(), completedEvent() );
        assertErrors( errors, 1 );
    }

    @Test
    void testValidateShowErrorRuleActionForEventsWithValidationStrategyOnComplete()
    {
        programStage.setValidationStrategy( ValidationStrategy.ON_COMPLETE );
        List<ProgramRuleIssue> errors = errorImplementer.validateEvent( bundle,
            getErrorRuleEffectsLinkedToDataElement(),
            completedEvent() );
        assertErrors( errors, 1 );
        assertErrorsWithDataElement( errors );
    }

    @Test
    void testValidateShowWarningRuleActionForEvents()
    {
        List<ProgramRuleIssue> warnings = warningImplementer.validateEvent( bundle, getWarningRuleEffects(),
            activeEvent() );
        assertWarnings( warnings, 1 );

        warnings = warningImplementer.validateEvent( bundle, getWarningRuleEffects(), completedEvent() );
        assertWarnings( warnings, 1 );
    }

    @Test
    void testValidateShowErrorOnCompleteRuleActionForEvents()
    {
        List<ProgramRuleIssue> errors = errorOnCompleteImplementer.validateEvent( bundle,
            getErrorOnCompleteRuleEffects(),
            activeEvent() );
        assertTrue( errors.isEmpty() );

        errors = errorOnCompleteImplementer.validateEvent( bundle, getErrorOnCompleteRuleEffects(), completedEvent() );
        assertErrors( errors, 1 );
    }

    @Test
    void testValidateShowWarningOnCompleteRuleActionForEvents()
    {
        List<ProgramRuleIssue> warnings = warningOnCompleteImplementer.validateEvent( bundle,
            getWarningOnCompleteRuleEffects(),
            activeEvent() );
        assertTrue( warnings.isEmpty() );

        warnings = warningOnCompleteImplementer.validateEvent( bundle, getWarningOnCompleteRuleEffects(),
            completedEvent() );
        assertWarnings( warnings, 1 );
    }

    public void assertErrors( List<ProgramRuleIssue> errors, int numberOfErrors )
    {
        assertIssues( errors, numberOfErrors, IssueType.ERROR );
    }

    public void assertWarnings( List<ProgramRuleIssue> warnings, int numberOfWarnings )
    {
        assertIssues( warnings, numberOfWarnings, IssueType.WARNING );
    }

    private void assertIssues( List<ProgramRuleIssue> errors, int numberOfErrors, IssueType issueType )
    {
        assertFalse( errors.isEmpty() );
        assertEquals( numberOfErrors, errors.size() );
        errors.forEach( e -> {
            assertEquals( "", e.getRuleUid() );
            assertEquals( issueType, e.getIssueType() );
            assertEquals( ValidationCode.E1300, e.getIssueCode() );
            assertTrue( e.getArgs().get( 0 ).contains( issueType.name() + CONTENT + " " + EVALUATED_DATA ) );
        } );
    }

    public void assertErrorsWithDataElement( List<ProgramRuleIssue> errors )
    {
        errors.forEach( e -> {
            assertEquals( "", e.getRuleUid() );
            assertEquals( IssueType.ERROR, e.getIssueType() );
            assertEquals( ValidationCode.E1300, e.getIssueCode() );
            assertEquals( IssueType.ERROR.name() + CONTENT + " " + EVALUATED_DATA + " (" + DATA_ELEMENT_ID + ")",
                e.getArgs().get( 0 ) );
        } );
    }

    private List<Event> getEvents()
    {
        return Lists.newArrayList( activeEvent(), completedEvent() );
    }

    private Event activeEvent()
    {
        Event activeEvent = new Event();
        activeEvent.setEvent( ACTIVE_EVENT_ID );
        activeEvent.setStatus( EventStatus.ACTIVE );
        activeEvent.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) );
        return activeEvent;
    }

    private Event completedEvent()
    {
        Event completedEvent = new Event();
        completedEvent.setEvent( COMPLETED_EVENT_ID );
        completedEvent.setStatus( EventStatus.COMPLETED );
        completedEvent.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) );
        return completedEvent;
    }

    private List<EventActionRule> getErrorRuleEffects()
    {
        return Lists.newArrayList(
            new EventActionRule( "", EVALUATED_DATA, null, IssueType.ERROR.name() + CONTENT, RuleActionType.ERROR,
                Collections.emptySet() ) );
    }

    private List<EventActionRule> getErrorRuleEffectsLinkedToDataElement()
    {
        return Lists.newArrayList(
            new EventActionRule( "", EVALUATED_DATA, DATA_ELEMENT_ID, IssueType.ERROR.name() + CONTENT,
                RuleActionType.ERROR, Collections.emptySet() ) );
    }

    private List<EventActionRule> getWarningRuleEffects()
    {
        return Lists.newArrayList(
            new EventActionRule( "", EVALUATED_DATA, null, IssueType.WARNING.name() + CONTENT, RuleActionType.WARNING,
                Collections.emptySet() ) );
    }

    private List<EventActionRule> getErrorOnCompleteRuleEffects()
    {
        return Lists.newArrayList(
            new EventActionRule( "", EVALUATED_DATA, null, IssueType.ERROR.name() + CONTENT,
                RuleActionType.ERROR_ON_COMPLETE, Collections.emptySet() ) );
    }

    private List<EventActionRule> getWarningOnCompleteRuleEffects()
    {
        return Lists.newArrayList(
            new EventActionRule( "", EVALUATED_DATA, null, IssueType.WARNING.name() + CONTENT,
                RuleActionType.WARNING_ON_COMPLETE, Collections.emptySet() ) );
    }
}
