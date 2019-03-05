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

import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.Grid;

import java.util.Map;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;

/**
 * Adds data element values to the given grid based on the given data query
 * parameters.
 *
 */
public class addDataElementValuesStep
    extends
    BaseStep
{

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {
        if ( !params.getAllDataElements().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimension( DataDimensionItemType.DATA_ELEMENT ).withIncludeNumDen( false ).build();

            Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( dataSourceParams );

            for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
            {
                Object value = AnalyticsUtils.getRoundedValueObject( params, entry.getValue() );

                grid.addRow().addValues( entry.getKey().split( DIMENSION_SEP ) ).addValue( value );

                if ( params.isIncludeNumDen() )
                {
                    grid.addNullValues( 5 );
                }
            }
        }
    }


}
