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
package org.hisp.dhis.tracker.imports.report;

import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.DefaultTrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerBundleReportMode;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.imports.preprocess.TrackerPreprocessService;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.ValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@ExtendWith(MockitoExtension.class)
class TrackerBundleImportReportTest {
  @Mock private TrackerBundleService trackerBundleService;

  @Mock private ValidationService validationService;

  @Mock private TrackerPreprocessService trackerPreprocessService;

  @Mock private Notifier notifier;

  @InjectMocks private DefaultTrackerImportService trackerImportService;

  private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

  @Test
  void testImportReportErrors() {
    ImportReport report =
        trackerImportService.buildImportReport(
            createImportReport(), TrackerBundleReportMode.ERRORS);

    assertEquals(Status.OK, report.getStatus());
    assertStats(report);
    assertNotNull(report.getValidationReport());
    assertTrue(report.getValidationReport().hasErrors());
    assertFalse(report.getValidationReport().hasWarnings());
  }

  @Test
  void testImportReportWarnings() {
    ImportReport report =
        trackerImportService.buildImportReport(
            createImportReport(), TrackerBundleReportMode.WARNINGS);

    assertEquals(Status.OK, report.getStatus());
    assertStats(report);
    assertNotNull(report.getValidationReport());
    assertTrue(report.getValidationReport().hasErrors());
    assertTrue(report.getValidationReport().hasWarnings());
  }

  @Test
  void testImportReportFull() {
    ImportReport report =
        trackerImportService.buildImportReport(createImportReport(), TrackerBundleReportMode.FULL);

    assertEquals(Status.OK, report.getStatus());
    assertStats(report);
    assertNotNull(report.getValidationReport());
    assertTrue(report.getValidationReport().hasErrors());
    assertTrue(report.getValidationReport().hasWarnings());
  }

  @Test
  void testSerializingAndDeserializingImportReport() throws JsonProcessingException {
    // Build BundleReport
    Map<TrackerType, TrackerTypeReport> typeReportMap = new HashMap<>();
    TrackerTypeReport typeReport = new TrackerTypeReport(TRACKED_ENTITY);
    Entity entity = new Entity(TRACKED_ENTITY);

    entity.setUid("BltTZV9HvEZ");
    typeReport.addEntity(entity);
    typeReport.getStats().setCreated(1);
    typeReport.getStats().setUpdated(2);
    typeReport.getStats().setDeleted(3);
    typeReportMap.put(TRACKED_ENTITY, typeReport);
    PersistenceReport persistenceReport = new PersistenceReport(typeReportMap);

    // Build ValidationReport
    ValidationReport tvr = ValidationReport.emptyReport();

    tvr.addErrors(
        List.of(
            new Error(
                "Could not find OrganisationUnit: ``, linked to Tracked Entity.",
                ValidationCode.E1049.name(),
                TRACKED_ENTITY.name(),
                "BltTZV9HvEZ",
                List.of("BltTZV9HvEZ"))));
    tvr.addWarnings(
        List.of(
            new Warning(
                "ProgramStage `l8oDIfJJhtg` does not allow user assignment",
                ValidationCode.E1120.name(),
                TrackerType.EVENT.name(),
                "BltTZV9HvEZ")));
    // Create the TrackerImportReport
    final Map<TrackerType, Integer> bundleSize = new HashMap<>();
    bundleSize.put(TRACKED_ENTITY, 1);
    ImportReport toSerializeReport =
        ImportReport.withImportCompleted(Status.ERROR, persistenceReport, tvr, bundleSize);
    // Serialize TrackerImportReport into String
    String jsonString = jsonMapper.writeValueAsString(toSerializeReport);
    // Deserialize the String back into TrackerImportReport
    ImportReport deserializedReport = jsonMapper.readValue(jsonString, ImportReport.class);
    // Verify Stats
    assertEquals(
        toSerializeReport.getStats().getIgnored(), deserializedReport.getStats().getIgnored());
    assertEquals(
        toSerializeReport.getStats().getDeleted(), deserializedReport.getStats().getDeleted());
    assertEquals(
        toSerializeReport.getStats().getUpdated(), deserializedReport.getStats().getUpdated());
    assertEquals(
        toSerializeReport.getStats().getCreated(), deserializedReport.getStats().getCreated());
    assertEquals(toSerializeReport.getStats().getTotal(), deserializedReport.getStats().getTotal());
    // Verify BundleReport
    assertEquals(
        toSerializeReport.getPersistenceReport().getStats().getIgnored(),
        deserializedReport.getPersistenceReport().getStats().getIgnored());
    assertEquals(
        toSerializeReport.getPersistenceReport().getStats().getDeleted(),
        deserializedReport.getPersistenceReport().getStats().getDeleted());
    assertEquals(
        toSerializeReport.getPersistenceReport().getStats().getUpdated(),
        deserializedReport.getPersistenceReport().getStats().getUpdated());
    assertEquals(
        toSerializeReport.getPersistenceReport().getStats().getCreated(),
        deserializedReport.getPersistenceReport().getStats().getCreated());
    assertEquals(
        toSerializeReport.getPersistenceReport().getStats().getTotal(),
        deserializedReport.getPersistenceReport().getStats().getTotal());
    TrackerTypeReport serializedReportTrackerTypeReport =
        toSerializeReport.getPersistenceReport().getTypeReportMap().get(TRACKED_ENTITY);
    TrackerTypeReport deserializedReportTrackerTypeReport =
        deserializedReport.getPersistenceReport().getTypeReportMap().get(TRACKED_ENTITY);
    // NotificationDataBundle is no more relevant to object equivalence, so
    // just asserting on all other fields.
    assertEquals(
        serializedReportTrackerTypeReport.getTrackerType(),
        deserializedReportTrackerTypeReport.getTrackerType());
    assertEquals(
        serializedReportTrackerTypeReport.getEntityReport(),
        deserializedReportTrackerTypeReport.getEntityReport());
    assertEquals(
        serializedReportTrackerTypeReport.getEntityReport(),
        deserializedReportTrackerTypeReport.getEntityReport());
    assertEquals(
        serializedReportTrackerTypeReport.getStats(),
        deserializedReportTrackerTypeReport.getStats());
    // Verify Validation Report - Error Reports
    assertEquals(
        toSerializeReport.getValidationReport().getErrors().get(0).getMessage(),
        deserializedReport.getValidationReport().getErrors().get(0).getMessage());
    assertEquals(
        toSerializeReport.getValidationReport().getErrors().get(0).getErrorCode(),
        deserializedReport.getValidationReport().getErrors().get(0).getErrorCode());
    assertEquals(
        toSerializeReport.getValidationReport().getErrors().get(0).getUid(),
        deserializedReport.getValidationReport().getErrors().get(0).getUid());
    // Verify Validation Report - Warning Reports
    assertEquals(
        toSerializeReport.getValidationReport().getWarnings().get(0).getWarningMessage(),
        deserializedReport.getValidationReport().getWarnings().get(0).getWarningMessage());
    assertEquals(
        toSerializeReport.getValidationReport().getWarnings().get(0).getWarningCode(),
        deserializedReport.getValidationReport().getWarnings().get(0).getWarningCode());
    assertEquals(
        toSerializeReport.getValidationReport().getWarnings().get(0).getUid(),
        deserializedReport.getValidationReport().getWarnings().get(0).getUid());
    assertEquals(
        toSerializeReport.getValidationReport().getWarnings().get(0).getTrackerType(),
        deserializedReport.getValidationReport().getWarnings().get(0).getTrackerType());

    assertEquals(toSerializeReport.getStats(), deserializedReport.getStats());
  }

  private void assertStats(ImportReport report) {
    assertNotNull(report.getStats());
    assertEquals(1, report.getStats().getCreated());
    assertEquals(0, report.getStats().getUpdated());
    assertEquals(0, report.getStats().getDeleted());
    assertEquals(1, report.getStats().getIgnored());
  }

  private ImportReport createImportReport() {
    final Map<TrackerType, Integer> bundleSize = new HashMap<>();
    bundleSize.put(TRACKED_ENTITY, 1);
    return ImportReport.withImportCompleted(
        Status.OK, createBundleReport(), createValidationReport(), bundleSize);
  }

  private ValidationReport createValidationReport() {
    return new ValidationReport(
        List.of(
            new Error(
                "Could not find OrganisationUnit: ``, linked to Tracked Entity.",
                ValidationCode.E1049.name(),
                TRACKED_ENTITY.name(),
                "BltTZV9HvEZ",
                List.of("BltTZV9HvEZ"))),
        List.of(
            new Warning(
                "ProgramStage `l8oDIfJJhtg` does not allow user assignment",
                ValidationCode.E1120.name(),
                TrackerType.EVENT.name(),
                "BltTZV9HvEZ")));
  }

  private PersistenceReport createBundleReport() {
    PersistenceReport persistenceReport = PersistenceReport.emptyReport();
    TrackerTypeReport typeReport = new TrackerTypeReport(TRACKED_ENTITY);
    Entity objectReport = new Entity(TRACKED_ENTITY, "TE_UID");
    typeReport.addEntity(objectReport);
    typeReport.getStats().incCreated();
    persistenceReport.getTypeReportMap().put(TRACKED_ENTITY, typeReport);
    return persistenceReport;
  }
}
