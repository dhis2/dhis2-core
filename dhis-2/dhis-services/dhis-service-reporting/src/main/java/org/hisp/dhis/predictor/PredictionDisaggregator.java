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
package org.hisp.dhis.predictor;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hisp.dhis.predictor.PredictionDisaggregatorUtils.getSortedOptions;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.period.Period;

import com.google.common.collect.ImmutableSet;

/**
 * Generator of disaggregated predictions.
 * <p>
 * If the predictor output data element has a non-default (disaggregation)
 * category combo (CC), and the predictor output (category) option combo (COC)
 * is null, this means that all disaggregations (COCs) of the output data
 * element are computed independently: one prediction for each COC.
 * <p>
 * For each data element in the generator expression having a CC matching that
 * of the output data element, we will fetch all the COCs of that data element
 * independently. When generating the prediction for each COC, we will use the
 * corresponding COC from each such data element. (This does not apply to data
 * element operands where an explicit COC is specified. They are used as
 * specified.)
 *
 * @author Jim Grace
 */
@Slf4j
@Builder
public class PredictionDisaggregator
{
    /**
     * The predictor being used.
     */
    private final Predictor predictor;

    /**
     * The system default category option combination.
     */
    private final CategoryOptionCombo defaultCOC;

    /**
     * The items in the generator and skipTest expressions.
     */
    private final Collection<DimensionalItemObject> items;

    /**
     * The output data element's category combination.
     */
    private final CategoryCombo outputCatCombo;

    /**
     * Whether we are generating disaggregated predictions.
     */
    private final boolean disagPredictions;

    /**
     * List for warnings of disaggregations we could not map.
     */
    private final Set<DataElementOperand> unmappedDeos = new ConcurrentHashSet<>();

    /**
     * When disaggregated predictions are used, this maps sorted options to the
     * output category option combination for the output data element.
     */
    @Builder.Default
    private final Map<String, String> sortedOptionsToCoc = emptyMap();

    /**
     * UIDs of the data element category combos that will be mapped to the
     * output category option combination of the output data element.
     */
    @Builder.Default
    private final Set<String> mappedCategoryCombos = emptySet();

    /**
     * Gets the items we will fetch the data for, including fetching any
     * existing predictions.
     * <p>
     * If this is a disaggregation prediction, then replace any matching data
     * elements (those with the same CC as the output data element) with a
     * wildcard data element operand, to collect all category option combos for
     * that data element.
     *
     * @return items to fetch data for
     */
    public Set<DimensionalItemObject> getDisaggregatedItems()
    {
        if ( !disagPredictions )
        {
            return new ImmutableSet.Builder<DimensionalItemObject>()
                .add( getOutputDataElementOperand() )
                .addAll( items ).build();
        }

        Set<DimensionalItemObject> inputItems = new ImmutableSet.Builder<DimensionalItemObject>()
            .add( predictor.getOutput() ) // Will be disaggregated below
            .addAll( items ).build();

        Set<DimensionalItemObject> itemSet = new HashSet<>();

        for ( DimensionalItemObject item : inputItems )
        {
            if ( item instanceof DataElement &&
                mappedCategoryCombos.contains( ((DataElement) item).getCategoryCombo().getUid() ) )
            {
                itemSet.add( new DataElementOperand( (DataElement) item, null ) );
            }
            else
            {
                itemSet.add( item );
            }
        }

        return itemSet;
    }

    /**
     * Gets the prediction output category option combo (COC). If we are
     * predicting for every COC in the output data element return null. If we
     * are predicting for only one COC, return that COC.
     *
     * @return Prediction output category option combo
     */
    public CategoryOptionCombo getOutputCoc()
    {
        return (disagPredictions)
            ? null
            : firstNonNull( predictor.getOutputCombo(), defaultCOC );
    }

    /**
     * Gets a data element operand representing the prediction output. The data
     * element operand will always include the output data element. If we are
     * predicting for every COC in the output data element the COC will be null.
     * If we are predicting for only one COC, that will be the COC.
     *
     * @return Prediction output data element operand
     */
    public DataElementOperand getOutputDataElementOperand()
    {
        return new DataElementOperand( predictor.getOutput(), getOutputCoc() );
    }

    /**
     * If this is a disaggregation prediction, then replaces each prediction
     * context with a list of prediction contexts, one for each category option
     * combination of the output data element.
     *
     * @param contexts list of disaggregation contexts
     * @return possibly expanded list of disaggregation contexts
     */
    public List<PredictionContext> getDisaggregateContexts( List<PredictionContext> contexts )
    {
        if ( !disagPredictions )
        {
            return contexts;
        }

        return contexts.stream()
            .map( this::disagregateContext )
            .flatMap( Collection::stream )
            .collect( toUnmodifiableList() );
    }

    public void issueMappingWarnings()
    {
        if ( !unmappedDeos.isEmpty() )
        {
            String deos = unmappedDeos.stream()
                .map( deo -> deo.getDataElement().getUid() + "." + deo.getCategoryOptionCombo().getUid() )
                .collect( joining( ", " ) );

            log.warn( String.format( "Predictor %s unmapped input disaggregations: %s", predictor.getUid(), deos ) );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Disaggregates a prediction context into a list of prediction contexts,
     * one for each output category option combo.
     */
    private List<PredictionContext> disagregateContext( PredictionContext context )
    {
        return outputCatCombo.getOptionCombos().stream()
            .map( coc -> getDisaggregatedContext( context, coc ) )
            .collect( toUnmodifiableList() );
    }

    /**
     * Generates a disaggregated context from an existing context but with
     * values for one particular disaggregation category option combination.
     */
    private PredictionContext getDisaggregatedContext( PredictionContext c, CategoryOptionCombo coc )
    {
        MapMap<Period, DimensionalItemObject, Object> periodValueMap = disaggregatePeriodValueMap( coc,
            c.getPeriodValueMap() );

        return new PredictionContext( coc, c.getAttributeOptionCombo(), c.getOutputPeriod(), periodValueMap );
    }

    /**
     * Creates a period value map with the original data elements restored that
     * were disaggregated (if the disaggregation has data). The data element is
     * given the value of the data element operand with the given COC.
     * <p>
     * The disaggregated data element operand values are also retained in the
     * map because it is possible that the expression could contain data element
     * operands with explicit COCs as well.
     */
    private MapMap<Period, DimensionalItemObject, Object> disaggregatePeriodValueMap( CategoryOptionCombo coc,
        MapMap<Period, DimensionalItemObject, Object> periodValueMap )
    {
        MapMap<Period, DimensionalItemObject, Object> disMap = new MapMap<>();

        for ( Map.Entry<Period, Map<DimensionalItemObject, Object>> e1 : periodValueMap.entrySet() )
        {
            Period period = e1.getKey();

            for ( Map.Entry<DimensionalItemObject, Object> e2 : e1.getValue().entrySet() )
            {
                DimensionalItemObject item = e2.getKey();
                Object value = e2.getValue();

                disMap.putEntry( period, item, value );

                if ( item instanceof DataElementOperand )
                {
                    DataElementOperand deo = (DataElementOperand) item;

                    if ( coc.getUid().equals( getCoc( deo ) ) )
                    {
                        disMap.putEntry( period, deo.getDataElement(), value );
                    }
                }
            }
        }

        return disMap;
    }

    /**
     * If a DEO's input COC maps to an output COC, returns the output COC, else
     * returns null.
     * <p>
     * If the input data element should map to the output COCs, but this COC
     * does not map, add the DEO to the list of unmapped DEOs so we can log it.
     */
    private String getCoc( DataElementOperand deo )
    {
        if ( !mappedCategoryCombos.contains( deo.getDataElement().getCategoryCombo().getUid() ) )
        {
            return null;
        }

        String sortedOptions = getSortedOptions( deo.getCategoryOptionCombo() );

        String coc = sortedOptionsToCoc.get( sortedOptions );

        if ( coc == null )
        {
            unmappedDeos.add( deo );
        }

        return coc;
    }
}
