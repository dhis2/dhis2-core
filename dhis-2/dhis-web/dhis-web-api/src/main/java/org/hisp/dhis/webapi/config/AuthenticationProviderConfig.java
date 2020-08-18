package org.hisp.dhis.webapi.config;

import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.ldap.authentication.DhisBindAuthenticator;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsUserDetailsService;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order( 910 )
@ComponentScan( basePackages = { "org.hisp.dhis" } )
@EnableWebSecurity
public class AuthenticationProviderConfig
{
    @Autowired
    TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

    @Autowired
    CustomLdapAuthenticationProvider customLdapAuthenticationProvider;

    @Autowired
    public void configureGlobal( AuthenticationManagerBuilder auth )
        throws Exception
    {
        auth.authenticationProvider( twoFactorAuthenticationProvider );
        auth.authenticationProvider( customLdapAuthenticationProvider );
    }

    @Autowired
    DefaultClientDetailsUserDetailsService defaultClientDetailsUserDetailsService;

    @Bean
    @DependsOn( "org.hisp.dhis.user.UserService" )
    public DhisBindAuthenticator dhisBindAuthenticator()
    {
        return new DhisBindAuthenticator();
//        UserDetailsServiceLdapAuthoritiesPopulator userDetailsServiceLdapAuthoritiesPopulator = new UserDetailsServiceLdapAuthoritiesPopulator(
//            defaultClientDetailsUserDetailsService );
    }
}
