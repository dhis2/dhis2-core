/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.security.jwt;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_DHIS2_INTERNAL_CLIENT_ID;

import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.oauth2.client.Dhis2OAuth2ClientService;
import org.hisp.dhis.security.oidc.DhisOidcClientRegistration;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserStore;
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

/**
 * Represent a custom AuthenticationManagerResolver to resolve authenticate JWT bearer token
 * requests. This class will look up the corresponding issuer configuration ({@link
 * DhisOidcClientRegistration}), based on the JWT "issuer" field information in the token and create
 * a new {@link DhisJwtAuthenticationProvider} with the resolved config looked up in the {@link
 * DhisOidcProviderRepository}
 *
 * <p>It will also create the authentication method to be called for authenticating the request.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 * @see
 *     org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver.DhisJwtAuthenticationProvider
 * @see org.hisp.dhis.security.oidc.DhisOidcProviderRepository
 * @see AuthenticationManagerResolver
 */
@Component
public class Dhis2JwtAuthenticationManagerResolver
    implements AuthenticationManagerResolver<HttpServletRequest> {

  @Autowired private DhisConfigurationProvider config;
  @Autowired private UserStore userStore;
  @Autowired private DhisOidcProviderRepository clientRegistrationRepository;
  @Autowired private Dhis2OAuth2ClientService oAuth2ClientService;
  @Autowired private UserService userService;

  private final Map<String, AuthenticationManager> authenticationManagers =
      new ConcurrentHashMap<>();

  private final Converter<HttpServletRequest, String> issuerConverter =
      new JwtClaimIssuerConverter();

  private JwtDecoder jwtDecoder;

  public void setJwtDecoder(JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
  }

  @Override
  public AuthenticationManager resolve(HttpServletRequest request) {
    String issuer = this.issuerConverter.convert(request);
    if (issuer == null) {
      throw new InvalidBearerTokenException("Missing issuer");
    }
    return getAuthenticationManager(issuer);
  }

  /**
   * Looks for a DhisOidcClientRegistration in the DhisOidcProviderRepository that matches the input
   * JWT "issuer". It creates a new DhisJwtAuthenticationProvider if it finds a matching config.
   *
   * <p>The DhisJwtAuthenticationProvider is configured with a custom {@link Converter} that
   * "converts" the incoming JWT token into a {@link DhisJwtAuthenticationToken}.
   *
   * <p>It also configures a JWT decoder that "decodes" incoming JSON string into a JWT token
   * ({@link Jwt}
   *
   * @param issuer JWT issuer to look up
   * @return a DhisJwtAuthenticationProvider
   */
  private AuthenticationManager getAuthenticationManager(String issuer) {
    return this.authenticationManagers.computeIfAbsent(
        issuer,
        s -> {
          Set<String> allRegistrationId = clientRegistrationRepository.getAllRegistrationId();
          int size = allRegistrationId.size();

          DhisOidcClientRegistration clientRegistration =
              clientRegistrationRepository.findByIssuerUri(issuer);
          if (clientRegistration == null) {
            throw new InvalidBearerTokenException("Invalid issuer");
          }
          // Create the authentication provider
          Converter<Jwt, DhisJwtAuthenticationToken> authConverter =
              getTokenConverter(clientRegistration);
          JwtDecoder decoder = getDecoder(issuer);
          return new DhisJwtAuthenticationProvider(decoder, authConverter)::authenticate;
        });
  }

  // Get a JwtDecoder based on the issuer. If a custom decoder has been set directly, use that one.
  private JwtDecoder getDecoder(String issuer) {
    // Use custom decoder if set directly, used in tests.
    if (jwtDecoder != null) {
      return jwtDecoder;
    }
    return JwtDecoders.fromIssuerLocation(issuer);
  }

  /**
   * Creates a custom Converter that converts a Jwt token into a DhisJwtAuthenticationToken.
   *
   * <p>This implementation will look for a user mapping claim value in the token and look up the
   * user based on this value. If a matching user is not found, an exception is thrown.
   *
   * @param clientRegistration the oidc client registration (IdP) that "owning/created" the token
   * @return a Converter that converts a Jwt token into a DhisJwtAuthenticationToken
   */
  private Converter<Jwt, DhisJwtAuthenticationToken> getTokenConverter(
      DhisOidcClientRegistration clientRegistration) {
    return jwt -> {
      List<String> audience = jwt.getAudience();

      // This is only set when when Authorization Server is enabled.
      String internalDhis2ClientId = config.getProperty(OIDC_DHIS2_INTERNAL_CLIENT_ID);

      if (clientRegistration.getClientRegistration().getClientId().equals(internalDhis2ClientId)) {
        // Special case for the dhis2-client client id, look up the audience which is a client id
        // registered in the DHIS2 server.
        boolean allMatch =
            audience.stream().allMatch(a -> oAuth2ClientService.findByClientId(a) != null);
        if (!allMatch) {
          throw new InvalidBearerTokenException("Invalid audience");
        }
      } else {
        Collection<String> clientIds = clientRegistration.getClientIds();
        Set<String> matchedClientIds =
            clientIds.stream().filter(audience::contains).collect(Collectors.toSet());
        if (matchedClientIds.isEmpty()) {
          throw new InvalidBearerTokenException("Invalid audience");
        }
      }

      String mappingClaimKey = clientRegistration.getMappingClaimKey();
      String mappingValue = jwt.getClaim(mappingClaimKey);
      User user;
      if (mappingClaimKey.equals("username")) {
        user = userStore.getUserByUsername(mappingValue);
      } else if (mappingClaimKey.equals("email")) {
        user = userStore.getUserByOpenId(mappingValue);
      } else {
        throw new InvalidBearerTokenException("Invalid mapping claim");
      }

      if (user == null) {
        throw new InvalidBearerTokenException(
            String.format(
                "Found no matching DHIS2 user for the mapping claim: '%s' with the value: '%s'",
                mappingClaimKey, mappingValue));
      }

      UserDetails currentUserDetails = userService.createUserDetails(user);
      Collection<GrantedAuthority> grantedAuthorities = user.getAuthorities();

      return new DhisJwtAuthenticationToken(
          jwt, grantedAuthorities, mappingValue, currentUserDetails);
    };
  }

  /**
   * Custom AuthenticationProvider that validates/authenticate the incoming Bearer token, converts
   * it to a Jwt token and then to a DhisJwtAuthenticationToken.
   */
  private record DhisJwtAuthenticationProvider(
      JwtDecoder jwtDecoder, Converter<Jwt, DhisJwtAuthenticationToken> jwtAuthenticationConverter)
      implements AuthenticationProvider {

    private DhisJwtAuthenticationProvider {
      checkNotNull(jwtDecoder);
      checkNotNull(jwtAuthenticationConverter);
    }

    @Override
    public Authentication authenticate(Authentication authentication)
        throws AuthenticationException {

      BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;
      Jwt jwt = decodeTokenString(bearer.getToken());
      DhisJwtAuthenticationToken token = this.jwtAuthenticationConverter.convert(jwt);
      if (token == null) {
        throw new InvalidBearerTokenException("Invalid token");
      }

      token.setAuthenticated(true);
      token.setDetails(bearer.getDetails());

      return token;
    }

    private Jwt decodeTokenString(String token) {
      try {
        return this.jwtDecoder.decode(token);
      } catch (BadJwtException failed) {
        throw new InvalidBearerTokenException(failed.getMessage(), failed);
      } catch (JwtException failed) {
        throw new AuthenticationServiceException(failed.getMessage(), failed);
      }
    }

    @Override
    public boolean supports(Class<?> authentication) {
      return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
  }

  /** Custom converter that extracts the "issuer" claim from the incoming JWT bearer token/string */
  private static class JwtClaimIssuerConverter implements Converter<HttpServletRequest, String> {
    private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();

    @Override
    public String convert(HttpServletRequest request) {
      String token = this.resolver.resolve(request);
      try {
        String issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
        if (issuer != null) {
          return issuer;
        }
      } catch (Exception ex) {
        throw new InvalidBearerTokenException(ex.getMessage(), ex);
      }
      throw new InvalidBearerTokenException("Missing issuer");
    }
  }
}
