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
package org.hisp.dhis.eventvisualization;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityProgramIndicatorDimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventVisualizationDeletionHandlerTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private EventVisualizationStore eventVisualizationStore;
  @Autowired private ProgramService programService;
  @Autowired private DbmsManager dbmsManager;

  @Test
  @DisplayName(
      "deleting a Program Indicator used as a line list program indicator dimension succeeds and removes the dangling dimension")
  void testDeleteProgramIndicatorUsedInProgramIndicatorDimension() {
    Program program = createProgram('A');
    programService.addProgram(program);

    ProgramIndicator programIndicator =
        createProgramIndicator('A', program, "V{enrollment_count}", "true");
    manager.save(programIndicator);

    EventVisualization eventVisualization = createEventVisualization('A', program);
    eventVisualization
        .getProgramIndicatorDimensions()
        .add(new TrackedEntityProgramIndicatorDimension(programIndicator, null, null));
    eventVisualizationStore.save(eventVisualization);
    dbmsManager.clearSession();

    manager.delete(manager.get(ProgramIndicator.class, programIndicator.getUid()));
    dbmsManager.flushSession();

    assertNull(manager.get(ProgramIndicator.class, programIndicator.getUid()));

    EventVisualization reloaded = eventVisualizationStore.getByUid(eventVisualization.getUid());
    assertTrue(reloaded.getProgramIndicatorDimensions().isEmpty());
  }
}
