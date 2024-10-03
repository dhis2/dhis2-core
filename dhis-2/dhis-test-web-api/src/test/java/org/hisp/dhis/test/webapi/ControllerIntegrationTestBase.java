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
package org.hisp.dhis.test.webapi;

import static java.lang.String.format;
import static org.hisp.dhis.test.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.test.web.WebClientUtils.failOnException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import jakarta.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.IntegrationTestBase;
import org.hisp.dhis.test.web.HttpMethod;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.web.WebClient;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Main base class for all Spring Mock MVC based controller integration tests. The Spring context is
 * configured to contain the Spring components to test the DHIS2 web app.
 *
 * <p>Concrete test classes should extend one of {@link PostgresControllerIntegrationTestBase} or
 * {@link H2ControllerIntegrationTestBase}.
 *
 * <p>Read through
 *
 * <ul>
 *   <li>{@link org.hisp.dhis.test}
 *   <li>{@link ContextConfiguration}
 *   <li>{@link ActiveProfiles}
 * </ul>
 *
 * if you are unsure how to get started and certainly before you create another base test class!
 *
 * @author Viet Nguyen
 */
@WebAppConfiguration
@ContextConfiguration(classes = {MvcTestConfig.class, WebTestConfig.class})
public abstract class ControllerIntegrationTestBase extends IntegrationTestBase
    implements WebClient {

  @Autowired protected WebApplicationContext webApplicationContext;

  @Autowired private RenderService _renderService;

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected DbmsManager dbmsManager;

  @Autowired private TransactionTemplate txTemplate;

  @Getter private User currentUser;

  protected MockMvc mvc;

  protected MockHttpSession session;

  protected final String getAdminUid() {
    return getAdminUser().getUid();
  }

  @BeforeEach
  void setup() {
    renderService = _renderService;

    mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    switchContextToUser(getAdminUser());
    currentUser = getAdminUser();
  }

  protected final void doInTransaction(Runnable operation) {
    final int defaultPropagationBehaviour = txTemplate.getPropagationBehavior();
    txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    txTemplate.execute(
        status -> {
          operation.run();
          return null;
        });
    // restore original propagation behaviour
    txTemplate.setPropagationBehavior(defaultPropagationBehaviour);
  }

  protected final User switchToAdminUser() {
    switchContextToUser(userService.getUser(getAdminUser().getUid()));
    return getAdminUser();
  }

  protected final User switchToNewUser(String username, String... authorities) {
    if (getAdminUser() != null) {
      // we need to be an admin to be allowed to create user groups
      switchContextToUser(getAdminUser());
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
    injectSecurityContextUser(user);

    session = new MockHttpSession();
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        SecurityContextHolder.getContext());
  }

  public static ResponseAdapter toResponse(MockHttpServletResponse response) {
    return new MockMvcResponseAdapter(response);
  }

  @Override
  public HttpResponse webRequest(
      HttpMethod method, String url, List<Header> headers, String contentType, String content) {
    return webRequest(buildMockRequest(method, url, headers, contentType, content));
  }

  protected final HttpResponse POST_MULTIPART(String url, MockMultipartFile part) {
    return webRequest(multipart(makeApiUrl(url)).file(part));
  }

  protected HttpResponse webRequest(MockHttpServletRequestBuilder request) {
    return failOnException(
        () ->
            new HttpResponse(
                toResponse(mvc.perform(request.session(session)).andReturn().getResponse())));
  }

  protected String makeApiUrl(String path) {
    if (path.startsWith("/api/")) {
      return path;
    }
    return "/api/" + path;
  }

  protected MockHttpServletRequestBuilder buildMockRequest(
      HttpMethod method, String url, List<Header> headers, String contentType, String content) {

    MockHttpServletRequestBuilder request =
        MockMvcRequestBuilders.request(
            org.springframework.http.HttpMethod.valueOf(method.name()), makeApiUrl(url));

    for (Header header : headers) {
      request.header(header.getName(), header.getValue());
    }
    if (contentType != null) {
      request.contentType(contentType);
    }
    if (content != null) {
      request.content(content);
    }

    return request;
  }

  private static class MockMvcResponseAdapter implements ResponseAdapter {

    private final MockHttpServletResponse response;

    MockMvcResponseAdapter(MockHttpServletResponse response) {
      this.response = response;
    }

    @Override
    public int getStatus() {
      return response.getStatus();
    }

    @Override
    public String getContent() {
      try {
        return response.getContentAsString(StandardCharsets.UTF_8);
      } catch (UnsupportedEncodingException ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public String getErrorMessage() {
      return response.getErrorMessage();
    }

    @Override
    public String getHeader(String name) {
      return response.getHeader(name);
    }

    @Override
    public String[] getCookies() {
      Cookie[] cookies = response.getCookies();
      return Arrays.stream(cookies).map(Cookie::getValue).toArray(String[]::new);
    }
  }

  protected final MvcResult webRequestWithMvcResult(MockHttpServletRequestBuilder request) {
    return failOnException(() -> mvc.perform(request.session(session)).andReturn());
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
