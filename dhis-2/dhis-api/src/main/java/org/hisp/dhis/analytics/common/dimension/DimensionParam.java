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
import java.util.List;
import java.util.Objects;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryItem;

import com.google.common.collect.ImmutableList;

/**
 * A wrapper for DimensionObject|QueryItem to abstract them
 */
@Data
@Builder( access = PRIVATE )
@RequiredArgsConstructor( access = PRIVATE )
public class DimensionParam
{
    private final DimensionSpecification dimensionSpecification;

    private final DimensionalObject dimensionalObject;

    private final QueryItem queryItem;

    private final DimensionParamType type;

    @Builder.Default
    private final List<String> items = new ArrayList<>();

    /**
     * allows to create an instance of DimensionPAram passing the object to wrap
     * (a DimensionalObject or a QueryItem), the type and a list of filters (can
     * be empty)
     */
    public static DimensionParam ofObject( DimensionSpecification dimensionSpecification,
        Object dimensionalObjectOrQueryItem, DimensionParamType dimensionParamType,
        List<String> items )
    {
        Objects.requireNonNull( dimensionSpecification );
        Objects.requireNonNull( dimensionalObjectOrQueryItem );
        Objects.requireNonNull( dimensionParamType );

        DimensionParamBuilder builder = DimensionParam.builder()
            .dimensionSpecification( dimensionSpecification )
            .type( dimensionParamType )
            .items( ImmutableList.copyOf( CollectionUtils.emptyIfNull( items ) ) );

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
        throw new IllegalArgumentException( "Only DimensionalObject or QueryItem are allowed, received "
            + dimensionalObjectOrQueryItem.getClass().getName() + " instead" );
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
        return !isDimensionalObject();
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
        return DimensionParamObjectType.byForeignType( queryItem.getItem().getDimensionItemType() );
    }

}
