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
package org.hisp.dhis.tracker.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AtomicModeIntegrationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testImportSuccessWithAtomicModeObjectIfThereIsAnErrorInOneTE() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/one_valid_te_and_one_invalid.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();

    ImportReport trackerImportTeReport = trackerImportService.importTracker(params, trackerObjects);

    assertNotNull(trackerImportTeReport);
    assertEquals(Status.OK, trackerImportTeReport.getStatus());
    assertEquals(1, trackerImportTeReport.getValidationReport().getErrors().size());
    assertNotNull(manager.get(TrackedEntity.class, "VALIDTEAAAA"));
    assertNull(manager.get(TrackedEntity.class, "INVALIDTEAA"));
  }

  @Test
  void testImportFailWithAtomicModeAllIfThereIsAnErrorInOneTE() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/one_valid_te_and_one_invalid.json");
    TrackerImportParams params = TrackerImportParams.builder().atomicMode(AtomicMode.ALL).build();

    ImportReport trackerImportTeReport = trackerImportService.importTracker(params, trackerObjects);
    assertNotNull(trackerImportTeReport);
    assertEquals(Status.ERROR, trackerImportTeReport.getStatus());
    assertEquals(1, trackerImportTeReport.getValidationReport().getErrors().size());
    assertNull(manager.get(TrackedEntity.class, "VALIDTEAAAA"));
    assertNull(manager.get(TrackedEntity.class, "INVALIDTEAA"));
  }
}
