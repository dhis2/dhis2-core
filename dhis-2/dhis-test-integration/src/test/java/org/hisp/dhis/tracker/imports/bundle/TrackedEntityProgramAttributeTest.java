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
package org.hisp.dhis.tracker.imports.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityProgramAttributeTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/te_program_with_tea_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testTrackedEntityProgramAttributeValue() throws IOException {
    testSetup.importTrackerData("tracker/te_program_with_tea_data.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(5, attributeValues.size());
  }

  @Test
  void testTrackedEntityProgramAttributeValueUpdate() throws IOException {
    testSetup.importTrackerData("tracker/te_program_with_tea_data.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(5, attributeValues.size());
    manager.clear();
    // update
    TrackerImportParams importParams =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    testSetup.importTrackerData("tracker/te_program_with_tea_update_data.json", importParams);

    trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    trackedEntity = trackedEntities.get(0);
    attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(5, attributeValues.size());
  }

  @Test
  void testTrackedEntityProgramAttributeValueUpdateAndDelete() throws IOException {
    testSetup.importTrackerData("tracker/te_program_with_tea_data.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity trackedEntity = trackedEntities.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(5, attributeValues.size());
    manager.clear();
    // update
    TrackerImportParams params =
        TrackerImportParams.builder()
            .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
            .build();
    testSetup.importTrackerData("tracker/te_program_with_tea_update_data.json", params);

    trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    trackedEntity = trackedEntities.get(0);
    attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntity);
    assertEquals(5, attributeValues.size());
    manager.clear();
    // delete
    params = TrackerImportParams.builder().importStrategy(TrackerImportStrategy.DELETE).build();
    testSetup.importTrackerData("tracker/te_program_with_tea_delete_data.json", params);

    trackedEntities =
        manager.getAll(TrackedEntity.class).stream().filter(te -> !te.isDeleted()).toList();
    assertEquals(0, trackedEntities.size());
  }
}
