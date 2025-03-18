/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.security.ldap.authentication;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class CustomLdapAuthenticationProvider extends LdapAuthenticationProvider {

  private final DhisConfigurationProvider configurationProvider;
  private final UserDetailsService userDetailsService;

  public CustomLdapAuthenticationProvider(
      LdapAuthenticator authenticator,
      UserDetailsService userDetailsService,
      LdapAuthoritiesPopulator authoritiesPopular,
      DhisConfigurationProvider configurationProvider) {

    super(authenticator, authoritiesPopular);

    checkNotNull(configurationProvider);

    this.userDetailsService = userDetailsService;
    this.configurationProvider = configurationProvider;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    Authentication authenticate = super.authenticate(authentication);

    LdapUserDetailsImpl user = (LdapUserDetailsImpl) authenticate.getPrincipal();
    UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

    if (userDetails == null) {
      String msg = format("Could not find DHIS 2 user with username: '%s'", user.getUsername());
      throw new UsernameNotFoundException(msg);
    }

    UsernamePasswordAuthenticationToken result =
        UsernamePasswordAuthenticationToken.authenticated(
            userDetails, user.getPassword(), userDetails.getAuthorities());
    result.setDetails(authenticate.getDetails());

    return result;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    if (!configurationProvider.isLdapConfigured()) {
      return false;
    }

    return super.supports(authentication);
  }
}
