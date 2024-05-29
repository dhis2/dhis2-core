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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class RelationshipImportTest extends TrackerTest {

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;
  @Autowired protected UserService _userService;
  private User userA;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");
    userA = userService.getUser("M5zQapPyTZI");
    TrackerImportParams params = TrackerImportParams.builder().userId(userA.getUid()).build();
    assertNoErrors(trackerImportService.importTracker(params, fromJson("tracker/single_te.json")));
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/single_enrollment.json")));
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/single_event.json")));
    manager.flush();
  }

  @Test
  void successImportingRelationships() throws IOException {
    userA = userService.getUser("M5zQapPyTZI");
    TrackerImportParams params = TrackerImportParams.builder().userId(userA.getUid()).build();
    ImportReport importReport =
        trackerImportService.importTracker(params, fromJson("tracker/relationships.json"));
    assertThat(importReport.getStatus(), is(Status.OK));
    assertThat(importReport.getStats().getCreated(), is(2));
  }

  @Test
  void shouldFailWhenUserNotAuthorizedToCreateRelationship() throws IOException {
    userA = userService.getUser("o1HMTIzBGo7");
    TrackerImportParams params = TrackerImportParams.builder().userId(userA.getUid()).build();

    ImportReport importReport =
        trackerImportService.importTracker(params, fromJson("tracker/relationships.json"));

    assertHasError(importReport, ValidationCode.E4020);
    assertThat(importReport.getStats().getIgnored(), is(2));
  }

  @Test
  void successUpdateRelationships() throws IOException {
    TrackerImportParams trackerImportParams =
        TrackerImportParams.builder().userId(userA.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/relationships.json");
    trackerImportService.importTracker(trackerImportParams, trackerObjects);
    trackerObjects = fromJson("tracker/relationshipToUpdate.json");
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport importReport =
        trackerImportService.importTracker(trackerImportParams, trackerObjects);
    assertThat(importReport.getStatus(), is(Status.OK));
    assertThat(importReport.getStats().getCreated(), is(0));
    assertThat(importReport.getStats().getIgnored(), is(1));
  }
}
