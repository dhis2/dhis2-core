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

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.QueryModifiers;

/**
 * @author Jim Grace
 */
public class PredictionDataFilter
{
    private PredictionDataFilter()
    {
        throw new UnsupportedOperationException( "util" );
    }

    /**
     * Filters prediction data by minDate/maxDate if specified.
     *
     * @param data data to be filtered
     * @return data that passes minDate/maxDate checks
     */
    public static PredictionData filter( PredictionData data )
    {
        return (data == null)
            ? null
            : new PredictionData( data.getOrgUnit(), filterValues( data.getValues() ), data.getOldPredictions() );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private static List<FoundDimensionItemValue> filterValues( List<FoundDimensionItemValue> values )
    {
        return values.stream()
            .filter( PredictionDataFilter::isValid )
            .collect( toList() );
    }

    private static boolean isValid( FoundDimensionItemValue item )
    {
        QueryModifiers mods = item.getDimensionalItemObject().getQueryMods();

        return mods == null
            || (mods.getMinDate() == null || !mods.getMinDate().after( item.getPeriod().getStartDate() ))
                && (mods.getMaxDate() == null || !mods.getMaxDate().before( item.getPeriod().getEndDate() ));
    }
}
