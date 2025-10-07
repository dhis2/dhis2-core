/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.webapi.controller.security.DcrWithJwksTest.KeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Slf4j
public class CreateKeyTest {

  @Test
  public void printKey() throws NoSuchAlgorithmException {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    java.security.KeyPair kp = kpg.generateKeyPair();
    RSAPublicKey rsaPublicKey = (RSAPublicKey) kp.getPublic();
    RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kp.getPrivate();
    String kid = UUID.randomUUID().toString();
    RSAKey rsaKey = new RSAKey.Builder(rsaPublicKey).privateKey(rsaPrivateKey).keyID(kid).build();
    JWKSet jwkSet = new JWKSet(rsaKey);

    KeyPair keyPair = new KeyPair(rsaKey, jwkSet);
    // print the private key so we can use it in Postman
    log.error("Private Key: \n\n{}\n\n", keyPair.rsaKey().toJSONString());
    log.error("Public Key: \n\n{}\n\n", keyPair.jwkSet().toString());
  }

  @Test
  public void readKeyFromJwkSet() throws ParseException, JOSEException {

    // read the private and public key from two strings
    String privateKey =
"""
{"p":"9_w-Gb6-b31VAVnJ8OaCJg1krzzTuMj2X5ZYDL7AVTN6-8EgIe1HkhmJ4h51txHyrZNTJUOLFJ23XMvwQivn80-bX9FAby_gxetwphW1Zh7Fcck7NdLUWSuaX2csui5iH1sEUMj-uL7hWUqeedUtusZIeNgwVzoC0Ex_UqjQhFk","kty":"RSA","q":"uewjHHiVjCPL6x1iWZPnqv-vL0in1lT8z7vp0Pfov7eCe4AvKWzeby10P4U82KCCy-64tYlyTduYpIYm4GhmY2awFQpFLq_Zzdv-KMcEnGdstgxzu8XeB_LxC1JNwu6UsdFl-YQ7U1UvXEUynXmf6CP1uINm_0Xyym6XoQEF1bs","d":"A642fm88Ov1_FFxfXPtWIVbE19_7S3Q6NYTpzmAX2Rer12rsxAwEyiHWApjxRfVqa0zhx0Fn3SnDjnAjGacgfPjbxGV_ZrCAEm_KhZj2Fa-b8gLvfDV3jQDoSXhyhdoCijYmVwwJ8Jju6m-N9wxFdrvAD-9v4XVk8L_ZzjzhKvZtwSKYnQGR8FP9GtT1bJy3y5nWGrV-lVPgMbh3KHVVZoGHJ8B0rJz4sclHV1FISFgOPM-6K9tShRNZeOzeEHr-6EfeFLLyf1Z2vwJEczTvyb1Ft_SR5ZELnuYDNIAWPwFoe3O3LK-2fuXd4CEzrMX3cFHKoRYOiD3M_lHICQ1pGQ","e":"AQAB","kid":"8dfe59af-499f-4e9f-9fc1-f84fecd6f35b","qi":"0XalUGdtmEhwmRC6ff9g4nmcWsOoNPVWRA_vRvpJAFljrAxonZ7xU0jpCog0CtzuceIyRM_NvleaYV2sNULfT_3IkwmPN8d1b8bCZ5HluzE5l-dVeySBYMZXs_uLaHOKsxGbWzQ-gSQVVBcKsh022sRNgtZ7moJ-lSdT8ZT72zo","dp":"z-7Jg3zU4VcN99v53-zoJFAGpIk0XjTjoLLHkahATTClZoNBFjGHWZHgc6FmwuJhwflONmi0Lc0w-rZl9pYqH3IYrfcfOBvFNS99fUWmnMIgfJBm_XKSa1KMVoKElnhd_jyrGbHvI6mp-tz-lNmTRpEMI4fiwMD7qvz5pa23acE","dq":"t78azE6YbhIKvOkjhCha83MSrgZ-aaNA4AV1heMdEizJNklvUt-XzgT8OLLzeZzY57ecsT2PzEbVSvSSg4Jqfp7EM2cdDJEbDwr221IlynWkyR7xWoipcO23MFs9IwQrzLmIsNrwzuEHl4eECIJleUXg2WR0bKGy3EIEdUjPfXU","n":"tBoHbOt24LIVa0ynUbu-ms7QqW9zXNL1kokmr31gUKXJRe7yXbI2C9sZGop6iwRzDZJMSwCJ9lVZjTdmqW-PUe75_UxS1vFBYOc9JR330_pssFPHLmG0lnrsXX7SvJvRyQs_yMsbls7XEex3FWVippDszmq8-93SuefUqlN-OIvqNkfk9gLBSq3cZOlmqpvl3s67tBpD1FAuTruGiXjnxvIV2Axq9iZ7--zRpQo9R9OcHjRpi2NCoPgbXm1DSxcJyn5yTxMBdRnXNaslAZOlab77oNRmIA3qhb_7exZbX-nRIK2xYuZv5Ws2DGtsDn2ztsYCqCk5FRPeiJ9mvSu6Aw"}
""";
    String publicKey =
"""
{"kty":"RSA","e":"AQAB","kid":"8dfe59af-499f-4e9f-9fc1-f84fecd6f35b","n":"tBoHbOt24LIVa0ynUbu-ms7QqW9zXNL1kokmr31gUKXJRe7yXbI2C9sZGop6iwRzDZJMSwCJ9lVZjTdmqW-PUe75_UxS1vFBYOc9JR330_pssFPHLmG0lnrsXX7SvJvRyQs_yMsbls7XEex3FWVippDszmq8-93SuefUqlN-OIvqNkfk9gLBSq3cZOlmqpvl3s67tBpD1FAuTruGiXjnxvIV2Axq9iZ7--zRpQo9R9OcHjRpi2NCoPgbXm1DSxcJyn5yTxMBdRnXNaslAZOlab77oNRmIA3qhb_7exZbX-nRIK2xYuZv5Ws2DGtsDn2ztsYCqCk5FRPeiJ9mvSu6Aw"}
""";

    // Parse the JWK JSON string
    JWK jwkpriv = JWK.parse(privateKey);
    JWK jwkpub = JWK.parse(publicKey);

    // Convert the JWK to an RSAKey object
    RSAKey rsaPrivateKey = (RSAKey) jwkpriv;
    RSAKey rsaPublicKey = (RSAKey) jwkpub;

    //    RSAKey rsaPrivateKey = RSAKey.parse(privateKey);
    //    RSAKey rsaPublicKey = RSAKey.parse(publicKey);

    RSAPrivateKey reconstructedPrivateKey = rsaPrivateKey.toRSAPrivateKey();
    RSAPublicKey reconstructedPublicKey = rsaPublicKey.toRSAPublicKey();

    RSAKey rsaKey =
        new RSAKey.Builder(reconstructedPublicKey)
            .privateKey(reconstructedPrivateKey)
            .keyID("kid")
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);

    KeyPair keyPair = new KeyPair(rsaKey, jwkSet);
    log.error("Private Key: \n\n{}\n\n", keyPair.rsaKey().toJSONString());
    log.error("Public Key: \n\n{}\n\n", keyPair.jwkSet().toString());
  }

  //  @Test
  //  public void testCreateKey() throws NoSuchAlgorithmException {
  //
  //    // Test implementation goes here
  //    this.keyPair = createKeys();
  //
  //    log.error("Public Key: {}", keyPair.jwkSet().toString());
  //
  //  }
  //
  //
  @Test
  public void testCreateAssertion() throws ParseException, JOSEException {

    String privateKey =
"""
{"p":"7NpubMgkBgHrUY6_LV8gtYLoCLTwaxfwJ784jdy6RG5W_HbXBWs8PDol4DbuomFRdSTi7E3ByqByRhfb74QS_B6lcBZHbf8qq-tXFlQIixOcWy4mIFxi-YJP0ofaRx84zpNCmEninZQJcI83Qg09NPRacyfZGmKzubnedupfazE","kty":"RSA","q":"25taKN8Q3UBhcalxPK4OWfWuCS4ePtWfZJIkNenitLtXpXVziZ32NAI44Fa8pM_Y6xOxAyPBANGx4H3tOJJllwrlk3bOEVde_QP3BPZqKglc5HyPBFB_9Wp_E0PmRaZP8sAqSszZ_TNIkwudyiMIVc_SEq4ZVbgGqDmk36BqcVU","d":"An9_Z9dCtuOW71jUqNzogDQr1rLCd7D1R9qZdaVhIF4AYBLIK4NihdEwTz5Ozp7rBXUHdMJV21SmBz4vWfez5cTN73jqvT6hPeTqKsfdmkbeOSAEvFO3FCiJmya_o5hH19Ddbv6OoxXrjYdTAba5we2ycJVw9ZxlZjFKr0LrAxYqgnyxKRcTpjxDBAnSYP2vhdA9XOe-2xL4oLCYuA_sOv9ux17b3j7u3o0SitZYaRRDaEKSNtUmrNaqldCXMz4TXoLk-X96d1Gm0i15wVcEiAUv59KiX0bWzvvte4-lEKmnWRvVLbug9UNEmFWFKiPd7z3VuyywjXD7VoQfeYoHkQ","e":"AQAB","kid":"1782cbae-5399-4888-925f-23a0b67c9a76","qi":"l4QsfoXMuLCARsrt0gkUW7MmQjQxPyZcvTCcIBYT96nWUjLBVF51J4t2geR1G39k7G4IPNd_ncUh2TfA5oIPV0HzOLgltc1ikzict0YmtB6Z3eNs0zmKDbYuQxgLhf7HmQVueDttmHTQdWfxl8EZjdIoKoq8SlZP1ffn-zdGUu0","dp":"vz9o5qF8pPUrw8EVkVc8nBPFtDIV5wN_QTjgO3w9U0AdMOHsoU2DZ8Y0CyWP7sHR-lr8eH4YurpvSzW8u1vT0IwWXbk_mAvvsr3mpfrYoyQEnoNW-c5fhMS6G27iy4bkYbj6jukp-L8uGBssLUNvZrDf0Bge73U_VfpFLZP11CE","dq":"BDLbLW7eXqyNcGPh81wMkDG_SxOjpmXlL0IQCSvYlsYCLfqp49auT14_giKGZsxGhHAS8VFsrjxUH0upDmzWHmYp74DU_cXi3gmGXoTrkLQvH3s2LMnxOFr55P9mVqyQWL7N0DJMdKfXWmBdevDihul7RbooZ9gl4G-BtHjXsCU","n":"yy6YIoZpXtsCcTEChhkOaYlYrkx2pscKWHIo55BoxBJYgf73HkeoDoX1LRLXOu2ycALU9in5ssvt2w3FbgbJvalUC2QRhpc6jAD4_i78JzqKC6xldhhvKRoOrMqi8FOy01j13ikG9KjpB1kNHi1soOivd_rFejILJfLGlvKp0idN3sYCo93UgUJHPyCU4juxqju8TvTaUoohshlM68Bph56EUBkSIJDB4kdt_3SM_HrVTpKoAY1w0F-KGpFSm9c13FeeZ1cYLLdL8tCWWOlVacWES_YZSefOOTbwb5NQvdHVWJD1sjKeFISQtAxAG0_4kmDHSc8VrSB1VM0f8kk4RQ"}
    """;
    JWK jwkpriv = JWK.parse(privateKey);
    RSAKey rsaPrivateKey = (RSAKey) jwkpriv;

    RSAPrivateKey reconstructedPrivateKey = rsaPrivateKey.toRSAPrivateKey();
    RSAPublicKey reconstructedPublicKey = (RSAPublicKey) rsaPrivateKey.toPublicKey();

    RSAKey rsaKey =
        new RSAKey.Builder(reconstructedPublicKey)
            .privateKey(reconstructedPrivateKey)
            .keyID("kid")
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);

    KeyPair keyPair = new KeyPair(rsaKey, jwkSet);
    log.error("Public Key: \n\n{}\n\n", keyPair.jwkSet().toString());

    String clientId = "yvLfKZWBeRD0MM6NqwUhwgyTtvuCfs1EbpdiLx9CwGw";

    JwsHeader assertionHeader =
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyPair.rsaKey().getKeyID()).build();

    JwtEncoder clientJwtEncoder =
        new NimbusJwtEncoder((selector, ctx) -> selector.select(keyPair.jwkSet()));

    JwtClaimsSet assertionClaims =
        JwtClaimsSet.builder()
            .issuer(clientId)
            .subject(clientId)
            .audience(List.of("http://localhost:8080/oauth2/token"))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
            .build();

    String clientAssertion =
        clientJwtEncoder
            .encode(JwtEncoderParameters.from(assertionHeader, assertionClaims))
            .getTokenValue(); // Test implementation goes here
    log.error("Client Assertion: \n\n{}\n\n", clientAssertion);
  }

  @Test
  public void creaatePKCE() throws Exception {
    // code verifier
    // code challenge
    // code challenge method

    String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    String s = generateCodeChallengeS256(codeVerifier);

    log.error("Code Challenge: \n\n{}\n\n", s);
    log.error("Code Verifier: \n\n{}\n\n", codeVerifier);
  }

  private static String generateCodeChallengeS256(String verifier) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
    return base64UrlNoPad(digest);
  }

  private static String base64UrlNoPad(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }
}
