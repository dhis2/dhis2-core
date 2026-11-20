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
package org.hisp.dhis.security.oidc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

/**
 * Unit tests for {@link DhisOidcLogoutSuccessHandler} verifying that the redirect_uri parameter is
 * validated against the device enrollment redirect allowlist from system settings.
 */
@ExtendWith(MockitoExtension.class)
class DhisOidcLogoutSuccessHandlerTest {

  @Mock private DhisConfigurationProvider config;
  @Mock private DhisOidcProviderRepository dhisOidcProviderRepository;
  @Mock private UserService userService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private SystemSettings systemSettings;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private Authentication authentication;

  private DhisOidcLogoutSuccessHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new DhisOidcLogoutSuccessHandler(
            config, dhisOidcProviderRepository, userService, systemSettingsService);
    handler.init();
  }

  private void stubAllowlist(String allowlist) {
    when(systemSettingsService.getCurrentSettings()).thenReturn(systemSettings);
    when(systemSettings.getDeviceEnrollmentRedirectAllowlist()).thenReturn(allowlist);
  }

  @Test
  @DisplayName("Allowed redirect URI results in redirect to that URI")
  void testAllowedRedirectUri() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("dhis2oauth://oauth");
    stubAllowlist("dhis2oauth://oauth");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("dhis2oauth://oauth");
  }

  @Test
  @DisplayName("Allowed redirect URI with wildcard glob pattern matches")
  void testAllowedRedirectUriWithGlob() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("dhis2oauth://oauth/callback");
    stubAllowlist("dhis2oauth://oauth/*");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("dhis2oauth://oauth/callback");
  }

  @Test
  @DisplayName("Disallowed redirect URI does not redirect to that URI")
  void testDisallowedRedirectUri() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("https://evil.com");
    stubAllowlist("dhis2oauth://oauth");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response, never()).sendRedirect("https://evil.com");
  }

  @Test
  @DisplayName("No redirect_uri parameter does not consult allowlist")
  void testNoRedirectUriParameter() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn(null);

    handler.onLogoutSuccess(request, response, authentication);

    verifyNoInteractions(systemSettingsService);
  }

  @Test
  @DisplayName("Empty redirect_uri parameter does not consult allowlist")
  void testBlankRedirectUriParameter() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("");

    handler.onLogoutSuccess(request, response, authentication);

    verifyNoInteractions(systemSettingsService);
  }

  @Test
  @DisplayName("Multiple allowlist entries - matching second entry succeeds")
  void testMultipleAllowlistEntries() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("myapp://callback");
    stubAllowlist("dhis2oauth://oauth, myapp://callback");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response).sendRedirect("myapp://callback");
  }

  @Test
  @DisplayName("Redirect URI not matching any allowlist entry is rejected")
  void testRedirectUriNotMatchingAnyEntry() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("https://attacker.com/steal");
    stubAllowlist("dhis2oauth://oauth, myapp://callback");

    handler.onLogoutSuccess(request, response, authentication);

    verify(response, never()).sendRedirect("https://attacker.com/steal");
  }

  @Test
  @DisplayName("Anonymous logout redirects to an allowed mobile callback")
  void testAnonymousLogoutAllowedCallback() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("dhis2oauth://oauth");
    stubAllowlist("dhis2oauth://oauth");

    handler.redirectAfterAnonymousLogout(request, response);

    verify(response).sendRedirect("dhis2oauth://oauth");
    verifyNoInteractions(userService, dhisOidcProviderRepository);
  }

  @Test
  @DisplayName("Anonymous logout with untrusted callback redirects to the login page")
  void testAnonymousLogoutUntrustedCallback() throws Exception {
    when(request.getParameter("redirect_uri")).thenReturn("https://evil.com");
    when(request.getContextPath()).thenReturn("/dhis");
    stubAllowlist("dhis2oauth://oauth");

    handler.redirectAfterAnonymousLogout(request, response);

    verify(response).sendRedirect("/dhis/login/");
    verify(response, never()).sendRedirect("https://evil.com");
    verifyNoInteractions(userService, dhisOidcProviderRepository);
  }

  @Test
  @DisplayName("Anonymous logout without callback redirects to the login page")
  void testAnonymousLogoutWithoutCallback() throws Exception {
    when(request.getContextPath()).thenReturn("");

    handler.redirectAfterAnonymousLogout(request, response);

    verify(response).sendRedirect("/login/");
    verifyNoInteractions(userService, dhisOidcProviderRepository, systemSettingsService);
  }

  /**
   * Linked accounts logout: only an authenticated OIDC POST may switch to a linked account. Uses a
   * real configuration, request and response so that redirects are asserted as an HTTP client would
   * see them; only the collaborators that hold data ({@link UserService}, provider repository) are
   * mocked and are stubbed only where the request reaches them.
   */
  @Nested
  @DisplayName("Linked accounts logout")
  class LinkedAccountsLogout {

    private static final String OPEN_ID = "alice@example.org";
    private static final String LOGOUT_URL = "/logged-out";
    private static final String RELOGIN_URL = "/relogin";

    private final Map<String, User> accounts =
        Map.of(
            "alice", user("alice", OPEN_ID, false),
            "alice2", user("alice2", OPEN_ID, false),
            "mallory", user("mallory", "other@example.org", false),
            "no-openid", user("no-openid", null, false),
            "disabled", user("disabled", OPEN_ID, true));

    private DhisOidcLogoutSuccessHandler linkedHandler;

    @BeforeEach
    void setUpLinkedAccounts() {
      Properties properties = new Properties();
      properties.setProperty(ConfigurationKey.OIDC_OAUTH2_LOGIN_ENABLED.getKey(), "on");
      properties.setProperty(ConfigurationKey.LINKED_ACCOUNTS_ENABLED.getKey(), "on");
      properties.setProperty(ConfigurationKey.LINKED_ACCOUNTS_LOGOUT_URL.getKey(), LOGOUT_URL);
      properties.setProperty(ConfigurationKey.LINKED_ACCOUNTS_RELOGIN_URL.getKey(), RELOGIN_URL);

      linkedHandler =
          new DhisOidcLogoutSuccessHandler(
              new TestDhisConfigurationProvider(properties),
              dhisOidcProviderRepository,
              userService,
              systemSettingsService);
      linkedHandler.init();
    }

    private static User user(String username, String openId, boolean disabled) {
      User user = new User();
      user.setUsername(username);
      user.setOpenId(openId);
      user.setDisabled(disabled);
      return user;
    }

    /** Authentication of the given kind for DHIS2 user alice; {@code none} is no authentication. */
    private static Authentication authentication(String kind, String emailClaim) {
      return switch (kind) {
        case "none" -> null;
        case "password" ->
            new UsernamePasswordAuthenticationToken(
                "alice", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        case "oidc", "unauthenticated-oidc" -> {
          UserDetails userDetails =
              UserDetails.empty()
                  .username("alice")
                  .authorities(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER")))
                  .build();
          DhisOidcUser principal =
              new DhisOidcUser(
                  userDetails, Map.of("sub", "sub-1", "email", emailClaim), "sub", null);
          OAuth2AuthenticationToken token =
              new OAuth2AuthenticationToken(
                  principal, List.of(new SimpleGrantedAuthority("ROLE_USER")), "google");
          token.setAuthenticated(kind.equals("oidc"));
          yield token;
        }
        default -> throw new IllegalArgumentException(kind);
      };
    }

    private static Authentication authentication(String kind) {
      return authentication(kind, OPEN_ID);
    }

    private static MockHttpServletRequest logoutRequest(String method, String switchTo) {
      MockHttpServletRequest logoutRequest = new MockHttpServletRequest(method, "/api/logout");
      if (switchTo != null) {
        logoutRequest.setParameter("switch", switchTo);
      }
      return logoutRequest;
    }

    private void givenRegistration() {
      when(dhisOidcProviderRepository.getDhisOidcClientRegistration("google"))
          .thenReturn(DhisOidcClientRegistration.builder().mappingClaimKey("email").build());
    }

    /** Resolves every looked-up username from {@link #accounts}; unknown usernames give null. */
    private void givenAccounts() {
      when(userService.getUserByUsername(anyString()))
          .thenAnswer(invocation -> accounts.get(invocation.<String>getArgument(0)));
    }

    private MockHttpServletResponse logout(
        MockHttpServletRequest logoutRequest, Authentication auth) throws Exception {
      MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
      linkedHandler.onLogoutSuccess(logoutRequest, logoutResponse, auth);
      return logoutResponse;
    }

    @Test
    @DisplayName("POST by an OIDC session switches to a linked account and redirects to re-login")
    void testSwitchToLinkedAccount() throws Exception {
      givenRegistration();
      givenAccounts();

      MockHttpServletResponse result =
          logout(logoutRequest("POST", "alice2"), authentication("oidc"));

      assertEquals(RELOGIN_URL, result.getRedirectedUrl());
      verify(userService).setActiveLinkedAccounts("alice", "alice2");
    }

    @Test
    @DisplayName("The acting user is the authenticated user, never the current request parameter")
    void testActingUserIsNotTakenFromRequest() throws Exception {
      givenRegistration();
      givenAccounts();
      MockHttpServletRequest spoofed = logoutRequest("POST", "alice2");
      spoofed.setParameter("current", "victim");

      MockHttpServletResponse result = logout(spoofed, authentication("oidc"));

      assertEquals(RELOGIN_URL, result.getRedirectedUrl());
      verify(userService).setActiveLinkedAccounts("alice", "alice2");
      verify(userService, never()).setActiveLinkedAccounts(eq("victim"), anyString());
      verify(userService, never()).getUserByUsername("victim");
    }

    @Test
    @DisplayName("Logout without a switch redirects to the logout url and does not touch accounts")
    void testLogoutWithoutSwitch() throws Exception {
      MockHttpServletResponse result = logout(logoutRequest("POST", null), authentication("oidc"));

      assertEquals(LOGOUT_URL, result.getRedirectedUrl());
      verifyNoInteractions(userService, dhisOidcProviderRepository);
    }

    @ParameterizedTest(name = "{0} request with {1} authentication")
    @CsvSource({
      "GET, oidc",
      "POST, none",
      "POST, password",
      "POST, unauthenticated-oidc",
    })
    @DisplayName("Switch that is not an authenticated OIDC POST never reads or writes accounts")
    void testSwitchWithoutAuthenticatedOidcPostIsIgnored(String method, String kind)
        throws Exception {
      MockHttpServletResponse result =
          logout(logoutRequest(method, "alice2"), authentication(kind));

      assertEquals(LOGOUT_URL, result.getRedirectedUrl());
      verifyNoInteractions(userService, dhisOidcProviderRepository);
    }

    @ParameterizedTest(name = "target {0}")
    @CsvSource({"mallory", "no-openid", "disabled", "ghost"})
    @DisplayName("Switch to an unlinked, OIDC-less, disabled or unknown account never writes")
    void testSwitchToIneligibleAccountIsIgnored(String target) throws Exception {
      givenRegistration();
      givenAccounts();

      MockHttpServletResponse result =
          logout(logoutRequest("POST", target), authentication("oidc"));

      assertEquals(LOGOUT_URL, result.getRedirectedUrl());
      verify(userService, never()).setActiveLinkedAccounts(anyString(), anyString());
    }

    @Test
    @DisplayName("Switch when the OIDC claim does not match the acting user's openId never writes")
    void testSwitchWithMismatchingMappingClaimIsIgnored() throws Exception {
      givenRegistration();
      givenAccounts();

      MockHttpServletResponse result =
          logout(
              logoutRequest("POST", "alice2"), authentication("oidc", "someone-else@example.org"));

      assertEquals(LOGOUT_URL, result.getRedirectedUrl());
      verify(userService, never()).setActiveLinkedAccounts(anyString(), anyString());
    }

    @Test
    @DisplayName("Switch for an unknown OIDC registration never reads or writes accounts")
    void testSwitchWithUnknownRegistrationIsIgnored() throws Exception {
      MockHttpServletResponse result =
          logout(logoutRequest("POST", "alice2"), authentication("oidc"));

      assertEquals(LOGOUT_URL, result.getRedirectedUrl());
      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Mobile callback takes precedence and never switches account")
    void testMobileCallbackNeverSwitches() throws Exception {
      stubAllowlist("dhis2oauth://oauth");
      MockHttpServletRequest mobile = logoutRequest("POST", "alice2");
      mobile.setParameter("redirect_uri", "dhis2oauth://oauth");

      MockHttpServletResponse result = logout(mobile, authentication("oidc"));

      assertEquals("dhis2oauth://oauth", result.getRedirectedUrl());
      verifyNoInteractions(userService, dhisOidcProviderRepository);
    }

    @Test
    @DisplayName("A switch does not leak its redirect target into the next logout")
    void testRedirectStateIsNotShared() throws Exception {
      givenRegistration();
      givenAccounts();

      MockHttpServletResponse first =
          logout(logoutRequest("POST", "alice2"), authentication("oidc"));
      MockHttpServletResponse second = logout(logoutRequest("POST", null), authentication("oidc"));

      assertEquals(RELOGIN_URL, first.getRedirectedUrl());
      assertEquals(LOGOUT_URL, second.getRedirectedUrl());
      verify(userService).setActiveLinkedAccounts("alice", "alice2");
    }
  }
}
