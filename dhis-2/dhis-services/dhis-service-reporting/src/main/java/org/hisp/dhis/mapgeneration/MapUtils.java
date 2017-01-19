package org.hisp.dhis.mapgeneration;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility class.
 * 
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class MapUtils
{
    private static final String COLOR_PREFIX = "#";
    private static final int COLOR_RADIX = 16;

    public static final int DEFAULT_MAP_WIDTH = 500;
    public static final int TITLE_HEIGHT = 20;

    /**
     * Linear interpolation of int.
     * 
     * @param a from
     * @param b to
     * @param t factor, typically 0-1
     * @return the interpolated int
     */
    public static int lerp( int a, int b, double t )
    {
        return a + (int) ((b - a) * t);
    }

    /**
     * Linear interpolation of double.
     * 
     * @param a from
     * @param b to
     * @param t factor, typically 0-1
     * @return the interpolated double
     */
    public static double lerp( double a, double b, double t )
    {
        return a + ((b - a) * t);
    }

    /**
     * Linear interpolation of RGB colors.
     * 
     * @param a from
     * @param b to
     * @param t interpolation factor, typically 0-1
     * @return the interpolated color
     */
    public static Color lerp( Color a, Color b, double t )
    {
        return new Color( lerp( a.getRed(), b.getRed(), t ), lerp( a.getGreen(), b.getGreen(), t ), lerp( a.getBlue(),
            b.getBlue(), t ), lerp( a.getAlpha(), b.getAlpha(), t ) );
    }

    /**
     * Creates a java.awt.Color from a dhis style color string, e.g. '#ff3200'
     * is an orange color.
     * 
     * @param string the color in string, e.g. '#ff3200'
     * @return the Color, or null if string is null or empty.
     */
    public static Color createColorFromString( String string )
    {
        if ( string == null || string.trim().isEmpty() )
        {
            return null;
        }
        
        string = string.startsWith( COLOR_PREFIX ) ? string.substring( 1 ) : string;
        
        return new Color( Integer.parseInt( string, COLOR_RADIX ) );
    }
    
    /**
     * Returns the number of non empty sub JsonNodes in the given JsonNode.
     * 
     * @param json the JsonNode.
     * @return the number of non empty sub JsonNodes.
     */
    public static int getNonEmptyNodes( JsonNode json )
    {
        int count = 0;
        
        for ( int i = 0; i < json.size(); i++ )
        {
            JsonNode node = json.get( i );
            
            count = nodeIsNonEmpty( node ) ? ++count : count;
        }
        
        return count;
    }
    
    /**
     * Indicates whether the given JsonNode is empty, which implies that the
     * node is not null and has a size greater than 0.
     * 
     * @param json the JsonNode.
     * @return true if the given JsonNode is non empty, false otherwise.
     */
    public static boolean nodeIsNonEmpty( JsonNode json )
    {
        return json != null && json.size() > 0;
    }
    
    // -------------------------------------------------------------------------
    // Map
    // -------------------------------------------------------------------------

    public static BufferedImage render( InternalMap map, Integer maxWidth, Integer maxHeight )
    {
        MapContent mapContent = new MapContent();

        // Convert map objects to features, and add them to the map
        
        for ( InternalMapLayer mapLayer : map.getLayers() )
        {
            for ( InternalMapObject mapObject : mapLayer.getMapObjects() )
            {
                mapContent.addLayer( createFeatureLayerFromMapObject( mapObject ) );
            }
        }

        // Create a renderer for this map
        
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent( mapContent );

        // Calculate image height
        
        ReferencedEnvelope mapBounds = mapContent.getMaxBounds();
        double widthToHeightFactor = mapBounds.getSpan( 0 ) / mapBounds.getSpan( 1 );
        int[] widthHeight = getWidthHeight( maxWidth, maxHeight, LegendSet.LEGEND_TOTAL_WIDTH, TITLE_HEIGHT, widthToHeightFactor );
        
        //LegendSet.LEGEND_TOTAL_WIDTH;
        
        Rectangle imageBounds = new Rectangle( 0, 0, widthHeight[0], widthHeight[1] );

        // Create an image and get the graphics context from it
        
        BufferedImage image = new BufferedImage( imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_ARGB );
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        renderer.paint( graphics, imageBounds, mapBounds );

        mapContent.dispose();
        
        return image;
    }

    public static BufferedImage renderTitle( String title, Integer width )
    {        
        BufferedImage image = new BufferedImage( width, TITLE_HEIGHT, BufferedImage.TYPE_INT_ARGB );
        Graphics2D g = (Graphics2D) image.getGraphics();
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( Color.BLACK );
        g.setFont( Legend.TITLE_FONT );
        g.drawString( title, LegendSet.LEGEND_MARGIN_LEFT, 12 );
        
        return image;
    }

    /**
     * Calculates the width and height of an two-dimensional area. If width is not
     * null, the width will be used and the height will be calculated. If the height 
     * is not null, the height will be used and the width will be calculated. If 
     * both width and height are not null, the width or height will be adjusted 
     * to the greatest value possible without exceeding any of max width and max 
     * height.
     * 
     * @param maxWidth the maximum width.
     * @param maxHeight the maximum height.
     * @param subtractWidth the value to subtract from final width
     * @param subtractHeight the value to subtract from final height 
     * @param widthFactor the width to height factor.
     * @return array where first position holds the width and second the height.
     * @throws IllegalArgumentException if none of width and height are specified.
     */
    public static int[] getWidthHeight( Integer maxWidth, Integer maxHeight, int subtractWidth, int subtractHeight, double widthFactor )
    {
        if ( maxWidth == null && maxHeight == null )
        {
            throw new IllegalArgumentException( "At least one of width and height must be specified" );
        }
        
        if ( maxWidth == null )
        {
            maxHeight -= subtractHeight;
            maxWidth = (int) Math.ceil( maxHeight * widthFactor );
        }   
        else if ( maxHeight == null )
        {
            maxWidth -= subtractWidth;
            maxHeight = (int) Math.ceil( maxWidth / widthFactor );
        }
        else // Both set
        {
            maxWidth -= subtractWidth;
            maxHeight -= subtractHeight;
            
            double maxWidthFactor = (double) maxWidth / maxHeight;
            
            if ( maxWidthFactor > widthFactor ) // Canvas wider than area
            {
                maxWidth = (int) Math.ceil( maxHeight * widthFactor );
            }
            else // Area wider than canvas
            {
                maxHeight = (int) Math.ceil( maxWidth / widthFactor );
            }
        }
        
        int[] result = { maxWidth, maxHeight };
        
        return result;
    }

    /**
     * Creates a feature layer based on a map object.
     */
    public static Layer createFeatureLayerFromMapObject( InternalMapObject mapObject )
    {
        Style style = mapObject.getStyle();
        
        SimpleFeatureType featureType = mapObject.getFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder( featureType );
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        
        featureBuilder.add( mapObject.getGeometry() );
        SimpleFeature feature = featureBuilder.buildFeature( null );

        featureCollection.add( feature );

        return new FeatureLayer( featureCollection, style );
    }

    /**
     * Creates an image with text indicating an error.
     */
    public static BufferedImage createErrorImage( String error )
    {
        String str = "Error creating map image: " + error;
        BufferedImage image = new BufferedImage( 500, 25, BufferedImage.TYPE_INT_RGB );
        Graphics2D graphics = image.createGraphics();

        graphics.setColor( Color.WHITE );
        graphics.fill( new Rectangle( 500, 25 ) );

        graphics.setColor( Color.RED );
        graphics.drawString( str, 1, 12 );

        return image;
    }  
}
