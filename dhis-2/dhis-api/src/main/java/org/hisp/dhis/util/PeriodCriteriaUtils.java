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

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;

import lombok.NoArgsConstructor;

import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.hisp.dhis.period.RelativePeriodEnum;

/**
 * Helper class that provides supportive methods to deal with query criteria and
 * periods.
 */
@NoArgsConstructor( access = PRIVATE )
public class PeriodCriteriaUtils
{
    /**
     * Defines a default period for the given criteria, if none is present.
     *
     * @param criteria {@link EventsAnalyticsQueryCriteria} query criteria.
     * @param defaultPeriod the default period to set, based on
     *        {@link RelativePeriodEnum}.
     */
    public static void defineDefaultPeriodForCriteria( EventsAnalyticsQueryCriteria criteria,
        RelativePeriodEnum defaultPeriod )
    {
        if ( !hasPeriod( criteria ) )
        {
            criteria.getDimension().add( PERIOD_DIM_ID + ":" + defaultPeriod.name() );
        }
    }

    /**
     * Defines a default period for the given criteria, if none is present.
     *
     * @param criteria {@link EnrollmentAnalyticsQueryCriteria} query criteria.
     * @param defaultPeriod the default period to set, based on
     *        {@link RelativePeriodEnum}.
     */
    public static void defineDefaultPeriodForCriteria( EnrollmentAnalyticsQueryCriteria criteria,
        RelativePeriodEnum defaultPeriod )
    {
        if ( !hasPeriod( criteria ) )
        {
            criteria.getDimension().add( PERIOD_DIM_ID + ":" + defaultPeriod.name() );
        }
    }

    /**
     * Checks is the given {@link EventsAnalyticsQueryCriteria} object has a
     * period dimension set.
     *
     * @param criteria {@link EventsAnalyticsQueryCriteria} query criteria.
     * @return true if the criteria contains a period dimension, (start and end
     *         date) or event date. False, otherwise.
     */
    private static boolean hasPeriod( EventsAnalyticsQueryCriteria criteria )
    {
        return criteria.getDimension().stream().anyMatch( d -> d.startsWith( PERIOD_DIM_ID ) )
            || !isBlank( criteria.getEventDate() )
            || !isBlank( criteria.getEnrollmentDate() )
            || (criteria.getStartDate() != null && criteria.getEndDate() != null)
            || !isBlank( criteria.getIncidentDate() )
            || !isBlank( criteria.getLastUpdated() )
            || !isBlank( criteria.getScheduledDate() )
            || criteria.getRelativePeriodDate() != null;
    }

    /**
     * Checks is the given {@link EnrollmentAnalyticsQueryCriteria} object has a
     * period dimension set.
     *
     * @param criteria {@link EnrollmentAnalyticsQueryCriteria} query criteria.
     * @return true if the criteria contains a period dimension, (start and end
     *         date) or enrollment date. False, otherwise.
     */
    private static boolean hasPeriod( EnrollmentAnalyticsQueryCriteria criteria )
    {
        return criteria.getDimension().stream().anyMatch( d -> d.startsWith( PERIOD_DIM_ID ) )
            || !isBlank( criteria.getEnrollmentDate() )
            || (criteria.getStartDate() != null && criteria.getEndDate() != null)
            || !isBlank( criteria.getIncidentDate() )
            || !isBlank( criteria.getLastUpdated() )
            || criteria.getRelativePeriodDate() != null;
    }
}
