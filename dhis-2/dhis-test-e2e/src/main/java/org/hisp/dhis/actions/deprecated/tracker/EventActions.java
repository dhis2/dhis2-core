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
package org.hisp.dhis.actions.deprecated.tracker;

import com.google.gson.JsonObject;
import java.util.List;
import org.hisp.dhis.actions.MaintenanceActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 * @deprecated this is a base test class for "old" (deprecated) tracker which will be removed with
 *     "old" tracker
 */
@Deprecated(since = "2.41")
public class EventActions extends RestApiActions {
  public EventActions() {
    super("/events");
  }

  /**
   * Hard deletes event.
   *
   * @param eventId
   * @return
   */
  @Override
  public ApiResponse delete(String eventId) {
    ApiResponse response = super.delete(eventId);
    new MaintenanceActions().removeSoftDeletedEvents();

    return response;
  }

  public ApiResponse softDelete(String eventId) {
    ApiResponse response = super.delete(eventId);

    response.validate().statusCode(200);

    return response;
  }

  public void softDelete(List<String> eventIds) {
    for (String id : eventIds) {
      softDelete(id);
    }
  }

  public JsonObject createEventBody(String orgUnitId, String programId, String programStageId) {
    JsonObject event = new JsonObject();

    event.addProperty("orgUnit", orgUnitId);
    event.addProperty("program", programId);
    event.addProperty("programStage", programStageId);
    event.addProperty("eventDate", "2018-12-01T00:00:00.000");

    return event;
  }
}
