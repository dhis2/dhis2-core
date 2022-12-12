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

import static org.hisp.dhis.tracker.validation.validators.All.all;
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

class AllTest
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
    void testAllAreCalled()
    {
        Validator<String> validator = all( String.class,
            new StringValidator( "one" ),
            new StringValidator( "two" ),
            all( String.class,
                new StringValidator( "three" ),
                new StringValidator( "four" ),
                all( String.class,
                    new StringValidator( "five" ) ) ) );

        validator.validate( reporter, bundle, "input" );

        assertEquals( List.of( "one", "two", "three", "four", "five" ), actualErrors() );
    }

    /**
     * Adds an error with {@link #error} message irrespective of the input.
     */
    @RequiredArgsConstructor
    private static class StringValidator implements Validator<String>
    {

        private final String error;

        @Override
        public void validate( ValidationErrorReporter reporter, TrackerBundle bundle, String input )
        {
            reporter.addError( new TrackerErrorReport( error, TrackerErrorCode.E9999, TrackerType.ENROLLMENT, "uid" ) );
        }
    }

    private List<String> actualErrors()
    {
        return reporter.getErrors().stream().map( TrackerErrorReport::getMessage ).collect( Collectors.toList() );
    }
}