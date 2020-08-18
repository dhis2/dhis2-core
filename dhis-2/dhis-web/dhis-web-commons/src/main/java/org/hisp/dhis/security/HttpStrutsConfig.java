package org.hisp.dhis.security;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.security.filter.CorsFilter;
import org.hisp.dhis.security.filter.CustomAuthenticationFilter;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsService;
import org.hisp.dhis.security.oprovider.OldOauthAuthenticationProvider;
import org.hisp.dhis.security.vote.ActionAccessVoter;
import org.hisp.dhis.security.vote.ExternalAccessVoter;
import org.hisp.dhis.security.vote.LogicalOrAccessDecisionManager;
import org.hisp.dhis.security.vote.ModuleAccessVoter;
import org.hisp.dhis.security.vote.SimpleAccessVoter;
import org.hisp.dhis.webapi.handler.CustomExceptionMappingAuthenticationFailureHandler;
import org.hisp.dhis.webapi.handler.DefaultAuthenticationSuccessHandler;
import org.hisp.dhis.webapi.security.DHIS2BasicAuthenticationEntryPoint;
import org.hisp.dhis.webapi.security.Http401LoginUrlAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.mobile.device.DeviceResolver;
import org.springframework.mobile.device.LiteDeviceResolver;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
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
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 2000 )
@ImportResource( locations = { "classpath*:/META-INF/dhis/beans.xml", "classpath*:/META-INF/dhis/beans-dataentry.xml",
    "classpath*:/META-INF/dhis/beans-maintenance-mobile.xml", "classpath*:/META-INF/dhis/beans-approval.xml" } )
@Slf4j
public class HttpStrutsConfig
{
    @Autowired
    public DataSource dataSource;

    private static List<String> clients = Arrays.asList( "google", "facebook" );

    private static String CLIENT_PROPERTY_KEY
        = "spring.security.oauth2.client.registration.";

//    @Autowired
//    private Environment env;

    private ClientRegistration getRegistration( String client )
    {
        String clientId = "cleintID";

//            env.getProperty(
//            CLIENT_PROPERTY_KEY + client + ".client-id");

        if ( clientId == null )
        {
            return null;
        }

        String clientSecret = "cleintSecret";

//            env.getProperty(
//            CLIENT_PROPERTY_KEY + client + ".client-secret");

        if ( client.equals( "google" ) )
        {
            return CommonOAuth2Provider.GOOGLE.getBuilder( client )
                .clientId( clientId ).clientSecret( clientSecret ).build();
        }
        if ( client.equals( "facebook" ) )
        {
            return CommonOAuth2Provider.FACEBOOK.getBuilder( client )
                .clientId( clientId ).clientSecret( clientSecret ).build();
        }
        return null;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository()
    {
        List<ClientRegistration> registrations = clients.stream()
            .map( c -> getRegistration( c ) )
            .filter( registration -> registration != null )
            .collect( Collectors.toList() );

        return new InMemoryClientRegistrationRepository( registrations );
    }

    @Configuration
    @Order( 1001 )
    @Import( { AuthorizationServerEndpointsConfiguration.class, AuthorizationServerEndpointsConfiguration.class } )
    public class Outh1SecurityConfig extends WebSecurityConfigurerAdapter implements AuthorizationServerConfigurer
    {

        @Autowired
        private AuthorizationServerEndpointsConfiguration endpoints;

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            AuthorizationServerSecurityConfigurer configurer = new AuthorizationServerSecurityConfigurer();
            FrameworkEndpointHandlerMapping handlerMapping = endpoints.oauth2EndpointHandlerMapping();
            http.setSharedObject( FrameworkEndpointHandlerMapping.class, handlerMapping );

            configure( configurer );
            http.apply( configurer );

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
                AuthenticationManagerBuilder authBuilder = builder
                    .getSharedObject( AuthenticationManagerBuilder.class );
                authBuilder.removeConfigurer( DaoAuthenticationConfigurer.class );
                authBuilder.authenticationProvider( oldOauthAuthenticationProvider );
            }
        }

        @Autowired
        OldOauthAuthenticationProvider oldOauthAuthenticationProvider;

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

    @Configuration
    @Order( 1010 )
    public class OidcSecurityConfig extends WebSecurityConfigurerAdapter
    {
        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            http
                .antMatcher( "/oauth2/**" )
                .authorizeRequests( authorize -> authorize
                    .antMatchers( "/oauth2/authorization/google" ).permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2Login()
                .and().csrf().disable()
                .addFilterBefore( CorsFilter.get(), BasicAuthenticationFilter.class )
                .addFilterBefore( CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class );
        }
    }

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

        OAuth2AuthenticationProcessingFilter resourcesServerFilter;

        private AuthenticationManager authenticationManager;

        private SecurityExpressionHandler<FilterInvocation> expressionHandler = new OAuth2WebSecurityExpressionHandler();

        private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

        private AccessDeniedHandler accessDeniedHandler = new OAuth2AccessDeniedHandler();

        private String resourceId = "oauth2-resource";

        private AuthenticationManager oauthAuthenticationManager( HttpSecurity http )
        {
            OAuth2AuthenticationManager oauthAuthenticationManager = new OAuth2AuthenticationManager();
            if ( authenticationManager != null )
            {
                if ( authenticationManager instanceof OAuth2AuthenticationManager )
                {
                    oauthAuthenticationManager = (OAuth2AuthenticationManager) authenticationManager;
                }
                else
                {
                    return authenticationManager;
                }
            }
            oauthAuthenticationManager.setResourceId( resourceId );
            oauthAuthenticationManager.setTokenServices( tokenServices );
            oauthAuthenticationManager.setClientDetailsService( clientDetailsService );

            return oauthAuthenticationManager;
        }

        protected void configure( HttpSecurity http )
            throws Exception
        {

            AuthenticationManager oauthAuthenticationManager = oauthAuthenticationManager( http );
            resourcesServerFilter = new OAuth2AuthenticationProcessingFilter();
            resourcesServerFilter.setAuthenticationEntryPoint( authenticationEntryPoint );
            resourcesServerFilter.setAuthenticationManager( oauthAuthenticationManager );

//            if (eventPublisher != null) {
//                resourcesServerFilter.setAuthenticationEventPublisher(eventPublisher);
//            }

//            if (tokenExtractor != null) {
//                resourcesServerFilter.setTokenExtractor(tokenExtractor);
//            }

//            if (authenticationDetailsSource != null) {
//                resourcesServerFilter.setAuthenticationDetailsSource(authenticationDetailsSource);
//            }

//            resourcesServerFilter = postProcess(resourcesServerFilter);
            resourcesServerFilter.setStateless( false );

//            // @formatter:off
//            http
//                .authorizeRequests().expressionHandler(expressionHandler)
//                .and()
//                .addFilterBefore(resourcesServerFilter, AbstractPreAuthenticatedProcessingFilter.class)
//                .exceptionHandling()
//                .accessDeniedHandler(accessDeniedHandler)
//                .authenticationEntryPoint(authenticationEntryPoint);


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

//                .addFilterBefore(resourcesServerFilter, AbstractPreAuthenticatedProcessingFilter.class)
                .addFilterAfter(resourcesServerFilter, BasicAuthenticationFilter.class)
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint);
        }

        @Bean
        public DHIS2BasicAuthenticationEntryPoint basicAuthenticationEntryPoint()
        {
            return new DHIS2BasicAuthenticationEntryPoint();
        }
    }

    @Configuration
    @Order( 3300 )
    public static class SessionWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter
    {
        @Bean
        public static SessionRegistryImpl sessionRegistry()
        {
            return new org.springframework.security.core.session.SessionRegistryImpl();
        }

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            http
                .sessionManagement()
                .sessionCreationPolicy( SessionCreationPolicy.ALWAYS )
                .enableSessionUrlRewriting( false )
                .maximumSessions( 10 )
                .expiredUrl( "/dhis-web-commons-security/logout.action" )
                .sessionRegistry( sessionRegistry() );
        }

    }

    @Configuration
    @Order( 2200 )
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter
    {

//        @Bean
//        public HttpFirewall allowUrlEncodedSlashHttpFirewall()
//        {
//            StrictHttpFirewall firewall = new StrictHttpFirewall();
//            firewall.setAllowUrlEncodedSlash( true );
//            firewall.setAllowSemicolon( true );
//            return firewall;
//        }

        @Override
        public void configure( WebSecurity web )
            throws Exception
        {
            super.configure( web );
            web
//                .httpFirewall( allowUrlEncodedSlashHttpFirewall() )
                .ignoring()
                .antMatchers( "/dhis-web-commons/javascripts/**" )
                .antMatchers( "/dhis-web-commons/css/**" )
                .antMatchers( "/dhis-web-commons/flags/**" )
                .antMatchers( "/dhis-web-commons/fonts/**" )
                .antMatchers( "/dhis-web-commons/i18nJavaScript.action" )
                .antMatchers( "/dhis-web-commons/security/**" )

                .antMatchers( "/api/files/style/external" )
                .antMatchers( "/external-static/**" )
                .antMatchers( "/favicon.ico" );
        }

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            http
                .authorizeRequests()

                .accessDecisionManager( accessDecisionManager() )

                .requestMatchers( analyticsPluginResources() ).permitAll()

                .antMatchers( "/oauth2/**" ).permitAll()
                .antMatchers( "/dhis-web-dashboard/**" ).hasAnyAuthority( "ALL", "M_dhis-web-dashboard" )
                .antMatchers( "/dhis-web-pivot/**" ).hasAnyAuthority( "ALL", "M_dhis-web-pivot" )
                .antMatchers( "/dhis-web-visualizer/**" ).hasAnyAuthority( "ALL", "M_dhis-web-visualizer" )
                .antMatchers( "/dhis-web-data-visualizer/**" ).hasAnyAuthority( "ALL", "M_dhis-web-data-visualizer" )
                .antMatchers( "/dhis-web-mapping/**" ).hasAnyAuthority( "ALL", "M_dhis-web-mapping" )
                .antMatchers( "/dhis-web-maps/**" ).hasAnyAuthority( "ALL", "M_dhis-web-maps" )
                .antMatchers( "/dhis-web-event-reports/**" ).hasAnyAuthority( "ALL", "M_dhis-web-event-reports" )
                .antMatchers( "/dhis-web-event-visualizer/**" ).hasAnyAuthority( "ALL", "M_dhis-web-event-visualizer" )
                .antMatchers( "/dhis-web-interpretation/**" ).hasAnyAuthority( "ALL", "M_dhis-web-interpretation" )
                .antMatchers( "/dhis-web-settings/**" ).hasAnyAuthority( "ALL", "M_dhis-web-settings" )
                .antMatchers( "/dhis-web-maintenance/**" ).hasAnyAuthority( "ALL", "M_dhis-web-maintenance" )
                .antMatchers( "/dhis-web-app-management/**" ).hasAnyAuthority( "ALL", "M_dhis-web-app-management" )
                .antMatchers( "/dhis-web-usage-analytics/**" ).hasAnyAuthority( "ALL", "M_dhis-web-usage-analytics" )
                .antMatchers( "/dhis-web-event-capture/**" ).hasAnyAuthority( "ALL", "M_dhis-web-event-capture" )
                .antMatchers( "/dhis-web-tracker-capture/**" ).hasAnyAuthority( "ALL", "M_dhis-web-tracker-capture" )
                .antMatchers( "/dhis-web-cache-cleaner/**" ).hasAnyAuthority( "ALL", "M_dhis-web-cache-cleaner" )
                .antMatchers( "/dhis-web-data-administration/**" )
                .hasAnyAuthority( "ALL", "M_dhis-web-data-administration" )
                .antMatchers( "/dhis-web-data-quality/**" ).hasAnyAuthority( "ALL", "M_dhis-web-data-quality" )
                .antMatchers( "/dhis-web-messaging/**" ).hasAnyAuthority( "ALL", "M_dhis-web-messaging" )
                .antMatchers( "/dhis-web-datastore/**" ).hasAnyAuthority( "ALL", "M_dhis-web-datastore" )
                .antMatchers( "/dhis-web-scheduler/**" ).hasAnyAuthority( "ALL", "M_dhis-web-scheduler" )
                .antMatchers( "/dhis-web-user/**" ).hasAnyAuthority( "ALL", "M_dhis-web-user" )

                .antMatchers( "/**" ).authenticated()
                .and()

                .formLogin()

                .failureHandler( authenticationFailureHandler() )
                .successHandler( authenticationSuccessHandler() )

                .loginPage( "/dhis-web-commons/security/login.action" ).permitAll()
                .usernameParameter( "j_username" ).passwordParameter( "j_password" )
                .loginProcessingUrl( "/dhis-web-commons-security/login.action" )
//                .defaultSuccessUrl( "/dhis-web-dashboard" )
                .failureUrl( "/dhis-web-commons/security/login.action" )
                .and()

                .logout()
                .logoutUrl( "/dhis-web-commons-security/logout.action" )
                .logoutSuccessUrl( "/" )
                .deleteCookies( "JSESSIONID" )
                .and()

                .exceptionHandling()
                .and()

                .csrf()
                .disable()

                .addFilterBefore( CorsFilter.get(), BasicAuthenticationFilter.class )
                .addFilterBefore( CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class );
        }

        @Bean
        public Http401LoginUrlAuthenticationEntryPoint entryPoint()
        {
            // Converts to a HTTP basic login if  "XMLHttpRequest".equals( request.getHeader( "X-Requested-With" ) )
            return new Http401LoginUrlAuthenticationEntryPoint( "/dhis-web-commons/security/login.action" );
        }

        @Bean
        public DeviceResolver deviceResolver()
        {
            return new LiteDeviceResolver();
        }

        @Bean
        public MappedRedirectStrategy mappedRedirectStrategy()
        {
            MappedRedirectStrategy mappedRedirectStrategy = new MappedRedirectStrategy();
            mappedRedirectStrategy.setRedirectMap( ImmutableMap.of(
                "/dhis-web-commons-stream/ping.action", "/"
                )
            );
            mappedRedirectStrategy.setDeviceResolver( deviceResolver() );

            return mappedRedirectStrategy;
        }

        @Bean
        public DefaultAuthenticationSuccessHandler authenticationSuccessHandler()
        {
            DefaultAuthenticationSuccessHandler successHandler = new DefaultAuthenticationSuccessHandler();
            successHandler.setRedirectStrategy( mappedRedirectStrategy() );
//            successHandler.setSessionTimeout( 10000 ); //TODO: FIX get real
            return successHandler;
        }

        @Bean
        public CustomExceptionMappingAuthenticationFailureHandler authenticationFailureHandler()
        {
            CustomExceptionMappingAuthenticationFailureHandler handler =
                new CustomExceptionMappingAuthenticationFailureHandler();

            // Handles the special case when a user failed to login because it has expired...
            handler.setExceptionMappings(
                ImmutableMap.of(
                    "org.springframework.security.authentication.CredentialsExpiredException",
                    "/dhis-web-commons/security/expired.action" ) );

            handler.setDefaultFailureUrl( "/dhis-web-commons/security/login.action?failed=true" );

            return handler;
        }

        @Bean
        public RequestMatcher analyticsPluginResources()
        {
            String pattern = ".*(dhis-web-mapping\\/map.js|dhis-web-visualizer\\/chart.js|dhis-web-maps\\" +
                "/map.js|dhis-web-event-reports\\/eventreport.js|dhis-web-event-visualizer\\/eventchart.js|dhis-web-pivot\\/reporttable.js)";

            return new org.springframework.security.web.util.matcher.RegexRequestMatcher( pattern, "GET" );
        }

        @Bean
        public ModuleAccessVoter moduleAccessVoter()
        {
            ModuleAccessVoter v = new ModuleAccessVoter();
            v.setAttributePrefix( "M_" );
            v.setAlwaysAccessible( ImmutableSet.of(
                "dhis-web-commons-menu",
                "dhis-web-commons-oust",
                "dhis-web-commons-ouwt",
                "dhis-web-commons-security",
                "dhis-web-commons-i18n",
                "dhis-web-commons-ajax",
                "dhis-web-commons-ajax-json",
                "dhis-web-commons-ajax-html",
                "dhis-web-commons-stream",
                "dhis-web-commons-help",
                "dhis-web-commons-about",
                "dhis-web-menu-management",
                "dhis-web-apps",
                "dhis-web-api-mobile",
                "dhis-web-portal",
                "dhis-web-uaa"
            ) );
            return v;
        }

        @Bean
        public ActionAccessVoter actionAccessVoter()
        {
            ActionAccessVoter v = new ActionAccessVoter();
            v.setAttributePrefix( "F_" );
            v.setRequiredAuthoritiesKey( "requiredAuthorities" );
            v.setAnyAuthoritiesKey( "anyAuthorities" );
            return v;
        }

        @Bean
        public WebExpressionVoter webExpressionVoter()
        {
            DefaultWebSecurityExpressionHandler h = new DefaultWebSecurityExpressionHandler();
            h.setDefaultRolePrefix( "" );
            WebExpressionVoter v = new WebExpressionVoter();
            v.setExpressionHandler( h );
            return v;
        }

        @Bean
        public LogicalOrAccessDecisionManager accessDecisionManager()
        {
            List<AccessDecisionManager> decisionVoters = Arrays.asList(
                new UnanimousBased( ImmutableList.of( new SimpleAccessVoter( "ALL" ) ) ),
                new UnanimousBased( ImmutableList.of( actionAccessVoter(), moduleAccessVoter() ) ),
                new UnanimousBased( ImmutableList.of( webExpressionVoter() ) ),
                new UnanimousBased( ImmutableList.of( ExternalAccessVoter.get() ) ),
                new UnanimousBased( ImmutableList.of( new AuthenticatedVoter() ) )
            );
            return new LogicalOrAccessDecisionManager( decisionVoters );
        }
    }
}