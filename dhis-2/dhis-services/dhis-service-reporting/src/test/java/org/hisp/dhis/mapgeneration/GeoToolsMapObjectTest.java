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
package org.hisp.dhis.mapgeneration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;
import java.io.IOException;
import org.geotools.geojson.geom.GeometryJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Kenneth Solbø Andersen <kennetsa@ifi.uio.no>
 */
class GeoToolsMapObjectTest {

  private InternalMapObject geoToolsMapObject;
  private GeometryJSON geometryJSON = new GeometryJSON();
  private String multiPolygonCoordinates =
      "[[[[11.11,22.22],[33.33,44.44],[55.55,66.66],[11.11,22.22]]],"
          + "[[[77.77,88.88],[99.99,11.11],[22.22,33.33],[77.77,88.88]]],"
          + "[[[44.44,55.55],[66.66,77.77],[88.88,99.99],[44.44,55.55]]]]";

  @BeforeEach
  void before() {
    geoToolsMapObject = new InternalMapObject();
  }

  @Test
  void testSetGetName() {
    geoToolsMapObject.setName("Name1");
    assertEquals("Name1", geoToolsMapObject.getName());
    geoToolsMapObject.setName("Another name");
    assertEquals("Another name", geoToolsMapObject.getName());
  }

  @Test
  void testSetGetValue() {
    geoToolsMapObject.setValue(489.3);
    assertEquals(geoToolsMapObject.getValue(), 0.00001, 489.3);
    geoToolsMapObject.setValue(41.423);
    assertEquals(geoToolsMapObject.getValue(), 0.00001, 41.423);
  }

  @Test
  void testSetGetRadius() {
    geoToolsMapObject.setRadius(32);
    assertEquals(geoToolsMapObject.getRadius(), 0.00001, 32.5264F);
    geoToolsMapObject.setRadius(61);
    assertEquals(geoToolsMapObject.getRadius(), 0.00001, 61441.5F);
  }

  @Test
  void testSetGetFillColor() {
    geoToolsMapObject.setFillColor(Color.BLUE);
    assertEquals(Color.BLUE, geoToolsMapObject.getFillColor());
    geoToolsMapObject.setFillColor(Color.CYAN);
    assertEquals(Color.CYAN, geoToolsMapObject.getFillColor());
  }

  @Test
  void testSetGetFillOpacity() {
    geoToolsMapObject.setFillOpacity(5.23F);
    assertEquals(geoToolsMapObject.getFillOpacity(), 0.00001, 5.23F);
    geoToolsMapObject.setFillOpacity(594208420.134F);
    assertEquals(geoToolsMapObject.getFillOpacity(), 0.00001, 594208420.134F);
  }

  @Test
  void testSetGetStrokeColor() {
    geoToolsMapObject.setStrokeColor(Color.GREEN);
    assertEquals(Color.GREEN, geoToolsMapObject.getStrokeColor());
    geoToolsMapObject.setStrokeColor(Color.WHITE);
    assertEquals(Color.WHITE, geoToolsMapObject.getStrokeColor());
  }

  @Test
  void testCreateFeatureType() throws IOException {
    Geometry geometry =
        geometryJSON.read(
            "{\"type\":\"MultiPolygon\", \"coordinates\":" + multiPolygonCoordinates + "}");
    geoToolsMapObject.setGeometry(geometry);
    assertDoesNotThrow(
        () -> {
          assertNotNull(MapUtils.createFeatureLayerFromMapObject(geoToolsMapObject));
        });
  }
}
