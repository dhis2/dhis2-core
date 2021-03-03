/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.security.jwt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.security.oidc.DhisClientRegistrationRepository;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import com.nimbusds.jwt.JWTParser;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class DhisJwtAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest>
{
    @Autowired
    private UserService userService;

    @Autowired
    private DhisClientRegistrationRepository clientRegistrationRepository;

    private final Map<String, AuthenticationManager> authenticationManagers = new ConcurrentHashMap<>();

    private final Converter<HttpServletRequest, String> issuerConverter = new JwtClaimIssuerConverter();

    @Override
    public AuthenticationManager resolve( HttpServletRequest request )
    {
        String issuer = this.issuerConverter.convert( request );
        if ( issuer == null )
        {
            throw new InvalidBearerTokenException( "Missing issuer" );
        }

        return getAuthenticationManager( issuer );
    }

    private AuthenticationManager getAuthenticationManager( String issuer )
    {
        return this.authenticationManagers.computeIfAbsent( issuer, s -> {

            DhisOidcClientRegistration clientRegistration = clientRegistrationRepository.findByIssuerUri( issuer );
            if ( clientRegistration == null )
            {
                throw new InvalidBearerTokenException( "Invalid issuer" );
            }

            JwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation( issuer );

            Converter<Jwt, DhisJwtAuthenticationToken> converter = getConverter( clientRegistration );

            return new DhisJwtAuthenticationProvider( jwtDecoder, converter )::authenticate;
        } );
    }

    private Converter<Jwt, DhisJwtAuthenticationToken> getConverter( DhisOidcClientRegistration clientRegistration )
    {
        return jwt -> {
            List<String> audience = jwt.getAudience();

            Collection<String> clientIds = clientRegistration.getClientIds();

            Set<String> matchedClientIds = clientIds.stream()
                .filter( audience::contains ).collect( Collectors.toSet() );

            if ( matchedClientIds.isEmpty() )
            {
                throw new InvalidBearerTokenException( "Invalid audience" );
            }

            String mappingClaimKey = clientRegistration.getMappingClaimKey();
            String mappingValue = jwt.getClaim( mappingClaimKey );

            UserCredentials userCredentials = userService.getUserCredentialsByOpenId( mappingValue );
            if ( userCredentials == null )
            {
                throw new InvalidBearerTokenException( String.format(
                    "Found no matching DHIS2 user for the mapping claim:'%s' with the value:'%s'",
                    mappingClaimKey, mappingValue ) );
            }

            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();

            return new DhisJwtAuthenticationToken( jwt, grantedAuthorities, mappingValue, userCredentials );
        };
    }

    private static class DhisJwtAuthenticationProvider implements AuthenticationProvider
    {
        private final JwtDecoder jwtDecoder;

        private final Converter<Jwt, DhisJwtAuthenticationToken> jwtAuthenticationConverter;

        public DhisJwtAuthenticationProvider( JwtDecoder jwtDecoder,
            Converter<Jwt, DhisJwtAuthenticationToken> jwtAuthenticationConverter )
        {
            checkNotNull( jwtDecoder );
            checkNotNull( jwtAuthenticationConverter );

            this.jwtDecoder = jwtDecoder;
            this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        }

        @Override
        public Authentication authenticate( Authentication authentication )
            throws AuthenticationException
        {
            BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;

            Jwt jwt = getJwt( bearer );

            DhisJwtAuthenticationToken token = this.jwtAuthenticationConverter.convert( jwt );
            if ( token == null )
            {
                throw new InvalidBearerTokenException( "Invalid token" );
            }

            token.setDetails( bearer.getDetails() );

            return token;
        }

        private Jwt getJwt( BearerTokenAuthenticationToken bearer )
        {
            try
            {
                return this.jwtDecoder.decode( bearer.getToken() );
            }
            catch ( BadJwtException failed )
            {
                throw new InvalidBearerTokenException( failed.getMessage(), failed );
            }
            catch ( JwtException failed )
            {
                throw new AuthenticationServiceException( failed.getMessage(), failed );
            }
        }

        @Override
        public boolean supports( Class<?> authentication )
        {
            return BearerTokenAuthenticationToken.class.isAssignableFrom( authentication );
        }
    }

    private static class JwtClaimIssuerConverter implements Converter<HttpServletRequest, String>
    {
        private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();

        @Override
        public String convert( HttpServletRequest request )
        {
            String token = this.resolver.resolve( request );

            try
            {
                String issuer = JWTParser.parse( token ).getJWTClaimsSet().getIssuer();
                if ( issuer != null )
                {
                    return issuer;
                }
            }
            catch ( Exception ex )
            {
                throw new InvalidBearerTokenException( ex.getMessage(), ex );
            }

            throw new InvalidBearerTokenException( "Missing issuer" );
        }
    }
}
