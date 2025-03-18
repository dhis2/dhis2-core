/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the settings translation feature included in {@link SystemSettingsService}
 *
 * @author Jan Bernitt
 */
class SystemSettingsTranslationServiceTest extends PostgresIntegrationTestBase {

  @Autowired private SystemSettingsTranslationService settingsTranslationService;

  @Test
  void testSaveSystemSettingTranslation_Add() throws Exception {
    putTranslation("applicationTitle", "en", "text-en");
    putTranslation("applicationTitle", "de", "text-de");
    putTranslation("loginPopup", "en", "text2-en");
    putTranslation("loginPopup", "it", "text2-it");

    assertTranslation("applicationTitle", "en", "text-en");
    assertTranslation("applicationTitle", "de", "text-de");
    assertNoTranslation("applicationTitle", "it");
    assertTranslation("loginPopup", "en", "text2-en");
    assertTranslation("loginPopup", "it", "text2-it");
    assertNoTranslation("loginPopup", "de");
  }

  @Test
  void testSaveSystemSettingTranslation_Update() throws Exception {
    putTranslation("applicationTitle", "en", "text-en");
    putTranslation("applicationTitle", "de", "text-de");

    assertTranslation("applicationTitle", "en", "text-en");
    assertTranslation("applicationTitle", "de", "text-de");

    putTranslation("applicationTitle", "en", "new-text-en");
    assertTranslation("applicationTitle", "en", "new-text-en");
    assertTranslation("applicationTitle", "de", "text-de");
  }

  @Test
  void testSaveSystemSettingTranslation_DeleteNull() throws Exception {
    putTranslation("applicationTitle", "en", "text-en");
    putTranslation("applicationTitle", "de", "text-de");

    putTranslation("applicationTitle", "en", null);
    assertNoTranslation("applicationTitle", "en");
    assertTranslation("applicationTitle", "de", "text-de");

    putTranslation("applicationTitle", "de", null);
    assertNoTranslation("applicationTitle", "en");
    assertNoTranslation("applicationTitle", "de");

    putTranslation("applicationTitle", "it", null);
    assertNoTranslation("applicationTitle", "it");
  }

  @Test
  void testSaveSystemSettingTranslation_DeleteEmpty() throws Exception {
    putTranslation("applicationTitle", "en", "text-en");
    putTranslation("applicationTitle", "de", "text-de");

    putTranslation("applicationTitle", "en", "");
    assertNoTranslation("applicationTitle", "en");
    assertTranslation("applicationTitle", "de", "text-de");

    putTranslation("applicationTitle", "de", "");
    assertNoTranslation("applicationTitle", "en");
    assertNoTranslation("applicationTitle", "de");

    putTranslation("applicationTitle", "it", "");
    assertNoTranslation("applicationTitle", "it");
  }

  private void putTranslation(String key, String locale, String text)
      throws ForbiddenException, BadRequestException {
    settingsTranslationService.putSystemSettingTranslation(key, locale, text);
  }

  private void assertTranslation(String key, String locale, String expected) {
    assertEquals(
        Optional.of(expected), settingsTranslationService.getSystemSettingTranslation(key, locale));
  }

  private void assertNoTranslation(String key, String locale) {
    assertEquals(
        Optional.empty(), settingsTranslationService.getSystemSettingTranslation(key, locale));
  }
}
