package org.hisp.dhis.analytics.util;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgramIndicatorCteOptimizerTest {

    @Test
    public void testLastScheduled() {
        ProgramIndicatorCteOptimizer transformer = new ProgramIndicatorCteOptimizer();

        String originalQuery = """
                    WITH pi_hgtnuhsqbml AS (
                        SELECT
                            enrollment
                        FROM analytics_enrollment_ur1edk5oe2n AS subax
                        WHERE ((
                            date_part('year',age(cast(
                                                 (
                                                 SELECT   scheduleddate
                                                 FROM     analytics_event_ur1edk5oe2n
                                                 WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                                 AND      scheduleddate IS NOT NULL
                                                 ORDER BY occurreddate DESC
                                                 LIMIT    1 ) AS date), cast(coalesce(completeddate,
                                      (
                                               SELECT   created
                                               FROM     analytics_event_ur1edk5oe2n
                                               WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                               AND      created IS NOT NULL
                                               ORDER BY occurreddate DESC
                                               LIMIT    1 )) AS date)))) * 12 + date_part('month',age(cast(
                                                        (
                                                        SELECT   scheduleddate
                                                        FROM     analytics_event_ur1edk5oe2n
                                                        WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                                        AND      scheduleddate IS NOT NULL
                                                        ORDER BY occurreddate DESC
                                                        LIMIT    1 ) AS date), cast(coalesce(completeddate,
                             (
                                      SELECT   created
                                      FROM     analytics_event_ur1edk5oe2n
                                      WHERE    analytics_event_ur1edk5oe2n.enrollment = subax.enrollment
                                      AND      created IS NOT NULL
                                      ORDER BY occurreddate DESC
                                      LIMIT    1 )) AS date)))) > 1
                    GROUP BY enrollment
                )
                SELECT
                    ax.enrollment
                FROM analytics_enrollment_ur1edk5oe2n AS ax
                LEFT JOIN pi_hgtnuhsqbml kektm ON kektm.enrollment = ax.enrollment
                WHERE (lastupdated >= '2015-01-01' AND lastupdated < '2025-01-01')
                AND ax."uidlevel1" = 'ImspTQPwCqd'
                LIMIT 101
                OFFSET 0
                """;

        String expected = """
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
                    date_part('year',
                    age(cast(ls.scheduleddate as date),
                    cast(coalesce(completeddate,
                    lc.created) as date))) * 12 + date_part('month',
                    age(cast(ls.scheduleddate as date),
                    cast(coalesce(completeddate,
                    lc.created) as date))) > 1
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

        String transformedQuery = transformer.transformSQL(originalQuery, false);

        assertEquals(normalizeSqlQuery(expected), normalizeSqlQuery(transformedQuery),
                "SQL queries should be equivalent after normalization");
    }

    @Test
    public void testComplex() {
        ProgramIndicatorCteOptimizer transformer = new ProgramIndicatorCteOptimizer();

        String originalQuery = """
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

        String expected = """
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
                    date_part('year',
                    age(cast(enrollmentdate as date),
                    cast(lc.created as date))) >= 5
                        and date_part('year',
                        age(cast(enrollmentdate as date),
                        cast(completeddate as date))) < 1
                            and coalesce(cast("cejWyOfXge6" as text),
                            '') = 'MALE'
                                and coalesce(cast(lv_H6uSAMO5WLD."H6uSAMO5WLD" as text),
                                '') = 'RDT'
                                    or "w75KJ2mc4zz" is not null
                                    and coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                                    '') = 'POSITIVE'
                                        or coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                                        '') = 'NEGATIVE'
                                            or not 3.14 = 0
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

        String transformedQuery = transformer.transformSQL(originalQuery, false);
        System.out.println(transformedQuery);
        assertEquals(normalizeSqlQuery(expected), normalizeSqlQuery(transformedQuery),
                "SQL queries should be equivalent after normalization");
    }


    @Test
    public void testRelationshipCount() {
        ProgramIndicatorCteOptimizer transformer = new ProgramIndicatorCteOptimizer();

        String originalQuery = """
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

        String expected = """
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
                    date_part('year',
                    age(cast(enrollmentdate as date),
                    cast(lc.created as date))) >= 5
                        and date_part('year',
                        age(cast(enrollmentdate as date),
                        cast(completeddate as date))) < 1
                            and coalesce(cast("cejWyOfXge6" as text),
                            '') = 'MALE'
                                and coalesce(cast(lv_H6uSAMO5WLD."H6uSAMO5WLD" as text),
                                '') = 'RDT'
                                    or "w75KJ2mc4zz" is not null
                                    and coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                                    '') = 'POSITIVE'
                                        or coalesce(cast(lv_cYGaxwK615G."cYGaxwK615G" as text),
                                        '') = 'NEGATIVE'
                                            or not 3.14 = 0
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

        String transformedQuery = transformer.transformSQL(originalQuery, false);
        System.out.println(transformedQuery);
        assertEquals(normalizeSqlQuery(expected), normalizeSqlQuery(transformedQuery),
                "SQL queries should be equivalent after normalization");
    }


    private String normalizeSqlQuery(String sql) {
        // 1. Convert to lowercase
        // 2. Remove all whitespace
        // 3. Format SQL using SqlFormatter
        // 4. Remove any remaining whitespace variations
        return SqlFormatter.format(sql)
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
}