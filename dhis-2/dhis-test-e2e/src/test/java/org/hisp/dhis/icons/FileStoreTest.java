/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.icons;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.MultiPartSpecification;
import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test support of multiple file store providers. */
class FileStoreTest extends ApiTest {

  private LoginActions loginActions;
  private RestApiActions fileResourceActions;
  private RestApiActions iconApi;

  @BeforeAll
  void beforeAll() {
    loginActions = new LoginActions();
    fileResourceActions = new RestApiActions("fileResources");
    iconApi = new RestApiActions("icons");
  }

  @AfterAll
  void cleanUp() {
    iconApi.delete("dhis2-icon");
    // file resource can't be deleted through the API, only through the FileResourceCleanupJob
  }

  @Test
  void shouldStoreFileResourceInExternalStore() {
    loginActions.loginAsSuperUser();

    File file = new File(getClass().getClassLoader().getResource("dhis2.png").getFile());
    MultiPartSpecification multiPart =
        new MultiPartSpecBuilder(file).fileName("dhis2.png").mimeType("image/png").build();
    String fileResourceId =
        given()
            .queryParam("domain", "ICON")
            .multiPart(multiPart)
            .contentType(ContentType.MULTIPART)
            .when()
            .post("/fileResources")
            .then()
            .statusCode(202)
            .extract()
            .jsonPath()
            .getString("response.fileResource.id");

    Callable<Boolean> fileIsStored =
        () ->
            "STORED"
                .equals(
                    fileResourceActions
                        .get("/" + fileResourceId)
                        .validateStatus(200)
                        .extract("storageStatus"));

    await()
        .atMost(4, TimeUnit.SECONDS)
        .pollInterval(200, TimeUnit.MILLISECONDS)
        .until(fileIsStored);

    fileResourceActions.get("/" + fileResourceId + "/data").validateStatus(200);
  }

  @Test
  @DisplayName("Creating an Icon with a FileResource results in an assigned FileResource")
  void test() {
    // given a FileResource exists
    File file = new File("src/test/resources/fileResources/dhis2.png");
    String frUid =
        given()
            .multiPart("file", file)
            .formParam("domain", "ICON")
            .contentType("multipart/form-data")
            .when()
            .post("/fileResources")
            .then()
            .statusCode(202)
            .extract()
            .path("response.fileResource.id");

    assertTrue(frUid != null && !frUid.isEmpty());

    // and it is unassigned
    fileResourceActions.get(frUid).validate().body("assigned", equalTo(false));

    // when creating an Icon with a ref to the FileResource
    iconApi
        .post(
            """
            {
               "id":"iconUid00x1",
               "fileResourceId":"%s",
               "key": "dhis2-icon",
               "description": "icon test",
               "keywords": []
            }
            """
                .formatted(frUid))
        .validate()
        .statusCode(201);

    // then the FileResource should now be assigned
    fileResourceActions.get(frUid).validate().body("assigned", equalTo(true));
  }
}
