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
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebLocale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link LocaleController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class LocaleControllerTest extends H2ControllerIntegrationTestBase {

  @AfterEach
  void cleanUp() {
    // make sure we reset the UI locale to default in case a test changes it
    DELETE("/systemSettings/keyUiLocale");
  }

  @Test
  void testAddLocale() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Locale created successfully",
        POST("/locales/dbLocales?language=en&country=GB").content(HttpStatus.CREATED));
  }

  @Test
  void testAddLocale_InvalidCountry() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Invalid country or language code.",
        POST("/locales/dbLocales?language=en&country=").content(HttpStatus.CONFLICT));
  }

  @Test
  void testAddLocale_InvalidLanguage() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Invalid country or language code.",
        POST("/locales/dbLocales?language=&country=GB").content(HttpStatus.CONFLICT));
  }

  @Test
  void testAddLocaleWithScript() {
    assertWebMessage(
        "Created",
        201,
        "OK",
        "Locale created successfully",
        POST("/locales/dbLocales?language=uz&country=UZ&script=Cyrl").content(HttpStatus.CREATED));
    JsonArray response = GET("/locales/db").content();
    assertEquals(1, response.size());
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("uz_UZ_Cyrl", firstElement.getLocale());
    assertEquals("o‘zbek (Oʻzbekiston, Cyrl)", firstElement.getName());
    assertEquals("Uzbek (Uzbekistan, Cyrl)", firstElement.getDisplayName());
  }

  @Test
  void testAddLocale_AlreadyExists() {
    assertStatus(HttpStatus.CREATED, POST("/locales/dbLocales?language=en&country=GB"));
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Locale code existed.",
        POST("/locales/dbLocales?language=en&country=GB").content(HttpStatus.CONFLICT));
  }

  @Test
  void testGetUiLocalesInUserLanguage() {
    String userEnglishLocale =
        GET("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID)
            .content("text/plain; charset=UTF-8");
    assertEquals("en", userEnglishLocale);

    JsonArray response = GET("/locales/ui").content();
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("ar", firstElement.getLocale());
    assertEquals("العربية", firstElement.getName());
    assertEquals("Arabic", firstElement.getDisplayName());
  }

  @Test
  @DisplayName("Indonesian locales are returned with the expected locale codes")
  void testGetUiIndonesianLocalesInUserLanguage() {
    // given
    String userEnglishLocale =
        GET("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID)
            .content("text/plain; charset=UTF-8");
    assertEquals("en", userEnglishLocale);

    // when
    JsonArray response = GET("/locales/ui").content();

    // then
    List<String> localeCodes =
        response.stream()
            .map(o -> o.as(JsonWebLocale.class))
            .map(JsonWebLocale::getLocale)
            .toList();

    assertTrue(localeCodes.containsAll(List.of("id", "id_ID")));
  }

  @Test
  void testGetUiLocaleAfterUserLanguageChange() {
    POST("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=fr");
    JsonArray response = GET("/locales/ui").content();
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("en", firstElement.getLocale());
    assertEquals("English", firstElement.getName());
    assertEquals("anglais", firstElement.getDisplayName());
  }

  @Test
  void testGetUiLocalesInServerLanguageWhenUserLanguageNotSet() {
    POST("/systemSettings/keyUiLocale/?value=es");
    DELETE("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID);
    JsonArray response = GET("/locales/ui").content();
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("bn", firstElement.getLocale());
    assertEquals("বাংলা", firstElement.getName());
    assertEquals("bengalí", firstElement.getDisplayName());
  }

  @Test
  void testGetUiLocalesInEnglishWhenUserAndServerLanguageNotSet() {
    DELETE("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID);
    DELETE("/systemSettings/keyUiLocale");
    JsonArray response = GET("/locales/ui").content();
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("ar", firstElement.getLocale());
    assertEquals("العربية", firstElement.getName());
    assertEquals("Arabic", firstElement.getDisplayName());
  }

  @Test
  void testGetDbLocales() {
    POST("/locales/dbLocales?country=IE&language=en");
    JsonArray response = GET("/locales/db").content();
    assertEquals(1, response.size());
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("en_IE", firstElement.getLocale());
    assertEquals("English (Ireland)", firstElement.getName());
    assertEquals("English (Ireland)", firstElement.getDisplayName());
  }

  @Test
  void testGetDbLocalesAfterUserLanguageChange() {
    POST("/locales/dbLocales?country=IE&language=en");
    JsonArray response = GET("/locales/db").content();
    assertEquals(1, response.size());
    JsonWebLocale firstElement = response.getObject(0).as(JsonWebLocale.class);
    assertEquals("en_IE", firstElement.getLocale());
    assertEquals("English (Ireland)", firstElement.getName());
    assertEquals("English (Ireland)", firstElement.getDisplayName());

    POST("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=fr");
    JsonArray response2 = GET("/locales/db").content();
    JsonWebLocale dbLocaleElement = response2.getObject(0).as(JsonWebLocale.class);
    assertEquals("en_IE", dbLocaleElement.getLocale());
    assertEquals("English (Ireland)", dbLocaleElement.getName());
    assertEquals("anglais (Irlande)", dbLocaleElement.getDisplayName());
  }
}
