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
package org.hisp.dhis.security.apikey;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
@Transactional
public class ApiTokenServiceImpl implements ApiTokenService
{
    private final ApiTokenStore apiTokenStore;

    public ApiTokenServiceImpl( ApiTokenStore apiTokenStore )
    {
        checkNotNull( apiTokenStore );

        this.apiTokenStore = apiTokenStore;
    }

    @Override
    @Transactional( readOnly = true )
    @Nonnull
    public List<ApiToken> getAll()
    {
        return this.apiTokenStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    @Nonnull
    public List<ApiToken> getAllOwning( @Nonnull User user )
    {
        return apiTokenStore.getAllOwning( user );
    }

    @Override
    @CheckForNull
    public ApiToken getWithUid( @Nonnull String uid )
    {
        return apiTokenStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    @CheckForNull
    public ApiToken getWithKey( @Nonnull String key, @Nonnull User user )
    {
        return apiTokenStore.getByKey( key, user );
    }

    @Override
    @Transactional( readOnly = true )
    @CheckForNull
    public ApiToken getWithKey( @Nonnull String key )
    {
        return apiTokenStore.getByKey( key );
    }

    @Override
    @Transactional
    public void save( @Nonnull ApiToken apiToken )
    {
        throw new IllegalArgumentException(
            "Tokens can not be saved, all tokens must be created with the createToken() method." );

    }

    @Override
    @Transactional
    public void update( @Nonnull ApiToken apiToken )
    {
        checkNotNull( apiToken, "Token can not be null" );
        Preconditions.checkArgument( StringUtils.isNotEmpty( apiToken.getKey() ), "Token key can not be null" );
        checkNotNull( apiToken.getCreatedBy(), "Token must have an owner" );
        checkNotNull( apiToken.getExpire(), "Token must have an expire value" );
        checkNotNull( apiToken.getType(), "Token must have an type value" );
        checkNotNull( apiToken.getVersion(), "Token must have an version value" );

        apiTokenStore.update( apiToken );

        // Invalidate cache here or let cache expire?
    }

    @Override
    @Transactional
    public void delete( @Nonnull ApiToken apiToken )
    {
        apiTokenStore.delete( apiToken );
        // Invalidate cache here or let cache expire?
    }

    @Override
    @Nonnull
    public Pair<char[], ApiToken> generatePatToken( @CheckForNull List<ApiTokenAttribute> tokenAttributes, long expire )
    {
        ApiTokenType type = ApiTokenType.PERSONAL_ACCESS_TOKEN_V1;

        char[] plaintextToken = generatePlainTextToken( type );

        final ApiToken token = ApiToken.builder().type( type )
            .version( type.getVersion() )
            .attributes( tokenAttributes == null ? new ArrayList<>() : tokenAttributes )
            .expire( expire )
            .key( ApiTokenType.hashToken( plaintextToken ) )
            .build();

        return Pair.of( plaintextToken, token );
    }

    protected static char[] generatePlainTextToken( ApiTokenType type )
    {
        char[] code = CodeGenerator.generateSecureCode( type.getLength() );

        Preconditions.checkArgument( code.length == type.getLength(),
            "Could not create new token, please try again." );

        char[] checksum = generateChecksum( type, code );

        char[] prefix = type.getPrefix().toCharArray();
        char[] underscore = new char[] { '_' };

        // Concatenate prefix, underscore, code, and checksum
        char[] token = new char[prefix.length + underscore.length + code.length + checksum.length];
        System.arraycopy( prefix, 0, token, 0, prefix.length );
        System.arraycopy( underscore, 0, token, prefix.length, underscore.length );
        System.arraycopy( code, 0, token, prefix.length + underscore.length, code.length );
        System.arraycopy( checksum, 0, token, prefix.length + underscore.length + code.length,
            checksum.length );

        return token;
    }

    private static char[] generateChecksum( ApiTokenType type, char[] secureCode )
    {
        String checksumType = type.getChecksumType();

        return switch ( checksumType )
        {
            case "CRC32" -> generateCrc32Checksum( secureCode );

            default -> throw new IllegalArgumentException( "Unknown checksum type: " + checksumType );
        };

    }

    private static char[] generateCrc32Checksum( char[] secureCode )
    {
        long checksum = CodeGenerator.generateCrc32Checksum( secureCode );

        // Convert checksum to a char array
        char[] checksumChars = Long.toString( checksum ).toCharArray();

        // Padding CRC32 checksum to 10 digits
        int paddingLength = 10 - checksumChars.length;
        char[] paddedChecksum = new char[10];
        Arrays.fill( paddedChecksum, '0' );
        System.arraycopy( checksumChars, 0, paddedChecksum, paddingLength, checksumChars.length );
        return paddedChecksum;
    }
}
