/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonMetadataVersion;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MetadataVersionControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  @DisplayName("A valid metadata version should be returned from the versions endpoint")
  void getMetadataVersionsTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject response = GET("/metadata/versions").content().as(JsonObject.class);
    JsonMetadataVersion metadataVersion =
        response.getArray("metadataversions").get(0, JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName("A valid current metadata version should be returned from the version endpoint")
  void getCurrentMetadataVersionTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonMetadataVersion metadataVersion =
        GET("/metadata/version").content().as(JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName("A valid metadata version by name should be returned from the version endpoint")
  void getMetadataVersionByNameTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonMetadataVersion metadataVersion =
        GET("/metadata/version?versionName=Version_2").content().as(JsonMetadataVersion.class);

    // then
    assertEquals("Version_2", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName(
      "Valid metadata version history should be returned from the version history endpoint")
  void getCurrentMetadataVersionHistoryTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject response = GET("/metadata/version/history").content().as(JsonObject.class);
    JsonMetadataVersion metadataVersion =
        response.getArray("metadataversions").get(0, JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName(
      "Valid metadata version history by name should be returned from the version history endpoint")
  void getCurrentMetadataVersionHistoryByNameTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonArray versions =
        GET("/metadata/version/history?versionName=Version_2")
            .content()
            .as(JsonObject.class)
            .getArray("metadataversions");

    // then
    assertEquals(2, versions.size());
  }

  @Test
  @DisplayName("Valid metadata version data should be returned from the version data endpoint")
  void getCurrentMetadataVersionDataTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject systemData =
        GET("/metadata/version/Version_2/data").content().as(JsonObject.class).getObject("system");

    // then
    assertNotNull(systemData.getString("id"));
    assertTrue(DateUtils.dateIsValid(systemData.getString("date").string()), "date is valid date");
  }

  @Test
  @DisplayName("Valid metadata version data (zipped) call should be successful")
  void getCurrentMetadataVersionDataZippedTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    HttpResponse response = GET("/metadata/version/Version_2/data.gz");

    // then
    assertTrue(response.success(), "response is successful");
    assertEquals("application/gzip", response.header("Content-Type"));
  }
}
