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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.hooks.*;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;

public class DefaultTrackerValidationServiceTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultTrackerValidationService trackerValidationService;

    @Mock
    private TrackedEntityAttributeValidationHook trackedEntityAttributeValidationHook;

    @Mock
    private EventDataValuesValidationHook eventDataValuesValidationHook;

    @Mock
    private TrackerBundle bundle;

    @Mock
    private User user;

    @Before
    public void setUp()
    {
        ReflectionTestUtils.setField( trackerValidationService, "validationHooks",
            Arrays.asList( trackedEntityAttributeValidationHook, eventDataValuesValidationHook ) );
    }

    @Test
    public void shouldNotValidateMissingUser()
    {
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.SKIP );
        trackerValidationService.validate( bundle );
        verifyNoInteractions( trackedEntityAttributeValidationHook );
    }

    @Test
    public void shouldNotValidateSuperUserSkip()
    {
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( true );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.SKIP );

        trackerValidationService.validate( bundle );

        verifyNoInteractions( trackedEntityAttributeValidationHook );
    }

    @Test
    public void shouldValidateSuperUserNoSkip()
    {
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( true );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );

        when( trackedEntityAttributeValidationHook.isEnabled() ).thenReturn( true );

        when( trackedEntityAttributeValidationHook.validate( any() ) )
            .thenReturn( ValidationErrorReporter.emptyReporter() );

        trackerValidationService.validate( bundle );

        verify( trackedEntityAttributeValidationHook, times( 1 ) ).validate( any() );
    }

    @Test
    public void shouldValidateHookNoError()
    {
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( false );
        when( trackedEntityAttributeValidationHook.isEnabled() ).thenReturn( true );
        when( eventDataValuesValidationHook.isEnabled() ).thenReturn( true );

        when( trackedEntityAttributeValidationHook.validate( any() ) )
            .thenReturn( ValidationErrorReporter.emptyReporter() );
        when( eventDataValuesValidationHook.validate( any() ) )
            .thenReturn( ValidationErrorReporter.emptyReporter() );

        TrackerValidationReport validationErrorReporter = trackerValidationService.validate( bundle );

        verify( trackedEntityAttributeValidationHook, times( 1 ) ).validate( any() );
        verify( eventDataValuesValidationHook, times( 1 ) ).validate( any() );
        assertFalse( validationErrorReporter.hasErrors() );
    }

    @Test
    public void shouldValidateHookWithErrors()
    {
        when( bundle.getUser() ).thenReturn( user );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( user.isSuper() ).thenReturn( false );

        when( trackedEntityAttributeValidationHook.isEnabled() ).thenReturn( true );
        TrackerImportValidationContext trackerImportValidationContext = mock( TrackerImportValidationContext.class );
        when( trackerImportValidationContext.getBundle() ).thenReturn( bundle );

        ValidationErrorReporter validationErrorReporterReturn = new ValidationErrorReporter(
            trackerImportValidationContext );
        validationErrorReporterReturn.addError( ValidationErrorReporter.newReport( TrackerErrorCode.E1000 ) );

        when( trackedEntityAttributeValidationHook.validate( any() ) )
            .thenReturn( validationErrorReporterReturn );

        TrackerValidationReport validationErrorReporter = trackerValidationService.validate( bundle );

        verify( trackedEntityAttributeValidationHook, times( 1 ) ).validate( any() );
        assertTrue( validationErrorReporter.hasErrors() );
    }
}
