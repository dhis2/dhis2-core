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
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import javax.annotation.Nonnull;

import org.springframework.util.Base64Utils;

import com.google.common.hash.Hashing;

/**
 * @author bobj
 */
public class CodeGenerator
{
    private CodeGenerator()
    {
        throw new IllegalStateException( "Utility class" );
    }

    public static final String LETTERS = "abcdefghijklmnopqrstuvwxyz"
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String ALPHANUMERIC_CHARS = "0123456789" + LETTERS;

    public static final int UID_CODE_SIZE = 11;

    private static final Pattern CODE_PATTERN = Pattern.compile( "^[a-zA-Z][a-zA-Z0-9]{10}$" );

    /**
     * 192 bit must be dividable by 3 to avoid padding "=".
     */
    private static final int URL_RANDOM_TOKEN_LENGTH = 24;

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
     * {@link #generateCode(int)}, this should only be used for security
     * purposes.
     *
     * @param codeSize the number of characters in the code.
     * @return the code.
     */
    public static char[] generateSecureCode( int codeSize )
    {
        SecureRandom r = SecureRandomHolder.GENERATOR;
        return generateRandomAlphanumericCode( codeSize, r );
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
     * Generates a cryptographically strong random token encoded in Base64
     *
     * @param lengthInBytes length in bytes of the token
     * @return a Base64 encoded string of the token
     */
    public static String getRandomSecureToken( int lengthInBytes )
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        byte[] tokenBytes = new byte[lengthInBytes];
        sr.nextBytes( tokenBytes );

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString( tokenBytes );
    }

    /**
     * Generates a random 32-character token to be used in URLs.
     *
     * @return a token.
     */
    public static String getRandomUrlToken()
    {
        SecureRandom sr = SecureRandomHolder.GENERATOR;
        byte[] tokenBytes = new byte[URL_RANDOM_TOKEN_LENGTH];
        sr.nextBytes( tokenBytes );

        return Base64Utils.encodeToUrlSafeString( tokenBytes );
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
}
