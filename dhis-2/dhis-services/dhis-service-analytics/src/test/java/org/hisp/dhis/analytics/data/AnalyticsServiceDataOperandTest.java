package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.DhisConvenienceTest.createCategoryOptionCombo;
import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.lang.math.RandomUtils;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceDataOperandTest
    extends
    AnalyticsServiceBaseTest
{
    @Test
    public void verifyAnalyticsServiceFiltersOutUnwantedDataElementOperands()
    {
        int timeUnit = 3;

        DataElementOperand dataElementOperand1 = createOperand( 'A', 'B' );
        DataElementOperand dataElementOperand2 = createOperand( 'C', 'D' );
        DataElementOperand dataElementOperand3 = createOperand( 'E', 'F' );
        // these operands are not requested by the query //
        DataElementOperand dataElementOperand4 = createOperand( 'G', 'H' );
        DataElementOperand dataElementOperand5 = createOperand( 'I', 'L' );

        List<DimensionalItemObject> periods = new ArrayList<>();

        // create a list of periods
        Stream.iterate( 1, i -> i + 1 ).limit( timeUnit ).forEach(
            x -> periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, x, 1, 0, 0 ).toDate() ) ) );

        DataQueryParams params = DataQueryParams.newBuilder().withOrganisationUnit( new OrganisationUnit( "aaaa" ) )
            // DATA ELEMENTS
            .withDataElements( newArrayList( dataElementOperand1, dataElementOperand2, dataElementOperand3 ) )
            .withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters( singletonList( new BaseDimensionalObject( "pe", DimensionType.PERIOD, periods ) ) ).build();

        initMock( params );

        // data element operands contains 15 rows (5 data elements operand * 3 periods)
        Map<String, Object> dataElementOperands = buildResponse( periods, dataElementOperand1, dataElementOperand2,
            dataElementOperand3, dataElementOperand4, dataElementOperand5 );

        when( analyticsManager.getAggregatedDataValues( any( DataQueryParams.class ),
            eq( AnalyticsTableType.DATA_VALUE ), eq( 0 ) ) )
                .thenReturn( CompletableFuture.completedFuture( dataElementOperands ) );

        Grid grid = target.getAggregatedDataValues( params );

        // the system has filtered out the dataElementOperands to the data elements
        // operands selected in the DataQueryParams
        assertThat( grid.getRows(), hasSize( 9 ) );
        assertTrue( gridContains( grid, getDeCocKey( dataElementOperand1, '.' ), "201401", "201402", "201403" ) );
        assertTrue( gridContains( grid, getDeCocKey( dataElementOperand2, '.' ), "201401", "201402", "201403" ) );
        assertTrue( gridContains( grid, getDeCocKey( dataElementOperand3, '.' ), "201401", "201402", "201403" ) );
    }

    private boolean gridContains( Grid grid, String key, String... isoPeriods )
    {
        List<List<Object>> rows = grid.getRows();
        for ( List<Object> row : rows )
        {
            for ( String period : isoPeriods )
            {
                if ( row.get( 0 ).equals( key ) && row.get( 1 ).equals( period ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    private DataElementOperand createOperand(char dataElementChar, char cocChar )
    {
        DataElement de = createDataElement( dataElementChar );
        CategoryOptionCombo coc = createCategoryOptionCombo( cocChar );
        return new DataElementOperand( de, coc );
    }

    private String getDataElementUid( DataElementOperand dataElementOperand )
    {
        return dataElementOperand.getDataElement().getUid();
    }

    private String getCoCUid( DataElementOperand dataElementOperand )
    {
        return dataElementOperand.getCategoryOptionCombo().getUid();
    }

    private String getDeCocKey( DataElementOperand dataElementOperand, char separator )
    {
        return getDataElementUid( dataElementOperand ) + separator + getCoCUid( dataElementOperand );
    }

    private Map<String, Object> buildResponse( List<DimensionalItemObject> periods, DataElementOperand... operands )
    {
        Map<String, Object> result = new HashMap<>();
        for ( DataElementOperand operand : operands )
        {
            for ( DimensionalItemObject period : periods )
            {
                Period p = (Period) period;
                result.put( getDeCocKey( operand, '-' ) + "-" + p.getIsoDate(), RandomUtils.nextLong() );
            }
        }

        return result;
    }
}
