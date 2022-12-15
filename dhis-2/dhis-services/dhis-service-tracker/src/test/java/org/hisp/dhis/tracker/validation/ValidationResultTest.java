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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.junit.jupiter.api.Test;

class ValidationResultTest
{

    @Test
    void addErrorIfItDoesNotExist()
    {

        Result report = new Result();
        Error error = newError();

        report.addError( error );

        assertNotNull( report.getErrors() );
        assertEquals( 1, report.getErrors().size() );
        assertContainsOnly( List.of( error ), report.getErrors() );

        report.addError( error );

        assertEquals( 1, report.getErrors().size() );
    }

    @Test
    void addErrorsIfTheyDoNotExist()
    {

        Result report = new Result();
        Error error1 = newError( CodeGenerator.generateUid(), ValidationCode.E1001 );
        Error error2 = newError( CodeGenerator.generateUid(), ValidationCode.E1002 );

        report.addError( error1 );

        assertContainsOnly( List.of( error1 ), report.getErrors() );

        report.addErrors( List.of( error1, error2 ) );

        assertContainsOnly( List.of( error1, error2 ), report.getErrors() );
    }

    @Test
    void addWarningIfItDoesNotExist()
    {

        Result report = new Result();
        Warning warning = newWarning();

        report.addWarning( warning );

        assertNotNull( report.getWarnings() );
        assertEquals( 1, report.getWarnings().size() );
        assertContainsOnly( List.of( warning ), report.getWarnings() );

        report.addWarning( warning );

        assertEquals( 1, report.getWarnings().size() );
    }

    @Test
    void addWarningsIfTheyDoNotExist()
    {

        Result report = new Result();
        Warning warning1 = newWarning( CodeGenerator.generateUid(), ValidationCode.E1001 );
        Warning warning2 = newWarning( CodeGenerator.generateUid(), ValidationCode.E1002 );

        report.addWarning( warning1 );

        assertContainsOnly( List.of( warning1 ), report.getWarnings() );

        report.addWarnings( List.of( warning1, warning2 ) );

        assertContainsOnly( List.of( warning1, warning2 ), report.getWarnings() );
    }

    @Test
    void hasErrorsReturnsFalse()
    {

        Result report = new Result();

        assertFalse( report.hasErrors() );
    }

    @Test
    void hasErrorsReturnsTrue()
    {

        Result report = new Result();

        report.addError( newError() );

        assertTrue( report.hasErrors() );
    }

    @Test
    void hasWarningsReturnsFalse()
    {

        Result report = new Result();

        assertFalse( report.hasWarnings() );
    }

    @Test
    void hasWarningsReturnsTrue()
    {

        Result report = new Result();

        report.addWarning( newWarning() );

        assertTrue( report.hasWarnings() );
    }

    @Test
    void hasErrorReportFound()
    {

        Result report = new Result();
        Error error = newError();
        report.addError( error );

        assertTrue( report.hasError( r -> error.getUid().equals( r.getUid() ) ) );
    }

    @Test
    void hasErrorReportNotFound()
    {

        Result report = new Result();
        Error error = newError( ValidationCode.E1006 );
        report.addError( error );

        assertFalse( report.hasError( r -> Objects.equals( ValidationCode.E1048.name(), r.getCode() ) ) );
    }

    @Test
    void hasWarningReportFound()
    {

        Result report = new Result();
        Warning warning = newWarning();
        report.addWarning( warning );

        assertTrue( report.hasWarning( r -> warning.getUid().equals( r.getUid() ) ) );
    }

    @Test
    void hasWarningReportNotFound()
    {

        Result report = new Result();
        Warning warning = newWarning( ValidationCode.E1006 );
        report.addWarning( warning );

        assertFalse( report.hasWarning( r -> Objects.equals( ValidationCode.E1048.name(), r.getCode() ) ) );
    }

    @Test
    void sizeReturnsErrorCountUniqueByUid()
    {

        Result report = new Result();
        Error error1 = newError( CodeGenerator.generateUid(), ValidationCode.E1006 );
        Error error2 = newError( error1.getUid(), ValidationCode.E1000 );
        Error error3 = newError( CodeGenerator.generateUid(), ValidationCode.E1000 );

        report
            .addError( error1 )
            .addError( error2 )
            .addError( error3 );

        assertNotNull( report.getErrors() );
        assertEquals( 3, report.getErrors().size() );
        assertEquals( 2, report.size() );
    }

    private Error newError()
    {
        return newError( ValidationCode.E9999 );
    }

    private Error newError( ValidationCode code )
    {
        return newError( CodeGenerator.generateUid(), code );
    }

    private Error newError( String uid, ValidationCode code )
    {
        return new Error( "", code, TrackerType.EVENT, uid );
    }

    private Error newError( TrackerDto dto )
    {
        return new Error( "", ValidationCode.E9999, dto.getTrackerType(), dto.getUid() );
    }

    private Warning newWarning()
    {
        return newWarning( CodeGenerator.generateUid(), ValidationCode.E9999 );
    }

    private Warning newWarning( ValidationCode code )
    {
        return newWarning( CodeGenerator.generateUid(), code );
    }

    private Warning newWarning( String uid, ValidationCode code )
    {
        return new Warning( "", code, TrackerType.EVENT, uid );
    }
}