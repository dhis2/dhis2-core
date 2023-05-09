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
import static org.hisp.dhis.common.CodeGenerator.getRandomSecureToken;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Service
@Transactional
public class ApiTokenServiceImpl implements ApiTokenService
{
    private static final Long DEFAULT_EXPIRE_TIME_IN_MILLIS = TimeUnit.DAYS.toMillis( 30 );

    private final ApiTokenStore apiTokenStore;

    public ApiTokenServiceImpl( ApiTokenStore apiTokenStore )
    {
        checkNotNull( apiTokenStore );

        this.apiTokenStore = apiTokenStore;
    }

    @Override
    @Transactional( readOnly = true )
    public List<ApiToken> getAll()
    {
        return this.apiTokenStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<ApiToken> getAllOwning( User user )
    {
        return apiTokenStore.getAllOwning( user );
    }

    @Override
    @Transactional( readOnly = true )
    public ApiToken getWithKey( String key, User user )
    {
        return apiTokenStore.getByKey( key, user );
    }

    @Override
    @Transactional( readOnly = true )
    public ApiToken getWithKey( String key )
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

        // Invalidate cache here or let cache expire ?
    }

    @Override
    @Transactional
    public void delete( @Nonnull ApiToken apiToken )
    {
        apiTokenStore.delete( apiToken );
        // Invalidate cache here or let cache expire ?
    }

    @Override
    public ApiToken initToken( ApiToken token )
    {
        Preconditions.checkNotNull( token );
        Preconditions.checkNotNull( token.getType() );

        token.setVersion( 1 );

        if ( token.getExpire() == null )
        {
            token.setExpire( System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_IN_MILLIS );
        }

        String randomSecureToken = getRandomSecureToken( 24 ).replaceAll( "[-_]", "x" );
        Preconditions.checkArgument( randomSecureToken.length() == 32,
            "Could not create new token, please try again." + randomSecureToken.length() );

        byte[] bytes = randomSecureToken.getBytes();
        CRC32 crc = new CRC32();
        crc.update( bytes, 0, bytes.length );
        long checksumLong = crc.getValue();

        token.setKey( String.format( "%s_%s%010d", token.getType().getPrefix(), randomSecureToken, checksumLong ) );

        Preconditions.checkArgument( token.getKey().length() == 48,
            "Could not create new token, please try again." );

        return token;
    }

    public String hashKey( String key )
    {
        return Hashing.sha256().hashBytes( key.getBytes( StandardCharsets.UTF_8 ) ).toString();
    }

    @Override
    public ApiToken getWithUid( String uid )
    {
        return apiTokenStore.getByUid( uid );
    }
}
