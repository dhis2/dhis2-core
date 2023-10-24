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
package org.hisp.dhis.metadata;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DataSetMetadataTest extends ApiTest {

  private RestApiActions restApiActions;

  @BeforeAll
  public void beforeAll() {
    restApiActions = new RestApiActions("dataEntry/metadata");
    LoginActions loginActions = new LoginActions();
    loginActions.loginAsSuperUser();
  }

  @Test
  void dataSetMetadataEtagFunctionalityTest() {
    // call endpoint to get current state
    Response response1 = restApiActions.get().validate().extract().response();

    int statusCode1 = response1.getStatusCode();
    assertEquals(200, statusCode1);
    String responseBody1 = response1.body().asString();
    assertNotNull(responseBody1);
    // get etag value from current state
    String eTagValue1 = response1.getHeader(HttpHeaders.ETAG);

    // make the same call again, this time passing the 'If-None-Match' header and the ETag value
    // from response 1
    Headers headers = new Headers(new Header(HttpHeaders.IF_NONE_MATCH, eTagValue1));
    Response response2 =
        restApiActions.getWithHeaders("", null, headers).validate().extract().response();

    int statusCode2 = response2.getStatusCode();

    // response status code should be 304 to indicate that the data has not changed
    assertEquals(304, statusCode2);

    // body should be empty as no data returned when no change in data
    assertEquals("", response2.body().asString());

    // ETags should match from response 1 & 2
    String eTagValue2 = response2.getHeader(HttpHeaders.ETAG);
    assertNotNull(eTagValue1);
    assertEquals(34, eTagValue2.length());
    assertEquals(eTagValue1, eTagValue2);

    // create new data set to trigger a change of data seen by the API
    given()
        .header("Content-type", "application/json")
        .and()
        .body(newDataSet())
        .when()
        .post("http://localhost:8080/api/dataSets")
        .then()
        .statusCode(201);

    // call again with 'If-None-Match' header and the previous ETag header value
    Response response3 =
        restApiActions.getWithHeaders("", null, headers).validate().extract().response();

    // new ETag should be received
    String eTagValue3 = response3.getHeader(HttpHeaders.ETAG);
    assertNotEquals(eTagValue1, eTagValue3);

    int statusCode3 = response3.getStatusCode();

    // response 1 & 3 bodies should not match
    assertNotEquals(responseBody1, response3.body().asString());
    assertEquals(200, statusCode3);

    assertNotNull(eTagValue3);
    assertEquals(34, eTagValue3.length());
    // ETags should not match
    assertNotEquals(eTagValue1, eTagValue3);
  }

  private String newDataSet() {
    return """
      {
        "name": "e2e test dataset 1",
        "shortName": "e2e test dataset 1",
        "periodType": "Daily",
        "organisationUnits": []
      }
    """
        .strip();
  }
}
