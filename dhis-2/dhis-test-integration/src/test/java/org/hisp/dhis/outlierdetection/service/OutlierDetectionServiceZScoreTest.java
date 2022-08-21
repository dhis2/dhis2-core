/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.outlierdetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionQuery;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierDetectionResponse;
import org.hisp.dhis.outlierdetection.OutlierDetectionService;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.math.StatsAccumulator;

/**
 * @author Lars Helge Overland
 */
class OutlierDetectionServiceZScoreTest extends NonTransactionalIntegrationTest
{

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

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
        addPeriods( m01, m02, m03, m04, m05, m06, m07, m08, m09, m10, m11, m12 );
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
    void testGetFromQuery()
    {
        OutlierDetectionQuery query = new OutlierDetectionQuery();
        query.setDe( Lists.newArrayList( "deabcdefghA", "deabcdefghB" ) );
        query.setStartDate( getDate( 2020, 1, 1 ) );
        query.setEndDate( getDate( 2020, 6, 1 ) );
        query.setOu( Lists.newArrayList( "ouabcdefghA", "ouabcdefghB" ) );
        query.setAlgorithm( OutlierDetectionAlgorithm.Z_SCORE );
        query.setThreshold( 2.5 );
        query.setMaxResults( 100 );
        OutlierDetectionRequest request = subject.getFromQuery( query );
        assertEquals( 2, request.getDataElements().size() );
        assertEquals( 2, request.getOrgUnits().size() );
        assertEquals( getDate( 2020, 1, 1 ), request.getStartDate() );
        assertEquals( getDate( 2020, 6, 1 ), request.getEndDate() );
        assertEquals( OutlierDetectionAlgorithm.Z_SCORE, request.getAlgorithm() );
        assertEquals( request.getThreshold(), DELTA, 2.5 );
        assertEquals( 100, request.getMaxResults() );
    }

    @Test
    void testGetOutlierValues()
    {
        // 12, 91, 11, 87 are outlier values with a z-score above 2.0
        addDataValues( new DataValue( deA, m01, ouA, coc, coc, "50" ), new DataValue( deA, m07, ouA, coc, coc, "51" ),
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
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2021, 1, 1 ) ).withOrgUnits( Lists.newArrayList( ouA ) )
            .withAlgorithm( OutlierDetectionAlgorithm.Z_SCORE ).withThreshold( 2.0 ).build();
        OutlierDetectionResponse response = subject.getOutlierValues( request );
        assertEquals( 4, response.getOutlierValues().size() );
        assertContainsOutlierValue( response, 12d );
        assertContainsOutlierValue( response, 91d );
        assertContainsOutlierValue( response, 11d );
        assertContainsOutlierValue( response, 87d );
    }

    @Test
    void testGetOutlierValuesWithDataStartEndDate()
    {
        // 12, 91 are outlier values with a z-score above 2.0
        addDataValues( new DataValue( deA, m01, ouA, coc, coc, "50" ), new DataValue( deA, m07, ouA, coc, coc, "51" ),
            new DataValue( deA, m02, ouA, coc, coc, "53" ), new DataValue( deA, m08, ouA, coc, coc, "59" ),
            new DataValue( deA, m03, ouA, coc, coc, "58" ), new DataValue( deA, m09, ouA, coc, coc, "55" ),
            new DataValue( deA, m04, ouA, coc, coc, "55" ), new DataValue( deA, m10, ouA, coc, coc, "52" ),
            new DataValue( deA, m05, ouA, coc, coc, "51" ), new DataValue( deA, m11, ouA, coc, coc, "58" ),
            new DataValue( deA, m06, ouA, coc, coc, "12" ), new DataValue( deA, m12, ouA, coc, coc, "91" ) );
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2021, 1, 1 ) ).withOrgUnits( Lists.newArrayList( ouA ) )
            .withAlgorithm( OutlierDetectionAlgorithm.Z_SCORE ).withThreshold( 2.0 )
            .withDataStartDate( getDate( 2019, 1, 1 ) ).withDataEndDate( getDate( 2020, 6, 1 ) ).build();
        OutlierDetectionResponse response = subject.getOutlierValues( request );
        assertEquals( 2, response.getOutlierValues().size() );
        assertContainsOutlierValue( response, 12d );
        assertContainsOutlierValue( response, 91d );
    }

    @Test
    void testGetOutlierValuesAsCsv()
        throws IOException
    {
        // 12, 91, 11, 87 are outlier values with a z-score above 2.0
        addDataValues( new DataValue( deA, m01, ouA, coc, coc, "50" ), new DataValue( deA, m07, ouA, coc, coc, "51" ),
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
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2021, 1, 1 ) ).withOrgUnits( Lists.newArrayList( ouA ) )
            .withAlgorithm( OutlierDetectionAlgorithm.Z_SCORE ).withThreshold( 2.0 ).build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        subject.getOutlierValuesAsCsv( request, out );
        List<String> csvLines = TextUtils.toLines( new String( out.toByteArray(), StandardCharsets.UTF_8 ) );
        final int endIndex = 61;
        assertEquals( 5, csvLines.size() );
        assertEquals( "de,deName,pe,ou,ouName,coc,cocName,aoc,lastUpdated,value,mean",
            csvLines.get( 0 ).substring( 0, endIndex ) );
        assertEquals( "deabcdefghA,DataElementA,202006,ouabcdefghA,OrganisationUnitA",
            csvLines.get( 1 ).substring( 0, endIndex ) );
        assertEquals( "deabcdefghB,DataElementB,202012,ouabcdefghA,OrganisationUnitA",
            csvLines.get( 2 ).substring( 0, endIndex ) );
        assertEquals( "deabcdefghA,DataElementA,202012,ouabcdefghA,OrganisationUnitA",
            csvLines.get( 3 ).substring( 0, endIndex ) );
        assertEquals( "deabcdefghB,DataElementB,202011,ouabcdefghA,OrganisationUnitA",
            csvLines.get( 4 ).substring( 0, endIndex ) );
    }

    @Test
    void testGetOutlierValue()
    {
        StatsAccumulator stats = new StatsAccumulator();
        stats.addAll( 31, 34, 38, 81, 39, 33 );
        double outlierValue = 81d;
        double threshold = 2.0;
        double mean = stats.mean();
        double stdDev = stats.populationStandardDeviation();
        double zScore = Math.abs( outlierValue - mean ) / stdDev;
        double meanAbsDev = Math.abs( outlierValue - mean );
        double lowerBound = mean - (stdDev * threshold);
        double upperBound = mean + (stdDev * threshold);
        assertEquals( mean, DELTA, 42.666 );
        assertEquals( stdDev, DELTA, 17.365 );
        assertEquals( zScore, DELTA, 2.207 );
        assertEquals( meanAbsDev, DELTA, 38.333 );
        assertEquals( lowerBound, DELTA, 7.936 );
        assertEquals( upperBound, DELTA, 77.397 );
        addDataValues( new DataValue( deA, m01, ouA, coc, coc, "31" ), new DataValue( deA, m02, ouA, coc, coc, "34" ),
            new DataValue( deA, m03, ouA, coc, coc, "38" ), new DataValue( deA, m04, ouA, coc, coc, "81" ),
            new DataValue( deA, m05, ouA, coc, coc, "39" ), new DataValue( deA, m06, ouA, coc, coc, "33" ) );
        OutlierDetectionRequest request = new OutlierDetectionRequest.Builder()
            .withDataElements( Lists.newArrayList( deA, deB ) )
            .withStartEndDate( getDate( 2020, 1, 1 ), getDate( 2021, 1, 1 ) ).withOrgUnits( Lists.newArrayList( ouA ) )
            .withAlgorithm( OutlierDetectionAlgorithm.Z_SCORE ).withThreshold( threshold ).build();
        OutlierDetectionResponse response = subject.getOutlierValues( request );
        assertEquals( 1, response.getOutlierValues().size() );
        OutlierValue outlier = response.getOutlierValues().get( 0 );
        assertEquals( deA.getUid(), outlier.getDe() );
        assertEquals( ouA.getUid(), outlier.getOu() );
        assertEquals( m04.getIsoDate(), outlier.getPe() );
        assertEquals( outlier.getValue(), DELTA, outlierValue );
        assertEquals( outlier.getMean(), DELTA, mean );
        assertEquals( outlier.getStdDev(), DELTA, stdDev );
        assertEquals( outlier.getZScore(), DELTA, zScore );
        assertEquals( outlier.getAbsDev(), DELTA, meanAbsDev );
        assertEquals( outlier.getLowerBound(), DELTA, lowerBound );
        assertEquals( outlier.getUpperBound(), DELTA, upperBound );
        assertFalse( outlier.getFollowup() );
    }

    private void assertContainsOutlierValue( OutlierDetectionResponse response, Double value )
    {
        assertTrue( response.getOutlierValues().stream().anyMatch( ov -> MathUtils.isEqual( ov.getValue(), value ) ) );
    }

    private void addPeriods( Period... periods )
    {
        Stream.of( periods ).forEach( periodService::addPeriod );
    }

    private void addDataValues( DataValue... dataValues )
    {
        Stream.of( dataValues ).forEach( dataValueService::addDataValue );
    }
}
