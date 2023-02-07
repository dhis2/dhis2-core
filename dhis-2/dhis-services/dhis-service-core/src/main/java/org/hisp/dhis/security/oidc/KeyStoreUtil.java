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
package org.hisp.dhis.security.oidc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class KeyStoreUtil
{
    private KeyStoreUtil()
    {
        throw new IllegalArgumentException( "This class should not be instantiated" );
    }

    public static KeyStore readKeyStore( String keystorePath, String keystorePassword )
        throws KeyStoreException,
        IOException,
        NoSuchAlgorithmException,
        CertificateException
    {
        KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        try ( InputStream is = new FileInputStream( keystorePath ) )
        {
            keyStore.load( is, keystorePassword.toCharArray() );
        }

        return keyStore;
    }

    public static RSAKey loadRSAPublicKey( final KeyStore keyStore, final String alias, final char[] pin )
        throws KeyStoreException,
        JOSEException
    {
        java.security.cert.Certificate cert = keyStore.getCertificate( alias );

        if ( cert.getPublicKey() instanceof RSAPublicKey )
        {
            return RSAKey.load( keyStore, alias, pin );
        }

        throw new JOSEException( "Unsupported public key algorithm: " + cert.getPublicKey().getAlgorithm() );
    }
}
