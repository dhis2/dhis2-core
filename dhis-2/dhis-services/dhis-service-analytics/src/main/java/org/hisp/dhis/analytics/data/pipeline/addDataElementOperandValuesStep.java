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

import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElementOperand;

import com.google.common.collect.Lists;

/**
 * Adds data element operand values to the given grid based on the given data
 * query parameters.
 *
 */
public class addDataElementOperandValuesStep
    extends
    BaseStep
{

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {
        if ( !params.getDataElementOperands().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimension( DataDimensionItemType.DATA_ELEMENT_OPERAND ).build();

            for ( DataElementOperand.TotalType type : DataElementOperand.TotalType.values() )
            {
                addDataElementOperandValues( dataSourceParams, grid, type );
            }
        }
    }

    /**
     * Adds data element operand values to the given grid.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     * @param totalType the operand {@link DataElementOperand.TotalType}.
     */
    private void addDataElementOperandValues( DataQueryParams params, Grid grid, DataElementOperand.TotalType totalType )
    {
        List<DataElementOperand> operands = asTypedList( params.getDataElementOperands() );
        operands = operands.stream().filter( o -> totalType.equals( o.getTotalType() ) ).collect( Collectors.toList() );

        if ( operands.isEmpty() )
        {
            return;
        }

        List<DimensionalItemObject> dataElements = Lists.newArrayList( DimensionalObjectUtils.getDataElements( operands ) );
        List<DimensionalItemObject> categoryOptionCombos = Lists.newArrayList( DimensionalObjectUtils.getCategoryOptionCombos( operands ) );
        List<DimensionalItemObject> attributeOptionCobos = Lists.newArrayList( DimensionalObjectUtils.getAttributeOptionCombos( operands ) );

        //TODO Check if data was dim or filter

        DataQueryParams.Builder builder = DataQueryParams.newBuilder( params )
                .removeDimension( DATA_X_DIM_ID )
                .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, dataElements ) );

        if ( totalType.isCategoryOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.CATEGORY_OPTION_COMBO, categoryOptionCombos ) );
        }

        if ( totalType.isAttributeOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( ATTRIBUTEOPTIONCOMBO_DIM_ID, DimensionType.ATTRIBUTE_OPTION_COMBO, attributeOptionCobos ) );
        }

        DataQueryParams operandParams = builder.build();

        Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( operandParams );

        aggregatedDataMap = AnalyticsUtils.convertDxToOperand( aggregatedDataMap, totalType );

        for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
        {
            Object value = AnalyticsUtils.getRoundedValueObject( operandParams, entry.getValue() );

            grid.addRow()
                    .addValues( entry.getKey().split( DIMENSION_SEP ) )
                    .addValue( value );

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( 5 );
            }
        }
    }
}
