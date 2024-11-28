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
package org.hisp.dhis.webapi.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.SystemAuthoritiesProvider;
import org.hisp.dhis.security.apikey.DhisApiTokenAuthenticationEntryPoint;
import org.hisp.dhis.security.basic.HttpBasicWebAuthenticationDetailsSource;
import org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver;
import org.hisp.dhis.security.jwt.DhisBearerJwtTokenAuthenticationEntryPoint;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.oidc.DhisAuthorizationCodeTokenResponseClient;
import org.hisp.dhis.security.oidc.DhisCustomAuthorizationRequestResolver;
import org.hisp.dhis.security.oidc.DhisOidcLogoutSuccessHandler;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.webapi.filter.CspFilter;
import org.hisp.dhis.webapi.filter.DhisCorsProcessor;
import org.hisp.dhis.webapi.security.Http401LoginUrlAuthenticationEntryPoint;
import org.hisp.dhis.webapi.security.apikey.ApiTokenAuthManager;
import org.hisp.dhis.webapi.security.apikey.Dhis2ApiTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The {@code DhisWebApiWebSecurityConfig} class configures mostly all authentication and
 * authorization related to the /api endpoint.
 *
 * <p>Almost all other endpoints are configured in {@code DhisWebCommonsWebSecurityConfig}
 *
 * <p>The biggest practical benefit of having separate configs for /api and the rest is that we can
 * start a server only serving request to /api/**
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order(1999)
public class DhisWebApiWebSecurityConfig {
  private static String apiContextPath = "/api";

  public static void setApiContextPath(String apiContextPath) {
    DhisWebApiWebSecurityConfig.apiContextPath = apiContextPath;
  }

  @Autowired public DataSource dataSource;

  @Autowired private DhisConfigurationProvider dhisConfig;

  @Autowired private DefaultAuthenticationEventPublisher authenticationEventPublisher;

  @Autowired private Dhis2JwtAuthenticationManagerResolver dhis2JwtAuthenticationManagerResolver;

  @Autowired private DhisBearerJwtTokenAuthenticationEntryPoint bearerTokenEntryPoint;

  @Autowired private DhisApiTokenAuthenticationEntryPoint apiTokenAuthenticationEntryPoint;

  @Autowired
  private TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource;

  @Autowired private DhisOidcLogoutSuccessHandler dhisOidcLogoutSuccessHandler;

  @Autowired
  private HttpBasicWebAuthenticationDetailsSource httpBasicWebAuthenticationDetailsSource;

  @Autowired private ConfigurationService configurationService;

  @Autowired private ApiTokenAuthManager apiTokenAuthManager;

  @Autowired private DhisOidcProviderRepository dhisOidcProviderRepository;

  @Autowired private DhisCustomAuthorizationRequestResolver dhisCustomAuthorizationRequestResolver;

  @Autowired private DhisAuthorizationCodeTokenResponseClient jwtPrivateCodeTokenResponseClient;

  @Autowired private RequestCache requestCache;

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  private static class CustomRequestMatcher implements RequestMatcher {

    private final List<String> excludePatterns =
        List.of("", "/", "/dhis-web-login", "/dhis-web-login/");

    @Override
    public boolean matches(HttpServletRequest request) {
      String requestURI = request.getRequestURI();
      return excludePatterns.stream().noneMatch(pattern -> pattern.equals(requestURI));
    }
  }

  @Bean
  public RequestCache requestCache() {
    HttpSessionRequestCache httpSessionRequestCache = new HttpSessionRequestCache();
    httpSessionRequestCache.setRequestMatcher(new CustomRequestMatcher());
    return httpSessionRequestCache;
  }

  @Bean(name = "customAuthenticationManager")
  @Primary
  protected AuthenticationManager authenticationManagers(
      TwoFactorAuthenticationProvider twoFactorProvider,
      @Qualifier("customLdapAuthenticationProvider")
          CustomLdapAuthenticationProvider ldapProvider) {

    ProviderManager providerManager =
        new ProviderManager(Arrays.asList(twoFactorProvider, ldapProvider));

    providerManager.setAuthenticationEventPublisher(authenticationEventPublisher);

    return providerManager;
  }

  static final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
        HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      /*
       * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
       * the CsrfToken when it is rendered in the response body.
       */
      this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      /*
       * If the request contains a request header, use CsrfTokenRequestAttributeHandler
       * to resolve the CsrfToken. This applies when a single-page application includes
       * the header value automatically, which was obtained via a cookie containing the
       * raw CsrfToken.
       */
      if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
        return super.resolveCsrfTokenValue(request, csrfToken);
      }
      /*
       * In all other cases (e.g. if the request contains a request parameter), use
       * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
       * when a server-side rendered form includes the _csrf request parameter as a
       * hidden input.
       */
      return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
  }

  static final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");
      // Render the token value to a cookie by causing the deferred token to be loaded
      csrfToken.getToken();

      filterChain.doFilter(request, response);
    }
  }

  @Bean
  protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    if (dhisConfig.isEnabled(ConfigurationKey.CSRF_ENABLED)) {
      http.csrf(
              c ->
                  c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                      .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
          .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
    } else {
      http.csrf(AbstractHttpConfigurer::disable);
    }

    http.cors(Customizer.withDefaults());
    http.requestCache().requestCache(requestCache);

    configureMatchers(http);
    configureFormLogin(http);
    configureCspFilter(http, dhisConfig, configurationService);
    configureApiTokenAuthorizationFilter(http);
    configureOAuthTokenFilters(http);

    setHttpHeaders(http);

    return http.build();
  }

  private void configureFormLogin(HttpSecurity http) throws Exception {
    http.formLogin()
        .authenticationDetailsSource(twoFactorWebAuthenticationDetailsSource)
        .loginPage("/dhis-web-login/")
        .usernameParameter("j_username")
        .passwordParameter("j_password")
        .loginProcessingUrl("/api/authentication/login")
        .failureUrl("/dhis-web-login/?error=true")
        .defaultSuccessUrl("/dhis-web-dashboard/", true)
        .permitAll();
  }

  public static void setHttpHeaders(HttpSecurity http) throws Exception {
    http.headers()
        .defaultsDisabled()
        .contentTypeOptions()
        .and()
        .xssProtection()
        .and()
        .httpStrictTransportSecurity();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return web ->
        web.debug(false)
            .ignoring()
            .requestMatchers(
                new AntPathRequestMatcher("/api/ping"), new AntPathRequestMatcher("/favicon.ico"));
  }

  private void configureMatchers(HttpSecurity http) throws Exception {
    http.securityContext(
        httpSecuritySecurityContextConfigurer ->
            httpSecuritySecurityContextConfigurer.requireExplicitSave(true));

    Set<String> providerIds = dhisOidcProviderRepository.getAllRegistrationId();
    http.authorizeHttpRequests(
            authorize -> {
              providerIds.forEach(
                  providerId ->
                      authorize
                          .requestMatchers(
                              new AntPathRequestMatcher("/oauth2/authorization/" + providerId))
                          .permitAll()
                          .requestMatchers(new AntPathRequestMatcher("/oauth2/code/" + providerId))
                          .permitAll());

              authorize
                  .requestMatchers(analyticsPluginResources())
                  .permitAll()

                  // BUNDLED APPS
                  ////////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-dashboard/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-dashboard")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-pivot/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-pivot")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-visualizer/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-visualizer")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-data-visualizer/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-data-visualizer")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-mapping/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-mapping")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-maps/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-maps")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-event-reports/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-event-reports")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-event-visualizer/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-event-visualizer")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-interpretation/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-interpretation")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-settings/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-settings")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-maintenance/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-maintenance")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-app-management/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-app-management")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-usage-analytics/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-usage-analytics")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-event-capture/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-event-capture")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-cache-cleaner/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-cache-cleaner")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-data-administration/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-data-administration")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-data-quality/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-data-quality")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-messaging/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-messaging")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-datastore/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-datastore")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-scheduler/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-scheduler")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-sms-configuration/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-sms-configuration")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-user/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-user")
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-aggregate-data-entry/**"))
                  .hasAnyAuthority("ALL", "M_dhis-web-aggregate-data-entry")

                  /////////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-login/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/login.html"))
                  .permitAll()

                  /////////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-commons/oidc/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-commons/javascripts/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-commons/css/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-commons/flags/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/dhis-web-commons/fonts/**"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher("/dhis-web-commons/security/logo_front.png"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher("/dhis-web-commons/security/logo_mobile.png"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/external-static/**"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/favicon.ico"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher("/static/**"))
                  .permitAll()

                  ///////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/locales/ui"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/loginConfig"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/auth/login"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/auth/forgotPassword"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/auth/passwordReset"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/auth/registration"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/auth/invite"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/authentication/login"))
                  .permitAll()
                  // Needs to be here because this overrides the previous one
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/account/recovery"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/account/restore"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/account"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/staticContent/**"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/externalFileResources/**"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/icons/*/icon.svg"))
                  .permitAll()
                  .requestMatchers(
                      new AntPathRequestMatcher(apiContextPath + "/**/files/style/external"))
                  .permitAll()
                  .requestMatchers(new AntPathRequestMatcher(apiContextPath + "/**/publicKeys/**"))
                  .permitAll()

                  ///////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher("/oauth2/**"))
                  .permitAll()

                  ///////////////////////////////////////////////////////////////////////////////////////////////

                  .requestMatchers(new AntPathRequestMatcher("/**"))
                  .authenticated();
            })

        /// HTTP BASIC///////////////////////////////////////
        .httpBasic()
        .authenticationDetailsSource(httpBasicWebAuthenticationDetailsSource)
        .addObjectPostProcessor(
            new ObjectPostProcessor<BasicAuthenticationFilter>() {
              @Override
              public <O extends BasicAuthenticationFilter> O postProcess(O filter) {
                // Explicitly set security context repository on http basic, is
                // NullSecurityContextRepository by default now.
                filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
                return filter;
              }
            });

    /// OIDC /////////
    http.oauth2Login(
            oauth2 ->
                oauth2
                    .tokenEndpoint()
                    .accessTokenResponseClient(jwtPrivateCodeTokenResponseClient)
                    .and()
                    .failureUrl("/dhis-web-login/?oidcFailure=true")
                    .clientRegistrationRepository(dhisOidcProviderRepository)
                    .loginProcessingUrl("/oauth2/code/*")
                    .authorizationEndpoint()
                    .authorizationRequestResolver(dhisCustomAuthorizationRequestResolver))

        ///////////////
        .exceptionHandling()
        .authenticationEntryPoint(entryPoint())
        .and()
        /// SESSION ////////////////
        /// LOGOUT //////////////////
        .logout()
        .logoutUrl("/dhis-web-commons-security/logout.action")
        .logoutSuccessHandler(dhisOidcLogoutSuccessHandler)
        .deleteCookies("JSESSIONID")
        .and()
        ////////////////////
        .sessionManagement()
        .sessionFixation()
        .migrateSession()
        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
        .enableSessionUrlRewriting(false)
        .maximumSessions(
            Integer.parseInt(dhisConfig.getProperty(ConfigurationKey.MAX_SESSIONS_PER_USER)))
        .expiredUrl("/dhis-web-commons-security/logout.action");
  }

  @Bean
  public Http401LoginUrlAuthenticationEntryPoint entryPoint() {
    // Converts to a HTTP basic login if "XMLHttpRequest".equals(
    // request.getHeader( "X-Requested-With" ) )
    return new Http401LoginUrlAuthenticationEntryPoint("/dhis-web-login/");
  }

  @Bean
  public RequestMatcher analyticsPluginResources() {
    String pattern =
        ".*(dhis-web-mapping\\/map.js|dhis-web-visualizer\\/chart.js|dhis-web-maps\\"
            + "/map.js|dhis-web-event-reports\\/eventreport.js|dhis-web-event-visualizer\\/eventchart.js|dhis-web-pivot\\/reporttable.js)";

    return new org.springframework.security.web.util.matcher.RegexRequestMatcher(pattern, "GET");
  }

  @Bean
  public CorsFilter corsFilter(ConfigurationService configurationService) {
    CorsFilter corsFilter = new CorsFilter(new UrlBasedCorsConfigurationSource());
    corsFilter.setCorsProcessor(new DhisCorsProcessor(configurationService));
    return corsFilter;
  }

  private void configureCspFilter(
      HttpSecurity http,
      DhisConfigurationProvider dhisConfig,
      ConfigurationService configurationService) {
    http.addFilterBefore(new CspFilter(dhisConfig, configurationService), HeaderWriterFilter.class);
  }

  private void configureApiTokenAuthorizationFilter(HttpSecurity http) {
    if (dhisConfig.isEnabled(ConfigurationKey.ENABLE_API_TOKEN_AUTHENTICATION)) {
      Dhis2ApiTokenFilter tokenFilter =
          new Dhis2ApiTokenFilter(
              apiTokenAuthManager, apiTokenAuthenticationEntryPoint, authenticationEventPublisher);

      http.addFilterBefore(tokenFilter, BasicAuthenticationFilter.class);
    }
  }

  /**
   * Enable either deprecated OAuth2 authorization filter or the new JWT OIDC token filter. They are
   * mutually exclusive and can not both be added to the chain at the same time.
   *
   * @param http HttpSecurity config
   */
  private void configureOAuthTokenFilters(HttpSecurity http) {
    if (dhisConfig.isEnabled(ConfigurationKey.ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION)) {
      http.addFilterAfter(getJwtBearerTokenAuthenticationFilter(), BasicAuthenticationFilter.class);
    }
  }

  /**
   * Creates and configures the JWT OIDC bearer token filter
   *
   * @return BearerTokenAuthenticationFilter to be added to the filter chain
   */
  private org.springframework.security.oauth2.server.resource.web.authentication
          .BearerTokenAuthenticationFilter
      getJwtBearerTokenAuthenticationFilter() {

    org.springframework.security.oauth2.server.resource.web.authentication
            .BearerTokenAuthenticationFilter
        jwtFilter =
            new org.springframework.security.oauth2.server.resource.web.authentication
                .BearerTokenAuthenticationFilter(dhis2JwtAuthenticationManagerResolver);

    jwtFilter.setAuthenticationEntryPoint(bearerTokenEntryPoint);
    jwtFilter.setBearerTokenResolver(new DefaultBearerTokenResolver());

    // Placeholder failure handler to "activate" the sending of auth failed
    // messages
    // to the central auth logger in DHIS2:
    // "AuthenticationLoggerListener"
    jwtFilter.setAuthenticationFailureHandler(
        (request, response, exception) -> {
          authenticationEventPublisher.publishAuthenticationFailure(
              exception,
              new AbstractAuthenticationToken(null) {
                @Override
                public Object getCredentials() {
                  return null;
                }

                @Override
                public Object getPrincipal() {
                  return null;
                }
              });

          bearerTokenEntryPoint.commence(request, response, exception);
        });

    return jwtFilter;
  }

  @Primary
  @Bean("org.hisp.dhis.security.SystemAuthoritiesProvider")
  public SystemAuthoritiesProvider systemAuthoritiesProvider() {
    return Authorities::getAllAuthorities;
  }
}
