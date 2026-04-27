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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms the OAuth2 Authorization Server surface is disabled on 2.43.0. The DCR enrollment
 * endpoint is unreachable (404) because it is gated by {@code AuthorizationServerEnabledCondition}.
 * The three CRUD/list controllers are superuser-only — non-admins get 403.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class OAuth2AuthorizationServerDisabledTest extends H2ControllerIntegrationTestBase {

  @Test
  void dcrEnrollDevice_returns404() {
    assertEquals(HttpStatus.NOT_FOUND, GET("/enrollDevice?client_id=x").status());
  }

  @Test
  void oauth2Clients_nonAdmin_returns403() {
    switchToNewUser("guest-clients");
    assertEquals(HttpStatus.FORBIDDEN, GET("/oAuth2Clients").status());
  }

  @Test
  void oauth2Authorizations_nonAdmin_returns403() {
    switchToNewUser("guest-auth");
    assertEquals(HttpStatus.FORBIDDEN, GET("/oAuth2Authorizations").status());
  }

  @Test
  void oauth2AuthorizationConsents_nonAdmin_returns403() {
    switchToNewUser("guest-consent");
    assertEquals(HttpStatus.FORBIDDEN, GET("/oAuth2AuthorizationConsents").status());
  }
}
