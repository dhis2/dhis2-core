/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Analytics component responsible for centralizing, evaluating and processing
 * query parameters provided by the consumer. It should act on the most granular
 * param objects to make it more reusable across all analytics queries.
 *
 * @author maikel arabori
 */
@AllArgsConstructor
@Component
public class ParamsHandler
{
    @Nonnull
    private final CurrentUserService currentUserService;

    /**
     * Adds the correct headers into the given {@Grid}.
     *
     * @param grid the {@link Grid}.
     * @param teiQueryParams all headers represent by a set of
     *        {@link GridHeader}.
     * @return a new set of {@link GridHeader} containing elements that only
     *         match the param headers name.
     */
    public void addHeaders( Grid grid, TeiQueryParams teiQueryParams )
    {
        Set<GridHeader> headers = getGridHeaders( teiQueryParams );

        headers.forEach( grid::addHeader );
    }

    /**
     * Returns a metadata {@link Map} based on the given arguments.
     *
     * @param grid the current {@link Grid}.
     * @param commonParams the {@link CommonParams}.
     * @param commonQueryRequest the {@link CommonQueryRequest}.
     * @param rowsCount the total of rows found for the current query.
     */
    // TODO: Remove CommonQueryRequest from here. The service and components should only see CommonParams.
    public void addMetaData( @Nonnull Grid grid, CommonParams commonParams,
        @Nonnull CommonQueryRequest commonQueryRequest, long rowsCount )
    {
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        if ( !commonQueryRequest.isSkipMeta() )
        {
            if ( commonParams != null )
            {
                Map<String, Object> metaData = new HashMap<>();

                addPaging( metaData, commonParams.getPagingParams(), grid, rowsCount );
                addDimensions( metaData, commonParams, commonQueryRequest );
                addOrgUnitsHierarchy( metaData, commonParams, commonQueryRequest, currentUserService.getCurrentUser() );

                grid.setMetaData( metaData );
            }
        }
    }

    private void addDimensions( Map<String, Object> metaData, CommonParams commonParams,
        CommonQueryRequest commonQueryRequest )
    {
        List<DimensionalObject> dimensionalObjects = getDimensionalObjects( commonParams );

        if ( isNotEmpty( dimensionalObjects ) )
        {
            Map<String, MetadataItem> metaDataItems = new HashMap<>();
            Map<String, List<String>> metaDataDimensions = new HashMap<>();

            dimensionalObjects.forEach( dimension -> {
                dimension.getItems()
                    .forEach( dio -> metaDataItems.put( dio.getUid(),
                        commonQueryRequest.isIncludeMetadataDetails()
                            ? new MetadataItem( dio.getDisplayName(), dio )
                            : new MetadataItem( dio.getDisplayName() ) ) );

                metaDataDimensions.put( dimension.getUid(), dimension.getItems().stream()
                    .map( PrimaryKeyObject::getUid ).collect( toList() ) );
            } );

            metaData.put( ITEMS.getKey(), metaDataItems );
            metaData.put( DIMENSIONS.getKey(), metaDataDimensions );
        }
    }

    private void addOrgUnitsHierarchy( Map<String, Object> metaData, CommonParams commonParams,
        CommonQueryRequest commonQueryRequest, User currentUser )
    {
        if ( hasDimensionalObjects( commonParams ) )
        {
            if ( commonQueryRequest.isHierarchyMeta() || commonQueryRequest.isShowHierarchy() )
            {
                List<OrganisationUnit> roots = currentUser.getOrganisationUnits().stream()
                    .sorted().collect( toList() );

                List<OrganisationUnit> organisationUnits = commonParams.getDimensionIdentifiers()
                    .stream()
                    .map( DimensionIdentifier::getDimension )
                    .map( DimensionParam::getDimensionalObject )
                    .map( DimensionalObject::getItems )
                    .flatMap( Collection::stream )
                    .filter( i -> i.getDimensionItemType() == ORGANISATION_UNIT )
                    .map( OrganisationUnit.class::cast )
                    .collect( toList() );

                Map<String, Object> orgUnitsMetadata = getOrganisationUnitsHierarchyIntoMetaData( roots,
                    organisationUnits, commonQueryRequest.isHierarchyMeta(), commonQueryRequest.isShowHierarchy() );

                metaData.putAll( orgUnitsMetadata );
            }
        }
    }

    private Map<String, Object> getOrganisationUnitsHierarchyIntoMetaData( List<OrganisationUnit> roots,
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

    /**
     * Applies paging to the given "metaData" if the given query specifies
     * paging=true.
     *
     * @param pagingParams the {@link AnalyticsPagingParams}.
     * @param grid the {@link Grid}.
     * @param rowsCount the total count.
     */
    private void addPaging( @Nonnull Map<String, Object> metaData, @Nonnull AnalyticsPagingParams pagingParams,
        @Nonnull Grid grid, long rowsCount )
    {
        if ( pagingParams.isPaging() )
        {
            Pager pager;

            if ( pagingParams.showTotalPages() )
            {
                pager = new Pager( pagingParams.getPageWithDefault(), rowsCount,
                    pagingParams.getPageSizeWithDefault() );

                // Always try to remove last row.
                removeLastRow( pagingParams, grid );
            }
            else
            {
                boolean isLastPage = handleLastPageFlag( pagingParams, grid );

                pager = new SlimPager( pagingParams.getPageWithDefault(), pagingParams.getPageSizeWithDefault(),
                    isLastPage );
            }

            metaData.put( PAGER.getKey(), pager );
        }
    }

    /**
     * This method will handle the "lastPage" flag. Here, we assume that the
     * given {@Grid} might have page results + 1. We use this assumption to
     * return the correct boolean value.
     *
     * @param pagingParams the {@link AnalyticsPagingParams}.
     * @param grid the {@link Grid}.
     *
     * @return return true if this is the last page, false otherwise.
     */
    private boolean handleLastPageFlag( AnalyticsPagingParams pagingParams, Grid grid )
    {
        boolean isLastPage = grid.getHeight() > 0 && grid.getHeight() < pagingParams.getPageSizePlusOne();

        removeLastRow( pagingParams, grid );

        return isLastPage;
    }

    /**
     * As grid should have page size + 1 results, we need to remove the last row
     * if there are more pages left.
     *
     * @param pagingParams the {@link AnalyticsPagingParams}.
     * @param grid the {@link Grid}.
     */
    private void removeLastRow( AnalyticsPagingParams pagingParams, Grid grid )
    {
        boolean hasNextPageRow = grid.getHeight() == pagingParams.getPageSizePlusOne();

        if ( hasNextPageRow )
        {
            grid.removeCurrentWriteRow();
        }
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
            .orElse( emptyList() )
            .stream()
            .map( DimensionIdentifier::getDimension );
    }
}
