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

import java.util.ArrayList;
import java.util.List;

/**
 * An interval set is a collection of map objects that are distributed into
 * intervals according to their value.
 * 
 * The core functionality of this class is encapsulated into its method
 * applyIntervalSetToMapLayer, which takes a map layer as input, creates an
 * interval set for it, and distributes its map objects into intervals according
 * to the given distribution strategy.
 * 
 * @author Olai Solheim <olais@ifi.uio.no>
 */
public class IntervalSet
{
    private List<Interval> intervals = new ArrayList<>();

    private InternalMapObject objectLow;
    
    private InternalMapObject objectHigh;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public IntervalSet()
    {
    }
    
    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Populates object low and object high based on the given list of map objects.
     */
    public IntervalSet setLowHigh( List<InternalMapObject> mapObjects )
    {
        for ( InternalMapObject mapObject : mapObjects )
        {
            if ( objectLow == null || mapObject.getValue() < objectLow.getValue() )
            {
                setObjectLow( mapObject );
            }
            
            if ( objectHigh == null || mapObject.getValue() > objectHigh.getValue() )
            {
                setObjectHigh( mapObject );
            }
        }
        
        return this;
    }
    
    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public List<Interval> getIntervals()
    {
        return intervals;
    }

    public void setIntervals( List<Interval> intervals )
    {
        this.intervals = intervals;
    }

    public InternalMapObject getObjectLow()
    {
        return objectLow;
    }

    public void setObjectLow( InternalMapObject objectLow )
    {
        this.objectLow = objectLow;
    }

    public InternalMapObject getObjectHigh()
    {
        return objectHigh;
    }

    public void setObjectHigh( InternalMapObject objectHigh )
    {
        this.objectHigh = objectHigh;
    }
}
