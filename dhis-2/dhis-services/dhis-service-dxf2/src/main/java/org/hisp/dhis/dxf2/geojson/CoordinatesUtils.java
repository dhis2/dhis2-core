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
package org.hisp.dhis.dxf2.geojson;

import java.util.function.BiConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;

/**
 * Utility to adjust GeoJSON coordinates of a geometry.
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoordinatesUtils {
  public static String geometryWithCoordinatePairs(JsonObject geometry) {
    if (geometry.isUndefined()) {
      return geometry.node().getDeclaration();
    }
    JsonArray coordinates = geometry.getArray("coordinates");
    int dimensions = coordinateDimensions(coordinates);
    JsonArray pair = coordinates;
    for (int d = 1; d < dimensions; d++) {
      pair = pair.getArray(0);
    }
    if (pair.size() == 2 || dimensions > 4) {
      return geometry.node().getDeclaration();
    }
    geometry = JsonValue.of(geometry.node().extract()).asObject(); // geometry as root node
    JsonArray newCoordinates = geometry.getArray("coordinates");
    return newCoordinates.node().replaceWith(coordinatesAsPairs(newCoordinates)).getDeclaration();
  }

  public static boolean coordinatesEmpty(JsonValue coordinates) {
    if (!coordinates.exists()) {
      return true;
    }
    if (!coordinates.isArray()) {
      return coordinates.isNull();
    }
    if (coordinates.as(JsonArray.class).isEmpty()) {
      return true;
    }
    if (coordinates.as(JsonArray.class).size() > 1) {
      return false;
    }
    return coordinatesEmpty(coordinates.as(JsonArray.class).get(0));
  }

  /**
   * @param coordinates a GeoJSON coordinates value
   * @return The number of array dimensions the provided GeoJSON coordinates have
   */
  public static int coordinateDimensions(JsonValue coordinates) {
    if (!coordinates.isArray()) {
      return 0;
    }
    JsonValue first = coordinates.as(JsonArray.class).get(0);
    int dimensions = 1;
    while (first.isArray()) {
      dimensions++;
      first = first.as(JsonArray.class).get(0);
    }
    return dimensions;
  }

  /**
   * @see #coordinatesAsPairs(JsonArray)
   * @param coordinates a GeoJSON coordinate array with 1 to 4 dimensions
   * @return GeoJSON of the provided array with innermost dimension reduced to pairs
   */
  public static String coordinatesAsPairs(String coordinates) {
    return coordinatesAsPairs(JsonValue.of(coordinates).as(JsonArray.class));
  }

  /**
   * Returns the provided coordinates reduced to only value pairs for the innermost arrays for 1-4
   * dimensions.
   *
   * <pre>
   *     [1]                  => [1]
   *     [1,2]                => [1,2]
   *     [1,2,3]              => [1,2]
   *     [[1,2,3],[4,5,6]]    => [[1,2],[4,5]]
   * </pre>
   *
   * @param coordinates a GeoJSON coordinate array with 1 to 4 dimensions
   * @return GeoJSON of the provided array with innermost dimension reduced to pairs
   */
  public static String coordinatesAsPairs(JsonArray coordinates) {
    int dimensions = coordinateDimensions(coordinates);
    StringBuilder asPairs = new StringBuilder();
    switch (dimensions) {
      case 1:
        coordinatesAsPairsDim1(coordinates, asPairs);
        break;
      case 2:
        coordinatesAsPairsDim2(coordinates, asPairs);
        break;
      case 3:
        coordinatesAsPairsDim3(coordinates, asPairs);
        break;
      case 4:
        coordinatesAsPairsDim4(coordinates, asPairs);
        break;
      default:
        throw new UnsupportedOperationException("Coordinates format not supported");
    }
    return asPairs.toString();
  }

  /**
   * E.g. {@code Point}
   *
   * <pre>
   *     [30.0, 10.0]
   * </pre>
   */
  private static void coordinatesAsPairsDim1(JsonValue coordinates, StringBuilder asPairs) {
    coordinatesAsPairs(
        2, coordinates, asPairs, (value, str) -> str.append(value.node().getDeclaration()));
  }

  /**
   * E.g. {@code LineString}
   *
   * <pre>
   *      [ [30.0, 10.0], [10.0, 30.0], [40.0, 40.0] ]
   * </pre>
   */
  private static void coordinatesAsPairsDim2(JsonValue coordinates, StringBuilder asPairs) {
    coordinatesAsPairs(-1, coordinates, asPairs, CoordinatesUtils::coordinatesAsPairsDim1);
  }

  /**
   * E.g. {@code Polygon}s
   *
   * <pre>
   *     [ [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ] ]
   * </pre>
   */
  private static void coordinatesAsPairsDim3(JsonValue coordinates, StringBuilder asPairs) {
    coordinatesAsPairs(-1, coordinates, asPairs, CoordinatesUtils::coordinatesAsPairsDim2);
  }

  /**
   * E.g. {@code MultiPolygon}s
   *
   * <pre>
   *     [[
   *             [[30.0, 20.0], [45.0, 40.0], [10.0, 40.0], [30.0, 20.0]]
   *         ], [
   *             [[15.0, 5.0], [40.0, 10.0], [10.0, 20.0], [5.0, 10.0], [15.0, 5.0]]
   *     ]]
   * </pre>
   */
  private static void coordinatesAsPairsDim4(JsonValue coordinates, StringBuilder asPairs) {
    coordinatesAsPairs(-1, coordinates, asPairs, CoordinatesUtils::coordinatesAsPairsDim3);
  }

  private static void coordinatesAsPairs(
      int n,
      JsonValue coordinates,
      StringBuilder asPairs,
      BiConsumer<JsonValue, StringBuilder> appendElement) {
    JsonArray arr = coordinates.as(JsonArray.class);
    int len = n <= 0 ? arr.size() : Math.min(n, arr.size());
    asPairs.append("[");
    for (int i = 0; i < len; i++) {
      if (i > 0) {
        asPairs.append(',');
      }
      appendElement.accept(arr.get(i), asPairs);
    }
    asPairs.append("]");
  }
}
