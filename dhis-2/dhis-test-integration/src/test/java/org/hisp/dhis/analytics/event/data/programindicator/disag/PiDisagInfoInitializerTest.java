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
package org.hisp.dhis.analytics.event.data.programindicator.disag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryMappingValidator;
import org.hisp.dhis.program.ProgramIndicator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jim Grace
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class PIDisagInfoInitializerTest extends AbstractPIDisagTest {

  @Autowired private ProgramCategoryMappingValidator mappingValidator;

  private PiDisagInfoInitializer target;

  @Override
  @BeforeAll
  protected void setUp() {
    super.setUp();
    target = new PiDisagInfoInitializer(mappingValidator);
  }

  @Test
  void testGetParamsWithDisaggregationInfo() {

    // Given
    assertFalse(eventQueryParams.hasPiDisagInfo());

    // When
    EventQueryParams params = target.getParamsWithDisaggregationInfo(eventQueryParams);

    // Then
    assertNotSame(eventQueryParams, params);
    assertTrue(params.hasPiDisagInfo());
  }

  @Test
  void testGetParamsWithDisaggregationInfoWithoutProgramIndicator() {

    // Given
    EventQueryParams testParams = new EventQueryParams.Builder().build();
    assertFalse(eventQueryParams.hasPiDisagInfo());

    // When
    EventQueryParams params = target.getParamsWithDisaggregationInfo(testParams);

    // Then
    assertSame(testParams, params);
    assertFalse(params.hasPiDisagInfo());
  }

  @Test
  void testGetParamsWithDisaggregationInfoWithDefaultCategoryCombos() {

    // Given
    ProgramIndicator pi = createProgramIndicator('A', program, "42", "");
    EventQueryParams testParams = new EventQueryParams.Builder().withProgramIndicator(pi).build();
    assertFalse(eventQueryParams.hasPiDisagInfo());

    // When
    EventQueryParams params = target.getParamsWithDisaggregationInfo(testParams);

    // Then
    assertSame(testParams, params);
    assertFalse(params.hasPiDisagInfo());
  }

  @Test
  void testDisaggregatedCategoriesAreNotNeededWhenNot() {
    // Given
    ProgramIndicator pi = createProgramIndicator('A', program, "42", "");
    EventQueryParams params = new EventQueryParams.Builder().withProgramIndicator(pi).build();

    // Then
    assertTrue(target.disaggregatedCategoriesAreNotNeeded(params));
  }

  @Test
  void testDisaggregatedCategoriesAreNotNeededWithCategories() {
    // Given
    ProgramIndicator pi = createProgramIndicator('A', program, "42", "");
    EventQueryParams params =
        new EventQueryParams.Builder().withProgramIndicator(pi).addDimension(category1).build();

    // Then
    assertFalse(target.disaggregatedCategoriesAreNotNeeded(params));
  }

  @Test
  void testDisaggregatedCategoriesAreNotNeededWithCoc() {
    // Given
    ProgramIndicator pi = createProgramIndicator('A', program, "42", "");
    pi.setCategoryCombo(catComboA);
    EventQueryParams params = new EventQueryParams.Builder().withProgramIndicator(pi).build();

    // Then
    assertFalse(target.disaggregatedCategoriesAreNotNeeded(params));
  }

  @Test
  void testDisaggregatedCategoriesAreNotNeededWithAoc() {
    // Given
    ProgramIndicator pi = createProgramIndicator('A', program, "42", "");
    pi.setAttributeCombo(catComboB);
    EventQueryParams params = new EventQueryParams.Builder().withProgramIndicator(pi).build();

    // Then
    assertFalse(target.disaggregatedCategoriesAreNotNeeded(params));
  }

  @Test
  void testGetPiDisagInfo() {
    // Given
    Set<String> expectedDimensionCategories = Set.of(category2.getUid());

    Set<String> expectedCocCategories = Set.of(category1.getUid(), category3.getUid());

    Map<String, ProgramCategoryMapping> expectedCategoryMappings =
        Map.of("CategoryId1", cm1, "CategoryId2", cm2, "CategoryId3", cm3);

    Map<String, String> expectedCocResolver =
        Map.of(
            "catOption0AcatOption0C", "cocAC678901",
            "catOption0AcatOption0D", "cocAD678901",
            "catOption0BcatOption0C", "cocBC678901",
            "catOption0BcatOption0D", "cocBD678901");

    Map<String, String> expectedAocResolver =
        Map.of(
            "catOption0E", "cocE5678901",
            "catOption0F", "cocF5678901");

    // When
    PiDisagInfo info = target.getPiDisagInfo(eventQueryParams);

    // Then
    assertNotNull(info);

    assertEquals(expectedDimensionCategories, info.getDimensionCategories());
    assertEquals(expectedCocCategories, new HashSet<>(info.getCocCategories())); // In any order
    assertEquals(expectedCategoryMappings, info.getCategoryMappings());
    assertEquals(expectedCocResolver, info.getCocResolver());
    assertEquals(expectedAocResolver, info.getAocResolver());
  }

  @Test
  void testGetCocCategories() {
    // Given
    Set<String> expectedCocCategories = Set.of(category1.getUid(), category3.getUid());

    // When
    List<String> result = target.getCocCategories(eventQueryParams);

    assertEquals(expectedCocCategories, new HashSet<>(result)); // In any order
  }

  @Test
  void testGetResolver() {
    // Given
    Map<String, String> expectedResolver =
        Map.of(
            "catOption0AcatOption0C", "cocAC678901",
            "catOption0AcatOption0D", "cocAD678901",
            "catOption0BcatOption0C", "cocBC678901",
            "catOption0BcatOption0D", "cocBD678901");

    // When
    Map<String, String> result = target.getResolver(catComboA);

    // Then
    assertEquals(expectedResolver, result);
  }
}
