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

import static java.util.Collections.singletonMap;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 * @author David Katuscak <katuscak.d@gmail.com>
 */
@OpenApi.Tags("system")
@Controller
@RequestMapping("/systemSettings")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@AllArgsConstructor
public class SystemSettingController {
  private final SystemSettingManager systemSettingManager;

  private final UserSettingService userSettingService;

  // -------------------------------------------------------------------------
  // Create
  // -------------------------------------------------------------------------

  @PostMapping(
      value = "/{key}",
      consumes = {
        ContextUtils.CONTENT_TYPE_JSON,
        ContextUtils.CONTENT_TYPE_TEXT,
        ContextUtils.CONTENT_TYPE_HTML
      })
  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @ResponseBody
  public WebMessage setSystemSettingOrTranslation(
      @PathVariable(value = "key") String key,
      @RequestParam(value = "locale", required = false) String locale,
      @RequestParam(value = "value", required = false) String value,
      @RequestBody(required = false) String valuePayload,
      HttpServletResponse response,
      HttpServletRequest request)
      throws WebMessageException {
    validateParameters(key, value, valuePayload);

    Optional<SettingKey> setting = SettingKey.getByName(key);

    if (!setting.isPresent()) {
      return conflict("Key is not supported: " + key);
    }

    value = ObjectUtils.firstNonNull(value, valuePayload);

    if (StringUtils.isEmpty(locale)) {
      return saveSystemSetting(key, value, setting.get());
    }
    return saveSystemSettingTranslation(key, locale, value, setting.get());
  }

  private WebMessage saveSystemSetting(String key, String value, SettingKey setting) {
    Serializable valueObject = SettingKey.getAsRealClass(key, value);

    systemSettingManager.saveSystemSetting(setting, valueObject);

    return ok("System setting '" + key + "' set to value '" + valueObject + "'.");
  }

  private WebMessage saveSystemSettingTranslation(
      String key, String locale, String value, SettingKey setting) {
    try {
      systemSettingManager.saveSystemSettingTranslation(setting, locale, value);
    } catch (IllegalStateException e) {
      return conflict(e.getMessage());
    }

    return ok(
        "Translation for system setting '"
            + key
            + "' and locale: '"
            + locale
            + "' set to: '"
            + value
            + "'");
  }

  private void validateParameters(String key, String value, String valuePayload)
      throws WebMessageException {
    if (key == null) {
      throw new WebMessageException(conflict("Key must be specified"));
    }

    if (value == null && valuePayload == null) {
      throw new WebMessageException(
          conflict("Value must be specified as query param or as payload"));
    }
  }

  @PostMapping(consumes = ContextUtils.CONTENT_TYPE_JSON)
  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @ResponseBody
  public WebMessage setSystemSettingV29(@RequestBody Map<String, Object> settings) {
    List<String> invalidKeys =
        settings.keySet().stream()
            .filter((key) -> !SettingKey.getByName(key).isPresent())
            .collect(Collectors.toList());

    if (!invalidKeys.isEmpty()) {
      return conflict("Key(s) is not supported: " + StringUtils.join(invalidKeys, ", "));
    }

    for (Entry<String, Object> entry : settings.entrySet()) {
      String key = entry.getKey();
      Serializable valueObject = SettingKey.getAsRealClass(key, entry.getValue().toString());
      systemSettingManager.saveSystemSetting(SettingKey.getByName(key).get(), valueObject);
    }

    return ok("System settings imported");
  }

  // -------------------------------------------------------------------------
  // Read
  // -------------------------------------------------------------------------

  @GetMapping(value = "/{key}", produces = ContextUtils.CONTENT_TYPE_TEXT)
  public @ResponseBody Serializable getSystemSettingOrTranslationAsPlainText(
      @PathVariable("key") String key,
      @RequestParam(value = "locale", required = false) String locale,
      HttpServletResponse response,
      @CurrentUser User currentUser) {
    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, CacheControl.noCache().cachePrivate().getHeaderValue());

    return String.valueOf(getSystemSettingOrTranslation(key, locale, currentUser));
  }

  @GetMapping(
      value = "/{key}",
      produces = {ContextUtils.CONTENT_TYPE_JSON, ContextUtils.CONTENT_TYPE_HTML})
  public @ResponseBody ResponseEntity<Map<String, Serializable>>
      getSystemSettingOrTranslationAsJson(
          @PathVariable("key") String key,
          @RequestParam(value = "locale", required = false) String locale,
          @CurrentUser User currentUser) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(singletonMap(key, getSystemSettingOrTranslation(key, locale, currentUser)));
  }

  private Serializable getSystemSettingOrTranslation(String key, String locale, User currentUser) {
    Optional<SettingKey> settingKey = SettingKey.getByName(key);
    if (!systemSettingManager.isConfidential(key) && settingKey.isPresent()) {
      Optional<String> localeToFetch = getLocaleToFetch(locale, key, currentUser);

      if (localeToFetch.isPresent()) {
        Optional<String> translation =
            systemSettingManager.getSystemSettingTranslation(settingKey.get(), localeToFetch.get());

        if (translation.isPresent()) {
          return translation.get();
        }
      }

      Serializable systemSetting =
          systemSettingManager.getSystemSetting(settingKey.get(), settingKey.get().getClazz());

      if (systemSetting == null) {
        return StringUtils.EMPTY;
      }

      return systemSetting;
    }

    return StringUtils.EMPTY;
  }

  private Optional<String> getLocaleToFetch(String locale, String key, User currentUser) {
    if (systemSettingManager.isTranslatable(key)) {
      if (StringUtils.isNotEmpty(locale)) {
        return Optional.of(locale);
      } else if (currentUser != null) {
        Locale userLocale =
            (Locale) userSettingService.getUserSetting(UserSettingKey.UI_LOCALE, currentUser);

        if (userLocale != null) {
          return Optional.of(userLocale.getLanguage());
        }
      }
    }

    return Optional.empty();
  }

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Serializable>> getSystemSettingsJson(
      @RequestParam(value = "key", required = false) Set<String> keys) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noCache().cachePrivate())
        .body(systemSettingManager.getSystemSettings(getSettingKeysToFetch(keys)));
  }

  private Set<SettingKey> getSettingKeysToFetch(Set<String> keys) {
    Set<SettingKey> settingKeys;

    if (keys != null) {
      keys.removeIf(systemSettingManager::isConfidential);
      settingKeys =
          keys.stream()
              .map(SettingKey::getByName)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toSet());
    } else {
      settingKeys = new HashSet<>(Arrays.asList(SettingKey.values()));
      settingKeys.removeIf(key -> systemSettingManager.isConfidential(key.getName()));
    }

    return settingKeys;
  }

  // -------------------------------------------------------------------------
  // Remove
  // -------------------------------------------------------------------------

  @DeleteMapping("/{key}")
  @PreAuthorize("hasRole('ALL') or hasRole('F_SYSTEM_SETTING')")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void removeSystemSetting(
      @PathVariable("key") String key,
      @RequestParam(value = "locale", required = false) String locale)
      throws WebMessageException {
    Optional<SettingKey> setting = SettingKey.getByName(key);

    if (!setting.isPresent()) {
      throw new WebMessageException(conflict("Key is not supported: " + key));
    }

    if (StringUtils.isNotEmpty(locale)) {
      systemSettingManager.saveSystemSettingTranslation(setting.get(), locale, StringUtils.EMPTY);
    } else {
      systemSettingManager.deleteSystemSetting(setting.get());
    }
  }
}
