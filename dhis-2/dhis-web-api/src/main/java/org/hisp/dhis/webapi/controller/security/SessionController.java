/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.security.Authorities.ALL;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.security.session.SessionCreationTimeProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final UserService userService;
  private final ObjectProvider<SessionCreationTimeProvider> sessionCreationTimeProvider;

  /**
   * Structured view of a single HTTP session for the sessions API.
   *
   * @param id SHA-1 hash of the raw session id
   * @param username session owner username
   * @param created session creation time when available, otherwise null
   * @param lastRequest last request time from the session registry
   * @param expired whether the session has been expired
   */
  public record UserSessionInfo(
      @JsonProperty String id,
      @JsonProperty String username,
      @JsonProperty @CheckForNull Date created,
      @JsonProperty Date lastRequest,
      @JsonProperty boolean expired) {}

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public List<UserSessionInfo> listAllSessions() {
    SessionCreationTimeProvider creationTimes = sessionCreationTimeProvider.getIfAvailable();
    return userService.listAllSessions().stream()
        .map(session -> toUserSessionInfo(session, creationTimes))
        .toList();
  }

  @DeleteMapping(value = "/{username}")
  @RequiresAuthority(anyOf = ALL)
  public void invalidateSessions(@PathVariable("username") String username) {
    userService.invalidateUserSessions(username);
  }

  @DeleteMapping
  @RequiresAuthority(anyOf = ALL)
  public void invalidateAllSessions() {
    for (SessionInformation session : userService.listAllSessions()) {
      if (!session.isExpired()) {
        session.expireNow();
      }
    }
  }

  private static UserSessionInfo toUserSessionInfo(
      SessionInformation session, @CheckForNull SessionCreationTimeProvider creationTimes) {
    Object principal = session.getPrincipal();
    String username =
        principal instanceof UserDetails userDetails
            ? userDetails.getUsername()
            : String.valueOf(principal);
    Date created =
        creationTimes != null ? creationTimes.getCreationTime(session.getSessionId()) : null;
    return new UserSessionInfo(
        HashUtils.hashSHA1(session.getSessionId().getBytes()),
        username,
        created,
        session.getLastRequest(),
        session.isExpired());
  }
}
