package org.hisp.dhis.analytics.data;

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
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
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;

    private List<Period> periods = new ArrayList<>();
    private Period peJan;
    private Period peFeb;
    private Period peMar;
    private Period peApril;

    private DataElementCategoryCombo categoryComboDef;
    private DataElementCategoryOptionCombo ocDef;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;

    // Results
    private HashMap<String, Double> ou_2017_keyValue = new HashMap<>();
    private HashMap<String, Double> ou_2017_01_keyValue = new HashMap<>();
    private HashMap<String, Double> ouB_2017_02_keyValue = new HashMap<>();
    private HashMap<String, Double> de_avg_2017_03_keyValue = new HashMap<>();
    private HashMap<String, Double> deC_ouB_2017_03_keyValue = new HashMap<>();
    private HashMap<String, Double> ouB_ouC_2017_02_keyValue = new HashMap<>();
    private HashMap<String, Double> ouA_2017_01_03_keyValue = new HashMap<>();

    private DataQueryParams ou_2017_params;
    private DataQueryParams ou_2017_01_params;
    private DataQueryParams ouB_2017_02_params;
    private DataQueryParams de_avg_2017_03_params;
    private DataQueryParams deC_ouB_2017_03_params;
    private DataQueryParams ouB_ouC_2017_02_params;
    private DataQueryParams ouA_2017_01_03_params;


    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private AnalyticsService analyticsService;

    @Override
    public void setUpTest() {
        categoryComboDef = categoryService.getDefaultDataElementCategoryCombo();

        ocDef = categoryService.getDefaultDataElementCategoryOptionCombo();
        ocDef.setCode("OC_DEF_CODE");
        categoryService.updateDataElementCategoryOptionCombo( ocDef );

        peJan = createPeriod( "2017-01" );
        peFeb = createPeriod( "2017-02" );
        peMar = createPeriod( "2017-03" );
        peApril = createPeriod( "2017-04" );

        periods.add(peJan);
        periods.add(peFeb);
        periods.add(peMar);
        periods.add(peApril);

        deA = createDataElement('A');
        deB = createDataElement('B');
        deC = createDataElement('C');
        deD = createDataElement('D');

        dataElementService.addDataElement(deA);
        dataElementService.addDataElement(deB);
        dataElementService.addDataElement(deC);
        dataElementService.addDataElement(deD);

        dataElements.add(deA);
        dataElements.add(deB);
        dataElements.add(deC);
        dataElements.add(deD);

        ouA = createOrganisationUnit('A');
        ouB = createOrganisationUnit('B');
        ouC = createOrganisationUnit('C');
        ouD = createOrganisationUnit('D');
        ouE = createOrganisationUnit('E');
        configureHierarchy(ouA, ouB, ouC, ouD, ouE);

        organisationUnitService.addOrganisationUnit(ouA);
        organisationUnitService.addOrganisationUnit(ouB);
        organisationUnitService.addOrganisationUnit(ouC);
        organisationUnitService.addOrganisationUnit(ouD);
        organisationUnitService.addOrganisationUnit(ouE);

        // Data values for A = 20
        List<DataValue> dataValuesA = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            dataValuesA.add(new DataValue(dataElements.get(i - 1), periods.get(i - 1), ouA, null, null));
            dataValuesA.get(i - 1).setValue(i * 2 + "");
            dataValueService.addDataValue(dataValuesA.get(i - 1));
        }

        // Data values for B = 16
        List<DataValue> dataValuesB = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            dataValuesB.add(new DataValue(dataElements.get(i - 1), periods.get(i - 1), ouB, null, null));
            dataValuesB.get(i - 1).setValue(i * 2 - 1 + "");
            dataValueService.addDataValue(dataValuesB.get(i - 1));
        }

        // Data values for C = 50
        List<DataValue> dataValuesC = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            dataValuesC.add(new DataValue(dataElements.get(i - 1), periods.get(i - 1), ouC, null, null));
            dataValuesC.get(i - 1).setValue(i * 5 + "");
            dataValueService.addDataValue(dataValuesC.get(i - 1));
        }

        // Data values for E = 4
        List<DataValue> dataValuesE = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            dataValuesE.add(new DataValue(dataElements.get(i - 1), periods.get(i - 1), ouE, null, null));
            dataValuesE.get(i - 1).setValue( "1" );
            dataValueService.addDataValue(dataValuesE.get(i - 1));
        }

        DataValue dataValue_100_m01 = new DataValue(deB, peJan, ouA, null, null);
        dataValue_100_m01.setValue( "100" );
        dataValueService.addDataValue(dataValue_100_m01);

        DataValue dataValue_2_m02 = new DataValue(deD, peFeb, ouA, null, null);
        dataValue_2_m02.setValue( "2" );
        dataValueService.addDataValue(dataValue_2_m02);

        DataValue dataValue_4_m01 = new DataValue(deD, peJan, ouC, null, null);
        dataValue_4_m01.setValue( "4" );
        dataValueService.addDataValue(dataValue_4_m01);

        DataValue dataValue_23_m02 = new DataValue(deC, peFeb, ouC, null, null);
        dataValue_23_m02.setValue( "23" );
        dataValueService.addDataValue(dataValue_23_m02);

        DataValue dataValue_66_m01 = new DataValue(deA, peJan, ouD, null, null);
        dataValue_66_m01.setValue( "66" );
        dataValueService.addDataValue(dataValue_66_m01);

        DataValue dataValue_233_m02 = new DataValue(deA, peFeb, ouD, null, null);
        dataValue_233_m02.setValue( "233" );
        dataValueService.addDataValue(dataValue_233_m02);

        DataValue dataValue_399_m02 = new DataValue(deB, peFeb, ouD, null, null);
        dataValue_399_m02.setValue( "399" );
        dataValueService.addDataValue(dataValue_399_m02);

        DataValue dataValue_32_m01 = new DataValue(deD, peJan, ouE, null, null);
        dataValue_32_m01.setValue( "32" );
        dataValueService.addDataValue(dataValue_32_m01);

        analyticsTableGenerator.generateTables(null, null, null, false);

        // Set params
        Period y2017 = createPeriod("2017");
        ou_2017_params = DataQueryParams.newBuilder()
                .withOrganisationUnits(organisationUnitService.getAllOrganisationUnits())
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017)
                .build();

        Period y2017_jan = createPeriod("2017-01");
        ou_2017_01_params = DataQueryParams.newBuilder()
                .withOrganisationUnits(organisationUnitService.getAllOrganisationUnits())
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_jan)
                .build();


        Period y2017_feb = createPeriod("2017-02");
        ouB_2017_02_params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouB)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_feb)
                .build();

        Period y2017_mar = createPeriod("2017-03");
        de_avg_2017_03_params = DataQueryParams.newBuilder()
                .withDataElements(dataElements)
                .withAggregationType(AggregationType.AVERAGE)
                .withSkipRounding(true)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_mar)
                .build();

        List<DataElement> dataElementsC = new ArrayList<>();
        dataElementsC.add(deC);
        deC_ouB_2017_03_params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouB)
                .withDataElements(dataElementsC)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_mar)
                .build();


        List<OrganisationUnit> organisationUnits = new ArrayList<>();
        organisationUnits.add(ouB);
        organisationUnits.add(ouC);

        ouB_ouC_2017_02_params = DataQueryParams.newBuilder()
                .withFilterOrganisationUnits(organisationUnits)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_feb)
                .build();


        List<Period> periodsFilter = new ArrayList<>();
        periodsFilter.add(peJan);
        periodsFilter.add(peMar);

        ouA_2017_01_03_params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouA)
                .withFilterPeriods(periodsFilter)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .build();

        // Set results
        ou_2017_keyValue.put("ouabcdefghA-2017", 949.0);
        ou_2017_keyValue.put("ouabcdefghB-2017", 750.0);
        ou_2017_keyValue.put("ouabcdefghC-2017", 77.0);
        ou_2017_keyValue.put("ouabcdefghD-2017", 698.0);
        ou_2017_keyValue.put("ouabcdefghE-2017", 36.0);

        ou_2017_01_keyValue.put("ouabcdefghA-201701", 211.0);
        ou_2017_01_keyValue.put("ouabcdefghB-201701", 100.0);
        ou_2017_01_keyValue.put("ouabcdefghC-201701", 9.0);
        ou_2017_01_keyValue.put("ouabcdefghD-201701", 66.0);

        ouB_2017_02_keyValue.put("ouabcdefghB-201702", 636.00);

        de_avg_2017_03_keyValue.put("deabcdefghC-201703", 6.75);

        deC_ouB_2017_03_keyValue.put("deabcdefghC-ouabcdefghB-201703", 6.0);

        ouB_ouC_2017_02_keyValue.put("201702", 669.0);

        ouA_2017_01_03_keyValue.put("ouabcdefghA", 238.0);
    }

    @Override
    public boolean emptyDatabaseAfterTest() {
        return true;
    }


    @Test
    @Ignore
    public void testMappingAggregation() {
        // Params: Sum for all org units for 2017
        Map<String, Object> aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(ou_2017_params);

        assertDataValueMapping(aggregatedDataValueMapping, ou_2017_keyValue);

        // Params: Sum for all org units in period 2017-01
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(ou_2017_01_params);

        assertDataValueMapping(aggregatedDataValueMapping, ou_2017_01_keyValue);

        // Params: Sum for org unit B in period 2017-02
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(ouB_2017_02_params);

        assertEquals(1, aggregatedDataValueMapping.size());

        assertDataValueMapping(aggregatedDataValueMapping, ouB_2017_02_keyValue);

        // Params: Average for data elements in period for 2017-03
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(de_avg_2017_03_params);

        assertDataValueMapping(aggregatedDataValueMapping, de_avg_2017_03_keyValue);

        // Params: Sum for data element C in period 2017-03, with organisation unit B
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(deC_ouB_2017_03_params);

        assertDataValueMapping(aggregatedDataValueMapping, deC_ouB_2017_03_keyValue);

        // Params: Sum in period 2017-02, with filter org unit B and C
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(ouB_ouC_2017_02_params);

        assertDataValueMapping(aggregatedDataValueMapping, ouB_ouC_2017_02_keyValue);

        // Params: Count: filter periods 2017-01 2017-03, for org unit A (root)
        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(ouA_2017_01_03_params);

        assertDataValueMapping(aggregatedDataValueMapping, ouA_2017_01_03_keyValue);
    }

    @Test
    public void testGridAggregation()
    {
        // Params: Sum for all org units for 2017
        Grid aggregatedDataValueGrid =  analyticsService.getAggregatedDataValues(ou_2017_params);

        assertEquals("2017", aggregatedDataValueGrid.getRow(0).get(1));
        assertDataValueGrid(aggregatedDataValueGrid, ou_2017_keyValue);

        // Params: Sum for all org units in period 2017-01
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(ou_2017_01_params);

        assertDataValueGrid(aggregatedDataValueGrid, ou_2017_01_keyValue);

        // Params: Sum for org unit B in period 2017-02
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(ouB_2017_02_params);

        assertDataValueGrid(aggregatedDataValueGrid, ouB_2017_02_keyValue);

        // Params: Average for data elements in period for 2017-03
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(de_avg_2017_03_params);

        assertDataValueGrid(aggregatedDataValueGrid, de_avg_2017_03_keyValue);

        // Params: Sum for data element C in period 2017-03, with organisation unit B
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(deC_ouB_2017_03_params);

        assertDataValueGrid(aggregatedDataValueGrid, deC_ouB_2017_03_keyValue);

        // Params: Sum in period 2017-02, with filter org unit B and C
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(ouB_ouC_2017_02_params);

        assertDataValueGrid(aggregatedDataValueGrid, ouB_ouC_2017_02_keyValue);

        // Params: Count: filter periods 2017-01 2017-03, for org unit A (root)
        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(ouA_2017_01_03_params);

        assertDataValueGrid(aggregatedDataValueGrid, ouA_2017_01_03_keyValue);
    }


    //  -------------------------------------------------------------------------
    //  Internal Logic
    //  -------------------------------------------------------------------------


    /**
     * Configure org unit hierarchy like so:
     *
     *                   A
     *                 /  \
     *                B    C
     *              /  \
     *             D    E
     *
     * @param A root
     * @param B leftRoot
     * @param C rightRoot
     * @param D leftB
     * @param E rightB
     */
    public void configureHierarchy(OrganisationUnit A, OrganisationUnit B, OrganisationUnit C, OrganisationUnit D, OrganisationUnit E) {
        A.getChildren().addAll(Sets.newHashSet(B, C));
        B.setParent(A);
        C.setParent(A);

        B.getChildren().addAll(Sets.newHashSet(D, E));
        D.setParent(B);
        E.setParent(B);
    }

    /**
     * Test if values from keyValue corresponds with values in aggregatedDataValueMapping.
     * Also test for null values, and "" as key in aggregatedDataValueMapping
     *
     * @param aggregatedDataValueMapping aggregated values
     * @param keyValue expected results
     */
    public void assertDataValueMapping(Map<String, Object> aggregatedDataValueMapping, HashMap<String, Double> keyValue)
    {
        assertNotNull(aggregatedDataValueMapping);
        assertNull(aggregatedDataValueMapping.get("testNull"));
        assertNull(aggregatedDataValueMapping.get(""));

        for(Map.Entry<String, Double> entry : keyValue.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();

            assertNotNull(aggregatedDataValueMapping.get( key ));
            assertEquals( "'" + key + "' --",
                    value, aggregatedDataValueMapping.get( key ));
        }
    }

    /**
     * Test if values from keyValue corresponds with values in aggregatedDataValueMapping.
     * Also test for null values, and "" as key in aggregatedDataValueMapping
     *
     * @param aggregatedDataValueGrid aggregated values
     * @param keyValue expected results
     */
    public void assertDataValueGrid(Grid aggregatedDataValueGrid, HashMap<String, Double> keyValue)
    {
        assertNotNull(aggregatedDataValueGrid);

        String period = aggregatedDataValueGrid.getValue(0, 1).toString();
        for(int i=0; i<aggregatedDataValueGrid.getRows().size(); i++)
        {
            String key = aggregatedDataValueGrid.getValue(i, 0).toString();
            assertNotEquals("", key);

            Double value = keyValue.get( key + "-" + period);
            if(value != null)
            {
                assertNotNull(aggregatedDataValueGrid.getRow(i));
                assertEquals("'" + aggregatedDataValueGrid.getValue(i, 0) + "' --",
                        value, aggregatedDataValueGrid.getValue(i, 2));
            }
        }
    }
}
