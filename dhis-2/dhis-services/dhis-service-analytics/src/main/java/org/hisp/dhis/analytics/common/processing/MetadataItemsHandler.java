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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.analytics.common.processing.MetadataParamsHandler.getItemUid;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptions;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptionsAsFilter;
import static org.hisp.dhis.analytics.orgunit.OrgUnitHelper.getActiveOrganisationUnits;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * This class handles the logic necessary to generate the metadata dimension
 * items.
 *
 * It builds all data structure and maps required by the metadata object. It
 * works on top of common objects, so it can be reused by different analytics
 * services/endpoints.
 *
 * This class and methods were pulled from other part of the code, so we could
 * have a centralized way to generate and keep the logic related to analytics
 * metadata elements. Light changes were applied to make the code slightly
 * cleaner. Major structural changes were not applied to reduce the risk of
 * bugs.
 */
public class MetadataItemsHandler
{
    /**
     * Handles all required logic/rules in order to return a map of metadata
     * item identifiers and its respective {@link MetadataItem}.
     *
     * @param grid the {@link Grid}.
     * @param commonParams the {@link CommonParams}.
     * @return the map of {@link MetadataItem}.
     */
    Map<String, MetadataItem> handle( Grid grid, CommonParams commonParams )
    {
        List<QueryItem> items = commonParams.delegate().getAllItems();
        Set<Option> itemOptions = commonParams.delegate().getItemsOptions();
        Map<String, List<Option>> optionsPresentInGrid = getItemOptions( grid, items );
        Set<Option> optionItems = getOptionItems( grid, itemOptions, items, optionsPresentInGrid );
        List<DimensionalObject> allDimensionalObjects = commonParams.delegate().getAllDimensionalObjects();
        List<DimensionItemKeywords.Keyword> periodKeywords = commonParams.delegate().getPeriodKeywords();
        Set<Legend> itemLegends = commonParams.delegate().getItemsLegends();
        List<Program> programs = commonParams.getPrograms();
        Set<ProgramStage> programStages = commonParams.delegate().getProgramStages();
        boolean includeMetadataDetails = commonParams.isIncludeMetadataDetails();
        DisplayProperty displayProperty = commonParams.getDisplayProperty();
        DimensionalItemObject value = commonParams.getValue();
        Map<String, MetadataItem> metadataItemMap = getDimensionMetadataItemMap( grid, allDimensionalObjects,
            displayProperty, programs, programStages, includeMetadataDetails );

        if ( value != null )
        {
            metadataItemMap.put( value.getUid(),
                new MetadataItem( value.getDisplayProperty( displayProperty ),
                    includeMetadataDetails ? value.getUid() : null, value.getCode() ) );
        }

        itemLegends.stream()
            .filter( Objects::nonNull )
            .forEach( legend -> metadataItemMap.put( legend.getUid(),
                new MetadataItem( legend.getDisplayName(), includeMetadataDetails ? legend.getUid() : null,
                    legend.getCode() ) ) );

        putItemOptionsIntoMap( metadataItemMap, optionItems, displayProperty, includeMetadataDetails );

        items.stream()
            .filter( Objects::nonNull )
            .forEach( item -> addItemToMap(
                metadataItemMap, item, includeMetadataDetails, displayProperty ) );

        boolean hasPeriodKeywords = periodKeywords != null && !periodKeywords.isEmpty();

        if ( hasPeriodKeywords )
        {
            for ( DimensionItemKeywords.Keyword keyword : periodKeywords )
            {
                if ( keyword.getMetadataItem() != null )
                {
                    metadataItemMap.put( keyword.getKey(),
                        new MetadataItem( keyword.getMetadataItem().getName() ) );
                }
            }
        }

        return metadataItemMap;
    }

    /**
     * Gets the set of {@link Option} based on the given items and options.
     *
     * @param grid the {@link Grid}.
     * @param itemOptions the set of {@link Option}.
     * @param items the list of {@link QueryItem}.
     * @param optionsPresentInGrid the map of {@link Option} present in the
     *        grid.
     * @return the set of relevant {@link Option}.
     */
    private Set<Option> getOptionItems( Grid grid, Set<Option> itemOptions, List<QueryItem> items,
        Map<String, List<Option>> optionsPresentInGrid )
    {
        Set<Option> optionItems = new LinkedHashSet<>();

        boolean hasResults = isNotEmpty( grid.getRows() );

        if ( hasResults )
        {
            optionItems.addAll( optionsPresentInGrid.values().stream()
                .flatMap( List::stream ).distinct().collect( toList() ) );
        }
        else
        {
            optionItems.addAll( getItemOptionsAsFilter( itemOptions, items ) );
        }

        return optionItems;
    }

    /**
     * Adds the given item to the given {@link MetadataItem} item map. It
     * respects the boolean "includeItemDetails".
     *
     * @param metadataItemMap the {@link MetadataItem} item map.
     * @param item the {@link QueryItem}.
     * @param includeItemDetails whether to include metadata item details.
     * @param displayProperty the {@link DisplayProperty}.
     */
    private void addItemToMap( Map<String, MetadataItem> metadataItemMap, QueryItem item,
        boolean includeItemDetails,
        DisplayProperty displayProperty )
    {
        MetadataItem metadataItem = new MetadataItem( item.getItem().getDisplayProperty( displayProperty ),
            returnSameOrNull( includeItemDetails, item.getItem() ) );

        metadataItemMap.put( getItemUid( item ), metadataItem );

        // Done for backwards compatibility.
        metadataItemMap.put( item.getItemId(), metadataItem );
    }

    /**
     * Returns a map of dimensions and related items identifiers, including org.
     * units, periods, data elements, programs, stages, etc.
     *
     * @param grid the {@link Grid}.
     * @param dimensionsAndFilters the list of {@link DimensionalObject}.
     * @param displayProperty the {@link DisplayProperty}.
     * @param programs the list of {@link Program}.
     * @param programStages the list of {@link ProgramStage}.
     * @param includeMetadataDetails whether to include item details.
     * @return a map of {@link MetadataItem} and its respective key.
     */
    private Map<String, MetadataItem> getDimensionMetadataItemMap( Grid grid,
        List<DimensionalObject> dimensionsAndFilters, DisplayProperty displayProperty,
        @CheckForNull List<Program> programs,
        @CheckForNull Set<ProgramStage> programStages, boolean includeMetadataDetails )
    {
        Map<String, MetadataItem> dimensionItemMap = new HashMap<>();

        putDimensionItemsIntoMap( dimensionItemMap, grid, dimensionsAndFilters, displayProperty,
            includeMetadataDetails );

        if ( isNotEmpty( programs ) )
        {
            programs.forEach(
                program -> dimensionItemMap.put( program.getUid(),
                    new MetadataItem( program.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, program ) ) ) );
        }

        if ( isNotEmpty( programStages ) )
        {
            programStages.forEach( programStage -> dimensionItemMap.put( programStage.getUid(),
                new MetadataItem( programStage.getDisplayProperty( displayProperty ),
                    returnSameOrNull( includeMetadataDetails, programStage ) ) ) );
        }
        else if ( isNotEmpty( programs ) )
        {
            programs.forEach(
                program -> {
                    for ( ProgramStage ps : program.getProgramStages() )
                    {
                        dimensionItemMap.put( ps.getUid(),
                            new MetadataItem( ps.getDisplayProperty( displayProperty ),
                                returnSameOrNull( includeMetadataDetails, ps ) ) );
                    }
                } );
        }

        return dimensionItemMap;
    }

    /**
     * Adds a list of {@link DimensionalObject} objects into the given map of
     * {@link MetadataItem}. It takes into consideration display property and
     * the metadata details flag and the {@link Grid} headers and rows.
     *
     * @param metadataItemMap the metadata item map.
     * @param grid the {@link Grid}.
     * @param dimensionsAndFilters the list of {@link DimensionalObject}.
     * @param displayProperty the {@link DisplayProperty}.
     * @param includeMetadataDetails whether to include option item details.
     */
    private void putDimensionItemsIntoMap( Map<String, MetadataItem> metadataItemMap, Grid grid,
        List<DimensionalObject> dimensionsAndFilters, DisplayProperty displayProperty,
        boolean includeMetadataDetails )
    {
        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        for ( DimensionalObject dimension : dimensionsAndFilters )
        {
            for ( DimensionalItemObject item : dimension.getItems() )
            {
                putPeriodOrDefaultItemIntoMap( metadataItemMap, displayProperty, includeMetadataDetails,
                    dimension.getDimensionType(), item );
                putDataElementIntoMap( metadataItemMap, displayProperty, includeMetadataDetails, item );

                if ( ORGANISATION_UNIT == dimension.getDimensionType() )
                {
                    organisationUnits = getActiveOrganisationUnits( grid, organisationUnits );
                }
            }

            for ( OrganisationUnit unit : organisationUnits )
            {
                for ( OrganisationUnit ancestor : unit.getAncestors() )
                {
                    metadataItemMap.put( ancestor.getUid(),
                        new MetadataItem( ancestor.getDisplayProperty( displayProperty ),
                            returnSameOrNull( includeMetadataDetails, ancestor ) ) );
                }
            }

            metadataItemMap.put( dimension.getDimension(),
                new MetadataItem( dimension.getDisplayProperty( displayProperty ),
                    returnSameOrNull( includeMetadataDetails, dimension ) ) );

            if ( dimension.getDimensionItemKeywords() != null )
            {
                dimension.getDimensionItemKeywords().getKeywords()
                    .forEach( b -> metadataItemMap.putIfAbsent( b.getKey(), b.getMetadataItem() ) );
            }
        }
    }

    /**
     * Adds the given {@link DimensionalItemObject} object into the given map of
     * {@link MetadataItem}. It takes into consideration display property and
     * the metadata details flag.
     *
     * @param metadataItemMap the metadata item map.
     * @param displayProperty the {@link DisplayProperty}.
     * @param includeMetadataDetails whether to include option item details.
     * @param dataElementItem the {@link DimensionalItemObject} to be added.
     */
    private void putDataElementIntoMap( Map<String, MetadataItem> metadataItemMap, DisplayProperty displayProperty,
        boolean includeMetadataDetails, DimensionalItemObject dataElementItem )
    {
        if ( DATA_ELEMENT == dataElementItem.getDimensionItemType() )
        {
            DataElement dataElement = (DataElement) dataElementItem;

            for ( CategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
            {
                metadataItemMap.put( coc.getUid(), new MetadataItem( coc.getDisplayProperty( displayProperty ),
                    returnSameOrNull( includeMetadataDetails, coc ) ) );
            }
        }
    }

    /**
     * Adds the given {@link DimensionalItemObject} object into the given map of
     * {@link MetadataItem}. It takes into consideration the dimension type,
     * display property and the metadata details flag.
     *
     * @param metadataItemMap the metadata item map.
     * @param displayProperty the {@link DisplayProperty}.
     * @param includeMetadataDetails whether to include option item details.
     * @param dimensionType the {@link DimensionType}.
     * @param periodOrDefaultItem the {@link DimensionalItemObject} to be added.
     */
    private void putPeriodOrDefaultItemIntoMap( Map<String, MetadataItem> metadataItemMap,
        DisplayProperty displayProperty,
        boolean includeMetadataDetails, DimensionType dimensionType, DimensionalItemObject periodOrDefaultItem )
    {
        Calendar calendar = PeriodType.getCalendar();

        if ( PERIOD == dimensionType && !calendar.isIso8601() )
        {
            Period period = (Period) periodOrDefaultItem;
            DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
            String isoDate = period.getPeriodType().getIsoDate( dateTimeUnit );

            metadataItemMap.put( isoDate, new MetadataItem( period.getDisplayName(),
                returnSameOrNull( includeMetadataDetails, period ) ) );
        }
        else
        {
            metadataItemMap.put( periodOrDefaultItem.getDimensionItem(),
                new MetadataItem( periodOrDefaultItem.getDisplayProperty( displayProperty ),
                    returnSameOrNull( includeMetadataDetails, periodOrDefaultItem ) ) );
        }
    }

    /**
     * Adds {@link Option} objects into the given map of {@link MetadataItem}.
     * It takes into consideration the display property and the metadata details
     * flag.
     *
     * @param metadataItemMap the metadata item map.
     * @param itemOptions the list of {@link Option}.
     * @param displayProperty the {@link DisplayProperty}.
     * @param includeMetadataDetails whether to include option item details.
     */
    private void putItemOptionsIntoMap( Map<String, MetadataItem> metadataItemMap,
        @CheckForNull Set<Option> itemOptions,
        DisplayProperty displayProperty, boolean includeMetadataDetails )
    {
        if ( isNotEmpty( itemOptions ) )
        {
            itemOptions.forEach( option -> metadataItemMap.put( option.getUid(),
                new MetadataItem( option.getDisplayProperty( displayProperty ),
                    returnSameOrNull( includeMetadataDetails, option.getUid() ), option.getCode() ) ) );
        }
    }

    /**
     * Utility method that simply returns the same given object if the flag is
     * true. Otherwise, it returns null.
     *
     * @param returnSame the boolean flag.
     * @param object the object to be returned.
     * @return the given object or null.
     * @param <T>
     */
    private <T extends Object> T returnSameOrNull( boolean returnSame, T object )
    {
        if ( returnSame )
        {
            return object;
        }

        return null;
    }
}
