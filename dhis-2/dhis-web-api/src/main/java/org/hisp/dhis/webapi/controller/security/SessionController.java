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
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
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
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class SessionController {

  private final SessionRegistry sessionRegistry;
  private final UserService userService;

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = ALL)
  public Map<String, String> listAllSessions() {
    return listAllUserSessions().stream()
        .collect(
            Collectors.toMap(
                s -> HashUtils.hashSHA1(s.getSessionId().getBytes()),
                s ->
                    ((UserDetails) s.getPrincipal()).getUsername()
                        + (s.isExpired() ? ", (inactive)" : ", (active)")));
  }

  private List<SessionInformation> listAllUserSessions() {
    List<SessionInformation> allSessions = new ArrayList<>();
    List<User> allUsers = userService.getAllUsers();
    for (User user : allUsers) {
      allSessions.addAll(sessionRegistry.getAllSessions(userService.createUserDetails(user), true));
    }
    return allSessions;
  }

  @DeleteMapping(value = "/{username}")
  @RequiresAuthority(anyOf = ALL)
  public void invalidateSessions(@PathVariable("username") String username) {
    User user = userService.getUserByUsername(username);
    UserDetails userDetails = userService.createUserDetails(user);
    if (userDetails != null) {
      List<SessionInformation> allSessions = sessionRegistry.getAllSessions(userDetails, false);
      allSessions.forEach(SessionInformation::expireNow);
    }
  }

  @DeleteMapping
  @RequiresAuthority(anyOf = ALL)
  public void invalidateAllSessions() {
    List<User> allUsers = userService.getAllUsers();
    for (User user : allUsers) {
      UserDetails userDetails = userService.createUserDetails(user);
      if (userDetails != null) {
        List<SessionInformation> allSessions = sessionRegistry.getAllSessions(userDetails, false);
        allSessions.forEach(SessionInformation::expireNow);
      }
    }
  }
}
