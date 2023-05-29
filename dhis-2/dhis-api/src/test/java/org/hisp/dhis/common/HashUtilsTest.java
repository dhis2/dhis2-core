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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class HashUtilsTest
{
    @Test
    void testIsValidSHA256HexFormat()
    {
        String validSHA256Hex = "635e253fddd8466788d9580983bda99c258e9dd8c5a60a032623fde6c3a2789d";
        assertTrue( HashUtils.isValidSHA256HexFormat( validSHA256Hex ) );
    }

    @Test
    void testIsNotValidSHA256HexFormat()
    {
        String invalidSHA256Hex = "6c196468f13817c3fc6e3ced80edef6fa9d7480e687e5f353ab126112f";
        assertFalse( HashUtils.isValidSHA256HexFormat( invalidSHA256Hex ) );
    }

    @Test
    void testSHA256Hex()
    {
        String input = "Hello, World!";
        String expectedChecksum = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

        String actualChecksum = HashUtils.hashSHA256( input );

        assertEquals( expectedChecksum, actualChecksum,
            "The calculated SHA256 checksum does not match the expected value." );
    }

    @Test
    void testSHA512Hex()
    {
        String input = "Hello, World!";
        String expectedChecksum = "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387";

        String actualChecksum = HashUtils.hashSHA512( input );

        assertEquals( expectedChecksum, actualChecksum,
            "The calculated SHA512 checksum does not match the expected value." );
    }
}
