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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class PasswordGeneratorTest
{
    @Test
    void testValidPasswordGenerationToSmall()
    {
        Exception exception = assertThrows( IllegalArgumentException.class,
            () -> PasswordGeneratorUtils.generateValidPassword( 3 ) );

        String expectedMessage = "Password must be at least 4 characters long";
        String actualMessage = exception.getMessage();

        assertTrue( actualMessage.contains( expectedMessage ) );
    }

    @Test
    void testValidPasswordGenerationMinLength()
    {
        char[] chars = PasswordGeneratorUtils.generateValidPassword( 4 );
        assertEquals( 4, chars.length );
    }

    @Test
    void testValidPasswordGeneration()
    {
        for ( int i = 0; i < 100; i++ )
        {
            char[] password = PasswordGeneratorUtils.generateValidPassword( 12 );
            testPassword( password );
        }
    }

    private static void testPassword( char[] password )
    {
        String passwordString = new String( password );

        boolean containsDigit = PasswordGeneratorUtils.containsDigit( password );
        boolean containsSpecial = PasswordGeneratorUtils.containsSpecialCharacter( password );
        boolean hasUppercase = PasswordGeneratorUtils.containsUppercaseCharacter( password );
        boolean hasLowercase = PasswordGeneratorUtils.containsLowercaseCharacter( password );
        assertTrue( containsSpecial && containsDigit && hasUppercase && hasLowercase );

        Matcher threeConsecutiveLetters = Pattern.compile( "[a-zA-Z][a-zA-Z][a-zA-Z]+" ).matcher( passwordString );
        assertFalse( threeConsecutiveLetters.find() );
    }
}
