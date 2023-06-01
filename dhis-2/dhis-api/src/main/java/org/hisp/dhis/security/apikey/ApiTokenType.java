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

import lombok.Getter;

/**
 * Class for representing the different types of API tokens and their
 * properties.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Getter
public enum ApiTokenType
{
    PERSONAL_ACCESS_TOKEN_V1( 1, "d2pat", 32, "SHA-256", "CRC32" ),
    PERSONAL_ACCESS_TOKEN_V2( 2, "d2p", 44, "SHA-256", "CRC32_B62" );

    private final String prefix;

    private final int version;

    private final int length;

    private final String hashType;

    private final String checksumType;

    ApiTokenType( int version, String prefix, int length, String hashType, String checksumType )
    {
        this.prefix = prefix;
        this.length = length;
        this.version = version;
        this.hashType = hashType;
        this.checksumType = checksumType;
    }

    public static ApiTokenType getDefaultPatType()
    {
        return PERSONAL_ACCESS_TOKEN_V2;
    }

    public static ApiTokenType fromToken( char[] token )
    {
        String tokenType = getTokenTypePrefix( token );

        for ( ApiTokenType type : ApiTokenType.values() )
        {
            if ( tokenType.equals( type.getPrefix() ) )
            {
                return type;
            }
        }
        throw new IllegalArgumentException( "No token type found" );
    }

    private static String getTokenTypePrefix( char[] token )
    {
        for ( int i = 0; i < token.length; i++ )
        {
            if ( token[i] == '_' )
            {
                return new String( token, 0, i );
            }
        }
        throw new IllegalArgumentException( "No token type prefix found" );
    }
}
