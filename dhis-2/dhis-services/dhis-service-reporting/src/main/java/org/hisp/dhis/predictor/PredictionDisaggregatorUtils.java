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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.dataelement.DataElement;

/**
 * Utilities for disaggregating predictions.
 *
 * @author Jim Grace
 */
public class PredictionDisaggregatorUtils
{
    private PredictionDisaggregatorUtils()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * Factory method to create a new {@link PredictionDisaggregator}.
     *
     * @param predictor the {@link Predictor}
     * @param defaultCOC the system default category option combo
     * @param items the expression items needed for the prediction
     * @return a {@link PredictionDisaggregator}
     */
    public static PredictionDisaggregator createPredictionDisaggregator( Predictor predictor,
        CategoryOptionCombo defaultCOC, Collection<DimensionalItemObject> items )
    {
        CategoryCombo outputCatCombo = predictor.getOutput().getCategoryCombo();
        boolean disagPredictions = !outputCatCombo.isDefault() && predictor.getOutputCombo() == null;

        PredictionDisaggregator.PredictionDisaggregatorBuilder builder = PredictionDisaggregator.builder()
            .predictor( predictor )
            .defaultCOC( defaultCOC )
            .items( items )
            .outputCatCombo( outputCatCombo )
            .disagPredictions( disagPredictions );

        if ( disagPredictions )
        {
            Map<String, String> sortedOptionsToCoc = getSortedOptionsToCoc( outputCatCombo );
            Set<String> getMappedCategoryCombos = getMappedCategoryCombos( items, sortedOptionsToCoc );

            builder.sortedOptionsToCoc( sortedOptionsToCoc )
                .mappedCategoryCombos( getMappedCategoryCombos );
        }

        return builder.build();
    }

    /**
     * Returns a concatenated string of the sorted option UIDs from the combo.
     *
     * @param coc category option combo containing the options.
     * @return String of sorted option UIDs.
     */
    public static String getSortedOptions( CategoryOptionCombo coc )
    {
        return coc.getCategoryOptions().stream()
            .map( CategoryOption::getUid )
            .sorted()
            .collect( joining() );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Generates a map from sorted options to a category option combo that is
     * valid for the output data element category combo.
     */
    private static Map<String, String> getSortedOptionsToCoc( CategoryCombo outputCatCombo )
    {
        return outputCatCombo.getOptionCombos().stream()
            .collect( toMap( PredictionDisaggregatorUtils::getSortedOptions, CategoryOptionCombo::getUid ) );
    }

    /**
     * Finds all the input category combos where every category option combo
     * maps to a valid category option combo for the output data element.
     */
    private static Set<String> getMappedCategoryCombos( Collection<DimensionalItemObject> items,
        Map<String, String> sortedOptionsMap )
    {
        Set<CategoryCombo> inputDataElementCategoryCombos = items.stream()
            .filter( DataElement.class::isInstance )
            .map( DataElement.class::cast )
            .map( DataElement::getCategoryCombo )
            .collect( toUnmodifiableSet() );

        Set<String> matchingUids = new HashSet<>();

        for ( CategoryCombo cc : inputDataElementCategoryCombos )
        {
            if ( cc.getOptionCombos().stream()
                .allMatch( coc -> sortedOptionsMap.containsKey( getSortedOptions( coc ) ) ) )
            {
                matchingUids.add( cc.getUid() );
            }
        }

        return matchingUids;
    }
}
