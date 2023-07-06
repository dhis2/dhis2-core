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

import java.util.Map;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.AuthenticationLoggerListener;
import org.hisp.dhis.security.ldap.authentication.CustomLdapAuthenticationProvider;
import org.hisp.dhis.security.ldap.authentication.DhisBindAuthenticator;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.UserDetailsServiceLdapAuthoritiesPopulator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Order(910)
@ComponentScan(basePackages = {"org.hisp.dhis"})
@EnableWebSecurity
public class AuthenticationProviderConfig {
  @Autowired private DhisConfigurationProvider configurationProvider;

  @Autowired TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

  @Autowired
  @Qualifier("ldapUserDetailsService")
  UserDetailsService ldapUserDetailsService;

  @Bean
  public TwoFactorWebAuthenticationDetailsSource twoFactorWebAuthenticationDetailsSource() {
    return new TwoFactorWebAuthenticationDetailsSource();
  }

  @Bean(name = "customLdapAuthenticationProvider")
  CustomLdapAuthenticationProvider customLdapAuthenticationProvider() {
    return new CustomLdapAuthenticationProvider(
        dhisBindAuthenticator(),
        userDetailsServiceLdapAuthoritiesPopulator(ldapUserDetailsService),
        configurationProvider);
  }

  @Bean
  public DefaultSpringSecurityContextSource defaultSpringSecurityContextSource() {
    DefaultSpringSecurityContextSource defaultSpringSecurityContextSource =
        new DefaultSpringSecurityContextSource(
            configurationProvider.getProperty(ConfigurationKey.LDAP_URL));
    defaultSpringSecurityContextSource.setUserDn(
        configurationProvider.getProperty(ConfigurationKey.LDAP_MANAGER_DN));
    defaultSpringSecurityContextSource.setPassword(
        configurationProvider.getProperty(ConfigurationKey.LDAP_MANAGER_PASSWORD));

    return defaultSpringSecurityContextSource;
  }

  @Bean
  public FilterBasedLdapUserSearch filterBasedLdapUserSearch() {
    return new FilterBasedLdapUserSearch(
        configurationProvider.getProperty(ConfigurationKey.LDAP_SEARCH_BASE),
        configurationProvider.getProperty(ConfigurationKey.LDAP_SEARCH_FILTER),
        defaultSpringSecurityContextSource());
  }

  @Bean
  @DependsOn("org.hisp.dhis.user.UserService")
  public DhisBindAuthenticator dhisBindAuthenticator() {
    DhisBindAuthenticator dhisBindAuthenticator =
        new DhisBindAuthenticator(defaultSpringSecurityContextSource());
    dhisBindAuthenticator.setUserSearch(filterBasedLdapUserSearch());
    return dhisBindAuthenticator;
  }

  @Bean
  public UserDetailsServiceLdapAuthoritiesPopulator userDetailsServiceLdapAuthoritiesPopulator(
      UserDetailsService userDetailsService) {
    return new UserDetailsServiceLdapAuthoritiesPopulator(userDetailsService);
  }

  @Bean
  public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
    DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher =
        new DefaultAuthenticationEventPublisher();
    defaultAuthenticationEventPublisher.setAdditionalExceptionMappings(
        Map.of(
            OAuth2AuthenticationException.class, AuthenticationFailureBadCredentialsEvent.class));
    return defaultAuthenticationEventPublisher;
  }

  @Bean
  public AuthenticationLoggerListener authenticationLoggerListener() {
    return new AuthenticationLoggerListener();
  }

  @Bean
  public AuthenticationListener authenticationListener() {
    return new AuthenticationListener();
  }
}
