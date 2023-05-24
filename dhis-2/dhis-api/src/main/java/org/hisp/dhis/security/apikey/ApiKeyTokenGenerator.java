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
package org.hisp.dhis.security.apikey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckForNull;

import lombok.Value;

import org.hisp.dhis.common.CRC32Utils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.HashUtils;

import com.google.common.base.Preconditions;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class ApiKeyTokenGenerator
{
    private ApiKeyTokenGenerator()
    {
        throw new IllegalStateException( "Utility class" );
    }

    /**
     * Generates a personal access token with the given attributes and
     * expiration time.
     *
     * @param attributes the attributes to include in the token
     * @param expire the expiration time in milliseconds since epoch
     * @return a token wrapper containing the plaintext token and the token
     */
    public static TokenWrapper generatePersonalAccessToken( @CheckForNull List<ApiTokenAttribute> attributes,
        long expire )
    {
        ApiTokenType type = ApiTokenType.getDefaultPatType();

        char[] plaintext = ApiKeyTokenGenerator.generatePlainTextToken( type );

        final ApiToken token = ApiToken.builder().type( type )
            .version( type.getVersion() )
            .attributes( attributes == null ? new ArrayList<>() : attributes )
            .expire( expire )
            .key( ApiKeyTokenGenerator.hashToken( plaintext ) )
            .build();

        return new TokenWrapper( plaintext, token );
    }

    protected static char[] generatePlainTextToken( ApiTokenType type )
    {
        char[] code = CodeGenerator.generateSecureRandomCode( type.getLength() );
        Preconditions.checkArgument( code.length == type.getLength(),
            "Could not create new token, please try again" );

        char[] prefix = type.getPrefix().toCharArray();
        char[] underscore = new char[] { '_' };
        char[] checksum = generateChecksum( type, code );

        // Concatenate prefix, underscore, code, and checksum
        char[] token = new char[prefix.length + underscore.length + code.length + checksum.length];
        System.arraycopy( prefix, 0, token, 0, prefix.length );
        System.arraycopy( underscore, 0, token, prefix.length, underscore.length );
        System.arraycopy( code, 0, token, prefix.length + underscore.length, code.length );
        System.arraycopy( checksum, 0, token, prefix.length + underscore.length + code.length,
            checksum.length );

        return token;
    }

    private static char[] generateChecksum( ApiTokenType type, char[] code )
    {
        String checksumType = type.getChecksumType();

        return switch ( checksumType )
        {
        case "CRC32" -> generateCRC32InDecimal( code );
        case "CRC32_B62" -> generateCRC32InBase62( code );
        default -> throw new IllegalArgumentException( "Unknown checksum type: " + checksumType );
        };
    }

    /**
     * Generates a CRC32 checksum.
     *
     * @param code the code to generate the checksum for.
     * @return the checksum as a char array.
     */
    public static char[] generateCRC32InDecimal( char[] code )
    {
        long checksum = CRC32Utils.generateCRC32Checksum( code );

        // Convert checksum to a char array
        char[] chars = Long.toString( checksum ).toCharArray();

        // Padding (prefixing with zeros) CRC32 checksum in decimal to 10 digits
        int paddingLength = 10 - chars.length;
        char[] paddedChecksum = new char[10];
        Arrays.fill( paddedChecksum, '0' );
        System.arraycopy( chars, 0, paddedChecksum, paddingLength, chars.length );
        return paddedChecksum;
    }

    /**
     * Generates a CRC32 checksum in base62.
     *
     * @param code the code to generate the checksum for.
     * @return the checksum as a char array.
     */
    public static char[] generateCRC32InBase62( char[] code )
    {
        long checksum = CRC32Utils.generateCRC32Checksum( code );
        String b62encoded = CRC32Utils.crc32ToBase62( checksum );
        return b62encoded.toCharArray();
    }

    /**
     * Validates the checksum of the plaintext token.
     *
     * @param plaintextToken the plaintext token
     * @return true if the checksum is valid, false otherwise
     */
    public static boolean isValidTokenChecksum( char[] plaintextToken )
    {
        ApiTokenType type = ApiTokenType.fromToken( plaintextToken );

        String tokenChecksumType = type.getChecksumType();

        return switch ( tokenChecksumType )
        {
        case "CRC32" -> validateCrc32Checksum( plaintextToken );
        case "CRC32_B62" -> validateCrc32B62Checksum( plaintextToken );
        default -> throw new IllegalArgumentException( "Unsupported checksum type: " + tokenChecksumType );
        };
    }

    private static boolean validateCrc32B62Checksum( char[] plaintextToken )
    {
        CodeAndChecksum codeAndChecksum = extractCodeAndChecksum( plaintextToken );
        return CRC32Utils.isMatchingCRC32Base62Checksum( codeAndChecksum.getCode(),
            codeAndChecksum.getChecksum() );
    }

    private static boolean validateCrc32Checksum( char[] plaintextToken )
    {
        CodeAndChecksum codeAndChecksum = extractCodeAndChecksum( plaintextToken );
        return CRC32Utils.isMatchingCRC32Checksum( codeAndChecksum.getCode(),
            codeAndChecksum.getChecksum() );
    }

    public static CodeAndChecksum extractCodeAndChecksum( char[] plaintextToken )
    {
        ApiTokenType type = ApiTokenType.fromToken( plaintextToken );

        int prefixLength = type.getPrefix().length();
        int codeLength = type.getLength();

        // Extract code from the plaintextToken
        char[] code = new char[codeLength];
        System.arraycopy( plaintextToken, prefixLength + 1, code, 0, codeLength );

        // Extract checksum from the plaintextToken
        int checksumLength = plaintextToken.length - codeLength - prefixLength - 1;
        char[] checksumChars = new char[checksumLength];
        System.arraycopy( plaintextToken, prefixLength + 1 + codeLength, checksumChars, 0, checksumLength );

        return new CodeAndChecksum( code, checksumChars );
    }

    /**
     * Hashes the token using the hash type specified in the token type.
     *
     * @param plaintextToken the plaintext token
     * @return the hashed token
     */
    public static String hashToken( char[] plaintextToken )
    {
        ApiTokenType tokenType = ApiTokenType.fromToken( plaintextToken );

        String tokenHashType = tokenType.getHashType();

        return switch ( tokenHashType )
        {
        case "SHA-256" -> HashUtils.hashSHA256( plaintextToken );
        case "SHA-512" -> HashUtils.hashSHA512( plaintextToken );

        default -> throw new IllegalArgumentException( "Unsupported hash type: " + tokenHashType );
        };
    }

    @Value
    public static class CodeAndChecksum
    {
        char[] code;

        char[] checksum;
    }

    @Value
    public static class TokenWrapper
    {
        char[] plaintextToken;

        ApiToken apiToken;
    }
}
