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

import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.DimensionConstants.DATA_X_DIM_ID;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.analytics.DataQueryParams;
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
abstract class AbstractPiDisagTest extends PostgresIntegrationTestBase {

  @Autowired protected IdentifiableObjectManager manager;

  protected CategoryOption optionA;
  protected CategoryOption optionB;
  protected CategoryOption optionC;
  protected CategoryOption optionD;
  protected CategoryOption optionE;
  protected CategoryOption optionF;
  protected CategoryOption optionG;
  protected CategoryOption optionH;

  protected CategoryOptionCombo cocAC;
  protected CategoryOptionCombo cocAD;
  protected CategoryOptionCombo cocBC;
  protected CategoryOptionCombo cocBD;
  protected CategoryOptionCombo cocEG;
  protected CategoryOptionCombo cocEH;
  protected CategoryOptionCombo cocFG;
  protected CategoryOptionCombo cocFH;

  protected Category category1;
  protected Category category2;
  protected Category category3;
  protected Category category4;

  protected CategoryCombo catComboA;
  protected CategoryCombo catComboB;

  protected ProgramCategoryOptionMapping omA;
  protected ProgramCategoryOptionMapping omB;
  protected ProgramCategoryOptionMapping omC;
  protected ProgramCategoryOptionMapping omD;
  protected ProgramCategoryOptionMapping omE;
  protected ProgramCategoryOptionMapping omF;
  protected ProgramCategoryOptionMapping omG;
  protected ProgramCategoryOptionMapping omH;

  protected List<ProgramCategoryOptionMapping> omList1;
  protected List<ProgramCategoryOptionMapping> omList2;
  protected List<ProgramCategoryOptionMapping> omList3;
  protected List<ProgramCategoryOptionMapping> omList4;

  protected ProgramCategoryMapping cm1;
  protected ProgramCategoryMapping cm2;
  protected ProgramCategoryMapping cm3;
  protected ProgramCategoryMapping cm4;

  protected Set<ProgramCategoryMapping> categoryMappings;
  protected Set<String> categoryMappingIds;

  protected static final String CM1_ID = "iOChed1vei4";
  protected static final String CM2_ID = "cwohgheSe3t";
  protected static final String CM3_ID = "Ttaesahgo2W";
  protected static final String CM4_ID = "dOoTh1an6Oh";

  protected static final String OUTPUT_DE = "OutputDeUid";

  protected Program program;

  protected ProgramIndicator programIndicator;

  /** Params without data value set and with a category dimension */
  protected EventQueryParams nonDataValueSetEventQueryParams;

  /** Params with data value set and with a category dimension */
  protected EventQueryParams dataValueSetEventQueryParams;

  /** Params with data value set and without a category dimension */
  protected EventQueryParams nonDimensionEventQueryParams;

  protected void setUp() {

    // Category options

    optionA = createCategoryOption('A');
    optionB = createCategoryOption('B');
    optionC = createCategoryOption('C');
    optionD = createCategoryOption('D');
    optionE = createCategoryOption('E');
    optionF = createCategoryOption('F');
    optionG = createCategoryOption('G');
    optionH = createCategoryOption('H');

    optionA.setUid("catOption0A");
    optionB.setUid("catOption0B");
    optionC.setUid("catOption0C");
    optionD.setUid("catOption0D");
    optionE.setUid("catOption0E");
    optionF.setUid("catOption0F");
    optionG.setUid("catOption0G");
    optionH.setUid("catOption0H");

    manager.save(optionA);
    manager.save(optionB);
    manager.save(optionC);
    manager.save(optionD);
    manager.save(optionE);
    manager.save(optionF);
    manager.save(optionG);
    manager.save(optionH);

    // Categories

    category1 = createCategory('1', optionA, optionB);
    category2 = createCategory('2', optionC, optionD);
    category3 = createCategory('3', optionE, optionF);
    category4 = createCategory('4', optionG, optionH);

    category1.setUid("CategoryId1");
    category2.setUid("CategoryId2");
    category3.setUid("CategoryId3");
    category4.setUid("CategoryId4");

    manager.save(category1);
    manager.save(category2);
    manager.save(category3);
    manager.save(category4);

    // CategoryCombos

    catComboA = createCategoryCombo('A', category1, category2);
    catComboB = createCategoryCombo('B', category3, category4);

    manager.save(catComboA);
    manager.save(catComboB);

    // CategoryOptionCombos

    cocAC = createCategoryOptionCombo("cocAC", "cocAC678901", catComboA, optionA, optionC);
    cocAD = createCategoryOptionCombo("cocAD", "cocAD678901", catComboA, optionA, optionD);
    cocBC = createCategoryOptionCombo("cocBC", "cocBC678901", catComboA, optionB, optionC);
    cocBD = createCategoryOptionCombo("cocBC", "cocBD678901", catComboA, optionB, optionD);
    cocEG = createCategoryOptionCombo("cocE", "cocEG678901", catComboB, optionE, optionG);
    cocEH = createCategoryOptionCombo("cocE", "cocEH678901", catComboB, optionE, optionH);
    cocFG = createCategoryOptionCombo("cocE", "cocFG678901", catComboB, optionF, optionG);
    cocFH = createCategoryOptionCombo("cocE", "cocFH678901", catComboB, optionF, optionH);

    manager.save(cocAC);
    manager.save(cocAD);
    manager.save(cocBC);
    manager.save(cocBD);
    manager.save(cocEG);
    manager.save(cocEH);
    manager.save(cocFG);
    manager.save(cocFH);

    catComboA.setOptionCombos(Set.of(cocAC, cocAD, cocBC, cocBD));
    catComboB.setOptionCombos(Set.of(cocEG, cocEH, cocFG, cocFH));

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
    omG =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionG.getUid())
            .filter("'filterG'==''")
            .build();
    omH =
        ProgramCategoryOptionMapping.builder()
            .optionId(optionH.getUid())
            .filter("'filterH'==''")
            .build();

    // List<ProgramCategoryOptionMapping>

    omList1 = List.of(omA, omB);
    omList2 = List.of(omC, omD);
    omList3 = List.of(omE, omF);
    omList4 = List.of(omG, omH);

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

    cm4 =
        ProgramCategoryMapping.builder()
            .id(CM4_ID)
            .categoryId(category4.getUid())
            .mappingName("Mapping 4")
            .optionMappings(omList4)
            .build();

    // Program

    program = createProgram('P');
    program.setCategoryMappings(Set.of(cm1, cm2, cm3, cm4));
    manager.save(program);

    // ProgramIndicator

    programIndicator = createProgramIndicator('A', program, "42", "true");
    programIndicator.setCategoryCombo(catComboA);
    programIndicator.setAttributeCombo(catComboB);
    programIndicator.setCategoryMappingIds(Set.of(CM1_ID, CM2_ID, CM3_ID, CM4_ID));
    programIndicator.setAggregateExportDataElement(OUTPUT_DE);
    manager.save(programIndicator);

    // EventQueryParams

    DataQueryParams dataQueryParams =
        DataQueryParams.newBuilder().withOutputFormat(DATA_VALUE_SET).build();

    nonDataValueSetEventQueryParams = withParamsFields(new EventQueryParams.Builder());
    dataValueSetEventQueryParams = withParamsFields(new EventQueryParams.Builder(dataQueryParams));
    nonDimensionEventQueryParams =
        new EventQueryParams.Builder(dataValueSetEventQueryParams)
            .removeDimension(category2.getDimension())
            .build();
  }

  private EventQueryParams withParamsFields(EventQueryParams.Builder builder) {
    return builder
        .withProgramIndicator(programIndicator)
        .addDimension(
            new BaseDimensionalObject(
                DATA_X_DIM_ID, DimensionType.DATA_X, List.of(programIndicator)))
        .addDimension(
            new BaseDimensionalObject(
                category2.getDimension(), DimensionType.CATEGORY, List.of(optionC)))
        .addFilter(
            new BaseDimensionalObject(
                category4.getDimension(), DimensionType.CATEGORY, List.of(optionG)))
        .withStartDate(new Date())
        .withEndDate(new Date())
        .build();
  }
}
