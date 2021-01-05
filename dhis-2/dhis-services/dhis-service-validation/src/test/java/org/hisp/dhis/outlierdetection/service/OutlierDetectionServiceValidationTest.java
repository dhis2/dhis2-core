package org.hisp.dhis.outlierdetection.service;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
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
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class OutlierDetectionServiceValidationTest
{
    @Mock
    private IdentifiableObjectManager idObjectManager;

    @Mock
    private ZScoreOutlierDetectionManager outlierDetectionManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private OutlierDetectionService subject;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private DataElement deA;
    private DataElement deB;
    private DataElement deC;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

    @Before
    public void setUp()
    {
        subject = new DefaultOutlierDetectionService( idObjectManager, outlierDetectionManager );

        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deC = createDataElement( 'C', ValueType.NUMBER, AggregationType.SUM );

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
