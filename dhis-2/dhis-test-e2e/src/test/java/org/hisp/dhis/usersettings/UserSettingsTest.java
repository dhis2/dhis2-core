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
package org.hisp.dhis.usersettings;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class UserSettingsTest extends ApiTest {
  private static final String UI_LOCALE_KEY = "keyUiLocale";
  private static final String DB_LOCALE_KEY = "keyDbLocale";

  private RestApiActions userSettingsActions;

  @BeforeEach
  public void setUp() {
    userSettingsActions = new RestApiActions("/userSettings");

    LoginActions loginActions = new LoginActions();
    loginActions.loginAsSuperUser();

    userSettingsActions.post(UI_LOCALE_KEY, "en").validateStatus(200);
    userSettingsActions.post(DB_LOCALE_KEY, "en").validateStatus(200);
  }

  @AfterAll
  public void after() {
    LoginActions loginActions = new LoginActions();
    loginActions.loginAsSuperUser();

    userSettingsActions.post(UI_LOCALE_KEY, "en").validateStatus(200);
    userSettingsActions.post(DB_LOCALE_KEY, "en").validateStatus(200);
  }

  @Test
  @DisplayName(
      "Retrieving the UI locale 'id' should be possible after setting the UI locale with 'id'")
  void setAndRetrieveUiLocale() {
    // given
    assertEquals(
        "en",
        userSettingsActions.get(UI_LOCALE_KEY).validateStatus(200).getAsString(),
        "default locale should be 'en'");

    // when
    userSettingsActions.post(UI_LOCALE_KEY, "id").validateStatus(200);

    // then
    assertEquals(
        "id",
        userSettingsActions.get(UI_LOCALE_KEY).validateStatus(200).getAsString(),
        "user setting ui locale should be 'id'");
  }

  @Test
  @DisplayName(
      "Retrieving the UI locale 'id_ID' should be possible after setting the UI locale with 'id_ID'")
  void setAndRetrieveUiLocale2() {
    // given
    assertEquals(
        "en",
        userSettingsActions.get(UI_LOCALE_KEY).validateStatus(200).getAsString(),
        "default locale should be 'en'");

    // when
    userSettingsActions.post(UI_LOCALE_KEY, "id_ID").validateStatus(200);

    // then
    assertEquals(
        "id_ID",
        userSettingsActions.get(UI_LOCALE_KEY).validateStatus(200).getAsString(),
        "user setting ui locale should be 'id_ID'");
  }

  @Test
  @DisplayName(
      "Retrieving all user settings should include the UI locale 'id_ID' after setting the UI locale with 'id_ID'")
  void setAndRetrieveUiLocale3() {
    // given
    assertEquals(
        "en",
        userSettingsActions.get(UI_LOCALE_KEY).validateStatus(200).getAsString(),
        "default locale should be 'en'");

    // when
    userSettingsActions.post(UI_LOCALE_KEY, "id_ID").validateStatus(200);

    // then
    userSettingsActions
        .get()
        .validateStatus(200)
        .validateStatus(200)
        .validate()
        .body("keyUiLocale", equalTo("id_ID"));
  }

  @Test
  void setInvalidUiLocalePath() {
    userSettingsActions
        .postNoBody(UI_LOCALE_KEY + "?value=invalidLocale")
        .validateStatus(400)
        .validate()
        .body("message", containsString("Invalid locale format"));
  }

  @Test
  void setInvalidUiLocaleBody() {
    userSettingsActions
        .post(UI_LOCALE_KEY, "invalidLocale")
        .validateStatus(400)
        .validate()
        .body("message", containsString("Invalid locale format"));
  }

  @Test
  void setAndRetrieveDbLocale() {
    // given
    assertEquals(
        "en",
        userSettingsActions.get(DB_LOCALE_KEY).validateStatus(200).getAsString(),
        "default locale should be 'en'");

    // when
    userSettingsActions.post(DB_LOCALE_KEY, "in").validateStatus(200);

    // then
    assertEquals(
        "in",
        userSettingsActions.get(DB_LOCALE_KEY).validateStatus(200).getAsString(),
        "user setting ui locale should be 'in'");
  }
}
