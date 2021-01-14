package org.hisp.dhis.outlierdetection.service;

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

import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.getOrgUnitPathClause;
import static org.hisp.dhis.period.PeriodType.getIsoPeriod;

import java.util.List;

import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.hisp.dhis.period.PeriodType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * Manager for database queries related to outlier data detection
 * based on z-score.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Repository
public class ZScoreOutlierDetectionManager
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ZScoreOutlierDetectionManager( NamedParameterJdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns a list of outlier data values based on z-score for the given request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return a list of {@link OutlierValue}.
     */
    public List<OutlierValue> getOutlierValues( OutlierDetectionRequest request )
    {
        final String ouPathClause = getOrgUnitPathClause( request.getOrgUnits() );

        final String sql =
            // Outer select
            "select dvs.de_uid, dvs.ou_uid, dvs.coc_uid, dvs.aoc_uid, " +
                "dvs.de_name, dvs.ou_name, dvs.coc_name, dvs.aoc_name, dvs.value, " +
                "dvs.pe_start_date, dvs.pt_name, " +
                "stats.mean as mean, " +
                "stats.std_dev as std_dev, " +
                "abs(dvs.value::double precision - stats.mean) as mean_abs_dev, " +
                "abs(dvs.value::double precision - stats.mean) / stats.std_dev as z_score, " +
                "stats.mean - (stats.std_dev * :threshold) as lower_bound, " +
                "stats.mean + (stats.std_dev * :threshold) as upper_bound " +
            // Data value query
            "from (" +
                "select dv.dataelementid, dv.sourceid, dv.periodid, dv.categoryoptioncomboid, dv.attributeoptioncomboid, " +
                "de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, " +
                "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, " +
                "pe.startdate as pe_start_date, pt.name as pt_name, " +
                "dv.value " +
                "from datavalue dv " +
                "inner join dataelement de on dv.dataelementid = de.dataelementid " +
                "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
                "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid " +
                "inner join period pe on dv.periodid = pe.periodid " +
                "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
                "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
                "where dv.dataelementid in (:data_element_ids) " +
                "and pe.startdate >= :start_date " +
                "and pe.enddate <= :end_date " +
                "and " + ouPathClause + " " +
                "and dv.deleted is false" +
            ") as dvs " +
            // Mean and std dev mapping query
            "inner join (" +
                "select dv.dataelementid as dataelementid, dv.sourceid as sourceid, " +
                "dv.categoryoptioncomboid as categoryoptioncomboid, " +
                "dv.attributeoptioncomboid as attributeoptioncomboid, " +
                "avg(dv.value::double precision) as mean, " +
                "stddev_pop(dv.value::double precision) as std_dev " +
                "from datavalue dv " +
                "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
                "where dv.dataelementid in (:data_element_ids) " +
                "and " + ouPathClause + " " +
                "and dv.deleted is false " +
                "group by dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid" +
            ") as stats " +
            // Query join
            "on dvs.dataelementid = stats.dataelementid " +
            "and dvs.sourceid = stats.sourceid " +
            "and dvs.categoryoptioncomboid = stats.categoryoptioncomboid " +
            "and dvs.attributeoptioncomboid = stats.attributeoptioncomboid " +
            "where stats.std_dev != 0.0 " +
            // Filter on z-score threshold
            "and (abs(dvs.value::double precision - stats.mean) / stats.std_dev) >= :threshold " +
            // Order and limit
            "order by " + request.getOrderBy().getKey() + " desc " +
            "limit :max_results;";

        final SqlParameterSource params = new MapSqlParameterSource()
            .addValue( "threshold", request.getThreshold() )
            .addValue( "data_element_ids", request.getDataElementIds() )
            .addValue( "start_date", request.getStartDate() )
            .addValue( "end_date", request.getEndDate() )
            .addValue( "max_results", request.getMaxResults() );

        final Calendar calendar = PeriodType.getCalendar();

        try
        {
            return jdbcTemplate.query( sql, params, getRowMapper( calendar ) );
        }
        catch ( DataIntegrityViolationException ex )
        {
            // Casting non-numeric data to double, catching exception is faster than filtering

            log.error( ErrorCode.E2207.getMessage(), ex );

            throw new IllegalQueryException( ErrorCode.E2207 );
        }
    }

    /**
     * Returns a {@link RowMapper} for {@link OutlierValue}.
     *
     * @param calendar the {@link Calendar}.
     * @return a {@link RowMapper}.
     */
    private RowMapper<OutlierValue> getRowMapper( final Calendar calendar )
    {
        return ( rs, rowNum ) -> {
            final OutlierValue outlier = new OutlierValue();

            final String isoPeriod = getIsoPeriod( calendar,
                rs.getString( "pt_name" ), rs.getDate( "pe_start_date" ) );

            outlier.setDe( rs.getString( "de_uid" ) );
            outlier.setDeName( rs.getString( "de_name" ) );
            outlier.setPe( isoPeriod );
            outlier.setOu( rs.getString( "ou_uid" ) );
            outlier.setOuName( rs.getString( "ou_name" ) );
            outlier.setCoc( rs.getString( "coc_uid" ) );
            outlier.setCocName( rs.getString( "coc_name" ) );
            outlier.setAoc( rs.getString( "aoc_uid" ) );
            outlier.setAocName( rs.getString( "aoc_name" ) );
            outlier.setValue( rs.getDouble( "value" ) );
            outlier.setMean( rs.getDouble( "mean" ) );
            outlier.setStdDev( rs.getDouble( "std_dev" ) );
            outlier.setAbsDev( rs.getDouble( "mean_abs_dev" ) );
            outlier.setZScore( rs.getDouble( "z_score" ) );
            outlier.setLowerBound( rs.getDouble( "lower_bound" ) );
            outlier.setUpperBound( rs.getDouble( "upper_bound" ) );

            return outlier;
        };
    }
}

