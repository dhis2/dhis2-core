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

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Accept;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.http.HttpClientAdapter.ContentType;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link SystemSettingsController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class SystemSettingsControllerTest extends H2ControllerIntegrationTestBase {

  @AfterEach
  void resetLocale() {
    POST("/systemSettings/keyUiLocale?value=en");
  }

  @Test
  void testSetSystemSettingOrTranslation_NoValue() {
    assertStatus(HttpStatus.BAD_REQUEST, POST("/systemSettings/xyz"));
  }

  @Test
  void testSetSystemSettingOrTranslation_Setting() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "System setting 'keyUiLocale' set to value 'en'.",
        POST(
                "/systemSettings/keyUiLocale?",
                Body("en"),
                ContentType(ContextUtils.CONTENT_TYPE_TEXT))
            .content(HttpStatus.OK));
  }

  @Test
  void testSetSystemSettingOrTranslation_Translation() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/applicationTitle?value=Hello"));
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Translation for system setting 'applicationTitle' and locale: 'de' set to: 'Ahoi'",
        POST("/systemSettings/applicationTitle?locale=de&value=Ahoi").content(HttpStatus.OK));
  }

  @Test
  void testSetSystemSetting() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "System settings imported",
        POST("/systemSettings", "{'keyUiLocale':'de'}").content(HttpStatus.OK));
    assertEquals(
        "de", GET("/systemSettings/keyUiLocale", Accept("text/plain")).content("text/plain"));
  }

  @Test
  void testSetSystemSetting_Empty() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "System settings imported",
        POST("/systemSettings", "{}").content(HttpStatus.OK));
  }

  @Test
  void testGetSystemSettingJson() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=de"));
    JsonObject settings = GET("/systemSettings/keyUiLocale").content(HttpStatus.OK);
    assertTrue(settings.isObject());
    assertEquals("de", settings.getString("keyUiLocale").string());
  }

  @Test
  void testGetSystemSettingsJson() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=de"));
    JsonObject settings = GET("/systemSettings?key=keyUiLocale").content(HttpStatus.OK);
    assertTrue(settings.isObject());
    assertEquals("de", settings.getString("keyUiLocale").string());
  }

  @Test
  void testGetSystemSettingsJson_AllKeys() {
    assertStatus(HttpStatus.OK, POST("/systemSettings/keyUiLocale?value=de"));
    JsonObject setting = GET("/systemSettings").content(HttpStatus.OK);
    assertTrue(setting.isObject());
    SystemSettings.keysWithDefaults().stream()
        .filter(key -> !SystemSettings.isConfidential(key))
        .forEach(key -> assertTrue(setting.get(key).exists(), "expected " + key));
    SystemSettings.keysWithDefaults().stream()
        .filter(SystemSettings::isConfidential)
        .forEach(
            key ->
                assertFalse(
                    setting.get(key).exists(), key + " is confidential and should not be exposed"));
  }

  @Test
  void testGetSystemSettingsJson_ByKey() {
    JsonObject settings = GET("/systemSettings?key=keyEmailUsername").content(HttpStatus.OK);
    assertEquals(1, settings.size());
    assertEquals(List.of("keyEmailUsername"), settings.names());

    settings =
        GET("/systemSettings?key=keyEmailUsername&key=keyEmailSender").content(HttpStatus.OK);
    assertEquals(2, settings.size());
    assertEquals(Set.of("keyEmailUsername", "keyEmailSender"), Set.copyOf(settings.names()));
  }

  @Test
  void testGetSystemSettingPlain() {
    assertEquals(
        "yyyy-MM-dd",
        GET("/systemSettings/keyDateFormat", Accept("text/plain")).content("text/plain"));
  }

  @Test
  void testGetSystemSettings_Forbidden() {
    switchToNewUser("someone");
    assertStatus(HttpStatus.FORBIDDEN, GET("/systemSettings/keyEmailPassword"));
    assertStatus(
        HttpStatus.FORBIDDEN, GET("/systemSettings/keyEmailPassword", Accept("application/json")));
    switchToAdminUser();
    assertStatus(HttpStatus.OK, GET("/systemSettings/keyEmailPassword"));
  }

  @Test
  void testGetSystemSettingAsJsonQueryParam_MultipleKeysDoExist() {
    JsonObject settings =
        GET("/systemSettings?key=keyDateFormat,jobsRescheduleAfterMinutes").content(HttpStatus.OK);
    assertEquals("yyyy-MM-dd", settings.getString("keyDateFormat").string());
    assertEquals(10, settings.getNumber("jobsRescheduleAfterMinutes").intValue());
  }
}
