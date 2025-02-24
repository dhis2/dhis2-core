/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.analytics.util.optimizer.cte.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import org.junit.jupiter.api.Test;

class CteOptimizationPipelineTest {

  private final CteOptimizationPipeline pipeline = new CteOptimizationPipeline();

  @Test
  void testLastScheduled() {
    String originalSql =
        """
        with pi_hgtnuhsqbml as (
          select
            enrollment,
            sum(1+1) as value
           from
              analytics_enrollment_ur1edk5oe2n as subax
           where
              ((
               date_part('year', age(cast(
              (
              select
                  scheduleddate
              from
                  analytics_event_ur1edk5oe2n
              where
                  analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                  and scheduleddate is not null
              order by
                  occurreddate desc
              limit 1 ) as date),
              cast(coalesce(completeddate,
              (
              select
                  created
              from
                  analytics_event_ur1edk5oe2n
              where
                  analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                  and created is not null
              order by
                  occurreddate desc
              limit 1 )) as date)))) * 12 ) > 1
              and "lw1SqmMlnfh" is not null
           group by
              enrollment )
           -- end of CTE
           select
              ax.enrollment
           from
              analytics_enrollment_ur1edk5oe2n as ax
           where
              (((
              lastupdated >= '2018-01-01' and lastupdated < '2018-04-28')))
              and ( ax."uidlevel1" = 'ImspTQPwCqd' )
      """;

    String expectedSql =
        """
        with last_sched as (
        select
            enrollment,
            scheduleddate
        from
            (
            select
                enrollment,
                scheduleddate,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1edk5oe2n
            where
                scheduleddate is not null) t
        where
            rn = 1),
        last_created as (
        select
            enrollment,
            created
        from
            (
            select
                enrollment,
                created,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1edk5oe2n
            where
                created is not null) t
        where
            rn = 1),
        pi_hgtnuhsqbml as (
        select
            subax.enrollment,
            sum((1 + 1)) as value
        from
            analytics_enrollment_ur1edk5oe2n as subax
        left join last_sched as ls on
            subax.enrollment = ls.enrollment
        left join last_created as lc on
            subax.enrollment = lc.enrollment
        where
            ((date_part('year',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date)))) * 12) > 1
                and "lw1SqmMlnfh" is not null
            group by
                subax.enrollment)
        select
            ax.enrollment
        from
            analytics_enrollment_ur1edk5oe2n as ax
        where
            (((lastupdated >= '2018-01-01'
                and lastupdated < '2018-04-28')))
            and (ax."uidlevel1" = 'ImspTQPwCqd')
      """;
    String transformedSql = pipeline.optimize(originalSql);
    assertEquals(normalizeSqlQuery(expectedSql), normalizeSqlQuery(transformedSql));
  }

  @Test
  void testLastCreated() {
    String originalSql =
        """
        with pi_inputcte AS (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n AS subax
            where (
                select   created
                from     analytics_event_ur1edk5oe2n
                where    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                and      created IS NOT NULL
                order by occurreddate DESC
                limit    1
            ) IS NOT NULL
        )
        select * from pi_inputcte;
        """;

    String expectedSql =
        """
        with last_created as (
            select
                enrollment,
                created
            from
                (
                select
                    enrollment,
                    created,
                    row_number() over (partition by enrollment
                order by
                    occurreddate desc) as rn
                from
                    analytics_event_ur1edk5oe2n
                where
                    created is not null) t
            where
                rn = 1),
            pi_inputcte as (
            select
                subax.enrollment
            from
                analytics_enrollment_ur1edk5oe2n as subax
            left join last_created as lc on
                subax.enrollment = lc.enrollment
            where
                lc.created is not null)
            select
                *
            from
                pi_inputcte
        """;
    String transformedSql = pipeline.optimize(originalSql);
    assertEquals(normalizeSqlQuery(expectedSql), normalizeSqlQuery(transformedSql));
  }

  @Test
  void testAggregatedRelationshipCount() {
    String originalSql =
        """
        with pi_inputcte AS (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n AS subax
            where (
                (select
                    sum(relationship_count)
                from
                    analytics_rs_relationship arr
                where
                    arr.trackedentityid = subax.trackedentity) > 10
            )
        )
        select * from analytics_enrollment_ur1edk5oe2n;
        """;

    String expectedSql =
        """
        with relationship_count_agg as (
        select
            trackedentityid,
            sum(relationship_count) as relationship_count
        from
            analytics_rs_relationship
        group by
            trackedentityid),
        pi_inputcte as (
        select
            subax.enrollment
        from
            analytics_enrollment_ur1edk5oe2n as subax
        left join relationship_count_agg as rlc on
            subax.trackedentity = rlc.trackedentityid
        where
            (rlc.relationship_count > 10))
        select
            *
        from
            analytics_enrollment_ur1edk5oe2n
        """;
    String transformedSql = pipeline.optimize(originalSql);
    assertEquals(normalizeSqlQuery(expectedSql), normalizeSqlQuery(transformedSql));
  }

  @Test
  void testNonAggregatedRelationshipCount() {
    String originalSql =
        """
        with pi_inputcte AS (
            select subax.enrollment
            from analytics_enrollment_ur1edk5oe2n AS subax
            where (
                (select relationship_count
                 from analytics_rs_relationship arr
                 where arr.trackedentityid = subax.trackedentity
                 and relationshiptypeuid = 'dk34dj3') > 10
            )
        )
        select * from analytics_enrollment_ur1edk5oe2n;
        """;

    String expectedSql =
        """
                with relationship_count as (
                select
                    trackedentityid,
                    relationship_count
                from
                    analytics_rs_relationship
                where
                    relationshiptypeuid = 'dk34dj3'),
                pi_inputcte as (
                select
                    subax.enrollment
                from
                    analytics_enrollment_ur1edk5oe2n as subax
                left join relationship_count as rlc on
                    subax.trackedentity = rlc.trackedentityid
                where
                    (rlc.relationship_count > 10))
                select
                    *
                from
                    analytics_enrollment_ur1edk5oe2n
                """;
    String transformedSql = pipeline.optimize(originalSql);
    assertEquals(normalizeSqlQuery(expectedSql), normalizeSqlQuery(transformedSql));
  }

  @Test
  void testLastScheduledComplex() {

    String originalQuery =
        """
        with pi_hgtnuhsqbml AS (
            select
                enrollment
            from analytics_enrollment_ur1edk5oe2n AS subax
            where ((
                date_part('year',age(cast(
                                     (
                                     select   scheduleddate
                                     from     analytics_event_ur1edk5oe2n
                                     where    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                     and      scheduleddate IS NOT NULL
                                     order by occurreddate DESC
                                     limit    1 ) AS date), cast(coalesce(completeddate,
                          (
                                   select   created
                                   from     analytics_event_ur1edk5oe2n
                                   where    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                   and      created IS NOT NULL
                                   order by occurreddate DESC
                                   limit    1 )) AS date)))) * 12 + date_part('month',age(cast(
                                            (
                                            select   scheduleddate
                                            from     analytics_event_ur1edk5oe2n
                                            where    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                            and      scheduleddate IS NOT NULL
                                            order by occurreddate DESC
                                            limit    1 ) AS date), cast(coalesce(completeddate,
                 (
                          select   created
                          from     analytics_event_ur1edk5oe2n
                          where    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                          and      created IS NOT NULL
                          order by occurreddate DESC
                          limit    1 )) AS date)))) > 1
                          GROUP BY enrollment
            )
            select
                ax.enrollment
            from analytics_enrollment_ur1edk5oe2n AS ax
            left join pi_hgtnuhsqbml kektm ON kektm.enrollment = ax.enrollment
            where (lastupdated >= '2015-01-01' and lastupdated < '2025-01-01')
            and ax."uidlevel1" = 'ImspTQPwCqd'
            limit 101
            OFFSET 0
        """;

    String expected =
        """
        with last_sched as (
         select
            enrollment,
            scheduleddate
         from
            (
            select
                enrollment,
                scheduleddate,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1edk5oe2n
            where
                scheduleddate is not null) t
         where
            rn = 1),
         last_created as (
         select
            enrollment,
            created
         from
            (
            select
                enrollment,
                created,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1edk5oe2n
            where
                created is not null) t
         where
            rn = 1),
         pi_hgtnuhsqbml as (
         select
            subax.enrollment
         from
            analytics_enrollment_ur1edk5oe2n as subax
         left join last_sched as ls on
            subax.enrollment = ls.enrollment
         left join last_created as lc on
            subax.enrollment = lc.enrollment
         where
            (((date_part('year',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date)))) * 12 + date_part('month',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date))))) > 1
         group by
            subax.enrollment)
         select
            ax.enrollment
         from
            analytics_enrollment_ur1edk5oe2n as ax
         left join pi_hgtnuhsqbml kektm on
            kektm.enrollment = ax.enrollment
         where
            (lastupdated >= '2015-01-01'
                and lastupdated < '2025-01-01')
            and ax."uidlevel1" = 'ImspTQPwCqd'
         limit 101 offset 0
        """;

    String transformedSql = pipeline.optimize(originalQuery);
    assertEquals(
        normalizeSqlQuery(expected),
        normalizeSqlQuery(transformedSql),
        "SQL queries should be equivalent after normalization");
  }

  @Test
  void testComplex() {

    String originalQuery =
        """
        with pi_qZOBw051LSf as (
        select
            enrollment,
            sum(1+1) as value
        from
            analytics_enrollment_iphinat79uw as subax
        where
            (date_part('year',
            age(cast(enrollmentdate as date),
            cast((
            select
                created
            from
                analytics_event_IpHINAT79UW
            where
                analytics_event_IpHINAT79UW.enrollment = subax.enrollment
                and created is not null
            order by
                occurreddate desc
            limit 1 ) as date)))) >= 5
            and (date_part('year',
            age(cast(enrollmentdate as date),
            cast(completeddate as date)))) < 1
            and coalesce("cejWyOfXge6"::text,
            '') = 'MALE'
            and (coalesce((
            select
                "H6uSAMO5WLD"
            from
                analytics_event_IpHINAT79UW
            where
                analytics_event_IpHINAT79UW.enrollment = subax.enrollment
                and "H6uSAMO5WLD" is not null
                and ps = 'A03MvHHogjR'
            order by
                occurreddate desc
            limit 1 )::text,
            '') = 'RDT'
            or "w75KJ2mc4zz" is not null)
            and (coalesce((
            select
                "cYGaxwK615G"
            from
                analytics_event_IpHINAT79UW
            where
                analytics_event_IpHINAT79UW.enrollment = subax.enrollment
                and "cYGaxwK615G" is not null
                and ps = 'ZzYYXq4fJie'
            order by
                occurreddate desc
            limit 1 )::text,
            '') = 'POSITIVE'
            or coalesce((
            select
                "cYGaxwK615G"
            from
                analytics_event_IpHINAT79UW
            where
                analytics_event_IpHINAT79UW.enrollment = subax.enrollment
                and "cYGaxwK615G" is not null
                and ps = 'ZzYYXq4fJie'
            order by
                occurreddate desc
            limit 1 )::text,
            '') = 'NEGATIVE')
            or not (3.14 = 0)
            or '2015-01-01' is not null
        group by
            enrollment )
        select
            ax.enrollment,
            coalesce(tmwkc.value,
            0) as qZOBw051LSf
        from
            analytics_enrollment_iphinat79uw as ax
        left join pi_qZOBw051LSf tmwkc on
            tmwkc.enrollment = ax.enrollment
        where
            (((lastupdated >= '2015-01-01'
                and lastupdated < '2025-01-01')))
            and (ax."uidlevel1" = 'ImspTQPwCqd' )
            and enrollmentstatus in ('COMPLETED')
        order by
            "lastupdated" desc nulls last
        limit 101 offset 0
        """;

    String expected =
        """
        with last_created as (
        select
            enrollment,
            created
        from
            (
            select
                enrollment,
                created,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_IpHINAT79UW
            where
                created is not null) t
        where
            rn = 1),
        last_value_h6usamo5wld as (
        select
            enrollment,
            "H6uSAMO5WLD"
        from
            (
            select
                enrollment,
                "H6uSAMO5WLD",
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_IpHINAT79UW
            where
                "H6uSAMO5WLD" is not null) t
        where
            rn = 1),
        last_value_cygaxwk615g as (
        select
            enrollment,
            "cYGaxwK615G"
        from
            (
            select
                enrollment,
                "cYGaxwK615G",
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_IpHINAT79UW
            where
                "cYGaxwK615G" is not null) t
        where
            rn = 1),
        pi_qZOBw051LSf as (
        select
            subax.enrollment,
            sum((1 + 1)) as value
        from
            analytics_enrollment_iphinat79uw as subax
        left join last_created as lc on
            subax.enrollment = lc.enrollment
        left join last_value_h6usamo5wld as lv_H6uSAMO5WLD on
            subax.enrollment = lv_H6uSAMO5WLD.enrollment
        left join last_value_cygaxwk615g as lv_cYGaxwK615G on
            subax.enrollment = lv_cYGaxwK615G.enrollment
        where
            (((((date_part('year',
            age(cast(enrollmentdate as date),
            cast(lc.created as date)))) >= 5
                and (date_part('year',
                age(cast(enrollmentdate as date),
                cast(completeddate as date)))) < 1
                    and coalesce(cast("cejWyOfXge6" as text),
                    '') = 'MALE'
                        and (((coalesce(cast(lv_H6uSAMO5WLD."H6uSAMO5WLD" as text),
                        '') = 'RDT'))
                            or (("w75KJ2mc4zz" is not null)))
                            and (((coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                            '') = 'POSITIVE'))
                                or ((coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                                '') = 'NEGATIVE')))))
                or ((not (3.14 = 0)))))
                or (('2015-01-01' is not null))
            group by
                subax.enrollment)
        select
            ax.enrollment,
            coalesce(tmwkc.value,
            0) as qZOBw051LSf
        from
            analytics_enrollment_iphinat79uw as ax
        left join pi_qZOBw051LSf tmwkc on
            tmwkc.enrollment = ax.enrollment
        where
            (((lastupdated >= '2015-01-01'
                and lastupdated < '2025-01-01')))
            and (ax."uidlevel1" = 'ImspTQPwCqd')
            and enrollmentstatus in ('COMPLETED')
        order by
            "lastupdated" desc nulls last
        limit 101 offset 0
        """;

    String transformedQuery = pipeline.optimize(originalQuery);
    assertEquals(
        normalizeSqlQuery(expected),
        normalizeSqlQuery(transformedQuery),
        "SQL queries should be equivalent after normalization");
  }

  @Test
  void testRelationshipCountComplex() {

    String originalQuery =
        """
        with pi_hgTNuHSqBmL as (
        select
            enrollment,
            sum(1+1) as value
        from
            analytics_enrollment_ur1edk5oe2n as subax
        where
            ((date_part('year',
            age(cast((
            select
                scheduleddate
            from
                analytics_event_ur1Edk5Oe2n
            where
                analytics_event_ur1Edk5Oe2n.enrollment = subax.enrollment
                and scheduleddate is not null
            order by
                occurreddate desc
            limit 1 ) as date),
            cast(coalesce(completeddate,
            (
            select
                created
            from
                analytics_event_ur1Edk5Oe2n
            where
                analytics_event_ur1Edk5Oe2n.enrollment = subax.enrollment
                and created is not null
            order by
                occurreddate desc
            limit 1 )) as date)))) * 12 + date_part('month',
            age(cast((
            select
                scheduleddate
            from
                analytics_event_ur1Edk5Oe2n
            where
                analytics_event_ur1Edk5Oe2n.enrollment = subax.enrollment
                and scheduleddate is not null
            order by
                occurreddate desc
            limit 1 ) as date),
            cast(coalesce(completeddate,
            (
            select
                created
            from
                analytics_event_ur1Edk5Oe2n
            where
                analytics_event_ur1Edk5Oe2n.enrollment = subax.enrollment
                and created is not null
            order by
                occurreddate desc
            limit 1 )) as date)))) > 1
            or nullif(cast((case
                when "OvY4VVhSDeJ" >= 0 then 1
                else 0
            end) as double precision),
            0) > 2
            or not ((
            select
                count("fCXKBdc27Bt")
            from
                analytics_event_ur1Edk5Oe2n
            where
                analytics_event_ur1Edk5Oe2n.enrollment = subax.enrollment
                and "fCXKBdc27Bt" is not null
                and "fCXKBdc27Bt" = 1
                and ps = 'EPEcjy3FWmI') > 0)
            and (extract(epoch
        from
            (cast(completeddate as timestamp) - cast(enrollmentdate as timestamp))) / 60) > 1
            and ("cejWyOfXge6" is not null)
            and (
            select
                sum(relationship_count)
            from
                analytics_rs_relationship arr
            where
                arr.trackedentityid = subax.trackedentity) > 0
            and "lw1SqmMlnfh" is not null
        group by
            enrollment )
        select
            ax.enrollment,
            coalesce(nvtqi.value,
            0) as hgTNuHSqBmL
        from
            analytics_enrollment_ur1edk5oe2n as ax
        left join pi_hgTNuHSqBmL nvtqi on
            nvtqi.enrollment = ax.enrollment
        where
            (((lastupdated >= '2015-01-01'
                and lastupdated < '2025-01-01')))
            and (ax."uidlevel1" = 'ImspTQPwCqd' )
            and nvtqi.value > 404
            and nvtqi.value is not null
        order by
            hgTNuHSqBmL asc nulls last
        limit 101 offset 0
        """;

    String expected =
        """
        with last_sched as (
         select
            enrollment,
            scheduleddate
         from
            (
            select
                enrollment,
                scheduleddate,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1Edk5Oe2n
            where
                scheduleddate is not null) t
         where
            rn = 1),
         last_created as (
         select
            enrollment,
            created
         from
            (
            select
                enrollment,
                created,
                row_number() over (partition by enrollment
            order by
                occurreddate desc) as rn
            from
                analytics_event_ur1Edk5Oe2n
            where
                created is not null) t
         where
            rn = 1),
         de_count_fCXKBdc27Bt as (
         select
            enrollment,
            count("fCXKBdc27Bt") as de_count
         from
            analytics_event_ur1Edk5Oe2n
         where
            "fCXKBdc27Bt" is not null
            and "fCXKBdc27Bt" = 1
            and ps = 'EPEcjy3FWmI'
         group by
            enrollment),
         relationship_count_agg as (
         select
            trackedentityid,
            sum(relationship_count) as relationship_count
         from
            analytics_rs_relationship
         group by
            trackedentityid),
         pi_hgTNuHSqBmL as (
         select
            subax.enrollment,
            sum((1 + 1)) as value
         from
            analytics_enrollment_ur1edk5oe2n as subax
         left join last_sched as ls on
            subax.enrollment = ls.enrollment
         left join last_created as lc on
            subax.enrollment = lc.enrollment
         left join de_count_fCXKBdc27Bt as dec_decount on
            subax.enrollment = dec_decount.enrollment
         left join relationship_count_agg as rlc on
            subax.trackedentity = rlc.trackedentityid
         where
            (((((((date_part('year',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date)))) * 12 + date_part('month',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date))))) > 1))
                or ((nullif(cast((case
                    when "OvY4VVhSDeJ" >= 0 then 1
                    else 0
                end) as double precision),
                0) > 2))))
                or ((not (dec_decount.de_count > 0)
                    and (((extract(epoch
                from
                    (cast(completeddate as timestamp) - cast(enrollmentdate as timestamp)))) / 60)) > 1
                        and ("cejWyOfXge6" is not null)
                            and rlc.relationship_count > 0
                            and "lw1SqmMlnfh" is not null))
            group by
                subax.enrollment)
         select
            ax.enrollment,
            coalesce(nvtqi.value,
            0) as hgTNuHSqBmL
         from
            analytics_enrollment_ur1edk5oe2n as ax
         left join pi_hgTNuHSqBmL nvtqi on
            nvtqi.enrollment = ax.enrollment
         where
            (((lastupdated >= '2015-01-01'
                and lastupdated < '2025-01-01')))
            and (ax."uidlevel1" = 'ImspTQPwCqd')
            and nvtqi.value > 404
            and nvtqi.value is not null
         order by
            hgTNuHSqBmL asc nulls last
         limit 101 offset 0
        """;

    String transformedQuery = pipeline.optimize(originalQuery);
    assertEquals(
        normalizeSqlQuery(expected),
        normalizeSqlQuery(transformedQuery),
        "SQL queries should be equivalent after normalization");
  }

  @Test
  void testComplexOperatorPrecedenceAndCaseExpressions() {
    // Test Case 1: Multiple OR conditions with ANDs
    String sql1 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (column2 < 1000)
                OR (column3 = 2 AND column4 = 3)
                OR (column5 = 4 AND column6 = 5)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    String transformedSql1 = pipeline.optimize(sql1);
    assertTrue(transformedSql1.contains("WHERE (("), "Should have nested parentheses");
    assertTrue(
        transformedSql1.contains(")) OR (("), "Should have parentheses around first OR conditions");
    assertTrue(
        transformedSql1.contains(")) OR (("),
        "Should have parentheses around second OR conditions");

    // Test Case 2: Nested CASE expressions with operator precedence
    String sql2 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (nullif(cast((case
                    when column1 is not null then
                        case when column2 > 50 then 2 else 1 end
                    else 0
                    end) as double precision), 0) < 1000)
                OR (column3 = 2)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    String transformedSql2 = pipeline.optimize(sql2);

    assertTrue(
        transformedSql2.contains(")) OR (("), "Should have parentheses around OR conditions");

    assertTrue(
        transformedSql2.contains(")) OR (("), "Should have parentheses around OR conditions");
  }

  @Test
  void testOperatorPrecedenceVariations() {
    // Test Case 1: Basic AND-OR with subquery
    String sql1 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (column2 < 1000)
                OR (column3 = 2)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    assertTransformation(sql1, "Basic AND-OR with subquery");

    // Test Case 2: Multiple OR conditions
    String sql2 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                OR (column2 < 1000)
                OR (column3 = 2)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    assertTransformation(sql2, "Multiple OR conditions");

    // Test Case 3: Complex nested CASE with OR
    String sql3 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (nullif(cast((case
                    when column1 is not null then
                        case when column2 > 50 then 2 else 1 end
                    else 0
                    end) as double precision), 0) < 1000)
                OR (column3 = 2)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    assertTransformation(sql3, "Complex nested CASE with OR");
  }

  @Test
  void testCastPreservation() {
    // Test Case 1: Simple date subtraction with cast
    String sql1 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
        )
        SELECT * FROM analytics_enrollment_test
        """;

    String transformed1 = pipeline.optimize(sql1);
    assertTrue(
        transformed1.contains("CAST('2020-06-01' AS date)"),
        "Should preserve CAST for date literal");
    assertTrue(
        transformed1.contains("CAST(lc.created AS date)"),
        "Should preserve CAST for column reference");

    // Test Case 2: Cast with complex expressions and operators
    String sql2 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (nullif(cast((case when column1 is not null then 1 else 0 end) as double precision), 0) < 1000)
        )
        SELECT * FROM analytics_enrollment_test
        """;

    String transformed2 = pipeline.optimize(sql2);
    assertTrue(transformed2.contains("CAST('2020-06-01' AS date)"), "Should preserve date CAST");
    assertTrue(transformed2.contains("CAST(lc.created AS date)"), "Should preserve created CAST");
    assertTrue(
        transformed2.contains("CAST((CASE WHEN"), "Should preserve CAST around CASE expression");
    assertTrue(
        transformed2.contains("AS double precision)"),
        "Should preserve CAST type for double precision");

    // Test Case 3: Your original complex query
    String sql3 =
        """
        WITH pi_test AS (
            SELECT enrollment
            FROM analytics_enrollment_test AS subax
            WHERE (cast('2020-06-01' as date) - cast(
                (SELECT created
                 FROM analytics_event_test
                 WHERE enrollment = subax.enrollment
                 AND created IS NOT NULL
                 ORDER BY occurreddate DESC
                 LIMIT 1) as date)) > 1
                AND (nullif(cast((case when "B6TnnFMgmCk" is not null then 1 else 0 end) as double precision), 0) < 1000)
                OR (1 = 2)
                AND coalesce((SELECT value FROM analytics_event_test
                             WHERE enrollment = subax.enrollment LIMIT 1), '') != 'DD'
        )
        SELECT * FROM analytics_enrollment_test
        """;

    String transformed3 = pipeline.optimize(sql3);
    assertTrue(
        transformed3.contains("CAST('2020-06-01' AS date)"), "Should preserve first date CAST");
    assertTrue(
        transformed3.contains("CAST(lc.created AS date)"), "Should preserve second date CAST");
    assertTrue(transformed3.contains("CAST((CASE WHEN"), "Should preserve CAST around CASE");
    // Verify the date subtraction operation is preserved
    assertTrue(
        transformed3.contains("CAST('2020-06-01' AS date) - CAST("),
        "Should preserve date subtraction with CASTs");
  }

  @Test
  void testSubqueryInSelectClause() {
    String originalQuery =
        """
            with pi_select_subquery as (
              select
                subax.enrollment,
                (select
                    "GQY2lXrypjO"
                 from
                    analytics_event_ur1edk5oe2n
                 where
                    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and "GQY2lXrypjO" is not null
                    and ps = 'ZzYYXq4fJie'
                 order by
                    occurreddate desc
                 limit 1) as data_element_value
              from
                analytics_enrollment_ur1edk5oe2n as subax
              where
                enrollmentstatus = 'COMPLETED'
              group by
                subax.enrollment
            )
            select
              ax.enrollment,
              ax.enrollmentdate,
              pi.data_element_value
            from
              analytics_enrollment_ur1edk5oe2n as ax
            left join pi_select_subquery pi on
              pi.enrollment = ax.enrollment
            where
              lastupdated >= '2018-01-01'
              and lastupdated < '2018-04-28'
              and ax."uidlevel1" = 'ImspTQPwCqd'
            """;

    String expectedQuery =
        """
            with last_value_gqy2lxrypjo as (
              select
                enrollment,
                "GQY2lXrypjO"
              from
                (
                select
                  enrollment,
                  "GQY2lXrypjO",
                  row_number() over (partition by enrollment
                order by
                  occurreddate desc) as rn
                from
                  analytics_event_ur1edk5oe2n
                where
                  "GQY2lXrypjO" is not null) t
              where
                rn = 1),
              pi_select_subquery as (
              select
                subax.enrollment,
                lv_GQY2lXrypjO."GQY2lXrypjO" as data_element_value
              from
                analytics_enrollment_ur1edk5oe2n as subax
              left join last_value_gqy2lxrypjo as lv_GQY2lXrypjO on
                subax.enrollment = lv_GQY2lXrypjO.enrollment
              where
                enrollmentstatus = 'COMPLETED'
              group by
                subax.enrollment)
              select
                ax.enrollment,
                ax.enrollmentdate,
                pi.data_element_value
              from
                analytics_enrollment_ur1edk5oe2n as ax
              left join pi_select_subquery pi on
                pi.enrollment = ax.enrollment
              where
                lastupdated >= '2018-01-01'
                and lastupdated < '2018-04-28'
                and ax."uidlevel1" = 'ImspTQPwCqd'
            """;

    String transformedQuery = pipeline.optimize(originalQuery);
    assertEquals(
        normalizeSqlQuery(expectedQuery),
        normalizeSqlQuery(transformedQuery),
        "SQL query with subquery in SELECT clause should be correctly transformed");
  }

  @Test
  void testSubqueriesInBothSelectAndWhereClause() {
    String originalQuery =
        """
            with pi_mixed_subqueries as (
              select
                subax.enrollment,
                (select
                    "GQY2lXrypjO"
                 from
                    analytics_event_ur1edk5oe2n
                 where
                    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and "GQY2lXrypjO" is not null
                    and ps = 'ZzYYXq4fJie'
                 order by
                    occurreddate desc
                 limit 1) as first_value,
                (select
                    "UXz7xuGCEhU"
                 from
                    analytics_event_ur1edk5oe2n
                 where
                    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and "UXz7xuGCEhU" is not null
                    and ps = 'A03MvHHogjR'
                 order by
                    occurreddate desc
                 limit 1) as second_value
              from
                analytics_enrollment_ur1edk5oe2n as subax
              where
                (select
                    created
                 from
                    analytics_event_ur1edk5oe2n
                 where
                    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                    and created is not null
                 order by
                    occurreddate desc
                 limit 1) is not null
                and (select
                      relationship_count
                    from
                      analytics_rs_relationship arr
                    where
                      arr.trackedentityid = subax.trackedentity) > 0
                and enrollmentstatus = 'COMPLETED'
              group by
                subax.enrollment
            )
            select
              ax.enrollment,
              pi.first_value,
              pi.second_value
            from
              analytics_enrollment_ur1edk5oe2n as ax
            left join pi_mixed_subqueries pi on
              pi.enrollment = ax.enrollment
            where
              lastupdated >= '2018-01-01'
              and lastupdated < '2018-04-28'
              and ax."uidlevel1" = 'ImspTQPwCqd'
            """;

    String expectedQuery =
        """
            with last_value_gqy2lxrypjo as (
              select
                enrollment,
                "GQY2lXrypjO"
              from
                (
                select
                  enrollment,
                  "GQY2lXrypjO",
                  row_number() over (partition by enrollment
                order by
                  occurreddate desc) as rn
                from
                  analytics_event_ur1edk5oe2n
                where
                  "GQY2lXrypjO" is not null) t
              where
                rn = 1),
              last_value_uxz7xugcehu as (
              select
                enrollment,
                "UXz7xuGCEhU"
              from
                (
                select
                  enrollment,
                  "UXz7xuGCEhU",
                  row_number() over (partition by enrollment
                order by
                  occurreddate desc) as rn
                from
                  analytics_event_ur1edk5oe2n
                where
                  "UXz7xuGCEhU" is not null) t
              where
                rn = 1),
              last_created as (
              select
                enrollment,
                created
              from
                (
                select
                  enrollment,
                  created,
                  row_number() over (partition by enrollment
                order by
                  occurreddate desc) as rn
                from
                  analytics_event_ur1edk5oe2n
                where
                  created is not null) t
              where
                rn = 1),
              relationship_count as (
              select
                trackedentityid,
                relationship_count
              from
                analytics_rs_relationship),
              pi_mixed_subqueries as (
              select
                subax.enrollment,
                lv_GQY2lXrypjO."GQY2lXrypjO" as first_value,
                lv_UXz7xuGCEhU."UXz7xuGCEhU" as second_value
              from
                analytics_enrollment_ur1edk5oe2n as subax
              left join last_value_gqy2lxrypjo as lv_GQY2lXrypjO on
                subax.enrollment = lv_GQY2lXrypjO.enrollment
              left join last_value_uxz7xugcehu as lv_UXz7xuGCEhU on
                subax.enrollment = lv_UXz7xuGCEhU.enrollment
              left join last_created as lc on
                subax.enrollment = lc.enrollment
              left join relationship_count as rlc on
                subax.trackedentity = rlc.trackedentityid
              where
                lc.created is not null
                  and rlc.relationship_count > 0
                  and enrollmentstatus = 'COMPLETED'
              group by
                subax.enrollment)
              select
                ax.enrollment,
                pi.first_value,
                pi.second_value
              from
                analytics_enrollment_ur1edk5oe2n as ax
              left join pi_mixed_subqueries pi on
                pi.enrollment = ax.enrollment
              where
                lastupdated >= '2018-01-01'
                and lastupdated < '2018-04-28'
                and ax."uidlevel1" = 'ImspTQPwCqd'
            """;

    String normalizedTransformed = normalizeSqlQuery(pipeline.optimize(originalQuery));

    // Assert that all expected CTEs are present
    assertTrue(
        normalizedTransformed.contains("last_created as ("), "Should contain last_created CTE");
    assertTrue(
        normalizedTransformed.contains("relationship_count as ("),
        "Should contain relationship_count CTE");
    assertTrue(
        normalizedTransformed.contains("last_value_gqy2lxrypjo as ("),
        "Should contain last_value_gqy2lxrypjo CTE");
    assertTrue(
        normalizedTransformed.contains("last_value_uxz7xugcehu as ("),
        "Should contain last_value_uxz7xugcehu CTE");

    // Assert that all expected joins are present
    assertTrue(
        normalizedTransformed.contains("left join last_created as lc on"),
        "Should join with last_created");
    assertTrue(
        normalizedTransformed.contains("left join relationship_count as rlc on"),
        "Should join with relationship_count");
    assertTrue(
        normalizedTransformed.contains("left join last_value_gqy2lxrypjo as lv_gqy2lxrypjo on"),
        "Should join with last_value_gqy2lxrypjo");
    assertTrue(
        normalizedTransformed.contains("left join last_value_uxz7xugcehu as lv_uxz7xugcehu on"),
        "Should join with last_value_uxz7xugcehu");

    // Assert that the mapped columns are used in SELECT
    assertTrue(
        normalizedTransformed.contains("lv_gqy2lxrypjo.\"gqy2lxrypjo\" as first_value"),
        "Should map first_value to CTE column");
    assertTrue(
        normalizedTransformed.contains("lv_uxz7xugcehu.\"uxz7xugcehu\" as second_value"),
        "Should map second_value to CTE column");
  }

  @Test
  void testAggregateWithSubqueryInSelect() {
    String originalQuery =
        """
            with pi_aggregate_select as (
              select
                subax.enrollment,
                avg(coalesce(cast(
                  (select "GQY2lXrypjO"
                   from analytics_event_ur1edk5oe2n
                   where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                      and "GQY2lXrypjO" is not null
                      and ps = 'ZzYYXq4fJie'
                   order by occurreddate desc
                   limit 1) as decimal), 0) -
                  coalesce(cast(
                    (select "UXz7xuGCEhU"
                     from analytics_event_ur1edk5oe2n
                     where analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                        and "UXz7xuGCEhU" is not null
                        and ps = 'A03MvHHogjR'
                     order by occurreddate desc
                     limit 1) as decimal), 0)) as calculated_value
              from
                analytics_enrollment_ur1edk5oe2n as subax
              where
                enrollmentstatus = 'COMPLETED'
              group by
                subax.enrollment
            )
            select
              ax.enrollment,
              pi.calculated_value
            from
              analytics_enrollment_ur1edk5oe2n as ax
            left join pi_aggregate_select pi on
              pi.enrollment = ax.enrollment
            where
              ax."uidlevel1" = 'ImspTQPwCqd'
            """;

    // Run the transformation
    String transformedQuery = pipeline.optimize(originalQuery);
    String normalizedTransformed = normalizeSqlQuery(transformedQuery);

    // Assert that all expected CTEs are present
    assertTrue(
        normalizedTransformed.contains("last_value_gqy2lxrypjo as ("),
        "Should contain last_value_gqy2lxrypjo CTE");
    assertTrue(
        normalizedTransformed.contains("last_value_uxz7xugcehu as ("),
        "Should contain last_value_uxz7xugcehu CTE");

    // Assert that all expected joins are present
    assertTrue(
        normalizedTransformed.contains("left join last_value_gqy2lxrypjo as lv_gqy2lxrypjo on"),
        "Should join with last_value_gqy2lxrypjo");
    assertTrue(
        normalizedTransformed.contains("left join last_value_uxz7xugcehu as lv_uxz7xugcehu on"),
        "Should join with last_value_uxz7xugcehu");

    // Assert that the subqueries are properly transformed in the expression
    assertTrue(
        normalizedTransformed.contains(
            "avg( ( coalesce(cast(lv_gqy2lxrypjo.\"gqy2lxrypjo\" as decimal), 0) - coalesce(cast(lv_uxz7xugcehu.\"uxz7xugcehu\" as decimal), 0) ) )"),
        "Should transform subqueries in AVG function");

    // Assert that the original WHERE condition is preserved
    assertTrue(
        normalizedTransformed.contains("where enrollmentstatus = 'completed'"),
        "Should preserve the original WHERE condition");

    // Assert that the outer query structure is maintained
    assertTrue(
        normalizedTransformed.contains("select ax.enrollment, pi.calculated_value from"),
        "Should maintain the original SELECT structure");
    assertTrue(
        normalizedTransformed.contains("where ax.\"uidlevel1\" = 'imsptqpwcqd'"),
        "Should maintain the original WHERE condition in outer query");
  }

  private void assertTransformation(String sql, String testDescription) {
    String transformedSql = pipeline.optimize(sql);

    // Extract the pi_test CTE content more reliably
    int piTestStart = transformedSql.indexOf("pi_test AS (");
    if (piTestStart == -1) {
      fail("Could not find pi_test CTE in transformed SQL");
    }

    // Find the end of the CTE by counting parentheses
    int openParens = 0;
    int closeParens = 0;
    int currentPos = piTestStart;
    int piTestEnd = -1;

    while (currentPos < transformedSql.length()) {
      char c = transformedSql.charAt(currentPos);
      if (c == '(') openParens++;
      if (c == ')') {
        closeParens++;
        if (openParens == closeParens) {
          piTestEnd = currentPos;
          break;
        }
      }
      currentPos++;
    }

    if (piTestEnd == -1) {
      fail("Could not find end of pi_test CTE");
    }

    String piTestCte = transformedSql.substring(piTestStart, piTestEnd + 1);

    // Extract WHERE clause
    int whereStart = piTestCte.indexOf(" WHERE ");
    if (whereStart == -1) {
      fail("Could not find WHERE clause in pi_test CTE");
    }

    String whereClause = piTestCte.substring(whereStart);

    assertTrue(
        whereClause.contains(")) OR (("),
        "Should have parentheses around OR conditions in "
            + testDescription
            + "\nWHERE clause: "
            + whereClause);
  }

  private String normalizeSqlQuery(String sql) {
    // 1. Convert to lowercase
    // 2. Remove all whitespace
    // 3. Format SQL using SqlFormatter
    // 4. Remove any remaining whitespace variations
    return SqlFormatter.format(sql).toLowerCase().replaceAll("\\s+", " ").trim();
  }
}
