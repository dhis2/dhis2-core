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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.analytics.tei.query.TeiFields.getGridHeaders;
import static org.hisp.dhis.common.DimensionItemType.ORGANISATION_UNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;
import static org.springframework.util.Assert.notNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.processing.ParamsEvaluator;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.CurrentUserService;
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
    private final CurrentUserService currentUserService;

    private final ParamsEvaluator paramsProcessor;

    /**
     * Based on the given headers and result map, this method takes care of the
     * logic needed to create a valid {@link Grid} object. If the given
     * "sqlQueryResult" is not present, the resulting {@link Grid} will have
     * empty rows.
     *
     * @param sqlQueryResult the optional of {@link SqlQueryResult}
     * @param teiQueryParams the {@link TeiQueryParams}
     * @param commonQueryRequest the {@link CommonQueryRequest}
     *
     * @return the {@link Grid} object
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null
     */
    public Grid createGrid( Optional<SqlQueryResult> sqlQueryResult, long rowsCount,
        @Nonnull TeiQueryParams teiQueryParams, @Nonnull CommonQueryRequest commonQueryRequest )
    {
        notNull( teiQueryParams, "The 'teiQueryParams' must not be null" );
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        Grid grid = new ListGrid();

        Set<GridHeader> headers = paramsProcessor.applyHeaders( getGridHeaders( teiQueryParams ),
            teiQueryParams.getCommonParams().getHeaders() );

        headers.forEach( grid::addHeader );

        // Adding rows.
        if ( sqlQueryResult.isPresent() )
        {
            grid.addRows( sqlQueryResult.get().result() );
        }

        // TODO: Extract this into a method named "addMetaData".
        if ( !commonQueryRequest.isSkipMeta() )
        {
            grid.setMetaData( getMetaData( teiQueryParams.getCommonParams(), commonQueryRequest, grid, rowsCount ) );
        }

        return grid;
    }

    /**
     * Applies paging to the given grid if the given query specifies paging.
     *
     * @param pagingParams the {@link AnalyticsPagingParams}.
     * @param grid the {@link Grid}.
     * @param rowsCount the total count.
     */
    private void addPaging( @Nonnull AnalyticsPagingParams pagingParams, @Nonnull Grid grid, long rowsCount )
    {
        Pager pager;

        if ( pagingParams.showTotalPages() )
        {
            pager = new Pager( pagingParams.getPageWithDefault(), rowsCount, pagingParams.getPageSizeWithDefault() );
        }
        else
        {
            pager = new SlimPager( pagingParams.getPageWithDefault(), pagingParams.getPageSizeWithDefault(),
                grid.hasLastDataRow() );
        }

        grid.getMetaData().put( PAGER.getKey(), pager );
    }

    /**
     * Returns a metadata {@link Map} based on the given arguments.
     *
     * @param commonParams the {@link CommonParams}
     * @param commonQueryRequest the {@link CommonQueryRequest}
     *
     * @return the metadata {@link Map}
     */
    // TODO: Remove CommonQueryRequest from here. The service and components should only see CommonParams.
    private Map<String, Object> getMetaData( CommonParams commonParams, @Nonnull CommonQueryRequest commonQueryRequest,
        @Nonnull Grid grid, long rowsCount )
    {
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        Map<String, Object> metaData = new HashMap<>();

        if ( commonParams == null )
        {
            return metaData;
        }

        addPaging( commonParams.getPagingParams(), grid, rowsCount );

        if ( hasDimensionalObjects( commonParams ) )
        {
            Map<String, MetadataItem> metaDataItems = new HashMap<>();
            Map<String, List<String>> metaDataDimensions = new HashMap<>();

            List<DimensionalObject> dimensionalObjects = getDimensionalObjects( commonParams );

            for ( DimensionalObject dimension : dimensionalObjects )
            {
                dimension.getItems()
                    .forEach( dio -> metaDataItems.put( dio.getUid(),
                        commonQueryRequest.isIncludeMetadataDetails()
                            ? new MetadataItem( dio.getDisplayName(), dio )
                            : new MetadataItem( dio.getDisplayName() ) ) );

                metaDataDimensions.put( dimension.getUid(), dimension.getItems().stream()
                    .map( PrimaryKeyObject::getUid ).collect( toList() ) );
            }

            metaData.put( ITEMS.getKey(), metaDataItems );
            metaData.put( DIMENSIONS.getKey(), metaDataDimensions );

            if ( commonQueryRequest.isHierarchyMeta() || commonQueryRequest.isShowHierarchy() )
            {
                List<OrganisationUnit> roots = currentUserService.getCurrentUser()
                    .getOrganisationUnits().stream().sorted().collect( toList() );

                List<OrganisationUnit> organisationUnits = commonParams.getDimensionIdentifiers()
                    .stream()
                    .flatMap( Collection::stream )
                    .map( DimensionIdentifier::getDimension )
                    .map( DimensionParam::getDimensionalObject )
                    .map( DimensionalObject::getItems )
                    .flatMap( Collection::stream )
                    .filter( i -> i.getDimensionItemType() == ORGANISATION_UNIT )
                    .map( OrganisationUnit.class::cast )
                    .collect( toList() );

                Map<String, Object> orgUnitsMetadata = addOrganisationUnitsHierarchyIntoMetaData( roots,
                    organisationUnits, commonQueryRequest.isHierarchyMeta(), commonQueryRequest.isShowHierarchy() );

                metaData.putAll( orgUnitsMetadata );
            }
        }

        return metaData;
    }

    private List<DimensionalObject> getDimensionalObjects( CommonParams commonParams )
    {
        return streamOfDimensionParams( commonParams )
            .filter( DimensionParam::isDimensionalObject )
            .map( DimensionParam::getDimensionalObject )
            .collect( toList() );
    }

    private boolean hasDimensionalObjects( CommonParams commonParams )
    {
        return streamOfDimensionParams( commonParams ).anyMatch( DimensionParam::isDimensionalObject );
    }

    private Stream<DimensionParam> streamOfDimensionParams( CommonParams commonParams )
    {
        return Optional.ofNullable( commonParams )
            .map( CommonParams::getDimensionIdentifiers )
            .orElse( Collections.emptyList() )
            .stream()
            .flatMap( Collection::stream )
            .map( DimensionIdentifier::getDimension );
    }

    private static Map<String, Object> addOrganisationUnitsHierarchyIntoMetaData( List<OrganisationUnit> roots,
        List<OrganisationUnit> organisationUnits, boolean hierarchyMeta, boolean showHierarchy )
    {
        Map<String, Object> metaData = new HashMap<>();

        if ( hierarchyMeta )
        {
            metaData.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
        }

        if ( showHierarchy )
        {
            Map<Object, List<?>> ancestorMap = organisationUnits.stream()
                .collect( toMap( OrganisationUnit::getUid, ou -> ou.getAncestorNames( roots, true ) ) );

            metaData.put( ORG_UNIT_ANCESTORS.getKey(), ancestorMap );
            metaData.put( ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap( organisationUnits, roots, true ) );
        }

        return metaData;
    }
}
