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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import lombok.Builder;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.AbstractTrackerDtoValidationHook;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

class DefaultTrackerValidationServiceTest
{

    private DefaultTrackerValidationService service;

    @Test
    void shouldNotValidateMissingUser()
    {
        TrackerBundle bundle = newBundle()
            .validationMode( ValidationMode.SKIP )
            .user( null )
            .build();

        TrackerValidationHook hook1 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        service.validate( bundle );

        verifyNoInteractions( hook1 );
    }

    @Test
    void shouldNotValidateSuperUserSkip()
    {
        TrackerBundle bundle = newBundle()
            .validationMode( ValidationMode.SKIP )
            .user( superUser() )
            .build();
        TrackerValidationHook hook1 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        service.validate( bundle );

        verifyNoInteractions( hook1 );
    }

    @Test
    void shouldValidateSuperUserNoSkip()
    {
        TrackerBundle bundle = newBundle()
            .validationMode( ValidationMode.FULL )
            .user( superUser() )
            .build();
        TrackerValidationHook hook1 = mock( TrackerValidationHook.class );
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1, hook2 ), Collections.emptyList() );

        service.validate( bundle );

        verify( hook1, times( 1 ) ).validate( any(), any() );
        verify( hook2, times( 1 ) ).validate( any(), any() );
    }

    private User superUser()
    {
        User user = mock( User.class );
        when( user.isSuper() ).thenReturn( true );
        return user;
    }

    @Builder
    private static class ValidationHook extends AbstractTrackerDtoValidationHook
    {
        private Boolean removeOnError;

        private Boolean needsToRun;

        private BiConsumer<ValidationErrorReporter, TrackedEntity> validateTrackedEntity;

        private BiConsumer<ValidationErrorReporter, Enrollment> validateEnrollment;

        private BiConsumer<ValidationErrorReporter, Event> validateEvent;

        @Override
        public void validateTrackedEntity( ValidationErrorReporter reporter, TrackerBundle bundle,
            TrackedEntity trackedEntity )
        {
            if ( this.validateTrackedEntity != null )
            {
                this.validateTrackedEntity.accept( reporter, trackedEntity );
            }
        }

        @Override
        public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
        {
            if ( this.validateEnrollment != null )
            {
                this.validateEnrollment.accept( reporter, enrollment );
            }
        }

        @Override
        public void validateEvent( ValidationErrorReporter reporter, TrackerBundle bundle, Event event )
        {
            if ( this.validateEvent != null )
            {
                this.validateEvent.accept( reporter, event );
            }
        }

        @Override
        public boolean removeOnError()
        {
            // using boxed Boolean, so we can test the default removeOnError
            // behavior of the AbstractTrackerDtoValidationHook
            // by default we delegate to AbstractTrackerDtoValidationHook
            return Objects.requireNonNullElseGet( this.removeOnError, super::removeOnError );
        }

        @Override
        public boolean needsToRun( TrackerImportStrategy strategy )
        {
            // using boxed Boolean, so we can test the default needsToRun
            // behavior of the AbstractTrackerDtoValidationHook
            // by default we delegate to AbstractTrackerDtoValidationHook
            return Objects.requireNonNullElseGet( this.needsToRun, () -> super.needsToRun( strategy ) );
        }
    }

    @Test
    void removeOnErrorHookPreventsFurtherValidationOfInvalidEntityEvenInFullValidationMode()
    {

        // Test shows
        // 1. Hooks with removeOnError==true remove invalid entities from the
        // TrackerBundle to prevent
        // subsequent hooks from validating it
        // 2. the TrackerBundle is mutated to only contain valid DTOs after
        // validation
        //
        // Currently, the bundle is mutated by
        // 1. AbstractTrackerDtoValidationHook removes invalid DTOs if the hook
        // has removeOnError == true
        // 2. DefaultTrackerValidationService removes invalid DTOs if they were
        // not previously removed
        // by AbstractTrackerDtoValidationHook i.e. hooks having removeOnError
        // == false

        Event validEvent = event();
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .events( events( invalidEvent, validEvent ) )
            .build();

        ValidationHook removeOnError = ValidationHook.builder()
            .removeOnError( true )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        // using default AbstractTrackerDtoValidationHook behavior of
        // removeOnError==false
        ValidationHook doNotRemoveOnError = ValidationHook.builder()
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E9999 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( removeOnError, doNotRemoveOnError ),
            Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 1, report.getErrors().size(), "only remove on error hook should add 1 error" );
        assertHasError( report, TrackerErrorCode.E1032, invalidEvent );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );
    }

    @Test
    void fullValidationModeAddsAllErrorsToReport()
    {

        // Test shows
        // in ValidationMode==FULL all hooks are called even with entities that
        // are already invalid (i.e. have an error
        // in the validation report)

        Event validEvent = event();
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .events( events( invalidEvent, validEvent ) )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .removeOnError( false )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        ValidationHook hook2 = ValidationHook.builder()
            .removeOnError( false )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E9999 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook1, hook2 ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 2, report.getErrors().size(), "both hooks should add 1 error each" );
        assertHasError( report, TrackerErrorCode.E1032, invalidEvent );
        assertHasError( report, TrackerErrorCode.E9999, invalidEvent );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );
    }

    @Test
    void failFastModePreventsFurtherValidationAfterFirstErrorIsAdded()
    {

        Event validEvent = event();
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .validationMode( ValidationMode.FAIL_FAST )
            .events( events( invalidEvent, validEvent ) )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .removeOnError( false )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1, hook2 ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertHasError( report, TrackerErrorCode.E1032, invalidEvent );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );

        verifyNoInteractions( hook2 );
    }

    @Test
    void needsToRunPreventsHookExecutionOnImportStrategyDeleteByDefault()
    {
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .importStrategy( TrackerImportStrategy.DELETE )
            .events( events( invalidEvent ) )
            .build();
        // StrategyPreProcessor sets the ImportStrategy in the bundle for every
        // dto
        bundle.setStrategy( invalidEvent, TrackerImportStrategy.DELETE );

        ValidationHook hook1 = ValidationHook.builder()
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertFalse( report.hasErrors() );
    }

    @Test
    void needsToRunPreventsHookExecutionIfReturnsFalse()
    {
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .events( events( invalidEvent ) )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .needsToRun( false )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertFalse( report.hasErrors() );
    }

    @Test
    void needsToRunExecutesHookIfReturnsTrue()
    {
        Event invalidEvent = event();

        TrackerBundle bundle = newBundle()
            .events( events( invalidEvent ) )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .needsToRun( true )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertHasError( report, TrackerErrorCode.E1032, invalidEvent );
    }

    @Test
    void warningsDoNotInvalidateAndRemoveEntities()
    {

        Event validEvent = event();

        TrackerBundle bundle = newBundle()
            .events( events( validEvent ) )
            .build();

        ValidationHook hook = ValidationHook.builder()
            .validateEvent( ( reporter, event ) -> {
                if ( validEvent.equals( event ) )
                {
                    reporter.addWarning( event, TrackerErrorCode.E1120 );
                }
            } )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertFalse( report.hasErrors() );
        assertTrue( report.hasWarnings() );
        assertEquals( 1, report.getWarnings().size() );
        assertHasWarning( report, TrackerErrorCode.E1120, validEvent );

        assertTrue( bundle.getEvents().contains( validEvent ) );
    }

    @Test
    void childEntitiesOfInvalidParentsAreStillValidated()
    {

        // Test shows
        // the children of a tracked entity will still be validated even if it
        // as a parent is invalid

        TrackedEntity invalidTrackedEntity = trackedEntity();
        Enrollment invalidEnrollment = enrollment();
        invalidTrackedEntity.setEnrollments( enrollments( invalidEnrollment ) );
        Event invalidEvent = event();
        invalidEnrollment.setEvents( events( invalidEvent ) );

        TrackerBundle bundle = newBundle()
            .validationMode( ValidationMode.FULL )
            .trackedEntities( trackedEntities( invalidTrackedEntity ) )
            .enrollments( invalidTrackedEntity.getEnrollments() )
            .events( invalidEnrollment.getEvents() )
            .build();

        ValidationHook hook = ValidationHook.builder()
            .validateTrackedEntity(
                ( reporter, te ) -> reporter.addErrorIf( () -> invalidTrackedEntity.equals( te ), te,
                    TrackerErrorCode.E1090 ) )
            .validateEnrollment( ( reporter, enrollment ) -> reporter.addErrorIf(
                () -> invalidEnrollment.equals( enrollment ), enrollment,
                TrackerErrorCode.E1069 ) )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        service = new DefaultTrackerValidationService( List.of( hook ), Collections.emptyList() );

        TrackerValidationReport report = service.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 3, report.getErrors().size() );
        assertHasError( report, TrackerErrorCode.E1090, invalidTrackedEntity );
        assertHasError( report, TrackerErrorCode.E1069, invalidEnrollment );
        assertHasError( report, TrackerErrorCode.E1032, invalidEvent );

        assertTrue( bundle.getTrackedEntities().isEmpty() );
        assertTrue( bundle.getEnrollments().isEmpty() );
        assertTrue( bundle.getEvents().isEmpty() );
    }

    private TrackedEntity trackedEntity()
    {
        return TrackedEntity.builder().trackedEntity( CodeGenerator.generateUid() ).build();
    }

    private Enrollment enrollment()
    {
        return Enrollment.builder().enrollment( CodeGenerator.generateUid() ).build();
    }

    private Event event()
    {
        return Event.builder().event( CodeGenerator.generateUid() ).build();
    }

    private List<TrackedEntity> trackedEntities( TrackedEntity... trackedEntities )
    {
        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot simply use List.of(), Arrays.asList()
        return new ArrayList<>( Arrays.asList( trackedEntities ) );
    }

    private List<Enrollment> enrollments( Enrollment... enrollments )
    {
        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot simply use List.of(), Arrays.asList()
        return new ArrayList<>( Arrays.asList( enrollments ) );
    }

    private List<Event> events( Event... events )
    {
        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot simply use List.of(), Arrays.asList()
        return new ArrayList<>( Arrays.asList( events ) );
    }

    private TrackerBundle.TrackerBundleBuilder newBundle()
    {
        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setIdSchemes( TrackerIdSchemeParams.builder().build() );
        return TrackerBundle.builder().preheat( preheat ).skipRuleEngine( true );
    }
}
