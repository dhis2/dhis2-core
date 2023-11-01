package org.hisp.dhis.outlierdetection.processor;

import org.hisp.dhis.outlierdetection.OutlierDetectionRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.hisp.dhis.outlierdetection.OutliersSqlParam.DATA_ELEMENT_IDS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.END_DATE;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.MAX_RESULTS;
import static org.hisp.dhis.outlierdetection.OutliersSqlParam.START_DATE;
import static org.hisp.dhis.outlierdetection.util.OutlierDetectionUtils.getOrgUnitPathClause;

public class AnalyticsMinMaxSqlStatementProcessor implements IOutlierSqlStatementProcessor {
    @Override
    public String getSqlStatement(OutlierDetectionRequest request) {
        final String ouPathClause = getOrgUnitPathClause(request.getOrgUnits());

        return
                "select de.uid as de_uid, ou.uid as ou_uid, coc.uid as coc_uid, aoc.uid as aoc_uid, "
                        + "de.name as de_name, ou.name as ou_name, coc.name as coc_name, aoc.name as aoc_name, "
                        + "pe.startdate as pe_start_date, pt.name as pt_name, "
                        + "dv.value::double precision as value, dv.followup as follow_up, "
                        + "least(abs(dv.value::double precision - mm.minimumvalue), "
                        + "abs(dv.value::double precision - mm.maximumvalue)) as bound_abs_dev, "
                        + "mm.minimumvalue as lower_bound, "
                        + "mm.maximumvalue as upper_bound "
                        + "from datavalue dv "
                        + "inner join dataelement de on dv.dataelementid = de.dataelementid "
                        + "inner join categoryoptioncombo coc on dv.categoryoptioncomboid = coc.categoryoptioncomboid "
                        + "inner join categoryoptioncombo aoc on dv.attributeoptioncomboid = aoc.categoryoptioncomboid "
                        + "inner join period pe on dv.periodid = pe.periodid "
                        + "inner join periodtype pt on pe.periodtypeid = pt.periodtypeid "
                        + "inner join organisationunit ou on dv.sourceid = ou.organisationunitid "
                        +
                        // Min-max value join
                        "inner join minmaxdataelement mm on (dv.dataelementid = mm.dataelementid "
                        + "and dv.sourceid = mm.sourceid and dv.categoryoptioncomboid = mm.categoryoptioncomboid) "
                        + "where dv.dataelementid in (:" + DATA_ELEMENT_IDS.getKey() + ") "
                        + "and pe.startdate >= :" + START_DATE.getKey() + " "
                        + "and pe.enddate <= :" + END_DATE.getKey() + " "
                        + "and "
                        + ouPathClause
                        + " "
                        + "and dv.deleted is false "
                        +
                        // Filter for values outside the min-max range
                        "and (dv.value::double precision < mm.minimumvalue or dv.value::double precision > mm.maximumvalue) "
                        +
                        // Order and limit
                        "order by bound_abs_dev desc "
                        + "limit :" + MAX_RESULTS.getKey() + ";";
    }

    @Override
    public SqlParameterSource getSqlParameterSource(OutlierDetectionRequest request) {
        return new MapSqlParameterSource()
                        .addValue(DATA_ELEMENT_IDS.getKey(), request.getDataElementIds())
                        .addValue(START_DATE.getKey(), request.getStartDate())
                        .addValue(END_DATE.getKey(), request.getEndDate())
                        .addValue(MAX_RESULTS.getKey(), request.getMaxResults());
    }
}
