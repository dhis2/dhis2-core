/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.i18n.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class I18nLocaleTest {

  @Test
  void testEqualsAndHashCodeUseUidCodeAndName() {
    I18nLocale first = locale("uidLocale01", "code", "French", "fr");
    first.setId(1);
    I18nLocale second = locale("uidLocale01", "code", "French", "fr_FR");
    second.setId(2);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void testNotEqualWhenUidDiffers() {
    assertNotEquals(
        locale("uidLocale01", "code", "French", "fr"),
        locale("uidLocale02", "code", "French", "fr"));
  }

  @Test
  void testNotEqualWhenCodeDiffers() {
    assertNotEquals(
        locale("uidLocale01", "codeA", "French", "fr"),
        locale("uidLocale01", "codeB", "French", "fr"));
  }

  @Test
  void testNotEqualWhenNameDiffers() {
    assertNotEquals(
        locale("uidLocale01", "code", "French", "fr"),
        locale("uidLocale01", "code", "German", "fr"));
  }

  @Test
  void testNotEqualToNull() {
    assertFalse(locale("uidLocale01", "code", "French", "fr").equals(null));
  }

  @Test
  void testNotEqualToOtherIdentifiableType() {
    User user = new User();
    user.setUid("uidLocale01");
    user.setCode("code");
    user.setName("French");

    assertNotEquals(locale("uidLocale01", "code", "French", "fr"), user);
  }

  @Test
  void testLocaleConstructorTakesDisplayNameAndCode() {
    I18nLocale locale = new I18nLocale(new Locale("fr", "FR"));

    assertEquals("French (France)", locale.getName());
    assertEquals("fr_FR", locale.getLocale());
  }

  @Test
  void testDisplayNameIsName() {
    assertEquals("French", locale("uidLocale01", null, "French", "fr").getDisplayName());
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    I18nLocale locale = locale("uidLocale01", null, "French", "fr");
    User user = new User();
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("rw------");

    locale.setCreatedBy(user);
    locale.setUser(user);
    locale.setSharing(sharing);
    locale.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Français")));
    locale.setHref("http://localhost/api/locales/dbLocales/uidLocale01");

    assertNull(locale.getCreatedBy());
    assertNull(locale.getUser());
    assertNull(locale.getSharing().getPublicAccess());
    assertTrue(locale.getTranslations().isEmpty());
    assertTrue(locale.getAttributeValues().isEmpty());
    assertNull(locale.getHref());
    assertNull(locale.getAccess());
  }

  @Test
  void testUnsupportedCollectionsAreFreshAndMutable() {
    I18nLocale locale = locale("uidLocale01", null, "French", "fr");

    Set<Translation> translations = locale.getTranslations();
    translations.add(new Translation(new Locale("fr"), "NAME", "Français"));

    assertNotSame(translations, locale.getTranslations());
    assertNotSame(locale.getSharing(), locale.getSharing());
  }

  private static I18nLocale locale(String uid, String code, String name, String locale) {
    I18nLocale i18nLocale = new I18nLocale();
    i18nLocale.setUid(uid);
    i18nLocale.setCode(code);
    i18nLocale.setName(name);
    i18nLocale.setLocale(locale);
    return i18nLocale;
  }
}
