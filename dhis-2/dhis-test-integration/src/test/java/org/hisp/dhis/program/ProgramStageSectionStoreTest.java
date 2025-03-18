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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Transactional
class ProgramStageSectionStoreTest extends PostgresIntegrationTestBase {

  @Autowired private ProgramStageSectionStore programStageSectionStore;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit organisationUnit;

  private ProgramStage stageA;

  private ProgramStage stageB;

  private ProgramStageSection sectionA;

  private ProgramStageSection sectionB;

  private List<DataElement> dataElements;

  @BeforeEach
  void setUp() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    Program program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
    stageA = createProgramStage('A', program);
    programStageService.saveProgramStage(stageA);
    DataElement dataElementA = createDataElement('A');
    DataElement dataElementB = createDataElement('B');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    ProgramStageDataElement stageDeA = createProgramStageDataElement(stageA, dataElementA, 1);
    ProgramStageDataElement stageDeB = createProgramStageDataElement(stageA, dataElementB, 2);
    programStageDataElementService.addProgramStageDataElement(stageDeA);
    programStageDataElementService.addProgramStageDataElement(stageDeB);
    dataElements = new ArrayList<>();
    dataElements.add(dataElementA);
    dataElements.add(dataElementB);
    stageB = new ProgramStage("B", program);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    program.setProgramStages(programStages);
    programService.updateProgram(program);
    sectionA = createProgramStageSection('A', 1);
    sectionA.setDataElements(dataElements);
    sectionB = createProgramStageSection('B', 2);
    Set<ProgramStageSection> sections = new HashSet<>();
    sections.add(sectionA);
    sections.add(sectionB);
    stageA.setProgramStageSections(sections);
  }

  @Test
  void testAddGet() {
    ProgramStageSection sectionA = createProgramStageSection('A', 1);
    sectionA.setDataElements(dataElements);
    programStageSectionStore.save(sectionA);
    long idA = sectionA.getId();
    assertEquals(sectionA, programStageSectionStore.get(idA));
  }

  @Test
  @DisplayName("retrieving program stage sections by data element returns expected entries")
  void getProgramStageSectionsByDataElement() {
    // given
    DataElement de1 = createDataElementAndSave('q');
    DataElement de2 = createDataElementAndSave('r');
    DataElement de3 = createDataElementAndSave('s');
    DataElement de4 = createDataElementAndSave('t');

    createProgramStageSectionAndSave('a', 1, de1, de2);
    createProgramStageSectionAndSave('b', 2, de3);
    createProgramStageSectionAndSave('c', 3, de4);

    // when
    List<ProgramStageSection> programStageSections =
        programStageSectionStore.getAllByDataElement(List.of(de1, de2, de3));

    // then
    assertEquals(2, programStageSections.size());
    assertTrue(
        programStageSections.stream()
            .flatMap(pss -> pss.getDataElements().stream())
            .toList()
            .containsAll(List.of(de1, de2, de3)));

    assertFalse(
        programStageSections.stream()
            .flatMap(pss -> pss.getDataElements().stream())
            .toList()
            .contains(de4));
  }

  private void createProgramStageSectionAndSave(char c, int order, DataElement... des) {
    ProgramStageSection pss = createProgramStageSection(c, order);
    pss.getDataElements().addAll(List.of(des));
    programStageSectionStore.save(pss);
  }

  private DataElement createDataElementAndSave(char c) {
    CategoryCombo cc = createCategoryCombo(c);
    manager.save(cc);

    DataElement de = createDataElement(c, cc);
    dataElementService.addDataElement(de);
    return de;
  }
}
