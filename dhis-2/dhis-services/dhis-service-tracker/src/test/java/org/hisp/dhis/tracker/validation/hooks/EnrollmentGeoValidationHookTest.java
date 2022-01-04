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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1012;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1074;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentGeoValidationHookTest
{

    private static final String PROGRAM = "Program";

    private EnrollmentGeoValidationHook hookToTest;

    @Mock
    private TrackerImportValidationContext validationContext;

    @BeforeEach
    public void setUp()
    {
        hookToTest = new EnrollmentGeoValidationHook();

        TrackerBundle bundle = TrackerBundle.builder().build();

        when( validationContext.getBundle() ).thenReturn( bundle );

        Program program = new Program();
        program.setFeatureType( FeatureType.POINT );
        when( validationContext.getProgram( PROGRAM ) ).thenReturn( program );
    }

    @Test
    void testGeometryIsValid()
    {
        // given
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( PROGRAM );
        enrollment.setGeometry( new GeometryFactory().createPoint() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        // when
        this.hookToTest.validateEnrollment( reporter, enrollment );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testEnrollmentWithNoProgramThrowsAnError()
    {
        // given
        Enrollment enrollment = new Enrollment();
        enrollment.setProgram( null );
        enrollment.setGeometry( new GeometryFactory().createPoint() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        assertThrows( NullPointerException.class, () -> this.hookToTest.validateEnrollment( reporter, enrollment ) );
    }

    @Test
    void testProgramWithNullFeatureTypeFailsGeometryValidation()
    {
        // given
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setProgram( PROGRAM );
        enrollment.setGeometry( new GeometryFactory().createPoint() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        // when
        Program program = new Program();
        when( validationContext.getProgram( PROGRAM ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1074, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testProgramWithFeatureTypeNoneFailsGeometryValidation()
    {
        // given
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setProgram( PROGRAM );
        enrollment.setGeometry( new GeometryFactory().createPoint() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        // when
        Program program = new Program();
        program.setFeatureType( FeatureType.NONE );
        when( validationContext.getProgram( PROGRAM ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1012, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testProgramWithFeatureTypeDifferentFromGeometryFails()
    {
        // given
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        enrollment.setProgram( PROGRAM );
        enrollment.setGeometry( new GeometryFactory().createPoint() );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );

        // when
        Program program = new Program();
        program.setFeatureType( FeatureType.MULTI_POLYGON );
        when( validationContext.getProgram( PROGRAM ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, enrollment );

        // then
        hasTrackerError( reporter, E1012, ENROLLMENT, enrollment.getUid() );
    }
}
