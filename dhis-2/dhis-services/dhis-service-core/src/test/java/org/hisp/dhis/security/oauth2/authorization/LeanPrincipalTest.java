/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.security.oauth2.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/**
 * Unit tests for {@link Dhis2OAuth2AuthorizationServiceImpl#leanPrincipal} — the swap that keeps
 * the persisted OAuth2 authorization principal Spring-native (so it never trips the Jackson
 * allowlist).
 */
class LeanPrincipalTest {

  @Test
  void noPrincipalAttribute_isUnchanged() {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("state", "xyz");
    assertSame(attributes, Dhis2OAuth2AuthorizationServiceImpl.leanPrincipal(attributes));
  }

  @Test
  void nonUserDetailsPrincipal_isUnchanged() {
    // client_credentials-style: an Authentication whose principal is the client id (a String).
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put(
        Principal.class.getName(),
        new UsernamePasswordAuthenticationToken("client-id", null, List.of()));
    assertSame(attributes, Dhis2OAuth2AuthorizationServiceImpl.leanPrincipal(attributes));
  }

  @Test
  void formLoginUserDetailsImplPrincipal_isLeaned() {
    UserDetailsImpl userDetails = userDetails("formuser");
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put(
        Principal.class.getName(),
        new UsernamePasswordAuthenticationToken(
            userDetails, "secret", userDetails.getAuthorities()));

    assertLean(Dhis2OAuth2AuthorizationServiceImpl.leanPrincipal(attributes), "formuser");
  }

  @Test
  void federatedDhisOidcUserPrincipal_isLeaned() {
    UserDetailsImpl userDetails = userDetails("oidcuser");
    Map<String, Object> oidcClaims = Map.of(IdTokenClaimNames.SUB, "google-sub-1");
    OidcIdToken idToken =
        OidcIdToken.withTokenValue("id-token")
            .subject("google-sub-1")
            .claims(c -> c.putAll(oidcClaims))
            .build();
    DhisOidcUser oidcPrincipal =
        new DhisOidcUser(userDetails, oidcClaims, IdTokenClaimNames.SUB, idToken);
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put(
        Principal.class.getName(),
        new OAuth2AuthenticationToken(oidcPrincipal, oidcPrincipal.getAuthorities(), "google"));

    // The DhisOidcUser's getName() is the IdP sub; the leaned principal must carry the DHIS2
    // username.
    assertLean(Dhis2OAuth2AuthorizationServiceImpl.leanPrincipal(attributes), "oidcuser");
  }

  private static void assertLean(Map<String, Object> result, String expectedUsername) {
    Object principal = result.get(Principal.class.getName());
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, principal);
    UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
    assertEquals(expectedUsername, token.getName());
    assertTrue(token.isAuthenticated());
    assertEquals(
        Set.of("ALL"),
        token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet()));
  }

  private static UserDetailsImpl userDetails(String username) {
    return UserDetailsImpl.builder()
        .uid("uid-" + username)
        .username(username)
        .authorities(new ArrayList<>(List.of(new SimpleGrantedAuthority("ALL"))))
        .allAuthorities(new HashSet<>(Set.of("ALL")))
        .allRestrictions(new HashSet<>())
        .userGroupIds(new HashSet<>())
        .userOrgUnitIds(new HashSet<>())
        .userDataOrgUnitIds(new HashSet<>())
        .userSearchOrgUnitIds(new HashSet<>())
        .userEffectiveSearchOrgUnitIds(new HashSet<>())
        .userRoleIds(new HashSet<>())
        .managedGroupLongIds(new HashSet<>())
        .userRoleLongIds(new HashSet<>())
        .build();
  }
}
