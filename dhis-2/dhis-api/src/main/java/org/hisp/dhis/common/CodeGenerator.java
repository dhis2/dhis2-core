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

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * @author bobj
 */
public class CodeGenerator
{

    private CodeGenerator()
    {
        throw new IllegalStateException( "Utility class" );
    }

    /*
     * The secure random number generator used by this class to create secure
     * random-based codes. It is in a holder class to defer initialization until
     * needed.
     */
    public static class SecureRandomHolder
    {
        static final SecureRandom GENERATOR = new SecureRandom();
    }

    public static final String NUMERIC_CHARS = "0123456789";

    public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

    public static final String ALPHANUMERIC_CHARS = NUMERIC_CHARS + LETTERS;

    public static final int UID_CODE_SIZE = 11;

    public static final String UID_PATTERN = "^[a-zA-Z][a-zA-Z0-9]{10}$";

    public static final Pattern CODE_PATTERN = Pattern.compile( "^[a-zA-Z][a-zA-Z0-9]{10}$" );

    /**
     * The minimum length of a random alphanumeric string, with the first
     * character always being a letter, needs to have at least 256 bits of
     * entropy.
     * <p>
     * <ol>
     * <li>Alphanumeric char = log2(62) = 5.954196310386875</li>
     * <li>Letter only char = log2(52) = 5.700439718141092</li>
     * <li>256 bits - 5.7 bits (first char) = 250.3 bits (total minus first
     * char)</li>
     * <li>250.3 bits / 5.95 bits ≈ 42.1 characters ≈ 43 characters (can't have
     * a fraction of a character)</li>
     * <li>43 + 1 (first char, we subtracted) = 44 characters</li>
     * </ol>
     * We add one extra character to ensure we have 256 bits of entropy.
     */
    public static final int SECURE_RANDOM_TOKEN_MIN_SIZE = 44;

    /**
     * Generates a string of random alphanumeric characters to the following
     * rules:
     * <ul>
     * <li>Alphanumeric characters only.</li>
     * <li>First character is alphabetic.</li>
     * </ul>
     *
     * @return a code.
     */
    private static char[] generateRandomAlphanumericCode( int codeSize, java.util.Random r )
    {
        char[] randomChars = new char[codeSize];

        // First char should be a letter
        randomChars[0] = LETTERS.charAt( r.nextInt( LETTERS.length() ) );

        for ( int i = 1; i < codeSize; ++i )
        {
            randomChars[i] = ALPHANUMERIC_CHARS.charAt( r.nextInt( ALPHANUMERIC_CHARS.length() ) );
        }

        return randomChars;
    }

    /**
     * Generates a string of random alphanumeric characters. Uses a
     * {@link ThreadLocalRandom} instance and is considered non-secure and
     * should not be used for security purposes.
     *
     * @param codeSize the number of characters in the code.
     * @return the code.
     */
    public static String generateCode( int codeSize )
    {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return new String( generateRandomAlphanumericCode( codeSize, r ) );
    }

    /**
     * Generates a UID according to the following rules:
     * <ul>
     * <li>Alphanumeric characters only.</li>
     * <li>Exactly 11 characters long.</li>
     * <li>First character is alphabetic.</li>
     * </ul>
     *
     * @return a UID.
     */
    public static String generateUid()
    {
        return generateCode( UID_CODE_SIZE );
    }

    /**
     * Generates a string of random alphanumeric characters. Uses a
     * {@link SecureRandom} instance and is slower than
     * {@link #generateCode(int)}, this should be used for security-related
     * purposes only.
     *
     * @param codeSize the number of characters in the code.
     * @return the code.
     */
    public static char[] generateSecureRandomCode( int codeSize )
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        return generateRandomAlphanumericCode( codeSize, sr );
    }

    /**
     * Generates a random secure 44-character(≈256-bit) token.
     * <p>
     * The token is generated using {@link SecureRandom} and should be used for
     * security-related purposes only.
     *
     * @return a token.
     */
    public static String getRandomSecureToken()
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        return new String( generateRandomAlphanumericCode( SECURE_RANDOM_TOKEN_MIN_SIZE, sr ) );
    }

    /**
     * Tests whether the given code is a valid UID.
     *
     * @param code the code to validate.
     * @return true if the code is valid.
     */
    public static boolean isValidUid( String code )
    {
        return code != null && CODE_PATTERN.matcher( code ).matches();
    }
}
