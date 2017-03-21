package org.hisp.dhis.analytics.data;

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisTest;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.csv.CsvImportService;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;

    private Period peA;
    private Period peB;
    private Period peC;

    List<DataElement> dataElements = new ArrayList<>();

    private DataElementCategoryCombo categoryComboDef;
    private DataElementCategoryOptionCombo ocDef;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;

    private OrganisationUnitGroup ouGroupA;
    private OrganisationUnitGroup ouGroupB;
    private OrganisationUnitGroup ouGroupC;

    private DataElementGroup deGroupA;
    private DataElementGroup deGroupB;
    private DataElementGroup deGroupC;

    private DataElementGroupSet deGroupSetA;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataValueSetService datavalueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private AnalyticsTableService analyticsTableService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private AnalyticsManager analyticsManager;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserService internalUserService;

    @Autowired
    private CsvImportService csvImportService;

    /*
     * Configure org unit hierarchy like so:
     *
     *                   A
     *                 /   \
     *                B     C
     *               / \
     *              D   E
     */
    public void configureHierarchy(OrganisationUnit A, OrganisationUnit B, OrganisationUnit C, OrganisationUnit D, OrganisationUnit E)
    {
        A.getChildren().addAll( Sets.newHashSet( B, C ) );
        B.setParent( A );
        C.setParent( A );

        B.getChildren().addAll( Sets.newHashSet( D, E ) );
        D.setParent( B );
        E.setParent( B );
    }

    @Override
    public void setUpTest() {
        super.userService = internalUserService;

        categoryComboDef = categoryService.getDefaultDataElementCategoryCombo();

        ocDef = categoryService.getDefaultDataElementCategoryOptionCombo();
        ocDef.setCode( "OC_DEF_CODE" );
        categoryService.updateDataElementCategoryOptionCombo( ocDef );

        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2017, 1, 1 ), getDate( 2017, 1, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2017, 2, 1 ), getDate( 2017, 2, 28 ) );
        peC = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2017, 3, 1 ), getDate( 2017, 3, 31 ) );

        deA = createDataElement( 'A', categoryComboDef );
        deB = createDataElement( 'B', categoryComboDef );
        deC = createDataElement( 'C', categoryComboDef );
        deD = createDataElement( 'D', categoryComboDef );
        deE = createDataElement( 'E', categoryComboDef );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );


        ouA = createOrganisationUnit( 'A');
        ouB = createOrganisationUnit( 'B');
        ouC = createOrganisationUnit( 'C');
        ouD = createOrganisationUnit( 'D');
        ouE = createOrganisationUnit( 'E');
        configureHierarchy(ouA, ouB, ouC, ouD, ouE);

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );

        dataElements.add(deA);
        dataElements.add(deB);
        dataElements.add(deC);
        dataElements.add(deD);
    }

    @Test
    public void testSimpleAggregation()
    {
        // Data values for A = 20
        List<DataValue> dataValuesA = new ArrayList<>();
        for(int i=1; i<5; i++) {
            dataValuesA.add(new DataValue(dataElements.get(i-1), peA, ouA, ocDef, ocDef));
            dataValuesA.get(i-1).setValue(i*2 + "");
            dataValueService.addDataValue(dataValuesA.get(i-1));
        }

        // Data values for B = 16
        List<DataValue> dataValuesB = new ArrayList<>();
        for(int i=1; i<5; i++) {
            dataValuesB.add(new DataValue(dataElements.get(i-1), peA, ouB, ocDef, ocDef));
            dataValuesB.get(i-1).setValue(i*2-1 + "");
            dataValueService.addDataValue(dataValuesB.get(i-1));
        }

        // Data values for C = 50
        List<DataValue> dataValuesC = new ArrayList<>();
        for(int i=1; i<5; i++) {
            dataValuesC.add(new DataValue(dataElements.get(i-1), peA, ouC, ocDef, ocDef));
            dataValuesC.get(i-1).setValue(i*5 + "");
            dataValueService.addDataValue(dataValuesC.get(i-1));
        }

        // Data values for E = 4
        List<DataValue> dataValuesE = new ArrayList<>();
        for(int i=1; i<5; i++) {
            dataValuesE.add(new DataValue(dataElements.get(i-1), peA, ouE, ocDef, ocDef));
            dataValuesE.get(i-1).setValue( "1" );
            dataValueService.addDataValue(dataValuesE.get(i-1));
        }

        analyticsTableGenerator.generateTables(null, null, null, false);

        // Params: Sum for all org units for 2017
        Period y2017 = createPeriod( "2017" );
        DataQueryParams params = DataQueryParams.newBuilder()
                .withOrganisationUnits( organisationUnitService.getAllOrganisationUnits() )
                .withAggregationType(AggregationType.SUM)
                .withOutputFormat(OutputFormat.ANALYTICS)
                .withPeriod(y2017)
                .build();

        Map<String, Object> aggregatedDataValueMapping =  analyticsService.getAggregatedDataValueMapping(params);

        assertNotNull(aggregatedDataValueMapping);

        assertNotNull(aggregatedDataValueMapping.get( "ouabcdefghA-2017" ));
        assertEquals(90.0, aggregatedDataValueMapping.get( "ouabcdefghA-2017" ));

        assertNotNull(aggregatedDataValueMapping.get( "ouabcdefghB-2017" ));
        assertEquals(20.0, aggregatedDataValueMapping.get( "ouabcdefghB-2017" ));

        assertNotNull(aggregatedDataValueMapping.get( "ouabcdefghC-2017" ));
        assertEquals(50.0, aggregatedDataValueMapping.get( "ouabcdefghC-2017" ));

        assertNull(aggregatedDataValueMapping.get( "ouabcdefghD-2017" ));

        assertNotNull(aggregatedDataValueMapping.get( "ouabcdefghE-2017" ));
        assertEquals(4.0, aggregatedDataValueMapping.get( "ouabcdefghE-2017" ));
    }
}
