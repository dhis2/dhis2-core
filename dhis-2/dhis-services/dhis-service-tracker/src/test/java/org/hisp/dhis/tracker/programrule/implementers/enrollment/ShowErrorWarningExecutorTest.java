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
package org.hisp.dhis.tracker.programrule.implementers.enrollment;

import static org.hisp.dhis.tracker.domain.EnrollmentStatus.*;
import static org.hisp.dhis.tracker.programrule.IssueType.ERROR;
import static org.hisp.dhis.tracker.programrule.IssueType.WARNING;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1300;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class ShowErrorWarningExecutorTest extends DhisConvenienceTest
{
    private final static String RULE_UID = "Rule uid";

    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String EVALUATED_DATA = "4.0";

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private ShowWarningOnCompleteExecutor warningOnCompleteExecutor = new ShowWarningOnCompleteExecutor(
        getErrorActionRule( WARNING ) );

    private ShowErrorOnCompleteExecutor errorOnCompleteExecutor = new ShowErrorOnCompleteExecutor(
        getErrorActionRule( ERROR ) );

    private ShowErrorExecutor showErrorExecutor = new ShowErrorExecutor( getErrorActionRule( ERROR ) );

    private ShowWarningExecutor showWarningExecutor = new ShowWarningExecutor( getErrorActionRule( WARNING ) );

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    @BeforeEach
    void setUpTest()
    {
        bundle = TrackerBundle.builder().build();
        bundle.setEnrollments( getEnrollments() );
        bundle.setPreheat( preheat );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForActiveEnrollment()
    {
        Optional<ProgramRuleIssue> error = showErrorExecutor.executeEnrollmentRuleAction( bundle, activeEnrollment() );

        assertTrue( error.isPresent() );
        assertEquals( ProgramRuleIssue.error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorActionIsTriggeredForCompletedEnrollment()
    {
        Optional<ProgramRuleIssue> error = showErrorExecutor.executeEnrollmentRuleAction( bundle,
            completedEnrollment() );

        assertTrue( error.isPresent() );
        assertEquals( ProgramRuleIssue.error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForActiveEnrollment()
    {
        Optional<ProgramRuleIssue> warning = showWarningExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );

        assertTrue( warning.isPresent() );
        assertEquals( ProgramRuleIssue.warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorActionIsTriggeredForCompletedEnrollment()
    {
        Optional<ProgramRuleIssue> warning = showWarningExecutor.executeEnrollmentRuleAction( bundle,
            completedEnrollment() );

        assertTrue( warning.isPresent() );
        assertEquals( ProgramRuleIssue.warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    @Test
    void shouldNotReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForActiveEnrollment()
    {
        Optional<ProgramRuleIssue> error = errorOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );

        assertFalse( error.isPresent() );
    }

    @Test
    void shouldReturnAnErrorWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEnrollment()
    {
        Optional<ProgramRuleIssue> error = errorOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            completedEnrollment() );

        assertTrue( error.isPresent() );
        assertEquals( ProgramRuleIssue.error( RULE_UID, E1300, validationMessage( ERROR ) ), error.get() );
    }

    @Test
    void shouldNotReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForActiveEnrollment()
    {
        Optional<ProgramRuleIssue> warning = warningOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );

        assertFalse( warning.isPresent() );
    }

    @Test
    void shouldReturnAWarningWhenAShowErrorOnCompleteActionIsTriggeredForCompletedEnrollment()
    {
        Optional<ProgramRuleIssue> warning = warningOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            completedEnrollment() );

        assertTrue( warning.isPresent() );
        assertEquals( ProgramRuleIssue.warning( RULE_UID, E1300, validationMessage( WARNING ) ), warning.get() );
    }

    private List<Enrollment> getEnrollments()
    {
        return List.of( activeEnrollment(), completedEnrollment() );
    }

    private Enrollment activeEnrollment()
    {
        return Enrollment.builder()
            .enrollment( ACTIVE_ENROLLMENT_ID )
            .status( ACTIVE )
            .build();
    }

    private Enrollment completedEnrollment()
    {
        return Enrollment.builder()
            .enrollment( COMPLETED_ENROLLMENT_ID )
            .status( COMPLETED )
            .build();
    }

    private ErrorWarningRuleAction getErrorActionRule( IssueType issueType )
    {
        return new ErrorWarningRuleAction( RULE_UID, EVALUATED_DATA, null, issueType.name() + CONTENT );
    }

    private String validationMessage( IssueType issueType )
    {
        return issueType.name() + CONTENT + " " + EVALUATED_DATA;
    }
}
