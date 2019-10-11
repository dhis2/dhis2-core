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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_ORGUNIT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CyclicReferenceException;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsServiceIndicatorTest
    extends DhisSpringTest
{
    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private DataElementService dataElementService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static String ERROR_STRING = "Item of type INDICATOR with identifier '%s' has a cyclic reference to another item";

    @Before
    public void setUp()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setUid( "dataElemenA" );
        dataElementA.setAggregationType( AggregationType.SUM );
        dataElementA.setDomainType( DataElementDomain.AGGREGATE );
        dataElementA.setName( "DeA" );
        dataElementService.addDataElement( dataElementA );
    }

    /**
     * IndicatorF -> IndicatorG -> IndicatorH -> IndicatorI
     *
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsNotTriggered()
    {
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorG}" );
        createIndicator( 'G', indicatorTypeB, "N{mindicatorH}/6" );
        createIndicator( 'H', indicatorTypeB, "N{mindicatorI}+2" );
        createIndicator( 'I', indicatorTypeB, "#{dataElemenA}/3" );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
    }

    /**
     *              IndicatorF
     *                   |
     *                   |
     *                   v
     *              IndicatorG
     *                 + +  +
     *                 | |  |
     * IndicatorH<-----+ |  +----->IndicatorL
     *    +   +          |
     *    |   |          v
     *    |   +------>IndicatorI
     *    |
     *    |
     *    |
     *    +------>IndicatorM
     *
     *
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsNotTriggered2()
    {
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorG}" );
        createIndicator( 'G', indicatorTypeB, "N{mindicatorH}*N{mindicatorI}-N{mindicatorL}" );

        createIndicator( 'H', indicatorTypeB, "N{mindicatorI}*N{indicatorM}" );
        createIndicator( 'I', indicatorTypeB, "#{dataElemenA}/2" );
        createIndicator( 'L', indicatorTypeB, "#{dataElemenA}/4" );

        createIndicator( 'M', indicatorTypeB, "#{dataElemenA}/6" );

        Grid grid = this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
        assertNotNull( grid );
    }

    /**
     *  IndicatorY ----> dataElementA
     *
     *  IndicatorF ----> IndicatorY
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsNotTriggered3()
    {
        // Given
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        IndicatorType indicatorTypeY = createIndicatorType( 'Y' );
        indicatorService.addIndicatorType( indicatorTypeB );
        indicatorService.addIndicatorType( indicatorTypeY );
        Indicator indicatorY = createIndicator( 'Y', indicatorTypeY, "#{dataElemenA}+1" );
        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorY}" );

        // When
        Grid grid = this.analyticsService
                .getAggregatedDataValues( createParamsWithRootIndicator( indicatorF, indicatorY ) );

        // Then
        assertThat( grid, is( not ( nullValue() )));
    }

    /**
     *  IndicatorY ----> dataElementA
     *
     *  IndicatorF ----> IndicatorY
     *
     *  IndicatorW ----> IndicatorY
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsNotTriggered4()
    {
        // Given
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        IndicatorType indicatorTypeY = createIndicatorType( 'Y' );
        indicatorService.addIndicatorType( indicatorTypeB );
        indicatorService.addIndicatorType( indicatorTypeY );

        Indicator indicatorY = createIndicator( 'Y', indicatorTypeY, "#{dataElemenA}+1" );
        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorY}" );
        Indicator indicatorW = createIndicator( 'W', indicatorTypeB, "N{mindicatorY}" );

        // When
        Grid grid = this.analyticsService
                .getAggregatedDataValues( createParamsWithRootIndicator( indicatorF, indicatorY, indicatorW ) );

        // Then
        assertThat( grid, is( not ( nullValue() )));
    }

    /**
     *  IndicatorF ----> IndicatorY
     *
     *  IndicatorW ----> IndicatorY
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsNotTriggered5()
    {
        // Given
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorY}" );
        Indicator indicatorW = createIndicator( 'W', indicatorTypeB, "N{mindicatorY}" );

        // When
        Grid grid = this.analyticsService
            .getAggregatedDataValues( createParamsWithRootIndicator( indicatorF, indicatorW ) );

        // Then
        assertThat( grid, is( not ( nullValue() )));
    }

    /**
     *       +------>IndicatorF
     *      |            |
     *      |            |
     *      |            v
     *      |       IndicatorG
     *      |          +
     *      +          |
     * IndicatorH<-----+
     *
     *
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsDetected()
    {
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorH}" );
        createIndicator( 'G', indicatorTypeB, "#{dataElemenA}/6" );
        createIndicator( 'H', indicatorTypeB, "N{mindicatorF}" );

        thrown.expect( CyclicReferenceException.class );
        thrown.expectMessage( String.format(ERROR_STRING, "mindicatorF") );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
    }

    /**
     *              IndicatorF
     *                   |
     *                   |
     *                   v
     *              IndicatorG<---------------------+
     *                 + +  +                       |
     *                 | |  |                       |
     * IndicatorH<-----+ |  +----->IndicatorL       |
     *    +              |                          |
     *    |              v                          |
     *    |           IndicatorI                    |
     *    |                                         |
     *    |                                         |
     *    |                                         |
     *    +------>IndicatorM+-----------------------+
     *
     *
     *
     *
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsDetected2()
    {
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorG}" );
        createIndicator( 'G', indicatorTypeB, "N{mindicatorH}*N{mindicatorI}-N{mindicatorL}" );
        createIndicator( 'H', indicatorTypeB, "N{mindicatorM}" );
        createIndicator( 'I', indicatorTypeB, "#{dataElemenA}/2" );
        createIndicator( 'L', indicatorTypeB, "#{dataElemenA}/4" );
        createIndicator( 'M', indicatorTypeB, "N{mindicatorG}" );

        thrown.expect( CyclicReferenceException.class );
        thrown.expectMessage( String.format(ERROR_STRING, "mindicatorG") );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
    }

    /**
     * IndicatorF <-------> IndicatorW
     */
    @Test
    public void verifyIndicatorCyclicDependencyIsDetected3()
    {
        // Given
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, "N{mindicatorF}" );
        Indicator indicatorW = createIndicator( 'W', indicatorTypeB, "#{dataElemenA}",
                "N{mindicatorW}" );

        // Then
        thrown.expect( CyclicReferenceException.class );
        thrown.expectMessage( String.format( ERROR_STRING, "mindicatorF" ) );

        // When
        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF, indicatorW ) );
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

    private Indicator createIndicator( char uniqueCharacter, IndicatorType type, String numerator, String denominator)
    {
        Indicator indicator = createIndicator(uniqueCharacter, type, numerator);
        indicator.setDenominator(denominator);

        return indicator;
    }

    private DataQueryParams createParamsWithRootIndicator( Indicator... indicator )
    {
        return DataQueryParams.newBuilder()
            // PERIOD
            .withPeriod( new Period( YearlyPeriodType.getPeriodFromIsoString( "2017W10" ) ) )
            // INDICATOR
            .withIndicators( newArrayList( indicator ) )
            .withDataElements(newArrayList( createDataElement( 'A', new CategoryCombo() ) ))
            .withIgnoreLimit( true )
            // FILTERS (OU)
            .withFilters( Collections.singletonList(
                new BaseDimensionalObject( "ou", DimensionType.ORGANISATION_UNIT, null, DISPLAY_NAME_ORGUNIT,
                    ImmutableList.of( new OrganisationUnit( "bbb", "bbb", "OU_2", null, null, "c2" ) ) ) ) )
            .build();
    }
}
