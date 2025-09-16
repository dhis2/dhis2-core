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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeDataElements;
import static org.hisp.dhis.analytics.common.AnalyticsDimensionsTestSupport.allValueTypeTEAs;
import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.aggregateAllowedValueTypesPredicate;
import static org.hisp.dhis.analytics.common.DimensionServiceCommonTest.queryDisallowedValueTypesPredicate;
import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.SystemUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventAnalyticsDimensionsServiceTest {
  private EventAnalyticsDimensionsService eventAnalyticsDimensionsService;

  private ProgramService programService;
  private ProgramStageService programStageService;
  private CategoryService categoryService;
  private Program program;
  private ProgramStage programStage;

  private static final String PROGRAM_UID = "aProgramUid";

  @BeforeEach
  void setup() {
    injectSecurityContextNoSettings(new SystemUser());

    programService = mock(ProgramService.class);
    programStageService = mock(ProgramStageService.class);
    categoryService = mock(CategoryService.class);

    program = mock(Program.class);
    programStage = mock(ProgramStage.class);

    when(programService.getProgram(any())).thenReturn(program);
    when(program.getUid()).thenReturn(PROGRAM_UID);
    when(programStageService.getProgramStage(any())).thenReturn(programStage);
    when(programStage.getProgram()).thenReturn(program);
    when(program.getDataElements()).thenReturn(allValueTypeDataElements());
    when(program.getProgramIndicators()).thenReturn(Collections.emptySet());
    when(program.getTrackedEntityAttributes()).thenReturn(allValueTypeTEAs());

    eventAnalyticsDimensionsService =
        new DefaultEventAnalyticsDimensionsService(
            programStageService, programService, categoryService, mock(AclService.class));
  }

  @Test
  void testQueryDoesntContainDisallowedValueTypes() {
    List<IdentifiableObject> analyticsDimensions =
        eventAnalyticsDimensionsService
            .getQueryDimensionsByProgramStageId(PROGRAM_UID, "anUid")
            .stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof DataElement)
            .map(de -> ((DataElement) de).getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .noneMatch(queryDisallowedValueTypesPredicate()));
  }

  @Test
  void testAggregateOnlyContainsAllowedValueTypes() {
    List<IdentifiableObject> analyticsDimensions =
        eventAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId("anUid").stream()
            .map(PrefixedDimension::getItem)
            .toList();

    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof DataElement)
            .map(de -> ((DataElement) de).getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
    assertTrue(
        analyticsDimensions.stream()
            .filter(b -> b instanceof TrackedEntityAttribute)
            .map(tea -> ((TrackedEntityAttribute) tea).getValueType())
            .allMatch(aggregateAllowedValueTypesPredicate()));
  }

  @Test
  void testAggregateIncludesCategoriesAndAttributeGroupSetsWhenNonDefaultCategoryCombo() {
    // Given a program with non-default category combo
    when(program.hasNonDefaultCategoryCombo()).thenReturn(true);

    // Categories linked to the program's category combo
    Category categoryA = new Category();
    categoryA.setUid("CatA");
    Category categoryB = new Category();
    categoryB.setUid("CatB");

    CategoryCombo categoryCombo = new CategoryCombo();
    categoryCombo.setCategories(List.of(categoryA, categoryB));
    when(program.getCategoryCombo()).thenReturn(categoryCombo);

    // Only attribute type group sets should be included
    CategoryOptionGroupSet attrGogs =
        new CategoryOptionGroupSet("AttrGOGS", org.hisp.dhis.common.DataDimensionType.ATTRIBUTE);
    attrGogs.setUid("COGS_ATTR");
    CategoryOptionGroupSet disaggGogs =
        new CategoryOptionGroupSet(
            "DisGOGS", org.hisp.dhis.common.DataDimensionType.DISAGGREGATION);
    disaggGogs.setUid("COGS_DIS");
    when(categoryService.getAllCategoryOptionGroupSets()).thenReturn(List.of(attrGogs, disaggGogs));

    // When
    List<IdentifiableObject> items =
        eventAnalyticsDimensionsService.getAggregateDimensionsByProgramStageId("stage1").stream()
            .map(PrefixedDimension::getItem)
            .toList();

    // Then: categories are included
    List<String> categoryUids =
        items.stream().filter(i -> i instanceof Category).map(IdentifiableObject::getUid).toList();
    assertTrue(categoryUids.containsAll(List.of("CatA", "CatB")));

    // And: only attribute COGS are included (DISAGGREGATION filtered out)
    List<String> cogsUids =
        items.stream()
            .filter(i -> i instanceof CategoryOptionGroupSet)
            .map(IdentifiableObject::getUid)
            .toList();
    assertTrue(cogsUids.contains("COGS_ATTR"));
    assertFalse(cogsUids.contains("COGS_DIS"));
  }
}
