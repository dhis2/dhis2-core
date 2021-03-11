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
package org.hisp.dhis.tracker.preheat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.supplier.*;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableList;

/**
 * @author Cambi Luca
 */
public class DefaultTrackerPreheatServiceTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private ClassBasedSupplier classBasedSupplier;

    @Mock
    private ApplicationContext applicationContext;

    @Captor
    private ArgumentCaptor<Class<PreheatSupplier>> preheatSupplierClassCaptor;

    @Captor
    private ArgumentCaptor<String> bean;

    private DefaultTrackerPreheatService preheatService;

    private final TrackerImportParams preheatParams = TrackerImportParams.builder()
        .user( getUser() )
        .trackedEntities( Collections.singletonList( new TrackedEntity() ) )
        .build();

    @Before
    public void setUp()
    {
        preheatService = new DefaultTrackerPreheatService( manager, ImmutableList.of(
            ClassBasedSupplier.class.getSimpleName() ) );

        preheatService.setApplicationContext( applicationContext );
        when( manager.get( User.class, getUser().getUid() ) ).thenReturn( getUser() );
    }

    @Test
    public void shouldGetFromContextAndAdd()
    {
        when( applicationContext.getBean( bean.capture(), preheatSupplierClassCaptor.capture() ) )
            .thenReturn( classBasedSupplier );

        doCallRealMethod().when( classBasedSupplier ).add( any(), any() );

        preheatService.preheat( preheatParams );

        verify( applicationContext ).getBean( bean.getValue(), preheatSupplierClassCaptor.getValue() );
        verify( classBasedSupplier ).add( any(), any() );
        verify( classBasedSupplier ).preheatAdd( any(), any() );
    }

    @Test
    public void shouldDoNothingWhenSupplierBeanNotFound()
    {
        when( applicationContext.getBean( bean.capture(), preheatSupplierClassCaptor.capture() ) )
            .thenThrow( new BeanCreationException( "e" ) );

        preheatService.preheat( preheatParams );

        verify( applicationContext ).getBean( bean.getValue(), preheatSupplierClassCaptor.getValue() );
        verify( classBasedSupplier, times( 0 ) ).add( any(), any() );
        verify( classBasedSupplier, times( 0 ) ).preheatAdd( any(), any() );
    }

    @Test
    public void shouldDoNothingWhenAddException()
    {
        when( applicationContext.getBean( bean.capture(), preheatSupplierClassCaptor.capture() ) )
            .thenReturn( classBasedSupplier );
        doThrow( new RuntimeException( "e" ) ).when( classBasedSupplier ).add( any(), any() );

        preheatService.preheat( preheatParams );

        verify( applicationContext ).getBean( bean.getValue(), preheatSupplierClassCaptor.getValue() );
        verify( classBasedSupplier ).add( any(), any() );
    }

    private User getUser()
    {
        User user = new User();
        user.setUid( "user1234" );
        return user;
    }
}
