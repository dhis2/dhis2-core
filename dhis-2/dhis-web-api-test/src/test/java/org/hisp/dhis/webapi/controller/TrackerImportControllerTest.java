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
package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportController}
 * using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class TrackerImportControllerTest extends DhisControllerConvenienceTest
{
    @Test
    void shouldReturnBadRequestWhenAllValidParametersArePassed()
    {
        assertWebMessage( "OK", 200, "OK", "Tracker job added",
            POST( "/tracker?async=true&reportMode=FULL" +
                "&importMode=VALIDATE" +
                "&idScheme=ATTRIBUTE:abcdefghilm" +
                "&importStrategy=CREATE_AND_UPDATE" +
                "&atomicMode=OBJECT" +
                "&flushMode=AUTO" +
                "&validationMode=FULL",
                "{}" ).content( HttpStatus.OK ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidReportModeIsPassedGettingJobReport()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid reportMode. Valid values are: [FULL, ERRORS, WARNINGS]",
            GET( "/tracker/jobs/AAA/report?reportMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidReportModeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid reportMode. Valid values are: [FULL, ERRORS, WARNINGS]",
            POST( "/tracker?async=false&reportMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidIdSchemeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not valid for parameter idScheme. It should be of type TrackerIdSchemeParam",
            POST( "/tracker?async=false&idScheme=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidAttributeIdSchemeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value ATTRIBUTE:abc is not valid for parameter idScheme. It should be of type TrackerIdSchemeParam",
            POST( "/tracker?async=false&idScheme=ATTRIBUTE:abc", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidFormatForAttributeIdSchemeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value ATTRIBUTE:abcdefghilm:invalid is not valid for parameter idScheme. It should be of type TrackerIdSchemeParam",
            POST( "/tracker?async=false&idScheme=ATTRIBUTE:abcdefghilm:invalid", "{}" )
                .content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidImportModeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid importMode. Valid values are: [COMMIT, VALIDATE]",
            POST( "/tracker?async=false&importMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidImportStrategyIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid importStrategy. Valid values are: [CREATE, UPDATE, PATCH, CREATE_AND_UPDATE, DELETE]",
            POST( "/tracker?async=false&importStrategy=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidAtomicModeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid atomicMode. Valid values are: [ALL, OBJECT]",
            POST( "/tracker?async=false&atomicMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidFlushModeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid flushMode. Valid values are: [OBJECT, AUTO]",
            POST( "/tracker?async=false&flushMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidValidationModeIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not a valid validationMode. Valid values are: [FULL, FAIL_FAST, SKIP]",
            POST( "/tracker?async=false&validationMode=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidSkipPatternValidationIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not valid for parameter skipPatternValidation. It should be of type boolean",
            POST( "/tracker?async=false&skipPatternValidation=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidSkipSideEffectsIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not valid for parameter skipSideEffects. It should be of type boolean",
            POST( "/tracker?async=false&skipSideEffects=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }

    @Test
    void shouldReturnBadRequestWhenInvalidSkipRuleEngineIsPassed()
    {
        assertWebMessage( "Bad Request", 400, "ERROR",
            "Value INVALID is not valid for parameter skipRuleEngine. It should be of type boolean",
            POST( "/tracker?async=false&skipRuleEngine=INVALID", "{}" ).content( HttpStatus.BAD_REQUEST ) );
    }
}
