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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.Map;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserDetailsImpl;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

/**
 * Jackson module that teaches the OAuth2 Authorization Server's persistence {@link
 * com.fasterxml.jackson.databind.ObjectMapper} (configured in {@link
 * Dhis2OAuth2AuthorizationServiceImpl}) how to (de)serialize the DHIS2-custom principal types that
 * can end up in an {@link
 * org.springframework.security.oauth2.server.authorization.OAuth2Authorization}'s {@code
 * attributes} map.
 *
 * <p>When a user authenticates through an external OIDC provider, the Spring principal carried in
 * the {@code authorization_code} flow is an {@code OAuth2AuthenticationToken} whose principal is a
 * {@link DhisOidcUser} wrapping a {@link UserDetailsImpl}. {@link SecurityJackson2Modules} enables
 * Jackson default typing guarded by an <em>allowlist</em>: a concrete type is only deserialized if
 * it is one of Spring's trusted classes or has an explicitly registered mixin. Neither {@code
 * DhisOidcUser} nor {@code UserDetailsImpl} is trusted, so without the mixins registered here a
 * read of such an authorization (e.g. the {@code /oauth2/token} exchange) fails with {@code "... is
 * not in the allowlist"}. Serialization succeeds either way, which is why the failure only surfaces
 * on read.
 *
 * <p>{@link DhisOidcUserMixin} both allowlists {@code DhisOidcUser} and declares how to rebuild it.
 * {@code UserDetailsImpl} already declares its own JSON shape (Lombok builder plus field
 * visibility) on the class; {@link UserDetailsImplMixin} is registered purely to add it to the
 * allowlist.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class Dhis2OAuth2PrincipalJackson2Module extends SimpleModule {

  public Dhis2OAuth2PrincipalJackson2Module() {
    super(Dhis2OAuth2PrincipalJackson2Module.class.getName());
  }

  @Override
  public void setupModule(SetupContext context) {
    context.setMixInAnnotations(DhisOidcUser.class, DhisOidcUserMixin.class);
    context.setMixInAnnotations(UserDetailsImpl.class, UserDetailsImplMixin.class);
  }

  /**
   * Mixin for {@link DhisOidcUser}. The wrapped {@link UserDetails}, the OIDC {@code attributes}
   * (claims), the name-attribute key and the ID token are sufficient to reconstruct the principal.
   * The {@code authorities} are derived from the wrapped user by the {@code DhisOidcUser}
   * constructor, so they are not a creator argument: they are still serialized (field visibility is
   * {@code ANY}, matching Spring's own {@code DefaultOAuth2UserMixin}) but ignored on read.
   *
   * <p>{@code "id"} is ignored because {@code DhisOidcUser} inherits {@link
   * org.hisp.dhis.common.UidObject#getUid()}, which is annotated {@code @JsonProperty("id")}; left
   * in, that string UID would be written as {@code "id"} and then fail on read against {@code
   * setId(Long)}. The user's identity is preserved in the nested {@code user} object.
   *
   * <p>No {@code @JsonTypeInfo} is needed: the principal's static type is the {@code OAuth2User}
   * interface, so the type id is already written by the mapper's allowlist default typing.
   */
  // Must be a class, not an interface (S1610): a Jackson mix-in that maps a @JsonCreator onto the
  // target's constructor has to declare a matching constructor, which interfaces cannot.
  @SuppressWarnings("java:S1610")
  @JsonAutoDetect(
      fieldVisibility = JsonAutoDetect.Visibility.ANY,
      getterVisibility = JsonAutoDetect.Visibility.NONE,
      isGetterVisibility = JsonAutoDetect.Visibility.NONE)
  @JsonIgnoreProperties(
      value = {"id"},
      ignoreUnknown = true)
  abstract static class DhisOidcUserMixin {
    @JsonCreator
    DhisOidcUserMixin(
        @JsonProperty("user") UserDetails user,
        @JsonProperty("attributes") Map<String, Object> attributes,
        @JsonProperty("nameAttributeKey") String nameAttributeKey,
        @JsonProperty("oidcIdToken") OidcIdToken oidcIdToken) {}
  }

  /**
   * Allowlist marker for {@link UserDetailsImpl}, whose own (de)serialization shape is declared on
   * the class. Registering any mix-in is what adds the type to {@link SecurityJackson2Modules}'
   * deserialization allowlist; the {@code ignoreUnknown} setting just mirrors the class so a stray
   * property never breaks the read.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface UserDetailsImplMixin {}
}
