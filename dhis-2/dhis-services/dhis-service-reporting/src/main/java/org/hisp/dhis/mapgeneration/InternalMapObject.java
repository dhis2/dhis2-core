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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.IOException;

/**
 * An internal representation of a map object (feature) in a map layer.
 * <p>
 * It encapsulates all the information of an atomic object on a map, i.e. its
 * name, value, fill color, fill opacity, stroke color, stroke width, and
 * potentially its radius should it be represented as a point.
 * <p>
 * It may be the associated with an interval of an interval set and should be
 * associated with a map layer.
 * <p>
 * Finally, one should extend this class with an implementation that uses a
 * specific platform, e.g. GeoTools to draw the map.
 *
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class InternalMapObject
{
    private static final float LINE_STROKE_WIDTH = 0.1f;

    private static final String CIRCLE = "Circle";
    private static final String POINT = "Point";
    private static final String POLYGON = "Polygon";
    private static final String MULTI_POLYGON = "MultiPolygon";
    private static final String GEOMETRIES = "geometries";

    public static final String TYPE_THEMATIC = "thematic";
    public static final String TYPE_BOUNDARY = "boundary";

    protected String name;

    protected double value;

    protected int radius;

    protected Color fillColor;

    protected float fillOpacity;

    protected Color strokeColor;

    protected InternalMapLayer mapLayer;

    protected Interval interval;

    private Geometry geometry;

    private MapLayerType mapLayerType;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public InternalMapObject()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Builds the GeoTools geometric primitive for a given organisation unit and
     * sets it for this map object.
     * <p>
     * Quick guide to how geometry is stored in DHIS:
     * <p>
     * Geometry for org units is stored in the DB as [[[[0.32, -33.87], [23.99,
     * -43.02], ...]]], and may be retrieved by calling the getCoordinates
     * method of OrganisationUnit.
     * <p>
     * The coordinates vary according to feature type, which can be found with a
     * call to getFeatureType of OrganisationUnit. It varies between the
     * following structures (names are omitted in the actual coordinates
     * string):
     * <p>
     * multipolygon = [ polygon0 = [ shell0 = [ point0 = [0.32, -33.87], point1
     * = [23.99, -43.02], point2 = [...]], hole0 = [...], hole1 = [...]],
     * polygon1 = [...] polygon2 = [...]] polygon = [ shell0 = [ point0 = [0.32,
     * -33.87], point1 = [23.99, -43.02]], hole0 = [...], hole1 = [...]]
     * <p>
     * point = [0.32, -33.87]
     * <p>
     * Multi-polygons are stored as an array of polygons. Polygons are stored as
     * an array of linear-rings, where the first linear-ring is the shell, and
     * remaining linear-rings are the holes in the polygon. Linear-rings are
     * stored as an array of points, which in turn is stored as an array of
     * (two) components as a floating point type.
     * <p>
     * There are three types of geometry that may be stored in a DHIS org unit:
     * point, polygon, and multi-polygon. This method supports all three.
     * <p>
     * NOTE However, as of writing, there is a bug in DHIS OrganisationUnit
     * where when getFeatureType reports type Polygon, getCoordinates really
     * returns coordinates in the format of type MultiPolygon.
     *
     * @param orgUnit the organisation unit
     */
    public void buildGeometryForOrganisationUnit( OrganisationUnit orgUnit )
    {
        // The final GeoTools primitive
        Geometry primitive = null;

        // The DHIS coordinates as string
        String coords = orgUnit.getCoordinates();

        // The json root that is parsed from the coordinate string
        JsonNode root = null;

        try
        {
            // Create a parser for the json and parse it into root
            JsonParser parser = new ObjectMapper().getFactory().createParser( coords );
            root = parser.readValueAsTree();
        }
        catch ( IOException ex )
        {
            throw new RuntimeException( "Failed to parse JSON", ex );
        }

        // Use the factory to build the correct type based on the feature type
        // Polygon is treated similarly as MultiPolygon        
        if ( orgUnit.getFeatureType() == FeatureType.POINT )
        {
            primitive = GeoToolsPrimitiveFromJsonFactory.createPointFromJson( root );
        }
        else if ( orgUnit.getFeatureType() == FeatureType.POLYGON )
        {
            primitive = GeoToolsPrimitiveFromJsonFactory.createMultiPolygonFromJson( root );
        }
        else if ( orgUnit.getFeatureType() == FeatureType.MULTI_POLYGON )
        {
            primitive = GeoToolsPrimitiveFromJsonFactory.createMultiPolygonFromJson( root );
        }
        else
        {
            throw new RuntimeException( "Not sure what to do with the feature type '" + orgUnit.getFeatureType() + "'" );
        }

        this.geometry = primitive;
    }

    public Style getStyle()
    {
        Style style = null;

        if ( geometry instanceof Point )
        {
            style = SLD.createPointStyle( CIRCLE, strokeColor, fillColor,
                fillOpacity, radius );
        }
        else if ( geometry instanceof Polygon || geometry instanceof MultiPolygon )
        {
            if ( MapLayerType.BOUNDARY.equals( mapLayerType ) )
            {
                style = SLD.createLineStyle( strokeColor, LINE_STROKE_WIDTH );
            }
            else
            {
                style = SLD.createPolygonStyle( strokeColor, fillColor, fillOpacity );
            }
        }
        else
        {
            style = SLD.createSimpleStyle( getFeatureType() );
        }

        return style;
    }

    /**
     * Creates a feature type for a GeoTools geometric primitive.
     */
    public SimpleFeatureType getFeatureType()
    {
        String type = "";

        if ( geometry instanceof Point )
        {
            type = POINT;
        }
        else if ( geometry instanceof Polygon )
        {
            type = POLYGON;
        }
        else if ( geometry instanceof MultiPolygon )
        {
            type = MULTI_POLYGON;
        }
        else
        {
            throw new IllegalArgumentException();
        }

        try
        {
            return DataUtilities.createType( GEOMETRIES, "geometry:" + type + ":srid=3785" );
        }
        catch ( SchemaException ex )
        {
            throw new RuntimeException( "failed to create geometry", ex );
        }
    }


    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public double getValue()
    {
        return this.value;
    }

    public void setValue( double value )
    {
        this.value = value;
    }

    public int getRadius()
    {
        return this.radius;
    }

    public void setRadius( int radius )
    {
        this.radius = radius;
    }

    public Color getFillColor()
    {
        return this.fillColor;
    }

    public void setFillColor( Color fillColor )
    {
        this.fillColor = fillColor;
    }

    public float getFillOpacity()
    {
        return this.fillOpacity;
    }

    public void setFillOpacity( float fillOpacity )
    {
        this.fillOpacity = fillOpacity;
    }

    public Color getStrokeColor()
    {
        return this.strokeColor;
    }

    public void setStrokeColor( Color strokeColor )
    {
        this.strokeColor = strokeColor;
    }

    public InternalMapLayer getMapLayer()
    {
        return this.mapLayer;
    }

    public void setMapLayer( InternalMapLayer mapLayer )
    {
        this.mapLayer = mapLayer;
    }

    public Interval getInterval()
    {
        return this.interval;
    }

    public void setInterval( Interval interval )
    {
        this.interval = interval;
        this.fillColor = interval.getColor();
    }

    public Geometry getGeometry()
    {
        return this.geometry;
    }

    public void setGeometry( Geometry geometry )
    {
        this.geometry = geometry;
    }

    public MapLayerType getMapLayerType()
    {
        return mapLayerType;
    }

    public void setMapLayerType( MapLayerType mapLayerType )
    {
        this.mapLayerType = mapLayerType;
    }

    @Override
    public String toString()
    {
        return String.format( "InternalMapObject {" + " name: \"%s\"," + " value: %.2f," + " radius: %d,"
                + " fillColor: %s," + " fillOpacity: %.2f" + " strokeColor: %s" + " }", name, value,
            radius, fillColor, fillOpacity, strokeColor );
    }
}
