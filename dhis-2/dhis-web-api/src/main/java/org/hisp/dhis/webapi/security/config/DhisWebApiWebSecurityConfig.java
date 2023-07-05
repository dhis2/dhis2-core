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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.ImpersonatingUserDetailsChecker;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.apikey.ApiTokenService;
import org.hisp.dhis.security.apikey.DhisApiTokenAuthenticationEntryPoint;
import org.hisp.dhis.security.basic.HttpBasicWebAuthenticationDetailsSource;
import org.hisp.dhis.security.jwt.Dhis2JwtAuthenticationManagerResolver;
import org.hisp.dhis.security.jwt.DhisBearerJwtTokenAuthenticationEntryPoint;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.oidc.DhisAuthorizationCodeTokenResponseClient;
import org.hisp.dhis.security.oidc.DhisCustomAuthorizationRequestResolver;
import org.hisp.dhis.security.oidc.DhisOidcLogoutSuccessHandler;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.oidc.OIDCLoginEnabledCondition;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.filter.CorsFilter;
import org.hisp.dhis.webapi.filter.CspFilter;
import org.hisp.dhis.webapi.filter.CustomAuthenticationFilter;
import org.hisp.dhis.webapi.security.ExternalAccessVoter;
import org.hisp.dhis.webapi.security.FormLoginBasicAuthenticationEntryPoint;
import org.hisp.dhis.webapi.security.apikey.ApiTokenAuthManager;
import org.hisp.dhis.webapi.security.apikey.Dhis2ApiTokenFilter;
import org.hisp.dhis.webapi.security.switchuser.DhisSwitchUserFilter;
import org.hisp.dhis.webapi.security.vote.LogicalOrAccessDecisionManager;
import org.hisp.dhis.webapi.security.vote.SimpleAccessVoter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UrlPathHelper;

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

  @Bean
  public SessionRegistryImpl sessionRegistry() {
    return new SessionRegistryImpl();
  }

  /** This class is configuring the OIDC login endpoints */
  @Configuration
  @Order(1010)
  @Conditional(value = OIDCLoginEnabledCondition.class)
  public static class OidcSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired private DhisOidcProviderRepository dhisOidcProviderRepository;

    @Autowired
    private DhisCustomAuthorizationRequestResolver dhisCustomAuthorizationRequestResolver;

    @Autowired private DefaultAuthenticationEventPublisher authenticationEventPublisher;

    @Autowired private DhisAuthorizationCodeTokenResponseClient jwtPrivateCodeTokenResponseClient;

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
      auth.authenticationEventPublisher(authenticationEventPublisher);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      Set<String> providerIds = dhisOidcProviderRepository.getAllRegistrationId();

      http.antMatcher("/oauth2/**")
          .authorizeRequests(
              authorize -> {
                providerIds.forEach(
                    providerId ->
                        authorize
                            .antMatchers("/oauth2/authorization/" + providerId)
                            .permitAll()
                            .antMatchers("/oauth2/code/" + providerId)
                            .permitAll());
                authorize.anyRequest().authenticated();
              })
          .oauth2Login(
              oauth2 ->
                  oauth2
                      .tokenEndpoint()
                      .accessTokenResponseClient(jwtPrivateCodeTokenResponseClient)
                      .and()
                      .failureUrl("/dhis-web-commons/security/login.action?oidcFailure=true")
                      .clientRegistrationRepository(dhisOidcProviderRepository)
                      .loginProcessingUrl("/oauth2/code/*")
                      .authorizationEndpoint()
                      .authorizationRequestResolver(dhisCustomAuthorizationRequestResolver))
          .csrf()
          .disable();

      setHttpHeaders(http);
    }
  }

  /** This configuration class is responsible for setting up the /api endpoints */
  @Configuration
  @Order(1100)
  public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
    @Autowired private DhisConfigurationProvider dhisConfig;

    @Autowired private TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

    @Autowired
    @Qualifier("customLdapAuthenticationProvider")
    private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

    @Autowired private DefaultAuthenticationEventPublisher authenticationEventPublisher;

    @Autowired private Dhis2JwtAuthenticationManagerResolver dhis2JwtAuthenticationManagerResolver;

    @Autowired private DhisBearerJwtTokenAuthenticationEntryPoint bearerTokenEntryPoint;

    @Autowired private DhisApiTokenAuthenticationEntryPoint apiTokenAuthenticationEntryPoint;

    @Autowired private ApiTokenService apiTokenService;

    @Autowired private UserService userService;

    @Autowired private CacheProvider cacheProvider;

    @Autowired private SecurityService securityService;

    @Autowired private ExternalAccessVoter externalAccessVoter;

    @Autowired
    private TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource;

    @Autowired private DhisOidcLogoutSuccessHandler dhisOidcLogoutSuccessHandler;

    @Autowired
    private HttpBasicWebAuthenticationDetailsSource httpBasicWebAuthenticationDetailsSource;

    @Autowired private ConfigurationService configurationService;

    @Autowired private ApiTokenAuthManager apiTokenAuthManager;

    @Override
    public void configure(AuthenticationManagerBuilder auth) {
      auth.authenticationProvider(customLdapAuthenticationProvider);
      auth.authenticationProvider(twoFactorAuthenticationProvider);

      auth.authenticationEventPublisher(authenticationEventPublisher);
    }

    public WebExpressionVoter apiWebExpressionVoter() {
      WebExpressionVoter voter = new WebExpressionVoter();

      DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
      handler.setDefaultRolePrefix("");

      voter.setExpressionHandler(handler);

      return voter;
    }

    public LogicalOrAccessDecisionManager apiAccessDecisionManager() {
      List<AccessDecisionManager> decisionVoters =
          Arrays.asList(
              new UnanimousBased(List.of(new SimpleAccessVoter("ALL"))),
              new UnanimousBased(List.of(apiWebExpressionVoter())),
              new UnanimousBased(List.of(externalAccessVoter)),
              new UnanimousBased(List.of(new AuthenticatedVoter())));

      return new LogicalOrAccessDecisionManager(decisionVoters);
    }

    private void configureAccessRestrictions(
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry
            authorize) {
      authorize
          .antMatchers("/impersonate")
          .hasAnyAuthority("ALL", "F_IMPERSONATE_USER")

          // Temporary solution for Struts less login page, will be removed when apps are fully
          // migrated
          .antMatchers("/index.html")
          .permitAll()
          .antMatchers(apiContextPath + "/authentication/login")
          .permitAll()
          .antMatchers(apiContextPath + "/account/recovery")
          .permitAll()
          .antMatchers(apiContextPath + "/account/restore")
          .permitAll()
          .antMatchers(apiContextPath + "/account")
          .permitAll()
          .antMatchers(apiContextPath + "/staticContent/*")
          .permitAll()
          .antMatchers(apiContextPath + "/externalFileResources/*")
          .permitAll()
          .antMatchers(apiContextPath + "/icons/*/icon.svg")
          .permitAll()
          .anyRequest()
          .authenticated()
          .accessDecisionManager(apiAccessDecisionManager());
    }

    /** This method configures almost everything security related to /api endpoints */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf().disable();

      configureMatchers(http);
      configureCspFilter(http, dhisConfig, configurationService);
      configureCorsFilter(http);
      configureMobileAuthFilter(http);
      configureApiTokenAuthorizationFilter(http);
      configureOAuthTokenFilters(http);

      setHttpHeaders(http);
    }

    private void configureMatchers(HttpSecurity http) throws Exception {
      String[] activeProfiles = getApplicationContext().getEnvironment().getActiveProfiles();

      http.securityContext(
          httpSecuritySecurityContextConfigurer ->
              httpSecuritySecurityContextConfigurer.requireExplicitSave(true));

      http.antMatcher(apiContextPath + "/**")
          .authorizeRequests(this::configureAccessRestrictions)
          .httpBasic()
          .authenticationDetailsSource(httpBasicWebAuthenticationDetailsSource)
          .authenticationEntryPoint(formLoginBasicAuthenticationEntryPoint())
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

      http.sessionManagement()
          .requireExplicitAuthenticationStrategy(true)
          .sessionFixation()
          .migrateSession()
          .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
          .enableSessionUrlRewriting(false)
          .maximumSessions(
              Integer.parseInt(dhisConfig.getProperty(ConfigurationKey.MAX_SESSIONS_PER_USER)))
          .expiredUrl("/dhis-web-commons-security/logout.action");

      // Special handling if we are running in embedded Jetty mode
      if (Arrays.asList(activeProfiles).contains("embeddedJetty")) {
        http.formLogin()
            .authenticationDetailsSource(twoFactorWebAuthenticationDetailsSource)
            .loginPage("/index.html")
            .usernameParameter("j_username")
            .passwordParameter("j_password")
            .loginProcessingUrl("/api/authentication/login")
            .defaultSuccessUrl("/dhis-web-dashboard", true)
            .failureUrl("/index.html?error=true")
            .permitAll()
            .and()
            .logout()
            .logoutUrl("/dhis-web-commons-security/logout.action")
            .logoutSuccessUrl("/")
            .logoutSuccessHandler(dhisOidcLogoutSuccessHandler)
            .deleteCookies("JSESSIONID");
      }
    }

    private void configureCspFilter(
        HttpSecurity http,
        DhisConfigurationProvider dhisConfig,
        ConfigurationService configurationService) {
      http.addFilterBefore(
          new CspFilter(dhisConfig, configurationService), HeaderWriterFilter.class);
    }

    private void configureCorsFilter(HttpSecurity http) {
      http.addFilterBefore(CorsFilter.get(), BasicAuthenticationFilter.class);
    }

    private void configureMobileAuthFilter(HttpSecurity http) {
      http.addFilterBefore(
          CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class);
    }

    private void configureApiTokenAuthorizationFilter(HttpSecurity http) {
      if (dhisConfig.isEnabled(ConfigurationKey.ENABLE_API_TOKEN_AUTHENTICATION)) {
        Dhis2ApiTokenFilter tokenFilter =
            new Dhis2ApiTokenFilter(
                apiTokenAuthManager,
                apiTokenAuthenticationEntryPoint,
                authenticationEventPublisher);

        http.addFilterBefore(tokenFilter, BasicAuthenticationFilter.class);
      }
    }

    /**
     * Enable either deprecated OAuth2 authorization filter or the new JWT OIDC token filter. They
     * are mutually exclusive and can not both be added to the chain at the same time.
     *
     * @param http HttpSecurity config
     */
    private void configureOAuthTokenFilters(HttpSecurity http) {
      if (dhisConfig.isEnabled(ConfigurationKey.ENABLE_JWT_OIDC_TOKEN_AUTHENTICATION)) {
        http.addFilterAfter(
            getJwtBearerTokenAuthenticationFilter(), BasicAuthenticationFilter.class);
      }
    }

    /**
     * Creates and configures the JWT OIDC bearer token filter
     *
     * @return BearerTokenAuthenticationFilter to be added to the filter chain
     */
    private BearerTokenAuthenticationFilter getJwtBearerTokenAuthenticationFilter() {
      BearerTokenAuthenticationFilter jwtFilter =
          new BearerTokenAuthenticationFilter(dhis2JwtAuthenticationManagerResolver);

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

    /**
     * Entrypoint to "re-direct" http basic authentications to the login form page. Without this,
     * the default http basic pop-up window in the browser will be used.
     *
     * @return DHIS2BasicAuthenticationEntryPoint entryPoint to use in http config.
     */
    @Bean
    public FormLoginBasicAuthenticationEntryPoint formLoginBasicAuthenticationEntryPoint() {
      return new FormLoginBasicAuthenticationEntryPoint("/dhis-web-commons/security/login.action");
    }

    @Bean
    public FormLoginBasicAuthenticationEntryPoint
        strutsLessFormLoginBasicAuthenticationEntryPoint() {
      return new FormLoginBasicAuthenticationEntryPoint("/login.html");
    }
  }

  /**
   * Customizes various "global" security related headers.
   *
   * @param http http security config builder
   * @throws Exception
   */
  public static void setHttpHeaders(HttpSecurity http) throws Exception {
    http.headers()
        .defaultsDisabled()
        .contentTypeOptions()
        .and()
        .xssProtection()
        .and()
        .httpStrictTransportSecurity();
  }

  @Bean("switchUserProcessingFilter")
  public SwitchUserFilter switchUserFilter(
      @Qualifier("userDetailsService") UserDetailsService userDetailsService,
      @Qualifier("dhisConfigurationProvider") DhisConfigurationProvider config) {
    DhisSwitchUserFilter filter = new DhisSwitchUserFilter(config);
    filter.setUserDetailsService(userDetailsService);
    filter.setUserDetailsChecker(new ImpersonatingUserDetailsChecker());
    filter.setSwitchUserMatcher(
        new AntPathRequestMatcher("/impersonate", "POST", true, new UrlPathHelper()));
    filter.setExitUserMatcher(
        new AntPathRequestMatcher("/impersonateExit", "POST", true, new UrlPathHelper()));
    filter.setSwitchFailureUrl("/dhis-web-dashboard");
    filter.setTargetUrl("/dhis-web-dashboard");
    return filter;
  }
}
