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
package org.hisp.dhis.security.config;

import static org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig.setHttpHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.MappedRedirectStrategy;
import org.hisp.dhis.security.authtentication.CustomAuthFailureHandler;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.oidc.DhisOidcLogoutSuccessHandler;
import org.hisp.dhis.security.oidc.DhisOidcProviderRepository;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.security.vote.ActionAccessVoter;
import org.hisp.dhis.security.vote.ModuleAccessVoter;
import org.hisp.dhis.webapi.filter.CorsFilter;
import org.hisp.dhis.webapi.filter.CspFilter;
import org.hisp.dhis.webapi.filter.CustomAuthenticationFilter;
import org.hisp.dhis.webapi.handler.DefaultAuthenticationSuccessHandler;
import org.hisp.dhis.webapi.security.ExternalAccessVoter;
import org.hisp.dhis.webapi.security.Http401LoginUrlAuthenticationEntryPoint;
import org.hisp.dhis.webapi.security.vote.LogicalOrAccessDecisionManager;
import org.hisp.dhis.webapi.security.vote.SimpleAccessVoter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.annotation.Order;
import org.springframework.mobile.device.DeviceResolver;
import org.springframework.mobile.device.LiteDeviceResolver;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.UnanimousBased;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.access.expression.WebExpressionVoter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * The {@code DhisWebCommonsWebSecurityConfig} class configures mostly all
 * authentication and authorization NOT on the /api endpoint.
 *
 * Almost all /api/* endpoints are configured in
 * {@code DhisWebApiWebSecurityConfig}
 *
 * Most of the configuration here is related to Struts security.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 2000 )
@ImportResource( locations = { "classpath*:/META-INF/dhis/beans.xml", "classpath*:/META-INF/dhis/beans-dataentry.xml",
    "classpath*:/META-INF/dhis/beans-maintenance-mobile.xml", "classpath*:/META-INF/dhis/beans-approval.xml" } )
public class DhisWebCommonsWebSecurityConfig
{
    /**
     * This configuration class is responsible for setting up the session
     * management.
     */
    @Configuration
    @Order( 3300 )
    public static class SessionWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter
    {
        @Autowired
        private DhisConfigurationProvider dhisConfig;

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
                .sessionFixation().migrateSession()
                .sessionCreationPolicy( SessionCreationPolicy.ALWAYS )
                .enableSessionUrlRewriting( false )
                .maximumSessions( Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.MAX_SESSIONS_PER_USER ) ) )
                .expiredUrl( "/dhis-web-commons-security/logout.action" )
                .sessionRegistry( sessionRegistry() );
        }
    }

    /**
     * This configuration class is responsible for setting up the form login and
     * everything related to the web pages.
     */
    @Configuration
    @Order( 2200 )
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter
    {
        @Autowired
        private TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource;

        @Autowired
        private I18nManager i18nManager;

        @Autowired
        private DhisConfigurationProvider dhisConfig;

        @Autowired
        private ExternalAccessVoter externalAccessVoter;

        @Autowired
        TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

        @Autowired
        private DhisOidcLogoutSuccessHandler dhisOidcLogoutSuccessHandler;

        @Autowired
        @Qualifier( "customLdapAuthenticationProvider" )
        private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

        @Autowired
        private CustomAuthFailureHandler customAuthFailureHandler;

        @Autowired
        private DefaultAuthenticationEventPublisher authenticationEventPublisher;

        @Autowired
        private DhisOidcProviderRepository dhisOidcProviderRepository;

        @Autowired
        private ConfigurationService configurationService;

        @Override
        public void configure( AuthenticationManagerBuilder auth )
            throws Exception
        {
            auth.authenticationProvider( customLdapAuthenticationProvider );
            auth.authenticationProvider( twoFactorAuthenticationProvider );
            auth.authenticationEventPublisher( authenticationEventPublisher );
        }

        @Override
        protected void configure( HttpSecurity http )
            throws Exception
        {
            http
                .authorizeRequests()
                .accessDecisionManager( accessDecisionManager() )
                .requestMatchers( analyticsPluginResources() ).permitAll()

                .antMatchers( "/api/staticContent/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/oidc/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/javascripts/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/css/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/flags/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/fonts/**" ).permitAll()
                .antMatchers( "/api/files/style/external" ).permitAll()
                .antMatchers( "/external-static/**" ).permitAll()
                .antMatchers( "/favicon.ico" ).permitAll()
                .antMatchers( "/api/publicKeys/**" ).permitAll()
                // Dynamic content
                .antMatchers( "/dhis-web-commons/i18nJavaScript.action" ).permitAll()
                .antMatchers( "/oauth2/**" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/enrolTwoFa.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/login.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/logout.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/expired.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/invite.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/restore.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/recovery.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/account.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/recovery.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/loginStrings.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/accountStrings.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/recoveryStrings.action" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/logo_front.png" ).permitAll()
                .antMatchers( "/dhis-web-commons/security/logo_mobile.png" ).permitAll()
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
                .antMatchers( "/dhis-web-sms-configuration/**" )
                .hasAnyAuthority( "ALL", "M_dhis-web-sms-configuration" )
                .antMatchers( "/dhis-web-user/**" ).hasAnyAuthority( "ALL", "M_dhis-web-user" )
                .antMatchers( "/dhis-web-aggregate-data-entry/**" )
                .hasAnyAuthority( "ALL", "M_dhis-web-aggregate-data-entry" )

                .antMatchers( "/**" ).authenticated()
                .and()

                .formLogin()
                .authenticationDetailsSource( twoFactorWebAuthenticationDetailsSource )
                .loginPage( "/dhis-web-commons/security/login.action" )
                .usernameParameter( "j_username" ).passwordParameter( "j_password" )
                .loginProcessingUrl( "/dhis-web-commons-security/login.action" )
                .failureHandler( customAuthFailureHandler )
                .successHandler( authenticationSuccessHandler() )
                .permitAll()
                .and()

                .logout()
                .logoutUrl( "/dhis-web-commons-security/logout.action" )
                .logoutSuccessUrl( "/" )
                .logoutSuccessHandler( dhisOidcLogoutSuccessHandler )
                .deleteCookies( "JSESSIONID" )
                .permitAll()
                .and()

                .exceptionHandling()
                .authenticationEntryPoint( entryPoint() )

                .and()

                .csrf()
                .disable()

                .addFilterBefore( new CspFilter( dhisConfig, configurationService ),
                    HeaderWriterFilter.class )

                .addFilterBefore( CorsFilter.get(), BasicAuthenticationFilter.class )
                .addFilterBefore( CustomAuthenticationFilter.get(), UsernamePasswordAuthenticationFilter.class );

            setHttpHeaders( http );
        }

        @Bean
        public Http401LoginUrlAuthenticationEntryPoint entryPoint()
        {
            // Converts to a HTTP basic login if "XMLHttpRequest".equals(
            // request.getHeader( "X-Requested-With" ) )
            return new Http401LoginUrlAuthenticationEntryPoint( "/dhis-web-commons/security/login.action" );
        }

        @Bean
        public DefaultAuthenticationSuccessHandler authenticationSuccessHandler()
        {
            DefaultAuthenticationSuccessHandler successHandler = new DefaultAuthenticationSuccessHandler();
            successHandler.setRedirectStrategy( mappedRedirectStrategy() );
            if ( dhisConfig.getProperty( ConfigurationKey.SYSTEM_SESSION_TIMEOUT ) != null )
            {
                successHandler.setSessionTimeout(
                    Integer.parseInt( dhisConfig.getProperty( ConfigurationKey.SYSTEM_SESSION_TIMEOUT ) ) );
            }

            return successHandler;
        }

        @Bean
        public MappedRedirectStrategy mappedRedirectStrategy()
        {
            MappedRedirectStrategy mappedRedirectStrategy = new MappedRedirectStrategy();
            mappedRedirectStrategy.setRedirectMap( Map.of( "/dhis-web-commons-stream/ping.action", "/" ) );
            mappedRedirectStrategy.setDeviceResolver( deviceResolver() );

            return mappedRedirectStrategy;
        }

        @Bean
        public DeviceResolver deviceResolver()
        {
            return new LiteDeviceResolver();
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
            ModuleAccessVoter voter = new ModuleAccessVoter();
            voter.setAttributePrefix( "M_" );
            voter.setAlwaysAccessible( Set.of(
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
                "dhis-web-uaa" ) );
            return voter;
        }

        @Bean
        public ActionAccessVoter actionAccessVoter()
        {
            ActionAccessVoter voter = new ActionAccessVoter();
            voter.setAttributePrefix( "F_" );
            voter.setRequiredAuthoritiesKey( "requiredAuthorities" );
            voter.setAnyAuthoritiesKey( "anyAuthorities" );
            return voter;
        }

        @Bean
        public WebExpressionVoter webExpressionVoter()
        {
            DefaultWebSecurityExpressionHandler h = new DefaultWebSecurityExpressionHandler();
            h.setDefaultRolePrefix( "" );
            WebExpressionVoter voter = new WebExpressionVoter();
            voter.setExpressionHandler( h );
            return voter;
        }

        @Bean( "accessDecisionManager" )
        public LogicalOrAccessDecisionManager accessDecisionManager()
        {
            List<AccessDecisionManager> decisionVoters = Arrays.asList(
                new UnanimousBased( List.of( new SimpleAccessVoter( "ALL" ) ) ),
                new UnanimousBased( List.of( actionAccessVoter(), moduleAccessVoter() ) ),
                new UnanimousBased( List.of( webExpressionVoter() ) ),
                new UnanimousBased( List.of( externalAccessVoter ) ),
                new UnanimousBased( List.of( new AuthenticatedVoter() ) ) );
            return new LogicalOrAccessDecisionManager( decisionVoters );
        }
    }
}
