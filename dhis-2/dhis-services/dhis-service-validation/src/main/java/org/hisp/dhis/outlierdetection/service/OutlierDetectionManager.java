package org.hisp.dhis.outlierdetection.service;

import java.util.List;

import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutlierValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

@Service
public class OutlierDetectionManager
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutlierDetectionManager( NamedParameterJdbcTemplate jdbcTemplate )
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OutlierValue> getOutliers( OutlierDetectionRequest query )
    {
        String ouPathClause = getOrgUnitPathClause( query );

        String sql =
            // Outer selection
            "select dvs.de_uid, dvs.periodid, dvs.ou_uid, dvs.coc_uid, dvs.aoc_uid, dvs.value, " +
                "stats.mean as mean, " +
                "stats.std_dev as std_dev, " +
                "abs(dvs.value::double precision - stats.mean) as mean_abs_dev, " +
                "abs(dvs.value::double precision - stats.mean) / stats.std_dev as z_score," +
                "stats.mean - (stats.std_dev * :threshold) as lower_bound," +
                "stats.mean + (stats.std_dev * :threshold) as upper_bound" +
            // Data value query
            "from (" +
                "select dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid," +
                "de.uid as de_uid, dv.periodid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, dv.value " +
                "from datavalue dv" +
                "inner join dataelement de on dv.dataelementid = de.dataelementid" +
                "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid " +
                "inner join categoryoptioncombo aoc on dv.categoryoptioncomboid = aoc.categoryoptioncomboid " +
                "inner join period pe on dv.periodid = pe.periodid " +
                "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
                "where dv.dataelementid in (:data_element_ids)" +
                "and pe.startdate >= ':start_date'" +
                "and pe.enddate <= ':end_date'" +
                "and " + ouPathClause +
                "and dv.deleted is false" +
            ") as dvs" +
            // Mean and std dev mapping query
            "inner join (" +
                "select dv.dataelementid as dataelementid, dv.sourceid as sourceid, " +
                "dv.categoryoptioncomboid as categoryoptioncomboid, " +
                "dv.attributeoptioncomboid as attributeoptioncomboid," +
                "avg(dv.value::double precision) as mean, " +
                "stddev_pop(dv.value::double precision) as std_dev" +
                "from datavalue dv" +
                "inner join organisationunit ou on dv.sourceid = ou.organisationunitid " +
                "where dv.dataelementid in (:data_element_ids)" +
                "and " + ouPathClause +
                "and dv.deleted is false" +
                "group by dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid" +
            ") as stats" +
            // Join data queries
            "on dvs.dataelementid = stats.dataelementid" +
            "and dvs.sourceid = stats.sourceid" +
            "and dvs.categoryoptioncomboid = stats.categoryoptioncomboid" +
            "and dvs.attributeoptioncomboid = stats.attributeoptioncomboid" +
            "where stats.std_dev != 0.0" +
            // Filter on z score, order and limit
            "and (abs(cast(dvs.value as double precision) - stats.mean) / stats.std_dev) > :threshold" +
            "order by :order_by desc" +
            "limit :max_results;";

        SqlParameterSource params = new MapSqlParameterSource()
            .addValue( "threshold", query.getThreshold() )
            .addValue( "data_element_ids", query.getDataElementIds() )
            .addValue( "start_date", query.getStartDateString() )
            .addValue( "end_date", query.getEndDateString() )
            .addValue( "oder_by", query.getOrderBy().getKey() )
            .addValue( "max_results", query.getMaxResults() );

        return jdbcTemplate.query( sql, params, (rs, rowNum) -> {
           OutlierValue outlier = new OutlierValue();
           outlier.setDe( rs.getString( "de_uid" ) );
           // TODO Period identifier
           outlier.setDe( rs.getString( "ou_uid" ) );
           outlier.setCoc( rs.getString( "coc_uid" ) );
           outlier.setAoc( rs.getString( "aoc_uid" ) );
           outlier.setValue( rs.getDouble( "value" ) );
           outlier.setMean( rs.getDouble( "mean" ) );
           outlier.setStdDev( rs.getDouble( "std_dev" ) );
           outlier.setMeanAbsDev( rs.getDouble( "mean_abs_dev" ) );
           outlier.setZScore( rs.getDouble( "z_score" ) );
           outlier.setLowerBound( rs.getDouble( "lower_bound" ) );
           outlier.setUpperBound( rs.getDouble( "upper_bound" ) );

           return outlier;
        });
    }

    /**
     * Returns an organisation unit 'path' like clause.
     *
     * @param query the {@link OutlierDetectionRequest}.
     * @return an organisation unit 'path' like clause.
     */
    private String getOrgUnitPathClause( OutlierDetectionRequest query )
    {
        String sql = "";

        for ( OrganisationUnit ou : query.getOrgUnits() )
        {
            sql += "ou.\"path\" like '" + ou.getPath() + "%' and ";
        }

        return TextUtils.removeLastAnd( sql );
    }
}
