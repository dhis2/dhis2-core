package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentDateValidationHookTest
{
    private EnrollmentDateValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext validationContext;

    @Before
    public void setUp()
    {
        hookToTest = new EnrollmentDateValidationHook();

        TrackerBundle bundle = TrackerBundle.builder().build();

        when( validationContext.getBundle() ).thenReturn( bundle );
    }

    @Test
    public void testMandatoryDatesMustBePresent()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( CodeGenerator.generateUid() );
        enrollment.setOccurredAt( DateUtils.getIso8601( new Date() ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        when( validationContext.getProgram( enrollment.getProgram() ) ).thenReturn( new Program() );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1025 ) );
    }

    @Test
    public void testDatesMustNotBeInTheFuture()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( CodeGenerator.generateUid() );
        final Date dateInTheFuture = org.apache.commons.lang.time.DateUtils.addDays( new Date(), 2 );

        enrollment.setOccurredAt( DateUtils.getIso8601( dateInTheFuture ) );
        enrollment.setEnrolledAt( DateUtils.getIso8601( dateInTheFuture ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        when( validationContext.getProgram( enrollment.getProgram() ) ).thenReturn( new Program() );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1020 ) );
        assertThat( reporter.getReportList().get( 1 ).getErrorCode(), is( TrackerErrorCode.E1021 ) );
    }

    @Test
    public void testDatesCanBeInTheFuture()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( CodeGenerator.generateUid() );
        final Date dateInTheFuture = org.apache.commons.lang.time.DateUtils.addDays( new Date(), 2 );

        enrollment.setOccurredAt( DateUtils.getIso8601( dateInTheFuture ) );
        enrollment.setEnrolledAt( DateUtils.getIso8601( dateInTheFuture ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        Program program = new Program();
        program.setSelectEnrollmentDatesInFuture( true );
        program.setSelectIncidentDatesInFuture( true );
        when( validationContext.getProgram( enrollment.getProgram() ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void testFailOnMissingOccurredAtDate()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( CodeGenerator.generateUid() );

        enrollment.setEnrolledAt( DateUtils.getIso8601( new Date() ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        Program program = new Program();
        program.setDisplayIncidentDate( true );
        when( validationContext.getProgram( enrollment.getProgram() ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( TrackerErrorCode.E1023 ) );
    }

}