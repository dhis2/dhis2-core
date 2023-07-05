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
package org.hisp.dhis.webapi;

import static java.lang.String.format;
import static org.hisp.dhis.utils.JavaToJson.singleToDoubleQuotes;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.web.WebClientUtils.failOnException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.utils.DhisMockMvcControllerTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class for all Spring Mock MVC based controller tests.
 *
 * @author Viet Nguyen
 */
public class DhisControllerTestBase extends DhisMockMvcControllerTest {
  protected MockMvc mvc;

  protected MockHttpSession session;

  protected User superUser;

  protected User currentUser;

  protected final String getSuperuserUid() {
    return superUser.getUid();
  }

  protected final User getCurrentUser() {
    return currentUser;
  }

  public User getSuperUser() {
    return superUser;
  }

  protected final User switchToSuperuser() {
    switchContextToUser(userService.getUser(superUser.getUid()));
    return superUser;
  }

  protected final User switchToNewUser(String username, String... authorities) {
    if (superUser != null) {
      // we need to be an admin to be allowed to create user groups
      switchContextToUser(superUser);
    }

    currentUser = createUserWithAuth(username, authorities);
    switchContextToUser(currentUser);
    return currentUser;
  }

  protected final User switchToNewUser(User user) {
    currentUser = user;
    switchContextToUser(currentUser);
    return currentUser;
  }

  protected void switchContextToUser(User user) {
    injectSecurityContext(user);

    session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        SecurityContextHolder.getContext());
  }

  protected final HttpResponse POST_MULTIPART(String url, MockMultipartFile part) {
    return webRequest(multipart(url).file(part));
  }

  @Override
  protected final HttpResponse webRequest(MockHttpServletRequestBuilder request) {
    return failOnException(
        () ->
            new HttpResponse(
                toResponse(mvc.perform(request.session(session)).andReturn().getResponse())));
  }

  protected final MvcResult webRequestWithMvcResult(MockHttpServletRequestBuilder request) {
    return failOnException(() -> mvc.perform(request.session(session)).andReturn());
  }

  protected final void assertJson(String expected, HttpResponse actual) {
    assertEquals(singleToDoubleQuotes(expected), actual.content().toString());
  }

  protected final String addDataElement(
      String name, String code, ValueType valueType, String optionSet, String categoryCombo) {
    return assertStatus(
        HttpStatus.CREATED, postNewDataElement(name, code, valueType, optionSet, categoryCombo));
  }

  protected final HttpResponse postNewDataElement(
      String name, String code, ValueType valueType, String optionSet, String categoryCombo) {
    return POST(
        "/dataElements/",
        format(
            "{'name':'%s', 'shortName':'%s', 'code':'%s', 'valueType':'%s', "
                + "'aggregationType':'SUM', 'zeroIsSignificant':false, 'domainType':'AGGREGATE', "
                + "'categoryCombo': {'id': '%s'},"
                + "'optionSet': %s"
                + "}",
            name,
            code,
            code,
            valueType,
            categoryCombo,
            optionSet == null ? "null" : "{'id':'" + optionSet + "'}"));
  }
}
