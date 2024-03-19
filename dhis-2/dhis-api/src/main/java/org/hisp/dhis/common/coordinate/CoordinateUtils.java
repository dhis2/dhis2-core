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
package org.hisp.dhis.common.coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.organisationunit.CoordinatesTuple;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

/**
 * @author Henning Håkonsen
 */
public class CoordinateUtils {
  private static final Pattern JSON_POINT_PATTERN = Pattern.compile("(\\[.*?])");

  private static final Pattern JSON_COORDINATE_PATTERN = Pattern.compile("(\\[{3}.*?]{3})");

  private static final Pattern COORDINATE_PATTERN = Pattern.compile("([\\-0-9.]+,[\\-0-9.]+)");

  public static boolean hasDescendantsWithCoordinates(Set<OrganisationUnit> organisationUnits) {
    return organisationUnits.stream().anyMatch(OrganisationUnit::hasCoordinates);
  }

  public static boolean isPolygon(FeatureType featureType) {
    return featureType != null && featureType.isPolygon();
  }

  public static boolean isPoint(FeatureType featureType) {
    return featureType != null && featureType == FeatureType.POINT;
  }

  public static List<CoordinatesTuple> getCoordinatesAsList(
      String coordinates, FeatureType featureType) {
    List<CoordinatesTuple> list = new ArrayList<>();

    if (coordinates != null && !coordinates.trim().isEmpty()) {
      Matcher jsonMatcher =
          isPoint(featureType)
              ? JSON_POINT_PATTERN.matcher(coordinates)
              : JSON_COORDINATE_PATTERN.matcher(coordinates);

      while (jsonMatcher.find()) {
        CoordinatesTuple tuple = new CoordinatesTuple();

        Matcher matcher = COORDINATE_PATTERN.matcher(jsonMatcher.group());

        while (matcher.find()) {
          tuple.addCoordinates(matcher.group());
        }

        list.add(tuple);
      }
    }

    return list;
  }

  public static String getCoordinatesFromGeometry(Geometry geometry) {
    String coordinatesKey = "\"coordinates\":";
    String crsKey = ",\"crs\":";

    GeoJsonWriter gjw = new GeoJsonWriter();
    String geojson = gjw.write(geometry).trim();

    return geojson.substring(
        geojson.indexOf(coordinatesKey) + coordinatesKey.length(), geojson.indexOf(crsKey));
  }
}
