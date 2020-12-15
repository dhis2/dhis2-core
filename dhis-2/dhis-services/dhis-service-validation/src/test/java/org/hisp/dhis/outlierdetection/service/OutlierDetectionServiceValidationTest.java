package org.hisp.dhis.outlierdetection.service;

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.PeriodType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import com.google.common.collect.Lists;

public class OutlierDetectionServiceValidationTest
{
    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private OutlierDetectionManager outlierDetectionManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private OutlierDetectionService subject;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;

    private DataSet dsA;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

    @Before
    public void setUp()
    {
        subject = new DefaultOutlierDetectionService( idObjectManager, outlierDetectionManager );

        PeriodType pt = new MonthlyPeriodType();

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deC = createDataElement( 'C', ValueType.NUMBER, AggregationType.SUM );

        dsA = createDataSet( 'A', pt );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        dsA.addDataSetElement( deC );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
    }

    @Test
    public void testSuccessfulValidation()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2020, 3, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA, ouB ) )
            .build();

        assertNull( subject.validateForErrorMessage( request ) );
    }

    @Test
    public void testErrorValidation()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2020, 3, 1 ) )
            .build();

        IllegalQueryException ex = assertThrows( IllegalQueryException.class, () -> subject.validate( request ) );
        assertEquals( ErrorCode.E2203, ex.getErrorCode() );
    }

    @Test
    public void testErrorNoDataElements()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2020, 7, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA, ouB ) )
            .build();

        assertEquals( ErrorCode.E2200, subject.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void testErrorStartAfterEndDates()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withStartEndDate( getDate( 2020, 6, 1 ), getDate( 2020, 3, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA, ouB ) )
            .build();

        assertEquals( ErrorCode.E2202, subject.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void testErrorNegativeThreshold()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2020, 6, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA, ouB ) )
            .withThreshold( -23.4 )
            .build();

        assertEquals( ErrorCode.E2204, subject.validateForErrorMessage( request ).getErrorCode() );
    }

    @Test
    public void testErrorNegativeMaxResults()
    {
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2020, 3, 1 ) )
            .withOrgUnits( Lists.newArrayList( ouA, ouB ) )
            .withMaxResults( -100 )
            .build();

        assertEquals( ErrorCode.E2205, subject.validateForErrorMessage( request ).getErrorCode() );
    }
}
