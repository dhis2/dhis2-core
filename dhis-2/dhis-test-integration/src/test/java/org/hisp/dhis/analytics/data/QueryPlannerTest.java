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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.DimensionalObjectUtils.getList;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.DimensionItem;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionItemObjectValue;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class QueryPlannerTest extends TransactionalIntegrationTest
{
    private static final AnalyticsTableType ANALYTICS_TABLE_TYPE = AnalyticsTableType.DATA_VALUE;

    @Autowired
    private QueryPlanner queryPlanner;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private PeriodType monthly = new MonthlyPeriodType();

    private PeriodType yearly = new YearlyPeriodType();

    private IndicatorType itA;

    private Indicator inA;

    private Indicator inB;

    private Program prA;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deE;

    private DataElement deF;

    private DataElement deG;

    private DataElement deH;

    private DataElement deI;

    private DataElement deJ;

    private DataElement deK;

    private ReportingRate rrA;

    private ReportingRate rrB;

    private ReportingRate rrC;

    private ReportingRate rrD;

    private CategoryCombo cc;

    private CategoryOptionCombo coc;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnit ouD;

    private OrganisationUnit ouE;

    private DataElementGroup degA;

    private DataElementGroup degB;

    private DataElementGroup degC;

    private DataElementGroupSet dgsA;

    private DataElementGroupSet dgsB;

    @Override
    public void setUpTest()
    {
        itA = createIndicatorType( 'A' );
        idObjectManager.save( itA );
        inA = createIndicator( 'A', itA );
        inB = createIndicator( 'B', itA );
        idObjectManager.save( inA );
        idObjectManager.save( inB );
        prA = createProgram( 'A' );
        idObjectManager.save( prA );
        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deE = createDataElement( 'E', ValueType.TEXT, AggregationType.NONE );
        deF = createDataElement( 'F', ValueType.TEXT, AggregationType.NONE );
        deG = createDataElement( 'G', ValueType.INTEGER, AggregationType.SUM );
        deH = createDataElement( 'H', ValueType.INTEGER, AggregationType.SUM );
        deI = createDataElement( 'I', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deJ = createDataElement( 'J', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        deK = createDataElement( 'K', ValueType.INTEGER, AggregationType.AVERAGE_SUM_ORG_UNIT );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deE );
        dataElementService.addDataElement( deF );
        dataElementService.addDataElement( deG );
        dataElementService.addDataElement( deH );
        dataElementService.addDataElement( deI );
        dataElementService.addDataElement( deJ );
        dataElementService.addDataElement( deK );
        DataSet dsA = createDataSet( 'A', monthly );
        DataSet dsB = createDataSet( 'B', monthly );
        DataSet dsC = createDataSet( 'C', yearly );
        DataSet dsD = createDataSet( 'D', yearly );
        dsC.addDataSetElement( deI, cc );
        dsC.addDataSetElement( deJ, cc );
        dsC.addDataSetElement( deK, cc );
        dataSetService.addDataSet( dsA );
        dataSetService.addDataSet( dsB );
        dataSetService.addDataSet( dsC );
        dataSetService.addDataSet( dsD );
        rrA = new ReportingRate( dsA );
        rrB = new ReportingRate( dsB );
        rrC = new ReportingRate( dsC );
        rrD = new ReportingRate( dsD );
        cc = categoryService.getDefaultCategoryCombo();
        coc = categoryService.getDefaultCategoryOptionCombo();
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );
        ouD = createOrganisationUnit( 'D' );
        ouE = createOrganisationUnit( 'E' );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        organisationUnitService.addOrganisationUnit( ouD );
        organisationUnitService.addOrganisationUnit( ouE );
        degA = createDataElementGroup( 'A' );
        degA.addDataElement( deA );
        degA.addDataElement( deB );
        degB = createDataElementGroup( 'B' );
        degB.addDataElement( deI );
        degB.addDataElement( deJ );
        degC = createDataElementGroup( 'C' );
        degC.addDataElement( deK );
        dataElementService.addDataElementGroup( degA );
        dataElementService.addDataElementGroup( degB );
        dataElementService.addDataElementGroup( degC );
        dgsA = createDataElementGroupSet( 'A' );
        dgsA.getMembers().add( degA );
        dgsB = createDataElementGroupSet( 'B' );
        dgsB.getMembers().add( degB );
        dgsB.getMembers().add( degC );
        dataElementService.addDataElementGroupSet( dgsA );
        dataElementService.addDataElementGroupSet( dgsB );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testSetGetCopy()
    {
        List<DimensionalItemObject> desA = getList( deA, deB );
        List<DimensionalItemObject> ousA = getList( ouA, ouB );
        List<DimensionalItemObject> ousB = getList( ouC, ouD );
        List<DimensionalItemObject> pesA = getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ) );
        List<DimensionalItemObject> pesB = getList( createPeriod( "200001" ), createPeriod( "200002" ) );
        DataQueryParams paramsA = DataQueryParams.newBuilder().withDataElements( desA ).withOrganisationUnits( ousA )
            .withPeriods( pesA ).build();
        DataQueryParams paramsB = DataQueryParams.newBuilder( paramsA ).withOrganisationUnits( ousB )
            .withPeriods( pesB ).build();
        assertEquals( desA, paramsA.getDataElements() );
        assertEquals( ousA, paramsA.getOrganisationUnits() );
        assertEquals( pesA, paramsA.getPeriods() );
        assertEquals( desA, paramsB.getDataElements() );
        assertEquals( ousB, paramsB.getOrganisationUnits() );
        assertEquals( pesB, paramsB.getPeriods() );
    }

    private String makeKey( DataElement de, CategoryOptionCombo coc, OrganisationUnit ou, String period )
    {
        return de.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + coc.getUid() + DIMENSION_SEP + ou.getUid() + DIMENSION_SEP
            + period;
    }

    @Test
    void testGetPermutationDimensionalItemValueMapCocEnabled()
    {
        MultiValuedMap<String, DimensionItemObjectValue> aggregatedDataMap = new ArrayListValuedHashMap<>();
        aggregatedDataMap.put( makeKey( deA, coc, ouA, "2000Q1" ), new DimensionItemObjectValue( deA, 1d ) );
        aggregatedDataMap.put( makeKey( deA, coc, ouA, "2000Q2" ), new DimensionItemObjectValue( deA, 2d ) );
        aggregatedDataMap.put( makeKey( deA, coc, ouB, "2000Q1" ), new DimensionItemObjectValue( deA, 3d ) );
        aggregatedDataMap.put( makeKey( deA, coc, ouB, "2000Q2" ), new DimensionItemObjectValue( deA, 4d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouA, "2000Q1" ), new DimensionItemObjectValue( deB, 5d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouA, "2000Q2" ), new DimensionItemObjectValue( deB, 6d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouB, "2000Q1" ), new DimensionItemObjectValue( deB, 7d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouB, "2000Q2" ), new DimensionItemObjectValue( deB, 8d ) );
        // Method under test //
        Map<String, List<DimensionItemObjectValue>> permutationMap = DataQueryParams
            .getPermutationDimensionalItemValueMap( aggregatedDataMap );
        assertNotNull( permutationMap );
        String ouAQ1Key = ouA.getUid() + DIMENSION_SEP + "2000Q1";
        String ouAQ2Key = ouA.getUid() + DIMENSION_SEP + "2000Q2";
        String ouBQ1Key = ouB.getUid() + DIMENSION_SEP + "2000Q1";
        String ouBQ2Key = ouB.getUid() + DIMENSION_SEP + "2000Q2";
        List<DimensionItemObjectValue> ouAQ1 = permutationMap.get( ouAQ1Key );
        List<DimensionItemObjectValue> ouAQ2 = permutationMap.get( ouAQ2Key );
        List<DimensionItemObjectValue> ouBQ1 = permutationMap.get( ouBQ1Key );
        List<DimensionItemObjectValue> ouBQ2 = permutationMap.get( ouBQ2Key );
        assertEquals( 2, ouAQ1.size() );
        assertEquals( 2, ouAQ2.size() );
        assertEquals( 2, ouBQ1.size() );
        assertEquals( 2, ouBQ2.size() );
        List<DimensionItemObjectValue> ouAQ1Expected = new ArrayList<>();
        ouAQ1Expected.add( new DimensionItemObjectValue( deA, 1d ) );
        ouAQ1Expected.add( new DimensionItemObjectValue( deB, 5d ) );
        List<DimensionItemObjectValue> ouAQ2Expected = new ArrayList<>();
        ouAQ2Expected.add( new DimensionItemObjectValue( deA, 2d ) );
        ouAQ2Expected.add( new DimensionItemObjectValue( deB, 6d ) );
        List<DimensionItemObjectValue> ouBQ1Expected = new ArrayList<>();
        ouBQ1Expected.add( new DimensionItemObjectValue( deA, 3d ) );
        ouBQ1Expected.add( new DimensionItemObjectValue( deB, 7d ) );
        List<DimensionItemObjectValue> ouBQ2Expected = new ArrayList<>();
        ouBQ2Expected.add( new DimensionItemObjectValue( deA, 4d ) );
        ouBQ2Expected.add( new DimensionItemObjectValue( deB, 8d ) );
        assertCollectionsMatch( ouAQ1Expected, ouAQ1 );
        assertCollectionsMatch( ouAQ2Expected, ouAQ2 );
        assertCollectionsMatch( ouBQ1Expected, ouBQ1 );
        assertCollectionsMatch( ouBQ2Expected, ouBQ2 );
    }

    @Test
    void testGetPermutationDimensionalItemValueMapCocDisabled()
    {
        MultiValuedMap<String, DimensionItemObjectValue> aggregatedDataMap = new ArrayListValuedHashMap<>();
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "200101",
            new DimensionItemObjectValue( deA, 1d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "200102",
            new DimensionItemObjectValue( deA, 2d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "200101",
            new DimensionItemObjectValue( deA, 3d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "200102",
            new DimensionItemObjectValue( deA, 4d ) );
        aggregatedDataMap.put( deB.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "200101",
            new DimensionItemObjectValue( deB, 5d ) );
        aggregatedDataMap.put( deB.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "200102",
            new DimensionItemObjectValue( deB, 6d ) );
        aggregatedDataMap.put( deB.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "200101",
            new DimensionItemObjectValue( deB, 7d ) );
        aggregatedDataMap.put( deB.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "200102",
            new DimensionItemObjectValue( deB, 8d ) );
        Map<String, List<DimensionItemObjectValue>> permutationMap = DataQueryParams
            .getPermutationDimensionalItemValueMap( aggregatedDataMap );
        assertNotNull( permutationMap );
        String ouAM1Key = ouA.getUid() + DIMENSION_SEP + "200101";
        String ouAM2Key = ouA.getUid() + DIMENSION_SEP + "200102";
        String ouBM1Key = ouB.getUid() + DIMENSION_SEP + "200101";
        String ouBM2Key = ouB.getUid() + DIMENSION_SEP + "200102";
        List<DimensionItemObjectValue> ouAM1 = permutationMap.get( ouAM1Key );
        List<DimensionItemObjectValue> ouAM2 = permutationMap.get( ouAM2Key );
        List<DimensionItemObjectValue> ouBM1 = permutationMap.get( ouBM1Key );
        List<DimensionItemObjectValue> ouBM2 = permutationMap.get( ouBM2Key );
        assertEquals( 2, ouAM1.size() );
        assertEquals( 2, ouAM2.size() );
        assertEquals( 2, ouBM1.size() );
        assertEquals( 2, ouBM2.size() );
        List<DimensionItemObjectValue> ouAM1Expected = new ArrayList<>();
        ouAM1Expected.add( new DimensionItemObjectValue( deA, 1d ) );
        ouAM1Expected.add( new DimensionItemObjectValue( deB, 5d ) );
        List<DimensionItemObjectValue> ouAM2Expected = new ArrayList<>();
        ouAM2Expected.add( new DimensionItemObjectValue( deA, 2d ) );
        ouAM2Expected.add( new DimensionItemObjectValue( deB, 6d ) );
        List<DimensionItemObjectValue> ouBM1Expected = new ArrayList<>();
        ouBM1Expected.add( new DimensionItemObjectValue( deA, 3d ) );
        ouBM1Expected.add( new DimensionItemObjectValue( deB, 7d ) );
        List<DimensionItemObjectValue> ouBM2Expected = new ArrayList<>();
        ouBM2Expected.add( new DimensionItemObjectValue( deA, 4d ) );
        ouBM2Expected.add( new DimensionItemObjectValue( deB, 8d ) );
        assertCollectionsMatch( ouAM1Expected, ouAM1 );
        assertCollectionsMatch( ouAM2Expected, ouAM2 );
        assertCollectionsMatch( ouBM1Expected, ouBM1 );
        assertCollectionsMatch( ouBM2Expected, ouBM2 );
    }

    @Test
    void testGetPermutationDimensionalItemValueMap()
    {
        MultiValuedMap<String, DimensionItemObjectValue> aggregatedDataMap = new ArrayListValuedHashMap<>();
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "2000Q1",
            new DimensionItemObjectValue( deA, 1d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouA.getUid() + DIMENSION_SEP + "2000Q2",
            new DimensionItemObjectValue( deA, 2d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "2000Q1",
            new DimensionItemObjectValue( deA, 3d ) );
        aggregatedDataMap.put( deA.getUid() + DIMENSION_SEP + ouB.getUid() + DIMENSION_SEP + "2000Q2",
            new DimensionItemObjectValue( deA, 4d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouA, "2000Q1" ), new DimensionItemObjectValue( deB, 5d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouA, "2000Q2" ), new DimensionItemObjectValue( deB, 6d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouB, "2000Q1" ), new DimensionItemObjectValue( deB, 7d ) );
        aggregatedDataMap.put( makeKey( deB, coc, ouB, "2000Q2" ), new DimensionItemObjectValue( deB, 8d ) );
        Map<String, List<DimensionItemObjectValue>> permutationMap = DataQueryParams
            .getPermutationDimensionalItemValueMap( aggregatedDataMap );
        assertNotNull( permutationMap );
        String ouAQ1Key = ouA.getUid() + DIMENSION_SEP + "2000Q1";
        String ouAQ2Key = ouA.getUid() + DIMENSION_SEP + "2000Q2";
        String ouBQ1Key = ouB.getUid() + DIMENSION_SEP + "2000Q1";
        String ouBQ2Key = ouB.getUid() + DIMENSION_SEP + "2000Q2";
        List<DimensionItemObjectValue> ouAQ1 = permutationMap.get( ouAQ1Key );
        List<DimensionItemObjectValue> ouAQ2 = permutationMap.get( ouAQ2Key );
        List<DimensionItemObjectValue> ouBQ1 = permutationMap.get( ouBQ1Key );
        List<DimensionItemObjectValue> ouBQ2 = permutationMap.get( ouBQ2Key );
        assertEquals( 2, ouAQ1.size() );
        assertEquals( 2, ouAQ2.size() );
        assertEquals( 2, ouBQ1.size() );
        assertEquals( 2, ouBQ2.size() );
        List<DimensionItemObjectValue> ouAQ1Expected = new ArrayList<>();
        ouAQ1Expected.add( new DimensionItemObjectValue( deA, 1d ) );
        ouAQ1Expected.add( new DimensionItemObjectValue( deB, 5d ) );
        List<DimensionItemObjectValue> ouAQ2Expected = new ArrayList<>();
        ouAQ2Expected.add( new DimensionItemObjectValue( deA, 2d ) );
        ouAQ2Expected.add( new DimensionItemObjectValue( deB, 6d ) );
        List<DimensionItemObjectValue> ouBQ1Expected = new ArrayList<>();
        ouBQ1Expected.add( new DimensionItemObjectValue( deA, 3d ) );
        ouBQ1Expected.add( new DimensionItemObjectValue( deB, 7d ) );
        List<DimensionItemObjectValue> ouBQ2Expected = new ArrayList<>();
        ouBQ2Expected.add( new DimensionItemObjectValue( deA, 4d ) );
        ouBQ2Expected.add( new DimensionItemObjectValue( deB, 8d ) );
        assertCollectionsMatch( ouAQ1Expected, ouAQ1 );
        assertCollectionsMatch( ouAQ2Expected, ouAQ2 );
        assertCollectionsMatch( ouBQ1Expected, ouBQ1 );
        assertCollectionsMatch( ouBQ2Expected, ouBQ2 );
    }

    /**
     * Ignores data element dimension and generates 2 x 3 = 6 combinations based
     * on organisation unit and period dimensions.
     */
    @Test
    void testGetDimensionOptionPermutations()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ) ) ).build();
        List<List<DimensionItem>> permutations = params.getDimensionItemPermutations();
        assertNotNull( permutations );
        assertEquals( 6, permutations.size() );
        for ( List<DimensionItem> permutation : permutations )
        {
            assertNotNull( permutation );
            assertEquals( 2, permutation.size() );
            assertEquals( ORGUNIT_DIM_ID, permutation.get( 0 ).getDimension() );
            assertEquals( PERIOD_DIM_ID, permutation.get( 1 ).getDimension() );
        }
    }

    @Test
    void testGetDataPeriodAggregationPeriodMap()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deC, deD ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000Q3" ),
                createPeriod( "2000Q4" ), createPeriod( "2001Q1" ), createPeriod( "2001Q2" ) ) )
            .withPeriodType( PeriodTypeEnum.QUARTERLY.getName() ).withDataPeriodType( new YearlyPeriodType() ).build();
        ListMap<DimensionalItemObject, DimensionalItemObject> map = params.getDataPeriodAggregationPeriodMap();
        assertEquals( 2, map.size() );
        assertTrue( map.containsKey( createPeriod( "2000" ) ) );
        assertTrue( map.containsKey( createPeriod( "2001" ) ) );
        assertEquals( 4, map.get( createPeriod( "2000" ) ).size() );
        assertEquals( 2, map.get( createPeriod( "2001" ) ).size() );
        assertTrue( map.get( createPeriod( "2000" ) ).contains( createPeriod( "2000Q1" ) ) );
        assertTrue( map.get( createPeriod( "2000" ) ).contains( createPeriod( "2000Q2" ) ) );
        assertTrue( map.get( createPeriod( "2000" ) ).contains( createPeriod( "2000Q3" ) ) );
        assertTrue( map.get( createPeriod( "2000" ) ).contains( createPeriod( "2000Q4" ) ) );
        assertTrue( map.get( createPeriod( "2001" ) ).contains( createPeriod( "2001Q1" ) ) );
        assertTrue( map.get( createPeriod( "2001" ) ).contains( createPeriod( "2001Q2" ) ) );
    }

    /**
     * Query spans two period types and two aggregation types. Splits in 2
     * queries for each period type, then splits in 4 queries on data elements
     * to satisfy optimal of 4 queries per query group.
     */
    @Test
    void planQueryA()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deC, deD ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "200101" ), createPeriod( "200103" ), createPeriod( "200105" ),
                createPeriod( "200107" ), createPeriod( "2002Q3" ), createPeriod( "2002Q4" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 8, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 3 period types. Splits in 3 queries for each period type,
     * then splits in 2 queries on organisation units to satisfy optimal for a
     * total of 6 queries.
     */
    @Test
    void planQueryB()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000" ),
                createPeriod( "200002" ), createPeriod( "200003" ), createPeriod( "200004" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 6 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 6, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 6, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 3 organisation unit levels. Splits in 3 queries for each
     * level, then splits in 2 queries on organisation units to satisfy optimal
     * for a total of 5 queries, as there are only 5 organisation units in
     * total.
     */
    @Test
    void planQueryC()
    {
        ouB.setParent( ouA );
        ouC.setParent( ouA );
        ouD.setParent( ouB );
        ouE.setParent( ouC );
        ouA.getChildren().add( ouB );
        ouA.getChildren().add( ouC );
        ouD.getChildren().add( ouB );
        ouC.getChildren().add( ouE );
        organisationUnitService.updateOrganisationUnit( ouA );
        organisationUnitService.updateOrganisationUnit( ouB );
        organisationUnitService.updateOrganisationUnit( ouC );
        organisationUnitService.updateOrganisationUnit( ouD );
        organisationUnitService.updateOrganisationUnit( ouE );
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000Q3" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 6 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 5, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 5, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 2 aggregation types. Splits on 2 aggregation types, then
     * splits one query on 3 days in period to satisfy optimal for a total of 4
     * queries.
     */
    @Test
    void planQueryD()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deC ) )
            .withOrganisationUnits( getList( ouA ) )
            .withPeriods( getList( createPeriod( "200001" ), createPeriod( "200002" ), createPeriod( "200003" ),
                createPeriod( "200004" ), createPeriod( "200005" ), createPeriod( "200006" ), createPeriod( "200007" ),
                createPeriod( "200008" ), createPeriod( "200009" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 4, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 3, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 2 aggregation types. Splits on 2 aggregation types, then
     * splits one query on 3 days in period to satisfy optimal for a total of 4
     * queries. No organisation units specified.
     */
    @Test
    void planQueryE()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deC ) )
            .withPeriods( getList( createPeriod( "200001" ), createPeriod( "200002" ), createPeriod( "200003" ),
                createPeriod( "200004" ), createPeriod( "200005" ), createPeriod( "200006" ), createPeriod( "200007" ),
                createPeriod( "200008" ), createPeriod( "200009" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 4, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 3, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Splits on 3 queries on organisation units for an optimal of 3 queries.
     */
    @Test
    void planQueryF()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "200001" ), createPeriod( "200002" ), createPeriod( "200003" ),
                createPeriod( "200004" ), createPeriod( "200005" ), createPeriod( "200006" ), createPeriod( "200007" ),
                createPeriod( "200008" ), createPeriod( "200009" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 3, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 3, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Splits in 4 queries on data elements, then 2 queries on organisation
     * units to satisfy optimal for a total of 8 queries.
     */
    @Test
    void planQueryH()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deC, deD ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withFilterPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000Q3" ),
                createPeriod( "2000Q4" ), createPeriod( "2001Q1" ), createPeriod( "2001Q2" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 8, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 3 period types. Splits in 3 queries for each period type,
     * then splits in 2 queries on data type, then splits in 2 queries on data
     * elements to satisfy optimal for a total of 12 queries, because query has
     * 2 different aggregation types.
     */
    @Test
    void planQueryI()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deE, deF ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000" ),
                createPeriod( "200002" ), createPeriod( "200003" ), createPeriod( "200004" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 6 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 12, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 6, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Splits in 4 queries on data sets to satisfy optimal for a total of 4
     * queries.
     */
    @Test
    void planQueryK()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withReportingRates( getList( rrA, rrB, rrC, rrD ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD, ouE ) )
            .withPeriods( getList( createPeriod( "2000Q1" ), createPeriod( "2000Q2" ), createPeriod( "2000Q3" ),
                createPeriod( "2000Q4" ), createPeriod( "2001Q1" ), createPeriod( "2001Q2" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        List<DataQueryParams> queries = queryGroups.getAllQueries();
        assertEquals( 4, queries.size() );
        for ( DataQueryParams query : queries )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Splits in 2 queries for each data type, then 2 queries for each data
     * element, then 2 queries for each organisation unit to satisfy optimal for
     * a total of 8 queries with 4 queries across 2 sequential queries.
     */
    @Test
    void planQueryL()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deE, deF ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD ) )
            .withFilterPeriods( getList( createPeriod( "2000Q1" ) ) ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 8, queryGroups.getAllQueries().size() );
        assertEquals( 2, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertDimensionNameNotNull( query );
            assertNotNull( query.getDataType() );
        }
    }

    /**
     * Splits in 4 queries for data elements to satisfy optimal for a total of 4
     * queries.
     */
    @Test
    void planQueryM()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB, deG, deH ) )
            .withOrganisationUnits( getList( ouA ) )
            .withPeriods( getList( createPeriod( "200101" ), createPeriod( "200103" ) ) ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 4, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
        }
    }

    /**
     * Query spans 5 QueryModifiers minDate/MaxDate combinations.
     */
    @Test
    void planQueryN()
    {
        QueryModifiers modsA = QueryModifiers.builder().minDate( parseDate( "2022-01-01" ) ).build();
        QueryModifiers modsB = QueryModifiers.builder().minDate( parseDate( "2022-02-01" ) ).build();
        QueryModifiers modsC = QueryModifiers.builder().maxDate( parseDate( "2022-12-31" ) ).build();
        QueryModifiers modsD = QueryModifiers.builder().minDate( parseDate( "2022-01-01" ) )
            .maxDate( parseDate( "2022-12-31" ) ).build();
        deC.setQueryMods( modsA );
        deD.setQueryMods( modsB );
        deG.setQueryMods( modsC );
        deH.setQueryMods( modsD );
        deI.setQueryMods( modsD );
        deC.setAggregationType( AggregationType.SUM );
        deD.setAggregationType( AggregationType.SUM );
        deI.setAggregationType( AggregationType.SUM );
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( getList( deA, deB, deC, deD, deG, deH, deI ) )
            .withPeriods( getList( createPeriod( "2022" ) ) )
            .build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 5, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 5, queryGroups.getLargestGroupSize() );
        List<DataQueryParams> group = queryGroups.getAllQueries();
        assertQueryMods( group, null, deA, deB );
        assertQueryMods( group, modsA, deC );
        assertQueryMods( group, modsB, deD );
        assertQueryMods( group, modsC, deG );
        assertQueryMods( group, modsD, deH, deI );
    }

    /**
     * Create 4 queries (one for each period) due to the FIRST aggregation type.
     */
    @Test
    void planQueryForFirstAggregationType()
    {
        planQueryForFirstOrLastAggregationType( AnalyticsAggregationType.FIRST );
    }

    /**
     * Create 4 queries (one for each period) due to the LAST aggregation type.
     */
    @Test
    void planQueryForLastAggregationType()
    {
        planQueryForFirstOrLastAggregationType( AnalyticsAggregationType.LAST );
    }

    /**
     * Create 4 queries (one for each period) due to the LAST aggregation type.
     */
    @Test
    void planQueryForLastInPeriodAggregationType()
    {
        planQueryForFirstOrLastAggregationType( AnalyticsAggregationType.LAST_IN_PERIOD );
    }

    private void planQueryForFirstOrLastAggregationType( AnalyticsAggregationType analyticsAggregationType )
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA ) )
            .withOrganisationUnits( getList( ouA ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200102" ), createPeriod( "200103" ), createPeriod( "200104" ) ) )
            .withAggregationType( analyticsAggregationType ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        List<DataQueryParams> queries = queryGroups.getAllQueries();
        assertEquals( 4, queries.size() );
        for ( DataQueryParams query : queries )
        {
            assertEquals( 1, query.getPeriods().size() );
            assertNotNull( query.getDimension( PERIOD_DIM_ID ) );
            assertEquals( MonthlyPeriodType.NAME.toLowerCase(),
                query.getDimension( PERIOD_DIM_ID ).getDimensionName() );
        }
    }

    /**
     * Splits in 4 queries for each period to satisfy optimal for a total of 4
     * queries, because all queries have different periods.
     */
    @Test
    void planQueryStartEndDateRestrictionQueryGrouperA()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA ) ).withPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200102" ), createPeriod( "200103" ), createPeriod( "200104" ) ) )
            .build();
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers = Lists.newArrayList();
        queryGroupers.add( q -> queryPlanner.groupByStartEndDateRestriction( q ) );
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).withQueryGroupers( queryGroupers ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        List<DataQueryParams> queries = queryGroups.getAllQueries();
        assertEquals( 4, queries.size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queries )
        {
            assertNull( query.getStartDate() );
            assertNull( query.getEndDate() );
            assertNotNull( query.getStartDateRestriction() );
            assertNotNull( query.getEndDateRestriction() );
            assertDimensionNameNotNull( query );
            DimensionalObject periodDim = query.getDimension( PERIOD_DIM_ID );
            assertNotNull( periodDim.getDimensionName() );
            assertTrue( periodDim.isFixed() );
        }
    }

    /**
     * Splits in 4 queries for each period to satisfy optimal for a total of 4
     * queries, because all queries have different periods.
     */
    @Test
    void planQueryStartEndDateRestrictionQueryGrouperB()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA ) ).withFilterPeriods( getList( createPeriod( "200101" ),
                createPeriod( "200102" ), createPeriod( "200103" ), createPeriod( "200104" ) ) )
            .build();
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers = Lists.newArrayList();
        queryGroupers.add( q -> queryPlanner.groupByStartEndDateRestriction( q ) );
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).withQueryGroupers( queryGroupers ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        List<DataQueryParams> queries = queryGroups.getAllQueries();
        assertEquals( 2, queries.size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 2, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queries )
        {
            assertNull( query.getStartDate() );
            assertNull( query.getEndDate() );
            assertNotNull( query.getStartDateRestriction() );
            assertNotNull( query.getEndDateRestriction() );
            assertDimensionNameNotNull( query );
            assertNull( query.getFilter( PERIOD_DIM_ID ) );
        }
    }

    /**
     * Split on two data elements. Set aggregation type average and value type
     * integer on query. Convert aggregation type from data elements to average
     * and then to average integer.
     */
    @Test
    void planQueryAggregationTypeA()
    {
        DataElement deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        DataElement deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.COUNT );
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA ) ).withPeriods( getList( createPeriod( "200101" ) ) )
            .withAggregationType( AnalyticsAggregationType.AVERAGE ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 2, queryGroups.getAllQueries().size() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertNotNull( query.getAggregationType() );
            assertEquals( AggregationType.AVERAGE, query.getAggregationType().getAggregationType() );
            assertEquals( DataType.NUMERIC, query.getAggregationType().getDataType() );
        }
    }

    /**
     * Split on two data elements. Set aggregation type average and value type
     * boolean on query. Convert aggregation type from data elements to average
     * and then to average boolean.
     */
    @Test
    void planQueryAggregationTypeB()
    {
        DataElement deA = createDataElement( 'A', ValueType.BOOLEAN, AggregationType.SUM );
        DataElement deB = createDataElement( 'B', ValueType.BOOLEAN, AggregationType.COUNT );
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deA, deB ) )
            .withOrganisationUnits( getList( ouA ) ).withPeriods( getList( createPeriod( "200101" ) ) )
            .withAggregationType( AnalyticsAggregationType.AVERAGE ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 2, queryGroups.getAllQueries().size() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertNotNull( query.getAggregationType() );
            assertEquals( AggregationType.AVERAGE, query.getAggregationType().getAggregationType() );
            assertEquals( DataType.BOOLEAN, query.getAggregationType().getDataType() );
        }
    }

    /**
     * Query is type disaggregation as aggregation period type for periods is
     * monthly and data elements period type is yearly. Split on two data
     * elements.
     */
    @Test
    void planQueryDataElementDisaggregation()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( getList( deI, deJ ) )
            .withOrganisationUnits( getList( ouA, ouB, ouC, ouD ) )
            .withPeriods( getList( createPeriod( "201001" ), createPeriod( "201003" ) ) ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 4, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 4, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
            assertNotNull( query.getDataPeriodType() );
            assertEquals( yearly, query.getDataPeriodType() );
            assertTrue( query.isDisaggregation() );
        }
    }

    /**
     * Query is type disaggregation as aggregation period type for periods is
     * monthly and data element groups period type is yearly. Split on two org
     * units.
     */
    @Test
    void planQueryDataElementGroupSetDisaggregation()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElementGroupSet( dgsB )
            .withOrganisationUnits( getList( ouA, ouB ) )
            .withPeriods( getList( createPeriod( "201001" ), createPeriod( "201003" ) ) ).build();
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withOptimalQueries( 4 )
            .withTableType( ANALYTICS_TABLE_TYPE ).build();
        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );
        assertEquals( 2, queryGroups.getAllQueries().size() );
        assertEquals( 1, queryGroups.getSequentialQueries().size() );
        assertEquals( 2, queryGroups.getLargestGroupSize() );
        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            assertTrue( samePeriodType( query.getPeriods() ) );
            assertDimensionNameNotNull( query );
            assertNotNull( query.getDataPeriodType() );
            assertEquals( yearly, query.getDataPeriodType() );
            assertTrue( query.isDisaggregation() );
        }
    }

    @Test
    void testWithTableTypeAndPartition()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withStartDate( getDate( 2014, 4, 1 ) )
            .withEndDate( getDate( 2016, 8, 1 ) ).build();
        assertTrue( params.hasStartEndDate() );
        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().withTableType( ANALYTICS_TABLE_TYPE )
            .build();
        DataQueryParams query = queryPlanner.withTableNameAndPartitions( params, plannerParams );
        Partitions partitions = query.getPartitions();
        Partitions expected = new Partitions( Sets.newHashSet( 0, 2014, 2015, 2016 ) );
        assertNotNull( partitions );
        assertEquals( 4, partitions.getPartitions().size() );
        assertEquals( expected, partitions );
        assertEquals( ANALYTICS_TABLE_TYPE.getTableName(), query.getTableName() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
    private static boolean samePeriodType( List<DimensionalItemObject> isoPeriods )
    {
        Iterator<DimensionalItemObject> periods = new ArrayList<>( isoPeriods ).iterator();
        PeriodType first = ((Period) periods.next()).getPeriodType();
        while ( periods.hasNext() )
        {
            PeriodType next = ((Period) periods.next()).getPeriodType();
            if ( !first.equals( next ) )
            {
                return false;
            }
        }
        return true;
    }

    private static void assertDimensionNameNotNull( DataQueryParams params )
    {
        for ( DimensionalObject dim : params.getDimensions() )
        {
            assertNotNull( dim.getDimensionName() );
        }
        for ( DimensionalObject filter : params.getFilters() )
        {
            assertNotNull( filter.getDimensionName() );
        }
    }

    private void assertCollectionsMatch( List<DimensionItemObjectValue> collection,
        final List<DimensionItemObjectValue> in )
    {
        Function<String, Number> findValueByUid = ( String uid ) -> in.stream()
            .filter( v -> v.getDimensionalItemObject().getUid().equals( uid ) ).findFirst().get().getValue();
        for ( DimensionItemObjectValue dimensionItemObjectValue : collection )
        {
            final Number val = findValueByUid.apply( dimensionItemObjectValue.getDimensionalItemObject().getUid() );
            assertEquals( val, dimensionItemObjectValue.getValue() );
        }
    }

    private void assertQueryMods( List<DataQueryParams> group, QueryModifiers mods, DataElement... elements )
    {
        List<DataElement> modElements = Arrays.asList( elements );

        for ( DataQueryParams params : group )
        {
            List<DimensionalItemObject> groupElements = params.getDataElements();
            assertNotEquals( 0, groupElements.size() );

            QueryModifiers groupMods = groupElements.get( 0 ).getQueryMods();

            if ( Objects.equals( mods, groupMods ) )
            {
                assertTrue( Objects.equals( params.getStartDate(), mods == null ? null : mods.getMinDate() ) );
                assertTrue( Objects.equals( params.getEndDate(), mods == null ? null : mods.getMaxDate() ) );

                assertEquals( modElements.size(), groupElements.size() );
                assertTrue( groupElements.containsAll( modElements ) );

                groupElements.forEach( e -> {
                    assertEquals( mods, e.getQueryMods() );
                } );

                return;
            }
        }

        throw new RuntimeException( "No group found for queryMods " + mods );
    }
}
