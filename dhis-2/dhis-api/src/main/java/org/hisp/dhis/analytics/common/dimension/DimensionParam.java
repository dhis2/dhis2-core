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

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.ORGANISATION_UNIT;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.byForeignType;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.DATE_FILTERS;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.DIMENSIONS;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamType.FILTERS;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.ValueType.DATETIME;
import static org.hisp.dhis.common.ValueType.TEXT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.common.ValueType;

/**
 * Object responsible to wrap/encapsulate instances of
 * DimensionObject|QueryItem|StaticDimension.
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
     * Allows to create an instance of DimensionParam. We should pass the object
     * to be wrapped (a {@link DimensionalObject}, a {@link QueryItem} or a
     * static dimension), the type ({@link DimensionParamType}) and a list of
     * filters ({@link List<String>}). This last can be empty.
     *
     * @param dimensionalObjectOrQueryItem either a {@link DimensionalObject} or
     *        {@link QueryItem}, or a static dimension.
     * @param dimensionParamType the {@link DimensionParamType} for the
     *        {@link DimensionParam} returned (weather it's a filter or a
     *        dimension).
     * @param items the list of items parameters for this DimensionParam.
     *
     * @return a new instance of {@link DimensionParam}.
     */
    public static DimensionParam ofObject( Object dimensionalObjectOrQueryItem, DimensionParamType dimensionParamType,
        List<String> items )
    {
        Objects.requireNonNull( dimensionalObjectOrQueryItem );
        Objects.requireNonNull( dimensionParamType );

        if ( dimensionParamType == DATE_FILTERS )
        {
            items = items.stream().map( item -> EQ + ":" + item ).collect( Collectors.toList() );
        }

        DimensionParamBuilder builder = DimensionParam.builder()
            .type( dimensionParamType )
            .items( DimensionParamItem.ofStrings( items ) );

        if ( dimensionalObjectOrQueryItem instanceof DimensionalObject )
        {
            return builder.dimensionalObject( (DimensionalObject) dimensionalObjectOrQueryItem ).build();
        }

        if ( dimensionalObjectOrQueryItem instanceof QueryItem )
        {
            return builder
                .queryItem( (QueryItem) dimensionalObjectOrQueryItem )
                .build();
        }

        // If this is neither a DimensionalObject nor a QueryItem, we try to see if it's a static Dimension.
        Optional<StaticDimension> staticDimension = StaticDimension.of( dimensionalObjectOrQueryItem.toString() );

        if ( staticDimension.isPresent() )
        {
            return builder.staticDimension( staticDimension.get() ).build();
        }

        String receivedIdentifier = dimensionalObjectOrQueryItem.getClass().equals( String.class )
            ? dimensionalObjectOrQueryItem.toString()
            : dimensionalObjectOrQueryItem.getClass().getName();

        throw new IllegalArgumentException(
            "Only DimensionalObject, QueryItem or static dimensions are allowed. Received " + receivedIdentifier
                + " instead" );
    }

    public static boolean isStaticDimensionIdentifier( String dimensionIdentifier )
    {
        return StaticDimension.of( dimensionIdentifier ).isPresent();
    }

    /**
     * @return true if this DimensionParams has some items on it.
     */
    public boolean hasRestrictions()
    {
        return isNotEmpty( items );
    }

    /**
     * @return true if this DimensionParam is a filter.
     */
    public boolean isFilter()
    {
        return type == FILTERS;
    }

    /**
     * @return true if this DimensionParam is a dimension
     */
    public boolean isDimension()
    {
        return type == DIMENSIONS;
    }

    public boolean isDimensionalObject()
    {
        return nonNull( dimensionalObject );
    }

    public boolean isQueryItem()
    {
        return nonNull( queryItem );
    }

    public boolean isStaticDimension()
    {
        return !isQueryItem() && !isDimensionalObject();
    }

    /**
     * @return the DimensionParamObjectType of this DimensionParam.
     */
    public DimensionParamObjectType getDimensionParamObjectType()
    {
        if ( isDimensionalObject() )
        {
            return byForeignType( dimensionalObject.getDimensionType() );
        }

        if ( isQueryItem() )
        {
            return byForeignType( queryItem.getItem().getDimensionItemType() );
        }

        return staticDimension.getDimensionParamObjectType();
    }

    public boolean isOfType( DimensionParamObjectType type )
    {
        return getDimensionParamObjectType() == type;
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

    @Override
    public String getUid()
    {
        if ( isDimensionalObject() )
        {
            return dimensionalObject.getUid();
        }

        if ( isQueryItem() )
        {
            return queryItem.getItem().getUid();
        }

        return staticDimension.getColumnName();
    }

    public boolean isPeriodDimension()
    {
        return isDimensionalObject() && dimensionalObject.getDimensionType() == PERIOD ||
            isStaticDimension() && staticDimension.getDimensionParamObjectType() == DimensionParamObjectType.PERIOD;
    }

    public String getName()
    {
        if ( isDimensionalObject() )
        {
            return dimensionalObject.getName();
        }

        if ( isQueryItem() )
        {
            return queryItem.getItem().getName();
        }

        return staticDimension.name();
    }

    @RequiredArgsConstructor
    public enum StaticDimension
    {
        OUNAME( TEXT, ORGANISATION_UNIT ),
        ENROLLMENTDATE( DATETIME, DimensionParamObjectType.PERIOD ),
        ENDDATE( DATETIME, DimensionParamObjectType.PERIOD ),
        INCIDENTDATE( DATETIME, DimensionParamObjectType.PERIOD ),
        EXECUTIONDATE( DATETIME, DimensionParamObjectType.PERIOD ),
        LASTUPDATED( DATETIME, DimensionParamObjectType.PERIOD ),
        CREATED( DATETIME, DimensionParamObjectType.PERIOD );

        private final ValueType valueType;

        @Getter
        private final String columnName;

        @Getter
        private final DimensionParamObjectType dimensionParamObjectType;

        StaticDimension( ValueType valueType, DimensionParamObjectType dimensionParamObjectType )
        {
            this.valueType = valueType;

            // By default, columnName is its own "name" in lowercase.
            this.columnName = lowerCase( name() );

            this.dimensionParamObjectType = dimensionParamObjectType;
        }

        static Optional<StaticDimension> of( String value )
        {
            return Arrays.stream( StaticDimension.values() )
                .filter( sd -> StringUtils.equalsIgnoreCase( sd.name(), value ) )
                .findFirst();
        }
    }
}
