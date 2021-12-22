/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.eventvisualization;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.contains;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.from;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;

/**
 * Responsible for handling and associating the simple event dimensions in the
 * EventAnalyticalObject instances.
 *
 * @author maikel arabori
 */
public class SimpleDimensionHandler
{
    /**
     * We use the EventAnalyticalObject interface because this handler is also
     * used by the deprecated EventReport. So, in order to being able to reuse
     * it, we make usage of the common interface.
     *
     * Once the EventReport is completely removed we can start using the actual
     * EventVisualization class.
     */
    private final EventAnalyticalObject eventAnalyticalObject;

    public SimpleDimensionHandler( final EventAnalyticalObject eventAnalyticalObject )
    {
        this.eventAnalyticalObject = eventAnalyticalObject;
    }

    /**
     * Based on the given dimension and parent attribute, this method will
     * return an instance representing the associated DimensionalObject. It
     * actually returns an instance of BaseDimensionalObject.
     *
     * @param dimension the dimension, ie: dx, pe, eventDate
     * @param parent the parent attribute
     * @return the respective dimensional object
     * @throws IllegalArgumentException if the dimension does not exist in
     *         {@link SimpleDimension.Type}
     */
    public DimensionalObject getDimensionalObject( final String dimension, final Attribute parent )
    {
        return new BaseDimensionalObject( dimension, from( dimension ).getParentType(),
            loadDimensionalItems( dimension, parent ) );
    }

    /**
     * Based on the given event analytical object provided in the constructor of
     * this class, this method will populate its respective list of simple event
     * dimensions.
     *
     * It, basically, iterates through columns, rows and filters in order to
     * extract the correct dimension and add it into the list of simple event
     * dimensions. This is obviously done by reference on top of the current
     * event analytical object.
     */
    public void associateDimensions()
    {
        associateDimensionalObjects( eventAnalyticalObject.getColumns(), COLUMN );
        associateDimensionalObjects( eventAnalyticalObject.getRows(), ROW );
        associateDimensionalObjects( eventAnalyticalObject.getFilters(), FILTER );
    }

    private void associateDimensionalObjects( final List<DimensionalObject> dimensionalObjects,
        final Attribute attribute )
    {
        if ( isNotEmpty( dimensionalObjects ) )
        {
            for ( final DimensionalObject object : dimensionalObjects )
            {
                if ( object != null && contains( object.getUid() ) )
                {
                    eventAnalyticalObject.getSimpleDimensions()
                        .add( createSimpleEventDimensionFor( object, attribute ) );
                }
            }
        }
    }

    private List<BaseDimensionalItemObject> loadDimensionalItems( final String dimension,
        final Attribute parent )
    {
        final List<BaseDimensionalItemObject> items = new ArrayList<>();

        for ( final SimpleDimension simpleDimension : eventAnalyticalObject.getSimpleDimensions() )
        {
            final boolean hasSameDimension = simpleDimension.getDimension().equals( dimension );

            if ( simpleDimension.belongsTo( parent ) && hasSameDimension )
            {
                items.addAll( simpleDimension.getValues().stream()
                    .map( BaseDimensionalItemObject::new ).collect( toList() ) );
            }
        }

        return items;
    }

    private SimpleDimension createSimpleEventDimensionFor( final DimensionalObject dimensionalObject,
        final Attribute attribute )
    {
        final SimpleDimension simpleDimension = new SimpleDimension();
        simpleDimension.setParent( attribute );
        simpleDimension.setDimension( dimensionalObject.getUid() );

        if ( isNotEmpty( dimensionalObject.getItems() ) )
        {
            simpleDimension.setValues(
                dimensionalObject.getItems().stream().map( DimensionalItemObject::getUid ).collect( toList() ) );
        }

        return simpleDimension;
    }
}
