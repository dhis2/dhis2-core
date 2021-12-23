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
package org.hisp.dhis.tracker.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.junit.jupiter.api.Test;

class ValidationErrorReporterTest
{

    @Test
    void hasErrorReportFound()
    {

        ValidationErrorReporter reporter = ValidationErrorReporter.emptyReporter();
        TrackerBundle bundle = mock( TrackerBundle.class );
        TrackerErrorReport error = TrackerErrorReport.builder()
            .errorCode( TrackerErrorCode.E1000 )
            .trackerType( TrackerType.EVENT )
            .build( bundle );
        reporter.getReportList().add( error );

        assertTrue( reporter.hasErrorReport( r -> TrackerType.EVENT.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasErrorReportNotFound()
    {

        ValidationErrorReporter reporter = ValidationErrorReporter.emptyReporter();
        TrackerBundle bundle = mock( TrackerBundle.class );
        TrackerErrorReport error = TrackerErrorReport.builder()
            .errorCode( TrackerErrorCode.E1000 )
            .trackerType( TrackerType.EVENT )
            .build( bundle );
        reporter.getReportList().add( error );

        assertFalse( reporter.hasErrorReport( r -> TrackerType.TRACKED_ENTITY.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasWarningReportFound()
    {

        ValidationErrorReporter reporter = ValidationErrorReporter.emptyReporter();
        TrackerBundle bundle = mock( TrackerBundle.class );
        TrackerWarningReport warning = TrackerWarningReport.builder()
            .warningCode( TrackerErrorCode.E1000 )
            .trackerType( TrackerType.EVENT )
            .build( bundle );
        reporter.getWarningsReportList().add( warning );

        assertTrue( reporter.hasWarningReport( r -> TrackerType.EVENT.equals( r.getTrackerType() ) ) );
    }

    @Test
    void hasWarningReportNotFound()
    {

        ValidationErrorReporter reporter = ValidationErrorReporter.emptyReporter();
        TrackerBundle bundle = mock( TrackerBundle.class );
        TrackerWarningReport warning = TrackerWarningReport.builder()
            .warningCode( TrackerErrorCode.E1000 )
            .trackerType( TrackerType.EVENT )
            .build( bundle );
        reporter.getWarningsReportList().add( warning );

        assertFalse( reporter.hasWarningReport( r -> TrackerType.TRACKED_ENTITY.equals( r.getTrackerType() ) ) );
    }
}