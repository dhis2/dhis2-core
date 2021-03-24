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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TrackedEntityTypeObjectBundleHookTest
{

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    private TrackedEntityTypeObjectBundleHook trackedEntityTypeObjectBundleHook;

    @Mock
    private ObjectBundle bundle;

    @Mock
    private Preheat preheat;

    private static TrackedEntityType trackedEntityType;

    private static TrackedEntityAttribute trackedEntityAttribute;

    @Before
    public void setUp()
    {
        trackedEntityType = new TrackedEntityType();

        when( bundle.getPreheat() ).thenReturn( preheat );
        when( bundle.getPreheatIdentifier() ).thenReturn( PreheatIdentifier.UID, PreheatIdentifier.UID );
    }

    @Test
    public void shouldReportNoErrorObjectIsNull()
    {

        assertEquals( 0, trackedEntityTypeObjectBundleHook.validate( null, bundle ).size() );
        verify( bundle, times( 0 ) ).getPreheat();
    }

    @Test
    public void shouldReportNoErrorDifferentClass()
    {

        assertEquals( 0, trackedEntityTypeObjectBundleHook.validate( new User(), bundle ).size() );
        verify( bundle, times( 0 ) ).getPreheat();
    }

    @Test
    public void shouldReportNoErrorTetHasNoTeas()
    {

        assertEquals( 0, trackedEntityTypeObjectBundleHook.validate( trackedEntityType, bundle ).size() );
        verify( bundle, times( 0 ) ).getPreheat();
    }

    @Test
    public void shouldReportNoErrorTeaExists()
    {

        when( preheat.get( any(), any() ) ).thenReturn( new TrackedEntityAttribute() );

        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setTrackedEntityType( trackedEntityType );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( new TrackedEntityAttribute() );

        trackedEntityType.setTrackedEntityTypeAttributes( Arrays.asList( trackedEntityTypeAttribute, null ) );

        assertEquals( 0, trackedEntityTypeObjectBundleHook.validate( trackedEntityType, bundle ).size() );
        verify( bundle, times( 1 ) ).getPreheat();
    }

    @Test
    public void shouldReportErrorTeaNotExists()
    {

        trackedEntityAttribute = new TrackedEntityAttribute();
        trackedEntityAttribute.setUid( "teaUid" );

        when( preheat.get( any(), any() ) ).thenReturn( null );

        TrackedEntityTypeAttribute trackedEntityTypeAttribute = new TrackedEntityTypeAttribute();
        trackedEntityTypeAttribute.setTrackedEntityType( trackedEntityType );
        trackedEntityTypeAttribute.setTrackedEntityAttribute( new TrackedEntityAttribute() );

        trackedEntityType.setTrackedEntityTypeAttributes( Collections.singletonList( trackedEntityTypeAttribute ) );

        assertEquals( 1, trackedEntityTypeObjectBundleHook.validate( trackedEntityType, bundle ).size() );
        verify( bundle, times( 1 ) ).getPreheat();
    }
}
