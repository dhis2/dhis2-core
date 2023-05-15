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
package org.hisp.dhis.webapi.security.utils;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.SecretKey;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public final class TestJwks
{
    public static final RSAKey DEFAULT_RSA_JWK = jwk(
        TestKeys.DEFAULT_PUBLIC_KEY,
        TestKeys.DEFAULT_PRIVATE_KEY ).build();

    public static final ECKey DEFAULT_EC_JWK = jwk(
        (ECPublicKey) TestKeys.DEFAULT_EC_KEY_PAIR.getPublic(),
        (ECPrivateKey) TestKeys.DEFAULT_EC_KEY_PAIR.getPrivate() ).build();

    public static final OctetSequenceKey DEFAULT_SECRET_JWK = jwk(
        TestKeys.DEFAULT_SECRET_KEY ).build();

    private TestJwks()
    {
    }

    public static RSAKey.Builder jwk( RSAPublicKey publicKey, RSAPrivateKey privateKey )
    {
        return new RSAKey.Builder( publicKey )
            .privateKey( privateKey )
            .keyUse( KeyUse.SIGNATURE )
            .keyID( "rsa-jwk-kid" );
    }

    public static ECKey.Builder jwk( ECPublicKey publicKey, ECPrivateKey privateKey )
    {
        Curve curve = Curve.forECParameterSpec( publicKey.getParams() );
        return new ECKey.Builder( curve, publicKey )
            .privateKey( privateKey )
            .keyUse( KeyUse.SIGNATURE )
            .keyID( "ec-jwk-kid" );
    }

    public static OctetSequenceKey.Builder jwk( SecretKey secretKey )
    {
        return new OctetSequenceKey.Builder( secretKey )
            .keyUse( KeyUse.SIGNATURE )
            .keyID( "secret-jwk-kid" );
    }
}
