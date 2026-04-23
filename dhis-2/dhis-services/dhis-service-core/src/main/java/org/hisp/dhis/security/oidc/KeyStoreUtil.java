/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;

/**
 * Utility for loading RSA keys from a PKCS12 or JKS keystore on disk. Used on two sides of DHIS2's
 * OAuth2/OIDC stack:
 *
 * <ul>
 *   <li>On the Relying Party side, to load the per-provider signing key used to build {@code
 *       private_key_jwt} client assertions when authenticating to an external IdP's token endpoint
 *       ({@code oidc.provider.<id>.keystore_path}, {@code keystore_password}, {@code key_alias},
 *       {@code key_password}).
 *   <li>On the authorization-server side, to load the signing key DHIS2 uses to mint its own tokens
 *       ({@code oauth2.server.jwt.keystore.*}).
 * </ul>
 */
public class KeyStoreUtil {
  private KeyStoreUtil() {
    throw new IllegalArgumentException("This class should not be instantiated");
  }

  /**
   * Loads a keystore from disk using the JVM's default keystore type.
   *
   * @param keystorePath filesystem path to the keystore file
   * @param keystorePassword password used to open the keystore
   * @return the loaded {@link KeyStore}
   * @throws KeyStoreException if the keystore type is not available
   * @throws IOException if the file cannot be read
   * @throws NoSuchAlgorithmException if the integrity algorithm is unavailable
   * @throws CertificateException if any certificate in the keystore cannot be loaded
   */
  public static KeyStore readKeyStore(String keystorePath, String keystorePassword)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream is = new FileInputStream(keystorePath)) {
      keyStore.load(is, keystorePassword.toCharArray());
    }

    return keyStore;
  }

  /**
   * Loads an RSA public key (and its key pair) from the given alias in a keystore as a Nimbus
   * {@link RSAKey}.
   *
   * @param keyStore the keystore to read from
   * @param alias the entry alias
   * @param pin the password protecting the private key entry
   * @return the loaded {@link RSAKey}
   * @throws KeyStoreException if the entry cannot be read
   * @throws JOSEException if the stored public key is not an RSA key
   */
  public static RSAKey loadRSAPublicKey(
      final KeyStore keyStore, final String alias, final char[] pin)
      throws KeyStoreException, JOSEException {
    java.security.cert.Certificate cert = keyStore.getCertificate(alias);

    if (cert.getPublicKey() instanceof RSAPublicKey) {
      return RSAKey.load(keyStore, alias, pin);
    }

    throw new JOSEException(
        "Unsupported public key algorithm: " + cert.getPublicKey().getAlgorithm());
  }
}
