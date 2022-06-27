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

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClient.ContentType;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.event.ProgramIndicatorController}
 * using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class ProgramIndicatorControllerTest extends DhisControllerConvenienceTest
{

    @Test
    void testGetExpressionDescription()
    {
        assertWebMessage( "OK", 200, "OK", "Valid",
            POST( "/programIndicators/expression/description", Body( "70" ), ContentType( TEXT_PLAIN_VALUE ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testGetExpressionDescription_MalformedExpression()
    {
        assertWebMessage( "OK", 200, "ERROR", "Expression is not valid",
            POST( "/programIndicators/filter/description", Body( "illegal" ), ContentType( TEXT_PLAIN_VALUE ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testValidateFilter()
    {
        assertWebMessage( "OK", 200, "OK", "Valid",
            POST( "/programIndicators/filter/description", Body( "1 < 2" ), ContentType( TEXT_PLAIN_VALUE ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testValidateFilter_MalformedExpression()
    {
        assertWebMessage( "OK", 200, "ERROR", "Expression is not valid",
            POST( "/programIndicators/filter/description", Body( "illegal" ), ContentType( TEXT_PLAIN_VALUE ) )
                .content( HttpStatus.OK ) );
    }

    @Test
    void testValidateFilterJson()
    {
        assertWebMessage( "OK", 200, "OK", "Valid",
            POST( "/programIndicators/filter/description", "{ 'expression': '1 < 2' }" ).content( HttpStatus.OK ) );
    }

    @Test
    void testGetJsonExpressionDescription()
    {
        assertWebMessage( "OK", 200, "OK", "Valid",
            POST( "/programIndicators/expression/description", "{ 'expression': 70 }" ).content( HttpStatus.OK ) );
    }
}
