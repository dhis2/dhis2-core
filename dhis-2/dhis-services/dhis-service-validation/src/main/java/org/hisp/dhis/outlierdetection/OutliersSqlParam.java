package org.hisp.dhis.outlierdetection;

public enum OutliersSqlParam {
    THRESHOLD("threshold"),
    DATA_ELEMENT_IDS("data_element_ids"),
    START_DATE("start_date"),
    END_DATE("end_date"),
    DATA_START_DATE("data_start_date"),
    DATA_END_DATE("data_end_date"),
    MAX_RESULTS("max_results");
    private final String key;

    OutliersSqlParam(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
