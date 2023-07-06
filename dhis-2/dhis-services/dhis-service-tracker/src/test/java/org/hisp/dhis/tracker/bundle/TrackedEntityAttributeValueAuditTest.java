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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditQueryParams;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class TrackedEntityAttributeValueAuditTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private TrackedEntityAttributeValueAuditService attributeValueAuditService;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/te_program_with_tea_allow_audit_metadata.json");
  }

  @Test
  void testTrackedEntityAttributeValueAuditCreate() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/te_program_with_tea_data.json");
    trackerImportParams.setUser(currentUserService.getCurrentUser());
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerImportParams);
    assertEquals(TrackerStatus.OK, trackerImportReport.getStatus());
    List<TrackedEntityInstance> trackedEntityInstances =
        manager.getAll(TrackedEntityInstance.class);
    assertEquals(1, trackedEntityInstances.size());
    TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get(0);
    List<TrackedEntityAttributeValue> attributeValues =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(trackedEntityInstance);
    assertEquals(5, attributeValues.size());

    List<TrackedEntityAttribute> attributes =
        attributeValues.stream()
            .map(TrackedEntityAttributeValue::getAttribute)
            .collect(Collectors.toList());
    List<TrackedEntityAttributeValueAudit> attributeValueAudits =
        attributeValueAuditService.getTrackedEntityAttributeValueAudits(
            new TrackedEntityAttributeValueAuditQueryParams()
                .setTrackedEntityAttributes(attributes)
                .setTrackedEntityInstances(trackedEntityInstances)
                .setAuditTypes(List.of(AuditType.CREATE)));
    assertEquals(5, attributeValueAudits.size());
  }

  @Test
  void testTrackedEntityAttributeValueAuditDelete() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/te_program_with_tea_data.json");
    trackerImportParams.setUser(currentUserService.getCurrentUser());
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerImportParams);
    assertEquals(TrackerStatus.OK, trackerImportReport.getStatus());
    List<TrackedEntityAttributeValue> attributeValues1 =
        trackedEntityAttributeValueService.getTrackedEntityAttributeValues(
            manager.getAll(TrackedEntityInstance.class).get(0));
    List<TrackedEntityAttribute> attributes1 =
        attributeValues1.stream()
            .map(TrackedEntityAttributeValue::getAttribute)
            .collect(Collectors.toList());
    trackerImportParams = fromJson("tracker/te_program_with_tea_null_data.json");
    trackerImportParams.setUser(currentUserService.getCurrentUser());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    trackerImportReport = trackerImportService.importTracker(trackerImportParams);
    assertEquals(TrackerStatus.OK, trackerImportReport.getStatus());
    List<TrackedEntityInstance> trackedEntityInstances =
        manager.getAll(TrackedEntityInstance.class);
    List<TrackedEntityAttributeValueAudit> attributeValueAudits =
        attributeValueAuditService.getTrackedEntityAttributeValueAudits(
            new TrackedEntityAttributeValueAuditQueryParams()
                .setTrackedEntityAttributes(attributes1)
                .setTrackedEntityInstances(trackedEntityInstances)
                .setAuditTypes(List.of(AuditType.DELETE)));
    assertEquals(1, attributeValueAudits.size());

    attributeValueAudits =
        attributeValueAuditService.getTrackedEntityAttributeValueAudits(
            new TrackedEntityAttributeValueAuditQueryParams()
                .setTrackedEntityAttributes(attributes1)
                .setTrackedEntityInstances(trackedEntityInstances)
                .setAuditTypes(List.of(AuditType.UPDATE)));
    assertEquals(1, attributeValueAudits.size());
  }
}
