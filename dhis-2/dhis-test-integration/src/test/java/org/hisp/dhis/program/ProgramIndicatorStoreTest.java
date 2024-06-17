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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author david mackessy
 */
class ProgramIndicatorStoreTest extends TransactionalIntegrationTest {

  @Autowired private ProgramIndicatorStore programIndicatorStore;
  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Test
  @DisplayName(
      "retrieving ProgramIndicator with expression DataElement refs returns expected entries")
  void programIndicatorExprWithDataElementRefsTest() {
    // given
    Program program = createProgram('p');
    identifiableObjectManager.save(program);

    ProgramIndicator pi1 = createProgramIndicator('1', program, "#{999999.de1}", "");
    ProgramIndicator pi2 = createProgramIndicator('2', program, "#{999999.de2}", "");
    ProgramIndicator pi3 = createProgramIndicator('3', program, "#{999999.111}", "");
    programIndicatorStore.save(pi1);
    programIndicatorStore.save(pi2);
    programIndicatorStore.save(pi3);

    // when
    List<ProgramIndicator> programIndicators =
        programIndicatorStore.getAllWithExpressionContainingStrings(List.of("de1", "de2"));

    // then
    assertEquals(2, programIndicators.size(), "program indicators should be of size 2");
    assertTrue(
        programIndicators.containsAll(List.of(pi1, pi2)),
        "retrieved result set should contain 2 program indicators");
  }

  @Test
  @DisplayName("retrieving ProgramIndicator with filter DataElement refs returns expected entries")
  void programIndicatorFilterWithDataElementRefsTest() {
    // given
    Program program = createProgram('p');
    identifiableObjectManager.save(program);

    ProgramIndicator pi1 = createProgramIndicator('1', program, "", "#{999999.de1}");
    ProgramIndicator pi2 = createProgramIndicator('2', program, "", "#{999999.de2}");
    ProgramIndicator pi3 = createProgramIndicator('3', program, "", "#{999999.111}");
    programIndicatorStore.save(pi1);
    programIndicatorStore.save(pi2);
    programIndicatorStore.save(pi3);

    // when
    List<ProgramIndicator> programIndicators =
        programIndicatorStore.getAllWithFilterContainingStrings(List.of("de1", "de2"));

    // then
    assertEquals(2, programIndicators.size(), "program indicators should be of size 2");
    assertTrue(
        programIndicators.containsAll(List.of(pi1, pi2)),
        "retrieved result set should contain 2 program indicators");
  }
}
