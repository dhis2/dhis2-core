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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;

import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.validation.Error;
import org.hisp.dhis.tracker.validation.Validation;
import org.hisp.dhis.tracker.validation.ValidationResult;

public class AssertTrackerValidationReport
{

    public static void assertHasError( ValidationResult report, TrackerErrorCode code, TrackerDto dto )
    {
        assertHasError( report, code, dto.getTrackerType(), dto.getUid() );
    }

    public static void assertHasError( ValidationResult report, TrackerErrorCode code, TrackerType type,
        String uid )
    {
        assertHasError( report.getErrors(), code, type, uid );
    }

    public static void assertHasError( List<Validation> errors, TrackerErrorCode code, TrackerType type,
        String uid )
    {
        assertNotEmpty( errors );
        assertTrue( errors.stream().anyMatch( err -> Objects.equals( code.name(), err.getCode() ) &&
            Objects.equals( type.name(), err.getType() ) &&
            uid.equals( err.getUid() ) ),
            String.format( "error with code %s, type %s, uid %s not found in error(s) %s", code,
                type, uid, errors ) );
    }

    public static void assertHasError( List<Error> errors, TrackerErrorCode code, TrackerType type,
        String uid, String messageContains )
    {
        assertNotEmpty( errors );
        assertTrue( errors.stream().anyMatch( err -> code == err.getErrorCode() &&
            type == err.getTrackerType() &&
            uid.equals( err.getUid() ) &&
            err.getMessage().contains( messageContains ) ),
            String.format( "error with code %s, type %s, uid %s and partial message '%s' not found in error(s) %s",
                code,
                type, uid, messageContains, errors ) );
    }

    public static void assertHasWarning( ValidationResult result, TrackerErrorCode code, TrackerDto dto )
    {
        assertHasWarning( result, code, dto.getTrackerType(), dto.getUid() );
    }

    public static void assertHasWarning( ValidationResult result, TrackerErrorCode code, TrackerType type,
        String uid )
    {
        assertTrue( result.hasWarnings(), "warning not found since report has no warnings" );
        assertTrue( result.hasWarning( warning -> Objects.equals( code.name(), warning.getCode() ) &&
            Objects.equals( type.name(), warning.getType() ) &&
            uid.equals( warning.getUid() ) ),
            String.format( "warning with code %s, type %s, uid %s not found in report with warnings(s) %s", code, type,
                uid, result.getWarnings() ) );
    }
}
