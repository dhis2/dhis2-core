package org.hisp.dhis.webapi.security.config;

import org.hisp.dhis.security.oauth2.DefaultClientDetailsService;
import org.hisp.dhis.webapi.filter.CorsFilter;
import org.hisp.dhis.webapi.filter.CustomAuthenticationFilter;
import org.hisp.dhis.webapi.oprovider.DhisOauthAuthenticationProvider;
import org.hisp.dhis.webapi.security.DHIS2BasicAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
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
     * This config is a modification to the config that is enabled by using the @EnableAuthorizationServer annotation.
     * The spring-security-oauth2 project is deprecated but as of now 19. August 2020 there is still no other alternative ready.
     * The candidate for replacing this is: https://github.com/spring-projects-experimental/spring-authorization-server
     */
    @Configuration
    @Order( 1001 )
    @Import( { AuthorizationServerEndpointsConfiguration.class, AuthorizationServerEndpointsConfiguration.class } )
    public class OAuth2SecurityConfig extends WebSecurityConfigurerAdapter implements AuthorizationServerConfigurer
    {
        @Autowired
        private AuthorizationServerEndpointsConfiguration endpoints;

        @Autowired
        private DhisOauthAuthenticationProvider dhisOauthAuthenticationProvider;

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
            // The /authorize endpoint is only accessible AFTER you have logged in,
            // you will get redirected to the form login if you try accessing it.
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
        }

        private class AuthorizationServerAuthenticationManagerConfigurer
            extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>
        {
            @Override
            public void init( HttpSecurity builder )
                throws Exception
            {
                // This is a strange quirk to remove the default DaoAuthenticationConfigurer,
                // that gets automatically assigned in the config init.
                // We only want one auth. provider (our own...)
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
        }

        @Override
        public void configure( ClientDetailsServiceConfigurer configurer )
            throws Exception
        {
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
            endpoints
                .prefix( "/uaa" )
                .authorizationCodeServices( jdbcAuthorizationCodeServices() )
                .tokenStore( tokenStore() )
                .authenticationManager( authenticationManager() );
        }
    }

    @Bean
    public TokenStore tokenStore()
    {
        return new JdbcTokenStore( dataSource );
    }

    @Bean( "tokenService1" )
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
        @Qualifier( "tokenService1" )
        public ResourceServerTokenServices tokenServices;

        @Autowired
        @Qualifier( "defaultClientDetailsService" )
        DefaultClientDetailsService clientDetailsService;

        final private SecurityExpressionHandler<FilterInvocation> expressionHandler = new OAuth2WebSecurityExpressionHandler();

        final private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

        final private AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();

        final private String resourceId = "oauth2-resource";

        /**
         * This AuthenticationManager is responsible for authorizing access, refresh and code
         * OAuth2 tokens from the /token and /authorize endpoints.
         * It is used only by the OAuth2AuthenticationProcessingFilter.
         */
        private AuthenticationManager oauthAuthenticationManager( HttpSecurity http )
        {
            OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
            oauthAuthenticationManager.setResourceId( resourceId );
            oauthAuthenticationManager.setTokenServices( tokenServices );
            oauthAuthenticationManager.setClientDetailsService( clientDetailsService );

            return oauthAuthenticationManager;
        }

        protected void configure( HttpSecurity http )
            throws Exception
        {
            AuthenticationManager oauthAuthenticationManager = oauthAuthenticationManager( http );
            OAuth2AuthenticationProcessingFilter resourcesServerFilter = new OAuth2AuthenticationProcessingFilter();
            resourcesServerFilter.setAuthenticationEntryPoint( authenticationEntryPoint );
            resourcesServerFilter.setAuthenticationManager( oauthAuthenticationManager );

//            if (eventPublisher != null) {
//                resourcesServerFilter.setAuthenticationEventPublisher(eventPublisher);
//            }

//            resourcesServerFilter = postProcess(resourcesServerFilter);
            resourcesServerFilter.setStateless( false );

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

                .addFilterBefore( CorsFilter.get(), BasicAuthenticationFilter.class )
                .addFilterBefore( CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class )

                .addFilterAfter( resourcesServerFilter, BasicAuthenticationFilter.class )
                .exceptionHandling()
                .accessDeniedHandler( accessDeniedHandler )
                .authenticationEntryPoint( authenticationEntryPoint );
        }

        @Bean
        public DHIS2BasicAuthenticationEntryPoint basicAuthenticationEntryPoint()
        {
            return new DHIS2BasicAuthenticationEntryPoint();
        }
    }
}
