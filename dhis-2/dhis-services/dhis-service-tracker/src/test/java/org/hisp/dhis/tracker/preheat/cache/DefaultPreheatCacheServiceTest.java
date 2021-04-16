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
package org.hisp.dhis.tracker.preheat.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

@RunWith( MockitoJUnitRunner.class )
public class DefaultPreheatCacheServiceTest
{
    private static final String CACHE_KEY = "cache_key";

    private static final String TEI_ID = "tei_id";

    @Mock
    private DhisConfigurationProvider config;

    @Mock
    private Environment environment;

    @InjectMocks
    private DefaultPreheatCacheService serviceToTest;

    @Before
    public void setup()
    {
        Mockito.when( environment.getActiveProfiles() ).thenReturn( new String[] {} );
    }

    @Test
    @Ignore
    public void testEnabledCacheIsCachingTEI()
    {
        Mockito.when( config.isEnabled( ConfigurationKey.TRACKER_IMPORT_PREHEAT_CACHE_ENABLED ) ).thenReturn( true );

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.empty() ) );

        TrackedEntityInstance objectToCache = new TrackedEntityInstance();
        serviceToTest.put( CACHE_KEY, TEI_ID, objectToCache, 10, 10 );

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.of( objectToCache ) ) );
        assertThat( serviceToTest.getAll( CACHE_KEY ).size(), is( 1 ) );

        serviceToTest.invalidateCache();

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.empty() ) );
    }

    @Test
    @Ignore
    public void testDisabledCacheIsNotCachingTEI()
    {
        Mockito.when( config.isEnabled( ConfigurationKey.TRACKER_IMPORT_PREHEAT_CACHE_ENABLED ) ).thenReturn( false );

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.empty() ) );

        TrackedEntityInstance objectToCache = new TrackedEntityInstance();
        serviceToTest.put( CACHE_KEY, TEI_ID, objectToCache, 10, 10 );

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.empty() ) );
        assertThat( serviceToTest.getAll( CACHE_KEY ).size(), is( 0 ) );

        serviceToTest.invalidateCache();

        assertThat( serviceToTest.get( CACHE_KEY, TEI_ID ), is( Optional.empty() ) );
    }
}