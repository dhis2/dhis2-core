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
package org.hisp.dhis.tracker.programrule;

import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionError;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.implementers.RuleEngineErrorLogger;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;

@ExtendWith( MockitoExtension.class )
class RuleEngineErrorLoggerTest extends DhisConvenienceTest
{

    private final static String RULE_EVENT_ID = "Rule_event_id";

    private final static String EVENT_ERROR_MESSAGE = "Event error message";

    private final static String RULE_ENROLLMENT_ID = "Rule_enrollment_id";

    private final static String ENROLLMENT_ERROR_MESSAGE = "Enrollment error message";

    private final static String ENROLLMENT_ID = "EnrollmentUid";

    private final static String EVENT_ID = "EventUid";

    private final static String TEI_ID = "TeiId";

    private static ProgramStage programStage;

    private final RuleEngineErrorLogger ruleEngineErrorLogger = new RuleEngineErrorLogger();

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @BeforeEach
    void setUpTest()
    {
        programStage = createProgramStage( 'A', 0 );
        programStage.setValidationStrategy( ValidationStrategy.ON_UPDATE_AND_INSERT );
        bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );
    }

    @Test
    void testValidateEventWithError()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( programStage ) ) ).thenReturn( programStage );
        List<ProgramRuleIssue> issues = ruleEngineErrorLogger.validateEvent( bundle, getRuleEventEffects(),
            getEvent() );

        assertFalse( issues.isEmpty() );
        assertEquals( WARNING, issues.get( 0 ).getIssueType() );
        assertEquals( RULE_EVENT_ID, issues.get( 0 ).getRuleUid() );
        assertEquals( TrackerErrorCode.E1300, issues.get( 0 ).getIssueCode() );
        assertEquals( EVENT_ERROR_MESSAGE, issues.get( 0 ).getArgs().get( 0 ) );
    }

    @Test
    void testValidateEnrollmentWithError()
    {
        List<ProgramRuleIssue> issues = ruleEngineErrorLogger.validateEnrollment( bundle, getRuleEnrollmentEffects(),
            getEnrollment() );

        assertFalse( issues.isEmpty() );
        assertEquals( WARNING, issues.get( 0 ).getIssueType() );
        assertEquals( RULE_ENROLLMENT_ID, issues.get( 0 ).getRuleUid() );
        assertEquals( TrackerErrorCode.E1300, issues.get( 0 ).getIssueCode() );
        assertEquals( ENROLLMENT_ERROR_MESSAGE, issues.get( 0 ).getArgs().get( 0 ) );
    }

    private Event getEvent()
    {
        Event event = new Event();
        event.setEvent( EVENT_ID );
        event.setProgramStage( MetadataIdentifier.ofUid( programStage ) );
        return event;
    }

    private Enrollment getEnrollment()
    {
        return Enrollment.builder()
            .enrollment( ENROLLMENT_ID )
            .trackedEntity( TEI_ID )
            .build();
    }

    private List<RuleEffect> getRuleEventEffects()
    {
        RuleAction ruleActionError = RuleActionError.create( EVENT_ERROR_MESSAGE );
        return Lists.newArrayList( RuleEffect.create( RULE_EVENT_ID, ruleActionError, EVENT_ERROR_MESSAGE ) );
    }

    private List<RuleEffect> getRuleEnrollmentEffects()
    {
        RuleAction ruleActionError = RuleActionError.create( ENROLLMENT_ERROR_MESSAGE );
        return Lists.newArrayList( RuleEffect.create( RULE_ENROLLMENT_ID, ruleActionError, ENROLLMENT_ERROR_MESSAGE ) );
    }
}
