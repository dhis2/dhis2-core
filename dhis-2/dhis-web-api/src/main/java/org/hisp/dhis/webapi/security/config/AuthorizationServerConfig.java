/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
public class AuthorizationServerConfig {

  @Autowired
  private TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(
            authorizationServerConfigurer,
            (authorizationServer) ->
                authorizationServer.oidc(Customizer.withDefaults()) // Enable OpenID Connect 1.0
            )
        .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
        .exceptionHandling(
            (exceptions) ->
                exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/dhis-web-login/"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  //  @Bean
  //  @Order(2)
  //  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
  //        http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
  //            // Form login handles the redirect to the login page from the
  //            // authorization server filter chain
  //            .formLogin(Customizer.withDefaults());

  //    http.formLogin(formLogin -> formLogin
  //        .authenticationDetailsSource(twoFactorWebAuthenticationDetailsSource)
  //        .loginPage("/XXX.html")
  //        .usernameParameter("j_username")
  //        .passwordParameter("j_password")
  //        .loginProcessingUrl("/loginAction")
  //        .defaultSuccessUrl("/dhis-web-dashboard", true)
  //        .failureUrl("/index.html?error=true")
  //    );
  //
  ////    http.authorizeHttpRequests(
  ////            (authorize) ->
  ////                authorize.requestMatchers(new AntPathRequestMatcher("/login")).permitAll())
  //
  //        // Form login handles the redirect to the login page from the
  //        // authorization server filter chain
  //        //    http
  //        http.formLogin(
  //            form -> form.authenticationDetailsSource(twoFactorWebAuthenticationDetailsSource)
  //                .loginPage("/XXX.html")
  //                .loginProcessingUrl("/login")
  //        );
  //
  //    //        .logout(logout -> logout.logoutUrl("/dhis-web-commons-security/logout.action"));
  //
  //    return http.build();
  //  }

  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer(UserService userService) {
    return (context) -> {
      OAuth2TokenType tokenType = context.getTokenType();
      if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
        String username = context.getPrincipal().getName();
        User user = userService.getUserByUsername(username);
        String email = user.getEmail();
        Builder claims = context.getClaims();
        claims.claim("email", email);
      }

      //      if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
      //        // Load the user info (this should include the email if available)
      //        //        OidcUserInfo userInfo =
      // userService.loadUser(context.getPrincipal().getName());
      //        // Merge all user info claims into the ID token claims
      //        //        context.getClaims().claims((claims) ->
      // claims.putAll(userInfo.getClaims()));
      //
      //        String username = context.getPrincipal().getName();
      //        User user = userService.getUserByUsername(username);
      //
      //        context.getClaims().claim("email", user.getEmail());
      //      }

      if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
        // Load the user info (this should include the email if available)
        //        OidcUserInfo userInfo = userService.loadUser(context.getPrincipal().getName());
        // Merge all user info claims into the ID token claims
        //        context.getClaims().claims((claims) -> claims.putAll(userInfo.getClaims()));

        String username = context.getPrincipal().getName();
        User user = userService.getUserByUsername(username);

        context.getClaims().claim("email", user.getEmail());
      }
    };
  }

  //  @Bean
  //  public RegisteredClientRepository registeredClientRepository() {
  //    RegisteredClient oidcClient =
  //        RegisteredClient.withId(UUID.randomUUID().toString())
  //            .clientId("dhis2-client")
  //            .clientSecret("$2a$12$FtWBAB.hWkR3SSul7.HWROr8/aEuUEjywnB86wrYz0HtHh4iam6/G")
  //            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
  //            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
  //            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
  //            //            .redirectUri("http://localhost:9090/oauth2/code/dhis2-client")
  //            .redirectUri("http://localhost:8080/oauth2/code/xxx")
  //            .postLogoutRedirectUri("http://127.0.0.1:8080/")
  //            .scope(OidcScopes.OPENID)
  //            .scope(OidcScopes.PROFILE)
  //            .scope(OidcScopes.EMAIL)
  //            .scope(StandardClaimNames.EMAIL)
  //            .scope(StandardClaimNames.EMAIL_VERIFIED)
  //            .clientSettings(ClientSettings.builder().requireAuthorizationConsent(true).build())
  //            .build();
  //
  //    return new InMemoryRegisteredClientRepository(oidcClient);
  //  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }
}
