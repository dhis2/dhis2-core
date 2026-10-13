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
package org.hisp.dhis.indicator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class IndicatorTypeTest {

  @AfterEach
  void tearDown() {
    ThreadUserSettings.clear();
  }

  @Test
  void testEqualsAndHashCodeUseUidCodeAndName() {
    IndicatorType first = indicatorType("IndTypeUid1", "code", "Percent");
    first.setId(1);
    first.setFactor(100);
    IndicatorType second = indicatorType("IndTypeUid1", "code", "Percent");
    second.setId(2);
    second.setFactor(1000);
    second.setNumber(true);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void testNotEqualWhenUidDiffers() {
    assertNotEquals(
        indicatorType("IndTypeUid1", "code", "Percent"),
        indicatorType("IndTypeUid2", "code", "Percent"));
  }

  @Test
  void testNotEqualWhenCodeDiffers() {
    assertNotEquals(
        indicatorType("IndTypeUid1", "codeA", "Percent"),
        indicatorType("IndTypeUid1", "codeB", "Percent"));
  }

  @Test
  void testNotEqualWhenNameDiffers() {
    assertNotEquals(
        indicatorType("IndTypeUid1", "code", "Percent"),
        indicatorType("IndTypeUid1", "code", "Per thousand"));
  }

  @Test
  void testNotEqualToNull() {
    assertFalse(indicatorType("IndTypeUid1", "code", "Percent").equals(null));
  }

  @Test
  void testNotEqualToOtherIdentifiableType() {
    User user = new User();
    user.setUid("IndTypeUid1");
    user.setCode("code");
    user.setName("Percent");

    assertNotEquals(indicatorType("IndTypeUid1", "code", "Percent"), user);
  }

  @Test
  void testConstructorSetsNameFactorAndNumber() {
    IndicatorType type = new IndicatorType("Number", 1, true);

    assertEquals("Number", type.getName());
    assertEquals(1, type.getFactor());
    assertTrue(type.isNumber());
  }

  @Test
  void testDisplayNameIsNameWithoutTranslation() {
    assertEquals("Percent", indicatorType("IndTypeUid1", null, "Percent").getDisplayName());
  }

  @Test
  void testDisplayNameIsTranslatedNameForUserDbLocale() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");
    type.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Pourcentage")));
    ThreadUserSettings.put(Map.of("keyDbLocale", "fr"));

    assertEquals("Pourcentage", type.getDisplayName());
    assertEquals("Pourcentage", type.getDisplayPropertyValue(IdScheme.NAME));
  }

  @Test
  void testSetTranslationsNullStoresEmptySet() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");
    type.setTranslations(null);

    assertNotNull(type.getTranslations());
    assertTrue(type.getTranslations().isEmpty());
  }

  @Test
  void testPropertyValue() {
    IndicatorType type = indicatorType("IndTypeUid1", "code", "Percent");
    type.setId(42);

    assertEquals("IndTypeUid1", type.getPropertyValue(IdScheme.UID));
    assertEquals("code", type.getPropertyValue(IdScheme.CODE));
    assertEquals("Percent", type.getPropertyValue(IdScheme.NAME));
    assertEquals("42", type.getPropertyValue(IdScheme.ID));
  }

  @Test
  void testHrefAndAccessAreKept() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");
    Access access = new Access();
    type.setHref("http://localhost/api/indicatorTypes/IndTypeUid1");
    type.setAccess(access);

    assertEquals("http://localhost/api/indicatorTypes/IndTypeUid1", type.getHref());
    assertSame(access, type.getAccess());
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");
    User user = new User();
    user.setUid("UserUid0001");
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("rw------");

    type.setCreatedBy(user);
    type.setUser(user);
    type.setOwner("UserUid0001");
    type.setSharing(sharing);
    type.addAttributeValue("AttrUid0001", "value");

    assertNull(type.getCreatedBy());
    assertNull(type.getUser());
    assertNull(type.getSharing().getOwner());
    assertNull(type.getSharing().getPublicAccess());
    assertTrue(type.getAttributeValues().isEmpty());
  }

  @Test
  void testSharingIsFreshAndMutable() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");

    Sharing sharing = type.getSharing();
    sharing.setPublicAccess("rw------");

    assertNotSame(sharing, type.getSharing());
    assertNull(type.getSharing().getPublicAccess());
  }

  @Test
  void testTranslationsAreMutable() {
    IndicatorType type = indicatorType("IndTypeUid1", null, "Percent");
    type.setTranslations(new HashSet<>());

    type.getTranslations().add(new Translation(new Locale("fr"), "NAME", "Pourcentage"));

    assertEquals(1, type.getTranslations().size());
  }

  private static IndicatorType indicatorType(String uid, String code, String name) {
    IndicatorType type = new IndicatorType();
    type.setUid(uid);
    type.setCode(code);
    type.setName(name);
    return type;
  }
}
