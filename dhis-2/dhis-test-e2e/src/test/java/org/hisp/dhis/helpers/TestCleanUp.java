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
package org.hisp.dhis.helpers;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.TestRunStorage;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.MaintenanceActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TestCleanUp {
  private Logger logger = LogManager.getLogger(TestCleanUp.class.getName());

  private int deleteCount = 0;

  /**
   * Deletes entities created during test run. Entities deleted one by one starting from last
   * created one.
   */
  public void deleteCreatedEntities() {
    Map<String, String> createdEntities = TestRunStorage.getCreatedEntities();
    List<String> reverseOrderedKeys = new ArrayList<>(createdEntities.keySet());
    Collections.reverse(reverseOrderedKeys);

    Iterator<String> iterator = reverseOrderedKeys.iterator();

    while (iterator.hasNext()) {
      String key = iterator.next();
      boolean deleted = deleteEntity(createdEntities.get(key), key);
      if (deleted) {
        TestRunStorage.removeEntity(createdEntities.get(key), key);
        createdEntities.remove(createdEntities.get(key), key);
      }

      new MaintenanceActions().removeSoftDeletedData();
    }

    while (deleteCount < 2 && !createdEntities.isEmpty()) {
      deleteCount++;
      deleteCreatedEntities();
    }

    TestRunStorage.removeAllEntities();
  }

  /**
   * Deletes entities created during test run.
   *
   * @param resources I.E /organisationUnits to delete created OU's.
   */
  public void deleteCreatedEntities(String... resources) {
    new LoginActions().loginAsSuperUser();

    for (String resource : resources) {
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
    Iterator<String> iterator = entitiesToDelete.keySet().iterator();

    while (iterator.hasNext()) {
      String key = iterator.next();

      deleteEntity(entitiesToDelete.get(key), key);
    }
  }

  public boolean deleteEntity(String resource, String id) {
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
