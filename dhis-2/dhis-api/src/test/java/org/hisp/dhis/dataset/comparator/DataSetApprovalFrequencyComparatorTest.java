package org.hisp.dhis.dataset.comparator;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DataSetApprovalFrequencyComparatorTest
{
    @Test
    public void testA()
    {
        DataSet dsA = new DataSet( "DataSetA", new YearlyPeriodType() );
        DataSet dsB = new DataSet( "DataSetB", new YearlyPeriodType() );
        DataSet dsC = new DataSet( "DataSetC", new MonthlyPeriodType() );
        DataSet dsD = new DataSet( "DataSetD", new QuarterlyPeriodType() );

        DataApprovalWorkflow workflow = new DataApprovalWorkflow( "Workflow A", new QuarterlyPeriodType(), null );

        dsA.setWorkflow( workflow );
        dsD.setWorkflow( workflow );
        
        List<DataSet> list = Lists.newArrayList( dsA, dsC, dsB, dsD );
        
        Collections.sort( list, DataSetApprovalFrequencyComparator.INSTANCE );

        assertEquals( dsD, list.get( 0 ) );
        assertEquals( dsA, list.get( 1 ) );
        assertEquals( dsC, list.get( 2 ) );
        assertEquals( dsB, list.get( 3 ) );
    }
    
    @Test
    public void testB()
    {
        DataSet dsA = new DataSet( "EA: Expenditures Site Level", new QuarterlyPeriodType() );
        DataSet dsB = new DataSet( "MER Results: Facility Based", new QuarterlyPeriodType() );
        DataSet dsC = new DataSet( "MER Results: Facility Based - DoD ONLY", new QuarterlyPeriodType() );

        DataApprovalWorkflow workflow = new DataApprovalWorkflow( "Workflow A", new QuarterlyPeriodType(), null );

        dsB.setWorkflow( workflow );
        
        List<DataSet> list = Lists.newArrayList( dsB, dsC, dsA );
        
        Collections.sort( list, DataSetApprovalFrequencyComparator.INSTANCE );

        assertEquals( dsB, list.get( 0 ) );
        assertEquals( dsA, list.get( 1 ) );
        assertEquals( dsC, list.get( 2 ) );
    }
    
    @Test
    public void testC()
    {
        DataSet dsA = new DataSet( "DataSetA", new YearlyPeriodType() );
        DataSet dsB = new DataSet( "DataSetB", new YearlyPeriodType() );
        DataSet dsC = new DataSet( "DataSetC", new MonthlyPeriodType() );
        DataSet dsD = new DataSet( "DataSetD", new QuarterlyPeriodType() );

        DataApprovalWorkflow workflow = new DataApprovalWorkflow( "Workflow A", new QuarterlyPeriodType(), null );

        dsA.setWorkflow( workflow );
        dsD.setWorkflow( workflow );
        
        DataElement deA = new DataElement();
        deA.addDataSet( dsA );
        deA.addDataSet( dsB );
        deA.addDataSet( dsC );
        deA.addDataSet( dsD );
        
        assertEquals( dsD, deA.getApprovalDataSet() );
    }
    
    @Test
    public void testD()
    {
        DataSet dsA = new DataSet( "EA: Expenditures Site Level", new QuarterlyPeriodType() );
        DataSet dsB = new DataSet( "MER Results: Facility Based", new QuarterlyPeriodType() );
        DataSet dsC = new DataSet( "MER Results: Facility Based - DoD ONLY", new QuarterlyPeriodType() );

        DataApprovalWorkflow workflow = new DataApprovalWorkflow( "Workflow A", new QuarterlyPeriodType(), null );

        dsB.setWorkflow( workflow );

        DataElement deA = new DataElement();
        deA.addDataSet( dsA );
        deA.addDataSet( dsB );
        deA.addDataSet( dsC );
        
        assertEquals( dsB, deA.getApprovalDataSet() );        
    }
}
