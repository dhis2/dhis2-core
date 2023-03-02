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

import static org.hisp.dhis.analytics.SortOrder.ASC;
import static org.hisp.dhis.analytics.SortOrder.DESC;
import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifierHelper.fromFullDimensionId;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.common.AnalyticsDateFilter;

@Getter
@RequiredArgsConstructor
public enum DimensionParamType
{
    DIMENSIONS( CommonQueryRequest::getDimension ),
    FILTERS( CommonQueryRequest::getFilter ),
    HEADERS( CommonQueryRequest::getHeaders ),
    DATE_FILTERS( commonQueryRequest -> Arrays.stream( AnalyticsDateFilter.values() )
        .map( analyticsDateFilter -> parseDate( commonQueryRequest, analyticsDateFilter ) )
        .filter( Objects::nonNull )
        .flatMap( Collection::stream )
        .collect( Collectors.toList() ) ),

    // The uidsGetter invoked on this enum, will return a collection made of:
    // - commonQueryRequest.getAsc(), suffixed by ":asc"
    // - commonQueryRequest.getDesc(), suffixed by ":desc"
    SORTING( commonQueryRequest -> Stream.concat(
        commonQueryRequest.getAsc().stream()
            .map( s -> s + ":" + ASC.getValue() ),
        commonQueryRequest.getDesc().stream()
            .map( s -> s + ":" + DESC.getValue() ) )
        .collect( Collectors.toList() ) );

    private static List<String> parseDate( CommonQueryRequest commonQueryRequest,
        AnalyticsDateFilter analyticsDateFilter )
    {
        String dateFilter = analyticsDateFilter.getTeiExtractor().apply( commonQueryRequest );
        if ( StringUtils.isEmpty( dateFilter ) )
        {
            return Collections.emptyList();
        }
        String[] dateFilterItems = dateFilter.split( ";" );
        return Stream.of( dateFilterItems )
            .map( dateFilterItem -> toDimensionParam( dateFilterItem, analyticsDateFilter ) )
            .collect( Collectors.toList() );
    }

    private static String toDimensionParam( String dateFilterItem, AnalyticsDateFilter analyticsDateFilter )
    {
        // Here, dateItem filter is in the form of
        // programUid.programStageUid.period we need to return
        // programUid.programStageUid.pe:period:analyticsDateFilter.
        StringDimensionIdentifier parsedItem = fromFullDimensionId( dateFilterItem );

        String period = parsedItem.getDimension().getUid();

        StringDimensionIdentifier dimensionIdentifier = StringDimensionIdentifier.of(
            parsedItem.getProgram(),
            parsedItem.getProgramStage(),
            StringUid.of( PERIOD_DIM_ID ) );

        return String.join( ":",
            dimensionIdentifier.toString(),
            period,
            analyticsDateFilter.name() );
    }

    // Getter method to retrieve the dimensions or filters from the
    // CommonQueryRequest.
    private final Function<CommonQueryRequest, Collection<String>> uidsGetter;
}
