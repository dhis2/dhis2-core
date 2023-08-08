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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.unauthorized;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags({"user", "management"})
@RestController
@RequestMapping("/userSettings")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class UserSettingController {
  @Autowired private UserSettingService userSettingService;

  @Autowired private UserService userService;

  @Autowired private CurrentUserService currentUserService;

  private static final Set<UserSettingKey> USER_SETTING_KEYS =
      Sets.newHashSet(UserSettingKey.values()).stream().collect(Collectors.toSet());

  // -------------------------------------------------------------------------
  // Resources
  // -------------------------------------------------------------------------

  @GetMapping
  public Map<String, Serializable> getAllUserSettings(
      @RequestParam(required = false, defaultValue = "true") boolean useFallback,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId,
      @RequestParam(value = "key", required = false) Set<String> keys,
      HttpServletResponse response)
      throws WebMessageException {
    User user = getUser(userId, username);

    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());

    if (keys == null) {
      return userSettingService.getUserSettingsWithFallbackByUserAsMap(
          user, USER_SETTING_KEYS, useFallback);
    }

    Map<String, Serializable> result = new HashMap<>();

    for (String key : keys) {
      UserSettingKey userSettingKey = getUserSettingKey(key);
      result.put(userSettingKey.getName(), userSettingService.getUserSetting(userSettingKey));
    }

    return result;
  }

  @OpenApi.Response(Serializable.class)
  @GetMapping(value = "/{key}")
  public void getUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(required = false, defaultValue = "true") boolean useFallback,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId,
      HttpServletResponse response)
      throws WebMessageException, IOException {
    UserSettingKey userSettingKey = getUserSettingKey(key);
    User user = getUser(userId, username);

    Serializable value =
        userSettingService
            .getUserSettingsWithFallbackByUserAsMap(
                user, Sets.newHashSet(userSettingKey), useFallback)
            .get(key);

    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
    response.setHeader(HttpHeaders.CONTENT_TYPE, ContextUtils.CONTENT_TYPE_TEXT);
    response.getWriter().print(value);
  }

  @PostMapping(value = "/{key}")
  public WebMessage setUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId,
      @RequestParam(required = false) String value,
      @RequestBody(required = false) String valuePayload)
      throws WebMessageException {
    UserSettingKey userSettingKey = getUserSettingKey(key);
    User user = getUser(userId, username);

    String newValue = ObjectUtils.firstNonNull(value, valuePayload);

    if (StringUtils.isEmpty(newValue)) {
      throw new WebMessageException(conflict("You need to specify a new value"));
    }

    userSettingService.saveUserSetting(
        userSettingKey, UserSettingKey.getAsRealClass(key, newValue), user);

    return ok("User setting saved");
  }

  @DeleteMapping(value = "/{key}")
  public void deleteUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId)
      throws WebMessageException {
    UserSettingKey userSettingKey = getUserSettingKey(key);
    User user = getUser(userId, username);

    userSettingService.deleteUserSetting(userSettingKey, user);
  }

  /**
   * Attempts to resolve the UserSettingKey based on the name (key) supplied
   *
   * @param key the name of a UserSettingKey
   * @return the UserSettingKey
   * @throws WebMessageException throws an exception if no UserSettingKey was found
   */
  private UserSettingKey getUserSettingKey(String key) throws WebMessageException {
    Optional<UserSettingKey> userSettingKey = UserSettingKey.getByName(key);

    if (!userSettingKey.isPresent()) {
      throw new WebMessageException(notFound("No user setting found with key: " + key));
    }

    return userSettingKey.get();
  }

  /**
   * Tries to find a user based on the uid or username. If none is supplied, currentUser will be
   * returned. If uid or username is found, it will also make sure the current user has access to
   * the user.
   *
   * @param uid the user uid
   * @param username the user username
   * @return the user found with uid or username, or current user if no uid or username was
   *     specified
   * @throws WebMessageException throws an exception if user was not found, or current user don't
   *     have access
   */
  private User getUser(String uid, String username) throws WebMessageException {
    User currentUser = currentUserService.getCurrentUser();
    User user;

    if (uid == null && username == null) {
      return currentUser;
    }

    if (uid != null) {
      user = userService.getUser(uid);
    } else {
      user = userService.getUserByUsername(username);
    }

    if (user == null) {
      throw new WebMessageException(
          conflict("Could not find user '" + ObjectUtils.firstNonNull(uid, username) + "'"));
    } else {
      Set<String> userGroups =
          user.getGroups().stream().map(UserGroup::getUid).collect(Collectors.toSet());

      if (!userService.canAddOrUpdateUser(userGroups) && !currentUser.canModifyUser(user)) {
        throw new WebMessageException(
            unauthorized("You are not authorized to access user: " + user.getUsername()));
      }
    }

    return user;
  }
}
