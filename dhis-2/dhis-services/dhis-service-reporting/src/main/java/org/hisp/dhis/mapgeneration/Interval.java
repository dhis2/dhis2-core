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
import java.util.List;

/**
 * An interval is a collection of map objects that have been distributed into
 * this interval.
 * 
 * It contains all the map objects that have values that lie in the range of
 * values this interval covers.
 * 
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class Interval
{
    /**
     * The color value associated with this interval.
     */
    private Color color;

    /**
     * The low boundary of values this interval covers.
     */
    private double valueLow;

    /**
     * The high boundary of values this interval covers.
     */
    private double valueHigh;

    /**
     * The map object members that fall into this interval category.
     */
    private List<InternalMapObject> members;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Interval( double valueLow, double valueHigh )
    {
        this.valueLow = valueLow;
        this.valueHigh = valueHigh;
        this.members = new ArrayList<>();
    }

    public Interval( Color color, double valueLow, double valueHigh )
    {
        this.color = color;
        this.valueLow = valueLow;
        this.valueHigh = valueHigh;
        this.members = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    /**
     * Adds a map object to this interval category.
     * 
     * @param member the member to add
     */
    public void addMember( InternalMapObject member )
    {
        this.members.add( member );
    }

    @Override
    public String toString()
    {
        return "[Low value: " + valueLow + ", high value: " + valueHigh + ", color: " + color + "]";
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /**
     * Gets the low value of this interval.
     * 
     * @return the low value
     */
    public double getValueLow()
    {
        return valueLow;
    }

    /**
     * Sets the low value of this interval.
     * 
     * @param valueLow the low value
     */
    public void setValueLow( double valueLow )
    {
        this.valueLow = valueLow;
    }

    /**
     * Gets the high value of this interval.
     * 
     * @return the high value
     */
    public double getValueHigh()
    {
        return valueHigh;
    }

    /**
     * Sets the high value of this interval.
     * 
     * @param valueHigh the high value
     */
    public void setValueHigh( double valueHigh )
    {
        this.valueHigh = valueHigh;
    }

    /**
     * Gets the color this interval has on the map.
     * 
     * @return the color
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Sets the color this interval has on the map.
     * 
     * @param color the color
     */
    public void setColor( Color color )
    {
        this.color = color;
    }

    /**
     * Returns a list of the members that have fallen into this interval
     * category, or null if none.
     * 
     * @return the list of members
     */
    public List<InternalMapObject> getMembers()
    {
        return members;
    }
}
