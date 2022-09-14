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
package org.hisp.dhis.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.ErrorCodeException;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.junit.jupiter.api.function.Executable;

/**
 * @author Jan Bernitt
 */
public final class Assertions
{
    private Assertions()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * Asserts that the given collection contains exactly the given items in any
     * order.
     *
     * @param <E> the type.
     * @param expected the expected items.
     * @param actual the actual collection.
     */
    public static <E> void assertContainsOnly( Collection<E> expected, Collection<E> actual )
    {
        assertNotNull( actual, String.format( "Expected collection to contain %s, got null instead", expected ) );

        List<E> missing = CollectionUtils.difference( expected, actual );
        List<E> extra = CollectionUtils.difference( actual, expected );
        assertAll( "assertContainsOnly found mismatch",
            () -> assertTrue( missing.isEmpty(), () -> String.format( "Expected %s to be in %s", missing, actual ) ),
            () -> assertTrue( extra.isEmpty(), () -> String.format( "Expected %s NOT to be in %s", extra, actual ) ),
            () -> assertEquals( expected.size(), actual.size(),
                String.format( "Expected %s or actual %s contains unexpected duplicates", expected, actual ) ) );
    }

    public static <K, V> void assertMapEquals( Map<K, V> expected, Map<K, V> actual )
    {
        for ( Map.Entry<K, V> e : expected.entrySet() )
        {
            assertEquals( e.getValue(), actual.get( e.getKey() ),
                String.format( "Expected value not found in %s", actual.toString() ) );
        }
        for ( Map.Entry<K, V> e : actual.entrySet() )
        {
            assertEquals( e.getValue(), expected.get( e.getKey() ),
                String.format( "Did not expect value in %s", actual.toString() ) );
        }
    }

    /**
     * Asserts that execution of the given executable throws an exception of the
     * expected type, returns the exception and that the error code of the
     * exception equals the given error code.
     *
     * @param <K>
     * @param expectedType the expected type.
     * @param errorCode the {@link ErrorCode}.
     * @param executable the {@link Executable}.
     */
    public static <K extends ErrorCodeException> void assertThrowsErrorCode(
        Class<K> expectedType, ErrorCode errorCode, Executable executable )
    {
        K ex = assertThrows( expectedType, executable );

        assertEquals( errorCode, ex.getErrorCode() );
    }

    /**
     * Asserts that the given collection is not null and empty.
     *
     * @param actual the collection.
     */
    public static void assertIsEmpty( Collection<?> actual )
    {
        assertNotNull( actual );
        assertTrue( actual.isEmpty(), actual.toString() );
    }

    /**
     * Asserts that the given collection is not null and not empty.
     *
     * @param actual the collection.
     */
    public static void assertNotEmpty( Collection<?> actual )
    {
        assertNotNull( actual );
        assertFalse( actual.isEmpty(), actual.toString() );
    }

    /**
     * Asserts that the given string starts with the expected prefix.
     *
     * @param expected expected prefix of actual string
     * @param actual actual string which should contain the expected prefix
     */
    public static void assertStartsWith( String expected, String actual )
    {
        assertNotNull( actual, () -> String
            .format( "expected string to start with '%s', got null instead", expected ) );
        assertTrue( actual.startsWith( expected ), () -> String
            .format( "expected string to start with '%s', got '%s' instead", expected, actual ) );
    }
}
