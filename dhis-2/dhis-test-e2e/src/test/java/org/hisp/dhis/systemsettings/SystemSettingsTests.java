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
package org.hisp.dhis.systemsettings;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.helpers.TestCleanUp;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.SystemSettingActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * @author David Katuscak <katuscak.d@gmail.com>
 */
class SystemSettingsTests extends ApiTest {
  private static final String APPLICATION_INTRO_KEY = "keyApplicationIntro";

  private static final String APPLICATION_FOOTER_KEY = "keyApplicationFooter";

  private static final String MAX_SYNC_ATTEMPTS_KEY = "syncMaxAttempts";

  private static final int MAX_SYNC_ATTEMPTS_DEFAULT_VALUE = 3;

  private static final String MAX_PASSWORD_LENGTH_KEY = "maxPasswordLength";

  private static final int MAX_PASSWORD_LENGTH_DEFAULT_VALUE = 72;

  private static final String EMAIL_SENDER_KEY = "keyEmailSender";

  private static final String DEFAULT_FOOTER = "Learn more ...";

  private static final String DEFAULT_INTRO = "Default - Welcome to the DHIS2";

  private static final String ENGLISH_INTRO = "English - Welcome to the DHIS2";

  private static final String FRENCH_INTRO = "French - Welcome to the DHIS2";

  private static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain; charset=UTF-8";

  private SystemSettingActions systemSettingActions;

  @BeforeEach
  public void setUp() {
    new TestCleanUp().deleteCreatedEntities("/systemSettings");

    systemSettingActions = new SystemSettingActions();

    LoginActions loginActions = new LoginActions();
    loginActions.loginAsDefaultUser();
  }

  private void prepareData() {
    QueryParamsBuilder params = new QueryParamsBuilder();
    params.add("value=" + DEFAULT_FOOTER);

    systemSettingActions.post(
        APPLICATION_FOOTER_KEY, CONTENT_TYPE_TEXT_PLAIN, new JsonObject(), params);

    params = new QueryParamsBuilder();
    params.add("value=" + DEFAULT_INTRO);

    systemSettingActions.post(
        APPLICATION_INTRO_KEY, CONTENT_TYPE_TEXT_PLAIN, new JsonObject(), params);

    params = new QueryParamsBuilder();
    params.add("value=" + ENGLISH_INTRO);
    params.add("locale=en");

    systemSettingActions.post(
        APPLICATION_INTRO_KEY, CONTENT_TYPE_TEXT_PLAIN, new JsonObject(), params);

    params = new QueryParamsBuilder();
    params.add("value=" + FRENCH_INTRO);
    params.add("locale=fr");

    systemSettingActions.post(
        APPLICATION_INTRO_KEY, CONTENT_TYPE_TEXT_PLAIN, new JsonObject(), params);
  }

  @Test
  void addSystemSetting() {
    String specificFooter = "Learn more at ";

    QueryParamsBuilder params = new QueryParamsBuilder();
    params.add("value=" + specificFooter);

    ApiResponse response =
        systemSettingActions.post(
            APPLICATION_FOOTER_KEY, CONTENT_TYPE_TEXT_PLAIN, new JsonObject(), params);

    response.validate().statusCode(200).body(containsString(specificFooter));
  }

  @Test
  void returnDefaultValueWhenTranslationIsNotAvailable() {
    prepareData();

    ApiResponse response =
        systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder().add("locale=pl"));

    response.validate().statusCode(200).body(containsString(DEFAULT_INTRO));
  }

  @Test
  void returnTranslationForUsersLocale() {
    prepareData();

    ApiResponse response =
        systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder().add("locale="));

    response.validate().log().all().statusCode(200).body(containsString(ENGLISH_INTRO));
  }

  @Test
  void returnTranslationForGivenLocale() {
    prepareData();

    ApiResponse response =
        systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder().add("locale=fr"));

    response.validate().statusCode(200).body(containsString(FRENCH_INTRO));
  }

  @Test
  void returnAllSystemSettings() {
    prepareData();

    ApiResponse response = systemSettingActions.get();

    response
        .validate()
        .statusCode(200)
        .body(APPLICATION_INTRO_KEY, equalTo(DEFAULT_INTRO))
        .body(APPLICATION_FOOTER_KEY, equalTo(DEFAULT_FOOTER));
  }

  @Test
  void deleteTranslationForGivenLocaleAndSettingKey() {
    prepareData();

    ApiResponse response =
        systemSettingActions.delete(
            APPLICATION_INTRO_KEY, new QueryParamsBuilder().add("locale=fr"));

    response.validate().statusCode(204);

    response =
        systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder().add("locale=fr"));

    response.validate().statusCode(200).body(containsString(DEFAULT_INTRO));
  }

  @Test
  void deleteSystemSetting() {
    prepareData();

    ApiResponse response = systemSettingActions.delete(APPLICATION_INTRO_KEY);

    response.validate().statusCode(204);

    response =
        systemSettingActions.get(
            APPLICATION_INTRO_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder());

    response.validate().statusCode(200);

    assertEquals(ENGLISH_INTRO, response.getAsString());
  }

  @Test
  void getDefaultSystemSettingAsText() {
    ApiResponse response =
        systemSettingActions.get(
            MAX_SYNC_ATTEMPTS_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_SYNC_ATTEMPTS_DEFAULT_VALUE)));

    // -----------------------------------------
    response =
        systemSettingActions.get(
            MAX_PASSWORD_LENGTH_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_PASSWORD_LENGTH_DEFAULT_VALUE)));

    // -----------------------------------------
    response =
        systemSettingActions.get(
            EMAIL_SENDER_KEY,
            CONTENT_TYPE_TEXT_PLAIN,
            CONTENT_TYPE_TEXT_PLAIN,
            new QueryParamsBuilder());
  }

  @Test
  void getDefaultSystemSettingAsJson() {
    ApiResponse response =
        systemSettingActions.get(
            MAX_SYNC_ATTEMPTS_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_SYNC_ATTEMPTS_DEFAULT_VALUE)));

    // -----------------------------------------
    response =
        systemSettingActions.get(
            MAX_PASSWORD_LENGTH_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_PASSWORD_LENGTH_DEFAULT_VALUE)));

    // -----------------------------------------
    response =
        systemSettingActions.get(
            EMAIL_SENDER_KEY,
            ContentType.JSON.toString(),
            ContentType.JSON.toString(),
            new QueryParamsBuilder());
  }

  @Test
  void getDefaultSystemSettingWithNonSpecifiedContentTypeAndAccept() {
    ApiResponse response =
        systemSettingActions.get(MAX_SYNC_ATTEMPTS_KEY, "", "", new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_SYNC_ATTEMPTS_DEFAULT_VALUE)));

    // -----------------------------------------
    response = systemSettingActions.get(MAX_PASSWORD_LENGTH_KEY, "", "", new QueryParamsBuilder());

    response
        .validate()
        .statusCode(200)
        .body(containsString(String.valueOf(MAX_PASSWORD_LENGTH_DEFAULT_VALUE)));

    // -----------------------------------------
    response = systemSettingActions.get(EMAIL_SENDER_KEY, "", "", new QueryParamsBuilder());
  }

  @Test
  @DisplayName(
      "A call to the system settings endpoint returns expected cache control header values")
  void getSystemSettingsCheckCacheControl() {
    ValidatableResponse validate = systemSettingActions.get().validate();
    validate.statusCode(200);
    validate.header("Cache-Control", "no-cache, no-store, must-revalidate");
  }

  @Test
  @DisplayName(
      "The value/default value returned for the Google Maps API key is an empty string, when JSON requested")
  void getSystemSettingsGoogleMapsApiKeyJson() {
    systemSettingActions
        .get("keyGoogleMapsApiKey")
        .validate()
        .statusCode(200)
        .body("keyGoogleMapsApiKey", emptyString());
  }
}
