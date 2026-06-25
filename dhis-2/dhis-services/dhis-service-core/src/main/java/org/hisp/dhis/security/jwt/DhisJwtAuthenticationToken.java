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
package org.hisp.dhis.security.jwt;

import java.util.Collection;
import java.util.List;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * DHIS2-specific {@link JwtAuthenticationToken} produced after a JWT bearer token has been
 * validated and mapped to a DHIS2 user.
 *
 * <p>Instances are created by {@link Dhis2JwtAuthenticationManagerResolver} once the token
 * signature has been verified against the issuer's JWKS and the token's mapping claim (typically
 * {@code username} or {@code email}) has been resolved to a DHIS2 {@link UserDetails}. The mapping
 * claim has already been consumed upstream by the resolver, so the wrapped {@link DhisOidcUser}
 * uses {@link IdTokenClaimNames#SUB} as its name attribute. The token wraps three things:
 *
 * <ul>
 *   <li>The raw validated {@link Jwt}, available through {@link #getToken()}.
 *   <li>The collection of {@link GrantedAuthority} authorities granted to the authenticated DHIS2
 *       user.
 *   <li>A {@link DhisOidcUser} principal that carries the full DHIS2 {@link UserDetails}, exposed
 *       via {@link #getPrincipal()}.
 * </ul>
 *
 * <p>This is the concrete {@code Authentication} instance returned by {@code
 * SecurityContextHolder.getContext().getAuthentication()} for every JWT-authenticated request, and
 * the type downstream code should cast to when it needs access to the DHIS2 user and the original
 * JWT claims.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DhisJwtAuthenticationToken extends JwtAuthenticationToken {
  private final DhisOidcUser dhisOidcUser;

  /**
   * Create a new authenticated token for a validated JWT and the DHIS2 user it maps to.
   *
   * @param jwt the validated JWT bearer token
   * @param authorities the authorities granted to the DHIS2 user
   * @param name the value of the token's mapping claim (used as the {@code name} of the
   *     authentication)
   * @param user the DHIS2 {@link UserDetails} the mapping claim resolved to
   */
  public DhisJwtAuthenticationToken(Jwt jwt, String name, UserDetails user) {
    super(jwt, List.of(), name);

    this.dhisOidcUser = new DhisOidcUser(user, jwt.getClaims(), IdTokenClaimNames.SUB, null);
  }

  /**
   * Return the {@link DhisOidcUser} principal wrapping the resolved DHIS2 {@link UserDetails} and
   * the JWT claims. Overrides the default {@link JwtAuthenticationToken} behaviour of returning the
   * raw {@link Jwt} as principal.
   */
  @Override
  public Object getPrincipal() {
    return this.dhisOidcUser;
  }

  @Override
  public Collection<GrantedAuthority> getAuthorities() {
    return (Collection<GrantedAuthority>) dhisOidcUser.getAuthorities();
  }
}
