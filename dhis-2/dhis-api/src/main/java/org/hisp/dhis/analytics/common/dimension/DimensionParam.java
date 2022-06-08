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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryItem;

import com.google.common.collect.ImmutableList;

/**
 * A wrapper for DimensionObject|QueryItem to abstract them
 */
@Data
@Builder
@AllArgsConstructor( access = AccessLevel.PRIVATE )
public class DimensionParam
{
    private final DimensionalObject dimensionalObject;

    private final QueryItem queryItem;

    private final DimensionParamType dimensionParamType;

    @Builder.Default
    private final List<String> items = new ArrayList<>();

    public static DimensionParam ofObject( Object dimensionalObjectOrQueryItem, DimensionParamType dimensionParamType,
        List<String> items )
    {
        Objects.requireNonNull( dimensionalObjectOrQueryItem );
        if ( dimensionalObjectOrQueryItem instanceof DimensionalObject )
        {
            return DimensionParam.builder()
                .dimensionalObject( (DimensionalObject) dimensionalObjectOrQueryItem )
                .dimensionParamType( dimensionParamType )
                .items( ImmutableList.copyOf( items ) )
                .build();
        }
        if ( dimensionalObjectOrQueryItem instanceof QueryItem )
        {
            return DimensionParam.builder()
                .queryItem( (QueryItem) dimensionalObjectOrQueryItem )
                .dimensionParamType( dimensionParamType )
                .items( ImmutableList.copyOf( items ) )
                .build();
        }
        throw new IllegalArgumentException( "Only DimensionalObject or QueryItem are allowed, received "
            + dimensionalObjectOrQueryItem.getClass().getName() + " instead" );
    }

    public boolean isFilter()
    {
        return !items.isEmpty();
    }

    private boolean isDimensionalObject()
    {
        return Objects.nonNull( dimensionalObject );
    }

    private boolean isQueryItem()
    {
        return !isDimensionalObject();
    }

    public DimensionParamObjectType getDimensionParamType()
    {
        if ( isDimensionalObject() )
        {
            return DimensionParamObjectType.byForeignType( dimensionalObject.getDimensionType() );
        }
        return DimensionParamObjectType.byForeignType( queryItem.getItem().getDimensionItemType() );
    }

}
