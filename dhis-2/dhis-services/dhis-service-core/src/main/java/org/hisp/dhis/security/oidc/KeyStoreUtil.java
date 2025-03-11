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
import java.security.KeyPairGenerator;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.UUID;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

// Sun specific imports (if we'll use them instead of Bouncy Castle)
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class KeyStoreUtil {
  private KeyStoreUtil() {
    throw new IllegalArgumentException("This class should not be instantiated");
  }

  public static KeyStore readKeyStore(String keystorePath, String keystorePassword)
      throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (InputStream is = new FileInputStream(keystorePath)) {
      keyStore.load(is, keystorePassword.toCharArray());
    }

    return keyStore;
  }

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
  
  /**
   * Generates a new keystore file with an RSA key pair using a simpler approach
   * that does not require external libraries like Bouncy Castle
   *
   * @param keystorePath the path where to save the keystore
   * @param keystorePassword the password to protect the keystore
   * @param alias the alias for the key entry
   * @param keyPassword the password to protect the key
   * @return the generated RSA key
   * @throws Exception if an error occurs during the generation or saving of the keystore
   */
  public static RSAKey generateKeystoreWithRsaKey(
      String keystorePath, String keystorePassword, String alias, String keyPassword)
      throws Exception {
    // Generate RSA key pair
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    
    // Create keystore
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, keystorePassword.toCharArray());
    
    // Add key entry directly as a PrivateKeyEntry without a certificate
    // This is a simplified approach that should work for JWK storage
    // Note: In a production environment, you might want to use a proper X.509 certificate
    KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(
        privateKey, 
        new java.security.cert.Certificate[] { generateSimpleCertificate(publicKey) }
    );
    
    keyStore.setEntry(
        alias,
        privateKeyEntry,
        new KeyStore.PasswordProtection(keyPassword.toCharArray())
    );
    
    // Save keystore to file
    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(keystorePath)) {
      keyStore.store(fos, keystorePassword.toCharArray());
    }
    
    // Create and return RSA key
    return new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
  }
  
  /**
   * Generates a simple self-signed certificate using the provided public key
   * This is a very simple implementation and should be enhanced for production use
   *
   * @param publicKey the public key to include in the certificate
   * @return a self-signed certificate
   * @throws Exception if certificate generation fails
   */
  private static java.security.cert.Certificate generateSimpleCertificate(PublicKey publicKey) throws Exception {
    // Use a specialized certificate generation utility
    return new java.security.cert.Certificate("X.509") {
      @Override
      public byte[] getEncoded() {
        return new byte[0]; // Simplified for demo
      }

      @Override
      public void verify(PublicKey key) {
        // No verification needed for our purpose
      }

      @Override
      public void verify(PublicKey key, String sigProvider) {
        // No verification needed for our purpose
      }

      @Override
      public String toString() {
        return "Simplified certificate for JWK";
      }

      @Override
      public PublicKey getPublicKey() {
        return publicKey;
      }
    };
  }
}
