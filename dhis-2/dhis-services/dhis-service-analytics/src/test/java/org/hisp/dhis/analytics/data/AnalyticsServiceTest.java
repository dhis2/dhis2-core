package org.hisp.dhis.analytics.data;

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorService;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

//mer avanserte tester -> Groupset
// mvn -Dtest=AnalyticsServiceTest test
// Statementbuilder
// ValidationNotificationServiceTest

/**
 * Created by henninghakonsen on 13/03/2017.
 * Project: dhis-2.
 */
public class AnalyticsServiceTest
    extends DhisTest
{
        private List<DataElement> dataElements = new ArrayList<>();

        private List<Period> periods = new ArrayList<>();

        private List<Indicator> indicators = new ArrayList<>();

        // Params for data query
        private DataQueryParams ou_2017_params;

        private DataQueryParams ou_2017_01_params;

        private DataQueryParams ouB_2017_02_params;

        private DataQueryParams de_avg_2017_03_params;

        private DataQueryParams deC_ouB_2017_03_params;

        private DataQueryParams deA_ouA_2017_Q3_params;

        private DataQueryParams ouB_ouC_2017_02_params;

        private DataQueryParams ouA_2017_01_03_params;

        private DataQueryParams ouB_2017_Q01_params;

        private DataQueryParams ouC_2017_Q01_params;

        private DataQueryParams inA_deA_2017_params;

        // Results
        private HashMap<String, Double> ou_2017_keyValue = new HashMap<>();

        private HashMap<String, Double> ou_2017_01_keyValue = new HashMap<>();

        private HashMap<String, Double> ouB_2017_02_keyValue = new HashMap<>();

        private HashMap<String, Double> de_avg_2017_03_keyValue = new HashMap<>();

        private HashMap<String, Double> deC_ouB_2017_03_keyValue = new HashMap<>();

        private HashMap<String, Double> deA_ouA_2017_Q3_keyValue = new HashMap<>();

        private HashMap<String, Double> ouB_ouC_2017_02_keyValue = new HashMap<>();

        private HashMap<String, Double> ouA_2017_01_03_keyValue = new HashMap<>();

        private HashMap<String, Double> ouB_2017_Q01_keyValue = new HashMap<>();

        private HashMap<String, Double> ouC_2017_Q01_keyValue = new HashMap<>();

        private HashMap<String, Double> inA_deA_2017_keyValue = new HashMap<>();

        @Autowired
        private DataElementService dataElementService;

        @Autowired
        private DataElementCategoryService categoryService;

        @Autowired
        private DataValueService dataValueService;

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

        //  Database (value, data element, period)
        //  --------------------------------------------------------------------
        //
        //  A:  2, deA, peJan - 4, deB, peFeb - 6, deC, peMar - 8, deD, peApril
        //      100, deB, peJan - 2, deD, peFeb
        //
        //  B:  1, deA, peJan - 3, deB, peFeb - 5, deC, peMar - 7, deD, peApril
        //
        //  C:  5, deA, peJan - 10, deB, peFeb - 15, deC, peMar - 20, deD, peApril
        //      4, deD, peJan - 23, deC, peFeb
        //
        //  D:  66, deA, peJan - 233, deA, peFeb - 399, deB, peFeb
        //
        //  E:  1, deA, peJan - 1, deB, peFeb - 1, deC, peMar - 1, deD, peApril
        //      32, deD, peJan
        //
        //  --------------------------------------------------------------------
        // TODO
        // Manage teardown of database

        // TODO
        // Make tests for set aggregation

        @Override
        public void setUpTest()
        {
                DataElementCategoryOptionCombo ocDef = categoryService.getDefaultDataElementCategoryOptionCombo();
                ocDef.setCode( "OC_DEF_CODE" );
                ocDef.setUid( "OC_DEF_UID" );
                categoryService.updateDataElementCategoryOptionCombo( ocDef );

                Period peJan = createPeriod( "2017-01" );
                Period peFeb = createPeriod( "2017-02" );
                Period peMar = createPeriod( "2017-03" );
                Period peApril = createPeriod( "2017-04" );

                periods.add( peJan );
                periods.add( peFeb );
                periods.add( peMar );
                periods.add( peApril );

                periodService.addPeriod( peJan );
                periodService.addPeriod( peFeb );
                periodService.addPeriod( peMar );
                periodService.addPeriod( peApril );

                DataElement deA = createDataElement( 'A' );
                deA.setCode( "DE_A" );
                DataElement deB = createDataElement( 'B' );
                deB.setCode( "DE_B" );
                DataElement deC = createDataElement( 'C' );
                deC.setCode( "DE_C" );
                DataElement deD = createDataElement( 'D' );
                deD.setCode( "DE_D" );

                dataElementService.addDataElement( deA );
                dataElementService.addDataElement( deB );
                dataElementService.addDataElement( deC );
                dataElementService.addDataElement( deD );

                dataElements.add( deA );
                dataElements.add( deB );
                dataElements.add( deC );
                dataElements.add( deD );

                OrganisationUnit ouA = createOrganisationUnit( 'A' );
                OrganisationUnit ouB = createOrganisationUnit( 'B' );
                OrganisationUnit ouC = createOrganisationUnit( 'C' );
                OrganisationUnit ouD = createOrganisationUnit( 'D' );
                OrganisationUnit ouE = createOrganisationUnit( 'E' );
                configureHierarchy( ouA, ouB, ouC, ouD, ouE );

                organisationUnitService.addOrganisationUnit( ouA );
                organisationUnitService.addOrganisationUnit( ouB );
                organisationUnitService.addOrganisationUnit( ouC );
                organisationUnitService.addOrganisationUnit( ouD );
                organisationUnitService.addOrganisationUnit( ouE );

                // Data values for A
                List<DataValue> dataValuesA = new ArrayList<>();
                for ( int i = 1; i < 5; i++ )
                {
                        dataValuesA
                            .add( new DataValue( dataElements.get( i - 1 ), periods.get( i - 1 ), ouA, ocDef, ocDef ) );
                        dataValuesA.get( i - 1 ).setValue( i * 2 + "" );
                        dataValueService.addDataValue( dataValuesA.get( i - 1 ) );
                }

                // Data values for B
                List<DataValue> dataValuesB = new ArrayList<>();
                for ( int i = 1; i < 5; i++ )
                {
                        dataValuesB
                            .add( new DataValue( dataElements.get( i - 1 ), periods.get( i - 1 ), ouB, ocDef, ocDef ) );
                        dataValuesB.get( i - 1 ).setValue( i * 2 - 1 + "" );
                        dataValueService.addDataValue( dataValuesB.get( i - 1 ) );
                }

                // Data values for C
                List<DataValue> dataValuesC = new ArrayList<>();
                for ( int i = 1; i < 5; i++ )
                {
                        dataValuesC
                            .add( new DataValue( dataElements.get( i - 1 ), periods.get( i - 1 ), ouC, ocDef, ocDef ) );
                        dataValuesC.get( i - 1 ).setValue( i * 5 + "" );
                        dataValueService.addDataValue( dataValuesC.get( i - 1 ) );
                }

                // Data values for E
                List<DataValue> dataValuesE = new ArrayList<>();
                for ( int i = 1; i < 5; i++ )
                {
                        dataValuesE
                            .add( new DataValue( dataElements.get( i - 1 ), periods.get( i - 1 ), ouE, ocDef, ocDef ) );
                        dataValuesE.get( i - 1 ).setValue( "1" );
                        dataValueService.addDataValue( dataValuesE.get( i - 1 ) );
                }

                // "Special" data values
                DataValue dataValue_100_m01 = new DataValue( deB, peJan, ouA, ocDef, ocDef );
                dataValue_100_m01.setValue( "100" );
                dataValueService.addDataValue( dataValue_100_m01 );

                DataValue dataValue_2_m02 = new DataValue( deD, peFeb, ouA, ocDef, ocDef );
                dataValue_2_m02.setValue( "2" );
                dataValueService.addDataValue( dataValue_2_m02 );

                DataValue dataValue_4_m01 = new DataValue( deD, peJan, ouC, ocDef, ocDef );
                dataValue_4_m01.setValue( "4" );
                dataValueService.addDataValue( dataValue_4_m01 );

                DataValue dataValue_23_m02 = new DataValue( deC, peFeb, ouC, ocDef, ocDef );
                dataValue_23_m02.setValue( "23" );
                dataValueService.addDataValue( dataValue_23_m02 );

                DataValue dataValue_66_m01 = new DataValue( deA, peJan, ouD, ocDef, ocDef );
                dataValue_66_m01.setValue( "66" );
                dataValueService.addDataValue( dataValue_66_m01 );

                DataValue dataValue_233_m02 = new DataValue( deA, peFeb, ouD, ocDef, ocDef );
                dataValue_233_m02.setValue( "233" );
                dataValueService.addDataValue( dataValue_233_m02 );

                DataValue dataValue_399_m02 = new DataValue( deB, peFeb, ouD, ocDef, ocDef );
                dataValue_399_m02.setValue( "399" );
                dataValueService.addDataValue( dataValue_399_m02 );

                DataValue dataValue_32_m01 = new DataValue( deD, peJan, ouE, ocDef, ocDef );
                dataValue_32_m01.setValue( "32" );
                dataValueService.addDataValue( dataValue_32_m01 );

                // Indicators
                IndicatorType indicatorType = createIndicatorType( 'A' );
                indicatorType.setFactor( 1 );

                indicatorService.addIndicatorType( indicatorType );

                Indicator indicatorA = createIndicator( 'A', indicatorType );
                String expressionA = "#{" + deA.getUid() + ".OC_DEF_UID" + "}";
                indicatorA.setNumerator( expressionA );
                indicatorA.setDenominator( "1" );

                indicatorService.addIndicator( indicatorA );

                // Generate analytics tables
                analyticsTableGenerator.generateTables( null, null, null, false );

                // Set params
                // all org units - 2017
                Period y2017 = createPeriod( "2017" );
                ou_2017_params = DataQueryParams.newBuilder()
                    .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017 )
                    .build();

                // all org units - jan 2017
                Period y2017_jan = createPeriod( "2017-01" );
                ou_2017_01_params = DataQueryParams.newBuilder()
                    .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017_jan )
                    .build();

                // org unit B - feb 2017
                Period y2017_feb = createPeriod( "2017-02" );
                ouB_2017_02_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouB )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017_feb )
                    .build();

                // all data elements - mar 2017
                Period y2017_mar = createPeriod( "2017-03" );
                de_avg_2017_03_params = DataQueryParams.newBuilder()
                    .withDataElements( dataElements )
                    .withAggregationType( AggregationType.AVERAGE )
                    .withSkipRounding( true )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017_mar )
                    .build();

                // org unit B - data element C - mar 2017
                List<DataElement> dataElementsC = new ArrayList<>();
                dataElementsC.add( deC );

                deC_ouB_2017_03_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouB )
                    .withDataElements( dataElementsC )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017_mar )
                    .build();

                // org unit A - data element A - Q1 2017
                List<DataElement> dataElementsA = new ArrayList<>();
                dataElementsA.add( deA );
                Period quarter = createPeriod( "2017Q1" );

                deA_ouA_2017_Q3_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouA )
                    .withDataElements( dataElementsA )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( quarter )
                    .build();

                // org units B and C - feb 2017
                List<OrganisationUnit> organisationUnits = new ArrayList<>();
                organisationUnits.add( ouB );
                organisationUnits.add( ouC );

                ouB_ouC_2017_02_params = DataQueryParams.newBuilder()
                    .withFilterOrganisationUnits( organisationUnits )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017_feb )
                    .build();

                // org unit A - jan and feb 2017
                List<Period> periodsFilter = new ArrayList<>();
                periodsFilter.add( peJan );
                periodsFilter.add( peMar );

                ouA_2017_01_03_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouA )
                    .withFilterPeriods( periodsFilter )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .build();

                // org unit B - Q1 2017
                ouB_2017_Q01_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouB )
                    .withPeriod( quarter )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .build();

                // org unit C - Q 2017
                ouC_2017_Q01_params = DataQueryParams.newBuilder()
                    .withOrganisationUnit( ouC )
                    .withPeriod( quarter )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .build();

                // indicator A - 2017
                List<Indicator> indicators = new ArrayList<>();
                indicators.add( indicatorA );
                inA_deA_2017_params = DataQueryParams.newBuilder()
                    .withIndicators( indicators )
                    .withAggregationType( AggregationType.SUM )
                    .withOutputFormat( OutputFormat.ANALYTICS )
                    .withPeriod( y2017 )
                    .build();

                // Set results
                ou_2017_keyValue.put( "ouabcdefghA-2017", 949.0 );
                ou_2017_keyValue.put( "ouabcdefghB-2017", 750.0 );
                ou_2017_keyValue.put( "ouabcdefghC-2017", 77.0 );
                ou_2017_keyValue.put( "ouabcdefghD-2017", 698.0 );
                ou_2017_keyValue.put( "ouabcdefghE-2017", 36.0 );

                ou_2017_01_keyValue.put( "ouabcdefghA-201701", 211.0 );
                ou_2017_01_keyValue.put( "ouabcdefghB-201701", 100.0 );
                ou_2017_01_keyValue.put( "ouabcdefghC-201701", 9.0 );
                ou_2017_01_keyValue.put( "ouabcdefghD-201701", 66.0 );
                ou_2017_01_keyValue.put( "ouabcdefghE-201701", 33.0 );

                ouB_2017_02_keyValue.put( "ouabcdefghB-201702", 636.00 );

                de_avg_2017_03_keyValue.put( "deabcdefghC-201703", 6.75 );

                deC_ouB_2017_03_keyValue.put( "deabcdefghC-ouabcdefghB-201703", 6.0 );

                deA_ouA_2017_Q3_keyValue.put( "deabcdefghA-ouabcdefghA-2017Q1", 308.0 );

                ouB_ouC_2017_02_keyValue.put( "201702", 669.0 );

                ouA_2017_01_03_keyValue.put( "ouabcdefghA", 238.0 );

                ouB_2017_Q01_keyValue.put( "ouabcdefghB-2017Q1", 742.0 );

                ouC_2017_Q01_keyValue.put( "ouabcdefghC-2017Q1", 57.0 );

                inA_deA_2017_keyValue.put( "inabcdefghA-2017", 308.0 );
        }

        @Override
        public boolean emptyDatabaseAfterTest()
        {
                return true;
        }

        @Override
        @Ignore
        public void tearDownTest()
        {
                analyticsTableGenerator.dropTables();
        }

        @Test
        public void testMappingAggregation()
        {
                // --------------------------------------------------------------------
                Map<String, Object> aggregatedDataValueMapping = analyticsService
                    .getAggregatedDataValueMapping( ou_2017_params );

                assertDataValueMapping( aggregatedDataValueMapping, ou_2017_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ou_2017_01_params );

                assertDataValueMapping( aggregatedDataValueMapping, ou_2017_01_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ouB_2017_02_params );

                assertEquals( 1, aggregatedDataValueMapping.size() );

                assertDataValueMapping( aggregatedDataValueMapping, ouB_2017_02_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( de_avg_2017_03_params );

                assertDataValueMapping( aggregatedDataValueMapping, de_avg_2017_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( deC_ouB_2017_03_params );

                assertDataValueMapping( aggregatedDataValueMapping, deC_ouB_2017_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ouB_ouC_2017_02_params );

                assertDataValueMapping( aggregatedDataValueMapping, ouB_ouC_2017_02_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ouA_2017_01_03_params );

                assertDataValueMapping( aggregatedDataValueMapping, ouA_2017_01_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ouB_2017_Q01_params );

                assertDataValueMapping( aggregatedDataValueMapping, ouB_2017_Q01_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( ouC_2017_Q01_params );

                assertDataValueMapping( aggregatedDataValueMapping, ouC_2017_Q01_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping( inA_deA_2017_params );

                assertDataValueMapping( aggregatedDataValueMapping, inA_deA_2017_keyValue );
        }

        @Test
        @Ignore
        public void testGridAggregation()
        {
                // --------------------------------------------------------------------
                Grid aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ou_2017_params );

                assertEquals( "2017", aggregatedDataValueGrid.getRow( 0 ).get( 1 ) );
                assertDataValueGrid( aggregatedDataValueGrid, ou_2017_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ou_2017_01_params );

                assertDataValueGrid( aggregatedDataValueGrid, ou_2017_01_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ouB_2017_02_params );

                assertDataValueGrid( aggregatedDataValueGrid, ouB_2017_02_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( de_avg_2017_03_params );

                assertDataValueGrid( aggregatedDataValueGrid, de_avg_2017_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( deC_ouB_2017_03_params );

                assertDataValueGrid( aggregatedDataValueGrid, deC_ouB_2017_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ouB_ouC_2017_02_params );

                assertDataValueGrid( aggregatedDataValueGrid, ouB_ouC_2017_02_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ouA_2017_01_03_params );

                assertDataValueGrid( aggregatedDataValueGrid, ouA_2017_01_03_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ouB_2017_Q01_params );

                assertDataValueGrid( aggregatedDataValueGrid, ouB_2017_Q01_keyValue );

                // --------------------------------------------------------------------
                aggregatedDataValueGrid = analyticsService.getAggregatedDataValues( ouC_2017_Q01_params );

                assertDataValueGrid( aggregatedDataValueGrid, ouC_2017_Q01_keyValue );
        }

        @Test
        @Ignore
        public void testSetAggregation()
        {
                // Params: Sum for all org units for 2017
                DataValueSet aggregatedDataValueSet = analyticsService
                    .getAggregatedDataValueSet( deC_ouB_2017_03_params );

                assertDataValueSet( aggregatedDataValueSet, deC_ouB_2017_03_keyValue );

                // Params: Sum for all org unit A, in data element a in Q1 2017
                aggregatedDataValueSet = analyticsService.getAggregatedDataValueSet( deA_ouA_2017_Q3_params );

                assertDataValueSet( aggregatedDataValueSet, deA_ouA_2017_Q3_keyValue );
        }

        //  -------------------------------------------------------------------------
        //  Internal Logic
        //  -------------------------------------------------------------------------

        /**
         * Configure org unit hierarchy like so:
         *
         *              A
         *            /  \
         *           B    C
         *         /  \
         *        D    E
         *
         * @param A root
         * @param B leftRoot
         * @param C rightRoot
         * @param D leftB
         * @param E rightB
         */
        private void configureHierarchy( OrganisationUnit A, OrganisationUnit B, OrganisationUnit C, OrganisationUnit D,
            OrganisationUnit E )
        {
                A.getChildren().addAll( Sets.newHashSet( B, C ) );
                B.setParent( A );
                C.setParent( A );

                B.getChildren().addAll( Sets.newHashSet( D, E ) );
                D.setParent( B );
                E.setParent( B );
        }

        /**
         * Test if values from keyValue corresponds with values in aggregatedDataValueMapping.
         * Also test for null values, and "" as key in aggregatedDataValueMapping
         *
         * @param aggregatedDataValueMapping aggregated values
         * @param keyValue                   expected results
         */
        private void assertDataValueMapping( Map<String, Object> aggregatedDataValueMapping,
            HashMap<String, Double> keyValue )
        {
                assertNotNull( aggregatedDataValueMapping );
                assertNull( aggregatedDataValueMapping.get( "testNull" ) );
                assertNull( aggregatedDataValueMapping.get( "" ) );

                for ( Map.Entry<String, Object> entry : aggregatedDataValueMapping.entrySet() )
                {
                        String key = entry.getKey();
                        Double value = (Double) entry.getValue();

                        assertNotNull( keyValue.get( key ) );
                        assertEquals( "'" + key + "' --",
                            value, keyValue.get( key ) );
                }
        }

        /**
         * Test if values from keyValue corresponds with values in aggregatedDataValueMapping.
         * Also test for null values.
         *
         * @param aggregatedDataValueGrid aggregated values
         * @param keyValue                expected results
         */
        private void assertDataValueGrid( Grid aggregatedDataValueGrid, HashMap<String, Double> keyValue )
        {
                assertNotNull( aggregatedDataValueGrid );
                for ( int i = 0; i < aggregatedDataValueGrid.getRows().size(); i++ )
                {
                        int numberOfDimensions = aggregatedDataValueGrid.getRows().get( 0 ).size() - 1;

                        String key = "";
                        for ( int j = 0; j < numberOfDimensions; j++ )
                        {
                                key += aggregatedDataValueGrid.getValue( i, j ).toString();
                                if ( j != numberOfDimensions - 1 )
                                        key += "-";
                        }

                        assertNotNull( keyValue.get( key ) );

                        Double value = keyValue.get( key );

                        assertNotNull( aggregatedDataValueGrid.getRow( i ) );
                        assertEquals( "'" + aggregatedDataValueGrid.getValue( i, 0 ) + "' --",
                            value, aggregatedDataValueGrid.getValue( i, numberOfDimensions ) );
                }
        }

        /**
         * Test if values from keyValue corresponds with values in aggregatedDataValueSet.
         * Also test for null values.
         *
         * @param aggregatedDataValueSet aggregated values
         * @param keyValue               expected results
         */
        private void assertDataValueSet( DataValueSet aggregatedDataValueSet, HashMap<String, Double> keyValue )
        {
                for ( org.hisp.dhis.dxf2.datavalue.DataValue dataValue : aggregatedDataValueSet.getDataValues() )
                {
                        String key =
                            dataValue.getDataElement() + "-" + dataValue.getOrgUnit() + "-" + dataValue.getPeriod();

                        assertNotNull( keyValue.get( key ) );
                        Double value = Double.parseDouble( dataValue.getValue() );
                        assertEquals( value, keyValue.get( key ) );
                }
        }
}
