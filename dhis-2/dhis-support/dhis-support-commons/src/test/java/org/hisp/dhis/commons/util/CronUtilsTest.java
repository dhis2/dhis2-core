package org.hisp.dhis.commons.util;
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Stian Sandvold
 */
public class CronUtilsTest
{
    @Test
    public void getCronExpresion()
    {
        assertEquals( "1 2 3 4 5 6", CronUtils.getCronExpression( "1", "2", "3", "4", "5", "6" ) );
    }

    @Test
    public void getDailyCronExpression()
    {
        assertEquals( "0 0 0 */1 * *", CronUtils.getDailyCronExpression( 0, 0 ) );
    }

    @Test
    public void getWeeklyCronExpressionForAllWeekdays()
    {
        assertEquals( "0 0 0 * * SUN", CronUtils.getWeeklyCronExpression( 0, 0, 0 ) );
        assertEquals( "0 0 0 * * MON", CronUtils.getWeeklyCronExpression( 0, 0, 1 ) );
        assertEquals( "0 0 0 * * TUE", CronUtils.getWeeklyCronExpression( 0, 0, 2 ) );
        assertEquals( "0 0 0 * * WED", CronUtils.getWeeklyCronExpression( 0, 0, 3 ) );
        assertEquals( "0 0 0 * * THU", CronUtils.getWeeklyCronExpression( 0, 0, 4 ) );
        assertEquals( "0 0 0 * * FRI", CronUtils.getWeeklyCronExpression( 0, 0, 5 ) );
        assertEquals( "0 0 0 * * SAT", CronUtils.getWeeklyCronExpression( 0, 0, 6 ) );
        assertEquals( "0 0 0 * * SUN", CronUtils.getWeeklyCronExpression( 0, 0, 7 ) );
    }

    @Test
    public void getMonthlyCronExpression()
    {
        assertEquals( "0 0 0 15 */1 *", CronUtils.getMonthlyCronExpression( 0, 0, 15 ) );
    }
}
