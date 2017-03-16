package org.hisp.dhis.analytics.data;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;

import static org.hisp.dhis.common.DimensionalObjectUtils.getList;

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

    private DataElementCategoryCombo categoryComboDef;
    private DataElementCategoryOptionCombo ocDef;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;
    private OrganisationUnit ouC;
    private OrganisationUnit ouD;
    private OrganisationUnit ouE;
    private OrganisationUnit ouF;
    private OrganisationUnit ouG;
    private OrganisationUnit ouH;
    private OrganisationUnit ouI;
    private OrganisationUnit ouJ;

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
    private StatementBuilder statementBuilder;

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
        deA.setDisplayName("A");
        deA.setUid("testA");
        deB = createDataElement( 'B', categoryComboDef );
        deB.setDisplayName("B");
        deB.setUid("testB");
        deC = createDataElement( 'C', categoryComboDef );
        deC.setDisplayName("C");
        deC.setUid("testC");
        deD = createDataElement( 'D', categoryComboDef );
        deD.setDisplayName("D");
        deD.setUid("testD");
        deE = createDataElement( 'E', categoryComboDef );
        deE.setDisplayName("E");
        deE.setUid("testE");

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
        ouF = createOrganisationUnit( 'F');
        ouG = createOrganisationUnit( 'G');
        ouH = createOrganisationUnit( 'H');
        ouI = createOrganisationUnit( 'I');
        ouJ = createOrganisationUnit( 'j');

        HashSet<OrganisationUnit> childOfA = new HashSet<>();
        childOfA.add(ouB);
        childOfA.add(ouC);

        ouA.setChildren(childOfA);

        HashSet<OrganisationUnit> childOfB = new HashSet<>();
        childOfB.add(ouD);
        childOfB.add(ouE);

        ouB.setChildren(childOfB);

        HashSet<OrganisationUnit> childOfC = new HashSet<>();
        childOfC.add(ouG);

        ouC.setChildren(childOfC);

        HashSet<OrganisationUnit> childOfD = new HashSet<>();
        childOfD.add(ouF);
        childOfD.add(ouJ);

        ouD.setChildren(childOfD);

        HashSet<OrganisationUnit> childOfG = new HashSet<>();
        childOfG.add(ouI);
        childOfG.add(ouH);

        ouG.setChildren(childOfG);

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );


    }

    @Test
    public void testAnalyticsTable()
    {
        /*InputStream in = null;
        try {
            in = new ClassPathResource( "csv/dataValueSetA.csv" ).getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImportSummary summary = dataValueSetService.saveDataValueSetCsv( in, null, null );*/
        //mer avanserte tester -> Groupset
        // Statementbuilder

        DataValue dataValue = new DataValue( deA, peA, ouA, ocDef, ocDef );
        dataValue.setValue("1");
        dataValueService.addDataValue(dataValue);

        DataValue dataValue1 = new DataValue( deA, peA, ouA, ocDef, ocDef );
        dataValue1.setValue("2");
        dataValueService.addDataValue(dataValue);

        for(DataValue dv: dataValueService.getAllDataValues()) {
            System.out.println(dv);
        }

        System.out.println("test statementBuilder.getAnalyze: '" + statementBuilder.getAnalyze("test") + "'");

        analyticsTableGenerator.generateTables(null, null, null, false);


        // Param to test analytics service
        Period y2017 = createPeriod( "2017" );
        DataQueryParams params = DataQueryParams.newBuilder()
                .withDataElements( getList( createDataElement( 'A' ), createDataElement( 'B' ) ) )
                .withPeriods( getList( y2017 ) )
                .withOrganisationUnits( getList( createOrganisationUnit( 'A' ) ) )
                .withDataPeriodType( new YearlyPeriodType() )
                .withAggregationType( AggregationType.AVERAGE_SUM_INT_DISAGGREGATION ).build();
    }
}
