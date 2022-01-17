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

import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.hasError;
import static org.hisp.dhis.tracker.validation.hooks.AssertTrackerValidationReport.hasWarning;
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
import java.util.function.BiConsumer;

import lombok.Builder;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.AbstractTrackerDtoValidationHook;
import org.hisp.dhis.user.User;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class DefaultTrackerValidationServiceTest
{

    private DefaultTrackerValidationService service;

    private TrackerValidationHook hook1;

    private TrackerBundle bundle;

    private User user;

    void setupMocks()
    {
        user = mock( User.class );
        bundle = mock( TrackerBundle.class );
        hook1 = mock( TrackerValidationHook.class );
    }

    @Test
    void shouldNotValidateMissingUser()
    {
        setupMocks();
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.SKIP );
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        service.validate( bundle );

        verifyNoInteractions( hook1 );
    }

    @Test
    void shouldNotValidateSuperUserSkip()
    {
        setupMocks();
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( true );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.SKIP );
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        service.validate( bundle );

        verifyNoInteractions( hook1 );
    }

    @Test
    void shouldValidateSuperUserNoSkip()
    {
        setupMocks();
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( true );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1, hook2 ), Collections.emptyList() );

        service.validate( bundle );

        verify( hook1, times( 1 ) ).validate( any(), any() );
        verify( hook2, times( 1 ) ).validate( any(), any() );
    }

    @Builder
    private static class ValidationHook extends AbstractTrackerDtoValidationHook
    {
        boolean removeOnError;

        private BiConsumer<ValidationErrorReporter, Event> validateEvent;

        private BiConsumer<ValidationErrorReporter, Enrollment> validateEnrollment;

        @Override
        public void validateEvent( ValidationErrorReporter reporter, Event event )
        {
            if ( this.validateEvent != null )
            {
                this.validateEvent.accept( reporter, event );
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
        public boolean removeOnError()
        {
            return this.removeOnError;
        }
    }

    @Test
    void reportAndRemoveInvalidFromBundleWithRemoveOnErrorHook()
    {

        // Test shows
        // 1. all ValidationErrorReports created by the individual hooks
        // are merged across hooks and DTOs and returned as one report from the
        // TrackerValidationService
        // 2. the TrackerBundle is mutated to only contain valid DTOs after
        // validation
        // Currently the bundle is mutated by
        // 1. AbstractTrackerDtoValidationHook removes invalid DTOs if the hook
        // has removeOnError == true
        // 2. DefaultTrackerValidationService removes invalid DTOs if the hook
        // has removeOnError == false

        Event validEvent = event();
        Event invalidEvent = event();

        Enrollment validEnrollment = enrollment();
        Enrollment invalidEnrollment = enrollment();

        TrackerBundle bundle = TrackerBundle.builder()
            .validationMode( ValidationMode.FULL )
            .skipRuleEngine( true )
            .events( events( invalidEvent, validEvent ) )
            .enrollments( enrollments( validEnrollment, invalidEnrollment ) )
            .build();

        ValidationHook removeOnError = ValidationHook.builder()
            .removeOnError( true )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        ValidationHook doNotRemoveOnError = ValidationHook.builder()
            .removeOnError( false )
            .validateEnrollment( ( reporter, enrollment ) -> reporter
                .addErrorIf( () -> invalidEnrollment.equals( enrollment ), enrollment, TrackerErrorCode.E1069 ) )
            .build();
        TrackerValidationService validationService = new DefaultTrackerValidationService(
            List.of( removeOnError, doNotRemoveOnError ),
            Collections.emptyList() );

        TrackerValidationReport report = validationService.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 2, report.getErrors().size() );
        hasError( report, TrackerErrorCode.E1032, invalidEvent );
        hasError( report, TrackerErrorCode.E1069, invalidEnrollment );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertFalse( bundle.getEnrollments().contains( invalidEnrollment ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );
        assertTrue( bundle.getEnrollments().contains( validEnrollment ) );
    }

    @Test
    void failFastDoesNotCallHooksAfterFailure()
    {

        Event validEvent = event();
        Event invalidEvent = event();

        TrackerBundle bundle = TrackerBundle.builder()
            .validationMode( ValidationMode.FAIL_FAST )
            .skipRuleEngine( true )
            .events( events( invalidEvent, validEvent ) )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .removeOnError( true )
            .validateEvent( ( reporter, event ) -> reporter.addErrorIf( () -> invalidEvent.equals( event ), event,
                TrackerErrorCode.E1032 ) )
            .build();
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        TrackerValidationService validationService = new DefaultTrackerValidationService( List.of( hook1, hook2 ),
            Collections.emptyList() );

        TrackerValidationReport report = validationService.validate( bundle );

        assertTrue( report.hasErrors() );
        hasError( report, TrackerErrorCode.E1032, invalidEvent );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );

        verifyNoInteractions( hook2 );
    }

    @Test
    void reportWarnings()
    {

        Event validEvent = event();

        TrackerBundle bundle = TrackerBundle.builder()
            .validationMode( ValidationMode.FULL )
            .skipRuleEngine( true )
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
        TrackerValidationService validationService = new DefaultTrackerValidationService(
            List.of( hook ),
            Collections.emptyList() );

        TrackerValidationReport report = validationService.validate( bundle );

        assertFalse( report.hasErrors() );
        assertTrue( report.hasWarnings() );
        assertEquals( 1, report.getWarnings().size() );
        hasWarning( report, TrackerErrorCode.E1120, validEvent );

        assertTrue( bundle.getEvents().contains( validEvent ) );
    }

    @NotNull
    private Enrollment enrollment()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( CodeGenerator.generateUid() );
        return enrollment;
    }

    @NotNull
    private Event event()
    {
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        return event;
    }

    @NotNull
    private List<Enrollment> enrollments( Enrollment... enrollments )
    {
        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot use List.of(), Arrays.asList()
        return new ArrayList<>( Arrays.asList( enrollments ) );
    }

    @NotNull
    private List<Event> events( Event... events )
    {
        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot use List.of(), Arrays.asList()
        return new ArrayList<>( Arrays.asList( events ) );
    }

}
