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
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.Grid;

import java.util.Map;

/**
 * Adds validation results to the given grid based on the given data query
 * parameters.
 */
public class addValidationResultValuesStep
    extends
    BaseStep
{
    @Override
    public void execute( DataQueryParams params, Grid grid )
    {
        if ( !params.getAllValidationResults().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                    .retainDataDimension( DataDimensionItemType.VALIDATION_RULE )
                    .withAggregationType( AnalyticsAggregationType.COUNT )
                    .withIncludeNumDen( false ).build();

            Map<String, Double> aggregatedDataMap = getAggregatedValidationResultMapObjectTyped( dataSourceParams );

            fillGridWithAggregatedDataMap(params, grid, aggregatedDataMap);
        }
    }

    /**
     * Generates a mapping between the count of a validation result.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between validation results and counts of them
     */
    private Map<String, Double> getAggregatedValidationResultMapObjectTyped( DataQueryParams params )
    {
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.VALIDATION_RESULT, Lists.newArrayList() ) );
    }
}
