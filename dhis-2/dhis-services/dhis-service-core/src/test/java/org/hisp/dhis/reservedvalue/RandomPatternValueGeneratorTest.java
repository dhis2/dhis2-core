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
package org.hisp.dhis.reservedvalue;

import static org.hisp.dhis.reservedvalue.RandomPatternValueGenerator.generateRandomValues;
import static org.hisp.dhis.util.Constants.RANDOM_GENERATION_CHUNK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link RandomPatternValueGenerator} random value generation.
 *
 * @author Jan Bernitt
 */
class RandomPatternValueGeneratorTest
{
    @Test
    void shouldGenerateRandomInteger()
    {
        generatedValuesShouldMatchPattern( "#", RANDOM_GENERATION_CHUNK );
    }

    @Test
    void shouldGenerateRandomIntegers()
    {
        generatedValuesShouldMatchPattern( "#####", RANDOM_GENERATION_CHUNK * 2 + 1 );
    }

    @Test
    void shouldGenerateRandomLowerCases()
    {
        generatedValuesShouldMatchPattern( "xxxxx", RANDOM_GENERATION_CHUNK );
    }

    @Test
    void shouldGenerateRandomUpperCases()
    {
        generatedValuesShouldMatchPattern( "XXXXX", RANDOM_GENERATION_CHUNK );
    }

    @Test
    void shouldGenerateRandomMixedUpperLoweCaseLetters()
    {
        generatedValuesShouldMatchPattern( "xxxXXX", RANDOM_GENERATION_CHUNK );
    }

    @Test
    void shouldGenerateRandomMixedLettersAndIntegers()
    {
        generatedValuesShouldMatchPattern( "xx###", RANDOM_GENERATION_CHUNK * 3 );
    }

    @Test
    void shouldGenerateRandomAll()
    {
        generatedValuesShouldMatchPattern( "***XXX###", RANDOM_GENERATION_CHUNK * 3 );
    }

    @Test
    void shouldGenerateMixed()
    {
        generatedValuesShouldMatchPattern( "XXxx##*", RANDOM_GENERATION_CHUNK * 3 );
    }

    @Test
    void shouldGenerateFromLongSegment()
    {
        generatedValuesShouldMatchPattern( "x".repeat( 50 ), RANDOM_GENERATION_CHUNK );
    }

    /**
     * Both a performance and a quality of randomness test. This should be fast
     * and still produce decent randomness.
     */
    @Test
    void shouldGenerateSufficientlyDifferentValues()
    {
        int n = 100000;
        List<String> values = generateRandomValues( "xxXX##**xxXX", n );
        assertTrue( new HashSet<>( values ).size() > values.size() / 2,
            "at least half the values should be unique" );
    }

    /**
     * Generates values matching the pattern and asserts the generated values
     * match.
     *
     * @param pattern generation and check pattern
     * @param expectedCount number of expected generated values
     */
    private void generatedValuesShouldMatchPattern( String pattern, int expectedCount )
    {
        assertMatchesPattern( pattern, expectedCount,
            generateRandomValues( pattern, RANDOM_GENERATION_CHUNK ) );
    }

    private void assertMatchesPattern( String expectedPattern, int expectedCount, List<String> actualValues )
    {
        for ( String actualValue : actualValues )
        {
            int length = expectedPattern.length();
            assertEquals( length, actualValue.length(), "value has not same length as the pattern" );
            for ( int i = 0; i < length; i++ )
            {
                char expected = expectedPattern.charAt( i );
                char actual = actualValue.charAt( i );
                switch ( expected )
                {
                    case 'x':
                        assertTrue( isLowerCaseLetter( actual ) );
                        break;
                    case 'X':
                        assertTrue( isUpperCaseLetter( actual ) );
                        break;
                    case '#':
                        assertTrue( isDigit( actual ) );
                        break;
                    case '*':
                        assertTrue( isDigit( actual ) || isLowerCaseLetter( actual ) || isUpperCaseLetter( actual ) );
                        break;
                    default:
                        throw new IllegalArgumentException(
                            "expected pattern contains invalid character: " + expected );
                }
            }
        }
        assertEquals( expectedCount, actualValues.size(), "number of generated values is different from the expected" );
    }

    private boolean isDigit( char actual )
    {
        return actual >= '0' && actual <= '9';
    }

    private boolean isUpperCaseLetter( char actual )
    {
        return actual >= 'A' && actual <= 'Z';
    }

    private boolean isLowerCaseLetter( char actual )
    {
        return actual >= 'a' && actual <= 'z';
    }
}
