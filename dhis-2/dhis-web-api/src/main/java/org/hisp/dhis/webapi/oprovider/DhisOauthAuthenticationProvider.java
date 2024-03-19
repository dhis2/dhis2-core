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
package org.hisp.dhis.webapi.oprovider;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.hisp.dhis.security.oauth2.DefaultClientDetailsUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Component
public class DhisOauthAuthenticationProvider extends DaoAuthenticationProvider {
  @Autowired
  public DhisOauthAuthenticationProvider(
      @Qualifier("defaultClientDetailsUserDetailsService")
          DefaultClientDetailsUserDetailsService detailsService) {
    setUserDetailsService(detailsService);
    setPasswordEncoder(NoOpPasswordEncoder.getInstance());
  }

  @Override
  public Authentication authenticate(Authentication auth) throws AuthenticationException {
    log.info(
        String.format(
            "DhisOauthAuthenticationProvider authenticate attempt Authentication.getName(): %s",
            auth.getName()));

    String username = auth.getName();

    UserDetails user = getUserDetailsService().loadUserByUsername(username);

    if (user == null) {
      throw new BadCredentialsException("Invalid username or password");
    }

    // -------------------------------------------------------------------------
    // Delegate authentication downstream, using User as
    // principal
    // -------------------------------------------------------------------------

    Authentication result = super.authenticate(auth);

    // Put detached state of the user credentials into the session as user
    // must not be updated during session execution
    user = SerializationUtils.clone(user);

    return new UsernamePasswordAuthenticationToken(
        user, result.getCredentials(), result.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }
}
