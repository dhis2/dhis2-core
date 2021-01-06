package org.hisp.dhis.outlierdetection.service;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class OutlierDetectionServiceMinMaxTest
    extends IntegrationTestBase
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MinMaxDataElementService minMaxService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private OutlierDetectionService subject;

    private DataElement deA;
    private DataElement deB;

    private Period m01, m02, m03, m04, m05, m06, m07, m08, m09, m10, m11, m12;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

    private CategoryOptionCombo coc;

    @Override
    public void setUpTest()
    {
        MonthlyPeriodType pt = new MonthlyPeriodType();

        m01 = pt.createPeriod( "202001" );
        m02 = pt.createPeriod( "202002" );
        m03 = pt.createPeriod( "202003" );
        m04 = pt.createPeriod( "202004" );
        m05 = pt.createPeriod( "202005" );
        m06 = pt.createPeriod( "202006" );
        m07 = pt.createPeriod( "202007" );
        m08 = pt.createPeriod( "202008" );
        m09 = pt.createPeriod( "202009" );
        m10 = pt.createPeriod( "202010" );
        m11 = pt.createPeriod( "202011" );
        m12 = pt.createPeriod( "202012" );

        periodService.addPeriod( m01 );
        periodService.addPeriod( m02 );
        periodService.addPeriod( m03 );
        periodService.addPeriod( m04 );
        periodService.addPeriod( m05 );
        periodService.addPeriod( m06 );
        periodService.addPeriod( m07 );
        periodService.addPeriod( m08 );
        periodService.addPeriod( m09 );
        periodService.addPeriod( m10 );
        periodService.addPeriod( m11 );
        periodService.addPeriod( m12 );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );

        idObjectManager.save( deA );
        idObjectManager.save( deB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );

        coc = categoryService.getDefaultCategoryOptionCombo();
    }

    @Test
    public void testGetFromQuery()
    {
        OutlierDetectionQuery query = new OutlierDetectionQuery();
        query.setDe( Lists.newArrayList( "deabcdefghA", "deabcdefghB" ) );
        query.setStartDate( getDate( 2020, 1, 1 ) );
        query.setEndDate( getDate( 2020, 6, 1 ) );
        query.setOu( Lists.newArrayList( "ouabcdefghA", "ouabcdefghB" ) );
        query.setAlgorithm( OutlierDetectionAlgorithm.MIN_MAX );
        query.setThreshold( 3.0 );
        query.setMaxResults( 200 );

        OutlierDetectionRequest request = subject.getFromQuery( query );

        assertEquals( 2, request.getDataElements().size() );
        assertEquals( 2, request.getOrgUnits().size() );
        assertEquals( getDate( 2020, 1, 1 ), request.getStartDate() );
        assertEquals( getDate( 2020, 6, 1 ), request.getEndDate() );
        assertEquals( OutlierDetectionAlgorithm.MIN_MAX, request.getAlgorithm() );
        assertEquals( 3.0, request.getThreshold(), DELTA );
        assertEquals( 200, request.getMaxResults() );
    }

    @Test
    public void testGetOutlierValues()
    {
        addDataValues(
            new DataValue( deA, m01, ouA, coc, coc, "50" ), new DataValue( deA, m07, ouA, coc, coc, "51" ),
            new DataValue( deA, m02, ouA, coc, coc, "53" ), new DataValue( deA, m08, ouA, coc, coc, "59" ),
            new DataValue( deA, m03, ouA, coc, coc, "58" ), new DataValue( deA, m09, ouA, coc, coc, "55" ),
            new DataValue( deA, m04, ouA, coc, coc, "55" ), new DataValue( deA, m10, ouA, coc, coc, "52" ),
            new DataValue( deA, m05, ouA, coc, coc, "51" ), new DataValue( deA, m11, ouA, coc, coc, "58" ),
            new DataValue( deA, m06, ouA, coc, coc, "12" ), new DataValue( deA, m12, ouA, coc, coc, "91" ),

            new DataValue( deB, m01, ouA, coc, coc, "41" ), new DataValue( deB, m02, ouA, coc, coc, "48" ),
            new DataValue( deB, m03, ouA, coc, coc, "45" ), new DataValue( deB, m04, ouA, coc, coc, "46" ),
            new DataValue( deB, m05, ouA, coc, coc, "49" ), new DataValue( deB, m06, ouA, coc, coc, "41" ),
            new DataValue( deB, m07, ouA, coc, coc, "41" ), new DataValue( deB, m08, ouA, coc, coc, "49" ),
            new DataValue( deB, m09, ouA, coc, coc, "42" ), new DataValue( deB, m10, ouA, coc, coc, "47" ),
            new DataValue( deB, m11, ouA, coc, coc, "11" ), new DataValue( deB, m12, ouA, coc, coc, "87" ) );

        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2021, 1, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA ) )
            .withAlgorithm( OutlierDetectionAlgorithm.MIN_MAX )
            .withThreshold( 2.0 ).build();

        OutlierDetectionResponse response = subject.getOutlierValues( request );

        // assertEquals( 4, response.getOutlierValues().size() );
    }

    private void addMinMaxValues( MinMaxDataElement minMaxValues )
    {
        Stream.of( minMaxValues ).forEach( mv -> minMaxService.addMinMaxDataElement( mv ) );
    }

    private void addDataValues( DataValue... dataValues )
    {
        Stream.of( dataValues ).forEach( dv -> dataValueService.addDataValue( dv ) );
    }
}
