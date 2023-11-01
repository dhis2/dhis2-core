package org.hisp.dhis.outlierdetection.processor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outlierdetection.Order;
import org.hisp.dhis.outlierdetection.OutlierDetectionAlgorithm;
import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.hisp.dhis.outlierdetection.OutliersSqlParam;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_ELEMENT_IDS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.START_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.THRESHOLD;

public class AnalyticsZScoreSqlStatementProcessor implements IOutlierSqlStatementProcessor {
    @Override
    public String getSqlStatement(OutlierDetectionRequest request) {
        final String ouPathClause = getOrgUnitPathClause(request.getOrgUnits());

        final boolean modifiedZ = request.getAlgorithm() == OutlierDetectionAlgorithm.MOD_Z_SCORE;

        String middle_value = modifiedZ ? " ax.percentile_middle_value":" ax.avg_middle_value";

        String order =
                request.getOrderBy() == Order.MEAN_ABS_DEV
                        ? "middle_value_abs_dev"
                        : request.getOrderBy().getKey();
        String thresholdParam = OutliersSqlParam.THRESHOLD.getKey();

        return  "select * from (select " +
                        "ax.dataelementid, " +
                        "ax.de_uid, " +
                        "ax.ou_uid, " +
                        "ax.coc_uid, " +
                        "ax.aoc_uid, " +
                        "ax.de_name, " +
                        "ax.ou_name, " +
                        "ax.coc_name, " +
                        "ax.aoc_name, " +
                        "ax.value, " +
                        "ax.follow_up, " +
                        "ax.startdate as pe_start_date, " +
                        "ax.pt_name, " +
                        middle_value + " as middle_value, " +
                        "ax.std_dev as std_dev, " +
                        "abs(ax.value::double precision - " + middle_value + ") as middle_value_abs_dev, " +
                        "(case when ax.std_dev = 0 then 0 " +
                        "      else abs(ax.value::double precision - " +
                        middle_value +" ) / ax.std_dev " +
                        "       end) as z_score, " +
                        middle_value + " - (ax.std_dev * :" + thresholdParam + ") as lower_bound, " +
                        middle_value + " + (ax.std_dev * :" + thresholdParam + ") as upper_bound " +
                        "from analytics_2023_ext ax " +
                        "where dataelementid in  (:" + DATA_ELEMENT_IDS.getKey() + ") " +
                        "and " + ouPathClause + " "+
                        "and ax.startdate >= :" + START_DATE.getKey() + " " +
                        "and ax.enddate <= :" + END_DATE.getKey() + ") t1 " +
                        "where t1.z_score > :" + thresholdParam + " " +
                        "order by " + order + " desc " +
                        "limit :" + MAX_RESULTS.getKey() + " ";
    }

    @Override
    public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
        return new MapSqlParameterSource()
                .addValue(THRESHOLD.getKey(), request.getThreshold())
                .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
                .addValue(START_DATE.getKey(), request.getStartDate())
                .addValue(END_DATE.getKey(), request.getEndDate())
                .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
    }

    private String getOrgUnitPathClause(List<OrganisationUnit> orgUnits) {
        StringBuilder sql = new StringBuilder("(");

        for (OrganisationUnit ou : orgUnits) {
            sql.append("ax.\"path\" like '").append(ou.getPath()).append("%' or ");
        }

        return StringUtils.trim(TextUtils.removeLastOr(sql.toString())) + ")";
    }
}


