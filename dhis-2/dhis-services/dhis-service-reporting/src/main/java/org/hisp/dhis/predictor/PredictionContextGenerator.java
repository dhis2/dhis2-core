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

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.period.Period;

/**
 * Generator of prediction contexts.
 *
 * @author Jim Grace
 */
public class PredictionContextGenerator
{
    private PredictionContextGenerator()
    {
    }

    /**
     * Generates prediction contexts. Each prediction context contains all the
     * values required to evaluate a predictor to (possibly) generate one
     * prediction.
     * <p>
     * All the data used to generate contexts has the same organisation unit.
     *
     * @param outputPeriods output periods (predict within each period)
     * @param values input prediction values (all with the same orgUnit)
     * @param defaultCategoryOptionCombo system default cat option combo
     * @return contexts for prediction evaluation
     */
    public static List<PredictionContext> getContexts( List<Period> outputPeriods,
        List<FoundDimensionItemValue> values, CategoryOptionCombo defaultCategoryOptionCombo )
    {
        List<PredictionContext> contexts = new ArrayList<>();

        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> aocMap = getAocMap(
            values, defaultCategoryOptionCombo );

        for ( Map.Entry<CategoryOptionCombo, MapMap<Period, DimensionalItemObject, Object>> e : aocMap.entrySet() )
        {
            CategoryOptionCombo aoc = e.getKey();
            MapMap<Period, DimensionalItemObject, Object> periodValueMap = e.getValue();

            for ( Period outputPeriod : outputPeriods )
            {
                contexts.add( new PredictionContext( aoc, outputPeriod, periodValueMap,
                    firstNonNull( periodValueMap.get( outputPeriod ), new HashMap<>() ) ) );
            }
        }

        return contexts;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Generates a map of data by attribute option combo and period.
     * <p>
     * If there is no data, then generate predictions for the default attribute
     * option combo. (If a predictor has missing value strategy NEVER_SKIP, it
     * could generate a value even if there is no input data.)
     */
    private static MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> getAocMap(
        List<FoundDimensionItemValue> values, CategoryOptionCombo defaultCategoryOptionCombo )
    {
        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map = getAocDataMap( values );

        if ( map.isEmpty() )
        {
            map.put( defaultCategoryOptionCombo, new MapMap<>() );
        }

        addNonAocData( map, values );

        return map;
    }

    /**
     * Generates a map from all the data that is stored by attribute option
     * combination.
     */
    private static MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> getAocDataMap(
        List<FoundDimensionItemValue> values )
    {
        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map = new MapMapMap<>();

        for ( FoundDimensionItemValue value : values )
        {
            if ( value.getAttributeOptionCombo() != null )
            {
                map.putEntry( value.getAttributeOptionCombo(), value.getPeriod(),
                    value.getDimensionalItemObject(), value.getValue() );
            }
        }

        return map;
    }

    /**
     * Adds non-attribute option combo data to data for each AOC.
     * <p>
     * Data that is not stored in the database with attribute option combos is
     * used in predictions for each attribute option combo.
     */
    private static void addNonAocData( MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map,
        List<FoundDimensionItemValue> values )
    {
        for ( FoundDimensionItemValue value : values )
        {
            if ( value.getAttributeOptionCombo() == null )
            {
                for ( CategoryOptionCombo aoc : map.keySet() )
                {
                    map.putEntry( aoc, value.getPeriod(), value.getDimensionalItemObject(), value.getValue() );
                }
            }
        }
    }
}
