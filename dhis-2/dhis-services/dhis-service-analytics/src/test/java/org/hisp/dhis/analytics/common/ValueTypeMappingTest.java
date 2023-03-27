/*
 * Copyright (c) 2004-2004, University of Oslo
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
package org.hisp.dhis.analytics.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.DECIMAL;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.NUMERIC;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.STRING;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.TEXT;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ValueTypeMapping}.
 */
class ValueTypeMappingTest
{
    @Test
    void testConvertSingleNumeric()
    {
        Object o = NUMERIC.convertSingle( "1" );
        assertThat( o, is( instanceOf( BigInteger.class ) ) );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Test
    void testConvertMultipleNumeric()
    {
        Object o = NUMERIC.convertMany( List.of( "1", "2", "3" ) );
        assertThat( o, is( instanceOf( List.class ) ) );
        ((List) o).forEach( e -> assertThat( e, is( instanceOf( BigInteger.class ) ) ) );
    }

    @Test
    void testConvertSingleDecimal()
    {
        Object o = DECIMAL.convertSingle( "1.1" );
        assertThat( o, is( instanceOf( BigDecimal.class ) ) );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Test
    void testConvertMultipleDecimal()
    {
        Object o = DECIMAL.convertMany( List.of( "1", "2", "3" ) );
        assertThat( o, is( instanceOf( List.class ) ) );
        ((List) o).forEach( e -> assertThat( e, is( instanceOf( BigDecimal.class ) ) ) );
    }

    @Test
    void testConvertSingleString()
    {
        Object o = STRING.convertSingle( "1" );
        assertThat( o, is( instanceOf( String.class ) ) );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Test
    void testConvertMultipleString()
    {
        Object o = STRING.convertMany( List.of( "1", "2", "3" ) );
        assertThat( o, is( instanceOf( List.class ) ) );
        ((List) o).forEach( e -> assertThat( e, is( instanceOf( String.class ) ) ) );
    }

    @Test
    void testConvertSingleText()
    {
        Object o = TEXT.convertSingle( "1" );
        assertThat( o, is( instanceOf( String.class ) ) );
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Test
    void testConvertMultipleText()
    {
        Object o = TEXT.convertMany( List.of( "1", "2", "3" ) );
        assertThat( o, is( instanceOf( List.class ) ) );
        ((List) o).forEach( e -> assertThat( e, is( instanceOf( String.class ) ) ) );
    }
}
