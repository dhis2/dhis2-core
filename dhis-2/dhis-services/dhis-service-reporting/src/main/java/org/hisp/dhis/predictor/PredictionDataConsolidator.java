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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * Consolidates the prediction data for one organisation unit at a time,
 * combining both aggregate data values and analytics data values.
 *
 * @author Jim Grace
 */
public class PredictionDataConsolidator
{
    private final PredictionDataValueFetcher dataValueFetcher;

    private final PredictionAnalyticsDataFetcher analyticsFetcher;

    private final Set<DataElement> dataElements;

    private final Set<DataElementOperand> dataElementOperands;

    private final Set<DimensionalItemObject> analyticsItems;

    private Queue<OrganisationUnit> orgUnitsRemaining;

    private Queue<PredictionData> readyPredictionData;

    @Setter // to change for testing
    private int analyticsBatchFetchSize = 500;

    /**
     * @param items dimensional items to be subsequently retrieved
     * @param includeDescendants whether to include descendants
     */
    public PredictionDataConsolidator( Set<DimensionalItemObject> items, boolean includeDescendants,
        PredictionDataValueFetcher dataValueFetcher, PredictionAnalyticsDataFetcher analyticsFetcher )
    {
        this.dataValueFetcher = dataValueFetcher;
        this.analyticsFetcher = analyticsFetcher;

        dataValueFetcher.setIncludeDeleted( true ).setIncludeDescendants( includeDescendants );

        dataElements = new HashSet<>();
        dataElementOperands = new HashSet<>();
        analyticsItems = new HashSet<>();

        categorizeItems( items );
    }

    /**
     * Initializes for data retrieval.
     *
     * @param orgUnits organisation units to fetch
     * @param orgUnitLevel level of organisation units to fetch
     * @param dataValueQueryPeriods existing periods for data value queries
     * @param analyticsQueryPeriods existing periods for analytics queries
     * @param outputDataElementOperand prediction output data element operand
     */
    public void init( Set<OrganisationUnit> currentUserOrgUnits, int orgUnitLevel, List<OrganisationUnit> orgUnits,
        Set<Period> dataValueQueryPeriods, Set<Period> analyticsQueryPeriods, Set<Period> existingOutputPeriods,
        DataElementOperand outputDataElementOperand )
    {
        orgUnitsRemaining = new ArrayDeque<>( orgUnits );

        readyPredictionData = new ArrayDeque<>();

        dataValueFetcher.init( currentUserOrgUnits, orgUnitLevel, orgUnits, dataValueQueryPeriods,
            existingOutputPeriods, dataElements, dataElementOperands, outputDataElementOperand );

        analyticsFetcher.init( analyticsQueryPeriods, analyticsItems );
    }

    /**
     * Returns the prediction data for one organisation unit, or null if
     * prediction data has been returned for all organisation units.
     *
     * @return prediction data
     */
    public PredictionData getData()
    {
        if ( readyPredictionData.isEmpty() && !orgUnitsRemaining.isEmpty() )
        {
            getPredictionDataBatch();
        }

        return readyPredictionData.poll();
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Categories DimensionalItemObjects found in the predictor expression (and
     * skip test) according to how their values will be fetched from either the
     * datavalue table (DataElement or DataElementOperand) or analytics.
     */
    private void categorizeItems( Set<DimensionalItemObject> items )
    {
        for ( DimensionalItemObject i : items )
        {
            if ( i instanceof DataElement )
            {
                dataElements.add( (DataElement) i );
            }
            else if ( i instanceof DataElementOperand )
            {
                dataElementOperands.add( (DataElementOperand) i );
            }
            else
            {
                analyticsItems.add( i );
            }
        }
    }

    /**
     * Gets a batch of data from data values and analytics.
     */
    private void getPredictionDataBatch()
    {
        // Get a batch of orgUnits to fetch analytics data from, with any
        // returned DataValues, up to analyticsBatchFetchSize.

        getDataValues();

        addOrgUnitsWithoutDataValues();

        // Fetch analytics data from this batch of orgUnits, add to ready data.

        List<FoundDimensionItemValue> analyticsValues = analyticsFetcher.getValues( getReadyOrgUnits() );

        addValuesToReadyData( analyticsValues );
    }

    /**
     * Gets DataValues for as many orgUnits as have them (up to
     * analyticsBatchFetchSize).
     */
    private void getDataValues()
    {
        PredictionData data;

        for ( int dataCount = 0; dataCount < analyticsBatchFetchSize
            && (data = dataValueFetcher.getData()) != null; dataCount++ )
        {
            readyPredictionData.add( data );

            orgUnitsRemaining.remove( data.getOrgUnit() );
        }
    }

    /**
     * If more orgUnits are needed, adds them (without data values) up to
     * analyticsBatchFetchSize or until no more orgUnits remain.
     */
    private void addOrgUnitsWithoutDataValues()
    {
        int countToAdd = Math.min(
            orgUnitsRemaining.size(),
            analyticsBatchFetchSize - readyPredictionData.size() );

        for ( int i = 0; i < countToAdd; i++ )
        {
            OrganisationUnit orgUnit = orgUnitsRemaining.poll();

            readyPredictionData.add( new PredictionData( orgUnit, new ArrayList<>(), Collections.emptyList() ) );
        }
    }

    /**
     * Gets a list of organisation units that are ready for analytics data.
     */
    private List<OrganisationUnit> getReadyOrgUnits()
    {
        return readyPredictionData.stream()
            .map( PredictionData::getOrgUnit )
            .collect( Collectors.toList() );
    }

    /**
     * Adds analytics values to the ready data.
     */
    private void addValuesToReadyData( List<FoundDimensionItemValue> analyticsValues )
    {
        Map<OrganisationUnit, PredictionData> map = readyPredictionData.stream()
            .collect( Collectors.toMap( PredictionData::getOrgUnit, p -> p ) );

        for ( FoundDimensionItemValue value : analyticsValues )
        {
            map.get( value.getOrganisationUnit() ).getValues().add( value );
        }
    }
}
