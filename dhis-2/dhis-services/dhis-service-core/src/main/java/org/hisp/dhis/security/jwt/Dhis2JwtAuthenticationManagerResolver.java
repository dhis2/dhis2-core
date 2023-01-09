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
package org.hisp.dhis.security.jwt;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.User;
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
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

import com.nimbusds.jwt.JWTParser;

/**
 * Represent a custom AuthenticationManagerResolver to resolve authenticate JWT
 * bearer token requests. This class will look up the corresponding issuer
 * configuration ({@link DhisOidcClientRegistration}), based on the JWT "issuer"
 * field information in the token and create a new
 * {@link DhisJwtAuthenticationProvider} with the resolved config looked up in
 * the {@link DhisOidcProviderRepository}
 *
 * <p>
 * It will also create the authentication method to be called for authenticating
 * the request.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @see org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver.DhisJwtAuthenticationProvider
 * @see org.hisp.dhis.security.oidc.DhisOidcProviderRepository
 * @see AuthenticationManagerResolver
 */
@Component
public class Dhis2JwtAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest>
{
    @Autowired
    private UserService userService;

    @Autowired
    private DhisOidcProviderRepository clientRegistrationRepository;

    private final Map<String, AuthenticationManager> authenticationManagers = new ConcurrentHashMap<>();

    private final Converter<HttpServletRequest, String> issuerConverter = new JwtClaimIssuerConverter();

    private JwtDecoder jwtDecoder;

    public void setJwtDecoder( JwtDecoder jwtDecoder )
    {
        this.jwtDecoder = jwtDecoder;
    }

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

    /**
     * Looks for a DhisOidcClientRegistration in the DhisOidcProviderRepository
     * that matches the input JWT "issuer". It creates a new
     * DhisJwtAuthenticationProvider if it finds a matching config.
     * <p>
     * The DhisJwtAuthenticationProvider is configured with a custom
     * {@link Converter} that "converts" the incoming JWT token into a
     * {@link DhisJwtAuthenticationToken}.
     * <p>
     * It also configures a JWT decoder that "decodes" incoming JSON string into
     * a JWT token ({@link Jwt}
     *
     * @param issuer JWT issuer to look up
     *
     * @return a DhisJwtAuthenticationProvider
     */
    private AuthenticationManager getAuthenticationManager( String issuer )
    {
        return this.authenticationManagers.computeIfAbsent( issuer, s -> {

            DhisOidcClientRegistration clientRegistration = clientRegistrationRepository.findByIssuerUri( issuer );
            if ( clientRegistration == null )
            {
                throw new InvalidBearerTokenException( "Invalid issuer" );
            }

            Converter<Jwt, DhisJwtAuthenticationToken> authConverter = getConverter( clientRegistration );
            JwtDecoder decoder = getDecoder( issuer );

            return new DhisJwtAuthenticationProvider( decoder, authConverter )::authenticate;
        } );
    }

    private JwtDecoder getDecoder( String issuer )
    {
        if ( jwtDecoder != null )
        {
            return jwtDecoder;
        }

        return JwtDecoders.fromIssuerLocation( issuer );
    }

    private Converter<Jwt, DhisJwtAuthenticationToken> getConverter( DhisOidcClientRegistration clientRegistration )
    {
        return jwt -> {
            List<String> audience = jwt.getAudience();

            Collection<String> clientIds = clientRegistration.getClientIds();

            Set<String> matchedClientIds = clientIds.stream()
                .filter( audience::contains )
                .collect( Collectors.toSet() );

            if ( matchedClientIds.isEmpty() )
            {
                throw new InvalidBearerTokenException( "Invalid audience" );
            }

            String mappingClaimKey = clientRegistration.getMappingClaimKey();
            String mappingValue = jwt.getClaim( mappingClaimKey );

            User user = userService.getUserByOpenId( mappingValue );
            if ( user == null )
            {
                throw new InvalidBearerTokenException( String.format(
                    "Found no matching DHIS2 user for the mapping claim:'%s' with the value:'%s'",
                    mappingClaimKey, mappingValue ) );
            }

            CurrentUserDetails currentUserDetails = userService.validateAndCreateUserDetails( user,
                user.getPassword() );

            Collection<GrantedAuthority> grantedAuthorities = user.getAuthorities();

            return new DhisJwtAuthenticationToken( jwt, grantedAuthorities, mappingValue, currentUserDetails );
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

            token.setAuthenticated( true );
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
