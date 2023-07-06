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
package org.hisp.dhis.commons.jackson.config.geometry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Arrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/** A custom serializer for a Geometry */
/*
 * A custom serializer is needed as
 * com.graphhopper.external:jackson-datatype-jts is obsolete and not compatible
 * with last version of Hibernate library
 */
public class GeometrySerializer extends JsonSerializer<Geometry> {
  private static final String COORDINATES = "coordinates";

  public void serialize(Geometry value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {
    this.writeGeometry(jgen, value);
  }

  public void writeGeometry(JsonGenerator jgen, Geometry value) throws IOException {
    if (value instanceof Polygon) {
      this.writePolygon(jgen, (Polygon) value);
    } else if (value instanceof Point) {
      this.writePoint(jgen, (Point) value);
    } else if (value instanceof MultiPoint) {
      this.writeMultiPoint(jgen, (MultiPoint) value);
    } else if (value instanceof MultiPolygon) {
      this.writeMultiPolygon(jgen, (MultiPolygon) value);
    } else if (value instanceof LineString) {
      this.writeLineString(jgen, (LineString) value);
    } else if (value instanceof MultiLineString) {
      this.writeMultiLineString(jgen, (MultiLineString) value);
    } else {
      if (!(value instanceof GeometryCollection)) {
        throw new RuntimeJsonMappingException(
            "Geometry type "
                + value.getClass().getName()
                + " cannot be serialized as GeoJSON.Supported types are: "
                + Arrays.asList(
                    Point.class.getName(),
                    LineString.class.getName(),
                    Polygon.class.getName(),
                    MultiPoint.class.getName(),
                    MultiLineString.class.getName(),
                    MultiPolygon.class.getName(),
                    GeometryCollection.class.getName()));
      }

      this.writeGeometryCollection(jgen, (GeometryCollection) value);
    }
  }

  private void writeGeometryCollection(JsonGenerator jgen, GeometryCollection value)
      throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "GeometryCollection");
    jgen.writeArrayFieldStart("geometries");

    for (int i = 0; i != value.getNumGeometries(); ++i) {
      this.writeGeometry(jgen, value.getGeometryN(i));
    }

    jgen.writeEndArray();
    jgen.writeEndObject();
  }

  private void writeMultiPoint(JsonGenerator jgen, MultiPoint value) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "MultiPoint");
    jgen.writeArrayFieldStart(COORDINATES);

    for (int i = 0; i != value.getNumGeometries(); ++i) {
      this.writePointCoords(jgen, (Point) value.getGeometryN(i));
    }

    jgen.writeEndArray();
    jgen.writeEndObject();
  }

  private void writeMultiLineString(JsonGenerator jgen, MultiLineString value) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "MultiLineString");
    jgen.writeArrayFieldStart(COORDINATES);

    for (int i = 0; i != value.getNumGeometries(); ++i) {
      this.writeLineStringCoords(jgen, (LineString) value.getGeometryN(i));
    }

    jgen.writeEndArray();
    jgen.writeEndObject();
  }

  @Override
  public Class<Geometry> handledType() {
    return Geometry.class;
  }

  private void writeMultiPolygon(JsonGenerator jgen, MultiPolygon value) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "MultiPolygon");
    jgen.writeArrayFieldStart(COORDINATES);

    for (int i = 0; i != value.getNumGeometries(); ++i) {
      this.writePolygonCoordinates(jgen, (Polygon) value.getGeometryN(i));
    }

    jgen.writeEndArray();
    jgen.writeEndObject();
  }

  private void writePolygon(JsonGenerator jgen, Polygon value) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "Polygon");
    jgen.writeFieldName(COORDINATES);
    this.writePolygonCoordinates(jgen, value);
    jgen.writeEndObject();
  }

  private void writePolygonCoordinates(JsonGenerator jgen, Polygon value) throws IOException {
    jgen.writeStartArray();
    this.writeLineStringCoords(jgen, value.getExteriorRing());

    for (int i = 0; i < value.getNumInteriorRing(); ++i) {
      this.writeLineStringCoords(jgen, value.getInteriorRingN(i));
    }

    jgen.writeEndArray();
  }

  private void writeLineStringCoords(JsonGenerator jgen, LineString ring) throws IOException {
    jgen.writeStartArray();

    for (int i = 0; i != ring.getNumPoints(); ++i) {
      Point p = ring.getPointN(i);
      this.writePointCoords(jgen, p);
    }

    jgen.writeEndArray();
  }

  private void writeLineString(JsonGenerator jgen, LineString lineString) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "LineString");
    jgen.writeFieldName(COORDINATES);
    this.writeLineStringCoords(jgen, lineString);
    jgen.writeEndObject();
  }

  private void writePoint(JsonGenerator jgen, Point p) throws IOException {
    jgen.writeStartObject();
    jgen.writeStringField("type", "Point");
    jgen.writeFieldName(COORDINATES);
    this.writePointCoords(jgen, p);
    jgen.writeEndObject();
  }

  private void writePointCoords(JsonGenerator jgen, Point p) throws IOException {
    jgen.writeStartArray();
    jgen.writeNumber(p.getCoordinate().x);
    jgen.writeNumber(p.getCoordinate().y);
    if (!Double.isNaN(p.getCoordinate().z)) {
      jgen.writeNumber(p.getCoordinate().z);
    }

    jgen.writeEndArray();
  }
}
