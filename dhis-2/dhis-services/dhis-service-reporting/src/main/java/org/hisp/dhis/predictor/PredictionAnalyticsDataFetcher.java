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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.Map4;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import com.google.common.collect.Lists;

/**
 * Fetches analytics data for a prediction.
 * <p>
 * This class can fetch values from analytics and return them for each
 * organisation unit. It assumes that they will be requested in ascending order
 * of organisation unit path.
 * <p>
 * It improves performance without using too much memory by fetching data from
 * analytics for multiple organisation units at a time (but not all organisation
 * units at once). It then returns data for the organisation unit requested.
 *
 * @author Jim Grace
 */
public class PredictionAnalyticsDataFetcher
{
    private final AnalyticsService analyticsService;

    public static final int PARTITION_SIZE = 500;

    private List<List<OrganisationUnit>> partitions;

    private List<OrganisationUnit> partition;

    private int partitionIndex;

    private Set<Period> periods;

    private Set<DimensionalItemObject> attributeOptionItems;

    private Set<DimensionalItemObject> nonAttributeOptionItems;

    private Map4<String, String, Period, DimensionalItemObject, Double> aocData;

    private MapMapMap<String, Period, DimensionalItemObject, Double> nonAocData;

    public PredictionAnalyticsDataFetcher( AnalyticsService analyticsService )
    {
        checkNotNull( analyticsService );

        this.analyticsService = analyticsService;
    }

    /**
     * Initializes for fetching analytics data.
     *
     * @param orgUnits organisation units to fetch.
     * @param periods periods to fetch.
     * @param attributeOptionItems items stored by AOC to fetch.
     * @param nonAttributeOptionItems items not stored by AOC to fetch.
     */
    public void init( List<OrganisationUnit> orgUnits, Set<Period> periods,
        Set<DimensionalItemObject> attributeOptionItems,
        Set<DimensionalItemObject> nonAttributeOptionItems )
    {
        this.periods = periods;
        this.attributeOptionItems = attributeOptionItems;
        this.nonAttributeOptionItems = nonAttributeOptionItems;

        partitions = Lists.partition( orgUnits, PARTITION_SIZE );

        partitionIndex = -1;

        getNextChunk(); // Get the first chunk of data
    }

    /**
     * Gets the analytics data for an organisation unit that is stored by
     * attribute option combination.
     *
     * @param orgUnit organisation unit to get data for.
     * @return value map by attribute option combo and period.
     */
    public MapMapMap<String, Period, DimensionalItemObject, Double> getAocData( OrganisationUnit orgUnit )
    {
        if ( attributeOptionItems.isEmpty() )
        {
            return new MapMapMap<>();
        }

        getNextChunkIfNeeded( orgUnit );

        return firstNonNull( aocData.get( orgUnit.getUid() ), new MapMapMap<>() );
    }

    /**
     * Gets the analytics data for an organisation unit that is not stored by
     * attribute option combination.
     *
     * @param orgUnit organisation unit to get data for.
     * @return value map by period.
     */
    public MapMap<Period, DimensionalItemObject, Double> getNonAocData( OrganisationUnit orgUnit )
    {
        if ( nonAttributeOptionItems.isEmpty() )
        {
            return new MapMap<>();
        }

        getNextChunkIfNeeded( orgUnit );

        return firstNonNull( nonAocData.get( orgUnit.getUid() ), new MapMap<>() );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Gets the next chunk if the org unit path isn't in this chunk.
     */
    private void getNextChunkIfNeeded( OrganisationUnit orgUnit )
    {
        if ( orgUnit.getPath().compareTo( partition.get( partition.size() - 1 ).getPath() ) > 0 )
        {
            getNextChunk();
        }
    }

    /**
     * Gets a chunk of analytics data for a partition of organisation units.
     */
    private void getNextChunk()
    {
        partitionIndex++;

        if ( partitionIndex >= partitions.size() )
        {
            throw new IllegalArgumentException(
                "Unexpected partitionIndex " + partitionIndex + " >= " + partitions.size() );
        }

        partition = partitions.get( partitionIndex );

        aocData = new Map4<>();
        nonAocData = new MapMapMap<>();

        if ( !attributeOptionItems.isEmpty() )
        {
            getDataValues( attributeOptionItems, true );
        }

        if ( !nonAttributeOptionItems.isEmpty() )
        {
            getDataValues( nonAttributeOptionItems, false );
        }
    }

    /**
     * Queries analytics for data.
     *
     * @param dimensionItems the dimensional item objects to fetch.
     * @param hasAttributeOptions whether these objects are stored with AOCs.
     */
    private void getDataValues( Set<DimensionalItemObject> dimensionItems, boolean hasAttributeOptions )
    {
        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withPeriods( new ArrayList<>( periods ) )
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withOrganisationUnits( partition );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
        }

        Grid grid = analyticsService.getAggregatedDataValues( paramsBuilder.build() );

        int peInx = grid.getIndexOfHeader( DimensionalObject.PERIOD_DIM_ID );
        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
        int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) : 0;
        int vlInx = grid.getWidth() - 1;

        Map<String, Period> periodLookup = periods.stream()
            .collect( Collectors.toMap( Period::getIsoDate, p -> p ) );
        Map<String, DimensionalItemObject> dimensionItemLookup = dimensionItems.stream()
            .collect( Collectors.toMap( DimensionalItemObject::getDimensionItem, d -> d ) );

        for ( List<Object> row : grid.getRows() )
        {
            String pe = (String) row.get( peInx );
            String dx = (String) row.get( dxInx );
            String ou = (String) row.get( ouInx );
            String ao = hasAttributeOptions ? (String) row.get( aoInx ) : null;
            Double vl = ((Number) row.get( vlInx )).doubleValue();

            Period period = periodLookup.get( pe );
            DimensionalItemObject dimensionItem = dimensionItemLookup.get( dx );

            if ( hasAttributeOptions )
            {
                aocData.putEntry( ou, ao, period, dimensionItem, vl );
            }
            else
            {
                nonAocData.putEntry( ou, period, dimensionItem, vl );
            }
        }
    }
}
