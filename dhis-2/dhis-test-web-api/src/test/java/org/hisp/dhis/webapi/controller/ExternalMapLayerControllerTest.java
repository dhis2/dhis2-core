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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.mapping.ExternalMapLayerController} using
 * (mocked) REST requests.
 */
@Transactional
class ExternalMapLayerControllerTest extends H2ControllerIntegrationTestBase {

  private static final String VALID_IMAGE =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCA";

  private static final Pattern ERROR_CODE = Pattern.compile("\"errorCode\"\\s*:\\s*\"(E\\d+)\"");

  @Test
  void testPost() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));

    JsonObject layer = GET("/externalMapLayers/{uid}", id).content();

    assertEquals("Test", layer.getString("name").string());
    assertEquals("http://test", layer.getString("url").string());
    assertEquals("Test description url", layer.getString("descriptionUrl").string());
    assertEquals(VALID_IMAGE, layer.getString("image").string());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "data:image/png;base64,iVBORw0KGgo=",
        "data:image/png;base64,iVBORw0KGg==",
        "data:image/jpeg;base64,/9j/4AAQSkZJRg+/",
        "data:image/jpg;base64,/9j/4AAQSkZJRg",
        "data:image/webp;base64,UklGRiQAAABXRUJQ"
      })
  void testPostValidImages(String image) {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(image)));
    assertEquals(image, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "data:image/png;base64,iVBORw0KGgo=",
        "data:image/png;base64,iVBORw0KGg==",
        "data:image/jpeg;base64,/9j/4AAQSkZJRg+/",
        "data:image/jpg;base64,/9j/4AAQSkZJRg",
        "data:image/webp;base64,UklGRiQAAABXRUJQ"
      })
  void testPostNoImage() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/externalMapLayers",
            """
              {
              "name": "Test",
              "imageFormat": "PNG",
              "mapService": "XYZ",
              "mapLayerPosition": "BASEMAP",
              "url": "http://test"
              }
              """));
  }

  void testPostNullImage() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/externalMapLayers",
            """
            {
            "name": "Test",
            "imageFormat": "PNG",
            "image": null,
            "mapService": "XYZ",
            "mapLayerPosition": "BASEMAP",
            "url": "http://test"
            }
            """));
  }

  /** It handles it as null. */
  void testPostEmptyImage() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/externalMapLayers",
            """
            {
            "name": "Test",
            "imageFormat": "PNG",
            "image": "",
            "mapService": "XYZ",
            "mapLayerPosition": "BASEMAP",
            "url": "http://test"
            }
            """));
  }

  @Test
  void testPostWithoutImage() {
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/externalMapLayers",
            """
            {
            "name": "Test",
            "imageFormat": "PNG",
            "mapService": "XYZ",
            "mapLayerPosition": "BASEMAP",
            "url": "http://test"
            }
            """));
  }

  @Test
  void testPostImageIsTooLarge() {
    assertStatus(HttpStatus.CONFLICT, POST("/externalMapLayers", layer(tooLargeImage())));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalid image",
        "  ",
        "data:image/png,AAAA",
        "data:image/png;AAAA",
        "data:image/png;base64,",
        "data:image/png;base64,not base64 !!!",
        "data:image/png;base64,AAAA===",
        "data:image/png;base64,AA=A",
        "data:image/gif;base64,R0lGODlh",
        "data:image/svg+xml;base64,PHN2Zz4=",
        "data:image/PNG;base64,iVBORw0KGgo=",
        "data:text/plain;base64,iVBORw0KGgo=",
        "data:image/png;base64,iVBORw0KGgo=\\n",
        " data:image/png;base64,iVBORw0KGgo="
      })
  void testPostImageIsInvalid(String image) {
    assertInvalidImage(POST("/externalMapLayers", layer(image)));
  }

  @Test
  void testPutImageIsInvalid() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));

    assertInvalidImage(PUT("/externalMapLayers/" + id, layer("data:image/png,AAAA")));
    assertEquals(
        VALID_IMAGE, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @Test
  void testPutImageIsValid() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));
    String newImage = "data:image/webp;base64,UklGRiQAAABXRUJQ";

    assertStatus(HttpStatus.OK, PUT("/externalMapLayers/" + id, layer(newImage)));
    assertEquals(
        newImage, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @Test
  void testPatchImageIsInvalid() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));

    assertInvalidImage(
        PATCH(
            "/externalMapLayers/" + id,
            "[{'op': 'replace', 'path': '/image', 'value': 'not an image'}]"));
    assertEquals(
        VALID_IMAGE, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @Test
  void testPatchImageIsTooLarge() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));

    assertStatus(
        HttpStatus.CONFLICT,
        PATCH(
            "/externalMapLayers/" + id,
            "[{'op': 'replace', 'path': '/image', 'value': '" + tooLargeImage() + "'}]"));
    assertEquals(
        VALID_IMAGE, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @Test
  void testPatchImageIsValid() {
    String id = assertStatus(HttpStatus.CREATED, POST("/externalMapLayers", layer(VALID_IMAGE)));
    String newImage = "data:image/jpeg;base64,/9j/4AAQSkZJRg==";

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/externalMapLayers/" + id,
            "[{'op': 'replace', 'path': '/image', 'value': '" + newImage + "'}]"));
    assertEquals(
        newImage, GET("/externalMapLayers/{uid}", id).content().getString("image").string());
  }

  @Test
  void testMetadataImportImageIsInvalid() {
    assertInvalidImage(POST("/metadata", metadata("ExtMapLay01", "not an image")));
    assertStatus(HttpStatus.NOT_FOUND, GET("/externalMapLayers/ExtMapLay01"));
  }

  @Test
  void testMetadataImportImageIsInvalidOnUpdate() {
    assertStatus(HttpStatus.OK, POST("/metadata", metadata("ExtMapLay02", VALID_IMAGE)));

    assertInvalidImage(POST("/metadata", metadata("ExtMapLay02", "data:image/png;AAAA")));
    assertEquals(
        VALID_IMAGE, GET("/externalMapLayers/ExtMapLay02").content().getString("image").string());
  }

  @Test
  void testMetadataImportImageIsTooLarge() {
    assertStatus(HttpStatus.CONFLICT, POST("/metadata", metadata("ExtMapLay03", tooLargeImage())));
    assertStatus(HttpStatus.NOT_FOUND, GET("/externalMapLayers/ExtMapLay03"));
  }

  @Test
  void testMetadataImportImageIsValid() {
    assertStatus(HttpStatus.OK, POST("/metadata", metadata("ExtMapLay04", VALID_IMAGE)));
    assertEquals(
        VALID_IMAGE, GET("/externalMapLayers/ExtMapLay04").content().getString("image").string());
  }

  private void assertInvalidImage(HttpResponse response) {
    JsonObject body = response.content(HttpStatus.CONFLICT);
    List<String> errorCodes = errorCodes(body.toJson());
    assertTrue(errorCodes.contains("E1552"), "Expected E1552 but got: " + body.toJson());
  }

  private static List<String> errorCodes(String json) {
    List<String> codes = new ArrayList<>();
    Matcher matcher = ERROR_CODE.matcher(json);
    while (matcher.find()) {
      codes.add(matcher.group(1));
    }
    return codes;
  }

  /** A syntactically valid image whose length exceeds the 2 MB limit. */
  private static String tooLargeImage() {
    return "data:image/png;base64," + "A".repeat(2097152);
  }

  private static String layer(String image) {
    return """
        {
        "name": "Test",
        "imageFormat": "PNG",
        "mapService": "XYZ",
        "mapLayerPosition": "BASEMAP",
        "url": "http://test",
        "descriptionUrl": "Test description url",
        "image": "$image"
        }
        """
        .replace("$image", image);
  }

  private static String metadata(String uid, String image) {
    return """
        {
        "externalMapLayers": [
          {
          "id": "$uid",
          "name": "Test $uid",
          "imageFormat": "PNG",
          "mapService": "XYZ",
          "mapLayerPosition": "BASEMAP",
          "url": "http://test",
          "image": "$image"
          }
        ]
        }
        """
        .replace("$uid", uid)
        .replace("$image", image);
  }
}
