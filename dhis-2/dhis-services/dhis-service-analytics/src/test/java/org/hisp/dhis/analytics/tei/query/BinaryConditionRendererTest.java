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
package org.hisp.dhis.analytics.tei.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.hisp.dhis.analytics.shared.ValueTypeMapping;
import org.hisp.dhis.common.QueryOperator;
import org.junit.jupiter.api.Test;

class BinaryConditionRendererTest
{

    @Test
    void testInWithSingleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( "field", QueryOperator.IN, List.of( "value" ), ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "\"field\" = :1", render );
        assertEquals( "value", queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testInWithMultipleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        String render = BinaryConditionRenderer
            .of( "field", QueryOperator.IN, values, ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "\"field\" in (:1)", render );
        assertEquals( values, queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testEqWithSingleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( "field", QueryOperator.EQ, List.of( "value" ), ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "\"field\" = :1", render );
        assertEquals( "value", queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testEqWithMultipleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        String render = BinaryConditionRenderer
            .of( "field", QueryOperator.EQ, values, ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "\"field\" in (:1)", render );
        assertEquals( values, queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testUnrecognizedOpThrowsIllegalArgumentException()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        BinaryConditionRenderer binaryConditionRenderer = BinaryConditionRenderer.of( "field", QueryOperator.LIKE,
            values, ValueTypeMapping.STRING, queryContext );
        IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
            binaryConditionRenderer::render );
        assertEquals( "Unimplemented operator: LIKE", exception.getMessage() );
    }
}
