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
import static org.hisp.dhis.test.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.test.web.WebClientUtils.failOnException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.servlet.http.Cookie;
import lombok.Getter;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.DhisConvenienceTest;
import org.hisp.dhis.test.config.H2DhisConfiguration;
import org.hisp.dhis.test.config.PostgresDhisConfiguration;
import org.hisp.dhis.test.utils.TestUtils;
import org.hisp.dhis.test.web.HttpMethod;
import org.hisp.dhis.test.web.HttpStatus;
import org.hisp.dhis.test.web.WebClient;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

/**
 * Main base class for all Spring Mock MVC based controller integration tests. The Spring context is
 * configured to contain the Spring components to test the DHIS2 web app.
 *
 * <p>Concrete test classes
 *
 * <ul>
 *   <li>should ideally extend one of {@link PostgresControllerIntegrationTestBase} or {@link
 *       H2ControllerIntegrationTestBase}
 *   <li>only if the above do not fit should they directly extend this class and pick the DB they
 *       want to test against via the {@link ActiveProfiles} matching one of the {@link Profile}
 *       declared in {@link PostgresDhisConfiguration} or {@link H2DhisConfiguration}
 * </ul>
 *
 * Refer to {@link ContextConfiguration} and {@link ActiveProfiles} before creating yet another base
 * class.
 *
 * @author Viet Nguyen
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(
    classes = {
      H2DhisConfiguration.class,
      PostgresDhisConfiguration.class,
      MvcTestConfiguration.class,
      WebTestConfiguration.class
    })
@Transactional
public abstract class ControllerTestBase extends DhisConvenienceTest implements WebClient {

  @Autowired protected WebApplicationContext webApplicationContext;

  @Autowired private UserService _userService;

  @Autowired private RenderService _renderService;

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected DbmsManager dbmsManager;

  @Autowired private TransactionTemplate txTemplate;

  @Getter protected User adminUser;

  @Getter protected User superUser;

  @Getter protected User currentUser;

  protected MockMvc mvc;

  protected MockHttpSession session;

  protected final String getSuperuserUid() {
    return superUser.getUid();
  }

  @BeforeEach
  void setup() {
    userService = _userService;
    renderService = _renderService;
    clearSecurityContext();

    this.adminUser = _preCreateInjectAdminUserWithoutPersistence();
    manager.persist(adminUser);
    _injectSecurityContextUser(adminUser);

    superUser = createAndAddAdminUser("ALL");

    mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

    switchContextToUser(superUser);
    currentUser = superUser;

    TestUtils.executeStartupRoutines(webApplicationContext);

    dbmsManager.flushSession();
    dbmsManager.clearSession();
  }

  private void _injectSecurityContextUser(User user) {
    if (user == null) {
      clearSecurityContext();
      return;
    }
    hibernateService.flushSession();
    User user1 = manager.find(User.class, user.getId());
    injectSecurityContext(UserDetails.fromUser(user1));
  }

  private User _preCreateInjectAdminUserWithoutPersistence() {
    UserRole role = createUserRole("Superuser_Test_" + CodeGenerator.generateUid(), "ALL");
    role.setUid(CodeGenerator.generateUid());
    manager.persist(role);
    User user = new User();
    String uid = CodeGenerator.generateUid();
    user.setUid(uid);
    user.setFirstName("Firstname_" + uid);
    user.setSurname("Surname_" + uid);
    user.setUsername(DEFAULT_USERNAME + "_test_" + CodeGenerator.generateUid());
    user.setPassword(DEFAULT_ADMIN_PASSWORD);
    user.getUserRoles().add(role);
    user.setLastUpdated(new Date());
    user.setCreated(new Date());
    return user;
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

  protected final User switchToSuperuser() {
    switchContextToUser(userService.getUser(superUser.getUid()));
    return superUser;
  }

  /**
   * Method which allows passing in actual {@link Authorities}. It calls the existing method {@link
   * ControllerTestBase#switchToNewUser(String, String...)} that accepts String authorities
   * underneath.
   *
   * @param username - username
   * @param authorities - varargs of {@link Authorities}
   * @return new {@link User}
   */
  protected final User switchToNewUserWithAuthorities(String username, Authorities... authorities) {
    return switchToNewUser(username, Authorities.toStringArray(authorities));
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
            org.springframework.http.HttpMethod.resolve(method.name()), makeApiUrl(url));

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
