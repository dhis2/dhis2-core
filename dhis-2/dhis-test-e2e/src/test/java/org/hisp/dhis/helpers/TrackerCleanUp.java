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
package org.hisp.dhis.helpers;

import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.ENROLLMENTS;
import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.EVENTS;
import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.RELATIONSHIPS;
import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.TRACKED_ENTITIES;
import static org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions.TRACKER_RESOURCES;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.tracker.TrackerImportExportActions;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;

/**
 * Deletes tracker data (tracked entities, enrollments, events and relationships) registered in
 * {@link TestRunStorage}. It sends one tracker import with {@code importStrategy=DELETE} per chunk.
 *
 * <p>The tracker importer decides the order of deletion and cascades it to dependent objects. For
 * example, deleting a tracked entity deletes its enrollments, events and relationships. The payload
 * only holds UIDs, so it does not matter which objects of a hierarchy were registered.
 *
 * <p>{@code atomicMode=OBJECT} makes sure an object that cannot be deleted does not block the
 * deletion of the others. Objects that do not exist or are already deleted are expected: a test
 * might have deleted them, or the deletion of a parent cascaded to them. Any other error means data
 * is left behind, which breaks test isolation, so the clean-up fails.
 *
 * <p>Tracker data is soft deleted. Callers that want to delete metadata referenced by it have to
 * purge it first via {@link
 * org.hisp.dhis.test.e2e.actions.MaintenanceActions#removeSoftDeletedData()}.
 */
public class TrackerCleanUp {
  private static final Logger logger = LogManager.getLogger(TrackerCleanUp.class.getName());

  private static final int CHUNK_SIZE = 500;

  /** Error codes meaning the object does not exist or is already deleted. */
  private static final Set<String> EXPECTED_ERROR_CODES =
      Set.of(
          // does not exist
          "E1063", // tracked entity
          "E1081", // enrollment
          "E1032", // event
          "E4016", // relationship
          // already deleted
          "E1114", // tracked entity
          "E1113", // enrollment
          "E1082", // event
          "E4017" // relationship
          );

  private record PayloadKeys(String collection, String uid) {}

  private static final Map<String, PayloadKeys> RESOURCE_TO_PAYLOAD_KEYS =
      Map.of(
          TRACKED_ENTITIES, new PayloadKeys("trackedEntities", "trackedEntity"),
          ENROLLMENTS, new PayloadKeys("enrollments", "enrollment"),
          EVENTS, new PayloadKeys("events", "event"),
          RELATIONSHIPS, new PayloadKeys("relationships", "relationship"));

  public static boolean isTrackerResource(String resource) {
    return resource != null && TRACKER_RESOURCES.contains(resource);
  }

  /**
   * Deletes all tracker data registered in {@link TestRunStorage}.
   *
   * @return true if any tracker data was sent for deletion
   */
  public boolean deleteCreatedTrackerData() {
    return deleteTrackerData(TestRunStorage.getCreatedEntities());
  }

  /**
   * Deletes the tracker data among the given entities and removes it from {@link TestRunStorage}.
   * Entities of non-tracker resources are ignored.
   *
   * @param entities map of UID to resource as stored by {@link TestRunStorage}
   * @return true if any tracker data was sent for deletion
   * @throws IllegalStateException if the importer reported an unexpected error
   */
  public boolean deleteTrackerData(Map<String, String> entities) {
    List<Map.Entry<String, String>> trackerEntities =
        entities.entrySet().stream().filter(e -> isTrackerResource(e.getValue())).toList();
    if (trackerEntities.isEmpty()) {
      return false;
    }

    new LoginActions().loginAsSuperUser();
    TrackerImportExportActions trackerActions = new TrackerImportExportActions();

    List<String> unexpectedErrors = new ArrayList<>();
    for (int from = 0; from < trackerEntities.size(); from += CHUNK_SIZE) {
      List<Map.Entry<String, String>> chunk =
          trackerEntities.subList(from, Math.min(from + CHUNK_SIZE, trackerEntities.size()));
      unexpectedErrors.addAll(delete(trackerActions, chunk));
      chunk.forEach(e -> TestRunStorage.removeEntity(e.getValue(), e.getKey()));
    }

    if (!unexpectedErrors.isEmpty()) {
      throw new IllegalStateException(
          "Failed to delete tracker data created by tests. It is left behind and might affect"
              + " other tests: "
              + unexpectedErrors);
    }
    return true;
  }

  private List<String> delete(
      TrackerImportExportActions trackerActions, List<Map.Entry<String, String>> entities) {
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            toPayload(entities),
            new QueryParamsBuilder()
                .add("importStrategy", "DELETE")
                .add("atomicMode", "OBJECT")
                .add("reportMode", "ERRORS")
                .add("async", "false"));

    // 409 is returned if no object could be deleted, for example if all were deleted already
    if (response.statusCode() != 200 && response.statusCode() != 409) {
      return List.of(
          String.format(
              "tracker DELETE import failed with status %d: %s",
              response.statusCode(), response.getAsString()));
    }

    logger.info(
        "Deleted {} of {} registered tracker objects",
        response.extract("stats.deleted"),
        entities.size());

    List<Map<String, Object>> errorReports = response.extractList("validationReport.errorReports");
    if (errorReports == null) {
      return List.of();
    }
    return errorReports.stream()
        .filter(error -> !EXPECTED_ERROR_CODES.contains(String.valueOf(error.get("errorCode"))))
        .map(error -> String.format("%s: %s", error.get("errorCode"), error.get("message")))
        .toList();
  }

  private static JsonObject toPayload(List<Map.Entry<String, String>> entities) {
    JsonObject payload = new JsonObject();
    for (Map.Entry<String, String> entity : entities) {
      PayloadKeys keys = RESOURCE_TO_PAYLOAD_KEYS.get(entity.getValue());
      if (!payload.has(keys.collection())) {
        payload.add(keys.collection(), new JsonArray());
      }
      JsonObject object = new JsonObject();
      object.addProperty(keys.uid(), entity.getKey());
      payload.getAsJsonArray(keys.collection()).add(object);
    }
    return payload;
  }
}
