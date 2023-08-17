/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

/**
 * @author Jan Bernitt
 */
public final class Assertions
{
    private Assertions()
    {
        throw new UnsupportedOperationException( "util" );
    }

    @SafeVarargs
    public static <E> void assertContainsOnly( Collection<E> actual, E... expected )
    {
        assertEquals( expected.length, actual.size() );

        for ( E e : expected )
        {
            assertTrue( "Expected " + e.toString() + " in " + actual, actual.contains( e ) );
        }
    }

    public static <K, V> void assertMapEquals( Map<K, V> expected, Map<K, V> actual )
    {
        for ( Map.Entry<K, V> e : expected.entrySet() )
        {
            assertEquals( "Expected value not in " + actual.toString(),
                e.getValue(), actual.get( e.getKey() ) );
        }

        for ( Map.Entry<K, V> e : actual.entrySet() )
        {
            assertEquals( "Did not expect value in " + actual,
                e.getValue(), expected.get( e.getKey() ) );
        }
    }

    /**
     * Asserts that the given collection is not null and empty.
     *
     * @param actual the collection.
     */
    public static void assertIsEmpty( Collection<?> actual )
    {
        assertNotNull( actual );
        assertTrue( actual.toString(), actual.isEmpty() );
    }

    /**
     * Asserts that the given collection is not null and not empty.
     *
     * @param actual the collection.
     */
    public static void assertNotEmpty( Collection<?> actual )
    {
        assertNotNull( actual );
        assertFalse( actual.isEmpty() );
    }

    /**
     * Asserts that the given string starts with the expected prefix.
     *
     * @param expected expected prefix of actual string
     * @param actual actual string which should contain the expected prefix
     */
    public static void assertStartsWith( String expected, String actual )
    {
        assertNotNull( String
            .format( "expected string to start with '%s', got null instead", expected ), actual );
        assertTrue( String
            .format( "expected string to start with '%s', got '%s' instead", expected, actual ),
            actual.startsWith( expected ) );
    }

    /**
     * Asserts that the given character sequence is contained within the actual
     * string.
     *
     * @param expected expected character sequence to be contained within the
     *        actual string
     * @param actual actual string which should contain the expected character
     *        sequence
     */
    public static void assertContains( CharSequence expected, String actual )
    {
        assertNotNull( String
            .format( "expected actual to contain '%s', got null instead", expected ), actual );
        assertTrue( String
            .format( "expected actual to contain '%s', got '%s' instead", expected, actual ),
            actual.contains( expected ) );
    }

    /**
     * Asserts that the given value is within the range of lower and upper bound
     * (inclusive i.e. [lower, upper]).
     *
     * @param lower lower bound
     * @param upper upper bound
     * @param actual actual value to be checked
     */
    public static void assertWithinRange( long lower, long upper, long actual )
    {
        assertTrue( String.format( "lower bound %d must be < than the upper bound %d", lower, upper ), lower < upper );

        assertGreaterOrEqual( lower, actual );
        assertLessOrEqual( upper, actual );
    }

    /**
     * Asserts that the given value is greater or equal than lower bound.
     *
     * @param lower lower bound
     * @param actual actual value to be checked
     */
    public static void assertGreaterOrEqual( long lower, long actual )
    {
        assertTrue( String.format( "Expected actual %d to be >= than lower bound %d", actual, lower ),
            actual >= lower );
    }

    /**
     * Asserts that the given value is less or equal than upper bound.
     *
     * @param upper upper bound
     * @param actual actual value to be checked
     */
    public static void assertLessOrEqual( long upper, long actual )
    {
        assertTrue( String.format( "Expected actual %d to be <= than upper bound %d", actual, upper ),
            actual <= upper );
    }
}
