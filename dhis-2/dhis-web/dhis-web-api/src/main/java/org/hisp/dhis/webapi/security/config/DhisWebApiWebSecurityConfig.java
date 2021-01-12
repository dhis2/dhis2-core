package org.hisp.dhis.webapi.security.config;

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

import com.google.common.collect.ImmutableList;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsService;
import org.hisp.dhis.security.oidc.DhisClientRegistrationRepository;
import org.hisp.dhis.security.oidc.DhisOAuth2AuthorizationRequestResolver;
import org.hisp.dhis.security.oidc.OidcEnabledCondition;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.webapi.filter.CorsFilter;
import org.hisp.dhis.webapi.filter.CustomAuthenticationFilter;
import org.hisp.dhis.webapi.oprovider.DhisOauthAuthenticationProvider;
import org.hisp.dhis.webapi.security.DHIS2BasicAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationProcessingFilter;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.oauth2.provider.expression.OAuth2WebSecurityExpressionHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.sql.DataSource;
import java.util.Set;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 1999 )
public class DhisWebApiWebSecurityConfig
{
    @Autowired
    public DataSource dataSource;

    /**
     * This configuration class is responsible for setting up the OAuth2 /token endpoint and /authorize endpoint.
     * This config is a modification of the config that is automatically enabled by using the @EnableAuthorizationServer annotation.
     * The spring-security-oauth2 project is deprecated, but as of August 19, 2020; there is still no other viable alternative available.
     */
    @Configuration
    @Order( 1001 )
    @Import( { AuthorizationServerEndpointsConfiguration.class, AuthorizationServerEndpointsConfiguration.class } )
    public class OAuth2SecurityConfig extends WebSecurityConfigurerAdapter implements AuthorizationServerConfigurer
    {
        @Autowired
        private TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

        @Autowired
        @Qualifier( "customLdapAuthenticationProvider" )
        private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

        @Autowired
        private AuthorizationServerEndpointsConfiguration endpoints;

        @Autowired
        private DhisOauthAuthenticationProvider dhisOauthAuthenticationProvider;

        @Autowired
        private DefaultAuthenticationEventPublisher authenticationEventPublisher;

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            AuthorizationServerSecurityConfigurer configurer = new AuthorizationServerSecurityConfigurer();
            FrameworkEndpointHandlerMapping handlerMapping = endpoints.oauth2EndpointHandlerMapping();
            http.setSharedObject( FrameworkEndpointHandlerMapping.class, handlerMapping );

            configure( configurer );
            http.apply( configurer );

            // This is the only endpoint we need to configure.
            // The other /authorize endpoint is only accessible AFTER you have been authorized.
            String tokenEndpointPath = handlerMapping.getServletPath( "/oauth/token" );

            http
                .authorizeRequests()
                .antMatchers( tokenEndpointPath ).fullyAuthenticated()
                .and()
                .requestMatchers()
                .antMatchers( tokenEndpointPath )
                .and()
                .sessionManagement().sessionCreationPolicy( SessionCreationPolicy.NEVER );

            http.apply( new AuthorizationServerAuthenticationManagerConfigurer() );

            setHttpHeaders( http );
        }

        private class AuthorizationServerAuthenticationManagerConfigurer
            extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>
        {
            @Override
            @SuppressWarnings( "unchecked" )
            public void init( HttpSecurity builder )
                throws Exception
            {
                // This is a quirk to remove the default DaoAuthenticationConfigurer,
                // that gets automatically assigned in the AuthorizationServerSecurityConfigurer.
                // We only want ONE authentication provider (our own...)
                AuthenticationManagerBuilder authBuilder = builder
                    .getSharedObject( AuthenticationManagerBuilder.class );
                authBuilder.removeConfigurer( DaoAuthenticationConfigurer.class );
                authBuilder.authenticationProvider( dhisOauthAuthenticationProvider );
            }
        }

        @Override
        public void configure( AuthorizationServerSecurityConfigurer security )
            throws Exception
        {
            // Intentionally empty
        }

        @Override
        public void configure( ClientDetailsServiceConfigurer configurer )
            throws Exception
        {
            // Intentionally empty
        }

        @Bean( "authorizationCodeServices" )
        public JdbcAuthorizationCodeServices jdbcAuthorizationCodeServices()
        {
            return new JdbcAuthorizationCodeServices( dataSource );
        }

        @Override
        public void configure( final AuthorizationServerEndpointsConfigurer endpoints )
            throws Exception
        {
            ProviderManager providerManager = new ProviderManager(
                ImmutableList.of( twoFactorAuthenticationProvider, customLdapAuthenticationProvider ) );
            if ( authenticationEventPublisher != null )
            {
                providerManager.setAuthenticationEventPublisher( authenticationEventPublisher );
            }
            endpoints
                .prefix( "/uaa" )
                .authorizationCodeServices( jdbcAuthorizationCodeServices() )
                .tokenStore( tokenStore() )
                .authenticationManager( providerManager );
        }
    }

    /**
     * This class is configuring the OIDC endpoints
     */
    @Configuration
    @Order( 1010 )
    @Conditional( value = OidcEnabledCondition.class )
    public static class OidcSecurityConfig extends WebSecurityConfigurerAdapter
    {
        @Autowired
        private DhisClientRegistrationRepository dhisClientRegistrationRepository;

        @Autowired
        private DhisOAuth2AuthorizationRequestResolver dhisOAuth2AuthorizationRequestResolver;

        @Autowired
        private DefaultAuthenticationEventPublisher authenticationEventPublisher;

        public void configure( AuthenticationManagerBuilder auth )
            throws Exception
        {
            auth.authenticationEventPublisher( authenticationEventPublisher );
        }

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            Set<String> providerIds = dhisClientRegistrationRepository.getAllRegistrationId();

            http
                .antMatcher( "/oauth2/**" )
                .authorizeRequests( authorize -> {
                        for ( String providerId : providerIds )
                        {
                            authorize
                                .antMatchers( "/oauth2/authorization/" + providerId ).permitAll()
                                .antMatchers( "/oauth2/code/" + providerId ).permitAll();
                        }
                        authorize.anyRequest().authenticated();
                    }
                )

                .oauth2Login( oauth2 -> oauth2
                    .failureUrl( "/dhis-web-dashboard" )
                    .clientRegistrationRepository( dhisClientRegistrationRepository )
                    .loginProcessingUrl( "/oauth2/code/*" )
                    .authorizationEndpoint()
                    .authorizationRequestResolver( dhisOAuth2AuthorizationRequestResolver )
                )

                .csrf().disable();

            setHttpHeaders( http );
        }
    }

    @Bean
    public TokenStore tokenStore()
    {
        return new JdbcTokenStore( dataSource );
    }

    @Bean( "defaultTokenService" )
    @Primary
    public DefaultTokenServices tokenServices()
    {
        final DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore( tokenStore() );
        defaultTokenServices.setSupportRefreshToken( true );
        return defaultTokenServices;
    }

    /**
     * This configuration class is responsible for setting up the /api endpoints
     */
    @Configuration
    @Order( 1100 )
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter
    {
        @Autowired
        @Qualifier( "defaultTokenService" )
        private ResourceServerTokenServices tokenServices;

        @Autowired
        @Qualifier( "defaultClientDetailsService" )
        private DefaultClientDetailsService clientDetailsService;

        @Autowired
        private TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

        @Autowired
        @Qualifier( "customLdapAuthenticationProvider" )
        private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

        @Autowired
        private DefaultAuthenticationEventPublisher authenticationEventPublisher;

        final private SecurityExpressionHandler<FilterInvocation> expressionHandler = new OAuth2WebSecurityExpressionHandler();

        final private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

        final private AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();

        public void configure( AuthenticationManagerBuilder auth )
            throws Exception
        {
            auth.authenticationProvider( twoFactorAuthenticationProvider );
            auth.authenticationProvider( customLdapAuthenticationProvider );
            auth.authenticationEventPublisher( authenticationEventPublisher );
        }

        /**
         * This AuthenticationManager is responsible for authorizing access, refresh and code
         * OAuth2 tokens from the /token and /authorize endpoints.
         * It is used only by the OAuth2AuthenticationProcessingFilter.
         */
        private AuthenticationManager oauthAuthenticationManager( HttpSecurity http )
        {
            OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
            oauthAuthenticationManager.setResourceId( "oauth2-resource" );
            oauthAuthenticationManager.setTokenServices( tokenServices );
            oauthAuthenticationManager.setClientDetailsService( clientDetailsService );

            return oauthAuthenticationManager;
        }

        protected void configure( HttpSecurity http )
            throws Exception
        {
            http
                .antMatcher( "/api/**" )
                .authorizeRequests( authorize -> authorize

                    .expressionHandler( expressionHandler )

                    .antMatchers( "/api/account/username" ).permitAll()
                    .antMatchers( "/api/account/recovery" ).permitAll()
                    .antMatchers( "/api/account/restore" ).permitAll()
                    .antMatchers( "/api/account/password" ).permitAll()
                    .antMatchers( "/api/account/validatePassword" ).permitAll()
                    .antMatchers( "/api/account/validateUsername" ).permitAll()
                    .antMatchers( "/api/account" ).permitAll()
                    .antMatchers( "/api/staticContent/*" ).permitAll()
                    .antMatchers( "/api/externalFileResources/*" ).permitAll()
                    .antMatchers( "/api/icons/*/icon.svg" ).permitAll()
                    .anyRequest().authenticated()
                )
                .httpBasic()
                .authenticationEntryPoint( basicAuthenticationEntryPoint() )
                .and().csrf().disable()

                .exceptionHandling()
                .accessDeniedHandler( accessDeniedHandler );

            http
                .addFilterBefore( CorsFilter.get(), BasicAuthenticationFilter.class )
                .addFilterBefore( CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class );

            // This is the OAuth2 resource filter it will be checking for the "Authorization: Bearer <token>" header if
            // the "Authorization: Basic <username:password>" header is not present or valid.
            OAuth2AuthenticationProcessingFilter resourcesServerFilter = new OAuth2AuthenticationProcessingFilter();
            resourcesServerFilter.setAuthenticationEntryPoint( authenticationEntryPoint );
            resourcesServerFilter.setAuthenticationManager( oauthAuthenticationManager( http ) );
            resourcesServerFilter.setStateless( false );

            // Adds the resource (oath2 token) filter after http basic filer.
            http.addFilterAfter( resourcesServerFilter, BasicAuthenticationFilter.class );

            setHttpHeaders( http );
        }

        @Bean
        public DHIS2BasicAuthenticationEntryPoint basicAuthenticationEntryPoint()
        {
            return new DHIS2BasicAuthenticationEntryPoint("/dhis-web-commons/security/login.action");
        }
    }

    /**
     * Customizes various "global" security related headers.
     *
     * @param http http config
     * @throws Exception
     */
    public static void setHttpHeaders( HttpSecurity http )
        throws Exception
    {
        http
            .headers()
            .defaultsDisabled()
            .contentTypeOptions()
            .and()
            .xssProtection()
            .and()
            .httpStrictTransportSecurity()
            .and()
            .frameOptions().sameOrigin();
    }
}
