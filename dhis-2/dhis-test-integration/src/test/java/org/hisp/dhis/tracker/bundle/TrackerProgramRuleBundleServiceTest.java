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
package org.hisp.dhis.tracker.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerProgramRuleBundleServiceTest extends TrackerTest
{

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActionService;

    @Override
    protected void initTest()
        throws IOException
    {
        ObjectBundle bundle = setUpMetadata( "tracker/event_metadata.json" );
        ProgramRule programRule = createProgramRule( 'A',
            bundle.getPreheat().get( PreheatIdentifier.UID, Program.class, "BFcipDERJwr" ) );
        programRuleService.addProgramRule( programRule );
        ProgramRuleAction programRuleAction = createProgramRuleAction( 'A', programRule );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleActionService.addProgramRuleAction( programRuleAction );
        programRule.getProgramRuleActions().add( programRuleAction );
        programRuleService.updateProgramRule( programRule );
    }

    @Test
    void testRunRuleEngineForEventOnBundleCreate()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/event_events_and_enrollment.json" );
        assertEquals( 8, trackerImportParams.getEvents().size() );
        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );
        trackerBundle = trackerBundleService.runRuleEngine( trackerBundle );
        assertEquals( trackerBundle.getEvents().size(), trackerBundle.getEventRuleEffects().size() );
    }
}
