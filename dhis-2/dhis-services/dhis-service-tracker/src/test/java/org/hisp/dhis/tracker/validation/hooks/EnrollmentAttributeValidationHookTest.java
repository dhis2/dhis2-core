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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EnrollmentAttributeValidationHookTest
{

    @InjectMocks
    private EnrollmentAttributeValidationHook hookToTest;

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    private Enrollment enrollment;

    @Mock
    private Program program;

    @Mock
    private TrackerPreheat preheat;

    @Mock
    private TrackedEntityInstance trackedEntityInstance;

    private final static String trackedEntity = "trackedEntity";

    private final static String trackedAttribute = "attribute";

    private final static String trackedAttribute1 = "attribute1";

    private TrackedEntityAttribute trackedEntityAttribute;

    private TrackedEntityAttribute trackedEntityAttribute1;

    @BeforeEach
    public void setUp()
    {

        trackedEntityAttribute = new TrackedEntityAttribute( "name", "description", ValueType.TEXT, false,
            false );
        trackedEntityAttribute.setUid( trackedAttribute );

        trackedEntityAttribute1 = new TrackedEntityAttribute( "name1", "description1", ValueType.TEXT, false,
            false );
        trackedEntityAttribute1.setUid( trackedAttribute1 );

        when( validationContext.getProgram( anyString() ) ).thenReturn( program );
        when( enrollment.getProgram() ).thenReturn( "program" );
        when( validationContext.getTrackedEntityAttribute( trackedAttribute ) ).thenReturn( trackedEntityAttribute );
        when( validationContext.getTrackedEntityAttribute( trackedAttribute1 ) ).thenReturn( trackedEntityAttribute1 );

        String uid = CodeGenerator.generateUid();
        when( enrollment.getUid() ).thenReturn( uid );
        when( enrollment.getEnrollment() ).thenReturn( uid );
        enrollment.setTrackedEntity( trackedEntity );

        TrackerBundle bundle = TrackerBundle.builder().build();
        bundle.setPreheat( preheat );

        when( validationContext.getBundle() ).thenReturn( bundle );
    }

    @Test
    void shouldFailValidationWhenValueIsNullAndAttributeIsMandatory()
    {
        // given 1 attribute has null value
        Attribute attribute = Attribute.builder().attribute( trackedAttribute ).valueType( ValueType.TEXT )
            .value( "value" ).build();
        Attribute attribute1 = Attribute.builder().attribute( trackedAttribute1 ).valueType( ValueType.TEXT ).build();

        // when both tracked attributes are mandatory
        when( program.getProgramAttributes() ).thenReturn( Arrays.asList(
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute, false, true ),
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute1, false, true ) ) );

        when( enrollment.getAttributes() ).thenReturn( Arrays.asList( attribute, attribute1 ) );
        when( trackedEntityInstance.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>(
                Arrays.asList( new TrackedEntityAttributeValue( trackedEntityAttribute, trackedEntityInstance ),
                    new TrackedEntityAttributeValue( trackedEntityAttribute1, trackedEntityInstance ) ) ) );
        when( preheat.getTrackedEntity( TrackerIdScheme.UID, enrollment.getTrackedEntity() ) )
            .thenReturn( trackedEntityInstance );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );
        hookToTest.validateEnrollment( reporter, enrollment );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        hasTrackerError( reporter, TrackerErrorCode.E1076, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldPassValidationWhenValueIsNullAndAttributeIsNotMandatory()
    {
        // given 1 attribute has null value
        Attribute attribute = Attribute.builder().attribute( trackedAttribute ).valueType( ValueType.TEXT )
            .value( "value" ).build();
        Attribute attribute1 = Attribute.builder().attribute( trackedAttribute1 ).valueType( ValueType.TEXT ).build();

        // when only 1 tracked attributes is mandatory
        when( program.getProgramAttributes() ).thenReturn( Arrays.asList(
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute, false, true ),
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute1, false, false ) ) );

        when( enrollment.getAttributes() ).thenReturn( Arrays.asList( attribute, attribute1 ) );
        when( trackedEntityInstance.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>(
                Arrays.asList( new TrackedEntityAttributeValue( trackedEntityAttribute, trackedEntityInstance ),
                    new TrackedEntityAttributeValue( trackedEntityAttribute1, trackedEntityInstance ) ) ) );
        when( preheat.getTrackedEntity( TrackerIdScheme.UID, enrollment.getTrackedEntity() ) )
            .thenReturn( trackedEntityInstance );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );
        hookToTest.validateEnrollment( reporter, enrollment );

        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    void shouldFailValidationWhenValueIsNullAndAttributeIsNotMandatoryAndAttributeNotExistsInTei()
    {
        // given 1 attribute has null value and do not exists in Tei
        Attribute attribute = Attribute.builder().attribute( trackedAttribute ).valueType( ValueType.TEXT )
            .value( "value" ).build();
        Attribute attribute1 = Attribute.builder().attribute( trackedAttribute1 ).valueType( ValueType.TEXT ).build();

        // when 2 tracked attributes are mandatory
        when( program.getProgramAttributes() ).thenReturn( Arrays.asList(
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute, false, true ),
            new ProgramTrackedEntityAttribute( program, trackedEntityAttribute1, false, true ) ) );

        when( enrollment.getAttributes() ).thenReturn( Arrays.asList( attribute, attribute1 ) );
        when( trackedEntityInstance.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>( Collections
                .singletonList( new TrackedEntityAttributeValue( trackedEntityAttribute, trackedEntityInstance ) ) ) );
        when( preheat.getTrackedEntity( TrackerIdScheme.UID, enrollment.getTrackedEntity() ) )
            .thenReturn( trackedEntityInstance );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );
        hookToTest.validateEnrollment( reporter, enrollment );

        assertThat( reporter.getReportList(), hasSize( 2 ) );
        hasTrackerError( reporter, TrackerErrorCode.E1076, TrackerType.ENROLLMENT, enrollment.getUid() );
        hasTrackerError( reporter, TrackerErrorCode.E1018, TrackerType.ENROLLMENT, enrollment.getUid() );
    }

    @Test
    void shouldFailValidationWhenAttributeIsNotPresentInDB()
    {
        Attribute attribute = Attribute.builder()
            .attribute( "invalidAttribute" )
            .valueType( ValueType.TEXT )
            .value( "value" )
            .build();

        when( program.getProgramAttributes() ).thenReturn( Collections.emptyList() );

        when( enrollment.getAttributes() ).thenReturn( Collections.singletonList( attribute ) );
        when( trackedEntityInstance.getTrackedEntityAttributeValues() )
            .thenReturn( new HashSet<>( Collections
                .singletonList( new TrackedEntityAttributeValue( trackedEntityAttribute, trackedEntityInstance ) ) ) );
        when( preheat.getTrackedEntity( TrackerIdScheme.UID, enrollment.getTrackedEntity() ) )
            .thenReturn( trackedEntityInstance );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, enrollment );
        hookToTest.validateEnrollment( reporter, enrollment );

        assertThat( reporter.getReportList(), hasSize( 1 ) );
        hasTrackerError( reporter,
            TrackerErrorCode.E1006,
            TrackerType.ENROLLMENT,
            enrollment.getUid() );
    }
}
