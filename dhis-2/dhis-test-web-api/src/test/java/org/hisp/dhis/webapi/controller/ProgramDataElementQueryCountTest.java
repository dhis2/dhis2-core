/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.test.config.QueryCountDataSourceProxy;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression for DHIS2-21903: Capture-app {@code GET /api/programs} field filter over nested
 * {@code programStages.programStageDataElements.dataElement} must not select each data element
 * by id once per PSDE row.
 *
 * <p>Author: Morten (Morty)
 */
@Transactional
@ContextConfiguration(classes = {QueryCountDataSourceProxy.class})
class ProgramDataElementQueryCountTest extends PostgresControllerIntegrationTestBase {

  private static final int DATA_ELEMENT_COUNT = 12;

  @Test
  @DisplayName("Program list nested dataElements are batch-loaded, not fetched per PSDE")
  void programStageDataElementsAreNotLoadedPerDataElement() {
    Program program = createProgramWithoutRegistration('Q');
    program.setName("QH21903-P");
    program.setShortName("QH21903P");

    ProgramStage stage = createProgramStage('Q', program);
    stage.setName("QH21903-S");

    Set<ProgramStageDataElement> psdes = new HashSet<>();
    for (int i = 0; i < DATA_ELEMENT_COUNT; i++) {
      char c = (char) ('a' + i);
      DataElement de = createDataElement(c);
      de.setName("QH21903-DE" + i);
      de.setShortName("QH21903DE" + i);
      manager.save(de);

      ProgramStageDataElement psde = createProgramStageDataElement(stage, de, i + 1);
      psdes.add(psde);
    }
    stage.setProgramStageDataElements(psdes);
    program.getProgramStages().add(stage);

    manager.save(program);
    manager.save(stage);
    for (ProgramStageDataElement psde : psdes) {
      manager.save(psde);
    }

    // Drop first-level cache so the list request reloads associations from the DB.
    manager.flush();
    manager.clear();
    injectSecurityContextUser(getAdminUser());
    QueryCountDataSourceProxy.clearCapturedSql();

    var response =
        GET(
            "/programs?fields=id,programStages[id,programStageDataElements[dataElement[id]]]"
                + "&filter=name:eq:QH21903-P&pageSize=10&paging=true");
    assertEquals(
        1,
        response.content().getArray("programs").size(),
        "expected the seeded QH21903-P program in the list response");

    long dataElementByIdQueries =
        QueryCountDataSourceProxy.countCapturedSqlMatching(
            "from dataelement");
    // Batch fetch should collapse N lazy loads into a single (or very few) selects.
    // Allow a small constant overhead for the program/stage graph itself, but never N.
    assertTrue(
        dataElementByIdQueries <= 2,
        "dataElement must be batch-loaded, but dataelement was queried "
            + dataElementByIdQueries
            + " times for "
            + DATA_ELEMENT_COUNT
            + " nested data elements");
  }
}
