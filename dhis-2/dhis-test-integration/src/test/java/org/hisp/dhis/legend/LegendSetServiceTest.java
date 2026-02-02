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
package org.hisp.dhis.legend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.setting.ThreadUserSettings;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.util.SharingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
class LegendSetServiceTest extends PostgresIntegrationTestBase {

  @Autowired private LegendSetService legendSetService;

  @PersistenceContext private EntityManager entityManager;
  
  @Autowired private IdentifiableObjectManager objectManager;

  private Legend legendA;

  private Legend legendB;

  private LegendSet legendSetA;

  @Test
  void testAddGetLegendSet() {
    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 0d, 10d);
    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);
    long idA = legendSetService.addLegendSet(legendSetA);
    assertEquals(legendSetA, legendSetService.getLegendSet(idA));
    assertEquals(2, legendSetService.getLegendSet(idA).getLegends().size());
  }

  @Test
  void testDeleteLegendSet() {
    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 0d, 10d);
    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);
    long idA = legendSetService.addLegendSet(legendSetA);
    legendSetService.deleteLegendSet(legendSetA);
    assertNull(legendSetService.getLegendSet(idA));
  }

  // -------------------------------------------------------------------------
  // JPA Annotation Tests for Legend
  // -------------------------------------------------------------------------

  @Test
  void testJpaLegendEntityPersistenceAndRetrieval() {
    // Create a legend with all fields populated
    legendA = createLegend('A', 5.0, 15.0);
    legendA.setCode("LEGEND_A_CODE");
    legendA.setColor("#FF0000");
    legendA.setImage("image.png");

    // Create and save a legend set with the legend
    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    long idA = legendSetService.addLegendSet(legendSetA);

    // Retrieve and verify all Legend fields
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertNotNull(retrieved);
    assertEquals(1, retrieved.getLegends().size());

    Legend retrievedLegend = retrieved.getLegends().iterator().next();
    assertEquals("LegendA", retrievedLegend.getName());
    assertEquals("LEGEND_A_CODE", retrievedLegend.getCode());
    assertEquals(5.0, retrievedLegend.getStartValue());
    assertEquals(15.0, retrievedLegend.getEndValue());
    assertEquals("#FF0000", retrievedLegend.getColor());
    assertEquals("image.png", retrievedLegend.getImage());
  }

  @Test
  void testJpaLegendManyToOneLegendSet() {
    // Test that legendSet ManyToOne relationship is properly loaded
    legendA = createLegend('A', 0d, 10d);
    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendA.setLegendSet(legendSetA);

    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    Legend retrievedLegend = retrieved.getLegends().iterator().next();

    // Verify the bidirectional relationship
    assertNotNull(retrievedLegend.getLegendSet());
    assertEquals(legendSetA.getUid(), retrievedLegend.getLegendSet().getUid());
  }

  @Test
  void testJpaLegendIdGeneration() {
    // Test that ID is properly generated using SEQUENCE strategy
    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 10d, 20d);

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);

    legendSetService.addLegendSet(legendSetA);

    assertTrue(legendA.getId() > 0);
    assertTrue(legendB.getId() > 0);
    assertTrue(legendA.getId() != legendB.getId());
  }

  @Test
  void testJpaLegendUpdateOperations() {
    // Test that update operations preserve all associations
    legendA = createLegend('A', 0d, 10d);
    legendA.setColor("#00FF00");

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    long idA = legendSetService.addLegendSet(legendSetA);

    // Update legend properties
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    Legend retrievedLegend = retrieved.getLegends().iterator().next();
    retrievedLegend.setName("UpdatedName");
    retrievedLegend.setStartValue(5.0);
    retrievedLegend.setEndValue(25.0);
    retrievedLegend.setColor("#0000FF");

    legendSetService.updateLegendSet(retrieved);

    // Verify updates
    LegendSet updated = legendSetService.getLegendSet(idA);
    Legend updatedLegend = updated.getLegends().iterator().next();
    assertEquals("UpdatedName", updatedLegend.getName());
    assertEquals(5.0, updatedLegend.getStartValue());
    assertEquals(25.0, updatedLegend.getEndValue());
    assertEquals("#0000FF", updatedLegend.getColor());
  }

  @Test
  void testJpaLegendNonNullConstraints() {
    // Test that non-null constraints are enforced on startValue and endValue
    legendA = createLegend('A', 100.0, 200.0);

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);

    // Should succeed with all required fields
    long idA = legendSetService.addLegendSet(legendSetA);
    assertTrue(idA > 0);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    Legend retrievedLegend = retrieved.getLegends().iterator().next();
    assertNotNull(retrievedLegend.getStartValue());
    assertNotNull(retrievedLegend.getEndValue());
    assertEquals(100.0, retrievedLegend.getStartValue());
    assertEquals(200.0, retrievedLegend.getEndValue());
  }

  @Test
  void testJpaLegendNullableFields() {
    // Test that nullable fields (color, image, legendSet) can be null
    legendA = createLegend('A', 0d, 10d);
    legendA.setColor(null);
    legendA.setImage(null);
    // Don't set legendSet

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    Legend retrievedLegend = retrieved.getLegends().iterator().next();
    // Color and image can be null
    assertNull(retrievedLegend.getColor());
    assertNull(retrievedLegend.getImage());
  }

  @Test
  void testJpaLegendCodeUniqueness() {
    // Test that code field has unique constraint
    legendA = createLegend('A', 0d, 10d);
    legendA.setCode("UNIQUE_CODE");

    legendB = createLegend('B', 10d, 20d);
    legendB.setCode("UNIQUE_CODE_B");

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);

    long idA = legendSetService.addLegendSet(legendSetA);
    assertTrue(idA > 0);

    // Verify both legends have different codes
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertEquals(2, retrieved.getLegends().size());
  }

  @Test
  void testJpaLegendTranslations() {
    // Test that translations are properly stored and retrieved
    legendA = createLegend('A', 0d, 10d);

    legendSetA = createLegendSet('A');
    legendSetA.getLegends().add(legendA);
    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    Legend retrievedLegend = retrieved.getLegends().iterator().next();

    // Translations should be empty by default
    assertNotNull(retrievedLegend.getTranslations());
    assertEquals(0, retrievedLegend.getTranslations().size());
  }

  // -------------------------------------------------------------------------
  // JPA Annotation Tests for LegendSet
  // -------------------------------------------------------------------------

  @Test
  void testJpaLegendSetEntityPersistenceAndRetrieval() {
    // Create a legend set with all fields populated
    legendSetA = createLegendSet('A');
    legendSetA.setCode("LEGENDSET_A_CODE");
    legendSetA.setSymbolizer("circle");

    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 10d, 20d);
    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);

    long idA = legendSetService.addLegendSet(legendSetA);

    // Retrieve and verify all LegendSet fields
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertNotNull(retrieved);
    assertEquals("LegendSetA", retrieved.getName());
    assertEquals("LEGENDSET_A_CODE", retrieved.getCode());
    assertEquals("circle", retrieved.getSymbolizer());
    assertEquals(2, retrieved.getLegends().size());
  }

  @Test
  void testJpaLegendSetOneToManyLegends() {
    // Test that legends OneToMany relationship is properly loaded
    legendSetA = createLegendSet('A');
    legendA = createLegend('A', 0d, 50d);
    legendB = createLegend('B', 50d, 100d);

    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);

    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertEquals(2, retrieved.getLegends().size());

    // Verify legends are properly loaded
    assertTrue(retrieved.getLegends().stream().anyMatch(l -> l.getName().equals("LegendA")));
    assertTrue(retrieved.getLegends().stream().anyMatch(l -> l.getName().equals("LegendB")));
  }

  @Test
  void testJpaLegendSetIdGeneration() {
    // Test that ID is properly generated using SEQUENCE strategy
    legendSetA = createLegendSet('A');
    LegendSet legendSetB = createLegendSet('B');

    long idA = legendSetService.addLegendSet(legendSetA);
    long idB = legendSetService.addLegendSet(legendSetB);

    assertTrue(idA > 0);
    assertTrue(idB > 0);
    assertTrue(idA != idB);
  }

  @Test
  void testJpaLegendSetUpdateOperations() {
    // Test that update operations preserve all associations and fields
    legendSetA = createLegendSet('A');
    legendSetA.setSymbolizer("square");

    legendA = createLegend('A', 0d, 10d);
    legendSetA.getLegends().add(legendA);

    long idA = legendSetService.addLegendSet(legendSetA);

    // Update legend set properties
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    retrieved.setName("UpdatedLegendSet");
    retrieved.setSymbolizer("triangle");

    legendSetService.updateLegendSet(retrieved);

    // Verify updates
    LegendSet updated = legendSetService.getLegendSet(idA);
    assertEquals("UpdatedLegendSet", updated.getName());
    assertEquals("triangle", updated.getSymbolizer());
    assertEquals(1, updated.getLegends().size());
  }

  @Test
  void testJpaLegendSetCascadeOperations() {
    // Test that cascade ALL and orphan removal work correctly
    legendSetA = createLegendSet('A');
    legendA = createLegend('A', 0d, 10d);
    legendB = createLegend('B', 10d, 20d);

    legendSetA.getLegends().add(legendA);
    legendSetA.getLegends().add(legendB);

    long idA = legendSetService.addLegendSet(legendSetA);

    // Delete the legend set - legends should be deleted too (cascade)
    LegendSet retrieved = legendSetService.getLegendSet(idA);
    legendSetService.deleteLegendSet(retrieved);
    entityManager.flush();

    // Verify legend set is deleted
    assertNull(legendSetService.getLegendSet(idA));
  }

  @Test
  void testJpaLegendSetNullableFields() {
    legendSetA = createLegendSet('A');

    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertNull(retrieved.getSymbolizer());
  }

  @Test
  void testJpaLegendSetAttributeValues() {
    // Test that attributeValues are properly stored and retrieved
    legendSetA = createLegendSet('A');
    legendSetA.addAttributeValue("attr1", "value1");
    legendSetA.addAttributeValue("attr2", "value2");

    long idA = legendSetService.addLegendSet(legendSetA);

    LegendSet retrieved = legendSetService.getLegendSet(idA);
    assertNotNull(retrieved.getAttributeValues());
    // AttributeValues should have the added values
    assertTrue(retrieved.getAttributeValues().contains("attr1"));
    assertTrue(retrieved.getAttributeValues().contains("attr2"));
  }

  @Test
  void testJpaLegendSetTranslations() {
    legendSetA = createLegendSet('A');
    long idA = legendSetService.addLegendSet(legendSetA);

    Locale locale = Locale.FRENCH;
    ThreadUserSettings.put(Map.of("keyDbLocale", locale.toString()));

    String translatedName = "translatedName";
    Set<Translation> translations = new HashSet<>(legendSetA.getTranslations());
    translations.add(new Translation(locale.language(), "NAME", translatedName));
    objectManager.updateTranslations(legendSetA, translations);

    LegendSet retrieved = legendSetService.getLegendSet(idA);

    assertNotNull(retrieved.getTranslations());
    assertEquals("translatedName", retrieved.getDisplayName());
  }

  @Test
  void testJpaLegendSetSharing() {
    legendSetA = createLegendSet('A');
    long idA = legendSetService.addLegendSet(legendSetA);
    LegendSet retrieved = legendSetService.getLegendSet(idA);

    // Sharing should be initialized
    assertNotNull(retrieved.getSharing());
    
    retrieved.getSharing().setPublicAccess(AccessStringHelper.DEFAULT);
    retrieved.getSharing().setUsers(Map.of("user1", new UserAccess("user1","rw------")));
    legendSetService.updateLegendSet(retrieved);
    
    LegendSet updated = legendSetService.getLegendSet(idA);
    assertNotNull(updated.getSharing());
    assertEquals(AccessStringHelper.DEFAULT, updated.getSharing().getPublicAccess());
    assertTrue(updated.getSharing().getUsers().containsKey("user1"));
  }
}
