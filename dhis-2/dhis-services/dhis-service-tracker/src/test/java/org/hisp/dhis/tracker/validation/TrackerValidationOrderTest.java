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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1048;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleMode;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TrackerValidationOrderTest extends DhisSpringTest
{
    @Autowired
    TrackerValidationService trackerValidationService;

    @Test
    void hooksAreExecutedInTrackerValidationConfigOrder()
    {
        // Test that hooks declared in TrackerValidationConfig validationHooks()
        // are injected
        // into the TrackerValidationService. This is important since order
        // matters in the current implementation.
        // Note that FAIL_FAST shows that although the event is also invalid due
        // to not having an orgUnit and more it
        // fails due to the first failed check which is the
        // PreCheckUidValidationHook

        Event event = new Event();
        event.setEvent( "invalidUid" );
        TrackerBundle bundle = TrackerBundle.builder()
            .importMode( TrackerBundleMode.VALIDATE )
            .validationMode( ValidationMode.FAIL_FAST )
            .skipRuleEngine( true )
            .events( Collections.singletonList( event ) )
            .build();

        TrackerValidationReport report = trackerValidationService.validate( bundle );

        assertTrue( report.hasErrors() );
        assertEquals( 1, report.getErrorReports().size() );
        assertEquals( E1048, report.getErrorReports().get( 0 ).getErrorCode() );
    }
}
