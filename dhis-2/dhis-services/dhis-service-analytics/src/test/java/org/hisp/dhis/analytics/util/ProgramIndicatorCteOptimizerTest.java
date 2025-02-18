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
package org.hisp.dhis.analytics.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import org.hisp.dhis.analytics.util.optimizer.cte.pipeline.CteOptimizationPipeline;
import org.junit.jupiter.api.Test;

class ProgramIndicatorCteOptimizerTest {

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
              sum(1 + 1) as value
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
    System.out.println(transformedSql);
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
  public void testLastScheduledComplex() {

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
  public void testComplex() {

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
            sum(1 + 1) as value
        from
            analytics_enrollment_iphinat79uw as subax
        left join last_created as lc on
            subax.enrollment = lc.enrollment
        left join last_value_h6usamo5wld as lv_H6uSAMO5WLD on
            subax.enrollment = lv_H6uSAMO5WLD.enrollment
        left join last_value_cygaxwk615g as lv_cYGaxwK615G on
            subax.enrollment = lv_cYGaxwK615G.enrollment
        where
            (date_part('year',
            age(cast(enrollmentdate as date),
            cast(lc.created as date)))) >= 5
                and (date_part('year',
                age(cast(enrollmentdate as date),
                cast(completeddate as date)))) < 1
                    and coalesce(cast("cejWyOfXge6" as text),
                    '') = 'MALE'
                        and (coalesce(cast(lv_H6uSAMO5WLD."H6uSAMO5WLD" as text),
                        '') = 'RDT'
                            or "w75KJ2mc4zz" is not null)
                        and (coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                        '') = 'POSITIVE'
                            or coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                            '') = 'NEGATIVE')
                            or not (3.14 = 0)
                                or '2015-01-01' is not null
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
  public void testRelationshipCountComplex() {

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
            sum(1 + 1) as value
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
            (((date_part('year',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date)))) * 12 + date_part('month',
            age(cast(ls.scheduleddate as date),
            cast(coalesce(completeddate,
            lc.created) as date))))) > 1
                or nullif(cast((0) as double precision),
                0) > 2
                    or not (dec_decount.de_count > 0)
                        and (((extract(epoch
                    from
                        (cast(completeddate as timestamp) - cast(enrollmentdate as timestamp)))) / 60)) > 1
                            and ("cejWyOfXge6" is not null)
                                and rlc.relationship_count > 0
                                and "lw1SqmMlnfh" is not null
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

  private String normalizeSqlQuery(String sql) {
    // 1. Convert to lowercase
    // 2. Remove all whitespace
    // 3. Format SQL using SqlFormatter
    // 4. Remove any remaining whitespace variations
    return SqlFormatter.format(sql).toLowerCase().replaceAll("\\s+", " ").trim();
  }
}
