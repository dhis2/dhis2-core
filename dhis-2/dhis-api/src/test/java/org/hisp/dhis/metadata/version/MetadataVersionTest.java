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
package org.hisp.dhis.metadata.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MetadataVersionTest {

  @Test
  void testEqualsAndHashCodeUseUidCodeAndName() {
    MetadataVersion first = version("VersionUid1", "code", "Version_1");
    first.setId(1);
    first.setHashCode("hash1");
    first.setType(VersionType.ATOMIC);
    MetadataVersion second = version("VersionUid1", "code", "Version_1");
    second.setId(2);
    second.setHashCode("hash2");
    second.setType(VersionType.BEST_EFFORT);
    second.setImportDate(new Date());

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @ParameterizedTest
  @CsvSource({
    "VersionUid2, code, Version_1",
    "VersionUid1, other, Version_1",
    "VersionUid1, code, Version_2"
  })
  void testNotEqualWhenUidCodeOrNameDiffers(String uid, String code, String name) {
    assertNotEquals(version("VersionUid1", "code", "Version_1"), version(uid, code, name));
  }

  @Test
  void testNotEqualToOtherIdentifiableType() {
    User user = new User();
    user.setUid("VersionUid1");
    user.setCode("code");
    user.setName("Version_1");

    assertNotEquals(version("VersionUid1", "code", "Version_1"), user);
  }

  @Test
  void testHashCodeFormula() {
    int expected =
        31 * (31 * "VersionUid1".hashCode() + "code".hashCode()) + "Version_1".hashCode();

    assertEquals(expected, version("VersionUid1", "code", "Version_1").hashCode());
  }

  @Test
  void testDisplayNameIsName() {
    MetadataVersion version = version("VersionUid1", null, "Version_1");

    assertEquals("Version_1", version.getDisplayName());
    assertEquals("Version_1", version.getDisplayPropertyValue(IdScheme.NAME));
  }

  @Test
  void testPropertyValue() {
    MetadataVersion version = version("VersionUid1", "code", "Version_1");
    version.setId(42);

    assertEquals("VersionUid1", version.getPropertyValue(IdScheme.UID));
    assertEquals("code", version.getPropertyValue(IdScheme.CODE));
    assertEquals("Version_1", version.getPropertyValue(IdScheme.NAME));
    assertEquals("42", version.getPropertyValue(IdScheme.ID));
  }

  @Test
  void testSetUserSetsCreatedByAndOwner() {
    User user = new User();
    user.setUid("UserUid0001");
    MetadataVersion version = version("VersionUid1", null, "Version_1");

    version.setUser(user);

    assertSame(user, version.getCreatedBy());
    assertSame(user, version.getUser());
    assertEquals("UserUid0001", version.getSharing().getOwner());
  }

  @Test
  void testTranslationsAndAttributeValuesIgnoreWrites() {
    MetadataVersion version = version("VersionUid1", null, "Version_1");

    version.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Version un")));
    version.getTranslations().add(new Translation(new Locale("fr"), "NAME", "Version un"));
    version.addAttributeValue("AttrUid0001", "value");
    version.setAttributeValues(AttributeValues.of("{\"AttrUid0001\":{\"value\":\"value\"}}"));

    assertTrue(version.getTranslations().isEmpty());
    assertTrue(version.getAttributeValues().isEmpty());
    assertEquals("Version_1", version.getDisplayName());
  }

  private static MetadataVersion version(String uid, String code, String name) {
    MetadataVersion version = new MetadataVersion(name, VersionType.ATOMIC);
    version.setUid(uid);
    version.setCode(code);
    return version;
  }
}
