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

import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolmentException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.security.LoginResponse.STATUS;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The AuthenticationController class is responsible for handling login authentication. It provides
 * endpoints for user login and manages session authentication strategies.
 *
 * <p>This class is targeted for the new LoginApp and JSON input/output.
 *
 * <p>This class is inspired by, and partly copied from the UsernamePasswordAuthenticationFilter and
 * AbstractAuthenticationProcessingFilter in Spring.
 *
 * <p>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(domain = User.class)
@RestController
@RequestMapping("/api/auth")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@Order(2103)
public class AuthenticationController {

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private DhisConfigurationProvider dhisConfig;
  @Autowired private SystemSettingManager settingManager;
  @Autowired private RequestCache requestCache;
  @Autowired private SessionRegistry sessionRegistry;
  @Autowired private UserService userService;

  @Autowired protected ApplicationEventPublisher eventPublisher;

  private SessionAuthenticationStrategy sessionStrategy = new NullAuthenticatedSessionStrategy();

  private final SecurityContextHolderStrategy securityContextHolderStrategy =
      SecurityContextHolder.getContextHolderStrategy();

  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  @PostConstruct
  public void init() {
    if (sessionRegistry != null) {

      int maxSessions =
          Integer.parseInt(dhisConfig.getProperty((ConfigurationKey.MAX_SESSIONS_PER_USER)));
      ConcurrentSessionControlAuthenticationStrategy concurrentStrategy =
          new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry);
      concurrentStrategy.setMaximumSessions(maxSessions);

      sessionStrategy =
          new CompositeSessionAuthenticationStrategy(
              List.of(
                  concurrentStrategy,
                  new SessionFixationProtectionStrategy(),
                  new RegisterSessionAuthenticationStrategy(sessionRegistry)));
    }
  }

  @PostMapping("/login")
  public LoginResponse login(
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody LoginRequest loginRequest) {

    try {
      validateRequest(loginRequest);

      Authentication authenticationResult = doAuthentication(request, loginRequest);

      this.sessionStrategy.onAuthentication(authenticationResult, request, response);
      saveContext(request, response, authenticationResult);

      String redirectUrl = getRedirectUrl(request, response);

      if (this.eventPublisher != null) {
        this.eventPublisher.publishEvent(
            new InteractiveAuthenticationSuccessEvent(authenticationResult, this.getClass()));
      }

      return LoginResponse.builder().loginStatus(STATUS.SUCCESS).redirectUrl(redirectUrl).build();

    } catch (TwoFactorAuthenticationException e) {
      return LoginResponse.builder().loginStatus(STATUS.INCORRECT_TWO_FACTOR_CODE).build();
    } catch (TwoFactorAuthenticationEnrolmentException e) {
      return LoginResponse.builder().loginStatus(STATUS.REQUIRES_TWO_FACTOR_ENROLMENT).build();

    } catch (CredentialsExpiredException e) {
      return LoginResponse.builder().loginStatus(STATUS.PASSWORD_EXPIRED).build();
    } catch (LockedException e) {
      return LoginResponse.builder().loginStatus(STATUS.ACCOUNT_LOCKED).build();
    } catch (DisabledException e) {
      return LoginResponse.builder().loginStatus(STATUS.ACCOUNT_DISABLED).build();
    } catch (AccountExpiredException e) {
      return LoginResponse.builder().loginStatus(STATUS.ACCOUNT_EXPIRED).build();
    }
  }

  private void validateRequest(LoginRequest loginRequest) {
    User user = userService.getUserByUsername(loginRequest.getUsername());
    if (user == null) {
      throw new BadCredentialsException("Bad credentials");
    }
    boolean isOIDCUser =
        user.isExternalAuth() && (user.getOpenId() != null && !user.getOpenId().isEmpty());
    if (isOIDCUser) {
      throw new BadCredentialsException("Bad credentials");
    }
  }

  private Authentication doAuthentication(HttpServletRequest request, LoginRequest loginRequest) {
    Authentication authenticationToken = createAuthenticationToken(request, loginRequest);
    return authenticationManager.authenticate(authenticationToken);
  }

  private static Authentication createAuthenticationToken(
      HttpServletRequest servletRequest, LoginRequest loginRequest) {

    String username = loginRequest.getUsername();
    String password = loginRequest.getPassword();
    String twoFactorCode = loginRequest.getTwoFactorCode();

    TwoFactorUsernamePasswordAuthenticationToken auth =
        new TwoFactorUsernamePasswordAuthenticationToken(username, password, twoFactorCode);
    auth.setDetails(new TwoFactorWebAuthenticationDetails(servletRequest, twoFactorCode));

    return auth;
  }

  private void saveContext(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);

    this.securityContextHolderStrategy.setContext(context);
    this.securityContextRepository.saveContext(context, request, response);
  }

  private String getRedirectUrl(HttpServletRequest request, HttpServletResponse response) {
    String redirectUrl =
        request.getContextPath() + "/" + settingManager.getStringSetting(SettingKey.START_MODULE);

    if (!redirectUrl.endsWith("/")) {
      redirectUrl += "/";
    }

    SavedRequest savedRequest = requestCache.getRequest(request, null);
    if (savedRequest != null) {
      DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) savedRequest;
      if (!filterSavedRequest(defaultSavedRequest)) {
        if (defaultSavedRequest.getQueryString() != null) {
          redirectUrl =
              defaultSavedRequest.getRequestURI() + "?" + defaultSavedRequest.getQueryString();
        } else {
          redirectUrl = defaultSavedRequest.getRequestURI();
        }
      }
      this.requestCache.removeRequest(request, response);
    }
    return redirectUrl;
  }

  private boolean filterSavedRequest(DefaultSavedRequest savedRequest) {
    String requestURI = savedRequest.getRequestURI();
    return !requestURI.endsWith(".html") && !requestURI.endsWith("/") && requestURI.contains(".");
  }
}
