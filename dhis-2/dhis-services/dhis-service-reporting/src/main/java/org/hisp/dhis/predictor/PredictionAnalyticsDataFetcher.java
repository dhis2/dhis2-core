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
package org.hisp.dhis.predictor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;

import com.google.common.collect.Lists;

/**
 * Fetches analytics data for predictions.
 * <p>
 * This class can fetch values from analytics and return them for a list of
 * organisation units.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor
public class PredictionAnalyticsDataFetcher
{
    private final AnalyticsService analyticsService;

    private final CategoryService categoryService;

    private Set<Period> periods;

    private Map<String, Period> periodLookup;

    private Set<DimensionalItemObject> attributeOptionItems;

    private Set<DimensionalItemObject> nonAttributeOptionItems;

    private Map<String, DimensionalItemObject> analyticsItemsLookup;

    private CachingMap<String, CategoryOptionCombo> cocLookup;

    private Map<String, OrganisationUnit> orgUnitLookup;

    /**
     * Initializes for fetching analytics data.
     *
     * @param periods periods to fetch
     * @param analyticsItems analytics items to fetch
     */
    public void init( Set<Period> periods, Set<DimensionalItemObject> analyticsItems )
    {
        this.periods = periods;

        categorizeAnalyticsItems( analyticsItems );

        periodLookup = periods.stream()
            .collect( Collectors.toMap( Period::getIsoDate, p -> p ) );

        analyticsItemsLookup = analyticsItems.stream()
            .collect( Collectors.toMap( DimensionalItemObject::getDimensionItem, d -> d ) );

        cocLookup = new CachingMap<>();
    }

    /**
     * Gets analytics values.
     *
     * @param orgUnits organisation units to get data for
     * @return values as fetched from analytics
     */
    public List<FoundDimensionItemValue> getValues( List<OrganisationUnit> orgUnits )
    {
        orgUnitLookup = orgUnits.stream()
            .collect( Collectors.toMap( OrganisationUnit::getUid, o -> o ) );

        List<FoundDimensionItemValue> values = getValuesInternal( orgUnits, attributeOptionItems, true );

        values.addAll( getValuesInternal( orgUnits, nonAttributeOptionItems, false ) );

        return values;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Categorizes analytics items according to whether or not they are stored
     * with an attribute option combo.
     */
    private void categorizeAnalyticsItems( Set<DimensionalItemObject> analyticsItems )
    {
        attributeOptionItems = new HashSet<>();
        nonAttributeOptionItems = new HashSet<>();

        for ( DimensionalItemObject o : analyticsItems )
        {
            if ( hasAttributeOptions( o ) )
            {
                attributeOptionItems.add( o );
            }
            else
            {
                nonAttributeOptionItems.add( o );
            }
        }
    }

    /**
     * Checks to see if a dimensional item object has values stored in the
     * database by attribute option combo
     */
    private boolean hasAttributeOptions( DimensionalItemObject o )
    {
        return o.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
            || ((ProgramIndicator) o).getAnalyticsType() != AnalyticsType.ENROLLMENT;
    }

    /**
     * Queries analytics for data.
     */
    private List<FoundDimensionItemValue> getValuesInternal( List<OrganisationUnit> orgUnits,
        Set<DimensionalItemObject> dimensionItems, boolean hasAttributeOptions )
    {
        List<FoundDimensionItemValue> values = new ArrayList<>();

        if ( dimensionItems.isEmpty() )
        {
            return values;
        }

        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( periods ) )
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withOrganisationUnits( orgUnits );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Collections.emptyList() );
        }

        Grid grid = analyticsService.getAggregatedDataValues( paramsBuilder.build() );

        int peInx = grid.getIndexOfHeader( DimensionalObject.PERIOD_DIM_ID );
        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
        int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) : 0;
        int vlInx = grid.getWidth() - 1;

        for ( List<Object> row : grid.getRows() )
        {
            String pe = (String) row.get( peInx );
            String dx = (String) row.get( dxInx );
            String ou = (String) row.get( ouInx );
            String ao = hasAttributeOptions ? (String) row.get( aoInx ) : null;
            Object vl = row.get( vlInx );

            Period period = periodLookup.get( pe );
            DimensionalItemObject item = analyticsItemsLookup.get( dx );
            OrganisationUnit orgUnit = orgUnitLookup.get( ou );
            CategoryOptionCombo attributeOptionCombo = hasAttributeOptions
                ? cocLookup.get( ao, () -> categoryService.getCategoryOptionCombo( ao ) )
                : null;

            values.add( new FoundDimensionItemValue( orgUnit, period, attributeOptionCombo, item, vl ) );
        }

        return values;
    }
}
