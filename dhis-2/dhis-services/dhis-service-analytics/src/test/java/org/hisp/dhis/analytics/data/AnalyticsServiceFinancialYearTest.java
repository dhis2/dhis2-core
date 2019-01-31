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

package org.hisp.dhis.analytics.data;

import static com.google.common.collect.Lists.newArrayList;
import static org.hisp.dhis.DhisConvenienceTest.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.common.IdentifiableObjectUtils.SEPARATOR;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Luciano Fiandesio
 */
@RunWith(Parameterized.class)
public class AnalyticsServiceFinancialYearTest extends AnalyticsServiceBaseTest {

    @Parameterized.Parameters( name = "{index}: financial year iso: ({0}) - denominator value: ({1})" )
    public static Iterable<Object[]> data()
    {
        return Arrays.asList( new Object[][] { { "2017April", 115.8D }, { "2017July", 77.5D }, { "2017Oct", 39.3D } } );
    }

    private String financialPeriodIso;

    private Double calculateDenominator;

    public AnalyticsServiceFinancialYearTest( String financialPeriodIso, Double calculateDenominator )
    {
        this.financialPeriodIso = financialPeriodIso;
        this.calculateDenominator = calculateDenominator;
    }

    @Test
    public void verifyWeightedAverageCalculationOfDenominatorWhenFinancialYearIsPeriod()
    {
        String UID_AVG = "h0xKKjijTdI";
        String UID_SUM = "fbfJHSPpUQD";
        String nextFinancialPeriodIso = plusOneYear( financialPeriodIso );
        DataElement deL = createDataElement( 'L', ValueType.INTEGER, AggregationType.AVERAGE );
        deL.setUid( UID_AVG );
        DataElement deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deA.setUid( UID_SUM );
        OrganisationUnit ouA = createOrganisationUnit( 'A' );

        DataQueryParams params = DataQueryParams.newBuilder()
                .withPeriods( getList( createPeriod( PeriodType.getPeriodTypeFromIsoString( financialPeriodIso ),
                        getDate( 2017, 10, 1 ), getDate( 2018, 9, 30 ) ),
                        createPeriod( PeriodType.getPeriodTypeFromIsoString( nextFinancialPeriodIso ),
                                getDate( 2018, 10, 1 ), getDate( 2019, 9, 30 ) )) )
                .withDataElements( getList( deA, deL ) )
                .withOrganisationUnits( getList( ouA ) )
                .withIgnoreLimit( true )
                .withSkipHeaders( true )
                .withSkipMeta( true )
                .build();
        initMock(params);

        // no need to return any meaningful DataQueryGroup //
        DataQueryParams firstQuery = DataQueryParams.newBuilder().build();
        DataQueryGroups dataQueryGroups = DataQueryGroups.newBuilder().withQueries( newArrayList( firstQuery ) )
            .build();
        when( queryPlanner.planQuery( any( DataQueryParams.class ), any( QueryPlannerParams.class ) ) )
            .thenReturn( dataQueryGroups );
        Map<String, Object> m = new HashMap<>();
        m.put( UID_SUM + SEPARATOR + financialPeriodIso, 1275D );
        // this two last values should be "compressed" into one, by the
        // weighted average algo for financial years
        m.put( UID_AVG + SEPARATOR + financialPeriodIso, 154D );
        m.put( UID_AVG + SEPARATOR + nextFinancialPeriodIso, 1D );
        when( analyticsManager.getAggregatedDataValues( firstQuery, AnalyticsTableType.DATA_VALUE, 0 ) )
            .thenReturn( CompletableFuture.completedFuture( m ) );
        Grid grid = target.getAggregatedDataValues( params );
        GridAsserter.addGrid( grid )
            .rowsHasSize( 2 )
            .rowWithUidContains( UID_AVG, financialPeriodIso, calculateDenominator )
            .verify();
    }

    private String plusOneYear(String financialPeriodIso) {

        return Integer.valueOf(financialPeriodIso.substring(0,4)) + 1 + financialPeriodIso.substring(4);
    }
}
