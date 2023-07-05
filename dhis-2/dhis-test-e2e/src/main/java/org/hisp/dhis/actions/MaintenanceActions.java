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
package org.hisp.dhis.actions;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MaintenanceActions extends RestApiActions {
  private Logger logger = LogManager.getLogger(MaintenanceActions.class.getName());

  public MaintenanceActions() {
    super("/maintenance");
  }

  public void removeSoftDeletedRelationships() {
    sendRequest(true, "softDeletedRelationshipRemoval=true");
  }

  public void removeSoftDeletedEvents() {
    sendRequest(true, "softDeletedEventRemoval=true");
  }

  public void removeSoftDeletedData() {
    sendRequest(
        true,
        "softDeletedEventRemoval=true",
        "softDeletedTrackedEntityRemoval=true",
        "softDeletedEnrollmentRemoval=true",
        "softDeletedRelationshipRemoval=true",
        "softDeletedDataValueRemoval=true");
  }

  private void sendRequest(boolean validate, String... queryParams) {
    ApiResponse apiResponse =
        super.post(new JsonObject(), new QueryParamsBuilder().addAll(queryParams));

    if (validate) {
      apiResponse.validate().statusCode(204);
      return;
    }

    if (apiResponse.statusCode() != 204) {
      logger.warn(
          String.format(
              "Maintenance failed with query params %s. Response: %s",
              queryParams, apiResponse.getBody().toString()));
    }
  }
}
