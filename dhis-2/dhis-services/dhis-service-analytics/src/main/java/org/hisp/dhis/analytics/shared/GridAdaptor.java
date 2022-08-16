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
package org.hisp.dhis.analytics.shared;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;
import static org.springframework.util.Assert.notNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
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

    /**
     * /** Based on the given headers and result map, this method takes care of
     * the logic needed to create a valid Grid object.
     *
     * @param sqlQueryResult
     * @param commonParams
     * @param commonQueryRequest
     * @return the Grid object
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null
     */
    public Grid createGrid( final SqlQueryResult sqlQueryResult, final CommonParams commonParams,
        final CommonQueryRequest commonQueryRequest )
    {
        notNull( sqlQueryResult, "The 'sqlQueryResult' must not be null" );
        notNull( sqlQueryResult.result(), "The 'sqlQueryResult.result' must not be null" );
        notNull( commonParams, "The 'commonParams' must not be null" );
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        final Grid grid = new ListGrid();
        grid.addHeaders( sqlQueryResult.result().getMetaData(), true );
        grid.addRows( sqlQueryResult.result() );

        if ( !commonQueryRequest.isSkipMeta() )
        {
            grid.setMetaData( getMetaData( commonParams, commonQueryRequest ) );
        }

        return grid;
    }

    /**
     * Returns the metadata map based on the commonParams and
     * commonQueryRequest.
     *
     * @param commonParams
     * @param commonQueryRequest
     * @return the metadata map
     */
    private Map<String, Object> getMetaData( final CommonParams commonParams,
        final CommonQueryRequest commonQueryRequest )
    {
        final Map<String, Object> metaData = new HashMap<>();

        metaData.put( PAGER.getKey(),
            commonQueryRequest.isTotalPages()
                ? new Pager( commonQueryRequest.getPage(), 1, commonQueryRequest.getPageSize() )
                : new SlimPager( commonQueryRequest.getPage(), commonQueryRequest.getPageSize(), true ) );

        if ( commonParams == null )
        {
            return metaData;
        }

        if ( commonParams.getDimensionIdentifiers() != null )
        {
            final Map<String, MetadataItem> metaDataItems = new HashMap<>();

            commonParams.getDimensionIdentifiers()
                .stream()
                .flatMap( Collection::stream )
                .map( DimensionIdentifier::getDimension )
                .map( DimensionParam::getDimensionalObject )
                .map( DimensionalObject::getItems )
                .flatMap( Collection::stream )
                .forEach( dio -> metaDataItems.put( dio.getUid(),
                    commonQueryRequest.isIncludeMetadataDetails()
                        ? new MetadataItem( dio.getDisplayName(), dio )
                        : new MetadataItem( dio.getDisplayName() ) ) );

            metaData.put( ITEMS.getKey(), metaDataItems );

            final Map<String, List<String>> metaDataDimensions = new HashMap<>();

            commonParams.getDimensionIdentifiers()
                .stream()
                .flatMap( Collection::stream )
                .forEach( di -> metaDataDimensions.put( di.getDimension().getDimensionalObject().getUid(),
                    di.getDimension().getDimensionalObject().getItems().stream()
                        .map( PrimaryKeyObject::getUid ).collect( toList() ) ) );

            metaData.put( DIMENSIONS.getKey(), metaDataDimensions );

            if ( commonQueryRequest.isHierarchyMeta() || commonQueryRequest.isShowHierarchy() )
            {
                final List<OrganisationUnit> roots = currentUserService.getCurrentUser()
                    .getOrganisationUnits().stream().sorted().collect( toList() );

                final List<OrganisationUnit> organisationUnits = commonParams.getDimensionIdentifiers()
                    .stream()
                    .flatMap( Collection::stream )
                    .map( DimensionIdentifier::getDimension )
                    .map( DimensionParam::getDimensionalObject )
                    .map( DimensionalObject::getItems )
                    .flatMap( Collection::stream )
                    .filter( i -> i.getDimensionItemType() == DimensionItemType.ORGANISATION_UNIT )
                    .map( OrganisationUnit.class::cast )
                    .collect( toList() );

                final Map<String, Object> orgUnitsMetadata = addOrganisationUnitsHierarchyIntoMetaData( roots,
                    organisationUnits, commonQueryRequest.isHierarchyMeta(), commonQueryRequest.isShowHierarchy() );

                metaData.putAll( orgUnitsMetadata );
            }
        }

        return metaData;
    }

    private static Map<String, Object> addOrganisationUnitsHierarchyIntoMetaData( final List<OrganisationUnit> roots,
        final List<OrganisationUnit> organisationUnits, final boolean hierarchyMeta, boolean showHierarchy )
    {
        final Map<String, Object> metaData = new HashMap<>();

        if ( hierarchyMeta )
        {
            metaData.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
        }

        if ( showHierarchy )
        {
            Map<Object, List<?>> ancestorMap = organisationUnits.stream()
                .collect( toMap( OrganisationUnit::getUid, ou -> ou.getAncestorNames( roots, true ) ) );

            metaData.put( ORG_UNIT_ANCESTORS.getKey(), ancestorMap );

            metaData.put( ORG_UNIT_NAME_HIERARCHY.getKey(),
                getParentNameGraphMap( organisationUnits, roots, true ) );
        }

        return metaData;
    }
}
