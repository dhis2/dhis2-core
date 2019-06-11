package org.hisp.dhis.config;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.ldap.authentication.DhisBindAuthenticator;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsService;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsUserDetailsService;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import javax.sql.DataSource;

/**
 * @author Luciano Fiandesio
 */
@EnableGlobalAuthentication
@Configuration( "coreSecurityConfig" )
public class SecurityConfig
{
    @Autowired
    private DhisConfigurationProvider configurationProvider;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultSpringSecurityContextSource defaultSpringSecurityContextSource()
    {
        DefaultSpringSecurityContextSource defaultSpringSecurityContextSource = new DefaultSpringSecurityContextSource(
            configurationProvider.getProperty( ConfigurationKey.LDAP_URL ) );
        defaultSpringSecurityContextSource
            .setUserDn( configurationProvider.getProperty( ConfigurationKey.LDAP_MANAGER_DN ) );
        defaultSpringSecurityContextSource
            .setPassword( configurationProvider.getProperty( ConfigurationKey.LDAP_MANAGER_PASSWORD ) );

        return defaultSpringSecurityContextSource;
    }

    @Bean
    public FilterBasedLdapUserSearch filterBasedLdapUserSearch()
    {
        return new FilterBasedLdapUserSearch( configurationProvider.getProperty( ConfigurationKey.LDAP_SEARCH_BASE ),
            configurationProvider.getProperty( ConfigurationKey.LDAP_SEARCH_FILTER ),
            defaultSpringSecurityContextSource() );
    }

    @Bean( "authorizationCodeServices" )
    public JdbcAuthorizationCodeServices jdbcAuthorizationCodeServices( DataSource dataSource )
    {
        return new JdbcAuthorizationCodeServices( dataSource );
    }

    @Bean
    public TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource()
    {
        return new TwoFactorWebAuthenticationDetailsSource();
    }

    @Bean( "tokenServices" )
    public DefaultTokenServices defaultTokenServices( DataSource dataSource )
    {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore( new JdbcTokenStore( dataSource ) );
        defaultTokenServices.setSupportRefreshToken( true );
        return defaultTokenServices;
    }

    @Bean
    public OAuth2AuthenticationManager oAuth2AuthenticationManager( DataSource dataSource,
        DefaultClientDetailsService defaultClientDetailsService )
    {

        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore( new JdbcTokenStore( dataSource ) );
        defaultTokenServices.setSupportRefreshToken( true );

        OAuth2AuthenticationManager oa2Manager = new OAuth2AuthenticationManager();
        oa2Manager.setTokenServices( defaultTokenServices );
        oa2Manager.setClientDetailsService( defaultClientDetailsService );
        return oa2Manager;
    }

    @Bean
    public TwoFactorAuthenticationProvider twoFactorAuthenticationProvider( UserService userService,
        UserDetailsService userDetailsService, SecurityService securityService )
    {
        TwoFactorAuthenticationProvider twoFactorAuthenticationProvider = new TwoFactorAuthenticationProvider();
        twoFactorAuthenticationProvider.setPasswordEncoder( bCryptPasswordEncoder() );
        twoFactorAuthenticationProvider.setUserService( userService );
        twoFactorAuthenticationProvider.setUserDetailsService( userDetailsService );
        twoFactorAuthenticationProvider.setSecurityService( securityService );

        return twoFactorAuthenticationProvider;
    }

    @Bean
    @DependsOn( "org.hisp.dhis.user.UserService" )
    public DhisBindAuthenticator dhisBindAuthenticator()
    {
        DhisBindAuthenticator dhisBindAuthenticator = new DhisBindAuthenticator( defaultSpringSecurityContextSource() );
        dhisBindAuthenticator.setUserSearch( filterBasedLdapUserSearch() );
        return dhisBindAuthenticator;
    }

    @Bean
    public UserDetailsServiceLdapAuthoritiesPopulator userDetailsServiceLdapAuthoritiesPopulator(
        UserDetailsService userDetailsService )
    {
        return new UserDetailsServiceLdapAuthoritiesPopulator( userDetailsService );
    }

    @Autowired
    private CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

    @Autowired
    private DefaultClientDetailsUserDetailsService defaultClientDetailsUserDetailsService;

    @Autowired
    public void configureGlobal( AuthenticationManagerBuilder auth, UserService userService,
        UserDetailsService userDetailsService, SecurityService securityService )
        throws Exception
    {
        TwoFactorAuthenticationProvider twoFactorAuthenticationProvider = new TwoFactorAuthenticationProvider();
        twoFactorAuthenticationProvider.setPasswordEncoder( bCryptPasswordEncoder() );
        twoFactorAuthenticationProvider.setUserService( userService );
        twoFactorAuthenticationProvider.setUserDetailsService( userDetailsService );
        twoFactorAuthenticationProvider.setSecurityService( securityService );

        auth.authenticationProvider( twoFactorAuthenticationProvider )
            .authenticationProvider( customLdapAuthenticationProvider )
            .userDetailsService( defaultClientDetailsUserDetailsService );
    }

    @Bean( "authenticationManager" )
    public AuthenticationManager authenticationManager( AuthenticationManagerBuilder auth )
    {
        return auth.getOrBuild();
    }
}
