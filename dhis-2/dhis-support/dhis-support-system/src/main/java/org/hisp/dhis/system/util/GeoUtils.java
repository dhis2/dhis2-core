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
package org.hisp.dhis.system.util;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.referencing.GeodeticCalculator;
import org.hisp.dhis.common.coordinate.CoordinateUtils;
import org.hisp.dhis.organisationunit.FeatureType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * @author Lars Helge Overland
 */
public class GeoUtils
{
    private static final Pattern SVG_TEXT_PATTERN = Pattern.compile( "text=\"(.*?)\"", Pattern.DOTALL );

    private static final String SVG_FONT_REGEX = "(\\s+)font=\"(.*?)\"";

    public static final int SRID = 4326;

    /**
     * Returns boundaries of a box shape which centre is the point defined by
     * the given longitude and latitude. The distance between the center point
     * and the edges of the box is defined in meters by the given distance.
     * Based on standard EPSG:4326 long/lat projection. The result is an array
     * of length 4 where the values at each index are:
     *
     * <ul>
     * <li>Index 0: Maximum latitude (north edge of box shape).</li>
     * <li>Index 1: Maximum longitude (east edge of box shape).</li>
     * <li>Index 2: Minimum latitude (south edge of box shape).</li>
     * <li>Index 3: Minimum longitude (west edge of box shape).</li>
     * </ul>
     *
     * @param longitude the longitude.
     * @param latitude the latitude.
     * @param distance the distance in meters to each box edge.
     * @return an array of length 4.
     */
    public static double[] getBoxShape( double longitude, double latitude, double distance )
    {
        double[] box = new double[4];

        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint( longitude, latitude );

        calc.setDirection( 0, distance );
        Point2D north = calc.getDestinationGeographicPoint();

        calc.setDirection( 90, distance );
        Point2D east = calc.getDestinationGeographicPoint();

        calc.setDirection( 180, distance );
        Point2D south = calc.getDestinationGeographicPoint();

        calc.setDirection( -90, distance );
        Point2D west = calc.getDestinationGeographicPoint();

        box[0] = north.getY();
        box[1] = east.getX();
        box[2] = south.getY();
        box[3] = west.getX();

        return box;
    }

    /**
     * Computes the distance between two points.
     *
     * @param from the origin point.
     * @param to the end point.
     * @return the orthodromic distance between the given points.
     */
    public static double getDistanceBetweenTwoPoints( Point2D from, Point2D to )
    {
        GeodeticCalculator calc = new GeodeticCalculator();
        calc.setStartingGeographicPoint( from );
        calc.setDestinationGeographicPoint( to );

        return calc.getOrthodromicDistance();
    }

    /**
     * Get GeometryJSON point.
     *
     * @param longitude the longitude.
     * @param latitude the latitude.
     * @return the GeoJSON representation of the given point.
     */
    public static Point getGeoJsonPoint( double longitude, double latitude )
        throws IOException
    {
        Point point = new GeometryJSON()
            .readPoint( new StringReader( "{\"type\":\"Point\", \"coordinates\":[" + longitude + ","
                + latitude + "]}" ) );

        point.setSRID( SRID );

        return point;
    }

    /**
     * Check if GeometryJSON point created with this coordinate is valid.
     *
     * @param latitude the latitude.
     * @param longitude the longitude.
     * @return true if the point is valid or false.
     */
    public static boolean checkGeoJsonPointValid( double longitude, double latitude )
    {
        try
        {
            return getGeoJsonPoint( longitude, latitude ).isValid();
        }
        catch ( Exception ex )
        {
            return false;
        }
    }

    /**
     * Check if the point coordinate falls within the polygon/MultiPolygon Shape
     *
     * @param longitude the longitude.
     * @param latitude the latitude.
     * @param geometry the GeoJSON coordinates of the MultiPolygon
     */
    public static boolean checkPointWithMultiPolygon( double longitude, double latitude,
        Geometry geometry )
    {
        try
        {
            boolean contains = false;

            Point point = getGeoJsonPoint( longitude, latitude );

            FeatureType featureType = FeatureType.getTypeFromName( geometry.getGeometryType() );

            if ( point != null && point.isValid() )
            {
                if ( featureType == FeatureType.POLYGON )
                {
                    Polygon polygon = (Polygon) geometry;
                    contains = polygon.contains( point );
                }
                else if ( featureType == FeatureType.MULTI_POLYGON )
                {
                    MultiPolygon multiPolygon = (MultiPolygon) geometry;
                    contains = multiPolygon.contains( point );
                }
            }

            return contains;
        }
        catch ( Exception ex )
        {
            return false;
        }
    }

    /**
     * Escapes the String encoded SVG.
     *
     * @param svg the String encoded SVG.
     * @return the escaped representation.
     */
    public static String replaceUnsafeSvgText( String svg )
    {
        if ( svg == null )
        {
            return null;
        }

        svg = replaceText( svg );
        svg = replaceInvalidPatterns( svg );

        return svg;
    }

    private static String replaceText( String svg )
    {
        StringBuffer sb = new StringBuffer();

        Matcher textMatcher = SVG_TEXT_PATTERN.matcher( svg );

        while ( textMatcher.find() )
        {
            String text = textMatcher.group( 1 );

            if ( text != null && !text.isEmpty() )
            {
                text = "text=\"" + text.replaceAll( "[<>&]", "" ) + "\"";
                textMatcher.appendReplacement( sb, text );
            }
        }

        return textMatcher.appendTail( sb ).toString();
    }

    private static String replaceInvalidPatterns( String svg )
    {
        svg = svg.replaceAll( SVG_FONT_REGEX, StringUtils.EMPTY );
        svg = svg.replaceAll( "fill=\"transparent\"", "fill=\"none\"" );

        return svg;
    }

    public static Geometry getGeometryFromCoordinatesAndType( FeatureType featureType, String coordinates )
        throws IOException
    {
        GeometryJSON geometryJSON = new GeometryJSON();

        if ( featureType.equals( FeatureType.NONE ) || featureType.equals( FeatureType.SYMBOL ) )
        {
            throw new IllegalArgumentException( "Invalid featureType '" + featureType.value() + "'" );
        }

        Geometry geometry = geometryJSON
            .read( String.format( "{\"type\": \"%s\", \"coordinates\": %s}", featureType.value(), coordinates ) );

        geometry.setSRID( SRID );

        return geometry;
    }

    public static String getCoordinatesFromGeometry( Geometry geometry )
    {
        return CoordinateUtils.getCoordinatesFromGeometry( geometry );
    }

}
