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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
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
class ShowErrorWarningValidatorTest extends DhisConvenienceTest
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
        bundle.setEnrollments( getEnrollments() );
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
    void testValidateShowErrorRuleActionForEnrollment()
    {
        List<ProgramRuleIssue> errors = errorImplementer.validateEnrollment( bundle, getErrorActionRules(),
            activeEnrollment() );
        assertErrors( errors, 1 );

        errorImplementer.validateEnrollment( bundle, getErrorActionRules(), completedEnrollment() );
        assertErrors( errors, 1 );
    }

    @Test
    void testValidateShowWarningRuleActionForEnrollment()
    {
        List<ProgramRuleIssue> warnings = warningImplementer.validateEnrollment( bundle, getWarningActionRules(),
            activeEnrollment() );
        assertWarnings( warnings, 1 );

        warningImplementer.validateEnrollment( bundle, getWarningActionRules(), completedEnrollment() );
        assertWarnings( warnings, 1 );
    }

    @Test
    void testValidateShowErrorOnCompleteRuleActionForEnrollment()
    {
        List<ProgramRuleIssue> errors = errorOnCompleteImplementer.validateEnrollment( bundle,
            getErrorOnCompleteActionRules(),
            activeEnrollment() );
        assertTrue( errors.isEmpty() );

        errors = errorOnCompleteImplementer.validateEnrollment( bundle, getErrorOnCompleteActionRules(),
            completedEnrollment() );
        assertErrors( errors, 1 );
    }

    @Test
    void testValidateShowWarningOnCompleteRuleActionForEnrollment()
    {
        List<ProgramRuleIssue> warnings = warningOnCompleteImplementer.validateEnrollment( bundle,
            getWarningOnCompleteActionRules(),
            activeEnrollment() );
        assertTrue( warnings.isEmpty() );

        warnings = warningOnCompleteImplementer.validateEnrollment( bundle, getWarningOnCompleteActionRules(),
            completedEnrollment() );
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

    private List<ErrorActionRule> getErrorActionRules()
    {
        return Lists.newArrayList(
            new ErrorActionRule( "", EVALUATED_DATA, null, IssueType.ERROR.name() + CONTENT ) );
    }

    private List<WarningActionRule> getWarningActionRules()
    {
        return Lists.newArrayList(
            new WarningActionRule( "", EVALUATED_DATA, null, IssueType.WARNING.name() + CONTENT ) );
    }

    private List<ErrorOnCompleteActionRule> getErrorOnCompleteActionRules()
    {
        return Lists.newArrayList(
            new ErrorOnCompleteActionRule( "", EVALUATED_DATA, null, IssueType.ERROR.name() + CONTENT ) );
    }

    private List<WarningOnCompleteActionRule> getWarningOnCompleteActionRules()
    {
        return Lists.newArrayList(
            new WarningOnCompleteActionRule( "", EVALUATED_DATA, null, IssueType.WARNING.name() + CONTENT ) );
    }
}
