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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_SYSTEM_SETTING;
import static org.hisp.dhis.user.CurrentUserUtil.getCurrentUserDetails;
import static org.hisp.dhis.util.JsonValueUtils.toJavaString;
import static org.hisp.dhis.webapi.utils.ContextUtils.noCacheNoStoreMustRevalidate;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.jsontree.Json;
import org.hisp.dhis.jsontree.JsonMap;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.setting.SystemSettingsTranslationService;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.intellij.lang.annotations.Language;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * @author Jan Bernitt
 */
@OpenApi.Document(
    entity = SystemSetting.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/systemSettings")
@AllArgsConstructor
public class SystemSettingsController {

  private final SystemSettingsService settingsService;
  private final SystemSettingsTranslationService settingsTranslationService;

  @PostMapping(value = "/{key}", params = "value")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingPlain(
      @PathVariable("key") String key, @RequestParam(value = "value") String value)
      throws NotFoundException, BadRequestException {
    settingsService.putAll(Map.of(key, value));
    return ok("System setting '" + key + "' set to value '" + value + "'.");
  }

  @PostMapping(value = "/{key}", consumes = TEXT_PLAIN_VALUE)
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingPlainBody(
      @PathVariable("key") String key, @RequestBody String value)
      throws NotFoundException, BadRequestException {
    return putSystemSettingPlain(key, value);
  }

  @PostMapping(value = "/{key}", consumes = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingJson(
      @PathVariable("key") String key, @Language("json") @RequestBody String value)
      throws ConflictException, NotFoundException, BadRequestException {
    return putSystemSettingPlain(key, toJavaString(JsonMixed.of(value)));
  }

  @PostMapping(consumes = ContextUtils.CONTENT_TYPE_JSON)
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingsJson(@RequestBody Map<String, String> settings)
      throws NotFoundException, BadRequestException {
    settingsService.putAll(settings);
    return ok("System settings imported");
  }

  @GetMapping(value = "/{key}", produces = TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getSystemSettingPlain(@PathVariable("key") String key)
      throws ForbiddenException, ConflictException, NotFoundException {
    checkKeyExists(key);
    if (SystemSettings.isConfidential(key) && !getCurrentUserDetails().isSuper())
      throw new ForbiddenException("Setting is marked as confidential");

    String value = "";
    // Note: This exception is added in for backwards compatibility
    if (SystemSettings.isTranslatable(key)) value = getSystemSettingTranslation(key);
    if (value.isEmpty())
      value = toJavaString(settingsService.getCurrentSettings().toJson(true, Set.of(key)).get(key));
    return ResponseEntity.ok().headers(noCacheNoStoreMustRevalidate()).body(value);
  }

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  @OpenApi.Response(SystemSettings.class)
  public ResponseEntity<JsonMap<JsonMixed>> getSystemSettingsJson(
      @RequestParam(required = false) Set<String> key) {
    SystemSettings settings = settingsService.getCurrentSettings();
    JsonMap<JsonMixed> res =
        key == null || key.isEmpty() ? settings.toJson(false) : settings.toJson(true, key);
    return ResponseEntity.ok().headers(noCacheNoStoreMustRevalidate()).body(res);
  }

  @GetMapping(value = "/{key}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonMap<JsonMixed>> getSystemSettingJson(
      @PathVariable("key") String key, @CurrentUser UserDetails currentUser)
      throws ForbiddenException, NotFoundException {
    checkKeyExists(key);
    if (SystemSettings.isConfidential(key) && !currentUser.isSuper())
      throw new ForbiddenException("Setting is marked as confidential");

    JsonMap<JsonMixed> value = Json.ofNull().asMap(JsonMixed.class);
    if (SystemSettings.isTranslatable(key)) {
      String translation = getSystemSettingTranslation(key);
      if (!translation.isEmpty()) {
        value = Json.object(obj -> obj.addString(key, translation)).asMap(JsonMixed.class);
      }
    }
    if (value.isNull()) value = settingsService.getCurrentSettings().toJson(true, Set.of(key));
    return ResponseEntity.ok().headers(noCacheNoStoreMustRevalidate()).body(value);
  }

  @DeleteMapping("/{key}")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseStatus(value = NO_CONTENT)
  public void removeSystemSetting(@PathVariable("key") String key) throws NotFoundException {
    checkKeyExists(key);
    settingsService.deleteAll(Set.of(key));
  }

  @DeleteMapping(params = "key")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseStatus(value = NO_CONTENT)
  public void removeSystemSetting(@RequestParam("key") Set<String> keys) {
    settingsService.deleteAll(keys == null ? Set.of() : keys);
  }

  /*

  Translations (should be replaced by using the datastore directly)

   */

  @GetMapping(value = "/{key}", params = "locale")
  public @ResponseBody ResponseEntity<String> getSystemSettingTranslation(
      @PathVariable("key") String key, @RequestParam("locale") String locale)
      throws ForbiddenException, ConflictException, NotFoundException {
    checkKeyExists(key);
    if (!SystemSettings.isTranslatable(key)) return getSystemSettingPlain(key);
    if (locale == null || locale.isEmpty()) locale = getUserUiLanguage();

    String value = settingsTranslationService.getSystemSettingTranslation(key, locale).orElse("");
    if (value.isEmpty())
      value =
          toJavaString(settingsService.getCurrentSettings().toJson(false, Set.of(key)).get(key));
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .headers(noCacheNoStoreMustRevalidate())
        .body(value);
  }

  @Nonnull
  private String getSystemSettingTranslation(String key) {
    return settingsTranslationService
        .getSystemSettingTranslation(key, getUserUiLanguage())
        .orElse("");
  }

  @PostMapping(
      value = "/{key}",
      params = {"locale", "value"})
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingTranslation(
      @PathVariable("key") String key,
      @RequestParam("locale") String locale,
      @RequestParam("value") String value)
      throws ForbiddenException, BadRequestException {
    if (value == null)
      throw new BadRequestException("Value must be specified as query param or as payload");
    settingsTranslationService.putSystemSettingTranslation(key, locale, value);
    return ok(
        "Translation for system setting '%s' and locale: '%s' set to: '%s'"
            .formatted(key, locale, value));
  }

  @PostMapping(value = "/{key}", params = "locale", consumes = TEXT_PLAIN_VALUE)
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseBody
  public WebMessage putSystemSettingTranslationBody(
      @PathVariable("key") String key,
      @RequestParam("locale") String locale,
      @RequestBody String value)
      throws ForbiddenException, BadRequestException {
    return putSystemSettingTranslation(key, locale, value);
  }

  @DeleteMapping(value = "/{key}", params = "locale")
  @RequiresAuthority(anyOf = F_SYSTEM_SETTING)
  @ResponseStatus(value = NO_CONTENT)
  public void removeSystemSettingTranslation(
      @PathVariable("key") String key, @RequestParam("locale") String locale)
      throws ForbiddenException, BadRequestException {
    settingsTranslationService.putSystemSettingTranslation(key, locale, StringUtils.EMPTY);
  }

  private static void checkKeyExists(String key) throws NotFoundException {
    if (!SystemSettings.keysWithDefaults().contains(key))
      throw new NotFoundException("Setting does not exist: " + key);
  }

  private static String getUserUiLanguage() {
    return UserSettings.getCurrentSettings().getUserUiLocale().getLanguage();
  }
}
