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
package org.hisp.dhis.tracker.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import lombok.Builder;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.AbstractTrackerDtoValidationHook;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TrackerValidationServiceReportTest
{

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

        // Note: the current AbstractTrackerDtoValidationHook relies on the
        // bundle "DTO" lists to be mutable
        // it uses iterator.remove() in validateTrackerDtos()
        // which is why we cannot use List.of(), Arrays.asList()
        Event validEvent = event();
        Event invalidEvent = event();
        List<Event> events = new ArrayList<>();
        events.add( invalidEvent );
        events.add( validEvent );

        Enrollment validEnrollment = enrollment();
        Enrollment invalidEnrollment = enrollment();
        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( validEnrollment );
        enrollments.add( invalidEnrollment );

        TrackerBundle bundle = TrackerBundle.builder()
            .validationMode( ValidationMode.FULL )
            .skipRuleEngine( true )
            .events( events )
            .enrollments( enrollments )
            .build();

        ValidationHook removeOnError = ValidationHook.builder()
            .removeOnError( true )
            .validateEvent( ( reporter, event ) -> {
                if ( invalidEvent.equals( event ) )
                {
                    reporter.addError(
                        TrackerErrorReport.newReport( TrackerErrorCode.E1032 )
                            .trackerType( TrackerType.EVENT )
                            .uid( event.getUid() ) );
                }
            } )
            .build();
        ValidationHook doNotRemoveOnError = ValidationHook.builder()
            .removeOnError( false )
            .validateEnrollment( ( reporter, enrollment ) -> {
                if ( invalidEnrollment.equals( enrollment ) )
                {
                    reporter.addError(
                        TrackerErrorReport.newReport( TrackerErrorCode.E1069 )
                            .trackerType( TrackerType.ENROLLMENT )
                            .uid( enrollment.getUid() ) );
                }
            } )
            .build();
        TrackerValidationService validationService = new DefaultTrackerValidationService(
            List.of( removeOnError, doNotRemoveOnError ),
            Collections.emptyList() );

        TrackerValidationReport report = validationService.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 2, report.getErrorReports().size() );
        assertTrue( report.getErrorReports().stream().anyMatch( err -> TrackerErrorCode.E1032 == err.getErrorCode()
            && TrackerType.EVENT == err.getTrackerType()
            && invalidEvent.getUid().equals( err.getUid() ) ) );
        assertTrue( report.getErrorReports().stream().anyMatch( err -> TrackerErrorCode.E1069 == err.getErrorCode()
            && TrackerType.ENROLLMENT == err.getTrackerType()
            && invalidEnrollment.getUid().equals( err.getUid() ) ) );

        assertFalse( bundle.getEvents().contains( invalidEvent ) );
        assertFalse( bundle.getEnrollments().contains( invalidEnrollment ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );
        assertTrue( bundle.getEnrollments().contains( validEnrollment ) );
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

    @Test
    void failFastDoesNotCallHooksAfterFailure()
    {

        Event validEvent = event();
        Event invalidEvent = event();
        List<Event> events = new ArrayList<>();
        events.add( invalidEvent );
        events.add( validEvent );

        TrackerBundle bundle = TrackerBundle.builder()
            .validationMode( ValidationMode.FAIL_FAST )
            .skipRuleEngine( true )
            .events( events )
            .build();

        ValidationHook hook1 = ValidationHook.builder()
            .removeOnError( true )
            .validateEvent( ( reporter, event ) -> {
                if ( invalidEvent.equals( event ) )
                {
                    reporter.addError(
                        TrackerErrorReport.newReport( TrackerErrorCode.E1032 )
                            .trackerType( TrackerType.EVENT )
                            .uid( event.getUid() ) );
                }
            } ).build();
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        TrackerValidationService validationService = new DefaultTrackerValidationService( List.of( hook1 ),
            Collections.emptyList() );

        TrackerValidationReport report = validationService.validate( bundle );

        assertTrue( report.hasErrors() );
        assertTrue( report.getErrorReports().stream().anyMatch( err -> TrackerErrorCode.E1032 == err.getErrorCode()
            && TrackerType.EVENT == err.getTrackerType()
            && invalidEvent.getUid().equals( err.getUid() ) ) );

        // TODO(TECH-880): Is this intentional? When in FAIL_FAST mode,
        // reporter.addError() throws a FailFastException
        // which makes sure we exit early and do not call any more hooks. It
        // also leads to no call to reporter.merge()
        // which is what adds DTOs into the invalidDTO list.
        assertTrue( bundle.getEvents().contains( invalidEvent ) );
        assertTrue( bundle.getEvents().contains( validEvent ) );

        verifyNoInteractions( hook2 );
    }
}
