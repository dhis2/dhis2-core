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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.tracker.validation.TrackerValidationHookService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;

public class TrackerValidationHookServiceTest
{
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private TrackerValidationHookService trackerValidationHookService;

    @Test
    public void shouldSortList()
    {
        ReflectionTestUtils.setField( trackerValidationHookService, "validationOrder",
            Arrays.asList( PreCheckUidValidationHook.class, EnrollmentAttributeValidationHook.class,
                TrackedEntityAttributeValidationHook.class, EventDataValuesValidationHook.class ) );

        ReflectionTestUtils.setField( trackerValidationHookService, "validationOrderMap",
            new HashMap<Class<? extends TrackerValidationHook>, Integer>()
            {
                {
                    put( PreCheckUidValidationHook.class, 0 );
                    put( EnrollmentAttributeValidationHook.class, 1 );
                    put( TrackedEntityAttributeValidationHook.class, 2 );
                    put( EventDataValuesValidationHook.class, 3 );
                }
            } );

        List<TrackerValidationHook> validationHooks = trackerValidationHookService
            .sortValidationHooks(
                Arrays.asList( new EventDataValuesValidationHook(),
                    new TrackedEntityAttributeValidationHook( null, mock( DhisConfigurationProvider.class ) ),
                    new EnrollmentAttributeValidationHook( null, mock( DhisConfigurationProvider.class ) ),
                    new PreCheckUidValidationHook() ) );

        assertEquals( 4, validationHooks.size() );
        assertThat( validationHooks.get( 0 ), instanceOf( PreCheckUidValidationHook.class ) );
        assertThat( validationHooks.get( 1 ), instanceOf( EnrollmentAttributeValidationHook.class ) );
        assertThat( validationHooks.get( 2 ), instanceOf( TrackedEntityAttributeValidationHook.class ) );
        assertThat( validationHooks.get( 3 ), instanceOf( EventDataValuesValidationHook.class ) );
    }

    @Test
    public void shouldFilterRuleEngineValidationHooks()
    {
        ReflectionTestUtils.setField( trackerValidationHookService, "ruleEngineValidationHooks",
            Arrays.asList( EnrollmentRuleValidationHook.class, EventRuleValidationHook.class ) );

        List<TrackerValidationHook> ruleEngineValidationHooks = trackerValidationHookService
            .getRuleEngineValidationHooks(
                Arrays.asList( new EventDataValuesValidationHook(),
                    new EnrollmentRuleValidationHook(),
                    new EnrollmentAttributeValidationHook( null, mock( DhisConfigurationProvider.class ) ),
                    new EventRuleValidationHook() ) );

        assertEquals( 2, ruleEngineValidationHooks.size() );
        assertTrue( ruleEngineValidationHooks.stream().anyMatch( h -> h instanceof EnrollmentRuleValidationHook ) );
        assertTrue( ruleEngineValidationHooks.stream().anyMatch( h -> h instanceof EventRuleValidationHook ) );
    }
}
