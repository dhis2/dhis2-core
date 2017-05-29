package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.Map;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.junit.Assert.*;
import static org.hisp.dhis.common.QueryFilter.OPTION_SEP;

/**
* @author Lars Helge Overland
*/
public class EventAnalyticsServiceMetadataTest
    extends DhisSpringTest
{
    private LegendSet lsA;
    
    private Legend leA;
    private Legend leB;
    private Legend leC;
    private Legend leD;
    
    private OptionSet osA;
    
    private Option opA;
    private Option opB;
    private Option opC;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    private DataElement deD;
    private DataElement deE;
    private DataElement deF;
    
    private Period peA;
    
    private OrganisationUnit ouA;

    private ProgramStage psA;
    private Program prA;
    
    @Autowired
    private EventAnalyticsService eventAnalyticsService;
    
    @Override
    public void setUpTest()
    {
        leA = createLegend( 'A', 0d, 10d );
        leB = createLegend( 'B', 11d, 20d );
        leC = createLegend( 'C', 21d, 30d );
        leD = createLegend( 'D', 31d, 40d );
        
        lsA = createLegendSet( 'A' );
        lsA.setLegends( Sets.newHashSet( leA, leB, leC, leD ) );
        
        opA = createOption( 'A' );
        opB = createOption( 'B' );
        opC = createOption( 'C' );
        
        osA = createOptionSet( 'A' );
        osA.setOptions( Lists.newArrayList( opA, opB, opC ) );
        
        deA = createDataElement( 'A', ValueType.INTEGER, AggregationType.SUM );
        deA.setLegendSets( Lists.newArrayList( lsA ) );
        
        deB = createDataElement( 'B', ValueType.INTEGER, AggregationType.SUM );
        deB.setLegendSets( Lists.newArrayList( lsA ) );
        
        deC = createDataElement( 'C', ValueType.INTEGER, AggregationType.SUM );
        
        deD = createDataElement( 'D', ValueType.INTEGER, AggregationType.SUM );
        
        deE = createDataElement( 'E', ValueType.TEXT, AggregationType.NONE );
        deE.setOptionSet( osA );
        
        deF = createDataElement( 'F', ValueType.TEXT, AggregationType.NONE );
        deF.setOptionSet( osA );
        
        peA = MonthlyPeriodType.getPeriodFromIsoString( "201701" );
        
        ouA = createOrganisationUnit( 'A' );

        prA = createProgram( 'A' );
        psA = createProgramStage( 'A', prA );
        prA.getProgramStages().add( psA );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void testGetQueryItemDimensionMetadata()
    {
        DimensionalObject periods = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peA ) );
        DimensionalObject orgUnits = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList( ouA ) );
        
        QueryItem itemLegendSet = new QueryItem( deA, lsA, deA.getValueType(), deA.getAggregationType(), null );

        QueryItem itemLegendSetFilter = new QueryItem( deB, lsA, deB.getValueType(), deB.getAggregationType(), null );
        itemLegendSetFilter.addFilter( new QueryFilter( QueryOperator.IN, leA.getUid() + OPTION_SEP + leB.getUid() + OPTION_SEP + leC.getUid() ) );
        
        QueryItem item = new QueryItem( deC, null, deC.getValueType(), deC.getAggregationType(), null );
        
        QueryItem itemFilter = new QueryItem( deD, null, deD.getValueType(), deD.getAggregationType(), null );
        itemFilter.addFilter( new QueryFilter( QueryOperator.GT, "10" ) );
        
        QueryItem itemOptionSet = new QueryItem( deE, null, deE.getValueType(), deE.getAggregationType(), osA );
        
        QueryItem itemOptionSetFilter = new QueryItem( deF, null, deE.getValueType(), deE.getAggregationType(), osA );
        itemOptionSetFilter.addFilter( new QueryFilter( QueryOperator.IN, opA.getCode() + OPTION_SEP + opB.getCode() ) );
                
        EventQueryParams params = new EventQueryParams.Builder()
            .withProgram( prA )
            .addDimension( periods )
            .addDimension( orgUnits )
            .addItem( itemLegendSet )
            .addItem( itemLegendSetFilter )
            .addItem( item )
            .addItem( itemFilter )
            .addItem( itemOptionSet )
            .addItem( itemOptionSetFilter )
            .withSkipData( true )
            .withSkipMeta( false )
            .withDisplayProperty( DisplayProperty.NAME )
            .withApiVersion( DhisApiVersion.V26 )
            .build();
        
        Grid grid = eventAnalyticsService.getAggregatedEventData( params );
        
        Map<String, Object> metadata = grid.getMetaData();
        
        assertNotNull( metadata );
        
        Map<String, Object> dimensionItems = (Map<String, Object>) metadata.get( AnalyticsMetaDataKey.DIMENSIONS.getKey() );

        assertNotNull( dimensionItems );
        
        List<String> itemsLegendSet = (List<String>) dimensionItems.get( itemLegendSet.getItemId() );
        List<String> itemsLegendSetFilter = (List<String>) dimensionItems.get( itemLegendSetFilter.getItemId() );
        List<String> items = (List<String>) dimensionItems.get( item.getItemId() );
        List<String> itemsFilter = (List<String>) dimensionItems.get( itemFilter.getItemId() );
        List<String> itemsOptionSet = (List<String>) dimensionItems.get( itemOptionSet.getItemId() );
        List<String> itemsOptionSetFilter = (List<String>) dimensionItems.get( itemOptionSetFilter.getItemId() );

        assertNotNull( itemsLegendSet );
        assertNotNull( itemsLegendSetFilter );
        assertNotNull( items );
        assertNotNull( itemsFilter );
        assertNotNull( itemsOptionSet );
        assertNotNull( itemsOptionSetFilter );
        
        assertEquals( 4, itemsLegendSet.size() );
        assertEquals( itemsLegendSet, Lists.newArrayList( leA.getUid(), leB.getUid(), leC.getUid(), leD.getUid() ) );
        assertEquals( 3, itemsLegendSetFilter.size() );
        assertTrue( itemsLegendSetFilter.containsAll( IdentifiableObjectUtils.getUids( Sets.newHashSet( leA, leB, leC ) ) ) );
        assertTrue( items.isEmpty() );
        assertTrue( itemsFilter.isEmpty() );
        assertTrue( itemsOptionSet.isEmpty() );
        assertEquals( 2, itemsOptionSetFilter.size() );
        assertTrue( itemsOptionSetFilter.containsAll( IdentifiableObjectUtils.getCodes( Sets.newHashSet( opA, opB ) ) ) );
    }
    
    @Test
    public void testLegendSetSortedLegends()
    {
        List<Legend> legends = Lists.newArrayList( leA, leB, leC, leD );
        
        assertEquals( legends, lsA.getSortedLegends() );
    }
}
