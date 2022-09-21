/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.analytics;

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.analytics.AggregationType.*;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.fromAggregationType;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class AnalyticsAggregationTypeTest
{
    @Test
    public void verifyFromAggregationType()
    {
        assertAggregationType( fromAggregationType( AVERAGE_SUM_ORG_UNIT ), SUM, AVERAGE );
        assertAggregationType( fromAggregationType( LAST ), SUM, LAST );
        assertAggregationType( fromAggregationType( LAST_AVERAGE_ORG_UNIT ), AVERAGE, LAST );
        assertAggregationType( fromAggregationType( FIRST ), SUM, FIRST );
        assertAggregationType( fromAggregationType( FIRST_AVERAGE_ORG_UNIT ), AVERAGE, FIRST );
        assertAggregationType( fromAggregationType( SUM ), SUM, SUM );
        assertAggregationType( fromAggregationType( LAST_IN_PERIOD ), SUM, LAST_IN_PERIOD );
        assertAggregationType( fromAggregationType( LAST_IN_PERIOD_AVERAGE_ORG_UNIT ), AVERAGE, LAST_IN_PERIOD );
    }

    private void assertAggregationType( AnalyticsAggregationType analyticsAggregationType,
        AggregationType aggregationType, AggregationType periodAggregationType )
    {
        assertThat( analyticsAggregationType.getAggregationType(), is( aggregationType ) );
        assertThat( analyticsAggregationType.getPeriodAggregationType(), is( periodAggregationType ) );
    }
}