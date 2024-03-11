package org.hisp.dhis.analytics.data;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.IntegrationTest;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.util.AnalyticsTestUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.*;
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
import org.hisp.dhis.organisationunit.*;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

import static org.hisp.dhis.expression.Operator.equal_to;
import static org.junit.Assert.assertEquals;

/**
 * Tests aggregation of data in analytics tables.
 * <p>
 * To create a new test:
 * <p>
 * <ul>
 * <li>Make new DataQueryParam/AnalyticalObject.</li>
 * <li>Add to 'dataQueryParams'/'analyticalObjectHashMap' map.</li>
 * <li>Add HashMap<String, Double> with expected output to results map.</li>
 * </ul>
 * 
 * @author Henning Haakonsen
 */
@org.junit.experimental.categories.Category( IntegrationTest.class )
public class AnalyticsServiceTest
    extends DhisTest
{
    private CategoryOptionCombo ocDef;

    private Map<String, DataQueryParams> dataQueryParams = new HashMap<>();

    private Map<String, AnalyticalObject> analyticalObjectHashMap = new HashMap<>();

    private Map<String, Map<String, Double>> results = new HashMap<>();

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
    private ValidationRuleStore validationRuleStore;

    @Autowired
    private ValidationResultStore validationResultStore;

    @Autowired
    private CompleteDataSetRegistrationService completeDataSetRegistrationService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Resource( name = "readOnlyJdbcTemplate" )
    private JdbcTemplate jdbcTemplate;

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
        throws IOException
    {
        // Set up meta data for data values
        // --------------------------------------------------------------------
        ReportingRate reportingRateA;
        ReportingRate reportingRateB;

        ocDef = categoryService.getDefaultCategoryOptionCombo();
        ocDef.setUid( "o1234578def" );
        categoryService.updateCategoryOptionCombo( ocDef );

        Period peJan = createPeriod( "2017-01" );
        Period peFeb = createPeriod( "2017-02" );
        Period peMar = createPeriod( "2017-03" );
        Period peApril = createPeriod( "2017-04" );
        Period quarter = createPeriod( "2017Q1" );

        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );
        periodService.addPeriod( peApril );

        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElement deC = createDataElement( 'C' );
        DataElement deD = createDataElement( 'D' );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );

        OrganisationUnit ouA = createOrganisationUnit( 'A' );

        OrganisationUnit ouB = createOrganisationUnit( 'B' );

        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        ouC.setOpeningDate( getDate( 2016, 4, 10 ) );
        ouC.setClosedDate( null );

        OrganisationUnit ouD = createOrganisationUnit( 'D' );
        ouD.setOpeningDate( getDate( 2016, 12, 10 ) );
        ouD.setClosedDate( null );

        OrganisationUnit ouE = createOrganisationUnit( 'E' );
        AnalyticsTestUtils.configureHierarchy( ouA, ouB, ouC, ouD, ouE );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );
        idObjectManager.save( ouD );
        idObjectManager.save( ouE );

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

        DataSet dataSetA = createDataSet( 'A' );
        dataSetA.setUid( "a23dataSetA" );

        dataSetA.addOrganisationUnit( ouD );
        dataSetA.addOrganisationUnit( ouC );

        DataSet dataSetB = createDataSet( 'B' );
        dataSetB.setUid( "a23dataSetB" );

        dataSetB.addOrganisationUnit( ouD );

        dataSetService.addDataSet( dataSetA );
        dataSetService.addDataSet( dataSetB );

        // Read data values from CSV files
        // --------------------------------------------------------------------
        List<String[]> dataValueLines = AnalyticsTestUtils.readInputFile( "csv/dataValues.csv" );
        parseDataValues( dataValueLines );

        List<String[]> dataSetRegistrationLines = AnalyticsTestUtils.readInputFile( "csv/dataSetRegistrations.csv" );
        parseDataSetRegistrations( dataSetRegistrationLines );

        // Make indicators
        // --------------------------------------------------------------------
        IndicatorType indicatorType_1 = createIndicatorType( 'A' );
        indicatorType_1.setFactor( 1 );

        indicatorService.addIndicatorType( indicatorType_1 );

        // deA
        Indicator indicatorA = createIndicator( 'A', indicatorType_1 );
        String expressionA = "#{" + deA.getUid() + "." + ocDef.getUid() + "}";
        indicatorA.setNumerator( expressionA );
        indicatorA.setDenominator( "1" );

        // deB + deC
        Indicator indicatorB = createIndicator( 'B', indicatorType_1 );
        String expressionB =
            "#{" + deB.getUid() + "." + ocDef.getUid() + "}" + "+#{" + deC.getUid() + "." + ocDef.getUid() + "}";
        indicatorB.setNumerator( expressionB );
        indicatorB.setDenominator( "1" );

        // (deB * deC) / 100
        Indicator indicatorC = createIndicator( 'C', indicatorType_1 );
        String expressionC =
            "#{" + deB.getUid() + "." + ocDef.getUid() + "}" + "*#{" + deC.getUid() + "." + ocDef.getUid() + "}";

        indicatorC.setNumerator( expressionC );
        indicatorC.setDenominator( "100" );

        // (deA * deC) / deB
        Indicator indicatorD = createIndicator( 'D', indicatorType_1 );
        String expressionD =
            "#{" + deA.getUid() + "." + ocDef.getUid() + "}" + "*#{" + deC.getUid() + "." + ocDef.getUid() + "}";
        indicatorD.setNumerator( expressionD );
        indicatorD.setDenominator( "#{" + deB.getUid() + "." + ocDef.getUid() + "}" );

        // deA * reporting rate B
        Indicator indicatorE = createIndicator( 'E', indicatorType_1 );
        reportingRateA = new ReportingRate( dataSetA );
        reportingRateB = new ReportingRate( dataSetB );

        String expressionE = "#{" + deA.getUid() + "." + ocDef.getUid() + "}" + "*(R{" + reportingRateB.getUid() + ".REPORTING_RATE} / 100)";
        indicatorE.setNumerator( expressionE );
        indicatorE.setDenominator( "1" );

        // deA * reporting rate A
        Indicator indicatorF = createIndicator( 'F', indicatorType_1 );

        String expressionF = "#{" + deA.getUid() + "." + ocDef.getUid() + "}" + "*(R{" + reportingRateA.getUid() + ".REPORTING_RATE} / 100)";
        indicatorF.setNumerator( expressionF );
        indicatorF.setDenominator( "1" );

        indicatorService.addIndicator( indicatorA );
        indicatorService.addIndicator( indicatorB );
        indicatorService.addIndicator( indicatorC );
        indicatorService.addIndicator( indicatorD );
        indicatorService.addIndicator( indicatorE );
        indicatorService.addIndicator( indicatorF );

        // Validation results
        CategoryOption optionA = new CategoryOption( "CategoryOptionA" );
        CategoryOption optionB = new CategoryOption( "CategoryOptionB" );
        categoryService.addCategoryOption( optionA );
        categoryService.addCategoryOption( optionB );

        Category categoryA = createCategory( 'A', optionA, optionB );
        categoryA.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        categoryService.addCategory( categoryA );

        CategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );
        categoryService.addCategoryCombo( categoryComboA );

        CategoryOptionCombo optionComboA = createCategoryOptionCombo( 'A', categoryComboA, optionA );
        CategoryOptionCombo optionComboB = createCategoryOptionCombo( 'B', categoryComboA, optionB );
        CategoryOptionCombo optionComboC = createCategoryOptionCombo( 'C', categoryComboA, optionA, optionB );

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

        ValidationRule validationRuleA = createValidationRule( 'A', equal_to, expressionVRA, expressionVRB, periodType );
        validationRuleA.setUid( "a234567vruA" );

        ValidationRule validationRuleB = createValidationRule( 'B', equal_to, expressionVRC, expressionVRD, periodType );
        validationRuleB.setUid( "a234567vruB" );
        validationRuleStore.save( validationRuleA );
        validationRuleStore.save( validationRuleB );

        Date today = new Date();
        ValidationResult validationResultBA = new ValidationResult( validationRuleA, peJan, ouB, optionComboA, 1.0,2.0, 3 );
        validationResultBA.setCreated(today);
        ValidationResult validationResultBB = new ValidationResult( validationRuleA, peJan, ouB, optionComboB, 1.0,2.0, 3 );
        validationResultBB.setCreated(today);
        ValidationResult validationResultAA = new ValidationResult( validationRuleA, peJan, ouA, optionComboA, 1.0,2.0, 3 );
        validationResultAA.setCreated(today);
        ValidationResult validationResultAB = new ValidationResult( validationRuleA, peJan, ouA, optionComboB, 1.0,2.0, 3 );
        validationResultAB.setCreated(today);

        ValidationResult validationResultBAB = new ValidationResult( validationRuleB, peJan, ouA, optionComboB, 1.0,2.0, 3 );
        validationResultBAB.setCreated(today);
        ValidationResult validationResultBBB = new ValidationResult( validationRuleB, peFeb, ouB, optionComboB, 1.0,2.0, 3 );
        validationResultBBB.setCreated(today);
        ValidationResult validationResultBBA = new ValidationResult( validationRuleB, peFeb, ouB, optionComboA, 1.0,2.0, 3 );

        validationResultStore.save( validationResultAA );
        validationResultStore.save( validationResultAB );
        validationResultStore.save( validationResultBB );
        validationResultStore.save( validationResultBA );
        validationResultStore.save( validationResultBAB );
        validationResultStore.save( validationResultBBB );
        validationResultStore.save( validationResultBBA );

        // Generate analytics tables
        // --------------------------------------------------------------------
        analyticsTableGenerator.generateTables( AnalyticsTableUpdateParams.newBuilder().build() );

        // Set parameters
        // --------------------------------------------------------------------
        List<Indicator> param_indicators = new ArrayList<>();
        List<ReportingRate> param_reportingRates = new ArrayList<>();

        // all org units - 2017
        Period y2017 = createPeriod( "2017" );
        DataQueryParams ou_2017_params = DataQueryParams.newBuilder()
            .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
            .withAggregationType( AnalyticsAggregationType.SUM ).withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // all org units - jan 2017
        Period y2017_jan = createPeriod( "2017-01" );
        DataQueryParams ou_2017_01_params = DataQueryParams.newBuilder()
            .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017_jan ).withOutputFormat( OutputFormat.ANALYTICS ).build();

        // org unit B - feb 2017
        Period y2017_feb = createPeriod( "2017-02" );
        DataQueryParams ouB_2017_02_params = DataQueryParams.newBuilder().withOrganisationUnit( ouB )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017_feb ).withOutputFormat( OutputFormat.ANALYTICS ).build();

        // all data elements - mar 2017
        Period y2017_mar = createPeriod( "2017-03" );
        DataQueryParams de_avg_2017_03_params = DataQueryParams.newBuilder()
            .withDataElements( dataElementService.getAllDataElements() )
            .withAggregationType( AnalyticsAggregationType.AVERAGE )
            .withSkipRounding( true ).withPeriod( y2017_mar ).withOutputFormat( OutputFormat.ANALYTICS ).build();

        // org unit B - data element C - mar 2017
        List<DataElement> dataElements1 = new ArrayList<>();
        dataElements1.add( deC );
        DataQueryParams deC_ouB_2017_03_params = DataQueryParams.newBuilder().withOrganisationUnit( ouB )
            .withDataElements( dataElements1 )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017_mar ).withOutputFormat( OutputFormat.ANALYTICS ).build();

        AnalyticalObject deC_ouB_2017_03_analytical = new ReportTable( "deC_ouB_2017_03", dataElements1,
            param_indicators, param_reportingRates, Lists.newArrayList( y2017_mar ), Lists.newArrayList( ouB ), false,
            true, true, null, null, null );

        // org unit A - data element A - Q1 2017
        List<DataElement> dataElements2 = new ArrayList<>();
        dataElements2.add( deA );

        DataQueryParams deA_ouA_2017_Q01_params = DataQueryParams.newBuilder().withOrganisationUnit( ouA )
            .withDataElements( dataElements2 )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        AnalyticalObject deA_ouA_2017_Q01_analytical = new ReportTable( "deA_ouA_2017_Q01", dataElements2,
            param_indicators, param_reportingRates, Lists.newArrayList( quarter ), Lists.newArrayList( ouA ), false,
            true, true, null, null, null );

        // org units B and C - feb 2017
        DataQueryParams ouB_ouC_2017_02_params = DataQueryParams.newBuilder()
            .withFilterOrganisationUnits( Lists.newArrayList( ouB, ouC ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017_feb )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // org unit A - jan and feb 2017
        DataQueryParams ouA_2017_01_03_params = DataQueryParams.newBuilder().withOrganisationUnit( ouA )
            .withFilterPeriods( Lists.newArrayList( peJan, peMar ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // org unit B - Q1 2017
        DataQueryParams ouB_2017_Q01_params = DataQueryParams.newBuilder().withOrganisationUnit( ouB )
            .withPeriod( quarter )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // org unit C - Q1 2017
        DataQueryParams ouC_2017_Q01_params = DataQueryParams.newBuilder().withOrganisationUnit( ouC )
            .withPeriod( quarter )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // indicator A - 2017
        DataQueryParams inA_2017_params = DataQueryParams.newBuilder()
            .withIndicators( Lists.newArrayList( indicatorA ) )
            .withAggregationType( AnalyticsAggregationType.SUM ).withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // indicator B (deB + deC) - 2017 Q1
        DataQueryParams inB_deB_deC_2017_Q01_params = DataQueryParams.newBuilder()
            .withIndicators( Lists.newArrayList( indicatorB ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // indicator C (deB * deC) in hundreds - 2017 Q1
        List<Indicator> param_indicators3 = new ArrayList<>();
        param_indicators3.add( indicatorC );
        DataQueryParams inC_deB_deC_2017_Q01_params = DataQueryParams.newBuilder().withIndicators( param_indicators3 )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        AnalyticalObject inC_deB_deC_2017_Q01_analytical = new ReportTable( "deA_ouA_2017_Q01", Lists.newArrayList(),
            param_indicators3, param_reportingRates, Lists.newArrayList( quarter ), Lists.newArrayList( ouA ), true,
            true, true, null, null, null );

        // indicator D (deA * deC)/deB - 2017 Q1
        DataQueryParams inD_deA_deB_deC_2017_Q01_params = DataQueryParams.newBuilder()
            .withIndicators( Lists.newArrayList( indicatorD ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // indicator E (deA * reporting rate B) / 100 - 2017 Q1
        DataQueryParams inE_deA_reRateA_2017_Q01_params = DataQueryParams.newBuilder().withOrganisationUnit( ouD )
            .withIndicators( Lists.newArrayList( indicatorE ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // indicator E (deA * reporting rate A) / 100 - 2017 Q1
        DataQueryParams inF_deA_reRateB_2017_Q01_params = DataQueryParams.newBuilder().withOrganisationUnit( ouD )
            .withIndicators( Lists.newArrayList( indicatorF ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( quarter )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Max value - org unit B and C - data element A - 2017 Feb
        DataQueryParams deA_ouB_ouC_2017_02_params = DataQueryParams.newBuilder()
            .withFilterOrganisationUnits( Lists.newArrayList( ouB, ouC ) ).withDataElements( Lists.newArrayList( deA ) )
            .withAggregationType( new AnalyticsAggregationType( AggregationType.MAX, AggregationType.MAX ) )
            .withPeriod( peFeb )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Average value - org unit C and E - data element A, B and D - 2017 April
        DataQueryParams deA_deB_deD_ouC_ouE_2017_04_params = DataQueryParams.newBuilder()
            .withFilterOrganisationUnits( Lists.newArrayList( ouC, ouE ) )
            .withDataElements( Lists.newArrayList( deA, deB, deD ) )
            .withAggregationType( AnalyticsAggregationType.AVERAGE )
            .withOutputFormat( OutputFormat.ANALYTICS )
            .withPeriod( peApril )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Sum org unit B - 2017-01-01 -> 2017-02-20
        DataQueryParams ouB_2017_01_01_2017_02_20_params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA, deB ) )
            .withOrganisationUnit( ouB )
            .withStartDate( getDate( 2017, 1, 1 ) )
            .withEndDate( getDate( 2017, 2, 20 ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS )
            .build();

        // Sum org unit B - 2017-01-01 -> 2017-02-20
        DataQueryParams ouB_2017_02_10_2017_06_20_params = DataQueryParams.newBuilder()
            .withOrganisationUnit( ouB )
            .withStartDate( getDate( 2017, 2, 10 ) )
            .withEndDate( getDate( 2017, 6, 20 ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS )
            .build();

        // Sum org group set A - 2017
        DataQueryParams ouGroupSetA_2017_params = DataQueryParams.newBuilder()
            .withDimensions( Lists.newArrayList( organisationUnitGroupSetA ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Sum org group set B - 2017
        DataQueryParams ouGroupSetB_2017_03_params = DataQueryParams.newBuilder()
            .withDimensions( Lists.newArrayList( organisationUnitGroupSetB ) )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withPeriod( y2017_mar )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Reportingrate for dataSet A - Q1 2017
        DataQueryParams reRate_2017_Q01_ouC_params = DataQueryParams.newBuilder()
            .withOrganisationUnit( ouC )
            .withReportingRates( Lists.newArrayList( reportingRateA ) )
            .withPeriod( quarter )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Reportingrate for dataSet B - Q1 2017
        DataQueryParams reRate_2017_Q01_ouD_params = DataQueryParams.newBuilder()
            .withOrganisationUnit( ouD )
            .withReportingRates( Lists.newArrayList( reportingRateB ) )
            .withPeriod( quarter )
            .withAggregationType( AnalyticsAggregationType.SUM )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        // Validation result queries
        DataQueryParams ou_2017_validationruleA_params = DataQueryParams.newBuilder()
            .withValidationRules( Lists.newArrayList( validationRuleA ) )
            .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
            .withAggregationType( AnalyticsAggregationType.COUNT )
            .withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        DataQueryParams ou_2017_validationruleB_params = DataQueryParams.newBuilder()
            .withValidationRules( Lists.newArrayList( validationRuleB ) )
            .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
            .withAggregationType( AnalyticsAggregationType.COUNT )
            .withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        DataQueryParams ou_2017_validationruleAB_params = DataQueryParams.newBuilder()
            .withValidationRules( Lists.newArrayList( validationRuleA, validationRuleB ) )
            .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
            .withAggregationType( AnalyticsAggregationType.COUNT )
            .withPeriod( y2017 )
            .withOutputFormat( OutputFormat.ANALYTICS ).build();

        dataQueryParams.put( "ou_2017", ou_2017_params );
        dataQueryParams.put( "ou_2017_01", ou_2017_01_params );
        dataQueryParams.put( "ouB_2017_02", ouB_2017_02_params );
        dataQueryParams.put( "de_avg_2017_03", de_avg_2017_03_params );
        dataQueryParams.put( "deC_ouB_2017_03", deC_ouB_2017_03_params );
        dataQueryParams.put( "deA_ouA_2017_Q01", deA_ouA_2017_Q01_params );
        dataQueryParams.put( "ouB__ouC_2017_02", ouB_ouC_2017_02_params );
        dataQueryParams.put( "ouA_2017_01_03", ouA_2017_01_03_params );
        dataQueryParams.put( "ouB_2017_Q01", ouB_2017_Q01_params );
        dataQueryParams.put( "ouC_2017_Q01", ouC_2017_Q01_params );
        dataQueryParams.put( "inA_2017", inA_2017_params );
        dataQueryParams.put( "inB_deB_deC_2017_Q01", inB_deB_deC_2017_Q01_params );
        dataQueryParams.put( "inC_deB_deC_2017_Q01", inC_deB_deC_2017_Q01_params );
        dataQueryParams.put( "inD_deA_deB_deC_2017_Q01", inD_deA_deB_deC_2017_Q01_params );
        dataQueryParams.put( "inE_deA_reRateA_2017_Q01", inE_deA_reRateA_2017_Q01_params );
        dataQueryParams.put( "inF_deA_reRateB_2017_Q01", inF_deA_reRateB_2017_Q01_params );
        dataQueryParams.put( "deA_ouB_ouC_2017_02", deA_ouB_ouC_2017_02_params );
        dataQueryParams.put( "deA_deB_deD_ouC_ouE_2017_04", deA_deB_deD_ouC_ouE_2017_04_params );
        dataQueryParams.put( "ouB_2017_01_01_2017_02_20", ouB_2017_01_01_2017_02_20_params );
        dataQueryParams.put( "ouB_2017_02_10_2017_06_20", ouB_2017_02_10_2017_06_20_params );
        dataQueryParams.put( "ouGroupSetA_2017", ouGroupSetA_2017_params );
        dataQueryParams.put( "ouGroupSetB_2017_03", ouGroupSetB_2017_03_params );
        dataQueryParams.put( "reRate_2017_Q01_ouC", reRate_2017_Q01_ouC_params );
        dataQueryParams.put( "reRate_2017_Q01_ouD", reRate_2017_Q01_ouD_params );
        dataQueryParams.put( "ou_2017_validationruleA", ou_2017_validationruleA_params );
        dataQueryParams.put( "ou_2017_validationruleB", ou_2017_validationruleB_params );
        dataQueryParams.put( "ou_2017_validationruleAB", ou_2017_validationruleAB_params );

        analyticalObjectHashMap.put( "deC_ouB_2017_03", deC_ouB_2017_03_analytical );
        analyticalObjectHashMap.put( "deA_ouA_2017_Q01", deA_ouA_2017_Q01_analytical );
        analyticalObjectHashMap.put( "inC_deB_deC_2017_Q01", inC_deB_deC_2017_Q01_analytical );

        // Set results
        // --------------------------------------------------------------------
        Map<String, Double> ou_2017_keyValue = new HashMap<>();
        ou_2017_keyValue.put( "ouabcdefghA-2017", 949.0 );
        ou_2017_keyValue.put( "ouabcdefghB-2017", 750.0 );
        ou_2017_keyValue.put( "ouabcdefghC-2017", 77.0 );
        ou_2017_keyValue.put( "ouabcdefghD-2017", 698.0 );
        ou_2017_keyValue.put( "ouabcdefghE-2017", 36.0 );

        Map<String, Double> ou_2017_01_keyValue = new HashMap<>();
        ou_2017_01_keyValue.put( "ouabcdefghA-201701", 211.0 );
        ou_2017_01_keyValue.put( "ouabcdefghB-201701", 100.0 );
        ou_2017_01_keyValue.put( "ouabcdefghC-201701", 9.0 );
        ou_2017_01_keyValue.put( "ouabcdefghD-201701", 66.0 );
        ou_2017_01_keyValue.put( "ouabcdefghE-201701", 33.0 );

        Map<String, Double> ouB_2017_02_keyValue = new HashMap<>();
        ouB_2017_02_keyValue.put( "ouabcdefghB-201702", 636.00 );

        Map<String, Double> de_avg_2017_03_keyValue = new HashMap<>();
        de_avg_2017_03_keyValue.put( "deabcdefghC-201703", 6.75 );

        Map<String, Double> deC_ouB_2017_03_keyValue = new HashMap<>();
        deC_ouB_2017_03_keyValue.put( "deabcdefghC-ouabcdefghB-201703", 6.0 );
        deC_ouB_2017_03_keyValue.put( "deabcdefghC-201703-ouabcdefghB", 6.0 );

        Map<String, Double> deA_ouA_2017_Q01_keyValue = new HashMap<>();
        deA_ouA_2017_Q01_keyValue.put( "deabcdefghA-ouabcdefghA-2017Q1", 308.0 );
        deA_ouA_2017_Q01_keyValue.put( "deabcdefghA-2017Q1-ouabcdefghA", 308.0 );

        Map<String, Double> ouB_ouC_2017_02_keyValue = new HashMap<>();
        ouB_ouC_2017_02_keyValue.put( "201702", 669.0 );

        Map<String, Double> ouA_2017_01_03_keyValue = new HashMap<>();
        ouA_2017_01_03_keyValue.put( "ouabcdefghA", 238.0 );

        Map<String, Double> ouB_2017_Q01_keyValue = new HashMap<>();
        ouB_2017_Q01_keyValue.put( "ouabcdefghB-2017Q1", 742.0 );

        Map<String, Double> ouC_2017_Q01_keyValue = new HashMap<>();
        ouC_2017_Q01_keyValue.put( "ouabcdefghC-2017Q1", 57.0 );

        Map<String, Double> inA_2017_keyValue = new HashMap<>();
        inA_2017_keyValue.put( "inabcdefghA-2017", 308.0 );

        Map<String, Double> inB_deB_deC_2017_Q01_keyValue = new HashMap<>();
        inB_deB_deC_2017_Q01_keyValue.put( "inabcdefghB-2017Q1", 567.0 );

        Map<String, Double> inC_deB_deC_2017_Q01_keyValue = new HashMap<>();
        inC_deB_deC_2017_Q01_keyValue.put( "inabcdefghC-2017Q1", 258.50 );
        inC_deB_deC_2017_Q01_keyValue.put( "inabcdefghC-2017Q1-ouabcdefghA", 258.50 );

        Map<String, Double> inD_deA_deB_deC_2017_Q01_keyValue = new HashMap<>();
        inD_deA_deB_deC_2017_Q01_keyValue.put( "inabcdefghD-2017Q1", 29.8 );

        Map<String, Double> inE_deA_reRateA_2017_Q01_keyValue = new HashMap<>();
        inE_deA_reRateA_2017_Q01_keyValue.put( "inabcdefghE-ouabcdefghD-2017Q1", 99.6 );

        Map<String, Double> inF_deA_reRateB_2017_Q01_keyValue = new HashMap<>();
        inF_deA_reRateB_2017_Q01_keyValue.put( "inabcdefghF-ouabcdefghD-2017Q1" , 199.4 );

        Map<String, Double> deA_ouB_ouC_2017_02_keyValue = new HashMap<>();
        deA_ouB_ouC_2017_02_keyValue.put( "deabcdefghA-201702", 233.0 );

        Map<String, Double> deA_deB_deD_ouC_ouE_2017_04_keyValue = new HashMap<>();
        deA_deB_deD_ouC_ouE_2017_04_keyValue.put( "deabcdefghD-201704", 10.5 );

        Map<String, Double> deA_deB_2017_Q01_keyValue = new HashMap<>();
        deA_deB_2017_Q01_keyValue.put( "2017Q1", 53.3 );

        Map<String, Double> ouB_2017_01_01_2017_02_20_keyValue = new HashMap<>();
        ouB_2017_01_01_2017_02_20_keyValue.put( "deabcdefghA-ouabcdefghB", 68.0 );

        Map<String, Double> ouB_2017_02_10_2017_06_20_keyValue = new HashMap<>();
        ouB_2017_02_10_2017_06_20_keyValue.put( "ouabcdefghB", 14.0 );

        Map<String, Double> ouGroupSetA_2017_keyValue = new HashMap<>();
        ouGroupSetA_2017_keyValue.put( "a2345groupA-2017", 138.0 );
        ouGroupSetA_2017_keyValue.put( "a2345groupB-2017", 811.0 );

        Map<String, Double> ouGroupSetB_2017_03_keyValue = new HashMap<>();
        ouGroupSetB_2017_03_keyValue.put( "a2345groupC-201703", 26.0 );
        ouGroupSetB_2017_03_keyValue.put( "a2345groupD-201703", 1.0 );

        Map<String, Double> reRate_2017_Q01_ouC_keyValue = new HashMap<>();
        reRate_2017_Q01_ouC_keyValue.put( "a23dataSetA.REPORTING_RATE-ouabcdefghC-2017Q1", 100.0 );

        Map<String, Double> reRate_2017_Q01_ouD_keyValue = new HashMap<>();
        reRate_2017_Q01_ouD_keyValue.put( "a23dataSetB.REPORTING_RATE-ouabcdefghD-2017Q1", 33.3 );

        Map<String, Double> ou_2017_validationruleA_keyValue = new HashMap<>();
        ou_2017_validationruleA_keyValue.put( "a234567vruA-ouabcdefghA-2017", 4.0 );
        ou_2017_validationruleA_keyValue.put( "a234567vruA-ouabcdefghB-2017", 2.0 );

        Map<String, Double> ou_2017_validationruleB_keyValue = new HashMap<>();
        ou_2017_validationruleB_keyValue.put( "a234567vruB-ouabcdefghA-2017", 3.0 );
        ou_2017_validationruleB_keyValue.put( "a234567vruB-ouabcdefghB-2017", 2.0 );

        Map<String, Double> ou_2017_validationruleAB_keyValue = new HashMap<>();
        ou_2017_validationruleAB_keyValue.put( "a234567vruA-ouabcdefghA-2017", 4.0 );
        ou_2017_validationruleAB_keyValue.put( "a234567vruA-ouabcdefghB-2017", 2.0 );
        ou_2017_validationruleAB_keyValue.put( "a234567vruB-ouabcdefghA-2017", 3.0 );
        ou_2017_validationruleAB_keyValue.put( "a234567vruB-ouabcdefghB-2017", 2.0 );

        results.put( "ou_2017", ou_2017_keyValue );
        results.put( "ou_2017_01", ou_2017_01_keyValue );
        results.put( "ouB_2017_02", ouB_2017_02_keyValue );
        results.put( "de_avg_2017_03", de_avg_2017_03_keyValue );
        results.put( "deC_ouB_2017_03", deC_ouB_2017_03_keyValue );
        results.put( "deA_ouA_2017_Q01", deA_ouA_2017_Q01_keyValue );
        results.put( "ouB__ouC_2017_02", ouB_ouC_2017_02_keyValue );
        results.put( "ouA_2017_01_03", ouA_2017_01_03_keyValue );
        results.put( "ouB_2017_Q01", ouB_2017_Q01_keyValue );
        results.put( "ouC_2017_Q01", ouC_2017_Q01_keyValue );
        results.put( "inA_2017", inA_2017_keyValue );
        results.put( "inB_deB_deC_2017_Q01", inB_deB_deC_2017_Q01_keyValue );
        results.put( "inC_deB_deC_2017_Q01", inC_deB_deC_2017_Q01_keyValue );
        results.put( "inD_deA_deB_deC_2017_Q01", inD_deA_deB_deC_2017_Q01_keyValue );
        results.put( "inE_deA_reRateA_2017_Q01", inE_deA_reRateA_2017_Q01_keyValue );
        results.put( "inF_deA_reRateB_2017_Q01", inF_deA_reRateB_2017_Q01_keyValue );
        results.put( "deA_ouB_ouC_2017_02", deA_ouB_ouC_2017_02_keyValue );
        results.put( "deA_deB_deD_ouC_ouE_2017_04", deA_deB_deD_ouC_ouE_2017_04_keyValue );
        results.put( "deA_deB_2017_Q01", deA_deB_2017_Q01_keyValue );
        results.put( "ouB_2017_01_01_2017_02_20", ouB_2017_01_01_2017_02_20_keyValue );
        results.put( "ouB_2017_02_10_2017_06_20", ouB_2017_02_10_2017_06_20_keyValue );
        results.put( "ouGroupSetA_2017", ouGroupSetA_2017_keyValue );
        results.put( "ouGroupSetB_2017_03", ouGroupSetB_2017_03_keyValue );
        results.put( "reRate_2017_Q01_ouC", reRate_2017_Q01_ouC_keyValue );
        results.put( "reRate_2017_Q01_ouD", reRate_2017_Q01_ouD_keyValue );
        results.put( "ou_2017_validationruleA", ou_2017_validationruleA_keyValue );
        results.put( "ou_2017_validationruleB", ou_2017_validationruleB_keyValue );
        results.put( "ou_2017_validationruleAB", ou_2017_validationruleAB_keyValue );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void tearDownTest()
    {
        analyticsTableGenerator.dropTables();
    }

    @Test
    public void queryValidationResultTable()
    {
        List<Map<String, Object>> resultMap = jdbcTemplate.queryForList( "select * from analytics_validationresult_2017;" );

        assertEquals(7, resultMap.size());
    }

    @Test
    public void testMappingAggregation()
    {
        Map<String, Object> aggregatedDataValueMapping;
        for ( Map.Entry<String, DataQueryParams> entry : dataQueryParams.entrySet() )
        {
            String key = entry.getKey();
            DataQueryParams params = entry.getValue();

            aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( params );

            AnalyticsTestUtils.assertResultMapping( aggregatedDataValueMapping, results.get( key ) );
        }

        for ( Map.Entry<String, AnalyticalObject> entry : analyticalObjectHashMap.entrySet() )
        {
            String key = entry.getKey();
            AnalyticalObject params = entry.getValue();

            aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( params );

            AnalyticsTestUtils.assertResultMapping( aggregatedDataValueMapping, results.get( key ) );
        }
    }

    @Test
    public void testGridAggregation()
    {
        Grid aggregatedDataValueGrid;
        for ( Map.Entry<String, DataQueryParams> entry : dataQueryParams.entrySet() )
        {
            String key = entry.getKey();
            DataQueryParams params = entry.getValue();

            aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( params );

            AnalyticsTestUtils.assertResultGrid( aggregatedDataValueGrid, results.get( key ) );
        }
    }

    @Test
    public void testSetAggregation()
    {
        // Params: Sum for all org units for 2017
        DataValueSet aggregatedDataValueSet = analyticsService
            .getAggregatedDataValueSet( dataQueryParams.get( "deC_ouB_2017_03" ) );

        AnalyticsTestUtils.assertResultSet( aggregatedDataValueSet, results.get( "deC_ouB_2017_03" ) );

        // Params: Sum for all org unit A, in data element a in Q1 2017
        aggregatedDataValueSet = analyticsService
            .getAggregatedDataValueSet( dataQueryParams.get( "deA_ouA_2017_Q01" ) );

        AnalyticsTestUtils.assertResultSet( aggregatedDataValueSet, results.get( "deA_ouA_2017_Q01" ) );
    }

    // -------------------------------------------------------------------------
    // Internal Logic
    // -------------------------------------------------------------------------

    /**
     * Adds data value based on input from vales
     *
     * @param lines the list of arrays of property values.
     */
    private void parseDataValues( List<String[]> lines )
    {
        for( String[] line : lines)
        {
            DataElement dataElement = dataElementService.getDataElement( line[0] );
            Period period = periodService.getPeriod( line[1] );
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( line[2] );

            DataValue dataValue = new DataValue( dataElement, period, organisationUnit, ocDef, ocDef );
            dataValue.setValue( line[3] );
            dataValueService.addDataValue( dataValue );
        }

        assertEquals( "Import of data values failed, number of imports are wrong",
            dataValueService.getAllDataValues().size(), 24 );
    }


    /**
     * Adds data set registrations based on input from vales
     *
     * @param lines the list of arrays of property values.
     */
    private void parseDataSetRegistrations( List<String[]> lines )
    {
        String storedBy = "johndoe";
        Date now = new Date();

        for ( String[] line : lines )
        {
            DataSet dataSet = dataSetService.getDataSet( line[0] );
            Period period = periodService.getPeriod( line[1] );
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( line[2] );

            CompleteDataSetRegistration completeDataSetRegistration = new CompleteDataSetRegistration( dataSet, period,
                organisationUnit, ocDef, now,
                storedBy );
            completeDataSetRegistrationService.saveCompleteDataSetRegistration( completeDataSetRegistration );
        }

        assertEquals( "Import of data set registrations failed, number of imports are wrong",
            completeDataSetRegistrationService.getAllCompleteDataSetRegistrations().size(), 15 );
    }
}
