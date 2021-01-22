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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.feedback.ErrorCode.E2014;
import static org.hisp.dhis.feedback.ErrorCode.E2016;
import static org.hisp.dhis.webapi.controller.dataitem.DataItemServiceFacade.DATA_TYPE_ENTITY_MAP;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;

/**
 * Helper class responsible for reading and extracting the URL filters.
 */
public class FilteringHelper
{
    private static final String DIMENSION_TYPE_IN_FILTER_PREFIX = "dimensionItemType:in:";

    private static final String DIMENSION_TYPE_EQUAL_FILTER_PREFIX = "dimensionItemType:eq:";

    /**
     * This method will return the respective BaseDimensionalItemObject class
     * from the filter provided.
     *
     * @param filter should have the format of
     *        "dimensionItemType:in:[INDICATOR,DATA_SET,...]", where INDICATOR
     *        and DATA_SET represents the BaseDimensionalItemObject. The valid
     *        types are found at
     *        {@link org.hisp.dhis.common.DataDimensionItemType}
     * @return the respective classes associated with the given IN filter
     * @throws IllegalQueryException if the filter points to a non supported
     *         class/entity.
     */
    public static Set<Class<? extends BaseDimensionalItemObject>> extractEntitiesFromInFilter( final String filter )
    {
        final Set<Class<? extends BaseDimensionalItemObject>> dimensionTypes = new HashSet<>( 0 );

        if ( contains( filter, DIMENSION_TYPE_IN_FILTER_PREFIX ) )
        {
            final String[] dimensionTypesInFilter = split( deleteWhitespace( substringBetween( filter, "[", "]" ) ),
                "," );

            if ( isNotEmpty( dimensionTypesInFilter ) )
            {
                for ( final String dimensionType : dimensionTypesInFilter )
                {
                    dimensionTypes.add( entityClassFromString( dimensionType ) );
                }
            }
            else
            {
                throw new IllegalQueryException( new ErrorMessage( E2014, filter ) );
            }
        }

        return dimensionTypes;
    }

    /**
     * This method will return the respective BaseDimensionalItemObject class
     * from the filter provided.
     *
     * @param filter should have the format of "dimensionItemType:eq:INDICATOR",
     *        where INDICATOR represents the BaseDimensionalItemObject. It could
     *        be any value represented by
     *        {@link org.hisp.dhis.common.DataDimensionItemType}
     * @return the respective class associated with the given filter
     * @throws IllegalQueryException if the filter points to a non supported
     *         class/entity.
     */
    public static Class<? extends BaseDimensionalItemObject> extractEntityFromEqualFilter( final String filter )
    {
        final byte DIMENSION_TYPE = 2;
        Class<? extends BaseDimensionalItemObject> entity = null;

        if ( hasEqualsDimensionTypeFilter( filter ) )
        {
            final String[] array = filter.split( ":" );
            final boolean hasDimensionType = array.length == 3;

            if ( hasDimensionType )
            {
                entity = entityClassFromString( array[DIMENSION_TYPE] );
            }
            else
            {
                throw new IllegalQueryException( new ErrorMessage( E2014, filter ) );
            }
        }

        return entity;
    }

    /**
     * Simply checks if the given list of filters contains a dimension type
     * filter.
     *
     * @param filters
     * @return true if a dimension type filter is found, false otherwise.
     */
    public static boolean containsDimensionTypeFilter( final List<String> filters )
    {
        if ( CollectionUtils.isNotEmpty( filters ) )
        {
            for ( final String filter : filters )
            {
                if ( hasEqualsDimensionTypeFilter( filter ) || hasInDimensionTypeFilter( filter ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasEqualsDimensionTypeFilter( final String filter )
    {
        return trimToEmpty( filter ).contains( DIMENSION_TYPE_EQUAL_FILTER_PREFIX );
    }

    private static boolean hasInDimensionTypeFilter( final String filter )
    {
        return trimToEmpty( filter ).contains( DIMENSION_TYPE_IN_FILTER_PREFIX );
    }

    private static Class<? extends BaseDimensionalItemObject> entityClassFromString( final String entityType )
    {
        final Class<? extends BaseDimensionalItemObject> entity = DATA_TYPE_ENTITY_MAP.get( entityType );

        if ( entity == null )
        {
            throw new IllegalQueryException(
                new ErrorMessage( E2016, entityType, "dimensionItemType",
                    Arrays.toString( DATA_TYPE_ENTITY_MAP.keySet().toArray() ) ) );
        }

        return entity;
    }
}
