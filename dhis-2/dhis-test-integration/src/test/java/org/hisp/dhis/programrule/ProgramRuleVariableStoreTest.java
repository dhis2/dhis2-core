/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.programrule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class ProgramRuleVariableStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataElementService dataElementService;
  @Autowired private ProgramRuleVariableStore programRuleVariableStore;
  @Autowired private ProgramStore programStore;

  @Test
  @DisplayName("retrieving Program Rule Variables by data element returns expected entries")
  void getProgramRuleVariablesByDataElementTest() {
    // given
    DataElement deA = createDataElementAndSave('A');
    DataElement deB = createDataElementAndSave('B');
    DataElement deC = createDataElementAndSave('C');

    Program program = createProgram('p');
    programStore.save(program);

    ProgramRuleVariable prv1 = createProgramRuleVariable('a', program);
    prv1.setDataElement(deA);
    ProgramRuleVariable prv2 = createProgramRuleVariable('b', program);
    prv2.setDataElement(deB);
    ProgramRuleVariable prv3 = createProgramRuleVariable('c', program);
    prv3.setDataElement(deC);
    ProgramRuleVariable prv4 = createProgramRuleVariable('d', program);

    programRuleVariableStore.save(prv1);
    programRuleVariableStore.save(prv2);
    programRuleVariableStore.save(prv3);
    programRuleVariableStore.save(prv4);

    // when
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableStore.getByDataElement(List.of(deA, deB, deC));

    // then
    assertEquals(3, programRuleVariables.size());
    assertTrue(
        programRuleVariables.stream()
            .map(ProgramRuleVariable::getDataElement)
            .toList()
            .containsAll(List.of(deA, deB, deC)));
  }

  @Test
  @DisplayName(
      "retrieving Program Rule Variables by data element with empty list returns expected entries")
  void getProgramRuleVariablesByDataElementEmptyListTest() {
    // when
    List<ProgramRuleVariable> programRuleVariables =
        programRuleVariableStore.getByDataElement(List.of());

    // then
    assertTrue(programRuleVariables.isEmpty());
  }

  private DataElement createDataElementAndSave(char c) {
    DataElement de = createDataElement(c);
    dataElementService.addDataElement(de);
    return de;
  }
}
