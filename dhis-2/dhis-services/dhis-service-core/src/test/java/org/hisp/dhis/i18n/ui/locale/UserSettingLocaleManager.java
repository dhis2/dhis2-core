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
package org.hisp.dhis.i18n.ui.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Locale;
import org.apache.commons.lang3.LocaleUtils;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.hisp.dhis.user.DefaultUserSettingService;
import org.hisp.dhis.user.UserSettingKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSettingLocaleManagerTest {

  @Mock private DefaultUserSettingService userSettingService;
  @Mock private ResourceBundleManager resourceBundleManager;
  @InjectMocks private UserSettingLocaleManager manager;

  @Test
  @DisplayName("When a user ui locale of 'id' is retrieved then a valid Locale of 'in' is returned")
  void testValidIdLocale() {
    // given
    String validLocale = "id";
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("in"), currentLocale);
  }

  @Test
  @DisplayName(
      "When a user ui locale of 'id_ID' is retrieved then a valid Locale of 'in_ID' is returned")
  void testValidIdLocale2() {
    // given
    String validLocale = "id_ID";
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("in_ID"), currentLocale);
  }

  @Test
  @DisplayName("When a user ui locale of 'en' is retrieved then a valid Locale of 'en' is returned")
  void testDefaultLocale() {
    // given
    Locale validLocale = LocaleUtils.toLocale("en");
    when(userSettingService.getUserSetting(UserSettingKey.UI_LOCALE)).thenReturn(validLocale);

    // when
    Locale currentLocale = manager.getCurrentLocale();

    // then
    assertNotNull(currentLocale);
    assertEquals(LocaleUtils.toLocale("en"), currentLocale);
  }
}
