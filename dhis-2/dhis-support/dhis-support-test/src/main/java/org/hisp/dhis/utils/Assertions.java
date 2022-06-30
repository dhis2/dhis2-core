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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.hisp.dhis.common.ErrorCodeException;
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

    @SafeVarargs
    public static <E> void assertContainsOnly( Collection<E> actual, E... expected )
    {
        assertEquals( expected.length, actual.size() );
        for ( E e : expected )
        {
            assertTrue( actual.contains( e ), "Expected " + e.toString() + " in " + actual.toString() );
        }
    }

    public static <K, V> void assertMapEquals( Map<K, V> expected, Map<K, V> actual )
    {
        for ( Map.Entry<K, V> e : expected.entrySet() )
        {
            assertEquals( e.getValue(), actual.get( e.getKey() ), "Expected value not in " + actual.toString() );
        }
        for ( Map.Entry<K, V> e : actual.entrySet() )
        {
            assertEquals( e.getValue(), expected.get( e.getKey() ), "Did not expect value in " + actual.toString() );
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
     * @param <E>
     * @param actual the collection.
     */
    public static <E> void assertIsEmpty( Collection<E> actual )
    {
        assertNotNull( actual );
        assertTrue( actual.isEmpty() );
    }
}
