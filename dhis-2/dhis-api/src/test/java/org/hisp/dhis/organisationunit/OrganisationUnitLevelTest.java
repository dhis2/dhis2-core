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
package org.hisp.dhis.organisationunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrganisationUnitLevelTest {

  @AfterEach
  void tearDown() {
    ThreadUserSettings.clear();
  }

  @Test
  void testEqualsAndHashCodeUseUidCodeAndName() {
    OrganisationUnitLevel first = level("OuLevelUid1", "code", "District");
    first.setId(1);
    first.setLevel(2);
    OrganisationUnitLevel second = level("OuLevelUid1", "code", "District");
    second.setId(2);
    second.setLevel(3);
    second.setOfflineLevels(1);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @ParameterizedTest
  @CsvSource({
    "OuLevelUid2, code, District",
    "OuLevelUid1, other, District",
    "OuLevelUid1, code, Chiefdom"
  })
  void testNotEqualWhenUidCodeOrNameDiffers(String uid, String code, String name) {
    assertNotEquals(level("OuLevelUid1", "code", "District"), level(uid, code, name));
  }

  @Test
  void testNotEqualToNullOrOtherIdentifiableType() {
    User user = new User();
    user.setUid("OuLevelUid1");
    user.setCode("code");
    user.setName("District");

    OrganisationUnitLevel level = level("OuLevelUid1", "code", "District");

    assertFalse(level.equals(null));
    assertNotEquals(level, user);
  }

  @Test
  void testDisplayNameIsNameWithoutTranslation() {
    assertEquals("District", level("OuLevelUid1", null, "District").getDisplayName());
  }

  @Test
  void testDisplayNameIsTranslatedNameForUserDbLocale() {
    OrganisationUnitLevel level = level("OuLevelUid1", null, "District");
    level.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Région")));
    ThreadUserSettings.put(Map.of("keyDbLocale", "fr"));

    assertEquals("Région", level.getDisplayName());
    assertEquals("Région", level.getDisplayPropertyValue(IdScheme.NAME));
  }

  @Test
  void testSetTranslationsNullStoresEmptySet() {
    OrganisationUnitLevel level = level("OuLevelUid1", null, "District");
    level.setTranslations(null);

    assertNotNull(level.getTranslations());
    assertTrue(level.getTranslations().isEmpty());
  }

  @Test
  void testPropertyValue() {
    OrganisationUnitLevel level = level("OuLevelUid1", "code", "District");
    level.setId(42);

    assertEquals("OuLevelUid1", level.getPropertyValue(IdScheme.UID));
    assertEquals("code", level.getPropertyValue(IdScheme.CODE));
    assertEquals("District", level.getPropertyValue(IdScheme.NAME));
    assertEquals("42", level.getPropertyValue(IdScheme.ID));
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    OrganisationUnitLevel level = level("OuLevelUid1", null, "District");
    User user = new User();
    user.setUid("UserUid0001");
    Sharing sharing = new Sharing();
    sharing.setPublicAccess("rw------");

    level.setCreatedBy(user);
    level.setUser(user);
    level.setOwner("UserUid0001");
    level.setSharing(sharing);
    level.addAttributeValue("AttrUid0001", "value");
    level.getSharing().setPublicAccess("rw------");

    assertNull(level.getCreatedBy());
    assertNull(level.getUser());
    assertNull(level.getSharing().getOwner());
    assertNull(level.getSharing().getPublicAccess());
    assertTrue(level.getAttributeValues().isEmpty());
  }

  private static OrganisationUnitLevel level(String uid, String code, String name) {
    OrganisationUnitLevel level = new OrganisationUnitLevel();
    level.setUid(uid);
    level.setCode(code);
    level.setName(name);
    return level;
  }
}
