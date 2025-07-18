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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class FieldPathHelperTest extends PostgresIntegrationTestBase {
  @Autowired private FieldPathHelper helper;

  @Test
  void testApplyPresetHowDoesCollectionNameWork() {
    Map<String, FieldPath> fieldMapPath = new HashMap<>();

    FieldPath preset = new FieldPath(FieldPreset.PERSISTED.getName(), List.of(), false, true);

    helper.applyPresets(List.of(preset), fieldMapPath, DataElement.class);

    assertPropertyExists("id", fieldMapPath);
    assertPropertyExists("name", fieldMapPath);
    assertPropertyExists("shortName", fieldMapPath);
    assertPropertyExists("description", fieldMapPath);
    assertPropertyExists("valueType", fieldMapPath);
    assertPropertyExists("aggregationType", fieldMapPath);
    assertPropertyExists("domainType", fieldMapPath);

    assertNull(fieldMapPath.get("access"));
    assertNull(fieldMapPath.get("dataSetElements"));
    assertNull(fieldMapPath.get("optionSet"));
    assertNull(fieldMapPath.get("categoryCombo"));
    assertNull(fieldMapPath.get("translations"));
  }

  @Test
  void testApplySimplePreset() {
    Map<String, FieldPath> fieldMapPath = new HashMap<>();

    FieldPath owner = new FieldPath(FieldPreset.SIMPLE.getName(), List.of(), false, true);

    helper.applyPresets(List.of(owner), fieldMapPath, DataElement.class);

    assertPropertyExists("id", fieldMapPath);
    assertPropertyExists("name", fieldMapPath);
    assertPropertyExists("shortName", fieldMapPath);
    assertPropertyExists("description", fieldMapPath);
    assertPropertyExists("valueType", fieldMapPath);
    assertPropertyExists("aggregationType", fieldMapPath);
    assertPropertyExists("domainType", fieldMapPath);

    assertNull(fieldMapPath.get("access"));
    assertNull(fieldMapPath.get("dataSetElements"));
    assertNull(fieldMapPath.get("optionSet"));
    assertNull(fieldMapPath.get("categoryCombo"));
    assertNull(fieldMapPath.get("translations"));
  }

  @Test
  void testApplyIdentifiablePreset() {
    Map<String, FieldPath> fieldMapPath = new HashMap<>();

    FieldPath owner = new FieldPath(FieldPreset.IDENTIFIABLE.getName(), List.of(), false, true);

    helper.applyPresets(List.of(owner), fieldMapPath, DataElement.class);

    assertPropertyExists("id", fieldMapPath);
    assertPropertyExists("name", fieldMapPath);
    assertPropertyExists("code", fieldMapPath);
    assertPropertyExists("created", fieldMapPath);
    assertPropertyExists("lastUpdated", fieldMapPath);
    assertPropertyExists("lastUpdatedBy", fieldMapPath);

    assertNull(fieldMapPath.get("shortName"));
    assertNull(fieldMapPath.get("description"));
  }

  @Test
  @DisplayName("Excluding skip sharing fields on the User class does not remove incorrect fields")
  void skipSharingFieldsExcludeCorrectFieldsTest() {
    // given skipSharing exclusions
    FieldPath user = new FieldPath("user", List.of(), true, false);
    FieldPath publicAccess = new FieldPath("publicAccess", List.of(), true, false);
    FieldPath userGroupAccesses = new FieldPath("userGroupAccesses", List.of(), true, false);
    FieldPath userAccesses = new FieldPath("userAccesses", List.of(), true, false);
    FieldPath externalAccess = new FieldPath("externalAccess", List.of(), true, false);
    FieldPath sharing = new FieldPath("sharing", List.of(), true, false);

    // and default preset owner
    FieldPath owner = new FieldPath("owner", List.of(), false, true);

    // when applying skipSharing exclusions for the User class
    List<FieldPath> result =
        helper.apply(
            List.of(
                user,
                owner,
                publicAccess,
                userGroupAccesses,
                userAccesses,
                externalAccess,
                sharing),
            User.class);

    // then only matching exclusions should have been applied
    // and fields starting with 'user' should still be present
    assertEquals(58, result.size()); // all user properties
    assertTrue(
        result.stream()
            .map(FieldPath::getName)
            .toList()
            .containsAll(List.of("username", "userRoles")));
  }

  @Test
  @DisplayName("nameable field filter for DataElement returns expected fields")
  void nameableFieldFilterTest() {
    // given
    FieldPath fieldPath = new FieldPath(FieldPreset.NAMEABLE.getName(), List.of());
    Map<String, FieldPath> fieldPathMap = new HashMap<>();

    // when
    helper.applyPresets(List.of(fieldPath), fieldPathMap, DataElement.class);

    // then
    assertEquals(7, fieldPathMap.size());
    assertTrue(
        FieldPreset.NAMEABLE
            .getFields()
            .containsAll(fieldPathMap.values().stream().map(FieldPath::getName).toList()));
  }

  @Test
  @DisplayName("persisted field filter for DataElement returns fields")
  void persistedFieldFilterTest() {
    // given
    FieldPath fieldPath = new FieldPath(FieldPreset.PERSISTED.getName(), List.of());
    Map<String, FieldPath> fieldPathMap = new HashMap<>();

    // when
    helper.applyPresets(List.of(fieldPath), fieldPathMap, DataElement.class);

    // then
    assertFalse(fieldPathMap.isEmpty());
  }

  private void assertPropertyExists(String propertyName, Map<String, FieldPath> fieldMapPath) {
    assertNotNull(fieldMapPath.get(propertyName));
    assertEquals(propertyName, fieldMapPath.get(propertyName).getName());
  }
}
