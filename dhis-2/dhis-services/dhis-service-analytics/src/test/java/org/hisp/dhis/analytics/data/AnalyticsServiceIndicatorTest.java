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
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.*;
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

    @Autowired
    private CategoryService categoryService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final static String ERROR_STRING = "Item of type INDICATOR with identifier '%s' has a cyclic reference to another item";

    private DataElement dataElementA;
    private CategoryOption coA;
    private CategoryOption coB;
    private CategoryOption coC;
    private CategoryOption coD;
    private CategoryOption coE;
    private CategoryOption coF;
    private CategoryOption coG;

    private Category cA;
    private Category cB;
    private Category cC;

    private CategoryCombo ccA;
    private CategoryCombo ccB;

    private CategoryOptionCombo cocA;
    private CategoryOptionCombo cocB;
    private CategoryOptionGroup cogA;

    @Before
    public void setUp()
    {
        dataElementA = createDataElement( 'A' );
        dataElementA.setAggregationType( AggregationType.SUM );
        dataElementA.setDomainType( DataElementDomain.AGGREGATE );
        dataElementA.setName( "DeA" );
        dataElementService.addDataElement( dataElementA );

        // Category options //
        coA = new CategoryOption( "male" );
        coB = new CategoryOption( "female" );
        coC = new CategoryOption( "neutral" );

        coD = new CategoryOption( "blue" );
        coE = new CategoryOption( "brown" );
        coF = new CategoryOption( "green" );

        coG = new CategoryOption( "older than 5" );

        saveCategoryOptions( coA, coB, coC, coD, coE, coF, coG );

        // Categories -> group of Category options //
        cA = createCategory( 'A' );
        cA.setCategoryOptions( Lists.newArrayList( coA, coB, coC ) );
        categoryService.addCategory( cA );

        cB = createCategory( 'B' );
        cA.setCategoryOptions( Lists.newArrayList( coD, coE, coF ) );
        categoryService.addCategory( cB );

        cC = createCategory( 'C' );
        cA.setCategoryOptions( Lists.newArrayList( coG ) );
        categoryService.addCategory( cC );

        // Category Combination -> aggregation of categories //

        ccA = createCategoryCombo( 'A', cA, cB );
        categoryService.addCategoryCombo( ccA );

        ccB = createCategoryCombo( 'B', cB, cC );
        categoryService.addCategoryCombo( ccB );

        // Category Option Combo -> combination of all possible Category Options in a
        // Category Combination
        cocA = createCategoryOptionCombo( 'A' );
        cocA.setCategoryCombo( ccA );

        cocB = createCategoryOptionCombo( 'B' );
        cocB.setCategoryCombo( ccB );

        ccA.getOptionCombos().add( cocA );
        ccA.getOptionCombos().add( cocB );

        categoryService.addCategoryOptionCombo( cocA );
        categoryService.addCategoryOptionCombo( cocB );

        cogA = createCategoryOptionGroup( 'A', coA, coB, coC, coD, coE, coF );
        categoryService.saveCategoryOptionGroup( cogA );

        dataElementA.setCategoryCombo( ccA );
    }

    private void saveCategoryOptions( CategoryOption... cos )
    {
        for ( CategoryOption categoryOption : cos )
        {
            categoryService.addCategoryOption( categoryOption );
        }
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
        createIndicator( 'I', indicatorTypeB, "#{dataelemena}/3" );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
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
        createIndicator( 'G', indicatorTypeB, "#{dataelemena}/6" );
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
        createIndicator( 'I', indicatorTypeB, "#{dataelemena}/2" );
        createIndicator( 'L', indicatorTypeB, "#{dataelemena}/4" );
        createIndicator( 'M', indicatorTypeB, "N{mindicatorG}" );

        thrown.expect( CyclicReferenceException.class );
        thrown.expectMessage( String.format(ERROR_STRING, "mindicatorG") );

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
        createIndicator( 'I', indicatorTypeB, "#{dataelemena}/2" );
        createIndicator( 'L', indicatorTypeB, "#{dataelemena}/4" );

        createIndicator( 'M', indicatorTypeB, "#{dataelemena}/6" );

        Grid grid = this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
        assertNotNull( grid );
    }


    @Test
    public void case1_COCUID_as_second_elem_works()
    {
        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, createIndicatorExp(dataElementA, cocA, cocB) );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
    }

    @Test
    public void case1_COGUID_as_second_elem_works()
    {

        IndicatorType indicatorTypeB = createIndicatorType( 'B' );
        indicatorService.addIndicatorType( indicatorTypeB );

        Indicator indicatorF = createIndicator( 'F', indicatorTypeB, createIndicatorExp(dataElementA, cogA, cocA)  );

        this.analyticsService.getAggregatedDataValues( createParamsWithRootIndicator( indicatorF ) );
    }

    private String createIndicatorExp( DataElement dataElement, CategoryOptionCombo categoryOptionCombo,
                                       CategoryOptionCombo attributeOptionCombo )
    {
        return String.format("#{%s.%s.%s}", dataElement.getUid(), categoryOptionCombo.getUid(), attributeOptionCombo.getUid());
    }

    private String createIndicatorExp( DataElement dataElement, CategoryOptionGroup categoryOptionGroup,
                                       CategoryOptionCombo attributeOptionCombo )
    {
        return String.format("#{%s.%s.%s}", dataElement.getUid(), categoryOptionGroup.getUid(), attributeOptionCombo.getUid());
    }

    private DataQueryParams createParamsWithRootIndicator( Indicator indicator )
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
