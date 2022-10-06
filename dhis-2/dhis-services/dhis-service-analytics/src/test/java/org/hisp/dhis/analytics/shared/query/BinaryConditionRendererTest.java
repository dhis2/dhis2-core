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
package org.hisp.dhis.analytics.shared.query;

import static org.hisp.dhis.analytics.shared.query.Field.ofQuotedField;
import static org.hisp.dhis.common.QueryOperator.*;
import static org.hisp.dhis.common.QueryOperator.LE;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.shared.ValueTypeMapping;
import org.hisp.dhis.analytics.tei.query.QueryContext;
import org.hisp.dhis.common.QueryOperator;
import org.junit.jupiter.api.Test;

class BinaryConditionRendererTest
{

    @Test
    void testInWithSingleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer.of( ofQuotedField( "field" ), QueryOperator.IN, List.of( "value" ),
            ValueTypeMapping.STRING, queryContext ).render();
        assertEquals( "\"field\" = :1", render );
        assertEquals( "value", queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testInNVWithSingleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.IN, List.of( "NV" ), ValueTypeMapping.STRING,
                queryContext )
            .render();
        assertEquals( "\"field\" is null", render );
        assertTrue( queryContext.getParametersByPlaceHolder().isEmpty(), "value" );
    }

    @Test
    void testInWithMultipleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.IN, values, ValueTypeMapping.STRING, queryContext )
            .render();
        assertEquals( "\"field\" in (:1)", render );
        assertEquals( values, queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testInNVWithMultipleValuesProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.IN, List.of( "NV", "value" ), ValueTypeMapping.STRING,
                queryContext )
            .render();
        assertEquals( "(\"field\" is null or \"field\" in (:1))", render );
        assertEquals( "value", queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testEqWithSingleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.EQ, List.of( "value" ), ValueTypeMapping.STRING,
                queryContext )
            .render();
        assertEquals( "\"field\" = :1", render );
        assertEquals( "value", queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testEqWithNVProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.EQ, List.of( "NV" ), ValueTypeMapping.STRING,
                queryContext )
            .render();
        assertEquals( "\"field\" is null", render );
        assertTrue( queryContext.getParametersByPlaceHolder().isEmpty() );
    }

    @Test
    void testEqWithMultipleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.EQ, values, ValueTypeMapping.STRING, queryContext )
            .render();
        assertEquals( "\"field\" in (:1)", render );
        assertEquals( values, queryContext.getParametersByPlaceHolder().get( "1" ) );
    }

    @Test
    void testEqWithNVMultipleValueProduceCorrectSql()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "NV" );
        String render = BinaryConditionRenderer
            .of( ofQuotedField( "field" ), QueryOperator.EQ, values, ValueTypeMapping.STRING, queryContext )
            .render();
        assertEquals( "(\"field\" is null or \"field\" in (:1))", render );
        assertEquals( "v1", queryContext.getParametersByPlaceHolder().get( "1" ) );
        assertEquals( 1, queryContext.getParametersByPlaceHolder().size() );
    }

    @Test
    void testLikeProduceCorrectSql()
    {
        Stream.of( QueryOperator.LIKE, QueryOperator.NLIKE )
            .forEach( operator -> {
                QueryContext queryContext = QueryContext.of( null );
                String render = BinaryConditionRenderer
                    .of( ofQuotedField( "field" ), operator, List.of( "value" ), ValueTypeMapping.STRING,
                        queryContext )
                    .render();
                assertEquals( "\"field\" " + operator.getValue() + " :1", render );
                assertEquals( "%value%", queryContext.getParametersByPlaceHolder().get( "1" ) );
            } );
    }

    @Test
    void testLikeCaseInsensitiveProduceCorrectSql()
    {
        Stream.of( QueryOperator.ILIKE, QueryOperator.NILIKE )
            .forEach( operator -> {
                QueryContext queryContext = QueryContext.of( null );
                String render = BinaryConditionRenderer
                    .of( ofQuotedField( "field" ), operator, List.of( "VaLuE" ), ValueTypeMapping.STRING,
                        queryContext )
                    .render();
                assertEquals( "lower(\"field\") " + (operator == QueryOperator.ILIKE ? "like" : "not like") + " :1",
                    render );
                assertEquals( "%value%", queryContext.getParametersByPlaceHolder().get( "1" ) );
            } );
    }

    @Test
    void testLikeWithNVProduceCorrectSql()
    {
        Stream.of( QueryOperator.LIKE, QueryOperator.NLIKE, QueryOperator.ILIKE, QueryOperator.NILIKE )
            .forEach( operator -> {
                QueryContext queryContext = QueryContext.of( null );
                String render = BinaryConditionRenderer
                    .of( ofQuotedField( "field" ), operator, List.of( "NV" ), ValueTypeMapping.STRING,
                        queryContext )
                    .render();
                assertEquals( "\"field\" is null", render );
                assertTrue( queryContext.getParametersByPlaceHolder().isEmpty() );
            } );
    }

    @Test
    void testComparisonOperatorsWithBigIntegers()
    {
        Stream.of( GT, GE, LT, LE, NEQ )
            .forEach( operator -> {
                QueryContext queryContext = QueryContext.of( null );
                String render = BinaryConditionRenderer
                    .of( ofQuotedField( "field" ), operator, List.of( "100" ), ValueTypeMapping.NUMERIC, queryContext )
                    .render();
                assertEquals( "\"field\" " + operator.getValue() + " :1", render );
                assertEquals( new BigInteger( "100" ), queryContext.getParametersByPlaceHolder().get( "1" ) );
            } );
    }

    @Test
    void testComparisonOperatorsWithBigDecimal()
    {
        Stream.of( GT, GE, LT, LE, NEQ )
            .forEach( operator -> {
                QueryContext queryContext = QueryContext.of( null );
                String render = BinaryConditionRenderer
                    .of( ofQuotedField( "field" ), operator, List.of( "100.1" ), ValueTypeMapping.DECIMAL,
                        queryContext )
                    .render();
                assertEquals( "\"field\" " + operator.getValue() + " :1", render );
                assertEquals( new BigDecimal( "100.1" ), queryContext.getParametersByPlaceHolder().get( "1" ) );
            } );
    }

    @Test
    void testUnrecognizedOpThrowsIllegalArgumentException()
    {
        QueryContext queryContext = QueryContext.of( null );
        List<String> values = List.of( "v1", "v2" );
        BinaryConditionRenderer binaryConditionRenderer = BinaryConditionRenderer.of( ofQuotedField( "field" ),
            QueryOperator.EW,
            values, ValueTypeMapping.STRING, queryContext );
        IllegalArgumentException exception = assertThrows( IllegalArgumentException.class,
            binaryConditionRenderer::render );
        assertEquals( "Unimplemented operator: EW", exception.getMessage() );
    }
}
