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
package org.hisp.dhis.webapi.controller;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.util.JsonValueUtils.toJavaString;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = User.class,
    group = OpenApi.Document.GROUP_CONFIG,
    classifiers = {"team:platform", "purpose:metadata"})
@RestController
@RequestMapping("/api/userSettings")
@RequiredArgsConstructor
public class UserSettingsController {

  private final UserSettingsService userSettingsService;
  private final UserService userService;

  @GetMapping
  @OpenApi.Response(UserSettings.class)
  public @ResponseBody ResponseEntity<JsonMap<JsonMixed>> getAllUserSettings(
      @RequestParam(required = false) Set<String> key,
      @RequestParam(required = false, defaultValue = "true") boolean useFallback,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId)
      throws ForbiddenException, NotFoundException {

    UserSettings settings = getUserSettings(userId, username, useFallback);
    JsonMap<JsonMixed> res =
        key == null || key.isEmpty() ? settings.toJson(false) : settings.toJson(false, key);
    return ResponseEntity.ok().cacheControl(CacheControl.noCache().cachePrivate()).body(res);
  }

  @GetMapping(value = "/{key}")
  public @ResponseBody String getUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(required = false, defaultValue = "true") boolean useFallback,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId,
      HttpServletResponse response)
      throws ForbiddenException, ConflictException, NotFoundException {
    checkKeyExists(key);

    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());
    response.setHeader(HttpHeaders.CONTENT_TYPE, ContextUtils.CONTENT_TYPE_TEXT);

    UserSettings settings = getUserSettings(userId, username, useFallback);
    return toJavaString(settings.toJson(false, Set.of(key)).get(key));
  }

  @PostMapping(value = "/{key}")
  public WebMessage putUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId,
      @RequestParam(required = false) String value,
      @RequestBody(required = false) String valuePayload)
      throws ForbiddenException, ConflictException, NotFoundException, BadRequestException {
    checkKeyExists(key);

    String newValue = firstNonNull(value, valuePayload);

    if (isEmpty(newValue)) throw new ConflictException("You need to specify a new value");

    if (username == null) username = getUsername(userId);
    userSettingsService.put(key, newValue, username);

    return ok("User setting saved");
  }

  @DeleteMapping(value = "/{key}")
  public void deleteUserSettingByKey(
      @PathVariable(value = "key") String key,
      @RequestParam(value = "user", required = false) String username,
      @OpenApi.Param({UID.class, User.class}) @RequestParam(value = "userId", required = false)
          String userId)
      throws ForbiddenException, NotFoundException, ConflictException, BadRequestException {
    checkKeyExists(key);
    if (username == null) username = getUsername(userId);
    userSettingsService.put(key, null, username);
  }

  /**
   * Tries to find a user based on the uid or username. If none is supplied, currentUser will be
   * returned. If uid or username is found, it will also make sure the current user has access to
   * the user.
   *
   * @param uid the user uid
   * @return the username
   */
  private String getUsername(String uid) throws ForbiddenException, NotFoundException {
    if (uid == null) return CurrentUserUtil.getCurrentUsername();

    User user = userService.getUser(uid);

    if (user == null) throw new NotFoundException(User.class, uid);

    Set<String> userGroups =
        user.getGroups().stream().map(UserGroup::getUid).collect(Collectors.toSet());

    UserDetails currentUser = getCurrentUserDetails();
    if (!userService.canAddOrUpdateUser(userGroups) && !currentUser.canModifyUser(user))
      throw new ForbiddenException("You are not authorized to access user: " + user.getUsername());

    return user.getUsername();
  }

  private UserSettings getUserSettings(String userId, String username, boolean useFallback)
      throws ForbiddenException, NotFoundException {
    if (username == null) username = getUsername(userId);
    return userSettingsService.getUserSettings(username, useFallback);
  }

  private static void checkKeyExists(String key) throws NotFoundException {
    if (!UserSettings.keysWithDefaults().contains(key))
      throw new NotFoundException("Setting does not exist: " + key);
  }
}
