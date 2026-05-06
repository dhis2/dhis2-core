/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Verifies that {@link DhisOidcUserService#loadUser(OidcUserRequest)} dispatches to the JSON path
 * or the JWT path based on the {@link UserInfoResponseType} configured on the provider
 * registration.
 *
 * <p>Uses {@code @Spy} on the service and {@code doReturn} to short-circuit the two path methods so
 * no real HTTP or Spring context is needed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DhisOidcUserServiceDispatchTest {

  @Mock private DhisOidcProviderRepository repo;
  @Mock private UserService userService;
  @Mock private SignedJwtUserInfoLoader signedJwtLoader;
  @Mock private OidcUserRequest request;
  @Mock private ClientRegistration clientRegistration;
  @Mock private OidcUser jsonResult;
  @Mock private OidcUser jwtResult;

  @Spy private DhisOidcUserService service = new DhisOidcUserService();

  @BeforeEach
  void wire() {
    service.userService = userService;
    service.clientRegistrationRepository = repo;
    service.signedJwtUserInfoLoader = signedJwtLoader;
    when(request.getClientRegistration()).thenReturn(clientRegistration);
    when(clientRegistration.getRegistrationId()).thenReturn("p1");
  }

  @Test
  void jsonRegistrationUsesJsonPath() {
    DhisOidcClientRegistration reg =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JSON)
            .build();
    when(repo.getDhisOidcClientRegistration("p1")).thenReturn(reg);
    doReturn(jsonResult).when(service).loadFromJsonUserInfo(request, reg);

    OidcUser result = service.loadUser(request);

    assertSame(jsonResult, result);
    verify(signedJwtLoader, never()).load(any(), any());
  }

  @Test
  void jwtRegistrationUsesJwtPath() {
    DhisOidcClientRegistration reg =
        DhisOidcClientRegistration.builder()
            .clientRegistration(clientRegistration)
            .mappingClaimKey("sub")
            .userInfoResponseType(UserInfoResponseType.JWT)
            .build();
    when(repo.getDhisOidcClientRegistration("p1")).thenReturn(reg);
    when(signedJwtLoader.load(request, reg)).thenReturn(jwtResult);

    OidcUser result = service.loadUser(request);

    assertSame(jwtResult, result);
    verify(service, never()).loadFromJsonUserInfo(any(), any());
  }
}
