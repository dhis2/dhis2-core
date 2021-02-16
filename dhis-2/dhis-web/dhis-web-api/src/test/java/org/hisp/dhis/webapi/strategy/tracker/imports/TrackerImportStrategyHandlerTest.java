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
package org.hisp.dhis.webapi.strategy.tracker.imports;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.TrackerBundleParams;
import org.hisp.dhis.webapi.controller.tracker.TrackerImportReportRequest;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.strategy.tracker.imports.impl.TrackerImportAsyncStrategyImpl;
import org.hisp.dhis.webapi.strategy.tracker.imports.impl.TrackerImportStrategyImpl;
import org.hisp.dhis.webapi.strategy.tracker.imports.impl.TrackerImportSyncStrategyImpl;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TrackerImportStrategyHandlerTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    TrackerImportStrategyImpl importStrategy;

    @Mock
    TrackerImportAsyncStrategyImpl importAsyncStrategy;

    @Mock
    TrackerImportSyncStrategyImpl importAsyncFalseStrategy;

    @Mock
    ContextService contextService;

    @Test
    public void shouldImportAsync()
    {
        TrackerImportReportRequest trackerImportReportRequest = TrackerImportReportRequest
            .builder()
            .contextService( contextService )
            .currentUser( new User() )
            .trackerBundleParams( TrackerBundleParams.builder().build() )
            .isAsync( true )
            .build();

        importStrategy.importReport( trackerImportReportRequest );

        verify( importAsyncStrategy ).importReport( any() );
        verify( importAsyncFalseStrategy, times( 0 ) ).importReport( any() );
    }

    @Test
    public void shouldNotImportAsync()
    {
        TrackerImportReportRequest trackerImportReportRequest = TrackerImportReportRequest
            .builder()
            .contextService( contextService )
            .currentUser( new User() )
            .trackerBundleParams( TrackerBundleParams.builder().build() )
            .build();

        importStrategy.importReport( trackerImportReportRequest );

        verify( importAsyncStrategy, times( 0 ) ).importReport( any() );
        verify( importAsyncFalseStrategy ).importReport( any() );
    }
}
