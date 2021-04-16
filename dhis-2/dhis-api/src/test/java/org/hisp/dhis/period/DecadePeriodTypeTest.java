/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.period;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Dusan Bernat
 */
public class DecadePeriodTypeTest
{
    private CalendarPeriodType periodType;

    private DateTime testDate;

    @Before
    public void before() {
        periodType = new YearlyPeriodType();
    }

    @Test
    public void testCreatePeriod() {
        testDate = new DateTime(2020, 8, 15, 0, 0);

        DateTime startDate = new DateTime(2020, 1, 1, 0, 0);
        DateTime endDate = new DateTime(2020, 12, 31, 0, 0);

        Period period = periodType.createPeriod(testDate.toDate());

        assertEquals(startDate.toDate(), period.getStartDate());
        assertEquals(endDate.toDate(), period.getEndDate());

        testDate = new DateTime(2020, 4, 15, 0, 0);

        period = periodType.createPeriod(testDate.toDate());

        assertEquals(startDate.toDate(), period.getStartDate());
        assertEquals(endDate.toDate(), period.getEndDate());
    }

    @Test
    public void testGenerateLastDecade() {
        testDate = new DateTime(2020, 4, 15, 0, 0);

        List<Period> periods = new DecadePeriodType().generateLastYears(testDate.toDate());

        assertEquals(10, periods.size());
        assertEquals(periodType.createPeriod(new DateTime(2011, 1, 1, 0, 0).toDate()), periods.get(0));
        assertEquals(periodType.createPeriod(new DateTime(2012, 1, 1, 0, 0).toDate()), periods.get(1));
        assertEquals(periodType.createPeriod(new DateTime(2013, 1, 1, 0, 0).toDate()), periods.get(2));
        assertEquals(periodType.createPeriod(new DateTime(2014, 1, 1, 0, 0).toDate()), periods.get(3));
        assertEquals(periodType.createPeriod(new DateTime(2015, 1, 1, 0, 0).toDate()), periods.get(4));
        assertEquals(periodType.createPeriod(new DateTime(2016, 1, 1, 0, 0).toDate()), periods.get(5));
        assertEquals(periodType.createPeriod(new DateTime(2017, 1, 1, 0, 0).toDate()), periods.get(6));
        assertEquals(periodType.createPeriod(new DateTime(2018, 1, 1, 0, 0).toDate()), periods.get(7));
        assertEquals(periodType.createPeriod(new DateTime(2019, 1, 1, 0, 0).toDate()), periods.get(8));
        assertEquals(periodType.createPeriod(new DateTime(2020, 1, 1, 0, 0).toDate()), periods.get(9));

    }
}
