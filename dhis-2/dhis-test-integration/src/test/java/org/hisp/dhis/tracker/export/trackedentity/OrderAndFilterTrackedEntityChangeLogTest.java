/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.export.trackedentity;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.Page;
import org.hisp.dhis.tracker.export.PageParams;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderAndFilterTrackedEntityChangeLogTest extends TrackerTest {
  @Autowired private TrackedEntityChangeLogService trackedEntityChangeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private User importUser;

  private TrackerImportParams importParams;

  private final PageParams defaultPageParams = new PageParams(null, null, false);

  private TrackerObjects trackerObjects;

  @BeforeAll
  void setUp() throws IOException {
    injectSecurityContextUser(getAdminUser());
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    importParams = TrackerImportParams.builder().build();
    trackerObjects = fromJson("tracker/event_and_enrollment.json");

    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));
  }

  @BeforeEach
  void resetSecurityContext() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldFilterChangeLogsWhenFilteringByUser()
      throws ForbiddenException, NotFoundException, BadRequestException {
    TrackedEntityChangeLogOperationParams params =
        TrackedEntityChangeLogOperationParams.builder()
            .filterBy("username", new QueryFilter(QueryOperator.EQ, importUser.getUsername()))
            .build();

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of("QS6w44flWAf"), null, params, defaultPageParams);

    Set<String> changeLogUsers =
        changeLogs.getItems().stream()
            .map(cl -> cl.getCreatedBy().getUsername())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(importUser.getUsername()), changeLogUsers);
  }

  @Test
  void shouldFilterChangeLogsWhenFilteringByAttribute()
      throws ForbiddenException, NotFoundException, BadRequestException {
    String trackedEntityAttribute = "toUpdate000";
    TrackedEntityChangeLogOperationParams params =
        TrackedEntityChangeLogOperationParams.builder()
            .filterBy("attribute", new QueryFilter(QueryOperator.EQ, trackedEntityAttribute))
            .build();

    Page<TrackedEntityChangeLog> changeLogs =
        trackedEntityChangeLogService.getTrackedEntityChangeLog(
            UID.of("dUE514NMOlo"), null, params, defaultPageParams);

    Set<String> changeLogAttributes =
        changeLogs.getItems().stream()
            .map(cl -> cl.getTrackedEntityAttribute().getUid())
            .collect(Collectors.toSet());
    assertContainsOnly(List.of(trackedEntityAttribute), changeLogAttributes);
  }
}
