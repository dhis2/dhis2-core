package org.hisp.dhis.analytics.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class EventQueryParamsTest
    extends DhisConvenienceTest
{
    private Option opA;
    private Option opB;
    private Option opC;
    private Option opD;
    private OptionSet osA;
    private OptionSet osB;
    private DataElement deA;
    private DataElement deB;
    
    @Before
    public void before()
    {
        opA = createOption( 'A' );
        opB = createOption( 'B' );
        opC = createOption( 'C' );
        opD = createOption( 'D' );
        osA = createOptionSet( 'A', opA, opB );
        osB = createOptionSet( 'B', opC, opD );
        
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
    }
    
    @Test
    public void testReplacePeriodsWithStartEndDates()
    {
        List<DimensionalItemObject> periods = new ArrayList<>();
        periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, 4, 1, 0, 0 ).toDate() ) );
        periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, 5, 1, 0, 0 ).toDate() ) );
        periods.add( new MonthlyPeriodType().createPeriod( new DateTime( 2014, 6, 1, 0, 0 ).toDate() ) );

        EventQueryParams params = new EventQueryParams.Builder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, periods ) ).build();
        
        assertNull( params.getStartDate() );
        assertNull( params.getEndDate() );
        
        params = new EventQueryParams.Builder( params )
            .withStartEndDatesForPeriods().build();
        
        assertEquals( new DateTime( 2014, 4, 1, 0, 0 ).toDate(), params.getStartDate() );
        assertEquals( new DateTime( 2014, 6, 30, 0, 0 ).toDate(), params.getEndDate() );        
    }
    
    @Test
    public void testGetItemLegends()
    {
        Legend leA = createLegend( 'A', 0d, 1d );
        Legend leB = createLegend( 'B', 1d, 2d );        
        LegendSet lsA = createLegendSet( 'A', leA, leB );
        
        QueryItem qiA = new QueryItem( deA, lsA, deA.getValueType(), deA.getAggregationType(), null );
        
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( qiA )
            .build();
        
        Set<Legend> expected = Sets.newHashSet( leA, leB );
        
        assertEquals( expected, params.getItemLegends() );
    }
    
    @Test
    public void testGetItemOptions()
    {
        QueryItem qiA = new QueryItem( deA, null, deA.getValueType(), deA.getAggregationType(), osA );
        QueryItem qiB = new QueryItem( deB, null, deB.getValueType(), deB.getAggregationType(), osB );

        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( qiA )
            .addItem( qiB )
            .build();
        
        Set<Option> expected = Sets.newHashSet( opA, opB, opC, opD );
        
        assertEquals( expected, params.getItemOptions() );
    }
    
    @Test
    public void testGetDuplicateQueryItems()
    {        
        QueryItem iA = new QueryItem( createDataElement( 'A', new DataElementCategoryCombo() ) );
        QueryItem iB = new QueryItem( createDataElement( 'B', new DataElementCategoryCombo() ) );
        QueryItem iC = new QueryItem( createDataElement( 'B', new DataElementCategoryCombo() ) );
        QueryItem iD = new QueryItem( createDataElement( 'D', new DataElementCategoryCombo() ) );

        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( iA )
            .addItem( iB )
            .addItem( iC )
            .addItem( iD ).build();
        
        List<QueryItem> duplicates = params.getDuplicateQueryItems();
        
        assertEquals( 1, duplicates.size() );
        assertTrue( duplicates.contains( iC ) );        
    }
}
