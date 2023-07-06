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
package org.hisp.dhis.actions.metadata;

import com.google.gson.JsonObject;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class SharingActions extends RestApiActions {
  public SharingActions() {
    super("/sharing");
  }

  public void setupSharingForConfiguredUserGroup(String type, String id) {

    JsonObject jsonObject =
        this.get(new QueryParamsBuilder().add("type=" + type).add("id=" + id).build()).getBody();

    jsonObject.add(
        "object",
        JsonObjectBuilder.jsonObject()
            .addProperty("publicAccess", "--------")
            .addUserGroupAccess()
            .build());

    this.post(jsonObject, new QueryParamsBuilder().add("type=" + type).add("id=" + id))
        .validate()
        .statusCode(200);
  }

  public void setupSharingForUsers(String type, String id, String... userIds) {
    JsonObject jsonObject =
        this.get(new QueryParamsBuilder().add("type=" + type).add("id=" + id).build()).getBody();

    for (String userId : userIds) {
      JsonObjectBuilder.jsonObject(jsonObject.getAsJsonObject("object"))
          .addOrAppendToArray(
              "userAccesses",
              new JsonObjectBuilder()
                  .addProperty("id", userId)
                  .addProperty("access", "rw------")
                  .build());
    }

    this.post(jsonObject, new QueryParamsBuilder().add("type=" + type).add("id=" + id))
        .validate()
        .statusCode(200);
  }
}
