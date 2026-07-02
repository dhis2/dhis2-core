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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Verifies that {@code POST /api/auth/updatePassword} is part of the unauthenticated {@code
 * permitAll} allowlist in {@link org.hisp.dhis.webapi.security.config.DhisWebApiWebSecurityConfig}
 * and is therefore reachable without authentication.
 *
 * <p>This is the JSON self-service endpoint a user with an expired password must call to set a new
 * one (the "require password change after N months" flow). Such a user is by definition not
 * authenticated, so the endpoint has to be reachable anonymously. It is safe to expose because
 * {@link org.hisp.dhis.webapi.controller.security.UserAccountController#updatePassword} only
 * proceeds for an expired account and only after the supplied old password matches the stored one.
 *
 * <p>It guards the JSON endpoint that replaced the removed legacy form-param {@code POST
 * /api/account/password}.
 *
 * <p>Unlike the default controller test bases, {@link AuthenticationApiTestBase} wires the real
 * {@link org.springframework.security.web.FilterChainProxy} into MockMvc, so the actual allowlist
 * is evaluated. The request is issued with raw {@code mvc.perform(...)} (not the {@code POST(...)}
 * helper, which would attach the admin session) so it reaches the filter chain anonymously.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class AuthUpdatePasswordPermitAllTest extends AuthenticationApiTestBase {

  @Test
  void postAuthUpdatePasswordIsReachableWithoutAuthentication() throws Exception {
    clearSecurityContext();

    // Anonymous request: no session, no Authorization header. The seeded "admin" user exists and is
    // not expired, so the controller short-circuits with 400 "Account is not expired" before any
    // password check. A 302 redirect to /login would mean the endpoint is not on the allowlist.
    mvc.perform(
            post("/api/auth/updatePassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"admin\",\"oldPassword\":\"irrelevant\",\"newPassword\":\"irrelevant\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("Account is not expired")));
  }
}
