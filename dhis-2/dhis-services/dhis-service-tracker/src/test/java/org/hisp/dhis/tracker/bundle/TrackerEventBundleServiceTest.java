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
package org.hisp.dhis.tracker.bundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class TrackerEventBundleServiceTest extends TrackerTest {

  @Autowired private ObjectBundleService objectBundleService;

  @Autowired private ObjectBundleValidationService objectBundleValidationService;

  @Autowired private TrackerBundleService trackerBundleService;

  @Autowired private ProgramStageInstanceStore programStageInstanceStore;

  @Override
  protected void initTest() throws IOException {
    Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata =
        renderService.fromMetadata(
            new ClassPathResource("tracker/event_metadata.json").getInputStream(),
            RenderFormat.JSON);
    ObjectBundleParams params = new ObjectBundleParams();
    params.setObjectBundleMode(ObjectBundleMode.COMMIT);
    params.setImportStrategy(ImportStrategy.CREATE);
    params.setObjects(metadata);
    ObjectBundle bundle = objectBundleService.create(params);
    ObjectBundleValidationReport validationReport = objectBundleValidationService.validate(bundle);
    assertFalse(validationReport.hasErrorReports());
    objectBundleService.commit(bundle);
  }

  @Test
  void testCreateSingleEventData() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/event_events_and_enrollment.json");
    assertEquals(8, trackerImportParams.getEvents().size());
    TrackerBundle trackerBundle = trackerBundleService.create(trackerImportParams);
    trackerBundleService.commit(trackerBundle);
    List<ProgramStageInstance> programStageInstances = programStageInstanceStore.getAll();
    assertEquals(8, programStageInstances.size());
  }

  @Test
  void testUpdateSingleEventData() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/event_events_and_enrollment.json");
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    TrackerBundle trackerBundle = trackerBundleService.create(trackerImportParams);
    trackerBundleService.commit(trackerBundle);
    assertEquals(8, programStageInstanceStore.getAll().size());
    trackerBundle =
        trackerBundleService.create(
            TrackerImportParams.builder()
                .events(trackerBundle.getEvents())
                .enrollments(trackerBundle.getEnrollments())
                .trackedEntities(trackerBundle.getTrackedEntities())
                .user(currentUserService.getCurrentUser())
                .build());
    trackerBundleService.commit(trackerBundle);
    assertEquals(8, programStageInstanceStore.getAll().size());
  }
}
