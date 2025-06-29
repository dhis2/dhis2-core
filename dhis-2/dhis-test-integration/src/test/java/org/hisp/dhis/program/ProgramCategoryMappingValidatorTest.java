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
package org.hisp.dhis.program;

import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataelement.DataElementDomain.TRACKER;
import static org.hisp.dhis.feedback.ErrorCode.E4071;
import static org.hisp.dhis.feedback.ErrorCode.E4072;
import static org.hisp.dhis.feedback.ErrorCode.E4073;
import static org.hisp.dhis.feedback.ErrorCode.E4074;
import static org.hisp.dhis.feedback.ErrorCode.E4079;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
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
class ProgramCategoryMappingValidatorTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  private ProgramCategoryMappingValidator target;

  private CategoryOption optionA;
  private CategoryOption optionB;
  private CategoryOption optionC;
  private CategoryOption optionD;
  private CategoryOption optionE;
  private CategoryOption optionF;

  private Category category1;
  private Category category2;
  private Category category3;

  private CategoryCombo catComboA;
  private CategoryCombo catComboB;

  private DataElement dataElement;

  private ProgramCategoryOptionMapping omA;
  private ProgramCategoryOptionMapping omADuplicate;
  private ProgramCategoryOptionMapping omB;
  private ProgramCategoryOptionMapping omC;
  private ProgramCategoryOptionMapping omD;
  private ProgramCategoryOptionMapping omE;
  private ProgramCategoryOptionMapping omF;

  private List<ProgramCategoryOptionMapping> omList1;
  private List<ProgramCategoryOptionMapping> omList2;
  private List<ProgramCategoryOptionMapping> omList3;

  private ProgramCategoryMapping cm1;
  private ProgramCategoryMapping cm2;
  private ProgramCategoryMapping cm3;

  private Set<ProgramCategoryMapping> categoryMappings;
  private Set<String> categoryMappingIds;

  private static final String CM1_ID = "catMapping1";
  private static final String CM2_ID = "catMapping2";
  private static final String CM3_ID = "catMapping3";

  @BeforeAll
  void setup() {
    target = new ProgramCategoryMappingValidator(manager);

    optionA = createCategoryOption('A');
    optionB = createCategoryOption('B');
    optionC = createCategoryOption('C');
    optionD = createCategoryOption('D');
    optionE = createCategoryOption('E');
    optionF = createCategoryOption('F');

    optionA.setUid("cataOptionA");
    optionB.setUid("cataOptionB");
    optionC.setUid("cataOptionC");
    optionD.setUid("cataOptionD");
    optionE.setUid("cataOptionE");
    optionF.setUid("cataOptionF");

    manager.save(optionA);
    manager.save(optionB);
    manager.save(optionC);
    manager.save(optionD);
    manager.save(optionE);
    manager.save(optionF);

    category1 = createCategory('1', optionA, optionB);
    category2 = createCategory('2', optionC, optionD);
    category3 = createCategory('3', optionE, optionF);

    category1.setUid("categoryId1");
    category2.setUid("categoryId2");
    category3.setUid("categoryId3");

    manager.save(category1);
    manager.save(category2);
    manager.save(category3);

    catComboA = createCategoryCombo('A', category1, category2);
    catComboB = createCategoryCombo('B', category3);

    catComboA.setUid("catagComboA");
    catComboB.setUid("catagComboB");

    manager.save(catComboA);
    manager.save(catComboB);

    dataElement = createDataElement('A', NUMBER, SUM, TRACKER);
    manager.save(dataElement);

    Program program = createProgram('A');
    manager.save(program);

    ProgramStage programStage = createProgramStage('A', Set.of(dataElement));
    programStage.setProgram(program);
    manager.save(programStage);

    String de = "#{" + programStage.getUid() + "." + dataElement.getUid() + "}";

    omA =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionA.getUid())
            .filter(de + " > 1")
            .build();
    omADuplicate =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionA.getUid())
            .filter(de + " > 100")
            .build();
    omB =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionB.getUid())
            .filter(de + " > 2")
            .build();
    omC =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionC.getUid())
            .filter(de + " > 3")
            .build();
    omD =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionD.getUid())
            .filter(de + " > 4")
            .build();
    omE =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionE.getUid())
            .filter(de + " > 5")
            .build();
    omF =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionF.getUid())
            .filter(de + " > 6")
            .build();

    omList1 = List.of(omA, omB);
    omList2 = List.of(omC, omD);
    omList3 = List.of(omE, omF);

    cm1 =
        ProgramCategoryMapping.builder()
            .id(CM1_ID)
            .categoryId(category1.getUid())
            .mappingName("Mapping 1")
            .optionMappings(omList1)
            .build();
    cm2 =
        ProgramCategoryMapping.builder()
            .id(CM2_ID)
            .categoryId(category2.getUid())
            .mappingName("Mapping 2")
            .optionMappings(omList2)
            .build();
    cm3 =
        ProgramCategoryMapping.builder()
            .id(CM3_ID)
            .categoryId(category3.getUid())
            .mappingName("Mapping 3")
            .optionMappings(omList3)
            .build();

    categoryMappings = Set.of(cm1, cm2, cm3);

    categoryMappingIds = Set.of(CM1_ID, CM2_ID, CM3_ID);
  }

  @Test
  void testResolveProgramCategoryMappings() {
    // Given
    Program program = createProgram(categoryMappings);

    // Then
    assertDoesNotThrow(() -> target.validateProgramCategoryMappings(program));
  }

  @Test
  void testResolveProgramCategoryMappingsBadCategoryId() {
    // Given
    ProgramCategoryMapping cmBad = cm1.toBuilder().categoryId("x2345678901").build();
    Program program = createProgram(Set.of(cmBad, cm2, cm3));

    // When throws
    ConflictException thrown =
        assertThrows(
            ConflictException.class, () -> target.validateProgramCategoryMappings(program));

    // Then
    assertEquals(E4072, thrown.getCode());
    assertEquals(
        "Program `programUidA` category mapping `catMapping1` category `x2345678901` not found",
        thrown.getMessage());
  }

  @Test
  void testResolveProgramCategoryMappingsBadOptionId() {
    // Given
    ProgramCategoryOptionMapping omBad = omA.toBuilder().optionId("x2345678901").build();
    ProgramCategoryMapping cmBad = cm1.toBuilder().optionMappings(List.of(omBad, omB)).build();
    Program program = createProgram(Set.of(cmBad, cm2, cm3));

    // When throws
    ConflictException thrown =
        assertThrows(
            ConflictException.class, () -> target.validateProgramCategoryMappings(program));

    // Then
    assertEquals(E4073, thrown.getCode());
    assertEquals(
        "Program `programUidA` category `categoryId1` mapping `catMapping1` option `x2345678901` not found",
        thrown.getMessage());
  }

  @Test
  void testResolveProgramCategoryMappingsDuplicateOptionId() {
    // Given
    ProgramCategoryMapping cmDuplicate =
        cm1.toBuilder().optionMappings(List.of(omA, omADuplicate)).build();
    Program program = createProgram(Set.of(cmDuplicate, cm2, cm3));

    // When throws
    ConflictException thrown =
        assertThrows(
            ConflictException.class, () -> target.validateProgramCategoryMappings(program));

    // Then
    assertEquals(E4079, thrown.getCode());
    assertEquals(
        "Program `programUidA` category mapping `catMapping1` has multiple option mappings for Category Option `cataOptionA`",
        thrown.getMessage());
  }

  @Test
  void testResolveProgramIndicatorCategoryMappings() throws ConflictException {
    // Given
    Program program = createProgram(categoryMappings);
    ProgramIndicator programIndicator = createProgramIndicator(program, categoryMappingIds);

    // When
    Set<ProgramCategoryMapping> result =
        target.getAndValidateProgramIndicatorCategoryMappings(programIndicator);

    // Then
    assertContainsOnly(categoryMappings, result);
  }

  @Test
  void testResolveProgramIndicatorCategoryMappingsBadMappingId() {
    // Given
    Program program = createProgram(categoryMappings);
    ProgramIndicator programIndicator =
        createProgramIndicator(program, Set.of("NoMappingId", CM2_ID, CM3_ID));

    // When throws
    ConflictException thrown =
        assertThrows(
            ConflictException.class,
            () -> target.getAndValidateProgramIndicatorCategoryMappings(programIndicator));

    // Then
    assertEquals(E4071, thrown.getCode());
    assertEquals(
        "Program indicator `programIndA` category mapping `NoMappingId` not found in program `programUidA`",
        thrown.getMessage());
  }

  @Test
  void testResolveProgramIndicatorCategoryMappingsMissingMapping() {
    // Given
    Program program = createProgram(Set.of(cm2, cm3));
    ProgramIndicator programIndicator = createProgramIndicator(program, Set.of(CM2_ID, CM3_ID));

    // When throws
    ConflictException thrown =
        assertThrows(
            ConflictException.class,
            () -> target.getAndValidateProgramIndicatorCategoryMappings(programIndicator));

    // Then
    assertEquals(E4074, thrown.getCode());
    assertEquals(
        "Program indicator `programIndA` disaggregation category `categoryId1` needs a category mapping in the program",
        thrown.getMessage());
  }

  /** Creates a program with a set of category mappings. */
  private Program createProgram(Set<ProgramCategoryMapping> categoryMappings) {
    Program program = createProgram('A');
    program.setUid("programUidA");
    program.setCategoryMappings(categoryMappings);

    return program;
  }

  /** Creates a program with a set of category mapping references. */
  private ProgramIndicator createProgramIndicator(Program program, Set<String> mappingIds) {
    ProgramIndicator programIndicator =
        createProgramIndicator('A', program, "dummy expression", "dummy filter");
    programIndicator.setUid("programIndA");
    programIndicator.setCategoryCombo(catComboA);
    programIndicator.setAttributeCombo(catComboB);
    programIndicator.setCategoryMappingIds(mappingIds);

    return programIndicator;
  }
}
