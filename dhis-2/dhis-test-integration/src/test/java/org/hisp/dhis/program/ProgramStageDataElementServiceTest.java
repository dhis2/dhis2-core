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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class ProgramStageDataElementServiceTest extends PostgresIntegrationTestBase {

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private DataElementService dataElementService;

  @Autowired private ProgramService programService;

  @Autowired private ProgramStageService programStageService;

  private OrganisationUnit organisationUnit;

  private ProgramStage stageA;

  private ProgramStage stageB;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private ProgramStageDataElement stageDataElementA;

  private ProgramStageDataElement stageDataElementB;

  @BeforeEach
  void setUp() {
    organisationUnit = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    Program program = createProgram('A', new HashSet<>(), organisationUnit);
    programService.addProgram(program);
    stageA = new ProgramStage("A", program);
    stageA.setSortOrder(1);
    stageA.setUid("StageA");
    programStageService.saveProgramStage(stageA);
    stageB = new ProgramStage("B", program);
    stageB.setSortOrder(2);
    programStageService.saveProgramStage(stageB);
    Set<ProgramStage> programStages = new HashSet<>();
    programStages.add(stageA);
    programStages.add(stageB);
    program.setProgramStages(programStages);
    programService.updateProgram(program);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    stageDataElementA = new ProgramStageDataElement(stageA, dataElementA, false, 1);
    stageDataElementB = new ProgramStageDataElement(stageA, dataElementB, false, 2);
  }

  @Test
  void testAddProgramStageDataElement() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    assertNotNull(programStageDataElementService.get(stageA, dataElementA));
    assertNotNull(programStageDataElementService.get(stageA, dataElementB));
  }

  @Test
  void testUpdateProgramStageDataElement() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    assertNotNull(programStageDataElementService.get(stageA, dataElementA));
    assertFalse(programStageDataElementService.get(stageA, dataElementA).isCompulsory());
    assertFalse(programStageDataElementService.get(stageA, dataElementA).getSkipAnalytics());
    stageDataElementA.setCompulsory(true);
    stageDataElementA.setSkipAnalytics(true);
    programStageDataElementService.updateProgramStageDataElement(stageDataElementA);
    assertTrue(programStageDataElementService.get(stageA, dataElementA).isCompulsory());
    assertTrue(programStageDataElementService.get(stageA, dataElementA).getSkipAnalytics());
  }

  @Test
  void testDeleteProgramStageDataElement() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    assertNotNull(programStageDataElementService.get(stageA, dataElementA));
    assertNotNull(programStageDataElementService.get(stageA, dataElementB));
    programStageDataElementService.deleteProgramStageDataElement(stageDataElementA);
    assertNull(programStageDataElementService.get(stageA, dataElementA));
    assertNotNull(programStageDataElementService.get(stageA, dataElementB));
    programStageDataElementService.deleteProgramStageDataElement(stageDataElementB);
    assertNull(programStageDataElementService.get(stageA, dataElementA));
    assertNull(programStageDataElementService.get(stageA, dataElementB));
  }

  @Test
  void testGetByStageElement() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    assertNotNull(programStageDataElementService.get(stageA, dataElementA));
    assertNotNull(programStageDataElementService.get(stageA, dataElementB));
  }

  @Test
  void testGetAllProgramStageDataElements() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    programStageDataElementService.addProgramStageDataElement(stageDataElementB);
    assertTrue(
        equals(
            programStageDataElementService.getAllProgramStageDataElements(),
            stageDataElementA,
            stageDataElementB));
  }

  @Test
  void testRemoveReferencedDataElement() {
    programStageDataElementService.addProgramStageDataElement(stageDataElementA);
    stageA.getProgramStageDataElements().addAll(Set.of(stageDataElementA));
    programStageService.updateProgramStage(stageA);
    assertThrows(
        DeleteNotAllowedException.class, () -> dataElementService.deleteDataElement(dataElementA));
  }
}
