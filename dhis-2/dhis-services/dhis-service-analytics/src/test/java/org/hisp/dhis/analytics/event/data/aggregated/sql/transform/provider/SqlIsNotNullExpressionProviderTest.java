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
package org.hisp.dhis.analytics.event.data.aggregated.sql.transform.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.analytics.event.data.aggregated.sql.transform.model.element.where.PredicateElement;
import org.junit.Test;

public class SqlIsNotNullExpressionProviderTest extends DhisTest
{
    private String sql;

    @Override
    public void setUpTest()
    {
        sql = "SELECT count (DISTINCT pi) AS value\n" +
            "FROM analytics_enrollment_uyjxktbwrnf AS ax\n" +
            " WHERE CAST((  SELECT \"PFXeJV8d7ja\"\n" +
            "                 FROM analytics_event_uYjxkTbwRNf\n" +
            "                WHERE analytics_event_uYjxkTbwRNf.pi = ax.pi\n" +
            "                  AND \"PFXeJV8d7ja\" IS NOT null\n" +
            "                  AND ps = 'LpWNjNGvCO5'\n" +
            "             ORDER BY executiondate DESC\n" +
            "                LIMIT 1) AS DATE) >= CAST('2021-01-01' AS DATE)\n" +
            "   AND CAST((  SELECT \"PFXeJV8d7ja\"\n" +
            "                 FROM analytics_event_uYjxkTbwRNf\n" +
            "                WHERE analytics_event_uYjxkTbwRNf.pi = ax.pi\n" +
            "                  AND \"PFXeJV8d7ja\" IS NOT null\n" +
            "                  AND ps = 'LpWNjNGvCO5'\n" +
            "             ORDER BY executiondate DESC\n" +
            "                LIMIT 1) AS DATE) < CAST('2022-01-01' AS DATE)\n" +
            "   AND (uidlevel1 = 'VGTTybr8UcS')\n" +
            "   AND (((SELECT count (\"ovY6E8BSdto\")\n" +
            "            FROM analytics_event_uYjxkTbwRNf\n" +
            "           WHERE analytics_event_uYjxkTbwRNf.pi = ax.pi\n" +
            "             AND \"ovY6E8BSdto\" IS NOT null\n" +
            "             AND \"ovY6E8BSdto\" = 'Positive'\n" +
            "             AND ps = 'dDHkBd3X8Ce') > 0))\n" +
            " LIMIT 100001";
    }

    @Test
    public void verifySqlIsNotNullExpressionProvider()
    {
        SqlIsNotNullExpressionProvider provider = new SqlIsNotNullExpressionProvider();

        List<PredicateElement> predicateElementList = provider.getProvider().apply( sql );

        assertEquals( 1, predicateElementList.size() );

        assertEquals( "is not null", predicateElementList.get( 0 ).getRelation() );

        assertEquals( "PFXeJV8d7ja.\"PFXeJV8d7ja\"", predicateElementList.get( 0 ).getLeftExpression() );

        assertEquals( "and", predicateElementList.get( 0 ).getLogicalOperator() );
    }
}
