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

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;
import static org.springframework.util.Assert.noNullElements;
import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.CommonQueryRequest;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.common.DimensionItemType;
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

    /**
     * /** Based on the given headers and result map, this method takes care of
     * the logic needed to create a valid Grid object.
     *
     * @param headers
     * @param resultMap
     * @return the Grid object
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null
     */
    public Grid createGrid( final List<GridHeader> headers,
        final Map<Column, List<Object>> resultMap, final CommonParams commonParams,
        final CommonQueryRequest commonQueryRequest )
    {
        notEmpty( headers, "The 'headers' must not be null/empty" );
        noNullElements( headers, "The 'headers' must not contain null elements" );
        notEmpty( resultMap, "The 'resultMap' must not be null/empty" );
        notNull( resultMap, "The 'queryResult' must not be null" );
        notNull( commonParams, "The 'commonParams' must not be null" );
        notNull( commonQueryRequest, "The 'commonQueryRequest' must not be null" );

        final Grid grid = new ListGrid();
        boolean rowsAdded = false;

        for ( final GridHeader header : headers )
        {
            final List<Object> columnRows = resultMap.get( Column.builder().alias( header.getName() ).build() );

            if ( !rowsAdded && isNotEmpty( columnRows ) )
            {
                columnRows.forEach( c -> grid.addRow() );
                rowsAdded = true;
            }

            if ( !commonQueryRequest.isSkipHeaders() )
            {
                // Note that the header column must match the result map key.
                grid.addHeader( header );
            }

            grid.addColumn( columnRows );
        }

        if ( !commonQueryRequest.isSkipMeta() )
        {
            grid.setMetaData( getMetadata( commonParams, commonQueryRequest ) );
        }

        return grid;
    }

    /**
     * returns metadata based on teiQueryParams and rendered by
     * commonQueryRequest
     *
     * @param commonParams
     * @param commonQueryRequest
     * @return
     */
    private Map<String, Object> getMetadata( final CommonParams commonParams,
        final CommonQueryRequest commonQueryRequest )
    {
        final Map<String, Object> metadata = new HashMap<>();

        metadata.put( PAGER.getKey(),
            commonQueryRequest.isTotalPages()
                ? new Pager( commonQueryRequest.getPage(), 1, commonQueryRequest.getPageSize() )
                : new SlimPager( commonQueryRequest.getPage(), commonQueryRequest.getPageSize(), true ) );

        if ( commonParams == null )
        {
            return metadata;
        }

        if ( commonParams.getDimensionIdentifiers() != null )
        {
            final Map<String, MetadataItem> metadataItems = new HashMap<>();

            commonParams.getDimensionIdentifiers()
                .stream()
                .flatMap( Collection::stream )
                .map( DimensionIdentifier::getDimension )
                .map( DimensionParam::getDimensionalObject )
                .map( DimensionalObject::getItems )
                .flatMap( Collection::stream )
                .forEach( dio -> metadataItems.put( dio.getUid(),
                    commonQueryRequest.isIncludeMetadataDetails()
                        ? new MetadataItem( dio.getDisplayName(), dio )
                        : new MetadataItem( dio.getDisplayName() ) ) );

            metadata.put( ITEMS.getKey(), metadataItems );

            final Map<String, List<String>> metadataDimensions = new HashMap<>();

            commonParams.getDimensionIdentifiers()
                .stream()
                .flatMap( Collection::stream )
                .forEach( di -> metadataDimensions.put( di.getDimension().getDimensionalObject().getUid(),
                    di.getDimension().getDimensionalObject().getItems().stream()
                        .map( PrimaryKeyObject::getUid ).collect( Collectors.toList() ) ) );

            metadata.put( DIMENSIONS.getKey(), metadataDimensions );

            if ( commonQueryRequest.isHierarchyMeta() || commonQueryRequest.isShowHierarchy() )
            {
                List<OrganisationUnit> roots = currentUserService.getCurrentUser()
                    .getOrganisationUnits().stream().sorted().collect( Collectors.toList() );

                List<OrganisationUnit> organisationUnits = commonParams.getDimensionIdentifiers()
                    .stream()
                    .flatMap( Collection::stream )
                    .map( DimensionIdentifier::getDimension )
                    .map( DimensionParam::getDimensionalObject )
                    .map( DimensionalObject::getItems )
                    .flatMap( Collection::stream )
                    .filter( i -> i.getDimensionItemType() == DimensionItemType.ORGANISATION_UNIT )
                    .map( OrganisationUnit.class::cast )
                    .collect( Collectors.toList() );

                final Map<String, Object> orgUnitsMetadata = putOrganisationUnitsHierarchyToMetadata( roots,
                    organisationUnits, commonQueryRequest.isHierarchyMeta(), commonQueryRequest.isShowHierarchy() );

                metadata.putAll( orgUnitsMetadata );
            }
        }

        return metadata;
    }

    private static Map<String, Object> putOrganisationUnitsHierarchyToMetadata( List<OrganisationUnit> roots,
        List<OrganisationUnit> organisationUnits,
        boolean hierarchyMeta, boolean showHierarchy )
    {
        final Map<String, Object> metadata = new HashMap<>();

        if ( hierarchyMeta )
        {
            metadata.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
        }

        if ( showHierarchy )
        {
            Map<Object, List<?>> ancestorMap = organisationUnits.stream()
                .collect( toMap( OrganisationUnit::getUid, ou -> ou.getAncestorNames( roots, true ) ) );

            metadata.put( ORG_UNIT_ANCESTORS.getKey(), ancestorMap );

            metadata.put( ORG_UNIT_NAME_HIERARCHY.getKey(),
                getParentNameGraphMap( organisationUnits, roots, true ) );
        }

        return metadata;
    }
}
