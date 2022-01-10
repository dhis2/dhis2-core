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
package org.hisp.dhis.tracker.report;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.junit.jupiter.api.Test;

class TrackerValidationReportTest
{

    @Test
    void addErrorIfItDoesNotExist()
    {

        TrackerValidationReport report = new TrackerValidationReport();
        TrackerErrorReport error = newError();

        report.add( error );

        assertNotNull( report.getErrorReports() );
        assertEquals( 1, report.getErrorReports().size() );
        assertContainsOnly( report.getErrorReports(), error );

        report.add( error );

        assertEquals( 1, report.getErrorReports().size() );
    }

    @Test
    void addWarningIfItDoesNotExist()
    {

        TrackerValidationReport report = new TrackerValidationReport();
        TrackerWarningReport warning = newWarning();

        report.addWarning( warning );

        assertNotNull( report.getWarningReports() );
        assertEquals( 1, report.getWarningReports().size() );
        assertContainsOnly( report.getWarningReports(), warning );

        report.addWarning( warning );

        assertEquals( 1, report.getWarningReports().size() );
    }

    @Test
    void hasErrorsReturnsFalse()
    {

        TrackerValidationReport report = new TrackerValidationReport();

        assertFalse( report.hasErrors() );
    }

    @Test
    void hasErrorsReturnsTrue()
    {

        TrackerValidationReport report = new TrackerValidationReport();

        report.add( newError() );

        assertTrue( report.hasErrors() );
    }

    @Test
    void hasWarningsReturnsFalse()
    {

        TrackerValidationReport report = new TrackerValidationReport();

        assertFalse( report.hasWarnings() );
    }

    @Test
    void hasWarningsReturnsTrue()
    {

        TrackerValidationReport report = new TrackerValidationReport();

        report.addWarning( newWarning() );

        assertTrue( report.hasWarnings() );
    }

    @Test
    void sizeReturnsErrorCountUniqueByUid()
    {

        TrackerValidationReport report = new TrackerValidationReport();
        TrackerErrorReport error1 = newError( CodeGenerator.generateUid(), TrackerErrorCode.E1006 );
        TrackerErrorReport error2 = newError( error1.getUid(), TrackerErrorCode.E1000 );
        TrackerErrorReport error3 = newError( CodeGenerator.generateUid(), TrackerErrorCode.E1000 );

        report.add( error1 );
        report.add( error2 );
        report.add( error3 );

        assertNotNull( report.getErrorReports() );
        assertEquals( 3, report.getErrorReports().size() );
        assertEquals( 2, report.size() );
    }

    private TrackerErrorReport newError()
    {
        return TrackerErrorReport.builder()
            .uid( CodeGenerator.generateUid() )
            .errorCode( TrackerErrorCode.E1006 )
            .addArg( "missingAttribute" )
            .build( TrackerBundle.builder().build() );
    }

    private TrackerErrorReport newError( String uid, TrackerErrorCode code )
    {
        return TrackerErrorReport.builder()
            .uid( uid )
            .errorCode( code )
            .build( TrackerBundle.builder().build() );
    }

    private TrackerWarningReport newWarning()
    {
        return TrackerWarningReport.builder()
            .uid( CodeGenerator.generateUid() )
            .warningCode( TrackerErrorCode.E1006 )
            .addArg( "missingAttribute" )
            .build( TrackerBundle.builder().build() );
    }
}