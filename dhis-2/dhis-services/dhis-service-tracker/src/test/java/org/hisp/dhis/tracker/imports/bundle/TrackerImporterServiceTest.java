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
package org.hisp.dhis.tracker.imports.bundle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.imports.DefaultTrackerImportService;
import org.hisp.dhis.tracker.imports.ParamsConverter;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerUserService;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.validation.ValidationResult;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar
 */
@ExtendWith( MockitoExtension.class )
class TrackerImporterServiceTest
{

    @Mock
    private TrackerBundleService trackerBundleService;

    @Mock
    private ValidationService validationService;

    @Mock
    private TrackerPreprocessService trackerPreprocessService;

    @Mock
    private TrackerUserService trackerUserService;

    @Mock
    private Notifier notifier;

    @Mock
    private ValidationResult validationResult;

    private DefaultTrackerImportService subject;

    private TrackerImportParams params = null;

    private final BeanRandomizer rnd = BeanRandomizer.create();

    @BeforeEach
    public void setUp()
    {
        subject = new DefaultTrackerImportService( trackerBundleService, validationService,
            trackerPreprocessService,
            trackerUserService, notifier );

        final List<Event> events = rnd.objects( Event.class, 3 ).collect( Collectors.toList() );

        params = TrackerImportParams.builder()
            .events( events )
            .enrollments( new ArrayList<>() )
            .relationships( new ArrayList<>() )
            .trackedEntities( new ArrayList<>() )
            .userId( "123" )
            .build();

        PersistenceReport persistenceReport = PersistenceReport.emptyReport();
        when( trackerUserService.getUser( anyString() ) ).thenReturn( getUser() );

        when( trackerBundleService.commit( any( TrackerBundle.class ) ) ).thenReturn( persistenceReport );

        when( validationService.validate( any( TrackerBundle.class ) ) )
            .thenReturn( validationResult );
        when( validationService.validateRuleEngine( any( TrackerBundle.class ) ) )
            .thenReturn( validationResult );
        when( trackerPreprocessService.preprocess( any( TrackerBundle.class ) ) )
            .thenReturn( ParamsConverter.convert( params ) );
    }

    @Test
    void testSkipSideEffect()
    {
        TrackerImportParams parameters = TrackerImportParams.builder()
            .events( params.getEvents() )
            .enrollments( new ArrayList<>() )
            .relationships( new ArrayList<>() )
            .trackedEntities( new ArrayList<>() )
            .skipSideEffects( true )
            .userId( "123" )
            .build();

        when( trackerBundleService.create( any( TrackerImportParams.class ) ) )
            .thenReturn( ParamsConverter.convert( parameters ) );

        subject.importTracker( parameters );

        verify( trackerBundleService, times( 0 ) ).handleTrackerSideEffects( anyList() );
    }

    @Test
    void testWithSideEffects()
    {
        doAnswer( invocationOnMock -> null ).when( trackerBundleService ).handleTrackerSideEffects( anyList() );
        when( trackerBundleService.create( any( TrackerImportParams.class ) ) )
            .thenReturn( ParamsConverter.convert( params ) );

        subject.importTracker( params );

        verify( trackerBundleService, times( 1 ) ).handleTrackerSideEffects( anyList() );
    }

    private User getUser()
    {
        User user = new User();
        user.setUid( "user1234" );
        return user;
    }
}
