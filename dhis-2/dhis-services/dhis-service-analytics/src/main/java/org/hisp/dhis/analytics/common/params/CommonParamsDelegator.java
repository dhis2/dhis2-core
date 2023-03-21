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
package org.hisp.dhis.analytics.common.params;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.program.ProgramStage;

/**
 * Component that provides useful methods based on the {@link CommonParams}
 * object. Its main goal is to remove the responsibilities of
 * {@link CommonParams} providing operations that highly depends on it (highly
 * coupling).
 *
 * @author maikel arabori
 */
@RequiredArgsConstructor
public class CommonParamsDelegator
{
    private final List<DimensionIdentifier<DimensionParam>> dimensionIdentifiers;

    /**
     * Retrieves all {@link Option} objects from all existing {@link QueryItem}
     * objects part of the current "dimensionIdentifiers".
     *
     * @return the set of {@link Option}.
     */
    public Set<Option> getItemsOptions()
    {
        return dimensionIdentifiers.stream()
            .filter( dimParam -> dimParam != null && dimParam.getDimension() != null )
            .map( DimensionIdentifier::getDimension )
            .filter( DimensionParam::isQueryItem )
            .map( DimensionParam::getQueryItem )
            .filter( QueryItem::hasOptionSet )
            .map( q -> q.getOptionSet().getOptions() )
            .flatMap( List::stream )
            .collect( toSet() );
    }

    /**
     * Retrieves all {@link QueryItem} objects from all existing query items
     * objects part of the current "dimensionIdentifiers".
     *
     * @return the list of {@link QueryItem}.
     */
    public List<QueryItem> getAllItems()
    {
        return dimensionIdentifiers.stream()
            .filter( dimParam -> dimParam != null && dimParam.getDimension() != null )
            .map( DimensionIdentifier::getDimension )
            .filter( DimensionParam::isQueryItem )
            .map( DimensionParam::getQueryItem )
            .collect( toList() );
    }

    /**
     * Retrieves all {@link QueryItem} objects filters present in the current
     * "dimensionIdentifiers".
     *
     * @return the list of {@link QueryItem}.
     */
    public List<QueryItem> getItemsAsFilters()
    {
        return dimensionIdentifiers.stream()
            .filter( dimParam -> dimParam != null && dimParam.getDimension() != null )
            .map( DimensionIdentifier::getDimension )
            .filter( dimension -> dimension.isFilter() && dimension.isQueryItem() )
            .map( DimensionParam::getQueryItem )
            .collect( toList() );
    }

    /**
     * Retrieves all {@link DimensionalObject} objects present in the current
     * "dimensionIdentifiers".
     *
     * @return the list of {@link QueryItem}.
     */
    public List<DimensionalObject> getAllDimensionalObjects()
    {
        return dimensionIdentifiers.stream()
            .filter( dimParam -> dimParam != null && dimParam.getDimension() != null )
            .map( DimensionIdentifier::getDimension )
            .filter( DimensionParam::isDimensionalObject )
            .map( DimensionParam::getDimensionalObject ).collect( toList() );
    }

    /**
     * Retrieves all {@link DimensionalItemObject} present in the current
     * "dimensionIdentifiers" that represents a "pe" dimension.
     *
     * @return the list of {@link QueryItem}.
     */
    public List<DimensionalItemObject> getPeriodDimensionOrFilterItems()
    {
        return getDimensionOptions( PERIOD_DIM_ID, getAllDimensionalObjects() );
    }

    /**
     * Retrieves all {@link DimensionalItemObject} present in the current
     * "dimensionIdentifiers" that represents a "ou" dimension.
     *
     * @return the list of {@link QueryItem}.
     */
    public List<DimensionalItemObject> getOrgUnitDimensionOrFilterItems()
    {
        return getDimensionOptions( ORGUNIT_DIM_ID, getAllDimensionalObjects() );
    }

    /**
     * Extracts all {@link Keyword} objects all existing
     * {@link DimensionalObject} in the current "dimensionIdentifiers".
     *
     * @return the list of {@link Keyword}.
     */
    public List<Keyword> getPeriodKeywords()
    {
        return getAllDimensionalObjects().stream()
            .filter( Objects::nonNull )
            .map( DimensionalObject::getDimensionItemKeywords )
            .filter( dimensionItemKeywords -> dimensionItemKeywords != null && !dimensionItemKeywords.isEmpty() )
            .flatMap( dk -> dk.getKeywords().stream() ).collect( toList() );
    }

    /**
     * Retrieves all {@link Legend} objects from all query items that are part
     * of the current "dimensionIdentifiers".
     *
     * @return the list of {@link Legend}.
     */
    public Set<Legend> getItemsLegends()
    {
        return getAllItems().stream()
            .filter( queryItem -> queryItem != null && queryItem.hasLegendSet() )
            .map( i -> i.getLegendSet().getLegends() )
            .flatMap( Set::stream )
            .collect( toSet() );
    }

    /**
     * Retrieves all {@link ProgramStage} objects from the current
     * "dimensionIdentifiers".
     *
     * @return the set of {@link ProgramStage}.
     */
    public Set<ProgramStage> getProgramStages()
    {
        return dimensionIdentifiers.stream()
            .filter( DimensionIdentifier::hasProgramStage )
            .map( dimParam -> dimParam.getProgramStage().getElement() )
            .collect( toSet() );
    }

    /**
     * Retrieves a list of {@link DimensionalItemObject} objects for the given
     * list of {@link DimensionalObject}, based on the given dimension key.
     * Returns an empty list if the dimension is not present.
     *
     * @param dimensionKey the dimension key.
     * @param dimensions the list of {@link DimensionalObject}.
     *
     * @return the list of {@link DimensionalItemObject} or empty list.
     */
    public List<DimensionalItemObject> getDimensionOptions( String dimensionKey, List<DimensionalObject> dimensions )
    {
        int index = dimensions.indexOf( new BaseDimensionalObject( dimensionKey ) );

        return index != -1 ? dimensions.get( index ).getItems() : emptyList();
    }
}
