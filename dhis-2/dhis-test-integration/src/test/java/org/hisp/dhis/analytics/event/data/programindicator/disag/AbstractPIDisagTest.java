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

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryOptionMapping;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jim Grace
 */
abstract class AbstractPIDisagTest extends PostgresIntegrationTestBase {

  @Autowired protected IdentifiableObjectManager manager;

  protected CategoryOption optionA;
  protected CategoryOption optionB;
  protected CategoryOption optionC;
  protected CategoryOption optionD;
  protected CategoryOption optionE;
  protected CategoryOption optionF;
  protected CategoryOption optionG;

  protected CategoryOptionCombo cocAC;
  protected CategoryOptionCombo cocAD;
  protected CategoryOptionCombo cocBC;
  protected CategoryOptionCombo cocBD;
  protected CategoryOptionCombo cocE;
  protected CategoryOptionCombo cocF;

  protected Category category1;
  protected Category category2;
  protected Category category3;

  protected CategoryCombo catComboA;
  protected CategoryCombo catComboB;

  protected ProgramCategoryOptionMapping omA;
  protected ProgramCategoryOptionMapping omB;
  protected ProgramCategoryOptionMapping omC;
  protected ProgramCategoryOptionMapping omD;
  protected ProgramCategoryOptionMapping omE;
  protected ProgramCategoryOptionMapping omF;

  protected List<ProgramCategoryOptionMapping> omList1;
  protected List<ProgramCategoryOptionMapping> omList2;
  protected List<ProgramCategoryOptionMapping> omList3;

  protected ProgramCategoryMapping cm1;
  protected ProgramCategoryMapping cm2;
  protected ProgramCategoryMapping cm3;

  protected Set<ProgramCategoryMapping> categoryMappings;
  protected Set<String> categoryMappingIds;

  protected static final String CM1_ID = "iOChed1vei4";
  protected static final String CM2_ID = "cwohgheSe3t";
  protected static final String CM3_ID = "Ttaesahgo2W";

  protected static final String OUTPUT_DE = "OutputDeUid";

  protected Program program;

  protected ProgramIndicator programIndicator;

  protected EventQueryParams eventQueryParams;

  protected void setUp() {

    // Category options

    optionA = createCategoryOption('A');
    optionB = createCategoryOption('B');
    optionC = createCategoryOption('C');
    optionD = createCategoryOption('D');
    optionE = createCategoryOption('E');
    optionF = createCategoryOption('F');
    optionG = createCategoryOption('G');

    optionA.setUid("catOption0A");
    optionB.setUid("catOption0B");
    optionC.setUid("catOption0C");
    optionD.setUid("catOption0D");
    optionE.setUid("catOption0E");
    optionF.setUid("catOption0F");
    optionG.setUid("catOption0G");

    manager.save(optionA);
    manager.save(optionB);
    manager.save(optionC);
    manager.save(optionD);
    manager.save(optionE);
    manager.save(optionF);
    manager.save(optionG);

    // Categories

    category1 = createCategory('1', optionA, optionB);
    category2 = createCategory('2', optionC, optionD);
    category3 = createCategory('3', optionE, optionF);

    category1.setUid("CategoryId1");
    category2.setUid("CategoryId2");
    category3.setUid("CategoryId3");

    manager.save(category1);
    manager.save(category2);
    manager.save(category3);

    // CategoryCombos

    catComboA = createCategoryCombo('A', category1, category2);
    catComboB = createCategoryCombo('B', category3);

    manager.save(catComboA);
    manager.save(catComboB);

    // CategoryOptionCombos

    cocAC = createCategoryOptionCombo("cocAC", "cocAC678901", catComboA, optionA, optionC);
    cocAD = createCategoryOptionCombo("cocAD", "cocAD678901", catComboA, optionA, optionD);
    cocBC = createCategoryOptionCombo("cocBC", "cocBC678901", catComboA, optionB, optionC);
    cocBD = createCategoryOptionCombo("cocBC", "cocBD678901", catComboA, optionB, optionD);
    cocE = createCategoryOptionCombo("cocE", "cocE5678901", catComboB, optionE);
    cocF = createCategoryOptionCombo("cocE", "cocF5678901", catComboB, optionF);

    manager.save(cocAC);
    manager.save(cocAD);
    manager.save(cocBC);
    manager.save(cocBD);
    manager.save(cocE);
    manager.save(cocF);

    catComboA.setOptionCombos(Set.of(cocAC, cocAD, cocBC, cocBD));
    catComboB.setOptionCombos(Set.of(cocE, cocF));

    manager.save(catComboA);
    manager.save(catComboB);

    // ProgramCategoryOptionMappings

    omA =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionA.getUid())
            .filter("'filterA'==''")
            .build();
    omB =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionB.getUid())
            .filter("'filterB'==''")
            .build();
    omC =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionC.getUid())
            .filter("'filterC'==''")
            .build();
    omD =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionD.getUid())
            .filter("'filterD'==''")
            .build();
    omE =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionE.getUid())
            .filter("'filterE'==''")
            .build();
    omF =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionF.getUid())
            .filter("'filterF'==''")
            .build();

    // List<ProgramCategoryOptionMapping>

    omList1 = List.of(omA, omB);
    omList2 = List.of(omC, omD);
    omList3 = List.of(omE, omF);

    // ProgramCategoryMapping

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

    // Program

    program = createProgram('P');
    program.setCategoryMappings(Set.of(cm1, cm2, cm3));
    manager.save(program);

    // ProgramIndicator

    programIndicator = createProgramIndicator('A', program, "42", "true");
    programIndicator.setCategoryCombo(catComboA);
    programIndicator.setAttributeCombo(catComboB);
    programIndicator.setCategoryMappingIds(Set.of(CM1_ID, CM2_ID, CM3_ID));
    programIndicator.setAggregateExportDataElement(OUTPUT_DE);
    manager.save(programIndicator);

    // EventQueryParams

    eventQueryParams =
        new EventQueryParams.Builder()
            .withProgramIndicator(programIndicator)
            .addDimension(
                new BaseDimensionalObject(
                    DATA_X_DIM_ID, DimensionType.DATA_X, List.of(programIndicator)))
            .addDimension(category2)
            .withStartDate(new Date())
            .withEndDate(new Date())
            .build();
  }
}
