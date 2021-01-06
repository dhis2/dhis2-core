package org.hisp.dhis.outlierdetection.service;

import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.getIsoPeriod;
import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.getOrgUnitPathClause;

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
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Manager for database queries related to outlier data detection
 * based on min-max values.
 *
 * @author Lars Helge Overland
 */
@Slf4j
@Service
public class MinMaxOutlierDetectionManager
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MinMaxOutlierDetectionManager( NamedParameterJdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns a list of outlier data values based on min-max values for the given request.
     *
     * @param request the {@link OutlierDetectionRequest}.
     * @return a list of {@link OutlierValue}.
     */
    public List<OutlierValue> getOutlierValues( OutlierDetectionRequest request )
    {
        final String ouPathClause = getOrgUnitPathClause( request.getOrgUnits() );

        final String sql =
            "select de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, " +
                "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, " +
                "pe.startdate as pe_start_date, pt.name as pt_name, " +
                "dv.value::double precision, " +
                "least(abs(dv.value::double precision - mm.minimumvalue), abs(dv.value::double precision - mm.maximumvalue)) as bound_dev, " +
                "mm.minimumvalue as lower_bound, " +
                "mm.maximumvalue as upper_bound " +
            "from datavalue dv " +
            "inner join dataelement de on dv.dataelementid = de.dataelementid " +
            "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
            "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid " +
            "inner join period pe on dv.periodid = pe.periodid " +
            "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid " +
            "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
            "left join minmaxdataelement mm on (dv.dataelementid = mm.dataelementid " +
                "and dv.sourceid = mm.sourceid and dv.categoryoptioncomboid = mm.categoryoptioncomboid) " +
            "where dv.dataelementid in (:data_element_ids) " +
            "and pe.startdate >= :start_date " +
            "and pe.enddate <= :end_date " +
            "and " + ouPathClause + " " +
            "and dv.deleted is false " +
            "and (dv.value::double precision < mm.minimumvalue or dv.value::double precision > mm.maximumvalue) " +
            "order by bound_dev desc " +
            "limit :max_results;";

        final SqlParameterSource params = new MapSqlParameterSource()
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
            outlier.setMean( rs.getDouble( "bound_dev" ) );
            outlier.setLowerBound( rs.getDouble( "lower_bound" ) );
            outlier.setUpperBound( rs.getDouble( "upper_bound" ) );

            return outlier;
        };
    }
}
