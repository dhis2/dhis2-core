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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.programrule.IssueType;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ShowErrorWarningExecutorTest extends DhisConvenienceTest
{

    private final static String CONTENT = "SHOW ERROR DATA";

    private final static String EVALUATED_DATA = "4.0";

    private final static String ACTIVE_ENROLLMENT_ID = "ActiveEnrollmentUid";

    private final static String COMPLETED_ENROLLMENT_ID = "CompletedEnrollmentUid";

    private ShowWarningOnCompleteExecutor warningOnCompleteExecutor = new ShowWarningOnCompleteExecutor(
        getErrorActionRule() );

    private ShowErrorOnCompleteExecutor errorOnCompleteExecutor = new ShowErrorOnCompleteExecutor(
        getErrorActionRule() );

    private ShowErrorExecutor showErrorExecutor = new ShowErrorExecutor( getErrorActionRule() );

    private ShowWarningExecutor showWarningExecutor = new ShowWarningExecutor( getErrorActionRule() );

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
    void testValidateShowErrorRuleActionForEnrollment()
    {
        Optional<ProgramRuleIssue> error = showErrorExecutor.executeEnrollmentRuleAction( bundle, activeEnrollment() );
        assertTrue( error.isPresent() );

        error = showErrorExecutor.executeEnrollmentRuleAction( bundle, completedEnrollment() );
        assertTrue( error.isPresent() );
    }

    @Test
    void testValidateShowWarningRuleActionForEnrollment()
    {
        Optional<ProgramRuleIssue> warning = showWarningExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );
        assertTrue( warning.isPresent() );

        warning = showWarningExecutor.executeEnrollmentRuleAction( bundle, completedEnrollment() );
        assertTrue( warning.isPresent() );
    }

    @Test
    void testValidateShowErrorOnCompleteRuleActionForEnrollment()
    {
        Optional<ProgramRuleIssue> error = errorOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );
        assertFalse( error.isPresent() );

        error = errorOnCompleteExecutor.executeEnrollmentRuleAction( bundle, completedEnrollment() );
        assertTrue( error.isPresent() );
    }

    @Test
    void testValidateShowWarningOnCompleteRuleActionForEnrollment()
    {
        Optional<ProgramRuleIssue> warning = warningOnCompleteExecutor.executeEnrollmentRuleAction( bundle,
            activeEnrollment() );
        assertFalse( warning.isPresent() );

        warning = warningOnCompleteExecutor.executeEnrollmentRuleAction( bundle, completedEnrollment() );
        assertTrue( warning.isPresent() );
    }

    private List<Enrollment> getEnrollments()
    {
        return Lists.newArrayList( activeEnrollment(), completedEnrollment() );
    }

    private Enrollment activeEnrollment()
    {
        Enrollment activeEnrollment = new Enrollment();
        activeEnrollment.setEnrollment( ACTIVE_ENROLLMENT_ID );
        activeEnrollment.setStatus( EnrollmentStatus.ACTIVE );
        return activeEnrollment;
    }

    private Enrollment completedEnrollment()
    {
        Enrollment completedEnrollment = new Enrollment();
        completedEnrollment.setEnrollment( COMPLETED_ENROLLMENT_ID );
        completedEnrollment.setStatus( EnrollmentStatus.COMPLETED );
        return completedEnrollment;
    }

    private ErrorWarningRuleAction getErrorActionRule()
    {
        return new ErrorWarningRuleAction( "", EVALUATED_DATA, null, IssueType.ERROR.name() + CONTENT );
    }
}
