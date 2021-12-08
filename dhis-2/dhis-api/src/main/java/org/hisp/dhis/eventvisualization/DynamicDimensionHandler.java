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
import static org.hisp.dhis.eventvisualization.DynamicDimension.DynamicType.from;
import static org.hisp.dhis.eventvisualization.DynamicDimension.DynamicType.isDynamic;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;

/**
 * Responsible for handling and associating the dynamic dimensions in the
 * EventVisualization.
 *
 * @author maikel arabori
 */
public class DynamicDimensionHandler
{
    private final EventVisualization eventVisualization;

    public DynamicDimensionHandler( final EventVisualization eventVisualization )
    {
        this.eventVisualization = eventVisualization;
    }

    public DimensionalObject getDynamicDimension( final String dimension, final Attribute attribute )
    {
        if ( dimension != null && isDynamic( dimension ) )
        {
            return new BaseDimensionalObject( dimension, from( dimension ).getDimensionType(),
                loadDynamicDimensionItems( dimension, attribute ) );
        }
        else
        {
            throw new IllegalArgumentException( "Not a valid dimension: " + dimension );
        }
    }

    private List<BaseDimensionalItemObject> loadDynamicDimensionItems( final String dimension,
        final Attribute attribute )
    {
        final List<BaseDimensionalItemObject> items = new ArrayList<>();

        for ( final DynamicDimension dynamicDimension : eventVisualization.getDynamicDimensions() )
        {
            final boolean hasSameDimension = dynamicDimension.getDimension().equals( dimension );

            if ( dynamicDimension.belongsTo( attribute ) && hasSameDimension )
            {
                items.addAll( dynamicDimension.getValues().stream()
                    .map( value -> new BaseDimensionalItemObject( value ) ).collect( toList() ) );
            }
        }

        return items;
    }

    public void associateDimensions()
    {
        if ( isNotEmpty( eventVisualization.getColumns() ) )
        {
            for ( final DimensionalObject column : eventVisualization.getColumns() )
            {
                if ( column != null && isDynamic( column.getUid() ) )
                {
                    eventVisualization.getDynamicDimensions().add( createDynamicDimensionFor( column, COLUMN ) );
                }
            }
        }

        if ( isNotEmpty( eventVisualization.getRows() ) )
        {
            for ( final DimensionalObject row : eventVisualization.getRows() )
            {
                if ( row != null && isDynamic( row.getUid() ) )
                {
                    eventVisualization.getDynamicDimensions().add( createDynamicDimensionFor( row, ROW ) );
                }
            }
        }

        if ( isNotEmpty( eventVisualization.getFilters() ) )
        {
            for ( final DimensionalObject filter : eventVisualization.getFilters() )
            {
                if ( filter != null && isDynamic( filter.getUid() ) )
                {
                    eventVisualization.getDynamicDimensions().add( createDynamicDimensionFor( filter, FILTER ) );
                }
            }
        }
    }

    private DynamicDimension createDynamicDimensionFor( final DimensionalObject dimensionalObject,
        final Attribute attribute )
    {
        final DynamicDimension dynamicDimension = new DynamicDimension();
        dynamicDimension.setParent( attribute );
        dynamicDimension.setDimension( dimensionalObject.getUid() );

        if ( isNotEmpty( dimensionalObject.getItems() ) )
        {
            dynamicDimension
                .setValues( dimensionalObject.getItems().stream().map( v -> v.getUid() ).collect( toList() ) );
        }

        return dynamicDimension;
    }
}
