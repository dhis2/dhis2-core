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
package org.hisp.dhis.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * @author David Mackessy
 */
class StreamUtilsTest
{

    @Test
    void testNullSafeCollectionToStreamValidCollectionWithElements()
    {
        Set<String> countries = Set.of( "Ireland", "Norway", "Spain", "Ghana" );
        Stream<String> stream = StreamUtils.streamOf( countries );
        Set<String> result = stream.collect( Collectors.toSet() );

        assertEquals( 4, result.size() );
        assertTrue( result.containsAll( countries ) );
    }

    @Test
    void testNullSafeCollectionToStreamValidCollectionWithSomeNullElements()
    {
        List<String> countries = new ArrayList<>( Arrays.asList( "Ireland", null, "Spain", null ) );

        List<String> result = assertDoesNotThrow( () -> StreamUtils.streamOf( countries ) ).toList();
        assertEquals( 4, result.size() );
        assertTrue( result.containsAll( countries ) );
    }

    @Test
    void testNullSafeCollectionToStreamNullCollection()
    {
        List<String> countries = null;

        List<String> result = assertDoesNotThrow( () -> StreamUtils.streamOf( countries ) ).toList();
        assertEquals( 0, result.size() );
    }
}
