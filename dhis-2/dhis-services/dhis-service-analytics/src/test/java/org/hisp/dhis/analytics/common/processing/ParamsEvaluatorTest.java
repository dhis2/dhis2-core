/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.GridHeader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ParamsEvaluator}.
 *
 * @author maikel arabori
 */
class ParamsEvaluatorTest
{
    private static ParamsEvaluator paramsEvaluator;

    @BeforeAll
    static void setUp()
    {
        paramsEvaluator = new ParamsEvaluator();
    }

    @Test
    void testApplyHeadersWithSuccess()
    {
        // Given
        Set<GridHeader> allHeaders = Set.of( getGridHeader( "n1", "c1" ), getGridHeader( "n2", "c2" ) );
        Set<String> paramHeaders = Set.of( "n2" );

        // When
        Set<GridHeader> resultHeaders = paramsEvaluator.applyHeaders( allHeaders, paramHeaders );

        // Then
        assertSame( paramHeaders.stream().findFirst().get(), resultHeaders.stream().findFirst().get().getName() );
        assertEquals( 1, resultHeaders.size() );
        assertEquals( 2, allHeaders.size() );
        assertEquals( 1, paramHeaders.size() );
    }

    @Test
    void testApplyHeadersWithSuccessWhenTheyHaveTheSameNames()
    {
        // Given
        Set<GridHeader> allHeaders = new LinkedHashSet<>();
        allHeaders.add( getGridHeader( "n1", "c1" ) );
        allHeaders.add( getGridHeader( "n2", "c2" ) );

        Set<String> paramHeaders = new LinkedHashSet<>();
        paramHeaders.add( "n2" );
        paramHeaders.add( "n1" );

        // When
        Set<GridHeader> resultHeaders = paramsEvaluator.applyHeaders( allHeaders, paramHeaders );

        // Then
        assertSame( "n2", resultHeaders.stream().collect( Collectors.toList() ).get( 0 ).getName() );
        assertSame( "n1", resultHeaders.stream().collect( Collectors.toList() ).get( 1 ).getName() );
        assertEquals( 2, resultHeaders.size() );
        assertEquals( 2, allHeaders.size() );
        assertEquals( 2, paramHeaders.size() );
    }

    @Test
    void testApplyHeadersWithEmptyParamHeaders()
    {
        // Given
        Set<GridHeader> allHeaders = Set.of( getGridHeader( "n1", "c1" ) );
        Set<String> emptyParamHeaders = emptySet();

        // When
        Set<GridHeader> resultHeaders = paramsEvaluator.applyHeaders( allHeaders, emptyParamHeaders );

        // Then
        assertSame( allHeaders, resultHeaders );
    }

    @Test
    void testApplyHeadersWithEmptyHeaders()
    {
        // Given
        Set<GridHeader> allHeaders = emptySet();
        Set<String> anyParamHeaders = Set.of( "n2" );

        // When
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> paramsEvaluator.applyHeaders( allHeaders, anyParamHeaders ),
            "Expected exception not thrown: applyHeaders()" );

        // Then
        assertTrue( ex.getMessage().contains( "The 'headers' must not be null/empty" ) );
    }

    private GridHeader getGridHeader( String name, String column )
    {
        return new GridHeader( name, column );
    }
}
