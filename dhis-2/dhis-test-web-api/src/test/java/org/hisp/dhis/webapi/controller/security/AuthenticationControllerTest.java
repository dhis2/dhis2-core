/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisAuthenticationApiTest;
import org.hisp.dhis.webapi.json.domain.JsonLoginResponse;
import org.hisp.dhis.webapi.json.domain.JsonUser;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@RunWith(SpringRunner.class)
class AuthenticationControllerTest extends DhisAuthenticationApiTest {

  @Autowired private SessionRegistry sessionRegistry;


  @Test void testSuccessfulLogin() {
    JsonLoginResponse response = POST("/auth/login",
        "{'username':'admin','password':'district'}").content(HttpStatus.OK)
        .as(JsonLoginResponse.class);

    assertEquals("SUCCESS", response.getLoginStatus());
    assertEquals("/dhis-web-dashboard", response.getRedirectUrl());


  }

  @Test void testWrongUsernameOrPassword() {
    JsonWebMessage response = POST("/auth/login",
        "{'username':'admin','password':'district9'}").content(HttpStatus.UNAUTHORIZED)
        .as(JsonWebMessage.class);

    assertEquals("Bad credentials", response.getMessage());
    assertEquals("Unauthorized", response.getHttpStatus());
    assertEquals(401, response.getHttpStatusCode());
    assertEquals("ERROR", response.getStatus());
  }


  @Test void testLogin() {
    clearSecurityContext();


    GET("/users",WebClient.CookieHeader("JSESSIONID=123")).content(HttpStatus.OK);

    HttpResponse response = POST("/auth/login", "{'username':'admin','password':'district'}");
    assertNotNull(response);
    String[] cookies = response.cookies();
    String cookie = response.header("Set-Cookie");
//    assertNotNull(cookie);

    assertEquals(1, sessionRegistry.getAllPrincipals().size());
    Object actual = sessionRegistry.getAllPrincipals().get(0);

    JsonUser user = GET("/me?fields=settings,id", WebClient.CookieHeader("1")).content()
        .as(JsonUser.class);

    assertNotNull(user);
  }
}
