/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.data.pipeline;

import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.Grid;

import java.util.Map;

/**
 * Adds values to the given grid based on dynamic dimensions from the given data
 * query parameters. This assumes that no fixed dimensions are part of the
 * query.
 *
 */
public class addDynamicDimensionValuesStep
    extends
    BaseStep
{

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {

        if ( params.getDataDimensionAndFilterOptions().isEmpty() && !params.isSkipData() )
        {
            Map<String, Double> aggregatedDataMap = getAggregatedDataValueMap(
                DataQueryParams.newBuilder( params ).withIncludeNumDen( false ).build() );

            fillGridWithAggregatedDataMap( params, grid, aggregatedDataMap );
        }
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping between
     * a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Double> getAggregatedDataValueMap(DataQueryParams params)
    {
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.DATA_VALUE, Lists.newArrayList() ) );
    }
}
