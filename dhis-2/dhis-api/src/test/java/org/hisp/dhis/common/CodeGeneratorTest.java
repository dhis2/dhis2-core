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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;

/**
 * @author bobj
 */
@Slf4j
class CodeGeneratorTest
{

    @Test
    void testGetUid()
    {
        int numberOfCodes = 500;
        Set<String> codes = new HashSet<>();
        for ( int n = 0; n < numberOfCodes; ++n )
        {
            String code = CodeGenerator.generateUid();
            // Test syntax
            assertTrue( code.substring( 0, 1 ).matches( "[a-zA-Z]" ) );
            assertTrue( code.matches( "[0-9a-zA-Z]{11}" ) );
            // Test uniqueness
            assertTrue( codes.add( code ) );
        }
    }

    @Test
    void testUidIsValid()
    {
        assertTrue( CodeGenerator.isValidUid( "mq4jAnN6fg3" ) );
        assertTrue( CodeGenerator.isValidUid( "QX4LpiTZmUH" ) );
        assertTrue( CodeGenerator.isValidUid( "rT1hdSWjfDC" ) );
        assertFalse( CodeGenerator.isValidUid( "1T1hdSWjfDC" ) );
        assertFalse( CodeGenerator.isValidUid( "QX4LpiTZmUHg" ) );
        assertFalse( CodeGenerator.isValidUid( "1T1hdS_WjfD" ) );
        assertFalse( CodeGenerator.isValidUid( "11111111111" ) );
    }

    @Test
    void testGetRandomUrlToken()
    {
        assertNotNull( CodeGenerator.getRandomSecureToken() );
        assertNotNull( CodeGenerator.getRandomSecureToken() );
        assertNotNull( CodeGenerator.getRandomSecureToken() );
        assertEquals( CodeGenerator.SECURE_RANDOM_TOKEN_MIN_LENGTH, (CodeGenerator.getRandomSecureToken()).length() );
        assertEquals( CodeGenerator.SECURE_RANDOM_TOKEN_MIN_LENGTH, (CodeGenerator.getRandomSecureToken()).length() );
        assertEquals( CodeGenerator.SECURE_RANDOM_TOKEN_MIN_LENGTH, (CodeGenerator.getRandomSecureToken()).length() );
    }

    @Test
    void testIsValidSHA256HexFormat()
    {
        String validSHA256Hex = "635e253fddd8466788d9580983bda99c258e9dd8c5a60a032623fde6c3a2789d";
        assertTrue( CodeGenerator.isValidSHA256HexFormat( validSHA256Hex ) );
    }

    @Test
    void testIsNotValidSHA256HexFormat()
    {
        String invalidSHA256Hex = "6c196468f13817c3fc6e3ced80edef6fa9d7480e687e5f353ab126112f";
        assertFalse( CodeGenerator.isValidSHA256HexFormat( invalidSHA256Hex ) );
    }

    @Test
    void testGenerateCrc32Checksum9Digit()
    {
        char[] input = "Heladasd loAAA adAAAAadasdasdasdasdsd!".toCharArray();
        long expectedChecksum = 821511666L;

        long actualChecksum = CodeGenerator.generateCrc32Checksum( input );
        assertEquals( expectedChecksum, actualChecksum,
            "The calculated CRC32 checksum does not match the expected value." );
    }

    @Test
    void testGenerateCrc32Checksum10Digit()
    {
        char[] input = "Hello, World!".toCharArray();
        long expectedChecksum = 1413314458L;

        long actualChecksum = CodeGenerator.generateCrc32Checksum( input );
        assertEquals( expectedChecksum, actualChecksum,
            "The calculated CRC32 checksum does not match the expected value." );
    }

    @Test
    void testGenerateCrc32ChecksumWithEmptyInput()
    {
        char[] input = "".toCharArray();
        long expectedChecksum = 0L;

        long actualChecksum = CodeGenerator.generateCrc32Checksum( input );

        assertEquals( expectedChecksum, actualChecksum,
            "The calculated CRC32 checksum does not match the expected value for an empty input." );
    }

    @Test
    void testSHA256Hex()
    {
        String input = "Hello, World!";
        String expectedChecksum = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

        String actualChecksum = CodeGenerator.hashSHA256( input );

        assertEquals( expectedChecksum, actualChecksum,
            "The calculated SHA256 checksum does not match the expected value." );
    }

    @Test
    void testSHA512Hex()
    {
        String input = "Hello, World!";
        String expectedChecksum = "374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387";

        String actualChecksum = CodeGenerator.hashSHA512( input );

        assertEquals( expectedChecksum, actualChecksum,
            "The calculated SHA512 checksum does not match the expected value." );
    }

    @Test
    void testValidPasswordGenerationToSmall()
    {
        Exception exception = assertThrows( IllegalArgumentException.class,
            () -> CodeGenerator.generateValidPassword( 3 ) );

        String expectedMessage = "Password must be at least 4 characters long";
        String actualMessage = exception.getMessage();

        assertTrue( actualMessage.contains( expectedMessage ) );
    }

    @Test
    void testValidPasswordGenerationMinLength()
    {
        char[] chars = CodeGenerator.generateValidPassword( 4 );
        assertEquals( 4, chars.length );
    }

    @Test
    void testValidPasswordGeneration()
    {
        for ( int i = 0; i < 100; i++ )
        {
            char[] password = CodeGenerator.generateValidPassword( 12 );
            testPassword( password );
        }
    }

    private static void testPassword( char[] password )
    {
        String passwordString = new String( password );

        boolean containsDigit = CodeGenerator.containsDigit( password );
        boolean containsSpecial = CodeGenerator.containsSpecialCharacter( password );
        boolean hasUppercase = CodeGenerator.containsUppercaseCharacter( password );
        boolean hasLowercase = CodeGenerator.containsLowercaseCharacter( password );
        assertTrue( containsSpecial && containsDigit && hasUppercase && hasLowercase );

        Matcher threeConsecutiveLetters = Pattern.compile( "[a-zA-Z][a-zA-Z][a-zA-Z]+" ).matcher( passwordString );
        assertFalse( threeConsecutiveLetters.find() );
    }
}
