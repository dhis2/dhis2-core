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

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolmentException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;
import org.hisp.dhis.security.spring2fa.TwoFactorCapableAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.controller.security.LoginResponse.STATUS;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Tags({"login"})
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AuthenticationController {

  private final TwoFactorCapableAuthenticationProvider twoFactorAuthenticationProvider;
  private final SystemSettingManager settingManager;
  private final RequestCache requestCache;

  @PostMapping("/login")
  public LoginResponse login(
      HttpServletRequest servletRequest, @RequestBody LoginRequest loginRequest) {

    Authentication authenticationToken = createAuthenticationToken(servletRequest, loginRequest);

    return authenticate(servletRequest, authenticationToken);
  }

  private LoginResponse authenticate(HttpServletRequest servletRequest, Authentication auth) {
    try {
        Authentication authentication = getAuthProvider().authenticate(auth);

      return createSuccessResponse(servletRequest, authentication);

    } catch (TwoFactorAuthenticationException e) {
      return LoginResponse.builder().loginStatus(STATUS.INCORRECT_TWO_FACTOR_CODE).build();
    } catch (TwoFactorAuthenticationEnrolmentException e) {
      return LoginResponse.builder().loginStatus(STATUS.REQUIRES_TWO_FACTOR_ENROLMENT).build();
    } catch (CredentialsExpiredException e) {
      return LoginResponse.builder().loginStatus(STATUS.PASSWORD_EXPIRED).build();
    }
  }

  private AuthenticationProvider getAuthProvider() {
    return twoFactorAuthenticationProvider;
  }

  private LoginResponse createSuccessResponse(
      HttpServletRequest servletRequest, Authentication authenticate) {

    String redirectUrl = "/" + settingManager.getStringSetting(SettingKey.START_MODULE);
    SavedRequest request = requestCache.getRequest(servletRequest, null);
    if (request != null) {
      DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) request;
      redirectUrl = defaultSavedRequest.getRequestURI();
    }

    return LoginResponse.builder().loginStatus(STATUS.SUCCESS).redirectUrl(redirectUrl).build();
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
}
