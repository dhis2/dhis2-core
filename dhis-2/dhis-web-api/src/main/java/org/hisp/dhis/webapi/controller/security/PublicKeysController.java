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
package org.hisp.dhis.webapi.controller.security;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Controller
@RequestMapping( "/publicKeys" )
@RequiredArgsConstructor
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class PublicKeysController
{

    private final DhisOidcProviderRepository clientRegistrationRepository;

    @GetMapping( value = "/{clientId}/jwks.json", produces = APPLICATION_JSON_VALUE )
    public @ResponseBody Map<String, Object> getKeys( @PathVariable( "clientId" ) String clientId )
        throws WebMessageException
    {
        DhisOidcClientRegistration dhisOidcClientRegistration = clientRegistrationRepository
            .getDhisOidcClientRegistration( clientId );

        JwsAlgorithm jwsAlgorithm = resolveAlgorithm( dhisOidcClientRegistration.getJwk() );
        if ( jwsAlgorithm == null )
        {
            throw new WebMessageException( conflict( ErrorCode.E3040.getMessage(), ErrorCode.E3040 ) );
        }

        RSAKey.Builder builder = new RSAKey.Builder( dhisOidcClientRegistration.getRsaPublicKey() )
            .keyUse( KeyUse.SIGNATURE )
            .algorithm( JWSAlgorithm.parse( jwsAlgorithm.toString() ) )
            .keyID( dhisOidcClientRegistration.getKeyId() );

        return new JWKSet( builder.build() ).toJSONObject();
    }

    private static JwsAlgorithm resolveAlgorithm( JWK jwk )
    {
        JwsAlgorithm jwsAlgorithm = null;

        if ( jwk.getAlgorithm() != null )
        {
            jwsAlgorithm = SignatureAlgorithm.from( jwk.getAlgorithm().getName() );
            if ( jwsAlgorithm == null )
            {
                jwsAlgorithm = MacAlgorithm.from( jwk.getAlgorithm().getName() );
            }
        }

        if ( jwsAlgorithm == null )
        {
            if ( KeyType.RSA.equals( jwk.getKeyType() ) )
            {
                jwsAlgorithm = SignatureAlgorithm.RS256;
            }
            else if ( KeyType.EC.equals( jwk.getKeyType() ) )
            {
                jwsAlgorithm = SignatureAlgorithm.ES256;
            }
            else if ( KeyType.OCT.equals( jwk.getKeyType() ) )
            {
                jwsAlgorithm = MacAlgorithm.HS256;
            }
        }

        return jwsAlgorithm;
    }
}
