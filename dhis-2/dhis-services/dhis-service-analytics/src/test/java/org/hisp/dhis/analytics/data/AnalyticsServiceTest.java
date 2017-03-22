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
    private PeriodService periodService;

    @Autowired
    private AnalyticsService analyticsService;

    /*
     * Configure org unit hierarchy like so:
     *
     *                   A
     *                 /   \
     *                B     C
     *               / \
     *              D   E
     */
    public void configureHierarchy(OrganisationUnit A, OrganisationUnit B, OrganisationUnit C, OrganisationUnit D, OrganisationUnit E) {
        A.getChildren().addAll(Sets.newHashSet(B, C));
        B.setParent(A);
        C.setParent(A);

        B.getChildren().addAll(Sets.newHashSet(D, E));
        D.setParent(B);
        E.setParent(B);
    }

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

        periodService.addPeriod( peJan );
        periodService.addPeriod( peFeb );
        periodService.addPeriod( peMar );
        periodService.addPeriod( peApril );

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
    }

    @Override
    public boolean emptyDatabaseAfterTest() {
        return true;
    }


    @Test
    @Ignore
    public void testMappingAggregation() {
        // Params: Sum for all org units for 2017
        Period y2017 = createPeriod("2017");
        DataQueryParams params = DataQueryParams.newBuilder()
                .withOrganisationUnits(organisationUnitService.getAllOrganisationUnits())
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017)
                .build();

        Map<String, Object> aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);
        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghA-2017"));
        assertEquals(949.0, aggregatedDataValueMapping.get("ouabcdefghA-2017"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghB-2017"));
        assertEquals(750.0, aggregatedDataValueMapping.get("ouabcdefghB-2017"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghC-2017"));
        assertEquals(77.0, aggregatedDataValueMapping.get("ouabcdefghC-2017"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghD-2017"));
        assertEquals(698.0, aggregatedDataValueMapping.get("ouabcdefghD-2017"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghE-2017"));
        assertEquals(36.0, aggregatedDataValueMapping.get("ouabcdefghE-2017"));

        assertNull(aggregatedDataValueMapping.get("testNull"));


        // Params: Sum for all org units in period 2017-01
        Period y2017_jan = createPeriod("2017-01");
        params = DataQueryParams.newBuilder()
                .withOrganisationUnits(organisationUnitService.getAllOrganisationUnits())
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_jan)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghA-201701"));
        assertEquals(211.0, aggregatedDataValueMapping.get("ouabcdefghA-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghB-201701"));
        assertEquals(100.0, aggregatedDataValueMapping.get("ouabcdefghB-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghC-201701"));
        assertEquals(9.0, aggregatedDataValueMapping.get("ouabcdefghC-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghD-201701"));
        assertEquals(66.0, aggregatedDataValueMapping.get("ouabcdefghD-201701"));

        // Params: Sum for org unit B in period 2017-02
        Period y2017_feb = createPeriod("2017-02");
        params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouB)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_feb)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertEquals(1, aggregatedDataValueMapping.size());

        assertNull(aggregatedDataValueMapping.get("ouabcdefghA-201702"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghB-201702"));
        assertEquals(636.0, aggregatedDataValueMapping.get("ouabcdefghB-201702"));

        // Params: Average for data elements in period for 2017-03
        Period y2017_mar = createPeriod("2017-03");
        params = DataQueryParams.newBuilder()
                .withDataElements(dataElements)
                .withAggregationType(AggregationType.AVERAGE)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_mar)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("deabcdefghC-201703"));
        assertEquals(6.8, aggregatedDataValueMapping.get("deabcdefghC-201703"));

        // Params: Sum for data element C in period 2017-03, with organisation unit B
        List<DataElement> dataElementsC = new ArrayList<>();
        dataElementsC.add(deC);
        params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouB)
                .withDataElements(dataElementsC)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_mar)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("deabcdefghC-ouabcdefghB-201703"));
        assertEquals(6.0, aggregatedDataValueMapping.get("deabcdefghC-ouabcdefghB-201703"));

        // Params: Sum in period 2017-02, with filter org unit B and C
        List<OrganisationUnit> organisationUnits = new ArrayList<>();
        organisationUnits.add(ouB);
        organisationUnits.add(ouC);

        params = DataQueryParams.newBuilder()
                .withFilterOrganisationUnits(organisationUnits)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_feb)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("201702"));
        assertEquals(669.0, aggregatedDataValueMapping.get("201702"));

        // Params: Count: filter periods 2017-01 2017-03, for org unit A (root)
        List<Period> periodsFilter = new ArrayList<>();
        periodsFilter.add(peJan);
        periodsFilter.add(peMar);

        params = DataQueryParams.newBuilder()
                .withOrganisationUnit(ouA)
                .withFilterPeriods(periodsFilter)
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .build();

        aggregatedDataValueMapping = analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghA"));
        assertEquals(238.0, aggregatedDataValueMapping.get("ouabcdefghA"));
    }

    @Test
    public void testGridAggregation()
    {
        // Params: Sum for all org units for 2017
        Period y2017 = createPeriod( "2017" );
        DataQueryParams params = DataQueryParams.newBuilder()
                .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017)
                .build();

        Grid aggregatedDataValueGrid =  analyticsService.getAggregatedDataValues(params);

        System.out.println(aggregatedDataValueGrid);
        assertNotNull(aggregatedDataValueGrid);

        HashMap<String, Double> gridOrgValue = new HashMap<>();
        gridOrgValue.put("ouabcdefghA", 949.0);
        gridOrgValue.put("ouabcdefghB", 750.0);
        gridOrgValue.put("ouabcdefghC", 77.0);
        gridOrgValue.put("ouabcdefghD", 698.0);
        gridOrgValue.put("ouabcdefghE", 36.0);

        assertEquals("2017", aggregatedDataValueGrid.getRow(0).get(1));

        assertNotNull(aggregatedDataValueGrid.getRow(0));
        assertEquals(gridOrgValue.get(aggregatedDataValueGrid.getValue(0, 0)), aggregatedDataValueGrid.getValue(0, 2));

        assertNotNull(aggregatedDataValueGrid.getRow(1));
        assertEquals(gridOrgValue.get(aggregatedDataValueGrid.getValue(1, 0)), aggregatedDataValueGrid.getValue(1, 2));

        assertNotNull(aggregatedDataValueGrid.getRow(2));
        assertEquals(gridOrgValue.get(aggregatedDataValueGrid.getValue(2, 0)), aggregatedDataValueGrid.getValue(2, 2));

        assertNotNull(aggregatedDataValueGrid.getRow(3));
        assertEquals(gridOrgValue.get(aggregatedDataValueGrid.getValue(3, 0)), aggregatedDataValueGrid.getValue(3, 2));

        assertNotNull(aggregatedDataValueGrid.getRow(4));
        assertEquals(gridOrgValue.get(aggregatedDataValueGrid.getValue(4, 0)), aggregatedDataValueGrid.getValue(4, 2));

        // Params: Sum for all org units in period 2017-01
        Period y2017_jan = createPeriod("2017-01");
        params = DataQueryParams.newBuilder()
                .withOrganisationUnits(organisationUnitService.getAllOrganisationUnits())
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017_jan)
                .build();

        aggregatedDataValueGrid = analyticsService.getAggregatedDataValues(params);

        assertNotNull(aggregatedDataValueGrid);
        System.out.println(aggregatedDataValueGrid);

        /*assertNotNull(aggregatedDataValueMapping.get("ouabcdefghA-201701"));
        assertEquals(211.0, aggregatedDataValueMapping.get("ouabcdefghA-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghB-201701"));
        assertEquals(100.0, aggregatedDataValueMapping.get("ouabcdefghB-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghC-201701"));
        assertEquals(9.0, aggregatedDataValueMapping.get("ouabcdefghC-201701"));

        assertNotNull(aggregatedDataValueMapping.get("ouabcdefghD-201701"));
        assertEquals(66.0, aggregatedDataValueMapping.get("ouabcdefghD-201701"));*/
    }

}
