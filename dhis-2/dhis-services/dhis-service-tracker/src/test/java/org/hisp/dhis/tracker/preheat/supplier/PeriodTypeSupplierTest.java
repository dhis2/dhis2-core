package org.hisp.dhis.tracker.preheat.supplier;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.cache.DefaultPreheatCacheService;
import org.hisp.dhis.tracker.preheat.cache.PreheatCacheService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.env.Environment;

/**
 * @author Luciano Fiandesio
 */
public class PeriodTypeSupplierTest
{
    private PeriodTypeSupplier supplier;

    @Mock
    private PeriodStore periodStore;

    @Mock
    private DhisConfigurationProvider conf;

    @Mock
    private Environment env;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Before
    public void setUp()
    {
        final PreheatCacheService cache = new DefaultPreheatCacheService( conf, env );
        supplier = new PeriodTypeSupplier( periodStore, cache );
        when( env.getActiveProfiles() ).thenReturn( new String[] {} );
    }

    @Test
    public void verifySupplier()
    {
        final List<Period> periods = rnd.randomObjects( Period.class, 20 );
        when( periodStore.getAll() ).thenReturn( periods );

        final TrackerImportParams params = TrackerImportParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        assertThat( preheat.getPeriodMap().values(), hasSize( 20 ) );
    }
}