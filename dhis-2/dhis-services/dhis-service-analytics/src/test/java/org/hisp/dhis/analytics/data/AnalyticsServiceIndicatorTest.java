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
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;

import java.util.Collections;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CyclicDependencyInDimensionItemsException;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceIndicatorTest extends DhisSpringTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private IndicatorService indicatorService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void verifyIndicatorCyclicDependencyIsDetected() {

        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorH}" );
        Indicator indicatorG = createIndicator( 'G', indicatorTypeB, "20" );
        Indicator indicatorH = createIndicator( 'H', indicatorTypeB, "N{mindicatorF}" );
        Indicator indicatorI = createIndicator( 'I', indicatorTypeB, "N{mindicatorF}*N{mindicatorG}-N{mindicatorH}" );

        DataQueryParams params = DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
            // DATA ELEMENTS
            .withDataElements( newArrayList( createDataElement( 'A', new CategoryCombo() ), indicatorI ) ).withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters( Collections.singletonList(
                    new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                            ImmutableList.of(
                                    new OrganisationUnit( "aaa", "aaa", "OU_1", null, null, "c1" ),
                                    new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" )
                            )
                    ) )
            )
            .build();

        thrown.expect(CyclicDependencyInDimensionItemsException.class);
        thrown.expectMessage("Item with uid mindicatorF has a cyclic connection with another item");
        this.analyticsService.getAggregatedDataValues( params );
    }

    private Indicator createIndicator( char uniqueCharacter, IndicatorType type, String numerator )
    {
        Indicator indicator = createIndicator( uniqueCharacter, type );

        indicator.setUid( "mindicator" + uniqueCharacter );
        indicator.setNumerator( numerator );
        indicator.setDenominator( "1" );

        indicatorService.addIndicator( indicator );
        return indicator;

    }
}
