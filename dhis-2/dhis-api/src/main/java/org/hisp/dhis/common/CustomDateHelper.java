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
package org.hisp.dhis.common;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.MULTI_CHOICES_OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper to convert date fields from controller's Criteria into a plain string
 * that can be merged into PE dimension.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CustomDateHelper
{
    public static String getCustomDateFilters( Predicate<AnalyticsDateFilter> appliesTo,
        Function<AnalyticsDateFilter, Function<Object, String>> function, Object criteria )
    {
        return Arrays.stream( AnalyticsDateFilter.values() )
            .filter( appliesTo )
            .filter( analyticsDateFilter -> function.apply( analyticsDateFilter ).apply( criteria ) != null )
            .map( analyticsDateFilter -> String.join( DIMENSION_NAME_SEP,
                handleMultiOptions(
                    function.apply( analyticsDateFilter ).apply( criteria ),
                    analyticsDateFilter.getTimeField().name() ) ) )
            .collect( Collectors.joining( OPTION_SEP ) );
    }

    private static String handleMultiOptions( String values, String timeField )
    {
        return Arrays.stream( withOptionSeparator( values ).split( OPTION_SEP ) )
            .map( aValue -> String.join( DIMENSION_NAME_SEP,
                aValue, timeField ) )
            .collect( Collectors.joining( OPTION_SEP ) );
    }

    private static String withOptionSeparator( String options )
    {
        return joinOnOptionSeparator( splitOnMultiOptionSeparator( options ) );
    }

    private static String joinOnOptionSeparator( String[] strings )
    {
        return StringUtils.join( strings, OPTION_SEP );
    }

    private static String[] splitOnMultiOptionSeparator( String s )
    {
        return StringUtils.split( s, MULTI_CHOICES_OPTION_SEP );
    }

    /**
     * Given existing dimensions from controller and the time filters passed,
     * returns a new collection of dimensions with proper period dimension set
     */
    public static Set<String> getDimensionsWithRefactoredPeDimension( Set<String> dimensions, String customDateFilters )
    {
        if ( customDateFilters.isEmpty() )
        {
            return dimensions;
        }

        return dimensions.stream()
            .filter( CustomDateHelper::isPeDimension )
            .findFirst()
            // if PE dimensions already exists, return dimensions, with
            // existing PE+customDateFilters
            .map( peDimension -> dimensionsWithRefactoredPe( dimensions, customDateFilters, peDimension ) )
            // if PE dimensions didn't exist, returns dimensions with a new
            // PE dimension,
            // represented by customDateFilters
            .orElseGet( () -> dimensionsWithNewPe( dimensions,
                String.join( DIMENSION_NAME_SEP, PERIOD_DIM_ID, customDateFilters ) ) );

    }

    private static Set<String> dimensionsWithNewPe( Set<String> dimension, String peDimension )
    {
        dimension.add( peDimension );
        return dimension;
    }

    /**
     * concatenate existing dimensions (but PE one) with a new pe dimension,
     * represented by existing PE+customDateFilters
     */
    private static Set<String> dimensionsWithRefactoredPe( Set<String> dimension, String customDateFilters,
        String peDimension )
    {
        return Stream.concat(
            dimension.stream().filter( d -> !isPeDimension( d ) ),
            Stream.of( String.join( OPTION_SEP, withoutTimeField( peDimension ), customDateFilters ) ) )
            .collect( Collectors.toSet() );
    }

    /**
     * "sanitize" legacy pe dimension, in case it is passed along with time
     * field specification. Example
     * pe:TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE -> pe:TODAY;LAST_WEEK
     */
    private static String withoutTimeField( String dimension )
    {
        dimension = dimension.replaceFirst( PERIOD_DIM_ID + DIMENSION_NAME_SEP, "" );
        return String.join( DIMENSION_NAME_SEP, PERIOD_DIM_ID,
            Arrays.stream( dimension.split( OPTION_SEP ) )
                .map( CustomDateHelper::removeTimeField )
                .collect( Collectors.joining( OPTION_SEP ) ) );
    }

    private static String removeTimeField( String dimensionItem )
    {
        String[] splitDimension = dimensionItem.split( DIMENSION_NAME_SEP );
        if ( splitDimension.length > 1 )
        {
            return splitDimension[0];
        }
        else
            return dimensionItem;
    }

    public static boolean isPeDimension( String dimension )
    {
        return dimension.startsWith( PERIOD_DIM_ID + DIMENSION_NAME_SEP );
    }
}
