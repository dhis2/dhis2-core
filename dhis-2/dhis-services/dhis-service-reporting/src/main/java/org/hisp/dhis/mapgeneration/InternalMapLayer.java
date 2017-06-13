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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.mapgeneration.comparator.IntervalLowValueAscComparator;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.util.Assert;

/**
 * An internal representation of a map layer in a map.
 * 
 * It encapsulates all the information of a layer on a map that should contain
 * map objects associated with the same data-set. Thus, a map layer should
 * represent grouped data from a data-set e.g. 'deaths from malaria' is one
 * layer, 'anc coverage' is another layer, etc.
 * 
 * It is typically built using the properties of an external map layer
 * (currently MapView) defined by the user.
 * 
 * Finally, one might extend this class with an implementation that uses a
 * specific platform, if needed.
 * 
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class InternalMapLayer
{
    protected String name;

    protected Period period;

    protected Integer method;

    protected String layer;
    
    protected Integer radiusHigh;

    protected Integer radiusLow;

    protected Color colorHigh;

    protected Color colorLow;

    protected float opacity;
    
    protected Integer classes;
    
    protected IntervalSet intervalSet;

    protected List<InternalMapObject> mapObjects;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public InternalMapLayer()
    {
        this.mapObjects = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "[Name: " + name + ", period: " + period + ", radius high: " + radiusHigh + ", radius low: " + radiusLow +
            ", color high: " + colorHigh + ", color low: " + colorLow + ", classes: " + classes + "]";
    }
    
    /**
     * Indicates whether this layer is a data layer.
     * @return
     */
    public boolean isDataLayer()
    {
        return MapView.DATA_LAYERS.contains( layer );
    }
        
    /**
     * Interpolates the radii of this map layer's set of map objects according
     * the highest and lowest values among them.
     */
    public void applyInterpolatedRadii()
    {
        Assert.notNull( mapObjects, "Map objects cannot be null" );
        Assert.notEmpty( mapObjects, "Map objects cannot be empty" );

        InternalMapObject min = null, max = null;

        // Determine the objects with the min and max values
        for ( InternalMapObject mapObject : mapObjects )
        {
            if ( min == null || mapObject.getValue() < min.getValue() )
            {
                min = mapObject;
            }
            
            if ( max == null || mapObject.getValue() > max.getValue() )
            {
                max = mapObject;
            }
        }

        // Determine and set the radius for each of the map objects according to
        // its value
        for ( InternalMapObject mapObject : mapObjects )
        {
            double factor = (mapObject.getValue() - min.getValue()) / (max.getValue() - min.getValue());
            int radius = MapUtils.lerp( radiusLow, radiusHigh, factor );
            mapObject.setRadius( radius );
        }
    }

    /**
     * Adds a map object to this map layer.
     * 
     * @param mapObject the map object
     */
    public void addMapObject( InternalMapObject mapObject )
    {
        mapObjects.add( mapObject );
    }
    
    /**
     * Indicates whether this map layer has any map objects.
     */
    public boolean hasMapObjects()
    {
        return mapObjects != null && !mapObjects.isEmpty();
    }

    /**
     * Creates a map object and adds it to this map layer. Sets this map layer
     * on the map object.
     * 
     * @param mapValue the map values to set on the map object.
     * @param unit the organisation unit which name to set on the map object.
     */
    public void addDataMapObject( double mapValue, OrganisationUnit unit )
    {
        InternalMapObject mapObject = new InternalMapObject();
        
        mapObject.setName( unit.getName() );
        mapObject.setValue( mapValue );
        mapObject.setFillOpacity( opacity );
        mapObject.setStrokeColor( Color.WHITE );
        mapObject.setMapLayerType( MapLayerType.THEMATIC );

        // Build and set the geometric primitive that outlines org unit on the map
        mapObject.buildGeometryForOrganisationUnit( unit );

        // Add the map object to the map layer
        addMapObject( mapObject );

        // Set the map layer for the map object
        mapObject.setMapLayer( this );
    }
    
    /**
     * Adds a map object for the given organisation unit to this map layer.
     * 
     * @param unit the organisation unit.
     */
    public void addBoundaryMapObject( OrganisationUnit unit )
    {
        InternalMapObject mapObject = new InternalMapObject();
        
        mapObject.setName( unit.getName() );
        mapObject.setFillOpacity( opacity );
        mapObject.setStrokeColor( Color.BLACK );
        mapObject.setMapLayerType( MapLayerType.BOUNDARY );

        mapObject.buildGeometryForOrganisationUnit( unit );
        addMapObject( mapObject );
        mapObject.setMapLayer( this );
    }
    
    /**
     * Sets an interval set on this map layer based on the given legend set.
     * 
     * @param legendSet the legend set.
     */
    public void setIntervalSetFromLegendSet( org.hisp.dhis.legend.LegendSet legendSet )
    {
        IntervalSet intervalSet = new IntervalSet();
        
        for ( org.hisp.dhis.legend.Legend legend : legendSet.getLegends() )
        {
            Color color = MapUtils.createColorFromString( legend.getColor() );
            
            Interval interval = new Interval( color, legend.getStartValue(), legend.getEndValue() );
            
            intervalSet.getIntervals().add( interval );
        }

        Collections.sort( intervalSet.getIntervals(), IntervalLowValueAscComparator.INSTANCE );
        
        this.intervalSet = intervalSet;
    }

    /**
     * Distribute this map layer's map objects into the given interval set and
     * update each map object with its interval.
     */
    public void distributeAndUpdateMapObjectsInIntervalSet()
    {
        for ( InternalMapObject mapObject : mapObjects )
        {
            for ( Interval interval : intervalSet.getIntervals() )
            {
                // If the map object's value is within this interval's
                // boundaries, add it to this interval
                if ( mapObject.getValue() >= interval.getValueLow() && mapObject.getValue() <= interval.getValueHigh() )
                {
                    // Add map object to interval and set interval for map object
                    interval.addMember( mapObject );
                    mapObject.setInterval( interval );

                    // Do not add to more than one interval
                    break;
                }
            }
        }
    }

    /**
     * Creates and applies a fixed length interval set to the given map layer.
     * 
     * How map objects are distributed among intervals depends on the
     * distribution strategy that is used, which may be either 'equal range' or
     * 'equal size'.
     * 
     * The 'equal range' strategy is defined by passing
     * DistributionStrategy.STRATEGY_EQUAL_RANGE to this method. It creates and
     * applies to the given map layer a fixed length interval set distributing
     * map objects into intervals that has the same range.
     * 
     * The 'equal size' strategy is defined by passing
     * DistributionStrategy.STRATEGY_EQUAL_SIZE to this method. It creates and
     * applies to the given map layer a fixed length interval set distributing
     * map objects into intervals that has (optimally) the same amount of map
     * objects.
     * 
     * For example, given the map object collection of a map layer
     * [a:3,b:2,c:5,d:18,e:0,f:50,g:22], where the objects with the lowest and
     * highest values are e:0 and f:50, this collection of map objects will
     * distribute differently into intervals depending on the distribution
     * strategy chosen.
     * 
     * Strategy 'equal range' with length 5: interval [e:0,b:2,a:3,c:5] range
     * 0-10 size 4 interval [d:18] range 11-20 size 1 interval [g:22] range
     * 21-30 size 1 interval [] range 31-40 size 0 interval [f:50] range 41-50
     * size 1
     * 
     * Strategy 'equal size' with length 5: interval [e:0,b:2] range 0-2 size 2
     * interval [a:3,c:5] range 3-5 size 2 interval [d:18] range 5-18 size 1
     * interval [g:22] range 18-22 size 1 interval [f:50] range 22-50 size 1
     * 
     * @param strategy the desired distribution strategy
     * @param mapLayer the map layer whose map objects to distribute
     * @param length the number of intervals in the set
     * @return the created interval set that was applied to this map layer
     */
    public void setAutomaticIntervalSet( int length )
    {
        if ( MapView.METHOD_EQUAL_INTERVALS.equals( method ) )
        {
            setEqualRangeIntervalSet( length );
        }
        else if ( MapView.METHOD_EQUAL_COUNTS.equals( method ) )
        {
            setEqualCountIntervalSet( length );
        }
        else
        {
            throw new RuntimeException( "Unsupported distribution strategy: " + method );
        }
    }

    /**
     * Creates and applies to the given map layer a fixed length interval set
     * distributing map objects into intervals that has the same range.
     * 
     * @param mapLayer the map layer whose map objects to distribute
     * @param length the number of equal sized intervals
     * @return the created interval set that was applied to this map layer
     */
    public void setEqualRangeIntervalSet( int length )
    {
        Assert.isTrue( length > 0, "Lengt must be a positive number" );
        Assert.notEmpty( mapObjects, "Map objects cannot be empty" );

        IntervalSet intervalSet = new IntervalSet().setLowHigh( mapObjects );

        // Set the color for each of the intervals according to highest/lowest values
        for ( int i = 0; i < length; i++ )
        {
            double low = MapUtils.lerp( intervalSet.getObjectLow().getValue(), intervalSet.getObjectHigh().getValue(), ((i + 0d) / length) );
            double high = MapUtils.lerp( intervalSet.getObjectLow().getValue(), intervalSet.getObjectHigh().getValue(), ((i + 1d) / length) );

            Color color = MapUtils.lerp( colorLow, colorHigh, (i + 0.5) / length );

            Interval interval = new Interval( low, high );
            interval.setColor( color );

            intervalSet.getIntervals().add( interval );
        }

        Collections.sort( intervalSet.getIntervals(), IntervalLowValueAscComparator.INSTANCE );
        
        this.intervalSet = intervalSet;
    }

    public void setEqualCountIntervalSet( int length )
    {
        Assert.isTrue( length > 0, "Lengt must be a positive number" );
        Assert.notEmpty( mapObjects, "Map objects cannot be empty" );

        IntervalSet intervalSet = new IntervalSet().setLowHigh( mapObjects );

        List<Double> values = getSortedMapObjectValues();
        
        Assert.notEmpty( values, "Values cannot be empty" );
        
        int range = values.size() / length;
        
        for ( int i = 0; i < length; i++ )
        {
            int lowIndex = range * i;
            int highIndex = Math.max( lowIndex + range - 1, 0 );
            
            double low = values.get( lowIndex );

            double high = values.get( highIndex );
            
            if ( length == i + 1 ) // At last position
            {
                high = values.get( values.size() - 1 );
            }

            Color color = MapUtils.lerp( colorLow, colorHigh, (i + 0.5) / length );

            Interval interval = new Interval( low, high );
            interval.setColor( color );
            
            intervalSet.getIntervals().add( interval );
        }

        Collections.sort( intervalSet.getIntervals(), IntervalLowValueAscComparator.INSTANCE );
        
        this.intervalSet = intervalSet;
    }
    
    private List<Double> getSortedMapObjectValues()
    {
        List<Double> values = new ArrayList<>();
        
        for ( InternalMapObject object : mapObjects )
        {
            values.add( object.getValue() );
        }
        
        Collections.sort( values );
        
        return values;
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public List<InternalMapObject> getMapObjects()
    {
        return mapObjects;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod( Period period )
    {
        this.period = period;
    }

    public Integer getMethod()
    {
        return method;
    }

    public void setMethod( Integer method )
    {
        this.method = method;
    }

    public String getLayer()
    {
        return layer;
    }

    public void setLayer( String layer )
    {
        this.layer = layer;
    }

    public Integer getRadiusHigh()
    {
        return radiusHigh;
    }

    public void setRadiusHigh( Integer radiusHigh )
    {
        this.radiusHigh = radiusHigh;
    }

    public Integer getRadiusLow()
    {
        return radiusLow;
    }

    public void setRadiusLow( Integer radiusLow )
    {
        this.radiusLow = radiusLow;
    }

    public Color getColorHigh()
    {
        return colorHigh;
    }

    public void setColorHigh( Color colorHigh )
    {
        this.colorHigh = colorHigh;
    }

    public Color getColorLow()
    {
        return colorLow;
    }

    public void setColorLow( Color colorLow )
    {
        this.colorLow = colorLow;
    }

    public float getOpacity()
    {
        return opacity;
    }

    public void setOpacity( float opacity )
    {
        this.opacity = opacity;
    }

    public Integer getClasses()
    {
        return classes;
    }

    public void setClasses( Integer classes )
    {
        this.classes = classes;
    }

    public IntervalSet getIntervalSet()
    {
        return intervalSet;
    }

    public void setIntervalSet( IntervalSet intervalSet )
    {
        this.intervalSet = intervalSet;
    }
}
