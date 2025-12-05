/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.fileresource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.response.Response;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hisp.dhis.ApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileResourceTest extends ApiTest {

  @Test
  @DisplayName(
      "Adding an org unit file resource results in 3 extra images saved (small, medium, large)")
  void orgUnitMultiImageTest() {
    // when creating an org unit file resource
    File file = new File("src/test/resources/fileResources/org_unit.png");
    String frUid = postFileResource(file, "ORG_UNIT");
    assertTrue(frUid != null && !frUid.isEmpty());

    // then there are small, medium & large versions of the original file
    assertMultiImageState(frUid);
  }

  @Test
  @DisplayName(
      "Adding a user avatar file resource results in 3 extra images saved (small, medium, large)")
  void avatarMultiImageTest() {
    // when creating an avatar file resource
    File file = new File("src/test/resources/fileResources/user_avatar.png");
    String frUid = postFileResource(file, "USER_AVATAR");
    assertTrue(frUid != null && !frUid.isEmpty());

    // then there are small, medium & large versions of the original file
    assertMultiImageState(frUid);
  }

  private void assertMultiImageState(String frUid) {
    // wait 3 seconds at most until multiple file sizes exist
    Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> twoDiffFileSizesPresent(frUid));

    // then there are small, medium & large versions of the original file
    // if this feature is not working correctly, then all of these calls return the default
    // original size, and will all have the same size Content-length
    long small = getFileResourceSizeByDimension(frUid, "small");
    long medium = getFileResourceSizeByDimension(frUid, "medium");
    long large = getFileResourceSizeByDimension(frUid, "large");

    // all are positive sizes
    assertTrue(small > 0 && medium > 0 && large > 0);

    // 3 different sizes exist
    assertTrue(large > medium);
    assertTrue(medium > small);
  }

  private Boolean twoDiffFileSizesPresent(String frUid) {
    long small = getFileResourceSizeByDimension(frUid, "small");
    long medium = getFileResourceSizeByDimension(frUid, "medium");
    return small > 0 && small < medium;
  }

  private long getFileResourceSizeByDimension(String uid, String dimension) {
    Response response =
        given().param("dimension", dimension).when().get("/fileResources/" + uid + "/data");

    String header = response.getHeader("Content-Length");
    return header != null ? Long.parseLong(header) : -1;
  }

  private String postFileResource(File file, String domain) {
    return given()
        .multiPart("file", file, "image/png")
        .formParam("domain", domain)
        .contentType("multipart/form-data")
        .when()
        .post("/fileResources")
        .then()
        .statusCode(202)
        .extract()
        .path("response.fileResource.id");
  }
}
