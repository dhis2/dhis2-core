/*
 * Copyright (c) 2004-2020-2021, University of Oslo
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
package org.hisp.dhis.webapi.utils;

/**
 * The Registered Header Parameter Names defined by the JSON Web Token (JWT),
 * JSON Web Signature (JWS) and JSON Web Encryption (JWE) specifications that
 * may be contained in the JOSE Header of a JWT.
 *
 * @author Anoop Garlapati
 * @author Joe Grandja
 * @since 0.0.1
 * @see JoseHeader
 * @see <a target="_blank" href=
 *      "https://tools.ietf.org/html/rfc7519#section-5">JWT JOSE Header</a>
 * @see <a target="_blank" href=
 *      "https://tools.ietf.org/html/rfc7515#section-4">JWS JOSE Header</a>
 * @see <a target="_blank" href=
 *      "https://tools.ietf.org/html/rfc7516#section-4">JWE JOSE Header</a>
 */
public final class JoseHeaderNames
{

    /**
     * {@code alg} - the algorithm header identifies the cryptographic algorithm
     * used to secure a JWS or JWE
     */
    public static final String ALG = "alg";

    /**
     * {@code jku} - the JWK Set URL header is a URI that refers to a resource
     * for a set of JSON-encoded public keys, one of which corresponds to the
     * key used to digitally sign a JWS or encrypt a JWE
     */
    public static final String JKU = "jku";

    /**
     * {@code jwk} - the JSON Web Key header is the public key that corresponds
     * to the key used to digitally sign a JWS or encrypt a JWE
     */
    public static final String JWK = "jwk";

    /**
     * {@code kid} - the key ID header is a hint indicating which key was used
     * to secure a JWS or JWE
     */
    public static final String KID = "kid";

    /**
     * {@code x5u} - the X.509 URL header is a URI that refers to a resource for
     * the X.509 public key certificate or certificate chain corresponding to
     * the key used to digitally sign a JWS or encrypt a JWE
     */
    public static final String X5U = "x5u";

    /**
     * {@code x5c} - the X.509 certificate chain header contains the X.509
     * public key certificate or certificate chain corresponding to the key used
     * to digitally sign a JWS or encrypt a JWE
     */
    public static final String X5C = "x5c";

    /**
     * {@code x5t} - the X.509 certificate SHA-1 thumbprint header is a
     * base64url-encoded SHA-1 thumbprint (a.k.a. digest) of the DER encoding of
     * the X.509 certificate corresponding to the key used to digitally sign a
     * JWS or encrypt a JWE
     */
    public static final String X5T = "x5t";

    /**
     * {@code x5t#S256} - the X.509 certificate SHA-256 thumbprint header is a
     * base64url-encoded SHA-256 thumbprint (a.k.a. digest) of the DER encoding
     * of the X.509 certificate corresponding to the key used to digitally sign
     * a JWS or encrypt a JWE
     */
    public static final String X5T_S256 = "x5t#S256";

    /**
     * {@code typ} - the type header is used by JWS/JWE applications to declare
     * the media type of a JWS/JWE
     */
    public static final String TYP = "typ";

    /**
     * {@code cty} - the content type header is used by JWS/JWE applications to
     * declare the media type of the secured content (the payload)
     */
    public static final String CTY = "cty";

    /**
     * {@code crit} - the critical header indicates that extensions to the
     * JWS/JWE/JWA specifications are being used that MUST be understood and
     * processed
     */
    public static final String CRIT = "crit";

    private JoseHeaderNames()
    {
    }

}
