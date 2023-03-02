/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.util;

import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_5_YEARS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PeriodCriteriaUtils}.
 */
class PeriodCriteriaUtilsTest
{
    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Event_forRelativePeriod()
    {
        // given
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = configureEventsAnalyticsQueryCriteriaWithPeriod(
            LAST_5_YEARS.name() );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( eventsAnalyticsQueryCriteria, LAST_5_YEARS );

        // then
        assertTrue( eventsAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( eventsAnalyticsQueryCriteria.getDesc() );
        assertEquals( eventsAnalyticsQueryCriteria.getDimension().stream().findFirst().get(),
            PERIOD_DIM_ID + ":" + LAST_5_YEARS );
    }

    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Event_forNoRelativePeriod()
    {
        // given
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = configureEventsAnalyticsQueryCriteriaWithPeriod(
            null );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( eventsAnalyticsQueryCriteria, LAST_5_YEARS );

        // then
        assertTrue( eventsAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( eventsAnalyticsQueryCriteria.getDesc() );
        assertEquals( eventsAnalyticsQueryCriteria.getDimension().stream().findFirst().get(),
            PERIOD_DIM_ID + ":" + LAST_5_YEARS );
    }

    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Event_forStartAndEndDate()
    {
        // given
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = configureEventsAnalyticsQueryCriteriaWithDateRange(
            new Date(), new Date() );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( eventsAnalyticsQueryCriteria, LAST_5_YEARS );

        // then
        assertFalse( eventsAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( eventsAnalyticsQueryCriteria.getDesc() );
    }

    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Enrollment_forRelativePeriod()
    {
        // given
        EnrollmentAnalyticsQueryCriteria enrollmentsAnalyticsQueryCriteria = configureEnrollmentAnalyticsQueryCriteriaWithPeriod(
            LAST_5_YEARS.name() );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( enrollmentsAnalyticsQueryCriteria,
            LAST_5_YEARS );

        // then
        assertTrue( enrollmentsAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( enrollmentsAnalyticsQueryCriteria.getDesc() );
        assertEquals( enrollmentsAnalyticsQueryCriteria.getDimension().stream().findFirst().get(),
            PERIOD_DIM_ID + ":" + LAST_5_YEARS );
    }

    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Enrollment_forNoRelativePeriod()
    {
        // given
        EnrollmentAnalyticsQueryCriteria enrollmentAnalyticsQueryCriteria = configureEnrollmentAnalyticsQueryCriteriaWithPeriod(
            null );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( enrollmentAnalyticsQueryCriteria, LAST_5_YEARS );

        // then
        assertTrue( enrollmentAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( enrollmentAnalyticsQueryCriteria.getDesc() );
        assertEquals( enrollmentAnalyticsQueryCriteria.getDimension().stream().findFirst().get(),
            PERIOD_DIM_ID + ":" + LAST_5_YEARS );
    }

    @Test
    void testDefineDefaultPeriodDimensionCriteriaWithOrderBy_Enrollment_forStartAndEndDate()
    {
        // given
        EnrollmentAnalyticsQueryCriteria enrollmentAnalyticsQueryCriteria = configureEnrollmentsAnalyticsQueryCriteriaWithDateRange(
            new Date(), new Date() );

        // when
        PeriodCriteriaUtils.defineDefaultPeriodForCriteria( enrollmentAnalyticsQueryCriteria, LAST_5_YEARS );

        // then
        assertFalse( enrollmentAnalyticsQueryCriteria.getDimension().stream().findFirst().isPresent() );
        assertNull( enrollmentAnalyticsQueryCriteria.getDesc() );
    }

    private EventsAnalyticsQueryCriteria configureEventsAnalyticsQueryCriteriaWithPeriod( String period )
    {
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = getDefaultEventsAnalyticsQueryCriteria();

        if ( period != null )
        {
            eventsAnalyticsQueryCriteria.getDimension().add( period );
        }

        return eventsAnalyticsQueryCriteria;
    }

    private EventsAnalyticsQueryCriteria configureEventsAnalyticsQueryCriteriaWithDateRange( Date startDate,
        Date endDate )
    {
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = getDefaultEventsAnalyticsQueryCriteria();

        eventsAnalyticsQueryCriteria.setStartDate( startDate );
        eventsAnalyticsQueryCriteria.setEndDate( endDate );

        return eventsAnalyticsQueryCriteria;
    }

    private EnrollmentAnalyticsQueryCriteria configureEnrollmentAnalyticsQueryCriteriaWithPeriod( String period )
    {
        EnrollmentAnalyticsQueryCriteria enrollmentsAnalyticsQueryCriteria = getDefaultEnrollmentsAnalyticsQueryCriteria();

        if ( period != null )
        {
            enrollmentsAnalyticsQueryCriteria.getDimension().add( period );
        }

        return enrollmentsAnalyticsQueryCriteria;
    }

    private EnrollmentAnalyticsQueryCriteria configureEnrollmentsAnalyticsQueryCriteriaWithDateRange( Date startDate,
        Date endDate )
    {
        EnrollmentAnalyticsQueryCriteria enrollmentsAnalyticsQueryCriteria = getDefaultEnrollmentsAnalyticsQueryCriteria();

        enrollmentsAnalyticsQueryCriteria.setStartDate( startDate );
        enrollmentsAnalyticsQueryCriteria.setEndDate( endDate );

        return enrollmentsAnalyticsQueryCriteria;
    }

    private EventsAnalyticsQueryCriteria getDefaultEventsAnalyticsQueryCriteria()
    {
        EventsAnalyticsQueryCriteria eventsAnalyticsQueryCriteria = new EventsAnalyticsQueryCriteria();
        Set<String> dimensions = new HashSet<>();
        eventsAnalyticsQueryCriteria.setDimension( dimensions );

        return eventsAnalyticsQueryCriteria;
    }

    private EnrollmentAnalyticsQueryCriteria getDefaultEnrollmentsAnalyticsQueryCriteria()
    {
        EnrollmentAnalyticsQueryCriteria enrollmentsAnalyticsQueryCriteria = new EnrollmentAnalyticsQueryCriteria();
        Set<String> dimensions = new HashSet<>();
        enrollmentsAnalyticsQueryCriteria.setDimension( dimensions );

        return enrollmentsAnalyticsQueryCriteria;
    }
}
