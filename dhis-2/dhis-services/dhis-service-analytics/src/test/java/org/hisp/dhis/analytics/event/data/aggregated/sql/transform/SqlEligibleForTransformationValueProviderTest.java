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
package org.hisp.dhis.analytics.event.data.aggregated.sql.transform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.event.data.aggregated.sql.transform.provider.SqlEligibleForTransformationValueProvider;
import org.junit.Test;

public class SqlEligibleForTransformationValueProviderTest extends DhisSpringTest
{
    private String notEligibleSql;

    private String eligibleSql;

    @Override
    public void setUpTest()
    {
        notEligibleSql = "SELECT count(psi) AS value, '20200201' AS Daily \n" +
            "FROM analytics_event_pnclhazartz AS ax \n" +
            "WHERE CAST(\"PFXeJV8d7ja\" AS date) < CAST('2020-02-02' AS date) \n" +
            "AND CAST(\"PFXeJV8d7ja\" AS date) >= CAST('2020-02-01' AS date) \n" +
            "AND (ax.\"uidlevel1\" IN ('VGTTybr8UcS')) LIMIT 100001";

        eligibleSql = "select count(pi) as value,'20200201' as Daily \n" +
            "from analytics_enrollment_uyjxktbwrnf as ax where \n" +
            "cast((select \"PFXeJV8d7ja\" from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n" +
            "order by executiondate desc limit 1 ) as date) < cast( '2020-02-02' as date )and \n" +
            "cast((select \"PFXeJV8d7ja\" from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi and \"PFXeJV8d7ja\" is not null and ps = 'LpWNjNGvCO5' \n" +
            "order by executiondate desc limit 1 ) as date) >= cast( '2020-02-01' as date )\n" +
            "and (uidlevel1 = 'VGTTybr8UcS' ) \n" +
            "and (((select count(\"ovY6E8BSdto\") \n" +
            "from analytics_event_uYjxkTbwRNf \n" +
            "where analytics_event_uYjxkTbwRNf.pi = ax.pi \n" +
            "and \"ovY6E8BSdto\" is not null \n" +
            "and \"ovY6E8BSdto\" = 'Positive' \n" +
            "and ps = 'dDHkBd3X8Ce') > 0)) limit 100001";
    }

    @Test
    public void verifySqlEligibleForTransformationValueProviderWithEligibleSql()
    {
        SqlEligibleForTransformationValueProvider provider = new SqlEligibleForTransformationValueProvider();

        boolean isEligible = provider.getProvider().apply( eligibleSql );

        assertTrue( isEligible );
    }

    @Test
    public void verifySqlEligibleForTransformationValueProviderWithNoEligibleSql()
    {
        SqlEligibleForTransformationValueProvider provider = new SqlEligibleForTransformationValueProvider();

        boolean isEligible = provider.getProvider().apply( notEligibleSql );

        assertFalse( isEligible );
    }
}
