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
package org.hisp.dhis.analytics.common.dimension;

import static lombok.AccessLevel.PRIVATE;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.FILTERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.common.ValueType;

/**
 * A wrapper for DimensionObject|QueryItem to abstract them
 */
@Data
@Slf4j
@Builder( access = PRIVATE )
@RequiredArgsConstructor( access = PRIVATE )
public class DimensionParam implements UidObject
{
    private final DimensionalObject dimensionalObject;

    private final QueryItem queryItem;

    private final StaticDimension staticDimension;

    private final DimensionParamType type;

    @Builder.Default
    private final List<DimensionParamItem> items = new ArrayList<>();

    /**
     * allows to create an instance of DimensionParam passing the object to wrap
     * (a DimensionalObject, a QueryItem or a static dimension), the type and a
     * list of filters (can be empty)
     *
     * @param dimensionalObjectOrQueryItem either an DimensionalObject or
     *        QueryItem, this instance wraps
     * @param dimensionParamType type of this parameter (wether it's a filter or
     *        a dimension)
     * @param items the items parameters fot this DimensionParam
     * @return a new instance of DimensionParams
     */
    public static DimensionParam ofObject( Object dimensionalObjectOrQueryItem, DimensionParamType dimensionParamType,
        List<String> items )
    {
        Objects.requireNonNull( dimensionalObjectOrQueryItem );
        Objects.requireNonNull( dimensionParamType );

        DimensionParamBuilder builder = DimensionParam.builder()
            .type( dimensionParamType )
            .items( DimensionParamItem.ofStrings( items ) );

        if ( dimensionalObjectOrQueryItem instanceof DimensionalObject )
        {
            return builder
                .dimensionalObject( (DimensionalObject) dimensionalObjectOrQueryItem )
                .build();
        }
        if ( dimensionalObjectOrQueryItem instanceof QueryItem )
        {
            return builder
                .queryItem( (QueryItem) dimensionalObjectOrQueryItem )
                .build();
        }

        // if it's neither a DimensionalObject nor a QueryItem, we try to see if
        // it's a static Dimension
        Optional<StaticDimension> staticDimension = StaticDimension.of( dimensionalObjectOrQueryItem.toString() );
        if ( staticDimension.isPresent() )
        {
            return builder
                .staticDimension( staticDimension.get() )
                .build();
        }

        String receivedIdentifier = dimensionalObjectOrQueryItem.getClass().equals( String.class )
            ? dimensionalObjectOrQueryItem.toString()
            : dimensionalObjectOrQueryItem.getClass().getName();

        throw new IllegalArgumentException(
            "Only DimensionalObject, QueryItem or static dimensions are allowed. Received " +
                receivedIdentifier + " instead" );
    }

    public static boolean isStaticDimensionIdentifier( String dimensionIdentifier )
    {
        return StaticDimension.of( dimensionIdentifier ).isPresent();
    }

    /**
     * @return true if this DimensionParams has some items on it
     */
    public boolean hasRestrictions()
    {
        return CollectionUtils.isNotEmpty( items );
    }

    /**
     * @return true if this DimensionParam is a filter
     */
    public boolean isFilter()
    {
        return type == FILTERS;
    }

    private boolean isDimensionalObject()
    {
        return Objects.nonNull( dimensionalObject );
    }

    private boolean isQueryItem()
    {
        return Objects.nonNull( queryItem );
    }

    private boolean isStaticDimension()
    {
        return !isQueryItem() && !isDimensionalObject();
    }

    /**
     * @return the DimensionParamObjectType of this DimensionParam
     */
    public DimensionParamObjectType getDimensionParamObjectType()
    {
        if ( isDimensionalObject() )
        {
            return DimensionParamObjectType.byForeignType( dimensionalObject.getDimensionType() );
        }
        if ( isQueryItem() )
        {
            return DimensionParamObjectType.byForeignType( queryItem.getItem().getDimensionItemType() );
        }

        return DimensionParamObjectType.STATIC_DIMENSION;
    }

    public ValueType getValueType()
    {
        if ( isDimensionalObject() )
        {
            return dimensionalObject.getValueType();
        }
        if ( isQueryItem() )
        {
            return queryItem.getValueType();
        }
        return staticDimension.valueType;
    }

    public String getDimensionObjectUid()
    {
        if ( isDimensionalObject() )
        {
            return dimensionalObject.getUid();
        }
        if ( isQueryItem() )
        {
            return queryItem.getItem().getUid();
        }
        return staticDimension.name();
    }

    @Override
    public String getUid()
    {
        return getDimensionObjectUid();
    }

    @RequiredArgsConstructor
    enum StaticDimension
    {
        OU( ValueType.TEXT ),
        ENROLLMENTDATE( ValueType.DATETIME ),
        EXECUTIONDATE( ValueType.DATETIME );
        // TODO: do we need more here ?

        private final ValueType valueType;

        static Optional<StaticDimension> of( String value )
        {
            return Arrays.stream( StaticDimension.values() )
                .filter( sd -> StringUtils.equalsIgnoreCase( sd.name(), value ) )
                .findFirst();
        }
    }

}
