package org.hisp.dhis.analytics.data;

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

import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.junit.Test;

import org.hisp.dhis.analytics.AggregationType;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class QueryPlannerUtilsTest
{
    @Test
    public void testGetAggregationType()
    {
        AnalyticsAggregationType typeA = new AnalyticsAggregationType( AggregationType.SUM, AggregationType.AVERAGE, DataType.NUMERIC, true );
        AnalyticsAggregationType typeB = new AnalyticsAggregationType( AggregationType.AVERAGE, AggregationType.AVERAGE, DataType.NUMERIC, true );
        
        assertEquals( typeA, 
            QueryPlannerUtils.getAggregationType( new AnalyticsAggregationType( AggregationType.SUM, AggregationType.AVERAGE ), ValueType.INTEGER, 
                new QuarterlyPeriodType(), new YearlyPeriodType() ) );
        
        assertEquals( typeB, 
            QueryPlannerUtils.getAggregationType( new AnalyticsAggregationType( AggregationType.AVERAGE ), ValueType.INTEGER, 
                new QuarterlyPeriodType(), new YearlyPeriodType() ) );
    }
    
    @Test
    public void testIsDisaggregation()
    {
        assertTrue( QueryPlannerUtils.isDisaggregation( new QuarterlyPeriodType(), new YearlyPeriodType() ) );
        assertTrue( QueryPlannerUtils.isDisaggregation( new MonthlyPeriodType(), new YearlyPeriodType() ) );
        assertTrue( QueryPlannerUtils.isDisaggregation( new FinancialAprilPeriodType(), new YearlyPeriodType() ) );
        assertTrue( QueryPlannerUtils.isDisaggregation( new FinancialOctoberPeriodType(), new YearlyPeriodType() ) );
        
        assertFalse( QueryPlannerUtils.isDisaggregation( new YearlyPeriodType(), new QuarterlyPeriodType() ) );        
        assertFalse( QueryPlannerUtils.isDisaggregation( new YearlyPeriodType(), new YearlyPeriodType() ) );
    }
    
    @Test
    public void testFromAggregationType()
    {
        assertEquals( new AnalyticsAggregationType( AggregationType.SUM, AggregationType.SUM ), AnalyticsAggregationType.fromAggregationType( AggregationType.SUM ) );
        assertEquals( new AnalyticsAggregationType( AggregationType.SUM, AggregationType.AVERAGE ), AnalyticsAggregationType.fromAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT ) );
    }
}
