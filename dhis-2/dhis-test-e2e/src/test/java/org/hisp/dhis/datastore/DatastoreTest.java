/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.datastore;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DatastoreTest extends ApiTest {

  private RestApiActions datastoreApiActions;
  private RestApiActions sharingActions;
  private LoginActions loginActions;

  private static String NAMESPACE = "football";

  @BeforeAll
  public void beforeAll() {
    datastoreApiActions = new RestApiActions("dataStore");
    sharingActions = new RestApiActions("sharing");
    loginActions = new LoginActions();
    //    loginActions.loginAsSuperUser();
  }

  @Test
  void datastoreTest() {
    // put 2 entries into namespace
    loginActions.loginAsAdmin();

    // call as superuser to fields api
    String key1 = "arsenal";
    String key2 = "spurs";
    datastoreApiActions.post("/"+ NAMESPACE + "/" + key1, newEntry(key1)).validate().statusCode(201);
    datastoreApiActions.post("/"+ NAMESPACE + "/" + key2, newEntry(key2)).validate().statusCode(201);

    // call fields api with fields name
//    ApiResponse response = dataStoreApiActions.get("/"+ NAMESPACE + "?fields=league");
    QueryParamsBuilder paramsBuilder = new QueryParamsBuilder().add("fields", "league");
    ApiResponse response = datastoreApiActions.get("/"+ NAMESPACE, paramsBuilder);

    JsonArray entries = response.getBody().getAsJsonArray("entries");
    //    String entries = response.getAsString();
    assertEquals("{\"key\":\"arsenal\",\"league\":\"prem\"}", entries.get(0).getAsJsonObject().getAsString());
    assertEquals("{\"key\":\"spurs\",\"league\":\"prem\"}", entries.get(1).getAsJsonObject().getAsString());
    assertTrue(entries.size() == 2);

    // make the same call again, this time passing the 'If-None-Match' header and the ETag value
    // from response 1
  }

  private String newEntry(String team) {
    return """
      {"name": "%s","league": "prem"}
    """
        .strip()
        .formatted(team);
  }
}
