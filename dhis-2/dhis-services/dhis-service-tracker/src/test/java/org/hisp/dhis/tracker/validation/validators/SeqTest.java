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
package org.hisp.dhis.tracker.validation.validators;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1001;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1002;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1005;
import static org.hisp.dhis.tracker.validation.validators.Seq.seq;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeqTest
{
    private ValidationErrorReporter reporter;

    private TrackerBundle bundle;

    @BeforeEach
    void setUp()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .build();
        reporter = new ValidationErrorReporter( idSchemes );
        bundle = TrackerBundle.builder().build();
    }

    @Test
    void testSeqCallsUntilFirstError()
    {
        Validator<String> validator = seq( String.class,
            new MatchingValidator( E1000, "one" ), // no error so moving on to the next sequence
            seq( String.class,
                new MatchingValidator( E1001, "two" ), // input matches so no further validators are called
                seq( String.class,
                    new MatchingValidator( E1002, "two" ),
                    new MatchingValidator( E1005, "three" ) ) ) );

        validator.validate( reporter, bundle, "two" );

        assertEquals( List.of( E1001 ), actualErrors() );
    }

    /**
     * Adds an error with the {@link #code} as the error code if the input
     * equals {@link #matches}.
     */
    @RequiredArgsConstructor
    private static class MatchingValidator implements Validator<String>
    {
        private final TrackerErrorCode code;

        private final String matches;

        @Override
        public void validate( ValidationErrorReporter reporter, TrackerBundle bundle, String input )
        {
            if ( matches.equals( input ) )
            {
                reporter.addError( new TrackerErrorReport( input, code, TrackerType.ENROLLMENT, "uid" ) );
            }
        }
    }

    private List<TrackerErrorCode> actualErrors()
    {
        return reporter.getErrors().stream().map( TrackerErrorReport::getErrorCode ).collect( Collectors.toList() );
    }
}