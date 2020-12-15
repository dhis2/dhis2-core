package org.hisp.dhis.outlierdetection.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class OutlierDetectionServiceTest
    extends IntegrationTestBase
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OutlierDetectionService subject;

    private DataElement deA;
    private DataElement deB;

    private Period m01;
    private Period m02;
    private Period m03;
    private Period m04;
    private Period m05;
    private Period m06;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

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

        periodService.addPeriod( m01 );
        periodService.addPeriod( m02 );
        periodService.addPeriod( m03 );
        periodService.addPeriod( m04 );
        periodService.addPeriod( m05 );
        periodService.addPeriod( m06 );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );

        idObjectManager.save( deA );
        idObjectManager.save( deB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
    }

    @Test
    public void testFromQuery()
    {
        OutlierDetectionQuery query = new OutlierDetectionQuery();
        query.setDe( Lists.newArrayList( "deabcdefghA", "deabcdefghB" ) );
        query.setStartDate( getDate( 2020, 1, 1 ) );
        query.setEndDate( getDate( 2020, 6, 1 ) );
        query.setOu( Lists.newArrayList( "ouabcdefghA", "ouabcdefghB" ) );
        query.setThreshold( 2.5 );
        query.setMaxResults( 100 );

        OutlierDetectionRequest request = subject.fromQuery( query );
        assertEquals( 2, request.getDataElements().size() );
        assertEquals( 2, request.getOrgUnits().size() );
        assertEquals( getDate( 2020, 1, 1 ), request.getStartDate() );
        assertEquals( getDate( 2020, 6, 1 ), request.getEndDate() );
        assertNotNull( request.getThreshold() );
        assertEquals( 100, request.getMaxResults() );
    }

}
