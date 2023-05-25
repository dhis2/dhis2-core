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
package org.hisp.dhis.helpers;

import java.security.SecureRandom;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class PasswordGenerator
{
    public static final String NUMERIC_CHARS = "0123456789";

    public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

    public static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':,./<>?©®™¢£¥€";

    public static final String NUMERIC_AND_SPECIAL_CHARS = NUMERIC_CHARS + SPECIAL_CHARS;

    public static final String ALL_CHARS = NUMERIC_CHARS + LETTERS + SPECIAL_CHARS;

    private static class SecureRandomHolder
    {
        static final SecureRandom GENERATOR = new SecureRandom();
    }

    private static char generateRandomCharacter( String charSet )
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        return charSet.charAt( sr.nextInt( charSet.length() ) );
    }

    /**
     * Generates a random password with the given size, has to be minimum 4
     * characters long.
     * <p>
     * The password will contain at least one digit, one special character, one
     * uppercase letter and one lowercase letter.
     * <p>
     * The password will not contain more than 2 consecutive letters to avoid
     * generating words.
     *
     * @param passwordSize the size of the password.
     * @return a random password.
     */
    public static String generateValidPassword( int passwordSize )
    {
        if ( passwordSize < 4 )
        {
            throw new IllegalArgumentException( "Password must be at least 4 characters long" );
        }

        char[] randomChars = new char[passwordSize];

        randomChars[0] = generateRandomCharacter( NUMERIC_CHARS );
        randomChars[1] = generateRandomCharacter( SPECIAL_CHARS );
        randomChars[2] = generateRandomCharacter( UPPERCASE_LETTERS );
        randomChars[3] = generateRandomCharacter( LOWERCASE_LETTERS );

        int consecutiveLetterCount = 2; // the last 2 characters are letters
        for ( int i = 4; i < passwordSize; ++i )
        {
            if ( consecutiveLetterCount >= 2 )
            {
                // After 2 consecutive letters, the next character should be a number or a special character
                randomChars[i] = generateRandomCharacter( NUMERIC_AND_SPECIAL_CHARS );
                consecutiveLetterCount = 0;
            }
            else
            {
                randomChars[i] = generateRandomCharacter( ALL_CHARS );
                if ( LETTERS.indexOf( randomChars[i] ) >= 0 )
                {
                    // If the character is a letter, increment the counter
                    consecutiveLetterCount++;
                }
                else
                {
                    // If the character is not a letter, reset the counter
                    consecutiveLetterCount = 0;
                }
            }
        }

        return new String( randomChars );
    }
}
