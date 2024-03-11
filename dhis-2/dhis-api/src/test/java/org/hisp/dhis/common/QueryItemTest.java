package org.hisp.dhis.common;

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

import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;

import static org.junit.Assert.*;

/**
* @author Lars Helge Overland
*/
public class QueryItemTest
{
    private Option opA;
    private Option opB;
    private Option opC;
    private OptionSet osA;
    
    private Legend leA;
    private Legend leB;
    private Legend leC;
    private LegendSet lsA;
    
    private DataElement deA;
    private DataElement deB;
    
    @Before
    public void before()
    {
        opA = new Option( "OptionA", "CODEA" );
        opA.setUid( "UIDA" );
        opB = new Option( "OptionB", "CODEB" );
        opB.setUid( "UIDB" );
        opC = new Option( "OptionC", "CODEC" );
        opC.setUid( "UIDC" );
        osA = new OptionSet( "OptionSetA", ValueType.TEXT, Lists.newArrayList( opA, opB, opC ) );
        
        leA = new Legend( "LegendA", 0d, 1d, "", "" );
        leA.setUid( "UIDA" );
        leB = new Legend( "LegendB", 1d, 2d, "", "" );
        leB.setUid( "UIDB" );
        leC = new Legend( "LegendC", 3d, 4d, "", "" );
        leC.setUid( "UIDC" );
        lsA = new LegendSet( "LegendSetA", "", Sets.newHashSet( leA, leB, leC ) );
        
        deA = new DataElement( "DataElementA" );
        deA.setOptionSet( osA );
        
        deB = new DataElement( "DataElementB" );
        deB.setLegendSets( Lists.newArrayList( lsA ) );
    }
    
    @Test
    public void testGetOptionSetQueryFilterItems()
    {
        QueryItem qiA = new QueryItem( deA, null, ValueType.TEXT, AggregationType.SUM, osA );
        qiA.addFilter( new QueryFilter( QueryOperator.IN, "CODEA;CODEB" ) );
        
        List<String> expected = Lists.newArrayList( "UIDA", "UIDB" );
            
        assertEquals( expected, qiA.getOptionSetFilterItemsOrAll() );
        
        QueryItem qiB = new QueryItem( deA, null, ValueType.TEXT, AggregationType.SUM, osA );

        expected = Lists.newArrayList( "UIDA", "UIDB", "UIDC" );
            
        assertEquals( expected, qiB.getOptionSetFilterItemsOrAll() );
        
    }
    
    @Test
    public void testGet()
    {
        QueryItem qiA = new QueryItem( deB, lsA, ValueType.TEXT, AggregationType.SUM, null );
        qiA.addFilter( new QueryFilter( QueryOperator.IN, "UIDA;UIDB" ) );

        List<String> expected = Lists.newArrayList( "UIDA", "UIDB" );
        
        assertEquals( expected, qiA.getLegendSetFilterItemsOrAll() );

        QueryItem qiB = new QueryItem( deB, lsA, ValueType.TEXT, AggregationType.SUM, null );

        expected = Lists.newArrayList( "UIDA", "UIDB", "UIDC" );
        
        assertEquals( expected, qiB.getLegendSetFilterItemsOrAll() );
    }
}
