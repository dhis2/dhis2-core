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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.BaseAnalyticalObject.NOT_A_VALID_DIMENSION;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;
import static org.hisp.dhis.eventvisualization.SimpleEventDimension.Type.contains;
import static org.hisp.dhis.eventvisualization.SimpleEventDimension.Type.from;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;

/**
 * Responsible for handling and associating the simple event dimensions in the
 * EventAnalyticalObject instances.
 *
 * @author maikel arabori
 */
public class SimpleEventDimensionHandler
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

    public SimpleEventDimensionHandler( final EventAnalyticalObject eventAnalyticalObject )
    {
        this.eventAnalyticalObject = eventAnalyticalObject;
    }

    public DimensionalObject getDimensionalObject( final String dimension, final Attribute attribute )
    {
        if ( dimension != null && contains( dimension ) )
        {
            return new BaseDimensionalObject( dimension, from( dimension ).getParentType(),
                loadDimensionalItems( dimension, attribute ) );
        }
        else
        {
            throw new IllegalArgumentException( format( NOT_A_VALID_DIMENSION, dimension ) );
        }
    }

    public void associateDimensions()
    {
        if ( isNotEmpty( eventAnalyticalObject.getColumns() ) )
        {
            for ( final DimensionalObject column : eventAnalyticalObject.getColumns() )
            {
                if ( column != null && contains( column.getUid() ) )
                {
                    eventAnalyticalObject.getSimpleEventDimensions().add( createStringDimensionFor( column, COLUMN ) );
                }
            }
        }

        if ( isNotEmpty( eventAnalyticalObject.getRows() ) )
        {
            for ( final DimensionalObject row : eventAnalyticalObject.getRows() )
            {
                if ( row != null && contains( row.getUid() ) )
                {
                    eventAnalyticalObject.getSimpleEventDimensions().add( createStringDimensionFor( row, ROW ) );
                }
            }
        }

        if ( isNotEmpty( eventAnalyticalObject.getFilters() ) )
        {
            for ( final DimensionalObject filter : eventAnalyticalObject.getFilters() )
            {
                if ( filter != null && contains( filter.getUid() ) )
                {
                    eventAnalyticalObject.getSimpleEventDimensions().add( createStringDimensionFor( filter, FILTER ) );
                }
            }
        }
    }

    private List<BaseDimensionalItemObject> loadDimensionalItems( final String dimension,
        final Attribute attribute )
    {
        final List<BaseDimensionalItemObject> items = new ArrayList<>();

        for ( final SimpleEventDimension simpleEventDimension : eventAnalyticalObject.getSimpleEventDimensions() )
        {
            final boolean hasSameDimension = simpleEventDimension.getDimension().equals( dimension );

            if ( simpleEventDimension.belongsTo( attribute ) && hasSameDimension )
            {
                items.addAll( simpleEventDimension.getValues().stream()
                    .map( value -> new BaseDimensionalItemObject( value ) ).collect( toList() ) );
            }
        }

        return items;
    }

    private SimpleEventDimension createStringDimensionFor( final DimensionalObject dimensionalObject,
        final Attribute attribute )
    {
        final SimpleEventDimension simpleEventDimension = new SimpleEventDimension();
        simpleEventDimension.setParent( attribute );
        simpleEventDimension.setDimension( dimensionalObject.getUid() );

        if ( isNotEmpty( dimensionalObject.getItems() ) )
        {
            simpleEventDimension
                .setValues( dimensionalObject.getItems().stream().map( v -> v.getUid() ).collect( toList() ) );
        }

        return simpleEventDimension;
    }
}
