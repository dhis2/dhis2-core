package org.hisp.dhis.outlierdetection.processor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.Order;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Date;
import java.util.List;

import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_ELEMENT_IDS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.THRESHOLD;

public class ZScoreSqlStatementProcessor implements IOutlierSqlStatementProcessor {
    @Override
    public String getSqlStatement(OutlierDetectionRequest request) {
        final String ouPathClause = getOrgUnitPathClause(request.getOrgUnits());
        final String dataStartDateClause = getDataStartDateClause(request.getDataStartDate());
        final String dataEndDateClause = getDataEndDateClause(request.getDataEndDate());

        final boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;
        final String middle_stats_calc =
                modifiedZ
                        ? "percentile_cont(0.5) within group(order by dv.value::double precision)"
                        : "avg(dv.value::double precision)";

        String order =
                request.getOrderBy() == Order.MEAN_ABS_DEV
                        ? "middle_value_abs_dev"
                        : request.getOrderBy().getKey();

        String thresholdParam = THRESHOLD.getKey();

        return
                "select dvs.de_uid, dvs.ou_uid, dvs.coc_uid, dvs.aoc_uid, "
                        + "dvs.de_name, dvs.ou_name, dvs.coc_name, dvs.aoc_name, dvs.value, dvs.follow_up, "
                        + "dvs.pe_start_date, dvs.pt_name, "
                        + "stats.middle_value as middle_value, "
                        + "stats.std_dev as std_dev, "
                        + "abs(dvs.value::double precision - stats.middle_value) as middle_value_abs_dev, "
                        + "abs(dvs.value::double precision - stats.middle_value) / stats.std_dev as z_score, "
                        + "stats.middle_value - (stats.std_dev * :" + thresholdParam + ") as lower_bound, "
                        + "stats.middle_value + (stats.std_dev * :" + thresholdParam + ") as upper_bound "
                        +
                        // Data value query
                        "from ("
                        + "select dv.dataelementid, dv.sourceid, dv.periodid, "
                        + "dv.categoryoptioncomboid, dv.attributeoptioncomboid, "
                        + "de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, "
                        + "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, "
                        + "pe.startdate as pe_start_date, pt.name as pt_name, "
                        + "dv.value as value, dv.followup as follow_up "
                        + "from datavalue dv "
                        + "inner join dataelement de on dv.dataelementid = de.dataelementid "
                        + "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid "
                        + "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid "
                        + "inner join period pe on dv.periodid = pe.periodid "
                        + "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid "
                        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
                        + "where dv.dataelementid in (:" + DATA_ELEMENT_IDS.getKey() + ") "
                        + "and pe.startdate >= :" + START_DATE.getKey() + " "
                        + "and pe.enddate <= :" + END_DATE.getKey() + " "
                        + "and "
                        + ouPathClause
                        + " "
                        + "and dv.deleted is false"
                        + ") as dvs "
                        +
                        // Mean or Median and std dev mapping query
                        "inner join ("
                        + "select dv.dataelementid as dataelementid, dv.sourceid as sourceid, "
                        + "dv.categoryoptioncomboid as categoryoptioncomboid, "
                        + "dv.attributeoptioncomboid as attributeoptioncomboid, "
                        + middle_stats_calc
                        + " as middle_value, "
                        + "stddev_pop(dv.value::double precision) as std_dev "
                        + "from datavalue dv "
                        + "inner join period pe on dv.periodid = pe.periodid "
                        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
                        + "where dv.dataelementid in (:" + DATA_ELEMENT_IDS.getKey() + ") "
                        + dataStartDateClause
                        + dataEndDateClause
                        + "and "
                        + ouPathClause
                        + " "
                        + "and dv.deleted is false "
                        + "group by dv.dataelementid, dv.sourceid, dv.categoryoptioncomboid, dv.attributeoptioncomboid"
                        + ") as stats "
                        +
                        // Query join
                        "on dvs.dataelementid = stats.dataelementid "
                        + "and dvs.sourceid = stats.sourceid "
                        + "and dvs.categoryoptioncomboid = stats.categoryoptioncomboid "
                        + "and dvs.attributeoptioncomboid = stats.attributeoptioncomboid "
                        + "where stats.std_dev != 0.0 "
                        +
                        // Filter on z-score threshold
                        "and (abs(dvs.value::double precision - stats.middle_value) / stats.std_dev) >= :" + thresholdParam + " "
                        +
                        // Order and limit
                        "order by "
                        + order
                        + " desc "
                        + "limit :" + MAX_RESULTS.getKey() + ";";
    }

    @Override
    public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
        return new MapSqlParameterSource()
                .addValue(THRESHOLD.getKey(), request.getThreshold())
                .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
                .addValue(START_DATE.getKey(), request.getStartDate())
                .addValue(END_DATE.getKey(), request.getEndDate())
                .addValue(DATA_START_DATE.getKey(), request.getDataStartDate())
                .addValue(DATA_END_DATE.getKey(), request.getDataEndDate())
                .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
    }

    private String getOrgUnitPathClause(List<OrganisationUnit> orgUnits) {
        StringBuilder sql = new StringBuilder("(");

        for (OrganisationUnit ou : orgUnits) {
            sql.append("ou.\"path\" like '").append(ou.getPath()).append("%' or ");
        }

        return StringUtils.trim(TextUtils.removeLastOr(sql.toString())) + ")";
    }

    private String getDataStartDateClause(Date dataStartDate) {
        return dataStartDate != null ? "and pe.startdate >= :" + DATA_START_DATE.getKey() + " " : StringUtils.EMPTY;
    }

    private String getDataEndDateClause(Date dataStartDate) {
        return dataStartDate != null ? "and pe.enddate <= :" + DATA_END_DATE.getKey() + " " : StringUtils.EMPTY;
    }
}
