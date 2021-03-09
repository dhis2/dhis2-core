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
package org.hisp.dhis.tracker.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */
public class TrackerSideEffectDataBundleTest
{
    @Test
    public void testSideEffectDataBundleForEnrollment()
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollment( "test-enrollment" );

        Map<String, List<RuleEffect>> enrollmentRuleEffects = new HashMap<>();
        enrollmentRuleEffects.put( enrollment.getEnrollment(), Lists.newArrayList() );

        ProgramInstance programInstance = new ProgramInstance();

        TrackerSideEffectDataBundle bundle = TrackerSideEffectDataBundle.builder()
            .enrollmentRuleEffects( enrollmentRuleEffects )
            .accessedBy( "testUser" )
            .importStrategy( TrackerImportStrategy.CREATE )
            .object( programInstance )
            .klass( ProgramInstance.class )
            .build();

        assertEquals( programInstance, bundle.getObject() );
        assertEquals( ProgramInstance.class, bundle.getKlass() );
        assertTrue( bundle.getEnrollmentRuleEffects().containsKey( "test-enrollment" ) );
        assertTrue( bundle.getEventRuleEffects().isEmpty() );
        assertEquals( TrackerImportStrategy.CREATE, bundle.getImportStrategy() );
        assertEquals( MessageType.TRACKER_SIDE_EFFECT, bundle.getMessageType() );
    }

    @Test
    public void testSideEffectDataBundleForEvent()
    {
        Event event = new Event();
        event.setEvent( "test-event" );

        Map<String, List<RuleEffect>> eventRuleEffects = new HashMap<>();
        eventRuleEffects.put( event.getEvent(), Lists.newArrayList() );

        ProgramStageInstance programStageInstance = new ProgramStageInstance();

        TrackerSideEffectDataBundle bundle = TrackerSideEffectDataBundle.builder()
            .eventRuleEffects( eventRuleEffects )
            .object( programStageInstance )
            .klass( ProgramStageInstance.class )
            .build();

        assertEquals( programStageInstance, bundle.getObject() );
        assertEquals( ProgramStageInstance.class, bundle.getKlass() );
        assertTrue( bundle.getEventRuleEffects().containsKey( "test-event" ) );
        assertTrue( bundle.getEnrollmentRuleEffects().isEmpty() );
    }
}
