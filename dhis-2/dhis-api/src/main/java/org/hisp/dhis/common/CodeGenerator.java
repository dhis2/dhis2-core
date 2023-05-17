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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.annotation.Nonnull;

import lombok.extern.slf4j.Slf4j;

import com.google.common.hash.Hashing;

/**
 * @author bobj
 */
@Slf4j
public class CodeGenerator
{
    private CodeGenerator()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static final String NUMERIC_CHARS = "0123456789";

    public static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

    public static final String LETTERS = LOWERCASE_LETTERS + UPPERCASE_LETTERS;

    public static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;':,./<>?©®™¢£¥€";

    public static final String ALPHANUMERIC_CHARS = NUMERIC_CHARS + LETTERS;

    public static final String NUMERIC_AND_SPECIAL_CHARS = NUMERIC_CHARS + SPECIAL_CHARS;

    public static final String ALL_CHARS = NUMERIC_CHARS + LETTERS + SPECIAL_CHARS;

    public static final int UID_CODE_SIZE = 11;

    private static final Pattern CODE_PATTERN = Pattern.compile( "^[a-zA-Z][a-zA-Z0-9]{10}$" );

    /**
     * The required minimum length of a random alphanumeric string, with the
     * first character always being a letter, will need to be so that we have at
     * least 256-bits of entropy.
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
     * We add one extra character to ensure we have at least 256 bits of
     * entropy.
     */
    public static final int SECURE_RANDOM_TOKEN_MIN_LENGTH = 44;

    /*
     * The random number generator used by this class to create random based
     * codes. In a holder class to defer initialization until needed.
     */
    private static class SecureRandomHolder
    {
        static final SecureRandom GENERATOR = new SecureRandom();
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
     * Generates a random secure 44-character(≈256-bits) token.
     * <p>
     * The token is generated using {@link SecureRandom} and should be used for
     * security-related purposes only.
     *
     * @return a token.
     */
    public static String getRandomSecureToken()
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        return new String( generateRandomAlphanumericCode( SECURE_RANDOM_TOKEN_MIN_LENGTH, sr ) );
    }

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
     * Calculates a checksum for the given input string.
     *
     * @param input the input char array.
     * @return the checksum.
     */
    public static long generateCrc32Checksum( @Nonnull char[] input )
    {
        Charset charset = StandardCharsets.UTF_8;
        CharBuffer charBuffer = CharBuffer.wrap( input );
        ByteBuffer byteBuffer = charset.encode( charBuffer );
        byte[] bytes = byteBuffer.array();

        CRC32 crc = new CRC32();
        crc.update( bytes, 0, bytes.length );
        return crc.getValue();
    }

    /**
     * Tests whether the given input string generates the same checksum.
     *
     * @param input the input string to checksum.
     * @param checksum the checksum to compare against.
     * @return true if they match.
     */
    public static boolean isMatchingCrc32Checksum( @Nonnull char[] input, @Nonnull String checksum )
    {
        long s1 = CodeGenerator.generateCrc32Checksum( input );
        long s2 = Long.parseLong( checksum );

        return s1 == s2;
    }

    /**
     * Calculates a SHA256 hash for the given input string.
     *
     * @param input the input string.
     * @return the hash.
     */
    public static String hashSHA256( @Nonnull byte[] input )
    {
        return Hashing.sha256().hashBytes( input ).toString();
    }

    public static String hashSHA256( @Nonnull String input )
    {
        return hashSHA256( input.getBytes( StandardCharsets.UTF_8 ) );
    }

    public static String hashSHA256( @Nonnull char[] input )
    {
        byte[] bytes = extractBytesFromChar( input );

        return hashSHA256( bytes );
    }

    /**
     * Calculates a SHA512 hash for the given input string.
     *
     * @param input the input string.
     * @return the hash.
     */
    public static String hashSHA512( @Nonnull byte[] input )
    {
        return Hashing.sha512().hashBytes( input ).toString();
    }

    public static String hashSHA512( @Nonnull String input )
    {
        return hashSHA512( input.getBytes( StandardCharsets.UTF_8 ) );
    }

    public static String hashSHA512( @Nonnull char[] input )
    {
        byte[] bytes = extractBytesFromChar( input );

        return hashSHA512( bytes );
    }

    private static byte[] extractBytesFromChar( char[] input )
    {
        Charset charset = StandardCharsets.UTF_8;
        CharBuffer charBuffer = CharBuffer.wrap( input );
        ByteBuffer byteBuffer = charset.encode( charBuffer );
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get( bytes );
        return bytes;
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

    public static boolean isValidSHA256HexFormat( String s )
    {
        // Check if the string is a valid hexadecimal number
        String hexPattern = "^[0-9a-fA-F]+$";
        Pattern pattern = Pattern.compile( hexPattern );
        if ( !pattern.matcher( s ).matches() )
        {
            return false;
        }

        // SHA-256 in hexadecimal are exactly 64 characters long
        return s.length() == 64;
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
    public static char[] generateValidPassword( int passwordSize )
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

        return randomChars;
    }

    public static boolean containsDigit( char[] chars )
    {
        for ( char c : chars )
        {
            if ( c >= '0' && c <= '9' )
            {
                return true;
            }
        }
        return false;
    }

    public static boolean containsSpecialCharacter( char[] chars )
    {
        for ( char c : chars )
        {
            if ( SPECIAL_CHARS.indexOf( c ) >= 0 )
            {
                return true;
            }
        }
        return false;
    }

    public static boolean containsUppercaseCharacter( char[] chars )
    {
        for ( char c : chars )
        {
            if ( c >= 'A' && c <= 'Z' )
            {
                return true;
            }
        }
        return false;
    }

    public static boolean containsLowercaseCharacter( char[] chars )
    {
        for ( char c : chars )
        {
            if ( c >= 'a' && c <= 'z' )
            {
                return true;
            }
        }
        return false;
    }
}
