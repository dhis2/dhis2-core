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
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
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
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        hookToTest = new EnrollmentGeoValidationHook();

        bundle = TrackerBundle.builder()
            .preheat( preheat )
            .build();

        Program program = new Program();
        program.setFeatureType( FeatureType.POINT );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM ) ) ).thenReturn( program );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void testGeometryIsValid()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .program( MetadataIdentifier.ofUid( PROGRAM ) )
            .geometry( new GeometryFactory().createPoint() )
            .build();

        // when
        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

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

        when( preheat.getProgram( (MetadataIdentifier) null ) ).thenReturn( null );

        assertThrows( NullPointerException.class,
            () -> this.hookToTest.validateEnrollment( reporter, bundle, enrollment ) );
    }

    @Test
    void testProgramWithNullFeatureTypeFailsGeometryValidation()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM ) )
            .geometry( new GeometryFactory().createPoint() )
            .build();

        // when
        Program program = new Program();
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM ) ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1074, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testProgramWithFeatureTypeNoneFailsGeometryValidation()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM ) )
            .geometry( new GeometryFactory().createPoint() )
            .build();

        // when
        Program program = new Program();
        program.setFeatureType( FeatureType.NONE );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM ) ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1012, ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void testProgramWithFeatureTypeDifferentFromGeometryFails()
    {
        // given
        Enrollment enrollment = Enrollment.builder()
            .enrollment( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( PROGRAM ) )
            .geometry( new GeometryFactory().createPoint() )
            .build();

        // when
        Program program = new Program();
        program.setFeatureType( FeatureType.MULTI_POLYGON );
        when( preheat.getProgram( MetadataIdentifier.ofUid( PROGRAM ) ) ).thenReturn( program );

        this.hookToTest.validateEnrollment( reporter, bundle, enrollment );

        // then
        hasTrackerError( reporter, E1012, ENROLLMENT, enrollment.getUid() );
    }
}
