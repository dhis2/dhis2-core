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
package org.hisp.dhis.tracker.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.junit.jupiter.api.Test;

class ReporterTest
{

    @Test
    void hasErrorReportFound()
    {

        Reporter reporter = new Reporter( TrackerIdSchemeParams.builder().build() );
        reporter.addError( eventError() );

        assertTrue( reporter.hasErrorReport( r -> TrackerType.EVENT.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasErrorReportNotFound()
    {

        Reporter reporter = new Reporter( TrackerIdSchemeParams.builder().build() );
        reporter.addError( eventError() );

        assertFalse( reporter.hasErrorReport( r -> TrackerType.TRACKED_ENTITY.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasWarningReportFound()
    {

        Reporter reporter = new Reporter( TrackerIdSchemeParams.builder().build() );
        reporter.addWarning( eventWarning() );

        assertTrue( reporter.hasWarningReport( r -> TrackerType.EVENT.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasWarningReportNotFound()
    {

        Reporter reporter = new Reporter( TrackerIdSchemeParams.builder().build() );
        reporter.addWarning( eventWarning() );

        assertFalse( reporter.hasWarningReport( r -> TrackerType.TRACKED_ENTITY.equals( r.getTrackerType() ) ) );
    }

    private Error eventError()
    {
        return new Error( "some error", ValidationCode.E1000, TrackerType.EVENT, "JgDfHAGzzfS" );
    }

    private Warning eventWarning()
    {
        return new Warning( "some warning", ValidationCode.E1000, TrackerType.EVENT, "JgDfHAGzzfS" );
    }
}