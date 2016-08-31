package org.hisp.dhis.mapgeneration;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.geotools.geometry.jts.JTSFactoryFinder;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Factory for producing GeoTools geometric primitives from coordinates in json.
 * 
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class GeoToolsPrimitiveFromJsonFactory
{
    // Factory creating GeoTools geometric primitives
    private static final GeometryFactory FACTORY = JTSFactoryFinder.getGeometryFactory( null );

    /**
     * Create a GeoTools geometric point primitive from coordinates in json.
     * 
     * @param json the json array of components
     * @return the point
     */
    public static Point createPointFromJson( JsonNode json )
    {
        return FACTORY.createPoint( createCoordinateFromJson( json ) );
    }

    /**
     * Create a GeoTools geometric coordinate primitive from coordinates in
     * json.
     * 
     * @param json the json array of components
     * @return the coordinate
     */
    public static Coordinate createCoordinateFromJson( JsonNode json )
    {
        // Parse the double values from the json and create the coordinate
        return new Coordinate( json.get( 0 ).asDouble(), json.get( 1 ).asDouble() );
    }

    /**
     * Create a GeoTools geometric multi-polygon primitive from coordinates in
     * json.
     * 
     * @param json the json array of polygons
     * @return the multi-polygon
     */
    public static MultiPolygon createMultiPolygonFromJson( JsonNode json )
    {
        // Native array of polygons to pass to GeoFactory
        Polygon[] polygons = new Polygon[MapUtils.getNonEmptyNodes( json )];

        // Read all the polygons from the json array
        for ( int i = 0; i < json.size(); i++ )
        {
            JsonNode node = json.get( i );
            
            if ( MapUtils.nodeIsNonEmpty( node ) )
            {
                polygons[i] = createPolygonFromJson( node );
            }
        }

        // Create the multi-polygon from factory
        return FACTORY.createMultiPolygon( polygons );
    }

    /**
     * Create a GeoTools geometric polygon primitive from coordinates in json.
     * 
     * @param json the json array of linear ring
     * @return the polygon
     */
    public static Polygon createPolygonFromJson( JsonNode json )
    {
        // Get the json array of coordinates representing the shell and make a
        // linear-ring out of them
        JsonNode shell = json.get( 0 );
        LinearRing sh = createLinearRingFromJson( shell );

        // Native array of linear-ring holes to pass to GeoFactory
        LinearRing[] holes = null;

        // Get the linear-ring holes if the polygon has any holes
        if ( json.size() > 1 )
        {
            // Allocate memory for the holes, i.e. minus the shell
            holes = new LinearRing[shell.size() - 1];

            // Read the json array of linear-ring into holes
            for ( int i = 1; i < shell.size(); i++ )
            {
                JsonNode hole = json.get( i );
                
                if ( hole != null && hole.size() > 0 )
                {
                    holes[i] = createLinearRingFromJson( hole );
                }
            }
        }

        // Create the polygon from factory
        return FACTORY.createPolygon( sh, holes );
    }

    /**
     * Create a GeoTools geometric linear-ring from coordinates in json.
     * 
     * @param json the json array of coordinates
     * @return the linear-ring
     */
    public static LinearRing createLinearRingFromJson( JsonNode json )
    {
        // Native array of coordinates to pass to GeoFactory
        Coordinate[] coords = new Coordinate[MapUtils.getNonEmptyNodes( json )];

        // Read the json array of coordinates
        for ( int i = 0; i < json.size(); i++ )
        {
            JsonNode node = json.get( i );
            
            if ( MapUtils.nodeIsNonEmpty( node ) )
            {
                coords[i] = createCoordinateFromJson( node );
            }
        }

        // Create the linear-ring from factory
        return FACTORY.createLinearRing( coords );
    }
}
