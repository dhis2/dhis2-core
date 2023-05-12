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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
final class KeyGeneratorUtils
{
    private KeyGeneratorUtils()
    {
    }

    static SecretKey generateSecretKey()
    {
        SecretKey hmacKey;
        try
        {
            hmacKey = KeyGenerator.getInstance( "HmacSha256" ).generateKey();
        }
        catch ( Exception ex )
        {
            throw new IllegalStateException( ex );
        }

        return hmacKey;
    }

    static KeyPair generateRsaKey()
    {
        KeyPair keyPair;
        try
        {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
            keyPairGenerator.initialize( 2048 );
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch ( Exception ex )
        {
            throw new IllegalStateException( ex );
        }

        return keyPair;
    }

    static KeyPair generateEcKey()
    {
        EllipticCurve ellipticCurve = new EllipticCurve( new ECFieldFp(
            new BigInteger( "115792089210356248762697446949407573530086143415290314195533631308867097853951" ) ),
            new BigInteger( "115792089210356248762697446949407573530086143415290314195533631308867097853948" ),
            new BigInteger( "41058363725152142129326129780047268409114441015993725554835256314039467401291" ) );

        ECPoint ecPoint = new ECPoint(
            new BigInteger( "48439561293906451759052585252797914202762949526041747995844080717082404635286" ),
            new BigInteger( "36134250956749795798585127919587881956611106672985015071877198253568414405109" ) );

        ECParameterSpec ecParameterSpec = new ECParameterSpec(
            ellipticCurve,
            ecPoint,
            new BigInteger( "115792089210356248762697446949407573529996955224135760342422259061068512044369" ),
            1 );

        KeyPair keyPair;
        try
        {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
            keyPairGenerator.initialize( ecParameterSpec );
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch ( Exception ex )
        {
            throw new IllegalStateException( ex );
        }

        return keyPair;
    }
}
