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
package org.hisp.dhis.tracker.imports.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.sideeffect.TrackerRuleEngineSideEffect;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */
class TrackerSideEffectDataBundleTest
{

    @Test
    void testSideEffectDataBundleForEnrollment()
    {
        org.hisp.dhis.tracker.imports.domain.Enrollment enrollment = new org.hisp.dhis.tracker.imports.domain.Enrollment();
        enrollment.setEnrollment( "test-enrollment" );
        Map<String, List<TrackerRuleEngineSideEffect>> enrollmentRuleEffects = new HashMap<>();
        enrollmentRuleEffects.put( enrollment.getEnrollment(), Lists.newArrayList() );
        String enrollmentUid = CodeGenerator.generateUid();
        TrackerSideEffectDataBundle bundle = TrackerSideEffectDataBundle.builder()
            .enrollmentRuleEffects( enrollmentRuleEffects ).accessedBy( "testUser" )
            .importStrategy( TrackerImportStrategy.CREATE ).object( enrollmentUid )
            .klass( Enrollment.class ).build();
        assertEquals( enrollmentUid, bundle.getObject() );
        assertEquals( Enrollment.class, bundle.getKlass() );
        assertTrue( bundle.getEnrollmentRuleEffects().containsKey( "test-enrollment" ) );
        assertTrue( bundle.getEventRuleEffects().isEmpty() );
        assertEquals( TrackerImportStrategy.CREATE, bundle.getImportStrategy() );
        assertEquals( MessageType.TRACKER_SIDE_EFFECT, bundle.getMessageType() );
    }

    @Test
    void testSideEffectDataBundleForEvent()
    {
        org.hisp.dhis.tracker.imports.domain.Event event = new org.hisp.dhis.tracker.imports.domain.Event();
        event.setEvent( "test-event" );
        Map<String, List<TrackerRuleEngineSideEffect>> eventRuleEffects = new HashMap<>();
        eventRuleEffects.put( event.getEvent(), Lists.newArrayList() );
        Event expected = new Event();
        expected.setAutoFields();
        TrackerSideEffectDataBundle bundle = TrackerSideEffectDataBundle.builder().eventRuleEffects( eventRuleEffects )
            .object( expected.getUid() ).klass( Event.class ).build();
        assertEquals( expected.getUid(), bundle.getObject() );
        assertEquals( Event.class, bundle.getKlass() );
        assertTrue( bundle.getEventRuleEffects().containsKey( "test-event" ) );
        assertTrue( bundle.getEnrollmentRuleEffects().isEmpty() );
    }
}
