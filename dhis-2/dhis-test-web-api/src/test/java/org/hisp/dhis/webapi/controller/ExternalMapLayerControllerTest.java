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

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.mapping.ExternalMapLayerController} using
 * (mocked) REST requests.
 */
@Transactional
class ExternalMapLayerControllerTest extends H2ControllerIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Test
  void testPost() {
    String id =
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
                        "url": "http://test",
                        "descriptionUrl": "Test description url",
                        "image": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCA"
                        }
                    """));

    JsonObject layer = GET("/externalMapLayers/{uid}", id).content();

    assertEquals("Test", layer.getString("name").string());
    assertEquals("http://test", layer.getString("url").string());
    assertEquals("Test description url", layer.getString("descriptionUrl").string());
    assertEquals(
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCA",
        layer.getString("image").string());
  }

  @Test
  void testPostImageIsTooLarge() {
    StringBuilder largeImage = new StringBuilder();
    for (int i = 0; i < 2097153; i++) {
      largeImage.append("a");
    }

    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/externalMapLayers",
            """
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
                .replace("$image", largeImage.toString())));
  }

  @Test
  void testPostImageIsInvalid() {
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/externalMapLayers",
            """
                    {
                    "name": "Test",
                    "imageFormat": "PNG",
                    "mapService": "XYZ",
                    "mapLayerPosition": "BASEMAP",
                    "url": "http://test",
                    "descriptionUrl": "Test description url",
                    "image": "invalid image"
                    }
                """));
  }

  @Test
  void testPostImageIsBlank() {
    assertStatus(
        HttpStatus.CONFLICT,
        POST(
            "/externalMapLayers",
            """
                    {
                    "name": "Test",
                    "imageFormat": "PNG",
                    "mapService": "XYZ",
                    "mapLayerPosition": "BASEMAP",
                    "url": "http://test",
                    "descriptionUrl": "Test description url",
                    "image": "  "
                    }
                """));
  }
}
