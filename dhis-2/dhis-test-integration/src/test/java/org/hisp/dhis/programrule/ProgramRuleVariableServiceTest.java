/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgramRuleVariableServiceTest extends SingleSetupIntegrationTestBase {

  private Program programA;

  private Program programB;

  private Program programC;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private DataElement dataElementC;

  private TrackedEntityAttribute attributeA;

  @Autowired private ProgramService programService;

  @Autowired private DataElementService dataElementService;

  @Autowired private TrackedEntityAttributeService attributeService;

  @Autowired private ProgramRuleVariableService variableService;

  @Override
  public void setUpTest() {
    programA = createProgram('A', null, null);
    programB = createProgram('B', null, null);
    programC = createProgram('C', null, null);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementC = createDataElement('C');
    attributeA = createTrackedEntityAttribute('A');
    programService.addProgram(programA);
    programService.addProgram(programB);
    programService.addProgram(programC);
    dataElementService.addDataElement(dataElementA);
    dataElementService.addDataElement(dataElementB);
    dataElementService.addDataElement(dataElementC);
    attributeService.addTrackedEntityAttribute(attributeA);
  }

  @Test
  void testAddGet() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableB =
        new ProgramRuleVariable(
            "RuleVariableB",
            programA,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            attributeA,
            null,
            true,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableC =
        new ProgramRuleVariable(
            "RuleVariableC",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    long idA = variableService.addProgramRuleVariable(variableA);
    long idB = variableService.addProgramRuleVariable(variableB);
    long idC = variableService.addProgramRuleVariable(variableC);
    assertEquals(variableA, variableService.getProgramRuleVariable(idA));
    assertEquals(variableB, variableService.getProgramRuleVariable(idB));
    assertEquals(variableC, variableService.getProgramRuleVariable(idC));
  }

  @Test
  void testGetByProgram() {
    ProgramRuleVariable variableD =
        new ProgramRuleVariable(
            "RuleVariableD",
            programB,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableE =
        new ProgramRuleVariable(
            "RuleVariableE",
            programB,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            attributeA,
            null,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableF =
        new ProgramRuleVariable(
            "RuleVariableF",
            programB,
            ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    // Add a var that is not part of programB....
    ProgramRuleVariable variableG =
        new ProgramRuleVariable(
            "RuleVariableG",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableD);
    variableService.addProgramRuleVariable(variableE);
    variableService.addProgramRuleVariable(variableF);
    variableService.addProgramRuleVariable(variableG);
    // Get all the 3 rules for programB
    List<ProgramRuleVariable> vars = variableService.getProgramRuleVariable(programB);
    assertEquals(3, vars.size());
    assertTrue(vars.contains(variableD));
    assertTrue(vars.contains(variableE));
    assertTrue(vars.contains(variableF));
    // Make sure that the var connected to program A is not returned as part
    // of list of vars in program B.
    assertFalse(vars.contains(variableG));
  }

  @Test
  void testUpdate() {
    ProgramRuleVariable variableH =
        new ProgramRuleVariable(
            "RuleVariableH",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    long idH = variableService.addProgramRuleVariable(variableH);
    variableH.setAttribute(attributeA);
    variableH.setDataElement(dataElementA);
    variableH.setName("newname");
    variableH.setProgram(programC);
    variableH.setSourceType(ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT);
    variableService.updateProgramRuleVariable(variableH);
    assertEquals(variableH, variableService.getProgramRuleVariable(idH));
  }

  @Test
  void testDeleteProgramRuleVariable() {
    ProgramRuleVariable ruleVariableI =
        new ProgramRuleVariable(
            "RuleVariableI",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable ruleVariableJ =
        new ProgramRuleVariable(
            "RuleVariableJ",
            programA,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            attributeA,
            null,
            false,
            null,
            ValueType.TEXT);
    long idI = variableService.addProgramRuleVariable(ruleVariableI);
    long idJ = variableService.addProgramRuleVariable(ruleVariableJ);
    assertNotNull(variableService.getProgramRuleVariable(idI));
    assertNotNull(variableService.getProgramRuleVariable(idJ));
    variableService.deleteProgramRuleVariable(ruleVariableI);
    assertNull(variableService.getProgramRuleVariable(idI));
    assertNotNull(variableService.getProgramRuleVariable(idJ));
    variableService.deleteProgramRuleVariable(ruleVariableJ);
    assertNull(variableService.getProgramRuleVariable(idI));
    assertNull(variableService.getProgramRuleVariable(idJ));
  }

  @Test
  void testShouldReturnTrueIfDataElementIsLinkedToProgramRuleVariable() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableB =
        new ProgramRuleVariable(
            "RuleVariableB",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM,
            null,
            dataElementB,
            true,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableA);
    variableService.addProgramRuleVariable(variableB);
    assertTrue(variableService.isLinkedToProgramRuleVariableCached(programA, dataElementA));
    assertTrue(variableService.isLinkedToProgramRuleVariableCached(programA, dataElementB));
  }

  @Test
  void testShouldReturnFalseIfDataElementIsNOTLinkedToProgramRuleVariable() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableA);
    assertFalse(variableService.isLinkedToProgramRuleVariableCached(programA, dataElementC));
  }

  @Test
  void testShouldReturnVariableIfNotLinkedToDataElement() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            null,
            false,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableA);
    List<ProgramRuleVariable> variables = variableService.getVariablesWithNoDataElement();
    assertEquals(1, variables.size());
    assertTrue(variables.contains(variableA));
  }

  @Test
  void testShouldReturnVariableIfNotLinkedToAttribute() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            null,
            null,
            false,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableA);
    List<ProgramRuleVariable> variables = variableService.getVariablesWithNoAttribute();
    assertEquals(1, variables.size());
    assertTrue(variables.contains(variableA));
  }

  @Test
  void testShouldNotReturnAnyVariableIfLinkedToDataObjects() {
    ProgramRuleVariable variableA =
        new ProgramRuleVariable(
            "RuleVariableA",
            programA,
            ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT,
            null,
            dataElementA,
            false,
            null,
            ValueType.TEXT);
    ProgramRuleVariable variableB =
        new ProgramRuleVariable(
            "RuleVariableB",
            programA,
            ProgramRuleVariableSourceType.TEI_ATTRIBUTE,
            attributeA,
            null,
            false,
            null,
            ValueType.TEXT);
    variableService.addProgramRuleVariable(variableA);
    variableService.addProgramRuleVariable(variableB);
    List<ProgramRuleVariable> variablesD = variableService.getVariablesWithNoDataElement();
    List<ProgramRuleVariable> variablesA = variableService.getVariablesWithNoAttribute();
    assertTrue(variablesD.isEmpty());
    assertTrue(variablesA.isEmpty());
  }
}
