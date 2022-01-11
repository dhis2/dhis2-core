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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultTrackerValidationServiceTest
{

    private DefaultTrackerValidationService service;

    private TrackerValidationHook hook1;

    private TrackerBundle bundle;

    private User user;

    @BeforeEach
    void setUp()
    {
        user = mock( User.class );
        bundle = mock( TrackerBundle.class );
        hook1 = mock( TrackerValidationHook.class );
    }

    @Test
    void shouldNotValidateMissingUser()
    {
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.SKIP );
        service = new DefaultTrackerValidationService( List.of( hook1 ), Collections.emptyList() );

        service.validate( bundle );

        verifyNoInteractions( hook1 );
    }

    @Test
    void shouldNotValidateSuperUserSkip()
    {
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
        when( bundle.getUser() ).thenReturn( user );
        when( user.isSuper() ).thenReturn( true );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        TrackerValidationHook hook2 = mock( TrackerValidationHook.class );
        service = new DefaultTrackerValidationService( List.of( hook1, hook2 ), Collections.emptyList() );

        service.validate( bundle );

        verify( hook1, times( 1 ) ).validate( any(), any() );
        verify( hook2, times( 1 ) ).validate( any(), any() );
    }
}
