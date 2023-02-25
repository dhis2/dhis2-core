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
package org.hisp.dhis.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.hisp.dhis.antlr.ParserException;
import org.junit.jupiter.api.Test;

/**
 * @author Jim Grace
 */
class PredictorPreprocessorExpressionTest
{
    @Test
    void testPredictorPreprossorExpression()
    {
        PredictorPreprocessorExpression ppe = new PredictorPreprocessorExpression(
            "forEach ?de in :DEG:G1234567890 --> #{?de.A1234567890}" );

        // PreprocessorExpresssion properties

        assertEquals( "forEach ?de in :DEG:G1234567890 --> #{?de.A1234567890}", ppe.getExpression() );
        assertEquals( "forEach ?de in :DEG:G1234567890", ppe.getPrefix() );
        assertEquals( List.of( "forEach", "?de", "in", ":DEG:G1234567890" ), ppe.getParts() );
        assertEquals( "#{?de.A1234567890}", ppe.getMain() );

        // PredictorPreprocessorExpression properties

        assertEquals( "?de", ppe.getVariable() );
        assertEquals( ":DEG:G1234567890", ppe.getTaggedDegUid() );
        assertEquals( "G1234567890", ppe.getDegUid() );
    }

    @Test
    void testInvalidPredictorPreprosserExpression()
    {
        // bad expression

        assertEquals(
            "Couldn't find preprocessor termination ' --> ' in 'forEach ?de in :DEG:G1234567890 #{?de.A1234567890}'",
            testError( "forEach ?de in :DEG:G1234567890 #{?de.A1234567890}" ) );

        assertEquals(
            "Predictor preprocessor expression should have four parts: 'forEach ?de :DEG:G1234567890 --> #{?de.A1234567890}'",
            testError( "forEach ?de :DEG:G1234567890 --> #{?de.A1234567890}" ) );

        // bad forEach

        assertEquals(
            "Predictor preprocessor expression must start with forEach: 'forEvery ?de in :DEG:G1234567890 --> #{?de.A1234567890}'",
            testError( "forEvery ?de in :DEG:G1234567890 --> #{?de.A1234567890}" ) );

        // bad variable

        assertEquals( "Variable 'de' must start with '?' in 'forEach de in :DEG:G1234567890 --> #{de.A1234567890}'",
            testError( "forEach de in :DEG:G1234567890 --> #{de.A1234567890}" ) );

        assertEquals(
            "Variable '?d_e' must start with a letter and contain only letters and numbers in 'forEach ?d_e in :DEG:G1234567890 --> #{de.A1234567890}'",
            testError( "forEach ?d_e in :DEG:G1234567890 --> #{de.A1234567890}" ) );

        // bad in

        assertEquals( "Keyword 'in' must be the third token in 'forEach ?de on :DEG:G1234567890 --> #{de.A1234567890}'",
            testError( "forEach ?de on :DEG:G1234567890 --> #{de.A1234567890}" ) );

        // bad :DEG:G1234567890

        assertEquals(
            "Tag 'DEG:G1234567890' must start with ':DEG:' in 'forEach ?de in DEG:G1234567890 --> #{de.A1234567890}'",
            testError( "forEach ?de in DEG:G1234567890 --> #{de.A1234567890}" ) );

        assertEquals(
            "UID 'G123456789' must start with a letter and contain 10 more letters and numbers in 'forEach ?de in :DEG:G123456789 --> #{de.A1234567890}'",
            testError( "forEach ?de in :DEG:G123456789 --> #{de.A1234567890}" ) );

        assertEquals(
            "UID '12345678901' must start with a letter and contain 10 more letters and numbers in 'forEach ?de in :DEG:12345678901 --> #{de.A1234567890}'",
            testError( "forEach ?de in :DEG:12345678901 --> #{de.A1234567890}" ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Assume an {@link Exception} and return the error message.
     */
    private String testError( String expression )
    {
        try
        {
            new PredictorPreprocessorExpression( expression );
            fail();
            return null;
        }
        catch ( ParserException ex )
        {
            return ex.getMessage();
        }
    }
}
