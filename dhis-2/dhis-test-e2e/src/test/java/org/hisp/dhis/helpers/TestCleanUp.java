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
package org.hisp.dhis.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.test.e2e.TestRunStorage;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.MaintenanceActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestCleanUp {
  private final Logger logger = LogManager.getLogger(TestCleanUp.class.getName());

  /** Number of passes over the non-tracker entities, to retry deletes blocked by references. */
  private static final int MAX_DELETE_PASSES = 3;

  /**
   * Non-tracker resources whose deletion is a soft delete. Soft-deleted rows block deletion of the
   * objects they reference until a {@code /maintenance} sweep physically purges them. Tracker data
   * is soft deleted too, see {@link TrackerCleanUp}. Everything else (plain metadata) is a hard
   * delete and needs no sweep.
   */
  private static final Set<String> SOFT_DELETABLE_RESOURCES = Set.of("dataValues", "dataValueSets");

  /**
   * Deletes all entities created during the test run.
   *
   * <ol>
   *   <li>Tracker data is deleted in bulk using a tracker import, see {@link TrackerCleanUp}.
   *   <li>Soft-deleted data is purged, so it does not block the deletion of metadata.
   *   <li>Remaining entities are deleted one by one, starting from the last created one. Deletes
   *       that fail, for example because the entity is still referenced, are retried in another
   *       pass.
   * </ol>
   *
   * @throws IllegalStateException if tracker data could not be deleted. All other entities are
   *     still attempted to be deleted.
   */
  public void deleteCreatedEntities() {
    IllegalStateException trackerCleanUpFailure = null;
    boolean purgeSoftDeletedData;
    try {
      purgeSoftDeletedData = new TrackerCleanUp().deleteCreatedTrackerData();
    } catch (IllegalStateException e) {
      trackerCleanUpFailure = e;
      purgeSoftDeletedData = true;
    }

    for (int pass = 0; pass < MAX_DELETE_PASSES; pass++) {
      // purge at most once per pass and only if soft-deletable data was deleted, as it is a
      // DB-wide sweep
      if (purgeSoftDeletedData) {
        new MaintenanceActions().removeSoftDeletedData();
      }

      Map<String, String> createdEntities = TestRunStorage.getCreatedEntities();
      if (createdEntities.isEmpty()) {
        break;
      }

      List<String> reverseOrderedKeys = new ArrayList<>(createdEntities.keySet());
      Collections.reverse(reverseOrderedKeys);

      purgeSoftDeletedData = false;
      for (String key : reverseOrderedKeys) {
        String resource = createdEntities.get(key);
        if (deleteEntity(resource, key)) {
          TestRunStorage.removeEntity(resource, key);
          purgeSoftDeletedData |= isSoftDeletable(resource);
        }
      }
    }

    TestRunStorage.removeAllEntities();

    if (trackerCleanUpFailure != null) {
      throw trackerCleanUpFailure;
    }
  }

  private static boolean isSoftDeletable(String resource) {
    if (resource == null) {
      return false;
    }
    String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
    return SOFT_DELETABLE_RESOURCES.contains(normalized);
  }

  /**
   * Deletes entities created during test run.
   *
   * @param resources I.E /organisationUnits to delete created OU's.
   */
  public void deleteCreatedEntities(String... resources) {
    Map<String, String> trackerEntities = new LinkedHashMap<>();
    for (String resource : resources) {
      if (TrackerCleanUp.isTrackerResource(resource)) {
        TestRunStorage.getCreatedEntities(resource)
            .forEach(id -> trackerEntities.put(id, resource));
      }
    }
    new TrackerCleanUp().deleteTrackerData(trackerEntities);

    new LoginActions().loginAsSuperUser();

    for (String resource : resources) {
      if (TrackerCleanUp.isTrackerResource(resource)) {
        continue;
      }
      List<String> entityIds = TestRunStorage.getCreatedEntities(resource);

      Iterator<String> iterator = entityIds.iterator();

      while (iterator.hasNext()) {
        boolean deleted = deleteEntity(resource, iterator.next());
        if (deleted) {
          iterator.remove();
        }
      }
    }
  }

  public void deleteCreatedEntities(LinkedHashMap<String, String> entitiesToDelete) {

    for (String key : entitiesToDelete.keySet()) {
      deleteEntity(entitiesToDelete.get(key), key);
    }
  }

  public boolean deleteEntity(String resource, String id) {
    if (TrackerCleanUp.isTrackerResource(resource)) {
      // tracker objects cannot be deleted via DELETE /api/{resource}/{uid}
      return new TrackerCleanUp().deleteTrackerData(Map.of(id, resource));
    }

    ApiResponse response = new RestApiActions(resource).delete(id + "?force=true");

    if (response.statusCode() == 200 || response.statusCode() == 404) {
      logger.info(String.format("Entity from resource %s with id %s deleted", resource, id));

      if (response.containsImportSummaries()) {
        return response.extract("response.importCount.deleted").equals(1);
      }

      return true;
    }

    logger.warn(
        String.format(
            "Entity from resource %s with id %s was not deleted. Status code: %s",
            resource, id, response.statusCode()));

    return false;
  }
}
