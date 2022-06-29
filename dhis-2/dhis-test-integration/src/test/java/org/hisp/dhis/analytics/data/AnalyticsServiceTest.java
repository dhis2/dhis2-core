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

import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.AggregationType.COUNT;
import static org.hisp.dhis.analytics.AggregationType.NONE;
import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.expression.Operator.equal_to;
import static org.hisp.dhis.utils.Assertions.assertMapEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.AnalyticsTestUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.system.util.CsvUtils;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;

/**
 * Tests aggregation of data in analytics tables.
 *
 * @author Henning Haakonsen (original)
 * @author Jim Grace (break cases into indidual tests)
 */
class AnalyticsServiceTest
    extends SingleSetupIntegrationTestBase
{
    private CategoryOptionCombo ocDef;

    private Category catDef;

    private Period peJan;

    private Period peFeb;

    private Period peMar;

    private Period peApril;

    private Period peMay;

    private Period peJune;

    private Period peJuly;

    private Period quarter;

    private Period year;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private DataElement deF;

    private DataElement deG;

    private DataElement deH;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    private OrganisationUnit ouE;

    private DataSet dataSetA;

    private DataSet dataSetB;

    private Indicator indicatorA;

    private ReportingRate reportingRateA;

    private ReportingRate reportingRateB;

    private ValidationRule validationRuleA;

    private ValidationRule validationRuleB;

    @Autowired
    private List<AnalyticsTableService> analyticsTableServices;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private ValidationRuleService validationRuleService;

    @Autowired
    private ValidationResultService validationResultService;

    @Autowired
    private CompleteDataSetRegistrationService completeDataSetRegistrationService;

    @Autowired
    @Qualifier( "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

    // --------------------------------------------------------------------------
    // Set up for all tests
    // --------------------------------------------------------------------------

    // Database (value, data element, period)
    // --------------------------------------------------------------------
    //
    // A: 2, deA, peJan - 4, deB, peFeb - 6, deC, peMar - 8, deD, peApril
    // 100, deB, peJan - 2, deD, peFeb
    //
    // B: 1, deA, peJan - 3, deB, peFeb - 5, deC, peMar - 7, deD, peApril
    //
    // C: 5, deA, peJan - 10, deB, peFeb - 15, deC, peMar - 20, deD, peApril
    // 4, deD, peJan - 23, deC, peFeb
    //
    // D: 66, deA, peJan - 233, deA, peFeb - 399, deB, peFeb
    //
    // E: 1, deA, peJan - 1, deB, peFeb - 1, deC, peMar - 1, deD, peApril
    // 32, deD, peJan
    //
    // --------------------------------------------------------------------
    @Override
    public void setUpTest()
        throws IOException,
        InterruptedException
    {
        setUpMetadata();
        setUpDataValues();
        setUpValidation();

        // We need to make sure that table generation start time is greater than
        // lastUpdated on tables populated in the setup
        Date oneSecondFromNow = Date
            .from( LocalDateTime.now().plusSeconds( 1 ).atZone( ZoneId.systemDefault() ).toInstant() );

        // Generate analytics tables
        analyticsTableGenerator.generateTables( AnalyticsTableUpdateParams.newBuilder()
            .withStartTime( oneSecondFromNow )
            .build(),
            NoopJobProgress.INSTANCE );
    }

    private void setUpMetadata()
    {
        ocDef = categoryService.getDefaultCategoryOptionCombo();
        ocDef.setUid( "o1234578def" );

        categoryService.updateCategoryOptionCombo( ocDef );
        catDef = categoryService.getDefaultCategory();
        catDef.setUid( "cat12345def" );
        categoryService.updateCategory( catDef );

        peJan = createPeriod( "2017-01" );
        peFeb = createPeriod( "2017-02" );
        peMar = createPeriod( "2017-03" );
        peApril = createPeriod( "2017-04" );
        peMay = createPeriod( "2017-05" );
        peJune = createPeriod( "2017-06" );
        peJuly = createPeriod( "2017-07" );

        // These periods don't need to be persisted:
        quarter = createPeriod( "2017Q1" );
        year = createPeriod( "2017" );

        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );
        periodService.addPeriod( peApril );
        periodService.addPeriod( peMay );
        periodService.addPeriod( peJune );
        periodService.addPeriod( peJuly );

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );
        deD = createDataElement( 'D' );
        deE = createDataElement( 'E', INTEGER, SUM );
        deF = createDataElement( 'F', BOOLEAN, COUNT );
        deG = createDataElement( 'G', TEXT, NONE );
        deH = createDataElement( 'H', DATE, NONE );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );
        dataElementService.addDataElement( deF );
        dataElementService.addDataElement( deG );
        dataElementService.addDataElement( deH );

        ouA = createOrganisationUnit( 'A' );

        ouB = createOrganisationUnit( 'B' );

        ouC = createOrganisationUnit( 'C' );
        ouC.setOpeningDate( getDate( 2016, 4, 10 ) );
        ouC.setClosedDate( null );

        ouD = createOrganisationUnit( 'D' );
        ouD.setOpeningDate( getDate( 2016, 12, 10 ) );
        ouD.setClosedDate( null );

        ouE = createOrganisationUnit( 'E' );

        AnalyticsTestUtils.configureHierarchy( ouA, ouB, ouC, ouD, ouE );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        OrganisationUnitGroup organisationUnitGroupA = createOrganisationUnitGroup( 'A' );
        organisationUnitGroupA.setUid( "a2345groupA" );
        organisationUnitGroupA.addOrganisationUnit( ouA );
        organisationUnitGroupA.addOrganisationUnit( ouB );

        OrganisationUnitGroup organisationUnitGroupB = createOrganisationUnitGroup( 'B' );
        organisationUnitGroupB.setUid( "a2345groupB" );
        organisationUnitGroupB.addOrganisationUnit( ouC );
        organisationUnitGroupB.addOrganisationUnit( ouD );
        organisationUnitGroupB.addOrganisationUnit( ouE );

        OrganisationUnitGroup organisationUnitGroupC = createOrganisationUnitGroup( 'C' );
        organisationUnitGroupC.setUid( "a2345groupC" );
        organisationUnitGroupC.addOrganisationUnit( ouA );
        organisationUnitGroupC.addOrganisationUnit( ouB );
        organisationUnitGroupC.addOrganisationUnit( ouC );

        OrganisationUnitGroup organisationUnitGroupD = createOrganisationUnitGroup( 'D' );
        organisationUnitGroupD.setUid( "a2345groupD" );
        organisationUnitGroupD.addOrganisationUnit( ouD );
        organisationUnitGroupD.addOrganisationUnit( ouE );

        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupA );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupB );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupC );
        organisationUnitGroupService.addOrganisationUnitGroup( organisationUnitGroupD );

        OrganisationUnitGroupSet organisationUnitGroupSetA = createOrganisationUnitGroupSet( 'A' );
        organisationUnitGroupSetA.setUid( "a234567setA" );
        OrganisationUnitGroupSet organisationUnitGroupSetB = createOrganisationUnitGroupSet( 'B' );
        organisationUnitGroupSetB.setUid( "a234567setB" );

        organisationUnitGroupSetA.getOrganisationUnitGroups().add( organisationUnitGroupA );
        organisationUnitGroupSetA.getOrganisationUnitGroups().add( organisationUnitGroupB );
        organisationUnitGroupSetB.getOrganisationUnitGroups().add( organisationUnitGroupC );
        organisationUnitGroupSetB.getOrganisationUnitGroups().add( organisationUnitGroupD );

        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSetA );
        organisationUnitGroupService.addOrganisationUnitGroupSet( organisationUnitGroupSetB );

        dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );
        dataSetA.addOrganisationUnit( ouC );
        dataSetA.addOrganisationUnit( ouD );

        dataSetB = createDataSet( 'B' );
        dataSetB.setUid( "a23dataSetB" );
        dataSetB.addOrganisationUnit( ouD );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        IndicatorType indicatorTypeA = createIndicatorType( 'A' );
        indicatorTypeA.setFactor( 1 );
        indicatorService.addIndicatorType( indicatorTypeA );

        indicatorA = createIndicator( 'A', indicatorTypeA );
        indicatorA.setUid( "indicatorId" );
        indicatorService.addIndicator( indicatorA );

        reportingRateA = new ReportingRate( dataSetA );
        reportingRateB = new ReportingRate( dataSetB );
    }

    private void setUpDataValues()
        throws IOException
    {
        // Read data values from CSV files
        List<String[]> dataValueLines = CsvUtils.readCsvAsListFromClasspath( "analytics/csv/dataValues.csv", true );
        parseDataValues( dataValueLines );
        List<String[]> dataSetRegistrationLines = CsvUtils.readCsvAsListFromClasspath(
            "analytics/csv/dataSetRegistrations.csv",
            true );
        parseDataSetRegistrations( dataSetRegistrationLines );
    }

    private void setUpValidation()
    {
        CategoryOption optionA = new CategoryOption( "CategoryOptionA" );
        CategoryOption optionB = new CategoryOption( "CategoryOptionB" );
        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );

        Category categoryA = createCategory( 'A', optionA, optionB );
        categoryA.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        categoryA.setUid( "categoryabA" );
        categoryService.addCategory( categoryA );

        CategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addCategoryCombo( categoryComboA );

        CategoryOptionCombo optionComboA = createCategoryOptionCombo( categoryComboA, optionA );
        CategoryOptionCombo optionComboB = createCategoryOptionCombo( categoryComboA, optionB );
        CategoryOptionCombo optionComboC = createCategoryOptionCombo( categoryComboA, optionA, optionB );
        categoryService.addCategoryOptionCombo( optionComboA );
        categoryService.addCategoryOptionCombo( optionComboB );
        categoryService.addCategoryOptionCombo( optionComboC );

        CategoryOptionGroup optionGroupA = createCategoryOptionGroup( 'A', optionA );
        CategoryOptionGroup optionGroupB = createCategoryOptionGroup( 'B', optionB );
        categoryService.saveCategoryOptionGroup( optionGroupA );
        categoryService.saveCategoryOptionGroup( optionGroupB );

        CategoryOptionGroupSet optionGroupSetB = new CategoryOptionGroupSet( "OptionGroupSetB" );
        categoryService.saveCategoryOptionGroupSet( optionGroupSetB );
        optionGroupSetB.addCategoryOptionGroup( optionGroupA );
        optionGroupSetB.addCategoryOptionGroup( optionGroupB );
        optionGroupA.getGroupSets().add( optionGroupSetB );
        optionGroupB.getGroupSets().add( optionGroupSetB );

        Expression expressionVRA = new Expression( "expressionA", "descriptionA" );
        Expression expressionVRB = new Expression( "expressionB", "descriptionB" );
        Expression expressionVRC = new Expression( "expressionC", "descriptionC" );
        Expression expressionVRD = new Expression( "expressionD", "descriptionD" );

        expressionService.addExpression( expressionVRA );
        expressionService.addExpression( expressionVRB );
        expressionService.addExpression( expressionVRC );
        expressionService.addExpression( expressionVRD );

        PeriodType periodType = PeriodType.getPeriodTypeByName( "Monthly" );

        validationRuleA = createValidationRule( 'A', equal_to, expressionVRA, expressionVRB,
            periodType );
        validationRuleA.setUid( "a234567vruA" );

        validationRuleB = createValidationRule( 'B', equal_to, expressionVRC, expressionVRD,
            periodType );
        validationRuleB.setUid( "a234567vruB" );
        validationRuleService.saveValidationRule( validationRuleA );
        validationRuleService.saveValidationRule( validationRuleB );

        ValidationResult resultAA = new ValidationResult( validationRuleA, peJan, ouA, optionComboA, 1.0, 2.0, 3 );
        ValidationResult resultAB = new ValidationResult( validationRuleA, peJan, ouA, optionComboB, 1.0, 2.0, 3 );
        ValidationResult resultBA = new ValidationResult( validationRuleA, peJan, ouB, optionComboA, 1.0, 2.0, 3 );
        ValidationResult resultBB = new ValidationResult( validationRuleA, peJan, ouB, optionComboB, 1.0, 2.0, 3 );
        ValidationResult resultBAB = new ValidationResult( validationRuleB, peJan, ouA, optionComboB, 1.0, 2.0, 3 );
        ValidationResult resultBBB = new ValidationResult( validationRuleB, peFeb, ouB, optionComboB, 1.0, 2.0, 3 );
        ValidationResult resultBBA = new ValidationResult( validationRuleB, peFeb, ouB, optionComboA, 1.0, 2.0, 3 );

        Date today = new Date();
        resultAA.setCreated( today );
        resultAB.setCreated( today );
        resultBA.setCreated( today );
        resultBB.setCreated( today );
        resultBAB.setCreated( today );
        resultBBB.setCreated( today );
        resultBBA.setCreated( today );

        validationResultService.saveValidationResults( List.of( resultAA, resultAB, resultBA, resultBB,
            resultBAB, resultBBB, resultBBA ) );
    }

    /**
     * Adds data value based on input from vales
     *
     * @param lines the list of arrays of property values.
     */
    private void parseDataValues( List<String[]> lines )
    {
        for ( String[] line : lines )
        {
            DataElement dataElement = dataElementService.getDataElement( line[0] );
            Period period = periodService.getPeriod( line[1] );
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( line[2] );
            DataValue dataValue = new DataValue( dataElement, period, organisationUnit, ocDef, ocDef );
            dataValue.setValue( line[3] );
            dataValueService.addDataValue( dataValue );
        }
        assertEquals( 30, dataValueService.getAllDataValues().size(),
            "Import of data values failed, number of imports are wrong" );
    }

    /**
     * Adds data set registrations based on input from vales
     *
     * @param lines the list of arrays of property values.
     */
    private void parseDataSetRegistrations( List<String[]> lines )
    {
        String storedBy = "johndoe";
        String lastUpdatedBy = "johndoe";
        Date now = new Date();
        for ( String[] line : lines )
        {
            DataSet dataSet = dataSetService.getDataSet( line[0] );
            Period period = periodService.getPeriod( line[1] );
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( line[2] );
            CompleteDataSetRegistration completeDataSetRegistration = new CompleteDataSetRegistration( dataSet, period,
                organisationUnit, ocDef, now, storedBy, new Date(), lastUpdatedBy, true );
            completeDataSetRegistrationService.saveCompleteDataSetRegistration( completeDataSetRegistration );
        }
        assertEquals( 15, completeDataSetRegistrationService.getAllCompleteDataSetRegistrations().size(),
            "Import of data set registrations failed, number of imports are wrong" );
    }

    // --------------------------------------------------------------------------
    // Tear down from all tests
    // --------------------------------------------------------------------------

    @Override
    public void tearDownTest()
    {
        for ( AnalyticsTableService service : analyticsTableServices )
        {
            service.dropTables();
        }
    }

    // --------------------------------------------------------------------------
    // Test helpers
    // --------------------------------------------------------------------------

    private void withIndicator( String numerator )
    {
        withIndicator( numerator, "1" );
    }

    private void withIndicator( String numerator, String denominator )
    {
        indicatorA.setNumerator( numerator );
        indicatorA.setDenominator( denominator );

        indicatorService.updateIndicator( indicatorA );
    }

    private void assertDataValues( Map<String, Object> expected, DataQueryParams params )
    {
        assertMapEquals( expected, analyticsService.getAggregatedDataValueMapping( params ) );

        AnalyticsTestUtils.assertResultGrid( expected, analyticsService.getAggregatedDataValues( params ) );
    }

    private Map<String, Object> getDataValueMapping( AnalyticalObject object )
    {
        return analyticsService.getAggregatedDataValueMapping( object );
    }

    private DataValueSet getDataValueSet( DataQueryParams params )
    {
        return analyticsService.getAggregatedDataValueSet( params );
    }

    // --------------------------------------------------------------------------
    // Tests
    // --------------------------------------------------------------------------

    @Test
    void queryValidationResultTable()
    {
        List<Map<String, Object>> resultMap = jdbcTemplate
            .queryForList( "select * from analytics_validationresult_2017;" );
        assertEquals( 7, resultMap.size() );
    }

    @Test
    void test_de_avg_2017_03()
    {
        List<DataElement> dataElements = List.of( deA, deB, deC, deD, deE );

        assertDataValues(
            Map.of( "deabcdefghC-201703", 6.75 ),
            DataQueryParams.newBuilder()
                .withDataElements( dataElements )
                .withAggregationType( AnalyticsAggregationType.AVERAGE )
                .withSkipRounding( true )
                .withPeriod( peMar )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_deC_ouB_2017_03()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withOrganisationUnit( ouB )
            .withDataElements( List.of( deC ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( peMar )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        assertDataValues(
            Map.of( "deabcdefghC-ouabcdefghB-201703", 6.0 ),
            params );

        AnalyticsTestUtils.assertResultSet(
            Map.of( "deabcdefghC-ouabcdefghB-201703", 6.0 ),
            getDataValueSet( params ) );

        assertMapEquals(
            Map.of( "deabcdefghC-201703-ouabcdefghB", 6.0 ),
            getDataValueMapping( new Visualization( "deC_ouB_2017_03",
                List.of( deC ), emptyList(), emptyList(), List.of( peMar ), List.of( ouB ),
                false, true, true ) ) );
    }

    @Test
    void test_deA_ouA_2017_Q01()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withOrganisationUnit( ouA )
            .withDataElements( List.of( deA ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        assertDataValues(
            Map.of( "deabcdefghA-ouabcdefghA-2017Q1", 308.0 ),
            params );

        AnalyticsTestUtils.assertResultSet(
            Map.of( "deabcdefghA-ouabcdefghA-2017Q1", 308.0 ),
            getDataValueSet( params ) );

        assertMapEquals(
            Map.of( "deabcdefghA-2017Q1-ouabcdefghA", 308.0 ),
            getDataValueMapping( new Visualization( "deA_ouA_2017_Q01",
                List.of( deA ), emptyList(), emptyList(), List.of( quarter ), List.of( ouA ),
                false, true, true ) ) );
    }

    @Test
    void testIndicatorWithDataElementOperand()
    {
        withIndicator( "#{" + deA.getUid() + "." + ocDef.getUid() + "}" );

        assertDataValues(
            Map.of( "indicatorId-2017", 308.0 ),
            DataQueryParams.newBuilder()
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( year )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorWithTwoDataElementOperands()
    {
        withIndicator(
            "#{" + deB.getUid() + "." + ocDef.getUid() + "}" + "+#{" + deC.getUid() + "." + ocDef.getUid() + "}" );

        assertDataValues(
            Map.of( "indicatorId-2017Q1", 567.0 ),
            DataQueryParams.newBuilder()
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( quarter )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorDividingByConstantDenominator()
    {
        withIndicator(
            "#{" + deB.getUid() + "." + ocDef.getUid() + "}" + "*#{" + deC.getUid() + "." + ocDef.getUid() + "}",
            "100" );

        assertDataValues(
            Map.of( "indicatorId-2017Q1", 258.50 ),
            DataQueryParams.newBuilder()
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( quarter )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );

        assertMapEquals(
            Map.of( "indicatorId-2017Q1-ouabcdefghA", 258.50 ),
            getDataValueMapping( new Visualization( "deA_ouA_2017_Q01", emptyList(),
                List.of( indicatorA ), emptyList(), List.of( quarter ), List.of( ouA ),
                true, true, true ) ) );
    }

    @Test
    void testIndicatorDividingByDeoDenominator()
    {
        withIndicator(
            "#{" + deA.getUid() + "." + ocDef.getUid() + "}" + "*#{" + deC.getUid() + "." + ocDef.getUid() + "}",
            "#{" + deB.getUid() + "." + ocDef.getUid() + "}" );

        assertDataValues(
            Map.of( "indicatorId-2017Q1", 29.8 ),
            DataQueryParams.newBuilder()
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( quarter )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorWithReportingRateA()
    {
        withIndicator( "#{" + deA.getUid() + "." + ocDef.getUid() + "}"
            + "*(R{" + reportingRateA.getUid() + ".REPORTING_RATE} / 100)" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghD-2017Q1", 199.4 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouD )
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( quarter )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorWithReportingRateB()
    {
        withIndicator( "#{" + deA.getUid() + "." + ocDef.getUid() + "}"
            + "*(R{" + reportingRateB.getUid() + ".REPORTING_RATE} / 100)" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghD-2017Q1", 99.6 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouD )
                .withIndicators( List.of( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( quarter )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorWithPeriodOffsets()
    {
        withIndicator( "#{" + deE.getUid() + "}.periodOffset(-1) + #{" + deE.getUid() + "}.periodOffset(-2)" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghA-201707", 3.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouA )
                .withIndicators( Lists.newArrayList( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriod( peJuly )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorSummingBooleans()
    {
        withIndicator( "#{" + deF.getUid() + "}" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghA-201701", 1.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouA )
                .withIndicators( Lists.newArrayList( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriods( List.of( peJan, peFeb ) )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorSubexpressionBoolean()
    {
        withIndicator( "subExpression( if( #{" + deF.getUid() + "}, 3, 4 ) )" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghA-201701", 3.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouA )
                .withIndicators( Lists.newArrayList( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriods( List.of( peJan, peFeb ) )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorSubexpressionText()
    {
        withIndicator( "subExpression( if( #{" + deG.getUid() + "} == 'abc', 5, 6 ) )" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghA-201701", 5.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouA )
                .withIndicators( Lists.newArrayList( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriods( List.of( peJan, peFeb ) )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void testIndicatorSubexpressionDate()
    {
        withIndicator( "subExpression( if( #{" + deH.getUid() + "} >= '2017-01-01', 7, 8 ) )" );

        assertDataValues(
            Map.of( "indicatorId-ouabcdefghA-201701", 7.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouA )
                .withIndicators( Lists.newArrayList( indicatorA ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withPeriods( List.of( peJan, peFeb ) )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_deA_ouB_ouC_2017_02()
    {
        assertDataValues(
            Map.of( "deabcdefghA-201702", 233.0 ),
            DataQueryParams.newBuilder()
                .withFilterOrganisationUnits( List.of( ouB, ouC ) )
                .withDataElements( List.of( deA ) )
                .withAggregationType( new AnalyticsAggregationType( AggregationType.MAX, AggregationType.MAX ) )
                .withPeriod( peFeb )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_deA_deB_deD_ouC_ouE_2017_04()
    {
        assertDataValues(
            Map.of( "deabcdefghD-201704", 10.5 ),
            DataQueryParams.newBuilder()
                .withFilterOrganisationUnits( Lists.newArrayList( ouC, ouE ) )
                .withDataElements( Lists.newArrayList( deA, deB, deD ) )
                .withAggregationType( AnalyticsAggregationType.AVERAGE )
                .withPeriod( peApril )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_ouB_2017_01_01_2017_02_20()
    {
        assertDataValues(
            Map.of( "deabcdefghA-ouabcdefghB", 68.0 ),
            DataQueryParams.newBuilder()
                .withDataElements( Lists.newArrayList( deA, deB ) )
                .withOrganisationUnit( ouB )
                .withStartDate( getDate( 2017, 1, 1 ) )
                .withEndDate( getDate( 2017, 2, 20 ) )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_reRate_2017_Q01_ouC()
    {
        assertDataValues(
            Map.of( "a23dataSetA.REPORTING_RATE-ouabcdefghC-2017Q1", 100.0 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouC )
                .withReportingRates( List.of( reportingRateA ) )
                .withPeriod( quarter )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_reRate_2017_Q01_ouD()
    {
        assertDataValues(
            Map.of( "a23dataSetB.REPORTING_RATE-ouabcdefghD-2017Q1", 33.3 ),
            DataQueryParams.newBuilder()
                .withOrganisationUnit( ouD )
                .withReportingRates( List.of( reportingRateB ) )
                .withPeriod( quarter )
                .withAggregationType( AnalyticsAggregationType.SUM )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_ou_2017_validationruleA()
    {
        assertDataValues(
            Map.of( "a234567vruA-ouabcdefghA-2017", 4.0,
                "a234567vruA-ouabcdefghB-2017", 2.0 ),
            DataQueryParams.newBuilder()
                .withValidationRules( List.of( validationRuleA ) )
                .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                .withPeriod( year )
                .withAggregationType( AnalyticsAggregationType.COUNT )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_ou_2017_validationruleB()
    {
        assertDataValues(
            Map.of( "a234567vruB-ouabcdefghA-2017", 3.0,
                "a234567vruB-ouabcdefghB-2017", 2.0 ),
            DataQueryParams.newBuilder()
                .withValidationRules( List.of( validationRuleB ) )
                .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                .withPeriod( year )
                .withAggregationType( AnalyticsAggregationType.COUNT )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }

    @Test
    void test_ou_2017_validationruleAB()
    {
        assertDataValues(
            Map.of( "a234567vruA-ouabcdefghA-2017", 4.0,
                "a234567vruA-ouabcdefghB-2017", 2.0,
                "a234567vruB-ouabcdefghA-2017", 3.0,
                "a234567vruB-ouabcdefghB-2017", 2.0 ),
            DataQueryParams.newBuilder()
                .withValidationRules( List.of( validationRuleA, validationRuleB ) )
                .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                .withPeriod( year )
                .withAggregationType( AnalyticsAggregationType.COUNT )
                .withOutputFormat( OutputFormat.ANALYTICS ).build() );
    }
}
