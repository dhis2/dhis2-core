/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.ENROLLMENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.EVENT;
import static org.hisp.dhis.analytics.AnalyticsTableType.ORG_UNIT_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.VALIDATION_RESULT;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests indicator subExpressions.
 *
 * @author Jim Grace
 */
class AnalyticsServiceSubExpressionTest
    extends IntegrationTestBase
{
    @Autowired
    private List<AnalyticsTableService> analyticsTableServices;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    private Period perA;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private Indicator indicatorA;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
        throws IOException,
        InterruptedException
    {
        perA = createPeriod( "2022-01" );
        periodService.addPeriod( perA );
        perA = periodService.reloadPeriod( perA );

        DataElement deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        DataElement deB = createDataElement( 'B', ValueType.TEXT, AggregationType.NONE );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouA );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );

        CategoryOption optionA = new CategoryOption( "CategoryOptionA" );
        CategoryOption optionB = new CategoryOption( "CategoryOptionB" );
        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );

        Category categoryA = createCategory( 'A', optionA, optionB );
        categoryService.addCategory( categoryA );

        CategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addCategoryCombo( categoryComboA );

        CategoryOptionCombo cocA = createCategoryOptionCombo( categoryComboA, optionA );
        CategoryOptionCombo cocB = createCategoryOptionCombo( categoryComboA, optionB );
        cocA.setUid( "OptionCombA" );
        cocB.setUid( "OptionCombB" );
        categoryService.addCategoryOptionCombo( cocA );
        categoryService.addCategoryOptionCombo( cocB );
        CategoryOptionCombo aocA = categoryService.getDefaultCategoryOptionCombo();

        categoryComboA.getOptionCombos().add( cocA );
        categoryComboA.getOptionCombos().add( cocB );
        categoryService.updateCategoryCombo( categoryComboA );

        IndicatorType indicatorTypeA = createIndicatorType( 'A' );
        indicatorTypeA.setFactor( 1 );
        indicatorService.addIndicatorType( indicatorTypeA );

        indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorA.setNumerator( "1" ); // to be overwritten
        indicatorA.setDenominator( "1" );
        indicatorService.addIndicator( indicatorA );

        BatchHandler<DataValue> handler = batchHandlerFactory.createBatchHandler(
            DataValueBatchHandler.class ).init();
        handler.addObject( newDataValue( deA, perA, ouB, cocA, aocA, "1" ) );
        handler.addObject( newDataValue( deA, perA, ouC, cocB, aocA, "2" ) );
        handler.addObject( newDataValue( deB, perA, ouB, cocA, aocA, "B" ) );
        handler.addObject( newDataValue( deB, perA, ouC, cocB, aocA, "C" ) );
        handler.flush();

        // Wait before generating analytics tables
        Thread.sleep( 1000 );

        // Generate analytics tables
        analyticsTableGenerator.generateTables( AnalyticsTableUpdateParams.newBuilder().build(),
            NoopJobProgress.INSTANCE );

        // Wait after generating analytics tables
        Thread.sleep( 1000 );
    }

    @Override
    public void tearDownTest()
    {
        for ( AnalyticsTableService service : analyticsTableServices )
        {
            service.dropTables();
        }
    }

    // -------------------------------------------------------------------------
    // Test
    // -------------------------------------------------------------------------

    @Test
    void testSubExpressions()
    {
        // Simple

        List<String> expectedSimple = List.of(
            "inabcdefghA-ouabcdefghA-9.0",
            "inabcdefghA-ouabcdefghB-4.0",
            "inabcdefghA-ouabcdefghC-5.0" );

        List<String> resultSimple = query( "subExpression(if(#{deabcdefghA}==1,4,5))", ouA, ouB, ouC );

        assertEquals( expectedSimple, resultSimple );

        // Multiple references to the same item within the subexpression

        List<String> expectedMultiple = List.of(
            "inabcdefghA-ouabcdefghA-2.0",
            "inabcdefghA-ouabcdefghB-0.0",
            "inabcdefghA-ouabcdefghC-2.0" );

        List<String> resultMultiple = query( "subExpression(if(#{deabcdefghA}<2,0,#{deabcdefghA}))", ouA, ouB, ouC );

        assertEquals( expectedMultiple, resultMultiple );

        // Text converted to numeric

        List<String> expectedFromText = List.of(
            "inabcdefghA-ouabcdefghB-3.0",
            "inabcdefghA-ouabcdefghC-4.0" );

        List<String> resultFromText = query( "subExpression(if(#{deabcdefghB}=='B',3,4))", ouB, ouC );

        assertEquals( expectedFromText, resultFromText );

        // References both inside and outside of the subexpression

        List<String> expectedInOut = List.of(
            "inabcdefghA-ouabcdefghA-32.0",
            "inabcdefghA-ouabcdefghB-10.0",
            "inabcdefghA-ouabcdefghC-22.0" );

        List<String> resultInOut = query( "10 * #{deabcdefghA} + subExpression(if(#{deabcdefghA}<2,0,#{deabcdefghA}))",
            ouA, ouB, ouC );

        assertEquals( expectedInOut, resultInOut );

        // Two subexpressions

        List<String> expectedTwo = List.of(
            "inabcdefghA-ouabcdefghA-21.0",
            "inabcdefghA-ouabcdefghB-10.0",
            "inabcdefghA-ouabcdefghC-11.0" );

        List<String> resultTwo = query(
            "subExpression(if(#{deabcdefghA}==1,3,5)) + subExpression(if(#{deabcdefghA}==2,6,7))",
            ouA, ouB, ouC );

        assertEquals( expectedTwo, resultTwo );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates a data value. Sets the last updated time to something in the past
     * because at the time of this writing analytics won't include the value if
     * it was last updated less than a second ago.
     */
    private DataValue newDataValue( DataElement de, Period pe, OrganisationUnit ou,
        CategoryOptionCombo coc, CategoryOptionCombo aoc, String value )
    {
        return new DataValue( de, pe, ou, coc, aoc, value, null, parseDate( "2022-01-01" ), null );
    }

    /**
     * Queries analytics with an indicator expression.
     */
    private List<String> query( String expression, OrganisationUnit... ous )
    {
        indicatorA.setNumerator( expression );
        indicatorService.updateIndicator( indicatorA );

        DataQueryParams params = DataQueryParams.newBuilder()
            .withIndicators( List.of( indicatorA ) )
            .withOrganisationUnits( List.of( ous ) )
            .withFilterPeriod( perA )
            .withOutputFormat( OutputFormat.ANALYTICS )
            .build();

        Map<String, Object> map = analyticsService.getAggregatedDataValueMapping( params );

        return map.entrySet().stream()
            .map( e -> e.getKey() + '-' + e.getValue() )
            .sorted().collect( Collectors.toList() );
    }
}
