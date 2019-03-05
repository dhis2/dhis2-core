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

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;

import java.util.Date;

import static org.hisp.dhis.analytics.DataQueryParams.*;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_HEADER_NAME;

/**
 * Adds headers to the given grid based on the given data query parameters.
 *
 *
 */
public class addHeaderStep
    extends
    BaseStep
{

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {

        if ( !params.isSkipData() && !params.isSkipHeaders() )
        {
            for ( DimensionalObject col : params.getDimensions() )
            {
                grid.addHeader( new GridHeader( col.getDimension(), col.getDisplayName(), ValueType.TEXT,
                    String.class.getName(), false, true ) );
            }

            if ( params.isShowHierarchy() && !params.getOrgUnitLevels().isEmpty() )
            {
                for ( DimensionalObject level : params.getOrgUnitLevelsAsDimensions() )
                {
                    grid.addHeader( new GridHeader( level.getDimension(), level.getDisplayName(), ValueType.TEXT, String.class.getName(), false, true ) );
                }
            }

            if ( params.isIncludePeriodStartEndDates() )
            {
                grid.addHeader( new GridHeader( PERIOD_START_DATE_ID, PERIOD_START_DATE_NAME, ValueType.DATETIME, Date.class.getName(), false, false ) );
                grid.addHeader( new GridHeader( PERIOD_END_DATE_ID, PERIOD_END_DATE_NAME, ValueType.DATETIME, Date.class.getName(), false, false ) );
            }

            grid.addHeader(
                new GridHeader( VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );

            if ( params.isIncludeNumDen() )
            {
                grid.addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                        .addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                        .addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                        .addHeader( new GridHeader( MULTIPLIER_ID, MULTIPLIER_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                        .addHeader( new GridHeader( DIVISOR_ID, DIVISOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );
            }
        }

    }
}
