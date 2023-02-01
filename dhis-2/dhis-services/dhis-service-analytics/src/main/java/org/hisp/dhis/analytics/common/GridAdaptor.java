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
package org.hisp.dhis.analytics.common;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.processing.MetadataDetailsHandler;
import org.hisp.dhis.analytics.common.processing.ParamsHandler;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Component that provides operations for generation or manipulation of Grid
 * objects. It basically encapsulates any required Grid logic that is not
 * supported by the Grid itself.
 *
 * @author maikel arabori
 */
@AllArgsConstructor
@Component
public class GridAdaptor
{
    private final ParamsHandler paramsHandler;

    private final MetadataDetailsHandler metadataDetailsHandler;

    private final AnalyticsSecurityManager analyticsSecurityManager;

    private final CurrentUserService currentUserService;

    /**
     * Based on the given headers and result map, this method takes care of the
     * logic needed to create a valid {@link Grid} object. If the given
     * "sqlQueryResult" is not present, the resulting {@link Grid} will have
     * empty rows.
     *
     * @param sqlQueryResult the optional of {@link SqlQueryResult}.
     * @param teiQueryParams the {@link TeiQueryParams}.
     *
     * @return the {@link Grid} object.
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null.
     */
    public Grid createGrid( Optional<SqlQueryResult> sqlQueryResult, long rowsCount,
        @Nonnull TeiQueryParams teiQueryParams )
    {
        notNull( teiQueryParams, "The 'teiQueryParams' must not be null" );

        Grid grid = new ListGrid();

        paramsHandler.addHeaders( grid, teiQueryParams );

        // Adding rows.
        if ( sqlQueryResult.isPresent() )
        {
            grid.addRows( sqlQueryResult.get().result() );
        }

        paramsHandler.addMetaData( grid, teiQueryParams.getCommonParams(), rowsCount );

        CommonParams commonParams = teiQueryParams.getCommonParams();

        List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers = commonParams.getDimensionIdentifiers();

        Set<Option> itemOptions = dimensionIdentifiers.stream()
            .map( dimParam -> dimParam.getDimension().getQueryItem() )
            .filter( QueryItem::hasOptionSet )
            .map( q -> q.getOptionSet().getOptions() )
            .flatMap( List::stream )
            .collect( toSet() );

        List<QueryItem> items = dimensionIdentifiers.stream()
            .map( dimParam -> dimParam.getDimension().getQueryItem() ).collect( toList() );

        List<QueryItem> itemsWithoutFilters = dimensionIdentifiers.stream()
            .filter( dimParam -> !dimParam.getDimension().isFilter() )
            .map( dimParam -> dimParam.getDimension().getQueryItem() )
            .collect( toList() );

        List<QueryItem> itemsAsFilters = dimensionIdentifiers.stream()
            .filter( dimParam -> dimParam.getDimension().isFilter() )
            .map( dimParam -> dimParam.getDimension().getQueryItem() )
            .collect( toList() );

        List<DimensionalObject> dimensions = dimensionIdentifiers.stream()
            .filter( dimensionIdentifier -> dimensionIdentifier.getDimension() != null
                && dimensionIdentifier.getDimension().getDimensionalObject() != null )
            .map( dimParam -> dimParam.getDimension().getDimensionalObject() ).collect( toList() );

        List<DimensionalItemObject> dimensionOrFilterItems = getDimensionOrFilterItems( PERIOD_DIM_ID, dimensions );

        User user = currentUserService.getCurrentUser();

        List<DimensionItemKeywords.Keyword> periodKeywords = dimensions.stream()
            .filter( dim -> dim != null )
            .map( DimensionalObject::getDimensionItemKeywords )
            .filter( dimensionItemKeywords -> dimensionItemKeywords != null && !dimensionItemKeywords.isEmpty() )
            .flatMap( dk -> dk.getKeywords().stream() ).collect( toList() );

        DimensionalItemObject value = commonParams.getValue();

        Set<Legend> itemLegends = items.stream()
            .filter( QueryItem::hasLegendSet )
            .map( i -> i.getLegendSet().getLegends() )
            .flatMap( Set::stream )
            .collect( toSet() );

        // TODO: Review Program and ProgramStages as we have multiple now.
        Program program = commonParams.getPrograms().get( 0 );
        ProgramStage programStage = dimensionIdentifiers.stream()
            .map( dimParam -> dimParam.getProgramStage().getElement() )
            .collect( toList() ).get( 0 );

        metadataDetailsHandler.addMetadata( grid, itemOptions, items, dimensionOrFilterItems, user, true, true, true,
            periodKeywords, value, commonParams.getDisplayProperty(), itemLegends, items, itemsWithoutFilters,
            itemsAsFilters, dimensions, program, programStage );

        // TODO: Set new parameters in CommonParams.

        // Retain only selected headers, if any.
        grid.retainColumns( teiQueryParams.getCommonParams().getHeaders() );

        return grid;
    }

    /**
     * Retrieves the options for the dimension or filter with the given
     * identifier. Returns an empty list if the dimension or filter is not
     * present.
     */
    public List<DimensionalItemObject> getDimensionOrFilterItems( String dimensionKey,
        List<DimensionalObject> dimensions )
    {
        return getDimensionOptions( dimensionKey, dimensions );
        //return !dimensionOptions.isEmpty() ? dimensionOptions : getFilterOptions( dimensionKey );
    }

    /**
     * Retrieves the options for the given dimension identifier. Returns an
     * empty list if the dimension is not present.
     */
    public List<DimensionalItemObject> getDimensionOptions( String dimensionKey, List<DimensionalObject> dimensions )
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( dimensionKey ) );

        return index != -1 ? dimensions.get( index ).getItems() : emptyList();
    }

    /**
     * Retrieves the options for the given filter. Returns an empty list if the
     * filter is not present.
     */
    public List<DimensionalItemObject> getFilterOptions( String dimensionKey, List<DimensionalObject> filters )
    {
        int index = filters.indexOf( new BaseDimensionalObject( dimensionKey ) );

        return index != -1 ? filters.get( index ).getItems() : new ArrayList<>();
    }

}
