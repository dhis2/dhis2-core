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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import org.hisp.dhis.common.DisplayProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserSettingKey}.
 *
 * @author Volker Schmidt
 */
class UserSettingKeyTest {

  @Test
  void getAsRealClassEnum() {
    Assertions.assertSame(
        DisplayProperty.SHORTNAME,
        UserSettingKey.getAsRealClass(UserSettingKey.ANALYSIS_DISPLAY_PROPERTY, "shortName"));
  }

  @Test
  void getAsRealClassOther() {
    Assertions.assertSame(
        "Test Layout",
        UserSettingKey.getAsRealClass(UserSettingKey.TRACKER_DASHBOARD_LAYOUT, "Test Layout"));
  }

  @Test
  @DisplayName("An exception should be thrown when an invalid locale is detected")
  void invalidLocaleTest() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "invalidLocale"));

    assertEquals("Invalid locale format: invalidLocale", ex.getMessage());
  }

  @Test
  @DisplayName("The correct Indonesian UI locale should be returned for 'id'")
  void validLocaleId1Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "id");

    assertEquals("id", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian UI locale should be returned for 'id_ID'")
  void validLocaleId2Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "id_ID");

    assertEquals("id_ID", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian UI locale should be returned for 'in'")
  void validLocaleId3Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "in");

    assertEquals("id", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian UI locale should be returned for 'in_ID'")
  void validLocaleId4Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "in_ID");

    assertEquals("id_ID", locale.toString());
  }

  @Test
  @DisplayName("The correct English UI locale should be returned for 'en'")
  void validLocaleEnTest() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.UI_LOCALE, "en");

    assertEquals("en", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian DB locale should be returned for 'in'")
  void validDbLocaleTest() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.DB_LOCALE, "in");

    assertEquals("in", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian DB locale should be returned for 'in_ID'")
  void validDbLocale2Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.DB_LOCALE, "in_ID");

    assertEquals("in_ID", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian DB locale should be returned for 'id'")
  void validDbLocale3Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.DB_LOCALE, "id");

    assertEquals("in", locale.toString());
  }

  @Test
  @DisplayName("The correct Indonesian DB locale should be returned for 'id_ID'")
  void validDbLocale4Test() {
    Serializable locale = UserSettingKey.getAsRealClass(UserSettingKey.DB_LOCALE, "id_ID");

    assertEquals("in_ID", locale.toString());
  }
}
