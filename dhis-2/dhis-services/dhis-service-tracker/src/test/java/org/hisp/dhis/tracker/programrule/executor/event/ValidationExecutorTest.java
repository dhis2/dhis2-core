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
package org.hisp.dhis.tracker.programrule.executor.event;

import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.hisp.dhis.tracker.programrule.ProgramRuleIssue.error;
import static org.hisp.dhis.tracker.programrule.ProgramRuleIssue.warning;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.programrule.executor.ValidationRuleAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ValidationExecutorTest extends DhisConvenienceTest
{
    private final static String RULE_UID = "Rule uid";

    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String EVALUATED_DATA = "4.0";

    private final static String ACTIVE_EVENT_ID = "EventUid";

    private final static String COMPLETED_EVENT_ID = "CompletedEventUid";

    private final static String PROGRAM_STAGE_ID = "ProgramStageId";

    private final static String DATA_ELEMENT_ID = "DataElementId";

    private final static String ANOTHER_DATA_ELEMENT_ID = "AnotherDataElementId";

    private ShowWarningOnCompleteExecutor warningOnCompleteExecutor = new ShowWarningOnCompleteExecutor(
        getValidationRuleAction( WARNING ) );

    private ShowErrorOnCompleteExecutor errorOnCompleteExecutor = new ShowErrorOnCompleteExecutor(
        getValidationRuleAction( ERROR ) );

    private ShowErrorExecutor showErrorExecutor = new ShowErrorExecutor( getValidationRuleAction( ERROR ) );

    private ShowWarningExecutor showWarningExecutor = new ShowWarningExecutor( getValidationRuleAction( WARNING ) );

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ProgramStage programStage;

    private ProgramStage anotherProgramStage;

    @BeforeEach
    void setUpTest()
    {
        programStage = createProgramStage( 'A', 0 );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementA = createProgramStageDataElement( programStage, dataElementA,
            0 );
        programStage.setProgramStageDataElements( Set.of( programStageDataElementA ) );
        anotherProgramStage = createProgramStage( 'B', 0 );
        anotherProgramStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setUid( ANOTHER_DATA_ELEMENT_ID );
        ProgramStageDataElement programStageDataElementB = createProgramStageDataElement( anotherProgramStage,
            dataElementB, 0 );
        anotherProgramStage.setProgramStageDataElements( Set.of( programStageDataElementB ) );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) ) ).thenReturn( programStage );

        bundle = TrackerBundle.builder().build();
        bundle.setEvents( getEvents() );
        bundle.setPreheat( preheat );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForActiveEvent()
    {
        Optional<ProgramRuleIssue> error = showErrorExecutor.executeRuleAction( bundle, activeEvent() );

        assertTrue( error.isPresent() );
        assertEquals( error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForCompletedEvent()
    {
        Optional<ProgramRuleIssue> error = showErrorExecutor.executeRuleAction( bundle,
            completedEvent() );

        assertTrue( error.isPresent() );
        assertEquals( error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForActiveEvent()
    {
        Optional<ProgramRuleIssue> warning = showWarningExecutor.executeRuleAction( bundle,
            activeEvent() );

        assertTrue( warning.isPresent() );
        assertEquals( warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForCompletedEvent()
    {
        Optional<ProgramRuleIssue> warning = showWarningExecutor.executeRuleAction( bundle,
            completedEvent() );

        assertTrue( warning.isPresent() );
        assertEquals( warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    @Test
    void shouldNotReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForActiveEvent()
    {
        Optional<ProgramRuleIssue> error = errorOnCompleteExecutor.executeRuleAction( bundle,
            activeEvent() );

        assertFalse( error.isPresent() );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEvent()
    {
        Optional<ProgramRuleIssue> error = errorOnCompleteExecutor.executeRuleAction( bundle,
            completedEvent() );

        assertTrue( error.isPresent() );
        assertEquals( error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldNotReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForActiveEvent()
    {
        Optional<ProgramRuleIssue> warning = warningOnCompleteExecutor.executeRuleAction( bundle,
            activeEvent() );

        assertFalse( warning.isPresent() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEvent()
    {
        Optional<ProgramRuleIssue> warning = warningOnCompleteExecutor.executeRuleAction( bundle,
            completedEvent() );

        assertTrue( warning.isPresent() );
        assertEquals( warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    private ValidationRuleAction getValidationRuleAction( IssueType issueType )
    {
        return new ValidationRuleAction( RULE_UID, EVALUATED_DATA, null, issueType.name() + CONTENT );
    }

    private List<Event> getEvents()
    {
        return List.of( activeEvent(), completedEvent() );
    }

    private Event activeEvent()
    {
        return Event.builder()
            .event( ACTIVE_EVENT_ID )
            .status( EventStatus.ACTIVE )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .build();
    }

    private Event completedEvent()
    {
        return Event.builder()
            .event( COMPLETED_EVENT_ID )
            .status( EventStatus.COMPLETED )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_ID ) )
            .build();
    }

    private String validationMessage( IssueType issueType )
    {
        return issueType.name() + CONTENT + " " + EVALUATED_DATA;
    }
}
