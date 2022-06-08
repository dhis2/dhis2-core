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

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;

/**
 * Generator of a map that, for each category combo (CC) found in an input data
 * element, returns a map from the category option combination disaggregations
 * of that CC to the disaggregations of the output category combo.
 *
 * @author Jim Grace
 */
public class CategoryComboDisaggregationMapGenerator
{
    private CategoryComboDisaggregationMapGenerator()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * Returns a map that, for each input category combo (CC), returns a map
     * from the category option combinations of that CC to the category option
     * combinations of the output category combo.
     * <p>
     * The map has entries only for those CCs where all the COCs can be mapped
     * to output COCs.
     *
     * @param outputCombo predictor output data element category combination.
     * @param inputCategoryCombos data element cat combos found in the input.
     * @return the map.
     */
    public static CategoryComboDisaggregationMap getCcDisagMap( CategoryCombo outputCombo,
        Set<CategoryCombo> inputCategoryCombos )
    {
        Map<String, String> optionMap = getOptionMap( outputCombo );

        CategoryComboDisaggregationMap ccDisagMap = new CategoryComboDisaggregationMap();

        for ( CategoryCombo categoryCombo : inputCategoryCombos )
        {
            if ( categoryCombo.getCategories().size() == outputCombo.getCategories().size() )
            {
                DisaggregationMap disagMap = getDisagMap( optionMap, categoryCombo );

                if ( disagMap != null )
                {
                    ccDisagMap.put( categoryCombo.getUid(), disagMap );
                }
            }
        }

        return ccDisagMap;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * For one input category combo, returns a map from its category option
     * combos to the output category option combos. If there is not a match for
     * every input category option combo, returns null.
     */
    private static DisaggregationMap getDisagMap( Map<String, String> optionMap,
        CategoryCombo inputCc )
    {
        DisaggregationMap disagMap = new DisaggregationMap();

        for ( CategoryOptionCombo inputCoc : inputCc.getOptionCombos() )
        {
            String outputCocUid = optionMap.get( getOptionString( inputCoc ) );

            if ( outputCocUid == null )
            {
                return null;
            }

            disagMap.put( inputCoc.getUid(), outputCocUid );
        }

        return disagMap;
    }

    /**
     * Returns a map from a String representation of the options of a category
     * combo to that category option combo.
     */
    private static Map<String, String> getOptionMap( CategoryCombo outputCombo )
    {
        return outputCombo.getOptionCombos().stream()
            .collect( toMap( CategoryComboDisaggregationMapGenerator::getOptionString, CategoryOptionCombo::getUid ) );
    }

    /**
     * Returns a String representing all the category options in a category
     * option combo by concatenating all the category option UIDs, sorted.
     */
    private static String getOptionString( CategoryOptionCombo coc )
    {
        return coc.getCategoryOptions().stream()
            .map( CategoryOption::getUid )
            .sorted()
            .collect( joining() );
    }
}
