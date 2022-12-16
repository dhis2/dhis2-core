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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasError;
import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.assertHasWarning;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultValidationServiceTest
{

    private DefaultValidationService service;

    private TrackerPreheat preheat;

    private TrackerBundle.TrackerBundleBuilder bundleBuilder;

    private TrackerBundle bundle;

    private Validator validator1;

    private Validator validator2;

    private Validators validators;

    private Validators ruleEngineValidators;

    @BeforeEach
    void setUp()
    {
        preheat = mock( TrackerPreheat.class );
        when( preheat.getIdSchemes() ).thenReturn( TrackerIdSchemeParams.builder().build() );

        validator1 = mock( Validator.class );
        validator2 = mock( Validator.class );
        when( validator1.needsToRun( any() ) ).thenReturn( true );
        when( validator2.needsToRun( any() ) ).thenReturn( true );

        validators = mock( Validators.class );
        ruleEngineValidators = mock( Validators.class );

        bundleBuilder = newBundle();
    }

    @Test
    void shouldNotValidateWhenModeIsSkipAndUserIsNull()
    {
        bundle = bundleBuilder
            .validationMode( ValidationMode.SKIP )
            .user( null )
            .trackedEntities( trackedEntities( trackedEntity() ) )
            .build();
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( validator1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        service.validate( bundle );

        verifyNoInteractions( validator1 );
    }

    @Test
    void shouldNotValidateWhenModeIsSkipAndUserIsASuperUser()
    {
        bundle = bundleBuilder
            .validationMode( ValidationMode.SKIP )
            .user( superUser() )
            .trackedEntities( trackedEntities( trackedEntity() ) )
            .build();
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( validator1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        service.validate( bundle );

        verifyNoInteractions( validator1 );
    }

    @Test
    void shouldValidateWhenModeIsNotSkipAndUserIsASuperUser()
    {
        TrackedEntity trackedEntity = trackedEntity();
        bundle = bundleBuilder
            .validationMode( ValidationMode.FULL )
            .user( superUser() )
            .trackedEntities( trackedEntities( trackedEntity ) )
            .build();
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( validator1, validator2 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        service.validate( bundle );

        verify( validator1, times( 1 ) ).validate( any(), any(), eq( trackedEntity ) );
        verify( validator2, times( 1 ) ).validate( any(), any(), eq( trackedEntity ) );
    }

    @Test
    void skipOnErrorValidatorPreventsFurtherValidationOfInvalidEntityEvenInFullValidationModeOfTrackedEntities()
    {
        // Test shows
        // 1. Validators with skipOnError==true will prevent subsequent validators from validating an invalid entity
        // 2. DefaultValidationService removes invalid entities from the TrackerBundle
        TrackedEntity validTrackedEntity = trackedEntity();
        TrackedEntity invalidTrackedEntity = trackedEntity();
        bundle = bundleBuilder
            .trackedEntities( trackedEntities( validTrackedEntity, invalidTrackedEntity ) )
            .build();

        Validator<TrackedEntity> skipOnError = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, TrackedEntity trackedEntity )
            {
                addErrorOnMatch( reporter, invalidTrackedEntity, trackedEntity, ValidationCode.E1032 );
            }

            @Override
            public boolean skipOnError()
            {
                return true; // subsequent validator will not be called
            }
        };

        Validator<TrackedEntity> doNotSkipOnError = ( r, b, e ) -> addErrorOnMatch( r, invalidTrackedEntity, e,
            ValidationCode.E9999 );
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( skipOnError, doNotSkipOnError ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 1, report.getErrors().size(), "only skip on error validator should add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidTrackedEntity ) );

        assertAll( "invalid tracked entities",
            () -> assertFalse( bundle.getTrackedEntities().contains( invalidTrackedEntity ) ),
            () -> assertTrue( bundle.getTrackedEntities().contains( validTrackedEntity ) ) );
    }

    @Test
    void fullValidationModeAddsAllErrorsToReportOfTrackedEntities()
    {
        // Test shows
        // in ValidationMode==FULL all validators are called even with entities that
        // are already invalid (i.e. have an error in the validation report)
        TrackedEntity validTrackedEntity = trackedEntity();
        TrackedEntity invalidTrackedEntity = trackedEntity();
        bundle = bundleBuilder
            .trackedEntities( trackedEntities( validTrackedEntity, invalidTrackedEntity ) )
            .build();

        Validator<TrackedEntity> v1 = ( r, b, e ) -> addErrorOnMatch( r, invalidTrackedEntity, e,
            ValidationCode.E1032 );
        Validator<TrackedEntity> v2 = ( r, b, e ) -> addErrorOnMatch( r, invalidTrackedEntity, e,
            ValidationCode.E9999 );
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( v1, v2 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 2, report.getErrors().size(), "both validators should each add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidTrackedEntity ),
            () -> assertHasError( report, ValidationCode.E9999, invalidTrackedEntity ) );

        assertAll( "invalid tracked entities",
            () -> assertFalse( bundle.getTrackedEntities().contains( invalidTrackedEntity ) ),
            () -> assertTrue( bundle.getTrackedEntities().contains( validTrackedEntity ) ) );
    }

    @Test
    void skipOnErrorValidatorPreventsFurtherValidationOfInvalidEntityEvenInFullValidationModeOfEnrollments()
    {
        // Test shows
        // 1. Validators with skipOnError==true will prevent subsequent validators from validating an invalid entity
        // 2. DefaultValidationService removes invalid entities from the TrackerBundle
        Enrollment validEnrollment = enrollment();
        Enrollment invalidEnrollment = enrollment();
        bundle = bundleBuilder
            .enrollments( enrollments( invalidEnrollment, validEnrollment ) )
            .build();
        Validator<Enrollment> skipOnError = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, Enrollment enrollment )
            {
                addErrorOnMatch( reporter, invalidEnrollment, enrollment, ValidationCode.E1032 );
            }

            @Override
            public boolean skipOnError()
            {
                return true; // subsequent validator will not be called
            }
        };

        Validator<Enrollment> doNotSkipOnError = ( r, b, e ) -> addErrorOnMatch( r, invalidEnrollment, e,
            ValidationCode.E9999 );
        when( validators.getEnrollmentValidators() ).thenReturn( List.of( skipOnError, doNotSkipOnError ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 1, report.getErrors().size(), "only skip on error validator should add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEnrollment ) );

        assertAll( "invalid enrollments",
            () -> assertFalse( bundle.getEnrollments().contains( invalidEnrollment ) ),
            () -> assertTrue( bundle.getEnrollments().contains( validEnrollment ) ) );
    }

    @Test
    void fullValidationModeAddsAllErrorsToReportOfEnrollments()
    {
        // Test shows
        // in ValidationMode==FULL all validators are called even with entities that
        // are already invalid (i.e. have an error in the validation report)
        Enrollment validEnrollment = enrollment();
        Enrollment invalidEnrollment = enrollment();
        bundle = bundleBuilder
            .enrollments( enrollments( invalidEnrollment, validEnrollment ) )
            .build();

        Validator<Enrollment> v1 = ( r, b, e ) -> addErrorOnMatch( r, invalidEnrollment, e, ValidationCode.E1032 );
        Validator<Enrollment> v2 = ( r, b, e ) -> addErrorOnMatch( r, invalidEnrollment, e, ValidationCode.E9999 );
        when( validators.getEnrollmentValidators() ).thenReturn( List.of( v1, v2 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 2, report.getErrors().size(), "both validators should each add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEnrollment ),
            () -> assertHasError( report, ValidationCode.E9999, invalidEnrollment ) );

        assertAll( "invalid events",
            () -> assertFalse( bundle.getEnrollments().contains( invalidEnrollment ) ),
            () -> assertTrue( bundle.getEnrollments().contains( validEnrollment ) ) );
    }

    @Test
    void skipOnErrorValidatorPreventsFurtherValidationOfInvalidEntityEvenInFullValidationModeOfEvents()
    {
        // Test shows
        // 1. Validators with skipOnError==true will prevent subsequent validators from validating an invalid entity
        // 2. DefaultValidationService removes invalid entities from the TrackerBundle
        Event validEvent = event();
        Event invalidEvent = event();

        bundle = bundleBuilder
            .events( events( invalidEvent, validEvent ) )
            .build();

        Validator<Event> skipOnError = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, Event event )
            {
                addErrorOnMatch( reporter, invalidEvent, event, ValidationCode.E1032 );
            }

            @Override
            public boolean skipOnError()
            {
                return true; // subsequent validator will not be called
            }
        };

        Validator<Event> doNotSkipOnError = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e,
            ValidationCode.E9999 );
        when( validators.getEventValidators() ).thenReturn( List.of( skipOnError, doNotSkipOnError ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 1, report.getErrors().size(), "only skip on error validator should add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEvent ) );

        assertAll( "invalid events",
            () -> assertFalse( bundle.getEvents().contains( invalidEvent ) ),
            () -> assertTrue( bundle.getEvents().contains( validEvent ) ) );
    }

    @Test
    void fullValidationModeAddsAllErrorsToReportOfEvents()
    {
        // Test shows
        // in ValidationMode==FULL all validators are called even with entities that
        // are already invalid (i.e. have an error in the validation report)
        Event validEvent = event();
        Event invalidEvent = event();

        bundle = bundleBuilder
            .events( events( invalidEvent, validEvent ) )
            .build();

        Validator<Event> v1 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E1032 );
        Validator<Event> v2 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E9999 );
        when( validators.getEventValidators() ).thenReturn( List.of( v1, v2 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 2, report.getErrors().size(), "both validators should each add 1 error" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEvent ),
            () -> assertHasError( report, ValidationCode.E9999, invalidEvent ) );

        assertAll( "invalid events",
            () -> assertFalse( bundle.getEvents().contains( invalidEvent ) ),
            () -> assertTrue( bundle.getEvents().contains( validEvent ) ) );
    }

    private static <T extends TrackerDto> void addErrorOnMatch( Reporter reporter, T expected, T actual,
        ValidationCode code )
    {
        reporter.addErrorIf( () -> Objects.equals( expected, actual ), actual, code );
    }

    @Test
    void failFastModePreventsFurtherValidationAfterFirstErrorIsAdded()
    {
        Event validEvent = event();
        Event invalidEvent = event();

        bundle = bundleBuilder
            .validationMode( ValidationMode.FAIL_FAST )
            .events( events( invalidEvent, validEvent ) )
            .build();

        Validator<Event> v1 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E1032 );
        Validator<Event> v2 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E9999 );
        when( validators.getEventValidators() ).thenReturn( List.of( v1, v2 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 1, report.getErrors().size(),
                "only first validator should add 1 error when mode is fail fast" ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEvent ) );

        assertAll( "invalid events",
            () -> assertFalse( bundle.getEvents().contains( invalidEvent ) ),
            () -> assertTrue( bundle.getEvents().contains( validEvent ) ) );
    }

    @Test
    void needsToRunPreventsValidatorExecutionOnImportStrategyDeleteByDefault()
    {
        Event invalidEvent = event();

        bundle = bundleBuilder
            .importStrategy( TrackerImportStrategy.DELETE )
            .events( events( invalidEvent ) )
            .build();
        // StrategyPreProcessor sets the ImportStrategy in the bundle for every
        // dto
        bundle.setStrategy( invalidEvent, TrackerImportStrategy.DELETE );

        Validator<Event> v1 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E1032 );
        when( validators.getEventValidators() ).thenReturn( List.of( v1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertFalse( report.hasErrors() );
    }

    @Test
    void needsToRunPreventsValidatorExecutionIfReturnsFalse()
    {
        bundle = bundleBuilder
            .events( events( event() ) )
            .build();

        Validator<Event> v1 = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, Event event )
            {
                reporter.addError( event, ValidationCode.E1000 );
            }

            @Override
            public boolean needsToRun( TrackerImportStrategy strategy )
            {
                return false; // this validator should NOT be run
            }
        };
        when( validators.getEventValidators() ).thenReturn( List.of( v1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertFalse( report.hasErrors() );
    }

    @Test
    void needsToRunExecutesHookIfReturnsTrue()
    {
        Event invalidEvent = event();
        bundle = bundleBuilder
            .events( events( invalidEvent ) )
            .build();

        Validator<Event> v1 = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, Event event )
            {
                reporter.addError( event, ValidationCode.E1032 );
            }

            @Override
            public boolean needsToRun( TrackerImportStrategy strategy )
            {
                return true; // this validator should be run
            }
        };
        when( validators.getEventValidators() ).thenReturn( List.of( v1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertHasError( report, ValidationCode.E1032, invalidEvent );
    }

    @Test
    void warningsDoNotInvalidateAndRemoveEntities()
    {
        Event validEvent = event();

        bundle = bundleBuilder
            .events( events( validEvent ) )
            .build();

        Validator<Event> v1 = ( r, b, e ) -> r.addWarning( validEvent, ValidationCode.E1120 );
        when( validators.getEventValidators() ).thenReturn( List.of( v1 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors and warnings",
            () -> assertFalse( report.hasErrors() ),
            () -> assertHasWarning( report, ValidationCode.E1120, validEvent ) );

        assertTrue( bundle.getEvents().contains( validEvent ) );
    }

    @Test
    void childEntitiesOfInvalidParentsAreStillValidated()
    {
        // Test shows
        // the children of a tracked entity will still be validated even if it
        // as a parent is invalid
        TrackedEntity invalidTrackedEntity = trackedEntity();
        Enrollment invalidEnrollment = enrollment( invalidTrackedEntity.getTrackedEntity() );
        invalidTrackedEntity.setEnrollments( enrollments( invalidEnrollment ) );
        Event invalidEvent = event();
        invalidEnrollment.setEvents( events( invalidEvent ) );

        bundle = bundleBuilder
            .validationMode( ValidationMode.FULL )
            .trackedEntities( trackedEntities( invalidTrackedEntity ) )
            .enrollments( invalidTrackedEntity.getEnrollments() )
            .events( invalidEnrollment.getEvents() )
            .build();

        Validator<TrackedEntity> v1 = ( r, b, t ) -> addErrorOnMatch( r, invalidTrackedEntity, t,
            ValidationCode.E1090 );
        Validator<Enrollment> v2 = ( r, b, e ) -> addErrorOnMatch( r, invalidEnrollment, e, ValidationCode.E1069 );
        Validator<Event> v3 = ( r, b, e ) -> addErrorOnMatch( r, invalidEvent, e, ValidationCode.E1032 );
        when( validators.getTrackedEntityValidators() ).thenReturn( List.of( v1 ) );
        when( validators.getEnrollmentValidators() ).thenReturn( List.of( v2 ) );
        when( validators.getEventValidators() ).thenReturn( List.of( v3 ) );
        service = new DefaultValidationService( validators, ruleEngineValidators );

        ValidationResult report = service.validate( bundle );

        assertAll( "errors",
            () -> assertTrue( report.hasErrors() ),
            () -> assertEquals( 3, report.getErrors().size() ),
            () -> assertHasError( report, ValidationCode.E1090, invalidTrackedEntity ),
            () -> assertHasError( report, ValidationCode.E1069, invalidEnrollment ),
            () -> assertHasError( report, ValidationCode.E1032, invalidEvent ) );

        assertAll( "persistable entities",
            () -> assertTrue( bundle.getTrackedEntities().isEmpty() ),
            () -> assertTrue( bundle.getEnrollments().isEmpty() ),
            () -> assertTrue( bundle.getEvents().isEmpty() ) );
    }

    private User superUser()
    {
        User user = mock( User.class );
        when( user.isSuper() ).thenReturn( true );
        return user;
    }

    private TrackedEntity trackedEntity()
    {
        return TrackedEntity.builder().trackedEntity( CodeGenerator.generateUid() ).build();
    }

    private Enrollment enrollment( String trackedEntity )
    {
        return Enrollment.builder().enrollment( CodeGenerator.generateUid() ).trackedEntity( trackedEntity ).build();
    }

    private Enrollment enrollment()
    {
        String trackedEntity = CodeGenerator.generateUid();
        when( preheat.exists( argThat(
            t -> t != null && t.getTrackerType() == TrackerType.TRACKED_ENTITY
                && trackedEntity.equals( t.getUid() ) ) ) )
                    .thenReturn( true );
        return Enrollment.builder().enrollment( CodeGenerator.generateUid() ).trackedEntity( trackedEntity ).build();
    }

    private Event event()
    {
        String enrollment = CodeGenerator.generateUid();
        when( preheat.exists( argThat(
            t -> t != null && t.getTrackerType() == TrackerType.ENROLLMENT && enrollment.equals( t.getUid() ) ) ) )
                .thenReturn( true );
        return Event.builder().event( CodeGenerator.generateUid() ).enrollment( enrollment ).build();
    }

    private List<TrackedEntity> trackedEntities( TrackedEntity... trackedEntities )
    {
        return List.of( trackedEntities );
    }

    private List<Enrollment> enrollments( Enrollment... enrollments )
    {
        return List.of( enrollments );
    }

    private List<Event> events( Event... events )
    {
        return List.of( events );
    }

    private TrackerBundle.TrackerBundleBuilder newBundle()
    {
        return TrackerBundle.builder().preheat( preheat ).skipRuleEngine( true );
    }
}
