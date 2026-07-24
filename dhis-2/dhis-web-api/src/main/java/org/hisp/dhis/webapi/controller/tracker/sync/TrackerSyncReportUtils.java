/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static java.lang.String.format;
import static org.hisp.dhis.dxf2.sync.SyncUtils.runSyncRequest;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1082;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1113;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1114;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4017;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.metadata.sync.exception.MetadataSyncServiceException;
import org.hisp.dhis.dxf2.sync.SyncUtils;
import org.hisp.dhis.dxf2.sync.SystemInstance;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.report.Entity;
import org.hisp.dhis.tracker.imports.report.Error;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * Shared helpers for parsing an {@link ImportReport} returned by a tracker data synchronization
 * request, and for sending that request. Used by both {@link TrackerDataSynchronizationService} and
 * {@link SingleEventDataSynchronizationService}.
 */
final class TrackerSyncReportUtils {

  private static final Set<String> ALREADY_DELETED_CODES =
      Set.of(E1082.name(), E1113.name(), E1114.name(), E4017.name());

  private TrackerSyncReportUtils() {}

  static ImportReport sendTrackerRequest(
      RestTemplate restTemplate,
      RenderService renderService,
      Map<String, ?> payload,
      SystemInstance instance,
      SystemSettings settings,
      String url) {
    RequestCallback requestCallback = createRequestCallback(payload, instance, renderService);
    int maxAttempts = settings.getSyncMaxAttempts();

    ResponseExtractor<ImportReport> extractor =
        response -> {
          try {
            return renderService.fromJson(response.getBody(), ImportReport.class);
          } catch (IOException e) {
            throw new MetadataSyncServiceException("Failed to parse tracker import response", e);
          }
        };

    ImportReport report =
        runSyncRequest(restTemplate, requestCallback, extractor, url, maxAttempts);

    if (report == null) {
      throw new MetadataSyncServiceException("Tracker sync returned null response");
    }
    return report;
  }

  private static RequestCallback createRequestCallback(
      Map<String, ?> payload, SystemInstance instance, RenderService renderService) {
    return request -> {
      request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      request
          .getHeaders()
          .set(
              SyncUtils.HEADER_AUTHORIZATION,
              CodecUtils.getBasicAuthString(instance.getUsername(), instance.getPassword()));
      renderService.toJson(request.getBody(), payload);
    };
  }

  static String formatFailedUids(Set<UID> failedUids) {
    return failedUids.isEmpty() ? "" : format(" (failed: %s)", failedUids);
  }

  static Set<UID> successfullyProcessedUids(ImportReport report, TrackerType type) {
    if (report.getPersistenceReport() == null) {
      return new HashSet<>();
    }
    TrackerTypeReport typeReport = report.getPersistenceReport().getTypeReportMap().get(type);
    if (typeReport == null) {
      return new HashSet<>();
    }
    return typeReport.getEntityReport().stream()
        .filter(entity -> entity.getErrorReports().isEmpty())
        .map(Entity::getUid)
        .collect(Collectors.toCollection(HashSet::new));
  }

  static Set<UID> failedUids(ImportReport report, TrackerType type) {
    Set<UID> failed = new HashSet<>();

    if (report.getValidationReport() != null) {
      report.getValidationReport().getErrors().stream()
          .filter(e -> type.name().equals(e.getTrackerType()))
          .map(Error::getUid)
          .forEach(failed::add);
    }

    if (report.getPersistenceReport() != null) {
      TrackerTypeReport typeReport = report.getPersistenceReport().getTypeReportMap().get(type);
      if (typeReport != null) {
        typeReport.getEntityReport().stream()
            .filter(entity -> !entity.getErrorReports().isEmpty())
            .map(Entity::getUid)
            .forEach(failed::add);
      }
    }

    return failed;
  }

  /**
   * Like {@link #failedUids}, but excludes entities whose only error(s) are {@link
   * #ALREADY_DELETED_CODES} — i.e. entities whose failure should block a parent entity's sync
   * timestamp, as opposed to one that failed only because "the delete already happened". Only
   * meaningful against a DELETE report.
   */
  static Set<UID> blockingFailedUids(ImportReport report, TrackerType type) {
    Set<UID> blocking = new HashSet<>();

    if (report.getValidationReport() != null) {
      report.getValidationReport().getErrors().stream()
          .filter(e -> type.name().equals(e.getTrackerType()))
          .filter(e -> !ALREADY_DELETED_CODES.contains(e.getErrorCode()))
          .map(Error::getUid)
          .forEach(blocking::add);
    }

    if (report.getPersistenceReport() != null) {
      TrackerTypeReport typeReport = report.getPersistenceReport().getTypeReportMap().get(type);
      if (typeReport != null) {
        typeReport.getEntityReport().stream()
            .filter(
                entity ->
                    entity.getErrorReports().stream()
                        .anyMatch(e -> !ALREADY_DELETED_CODES.contains(e.getErrorCode())))
            .map(Entity::getUid)
            .forEach(blocking::add);
      }
    }

    return blocking;
  }

  /**
   * Entities that either succeeded outright, or whose only error(s) in a DELETE report are {@link
   * #ALREADY_DELETED_CODES} — meaning the delete's goal was already achieved by an earlier attempt
   * or an out-of-band delete. Only meaningful against a DELETE report.
   */
  static Set<UID> alreadyDeletedOrSucceededUids(ImportReport report, TrackerType type) {
    Set<UID> result = new HashSet<>(successfullyProcessedUids(report, type));
    Set<UID> blocking = blockingFailedUids(report, type);
    failedUids(report, type).stream().filter(uid -> !blocking.contains(uid)).forEach(result::add);
    return result;
  }
}
