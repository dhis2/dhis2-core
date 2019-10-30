/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.cache;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CacheKeyBuilderTest
{
    @Test
    public void testBuildWithKey()
    {
        // Given
        final String anyKey = "anyKey";

        // When
        final Key builtKey = new CacheKeyBuilder().build( anyKey );

        // Then
        assertThat( builtKey.toString(), containsString( anyKey ) );
        assertThat( builtKey.toString(), not( containsString( CacheKeyBuilder.SEPARATOR ) ) );
        assertThat( builtKey.toString(), not( containsString( CacheKeyBuilder.DELIMITER ) ) );
    }

    @Test
    public void testBuildWithKeyAndAdditional()
    {
        // Given
        final String anyKey = "anyKey";
        final String additionalOne = "anything-1";
        final String additionalTwo = "anything-2";
        final String[] anyAdditional = { additionalOne, additionalTwo };

        // When
        final Key builtKey = new CacheKeyBuilder().build( anyKey, anyAdditional );

        // Then
        assertThat( builtKey.toString(), containsString( anyKey ) );
        assertThat( builtKey.toString(), containsString( additionalOne ) );
        assertThat( builtKey.toString(), containsString( additionalTwo ) );
        assertThat( builtKey.toString(), containsString( CacheKeyBuilder.SEPARATOR ) );
        assertThat( builtKey.toString(), containsString( CacheKeyBuilder.DELIMITER ) );
    }
}